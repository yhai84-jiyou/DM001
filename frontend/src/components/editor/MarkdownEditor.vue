<template>
  <div class="markdown-editor">
    <div style="display:flex; gap: 16px; min-height: 500px;">
      <textarea
        v-model="content"
        style="flex:1; border:1px solid #dcdfe6; border-radius:4px; padding:16px; font-family:monospace; font-size:14px; resize:vertical;"
        :placeholder="placeholder"
        @input="onInput"
      ></textarea>
      <div style="flex:1; border:1px solid #dcdfe6; border-radius:4px; padding:16px; overflow:auto;">
        <p style="color: #909399;">Markdown 预览 (md-editor-v3 将在依赖安装后启用)</p>
        <div v-html="content"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

const props = defineProps<{
  modelValue?: string
  placeholder?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [val: string]
}>()

const content = ref(props.modelValue || '')

watch(() => props.modelValue, (v) => {
  if (v !== undefined) content.value = v
})

function onInput() {
  emit('update:modelValue', content.value)
}
</script>
