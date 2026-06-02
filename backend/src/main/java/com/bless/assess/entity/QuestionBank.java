package com.bless.assess.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 题库表
 */
@Data
@TableName("question_bank")
public class QuestionBank {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    /**
     * 题干
     */
    private String stem;
    
    /**
     * 题型 SINGLE/MULTI/JUDGE/BLANK
     */
    private String questionType;
    
    /**
     * 选项 JSON格式 [{"optionId":1,"text":"A选项","isCorrect":true},...]
     */
    private String options;
    
    /**
     * 正确答案 JSON格式 [1,2] 或 "正确答案文本"
     */
    private String correctAnswer;
    
    /**
     * 难度等级 1-5
     */
    private Integer difficulty;
    
    /**
     * 标签 JSON数组
     */
    private String tags;
    
    /**
     * 是否启用
     */
    private Boolean enabled;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @TableLogic
    private Integer deleted;
}
