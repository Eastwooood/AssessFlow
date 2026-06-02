package com.bless.assess.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 题型规则配置
 */
@Data
@TableName("question_type_conf")
public class QuestionTypeConf {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    /**
     * 题型 SINGLE/MULTI/JUDGE/BLANK
     */
    private String questionType;
    
    /**
     * 是否打乱选项
     */
    private Boolean shuffleOptions;
    
    /**
     * 评分方式
     */
    private String gradingMethod;
    
    /**
     * 附加参数（JSON格式，如多选部分得分比例）
     */
    private String extraParams;
}
