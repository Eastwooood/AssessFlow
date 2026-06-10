package com.bless.assess.util;

import com.bless.assess.exception.BusinessException;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全工具类：从 SecurityContext 获取当前用户
 */
public final class SecurityUtil {

    public static Long currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new BusinessException("UNAUTHORIZED", "未登录");
        }
        Object p = auth.getPrincipal();
        if (p instanceof Long uid) return uid;
        throw new BusinessException("UNAUTHORIZED", "未登录");
    }
}
