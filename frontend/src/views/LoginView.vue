<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '../composables/auth'
import { useTheme } from '../composables/theme'
import AppIcon from '../components/AppIcon.vue'
const route = useRoute()
const router = useRouter()
const { login } = useAuth()
const { dark, toggle } = useTheme()
const staff = computed(() => !!route.meta.staffLogin)
const account = ref('')
const password = ref('')
const busy = ref(false)
const error = ref('')
async function submit() {
  error.value = ''
  if (!account.value.trim() || !password.value) { error.value = '请填写账号和密码'; return }
  busy.value = true
  try {
    await login(staff.value, account.value.trim(), password.value)
    const redirect = route.query.redirect
    await router.push(typeof redirect === 'string' && redirect.startsWith('/') && !redirect.startsWith('//') ? redirect : staff.value ? '/documents' : '/chat')
  } catch (e) { error.value = (e as Error).message }
  finally { busy.value = false }
}
</script>
<template>
  <div class="login-page">
    <header class="login-header"><div class="brand"><span class="brand-mark"><AppIcon name="books" :size="25" /></span><span>KnowEngine<small>知识引擎</small></span></div><button class="icon-button" :aria-label="dark ? '切换浅色模式' : '切换深色模式'" @click="toggle"><AppIcon :name="dark ? 'sun' : 'moon'" /></button></header>
    <main class="login-main">
      <section class="login-intro"><span class="intro-line"></span><h1>让每一次提问，<br />都有据可循。</h1><p>连接分散的文档与业务知识，<br />在对话中找到清晰、可追溯的答案。</p><div class="login-features"><div><AppIcon name="file" /><span>知识汇集<small>文档、表格与图片</small></span></div><div><AppIcon name="chat" /><span>自然提问<small>基于知识生成回答</small></span></div><div><AppIcon name="book" /><span>查看出处<small>保留原文与引用</small></span></div></div></section>
      <section class="login-form-panel"><div class="login-tabs" aria-label="登录身份"><router-link to="/login" :class="{ selected: !staff }">客户登录</router-link><router-link to="/staff-login" :class="{ selected: staff }">员工登录</router-link></div><h2>{{ staff ? '进入员工工作台' : '欢迎回来' }}</h2><p class="muted">{{ staff ? '管理知识文档，维护可靠的回答。' : '登录后，继续你的知识探索。' }}</p>
        <el-alert v-if="route.query.reason === 'expired'" title="登录已过期，请重新登录" type="info" :closable="false" />
        <el-alert v-if="route.query.reason === 'network'" title="暂时无法连接服务，请检查后端是否启动" type="error" :closable="false" />
        <form class="login-form" @submit.prevent="submit"><div class="field"><label for="account">{{ staff ? '工号' : '手机号' }}</label><el-input id="account" v-model="account" :placeholder="staff ? '输入你的工号' : '输入你的手机号'" size="large" autocomplete="username" :disabled="busy" /></div><div class="field"><label for="password">密码</label><el-input id="password" v-model="password" type="password" placeholder="输入登录密码" size="large" show-password autocomplete="current-password" :disabled="busy" /></div><p v-if="error" class="inline-error" role="alert">{{ error }}</p><el-button native-type="submit" type="primary" size="large" class="login-submit" :disabled="busy">{{ busy ? '正在登录…' : '登录' }}<AppIcon v-if="!busy" name="right" :size="18" /></el-button></form><p class="login-help">{{ staff ? '使用已有员工账号登录。' : '如需开通账号，请联系管理员。' }}</p>
      </section>
    </main><footer class="login-footer"><span>KnowEngine 知识引擎</span><span>Qwen3.8 27B</span></footer>
  </div>
</template>
