package com.bless.assess.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 评分规则配置
 */
@Data
@TableName("grading_rule_conf")
public class GradingRuleConf {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    /**
     * 适用题型
     */
    private String questionType;
    
    /**
     * 规则名称
     */
    private String ruleName;
    
    /**
     * 每题分值
     */
    private Double scorePerQuestion;
    
    /**
     * 全对是否满分
     */
    private Boolean fullScoreOnAllCorrect;
    
    /**
     * 是否支持部分得分
     */
    private Boolean allowPartialScore;
    
    /**
     * 错选是否0分
     */
    private Boolean zeroOnWrongChoice;
    
    /**
     * 部分得分比例（0-1）
     */
    private Double partialScoreRatio;
}
