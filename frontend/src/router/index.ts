import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { authApi } from '@/api/auth'

const routes: RouteRecordRaw[] = [
  // Public documentation pages
  {
    path: '/',
    component: () => import('@/views/public/DocLayout.vue'),
    children: [
      { path: '', name: 'Home', component: () => import('@/views/public/HomePage.vue') },
      { path: 'doc/:slug', name: 'Article', component: () => import('@/views/public/ArticlePage.vue') },
      { path: 'search', name: 'Search', component: () => import('@/views/public/SearchPage.vue') },
    ],
  },
  // Admin pages
  {
    path: '/admin/login',
    name: 'Login',
    component: () => import('@/views/admin/LoginPage.vue'),
  },
  {
    path: '/admin/setup',
    name: 'Setup',
    component: () => import('@/views/admin/SetupPage.vue'),
  },
  {
    path: '/admin/change-password',
    name: 'ForceChangePassword',
    component: () => import('@/views/admin/ChangePasswordPage.vue'),
  },
  {
    path: '/admin',
    component: () => import('@/views/admin/AdminLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', name: 'Dashboard', component: () => import('@/views/admin/DashboardPage.vue') },
      { path: 'articles', name: 'ArticleList', component: () => import('@/views/admin/ArticleListPage.vue') },
      { path: 'articles/new', name: 'ArticleCreate', component: () => import('@/views/admin/ArticleEditorPage.vue') },
      { path: 'articles/:id/edit', name: 'ArticleEdit', component: () => import('@/views/admin/ArticleEditorPage.vue') },
      { path: 'articles/trash', name: 'ArticleTrash', component: () => import('@/views/admin/ArticleTrashPage.vue') },
      { path: 'categories', name: 'CategoryManager', component: () => import('@/views/admin/CategoryManagerPage.vue') },
      { path: 'visibility', name: 'VisibilityManager', component: () => import('@/views/admin/VisibilityManagerPage.vue') },
      { path: 'media', name: 'MediaManager', component: () => import('@/views/admin/MediaManagerPage.vue') },
      { path: 'users', name: 'UserManager', component: () => import('@/views/admin/UserManagerPage.vue'), meta: { requiresAdmin: true } },
      { path: 'logs', name: 'OperationLogs', component: () => import('@/views/admin/OperationLogsPage.vue') },
      { path: 'profile', name: 'Profile', component: () => import('@/views/admin/ProfilePage.vue') },
    ],
  },
  // 404
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('@/views/NotFound.vue') },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(_to, _from, savedPosition) {
    if (savedPosition) return savedPosition
    return { top: 0 }
  },
})

router.beforeEach(async (to, _from, next) => {
  const authStore = useAuthStore()

  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    return next({ name: 'Login', query: { redirect: to.fullPath } })
  }

  if (to.meta.requiresAuth && authStore.isLoggedIn && !authStore.user) {
    try {
      const { data } = await authApi.me()
      authStore.setUser(data)
    } catch {
      authStore.logout()
      return next({ name: 'Login' })
    }
  }

  if (to.meta.requiresAuth && authStore.user?.forcePasswordChange && to.name !== 'ForceChangePassword') {
    return next({ name: 'ForceChangePassword' })
  }

  if (to.meta.requiresAdmin && authStore.user?.role !== 'ADMIN') {
    return next({ name: 'Dashboard' })
  }

  next()
})

export default router
