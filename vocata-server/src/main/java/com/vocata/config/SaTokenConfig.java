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
            SaRouter.match("/open/**", "/actuator/health", "/error", "/favicon.ico", "/debug/**")
                    .stop();
            
            // 静态资源，无需认证
            SaRouter.match("/static/**", "/images/**", "/css/**", "/js/**")
                    .stop();
            
            // 认证相关接口，无需预先认证（注意：这里的路径是去掉/api前缀后的路径）
            SaRouter.match("/client/auth/login", "/client/auth/register", 
                          "/client/auth/send-register-code", "/client/auth/send-reset-code", 
                          "/client/auth/reset-password", "/client/auth/refresh-token")
                    .stop();
            
            // 管理员专用接口，需要管理员权限
            SaRouter.match("/admin/**").check(r -> {
                StpUtil.checkLogin();
                setUserContext();
                UserContext.checkAdmin();
            });
            
            // 其他所有接口，需要登录认证
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
