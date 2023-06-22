package com.cang.fetch_cien.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * @author yue
 * create date: 2023/6/22 15:03
 */
@Data
public class FileDto {
    private String fileName;

    private String fileUrl;

    private LocalDate postedDate;

}
