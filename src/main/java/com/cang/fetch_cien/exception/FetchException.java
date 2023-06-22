package com.cang.fetch_cien.exception;

/**
 * @author 通用异常类
 * create date: 2023/6/22 14:06
 */
public class FetchException extends RuntimeException {
    public FetchException(String reason) {
        super(reason);
    }

    public FetchException(String reason, Throwable cause) {
        super(reason, cause);
    }
}
