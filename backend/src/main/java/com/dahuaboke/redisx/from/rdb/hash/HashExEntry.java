package com.dahuaboke.redisx.from.rdb.hash;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * @Desc:
 * @Author：zhh
 * @Date：2025/2/7 16:06
 */
public class HashExEntry {
    private  byte[] value;

    private  long lastTime;

    HashExEntry(byte[] value, byte[] expireTime) {
        this.value = value;
        //转化成unix时间戳。如果key没有过期时间, expireTime就是单字节 0。 如果有过期时间,是8个字节代表unix时间戳
        String time = new String(expireTime);
        long l = Long.parseLong(time);
        //剩余多少时间
        this.lastTime = (l - System.currentTimeMillis() );
    }

    public byte[] getValue() {
        return value;
    }

    public void setValue(byte[] value) {
        this.value = value;
    }

    public long getLastTime() {
        return lastTime;
    }

    public void setLastTime(long lastTime) {
        this.lastTime = lastTime;
    }


}
