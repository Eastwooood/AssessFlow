import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getSessionSnapshot, submitAnswer, abandonSession } from '../api/exam'
import type { SessionVO, SubmitAnswerRequest } from '../types'

export default function ExamPage() {
  const { sessionId } = useParams<{ sessionId: string }>()
  const navigate = useNavigate()
  const [session, setSession] = useState<SessionVO | null>(null)
  const [loading, setLoading] = useState(false)
  const [selected, setSelected] = useState<number[]>([])

  const fetchSession = async () => {
    if (!sessionId) return
    setLoading(true)
    try {
      const res = await getSessionSnapshot(Number(sessionId))
      setSession(res.data)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchSession()
  }, [sessionId])

  useEffect(() => {
    setSelected([])
  }, [session?.pendingStep?.questionId])

  const handleSubmit = async () => {
    if (!session || !session.pendingStep) return
    if (selected.length === 0) {
      alert('请至少选择一个选项')
      return
    }
    const payload: SubmitAnswerRequest = {
      type: session.pendingStep.type,
      chosen: selected,
    }
    setLoading(true)
    try {
      const res = await submitAnswer(session.sessionId, payload)
      setSession(res.data)
      alert('提交成功')
    } finally {
      setLoading(false)
    }
  }

  const handleAbandon = async () => {
    if (!session) return
    setLoading(true)
    try {
      const res = await abandonSession(session.sessionId)
      setSession(res.data)
      alert('已放弃会话')
    } finally {
      setLoading(false)
    }
  }

  if (!session) {
    return (
      <div style={{ height: '100vh', display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
        <div>加载中...</div>
      </div>
    )
  }

  const isFinished = session.status === 'FINISHED' || session.status === 'ABANDONED'

  return (
    <div style={{ padding: 24, maxWidth: 800, margin: '0 auto' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <h2 style={{ margin: 0 }}>考试会话 #{session.sessionId}</h2>
        <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
          <span>步骤: {session.progress?.current} / {session.progress?.total}</span>
          <span>得分: {session.totalScore ?? 0}</span>
          <button className="btn" onClick={() => navigate('/dashboard')}>返回控制台</button>
        </div>
      </div>

      {isFinished ? (
        <div className="card">
          <h3>考试已结束</h3>
          <p><strong>状态:</strong> {session.status}</p>
          {session.finishInfo && (
            <div style={{ marginTop: 12 }}>
              <p>最终得分: {session.finishInfo.finalScore}</p>
              <p>总题数: {session.finishInfo.totalQuestions}</p>
              <p>正确数: {session.finishInfo.correctCount}</p>
            </div>
          )}
        </div>
      ) : (
        <>
          {session.pendingStep ? (
            <div className="card">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
                <h3 style={{ margin: 0 }}>题目 (类型: {session.pendingStep.type})</h3>
                <span style={{ color: '#888', fontSize: 12 }}>questionId: {session.pendingStep.questionId}</span>
              </div>
              <h4>{session.pendingStep.stem}</h4>
              <div style={{ marginTop: 16 }}>
                {session.pendingStep.type === 'SINGLE' ? (
                  <div>
                    {session.pendingStep.options.map((opt) => (
                      <label key={opt.optionId} className="radio">
                        <input
                          type="radio"
                          name="answer"
                          value={opt.optionId}
                          checked={selected[0] === opt.optionId}
                          onChange={() => setSelected([opt.optionId])}
                        />
                        {opt.text}
                      </label>
                    ))}
                  </div>
                ) : (
                  <div>
                    {session.pendingStep.options.map((opt) => (
                      <label key={opt.optionId} className="checkbox">
                        <input
                          type="checkbox"
                          value={opt.optionId}
                          checked={selected.includes(opt.optionId)}
                          onChange={(e) => {
                            if (e.target.checked) {
                              setSelected([...selected, opt.optionId])
                            } else {
                              setSelected(selected.filter((id) => id !== opt.optionId))
                            }
                          }}
                        />
                        {opt.text}
                      </label>
                    ))}
                  </div>
                )}
              </div>
              <div style={{ marginTop: 24, display: 'flex', gap: 12 }}>
                <button className="btn btn-primary" onClick={handleSubmit} disabled={loading}>提交答案</button>
                <button className="btn btn-danger" onClick={handleAbandon} disabled={loading}>放弃考试</button>
              </div>
            </div>
          ) : (
            <div className="card">
              <h3>等待中</h3>
              <p>当前没有待作答的题目，请返回控制台查看最新状态。</p>
              <div style={{ marginTop: 12 }}>
                <button className="btn" onClick={fetchSession}>刷新状态</button>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  )
}
