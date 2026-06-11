package com.campus.study.enums;

public enum PreviewErrorCode {

    SUCCESS(200, "预览正常"),
    MATERIAL_NOT_FOUND(4001, "资料不存在"),
    MATERIAL_OFFLINE(4002, "资料已下架"),
    FILE_NOT_FOUND(4003, "文件不存在"),
    FORMAT_NOT_SUPPORTED(4004, "该文件格式暂不支持在线预览"),
    PREVIEW_GENERATE_ERROR(5001, "预览生成异常，请稍后重试"),
    FILE_EMPTY(4005, "文件内容为空"),
    FILE_TOO_LARGE(4006, "文件过大，暂不支持在线预览"),
    UNKNOWN_ERROR(5000, "未知错误");

    private final Integer code;
    private final String message;

    PreviewErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
