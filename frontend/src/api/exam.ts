import request from './request'
import type { ApiResult, LoginVO, LoginForm, SessionVO, SubmitAnswerRequest } from '../types'

export const login = (data: LoginForm) =>
  request.post<ApiResult<LoginVO>>('/auth/login', data).then((res) => res.data)

export const startExam = (paperId: number) =>
  request.post<ApiResult<SessionVO>>(`/exams/${paperId}/sessions`).then((res) => res.data)

export const getSessionSnapshot = (sessionId: number) =>
  request.get<ApiResult<SessionVO>>(`/sessions/${sessionId}`).then((res) => res.data)

export const submitAnswer = (sessionId: number, data: SubmitAnswerRequest) =>
  request.post<ApiResult<SessionVO>>(`/sessions/${sessionId}/submit`, data).then((res) => res.data)

export const abandonSession = (sessionId: number) =>
  request.post<ApiResult<SessionVO>>(`/sessions/${sessionId}/abandon`).then((res) => res.data)

export const gmJumpToStep = (sessionId: number, to: number) =>
  request.post<ApiResult<SessionVO>>(`/admin/sessions/${sessionId}/jump?to=${to}`).then((res) => res.data)

export const gmForceFinish = (sessionId: number) =>
  request.post<ApiResult<SessionVO>>(`/admin/sessions/${sessionId}/finish`).then((res) => res.data)
