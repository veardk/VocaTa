package com.vocata.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.time.LocalDate;

/**
 * 用户个人信息响应DTO
 *
 * @author vocata
 * @since 2025-09-24
 */
public class UserProfileResponse {

    /**
     * 用户ID
     */
    private String id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 性别 (0:未设置 1:男 2:女)
     */
    private Integer gender;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 生日
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;

    /**
     * 注册时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createDate;

    public UserProfileResponse() {
    }

    public UserProfileResponse(String id, String username, String email, String nickname,
                             String avatar, Integer gender, String phone,
                             LocalDate birthday, LocalDateTime createDate) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.nickname = nickname;
        this.avatar = avatar;
        this.gender = gender;
        this.phone = phone;
        this.birthday = birthday;
        this.createDate = createDate;
    }

    public static UserProfileResponseBuilder builder() {
        return new UserProfileResponseBuilder();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    public LocalDateTime getCreateDate() {
        return createDate;
    }

    public void setCreateDate(LocalDateTime createDate) {
        this.createDate = createDate;
    }

    public static class UserProfileResponseBuilder {
        private String id;
        private String username;
        private String email;
        private String nickname;
        private String avatar;
        private Integer gender;
        private String phone;
        private LocalDate birthday;
        private LocalDateTime createDate;

        public UserProfileResponseBuilder id(String id) {
            this.id = id;
            return this;
        }

        public UserProfileResponseBuilder username(String username) {
            this.username = username;
            return this;
        }

        public UserProfileResponseBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserProfileResponseBuilder nickname(String nickname) {
            this.nickname = nickname;
            return this;
        }

        public UserProfileResponseBuilder avatar(String avatar) {
            this.avatar = avatar;
            return this;
        }

        public UserProfileResponseBuilder gender(Integer gender) {
            this.gender = gender;
            return this;
        }

        public UserProfileResponseBuilder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public UserProfileResponseBuilder birthday(LocalDate birthday) {
            this.birthday = birthday;
            return this;
        }

        public UserProfileResponseBuilder createDate(LocalDateTime createDate) {
            this.createDate = createDate;
            return this;
        }

        public UserProfileResponse build() {
            return new UserProfileResponse(id, username, email, nickname, avatar,
                                         gender, phone, birthday, createDate);
        }
    }
}