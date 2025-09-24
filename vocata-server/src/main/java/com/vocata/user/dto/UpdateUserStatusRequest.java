package com.vocata.user.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 更新用户状态请求
 *
 * @author vocata
 * @since 2025-09-24
 */
public class UpdateUserStatusRequest {

    @NotNull(message = "状态不能为空")
    private Integer status;

    public UpdateUserStatusRequest() {}

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
