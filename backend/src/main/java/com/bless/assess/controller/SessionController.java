package com.bless.assess.controller;

import com.bless.assess.dto.SubmitAnswerRequest;
import com.bless.assess.service.ExamSessionService;
import com.bless.assess.vo.ApiResult;
import com.bless.assess.vo.SessionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 会话接口（提交答案、查询快照）
 */
@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
public class SessionController {
    
    private final ExamSessionService examSessionService;
    
    /**
     * 提交答案
     */
    @PostMapping("/{sessionId}/submit")
    public ApiResult<SessionVO> submitAnswer(
            @PathVariable Long sessionId,
            @Valid @RequestBody SubmitAnswerRequest request) {
        SessionVO vo = examSessionService.submitAnswer(sessionId, request);
        return ApiResult.success(vo);
    }
    
    /**
     * 查询会话快照（断线续考用）
     */
    @GetMapping("/{sessionId}")
    public ApiResult<SessionVO> getSessionSnapshot(@PathVariable Long sessionId) {
        SessionVO vo = examSessionService.getSessionSnapshot(sessionId);
        return ApiResult.success(vo);
    }
}
