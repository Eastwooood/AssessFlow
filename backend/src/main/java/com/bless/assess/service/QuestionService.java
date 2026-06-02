package com.bless.assess.service;

import com.bless.assess.dto.CreateQuestionRequest;
import com.bless.assess.entity.QuestionBank;
import com.bless.assess.mapper.QuestionBankMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 题库管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionService {
    
    private final QuestionBankMapper questionBankMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * GM后台：造题入库
     */
    public QuestionBank createQuestion(CreateQuestionRequest request) {
        QuestionBank question = new QuestionBank();
        question.setStem(request.getStem());
        question.setQuestionType(request.getQuestionType());
        question.setDifficulty(request.getDifficulty());
        question.setEnabled(true);
        
        // 序列化选项
        try {
            if (request.getOptions() != null) {
                question.setOptions(objectMapper.writeValueAsString(request.getOptions()));
            }
            if (request.getCorrectAnswer() != null) {
                question.setCorrectAnswer(objectMapper.writeValueAsString(request.getCorrectAnswer()));
            }
            if (request.getTags() != null) {
                question.setTags(objectMapper.writeValueAsString(request.getTags()));
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON序列化失败", e);
        }
        
        question.setCreatedAt(java.time.LocalDateTime.now());
        question.setUpdatedAt(java.time.LocalDateTime.now());
        
        questionBankMapper.insert(question);
        
        log.info("造题成功: questionId={}, type={}", question.getId(), request.getQuestionType());
        return question;
    }
    
    /**
     * 根据ID查询题目（不含答案详情）
     */
    public QuestionBank getQuestionById(Long id) {
        return questionBankMapper.selectById(id);
    }
}
