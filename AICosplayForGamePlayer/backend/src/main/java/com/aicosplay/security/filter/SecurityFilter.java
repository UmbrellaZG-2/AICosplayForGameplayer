package com.aicosplay.security.filter;

/**
 * 安全过滤器接口，定义责任链中的每个处理节点
 */
public interface SecurityFilter {
    
    /**
     * 设置下一个过滤器
     * @param nextFilter 下一个过滤器
     * @return 返回下一个过滤器，用于链式调用
     */
    SecurityFilter setNext(SecurityFilter nextFilter);
    
    /**
     * 处理安全检查
     * @param context 安全上下文，包含需要检查的数据
     * @return 处理结果，true表示通过检查，false表示未通过
     */
    boolean doFilter(SecurityContext context);
}