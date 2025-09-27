package com.vocata.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.vocata.common.handler.UuidTypeHandler;
import com.vocata.common.utils.UserContext;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.JdbcType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * MyBatis Plus配置
 */
@Configuration
public class MybatisPlusConfig {

    @Autowired
    private DataSource dataSource;

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
     * 自定义SqlSessionFactory，注册TypeHandler并配置GlobalConfig
     */
    @Bean
    @Primary
    public SqlSessionFactory sqlSessionFactory() throws Exception {
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);

        // 创建MyBatis配置
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);

        // 注册UUID类型处理器
        configuration.getTypeHandlerRegistry().register(UUID.class, JdbcType.OTHER, UuidTypeHandler.class);
        System.out.println("✅ UUID TypeHandler 已注册到配置中");

        // 设置配置
        factory.setConfiguration(configuration);

        // 配置MyBatis Plus的GlobalConfig，确保自动填充功能正常工作
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setMetaObjectHandler(metaObjectHandler());
        factory.setGlobalConfig(globalConfig);

        // 添加插件
        factory.setPlugins(mybatisPlusInterceptor());

        return factory.getObject();
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

                // 如果用户ID为null或为访客用户(-1L)，使用系统默认值0
                if (userId == null || userId == -1L) {
                    userId = 0L;
                }

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

                // 如果用户ID为null或为访客用户(-1L)，使用系统默认值0
                if (userId == null || userId == -1L) {
                    userId = 0L;
                }

                this.strictUpdateFill(metaObject, "updateId", Long.class, userId);
                this.strictUpdateFill(metaObject, "updateDate", LocalDateTime.class, now);
            }
        };
    }
}