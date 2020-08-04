<template>
  <div>
    <a-card :bordered="false" style="margin-top: 20px;">
      <div class="table-page-search-wrapper">
        <a-form layout="inline">
          <a-row :gutter="48">
            <div class="fold">
              <a-col :md="8" :sm="24">
                <a-form-item
                  label="名称"
                  :labelCol="{span: 4}"
                  :wrapperCol="{span: 18, offset: 2}">
                  <a-input v-model="queryParams.name"/>
                </a-form-item>
              </a-col>
              <a-col :md="8" :sm="24">
                <a-form-item
                  label="创建时间"
                  :labelCol="{span: 4}"
                  :wrapperCol="{span: 18, offset: 2}">
                  <range-date @change="handleDateChange" ref="createTime"></range-date>
                </a-form-item>
              </a-col>
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
                    v-permit="'role:export'"
                    @click="exportExcel">
                  </a-button>
                  <a-button
                    v-permit="'role:delete'"
                    type="primary"
                    shape="circle"
                    icon="plus"
                    @click="addProject">
                  </a-button>
                  <a-button
                    v-permit="'role:delete'"
                    type="primary"
                    shape="circle"
                    icon="minus"
                    @click="batchDelete">
                  </a-button>
                </span>
              </a-col>
            </div>
          </a-row>
        </a-form>
      </div>
      <!-- 表格区域 -->
      <a-table
        ref="TableInfo"
        :columns="columns"
        rowKey="id"
        :dataSource="dataSource"
        :pagination="pagination"
        :loading="loading"
        :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"
        :scroll="{ x: 900 }"
        @change="handleTableChange" >
        <template slot="operation" slot-scope="text, record">
          <a-popconfirm
            title="确定要pull最新代码并重新编译该项目吗?"
            cancel-text="No"
            ok-text="Yes"
            @confirm="handleBuild(record)"
          >
            <a-icon type="thunderbolt" theme="twoTone" twoToneColor="#4a9ff5" title="编译"></a-icon>
          </a-popconfirm>
          <a-icon
            v-permit="'role:update'"
            type="setting"
            theme="twoTone"
            twoToneColor="#4a9ff5"
            @click="edit(record)"
            title="修改角色">
          </a-icon>
          <a-icon type="eye" theme="twoTone" twoToneColor="#4a9ff5" @click="handleView(record)" title="查看"></a-icon>
        </template>
      </a-table>

    </a-card>
  </div>

</template>
<script>
import RangeDate from '@/components/DateTime/RangeDate'
import { build, list, remove } from '@/api/project'
export default {
  components: { RangeDate },
  data () {
    return {
      loading: false,
      advanced: false,
      dataSource: [],
      selectedRowKeys: [],
      queryParams: {},
      sortedInfo: null,
      pagination: {
        pageSizeOptions: ['10', '20', '30', '40', '100'],
        defaultCurrent: 1,
        defaultPageSize: 10,
        showQuickJumper: true,
        showSizeChanger: true,
        showTotal: (total, range) => `显示 ${range[0]} ~ ${range[1]} 条记录，共 ${total} 条记录`
      }
    }
  },
  filters: {
    subString (value,length) {
      if (!value) return ''
      value = value.toString().substring(0,length)
      return value
    }
  },
  computed: {
    columns () {
      let { sortedInfo } = this
      sortedInfo = sortedInfo || {}
      return [{
        title: '项目名称',
        dataIndex: 'name'
      }, {
        title: '托管平台',
        dataIndex: 'resptype',
        customRender: (text, row, index) => {
          switch (text) {
            case 1:
              return <a-icon type="github"></a-icon>
            case 2:
              return <a-icon type="medium"></a-icon>
            default:
              return text
          }
        },
      },
      {
        title: 'Branches',
        dataIndex: 'branches'
      },
      {
        title: 'Repository URL',
        dataIndex: 'url',
        customRender: (text, row, index) => {
          return <span><a-tooltip>
            <template slot="title">
              {{ text }}
            </template>
            {{text}}
          </a-tooltip>
            <a-icon type="copy"></a-icon>
          </span>
        },
      },{
        title: 'Last Build',
        dataIndex: 'date'
      },
        {
        title: '操作',
        dataIndex: 'operation',
        scopedSlots: { customRender: 'operation' },
        fixed: 'right',
        width: 120
      }]
    }
  },
  mounted () {
    this.fetch()
  },
  methods: {
    onSelectChange (selectedRowKeys) {
      console.log(selectedRowKeys)
      this.selectedRowKeys = selectedRowKeys
    },
    handleChange (info) {
      const status = info.file.status
      if (status === 'done') {
        this.loading = false
        this.$message.success(`${info.file.name} file uploaded successfully.`)
      } else if (status === 'error') {
        this.loading = false
        this.$message.error(`${info.file.name} file upload failed.`)
      }
    },
    handleDateChange (value) {
      if (value) {
        this.queryParams.dateFrom = value[0]
        this.queryParams.dateTo = value[1]
      }
    },
    handleBuild (record) {
      this.$notification.open({
        message: '编译通知',
        description: '已发送编译请求,后台正在执行编译,该操作可能花几分钟甚至更多时间来完成编译,请耐心等待',
        icon: <a-icon type="smile" style="color: #108ee9" />,
      })
      build({
        id: record.id
      }).then(() => {
        that.$message.success('编译成功')
      })
    },
    batchDelete () {
      if (!this.selectedRowKeys.length) {
        this.$message.warning('请选择需要删除的记录')
        return
      }
      const that = this
      that.$confirm({
        title: '确定删除所选中的记录?',
        content: '当您点击确定按钮后，这些记录将会被彻底删除',
        okText: '确定',
        okType: 'danger',
        cancelText: '取消',
        onOk () {
          remove({
            id: that.selectedRowKeys.join(',')
          }).then(() => {
            that.$message.success('删除成功')
            that.selectedRowKeys = []
            that.fetch()
          })
        },
        onCancel () {
          that.selectedRowKeys = []
          that.$message.info('已取消删除')
        }
      })
    },
    exportExcel () {
      const { sortedInfo } = this
      let sortField, sortOrder
      // 获取当前列的排序和列的过滤规则
      if (sortedInfo) {
        sortField = sortedInfo.field
        sortOrder = sortedInfo.order
      }
      $export({
        sortField: sortField,
        sortOrder: sortOrder,
        ...this.queryParams
      })
    },
    search () {
      const { sortedInfo } = this
      let sortField, sortOrder
      // 获取当前列的排序和列的过滤规则
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
    addProject () {
      this.$router.push({ 'path': 'addproject' })
    },
    reset () {
      // 取消选中
      this.selectedRowKeys = []
      // 重置列排序规则
      this.sortedInfo = null
      // 重置查询参数
      this.queryParams = {}
      // 清空时间选择
      this.$refs.createTime.reset()
      this.fetch()
    },
    handleTableChange (pagination, filters, sorter) {
      this.sortedInfo = sorter
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
      list({
        ...params
      }).then((resp) => {
        const pagination = { ...this.pagination }
        pagination.total = resp.data.total
        this.dataSource = resp.data.records
        this.pagination = pagination
        this.loading = false
      })
    }
  }
}
</script>

<style>
.ant-upload.ant-upload-drag p.ant-upload-drag-icon .anticon {
  font-size: 100px;
}
.tooltip {
  width: 150px;
}
</style>
