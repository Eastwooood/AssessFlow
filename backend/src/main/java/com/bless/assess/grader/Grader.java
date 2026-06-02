package com.bless.assess.grader;

/**
 * 评分器接口（策略模式）
 * 新增题型只需实现此接口并注册到 GradingService
 */
public interface Grader {
    
    /**
     * 支持的题型
     */
    String supportedType();
    
    /**
     * 评分方法
     * @param correctAnswer 正确答案集合（选项ID列表）
     * @param chosenAnswer 用户作答答案集合
     * @return 得分
     */
    double grade(java.util.Set<Long> correctAnswer, java.util.Set<Long> chosenAnswer);
}
