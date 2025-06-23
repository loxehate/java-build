package com.dahuaboke.redisx.from.rdb.hash;


import com.dahuaboke.redisx.from.rdb.ParserManager;
import com.dahuaboke.redisx.from.rdb.base.Parser;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Desc: Redis >= 7.4 版本
 * Hash LP(ListPack) with HFEs(Hash Field expired). Doesn't attach min TTL at start (7.4 RC)
 * @Author：zhh
 * @Date：2025/2/7 14:21
 */
public class HashListPackExPreGaParser implements Parser {
    @Override
    public Map<byte[], HashExEntry> parse(ByteBuf byteBuf) {
        Map<byte[], HashExEntry> map = new HashMap<>();
        //读取字符串,该字符串是一个listPack
        byte[] bytes = ParserManager.STRING_00.parse(byteBuf);
        // 创建一个ByteBuf
        ByteBuf buf = Unpooled.buffer();
        // 将byte数组写入ByteBuf
        buf.writeBytes(bytes);
        List<byte[]> list = ParserManager.LISTPACK.parse(buf);
        for (int i = 0; i < list.size(); i += 3) {
            if (i + 2 < list.size()) {
                byte[] key = list.get(i);
                byte[] value = list.get(i + 1);
                byte[] ttl = list.get(i + 2);
                HashExEntry hashExEntry = new HashExEntry(value,ttl);
                map.put(key, hashExEntry);
            } else {
                break;
            }
        }
        return map;
    }
}
