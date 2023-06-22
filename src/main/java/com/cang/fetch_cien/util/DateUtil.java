package com.cang.fetch_cien.util;

import org.apache.commons.lang3.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

/**
 * @author yue
 * create date: 2023/6/22 11:41
 */
public class DateUtil {
    /**
     * @param str 必须为yyyy-MM-dd格式
     * @return 格式化后的Date。
     */
    public static Date getDateFromStr(String str) {
        if (StringUtils.isBlank(str) || str.contains("会话")) {
            return Date.from(LocalDateTime.now().plusDays(1).atZone(ZoneId.systemDefault()).toInstant());
        }
        try {
            return Date.from(OffsetDateTime.parse(str).toInstant());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
