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
                <a-input v-model="queryParams['projectName']"/>
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
                  @click="handleSearch">
                </a-button>
                <a-button
                  type="primary"
                  shape="circle"
                  icon="rest"
                  @click="handleReset">
                </a-button>
                <a-button
                  v-permit="'role:delete'"
                  type="primary"
                  shape="circle"
                  icon="plus"
                  @click="addTask">
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
      size="middle"
      rowKey="name"
      :dataSource="dataSource"
      :pagination="pagination"
      :loading="loading"
      :scroll="{ x: 700 }"
      @change="handleTableChange">

      <template slot="appName" slot-scope="text, record">
        <a-badge dot title="应用已更新,需重新发布" v-if="record.deploy === 1">
          {{ record.appName }}
        </a-badge>
        <span v-else>
          {{ record.appName }}
        </span>
      </template>

      <template slot="duration" slot-scope="text, record">
          {{ record.duration | duration }}
      </template>

      <template slot="state" slot-scope="state">
        <!--
          CREATED(0),
          DEPLOYING(1),
          DEPLOYED(2),
          STARTING(3),
          RESTARTING(4),
          RUNNING(5),
          FAILING(6),
          FAILED(7),
          CANCELLING(8),
          CANCELED(9),
          FINISHED(10),
          SUSPENDED(11),
          RECONCILING(12),
          LOST(13);

         TOTAL: '#112641',
          RUNNING: '#52c41a',
          FAILED: '#f5222d',
          FINISHED: '#1890ff',
          CANCELED: '#fa8c16',
          CANCELING: '#faad14',
          CREATED: '#2f54eb',
          DEPLOYING: '#13c2c2',
          RECONCILING: '#eb2f96',
          IN_PROGRESS: '#faad14',
          SCHEDULED: '#722ed1',
          COMPLETED: '#1890ff',
          RESTARTING: '#13c2c2'
        -->
        <div class="app_state">
          <a-tag color="#2f54eb" v-if="state === 0">CREATED</a-tag>
          <a-tag color="#1ABBDC" v-if="state === 1">DEPLOYING</a-tag>
          <a-tag color="#108ee9" v-if="state === 2">DEPLOYED</a-tag>
          <a-tag color="#1AB58E" v-if="state === 3">STARTING</a-tag>
          <a-tag color="#13c2c2" v-if="state === 4">RESTARTING</a-tag>
          <a-tag color="#52c41a" v-if="state === 5">RUNNING</a-tag>
          <a-tag color="#fa541c" v-if="state === 6">FAILING</a-tag>
          <a-tag color="#f5222d" v-if="state === 7">FAILED</a-tag>
          <a-tag color="#faad14" v-if="state === 8">CANCELLING</a-tag>
          <a-tag color="#fa8c16" v-if="state === 9">CANCELED</a-tag>
          <a-tag color="#1890ff" v-if="state === 10">FINISHED</a-tag>
          <a-tag color="#722ed1" v-if="state === 11">SUSPENDED</a-tag>
          <a-tag color="#eb2f96" v-if="state === 12">RECONCILING</a-tag>
          <a-tag color="#000000" v-if="state === 13">LOST</a-tag>
        </div>
      </template>

      <template slot="operation" slot-scope="text, record">
        <a-icon
          v-show="record.deploy === 1 && record.state !== 1 "
          v-permit="'role:update'"
          type="upload"
          style="color:#4a9ff5"
          @click="handleDeploy(record)">
        </a-icon>
        <a-icon
          v-permit="'role:update'"
          type="setting"
          theme="twoTone"
          twoToneColor="#4a9ff5"
          @click="handleEdit(record)"
          title="修改角色">
        </a-icon>
        <template>
          <a-popconfirm
            v-show="
            record.state === 0
            || record.state === 2
            || record.state === 7
            || record.state === 9
            || record.state === 10
            || record.state === 11
            || record.state === 13"
            v-permit="'role:update'"
            title="确定要启动该应用吗?"
            ok-text="确定"
            cancel-text="取消"
            @confirm="handleStartUp(record)">
            <a-icon slot="icon" type="question-circle-o" style="color: red"/>
            <a-icon
              type="play-circle"
              theme="twoTone"
              twoToneColor="#4a9ff5"
              title="启动应用">
            </a-icon>
          </a-popconfirm>
        </template>

        <a-icon type="poweroff"
                title="停止应用"
                style="color: #4a9ff5"
                v-show="record.state === 5"
                @click="handleCancel(record)">
        </a-icon>


        <a-icon type="eye"
                v-show="record.state === 5"
                theme="twoTone"
                twoToneColor="#4a9ff5"
                @click="handleView(record)" title="查看">
        </a-icon>

        <a-icon type="profile" theme="twoTone" twoToneColor="#4a9ff5" @click="handleDetail(item)" />

      </template>

    </a-table>

    <a-modal v-model="deployVisible" on-ok="handleDeployOk">
      <template slot="title">
        <a-icon slot="icon" type="question-circle-o" style="color: red"/>
        确定要重新发布应用吗?
      </template>
      <template slot="footer">
        <a-button key="back" @click="handleDeployNo">
          取消
        </a-button>
        <a-button key="submit" type="primary" :loading="loading" @click="handleDeployOk">
          确定
        </a-button>
      </template>
      <a-form @submit="handleDeployOk" :form="formDeploy">
        <a-form-item>
          <a-textarea
            rows="3"
            placeholder="应用重新发布前会先备份当前的应用,请输入当前应用的备份描述信息,以便回滚版本时找回"
            v-decorator="['description',{ rules: [{ required: true, message: '请输入备份描述' } ]}]">
          </a-textarea>
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model="cancelVisible" on-ok="handleCancelOk">
      <template slot="title">
        <a-icon slot="icon" type="poweroff" style="color: red"/>
        Cancel application
      </template>

      <a-form @submit="handleCancelOk" :form="formSavePoint">

        <a-form-item
          label="Drain"
          :labelCol="{lg: {span: 5}, sm: {span: 5}}"
          :wrapperCol="{lg: {span: 17}, sm: {span: 5} }">
          <a-input-number
            :min="1"
            :step="1000"
            placeholder="Send before max watermark stoping"
            v-decorator="['drain',{ rules: [{ trigger:'submit', message: ' Send MAX_WATERMARK before taking the savepoint and stopping the pipelne' } ]}]">
          </a-input-number>
        </a-form-item>

        <a-form-item
          label="Savepoint"
          :labelCol="{lg: {span: 5}, sm: {span: 5}}"
          :wrapperCol="{lg: {span: 17}, sm: {span: 5} }">
          <a-textarea
            rows="3"
            placeholder="Path to the savepoint (with schema hdfs://)"
            v-decorator="['savePoint',{ rules: [{ trigger:'submit', message: 'Path to the savepoint (for example hdfs:///flink/savepoint-1537)' } ]}]">
          </a-textarea>
        </a-form-item>

      </a-form>

      <template slot="footer">
        <a-button key="back" @click="handleCancelNo">
          取消
        </a-button>
        <a-button key="submit" type="primary" :loading="loading" @click="handleCancelOk">
          确定
        </a-button>
      </template>
    </a-modal>

  </a-card>
</template>
<script>
import RangeDate from '@comp/DateTime/RangeDate'
import {list, remove, cancel, deploy, startUp, yarn} from '@api/application'

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
      deployVisible: false,
      cancelVisible: false,
      formDeploy: null,
      formSavePoint: null,
      application: null,
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
        title: 'Job Name',
        dataIndex: 'appName',
        width: 200,
        fixed: 'left',
        ellipsis: true,
        scopedSlots: {customRender: 'appName'},
      }, {
        title: 'Project',
        dataIndex: 'projectName',
        ellipsis: true,
        width: 200
      }, {
        title: 'Start Time',
        dataIndex: 'startTime',
        sorter: true,
        sortOrder: sortedInfo.columnKey === 'startTime' && sortedInfo.order,
        width: 180
      }, {
        title: 'Duration',
        dataIndex: 'duration',
        sorter: true,
        sortOrder: sortedInfo.columnKey === 'duration' && sortedInfo.order,
        scopedSlots: {customRender: 'duration'},
        width: 100
      },{
        title: 'End Time',
        dataIndex: 'endTime',
        sorter: true,
        sortOrder: sortedInfo.columnKey === 'endTime' && sortedInfo.order,
        width: 180
      }, {
        title: 'Status',
        dataIndex: 'state',
        width: 80,
        scopedSlots: {customRender: 'state'},
        fixed: 'right'
      }, {
        title: 'Operation',
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
    const timer = window.setInterval(() => this.handleFetch(false), 1000)
    this.$once('hook:beforeDestroy', () => {
      clearInterval(timer);
    })
  },

  filters: {
    duration(ms) {
      let ss = 1000
      let mi = ss * 60
      let hh = mi * 60
      let dd = hh * 24

      let day = parseInt(ms / dd)
      let hour = parseInt((ms - day * dd) / hh)
      let minute = parseInt((ms - day * dd - hour * hh)/ mi)
      let seconds = parseInt((ms - day * dd - hour * hh - minute * mi) / ss)

      if (day > 0) {
        return day + "D " + hour + "h " + minute + "m " + seconds + "s"
      } else if (hour > 0) {
        return hour + "h " + minute + "m " + seconds + "s"
      } else if (minute > 0) {
        return minute + "m " + seconds + "s"
      } else {
        return 0 + "m " + seconds + "s"
      }
    }
  },

  beforeMount() {
    this.formDeploy = this.$form.createForm(this)
    this.formSavePoint = this.$form.createForm(this)
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
      this.deployVisible = true
      this.application = value
    },

    handleDeployNo() {
      this.deployVisible = false
      this.formDeploy.resetFields()
    },

    handleDeployOk() {
      this.formDeploy.validateFields((err, values) => {
        if (!err) {
          this.handleDeployNo()
          this.$message.info(
            '已发送部署请求,后台正在执行部署,请耐心等待',
            3,
          )
          deploy({
            id: this.application.id,
            backUpDescription: values.description
          }).then((resp) => {
            console.log(resp)
          })
        }
      })
    },

    handleCancel(value) {
      this.cancelVisible = true
      this.application = value
    },

    handleCancelNo() {
      this.cancelVisible = false
      this.formSavePoint.resetFields()
    },

    handleCancelOk() {
      this.formSavePoint.validateFields((err, values) => {
        if (!err) {
          this.handleCancelNo()
          this.$message.info(
            '已发送停止请求,该应用正在停止',
            3,
          )
          cancel({
            id: this.application.id,
            savePoint: values.savePoint,
            drain: values.drain
          }).then((resp) => {
            console.log(resp)
          })
        }
      })
    },

    handleDetail () {
      this.$router.push({'path': '/flink/app/detail'})
    },

    handleSearch() {
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

    handleReset() {
      // 取消选中
      this.selectedRowKeys = []
      // 重置列排序规则
      this.sortedInfo = null
      // 重置查询参数
      this.queryParams = {}
      // 清空时间选择
      this.$refs.createTime.reset()
      this.handleFetch()
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
      if (loading) this.loading = true
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
      window.open(this.yarn + "/proxy/" + params['appId'] + "/")
    },

    addTask() {
      this.$router.push({'path': '/flink/app/add'})
    },

    handleStartUp(app) {
      this.$message.info(
        '已发送启动请求,该应用正在启动中',
        3,
      )
      startUp({
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

<style scoped>
.ant-upload.ant-upload-drag p.ant-upload-drag-icon .anticon {
  font-size: 100px;
}

.app_state {
  width: 80px;
}

.app_state > .ant-tag {
  border-radius: 0;
  font-weight: 700;
  font-size: 13px;
  text-align: center;
  padding: 1px 5px;
  cursor: default;
}

.ant-modal-header {
  border-bottom: unset;
}

.ant-modal-footer {
  border-top: unset;
}

.ant-modal-body {
  padding-bottom: 5px;
  padding-top: 5px;
}
.ant-input-number {
  width: 100%;
}

</style>
