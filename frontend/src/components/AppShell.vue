<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuth } from '../composables/auth'
import { useTheme } from '../composables/theme'
import AppIcon from './AppIcon.vue'
const { user, logout } = useAuth()
const { dark, toggle } = useTheme()
const route = useRoute()
const router = useRouter()
const open = ref(false)
watch(() => route.fullPath, () => { open.value = false })
async function exit() {
  try { await logout(); await router.push('/login') }
  catch (e) { ElMessage.error((e as Error).message) }
}
</script>
<template>
  <div class="app-shell">
    <button v-if="open" class="nav-scrim" aria-label="关闭导航" @click="open = false" />
    <aside class="app-sidebar" :class="{ 'is-open': open }">
      <router-link class="brand" to="/chat"><span class="brand-mark"><AppIcon name="books" :size="24" /></span><span>KnowEngine<small>知识引擎</small></span></router-link>
      <div class="workspace-label">工作空间</div>
      <nav class="main-nav" aria-label="主导航">
        <router-link to="/chat"><AppIcon name="chat" />AI 对话助手</router-link>
        <template v-if="user?.userType === 'staff'">
          <router-link to="/documents"><AppIcon name="folder" />文档管理</router-link>
          <router-link to="/documents/upload"><AppIcon name="upload" />知识文档上传</router-link>
          <router-link to="/qa-cache"><AppIcon name="review" />问答缓存审核</router-link>
        </template>
      </nav>
      <div class="sidebar-bottom">
        <div class="model-note"><AppIcon name="book" :size="17" /><div>Qwen3.8 27B<small>文本与图片理解</small></div></div>
        <div class="user-row"><span class="avatar">{{ (user?.name || user?.nickname || '用').slice(0, 1) }}</span><div class="user-name">{{ user?.name || user?.nickname || '当前用户' }}<small>{{ user?.userType === 'staff' ? '员工工作台' : '客户空间' }}</small></div><button class="icon-button" :aria-label="dark ? '切换浅色模式' : '切换深色模式'" @click="toggle"><AppIcon :name="dark ? 'sun' : 'moon'" :size="18" /></button></div>
        <button class="logout-button" @click="exit"><AppIcon name="logout" :size="16" />退出登录</button>
      </div>
    </aside>
    <main class="app-main">
      <header class="topbar"><button class="icon-button mobile-menu" aria-label="打开导航" @click="open = !open"><AppIcon name="menu" /></button><div class="breadcrumb">工作空间<AppIcon name="next" :size="13" /><strong>{{ route.meta.title }}</strong></div><span class="topbar-note">连接知识，找到答案</span></header>
      <slot />
    </main>
  </div>
</template>
