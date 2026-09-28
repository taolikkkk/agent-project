<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../api/http'
import type { KnowledgeDocument } from '../api/types'
import AppIcon from '../components/AppIcon.vue'
const route = useRoute()
const router = useRouter()
const newVersion = computed(() => typeof route.query.docId === 'string')
const selected = ref<File>()
const picker = ref<HTMLInputElement>()
const dragging = ref(false)
const busy = ref(false)
const error = ref('')
const title = ref('')
const description = ref('')
const version = ref(newVersion.value ? '' : '1.0.0')
const knowledgeBaseType = ref('DOCUMENT_SEARCH')
const accessibleBy = ref('VISITOR')
const tableName = ref('')
function choose(file?: File) {
  error.value = ''
  if (!file) return
  if (file.size > 100 * 1024 * 1024) { error.value = '单个文件不能超过 100 MB'; return }
  if (!/\.(pdf|doc|docx|xls|xlsx|csv|md|txt)$/i.test(file.name)) { error.value = '请选择 PDF、Word、Excel、CSV、Markdown 或 TXT 文件'; return }
  selected.value = file
  if (!title.value) title.value = file.name.replace(/\.[^.]+$/, '')
}
function drop(event: DragEvent) { dragging.value = false; if (!busy.value) choose(event.dataTransfer?.files[0]) }
async function submit() {
  error.value = ''
  if (!selected.value) { error.value = '请先选择文件'; return }
  if (!version.value || !/^\d+\.\d+\.\d+$/.test(version.value)) { error.value = '版本号请使用 1.0.0 格式'; return }
  if (!newVersion.value && (!title.value.trim() || !description.value.trim())) { error.value = '请填写文档标题和描述'; return }
  if (!newVersion.value && knowledgeBaseType.value === 'DATA_QUERY' && (!/^[a-z][a-z0-9_]*$/.test(tableName.value) || !/\.(xlsx?|csv)$/i.test(selected.value.name))) { error.value = '数据查询需上传 Excel 或 CSV，并填写小写字母开头的英文表名'; return }
  const form = new FormData()
  form.append('file', selected.value)
  form.append('version', version.value)
  if (newVersion.value) { form.append('docId', String(route.query.docId)); form.append('changelog', description.value) }
  else { form.append('title', title.value.trim()); form.append('description', description.value.trim()); form.append('knowledgeBaseType', knowledgeBaseType.value); form.append('accessibleBy', accessibleBy.value); if (tableName.value) form.append('tableName', tableName.value) }
  busy.value = true
  try {
    await api<KnowledgeDocument>(`/api/document/${newVersion.value ? 'upload-version' : 'upload'}`, { method: 'POST', body: form })
    ElMessage.success('上传成功，文档正在处理')
    await router.push('/documents')
  } catch (e) { error.value = (e as Error).message }
  finally { busy.value = false }
}
</script>
<template>
  <section class="page upload-page"><div class="page-heading"><div><h1>{{ newVersion ? '上传新版本' : '知识文档上传' }}</h1><p class="muted">{{ newVersion ? '更新文档内容，保留可追溯的版本记录。' : '把分散的信息，整理成可检索的知识。' }}</p></div><el-button @click="router.push('/documents')"><AppIcon name="back" :size="16" />返回文档管理</el-button></div>
    <div class="upload-layout"><form class="panel panel-content" @submit.prevent="submit"><input ref="picker" type="file" class="file-input" accept=".pdf,.doc,.docx,.xls,.xlsx,.csv,.md,.txt" :disabled="busy" aria-label="选择知识文档" @change="choose(($event.target as HTMLInputElement).files?.[0])" /><button type="button" class="upload-drop" :class="{ dragging, chosen: selected }" :disabled="busy" @click="picker?.click()" @dragover.prevent="dragging = true" @dragleave="dragging = false" @drop.prevent="drop"><span class="upload-symbol"><AppIcon :name="selected ? 'file' : 'upload'" :size="29" /></span><strong>{{ selected ? selected.name : '拖拽文件到此处，或点击选择' }}</strong><small>{{ selected ? `${(selected.size / 1024).toFixed(1)} KB · 点击更换文件` : 'PDF、Word、Excel、CSV、Markdown、TXT' }}</small><span v-if="!selected" class="upload-limit">单个文件不超过 100 MB</span></button>
      <div v-if="!newVersion" class="field"><label for="doc-title">文档标题</label><el-input id="doc-title" v-model="title" placeholder="使用清晰、易于检索的名称" maxlength="200" :disabled="busy" /></div>
      <div class="field-pair"><div v-if="!newVersion" class="field"><label for="knowledge-type">知识库类型</label><el-select id="knowledge-type" v-model="knowledgeBaseType" :disabled="busy"><el-option label="文档搜索" value="DOCUMENT_SEARCH" /><el-option label="数据查询" value="DATA_QUERY" /></el-select></div><div class="field"><label for="version">版本号</label><el-input id="version" v-model="version" placeholder="例如 1.0.0" :disabled="busy" /><small v-if="newVersion">新版本号需要高于现有版本。</small></div></div>
      <div v-if="!newVersion && knowledgeBaseType === 'DATA_QUERY'" class="field"><label for="table-name">数据表名</label><el-input id="table-name" v-model="tableName" placeholder="例如 vehicle_service_records" :disabled="busy" /><small>使用小写字母、数字和下划线，以字母开头。</small></div>
      <div v-if="!newVersion" class="field"><label for="permission">可见范围</label><el-select id="permission" v-model="accessibleBy" :disabled="busy"><el-option label="所有用户" value="VISITOR" /><el-option label="车主及客服" value="OWNER" /><el-option label="仅客服员工" value="CUSTOMER_SERVICE" /></el-select><small>检索结果会按照当前用户的身份进行过滤。</small></div>
      <div class="field"><label for="description">{{ newVersion ? '版本变更说明' : '文档描述' }}</label><el-input id="description" v-model="description" type="textarea" :rows="3" :placeholder="newVersion ? '说明本次更新的主要内容' : '简要说明文档内容和适用场景'" :disabled="busy" /></div><p v-if="error" class="inline-error" role="alert">{{ error }}</p><el-alert v-if="busy" title="文件正在上传与解析，请保持页面打开。" type="info" :closable="false" /><div class="form-actions"><el-button :disabled="busy" @click="router.push('/documents')">取消</el-button><el-button native-type="submit" type="primary" :disabled="busy">{{ busy ? '正在处理…' : '上传文档' }}<AppIcon name="upload" :size="16" /></el-button></div>
    </form><aside class="upload-guide"><h3>从文件到知识</h3><div><AppIcon name="upload" /><h4>上传与解析</h4><p>提取文档中的文本、表格和图片内容。</p></div><div><AppIcon name="file" /><h4>组织知识片段</h4><p>按内容结构切分，保留上下文关联。</p></div><div><AppIcon name="search" /><h4>建立检索索引</h4><p>完成向量化后，即可在对话中检索引用。</p></div><p class="guide-note">可以在文档管理中查看处理状态、调整分段并管理历史版本。</p></aside></div>
  </section>
</template>
<style scoped>
.upload-page { max-width: 1220px; }
.upload-layout { display: grid; grid-template-columns: minmax(0,1fr) 238px; gap: 36px; align-items: start; }
.file-input { position: absolute; width: 1px; height: 1px; opacity: 0; }
.upload-drop { width: 100%; border: 1px dashed var(--line); border-radius: 10px; background: var(--canvas); display: flex; flex-direction: column; align-items: center; padding: 28px 16px; margin-bottom: 27px; color: var(--text); }
.upload-drop:hover,.upload-drop.dragging { background: var(--accent-soft); border-color: var(--accent); }
.upload-symbol { display: grid; place-items: center; width: 52px; height: 52px; border-radius: 11px; color: var(--accent); background: var(--accent-soft); margin-bottom: 16px; }
.upload-drop strong { font-size: 13px; font-weight: 500; overflow-wrap: anywhere; max-width: 100%; }
.upload-drop small { font-size: 11px; color: var(--muted); margin-top: 10px; }
.upload-limit { font-size: 10px; color: var(--muted); margin-top: 6px; }
.upload-guide { padding-top: 22px; }
.upload-guide > h3 { font-size: 14px; margin-bottom: 27px; }
.upload-guide > div { position: relative; padding-left: 31px; margin-bottom: 29px; }
.upload-guide svg { position: absolute; left: 0; top: 0; color: var(--accent); }
.upload-guide h4 { font-size: 12px; font-weight: 500; margin: 0 0 8px; }
.upload-guide p { color: var(--muted); font-size: 11px; line-height: 1.9; }
.guide-note { border-top: 1px solid var(--line); padding-top: 20px; }
@media (max-width: 1000px) { .upload-layout { grid-template-columns: minmax(0,1fr); } .upload-guide { display: none; } }
</style>
