import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue')
  },
  {
    path: '/',
    component: () => import('@/layout/AppLayout.vue'),
    meta: { requiresAuth: true },
    redirect: '/dashboard',
    children: [
      {
        path: 'submit',
        name: 'SubmitIndex',
        component: () => import('@/views/submit/Index.vue')
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/Dashboard.vue')
      },
      {
        path: 'achievement/public',
        name: 'PublicAchievementList',
        component: () => import('@/views/achievement/PublicList.vue')
      },
      {
        path: 'competition/list',
        name: 'CompetitionList',
        component: () => import('@/views/competition/List.vue')
      },
      {
        path: 'competition/submit',
        name: 'CompetitionSubmit',
        component: () => import('@/views/competition/Submit.vue')
      },
      {
        path: 'competition/detail/:id',
        name: 'CompetitionDetail',
        component: () => import('@/views/competition/Detail.vue')
      },
      {
        path: 'competition/edit/:id',
        name: 'CompetitionEdit',
        component: () => import('@/views/competition/Submit.vue')
      },
      {
        path: 'review/list',
        name: 'ReviewList',
        component: () => import('@/views/review/List.vue')
      },
      {
        path: 'review/detail/:id',
        name: 'ReviewDetail',
        component: () => import('@/views/review/Detail.vue')
      },
      {
        path: 'audit/list',
        name: 'AuditList',
        component: () => import('@/views/audit/AuditList.vue')
      },
      {
        path: 'audit/secretary',
        redirect: '/audit/list'
      },
      {
        path: 'audit/leader',
        redirect: '/audit/list'
      },
      {
        path: 'notifications',
        name: 'Notification',
        component: () => import('@/views/notification/Notification.vue')
      },
      {
        path: 'project/submit',
        name: 'ProjectSubmit',
        component: () => import('@/views/project/Submit.vue')
      },
      {
        path: 'project/list',
        name: 'ProjectList',
        component: () => import('@/views/project/List.vue')
      },
      {
        path: 'project/detail/:id',
        name: 'ProjectDetail',
        component: () => import('@/views/project/Detail.vue')
      },
      {
        path: 'project/edit/:id',
        name: 'ProjectEdit',
        component: () => import('@/views/project/Submit.vue')
      },
      {
        path: 'paper/submit',
        name: 'PaperSubmit',
        component: () => import('@/views/paper/Submit.vue')
      },
      {
        path: 'paper/list',
        name: 'PaperList',
        component: () => import('@/views/paper/List.vue')
      },
      {
        path: 'paper/detail/:id',
        name: 'PaperDetail',
        component: () => import('@/views/paper/Detail.vue')
      },
      {
        path: 'paper/edit/:id',
        name: 'PaperEdit',
        component: () => import('@/views/paper/Submit.vue')
      },
      {
        path: 'software/submit',
        name: 'SoftwareSubmit',
        component: () => import('@/views/software/Submit.vue')
      },
      {
        path: 'software/list',
        name: 'SoftwareList',
        component: () => import('@/views/software/List.vue')
      },
      {
        path: 'software/detail/:id',
        name: 'SoftwareDetail',
        component: () => import('@/views/software/Detail.vue')
      },
      {
        path: 'software/edit/:id',
        name: 'SoftwareEdit',
        component: () => import('@/views/software/Submit.vue')
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/user/Profile.vue')
      },
      {
        path: 'admin/users',
        name: 'UserManagement',
        component: () => import('@/views/admin/UserManagement.vue')
      },
      {
        path: 'admin/logs',
        name: 'OperationLogs',
        component: () => import('@/views/admin/OperationLogs.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  if (to.matched.length === 0) {
    next('/login')
    return
  }
  // 检查需要认证的路由
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth)
  const token = localStorage.getItem('token')
  if (requiresAuth && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
