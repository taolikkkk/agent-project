<script setup lang="ts">
import { PhBooks, PhChatCircleText, PhFolderOpen, PhUploadSimple, PhCheckSquare, PhSun, PhMoon, PhSignOut, PhArrowUp, PhPlus, PhX, PhMagnifyingGlass, PhArrowSquareOut, PhFileText, PhArrowRight, PhSidebarSimple, PhTrash, PhArrowClockwise, PhArrowLeft, PhBookOpen, PhThumbsUp, PhThumbsDown, PhStop, PhCopy, PhCaretRight, PhCheck, PhCar, PhList } from '@phosphor-icons/vue'
const icons = { books: PhBooks, chat: PhChatCircleText, folder: PhFolderOpen, upload: PhUploadSimple, review: PhCheckSquare, sun: PhSun, moon: PhMoon, logout: PhSignOut, send: PhArrowUp, plus: PhPlus, close: PhX, search: PhMagnifyingGlass, external: PhArrowSquareOut, file: PhFileText, right: PhArrowRight, sidebar: PhSidebarSimple, trash: PhTrash, refresh: PhArrowClockwise, back: PhArrowLeft, book: PhBookOpen, like: PhThumbsUp, dislike: PhThumbsDown, stop: PhStop, copy: PhCopy, next: PhCaretRight, check: PhCheck, car: PhCar, menu: PhList }
defineProps<{ name: keyof typeof icons; size?: number }>()
</script>
<template><component :is="icons[name]" :size="size || 20" weight="regular" aria-hidden="true" /></template>
