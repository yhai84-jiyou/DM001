import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '../api/request'

interface DictItem {
  id: string
  name: string
  code: string
  color: string | null
  icon: string | null
  sort_order: number
  is_default: boolean
  extra_config: Record<string, any> | null
}

export const useDictionaryStore = defineStore('dictionary', () => {
  const cache = ref<Record<string, DictItem[]>>({})

  async function loadCategory(categoryCode: string): Promise<DictItem[]> {
    if (cache.value[categoryCode]) {
      return cache.value[categoryCode]
    }
    const res: any = await request.get(`/dictionary/${categoryCode}/items`)
    cache.value[categoryCode] = res.data
    return res.data
  }

  function getItems(categoryCode: string): DictItem[] {
    return cache.value[categoryCode] || []
  }

  function getItemByCode(categoryCode: string, itemCode: string): DictItem | undefined {
    return cache.value[categoryCode]?.find((item) => item.code === itemCode)
  }

  function clearCache() {
    cache.value = {}
  }

  return { cache, loadCategory, getItems, getItemByCode, clearCache }
})
