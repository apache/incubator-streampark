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
                label="Team Name"
                :label-col="{span: 4}"
                :wrapper-col="{span: 18, offset: 2}">
                <a-input
                  v-model="queryParams.teamName"/>
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
                v-permit="'team:add'"
                @click="handleTeamAdd"/>
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
        slot-scope="text, team">
        <svg-icon
          v-permit="'team:update'"
          name="edit"
          border
          @click.native="handleTeamEdit(team)"
          title="modify"/>
        <a-popconfirm
          v-permit="'team:delete'"
          title="Are you sure delete this team ?"
          cancel-text="No"
          ok-text="Yes"
          @confirm="handleTeamDelete(team)">
          <svg-icon name="remove" border/>
        </a-popconfirm>
      </template>
    </a-table>

    <!-- Add team -->
    <team-add
      @close="handleTeamAddClose"
      @success="handleTeamAddSuccess"
      :visible="teamAdd.visible"/>
    <!-- Edit team -->
    <team-edit
      ref="teamEdit"
      @close="handleTeamEditClose"
      @success="handleTeamEditSuccess"
      :visible="teamEdit.visible"/>
  </a-card>
</template>

<script>
import TeamAdd from './TeamAdd'
import TeamEdit from './TeamEdit'
import RangeDate from '@/components/DateTime/RangeDate'
import SvgIcon from '@/components/SvgIcon'

import {list, remove} from '@/api/team'

export default {
  name: 'Team',
  components: {TeamAdd, TeamEdit, RangeDate, SvgIcon},
  data() {
    return {
      teamAdd: {
        visible: false
      },
      teamEdit: {
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
    columns() {
      let {sortedInfo} = this
      sortedInfo = sortedInfo || {}
      return [{
        title: 'Team Name',
        dataIndex: 'teamName',
        sorter: true,
        sortOrder: sortedInfo.columnKey === 'teamName' && sortedInfo.order
      }, {
        title: 'Description',
        dataIndex: 'description',
        scopedSlots: {customRender: 'description'},
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
        scopedSlots: {customRender: 'operation'}
      }]
    }
  },

  mounted() {
    this.fetch()
  },

  methods: {
    handleTeamAdd() {
      this.teamAdd.visible = true
    },
    handleTeamAddClose() {
      this.teamAdd.visible = false
    },
    handleTeamAddSuccess() {
      this.teamAdd.visible = false
      this.$message.success('add team successfully')
      this.search()
    },
    handleTeamEdit(record) {
      this.$refs.teamEdit.setFormValues(record)
      this.teamEdit.visible = true
    },
    handleTeamEditClose() {
      this.teamEdit.visible = false
    },
    handleTeamEditSuccess() {
      this.teamEdit.visible = false
      this.$message.success('modify team successfully')
      this.search()
    },
    handleQueryDateChange(value) {
      if (value) {
        this.queryParams.createTimeFrom = value[0]
        this.queryParams.createTimeTo = value[1]
      }
    },
    handleTeamDelete(team) {
      remove({
        id: team.id
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

      if (params.sortField === 'teamName') {
        params.sortField = 'team_name'
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
