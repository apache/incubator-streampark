<template>
  <a-drawer
    :mask-closable="false"
    width="650"
    placement="right"
    :closable="true"
    @close="close"
    :visible="roleInfoVisiable"
    style="height: calc(100% - 55px);overflow: auto;padding-bottom: 53px;">
    <template slot="title">
      <a-icon type="smile" />
      Role Info
    </template>
    <p>
      <a-icon
        type="crown" />&nbsp;&nbsp;Role Name：{{ roleInfoData.roleName }}
    </p>
    <p
      :title="roleInfoData.remark">
      <a-icon
        type="book" />&nbsp;&nbsp;Description：{{ roleInfoData.remark }}
    </p>
    <p>
      <a-icon
        type="clock-circle" />&nbsp;&nbsp;Create Time：{{ roleInfoData.createTime }}
    </p>
    <p>
      <a-icon
        type="clock-circle" />&nbsp;&nbsp;Update Time：{{ roleInfoData.modifyTime? roleInfoData.modifyTime : '暂未修改' }}
    </p>
    <p>
      <a-icon
        type="trophy" />&nbsp;&nbsp;Permission：
      <a-tree
        :key="key"
        :check-strictly="false"
        :checkable="true"
        :default-checked-keys="checkedKeys[0]"
        :default-expanded-keys="checkedKeys[0]"
        :tree-data="menuTreeData" />
    </p>
  </a-drawer>
</template>
<script>
import { list as getMenu } from '@/api/menu'
import { roleMenu } from '@/api/role'

export default {
  name: 'RoleInfo',
  props: {
    roleInfoVisiable: {
      type: Boolean,
      require: true,
      default: false
    },
    roleInfoData: {
      type: Object,
      default: () => ({}),
      require: true
    }
  },
  data () {
    return {
      key: +new Date(),
      loading: true,
      checkedKeys: [],
      menuTreeData: [],
      selectedKeysAndHalfCheckedKeys:[],
      leftNodes: []
    }
  },
  methods: {
    close () {
      this.$emit('close')
      this.checkedKeys = []
    },
    // 默认父节点为 "/"
    deepList(data) {
      data.map((item) => {
        if (item.children && item.children.length >0) {
          this.deepList(item.children)
        } else {
          // 存放所有叶子节点
          this.leftNodes.push (item.id)
        }
      })
    }
  },
  watch: {
    roleInfoVisiable () {
      if (this.roleInfoVisiable) {
        getMenu().then((r) => {
          // 得到所有叶子节点
          this.deepList(r.data.rows.children)
          this.menuTreeData = r.data.rows.children
          roleMenu({
            roleId: this.roleInfoData.roleId
          }).then((resp) => {
            // 后台返回的数据与叶子节点做交集,得到选中的子节点
            const result = [...new Set(this.leftNodes)].filter((item) => new Set(eval(resp.data)).has(item))
            //将结果赋值给v-model绑定的属性
            const selectedKey = [...result]
            const length = this.checkedKeys.length
            this.checkedKeys.splice(0, length, selectedKey)
            this.key = +new Date()
          })
        })
      }
    }
  }
}
</script>
