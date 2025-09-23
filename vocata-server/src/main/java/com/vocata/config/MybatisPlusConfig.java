package com.vocata.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.vocata.common.utils.UserContext;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis Plus配置
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 分页插件配置
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 添加分页插件
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.POSTGRE_SQL);
        paginationInterceptor.setMaxLimit(100L); // 单页最大数量限制
        interceptor.addInnerInterceptor(paginationInterceptor);

        return interceptor;
    }

    /**
     * 自动填充处理器
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                Long userId = UserContext.getUserIdSafely();
                LocalDateTime now = LocalDateTime.now();

                this.strictInsertFill(metaObject, "createId", Long.class, userId);
                this.strictInsertFill(metaObject, "createDate", LocalDateTime.class, now);
                this.strictInsertFill(metaObject, "updateId", Long.class, userId);
                this.strictInsertFill(metaObject, "updateDate", LocalDateTime.class, now);
                this.strictInsertFill(metaObject, "isDelete", Integer.class, 0);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                Long userId = UserContext.getUserIdSafely();
                LocalDateTime now = LocalDateTime.now();

                this.strictUpdateFill(metaObject, "updateId", Long.class, userId);
                this.strictUpdateFill(metaObject, "updateDate", LocalDateTime.class, now);
            }
        };
    }
}