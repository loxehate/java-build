package com.dahuaboke.redisx.from.rdb.hash;


import com.dahuaboke.redisx.from.rdb.ParserManager;
import com.dahuaboke.redisx.from.rdb.base.Parser;
import io.netty.buffer.ByteBuf;

import java.util.HashMap;
import java.util.Map;

/**
 * @Desc: Redis >= 7.4 版本
 * Hash with HFEs(Hash Field expired). Doesn't attach min TTL at start (7.4 RC)
 * @Author：zhh
 * @Date：2025/2/7 14:20
 */
public class HashExPreGaParser implements Parser {
    @Override
    public Map<byte[], HashExEntry> parse(ByteBuf byteBuf) {
        Map<byte[], HashExEntry> map = new HashMap<>();
        long len = ParserManager.LENGTH.parse(byteBuf).len;
        for (int i = 0; i < len; i++) {
            byte[] key = ParserManager.STRING_00.parse(byteBuf);
            byte[] value = ParserManager.STRING_00.parse(byteBuf);
            byte[] ttl = ParserManager.STRING_00.parse(byteBuf);
            HashExEntry hashExEntry = new HashExEntry(value,ttl);
            map.put(key, hashExEntry);
        }
        return map;
    }
}
