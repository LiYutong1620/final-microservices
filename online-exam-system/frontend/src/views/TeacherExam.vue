<template>
  <div class="teacher-layout">
    <el-header class="nav-header">
      <div class="title">在线考试系统 - 教师教研管理中心</div>
      <div class="profile">
        <span>你好, {{ teacherName }} (教研员)</span>
        <el-button size="small" type="danger" @click="logout" style="margin-left: 20px;">退出系统</el-button>
      </div>
    </el-header>

    <div class="main-content">
      <el-tabs v-model="activeTab" class="tab-pane">
        <!-- 题库管理 -->
        <el-tab-pane label="在线题库维护" name="questions">
          <div class="header-action">
            <el-button type="primary" @click="addQuestionDialogVisible = true">录入新选择题</el-button>
          </div>
          <el-table :data="questions" border style="width: 100%; margin-top: 15px;">
            <th-table-column prop="id" label="ID" width="80"></th-table-column>
            <el-table-column prop="category" label="试题所属科目" width="140"></el-table-column>
            <el-table-column prop="title" label="题目描述干字"></el-table-column>
            <el-table-column label="备选答案">
              <template #default="scope">
                <div class="options-preview">
                  <span>A: {{ scope.row.optionA }}</span> |
                  <span>B: {{ scope.row.optionB }}</span> |
                  <span>C: {{ scope.row.optionC }}</span> |
                  <span>D: {{ scope.row.optionD }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="answer" label="标准参考答案" width="110" align="center">
              <template #default="scope">
                <el-tag type="success">{{ scope.row.answer }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="score" label="参考分值" width="100" align="center">
              <template #default="scope">
                <span style="font-weight: bold; color: #e6a23c;">{{ scope.row.score }} 分</span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 创建并发布新考试 -->
        <el-tab-pane label="发布组卷考试" name="create_exam">
          <el-row :gutter="20">
            <el-col :span="10">
              <el-card class="box-card" header="设置考试基本参数">
                <el-form :model="examForm" label-width="120px">
                  <el-form-item label="考试名称 *">
                    <el-input v-model="examForm.title" placeholder="如：2026年Spring Cloud中考复测"></el-input>
                  </el-form-item>
                  <el-form-item label="开始考试时间">
                    <el-date-picker
                        v-model="examForm.startTime"
                        type="datetime"
                        placeholder="选择开始日期时间"
                        value-format="YYYY-MM-DDTHH:mm:ss"
                        style="width: 100%;"
                    ></el-date-picker>
                  </el-form-item>
                  <el-form-item label="考试截止交卷">
                    <el-date-picker
                        v-model="examForm.endTime"
                        type="datetime"
                        placeholder="选择结束日期时间"
                        value-format="YYYY-MM-DDTHH:mm:ss"
                        style="width: 100%;"
                    ></el-date-picker>
                  </el-form-item>
                  <el-form-item label="已选关联题目">
                    <el-tag v-if="selectedQuestionIds.length === 0" type="danger">请在右侧勾选题库</el-tag>
                    <el-tag v-else type="success">已选中 {{ selectedQuestionIds.length }} 道选择题</el-tag>
                  </el-form-item>
                  <el-form-item label="自动计算总分">
                    <span style="font-size: 18px; font-weight: bold; color: rgb(245, 108, 108);">{{ calculatedTotalScore }} 分</span>
                  </el-form-item>
                  <div style="text-align: right; margin-top: 30px;">
                    <el-button type="warning" @click="submitNewExam" style="width: 100%;">发布考试并绑定题目IDs</el-button>
                  </div>
                </el-form>
              </el-card>
            </el-col>
            <el-col :span="14">
              <el-card class="box-card" header="勾选试题进行快速组卷">
                <el-table
                    ref="qTable"
                    :data="questions"
                    border
                    style="width: 100%"
                    @selection-change="handleSelectionChange"
                >
                  <el-table-column type="selection" width="55"></el-table-column>
                  <el-table-column prop="category" label="分类" width="110"></el-table-column>
                  <el-table-column prop="title" label="题干名称" show-overflow-tooltip></el-table-column>
                  <el-table-column prop="score" label="分值" width="80" align="center"></el-table-column>
                </el-table>
              </el-card>
            </el-col>
          </el-row>
        </el-tab-pane>

        <!-- 答卷及分数看板 -->
        <el-tab-pane label="考生成绩大盘与批阅" name="monitor">
          <div class="filter-bar">
            <span>选择监控考试：</span>
            <el-select v-model="selectedExamId" placeholder="选择发布项" @change="fetchExamGrades">
              <el-option
                  v-for="exam in exams"
                  :key="exam.id"
                  :label="exam.title"
                  :value="exam.id"
              ></el-option>
            </el-select>
          </div>

          <el-table :data="grades" border style="width: 100%; margin-top: 15px;">
            <el-table-column prop="studentId" label="考号（学工号）" width="160"></el-table-column>
            <el-table-column prop="studentName" label="学生姓名" width="140"></el-table-column>
            <el-table-column prop="examTitle" label="参加考试"></el-table-column>
            <el-table-column prop="finalScore" label="卷面总成绩" width="140" align="center">
              <template #default="scope">
                <span class="score-pill">{{ scope.row.finalScore }} 分</span>
              </template>
            </el-table-column>
            <el-table-column label="交卷存档时间" width="220">
              <template #default="scope">
                {{ formatDate(scope.row.submitTime) }}
              </template>
            </el-table-column>
          </el-table>
          <div v-if="grades.length === 0" style="text-align: center; color: #909399; margin-top: 30px;">
            暂无当前考试的学生提交记录
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 录入题目弹窗 -->
    <el-dialog v-model="addQuestionDialogVisible" title="录入单项选择题题库" width="600px">
      <el-form :model="questionForm" label-width="100px">
        <el-form-item label="题目标题 *">
          <el-input type="textarea" :rows="3" v-model="questionForm.title" placeholder="主要针对题意考察的题干描述字句"></el-input>
        </el-form-item>
        <el-form-item label="备选项 A *">
          <el-input v-model="questionForm.optionA"></el-input>
        </el-form-item>
        <el-form-item label="备选项 B *">
          <el-input v-model="questionForm.optionB"></el-input>
        </el-form-item>
        <el-form-item label="备选项 C *">
          <el-input v-model="questionForm.optionC"></el-input>
        </el-form-item>
        <el-form-item label="备选项 D *">
          <el-input v-model="questionForm.optionD"></el-input>
        </el-form-item>
        <el-row :gutter="10">
          <el-col :span="12">
            <el-form-item label="答案 *">
              <el-select v-model="questionForm.answer" style="width: 100%;">
                <el-option label="A 选项" value="A"></el-option>
                <el-option label="B 选项" value="B"></el-option>
                <el-option label="C 选项" value="C"></el-option>
                <el-option label="D 选项" value="D"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分值 *">
              <el-input-number v-model="questionForm.score" :min="1" style="width: 100%;"></el-input-number>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="学科分类 *">
          <el-input v-model="questionForm.category" placeholder="如 Java开发, Redis, 计算机网络"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="addQuestionDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitNewQuestion">添加到数据库题库</el-button>
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
      teacherName: localStorage.getItem('username') || '教师',
      activeTab: 'questions',
      questions: [],
      exams: [],
      grades: [],
      selectedExamId: '',

      addQuestionDialogVisible: false,
      questionForm: {
        title: '',
        optionA: '',
        optionB: '',
        optionC: '',
        optionD: '',
        answer: 'A',
        score: 10,
        category: 'Spring Cloud'
      },

      examForm: {
        title: '',
        startTime: '2026-05-26T10:00:00',
        endTime: '2026-05-26T12:00:00'
      },
      selectedQuestionIds: [],
      calculatedTotalScore: 0
    }
  },
  mounted() {
    this.fetchQuestions()
    this.fetchExams()
  },
  methods: {
    async fetchQuestions() {
      try {
        const res = await axios.get('/api/question/list')
        if (res.data.code === 200) {
          this.questions = res.data.questions
        }
      } catch (err) {
        this.$message.error('无法连接 question-service 服务题库')
      }
    },
    async fetchExams() {
      try {
        const res = await axios.get('/api/exam/list')
        if (res.data.code === 200) {
          this.exams = res.data.exams
        }
      } catch (err) {
        this.$message.error('无法连接 exam-service 服务端')
      }
    },
    async fetchExamGrades() {
      if (!this.selectedExamId) return
      try {
        const res = await axios.get(`/api/score/exam/${this.selectedExamId}`)
        if (res.data.code === 200) {
          this.grades = res.data.scores
        }
      } catch (err) {
        this.$message.error('熔断或无法连接 score-service 管理接口')
      }
    },
    async submitNewQuestion() {
      const q = this.questionForm
      if (!q.title || !q.optionA || !q.optionB || !q.optionC || !q.optionD) {
        this.$message.warning('请确保题设要素完整后提交！')
        return
      }
      try {
        const res = await axios.post('/api/question/add', q)
        if (res.data.code === 200) {
          this.$message.success('选择题添加成功，并已同步数据库记录！')
          this.addQuestionDialogVisible = false
          this.fetchQuestions()
          // 重置
          this.questionForm = {
            title: '',
            optionA: '',
            optionB: '',
            optionC: '',
            optionD: '',
            answer: 'A',
            score: 10,
            category: 'Spring Cloud'
          }
        } else {
          this.$message.error(res.data.message)
        }
      } catch (err) {
        this.$message.error('API添加超时或网关拒绝（X-User-Role拦截权限不符）')
      }
    },
    handleSelectionChange(val) {
      this.selectedQuestionIds = val.map(x => x.id)
      this.calculatedTotalScore = val.reduce((acc, current) => acc + current.score, 0)
    },
    async submitNewExam() {
      if (!this.examForm.title.trim()) {
        this.$message.warning('请设置计划考试科目名称名称！')
        return
      }
      if (this.selectedQuestionIds.length === 0) {
        this.$message.warning('不绑定考题无法组卷，请勾选右侧试题！')
        return
      }
      try {
        const res = await axios.post('/api/exam/create', {
          title: this.examForm.title,
          startTime: this.examForm.startTime,
          endTime: this.examForm.endTime,
          questionIds: this.selectedQuestionIds,
          totalScore: this.calculatedTotalScore
        })
        if (res.data.code === 200) {
          this.$message.success(res.data.message || '组卷试卷发布成功！')
          this.examForm.title = ''
          this.$refs.qTable.clearSelection()
          this.fetchExams()
        } else {
          this.$message.error(res.data.message)
        }
      } catch (err) {
        this.$message.error('考试组卷创建失败：网关未放行或角色无权。')
      }
    },
    logout() {
      localStorage.clear()
      this.$message.success('已安全退出微服务教师系统')
      this.$router.push('/login')
    },
    formatDate(isoString) {
      if (!isoString) return ''
      const d = new Date(isoString)
      return d.toLocaleString()
    }
  }
}
</script>

<style scoped>
.teacher-layout {
  min-height: 100vh;
  background-color: #f5f7fa;
}
.nav-header {
  background-color: #409eff;
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
.header-action {
  text-align: right;
  margin-bottom: 10px;
}
.options-preview span {
  margin-right: 15px;
  color: #606266;
}
.box-card {
  margin-bottom: 20px;
}
.filter-bar {
  background: #fff;
  padding: 15px;
  border-radius: 4px;
  margin-bottom: 20px;
  border: 1px solid #ebeef5;
}
.score-pill {
  font-weight: bold;
  color: #67c23a;
  background: #f0f9eb;
  padding: 4px 10px;
  border-radius: 20px;
  border: 1px solid #c2e7b0;
}
</style>
