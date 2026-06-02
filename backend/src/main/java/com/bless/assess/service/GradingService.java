package com.bless.assess.service;

import com.bless.assess.entity.GradingRuleConf;
import com.bless.assess.grader.Grader;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 通用可扩展评分服务
 * 采用Grader注册表分发策略
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GradingService {
    
    private final List<Grader> graders;
    private final GradingRuleService gradingRuleService;
    private final Map<String, Grader> graderRegistry = new HashMap<>();
    
    @PostConstruct
    public void init() {
        for (Grader grader : graders) {
            String[] types = grader.supportedType().split(",");
            for (String type : types) {
                graderRegistry.put(type.trim(), grader);
            }
        }
        log.info("评分器注册完成: {}", graderRegistry.keySet());
    }
    
    /**
     * 统一评分入口
     * @param questionType 题型
     * @param correctAnswer 正确答案集合
     * @param chosenAnswer 用户作答集合
     * @return 实际得分（已乘以单题分值）
     */
    public double grade(String questionType, Set<Long> correctAnswer, Set<Long> chosenAnswer) {
        Grader grader = graderRegistry.get(questionType);
        if (grader == null) {
            log.warn("未找到题型[{}]对应的评分器", questionType);
            return 0.0;
        }
        
        double ratio = grader.grade(correctAnswer, chosenAnswer);
        GradingRuleConf rule = gradingRuleService.getRuleByType(questionType);
        double scorePerQuestion = rule != null ? rule.getScorePerQuestion() : 10.0;
        
        log.debug("题型={}, 比例得分={}, 最终得分={}", questionType, ratio, ratio * scorePerQuestion);
        return ratio * scorePerQuestion;
    }
}
