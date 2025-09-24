package com.vocata.file.dto;

/**
 * 上传凭证响应
 */
public class UploadTokenResponse {

    private String token;

    private String key;

    private Long expires;

    public UploadTokenResponse() {
    }

    public UploadTokenResponse(String token, String key, Long expires) {
        this.token = token;
        this.key = key;
        this.expires = expires;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Long getExpires() {
        return expires;
    }

    public void setExpires(Long expires) {
        this.expires = expires;
    }
}