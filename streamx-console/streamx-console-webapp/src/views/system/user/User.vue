<template>
  <a-card :bordered="false">
    <div class="table-page-search-wrapper">
      <a-form layout="inline">
        <a-row :gutter="48">
          <div class="fold">
            <a-col :md="8" :sm="24">
              <a-form-item
                label="User Name"
                :labelCol="{span: 4}"
                :wrapperCol="{span: 18, offset: 2}">
                <a-input v-model="queryParams.username"/>
              </a-form-item>
            </a-col>
            <template>
              <a-col :md="8" :sm="24">
                <a-form-item
                  label="Create Time"
                  :labelCol="{span: 4}"
                  :wrapperCol="{span: 18, offset: 2}">
                  <range-date
                    @change="handleDateChange"
                    ref="createTime">
                  </range-date>
                </a-form-item>
              </a-col>
            </template>
          </div>
          <a-col :md="8" :sm="24">
              <span class="table-page-search-bar">
              <a-button
                type="primary"
                shape="circle"
                icon="search"
                @click="search">
              </a-button>
              <a-button
                type="primary"
                shape="circle"
                icon="rest"
                @click="reset">
              </a-button>
              <a-button
                type="primary"
                shape="circle"
                icon="export"
                v-permit="'user:export'"
                @click="exportExcel">
              </a-button>
              <a-button
                type="primary"
                shape="circle"
                icon="plus"
                v-permit="'user:add'"
                @click="add">
              </a-button>
              <a-button
                v-permit="'user:delete'"
                type="primary"
                shape="circle"
                icon="minus"
                @click="batchDelete">
              </a-button>
            </span>
          </a-col>

        </a-row>
      </a-form>
    </div>

    <!-- 表格区域 -->
    <a-table
      ref="TableInfo"
      :columns="columns"
      :dataSource="dataSource"
      :pagination="pagination"
      :loading="loading"
      :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"
      :scroll="{ x: 900 }"
      @change="handleTableChange" >

      <template slot="email" slot-scope="text, record">
        <a-popover placement="topLeft">
          <template slot="content">
            <div>{{ text }}</div>
          </template>
          <p style="width: 150px;margin-bottom: 0">{{ text }}</p>
        </a-popover>
      </template>
      <template slot="operation" slot-scope="text, record">
        <a-icon type="setting" theme="twoTone" twoToneColor="#4a9ff5" @click="edit(record)" title="修改用户"></a-icon>
        <a-icon type="eye" theme="twoTone" twoToneColor="#42b983" @click="view(record)" title="查看"></a-icon>
        <a-badge v-noPermit="'user:update','user:view'" status="warning" text="无权限"></a-badge>
      </template>
    </a-table>

    <!-- 用户信息查看 -->
    <user-info
      :userInfoData="userInfo.data"
      :userInfoVisiable="userInfo.visiable"
      @close="handleUserInfoClose">
    </user-info>
    <!-- 新增用户 -->
    <user-add
      @close="handleUserAddClose"
      @success="handleUserAddSuccess"
      :userAddVisiable="userAdd.visiable">
    </user-add>
    <!-- 修改用户 -->
    <user-edit
      ref="userEdit"
      @close="handleUserEditClose"
      @success="handleUserEditSuccess"
      :userEditVisiable="userEdit.visiable">
    </user-edit>

  </a-card>

</template>

<script>
import UserInfo from './UserInfo'
import UserAdd from './UserAdd'
import UserEdit from './UserEdit'
import RangeDate from '@/components/DateTime/RangeDate'
import { list, remove, $export } from '@/api/user'

export default {
  name: 'User',
  components: { UserInfo, UserAdd, UserEdit, RangeDate },
  data () {
    return {
      userInfo: {
        visiable: false,
        data: {}
      },
      userAdd: {
        visiable: false
      },
      userEdit: {
        visiable: false
      },
      queryParams: {},
      filteredInfo: null,
      sortedInfo: null,
      paginationInfo: null,
      dataSource: [],
      selectedRowKeys: [],
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
        title: 'User Name',
        dataIndex: 'username',
        sorter: true,
        sortOrder: sortedInfo.columnKey === 'username' && sortedInfo.order
      }, {
        title: 'Nick Name',
        dataIndex: 'nickName'
      }, {
        title: 'Status',
        dataIndex: 'status',
        customRender: (text, row, index) => {
          switch (text) {
            case '0': return <a-tag color="red"> Locked </a-tag>
            case '1': return <a-tag color="cyan"> Effective </a-tag>
            default: return text
          }
        },
        filters: [
          { text: 'Effective', value: '1' },
          { text: 'Locked', value: '0' }
        ],
        filterMultiple: false,
        filteredValue: filteredInfo.status || null,
        onFilter: (value, record) => record.status.includes(value)
      }, {
        title: 'Create Time',
        dataIndex: 'createTime',
        sorter: true,
        sortOrder: sortedInfo.columnKey === 'createTime' && sortedInfo.order
      },
      {
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
    onSelectChange (selectedRowKeys) {
      this.selectedRowKeys = selectedRowKeys
    },
    view (record) {
      this.userInfo.data = record
      this.userInfo.visiable = true
    },
    add () {
      this.userAdd.visiable = true
    },
    handleUserAddClose () {
      this.userAdd.visiable = false
    },
    handleUserAddSuccess () {
      this.userAdd.visiable = false
      this.$message.success('新增用户成功，初始密码为adminx123')
      this.search()
    },
    edit (record) {
      this.$refs.userEdit.setFormValues(record)
      this.userEdit.visiable = true
    },
    handleUserEditClose () {
      this.userEdit.visiable = false
    },
    handleUserEditSuccess () {
      this.userEdit.visiable = false
      this.$message.success('修改用户成功')
      this.search()
    },
    handleUserInfoClose () {
      this.userInfo.visiable = false
    },
    handleDateChange (value) {
      if (value) {
        this.queryParams.createTimeFrom = value[0]
        this.queryParams.createTimeTo = value[1]
      }
    },
    batchDelete () {
      if (!this.selectedRowKeys.length) {
        this.$message.warning('请选择需要删除的记录')
        return
      }
      const that = this
      this.$confirm({
        title: '确定删除所选中的记录?',
        content: '当您点击确定按钮后，这些记录将会被彻底删除',
        okText: '确定',
        okType: 'danger',
        cancelText: '取消',
        centered: true,
        onOk () {
          const userIds = []
          for (const key of that.selectedRowKeys) {
            userIds.push(that.dataSource[key].userId)
          }
          remove({
            userIds: userIds.join(',')
          }).then(() => {
            that.$message.success('delete successful')
            that.selectedRowKeys = []
            that.search()
          })
        },
        onCancel () {
          that.selectedRowKeys = []
          that.$message.info('cancel delete')
        }
      })
    },

    resetPassword () {
      if (!this.selectedRowKeys.length) {
        this.$message.warning('请选择需要重置密码的用户')
        return
      }
      const that = this
      this.$confirm('this option will be reset password, are yor sure?', 'warning', {
        confirmButtonText: 'Yes',
        cancelButtonText: 'No',
        type: 'warning'
      }).then(() => {
        const usernames = []
        for (const key of that.selectedRowKeys) {
          usernames.push(that.dataSource[key].username)
        }
        that.$put('user/password/reset', {
          usernames: usernames.join(',')
        }).then(() => {
          that.$message.success('reset password successful')
          that.selectedRowKeys = []
        })
      }).catch(() => {
        that.selectedRowKeys = []
        this.$message({
          type: 'info',
          message: 'cancel delete'
        })
      })
    },
    exportExcel () {
      const { sortedInfo, filteredInfo } = this
      let sortField, sortOrder
      // 获取当前列的排序和列的过滤规则
      if (sortedInfo) {
        sortField = sortedInfo.field
        sortOrder = sortedInfo.order
      }
      $export({
        sortField: sortField,
        sortOrder: sortOrder,
        ...this.queryParams,
        ...filteredInfo
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
      // 取消选中
      this.selectedRowKeys = []
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
      this.userInfo.visiable = false
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
      list({ ...params }).then((resp) => {
        const pagination = { ...this.pagination }
        pagination.total = resp.total
        this.dataSource = resp.rows
        this.pagination = pagination
        // 数据加载完毕，关闭loading
        this.loading = false
      })
    }
  }
}
</script>
