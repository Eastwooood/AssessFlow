package com.bless.assess.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {
    private String token;
    private Long userId;
    private String role;
}
