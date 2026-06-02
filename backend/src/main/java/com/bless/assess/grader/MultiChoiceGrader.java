package com.bless.assess.grader;

import com.bless.assess.entity.GradingRuleConf;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 多选题评分器：支持全对满分/部分得分/错选0分
 */
@Component
@RequiredArgsConstructor
public class MultiChoiceGrader implements Grader {
    
    private final com.bless.assess.service.GradingRuleService gradingRuleService;
    
    @Override
    public String supportedType() {
        return "MULTI";
    }
    
    @Override
    public double grade(Set<Long> correctAnswer, Set<Long> chosenAnswer) {
        GradingRuleConf rule = gradingRuleService.getRuleByType("MULTI");
        
        if (rule == null) {
            // 默认规则：全对满分，否则0分
            return correctAnswer.equals(chosenAnswer) ? 1.0 : 0.0;
        }
        
        // 错选是否0分
        if (Boolean.TRUE.equals(rule.getZeroOnWrongChoice())) {
            // 检查是否有错误选项（用户选了但不在正确答案中）
            for (Long choice : chosenAnswer) {
                if (!correctAnswer.contains(choice)) {
                    return 0.0;  // 错选直接0分
                }
            }
        }
        
        // 全对满分
        if (correctAnswer.equals(chosenAnswer)) {
            return 1.0;
        }
        
        // 部分得分
        if (Boolean.TRUE.equals(rule.getAllowPartialScore())) {
            // 计算交集大小 / 正确答案数
            int correct = 0;
            for (Long choice : chosenAnswer) {
                if (correctAnswer.contains(choice)) {
                    correct++;
                }
            }
            return (double) correct / correctAnswer.size() * (rule.getPartialScoreRatio() != null ? rule.getPartialScoreRatio() : 0.5);
        }
        
        return 0.0;
    }
}
