package com.bless.assess.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bless.assess.entity.GradingRuleConf;
import com.bless.assess.mapper.GradingRuleConfMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * 评分规则服务（支持缓存）
 */
@Service
@RequiredArgsConstructor
public class GradingRuleService {
    
    private final GradingRuleConfMapper gradingRuleConfMapper;
    
    /**
     * 根据题型获取评分规则（带缓存，支持热加载时失效）
     */
    @Cacheable(value = "gradingRules", key = "#questionType")
    public GradingRuleConf getRuleByType(String questionType) {
        return gradingRuleConfMapper.selectOne(
                new LambdaQueryWrapper<GradingRuleConf>()
                        .eq(GradingRuleConf::getQuestionType, questionType)
        );
    }
}
