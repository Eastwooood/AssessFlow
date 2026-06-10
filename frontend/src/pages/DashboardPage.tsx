import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { startExam, getSessionSnapshot, gmJumpToStep, gmForceFinish } from '../api/exam'
import type { SessionVO } from '../types'

export default function DashboardPage() {
  const navigate = useNavigate()
  const [paperId, setPaperId] = useState('')
  const [sessionIdInput, setSessionIdInput] = useState('')
  const [session, setSession] = useState<SessionVO | null>(null)
  const [loading, setLoading] = useState(false)

  const handleStart = async () => {
    if (!paperId) return alert('请输入试卷ID')
    setLoading(true)
    try {
      const res = await startExam(Number(paperId))
      setSession(res.data)
      alert('开考成功')
    } finally {
      setLoading(false)
    }
  }

  const handleQuery = async () => {
    if (!sessionIdInput) return alert('请输入会话ID')
    setLoading(true)
    try {
      const res = await getSessionSnapshot(Number(sessionIdInput))
      setSession(res.data)
      alert('查询成功')
    } finally {
      setLoading(false)
    }
  }

  const handleJump = async () => {
    if (!session) return
    const to = window.prompt('请输入要跳转到的步骤索引', String(session.stepCursor))
    if (to === null) return
    setLoading(true)
    try {
      const res = await gmJumpToStep(session.sessionId, Number(to))
      setSession(res.data)
      alert('跳转成功')
    } finally {
      setLoading(false)
    }
  }

  const handleFinish = async () => {
    if (!session) return
    setLoading(true)
    try {
      const res = await gmForceFinish(session.sessionId)
      setSession(res.data)
      alert('强制交卷成功')
    } finally {
      setLoading(false)
    }
  }

  const enterExam = () => {
    if (session) navigate(`/exam/${session.sessionId}`)
  }

  const logout = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    navigate('/login')
  }

  const tagColor = (status: string) => {
    if (status === 'FINISHED') return 'tag-green'
    if (status === 'ABANDONED') return 'tag-red'
    return 'tag-blue'
  }

  return (
    <div style={{ padding: 24, maxWidth: 900, margin: '0 auto' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <h2 style={{ margin: 0 }}>控制台</h2>
        <button className="btn" onClick={logout}>退出登录</button>
      </div>

      <div className="card">
        <h3 style={{ marginTop: 0 }}>操作区</h3>
        <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', alignItems: 'center' }}>
          <input
            className="input"
            placeholder="试卷ID"
            value={paperId}
            onChange={(e) => setPaperId(e.target.value)}
            style={{ width: 180 }}
          />
          <button className="btn btn-primary" onClick={handleStart} disabled={loading}>开始考试</button>
          <input
            className="input"
            placeholder="会话ID"
            value={sessionIdInput}
            onChange={(e) => setSessionIdInput(e.target.value)}
            style={{ width: 180 }}
          />
          <button className="btn" onClick={handleQuery} disabled={loading}>查询会话</button>
        </div>
      </div>

      {session && (
        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
            <h3 style={{ margin: 0 }}>会话信息</h3>
            <div style={{ display: 'flex', gap: 8 }}>
              {session.status === 'IN_PROGRESS' && (
                <button className="btn btn-primary" onClick={enterExam}>进入答题</button>
              )}
              <button className="btn" onClick={handleJump} disabled={loading}>GM跳转</button>
              <button className="btn btn-danger" onClick={handleFinish} disabled={loading}>GM交卷</button>
            </div>
          </div>

          <table>
            <tbody>
              <tr><th>会话ID</th><td>{session.sessionId}</td><th>状态</th><td><span className={`tag ${tagColor(session.status)}`}>{session.status}</span></td></tr>
              <tr><th>试卷ID</th><td>{session.paperId}</td><th>步骤游标</th><td>{session.stepCursor}</td></tr>
              <tr><th>进度</th><td>{session.progress?.current} / {session.progress?.total}</td><th>累计得分</th><td>{session.totalScore ?? 0}</td></tr>
              <tr><th>开始时间</th><td>{session.startedAt}</td><th>更新时间</th><td>{session.updatedAt}</td></tr>
            </tbody>
          </table>

          {session.finishInfo && (
            <div style={{ marginTop: 16 }}>
              <h4>结案信息</h4>
              <table>
                <tbody>
                  <tr><th>最终得分</th><td>{session.finishInfo.finalScore}</td><th>总题数</th><td>{session.finishInfo.totalQuestions}</td></tr>
                  <tr><th>正确数</th><td>{session.finishInfo.correctCount}</td><td></td><td></td></tr>
                </tbody>
              </table>
            </div>
          )}

          {session.immediateResults && session.immediateResults.length > 0 && (
            <div style={{ marginTop: 16 }}>
              <h4>已结算结果</h4>
              <table>
                <thead><tr><th>步骤</th><th>类型</th><th>得分</th></tr></thead>
                <tbody>
                  {session.immediateResults.map((r) => (
                    <tr key={r.stepIndex}>
                      <td>{r.stepIndex}</td>
                      <td>{r.type}</td>
                      <td>{r.gotScore}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
