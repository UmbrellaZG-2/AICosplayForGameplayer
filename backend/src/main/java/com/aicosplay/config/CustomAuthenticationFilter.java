package com.aicosplay.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * 自定义认证过滤器，用于识别会话中已登录的用户
 */
@Component
public class CustomAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        
        // 检查会话中是否存在用户名属性
        String username = (String) request.getSession().getAttribute("username");
        
        // 如果用户名存在且当前上下文没有认证信息
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // 创建一个简单的认证令牌（这里我们只需要标记用户已认证，不关心具体权限）
            UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                            username, 
                            null, 
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                    );
            
            // 设置认证信息到安全上下文
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        
        // 继续过滤器链
        filterChain.doFilter(request, response);
    }
}