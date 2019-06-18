<template>
  <a-card :bordered="false" class="card-area">
    <div :class="advanced ? 'search' : null">
      <a-form layout="horizontal">
        <div :class="advanced ? null: 'fold'">
          <a-row >
            <a-col :md="12" :sm="24" >
              <a-form-item
                label="Bean名称"
                :labelCol="{span: 5}"
                :wrapperCol="{span: 18, offset: 1}">
                <a-input v-model="queryParams.beanName"/>
              </a-form-item>
            </a-col>
            <a-col :md="12" :sm="24" >
              <a-form-item
                label="方法名称"
                :labelCol="{span: 5}"
                :wrapperCol="{span: 18, offset: 1}">
                <a-input v-model="queryParams.methodName"/>
              </a-form-item>
            </a-col>
          </a-row>
          <a-row v-if="advanced">
            <a-col :md="12" :sm="24" >
              <a-form-item
                label="方法参数"
                :labelCol="{span: 5}"
                :wrapperCol="{span: 18, offset: 1}">
                <a-input v-model="queryParams.params"/>
              </a-form-item>
            </a-col>
            <a-col :md="12" :sm="24" >
              <a-form-item
                label="执行时间"
                :labelCol="{span: 5}"
                :wrapperCol="{span: 18, offset: 1}">
                <range-date @change="handleDateChange" ref="createTime"></range-date>
              </a-form-item>
            </a-col>
          </a-row>
        </div>
        <span style="float: right; margin-top: 3px;">
          <a-button type="primary" @click="search">查询</a-button>
          <a-button style="margin-left: 8px" @click="reset">重置</a-button>
          <a @click="toggleAdvanced" style="margin-left: 8px">
            {{advanced ? '收起' : '展开'}}
            <a-icon :type="advanced ? 'up' : 'down'" />
          </a>
        </span>
      </a-form>
    </div>
    <div>
      <div class="operator">
        <a-button v-hasPermission="'job:add'" ghost type="primary" @click="add">新增</a-button>
        <a-button v-hasPermission="'job:delete'" @click="batchDelete">删除</a-button>
        <a-dropdown v-hasPermission="'job:export'">
          <a-menu slot="overlay">
            <a-menu-item key="export-data" @click="exprotExccel">导出Excel</a-menu-item>
          </a-menu>
          <a-button>
            更多操作 <a-icon type="down" />
          </a-button>
        </a-dropdown>
      </div>
      <!-- 表格区域 -->
      <a-table ref="TableInfo"
               :columns="columns"
               :dataSource="dataSource"
               :pagination="pagination"
               :loading="loading"
               :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"
               @change="handleTableChange" :scroll="{ x: 1210 }">
        <template slot="method" slot-scope="text, record">
          <a-popover placement="topLeft">
            <template slot="content">
              <div>{{text}}</div>
            </template>
            <p style="width: 200px;margin-bottom: 0">{{text}}</p>
          </a-popover>
        </template>
        <template slot="params" slot-scope="text, record">
          <a-popover placement="topLeft">
            <template slot="content">
              <div style="max-width: 300px;">{{text}}</div>
            </template>
            <p style="width: 80px;margin-bottom: 0">{{text}}</p>
          </a-popover>
        </template>
        <template slot="operations" slot-scope="text, record">
          <a-icon v-hasPermission="'job:update'" type="setting" theme="twoTone" twoToneColor="#4a9ff5" @click="edit(record)" title="修改"></a-icon>
          &nbsp;
          <a-dropdown v-hasAnyPermission="'job:run','job:pause','job:resume'">
            <a class="ant-dropdown-link">
              <a-icon type="down-circle" style="font-size: 1.1rem"/>
            </a>
            <a-menu slot="overlay">
              <a-menu-item v-hasPermission="'job:run'">
                <a href="javascript:void(0)" @click="runJob(record)">立即执行</a>
              </a-menu-item>
              <a-menu-item v-hasPermission="'job:pause'" v-if="record.status === '0'">
                <a href="javascript:void(0)" @click="pauseJob(record)">暂停任务</a>
              </a-menu-item>
              <a-menu-item v-hasPermission="'job:resume'" v-if="record.status === '1'">
                <a href="javascript:void(0)" @click="resumeJob(record)">恢复任务</a>
              </a-menu-item>
            </a-menu>
          </a-dropdown>
          <a-badge v-hasNoPermission="'job:update','job:run','job:pause','job:resume'" status="warning" text="无权限"></a-badge>
        </template>
      </a-table>
    </div>
    <!-- 新增任务 -->
    <job-add
      @success="handleJobAddSuccess"
      @close="handleJobAddClose"
      :jobAddVisiable="jobAddVisiable">
    </job-add>
    <!-- 修改任务 -->
    <job-edit
      ref="jobEdit"
      @success="handleJobEditSuccess"
      @close="handleJobEditClose"
      :jobEditVisiable="jobEditVisiable">
    </job-edit>
  </a-card>
</template>

<script>
import JobAdd from './JobAdd'
import JobEdit from './JobEdit'
import RangeDate from '@/components/datetime/RangeDate'

export default {
  name: 'Job',
  components: {JobAdd, JobEdit, RangeDate},
  data () {
    return {
      advanced: false,
      dataSource: [],
      selectedRowKeys: [],
      queryParams: {},
      filteredInfo: null,
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
      loading: false,
      jobAddVisiable: false,
      jobEditVisiable: false
    }
  },
  computed: {
    columns () {
      let { sortedInfo, filteredInfo } = this
      sortedInfo = sortedInfo || {}
      filteredInfo = filteredInfo || {}
      return [{
        title: 'Bean名称',
        dataIndex: 'beanName'
      }, {
        title: '方法名称',
        dataIndex: 'methodName'
      }, {
        title: '方法参数',
        dataIndex: 'params',
        scopedSlots: { customRender: 'params' },
        width: 120
      }, {
        title: 'Cron表达式',
        dataIndex: 'cronExpression'
      }, {
        title: '备注',
        dataIndex: 'remark'
      }, {
        title: '状态',
        dataIndex: 'status',
        customRender: (text, row, index) => {
          switch (text) {
            case '0':
              return <a-tag color="green">正常</a-tag>
            case '1':
              return <a-tag color="orange">暂停</a-tag>
            default:
              return text
          }
        },
        filters: [
          { text: '正常', value: '0' },
          { text: '暂停', value: '1' }
        ],
        filterMultiple: false,
        filteredValue: filteredInfo.status || null,
        onFilter: (value, record) => record.status.includes(value)
      }, {
        title: '创建时间',
        dataIndex: 'createTime',
        sorter: true,
        sortOrder: sortedInfo.columnKey === 'createTime' && sortedInfo.order
      }, {
        title: '操作',
        dataIndex: 'operations',
        scopedSlots: { customRender: 'operations' },
        fixed: 'right',
        width: 100
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
    toggleAdvanced () {
      this.advanced = !this.advanced
      if (!this.advanced) {
        this.queryParams.createTimeFrom = ''
        this.queryParams.createTimeTo = ''
        this.queryParams.params = ''
      }
    },
    handleDateChange (value) {
      if (value) {
        this.queryParams.createTimeFrom = value[0]
        this.queryParams.createTimeTo = value[1]
      }
    },
    handleJobAddSuccess () {
      this.jobAddVisiable = false
      this.$message.success('新增定时任务成功')
      this.search()
    },
    handleJobAddClose () {
      this.jobAddVisiable = false
    },
    add () {
      this.jobAddVisiable = true
    },
    handleJobEditSuccess () {
      this.jobEditVisiable = false
      this.$message.success('修改定时任务成功')
      this.search()
    },
    handleJobEditClose () {
      this.jobEditVisiable = false
    },
    edit (record) {
      this.$refs.jobEdit.setFormValues(record)
      this.jobEditVisiable = true
    },
    runJob (record) {
      let jobId = record.jobId
      this.$get('job/run/' + jobId).then(() => {
        this.$message.success('执行定时任务成功')
        this.search()
      }).catch(() => {
        this.$message.error('执行定时任务失败')
      })
    },
    pauseJob (record) {
      let jobId = record.jobId
      this.$get('job/pause/' + jobId).then(() => {
        this.$message.success('暂停定时任务成功')
        this.search()
      }).catch(() => {
        this.$message.error('暂停定时任务失败')
      })
    },
    resumeJob (record) {
      let jobId = record.jobId
      this.$get('job/resume/' + jobId).then(() => {
        this.$message.success('恢复定时任务成功')
        this.search()
      }).catch(() => {
        this.$message.error('恢复定时任务失败')
      })
    },
    batchDelete () {
      if (!this.selectedRowKeys.length) {
        this.$message.warning('请选择需要删除的记录')
        return
      }
      let that = this
      this.$confirm({
        title: '确定删除所选中的记录?',
        content: '当您点击确定按钮后，这些记录将会被彻底删除',
        centered: true,
        onOk () {
          let jobIds = []
          for (let key of that.selectedRowKeys) {
            jobIds.push(that.dataSource[key].jobId)
          }
          that.$delete('job/' + jobIds.join(',')).then(() => {
            that.$message.success('删除成功')
            that.selectedRowKeys = []
            that.search()
          })
        },
        onCancel () {
          that.selectedRowKeys = []
        }
      })
    },
    exprotExccel () {
      let {sortedInfo, filteredInfo} = this
      let sortField, sortOrder
      // 获取当前列的排序和列的过滤规则
      if (sortedInfo) {
        sortField = sortedInfo.field
        sortOrder = sortedInfo.order
      }
      this.$export('job/excel', {
        sortField: sortField,
        sortOrder: sortOrder,
        ...this.queryParams,
        ...filteredInfo
      })
    },
    search () {
      let {sortedInfo, filteredInfo} = this
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
      if (this.advanced) {
        this.$refs.createTime.reset()
      }
      this.fetch()
    },
    handleTableChange (pagination, filters, sorter) {
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
      this.$get('job', {
        ...params
      }).then((r) => {
        let data = r.data
        const pagination = { ...this.pagination }
        pagination.total = data.total
        this.loading = false
        this.dataSource = data.rows
        this.pagination = pagination
      })
    }
  }
}
</script>

<style lang="less" scoped>
  @import "../../../../static/less/Common";
</style>
