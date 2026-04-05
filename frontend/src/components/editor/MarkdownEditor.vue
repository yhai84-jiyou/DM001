<template>
  <div class="markdown-editor-wrapper">
    <MdEditor
      v-model="content"
      :theme="'light'"
      :language="'zh-CN'"
      :preview="true"
      :toolbarsExclude="['github']"
      :style="{ height: '500px' }"
      @onUploadImg="handleUploadImg"
      @onChange="handleChange"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { mediaApi } from '@/api/media'

const props = defineProps<{
  modelValue?: string
  placeholder?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [val: string]
}>()

const content = ref(props.modelValue || '')

watch(() => props.modelValue, (v) => {
  if (v !== undefined && v !== content.value) {
    content.value = v
  }
})

function handleChange(val: string) {
  emit('update:modelValue', val)
}

async function handleUploadImg(files: File[], callback: (urls: string[]) => void) {
  const urls: string[] = []
  for (const file of files) {
    const formData = new FormData()
    formData.append('file', file)
    try {
      const { data } = await mediaApi.upload(formData)
      urls.push(`/uploads/${data.filePath}`)
    } catch {
      console.error('Image upload failed:', file.name)
    }
  }
  callback(urls)
}
</script>

<style scoped>
.markdown-editor-wrapper {
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  overflow: hidden;
}
</style>
