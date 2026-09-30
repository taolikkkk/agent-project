<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api, consumeSse, json, query } from '../api/http'
import type { Conversation, Message, Reference } from '../api/types'
import AppIcon from '../components/AppIcon.vue'
import MarkdownText from '../components/MarkdownText.vue'

const conversations = ref<Conversation[]>([])
const messages = ref<Message[]>([])
const current = ref('')
const draft = ref('')
const filter = ref('')
const loading = ref(true)
const streaming = ref(false)
const error = ref('')
const historyOpen = ref(false)
const references = ref<Reference[]>([])
const referencesOpen = ref(false)
const feed = ref<HTMLElement>()
let controller: AbortController | undefined
const filtered = computed(() => conversations.value.filter(c => c.title?.includes(filter.value)))
const title = computed(() => conversations.value.find(c => c.conversationId === current.value)?.title || '新的对话')
const suggestions = ['帮我了解车辆保养的注意事项', '不同车型的配置有什么区别？', '如何查询车辆的售后服务政策？']
async function loadConversations() { conversations.value = await api<Conversation[]>('/chat/list') }
onMounted(async () => {
  try { await loadConversations() } catch (e) { error.value = (e as Error).message }
  finally { loading.value = false }
})
onBeforeUnmount(() => controller?.abort())
async function scrollDown() { await nextTick(); if (feed.value) feed.value.scrollTop = feed.value.scrollHeight }
async function select(c: Conversation) {
  if (streaming.value) return
  error.value = ''; loading.value = true; historyOpen.value = false
  try { const rows = await api<Message[]>(`/chat/messages?${query({ conversationId: c.conversationId })}`); current.value = c.conversationId; messages.value = rows; await scrollDown() }
  catch (e) { error.value = (e as Error).message }
  finally { loading.value = false }
}
function newChat() { if (streaming.value) return; current.value = ''; messages.value = []; draft.value = ''; error.value = ''; historyOpen.value = false }
async function remove(c: Conversation) {
  try {
    await ElMessageBox.confirm('删除后无法恢复该会话。', '删除对话', { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' })
    await api(`/chat/${encodeURIComponent(c.conversationId)}`, { method: 'DELETE' })
    if (current.value === c.conversationId) newChat()
    await loadConversations()
  } catch (e) { if (e instanceof Error) ElMessage.error(e.message) }
}
function showReferences(refs: Reference[]) { references.value = refs; referencesOpen.value = true }
function safeUrl(url?: string) { return url && /^https?:\/\//i.test(url) ? url : undefined }
function isGraphReference(reference: Reference) { return reference.retrievalSource === 'GRAPH_DB' || (!reference.documentTitle && reference.documentId === 'null') }
const graphReferences = computed(() => references.value.filter(isGraphReference))
const documentReferences = computed(() => references.value.filter(reference => !isGraphReference(reference)))
function graphResult(reference: Reference) {
  const value = reference.chunkContent?.trim() || '未返回可展示的图谱数据'
  try { const parsed = JSON.parse(value); return typeof parsed === 'string' ? parsed : value } catch { return value.replace(/^"|"$/g, '') }
}
function referenceButtonLabel(refs: Reference[]) {
  const graphCount = refs.filter(isGraphReference).length
  return graphCount ? `查看 ${graphCount} 项图谱结果${refs.length > graphCount ? `及 ${refs.length - graphCount} 条文档依据` : ''}` : `查看 ${refs.length} 条参考资料`
}
async function feedback(message: Message, helpful: boolean) {
  try { await api(`/chat/message/${message.messageId}/feedback`, json('POST', { helpful })); message.helpful = helpful }
  catch (e) { ElMessage.error((e as Error).message) }
}
async function copy(text: string) {
  try { await navigator.clipboard.writeText(text); ElMessage.success('回答已复制') }
  catch { ElMessage.error('复制失败，请手动选择文本') }
}
async function send() {
  const content = draft.value.trim()
  if (!content || streaming.value) return
  draft.value = ''; error.value = ''; streaming.value = true
  messages.value.push({ type: 'USER', content })
  messages.value.push({ type: 'ASSISTANT', content: '', progress: '正在连接知识引擎…', helpful: null })
  const answer = messages.value[messages.value.length - 1]!
  controller = new AbortController()
  await scrollDown()
  try {
    const response = await fetch('/chat/send', { method: 'POST', credentials: 'same-origin', signal: controller.signal, headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'Accept': 'text/event-stream' }, body: query({ content, conversationId: current.value }) })
    if (!response.ok || !response.body) {
      if (response.status === 401) window.dispatchEvent(new Event('auth-expired'))
      const body = await response.text()
      let reason = body
      try { const parsed = JSON.parse(body); reason = parsed.msg || parsed.message || body } catch { /* 兼容纯文本错误。 */ }
      throw new Error(reason || '发送失败，请稍后重试')
    }
    await consumeSse(response.body, data => {
      const event = /^\[([A-Z_]+)\]:([\s\S]*)$/.exec(data)
      if (event) {
        const kind = event[1], value = event[2]!
        if (kind === 'DONE') current.value = value.trim()
        else if (kind === 'ANSWER_MESSAGE') answer.messageId = value.trim()
        else if (kind === 'PROGRESS') answer.progress = value
        else if (kind === 'REFERENCE') answer.ragReferences = JSON.parse(value)
        else if (kind === 'WARN' || kind === 'ERROR') answer.error = value
        else if (kind === 'CARD') answer.content += value
        else if (kind === 'CARD_CHOICE_CAR' || kind === 'CARD_CHOICE_MYCAR') answer.cards = { kind, choices: JSON.parse(value) }
      } else if (data !== '[DONE]') { answer.content += data; answer.progress = undefined }
      void scrollDown()
    })
    if (!answer.content && !answer.cards && !answer.error) answer.error = '未收到回答，请重新提问。'
  } catch (e) {
    if ((e as Error).name === 'AbortError') answer.progress = '已停止生成'
    else { answer.error = (e as Error).message; answer.progress = undefined }
  } finally {
    streaming.value = false
    if (answer.progress !== '已停止生成') answer.progress = undefined
    try { await loadConversations() } catch { /* 回答保留，下一次操作可重试会话列表。 */ }
    await scrollDown()
  }
}
function choices(message: Message): Record<string, unknown>[] { return (message.cards?.choices as Record<string, unknown>[]) || [] }
function carName(car: Record<string, unknown>) { return String(car.fullName || car.nickname || car.carName || car.modelName || car.model || car.carModel || car.name || car.brand || '选择该车辆') }
function chooseCar(car: Record<string, unknown>) { draft.value = `我选择的车辆是 ${carName(car)}，车辆信息：${JSON.stringify(car)}。请继续回答上一个问题。`; void send() }
function enter(event: KeyboardEvent) { if (!event.shiftKey && !event.isComposing) { event.preventDefault(); void send() } }
</script>

<template>
  <section class="chat-layout">
    <aside class="conversation-panel" :class="{ 'history-open': historyOpen }">
      <div class="conversation-top"><h2>对话记录</h2><button class="icon-button" aria-label="新建对话" :disabled="streaming" @click="newChat"><AppIcon name="plus" :size="19" /></button><button class="icon-button history-close" aria-label="关闭对话记录" @click="historyOpen = false"><AppIcon name="close" /></button></div>
      <el-input v-model="filter" placeholder="搜索对话" aria-label="搜索对话"><template #prefix><AppIcon name="search" :size="16" /></template></el-input>
      <div class="conversation-list"><el-skeleton v-if="loading && !conversations.length" :rows="4" /><p v-else-if="!filtered.length" class="conversation-empty">{{ filter ? '没有匹配的对话' : '新的想法，从一次提问开始。' }}</p><div v-for="c in filtered" :key="c.conversationId" class="conversation-item" :class="{ active: current === c.conversationId }"><button :disabled="streaming" class="conversation-select" @click="select(c)"><AppIcon name="chat" :size="16" /><span>{{ c.title || '未命名对话' }}</span></button><button class="icon-button conversation-delete" aria-label="删除对话" :disabled="streaming" @click="remove(c)"><AppIcon name="trash" :size="14" /></button></div></div>
      <button class="new-chat-bottom" :disabled="streaming" @click="newChat"><AppIcon name="plus" :size="18" />新建对话</button>
    </aside>
    <div class="chat-workspace">
      <header class="chat-header"><button class="icon-button history-trigger" aria-label="查看对话记录" @click="historyOpen = !historyOpen"><AppIcon name="sidebar" /></button><h2>{{ title }}</h2><span>Qwen3.8 27B</span></header>
      <el-alert v-if="error" :title="error" type="error" @close="error = ''" />
      <div ref="feed" class="chat-feed" aria-live="polite" aria-relevant="additions text">
        <div v-if="!messages.length && !loading" class="chat-welcome"><span class="welcome-symbol"><AppIcon name="books" :size="34" /></span><h1>今天，想了解些什么？</h1><p>从你的问题出发，在知识中寻找答案。</p><div class="suggestion-list"><button v-for="suggestion in suggestions" :key="suggestion" @click="draft = suggestion"><AppIcon name="chat" :size="17" /><span>{{ suggestion }}</span><AppIcon name="right" :size="16" /></button></div></div>
        <el-skeleton v-else-if="loading && !messages.length" :rows="5" class="chat-skeleton" />
        <div v-else class="message-list"><article v-for="(message, index) in messages" :key="message.messageId || index" class="message" :class="message.type.toLowerCase()"><div class="message-author"><span class="message-icon"><AppIcon v-if="message.type === 'ASSISTANT'" name="books" :size="18" /><span v-else>你</span></span><strong>{{ message.type === 'ASSISTANT' ? 'KnowEngine' : '你' }}</strong><span v-if="message.cacheHit" class="status-tag good">已审核回答</span></div><div class="message-body"><p v-if="message.type === 'USER'" class="user-content">{{ message.content }}</p><MarkdownText v-else-if="message.content" :text="message.content" /><div v-if="message.progress" class="message-progress"><span class="progress-line" />{{ message.progress }}</div><p v-if="message.error" class="inline-error" role="alert">{{ message.error }}</p><div v-if="message.cards" class="car-choices"><button v-for="(car, carIndex) in choices(message)" :key="carIndex" :disabled="streaming" @click="chooseCar(car)"><AppIcon name="car" /><span>{{ carName(car) }}</span><AppIcon name="right" :size="15" /></button></div><button v-if="message.ragReferences?.length" class="reference-button" @click="showReferences(message.ragReferences)"><AppIcon name="book" :size="16" />{{ referenceButtonLabel(message.ragReferences) }}<AppIcon name="next" :size="14" /></button><div v-if="message.type === 'ASSISTANT' && message.content && (!streaming || index !== messages.length - 1)" class="message-actions"><button class="icon-button" aria-label="复制回答" @click="copy(message.content)"><AppIcon name="copy" :size="16" /></button><template v-if="message.messageId"><button class="icon-button" :class="{ 'is-selected': message.helpful === true }" :disabled="message.helpful != null" aria-label="有帮助" @click="feedback(message, true)"><AppIcon name="like" :size="16" /></button><button class="icon-button" :class="{ 'is-selected': message.helpful === false }" :disabled="message.helpful != null" aria-label="没有帮助" @click="feedback(message, false)"><AppIcon name="dislike" :size="16" /></button></template></div></div></article></div>
      </div>
      <form class="composer-area" @submit.prevent="send"><div class="composer"><label class="composer-label" for="question">你的问题</label><textarea id="question" v-model="draft" rows="2" placeholder="输入问题，开始与知识对话…" :disabled="streaming" @keydown.enter="enter" /><div class="composer-bottom"><span>Enter 发送，Shift + Enter 换行</span><button v-if="streaming" type="button" class="send-button" aria-label="停止生成" @click="controller?.abort()"><AppIcon name="stop" :size="18" /></button><button v-else class="send-button" type="submit" :disabled="!draft.trim() || loading" aria-label="发送问题"><AppIcon name="send" :size="21" /></button></div></div><p class="composer-note">回答由模型生成，重要信息请结合参考资料核实。</p></form>
    </div>
    <el-drawer v-model="referencesOpen" title="参考资料" size="420px">
      <p class="reference-intro">{{ graphReferences.length ? '图谱结果来自 Neo4j 的实体关系查询；知识库文档可打开原文核验。' : '回答所引用的知识片段，可进一步查看原文。' }}</p>
      <div class="reference-list">
        <article v-if="graphReferences.length" class="reference-item graph-reference">
          <div class="reference-heading"><span class="reference-source graph">Neo4j 图数据库</span><h3>图谱关系结果</h3></div>
          <p class="reference-description">查询到的实体或关系如下：</p>
          <ul class="graph-results"><li v-for="(reference, index) in graphReferences" :key="`graph-${index}`">{{ graphResult(reference) }}</li></ul>
        </article>
        <article v-for="(reference, index) in documentReferences" :key="`document-${index}`" class="reference-item">
          <div class="reference-heading"><span class="reference-source">知识库文档</span><h3>{{ reference.documentTitle || '知识资料' }}</h3></div>
          <p>{{ reference.chunkContent }}</p>
          <a v-if="safeUrl(reference.url)" :href="safeUrl(reference.url)" target="_blank" rel="noopener noreferrer">查看原文<AppIcon name="external" :size="14" /></a>
        </article>
      </div>
    </el-drawer>
  </section>
</template>

<style scoped>
.chat-layout { height: calc(100dvh - 66px); display: grid; grid-template-columns: 224px minmax(0,1fr); }
.conversation-panel { border-right: 1px solid var(--line); background: var(--surface); padding: 18px 14px; display: flex; flex-direction: column; min-height: 0; }
.conversation-top { display: flex; align-items: center; margin-bottom: 14px; }
.conversation-top h2 { font-size: 12px; font-weight: 500; margin: 0 auto 0 5px; }
.conversation-list { flex: 1; overflow: auto; margin-top: 22px; }
.conversation-empty { font-size: 11px; line-height: 1.8; color: var(--muted); padding: 10px; }
.conversation-item { display: flex; align-items: center; border-radius: 7px; margin-bottom: 4px; }
.conversation-item:hover { background: var(--hover); }
.conversation-item.active { background: var(--accent-soft); }
.conversation-select { display: flex; align-items: center; gap: 8px; padding: 12px 9px; color: var(--muted); border: 0; background: none; flex: 1; min-width: 0; text-align: left; font-size: 12px; }
.conversation-select svg { flex-shrink: 0; }
.conversation-select span { overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.active .conversation-select { color: var(--accent); }
.conversation-delete { opacity: 0; width: 28px; min-width: 28px; }
.conversation-item:hover .conversation-delete, .conversation-item:focus-within .conversation-delete { opacity: 1; }
.new-chat-bottom { display: flex; gap: 8px; align-items: center; justify-content: center; color: var(--text); border: 1px solid var(--line); border-radius: 8px; padding: 10px; background: var(--surface); margin-top: 16px; font-size: 12px; }
.chat-workspace { display: flex; flex-direction: column; min-width: 0; min-height: 0; }
.chat-header { height: 62px; padding: 0 30px; display: flex; align-items: center; gap: 12px; flex-shrink: 0; }
.chat-header h2 { font-size: 13px; margin: 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; font-weight: 500; }
.chat-header > span { margin-left: auto; color: var(--muted); font-size: 10px; white-space: nowrap; }
.chat-feed { flex: 1; overflow-y: auto; min-height: 0; padding: 10px 34px 30px; }
.chat-welcome { max-width: 560px; margin: 7vh auto 30px; }
.welcome-symbol { width: 58px; height: 58px; border-radius: 14px; background: var(--accent-soft); color: var(--accent); display: grid; place-items: center; margin-bottom: 24px; }
.chat-welcome h1 { font-size: clamp(23px,2.4vw,32px); letter-spacing: -.7px; margin-bottom: 13px; }
.chat-welcome > p { color: var(--muted); font-size: 13px; margin-bottom: 30px; }
.suggestion-list { display: grid; gap: 9px; }
.suggestion-list button { display: flex; align-items: center; gap: 12px; text-align: left; padding: 15px 17px; border: 1px solid var(--line); border-radius: 9px; background: var(--surface); color: var(--muted); font-size: 12px; }
.suggestion-list button:hover { border-color: var(--accent); color: var(--accent); }
.suggestion-list button span { flex: 1; }
.chat-skeleton { max-width: 600px; margin: 40px auto; }
.message-list { max-width: 760px; margin: 0 auto; }
.message { margin: 20px 0 30px; }
.message-author { display: flex; gap: 10px; align-items: center; margin-bottom: 12px; }
.message-author strong { font-size: 12px; font-weight: 500; }
.message-icon { display: grid; place-items: center; height: 28px; width: 28px; border-radius: 7px; background: var(--accent-soft); color: var(--accent); font-size: 11px; }
.user .message-icon { background: var(--hover); color: var(--muted); }
.message-body { padding-left: 38px; }
.user-content { white-space: pre-wrap; line-height: 1.8; margin: 0; }
.message-progress { color: var(--muted); font-size: 12px; padding: 12px 0; display: flex; align-items: center; gap: 8px; }
.progress-line { width: 22px; height: 4px; background: var(--accent); border-radius: 2px; }
.message-actions { display: flex; gap: 3px; margin-top: 12px; }
.is-selected { color: var(--accent); background: var(--accent-soft); opacity: 1; }
.reference-button { margin-top: 17px; border: 1px solid var(--line); background: var(--surface); color: var(--accent); display: inline-flex; gap: 8px; align-items: center; padding: 8px 12px; font-size: 11px; border-radius: 7px; }
.reference-intro { color: var(--muted); font-size: 12px; line-height: 1.7; margin: 0 0 16px; }
.reference-list { display: grid; gap: 12px; }
.reference-item { border: 1px solid var(--line); border-radius: 10px; padding: 15px; background: var(--surface); }
.reference-heading { display: flex; align-items: center; gap: 9px; margin-bottom: 11px; }
.reference-heading h3 { font-size: 14px; margin: 0; }
.reference-source { color: var(--muted); background: var(--hover); border-radius: 999px; padding: 3px 7px; font-size: 10px; white-space: nowrap; }
.reference-source.graph { background: #e5f1eb; color: var(--accent); }
.reference-item > p { color: var(--muted); font-size: 12px; white-space: pre-wrap; line-height: 1.7; margin: 0; }
.reference-item a { color: var(--accent); display: inline-flex; align-items: center; gap: 5px; font-size: 12px; margin-top: 13px; }
.graph-reference { background: #f7fbf8; border-color: #cfe3d7; }
.reference-description { margin-bottom: 8px !important; }
.graph-results { display: grid; gap: 8px; list-style: none; margin: 0; padding: 0; }
.graph-results li { background: var(--surface); border: 1px solid #d9e9df; border-radius: 7px; color: var(--text); font-size: 12px; line-height: 1.6; padding: 9px 10px; }
.composer-area { padding: 10px 34px 16px; flex-shrink: 0; max-width: 860px; width: 100%; margin: 0 auto; }
.composer { border: 1px solid var(--line); border-radius: 12px; background: var(--surface); padding: 15px 18px 12px; box-shadow: 0 4px 18px #2b513809; }
.composer:focus-within { border-color: var(--accent); }
.composer-label { display: block; font-size: 10px; color: var(--muted); margin-bottom: 8px; }
.composer textarea { width: 100%; border: 0; background: transparent; color: var(--text); resize: vertical; max-height: 160px; min-height: 48px; line-height: 1.8; outline: none; font-size: 13px; padding: 0; }
.composer textarea::placeholder { color: var(--muted); }
.composer-bottom { display: flex; justify-content: space-between; align-items: center; margin-top: 7px; }
.composer-bottom > span { font-size: 10px; color: var(--muted); }
.send-button { height: 34px; width: 34px; border: 0; border-radius: 8px; color: var(--surface); background: var(--accent); display: grid; place-items: center; }
.send-button:disabled { opacity: .45; }
.composer-note { text-align: center; color: var(--muted); font-size: 10px; margin: 10px 0 0; }
.history-trigger,.history-close { display: none; }
.car-choices { display: grid; gap: 8px; margin-top: 12px; }
.car-choices button { border: 1px solid var(--line); border-radius: 8px; padding: 12px; display: flex; align-items: center; gap: 10px; color: var(--text); background: var(--surface); }
.car-choices span { flex: 1; text-align: left; }
@media (max-width: 1100px) { .chat-layout { grid-template-columns: 185px minmax(0,1fr); } .chat-feed { padding: 10px 24px 25px; } .composer-area { padding: 10px 22px 16px; } .chat-header { padding: 0 22px; } .chat-welcome { margin-top: 5vh; } }
@media (max-width: 767px) { .chat-layout { display: block; height: calc(100dvh - 58px); position: relative; } .conversation-panel { display: none; } .conversation-panel.history-open { display: flex; position: absolute; inset: 0 18% 0 0; z-index: 10; box-shadow: 14px 0 30px #15271e15; } .history-trigger,.history-close { display: inline-flex; } .chat-workspace { height: 100%; } .chat-header { height: 52px; padding: 0 12px; } .chat-feed { padding: 6px 20px 24px; } .chat-welcome { margin-top: 28px; } .chat-welcome h1 { font-size: 24px; } .chat-welcome p { font-size: 12px; } .composer-area { padding: 8px 14px 12px; } .composer-bottom > span { font-size: 9px; } .message-body { padding-left: 0; } .message { margin: 14px 0 28px; } .conversation-delete { opacity: 1; } .suggestion-list button { font-size: 11px; padding: 12px; } }
</style>
