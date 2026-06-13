package com.campus.study.enums;

public enum FileInspectionHandleStatus {

    PENDING(0, "待处理"),
    PROCESSING(1, "处理中"),
    RESOLVED(2, "已解决"),
    IGNORED(3, "已忽略"),
    DELETED(4, "已删除记录");

    private final Integer code;
    private final String name;

    FileInspectionHandleStatus(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public Integer getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static FileInspectionHandleStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (FileInspectionHandleStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
