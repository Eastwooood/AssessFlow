package com.bless.assess.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话视图VO（返回给前端）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionVO {
    
    private Long sessionId;
    
    private Long userId;
    
    private Long paperId;
    
    private String status;
    
    /**
     * 当前步骤游标位置
     */
    private Integer stepCursor;
    
    /**
     * 进度 {current: 当前步骤, total: 总步骤数}
     */
    private Progress progress;
    
    /**
     * 累计总分
     */
    private Double totalScore;
    
    /**
     * 已结算的即时结果列表（不含正确答案详情）
     */
    private List<ImmediateResult> immediateResults;
    
    /**
     * 待作答的当前步骤信息
     */
    private PendingStep pendingStep;
    
    /**
     * 考试完成结案信息
     */
    private FinishInfo finishInfo;
    
    private LocalDateTime startedAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Progress {
        private Integer current;
        private Integer total;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImmediateResult {
        private Integer stepIndex;
        private String type;
        private Double gotScore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PendingStep {
        private String type;
        private Long questionId;
        private String stem;
        private List<OptionVO> options;
        
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class OptionVO {
            private Long optionId;
            private String text;
            // 注意：绝不返回 isCorrect 字段
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinishInfo {
        private Double finalScore;
        private Integer totalQuestions;
        private Integer correctCount;
    }
}
