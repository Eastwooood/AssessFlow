import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { login } from '../api/exam'

export default function LoginPage() {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  const [form, setForm] = useState({ username: '', password: '' })

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!form.username || !form.password) {
      alert('请输入用户名和密码')
      return
    }
    setLoading(true)
    try {
      const res = await login(form)
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('userId', String(res.data.userId))
      alert('登录成功')
      navigate('/dashboard')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ height: '100vh', display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
      <div className="card" style={{ width: 360 }}>
        <h2 style={{ textAlign: 'center', marginTop: 0 }}>AssessFlow 验证端</h2>
        <form onSubmit={onSubmit}>
          <div style={{ marginBottom: 12 }}>
            <label style={{ display: 'block', marginBottom: 4, fontSize: 14, color: '#666' }}>用户名</label>
            <input
              className="input"
              placeholder="请输入用户名"
              value={form.username}
              onChange={(e) => setForm({ ...form, username: e.target.value })}
            />
          </div>
          <div style={{ marginBottom: 16 }}>
            <label style={{ display: 'block', marginBottom: 4, fontSize: 14, color: '#666' }}>密码</label>
            <input
              className="input"
              type="password"
              placeholder="请输入密码"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
            />
          </div>
          <button type="submit" className="btn btn-primary" disabled={loading} style={{ width: '100%' }}>
            {loading ? '登录中...' : '登录'}
          </button>
        </form>
      </div>
    </div>
  )
}
