package com.bless.assess.grader;

import org.springframework.stereotype.Component;
import java.util.Set;

/**
 * 填空题评分器：文本归一化比对
 */
@Component
public class BlankFillGrader implements Grader {
    
    @Override
    public String supportedType() {
        return "BLANK";
    }
    
    @Override
    public double grade(Set<Long> correctAnswer, Set<Long> chosenAnswer) {
        // 填空题通过选项ID匹配，完全匹配得满分
        if (correctAnswer.equals(chosenAnswer)) {
            return 1.0;
        }
        return 0.0;
    }
}
