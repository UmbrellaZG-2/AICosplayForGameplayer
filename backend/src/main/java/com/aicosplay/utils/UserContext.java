package com.aicosplay.utils;

import com.aicosplay.entity.User;
import com.aicosplay.exception.UnauthorizedException;
import com.aicosplay.exception.UserNotFoundException;
import com.aicosplay.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 用户上下文工具类，用于获取当前登录用户信息和验证用户身份
 */
@Component
public class UserContext {
    
    private static final String USERNAME_SESSION_KEY = "username";
    
    private static UserService userService;
    
    @Autowired
    public void setUserService(UserService userService) {
        UserContext.userService = userService;
    }
    
    /**
     * 获取当前HTTP会话
     * @return HttpSession对象
     */
    public static HttpSession getCurrentSession() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            return attributes.getRequest().getSession();
        }
        return null;
    }
    
    /**
     * 检查用户是否已登录
     * @return 是否已登录
     */
    public static boolean isUserLoggedIn() {
        HttpSession session = getCurrentSession();
        return session != null && session.getAttribute(USERNAME_SESSION_KEY) != null;
    }
    
    /**
     * 获取当前登录用户的用户名
     * @return 用户名
     * @throws UnauthorizedException 如果用户未登录
     */
    public static String getCurrentUsername() {
        HttpSession session = getCurrentSession();
        if (session == null || session.getAttribute(USERNAME_SESSION_KEY) == null) {
            throw new UnauthorizedException();
        }
        return (String) session.getAttribute(USERNAME_SESSION_KEY);
    }
    
    /**
     * 获取当前登录用户信息
     * @return 用户对象
     * @throws UnauthorizedException 如果用户未登录
     * @throws UserNotFoundException 如果用户不存在
     */
    public static User getCurrentUser() {
        String username = getCurrentUsername();
        return userService.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException());
    }
    
    /**
     * 设置用户登录状态
     * @param username 用户名
     * @param session HttpSession对象
     */
    public static void setUserLoggedIn(String username, HttpSession session) {
        session.setAttribute(USERNAME_SESSION_KEY, username);
    }
    
    /**
     * 设置用户登录状态
     * @param user 用户对象
     */
    public static void setLoggedIn(User user) {
        HttpSession session = getCurrentSession();
        if (session != null) {
            session.setAttribute(USERNAME_SESSION_KEY, user.getUsername());
        }
    }
    
    /**
     * 清除用户登录状态（登出）
     * @param session HttpSession对象
     */
    public static void clearUserLoginStatus(HttpSession session) {
        session.removeAttribute(USERNAME_SESSION_KEY);
    }
    
    /**
     * 清除当前用户登录状态（登出）
     */
    public static void clearLoginStatus() {
        HttpSession session = getCurrentSession();
        if (session != null) {
            session.removeAttribute(USERNAME_SESSION_KEY);
        }
    }
}