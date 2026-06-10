package com.bless.assess.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 系统用户表
 */
@Data
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String username;
    private String password;
    private String role;
    private LocalDateTime createdAt;
}
