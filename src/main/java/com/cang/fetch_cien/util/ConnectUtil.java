package com.cang.fetch_cien.util;

import com.cang.fetch_cien.dto.CookieDto;
import org.jsoup.Connection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * @author yue
 * create date: 2023/6/22 13:42
 */
public class ConnectUtil {
    /**
     * 链接超时时间
     */
    private static final int CONNECT_TIMEOUT;
    /**
     * cookie文本保存目录
     */
    private static final String COOKIE_FILEPATH;
    /**
     * 是否使用代理,默认不使用
     */
    private static boolean USE_PROXY = false;
    /**
     * 使用代理时,需要写入代理的服务器地址URL
     */
    private static final String PROXY_SERVER_URL;
    /**
     * 使用代理时,需要写入代理的服务器端口
     */
    private static final int PROXY_PORT;
    /**
     * 用户cookie Map
     */
    private static Map<String, String> cookies;

    static {
        CONNECT_TIMEOUT = Integer.parseInt(PropertyScope.getProperty("connectTimeout"));
        COOKIE_FILEPATH = PropertyScope.getProperty("cookieFilePath");
        USE_PROXY = Boolean.parseBoolean(PropertyScope.getProperty("useProxy"));
        PROXY_SERVER_URL = PropertyScope.getProperty("proxyServerUrl");
        PROXY_PORT = Integer.parseInt(PropertyScope.getProperty("proxyPort"));
        parseCookieText();
    }

    /**
     * 包装Jsoup连接，为每次连接添加cookie以获取访问权限。
     */
    public static void prepareConnection(Connection connection) {
        connection.timeout(CONNECT_TIMEOUT);
        if (USE_PROXY) {
            connection.proxy(PROXY_SERVER_URL, PROXY_PORT);
        }
        connection.cookies(cookies);
    }

    /**
     * 从cookie文本获取cookie Map
     */
    private static void parseCookieText() {
        try {
            // 读取文件,分割字符串
            List<CookieDto> cookieDtoList = Files.readAllLines(Paths.get(COOKIE_FILEPATH)).stream().map(s -> {
                        String[] split = s.split("\t");
                        CookieDto cookieDto = new CookieDto();
                        cookieDto.setName(split[0]);
                        cookieDto.setValue(split[1]);
                        cookieDto.setDomain(split[2]);
                        cookieDto.setExpireDate((DateUtil.getDateFromStr(split[4])));
                        return cookieDto;
                    }).filter(c -> c.getDomain().contains("ci-en") || c.getDomain().contains("dlsite"))
                    .collect(Collectors.toList());


            // 安全检查
            Optional.of(cookieDtoList)
                    .filter(list -> list.size() > 0)
                    .orElseThrow(() -> new RuntimeException("没有cookie无法抓取"));

            // 映射为Map
            cookies = cookieDtoList.stream()
                    .collect(Collectors.toMap(CookieDto::getName, CookieDto::getValue, (c1, c2) -> c1));
        } catch (IOException e) {
            throw new RuntimeException(COOKIE_FILEPATH + " 为无效路径", e);
        }
    }
}
