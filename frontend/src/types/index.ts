export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export interface LoginVO {
  token: string
  userId: number
  role: string
}

export interface LoginForm {
  username: string
  password: string
}

export interface SubmitAnswerRequest {
  type: string
  chosen: number[]
}

export interface OptionVO {
  optionId: number
  text: string
}

export interface PendingStep {
  type: string
  questionId: number
  stem: string
  options: OptionVO[]
}

export interface ImmediateResult {
  stepIndex: number
  type: string
  gotScore: number
}

export interface Progress {
  current: number
  total: number
}

export interface FinishInfo {
  finalScore: number
  totalQuestions: number
  correctCount: number
}

export interface SessionVO {
  sessionId: number
  userId: number
  paperId: number
  status: string
  stepCursor: number
  progress: Progress
  totalScore: number
  immediateResults: ImmediateResult[]
  pendingStep: PendingStep | null
  finishInfo: FinishInfo | null
  startedAt: string
  updatedAt: string
}
