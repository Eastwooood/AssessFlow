package com.bless.assess.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 环节类型枚举
 */
@Getter
@AllArgsConstructor
public enum StepType {
    // 自动环节
    INFO("信息展示环节", true),
    SCORE_SNAPSHOT("分数快照环节", true),
    
    // 作答环节
    SINGLE("单选题", false),
    MULTI("多选题", false),
    JUDGE("判断题", false),
    BLANK("填空题", false);

    private final String description;
    private final boolean auto;  // 是否为自动结算环节
    
    public boolean isAutoStep() {
        return this.auto;
    }

    public boolean isAnswerStep() {
        return !this.auto;
    }
}
