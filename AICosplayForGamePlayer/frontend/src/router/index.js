import { createRouter, createWebHistory } from 'vue-router'
import LoginPage from '../views/LoginPage.vue'
import RegisterPage from '../views/RegisterPage.vue'
import ChatPage from '../views/NewChatPage.vue'
import AboutPage from '../views/AboutPage.vue'
import UserProfilePage from '../views/UserProfilePage.vue'
import RoleSelectionPage from '../views/RoleSelectionPage.vue'

const routes = [
  {
    path: '/',
    name: 'login',
    component: LoginPage
  },
  {
    path: '/register',
    name: 'register',
    component: RegisterPage
  },
  {
    path: '/forgot-password',
    name: 'forgot-password',
    component: () => import('../views/ForgotPasswordPage.vue')
  },
  {
    path: '/chat',
    name: 'chat',
    component: ChatPage,
    meta: {
      requiresAuth: true
    }
  },
  {
    path: '/about',
    name: 'about',
    component: AboutPage
  },
  {
    path: '/profile',
    name: 'profile',
    component: UserProfilePage,
    meta: {
      requiresAuth: true
    }
  },
  {
    path: '/role-selection',
    name: 'roleSelection',
    component: RoleSelectionPage,
    meta: {
      requiresAuth: true
    }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫，检查是否需要登录
router.beforeEach((to, from, next) => {
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth)
  const isLoggedIn = localStorage.getItem('isLoggedIn') === 'true'

  if (requiresAuth && !isLoggedIn) {
    next('/')
  } else {
    next()
  }
})

export default router