package com.cang.fetch_cien.dto;

import lombok.Data;

import java.util.Date;

/**
 * @author yue
 * create date: 2023/6/22 11:33
 */
@Data
public class CookieDto {
    private String name;
    private String value;
    private String domain;
    private Date expireDate;
}
