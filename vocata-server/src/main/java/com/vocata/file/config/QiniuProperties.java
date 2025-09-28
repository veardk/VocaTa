package com.vocata.file.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 七牛云配置属性
 */
@Component
@ConfigurationProperties(prefix = "qiniu")
public class QiniuProperties {

    private String accessKey;

    private String secretKey;

    private String bucket;

    private String domain;

    private Long uploadTokenExpires = 3600L;

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public Long getUploadTokenExpires() {
        return uploadTokenExpires;
    }

    public void setUploadTokenExpires(Long uploadTokenExpires) {
        this.uploadTokenExpires = uploadTokenExpires;
    }
}