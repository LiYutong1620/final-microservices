<template>
  <div class="login-container">
    <el-card class="login-card">
      <h2>微服务在线考试系统 - 认证登录</h2>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="学生登录" name="student"></el-tab-pane>
        <el-tab-pane label="教师登录" name="teacher"></el-tab-pane>
      </el-tabs>
      
      <el-form :model="form" label-width="80px" class="form">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入学号/工号"></el-input>
        </el-form-item>
        <el-form-item label="密  码">
          <el-input v-model="form.password" type="password" placeholder="推荐默认123456" show-password></el-input>
        </el-form-item>
        
        <div style="text-align: center; margin-top: 20px;">
          <el-button type="primary" @click="handleLogin" style="width: 100%;">登陆认证</el-button>
        </div>
        
        <div class="tip">
          <span @click="handleRegister">没有账户？点击注册一个</span>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  data() {
    return {
      activeTab: 'student',
      form: { username: '', password: '' }
    }
  },
  methods: {
    async handleLogin() {
      try {
        const res = await axios.post('/api/auth/login', {
          username: this.form.username,
          password: this.form.password,
          role: this.activeTab.toUpperCase()
        })
        if (res.data.code === 200) {
          localStorage.setItem('token', res.data.token)
          localStorage.setItem('role', res.data.user.role)
          localStorage.setItem('username', res.data.user.nickname)
          this.$message.success('登录成功！')
          
          if (res.data.user.role === 'TEACHER') {
            this.$router.push('/teacher/exam')
          } else {
            this.$router.push('/student/exam')
          }
        } else {
          this.$message.error(res.data.message)
        }
      } catch (err) {
        this.$message.error('连接网关服务错误')
      }
    },
    async handleRegister() {
      try {
        const res = await axios.post('/api/auth/register', {
          username: this.form.username,
          password: this.form.password,
          role: this.activeTab.toUpperCase(),
          nickname: this.form.username + ' ' + (this.activeTab === 'teacher' ? '教师' : '同学')
        })
        if (res.data.code === 200) {
          this.$message.success('注册成功，请登录！')
        } else {
          this.$message.error(res.data.message)
        }
      } catch (err) {
        this.$message.error('网络出现异常')
      }
    }
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-linear: linear-gradient(135deg, #1f4068, #162447);
}
.login-card {
  width: 420px;
  border-radius: 8px;
}
h2 {
  text-align: center;
  color: #303133;
}
.tip {
  text-align: center;
  margin-top: 15px;
  color: #409eff;
  cursor: pointer;
  font-size: 13px;
}
</style>
