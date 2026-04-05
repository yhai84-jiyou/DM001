<template>
  <div class="rich-text-editor">
    <div class="editor-toolbar" id="toolbar-container"></div>
    <div class="editor-content" id="editor-container" style="min-height: 400px; border: 1px solid #dcdfe6; border-radius: 4px; padding: 16px;">
      <p style="color: #909399;">富文本编辑器加载中... (WangEditor 将在依赖安装后启用)</p>
      <textarea
        v-model="content"
        style="width:100%; min-height:350px; border:none; outline:none; resize:vertical; font-size:14px;"
        :placeholder="placeholder"
        @input="onInput"
      ></textarea>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

const props = defineProps<{
  modelValue?: string
  html?: string
  placeholder?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [val: string]
  'update:html': [val: string]
}>()

const content = ref(props.modelValue || '')

watch(() => props.modelValue, (v) => {
  if (v !== undefined) content.value = v
})

function onInput() {
  emit('update:modelValue', content.value)
  emit('update:html', content.value)
}
</script>
