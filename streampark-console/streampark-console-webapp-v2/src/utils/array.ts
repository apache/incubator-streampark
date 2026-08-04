/** Converts a flat list with `id` and `pid` fields into a tree. */
export function arrayToTree(arr: any[]) {
    const res: any = []
    const map = new Map()

    arr.forEach((item) => {
        map.set(item.id, item)
    })

    arr.forEach((item) => {
        const parent = item.pid && map.get(item.pid)
        if (parent) {
            if (parent?.children) parent.children.push(item)
            else parent.children = [item]
        } else {
            res.push(item)
        }
    })
    return res
}
