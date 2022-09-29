<!--

    Licensed to the Apache Software Foundation (ASF) under one or more
    contributor license agreements.  See the NOTICE file distributed with
    this work for additional information regarding copyright ownership.
    The ASF licenses this file to You under the Apache License, Version 2.0
    (the "License"); you may not use this file except in compliance with
    the License.  You may obtain a copy of the License at

       https://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

-->

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
    // The default parent node is "/"
    deepList(data) {
      data.map((item) => {
        if (item.children && item.children.length >0) {
          this.deepList(item.children)
        } else {
          // Store all leaf nodes
          this.leftNodes.push (item.id)
        }
      })
    }
  },
  watch: {
    roleInfoVisiable () {
      if (this.roleInfoVisiable) {
        getMenu().then((r) => {
          // get all leaf nodes
          this.deepList(r.data.rows.children)
          this.menuTreeData = r.data.rows.children
          roleMenu({
            roleId: this.roleInfoData.roleId
          }).then((resp) => {
            // The data returned in the background is intersected with the leaf node to get the selected child node
            const result = [...new Set(this.leftNodes)].filter((item) => new Set(eval(resp.data)).has(item))
            // Assign the result to the v-model bound property
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
