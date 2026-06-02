package com.bless.assess.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 考试会话状态枚举
 */
@Getter
@AllArgsConstructor
public enum SessionStatus {
    INIT("初始化"),
    IN_PROGRESS("进行中"),
    AWAIT_ANSWER("等待作答"),
    FINISHED("已完成"),
    ABANDONED("已放弃");

    private final String description;
}
