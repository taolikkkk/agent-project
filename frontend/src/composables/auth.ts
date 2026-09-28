import { ref } from 'vue'
import { api, json } from '../api/http'
import type { User } from '../api/types'

const user = ref<User | null>(null)
let checked = false
export function useAuth() {
  async function restore() {
    if (checked) return
    const loggedIn = await api<boolean>('/auth/isLogin')
    user.value = loggedIn ? await api<User>('/auth/userInfo') : null
    checked = true
  }
  async function login(staff: boolean, account: string, password: string) {
    user.value = await api<User>(staff ? '/auth/staffLogin' : '/auth/login', json('POST', staff ? { empId: account, password } : { phone: account, password }))
    checked = true
  }
  async function logout() {
    await api('/auth/logout', json('POST'))
    clear()
  }
  function clear() { user.value = null; checked = true }
  return { user, restore, login, logout, clear }
}
