package com.campus.study.enums;

public enum FileInspectionExceptionType {

    FILE_MISSING(1, "文件缺失", "数据库记录存在但物理文件不存在"),
    FILE_EMPTY(2, "文件为空", "文件存在但大小为0字节"),
    FILE_SIZE_MISMATCH(3, "文件大小不匹配", "数据库记录的文件大小与实际文件大小不一致"),
    FILE_HASH_MISMATCH(4, "文件哈希不匹配", "数据库记录的文件哈希与实际文件哈希不一致（可能损坏）"),
    FILE_UNREADABLE(5, "文件无法读取", "文件存在但无法正常读取（可能权限问题或损坏）"),
    PREVIEW_FAILED(6, "无法预览", "文件存在但无法生成预览"),
    FILE_URL_EMPTY(7, "文件地址为空", "数据库记录中file_url字段为空"),
    EXTENSION_INVALID(8, "文件扩展名无效", "文件扩展名不在系统允许的格式列表中"),
    MATERIAL_OFFLINE(9, "资料已下架", "资料状态已标记为下架");

    private final Integer code;
    private final String name;
    private final String description;

    FileInspectionExceptionType(Integer code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public static FileInspectionExceptionType fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (FileInspectionExceptionType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
