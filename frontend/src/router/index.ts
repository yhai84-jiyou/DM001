import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('../views/login/LoginPage.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('../views/register/RegisterPage.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/',
      component: () => import('../components/layout/AppLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        { path: '', redirect: '/dashboard' },
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('../views/dashboard/Overview.vue'),
        },
        {
          path: 'issues',
          name: 'IssueList',
          component: () => import('../views/issue/IssueList.vue'),
        },
        {
          path: 'issues/new',
          name: 'IssueCreate',
          component: () => import('../views/issue/IssueForm.vue'),
        },
        {
          path: 'issues/:id',
          name: 'IssueDetail',
          component: () => import('../views/issue/IssueDetail.vue'),
        },
        {
          path: 'issues/:id/edit',
          name: 'IssueEdit',
          component: () => import('../views/issue/IssueForm.vue'),
        },
        {
          path: 'reports',
          name: 'Reports',
          component: () => import('../views/report/DailyReport.vue'),
        },
        {
          path: 'admin/users',
          name: 'UserManage',
          component: () => import('../views/admin/UserManage.vue'),
        },
        {
          path: 'admin/org',
          name: 'OrgManage',
          component: () => import('../views/admin/OrgManage.vue'),
        },
        {
          path: 'admin/dict',
          name: 'DictManage',
          component: () => import('../views/admin/DictManage.vue'),
        },
        {
          path: 'admin/roles',
          name: 'RoleManage',
          component: () => import('../views/admin/RoleManage.vue'),
        },
      ],
    },
  ],
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('access_token')
  if (to.meta.requiresAuth !== false && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router
