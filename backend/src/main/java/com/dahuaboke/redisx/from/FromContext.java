package com.dahuaboke.redisx.from;

import com.dahuaboke.redisx.Context;
import com.dahuaboke.redisx.common.ConcurrentLinkedMap;
import com.dahuaboke.redisx.common.Constants;
import com.dahuaboke.redisx.common.cache.CacheManager;
import com.dahuaboke.redisx.common.command.from.SyncCommand;
import com.dahuaboke.redisx.common.enums.FilterType;
import com.dahuaboke.redisx.common.enums.Mode;
import com.dahuaboke.redisx.handler.ClusterInfoHandler;
import com.dahuaboke.redisx.handler.SentinelInfoHandler;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 2024/5/6 16:18
 * auth: dahua
 * desc: 从节点上下文
 */
public class FromContext extends Context {

    private static final Logger logger = LoggerFactory.getLogger(FromContext.class);
    private Channel fromChannel;
    private int slotBegin;
    private int slotEnd;
    private FromClient fromClient;
    private boolean rdbAckOffset = false;
    private boolean alwaysFullSync;
    private boolean syncRdb;
    private int unSyncCommandLength = 0;
    private String masterId;
    private boolean isNodesInfoContext;
    private CountDownLatch nodesInfoFlag;
    private String fromMasterName;
    private boolean connectFromMaster;
    private ConcurrentLinkedMap<SyncCommand, Integer> offsetCache = new ConcurrentLinkedMap<>();
    private boolean onlyRdb;
    private boolean filterEnable;
    private String filterCharset;
    private FilterType filterType;

    public FromContext(CacheManager cacheManager, String host, int port, boolean startConsole, boolean startByConsole, Mode fromMode, Mode toMode, boolean alwaysFullSync, boolean syncRdb, boolean isNodesInfoContext, String fromMasterName, boolean connectFromMaster, boolean isGetMasterNodeInfo, boolean onlyRdb, boolean filterEnable, String filterCharset, FilterType filterType) {
        super(cacheManager, host, port, fromMode, toMode, startConsole, startByConsole);
        if (startByConsole) {
            replyQueue = new LinkedBlockingDeque();
        }
        this.alwaysFullSync = alwaysFullSync;
        this.syncRdb = syncRdb;
        this.isNodesInfoContext = isNodesInfoContext;
        this.fromMasterName = fromMasterName;
        this.connectFromMaster = connectFromMaster;
        if (isNodesInfoContext) {
            nodesInfoFlag = new CountDownLatch(1);
        } else if (Mode.CLUSTER == fromMode) {
            ClusterInfoHandler.SlotInfo fromClusterNodeInfo = cacheManager.getFromClusterNodeInfoByIpAndPort(host, port);
            if (fromClusterNodeInfo != null) {
                this.slotBegin = fromClusterNodeInfo.getSlotStart();
                this.slotEnd = fromClusterNodeInfo.getSlotEnd();
            } else {
                throw new IllegalStateException("Slot info error");
            }
        }
        this.isGetMasterNodeInfo = isGetMasterNodeInfo;
        this.onlyRdb = onlyRdb;
        this.filterEnable = filterEnable;
        this.filterCharset = filterCharset;
        this.filterType = filterType;
    }

    public String getId() {
        return cacheManager.getId();
    }

    public boolean publish(SyncCommand command) {
        if (!startByConsole) {
            if (command.isNeedAddLengthToOffset()) {
                offsetCache.putIndex(command);
            }
            return cacheManager.publish(command);
        } else {
            if (replyQueue == null) {
                throw new IllegalStateException("By console mode replyQueue need init");
            } else {
                return replyQueue.offer(command.getStringCommand());
            }
        }
    }

    public void setFromChannel(Channel fromChannel) {
        this.fromChannel = fromChannel;
    }

    public String getLocalHost() {
        InetSocketAddress inetSocketAddress = (InetSocketAddress) this.fromChannel.localAddress();
        return inetSocketAddress.getHostString();
    }

    public int getLocalPort() {
        InetSocketAddress inetSocketAddress = (InetSocketAddress) this.fromChannel.localAddress();
        return inetSocketAddress.getPort();
    }

    public void setFromClient(FromClient fromClient) {
        this.fromClient = fromClient;
    }

    @Override
    public boolean isAdapt(Mode mode, byte[] command) {
        if (Mode.CLUSTER == mode && command != null) {
            int hash = calculateHash(command) % Constants.COUNT_SLOT_NUMS;
            return hash >= slotBegin && hash <= slotEnd;
        } else {
            //哨兵模式或者单节点则只存在一个为ToContext类型的context
            return true;
        }
    }

    @Override
    public String sendCommand(Object command, int timeout) {
        if (replyQueue == null) {
            throw new IllegalStateException("By console mode replyQueue need init");
        } else {
            replyQueue.clear();
            fromClient.sendCommand((String) command);
            try {
                return replyQueue.poll(timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                return null;
            }
        }
    }

    public void close() {
        close(false);
    }

    public boolean close(boolean isSync) {
        cacheManager.remove(this);
        if (nodesInfoFlag != null) {
            nodesInfoFlag.countDown();
        }
        return this.fromClient.destroy(isSync);
    }

    public long getOffset() {
        return cacheManager.getNodeMessage(host, port).getOffset();
    }

    public void setOffset(long offset) {
        cacheManager.setNodeMessage(this.host, this.port, masterId, offset);
    }

    public CacheManager.NodeMessage getNodeMessage() {
        return cacheManager.getNodeMessage(this.host, this.port);
    }

    public void ackOffset() {
        if (fromChannel != null && fromChannel.isActive() && fromChannel.pipeline().get(Constants.INIT_SYNC_HANDLER_NAME) == null) {
            offsetAddUp();
            CacheManager.NodeMessage nodeMessage = getNodeMessage();
            if (nodeMessage != null) {
                long offset = getOffset() + unSyncCommandLength;
                fromChannel.writeAndFlush(Constants.ACK_COMMAND_PREFIX + offset);
                logger.trace("Ack offset [{}]", offset);
            }
        }
    }

    public synchronized int offsetAddUp() {
        SyncCommand command;
        int size = 0;
        while (offsetCache != null && (command = offsetCache.getFirstKey()) != null) {
            Integer value = offsetCache.get(command);
            if (value != null) {
                long offset = getOffset();
                setOffset(offset + value);
                offsetCache.removeKey(command);
            } else {
                break;
            }
        }
        if (offsetCache != null) {
            size = offsetCache.size();
        }
        return size;
    }

    public void cacheOffset(SyncCommand syncCommand) {
        offsetCache.putValue(syncCommand, syncCommand.getSyncLength());
    }

    public boolean checkCacheOffsetIsEmpty() {
        return offsetCache == null || offsetCache.size() == 0;
    }

    public boolean isRdbAckOffset() {
        return rdbAckOffset;
    }

    public void setRdbAckOffset(boolean rdbAckOffset) {
        this.rdbAckOffset = rdbAckOffset;
    }

    public String getPassword() {
        return cacheManager.getFromPassword();
    }

    public boolean isAlwaysFullSync() {
        return alwaysFullSync;
    }

    public boolean redisVersionBeyond3() {
        return cacheManager.getRedisVersion().charAt(0) > '3';
    }

    public boolean isSyncRdb() {
        return syncRdb;
    }

    public int getUnSyncCommandLength() {
        return unSyncCommandLength;
    }

    public void appendUnSyncCommandLength(int length) {
        this.unSyncCommandLength += length;
    }

    public void clearUnSyncCommandLength() {
        this.unSyncCommandLength = 0;
    }

    public void setMasterId(String masterId) {
        this.masterId = masterId;
    }

    public void addSlotInfo(ClusterInfoHandler.SlotInfo slotInfo) {
        this.cacheManager.addFromClusterNodesInfo(slotInfo);
    }

    public void setFromNodesInfoGetSuccess() {
        if (nodesInfoFlag != null) {
            nodesInfoFlag.countDown();
        }
    }

    public boolean nodesInfoGetSuccess(int timeout) {
        try {
            return nodesInfoFlag.await(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return false;
        }
    }

    public void reSetNodesInfoFlag() {
        nodesInfoFlag = new CountDownLatch(1);
    }

    public boolean isNodesInfoContext() {
        return isNodesInfoContext;
    }

    public String getFromMasterName() {
        return fromMasterName;
    }

    public void setSentinelMasterInfo(String host, int port) {
        cacheManager.setFromSentinelMaster(new InetSocketAddress(host, port));
    }

    public boolean isConnectFromMaster() {
        return connectFromMaster;
    }

    public void addSentinelSlaveInfo(SentinelInfoHandler.SlaveInfo fromSentinelNodeInfo) {
        cacheManager.addFromSentinelNodesInfo(fromSentinelNodeInfo);
    }

    public void setFromStarted(boolean started) {
        cacheManager.setFromIsStarted(started);
    }

    public int getSlotBegin() {
        return slotBegin;
    }

    public int getSlotEnd() {
        return slotEnd;
    }

    public boolean isOnlyRdb() {
        return onlyRdb;
    }

    public boolean isFilterEnable() {
        return filterEnable;
    }

    public String getFilterCharset() {
        return filterCharset;
    }

    public FilterType getFilterType() {
        return filterType;
    }

    public boolean checkMatchFilterRules(String key) {
        for (Pattern pattern : cacheManager.getPatterns()) {
            Matcher matcher = pattern.matcher(key);
            if (matcher.matches()) {
                return true;
            }
        }
        return false;
    }
}
