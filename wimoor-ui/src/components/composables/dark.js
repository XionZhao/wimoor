import { ref, computed, watchEffect } from 'vue'

const STORAGE_KEY = 'vueuse-color-scheme'

function getSystemDark() {
  return window.matchMedia('(prefers-color-scheme: dark)').matches
}

function applyClass(dark) {
  const el = document.documentElement
  if (dark) {
    el.classList.add('dark')
  } else {
    el.classList.remove('dark')
  }
}

const stored = localStorage.getItem(STORAGE_KEY)
const mode = ref(stored || 'auto') // 'auto' | 'dark' | 'light'

const isDark = computed({
  get() {
    if (mode.value === 'auto') return getSystemDark()
    return mode.value === 'dark'
  },
  set(val) {
    mode.value = val ? 'dark' : 'light'
  }
})

watchEffect(() => {
  applyClass(isDark.value)
  localStorage.setItem(STORAGE_KEY, mode.value)
})

// listen to system preference changes
window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
  if (mode.value === 'auto') {
    applyClass(isDark.value)
  }
})

/**
 * 带右上角圆形扩散/收敛动画的主题切换
 * 切到亮色：白色从右上角扩散
 * 切到暗色：白色收敛到右上角
 */
async function toggleDarkWithAnimation() {
  // 不支持 View Transitions API 时直接切换
  if (!document.startViewTransition) {
    isDark.value = !isDark.value
    return isDark.value
  }

  // 右上角坐标
  const cornerX = innerWidth
  const cornerY = 0
  // 能覆盖整个页面的半径
  const maxRadius = Math.hypot(innerWidth, innerHeight)

  // 当前是暗色，即将切到亮色
  const goingLight = isDark.value

  const transition = document.startViewTransition(() => {
    isDark.value = !isDark.value
  })

  transition.ready.then(() => {
    if (goingLight) {
      // 切到亮色：新主题（白色）从右上角扩散展开
      // 新层在上层（z-index:2），从 0 扩散到全屏
      document.documentElement.animate(
        {
          clipPath: [
            `circle(0px at ${cornerX}px ${cornerY}px)`,
            `circle(${maxRadius}px at ${cornerX}px ${cornerY}px)`
          ],
          zIndex: ['2', '2']
        },
        {
          duration: 500,
          easing: 'ease-out',
          pseudoElement: '::view-transition-new(root)'
        }
      )
      // 旧层（黑色）保持全屏在下层
      document.documentElement.animate(
        {
          zIndex: ['1', '1']
        },
        {
          duration: 500,
          pseudoElement: '::view-transition-old(root)'
        }
      )
    } else {
      // 切到暗色：旧主题（白色）收缩到右上角消失
      // 旧层在上层（z-index:2），从全屏收缩到 0
      document.documentElement.animate(
        {
          clipPath: [
            `circle(${maxRadius}px at ${cornerX}px ${cornerY}px)`,
            `circle(0px at ${cornerX}px ${cornerY}px)`
          ],
          zIndex: ['2', '2']
        },
        {
          duration: 500,
          easing: 'ease-in',
          pseudoElement: '::view-transition-old(root)'
        }
      )
      // 新层（黑色）保持全屏在下层
      document.documentElement.animate(
        {
          zIndex: ['1', '1']
        },
        {
          duration: 500,
          pseudoElement: '::view-transition-new(root)'
        }
      )
    }
  })

  return isDark.value
}

function useToggle(refVal) {
  return () => {
    refVal.value = !refVal.value
    return refVal.value
  }
}

export { isDark }
export const toggleDark = useToggle(isDark)
export { toggleDarkWithAnimation }
export { useToggle }
