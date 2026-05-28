import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '../api/request'

interface UserInfo {
  id: string
  name: string
  phone: string
  user_type: string
  vendor_role: string | null
  org_id: string
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('access_token') || '')
  const user = ref<UserInfo | null>(null)

  const isLoggedIn = computed(() => !!token.value)
  const isClient = computed(() => user.value?.user_type === 'CLIENT')
  const isVendor = computed(() => user.value?.user_type === 'VENDOR')

  async function login(phone: string, password: string) {
    const res: any = await request.post('/auth/login', { phone, password })
    token.value = res.data.access_token
    localStorage.setItem('access_token', res.data.access_token)
    await fetchMe()
  }

  async function fetchMe() {
    const res: any = await request.get('/auth/me')
    user.value = res.data
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('access_token')
  }

  return { token, user, isLoggedIn, isClient, isVendor, login, fetchMe, logout }
})
