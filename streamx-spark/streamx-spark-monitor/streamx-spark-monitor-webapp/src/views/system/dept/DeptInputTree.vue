<template>
  <a-tree-select
    :allowClear="true"
    :dropdownStyle="{ maxHeight: '220px', overflow: 'auto' }"
    :treeData="deptTreeData"
    v-model="value">
  </a-tree-select>
</template>

<script>
export default {
  name: 'DetpInputTree',
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
    this.$get('dept').then((r) => {
      this.deptTreeData = r.data.rows.children
    })
  },
  watch: {
    value (value) {
      this.$emit('change', value)
    }
  }
}
</script>
