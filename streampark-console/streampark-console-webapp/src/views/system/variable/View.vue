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
        <a-row
          :gutter="48">
          <div
            class="fold">
            <a-col
              :md="8"
              :sm="24">
              <a-form-item
                label="Variable Code"
                :label-col="{span: 4}"
                :wrapper-col="{span: 18, offset: 2}">
                <a-input
                  v-model="queryParams.variableCode" />
              </a-form-item>
            </a-col>
            <a-col
              :md="8"
              :sm="24">
              <a-form-item
                label="Description"
                :label-col="{span: 4}"
                :wrapper-col="{span: 18, offset: 2}">
                <a-input
                  v-model="queryParams.description" />
              </a-form-item>
            </a-col>
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
                @click="search" />
              <a-button
                type="primary"
                shape="circle"
                icon="rest"
                @click="reset" />
              <a-button
                type="primary"
                shape="circle"
                icon="plus"
                v-permit="'variable:add'"
                @click="handleAdd" />
            </span>
          </a-col>
        </a-row>
      </a-form>
    </div>

    <!-- 表格区域 -->
    <a-table
      ref="TableInfo"
      :columns="columns"
      :data-source="dataSource"
      :pagination="pagination"
      :loading="loading"
      :scroll="{ x: 900 }"
      @change="handleTableChange">
      <template
        slot="email"
        slot-scope="text">
        <a-popover
          placement="topLeft">
          <template
            slot="content">
            <div>
              {{ text }}
            </div>
          </template>
          <p
            style="width: 150px;margin-bottom: 0">
            {{ text }}
          </p>
        </a-popover>
      </template>
      <template
        slot="operation"
        slot-scope="text, record">
        <svg-icon
          v-permit="'variable:update'"
          name="edit"
          border
          @click.native="handleEdit(record)"
          title="modify" />
        <svg-icon
          name="see"
          border
          @click.native="handleView(record)"
          title="view" />
        <a-popconfirm
          v-permit="'variable:delete'"
          title="Are you sure delete this variable ?"
          cancel-text="No"
          ok-text="Yes"
          @confirm="handleDelete(record)">
          <svg-icon name="remove" border/>
        </a-popconfirm>
      </template>
    </a-table>

    <!-- view variable -->
    <variable-info
      :data="variableInfo.data"
      :visible="variableInfo.visible"
      @close="handleVariableInfoClose" />
    <!-- add variable -->
    <variable-add
      @close="handleVariableAddClose"
      @success="handleVariableAddSuccess"
      :visible="variableAdd.visible" />
    <!-- edit variable -->
    <variable-edit
      ref="variableEdit"
      @close="handleVariableEditClose"
      @success="handleVariableEditSuccess"
      :visible="variableEdit.visible" />
  </a-card>
</template>

<script>
import VariableInfo from './Detail'
import VariableAdd from './Add'
import VariableEdit from './Edit'
import SvgIcon from '@/components/SvgIcon'
import { list, deleteVariable} from '@/api/variable'

export default {
  name: 'Variable',
  components: { VariableInfo, VariableAdd, VariableEdit, SvgIcon },
  data () {
    return {
      variableInfo: {
        visible: false,
        data: {}
      },
      variableAdd: {
        visible: false
      },
      variableEdit: {
        visible: false
      },
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
    columns () {
      let { sortedInfo, filteredInfo } = this  // eslint-disable-line no-unused-vars
      sortedInfo = sortedInfo || {}
      filteredInfo = filteredInfo || {}
      return [{
        title: 'Variable Code',
        dataIndex: 'variableCode',
        sorter: true,
        sortOrder: sortedInfo.columnKey === 'variableCode' && sortedInfo.order
      }, {
        title: 'Variable Value',
        dataIndex: 'variableValue'
      }, {
        title: 'Description',
        dataIndex: 'description'
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
    }
  },

  mounted () {
    this.fetch()
  },

  methods: {
    handleView (record) {
      this.variableInfo.data = record
      this.variableInfo.visible = true
    },
    handleAdd () {
      this.variableAdd.visible = true
    },
    handleVariableAddClose () {
      this.variableAdd.visible = false
    },
    handleVariableAddSuccess () {
      this.variableAdd.visible = false
      this.$message.success('add variable successfully')
      this.search()
    },
    handleEdit (record) {
      this.$refs.variableEdit.setFormValues(record)
      this.variableEdit.visible = true
    },
    handleVariableEditClose () {
      this.variableEdit.visible = false
    },
    handleVariableEditSuccess () {
      this.variableEdit.visible = false
      this.$message.success('modify variable successfully')
      this.search()
    },
    handleVariableInfoClose () {
      this.variableInfo.visible = false
    },
    handleDelete (record) {
      deleteVariable({
        id: record.id,
        teamId : record.teamId,
        variableCode : record.variableCode,
        variableValue : record.variableValue
      }).then((resp) => {
        if (resp.status === 'success') {
          this.$message.success('delete successful')
          this.search()
        } else {
          this.$swal.fire(
            'Failed',
            resp.message,
            'error'
          )
        }
      })
    },
    search () {
      const { sortedInfo, filteredInfo } = this
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
    reset () {
      this.$refs.TableInfo.pagination.current = this.pagination.defaultCurrent
      if (this.paginationInfo) {
        this.paginationInfo.current = this.pagination.defaultCurrent
        this.paginationInfo.pageSize = this.pagination.defaultPageSize
      }
      this.filteredInfo = null
      this.sortedInfo = null
      this.queryParams = {}
      this.$refs.createTime.reset()
      this.fetch()
    },
    handleTableChange (pagination, filters, sorter) {
      this.paginationInfo = pagination
      this.filteredInfo = filters
      this.sortedInfo = sorter
      this.variableInfo.visible = false
      this.fetch({
        sortField: sorter.field,
        sortOrder: sorter.order,
        ...this.queryParams,
        ...filters
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
      if(params.status != null && params.status.length>0) {
        params.status = params.status[0]
      } else {
        delete params.status
      }

      if (params.sortField === 'createTime') {
        params.sortField = 'create_time'
      }

      list({ ...params }).then((resp) => {
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
