package com.bless.assess.controller;

import com.bless.assess.dto.CreateQuestionRequest;
import com.bless.assess.entity.QuestionBank;
import com.bless.assess.service.ExamSessionService;
import com.bless.assess.service.QuestionService;
import com.bless.assess.vo.ApiResult;
import com.bless.assess.vo.SessionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * GM运营后台接口
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    
    private final QuestionService questionService;
    private final ExamSessionService examSessionService;
    
    /**
     * 造题入库
     */
    @PostMapping("/questions")
    public ApiResult<QuestionBank> createQuestion(@Valid @RequestBody CreateQuestionRequest request) {
        QuestionBank question = questionService.createQuestion(request);
        return ApiResult.success(question);
    }
    
    /**
     * GM跳转到指定环节
     */
    @PostMapping("/sessions/{id}/jump")
    public ApiResult<SessionVO> jumpToStep(
            @PathVariable Long id,
            @RequestParam Integer to) {
        SessionVO vo = examSessionService.gmJumpToStep(id, to);
        return ApiResult.success(vo);
    }
    
    /**
     * GM一键交卷
     */
    @PostMapping("/sessions/{id}/finish")
    public ApiResult<SessionVO> forceFinish(@PathVariable Long id) {
        SessionVO vo = examSessionService.gmForceFinish(id);
        return ApiResult.success(vo);
    }
}
