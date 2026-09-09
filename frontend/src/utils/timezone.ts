/**
 * 时区检测（跨端兼容说明）：
 *   - PC / 移动浏览器 / Android WebView：使用 Intl API，无需申请任何权限；
 *   - 小程序：请替换为 `wx.getSystemInfoSync().timezone`（如 tz 格式不一致需转换为 IETF 格式）；
 *   - Android 原生：可用 `TimeZone.getDefault().getID()` 传入。
 */
export function detectTimezone(): string {
  try {
    const zone = Intl.DateTimeFormat().resolvedOptions().timeZone
    return zone || 'Asia/Shanghai'
  } catch {
    return 'Asia/Shanghai'
  }
}
