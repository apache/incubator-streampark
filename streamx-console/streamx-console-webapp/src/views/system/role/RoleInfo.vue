<template>
  <a-drawer
    title="角色信息"
    :maskClosable="false"
    width="650"
    placement="right"
    :closable="true"
    @close="close"
    :visible="roleInfoVisiable"
    style="height: calc(100% - 55px);overflow: auto;padding-bottom: 53px;">
    <p>
      <a-icon type="crown"/>&nbsp;&nbsp;角色名称：{{ roleInfoData.roleName }}
    </p>
    <p :title="roleInfoData.remark">
      <a-icon type="book"/>&nbsp;&nbsp;角色描述：{{ roleInfoData.remark }}
    </p>
    <p>
      <a-icon type="clock-circle"/>&nbsp;&nbsp;创建时间：{{ roleInfoData.createTime }}
    </p>
    <p>
      <a-icon type="clock-circle"/>&nbsp;&nbsp;修改时间：{{ roleInfoData.modifyTime? roleInfoData.modifyTime : '暂未修改' }}
    </p>
    <p>
      <a-icon type="trophy"/>&nbsp;&nbsp;所拥有的权限：
      <a-tree
        :key="key"
        :checkStrictly="true"
        :checkable="true"
        :defaultCheckedKeys="checkedKeys[0]"
        :defaultExpandedKeys="checkedKeys[0]"
        :treeData="menuTreeData">
      </a-tree>
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
      require: true,
      default: false
    },
    roleInfoData: {
      require: true
    }
  },
  data () {
    return {
      key: +new Date(),
      loading: true,
      checkedKeys: [],
      menuTreeData: []
    }
  },
  methods: {
    close () {
      this.$emit('close')
      this.checkedKeys = []
    }
  },
  watch: {
    roleInfoVisiable () {
      if (this.roleInfoVisiable) {
        getMenu().then((resp) => {
          this.menuTreeData = resp.rows.children
          roleMenu({
            roleId: this.roleInfoData.roleId
          }).then((resp) => {
            const length = this.checkedKeys.length
            this.checkedKeys.splice(0, length, resp)
            this.key = +new Date()
          })
        })
      }
    }
  }
}
</script>
