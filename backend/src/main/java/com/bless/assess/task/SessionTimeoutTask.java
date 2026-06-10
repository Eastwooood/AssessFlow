package com.bless.assess.task;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bless.assess.entity.ExamSession;
import com.bless.assess.enums.SessionStatus;
import com.bless.assess.mapper.ExamSessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 会话超时扫描任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionTimeoutTask {

    private final ExamSessionMapper examSessionMapper;

    @Value("${assess.session.timeout-minutes:30}")
    private long timeoutMinutes;

    @Scheduled(fixedDelayString = "${assess.session.scan-ms:60000}")
    public void scanTimeout() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(timeoutMinutes);
        int rows = examSessionMapper.update(null, new LambdaUpdateWrapper<ExamSession>()
                .in(ExamSession::getStatus, SessionStatus.IN_PROGRESS.name(), SessionStatus.AWAIT_ANSWER.name())
                .lt(ExamSession::getUpdatedAt, deadline)
                .set(ExamSession::getStatus, SessionStatus.ABANDONED.name())
                .set(ExamSession::getUpdatedAt, LocalDateTime.now()));
        if (rows > 0) {
            log.warn("超时扫描置ABANDONED: {} 条会话", rows);
        }
    }
}
