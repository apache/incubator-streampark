<template>
  <div class="theme-color" :style="{backgroundColor: color}" @click="toggle">
    <a-icon v-if="sChecked" type="check" />
  </div>
</template>

<script>
const Group = {
  name: 'ColorCheckboxGroup',
  props: {
    defaultValues: {
      required: false
    },
    multiple: {
      type: Boolean,
      required: false,
      default: false
    }
  },
  data () {
    return {
      values: [],
      options: []
    }
  },
  computed: {
    colors () {
      let colors = []
      this.options.forEach(item => {
        if (item.sChecked) {
          colors.push(item.color)
        }
      })
      return colors
    }
  },
  provide () {
    return {
      groupContext: this
    }
  },
  watch: {
    values: function (newVal, oldVal) {
      if (!(newVal.length === 1 && oldVal.length === 1 && newVal[0] === oldVal[0]) || this.multiple) {
        this.$emit('change', this.values, this.colors)
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
    const clear = h('div', {attrs: {style: 'clear: both'}})
    return h(
      'div',
      {},
      [this.$slots.default, clear]
    )
  }
}

export default {
  name: 'ColorCheckbox',
  Group: Group,
  props: {
    color: {
      required: true
    },
    value: {
      required: true
    },
    checked: {
      required: false,
      default: false
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
      const value = {
        value: this.value,
        color: this.color,
        checked: this.sChecked
      }
      this.$emit('change', value)
      const groupContext = this.groupContext
      if (groupContext) {
        groupContext.handleChange(value)
      }
    }
  },
  created () {
    const groupContext = this.groupContext
    if (groupContext) {
      this.sChecked = groupContext.defaultValues.indexOf(this.value) >= 0
      groupContext.options.push(this)
    }
  },
  methods: {
    toggle () {
      this.sChecked = !this.sChecked
    }
  }
}
</script>

<style lang="less" scoped>
  .theme-color{
    float: left;
    width: 20px;
    height: 20px;
    border-radius: 2px;
    cursor: pointer;
    margin-right: 8px;
    text-align: center;
    color: #fff;
    font-weight: bold;
    margin-top: .5rem;
  }
</style>
