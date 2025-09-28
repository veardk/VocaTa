package com.vocata.file.config;

import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 七牛云配置
 *
 * @author vocata
 * @since 2025-09-24
 */
@Configuration
@ConfigurationProperties(prefix = "qiniu")
public class QiniuConfig {

    /**
     * Access Key
     */
    private String accessKey;

    /**
     * Secret Key
     */
    private String secretKey;

    /**
     * 存储空间名称
     */
    private String bucket;

    /**
     * 访问域名
     */
    private String domain;

    /**
     * 存储区域
     */
    private String region;

    /**
     * 七牛云认证
     */
    @Bean
    public Auth auth() {
        return Auth.create(accessKey, secretKey);
    }

    /**
     * 七牛云上传管理器
     */
    @Bean
    public UploadManager uploadManager() {
        // 根据region配置选择存储区域
        Region qiniuRegion = getQiniuRegion();
        com.qiniu.storage.Configuration cfg = new com.qiniu.storage.Configuration(qiniuRegion);
        return new UploadManager(cfg);
    }

    /**
     * 七牛云存储空间管理器
     */
    @Bean
    public BucketManager bucketManager(Auth auth) {
        Region qiniuRegion = getQiniuRegion();
        com.qiniu.storage.Configuration cfg = new com.qiniu.storage.Configuration(qiniuRegion);
        return new BucketManager(auth, cfg);
    }

    /**
     * 根据配置获取七牛云区域
     */
    private Region getQiniuRegion() {
        switch (region.toLowerCase()) {
            case "huadong":
                return Region.huadong();
            case "huabei":
                return Region.huabei();
            case "huanan":
                return Region.huanan();
            case "beimei":
                return Region.beimei();
            default:
                return Region.autoRegion();
        }
    }

    // Getter and Setter methods

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

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }
}