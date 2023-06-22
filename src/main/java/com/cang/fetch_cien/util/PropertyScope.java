package com.cang.fetch_cien.util;

import java.io.IOException;
import java.util.Properties;

/**
 * @author yue
 * create date: 2023/6/22 15:00
 */
public class PropertyScope {

    private static final Properties PROPERTIES;

    static {
        PROPERTIES = new Properties();
        try {
            PROPERTIES.load(ConnectUtil.class.getClassLoader().getResourceAsStream("config.properties"));
        } catch (IOException e) {
            throw new RuntimeException("无法加载配置文件,请检查文件是否存在");
        }
    }

    public static String getProperty(String key) {
        return PROPERTIES.getProperty(key);
    }
}
