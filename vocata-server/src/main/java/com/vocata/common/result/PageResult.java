package com.vocata.common.result;

import java.io.Serializable;
import java.util.List;

/**
 * 分页查询结果封装类
 */
public class PageResult<T> implements Serializable {

    private Integer pageNum;
    private Integer pageSize;
    private Long total;
    private Integer totalPages;
    private List<T> list;
    private List<T> records; // 兼容性字段，与list指向同一个数据
    private Boolean hasNext;
    private Boolean hasPrevious;

    public PageResult() {}

    public PageResult(Integer pageNum, Integer pageSize, Long total, List<T> list) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.total = total;
        this.list = list;
        this.records = list; // 保持兼容性
        this.totalPages = (int) Math.ceil((double) total / pageSize);
        this.hasNext = pageNum < totalPages;
        this.hasPrevious = pageNum > 1;
    }

    public static <T> PageResult<T> of(Integer pageNum, Integer pageSize, Long total, List<T> list) {
        return new PageResult<>(pageNum, pageSize, total, list);
    }

    // Getters and Setters
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

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
        this.records = list; // 保持兼容性
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
        this.list = records; // 保持兼容性
    }

    public Boolean getHasNext() {
        return hasNext;
    }

    public void setHasNext(Boolean hasNext) {
        this.hasNext = hasNext;
    }

    public Boolean getHasPrevious() {
        return hasPrevious;
    }

    public void setHasPrevious(Boolean hasPrevious) {
        this.hasPrevious = hasPrevious;
    }
}
