<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api, json, query } from '../api/http'
import type { KnowledgeDocument, DocumentVersion, Segment, Page } from '../api/types'
import AppIcon from '../components/AppIcon.vue'
import StatusTag from '../components/StatusTag.vue'
const route = useRoute()
const router = useRouter()
const rows = ref<KnowledgeDocument[]>([])
const total = ref(0), page = ref(1)
const loading = ref(true), error = ref(''), busy = ref(false)
const filters = reactive({ docTitle: '', status: '', knowledgeBaseType: '' })
const selected = ref<KnowledgeDocument[]>([])
const editing = ref<KnowledgeDocument>()
const editOpen = ref(false)
const current = ref<KnowledgeDocument>()
const detailOpen = ref(false)
const detailTab = ref('versions')
const detailLoading = ref(false), detailError = ref('')
const versions = ref<DocumentVersion[]>([])
const segments = ref<Segment[]>([]), segmentTotal = ref(0), segmentPage = ref(1), segmentVersion = ref('')
const editSegment = ref<Segment>(), segmentEditOpen = ref(false)
const dataRows = ref<Record<string, unknown>[]>([]), dataTotal = ref(0), dataPage = ref(1)
const splitOpen = ref(false)
const defaultSplit = { splitType: 'SMART', chunkSize: 1000, overlap: 100, titleLevel: 2, separator: '', regex: '' }
const split = reactive({ ...defaultSplit })
const splitOptions = [
  { value: 'SMART', label: '智能切分', description: '按 Markdown 的标题结构组织内容，并自动保留约 10% 的上下文。适合大多数文档。' },
  { value: 'TITLE', label: '按标题切分', description: '按指定 Markdown 标题层级分段，适合章节层次清晰的说明文档。' },
  { value: 'LENGTH', label: '按长度切分', description: '按设定的分块大小切分，适合没有稳定结构的纯文本。' },
  { value: 'SEPARATOR', label: '按分隔符切分', description: '遇到指定分隔符时优先切分，适合条目、问答或固定版式内容。' },
  { value: 'REGEX', label: '按正则切分', description: '使用正则表达式识别边界，适合格式规律明确的文本。' },
]
const selectedSplitOption = computed(() => splitOptions.find(option => option.value === split.splitType) ?? splitOptions[0])
const usesManualOverlap = computed(() => split.splitType !== 'SMART')
const splitDescription = computed(() => `将使用「${selectedSplitOption.value.label}」生成知识片段；提交后系统会自动建立检索索引。`)
const statuses: Record<string,string> = { UPLOADED: '已上传', CONVERTING: '解析中', CONVERTED: '待切分', CHUNKED: '已切分', VECTOR_STORED: '可检索', STORED: '已存储' }
const permissions: Record<string,string> = { VISITOR: '所有用户', OWNER: '车主及客服', CUSTOMER_SERVICE: '仅客服员工' }
function date(value?: string) { return value ? value.replace('T',' ').slice(0,16) : '暂无记录' }
function safeUrl(value?: string) { return value && /^https?:\/\//i.test(value) ? value : undefined }
function pretty(value?: string) { try { return JSON.stringify(JSON.parse(value || '{}'), null, 2) } catch { return value } }
async function load(reset = false) {
  if (reset) page.value = 1
  loading.value = true; error.value = ''
  try { const result = await api<Page<KnowledgeDocument>>(`/api/document/page?${query({ ...filters, current: page.value, size: 10 })}`); rows.value = result.records; total.value = Number(result.total) }
  catch (e) { error.value = (e as Error).message }
  finally { loading.value = false }
}
async function continueUploadFlow() {
  if (route.query.next !== 'split' || typeof route.query.docId !== 'string') return
  const docId = Number(route.query.docId)
  if (!Number.isSafeInteger(docId)) return
  try {
    const document = await api<KnowledgeDocument>(`/api/document/${docId}`)
    if (document.status === 'CONVERTED') openSplit(document)
    else ElMessage.info('文档正在解析，解析完成并显示“待切分”后可开始切分。')
  } catch (e) { ElMessage.error((e as Error).message) }
  finally { await router.replace('/documents') }
}
onMounted(async () => { await load(); await continueUploadFlow() })
watch(page, () => load())
async function remove(doc?: KnowledgeDocument) {
  const items = doc ? [doc] : selected.value
  try {
    await ElMessageBox.confirm(`将删除${items.length === 1 ? `「${items[0]!.docTitle}」` : `选中的 ${items.length} 份文档`}及关联分段。`, '删除文档', { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' })
    await api(doc ? `/api/document/${doc.docId}` : `/api/document/batch?${query({ ids: items.map(d => d.docId).join(',') })}`, { method: 'DELETE' })
    ElMessage.success('文档已删除'); selected.value = []; await load()
  } catch (e) { if (e instanceof Error) ElMessage.error(e.message) }
}
function edit(doc: KnowledgeDocument) { editing.value = { ...doc }; editOpen.value = true }
async function save() {
  if (!editing.value?.docTitle.trim()) { ElMessage.error('请输入文档标题'); return }
  busy.value = true
  try { if (!await api<boolean>('/api/document', json('PUT', editing.value))) throw new Error('文档已变化，请刷新后重试'); editOpen.value = false; ElMessage.success('文档已更新'); await load() }
  catch (e) { ElMessage.error((e as Error).message) }
  finally { busy.value = false }
}
async function loadVersions() { if (current.value) versions.value = await api<DocumentVersion[]>(`/api/document/versions/${current.value.docId}`) }
async function loadSegments() {
  if (!current.value) return
  const result = await api<Page<Segment>>(`/api/segment/page-by-document?${query({ documentId: current.value.docId, documentVersion: segmentVersion.value, current: segmentPage.value, size: 10 })}`)
  segments.value = result.records; segmentTotal.value = Number(result.total)
}
async function loadData() {
  if (!current.value) return
  const result = await api<Page<Record<string, unknown>>>(`/api/document/data/${current.value.docId}?${query({ current: dataPage.value, size: 20 })}`)
  dataRows.value = result.records; dataTotal.value = Number(result.total)
}
async function loadDetail() {
  detailError.value = ''; detailLoading.value = true
  try { if (detailTab.value === 'versions') await loadVersions(); else if (detailTab.value === 'segments') await loadSegments(); else if (detailTab.value === 'data') await loadData() }
  catch (e) { detailError.value = (e as Error).message }
  finally { detailLoading.value = false }
}
async function detail(doc: KnowledgeDocument) {
  current.value = doc; detailTab.value = 'versions'; versions.value = []; segments.value = []; segmentPage.value = 1; segmentVersion.value = ''; dataPage.value = 1; detailOpen.value = true; await loadDetail()
}
watch([detailTab, segmentPage, segmentVersion, dataPage], () => { if (detailOpen.value) void loadDetail() })
async function versionAction(version: DocumentVersion, action: string) {
  const name = action === 'switch' ? '切换到此版本并重建索引' : action === 'activate' ? '重新启用此版本' : '停用此版本'
  try {
    await ElMessageBox.confirm(`确定${name}？`, '版本操作', { confirmButtonText: '确定', cancelButtonText: '取消' })
    busy.value = true
    await api(`/api/document/${action}-version?${query({ docId: current.value?.docId, versionId: version.versionId })}`, { method: 'POST' })
    await loadVersions(); await load(); ElMessage.success('版本操作完成')
  } catch (e) { if (e instanceof Error) ElMessage.error(e.message) }
  finally { busy.value = false }
}
function canStartSplit(doc: KnowledgeDocument) { return doc.knowledgeBaseType === 'DOCUMENT_SEARCH' && doc.status === 'CONVERTED' }
function restoreSplit(doc: KnowledgeDocument) {
  Object.assign(split, defaultSplit)
  try {
    const saved = JSON.parse(doc.extension || '{}')?.splitParam
    if (!saved || !splitOptions.some(option => option.value === saved.splitType)) return
    Object.assign(split, {
      splitType: saved.splitType,
      chunkSize: Number.isInteger(saved.chunkSize) ? saved.chunkSize : defaultSplit.chunkSize,
      overlap: Number.isInteger(saved.overlap) ? saved.overlap : defaultSplit.overlap,
      titleLevel: Number.isInteger(saved.titleLevel) ? saved.titleLevel : defaultSplit.titleLevel,
      separator: typeof saved.separator === 'string' ? saved.separator : '',
      regex: typeof saved.regex === 'string' ? saved.regex : '',
    })
  } catch { /* 扩展信息无法解析时使用默认配置。 */ }
}
function openSplit(doc: KnowledgeDocument) { current.value = doc; restoreSplit(doc); splitOpen.value = true }
function handleDocumentAction(command: string, doc: KnowledgeDocument) {
  if (command === 'edit') edit(doc)
  else if (command === 'upload') void router.push({ path: '/documents/upload', query: { docId: doc.docId } })
  else if (command === 'delete') void remove(doc)
}
async function splitDocument() {
  if (!current.value) return
  if (!Number.isInteger(split.chunkSize) || split.chunkSize < 50) { ElMessage.error('分块大小至少为 50'); return }
  if (usesManualOverlap.value && (!Number.isInteger(split.overlap) || split.overlap < 0 || split.overlap >= split.chunkSize)) { ElMessage.error('重叠长度需大于等于 0，且小于分块大小'); return }
  if (split.splitType === 'TITLE' && (!Number.isInteger(split.titleLevel) || split.titleLevel < 1 || split.titleLevel > 6)) { ElMessage.error('标题层级需在 1 到 6 之间'); return }
  if (split.splitType === 'SEPARATOR' && !split.separator.trim()) { ElMessage.error('请输入分隔符'); return }
  if (split.splitType === 'REGEX' && !split.regex.trim()) { ElMessage.error('请输入正则表达式'); return }
  busy.value = true
  const params = {
    splitType: split.splitType,
    chunkSize: split.chunkSize,
    overlap: usesManualOverlap.value ? split.overlap : undefined,
    titleLevel: split.splitType === 'TITLE' ? split.titleLevel : undefined,
    separator: split.splitType === 'SEPARATOR' ? split.separator.trim() : undefined,
    regex: split.splitType === 'REGEX' ? split.regex.trim() : undefined,
  }
  try { const count = await api<number>(`/api/document/split/${current.value.docId}?${query(params)}`, { method: 'POST' }); ElMessage.success(`已生成 ${count} 个片段，正在建立检索索引`); splitOpen.value = false; await load() }
  catch (e) { ElMessage.error((e as Error).message) }
  finally { busy.value = false }
}
function startEditSegment(segment: Segment) { editSegment.value = { ...segment }; segmentEditOpen.value = true }
async function saveSegment() {
  if (!editSegment.value?.text.trim()) { ElMessage.error('片段内容不能为空'); return }
  busy.value = true
  try { if (!await api<boolean>('/api/segment', json('PUT', editSegment.value))) throw new Error('片段已变化，请刷新后重试'); segmentEditOpen.value = false; await loadSegments(); ElMessage.success('片段已更新') }
  catch (e) { ElMessage.error((e as Error).message) }
  finally { busy.value = false }
}
async function removeSegment(segment: Segment) {
  try { await ElMessageBox.confirm('删除此片段及对应检索向量？', '删除片段', { confirmButtonText: '删除', cancelButtonText: '取消' }); await api(`/api/segment/${segment.id}`, { method: 'DELETE' }); await loadSegments() }
  catch (e) { if (e instanceof Error) ElMessage.error(e.message) }
}
</script>
<template>
  <section class="page"><div class="page-heading"><div><h1>文档管理</h1><p class="muted">维护知识来源，让每一份文档持续发挥价值。</p></div><el-button type="primary" @click="router.push('/documents/upload')"><AppIcon name="plus" :size="17" />上传文档</el-button></div>
    <form class="toolbar" @submit.prevent="load(true)"><el-input v-model="filters.docTitle" placeholder="搜索文档标题" aria-label="文档标题" clearable @clear="load(true)"><template #prefix><AppIcon name="search" :size="17" /></template></el-input><el-select v-model="filters.knowledgeBaseType" placeholder="全部类型" aria-label="知识库类型" clearable @change="load(true)"><el-option label="文档搜索" value="DOCUMENT_SEARCH" /><el-option label="数据查询" value="DATA_QUERY" /></el-select><el-select v-model="filters.status" placeholder="全部状态" aria-label="处理状态" clearable @change="load(true)"><el-option v-for="(label, value) in statuses" :key="value" :value="value" :label="label" /></el-select><el-button native-type="submit">搜索</el-button><div class="toolbar-end"><el-button v-if="selected.length" @click="remove()">删除选中 {{ selected.length }} 项</el-button><button class="icon-button" aria-label="刷新文档列表" type="button" @click="load()"><AppIcon name="refresh" :size="19" /></button></div></form>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <div class="panel"><div v-if="loading" class="panel-content"><el-skeleton :rows="7" /></div><div v-else-if="!rows.length" class="empty-state"><span class="empty-icon"><AppIcon name="folder" :size="30" /></span><h3>{{ filters.docTitle || filters.status || filters.knowledgeBaseType ? '没有找到匹配的文档' : '知识库等待你的第一份文档' }}</h3><p>上传文档后，可以在这里管理版本、查看片段和跟踪处理状态。</p><el-button type="primary" @click="router.push('/documents/upload')">上传文档</el-button></div><el-table v-else :data="rows" row-key="docId" @selection-change="selected = $event"><el-table-column type="selection" width="44" /><el-table-column label="文档" min-width="260"><template #default="{ row }"><div class="doc-title"><span class="file-badge"><AppIcon name="file" :size="20" /></span><div><button class="text-button" style="text-align:left;padding:0" @click="detail(row)"><strong>{{ row.docTitle }}</strong></button><small>{{ row.description || '暂无描述' }}</small></div></div></template></el-table-column><el-table-column label="类型" width="105"><template #default="{ row }">{{ row.knowledgeBaseType === 'DATA_QUERY' ? '数据查询' : '文档搜索' }}</template></el-table-column><el-table-column label="状态" width="100"><template #default="{ row }"><StatusTag :status="row.status" /></template></el-table-column><el-table-column label="更新时间" width="155"><template #default="{ row }"><span class="muted">{{ date(row.updatedAt) }}</span></template></el-table-column><el-table-column label="操作" width="230" fixed="right"><template #default="{ row }"><div class="action-row"><button class="text-button" @click="detail(row)">详情</button><el-button v-if="canStartSplit(row)" type="primary" link @click="openSplit(row)">开始切分</el-button><el-dropdown trigger="click" @command="(command: string) => handleDocumentAction(command, row)"><button class="text-button" aria-label="更多文档操作">更多</button><template #dropdown><el-dropdown-menu><el-dropdown-item command="edit">编辑信息</el-dropdown-item><el-dropdown-item command="upload">上传新版本</el-dropdown-item><el-dropdown-item command="delete" divided>删除文档</el-dropdown-item></el-dropdown-menu></template></el-dropdown></div></template></el-table-column></el-table><div class="panel-footer"><span>共 {{ total }} 份文档</span><el-pagination v-model:current-page="page" :page-size="10" :total="total" layout="prev, pager, next" /></div></div>
    <el-dialog v-model="editOpen" title="编辑文档信息" width="560px"><template v-if="editing"><div class="field"><label for="edit-title">文档标题</label><el-input id="edit-title" v-model="editing.docTitle" /></div><div class="field"><label for="edit-description">文档描述</label><el-input id="edit-description" v-model="editing.description" type="textarea" :rows="3" /></div><div class="field"><label for="edit-access">可见范围</label><el-select id="edit-access" v-model="editing.accessibleBy"><el-option v-for="(label, value) in permissions" :key="value" :label="label" :value="value" /></el-select></div></template><template #footer><el-button @click="editOpen = false">取消</el-button><el-button type="primary" :disabled="busy" @click="save">{{ busy ? '正在保存…' : '保存' }}</el-button></template></el-dialog>
    <el-drawer v-model="detailOpen" :title="current?.docTitle || '文档详情'" size="760px"><div v-if="current" class="detail-meta"><StatusTag :status="current.status" /><span>{{ permissions[current.accessibleBy || ''] || current.accessibleBy }}</span><span>更新于 {{ date(current.updatedAt) }}</span></div><el-tabs v-model="detailTab"><el-tab-pane label="版本记录" name="versions" /><el-tab-pane label="知识片段" name="segments" /><el-tab-pane v-if="current?.knowledgeBaseType === 'DATA_QUERY'" label="表格数据" name="data" /><el-tab-pane label="元数据" name="metadata" /></el-tabs><el-alert v-if="detailError" :title="detailError" type="error" :closable="false" /><el-skeleton v-if="detailLoading" :rows="5" /><template v-else-if="detailTab === 'versions'"><el-button class="version-upload" @click="router.push({ path: '/documents/upload', query: { docId: current?.docId } })"><AppIcon name="upload" :size="16" />上传新版本</el-button><p v-if="!versions.length" class="muted">暂无版本记录。</p><article v-for="version in versions" :key="version.versionId" class="version-entry"><div class="version-header"><strong>{{ version.version }}</strong><StatusTag :status="version.status" /><span class="muted">{{ date(version.createdAt) }}</span></div><p class="muted">{{ version.changelog || '暂无变更说明' }}</p><div class="action-row"><button class="text-button" :disabled="busy" @click="versionAction(version, 'switch')">切换版本</button><button v-if="version.status === 'VECTOR_STORED'" class="text-button" :disabled="busy" @click="versionAction(version, 'deactivate')">停用</button><button v-else-if="version.status === 'CHUNKED' || version.status === 'STORED'" class="text-button" :disabled="busy" @click="versionAction(version, 'activate')">启用</button><button class="text-button" @click="segmentVersion = version.versionId; detailTab = 'segments'">查看片段</button><a v-if="safeUrl(version.docUrl)" class="text-button" :href="safeUrl(version.docUrl)" target="_blank" rel="noopener noreferrer">查看原文</a></div></article></template>
      <template v-else-if="detailTab === 'segments'"><div class="segment-toolbar"><span class="muted">共 {{ segmentTotal }} 个片段</span><el-select v-model="segmentVersion" placeholder="全部版本" clearable aria-label="片段所属版本"><el-option v-for="version in versions" :key="version.versionId" :value="version.versionId" :label="version.version" /></el-select></div><p v-if="!segments.length" class="muted">暂无知识片段，文档解析完成后可执行切分。</p><article v-for="segment in segments" :key="segment.id" class="segment-entry"><div class="version-header"><strong>片段 {{ segment.chunkOrder }}</strong><StatusTag :status="segment.status" /><span v-if="segment.skipEmbedding" class="muted">父分段</span></div><p class="segment-text">{{ segment.text }}</p><details v-if="segment.metadata"><summary>片段元数据</summary><pre>{{ pretty(segment.metadata) }}</pre></details><div class="action-row"><button class="text-button" :disabled="segment.skipEmbedding === 1" @click="startEditSegment(segment)">编辑片段</button><button class="text-button" @click="removeSegment(segment)">删除</button></div></article><el-pagination v-model:current-page="segmentPage" :page-size="10" :total="segmentTotal" layout="prev, pager, next" /></template>
      <template v-else-if="detailTab === 'data'"><el-table :data="dataRows"><el-table-column v-for="column in Object.keys(dataRows[0] || {})" :key="column" :prop="column" :label="column" min-width="140" show-overflow-tooltip /></el-table><div class="panel-footer"><span>共 {{ dataTotal }} 条记录</span><el-pagination v-model:current-page="dataPage" :page-size="20" :total="dataTotal" layout="prev, pager, next" /></div></template><pre v-else class="metadata-block">{{ pretty(current?.extension) }}</pre>
    </el-drawer>
    <el-dialog v-model="segmentEditOpen" title="编辑知识片段" width="660px"><div v-if="editSegment" class="field"><label for="segment-content">片段内容</label><el-input id="segment-content" v-model="editSegment.text" type="textarea" :rows="12" /><small>保存后会同步更新该片段的检索向量。</small></div><template #footer><el-button @click="segmentEditOpen = false">取消</el-button><el-button type="primary" :disabled="busy" @click="saveSegment">{{ busy ? '正在保存…' : '保存' }}</el-button></template></el-dialog>
    <el-dialog v-model="splitOpen" title="切分文档" width="560px"><el-alert :title="splitDescription" type="info" :closable="false" /><div class="field"><label for="split-type">切分方式 <small>（必填）</small></label><el-select id="split-type" v-model="split.splitType"><el-option v-for="option in splitOptions" :key="option.value" :label="option.label" :value="option.value" /></el-select><small>{{ selectedSplitOption.description }}</small></div><div class="field-pair"><div class="field"><label>分块大小 <small>（必填）</small></label><el-input-number v-model="split.chunkSize" :min="50" :max="16000" aria-label="分块大小" /><small>每个知识片段的最大长度。</small></div><div v-if="usesManualOverlap" class="field"><label>重叠长度</label><el-input-number v-model="split.overlap" :min="0" :max="Math.max(0, split.chunkSize - 1)" aria-label="重叠长度" /><small>相邻片段保留的上下文长度。</small></div></div><div v-if="split.splitType === 'SMART'" class="split-note">智能切分会自动使用约为分块大小 10% 的重叠长度。</div><div v-if="split.splitType === 'TITLE'" class="field"><label>标题层级 <small>（必填）</small></label><el-input-number v-model="split.titleLevel" :min="1" :max="6" aria-label="标题层级" /><small>1 为一级标题，6 为六级标题。</small></div><div v-if="split.splitType === 'SEPARATOR'" class="field"><label for="separator">分隔符 <small>（必填）</small></label><el-input id="separator" v-model="split.separator" placeholder="例如：--- 或 \\n" /><small>按此分隔符优先划分片段。</small></div><div v-if="split.splitType === 'REGEX'" class="field"><label for="regex">正则表达式 <small>（必填）</small></label><el-input id="regex" v-model="split.regex" placeholder="例如：\\n#{1,6}\\s" /><small>使用 Java 正则表达式描述切分边界。</small></div><template #footer><el-button :disabled="busy" @click="splitOpen = false">取消</el-button><el-button type="primary" :disabled="busy" @click="splitDocument">{{ busy ? '正在切分…' : '开始切分' }}</el-button></template></el-dialog>
  </section>
</template>
<style scoped>
.version-upload { margin: 6px 0 22px; }
.version-entry,.segment-entry { padding: 18px 0; border-bottom: 1px solid var(--line); }
.version-header { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; font-size: 12px; flex-wrap: wrap; }
.version-header strong { font-weight: 600; font-size: 14px; }
.version-header > span:last-child { margin-left: auto; }
.version-entry p { font-size: 12px; }
.segment-text { white-space: pre-wrap; overflow-wrap: anywhere; line-height: 1.85; font-size: 13px; max-height: 340px; overflow: auto; }
.segment-toolbar { display: flex; justify-content: space-between; gap: 18px; align-items: center; margin-bottom: 15px; }
.segment-toolbar .el-select { width: 180px; }
.split-note { margin: -6px 0 18px; color: var(--muted); font-size: 12px; line-height: 1.7; }
summary { cursor: pointer; font-size: 12px; color: var(--muted); padding: 8px 0; }
pre { font-size: 11px; white-space: pre-wrap; overflow-wrap: anywhere; background: var(--canvas); padding: 16px; line-height: 1.7; border-radius: 8px; }
</style>
