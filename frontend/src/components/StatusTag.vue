<script setup lang="ts">
import { computed } from 'vue'
const props = defineProps<{ status: string }>()
const labels: Record<string, string> = { INIT: '等待处理', UPLOADED: '已上传', CONVERTING: '解析中', CONVERTED: '待切分', CHUNKED: '已切分', VECTOR_STORED: '可检索', STORED: '已存储', FAILED: '处理失败', PENDING: '待审核', APPROVED: '待发布', ACTIVE: '已发布', REJECTED: '已拒绝', DISABLED: '已下线' }
const good = computed(() => ['VECTOR_STORED', 'ACTIVE'].includes(props.status))
</script>
<template><span class="status-tag" :class="{ good, 'status-muted': ['DISABLED', 'REJECTED'].includes(status) }">{{ labels[status] || status }}</span></template>
