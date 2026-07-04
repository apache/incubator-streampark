import { fetchDictList } from '@/service'
import { session } from '@/utils'

export const useDictStore = defineStore('dict-store', {
  state: () => {
    return {
      dictMap: {} as DictMap,
      isInitDict: false,
    }
  },
  actions: {
    async dict(code: string) {
      // 调用前初始化
      if (!this.dictMap) {
        this.initDict()
      }

      const targetDict = await this.getDict(code)

      return {
        data: () => targetDict,
        enum: () => Object.fromEntries(targetDict.map(({ value, label }) => [value, label])),
        valueMap: () => Object.fromEntries(targetDict.map(({ value, ...data }) => [value, data])),
        labelMap: () => Object.fromEntries(targetDict.map(({ label, ...data }) => [label, data])),
      }
    },
    async getDict(code: string) {
      const isExist = Reflect.has(this.dictMap, code)

      if (isExist) {
        return this.dictMap[code]
      }
      else {
        return await this.getDictByNet(code)
      }
    },

    async getDictByNet(code: string) {
      const { data, isSuccess } = await fetchDictList(code)
      if (isSuccess) {
        Reflect.set(this.dictMap, code, data)
        // 同步至session
        session.set('dict', this.dictMap)
        return data
      }
      else {
        throw new Error(`Failed to get ${code} dictionary from network, check ${code} field or network`)
      }
    },
    initDict() {
      const dict = session.get('dict')
      if (dict) {
        Object.assign(this.dictMap, dict)
      }
      this.isInitDict = true
    },
  },
})
