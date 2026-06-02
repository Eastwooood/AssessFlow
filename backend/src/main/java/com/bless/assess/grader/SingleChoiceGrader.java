package com.bless.assess.grader;

import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 单选题/判断题评分器：全对满分，错选0分
 */
@Component
public class SingleChoiceGrader implements Grader {
    
    @Override
    public String supportedType() {
        // 同时支持单选和判断题，评分逻辑一致
        return "SINGLE,JUDGE";
    }
    
    @Override
    public double grade(Set<Long> correctAnswer, Set<Long> chosenAnswer) {
        if (correctAnswer.equals(chosenAnswer)) {
            return 1.0;  // 满分比例
        }
        return 0.0;     // 错选0分
    }
}
