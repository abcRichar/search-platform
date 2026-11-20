package com.example.search.common;

/** 统一返回结构 */
public class Result<T> {
    private int code;
    private T data;
    private String message;

    public Result() {}

    public Result(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(0, data, "ok");
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, null, message);
    }

    public int getCode() { return code; }
    public T getData() { return data; }
    public String getMessage() { return message; }
}
