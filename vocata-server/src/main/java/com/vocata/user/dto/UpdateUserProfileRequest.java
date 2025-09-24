package com.vocata.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 更新用户个人信息请求DTO
 *
 * @author vocata
 * @since 2025-09-24
 */
public class UpdateUserProfileRequest {

    /**
     * 昵称
     */
    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickname;

    /**
     * 头像URL
     */
    @Size(max = 500, message = "头像URL长度不能超过500个字符")
    private String avatar;

    /**
     * 性别 (0:未设置 1:男 2:女)
     */
    private Integer gender;

    /**
     * 手机号
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 生日
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;

    public UpdateUserProfileRequest() {
    }

    public UpdateUserProfileRequest(String nickname, String avatar, Integer gender,
                                  String phone, LocalDate birthday) {
        this.nickname = nickname;
        this.avatar = avatar;
        this.gender = gender;
        this.phone = phone;
        this.birthday = birthday;
    }

    public static UpdateUserProfileRequestBuilder builder() {
        return new UpdateUserProfileRequestBuilder();
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

    public static class UpdateUserProfileRequestBuilder {
        private String nickname;
        private String avatar;
        private Integer gender;
        private String phone;
        private LocalDate birthday;

        public UpdateUserProfileRequestBuilder nickname(String nickname) {
            this.nickname = nickname;
            return this;
        }

        public UpdateUserProfileRequestBuilder avatar(String avatar) {
            this.avatar = avatar;
            return this;
        }

        public UpdateUserProfileRequestBuilder gender(Integer gender) {
            this.gender = gender;
            return this;
        }

        public UpdateUserProfileRequestBuilder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public UpdateUserProfileRequestBuilder birthday(LocalDate birthday) {
            this.birthday = birthday;
            return this;
        }

        public UpdateUserProfileRequest build() {
            return new UpdateUserProfileRequest(nickname, avatar, gender, phone, birthday);
        }
    }
}