import { createRouter, createWebHistory } from 'vue-router'
import { useAuth } from './composables/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/chat' },
    { path: '/login', component: () => import('./views/LoginView.vue'), meta: { public: true } },
    { path: '/staff-login', component: () => import('./views/LoginView.vue'), meta: { public: true, staffLogin: true } },
    { path: '/chat', component: () => import('./views/ChatView.vue'), meta: { title: 'AI 对话助手' } },
    { path: '/documents/upload', component: () => import('./views/UploadView.vue'), meta: { title: '知识文档上传', staff: true } },
    { path: '/documents/:documentType?/:materialCategory?', component: () => import('./views/DocumentsView.vue'), meta: { title: '文档管理', staff: true } },
    { path: '/qa-cache', component: () => import('./views/CacheView.vue'), meta: { title: '问答缓存审核', staff: true } },
    { path: '/login.html', redirect: '/login' },
    { path: '/staff-login.html', redirect: '/staff-login' },
    { path: '/chat.html', redirect: '/chat' },
    { path: '/document.html', redirect: '/documents' },
    { path: '/upload.html', redirect: '/documents/upload' },
    { path: '/qa-cache.html', redirect: '/qa-cache' },
    { path: '/:pathMatch(.*)*', redirect: '/chat' },
  ],
})
router.beforeEach(async to => {
  document.title = `${to.meta.title || '登录'} · KnowEngine`
  if (to.meta.public) return
  const auth = useAuth()
  try { await auth.restore() } catch { return { path: '/login', query: { reason: 'network' } } }
  if (!auth.user.value) return { path: to.meta.staff ? '/staff-login' : '/login', query: { redirect: to.fullPath } }
  if (to.meta.staff && auth.user.value.userType !== 'staff') return '/chat'
})
window.addEventListener('auth-expired', () => {
  const auth = useAuth()
  auth.clear()
  if (!router.currentRoute.value.meta.public) void router.replace({ path: '/login', query: { reason: 'expired' } })
})
export default router
