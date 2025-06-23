package com.dahuaboke.redisx.from.rdb.list;

import com.dahuaboke.redisx.from.rdb.ParserManager;
import com.dahuaboke.redisx.from.rdb.base.Parser;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.LinkedList;
import java.util.List;

/**
 * @Desc: quickList = list+zipList
 * Redis < 7.x 版本
 * @Author：zhh
 * @Date：2024/5/20 13:48
 */
public class ListQuickListParser implements Parser {

    public List<byte[]> parse(ByteBuf byteBuf) {
        //元素个数
        long len = ParserManager.LENGTH.parse(byteBuf).len;
        List<byte[]> list = new LinkedList<>();
        for (int i = 0; i < len; i++) {
            byte[] bytes = ParserManager.STRING_00.parse(byteBuf);
            // 创建一个ByteBuf
            ByteBuf buf = Unpooled.buffer();
            // 将byte数组写入ByteBuf
            buf.writeBytes(bytes);
            List<byte[]> listByte = ParserManager.ZIPLIST.parse(buf);
            list.addAll(listByte);
            // 释放ByteBuf的内存
            buf.release();
        }
        return list;
    }
}
