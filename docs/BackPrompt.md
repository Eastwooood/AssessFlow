# 自适应在线测评引擎需求文档（纯文字提取）
## 技术栈：Java17 + SpringBoot3 + MyBatis-Plus + MySQL + Redis，可中断续考、防作弊在线考试系统
## 一、核心模型
### 1. exam_session（考试会话主表）
字段：id、user_id、paper_id、step_cursor(int)、status(枚举：INIT/IN_PROGRESS/AWAIT_ANSWER/FINISHED/ABANDONED)、total_score、started_at、updated_at；
附加：steps（JSON存储List<SessionStep>，或拆分为session_step子表）。

### 2. SessionStep（单步环节）
字段：index、type(枚举 INFO/SCORE_SNAPSHOT自动环节；SINGLE/MULTI/JUDGE/BLANK作答环节)、question_id、candidate_options(List<Long>，服务端打乱候选项、仅服务端存储、不返回前端)、user_answer、got_score、settled。

### 不变量规则
step_cursor永远指向**下一个未结算环节**；
自动环节就地结算后cursor+1；作答环节submit结算后cursor+1，作答期间cursor停留在当前下标。

## 二、5张配置表（全部支持热加载reload，配置重载不影响正在运行中的考试会话）
1. PAPER_TEMPLATE_CONF：试卷模板配置，定义整套试卷环节序列
2. QUESTION_TYPE_CONF：题型规则配置，是否打乱选项、对应评分方式
3. GRADING_RULE_CONF：评分规则配置
4. QUESTION_BANK_CONF：题库表，题干/题型/选项/正确答案/难度/标签
5. QUESTION_BLACKLIST_CONF：题目下架黑名单

## 三、对外接口（业务核心：2个业务交互接口 + 1个会话恢复接口 + GM后台管理接口）
### 1. POST /api/exams/{paperId}/sessions 【开考创建会话】
- 创建exam_session会话；按试卷模板生成全量steps；
- 对固定题预生成候选选项（服务端打乱并持久化入库）；
- 内部调用advanceExam推进引擎，自动跳到第一个作答环节；
- 返回会话视图数据。

### 2. POST /api/sessions/{sessionId}/submit 【提交答案】
前置三连校验：
① 当前会话status==AWAIT_ANSWER；
② 当前step.type和请求提交题型一致；
③ 用户所选答案req.chosen全部落在step.candidate_options候选集合内；
- 校验通过后调用GradingService完成评分、累加total_score、标记step.settled=true；
- 内部调用advanceExam续跑考试引擎；
- 返回最新会话视图。

### 3. GET /api/sessions/{sessionId} 【查询会话快照/断线续考】
返回会话快照：status/stepCursor/progress/immediateResults[]/pendingStep{type,questionId,stem,options[{optionId,text}]}、finish结案信息；
> 关键约束：pendingStep.options 绝不向下透出正确答案isCorrect字段。

### 4. GM运营后台接口
1. POST /api/admin/questions 【造题入库】
2. POST /api/admin/sessions/{id}/jump?to=N 【GM跳环节】
3. POST /api/admin/sessions/{id}/finish 【GM一键交卷】
   GM接口同样走完整参数校验、数据持久化逻辑。

## 四、ExamEngine.advanceExam 考试推进引擎核心规则
从step_cursor下标开始顺序遍历steps：
1. 遇到**INFO/SCORE_SNAPSHOT自动环节**：就地结算、settled=true、cursor+1，继续向后遍历；
2. 遇到**作答环节**：
    - 固定题型：开考startExam阶段已经预生成候选；
    - 自适应题型：进入本环节时，根据考生历史正确率从题库即时抽题生成候选项；
    - 命中作答环节后终止遍历return，等待前端submit提交；
3. 全部steps遍历走完：会话status置FINISHED，考试结束。

## 五、三大核心设计
### 核心3：预生成 vs 即时生成 + 三级回退容错
- 固定题：开考startExam时**预生成候选项**；
- 自适应题：进入作答环节时，依据考生历史正确率**即时从题库抽题**；
- 作答取题三级回退兜底（防止题目下架导致考试作废）：
  ① 优先使用预生成题目；
  ② 校验题目仍在题库内、不在QUESTION_BLACKLIST下架名单；
  ③ 校验失败则同难度即时重抽新题 + 打印WARN日志，绝不整卷作废。

### 核心4：通用可扩展评分器GradingService
```java
// 统一入参：题型、正确答案集合、用户作答集合
GradingService.grade(type, correct, chosen)
```
- 采用Grader注册表分发策略：
    - SINGLE/JUDGE：答案全等才满分，错选0分；
    - MULTI多选：按GRADING_RULE配置支持全对满分/部分得分/错选0分；
    - BLANK填空：答案文本归一化后比对得分；
- 新增题型仅需要新增Grader实现并注册，引擎主体代码零改动。

## 六、其他工程约束
1. Controller层做薄：只做参数接收、鉴权、调用Service，全部业务下沉至Service；
2. 分布式防重复：Redis对同一会话submit加分布式锁，使用step_cursor做幂等键，防并发重复提交/接口重放；
3. 统一全局异常处理器 + 标准化返回错误码，关键校验失败返回友好错误、不泄露底层内部细节；
4. 交付物：MySQL建表SQL、项目核心实体类、application.yml配置样例，附带可跑通「开考→答3题→交卷」的集成测试用例。

## 补充流程注释
- 自动环节：结算完毕cursor+1向后继续；
- 作答环节：cursor停在当前下标、status=AWAIT_ANSWER，用户submit提交并结算完毕后再cursor+1；
- GET查询接口返回全量进度progress{current,total}，前端依据返回pendingStep重建考试UI，用于断线续考场景。

