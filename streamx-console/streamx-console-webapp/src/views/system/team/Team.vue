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
                  v-model="queryParams.teamName" />
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
                    @change="handleDateChange"
                    ref="createTime" />
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
                v-permit="'user:add'"
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
        slot="operation"
        slot-scope="text, record">
        <a-popconfirm
          v-permit="'team:delete'"
          title="Are you sure delete this team ?"
          cancel-text="No"
          ok-text="Yes"
          @confirm="handleDelete(record)">
          <svg-icon name="remove" border/>
        </a-popconfirm>
      </template>
    </a-table>

    <!-- 新增用户 -->
    <user-add
      @close="handleUserAddClose"
      @success="handleUserAddSuccess"
      :visible="userAdd.visible" />
    <!-- 修改用户 -->
    <user-edit
      ref="userEdit"
      @close="handleUserEditClose"
      @success="handleUserEditSuccess"
      :visible="userEdit.visible" />
  </a-card>
</template>

<script>

import UserAdd from './TeamAdd'
import RangeDate from '@/components/DateTime/RangeDate'
import SvgIcon from '@/components/SvgIcon'

import { list, remove} from '@/api/team'
import storage from '@/utils/storage'
import {USER_NAME} from '@/store/mutation-types'

export default {
  name: 'Team',
  components: { UserAdd, RangeDate, SvgIcon },
  data () {
    return {
      userAdd: {
        visible: false
      },
      userEdit: {
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
      let { sortedInfo, filteredInfo } = this
      sortedInfo = sortedInfo || {}
      filteredInfo = filteredInfo || {}
      return [{
        title: 'Team Code',
        dataIndex: 'teamCode'
      },  {
        title: 'Team Name',
        dataIndex: 'teamName'
      },  {
        title: 'Create Time',
        dataIndex: 'createTime',
        sorter: true,
        sortOrder: sortedInfo.columnKey === 'create_time' && sortedInfo.order
      },
      {
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
    handleAdd () {
      this.userAdd.visible = true
    },
    handleUserAddClose () {
      this.userAdd.visible = false
    },
    handleUserAddSuccess () {
      this.userAdd.visible = false
      this.$message.success('新增Team成功')
      this.search()
    },
    handleEdit (record) {
      // this.SetAppId(app.id)
      this.$router.push({'path': '/system/role'})

      // this.$refs.userEdit.setFormValues(record)
      // this.userEdit.visible = true
    },
    handleUserEditClose () {
      this.userEdit.visible = false
    },
    handleUserEditSuccess () {
      this.userEdit.visible = false
      this.$message.success('修改用户成功')
      this.search()
    },
    handleDateChange (value) {
      if (value) {
        this.queryParams.createTimeFrom = value[0]
        this.queryParams.createTimeTo = value[1]
      }
    },
    handleDelete (record) {
      remove({
        teamId: record.teamId
      }).then((resp) => {
        if (resp.message === 'success') {
          this.$message.success('delete successful')
          this.search()
        } else {
          this.$message.error(resp.message)
        }
      })

    },
    resetPassword (user) {
      this.$swal.fire({
        title: 'reset password, are yor sure?',
        showCancelButton: true,
        confirmButtonText: `Yes`,
      }).then((result) => {
        if (result.isConfirmed) {
          resetPassword( {
            usernames: user.username
          }).then((resp) => {
            if (resp.status === 'success') {
              this.$swal.fire(
                'reset password successful, user ['+ user.username + '] new password is streamx666', '',
                'success'
              )
            }
          })
        }
      })
    },

    search () {
      const { sortedInfo, filteredInfo } = this
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
    reset () {
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
    handleTableChange (pagination, filters, sorter) {
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
    fetch (params = {}) {
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
        // 数据加载完毕，关闭loading
        this.loading = false
      })
    }
  }
}
</script>
