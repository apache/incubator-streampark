<template>
  <div class="img-check-box" @click="toggle">
    <img :src="img" />
    <div v-if="sChecked" class="check-item">
      <a-icon type="check" />
    </div>
  </div>
</template>

<script>
const Group = {
  name: 'ImgCheckboxGroup',
  props: {
    multiple: {
      type: Boolean,
      required: false,
      default: false
    },
    defaultValues: {
      type: Array,
      required: false,
      default: () => []
    }
  },
  data () {
    return {
      values: [],
      options: []
    }
  },
  provide () {
    return {
      groupContext: this
    }
  },
  watch: {
    'values': function (newVal, oldVal) {
      // 此条件是为解决单选时，触发两次chang事件问题
      if (!(newVal.length === 1 && oldVal.length === 1 && newVal[0] === oldVal[0])) {
        this.$emit('change', this.values)
      }
    }
  },
  methods: {
    handleChange (option) {
      if (!option.checked) {
        this.values = this.values.filter(item => item !== option.value)
      } else {
        if (!this.multiple) {
          this.values = [option.value]
          this.options.forEach(item => {
            if (item.value !== option.value) {
              item.sChecked = false
            }
          })
        } else {
          this.values.push(option.value)
        }
      }
    }
  },
  render (h) {
    return h(
      'div',
      {
        attrs: {style: 'display: flex'}
      },
      [this.$slots.default]
    )
  }
}

export default {
  name: 'ImgCheckbox',
  Group,
  props: {
    checked: {
      type: Boolean,
      required: false,
      default: false
    },
    img: {
      type: String,
      required: true
    },
    value: {
      required: true
    }
  },
  data () {
    return {
      sChecked: this.checked
    }
  },
  inject: ['groupContext'],
  watch: {
    'sChecked': function (val) {
      const option = {
        value: this.value,
        checked: this.sChecked
      }
      this.$emit('change', option)
      const groupContext = this.groupContext
      if (groupContext) {
        groupContext.handleChange(option)
      }
    }
  },
  created () {
    const groupContext = this.groupContext
    if (groupContext) {
      this.sChecked = groupContext.defaultValues.length > 0 ? groupContext.defaultValues.indexOf(this.value) >= 0 : this.sChecked
      groupContext.options.push(this)
    }
  },
  methods: {
    toggle () {
      if (this.sChecked) {
        return
      }
      this.sChecked = !this.sChecked
    }
  }
}
</script>

<style lang="less" scoped>
  .img-check-box{
    margin-right: 16px;
    position: relative;
    border-radius: 4px;
    cursor: pointer;
    .check-item{
      position: absolute;
      top: 0;
      right: 0;
      width: 100%;
      padding-top: 15px;
      padding-left: 24px;
      height: 100%;
      color: #1890ff;
      font-size: 14px;
      font-weight: bold;
    }
  }
</style>
