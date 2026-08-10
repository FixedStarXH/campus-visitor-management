/**
 * Copyright (c) 2016-2019 人人开源 All rights reserved.
 *
 * https://www.renren.io
 *
 * 版权所有，侵权必究！
 */

package io.renren.config;

import io.renren.modules.sys.jwt.JWTFilter;
import io.renren.modules.sys.jwt.JWTRealm;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.spring.LifecycleBeanPostProcessor;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.Filter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shiro配置
 *
 * @author Mark sunlightcs@gmail.com
 */
@Configuration
public class ShiroConfig {

    @Bean("securityManager")
    public SecurityManager securityManager(JWTRealm jwtRealm) {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        securityManager.setRealm(jwtRealm);
        securityManager.setRememberMeManager(null);
        return securityManager;
    }

    @Bean("shiroFilter")
    public ShiroFilterFactoryBean shiroFilter(SecurityManager securityManager) {
        ShiroFilterFactoryBean shiroFilter = new ShiroFilterFactoryBean();
        shiroFilter.setSecurityManager(securityManager);
        
        //jwt token过滤
        Map<String, Filter> filters = new HashMap<>();
        filters.put("jwt", new JWTFilter());
        shiroFilter.setFilters(filters);

        Map<String, String> filterMap = new LinkedHashMap<>();
        filterMap.put("/webjars/**", "anon");
        filterMap.put("/druid/**", "anon");
        filterMap.put("/app/**", "anon");
        filterMap.put("/sys/login", "anon");
        filterMap.put("/admin/manager/**", "anon");
        filterMap.put("/admin/config/**", "anon");
        filterMap.put("/admin/monitor/**", "anon");
        filterMap.put("/admin/record/**", "anon");
        filterMap.put("/admin/application/**", "anon");
        filterMap.put("/admin/user/**", "anon");
        filterMap.put("/sys/password", "anon");
        filterMap.put("/api/user/register", "anon");  // 注册接口(新)
        filterMap.put("/api/user/login", "anon");  // 登录接口(新)
        filterMap.put("/api/user/logout", "anon"); // 退出登录接口(新)
        filterMap.put("/api/user/auth/refresh", "anon"); // 刷新token接口(新)
        filterMap.put("/api/user/auth/verify", "anon"); // 验证token接口(新)
        filterMap.put("/api/user/info", "anon");  // 用户信息接口(新)
        filterMap.put("/api/user/update", "anon");  // 更新用户信息接口(新)
        filterMap.put("/api/user/password", "anon");  // 修改密码接口(新)

        filterMap.put("/api/captcha", "anon"); // 验证码接口(新)
        filterMap.put("/api/application/submit", "anon"); // 提交入校申请接口(新)
        filterMap.put("/api/application/list", "anon"); // 申请个人列表查询接口(新)
        filterMap.put("/api/application/detail/**", "anon"); // 申请详情查询接口(新)
        filterMap.put("/api/application/cancel/**", "anon"); // 取消申请接口(新)
        filterMap.put("/api/record/my", "anon"); // 我的入校记录查询接口(新)
        filterMap.put("/api/visitor/export", "anon"); // 访客导出接口(新)
        filterMap.put("/api/ers/**", "anon"); // ERS访客端API(新)

        filterMap.put("/swagger/**", "anon");
        filterMap.put("/v2/api-docs", "anon");
        filterMap.put("/swagger-ui.html", "anon");
        filterMap.put("/swagger-resources/**", "anon");
        filterMap.put("/captcha.jpg", "anon");
        filterMap.put("/aaa.txt", "anon");
        filterMap.put("/", "anon");
        filterMap.put("/health", "anon");
        filterMap.put("/**", "jwt");
        shiroFilter.setFilterChainDefinitionMap(filterMap);

        return shiroFilter;
    }

    @Bean("lifecycleBeanPostProcessor")
    public LifecycleBeanPostProcessor lifecycleBeanPostProcessor() {
        return new LifecycleBeanPostProcessor();
    }

    @Bean
    public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(SecurityManager securityManager) {
        AuthorizationAttributeSourceAdvisor advisor = new AuthorizationAttributeSourceAdvisor();
        advisor.setSecurityManager(securityManager);
        return advisor;
    }

}
