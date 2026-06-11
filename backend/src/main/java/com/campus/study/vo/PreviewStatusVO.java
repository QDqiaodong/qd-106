package com.campus.study.vo;

import java.io.Serializable;

public class PreviewStatusVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer code;

    private String message;

    private Boolean previewable;

    private String fileType;

    private Long fileSize;

    private String previewUrl;

    private String fallbackTip;

    private Boolean downloadable;

    public PreviewStatusVO() {
    }

    public PreviewStatusVO(Integer code, String message, Boolean previewable) {
        this.code = code;
        this.message = message;
        this.previewable = previewable;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getPreviewable() {
        return previewable;
    }

    public void setPreviewable(Boolean previewable) {
        this.previewable = previewable;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public void setPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
    }

    public String getFallbackTip() {
        return fallbackTip;
    }

    public void setFallbackTip(String fallbackTip) {
        this.fallbackTip = fallbackTip;
    }

    public Boolean getDownloadable() {
        return downloadable;
    }

    public void setDownloadable(Boolean downloadable) {
        this.downloadable = downloadable;
    }
}
