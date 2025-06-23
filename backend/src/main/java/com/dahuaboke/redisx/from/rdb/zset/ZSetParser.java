package com.dahuaboke.redisx.from.rdb.zset;

import com.dahuaboke.redisx.from.rdb.ParserManager;
import com.dahuaboke.redisx.from.rdb.base.Parser;
import io.netty.buffer.ByteBuf;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @Desc:
 * @Author：zhh
 * @Date：2024/5/20 17:13
 */
public class ZSetParser implements Parser {

    public Set<ZSetEntry> parse(ByteBuf byteBuf) {
        Set<ZSetEntry> zset = new LinkedHashSet<>();
        long len = ParserManager.LENGTH.parse(byteBuf).len;
        for (int i = 0; i < len; i++) {
            byte[] element = ParserManager.STRING_00.parse(byteBuf);
            double score = this.parseDoubleValue(byteBuf);
            zset.add(new ZSetEntry(element, score));
        }
        return zset;
    }

    public double parseDoubleValue(ByteBuf byteBuf) {
        int len = byteBuf.readByte() & 0xFF;
        switch (len) {
            case 255:
                return Double.NEGATIVE_INFINITY;
            case 254:
                return Double.POSITIVE_INFINITY;
            case 253:
                return Double.NaN;
            default:
                byte[] bytes = new byte[len];
                byteBuf.readBytes(byteBuf);
                return Double.valueOf(new String(bytes));
        }
    }

}
