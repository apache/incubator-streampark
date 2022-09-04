<template>
  <a-range-picker
    :key="id"
    ref="rangeDate"
    @change="onChange"
    style="width: 100%" />
</template>

<script>
export default {
  name: 'RangeDate',
  data () {
    return {
      id: +new Date()
    }
  },
  methods: {
    onChange (date, dateString) {
      this.$emit('change', dateString)
    },
    reset () {
      this.id = +new Date()
    }
  }
}
</script>
