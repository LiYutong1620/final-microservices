import { createRouter, createWebHashHistory } from 'vue-router'
import Login from '../views/Login.vue'
import StudentExam from '../views/StudentExam.vue'
import TeacherExam from '../views/TeacherExam.vue'
import Score from '../views/Score.vue'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', component: Login },
  { path: '/student/exam', component: StudentExam },
  { path: '/teacher/exam', component: TeacherExam },
  { path: '/student/score', component: Score }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

// 路由阻断JWT登录拦截
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
