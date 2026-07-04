/**
 * 将给定的数组转换为树形结构。
 * @param arr - 原始数组，其中每个元素包含id和pid属性，pid表示父级id。
 * @returns 返回转换后的树形结构数组。
 */
export function arrayToTree(arr: any[]) {
  // 初始化结果数组
  const res: any = []
  // 使用Map存储数组元素，以id为键，元素本身为值
  const map = new Map()

  // 遍历数组，将每个元素以id为键存储到Map中
  arr.forEach((item) => {
    map.set(item.id, item)
  })

  // 再次遍历数组，根据pid将元素组织成树形结构
  arr.forEach((item) => {
    // 获取当前元素的父级元素
    const parent = item.pid && map.get(item.pid)
    // 如果有父级元素
    if (parent) {
      // 如果父级元素已有子元素，则将当前元素追加到子元素数组中
      if (parent?.children)
        parent.children.push(item)
      // 如果父级元素没有子元素，则创建子元素数组，并将当前元素作为第一个元素
      else
        parent.children = [item]
    }
    // 如果没有父级元素，则将当前元素直接添加到结果数组中
    else {
      res.push(item)
    }
  })
  // 返回组织好的树形结构数组
  return res
}
