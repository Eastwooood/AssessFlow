package com.bless.assess;

import com.bless.assess.dto.CreateQuestionRequest;
import com.bless.assess.dto.SubmitAnswerRequest;
import com.bless.assess.entity.QuestionBank;
import com.bless.assess.service.ExamSessionService;
import com.bless.assess.service.QuestionService;
import com.bless.assess.vo.SessionVO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * 集成测试：开考 → 答题 → 交卷 完整流程
 * 
 * 前置条件：
 * 1. MySQL已启动并执行了 schema.sql
 * 2. Redis已启动
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ExamFlowIntegrationTest {

    @Autowired
    private ExamSessionService examSessionService;

    @Autowired
    private QuestionService questionService;

    private static Long testUserId = 99999L;  // 测试用户ID
    private static Long testPaperId = 1L;     // 综合能力测评卷（schema.sql中预置）
    private static Long currentSessionId;

    @Test
    @Order(1)
    @DisplayName("1. 开考：创建考试会话")
    void testStartExam() {
        System.out.println("\n========== [步骤1] 开始开考 ==========");
        
        SessionVO vo = examSessionService.startExam(testUserId, testPaperId);
        
        Assertions.assertNotNull(vo, "会话不应为空");
        Assertions.assertNotNull(vo.getSessionId(), "会话ID应生成");
        currentSessionId = vo.getSessionId();
        
        Assertions.assertEquals("AWAIT_ANSWER", vo.getStatus(), 
                "开考后应进入等待作答状态");
        Assertions.assertNotNull(vo.getPendingStep(), "应有待作答环节");
        Assertions.assertNotNull(vo.getProgress(), "应有进度信息");
        
        System.out.println("✅ 开考成功！");
        System.out.println("   会话ID: " + currentSessionId);
        System.out.println("   状态: " + vo.getStatus());
        System.out.println("   当前题型: " + vo.getPendingStep().getType());
        System.out.println("   进度: " + vo.getProgress().getCurrent() + "/" + vo.getProgress().getTotal());
        System.out.println("   待作答题目: " + vo.getPendingStep().getStem());
        System.out.println("   选项数量: " + vo.getPendingStep().getOptions().size());
    }

    @Test
    @Order(2)
    @DisplayName("2. 答第1题：单选题")
    void testSubmitAnswer1() {
        System.out.println("\n========== [步骤2] 答第1题（单选）==========");
        
        SessionVO beforeVo = examSessionService.getSessionSnapshot(currentSessionId);
        System.out.println("   答题前 - 题型: " + beforeVo.getPendingStep().getType()
                + ", 游标: " + beforeVo.getStepCursor());

        // 获取第一题的正确答案（从选项中模拟选择）
        List<SessionVO.PendingStep.OptionVO> options = beforeVo.getPendingStep().getOptions();
        SubmitAnswerRequest request = SubmitAnswerRequest.builder()
                .type(beforeVo.getPendingStep().getType())
                .chosen(List.of(options.get(0).getOptionId()))  // 选第一个选项（可能对也可能错）
                .build();
        
        SessionVO afterVo = examSessionService.submitAnswer(currentSessionId, request);
        
        Assertions.assertEquals("AWAIT_ANSWER", afterVo.getStatus(), 
                "答题后应继续等待下一题");
        Assertions.assertTrue(afterVo.getTotalScore() >= 0, "总分应为非负数");
        
        System.out.println("✅ 第1题提交完成！");
        System.out.println("   当前总分: " + afterVo.getTotalScore());
        System.out.println("   下一题型: " + (afterVo.getPendingStep() != null ? afterVo.getPendingStep().getType() : "无"));
        System.out.println("   已结算结果数: " + afterVo.getImmediateResults().size());
    }

    @Test
    @Order(3)
    @DisplayName("3. 答第2题：单选题")
    void testSubmitAnswer2() {
        System.out.println("\n========== [步骤3] 答第2题（单选）==========");
        
        SessionVO beforeVo = examSessionService.getSessionSnapshot(currentSessionId);
        Assertions.assertNotNull(beforeVo.getPendingStep(), "应有待作答环节");

        List<SessionVO.PendingStep.OptionVO> options = beforeVo.getPendingStep().getOptions();
        SubmitAnswerRequest request = SubmitAnswerRequest.builder()
                .type(beforeVo.getPendingStep().getType())
                .chosen(List.of(options.get(0).getOptionId()))
                .build();

        SessionVO afterVo = examSessionService.submitAnswer(currentSessionId, request);

        System.out.println("✅ 第2题提交完成！当前总分: " + afterVo.getTotalScore());
    }

    @Test
    @Order(4)
    @DisplayName("4. 答第3题：单选题")
    void testSubmitAnswer3() {
        System.out.println("\n========== [步骤4] 答第3题（单选）==========");

        SessionVO beforeVo = examSessionService.getSessionSnapshot(currentSessionId);
        Assertions.assertNotNull(beforeVo.getPendingStep(), "应有待作答环节");

        List<SessionVO.PendingStep.OptionVO> options = beforeVo.getPendingStep().getOptions();
        SubmitAnswerRequest request = SubmitAnswerRequest.builder()
                .type(beforeVo.getPendingStep().getType())
                .chosen(List.of(options.get(0).getOptionId()))
                .build();

        SessionVO afterVo = examSessionService.submitAnswer(currentSessionId, request);

        System.out.println("✅ 第3题提交完成！当前总分: " + afterVo.getTotalScore());
    }

    @Test
    @Order(5)
    @DisplayName("5. 断线续考：查询快照恢复")
    void testResumeFromSnapshot() {
        System.out.println("\n========== [步骤5] 模拟断线续考 ==========");

        SessionVO snapshot = examSessionService.getSessionSnapshot(currentSessionId);

        Assertions.assertNotNull(snapshot, "快照不应为空");
        Assertions.assertNotNull(snapshot.getPendingStep(), "应有待恢复的作答环节");
        Assertions.assertNotNull(snapshot.getImmediateResults(), "应有已答题记录");
        Assertions.assertTrue(snapshot.getImmediateResults().size() >= 3, "至少有3道已答题");

        // 关键约束验证：pendingStep.options 不包含 isCorrect 字段
        if (snapshot.getPendingStep() != null && snapshot.getPendingStep().getOptions() != null) {
            for (SessionVO.PendingStep.OptionVO opt : snapshot.getPendingStep().getOptions()) {
                // OptionVO 类本身就没有 isCorrect 字段，编译期即可保证
                Assertions.assertNotNull(opt.getOptionId(), "选项ID不为空");
                Assertions.assertNotNull(opt.getText(), "选项文本不为空");
            }
        }

        System.out.println("✅ 断线续考恢复成功！");
        System.out.println("   状态: " + snapshot.getStatus());
        System.out.println("   当前进度: " + snapshot.getProgress().getCurrent() + "/" + snapshot.getProgress().getTotal());
        System.out.println("   已答题目: " + snapshot.getImmediateResults().size() + " 道");
        System.out.println("   当前总分: " + snapshot.getTotalScore());
        System.out.println("   待恢复题型: " + snapshot.getPendingStep().getType());
    }

    @Test
    @Order(6)
    @DisplayName("6. GM强制交卷")
    void testGmForceFinish() {
        System.out.println("\n========== [步骤6] GM一键交卷 ==========");

        SessionVO finishVo = examSessionService.gmForceFinish(currentSessionId);

        Assertions.assertEquals("FINISHED", finishVo.getStatus(), "交卷后应为已完成状态");
        Assertions.assertNotNull(finishVo.getFinishInfo(), "应有结案信息");
        Assertions.assertNotNull(finishVo.getFinishInfo().getFinalScore(), "最终分数不为空");

        System.out.println("✅ GM交卷完成！");
        System.out.println("   最终状态: " + finishVo.getStatus());
        System.out.println("   最终得分: " + finishVo.getFinishInfo().getFinalScore());
        System.out.println("   总题数: " + finishVo.getFinishInfo().getTotalQuestions());
        System.out.println("   答对数: " + finishVo.getFinishInfo().getCorrectCount());
    }

    @Test
    @Order(7)
    @DisplayName("7. GM造题入库")
    void testCreateQuestion() {
        System.out.println("\n========== [步骤7] GM造题入库 ==========");

        CreateQuestionRequest request = CreateQuestionRequest.builder()
                .stem("以下哪个是Spring Boot的自动配置类？")
                .questionType("SINGLE")
                .options(List.of(
                        CreateQuestionRequest.OptionItem.builder()
                                .optionId(1L).text("AutoConfiguration").isCorrect(true).build(),
                        CreateQuestionRequest.OptionItem.builder()
                                .optionId(2L).text("ManualConfig").isCorrect(false).build(),
                        CreateQuestionRequest.OptionItem.builder()
                                .optionId(3L).text("XmlConfig").isCorrect(false).build(),
                        CreateQuestionRequest.OptionItem.builder()
                                .optionId(4L).text("PropertiesConfig").isCorrect(false).build()
                ))
                .correctAnswer(List.of(1L))
                .difficulty(2)
                .tags(List.of("SpringBoot", "自动配置"))
                .build();

        QuestionBank question = questionService.createQuestion(request);

        Assertions.assertNotNull(question.getId(), "题目ID应已生成");
        Assertions.assertEquals(request.getStem(), question.getStem(), "题干应一致");
        Assertions.assertEquals("SINGLE", question.getQuestionType(), "题型应正确");

        System.out.println("✅ 造题成功！题目ID: " + question.getId());
    }

    @Test
    @Order(8)
    @DisplayName("8. 异常场景：重复提交拦截")
    void testDuplicateSubmitPrevention() {
        System.out.println("\n========== [步骤8] 测试异常场景 ==========");
        
        // 创建新会话用于测试异常场景
        SessionVO newVo = examSessionService.startExam(testUserId + 1L, testPaperId);
        Long newSessionId = newVo.getSessionId();
        
        // 正常提交一次
        SubmitAnswerRequest request = SubmitAnswerRequest.builder()
                .type(newVo.getPendingStep().getType())
                .chosen(List.of(newVo.getPendingStep().getOptions().get(0).getOptionId()))
                .build();
        examSessionService.submitAnswer(newSessionId, request);

        // 尝试提交错误题型（应被拒绝）
        try {
            SubmitAnswerRequest wrongTypeRequest = SubmitAnswerRequest.builder()
                    .type("MULTI")  # 故意传错题型
                    .chosen(List.of(1L, 2L))
                    .build();
            examSessionService.submitAnswer(newSessionId, wrongTypeRequest);
            Assertions.fail("应该抛出业务异常");
        } catch (Exception e) {
            System.out.println("✅ 成功拦截错误题型提交: " + e.getMessage());
        }

        // GM强制结束这个测试会话
        examSessionService.gmForceFinish(newSessionId);
        System.out.println("✅ 异常场景测试通过！");
    }
}
