<script setup lang="ts">
import { computed } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
defineOptions({ inheritAttrs: false })
const props = defineProps<{ text: string }>()
const content = computed(() => DOMPurify.sanitize(marked.parse(props.text, { async: false, breaks: true }) as string, { ADD_ATTR: ['target'], FORBID_TAGS: ['style', 'form', 'input'] }))
</script>
<template><div class="markdown" v-html="content" /></template>
