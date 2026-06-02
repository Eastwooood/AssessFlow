package com.bless.assess.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 考试会话主表
 */
@Data
@TableName("exam_session")
public class ExamSession {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private Long userId;
    
    private Long paperId;
    
    /**
     * 步骤游标：指向下一个未结算环节
     */
    private Integer stepCursor;
    
    /**
     * 会话状态
     */
    private String status;
    
    /**
     * 总分
     */
    private Double totalScore;
    
    /**
     * 开始时间
     */
    private LocalDateTime startedAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    @TableLogic
    private Integer deleted;
}
