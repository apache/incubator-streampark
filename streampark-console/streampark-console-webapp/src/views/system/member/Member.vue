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
  <a-card :bordered="false">
    <div
      class="table-page-search-wrapper">
      <a-form
        layout="inline">
        <a-row>
          <div
            class="fold">
            <a-col
              :md="4">
              <a-form-item
                label="User Name"
                :label-col="{span: 4}"
                :wrapper-col="{span: 18}">
                <a-input
                  style="width: 90%"
                  v-model="queryParams.userName"/>
              </a-form-item>
            </a-col>
            <a-col
              :md="3">
              <a-form-item
                label="Role"
                v-bind="formItemLayout">
                <a-select
                  mode="single"
                  :allow-clear="true"
                  style="width: 80%"
                  @change="handleQueryRoleChange"
                  v-decorator="['roleName']">
                  <a-select-option
                    v-for="r in roleData"
                    :key="r.roleName">
                    {{ r.roleName }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <template>
              <a-col
                :md="8"
                :sm="24">
                <a-form-item
                  label="Create Time"
                  :label-col="{span: 4}"
                  :wrapper-col="{span: 18, offset: 2}">
                  <range-date
                    @change="handleQueryDateChange"
                    ref="createTime"/>
                </a-form-item>
              </a-col>
            </template>
          </div>
          <a-col
            :md="8"
            :sm="24">
            <span
              class="table-page-search-bar">
              <a-button
                type="primary"
                shape="circle"
                icon="search"
                @click="search"/>
              <a-button
                type="primary"
                shape="circle"
                icon="rest"
                @click="reset"/>
              <a-button
                type="primary"
                shape="circle"
                icon="plus"
                v-permit="'member:add'"
                @click="handleMemberAdd"/>
            </span>
          </a-col>
        </a-row>
      </a-form>
    </div>

    <!-- Table -->
    <a-table
      ref="TableInfo"
      :columns="columns"
      :data-source="dataSource"
      :pagination="pagination"
      :loading="loading"
      :scroll="{ x: 900 }"
      @change="handleTableChange">
      <template
        slot="operation"
        slot-scope="text, member">
        <svg-icon
          v-permit="'member:update'"
          name="edit"
          border
          @click.native="handleMemberEdit(member)"
          title="modify"/>
        <a-popconfirm
          v-permit="'member:delete'"
          title="Are you sure delete this member ?"
          cancel-text="No"
          ok-text="Yes"
          @confirm="handleMemberDelete(member)">
          <svg-icon name="remove" border/>
        </a-popconfirm>
      </template>
    </a-table>

    <!-- Add member -->
    <member-add
      @close="handleMemberAddClose"
      @success="handleMemberAddSuccess"
      :visible="memberAdd.visible"/>
    <!-- Edit member -->
    <member-edit
      ref="memberEdit"
      @close="handleMemberEditClose"
      @success="handleMemberEditSuccess"
      :visible="memberEdit.visible"/>
  </a-card>
</template>

<script>
import MemberAdd from './MemberAdd'
import MemberEdit from './MemberEdit'
import RangeDate from '@/components/DateTime/RangeDate'
import SvgIcon from '@/components/SvgIcon'

import {list, remove} from '@/api/member'
import {list as getRole} from '@/api/role'

const formItemLayout = {
  labelCol: { span: 4 },
  wrapperCol: { span: 18 }
}
export default {
  name: 'Member',
  components: {MemberAdd, MemberEdit, RangeDate, SvgIcon},
  data() {
    return {
      memberAdd: {
        visible: false
      },
      memberEdit: {
        visible: false
      },
      formItemLayout,
      roleData: [],
      roleName: null,
      queryParams: {},
      filteredInfo: null,
      sortedInfo: null,
      paginationInfo: null,
      dataSource: [],
      loading: false,
      pagination: {
        pageSizeOptions: ['10', '20', '30', '40', '100'],
        defaultCurrent: 1,
        defaultPageSize: 10,
        showQuickJumper: true,
        showSizeChanger: true,
        showTotal: (total, range) => `display ${range[0]} ~ ${range[1]} records，total ${total}`
      }
    }
  },
  computed: {
    columns() {
      let {sortedInfo} = this
      sortedInfo = sortedInfo || {}
      return [{
        title: 'User Name',
        dataIndex: 'userName',
        sorter: true,
        sortOrder: sortedInfo.columnKey === 'userName' && sortedInfo.order
      }, {
        title: 'Role Name',
        dataIndex: 'roleName',
        sorter: true,
        sortOrder: sortedInfo.columnKey === 'roleName' && sortedInfo.order
      }, {
        title: 'Create Time',
        dataIndex: 'createTime',
        sorter: true,
        sortOrder: sortedInfo.columnKey === 'createTime' && sortedInfo.order
      }, {
        title: 'Modify Time',
        dataIndex: 'modifyTime',
        sorter: true,
        sortOrder: sortedInfo.columnKey === 'modifyTime' && sortedInfo.order
      }, {
        title: 'Operation',
        dataIndex: 'operation',
        scopedSlots: {customRender: 'operation'}
      }]
    }
  },

  mounted() {
    this.fetch()
    getRole(
      {'pageSize': '9999'}
    ).then((resp) => {
      this.roleData = resp.data.records
    })
  },

  methods: {
    handleMemberAdd() {
      this.memberAdd.visible = true
    },
    handleMemberAddClose() {
      this.memberAdd.visible = false
    },
    handleMemberAddSuccess() {
      this.memberAdd.visible = false
      this.$message.success('add member successfully')
      this.search()
    },
    handleMemberEdit(record) {
      this.$refs.memberEdit.setFormValues(record)
      this.memberEdit.visible = true
    },
    handleMemberEditClose() {
      this.memberEdit.visible = false
    },
    handleMemberEditSuccess() {
      this.memberEdit.visible = false
      this.$message.success('modify member successfully')
      this.search()
    },
    handleQueryRoleChange(roleName) {
      this.roleName = roleName
      this.search()
    },
    handleQueryDateChange(value) {
      if (value) {
        this.queryParams.createTimeFrom = value[0]
        this.queryParams.createTimeTo = value[1]
      }
    },
    handleMemberDelete(member) {
      remove({
        id: member.id
      }).then((resp) => {
        if (resp.status === 'success') {
          this.$message.success('delete successful')
          this.search()
        } else {
          this.$message.error('delete failed')
        }
      })
    },

    search() {
      const {sortedInfo, filteredInfo} = this
      let sortField, sortOrder
      if (sortedInfo) {
        sortField = sortedInfo.field
        sortOrder = sortedInfo.order
      }
      this.fetch({
        sortField: sortField,
        sortOrder: sortOrder,
        ...this.queryParams,
        ...filteredInfo
      })
    },
    reset() {
      // reset pagination
      this.$refs.TableInfo.pagination.current = this.pagination.defaultCurrent
      if (this.paginationInfo) {
        this.paginationInfo.current = this.pagination.defaultCurrent
        this.paginationInfo.pageSize = this.pagination.defaultPageSize
      }
      // reset filteredInfo, sortedInfo and queryParams
      this.filteredInfo = null
      this.sortedInfo = null
      this.queryParams = {}
      this.$refs.createTime.reset()
      this.fetch()
    },
    handleTableChange(pagination, filters, sorter) {
      this.paginationInfo = pagination
      this.filteredInfo = filters
      this.sortedInfo = sorter
      this.fetch({
        sortField: sorter.field,
        sortOrder: sorter.order,
        ...this.queryParams,
        ...filters
      })
    },
    fetch(params = {}) {
      this.loading = true
      if (this.paginationInfo) {
        // Set pagination
        this.$refs.TableInfo.pagination.current = this.paginationInfo.current
        this.$refs.TableInfo.pagination.pageSize = this.paginationInfo.pageSize
        params.pageSize = this.paginationInfo.pageSize
        params.pageNum = this.paginationInfo.current
      } else {
        // Set the default pagination
        params.pageSize = this.pagination.defaultPageSize
        params.pageNum = this.pagination.defaultCurrent
      }
      if (params.status != null && params.status.length > 0) {
        params.status = params.status[0]
      } else {
        delete params.status
      }

      if (params.sortField === 'roleName') {
        params.sortField = 'role_name'
      }

      if (params.sortField === 'createTime') {
        params.sortField = 'create_time'
      }

      if (params.sortField === 'modifyTime') {
        params.sortField = 'modify_time'
      }

      list({
        ...params,
        roleName: this.roleName
      }).then((resp) => {
        const pagination = {...this.pagination}
        pagination.total = parseInt(resp.data.total)
        this.dataSource = resp.data.records
        this.pagination = pagination
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    }
  }
}
</script>
