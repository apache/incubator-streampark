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
  <a-card
    :bordered="false">
    <div
      class="table-page-search-wrapper">
      <a-form
        layout="inline">
        <a-row
          :gutter="48">
          <div
            class="fold">
            <a-col
              :md="8"
              :sm="24">
              <a-form-item
                label="Role"
                :label-col="{span: 4}"
                :wrapper-col="{span: 18, offset: 2}">
                <a-input
                  v-model="queryParams.roleName" />
              </a-form-item>
            </a-col>
            <a-col
              :md="8"
              :sm="24">
              <a-form-item
                label="Create Time"
                :label-col="{span: 4}"
                :wrapper-col="{span: 18, offset: 2}">
                <range-date
                  @change="handleDateChange"
                  ref="createTime" />
              </a-form-item>
            </a-col>

            <a-col
              :md="8"
              :sm="24">
              <span
                class="table-page-search-bar">
                <a-button
                  type="primary"
                  shape="circle"
                  icon="search"
                  @click="search" />
                <a-button
                  type="primary"
                  shape="circle"
                  icon="rest"
                  @click="reset" />
                <a-button
                  v-permit="'role:add'"
                  type="primary"
                  shape="circle"
                  icon="plus"
                  @click="add" />
              </span>
            </a-col>
          </div>
        </a-row>
      </a-form>
    </div>

    <!-- Table Info -->
    <a-table
      ref="TableInfo"
      :columns="columns"
      :data-source="dataSource"
      :pagination="pagination"
      :loading="loading"
      :scroll="{ x: 900 }"
      @change="handleTableChange">
      <template
        slot="remark"
        slot-scope="text">
        <a-popover
          placement="topLeft">
          <template
            slot="content">
            <div
              style="max-width: 200px">
              {{ text }}
            </div>
          </template>
          <p
            style="width: 200px;margin-bottom: 0">
            {{ text }}
          </p>
        </a-popover>
      </template>
      <template
        slot="operation"
        slot-scope="text, record">
        <svg-icon
          v-if="(record.roleName !== 'admin' || userName === 'admin')"
          v-permit="'role:update'"
          name="edit"
          border
          @click.native="edit(record)"
          title="Edit role" />
        <svg-icon
          name="see"
          border
          @click.native="view(record)"
          title="View" />
        <a-popconfirm
          v-permit="'role:delete'"
          v-if="record.roleName !== 'admin'"
          title="Are you sure delete this Role ?"
          cancel-text="No"
          ok-text="Yes"
          @confirm="handleDelete(record)">
          <svg-icon name="remove" border/>
        </a-popconfirm>
      </template>
    </a-table>
    <!-- View role info -->
    <role-info
      @close="handleRoleInfoClose"
      :role-info-visiable="roleInfo.visiable"
      :role-info-data="roleInfo.data" />
    <!-- Add role -->
    <role-add
      @close="handleRoleAddClose"
      @success="handleRoleAddSuccess"
      :role-add-visiable="roleAdd.visiable" />
    <!-- Edit role -->
    <role-edit
      ref="roleEdit"
      :role-info-data="roleInfo.data"
      @close="handleRoleEditClose"
      @success="handleRoleEditSuccess"
      :role-edit-visiable="roleEdit.visiable" />
  </a-card>
</template>

<script>
import RangeDate from '@/components/DateTime/RangeDate'
import RoleAdd from './RoleAdd'
import RoleInfo from './RoleInfo'
import RoleEdit from './RoleEdit'
import SvgIcon from '@/components/SvgIcon'
import { list, remove } from '@/api/role'
import storage from '@/utils/storage'
import {USER_NAME} from '@/store/mutation-types'

export default {
  name: 'Role',
  components: { RangeDate, RoleInfo, RoleAdd, RoleEdit, SvgIcon },
  data () {
    return {
      advanced: false,
      roleInfo: {
        visiable: false,
        data: {}
      },
      roleAdd: {
        visiable: false
      },
      roleEdit: {
        visiable: false
      },
      queryParams: {
        createTimeFrom: '',
        createTimeTo: ''
      },
      dataSource: [],
      sortedInfo: null,
      paginationInfo: null,
      pagination: {
        pageSizeOptions: ['10', '20', '30', '40', '100'],
        defaultCurrent: 1,
        defaultPageSize: 10,
        showQuickJumper: true,
        showSizeChanger: true,
        showTotal: (total, range) => `显示 ${range[0]} ~ ${range[1]} 条记录，共 ${total} 条记录`
      },
      loading: false
    }
  },
  computed: {
    columns () {
      let { sortedInfo } = this
      sortedInfo = sortedInfo || {}
      return [{
        title: 'Role Name',
        dataIndex: 'roleName'
      }, {
        title: 'Description',
        dataIndex: 'remark',
        scopedSlots: { customRender: 'remark' },
        width: 350
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
        scopedSlots: { customRender: 'operation' }
      }]
    },
    userName() {
      return storage.get(USER_NAME)
    }
  },
  mounted () {
    this.fetch()
  },
  methods: {
    add () {
      this.roleAdd.visiable = true
    },
    handleRoleAddClose () {
      this.roleAdd.visiable = false
    },
    handleRoleAddSuccess () {
      this.roleAdd.visiable = false
      this.$message.success('Add role success')
      this.search()
    },
    view (record) {
      this.roleInfo.data = record
      this.roleInfo.visiable = true
    },
    handleRoleInfoClose () {
      this.roleInfo.visiable = false
    },
    edit (record) {
      this.$refs.roleEdit.setFormValues(record)
      this.roleInfo.data = record
      this.roleEdit.visiable = true
    },
    handleRoleEditSuccess () {
      this.roleEdit.visiable = false
      this.$message.success('Edit role success.')
      this.search()
    },
    handleRoleEditClose () {
      this.roleEdit.visiable = false
    },
    handleDateChange (value) {
      if (value) {
        this.queryParams.createTimeFrom = value[0]
        this.queryParams.createTimeTo = value[1]
      }
    },
    handleDelete (record) {
      remove({
        roleId: record.roleId
      }).then((resp) => {
        if ( resp.status === 'success' ) {
          this.$message.success('delete successful')
          this.search()
        }
      })
    },
    search () {
      const { sortedInfo } = this
      let sortField, sortOrder
      if (sortedInfo) {
        sortField = sortedInfo.field
        sortOrder = sortedInfo.order
      }
      this.fetch({
        sortField: sortField,
        sortOrder: sortOrder,
        ...this.queryParams
      })
    },
    reset () {
      // Reset pagination
      this.$refs.TableInfo.pagination.current = this.pagination.defaultCurrent
      if (this.paginationInfo) {
        this.paginationInfo.current = this.pagination.defaultCurrent
        this.paginationInfo.pageSize = this.pagination.defaultPageSize
      }
      // Reset sortedInfo
      this.sortedInfo = null
      // Reset queryParams
      this.queryParams = {}
      // Reset createTime
      this.$refs.createTime.reset()
      this.fetch()
    },
    handleTableChange (pagination, filters, sorter) {
      this.paginationInfo = pagination
      this.sortedInfo = sorter
      this.fetch({
        sortField: sorter.field,
        sortOrder: sorter.order,
        ...this.queryParams
      })
    },
    fetch (params = {}) {
      this.loading = true
      if (this.paginationInfo) {
        this.$refs.TableInfo.pagination.current = this.paginationInfo.current
        this.$refs.TableInfo.pagination.pageSize = this.paginationInfo.pageSize
        params.pageSize = this.paginationInfo.pageSize
        params.pageNum = this.paginationInfo.current
      } else {
        params.pageSize = this.pagination.defaultPageSize
        params.pageNum = this.pagination.defaultCurrent
      }

      if (params.sortField === 'createTime') {
        params.sortField = 'create_time'
      }

      if (params.sortField === 'modifyTime') {
        params.sortField = 'modify_time'
      }

      list({
        ...params
      }).then((resp) => {
        const pagination = { ...this.pagination }
        pagination.total = parseInt(resp.data.total)
        this.dataSource = resp.data.records
        this.pagination = pagination
        this.loading = false
      })
    }
  }
}
</script>
