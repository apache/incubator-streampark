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
  <a-card :bordered="false" style="margin-top: 20px;">
    <a-descriptions
      bordered
      size="middle"
      layout="vertical">
      <template slot="title">
        <span class="app-bar">Variable "{{ this.variable }}" used list</span>
        <a-button
          type="primary"
          shape="circle"
          icon="arrow-left"
          @click="handleGoBack()"
          style="float: right;margin-top: -8px"/>
        <a-divider
          style="margin-top: 5px;margin-bottom: -5px"/>
      </template>
    </a-descriptions>

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
        slot="jobName"
        slot-scope="text, record">
        <span
          class="app_type app_jar"
          v-if="record['jobType'] === 1">
          JAR
        </span>
        <span
          class="app_type app_sql"
          v-if="record['jobType'] === 2">
          SQL
        </span>

        <span>
          <a-tooltip>
            {{ record.jobName }}
          </a-tooltip>
        </span>
      </template>
    </a-table>
  </a-card>
</template>
<script>
import {mapActions, mapGetters} from 'vuex'
import SvgIcon from '@/components/SvgIcon'
import {dependApps} from '@/api/variable'

export default {
  components: {SvgIcon},
  data() {
    return {
      variable: null,
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
        title: 'Application Name',
        dataIndex: 'jobName',
        width: 500,
        scopedSlots: {customRender: 'jobName'},
      }, {
        title: 'Owner',
        dataIndex: 'nickName'
      }, {
        title: 'Create Time',
        dataIndex: 'createTime'
      }]
    }
  },

  mounted() {
    const variableCode = this.variableCode()
    this.variable = variableCode
    if (variableCode) {
      this.CleanVariableCode()
      const params = {variableCode: variableCode}
      this.fetch(params)
    }
  },

  methods: {
    ...mapActions(['CleanVariableCode']),
    ...mapGetters(['variableCode']),

    fetch (params) {
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

      dependApps({ ...params }).then((resp) => {
        const pagination = { ...this.pagination }
        pagination.total = parseInt(resp.data.total)
        this.dataSource = resp.data.records
        this.pagination = pagination
        this.loading = false
      })
    },
    handleTableChange (pagination, filters, sorter) {
      this.paginationInfo = pagination
      const params = {variableCode: this.variable}
      this.fetch(params)
    },
    handleGoBack() {
      this.$router.back(-1)
    }
  }
}
</script>

<style lang="less">
@import "DependApps";
</style>
