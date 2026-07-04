/**
 * 统一化存储单位，字节转化为英文缩写`bytes`, `KB`, `MB`, `GB`
 *
 * @param {number} bytes 需要转换的字节大小
 * @returns {string} 转化后的字节字符串
 * @example
 * ```
 * // Output: '1 MB'
 * normalizeSizeUnits(1048576)
 * ```
 */
export function normalizeSizeUnits(bytes: number): string {
  if (bytes === 0)
    return '0 bytes'

  const units = ['bytes', 'KB', 'MB', 'GB']
  const index = Math.floor(Math.log(bytes) / Math.log(1024))
  const size = +(bytes / 1024 ** index).toFixed(2)
  const unit = units[index]

  return `${size} ${unit}`
}
