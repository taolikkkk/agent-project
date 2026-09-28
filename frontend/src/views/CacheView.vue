<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api, json, query } from '../api/http'
import type { CacheEntry } from '../api/types'
import AppIcon from '../components/AppIcon.vue'
import StatusTag from '../components/StatusTag.vue'
import MarkdownText from '../components/MarkdownText.vue'
const rows = ref<CacheEntry[]>([])
const status = ref('PENDING'), page = ref(1), error = ref('')
const loading = ref(true), busy = ref(false), detailOpen = ref(false), batchOpen = ref(false)
const selected = ref<string[]>([])
const current = ref<CacheEntry>()
const review = reactive({ question: '', answer: '', note: '', validDays: 30, publicConfirmed: false })
const batch = reactive({ kind: 'approve', note: '', validDays: 30, publicConfirmed: false })
const statuses = [{ value: 'PENDING', label: '待审核' }, { value: 'APPROVED', label: '待发布' }, { value: 'ACTIVE', label: '已发布' }, { value: 'REJECTED', label: '已拒绝' }, { value: 'DISABLED', label: '已下线' }]
const allSelected = computed({ get: () => rows.value.length > 0 && selected.value.length === rows.value.length, set: checked => { selected.value = checked ? rows.value.map(e => e.id) : [] } })
const evidence = computed(() => { try { const data = JSON.parse(String(current.value?.sources || '[]')); return Array.isArray(data) ? data : [data] } catch { return [] } })
async function load(reset = false) {
  if (reset) page.value = 1
  loading.value = true; error.value = ''; selected.value = []
  try { rows.value = await api<CacheEntry[]>(`/api/qa-cache?${query({ status: status.value, page: page.value })}`) }
  catch (e) { error.value = (e as Error).message; rows.value = [] }
  finally { loading.value = false }
}
onMounted(() => load())
watch(status, () => load(true))
watch(page, () => load())
function open(entry: CacheEntry) {
  current.value = entry
  Object.assign(review, { question: entry.question, answer: entry.answer, note: '', validDays: 30, publicConfirmed: false })
  detailOpen.value = true
}
async function act(kind: string) {
  if (!current.value) return
  if (kind === 'approve' && (!review.question.trim() || !review.answer.trim() || !review.publicConfirmed)) { ElMessage.error('请完善问答并确认答案可以公开复用'); return }
  busy.value = true
  try {
    await api(`/api/qa-cache/${current.value.id}/${kind}`, json('POST', kind === 'publish' ? undefined : review))
    detailOpen.value = false; ElMessage.success('操作已完成'); await load()
  } catch (e) { ElMessage.error((e as Error).message) }
  finally { busy.value = false }
}
function openBatch(kind: string) { Object.assign(batch, { kind, note: '', validDays: 30, publicConfirmed: false }); batchOpen.value = true }
async function batchAct() {
  if (batch.kind === 'approve' && !batch.publicConfirmed) return
  busy.value = true
  let success = 0
  const failures: string[] = []
  for (const id of selected.value) {
    const entry = rows.value.find(row => row.id === id)!
    try { await api(`/api/qa-cache/${id}/${batch.kind}`, json('POST', { ...batch, question: entry.question, answer: entry.answer })); success++ }
    catch (e) { failures.push((e as Error).message) }
  }
  busy.value = false; batchOpen.value = false
  await load()
  if (failures.length) ElMessage.warning(`完成 ${success} 项，${failures.length} 项未完成：${failures[0]}`)
  else ElMessage.success(`已处理 ${success} 项`)
}
async function task(kind: 'curate' | 'retire') {
  try {
    await ElMessageBox.confirm(kind === 'curate' ? '根据最近七天的有效问答整理待审核条目，可能需要等待片刻。' : '检查缓存有效性并清理应下线的条目。', kind === 'curate' ? '整理高频问答' : '检查缓存有效性', { confirmButtonText: '开始执行', cancelButtonText: '取消' })
    busy.value = true
    const count = await api<number>(`/api/qa-cache/${kind}`)
    ElMessage.success(kind === 'curate' ? `整理完成，新增 ${count} 条待审核问答` : '有效性检查完成')
    await load()
  } catch (e) { if (e instanceof Error) ElMessage.error(e.message) }
  finally { busy.value = false }
}
function date(value: unknown) { return value ? String(value).replace('T', ' ').slice(0,16) : '暂无记录' }
</script>
<template>
  <section class="page cache-page"><div class="page-heading"><div><h1>问答缓存审核</h1><p class="muted">把经过核实的回答，沉淀为可以复用的知识。</p></div><el-dropdown trigger="click" @command="task"><el-button :disabled="busy"><AppIcon name="refresh" :size="17" />维护缓存</el-button><template #dropdown><el-dropdown-menu><el-dropdown-item command="curate">整理高频问答</el-dropdown-item><el-dropdown-item command="retire">检查缓存有效性</el-dropdown-item></el-dropdown-menu></template></el-dropdown></div>
    <el-tabs v-model="status" class="cache-tabs"><el-tab-pane v-for="item in statuses" :key="item.value" :label="item.label" :name="item.value" /></el-tabs>
    <el-alert v-if="error" :title="error" type="error" :closable="false"><el-button size="small" @click="load()">重新加载</el-button></el-alert>
    <div v-if="loading" class="panel panel-content"><el-skeleton :rows="7" /></div><div v-else-if="!rows.length && !error" class="empty-state panel"><span class="empty-icon"><AppIcon name="review" :size="29" /></span><h3>暂时没有{{ statuses.find(s => s.value === status)?.label }}的问答</h3><p>有效问答整理后会出现在待审核列表，审核通过即可发布使用。</p></div>
    <template v-else-if="rows.length"><div class="cache-selection"><el-checkbox v-model="allSelected" :indeterminate="selected.length > 0 && !allSelected">全选当前页</el-checkbox><span class="muted">{{ selected.length ? `已选择 ${selected.length} 项` : `本页 ${rows.length} 项` }}</span><div class="toolbar-end"><template v-if="selected.length"><el-button v-if="status === 'PENDING'" size="small" @click="openBatch('reject')">批量拒绝</el-button><el-button v-if="status === 'PENDING'" type="primary" size="small" @click="openBatch('approve')">批量通过</el-button><el-button v-if="status === 'ACTIVE' || status === 'APPROVED'" size="small" @click="openBatch('disable')">批量下线</el-button></template><button class="icon-button" aria-label="刷新审核列表" @click="load()"><AppIcon name="refresh" :size="18" /></button></div></div><div class="cache-list"><article v-for="entry in rows" :key="entry.id" class="cache-entry"><el-checkbox v-model="selected" :value="entry.id" :aria-label="`选择 ${entry.question}`" /><button class="cache-entry-main" @click="open(entry)"><div class="cache-entry-heading"><h3>{{ entry.question }}</h3><StatusTag :status="entry.status" /></div><p>{{ entry.answer }}</p><div class="cache-metadata"><span>{{ entry.frequency }} 个独立会话</span><span>命中 {{ entry.hits || 0 }} 次</span><span>创建于 {{ date(entry.createdAt) }}</span></div></button><button class="icon-button" aria-label="查看审核详情" @click="open(entry)"><AppIcon name="next" :size="18" /></button></article></div><div class="panel-footer"><span>第 {{ page }} 页</span><div><el-button :disabled="page <= 1 || loading" @click="page--">上一页</el-button><el-button :disabled="rows.length < 20 || loading" @click="page++">下一页</el-button></div></div></template>
    <el-drawer v-model="detailOpen" title="审核问答" size="700px" :close-on-click-modal="!busy"><template v-if="current"><div class="detail-meta"><StatusTag :status="current.status" /><span>{{ current.frequency }} 个独立会话</span><span>命中 {{ current.hits || 0 }} 次</span><span>有效期至 {{ date(current.expiresAt) }}</span></div><el-alert v-if="current.reason" :title="String(current.reason)" type="info" :closable="false" /><el-alert v-if="current.disableDetail || current.indexError" :title="String(current.disableDetail || current.indexError)" type="warning" :closable="false" /><div class="field"><label for="review-question">标准问题</label><el-input id="review-question" v-model="review.question" type="textarea" :rows="2" :readonly="current.status !== 'PENDING'" maxlength="500" /></div><div class="field"><label for="review-answer">标准回答</label><el-input v-if="current.status === 'PENDING'" id="review-answer" v-model="review.answer" type="textarea" :rows="8" maxlength="16000" /><MarkdownText v-else :text="review.answer" /></div><details v-if="evidence.length" class="evidence"><summary>查看 {{ evidence.length }} 条来源证据</summary><article v-for="(source, index) in evidence" :key="index"><h4>{{ source.question || source.userQuestion || `来源 ${index + 1}` }}</h4><MarkdownText :text="source.answer || source.content || JSON.stringify(source, null, 2)" /></article></details><template v-if="['PENDING','DISABLED','ACTIVE','APPROVED'].includes(current.status)"><div class="field"><label for="review-note">审核意见</label><el-input id="review-note" v-model="review.note" type="textarea" :rows="2" maxlength="2000" placeholder="填写核实结论或操作原因" /></div><div v-if="current.status === 'PENDING' || current.status === 'DISABLED'" class="field"><label>有效期（天）</label><el-input-number v-model="review.validDays" :min="1" :max="90" aria-label="有效天数" /></div><el-checkbox v-if="current.status === 'PENDING'" v-model="review.publicConfirmed">我已核实来源，确认此回答适合公开复用</el-checkbox></template></template><template #footer><div class="review-actions"><el-button :disabled="busy" @click="detailOpen = false">关闭</el-button><template v-if="current?.status === 'PENDING'"><el-button :disabled="busy" @click="act('reject')">拒绝</el-button><el-button type="primary" :disabled="busy || !review.publicConfirmed" @click="act('approve')">{{ busy ? '正在处理…' : '通过并发布' }}</el-button></template><template v-else-if="current?.status === 'APPROVED'"><el-button :disabled="busy" @click="act('disable')">下线</el-button><el-button type="primary" :disabled="busy" @click="act('publish')">发布</el-button></template><el-button v-else-if="current?.status === 'ACTIVE'" :disabled="busy" @click="act('disable')">下线</el-button><el-button v-else-if="current?.status === 'DISABLED'" type="primary" :disabled="busy" @click="act('reactivate')">重新激活</el-button></div></template></el-drawer>
    <el-dialog v-model="batchOpen" title="批量处理问答" width="520px" :close-on-click-modal="!busy"><p class="muted">将处理选中的 {{ selected.length }} 条问答。</p><div class="field"><label for="batch-note">审核意见</label><el-input id="batch-note" v-model="batch.note" type="textarea" :rows="3" /></div><template v-if="batch.kind === 'approve'"><div class="field"><label>有效期（天）</label><el-input-number v-model="batch.validDays" :min="1" :max="90" aria-label="批量有效天数" /></div><el-checkbox v-model="batch.publicConfirmed">我已逐条核实所选问答，确认均适合公开复用</el-checkbox></template><template #footer><el-button :disabled="busy" @click="batchOpen = false">取消</el-button><el-button type="primary" :disabled="busy || (batch.kind === 'approve' && !batch.publicConfirmed)" @click="batchAct">{{ busy ? '正在处理…' : '确认处理' }}</el-button></template></el-dialog>
  </section>
</template>
<style scoped>
.cache-page { max-width: 1280px; }
.cache-tabs { margin-bottom: 20px; }
.cache-selection { display: flex; align-items: center; gap: 22px; margin-bottom: 14px; font-size: 11px; }
.cache-list { background: var(--surface); border: 1px solid var(--line); border-radius: 12px; overflow: hidden; }
.cache-entry { display: flex; align-items: flex-start; padding: 24px; gap: 18px; border-bottom: 1px solid var(--line); }
.cache-entry:last-child { border-bottom: 0; }
.cache-entry-main { text-align: left; border: 0; background: transparent; padding: 2px 0; color: var(--text); flex: 1; min-width: 0; }
.cache-entry-heading { display: flex; align-items: flex-start; gap: 20px; justify-content: space-between; }
.cache-entry-heading h3 { line-height: 1.65; font-size: 15px; margin-bottom: 10px; }
.cache-entry-main > p { display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; line-height: 1.85; font-size: 12px; color: var(--muted); margin-bottom: 16px; }
.cache-metadata { display: flex; gap: 23px; color: var(--muted); font-size: 10px; flex-wrap: wrap; }
.evidence { background: var(--canvas); border: 1px solid var(--line); border-radius: 9px; padding: 15px; margin: 22px 0; }
.evidence summary { cursor: pointer; color: var(--accent); font-size: 12px; }
.evidence article { padding: 16px 0; border-bottom: 1px solid var(--line); }
.evidence article:last-child { border: 0; }
.evidence h4 { font-size: 13px; line-height: 1.7; }
.review-actions { display: flex; justify-content: flex-end; gap: 8px; }
@media (max-width: 767px) { .cache-entry { padding: 18px 14px; gap: 10px; } .cache-entry > .icon-button { display: none; } .cache-entry-heading { flex-wrap: wrap; gap: 3px; margin-bottom: 10px; } .cache-metadata { gap: 10px; } .cache-selection { flex-wrap: wrap; gap: 10px; } .cache-selection .toolbar-end { width: auto; } }
</style>
