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
                label="User Name"
                :label-col="{span: 4}"
                :wrapper-col="{span: 18, offset: 2}">
                <a-input
                  v-model="queryParams.username"/>
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
                @click.native="search"/>
              <a-button
                type="primary"
                v-permit="'token:add'"
                v-text="'创建token'"
                @click="handleAdd"/>
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
        slot="token-text"
        slot-scope="text,record">

        <a-tooltip placement="rightBottom" :title="record.token">
          <div style="overflow: hidden;text-overflow: ellipsis;display: -webkit-box;-webkit-line-clamp: 2;-webkit-box-orient: vertical; ">
            {{ record.token }}
          </div>
        </a-tooltip>

      </template>

      <template
        slot="token-status"
        slot-scope="text,record">
        <a-switch
          checked-children="on"
          un-checked-children="off"
          :checked="Boolean(record.finalTokenStatus)"
          @change="handleUpdateStatus($event,record)"/>
      </template>

      <template
        slot="operation"
        slot-scope="text, record">

        <a-tooltip title="Copy Token">
          <a-button
            v-permit="'token:view'"
            name="copy"
            @click.native="copyToken(record)"
            shape="circle"
            size="small"
            style="margin-left: 8px"
            class="control-button ctl-btn-color">
            <a-icon type="copy"/>
          </a-button>
        </a-tooltip>

        <a-tooltip title="Delete Token">
          <a-popconfirm
            v-permit="'token:delete'"
            title="Are you sure delete this token ?"
            cancel-text="No"
            ok-text="Yes"
            @confirm="handleDelete(record)">
            <a-button
              type="danger"
              shape="circle"
              size="small"
              style="margin-left: 8px"
              class="control-button">
              <a-icon type="delete"/>
            </a-button>
          </a-popconfirm>
        </a-tooltip>
      </template>
    </a-table>


    <token-add
      @close="handleTokenAddClose"
      @success="handleTokenAddSuccess"
      :visible="tokenAdd.visible"/>

  </a-card>
</template>

<script>
import TokenAdd from './TokenAdd'
import RangeDate from '@/components/DateTime/RangeDate'
import SvgIcon from '@/components/SvgIcon'

import {deleteToken, list, updateTokenStatus} from '@/api/token'
import storage from '@/utils/storage'
import {USER_NAME} from '@/store/mutation-types'

export default {
  name: 'Token',
  components: {RangeDate, SvgIcon, TokenAdd},
  data() {
    return {
      tokenAdd: {
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
      let {sortedInfo, filteredInfo} = this
      sortedInfo = sortedInfo || {}
      filteredInfo = filteredInfo || {}
      return [{
        title: 'User Name',
        dataIndex: 'username',
        width: 150,
        sorter: true,
        sortOrder: sortedInfo.columnKey === 'username' && sortedInfo.order
      }, {
        title: 'Token',
        dataIndex: 'token',
        scopedSlots: {customRender: 'token-text'}
      }, {
        title: 'Description',
        dataIndex: 'description'
      }, {
        title: 'Create Time',
        dataIndex: 'createTime'
      }, {
        title: 'Expire Time',
        dataIndex: 'expireTime',
        sorter: true,
        sortOrder: sortedInfo.columnKey === 'expireTime' && sortedInfo.order
      }, {
        title: 'Status',
        dataIndex: 'status',
        width: 150,
        scopedSlots: {customRender: 'token-status'}
      },
        {
          title: 'Operation',
          width: 150,
          dataIndex: 'operation',
          scopedSlots: {customRender: 'operation'}
        }]
    },
    userName() {
      return storage.get(USER_NAME)
    }
  },

  mounted() {
    this.fetch()
  },

  methods: {
    handleUpdateStatus(checked, record) {
      updateTokenStatus({
        status: checked ? 1 : 0,
        tokenId: record.id
      }).then((resp) => {
        if (resp.code !== undefined && resp.code.toString() === '2000') {
          this.$message.success('update status successful')
          this.search()
        } else if (resp.code !== undefined && resp.code.toString() === '3001') {
          this.$message.error(resp.message)
        } else {
          this.$message.error('update failed')
        }
      })
    },
    search() {
      const {sortedInfo, filteredInfo} = this
      let sortField, sortOrder
      // 获取当前列的排序和列的过滤规则
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

    handleDelete(record) {
      record.id = parseInt(record.id)
      deleteToken({
        tokenId: record.id
      }).then((resp) => {
        if (resp.status === 'success') {
          this.$message.success('delete successful')
          this.search()
        } else {
          this.$message.error('delete failed')
        }
      })
    },
    handleAdd() {
      this.tokenAdd.visible = true
    },
    handleTokenAddClose() {
      this.tokenAdd.visible = false
    },
    handleTokenAddSuccess() {
      this.tokenAdd.visible = false
      this.$message.success('新增令牌成功')
      this.search()
    },

    copyToken(record) {
      const oInput = document.createElement('input')
      oInput.value = record.token
      document.body.appendChild(oInput)
      // 选择对象
      oInput.select()
      document.execCommand('Copy')
      this.$message.success('复制成功')
      oInput.remove()
    },
    handleDateChange(value) {
      if (value) {
        this.queryParams.createTimeFrom = value[0]
        this.queryParams.createTimeTo = value[1]
      }
    },
    reset() {
      // 重置分页
      this.$refs.TableInfo.pagination.current = this.pagination.defaultCurrent
      if (this.paginationInfo) {
        this.paginationInfo.current = this.pagination.defaultCurrent
        this.paginationInfo.pageSize = this.pagination.defaultPageSize
      }
      // 重置列过滤器规则
      this.filteredInfo = null
      // 重置列排序规则
      this.sortedInfo = null
      // 重置查询参数
      this.queryParams = {}
      this.$refs.createTime.reset()
      this.fetch()
    },
    handleTableChange(pagination, filters, sorter) {
      // 将这三个参数赋值给Vue data，用于后续使用
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
      // 显示loading
      this.loading = true
      if (this.paginationInfo) {
        // 如果分页信息不为空，则设置表格当前第几页，每页条数，并设置查询分页参数
        this.$refs.TableInfo.pagination.current = this.paginationInfo.current
        this.$refs.TableInfo.pagination.pageSize = this.paginationInfo.pageSize
        params.pageSize = this.paginationInfo.pageSize
        params.pageNum = this.paginationInfo.current
      } else {
        // 如果分页信息为空，则设置为默认值
        params.pageSize = this.pagination.defaultPageSize
        params.pageNum = this.pagination.defaultCurrent
      }
      if (params.status != null && params.status.length > 0) {
        params.status = params.status[0]
      } else {
        delete params.status
      }

      if (params.sortField === 'createTime') {
        params.sortField = 'create_time'
      }

      list({...params}).then((resp) => {
        const pagination = {...this.pagination}
        pagination.total = parseInt(resp.data.total)
        this.dataSource = resp.data.records
        this.pagination = pagination
        // 数据加载完毕，关闭loading
        this.loading = false
      })
    }
  }
}
</script>

