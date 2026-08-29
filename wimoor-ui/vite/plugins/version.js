import { writeFileSync } from 'fs'
import { resolve } from 'path'

export default function createVersionPlugin() {
  return {
    name: 'vite-plugin-version',
    enforce: 'post',
    buildStart() {
      // 生成版本号：时间戳 + 随机字符串
      const version = `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
      const versionInfo = {
        version,
        timestamp: Date.now(),
        buildTime: new Date().toISOString()
      }

      // 写入版本文件到public目录
      const versionPath = resolve(process.cwd(), 'public/version.json')
      writeFileSync(versionPath, JSON.stringify(versionInfo, null, 2))
      console.log(`\n[version] Version file generated: ${version}`)
    }
  }
}
