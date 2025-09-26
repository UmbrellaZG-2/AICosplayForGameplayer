package com.aicosplay.security.filter;

/**
 * 基础过滤器抽象类，实现责任链模式的基本功能
 */
public abstract class BaseSecurityFilter implements SecurityFilter {
    
    protected SecurityFilter nextFilter;
    
    @Override
    public SecurityFilter setNext(SecurityFilter nextFilter) {
        this.nextFilter = nextFilter;
        return nextFilter;
    }
    
    @Override
    public boolean doFilter(SecurityContext context) {
        // 如果当前过滤器通过检查，且有下一个过滤器，则传递给下一个过滤器
        if (doFilterInternal(context) && nextFilter != null) {
            return nextFilter.doFilter(context);
        }
        // 返回当前检查结果
        return context.isSafe();
    }
    
    /**
     * 具体的过滤逻辑，由子类实现
     * @param context 安全上下文
     * @return 当前过滤器的处理结果
     */
    protected abstract boolean doFilterInternal(SecurityContext context);
}