<template>
  <a-tree-select
    :allowClear="true"
    :dropdownStyle="{ maxHeight: '220px', overflow: 'auto' }"
    :treeData="deptTreeData"
    v-model="value">
  </a-tree-select>
</template>

<script>
import { list } from '@/api/dept'

export default {
  name: 'DeptTree',
  data () {
    return {
      deptTreeData: [],
      value: undefined
    }
  },
  methods: {
    reset () {
      this.value = ''
    }
  },
  mounted () {
    list().then((r) => {
      this.deptTreeData = r.rows.children
    })
  },
  watch: {
    value (value) {
      this.$emit('change', value)
    }
  }
}
</script>
