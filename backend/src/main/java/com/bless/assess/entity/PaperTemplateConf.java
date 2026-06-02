package com.bless.assess.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 试卷模板配置
 */
@Data
@TableName("paper_template_conf")
public class PaperTemplateConf {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private String paperName;
    
    /**
     * 环节序列 JSON格式 [{"type":"INFO","title":"..."},{"type":"SINGLE","questionCount":5},...]
     */
    private String stepSequence;
    
    /**
     * 是否启用
     */
    private Boolean enabled;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
