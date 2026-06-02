package com.bless.assess.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bless.assess.dto.SubmitAnswerRequest;
import com.bless.assess.entity.*;
import com.bless.assess.enums.SessionStatus;
import com.bless.assess.enums.StepType;
import com.bless.assess.exception.BusinessException;
import com.bless.assess.mapper.*;
import com.bless.assess.vo.SessionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 考试会话核心服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamSessionService {
    
    private final ExamSessionMapper examSessionMapper;
    private final SessionStepMapper sessionStepMapper;
    private final QuestionBankMapper questionBankMapper;
    private final PaperTemplateConfMapper paperTemplateConfMapper;
    private final QuestionBlacklistConfMapper blacklistMapper;
    private final GradingService gradingService;
    private final StringRedisTemplate redisTemplate;
    private static final String SUBMIT_LOCK_PREFIX = "exam:submit:lock:";
    private static final int LOCK_TIMEOUT_SECONDS = 10;
    
    /**
     * 开考：创建会话并初始化步骤
     */
    @Transactional
    public SessionVO startExam(Long userId, Long paperId) {
        // 1. 获取试卷模板配置
        PaperTemplateConf template = paperTemplateConfMapper.selectById(paperId);
        if (template == null || !Boolean.TRUE.equals(template.getEnabled())) {
            throw new BusinessException("试卷模板不存在或未启用");
        }
        
        // 2. 创建会话
        ExamSession session = new ExamSession();
        session.setUserId(userId);
        session.setPaperId(paperId);
        session.setStepCursor(0);
        session.setStatus(SessionStatus.IN_PROGRESS.name());
        session.setTotalScore(0.0);
        session.setStartedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        session.setCreatedAt(LocalDateTime.now());
        examSessionMapper.insert(session);
        
        // 3. 解析模板生成步骤序列
        List<Map<String, Object>> stepSequence = parseStepSequence(template.getStepSequence());
        List<SessionStep> steps = generateSteps(session.getId(), stepSequence);
        
        // 4. 批量插入步骤
        for (SessionStep step : steps) {
            sessionStepMapper.insert(step);
        }
        
        // 5. 运行推进引擎
        advanceExam(session);
        
        // 6. 更新会话
        examSessionMapper.updateById(session);
        
        log.info("开考成功: sessionId={}, userId={}, paperId={}", session.getId(), userId, paperId);
        return buildSessionVO(session, steps);
    }
    
    /**
     * 提交答案（带分布式锁防重复提交）
     */
    @Transactional
    public SessionVO submitAnswer(Long sessionId, SubmitAnswerRequest request) {
        // 1. 加分布式锁（使用stepCursor作为幂等键的一部分）
        String lockKey = SUBMIT_LOCK_PREFIX + sessionId;
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(locked)) {
            throw new BusinessException("SUBMIT_BUSY", "请勿重复提交，正在处理中");
        }
        
        try {
            // 2. 查询会话
            ExamSession session = examSessionMapper.selectById(sessionId);
            if (session == null) {
                throw new BusinessException("SESSION_NOT_FOUND", "会话不存在");
            }
            
            // 前置校验三连
            if (!SessionStatus.AWAIT_ANSWER.name().equals(session.getStatus())) {
                throw new BusinessException("STATUS_ERROR", "当前状态不允许提交答案");
            }
            
            List<SessionStep> steps = sessionStepMapper.selectBySessionIdOrderByIndex(sessionId);
            if (session.getStepCursor() >= steps.size()) {
                throw new BusinessException("NO_PENDING_STEP", "没有待作答的环节");
            }
            
            SessionStep currentStep = steps.get(session.getStepCursor());
            
            // 校验题型一致性
            if (!request.getType().equals(currentStep.getType())) {
                throw new BusinessException("TYPE_MISMATCH", "提交的题型与当前环节不匹配");
            }
            
            // 校验用户答案在候选选项内
            Set<Long> candidateOptions = parseCandidateOptions(currentStep.getCandidateOptions());
            Set<Long> chosenAnswers = new HashSet<>(request.getChosen());
            for (Long choice : chosenAnswers) {
                if (!candidateOptions.contains(choice)) {
                    throw new BusinessException("INVALID_CHOICE", "所选答案不在候选范围内");
                }
            }
            
            // 3. 评分
            QuestionBank question = questionBankMapper.selectById(currentStep.getQuestionId());
            if (question == null) {
                throw new BusinessException("QUESTION_NOT_FOUND", "题目不存在");
            }
            
            Set<Long> correctAnswers = parseCorrectAnswer(question.getCorrectAnswer());
            double gotScore = gradingService.grade(request.getType(), correctAnswers, chosenAnswers);
            
            // 4. 更新步骤状态
            currentStep.setUserAnswer(com.fasterxml.jackson.core.type.TypeReference.defaultInstance()
                    .getType().toString());  // 简化处理，实际应序列化chosenAnswers
            currentStep.setUserAnswer(chosenAnswers.toString());
            currentStep.setGotScore(gotScore);
            currentStep.setSettled(true);
            sessionStepMapper.updateById(currentStep);
            
            // 5. 累加总分
            session.setTotalScore(session.getTotalScore() + gotScore);
            session.setUpdatedAt(LocalDateTime.now());
            
            // 6. 推进引擎
            advanceExam(session);
            
            // 7. 持久化
            examSessionMapper.updateById(session);
            
            log.info("答题提交完成: sessionId={}, stepIndex={}, score={}", 
                    sessionId, currentStep.getStepIndex(), gotScore);
            
            return buildSessionVO(session, steps);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }
    
    /**
     * 查询会话快照（断线续考用）
     */
    public SessionVO getSessionSnapshot(Long sessionId) {
        ExamSession session = examSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("SESSION_NOT_FOUND", "会话不存在");
        }
        
        List<SessionStep> steps = sessionStepMapper.selectBySessionIdOrderByIndex(sessionId);
        return buildSessionVO(session, steps);
    }
    
    /**
     * 考试推进引擎（核心逻辑）
     */
    public void advanceExam(ExamSession session) {
        List<SessionStep> steps = sessionStepMapper.selectBySessionIdOrderByIndex(session.getId());
        Set<Long> blacklistedIds = blacklistMapper.selectAllBlacklistedIds();
        
        while (session.getStepCursor() < steps.size()) {
            SessionStep step = steps.get(session.getStepCursor());
            StepType stepType = StepType.valueOf(step.getType());
            
            if (stepType.isAutoStep()) {
                // 自动环节：就地结算、cursor+1
                step.setSettled(true);
                sessionStepMapper.updateById(step);
                session.setStepCursor(session.getStepCursor() + 1);
                log.debug("自动环节结算: stepIndex={}", step.getStepIndex());
            } else {
                // 作答环节
                if (step.getQuestionId() != null) {
                    // 题目三级回退验证
                    boolean valid = validateQuestion(step.getQuestionId(), blacklistedIds);
                    
                    if (!valid) {
                        // 三级回退：同难度即时重抽
                        log.warn("题目已下架，执行三级回退重抽: originalQuestionId={}", step.getQuestionId());
                        QuestionBank newQuestion = fallbackReselect(step.getType(), step.getQuestionId(), blacklistedIds);
                        if (newQuestion != null) {
                            step.setQuestionId(newQuestion.getId());
                            // 重新打乱候选选项
                            Set<Long> newCandidates = shuffleAndExtractOptionIds(newQuestion);
                            step.setCandidateOptions(newCandidates.toString());
                            sessionStepMapper.updateById(step);
                            log.info("回退重抽成功: newQuestionId={}, sessionId={}", newQuestion.getId(), session.getId());
                        } else {
                            log.error("回退重抽失败，跳过本题: sessionId={}, stepIndex={}", session.getId(), step.getStepIndex());
                            step.setSettled(true);
                            session.setStepCursor(session.getStepCursor() + 1);
                            continue;
                        }
                    }
                } else {
                    // 自适应题型：即时从题库抽题
                    QuestionBank adaptiveQuestion = selectAdaptiveQuestion(
                            step.getType(), calculateCorrectRate(steps), blacklistedIds);
                    if (adaptiveQuestion != null) {
                        step.setQuestionId(adaptiveQuestion.getId());
                        Set<Long> candidates = shuffleAndExtractOptionIds(adaptiveQuestion);
                        step.setCandidateOptions(candidates.toString());
                        sessionStepMapper.updateById(step);
                    }
                }
                
                // 命中作答环节，终止遍历，等待前端submit
                session.setStatus(SessionStatus.AWAIT_ANSWER.name());
                session.setUpdatedAt(LocalDateTime.now());
                log.debug("进入作答等待: stepIndex={}, type={}", step.getStepIndex(), step.getType());
                break;
            }
        }
        
        // 全部步骤走完
        if (session.getStepCursor() >= steps.size()) {
            session.setStatus(SessionStatus.FINISHED.name());
            session.setUpdatedAt(LocalDateTime.now());
            log.info("考试结束: sessionId={}, totalScore={}", session.getId(), session.getTotalScore());
        }
    }
    
    /**
     * GM后台：强制跳转到指定环节
     */
    @Transactional
    public SessionVO gmJumpToStep(Long sessionId, Integer targetStep) {
        ExamSession session = examSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("SESSION_NOT_FOUND", "会话不存在");
        }
        
        List<SessionStep> steps = sessionStepMapper.selectBySessionIdOrderByIndex(sessionId);
        if (targetStep < 0 || targetStep >= steps.size()) {
            throw new BusinessException("INVALID_STEP", "目标环节超出范围");
        }
        
        // 结算中间所有自动环节
        for (int i = Math.min(session.getStepCursor(), targetStep); i < targetStep; i++) {
            SessionStep step = steps.get(i);
            StepType type = StepType.valueOf(step.getType());
            if (type.isAutoStep()) {
                step.setSettled(true);
                sessionStepMapper.updateById(step);
            }
        }
        
        session.setStepCursor(targetStep);
        session.setStatus(SessionStatus.AWAIT_ANSWER.name());
        session.setUpdatedAt(LocalDateTime.now());
        examSessionMapper.updateById(session);
        
        // 推进到目标位置
        advanceExam(session);
        examSessionMapper.updateById(session);
        
        log.warn("GM跳转: sessionId={}, toStep={}", sessionId, targetStep);
        return buildSessionVO(session, steps);
    }
    
    /**
     * GM后台：一键交卷
     */
    @Transactional
    public SessionVO gmForceFinish(Long sessionId) {
        ExamSession session = examSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("SESSION_NOT_FOUND", "会话不存在");
        }
        
        session.setStatus(SessionStatus.FINISHED.name());
        session.setUpdatedAt(LocalDateTime.now());
        examSessionMapper.updateById(session);
        
        log.warn("GM强制交卷: sessionId={}", sessionId);
        return getSessionSnapshot(sessionId);
    }
    
    // ==================== 私有辅助方法 ====================
    
    private List<Map<String, Object>> parseStepSequence(String json) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json, 
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            throw new BusinessException("TEMPLATE_PARSE_ERROR", "试卷模板解析失败");
        }
    }
    
    private List<SessionStep> generateSteps(Long sessionId, List<Map<String, Object>> sequence) {
        List<SessionStep> steps = new ArrayList<>();
        int index = 0;
        
        for (Map<String, Object> item : sequence) {
            String typeStr = (String) item.get("type");
            StepType type = StepType.valueOf(typeStr);
            
            if (type.isAutoStep()) {
                // 自动环节：1个INFO或SCORE_SNAPSHOT
                SessionStep step = createStep(sessionId, index++, typeStr, null);
                steps.add(step);
            } else {
                // 作答环节：根据questionCount创建多个步骤
                int count = item.containsKey("questionCount") ? (Integer) item.get("questionCount") : 1;
                
                // 固定题：预生成候选选项
                if (!isAdaptive(type)) {
                    List<QuestionBank> questions = questionBankMapper.randomSelectByType(typeStr, count);
                    Set<Long> blacklistedIds = blacklistMapper.selectAllBlacklistedIds();
                    
                    for (QuestionBank q : questions) {
                        SessionStep step = createStep(sessionId, index++, typeStr, q.getId());
                        
                        // 打乱候选选项
                        Set<Long> candidateIds = shuffleAndExtractOptionIds(q);
                        step.setCandidateOptions(candidateIds.toString());
                        steps.add(step);
                    }
                } else {
                    // 自适应题：暂时只创建空壳，进入环节时再抽题
                    for (int i = 0; i < count; i++) {
                        SessionStep step = createStep(sessionId, index++, typeStr, null);
                        steps.add(step);
                    }
                }
            }
        }
        
        return steps;
    }
    
    private SessionStep createStep(Long sessionId, Integer index, String type, Long questionId) {
        SessionStep step = new SessionStep();
        step.setSessionId(sessionId);
        step.setStepIndex(index);
        step.setType(type);
        step.setQuestionId(questionId);
        step.setSettled(false);
        step.setGotScore(0.0);
        return step;
    }
    
    private Set<Long> parseCandidateOptions(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptySet();
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return new HashSet<>(mapper.readValue(json, 
                    new com.fasterxml.jackson.core.type.TypeReference<List<Long>>() {}));
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }
    
    private Set<Long> parseCorrectAnswer(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptySet();
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return new HashSet<>(mapper.readValue(json, 
                    new com.fasterxml.jackson.core.type.TypeReference<List<Long>>() {}));
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }
    
    private Set<Long> shuffleAndExtractOptionIds(QuestionBank question) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List<Map<String, Object>> options = mapper.readValue(question.getOptions(),
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
            
            List<Long> optionIds = options.stream()
                    .map(o -> ((Number) o.get("optionId")).longValue())
                    .collect(Collectors.toList());
            
            // 服务端打乱
            Collections.shuffle(optionIds);
            return new LinkedHashSet<>(optionIds);
        } catch (Exception e) {
            log.error("解析选项失败: questionId={}", question.getId(), e);
            return Collections.emptySet();
        }
    }
    
    private boolean validateQuestion(Long questionId, Set<Long> blacklistedIds) {
        // 三级回退验证
        // ① 检查题目是否存在且启用
        QuestionBank q = questionBankMapper.selectById(questionId);
        if (q == null || !Boolean.TRUE.equals(q.getEnabled())) {
            return false;
        }
        // ② 检查是否在黑名单中
        if (blacklistedIds.contains(questionId)) {
            return false;
        }
        return true;
    }
    
    private QuestionBank fallbackReselect(String type, Long originalQuestionId, Set<Long> blacklistedIds) {
        QuestionBank orig = questionBankMapper.selectById(originalQuestionId);
        int difficulty = orig != null ? orig.getDifficulty() : 3;
        List<QuestionBank> candidates = questionBankMapper.randomSelectByTypeAndDifficulty(type, difficulty, 1);
        return candidates.isEmpty() ? null : candidates.get(0);
    }
    
    private boolean isAdaptive(StepType type) {
        // 可根据配置扩展自适应题型判断逻辑
        return false;  // 目前默认都是固定题
    }
    
    private QuestionBank selectAdaptiveQuestion(String type, double correctRate, Set<Long> blacklistedIds) {
        // 自适应出题：根据正确率调整难度
        int difficulty;
        if (correctRate > 0.8) {
            difficulty = 4;  // 高正确率 → 提高难度
        } else if (correctRate > 0.5) {
            difficulty = 3;
        } else {
            difficulty = 2;  // 低正确率 → 降低难度
        }
        
        List<QuestionBank> candidates = questionBankMapper.randomSelectByTypeAndDifficulty(type, difficulty, 1);
        return candidates.isEmpty() ? null : candidates.get(0);
    }
    
    private double calculateCorrectRate(List<SessionStep> steps) {
        long answeredCount = steps.stream().filter(s -> s.getSettled()).count();
        long correctCount = steps.stream()
                .filter(s -> s.getSettled() && s.getGotScore() != null && s.getGotScore() > 0)
                .count();
        return answeredCount > 0 ? (double) correctCount / answeredCount : 0.5;
    }
    
    private SessionVO buildSessionVO(ExamSession session, List<SessionStep> steps) {
        SessionVO vo = SessionVO.builder()
                .sessionId(session.getId())
                .userId(session.getUserId())
                .paperId(session.getPaperId())
                .status(session.getStatus())
                .stepCursor(session.getStepCursor())
                .totalScore(session.getTotalScore())
                .startedAt(session.getStartedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
        
        // 进度
        vo.setProgress(SessionVO.Progress.builder()
                .current(Math.min(session.getStepCursor(), steps.size()))
                .total(steps.size())
                .build());
        
        // 已结算的即时结果
        List<SessionVO.ImmediateResult> immediateResults = steps.stream()
                .filter(s -> Boolean.TRUE.equals(s.getSettled()))
                .map(s -> SessionVO.ImmediateResult.builder()
                        .stepIndex(s.getStepIndex())
                        .type(s.getType())
                        .gotScore(s.getGotScore())
                        .build())
                .collect(Collectors.toList());
        vo.setImmediateResults(immediateResults);
        
        // 待作答环节（关键约束：绝不透出isCorrect字段）
        if (SessionStatus.AWAIT_ANSWER.name().equals(session.getStatus()) 
                && session.getStepCursor() < steps.size()) {
            SessionStep pending = steps.get(session.getStepCursor());
            if (pending.getQuestionId() != null) {
                QuestionBank question = questionBankMapper.selectById(pending.getQuestionId());
                if (question != null) {
                    List<SessionVO.PendingStep.OptionVO> optionVOs = extractSafeOptions(question);
                    vo.setPendingStep(SessionVO.PendingStep.builder()
                            .type(pending.getType())
                            .questionId(pending.getQuestionId())
                            .stem(question.getStem())
                            .options(optionVOs)
                            .build());
                }
            }
        }
        
        // 结案信息
        if (SessionStatus.FINISHED.name().equals(session.getStatus())) {
            long totalQuestions = steps.stream()
                    .filter(s -> StepType.valueOf(s.getType()).isAnswerStep())
                    .count();
            long correctCount = steps.stream()
                    .filter(s -> s.getSettled() && s.getGotScore() != null && s.getGotScore() > 0)
                    .count();
            vo.setFinishInfo(SessionVO.FinishInfo.builder()
                    .finalScore(session.getTotalScore())
                    .totalQuestions((int) totalQuestions)
                    .correctCount((int) correctCount)
                    .build());
        }
        
        return vo;
    }
    
    private List<SessionVO.PendingStep.OptionVO> extractSafeOptions(QuestionBank question) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List<Map<String, Object>> options = mapper.readValue(question.getOptions(),
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
            
            return options.stream()
                    .map(o -> SessionVO.PendingStep.OptionVO.builder()
                            .optionId(((Number) o.get("optionId")).longValue())
                            .text((String) o.get("text"))
                            // 绝对不返回 isCorrect 字段
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("提取安全选项失败", e);
            return Collections.emptyList();
        }
    }
}
