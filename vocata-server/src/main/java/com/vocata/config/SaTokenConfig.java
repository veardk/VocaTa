package com.vocata.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import com.vocata.common.utils.UserContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token配置
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler -> {
            // 全局登录检查
            SaRouter.match("/**")
                    .notMatch("/api/open/**")
                    .notMatch("/api/client/auth/**")
                    .notMatch("/static/**")
                    .notMatch("/images/**")
                    .notMatch("/css/**")
                    .notMatch("/js/**")
                    .notMatch("/actuator/health")
                    .notMatch("/error")
                    .notMatch("/favicon.ico")
                    .check(r -> {
                        StpUtil.checkLogin();
                        // 设置用户上下文
                        setUserContext();
                    });

            // 管理端权限控制
            SaRouter.match("/api/admin/**").check(r -> {
                StpUtil.checkLogin();
                setUserContext();
                UserContext.checkAdmin();
            });

        })).addPathPatterns("/**");
    }

    /**
     * 设置用户上下文
     */
    private void setUserContext() {
        if (StpUtil.isLogin()) {
            // 从Sa-Token中获取用户信息并设置到上下文
            Long userId = StpUtil.getLoginIdAsLong();

            // 这里可以从缓存或数据库获取用户详细信息
            // 暂时设置基本信息
            UserContext.UserContextDTO userContext = new UserContext.UserContextDTO();
            userContext.setUserId(userId);
            // 可以从Session中获取更多用户信息
            userContext.setUsername((String) StpUtil.getSession().get("username"));
            userContext.setIsAdmin((Boolean) StpUtil.getSession().get("isAdmin"));
            userContext.setEmail((String) StpUtil.getSession().get("email"));

            UserContext.set(userContext);
        }
    }
}