package com.bless.assess.controller;

import com.bless.assess.service.ExamSessionService;
import com.bless.assess.util.SecurityUtil;
import com.bless.assess.vo.ApiResult;
import com.bless.assess.vo.SessionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 考试业务接口（薄Controller层：只做参数接收、鉴权、调用Service）
 */
@RestController
@RequestMapping("/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamSessionService examSessionService;

    /**
     * 开考：创建考试会话
     */
    @PostMapping("/{paperId}/sessions")
    public ApiResult<SessionVO> startExam(@PathVariable Long paperId) {
        Long userId = SecurityUtil.currentUserId();
        SessionVO vo = examSessionService.startExam(userId, paperId);
        return ApiResult.success(vo);
    }
}
