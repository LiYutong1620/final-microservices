<template>
  <div class="score-layout">
    <el-header class="nav-header">
      <div class="title">在线考试系统 - 学生成绩记录</div>
      <div class="profile">
        <span>你好, {{ studentName }} (学生)</span>
        <el-button size="small" type="primary" @click="$router.push('/student/exam')">返回考试中心</el-button>
        <el-button size="small" type="danger" @click="logout" style="margin-left: 15px;">退出</el-button>
      </div>
    </el-header>

    <div class="main-content">
      <el-card class="score-card">
        <template #header>
          <div class="card-header">
            <span style="font-weight: bold; font-size: 16px;">📚 既往考核所得成绩情况汇总表</span>
          </div>
        </template>

        <el-table :data="historyScores" border style="width: 100%">
          <el-table-column prop="examTitle" label="评估考试科目"></el-table-column>
          <el-table-column prop="finalScore" label="所得真实得分" width="160" align="center">
            <template #default="scope">
              <span class="score-num">{{ scope.row.finalScore }} 分</span>
            </template>
          </el-table-column>
          <el-table-column label="交卷校验归档时间" width="240">
            <template #default="scope">
              {{ formatDate(scope.row.submitTime) }}
            </template>
          </el-table-column>
        </el-table>

        <div v-if="historyScores.length === 0" class="empty-tip">
          您当前在系统内没有任何答卷记录。请返回考试中心并在生效时段内参加单次考试！
        </div>
      </el-card>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  data() {
    return {
      studentName: localStorage.getItem('username') || '同学',
      historyScores: []
    }
  },
  mounted() {
    this.fetchScores()
  },
  methods: {
    async fetchScores() {
      try {
        const res = await axios.get('/api/score/history')
        if (res.data.code === 200) {
          this.historyScores = res.data.history
        }
      } catch (err) {
        this.$message.error('无法连接 score-service 查询答题卷历史，请检查服务心跳')
      }
    },
    logout() {
      localStorage.clear()
      this.$message.success('退出成功')
      this.$router.push('/login')
    },
    formatDate(isoString) {
      if (!isoString) return ''
      return new Date(isoString).toLocaleString()
    }
  }
}
</script>

<style scoped>
.score-layout {
  min-height: 100vh;
  background-color: #f5f7fa;
}
.nav-header {
  background-color: #67c23a;
  color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 40px;
}
.nav-header .title {
  font-size: 18px;
  font-weight: bold;
}
.main-content {
  padding: 30px 40px;
}
.score-card {
  max-width: 1000px;
  margin: 0 auto;
}
.score-num {
  font-size: 16px;
  font-weight: bold;
  color: #e6a23c;
}
.empty-tip {
  text-align: center;
  color: #909399;
  margin-top: 40px;
  margin-bottom: 20px;
  font-size: 14px;
}
</style>
