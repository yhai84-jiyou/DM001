<template>
  <div class="rich-text-editor">
    <Toolbar
      :editor="editorRef"
      :defaultConfig="toolbarConfig"
      :mode="mode"
      style="border-bottom: 1px solid #e8e8e8;"
    />
    <Editor
      :defaultConfig="editorConfig"
      :mode="mode"
      v-model="valueHtml"
      style="min-height: 400px; overflow-y: hidden;"
      @onCreated="handleCreated"
      @onChange="handleChange"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, shallowRef, onBeforeUnmount, watch } from 'vue'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import type { IDomEditor, IToolbarConfig, IEditorConfig } from '@wangeditor/editor'
import '@wangeditor/editor/dist/css/style.css'
import { mediaApi } from '@/api/media'

const props = defineProps<{
  modelValue?: string
  html?: string
  placeholder?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [val: string]
  'update:html': [val: string]
}>()

const mode = 'default'
const editorRef = shallowRef<IDomEditor>()
const valueHtml = ref(props.html || props.modelValue || '')

// Sync from parent
watch(() => props.html, (v) => {
  if (v !== undefined && v !== valueHtml.value) {
    valueHtml.value = v
  }
})
watch(() => props.modelValue, (v) => {
  if (v !== undefined && !props.html && v !== valueHtml.value) {
    valueHtml.value = v
  }
})

const toolbarConfig: Partial<IToolbarConfig> = {
  excludeKeys: [
    'group-video', // We handle video upload ourselves below
  ],
}

const editorConfig: Partial<IEditorConfig> = {
  placeholder: props.placeholder || '请输入文章内容...',
  MENU_CONF: {
    uploadImage: {
      async customUpload(file: File, insertFn: (url: string, alt?: string, href?: string) => void) {
        const formData = new FormData()
        formData.append('file', file)
        try {
          const { data } = await mediaApi.upload(formData)
          insertFn(`/uploads/${data.filePath}`, data.originalName, '')
        } catch {
          console.error('Image upload failed')
        }
      },
    },
    uploadVideo: {
      async customUpload(file: File, insertFn: (url: string, poster?: string) => void) {
        const formData = new FormData()
        formData.append('file', file)
        try {
          const { data } = await mediaApi.upload(formData)
          insertFn(`/uploads/${data.filePath}`, '')
        } catch {
          console.error('Video upload failed')
        }
      },
    },
  },
}

function handleCreated(editor: IDomEditor) {
  editorRef.value = editor
}

function handleChange(editor: IDomEditor) {
  const html = editor.getHtml()
  const text = editor.getText()
  emit('update:html', html)
  emit('update:modelValue', text)
}

onBeforeUnmount(() => {
  const editor = editorRef.value
  if (editor) editor.destroy()
})
</script>

<style scoped>
.rich-text-editor {
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  overflow: hidden;
}
</style>
