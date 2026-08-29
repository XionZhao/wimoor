/**
 * 版本检测工具
 * 用于检测前端应用是否有新版本更新
 */

let currentVersion = null
let timer = null
let isChecking = false

// 版本更新回调
let onUpdateCallback = null

/**
 * 获取当前版本
 * @returns {Promise<string>} 版本号
 */
async function getVersion() {
  try {
    const response = await fetch(`/version.json?t=${Date.now()}`)
    if (!response.ok) {
      throw new Error('Failed to fetch version')
    }
    const data = await response.json()
    return data.version
  } catch (error) {
    console.warn('[Version] 获取版本号失败:', error)
    return null
  }
}

/**
 * 检查版本更新
 * @returns {Promise<boolean>} 是否有更新
 */
async function checkUpdate() {
  if (isChecking) return false
  isChecking = true

  try {
    const latestVersion = await getVersion()
    
    if (!latestVersion) {
      return false
    }

    // 首次获取版本，记录当前版本
    if (!currentVersion) {
      currentVersion = latestVersion
      console.log(`[Version] 当前版本: ${currentVersion}`)
      return false
    }

    // 版本不一致，说明有更新
    if (currentVersion !== latestVersion) {
      console.log(`[Version] 检测到新版本: ${latestVersion}`)
      return true
    }

    return false
  } finally {
    isChecking = false
  }
}

/**
 * 开始版本检测
 * @param {Function} onUpdate - 检测到更新时的回调函数
 * @param {number} interval - 检测间隔（毫秒），默认5分钟
 */
export function startVersionCheck(onUpdate, interval = 5 * 60 * 1000) {
  if (timer) {
    stopVersionCheck()
  }

  onUpdateCallback = onUpdate

  // 首次检测
  checkUpdate().then(hasUpdate => {
    if (hasUpdate && onUpdateCallback) {
      onUpdateCallback()
    }
  })

  // 定期检测
  timer = setInterval(async () => {
    const hasUpdate = await checkUpdate()
    if (hasUpdate && onUpdateCallback) {
      onUpdateCallback()
    }
  }, interval)

  console.log(`[Version] 版本检测已启动，检测间隔: ${interval / 1000}秒`)
}

/**
 * 停止版本检测
 */
export function stopVersionCheck() {
  if (timer) {
    clearInterval(timer)
    timer = null
    onUpdateCallback = null
    console.log('[Version] 版本检测已停止')
  }
}

/**
 * 手动刷新页面
 */
export function refreshPage() {
  window.location.reload()
}
