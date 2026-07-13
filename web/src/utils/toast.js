// 居中深色半透明胶囊 toast（设计规范：rgba(0,0,0,.75) 白字 13px, 1.5-2s）
export function toast(title, icon = 'none') {
  uni.showToast({ title: String(title || ''), icon, mask: false, duration: 1800 })
}

export function loading(title = '加载中') {
  uni.showLoading({ title, mask: true })
}

export function hideLoading() {
  uni.hideLoading()
}

export function confirm(content, title = '提示') {
  return new Promise((resolve) => {
    uni.showModal({
      title,
      content,
      confirmColor: '#ff2442',
      success: (res) => resolve(res.confirm),
      fail: () => resolve(false),
    })
  })
}
