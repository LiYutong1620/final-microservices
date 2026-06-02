<template>
  <div class="exam-layout">
    <el-header class="nav-header">
      <div class="title">在线考试系统 - 学生中心</div>
      <div class="profile">
        <span>你好, {{ studentName }} (学生)</span>
        <el-button size="small" type="danger" @click="logout" style="margin-left: 20px;">退出</el-button>
      </div>
    </el-header>

    <div class="main-content">
      <el-tabs v-model="tabActive" class="tab-pane">
        <!-- 考试中心 -->
        <el-tab-pane label="当前可以参加的考试" name="exam_list">
          <div class="max-config-tip">
            <el-tag type="info">Nacos 考试承载热更新阀值: 同一考试时段参加限流 {{ maxStudents }} 人</el-tag>
          </div>
          <el-table :data="exams" border style="width: 100%; margin-top:15px;">
            <el-table-column prop="title" label="考试名称"></el-table-column>
            <el-table-column label="起止时间">
              <template #default="scope">
                {{ formatDate(scope.row.startTime) }} ~ {{ formatDate(scope.row.endTime) }}
              </template>
            </el-table-column>
            <el-table-column prop="totalScore" label="总分" width="100"></el-table-column>
            <el-table-column label="操作" width="180">
              <template #default="scope">
                <el-button 
                  v-if="scope.row.status === 'RUNNING'" 
                  type="success" 
                  size="small" 
                  @click="enterExam(scope.row)">
                  进入考试
                </el-button>
                <el-tag v-else-if="scope.row.status === 'UPCOMING'" type="info">未开始</el-tag>
                <el-tag v-else type="danger">已结束</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 我的活动记录 -->
        <el-tab-pane label="我的成绩和答题记录" name="history">
          <el-table :data="historyScores" border style="width: 100%">
            <el-table-column prop="examTitle" label="考试项目"></el-table-column>
            <el-table-column prop="finalScore" label="所得成绩" width="120">
              <template #default="scope">
                <span class="score-num">{{ scope.row.finalScore }} 分</span>
              </template>
            </el-table-column>
            <el-table-column label="交卷时间">
              <template #default="scope">
                {{ formatDate(scope.row.submitTime) }}
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 参加考试弹窗 -->
    <el-dialog v-model="examDialogVisible" :title="'答题卡: ' + currentExam.title" width="800px" :close-on-click-modal="false">
      <div class="exam-body" v-if="questions.length > 0">
        <div v-for="(q, idx) in questions" :key="q.id" class="question-item">
          <p class="title">第 {{ idx+1 }} 题 ({{ q.score }}分): {{ q.title }}</p>
          <el-radio-group v-model="answers[q.id]">
            <el-radio label="A">A. {{ q.optionA }}</el-radio>
            <el-radio label="B">B. {{ q.optionB }}</el-radio>
            <el-radio label="C">C. {{ q.optionC }}</el-radio>
            <el-radio label="D">D. {{ q.optionD }}</el-radio>
          </el-radio-group>
        </div>
      </div>
      <div v-else>正在拉取卷数据...</div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="examDialogVisible = false">取消放弃</el-button>
          <el-button type="success" :loading="submitLoading" @click="submitPapers">完成交卷 (调用score-service接口)</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  data() {
    return {
      studentName: localStorage.getItem('username') || '',
      tabActive: 'exam_list',
      exams: [],
      historyScores: [],
      maxStudents: 50,
      
      examDialogVisible: false,
      currentExam: {},
      questions: [],
      answers: {},
      submitLoading: false
    }
  },
  created() {
    this.initConfig()
    this.loadExams()
    this.loadHistory()
  },
  methods: {
    formatDate(d) {
      if (!d) return ''
      return new Date(d).toLocaleString()
    },
    async initConfig() {
      try {
        const res = await axios.get('/api/exam/config/max-students', {
          headers: { 'Authorization': 'Bearer ' + localStorage.getItem('token') }
        })
        this.maxStudents = res.data.maxStudents
      } catch(e) {}
    },
    async loadExams() {
      try {
        const res = await axios.get('/api/exam/list', {
          headers: { 'Authorization': 'Bearer ' + localStorage.getItem('token') }
        })
        this.exams = res.data.exams
      } catch(e) {}
    },
    async loadHistory() {
      try {
        const res = await axios.get('/api/score/history', {
          headers: { 'Authorization': 'Bearer ' + localStorage.getItem('token') }
        })
        this.historyScores = res.data.history
      } catch(e) {}
    },
    async enterExam(exam) {
      this.currentExam = exam
      this.examDialogVisible = true
      this.answers = {}
      
      // 远程根据 IDs 批量查出题单
      try {
        const res = await axios.get('/api/question/batch?ids=' + exam.questionIds, {
          headers: { 'Authorization': 'Bearer ' + localStorage.getItem('token') }
        })
        this.questions = res.data
        // 初始化空答案单
        this.questions.forEach(q => { this.answers[q.id] = '' })
      } catch(err) {
        this.$message.error('无法成功载入试题内容，请稍后再试！')
      }
    },
    async submitPapers() {
      // 检验是否所有题均已解答
      for (let q of this.questions) {
        if (!this.answers[q.id]) {
          this.$message.warning('请回答全部考题后再提交卷包！')
          return
        }
      }

      this.submitLoading = true
      try {
        // 调用成绩微服务 score-service API
        const res = await axios.post('/api/score/submit', {
          examId: this.currentExam.id,
          examTitle: this.currentExam.title,
          answers: this.answers
        }, {
          headers: { 'Authorization': 'Bearer ' + localStorage.getItem('token') }
        })

        if (res.data.code === 200) {
          this.$message.success('答卷自动批改成功！分数为: ' + res.data.score.finalScore + '分')
          this.examDialogVisible = false
          this.loadHistory()
          this.tabActive = 'history'
        } else if (res.data.code === 429) {
          // Sentinel 限流阻断返回
          this.$message.error('无法批改成绩：' + res.data.message)
        } else {
          this.$message.error(res.data.message || '考试评分失败！')
        }
      } catch (err) {
        this.$message.error('服务通信出错，熔断降级已被Sentinel触发兜底! (阅卷服务繁忙)')
      } finally {
        this.submitLoading = false
      }
    },
    logout() {
      localStorage.clear()
      this.$router.push('/login')
    }
  }
}
</script>

<style scoped>
.nav-header {
  background-color: #ffffff;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 40px;
}
.nav-header .title {
  font-size: 18px;
  font-weight: bold;
  color: #1f4068;
}
.main-content {
  padding: 30px 40px;
}
.max-config-tip {
  margin-bottom: 10px;
}
.question-item {
  border-bottom: 1px solid #ebeef5;
  padding: 15px 10px;
}
.question-item .title {
  font-weight: 500;
  margin-bottom: 15px;
}
.score-num {
  font-size: 16px;
  font-weight: bold;
  color: #e6a23c;
}
</style>
