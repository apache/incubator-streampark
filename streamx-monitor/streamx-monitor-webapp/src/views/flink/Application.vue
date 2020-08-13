<template>
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
                <a-input v-model="queryParams.projectName"/>
              </a-form-item>
            </a-col>
            <a-col :md="8" :sm="24">
              <a-form-item
                label="创建时间"
                :labelCol="{span: 4}"
                :wrapperCol="{span: 18, offset: 2}">
                <range-date
                  @change="handleDateChange"
                  ref="createTime">
                </range-date>
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
                  @click="addTask">
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
      rowKey="name"
      :dataSource="dataSource"
      :pagination="pagination"
      :loading="loading"
      :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"
      :scroll="{ x: 700 }"
      @change="handleTableChange">
      <template slot="state" slot-scope="state">
        <!--
        ACCEPTED(5),
        RUNNING(6),
        CANCELLING(7),
        CANCELED(8),
        FINISHED(9),
        FAILED(10),
        LOST(11);
        -->
        <a-tag color="#108ee9" v-if="state === 0">CREATED</a-tag>
        <a-tag color="#87d068" v-if="state === 1">DEPLOYING</a-tag>
        <a-tag color="cyan" v-if="state === 2">NEW</a-tag>
        <a-tag color="#f50" v-if="state === 3">NEW_SAVING</a-tag>
        <a-tag color="#f50" v-if="state === 4">SUBMITTED</a-tag>
        <a-tag color="#f50" v-if="state === 5">ACCEPTED</a-tag>
        <a-tag color="#87d068" v-if="state === 6">RUNNING</a-tag>
        <a-tag color="rgb(250, 140, 22)" v-if="state === 7">CANCELLING</a-tag>
        <a-tag color="rgb(250, 140, 22)" v-if="state === 8">CANCELED</a-tag>
        <a-tag color="#f50" v-if="state === 9">FINISHED</a-tag>
        <a-tag color="#f50" v-if="state === 10">FAILED</a-tag>
        <a-tag color="#000" v-if="state === 11">LOST</a-tag>
      </template>
      <template slot="operation" slot-scope="text, record">
        <a-icon
          v-permit="'role:update'"
          v-show="record.deploy === 1"
          type="thunderbolt"
          theme="twoTone"
          twoToneColor="#4a9ff5"
          @click="handleDeploy(record)"
          title="发布任务">
        </a-icon>
        <a-icon
          v-permit="'role:update'"
          type="setting"
          theme="twoTone"
          twoToneColor="#4a9ff5"
          @click="handleEdit(record)"
          title="修改角色">
        </a-icon>

        <template v-show="record.state !== 6">
          <a-popconfirm
            title="确定要启动该项目吗?"
            ok-text="启动"
            cancel-text="取消"
            @confirm="handleStartUp(record)">
            <a-icon
              v-permit="'role:update'"
              type="play-circle"
              theme="twoTone"
              twoToneColor="#4a9ff5"
              title="提交任务">
            </a-icon>
          </a-popconfirm>
        </template>

        <template>
          <a-popconfirm
            v-show="record.state === 6"
            title="确定要停止该项目吗?"
            ok-text="停止"
            cancel-text="取消"
            @confirm="handleCancel(record)">
            <a-icon type="poweroff"
                    title="取消任务">
            </a-icon>
          </a-popconfirm>

          <a-icon type="eye"
                  v-show="record.state === 6"
                  theme="twoTone"
                  twoToneColor="#42b983"
                  @click="handleView(record)" title="查看">
          </a-icon>

        </template>

      </template>
    </a-table>
  </a-card>
</template>
<script>
import RangeDate from '@/components/DateTime/RangeDate'
import {list, remove, cancel, deploy, startUp, yarn} from '@/api/application'

export default {
  components: {RangeDate},
  data() {
    return {
      loading: false,
      advanced: false,
      dataSource: [],
      selectedRowKeys: [],
      queryParams: {},
      sortedInfo: null,
      yarn: null,
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
  computed: {
    columns() {
      let {sortedInfo} = this
      sortedInfo = sortedInfo || {}
      return [{
        title: '应用名称',
        dataIndex: 'appName',
        width: 150,
        fixed: 'left'
      },{
        title: '所属项目',
        dataIndex: 'projectName',
        width: 200
      },  {
        title: '开始时间',
        dataIndex: 'startTime',
        sorter: true,
        sortOrder: sortedInfo.columnKey === 'date' && sortedInfo.order,
        width: 180
      },  {
        title: '结束时间',
        dataIndex: 'endTime',
        sorter: true,
        sortOrder: sortedInfo.columnKey === 'date' && sortedInfo.order,
        width: 180
      }, {
        title: '应用ID',
        dataIndex: 'appId',
        width: 270
      }, {
        title: '状态',
        dataIndex: 'state',
        width: 100,
        scopedSlots: {customRender: 'state'},
        fixed: 'right'
      }, {
        title: '操作',
        dataIndex: 'operation',
        scopedSlots: {customRender: 'operation'},
        fixed: 'right',
        width: 150
      }]
    }
  },

  mounted() {
    this.handleYarn()
    this.handleFetch(true)
    window.setInterval(() => this.handleFetch(false), 1000)
  },

  methods: {

    onSelectChange(selectedRowKeys) {
      console.log(selectedRowKeys)
      this.selectedRowKeys = selectedRowKeys
    },

    handleDateChange(value) {
      if (value) {
        this.queryParams.dateFrom = value[0]
        this.queryParams.dateTo = value[1]
      }
    },

    handleDeploy(value) {
      deploy({
        id: value.id,
        projectId: value.projectId,
        module: value.module
      }).then((resp) => {
        console.log(resp)
      })
    },

    batchDelete() {
      this.$router.push({'path': 'addtest'})

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
        onOk() {
          remove({
            name: that.selectedRowKeys.join(',')
          }).then(() => {
            that.$message.success('删除成功')
            that.selectedRowKeys = []
            that.fetch()
          })
        },
        onCancel() {
          that.selectedRowKeys = []
          that.$message.info('已取消删除')
        }
      })
    },

    search() {
      const {sortedInfo} = this
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

    reset() {
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

    handleTableChange(pagination, filters, sorter) {
      this.sortedInfo = sorter
      this.fetch({
        sortField: sorter.field,
        sortOrder: sorter.order,
        ...this.queryParams,
        ...filters
      })
    },

    handleFetch(loading) {
      if(loading) this.loading = true
      const params = {}
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
        this.loading = false
        const pagination = {...this.pagination}
        pagination.total = resp.data.total
        this.dataSource = resp.data.records
        this.pagination = pagination
      })
    },

    handleYarn(params = {}) {
      yarn({}).then((resp) => {
        this.yarn = resp.data
      })
    },

    handleView(params) {
      window.open(this.yarn + "/proxy/" + params.appId + "/")
    },

    addTask() {
      this.$router.push({'path': 'addapp'})
    },

    handleStartUp(app) {
      this.$notification.open({
        message: '已发送任务启动请求',
        icon: <a-icon type="smile" style="color: #108ee9" />
      })
      startUp({
        id: app.id
      }).then((resp) => {
        console.log(resp)
      })
    },

    handleCancel (app) {
      cancel({
        id: app.id
      }).then((resp) => {
        console.log(resp)
      })
    },

    exportExcel() {

    }

  }
}
</script>

<style>
.ant-upload.ant-upload-drag p.ant-upload-drag-icon .anticon {
  font-size: 100px;
}
.ant-tag {
  border-radius: 0;
  font-weight: 700;
  text-align: center;
  padding: 3px 5px;
  cursor: default;
}
</style>
