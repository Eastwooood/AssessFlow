package com.bless.assess.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 题目黑名单配置
 */
@Data
@TableName("question_blacklist_conf")
public class QuestionBlacklistConf {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    /**
     * 题目ID
     */
    private Long questionId;
    
    /**
     * 下架原因
     */
    private String reason;
    
    /**
     * 下架时间
     */
    private java.time.LocalDateTime disabledAt;
}
