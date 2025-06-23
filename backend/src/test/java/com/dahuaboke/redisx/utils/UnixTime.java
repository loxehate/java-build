package com.dahuaboke.redisx.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * ClassName: UnixTime
 * Package: dahuaboke.redisx
 * Description:
 *
 * @Author zhangdalu
 * @Create 2025/2/11 15:57
 * @Version 1.0
 */
public class UnixTime {
    public static void main(String[] args) {
        //时间-》时间戳
        // 定义特定时间的字符串
        String specificTime = "2025-02-12 09:30:00";
        // 定义日期格式
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            // 将字符串解析为 Date 对象
            Date date = sdf.parse(specificTime);
            // 获取时间戳（毫秒）
            long timestampInMillis = date.getTime();
            // 转换为秒级时间戳
            long timestampInSeconds = timestampInMillis / 1000;
            System.out.println("特定时间: " + specificTime);
            System.out.println("对应的时间戳（毫秒）: " + timestampInMillis);
            System.out.println("对应的时间戳（秒）: " + timestampInSeconds);
        } catch (ParseException e) {
            e.printStackTrace();
        }


        //时间戳-》时间

        // 定义毫秒时间戳
        long milliseconds = 1739265973046L;
        // 创建 Date 对象
        Date date = new Date(milliseconds);
        // 定义日期格式
        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // 格式化日期
        String formattedDate = sdf1.format(date);
        // 输出转换后的时间
        System.out.println("毫秒时间戳转换后的时间：" + formattedDate);


        // 定义秒级时间戳
        long seconds = 1739265900;
        // 因为 Date 类构造函数接受的是毫秒数，所以将秒转换为毫秒
        long milliseconds2 = seconds * 1000;
        // 创建 Date 对象
        Date date1 = new Date(milliseconds2);
        // 定义日期格式
        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // 格式化日期
        String formattedDate2 = sdf.format(date1);
        // 输出转换后的时间
        System.out.println("转换后的时间：" + formattedDate2);


    }
}
