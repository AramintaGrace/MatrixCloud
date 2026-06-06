import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../store/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: 'chat',
        name: 'Chat',
        component: () => import('../views/Chat.vue')
      },
      {
        path: 'teams',
        name: 'Teams',
        component: () => import('../views/Teams.vue')
      },
      {
        path: 'meetings',
        name: 'Meetings',
        component: () => import('../views/Meetings.vue')
      },
      {
        path: 'calendar',
        name: 'Calendar',
        component: () => import('../views/Calendar.vue')
      },
      {
        path: 'people',
        name: 'People',
        component: () => import('../views/People.vue')
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('../views/Profile.vue')
      }
    ]
  },
  {
    path: '/admin',
    component: () => import('../views/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('../views/AdminDashboard.vue')
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('../views/AdminUsers.vue')
      },
      {
        path: 'teams',
        name: 'AdminTeams',
        component: () => import('../views/AdminTeams.vue')
      },
      {
        path: 'settings',
        name: 'AdminSettings',
        component: () => import('../views/AdminSettings.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  if (to.meta.requiresAuth && !userStore.token) {
    next('/login')
  } else if (to.path === '/login' && userStore.token) {
    // 登录后根据角色跳转
    if (userStore.user?.role === 'ADMIN') {
      next('/admin/dashboard')
    } else {
      next('/chat')
    }
  } else if (to.meta.requiresAdmin && userStore.user?.role !== 'ADMIN') {
    next('/')
  } else {
    next()
  }
})

export default router
