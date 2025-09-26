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
 * 
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler -> {
            // 公开接口，无需认证
            SaRouter.match("/api/open/**", "/actuator/health", "/error", "/favicon.ico", "/debug/**",
                          "/ws/**", "/websocket/**")
                    .stop();

            // 静态资源，无需认证
            SaRouter.match("/static/**", "/images/**", "/css/**", "/js/**",
                          "/websocket-test.html", "/*.html", "/*.css", "/*.js",
                          "/favicon.ico", "/*.ico")
                    .stop();

            // 客户端认证相关接口，无需预先认证
            SaRouter.match("/api/client/auth/login", "/api/client/auth/register",
                          "/api/client/auth/send-register-code", "/api/client/auth/send-reset-code",
                          "/api/client/auth/reset-password", "/api/client/auth/refresh-token")
                    .stop();

            // 管理员认证接口，无需预先认证（但会在服务层验证管理员身份）
            SaRouter.match("/api/admin/auth/login", "/api/admin/auth/refresh-token")
                    .stop();

            // 管理员专用接口，需要管理员权限
            SaRouter.match("/api/admin/**").check(r -> {
                StpUtil.checkLogin();
                setUserContext();
                UserContext.checkAdmin();
            });

            // 客户端接口，需要登录（管理员和普通用户都能访问）
            SaRouter.match("/api/client/**").check(r -> {
                StpUtil.checkLogin();
                setUserContext();
            });

            // 其他接口，需要登录认证（通用接口）
            SaRouter.match("/**").check(r -> {
                StpUtil.checkLogin();
                setUserContext();
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
