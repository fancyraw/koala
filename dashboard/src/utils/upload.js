import axios from 'axios'
import { api } from '@/api'

// 取后端直传凭证，把文件直传对象存储，返回可访问的 publicUrl。
// 本地未配置对象存储时后端签发 mock 凭证，直传可能失败——与 C 端一致。
export async function uploadFile(file) {
  const cred = await api.uploadCredential()
  const form = new FormData()
  Object.entries(cred.params || {}).forEach(([k, v]) => form.append(k, v))
  if (cred.key) form.append('key', cred.key)
  form.append('file', file)
  await axios.post(cred.uploadUrl, form)
  return cred.publicUrl
}
