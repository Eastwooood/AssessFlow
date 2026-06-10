package com.bless.assess.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bless.assess.entity.QuestionTypeConf;
import com.bless.assess.mapper.QuestionTypeConfMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * 题型配置服务（支持缓存热加载）
 */
@Service
@RequiredArgsConstructor
public class QuestionTypeConfService {

    private final QuestionTypeConfMapper mapper;

    @Cacheable(value = "questionTypeConf", key = "#questionType")
    public QuestionTypeConf getByType(String questionType) {
        return mapper.selectOne(new LambdaQueryWrapper<QuestionTypeConf>()
                .eq(QuestionTypeConf::getQuestionType, questionType));
    }

    /** 默认打乱，无配置时兜底 true，保证旧行为不变 */
    public boolean shouldShuffle(String questionType) {
        QuestionTypeConf conf = getByType(questionType);
        return conf == null || !Boolean.FALSE.equals(conf.getShuffleOptions());
    }
}
