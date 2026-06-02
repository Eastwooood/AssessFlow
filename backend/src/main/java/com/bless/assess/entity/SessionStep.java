package com.bless.assess.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.List;

/**
 * 会话步骤子表
 */
@Data
@TableName("session_step")
public class SessionStep {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    /**
     * 所属会话ID
     */
    private Long sessionId;
    
    /**
     * 步骤索引（从0开始）
     */
    private Integer stepIndex;
    
    /**
     * 环节类型
     */
    private String type;
    
    /**
     * 题目ID（作答环节有值）
     */
    private Long questionId;
    
    /**
     * 候选选项列表（服务端打乱后存储，不返回前端）
     * JSON格式存储 List<Long>
     */
    private String candidateOptions;
    
    /**
     * 用户答案（JSON格式）
     */
    private String userAnswer;
    
    /**
     * 本题得分
     */
    private Double gotScore;
    
    /**
     * 是否已结算
     */
    private Boolean settled;
    
    @TableLogic
    private Integer deleted;
}
