package com.vocata.admin.dto;

/**
 * 管理后台用户查询请求DTO
 *
 * @author vocata
 * @since 2025-09-24
 */
public class UserQueryRequest {

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 10;

    /**
     * 用户名模糊查询
     */
    private String username;

    /**
     * 邮箱模糊查询
     */
    private String email;

    /**
     * 用户状态筛选
     */
    private Integer status;

    public UserQueryRequest() {
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}