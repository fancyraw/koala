import { api } from '@/api'

// 七牛直传：uni.uploadFile 以 multipart 传 file，附带 token + key，返回体含 key。
// 未配七牛密钥时后端签发 mock token，直传会失败——本地联调仅走到这一步为预期。
function qiniuUpload(cred, filePath) {
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: cred.uploadUrl,
      filePath,
      name: 'file',
      formData: {
        token: cred.params.token,
        key: cred.key,
      },
      success: (res) => {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve(cred.publicUrl)
        } else {
          reject(new Error(`上传失败(${res.statusCode})`))
        }
      },
      fail: reject,
    })
  })
}

// 选图并直传，返回可访问 URL 数组。count 为剩余可选张数。
export function chooseAndUpload(count = 1) {
  return new Promise((resolve, reject) => {
    uni.chooseImage({
      count,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: async (chosen) => {
        const paths = chosen.tempFilePaths || []
        if (!paths.length) return resolve([])
        uni.showLoading({ title: '上传中', mask: true })
        try {
          const urls = []
          for (const p of paths) {
            const cred = await api.uploadCredential()
            const url = await uploadByProvider(cred, p)
            urls.push(url)
          }
          resolve(urls)
        } catch (e) {
          reject(e)
        } finally {
          uni.hideLoading()
        }
      },
      fail: (err) => {
        // 用户取消不算错误
        if (err && /cancel/i.test(err.errMsg || '')) return resolve([])
        reject(err)
      },
    })
  })
}

function uploadByProvider(cred, filePath) {
  switch (cred.provider) {
    case 'qiniu':
      return qiniuUpload(cred, filePath)
    default:
      return Promise.reject(new Error(`暂不支持的存储 provider: ${cred.provider}`))
  }
}
