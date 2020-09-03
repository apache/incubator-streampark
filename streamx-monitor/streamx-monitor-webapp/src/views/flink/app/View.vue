<template>
  <a-card :bordered="false" style="margin-top: 20px;">
    <!-- 表格区域 -->
    <a-table
      ref="TableInfo"
      :columns="columns"
      size="middle"
      rowKey="id"
      class="app_list"
      :dataSource="dataSource"
      :pagination="pagination"
      :loading="loading"
      :scroll="{ x: 700 }"
      @change="handleTableChange">

      <div
        slot="filterDropdown"
        slot-scope="{ setSelectedKeys, selectedKeys, confirm, clearFilters, column }"
        style="padding: 8px">
        <a-input v-ant-ref="c => (searchInput = c)"
          :placeholder="`Search ${column.dataIndex}`"
          :value="selectedKeys[0]"
          style="width: 220px; margin-bottom: 8px; display: block;"
          @change="e => setSelectedKeys(e.target.value ? [e.target.value] : [])"
          @pressEnter="() => handleSearch(selectedKeys, confirm, column.dataIndex)"/>
        <a-button
          type="primary"
          icon="search"
          size="small"
          style="width: 90px; margin-right: 8px"
          @click="() => handleSearch(selectedKeys, confirm, column.dataIndex)">
          Search
        </a-button>
        <a-button size="small" style="width: 90px" @click="() => handleReset(clearFilters)">
          Reset
        </a-button>
      </div>

      <a-icon slot="filterIcon"
        slot-scope="filtered"
        type="search"
        :style="{ color: filtered ? '#108ee9' : undefined }"/>

      <template slot="filterRender" slot-scope="text, record, index, column">
        <template v-if="searchText && searchedColumn === column.dataIndex">
          <a-badge v-if="column.dataIndex === 'jobName' && record.deploy === 1" dot title="应用已更新,需重新发布">
            <template v-if="text.length>25">
              <a-tooltip placement="top">
                <template slot="title">
                  {{ text }}
                </template>
                <template v-for="(fragment, i) in text.substr(0,25).toString().split(new RegExp(`(?<=${searchText})|(?=${searchText})`, 'i'))">
                  <mark v-if="fragment.toLowerCase() === searchText.toLowerCase()" :key="i" class="highlight">
                    {{ fragment }}
                  </mark>
                  <template v-else>
                    {{ fragment }}
                  </template>
                </template>
                ...
              </a-tooltip>
            </template>
            <template v-else>
              <template v-for="(fragment, i) in text.toString().split(new RegExp(`(?<=${searchText})|(?=${searchText})`, 'i'))">
                <mark v-if="fragment.toLowerCase() === searchText.toLowerCase()" :key="i" class="highlight">
                  {{ fragment }}
                </mark>
                <template v-else>
                  {{ fragment }}
                </template>
              </template>
            </template>
          </a-badge>
          <template v-else>
             <template v-for="(fragment, i) in text.substr(0,25).toString().split(new RegExp(`(?<=${searchText})|(?=${searchText})`, 'i'))">
              <mark v-if="fragment.toLowerCase() === searchText.toLowerCase()" :key="i" class="highlight">
                {{ fragment }}
              </mark>
              <template v-else>
                {{ fragment }}
              </template>
            </template>
          </template>
        </template>
        <template v-else>
          <a-badge dot title="应用已更新,需重新发布" v-if="column.dataIndex === 'jobName' && record.deploy === 1">
            <ellipsis :length="45" tooltip>
              {{ text }}
            </ellipsis>
          </a-badge>
          <span v-else>
            <ellipsis :length="45" tooltip>
              {{ text }}
            </ellipsis>
          </span>
        </template>
      </template>

      <template slot="duration" slot-scope="text, record">
          {{ record.duration | duration }}
      </template>

      <template slot="endTime" slot-scope="text, record">
        <span v-if="record.endTime">
          {{ record.endTime}}
        </span>
        <span v-else> - </span>
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
          <a-tag color="#1ABBDC" v-if="state === 1" class="status-processing-deploying">DEPLOYING</a-tag>
          <a-tag color="#108ee9" v-if="state === 2">DEPLOYED</a-tag>
          <a-tag color="#1AB58E" v-if="state === 3" class="status-processing-starting">STARTING</a-tag>
          <a-tag color="#13c2c2" v-if="state === 4" class="status-processing-restarting">RESTARTING</a-tag>
          <a-tag color="#52c41a" v-if="state === 5" class="status-processing-running">RUNNING</a-tag>
          <a-tag color="#fa541c" v-if="state === 6" class="status-processing-failing">FAILING</a-tag>
          <a-tag color="#f5222d" v-if="state === 7">FAILED</a-tag>
          <a-tag color="#faad14" v-if="state === 8" class="status-processing-cancelling">CANCELLING</a-tag>
          <a-tag color="#fa8c16" v-if="state === 9" >CANCELED</a-tag>
          <a-tag color="#1890ff" v-if="state === 10">FINISHED</a-tag>
          <a-tag color="#722ed1" v-if="state === 11">SUSPENDED</a-tag>
          <a-tag color="#eb2f96" v-if="state === 12" class="status-processing-reconciling">RECONCILING</a-tag>
          <a-tag color="#000000" v-if="state === 13">LOST</a-tag>
        </div>
      </template>

      <template slot="customOperation">
        Operation
        <a-button
          v-permit="'role:delete'"
          type="primary"
          shape="circle"
          icon="plus"
          style="margin-left: 20px; width: 25px;height: 25px;min-width: 25px"
          @click="handleAdd">
        </a-button>
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
import Ellipsis from '@/components/Ellipsis'
import RangeDate from '@comp/DateTime/RangeDate'
import {list, remove, cancel, deploy, startUp, closeDeploy, yarn} from '@api/application'

export default {
  components: { RangeDate, Ellipsis},
  data() {
    return {
      loading: false,
      advanced: false,
      dataSource: [],
      selectedRowKeys: [],
      queryParams: {},
      sortedInfo: null,
      filteredInfo: null,
      yarn: null,
      deployVisible: false,
      cancelVisible: false,
      formDeploy: null,
      formSavePoint: null,
      application: null,
      searchText: '',
      searchInput: null,
      searchedColumn: '',
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
      let { sortedInfo, filteredInfo } = this
      sortedInfo = sortedInfo || {}
      filteredInfo = filteredInfo || {}
      return [{
        title: 'Job Name',
        dataIndex: 'jobName',
        width: 250,
        fixed: 'left',
        scopedSlots: {
          filterDropdown: 'filterDropdown',
          filterIcon: 'filterIcon',
          customRender: 'filterRender',
        },
        onFilter: (value, record) =>
          record.jobName
            .toString()
            .toLowerCase()
            .includes(value.toLowerCase()),
        onFilterDropdownVisibleChange: visible => {
          if (visible) {
            setTimeout(() => {
              this.searchInput.focus()
            })
          }
        }
      }, {
        title: 'Project',
        dataIndex: 'projectName',
        width: 200,
        scopedSlots: {
          filterDropdown: 'filterDropdown',
          filterIcon: 'filterIcon',
          customRender: 'filterRender',
        },
        onFilter: (value, record) =>
          record['projectName']
            .toString()
            .toLowerCase()
            .includes(value.toLowerCase()),
        onFilterDropdownVisibleChange: visible => {
          if (visible) {
            setTimeout(() => {
              this.searchInput.focus()
            })
          }
        }
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
        width: 150
      },{
        title: 'End Time',
        dataIndex: 'endTime',
        sorter: true,
        sortOrder: sortedInfo.columnKey === 'endTime' && sortedInfo.order,
        scopedSlots: {customRender: 'endTime'},
        width: 180
      }, {
        title: 'Status',
        dataIndex: 'state',
        width: 120,
        scopedSlots: {customRender: 'state'},
        filters: [
          { text: 'CREATED', value: 0 },
          { text: 'DEPLOYING', value: 1 },
          { text: 'DEPLOYED', value: 2 },
          { text: 'STARTING', value: 3 },
          { text: 'RESTARTING', value: 4 },
          { text: 'RUNNING', value: 5 },
          { text: 'FAILING', value: 6 },
          { text: 'FAILED', value: 7 },
          { text: 'CANCELED', value: 9 },
          { text: 'FINISHED', value: 10 },
          { text: 'LOST', value: 13 }
        ],
        fixed: 'right',
        filteredValue: filteredInfo.state || null,
        onFilter: (value, record) => {
          console.log(record.state === value)
          console.log(value + "------")
          return record.state === value
        },
        sorter: (a, b) => a.state - b.state,
        sortOrder: sortedInfo.columnKey === 'state' && sortedInfo.order
      }, {
        dataIndex: 'operation',
        key: 'operation',
        scopedSlots: {customRender: 'operation'},
        slots: { title: 'customOperation' },
        fixed: 'right',
        width: 150
      }]
    }
  },

  mounted() {
    this.handleYarn()
    this.handleFetch(true)
    //const timer = window.setInterval(() => this.handleFetch(false), 1000)
    this.$once('hook:beforeDestroy', () => {
    //  clearInterval(timer);
    })
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

    handleSearch(selectedKeys, confirm, dataIndex) {
      confirm()
      this.searchText = selectedKeys[0]
      this.searchedColumn = dataIndex
      this.queryParams[this.searchedColumn] = this.searchText
      const {sortedInfo} = this
      // 获取当前列的排序和列的过滤规则
      if (sortedInfo) {
        this.queryParams['sortField'] = sortedInfo.field
        this.queryParams['sortOrder'] = sortedInfo.order
      }
      console.log(this.queryParams)
    },

    handleReset(clearFilters) {
      clearFilters()
      this.searchText = ''
      // 重置列排序规则
      this.sortedInfo = null
      // 重置查询参数
      this.queryParams = {}
    },

    handleTableChange(pagination, filters, sorter) {
      this.sortedInfo = sorter
      this.queryParams['sortField'] = sorter.field
      this.queryParams['sortOrder'] = sorter.order
      this.handleFetch(true)
    },

    handleFetch(loading) {
      if (loading) this.loading = true
      const params = Object.assign(this.queryParams,{})
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

    handleAdd() {
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

    handleCloseDeploy(app) {
      closeDeploy({
        id: app.id
      }).then((resp) => {
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
  padding: 0 4px;
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

.close-deploy {
  left: 12px;
  font-size: 8px;
  font-weight: bold;
  top: -8px;
}

.app_list >>> .ant-table-thead > tr > td, .app_list >>> .ant-table-tbody > tr > td {
  padding: 9px 9px !important;
}

.status-processing-deploying{ animation: deploying 800ms ease-out infinite alternate; }
.status-processing-starting{ animation: starting 800ms ease-out infinite alternate; }
.status-processing-restarting{ animation: restarting 800ms ease-out infinite alternate; }
.status-processing-running { animation: running 800ms ease-out infinite alternate; }
.status-processing-failing { animation: failing 800ms ease-out infinite alternate; }
.status-processing-cancelling { animation: cancelling 800ms ease-out infinite alternate; }
.status-processing-reconciling { animation: reconciling 800ms ease-out infinite alternate; }

@keyframes deploying {
  0% {
    border-color: #1ABBDC;
    box-shadow: 0 0 1px #1ABBDC, inset 0 0 2px #1ABBDC;
  }
  100% {
    border-color: #1ABBDC;
    box-shadow: 0 0 10px #1ABBDC, inset 0 0 5px #1ABBDC;
  }
}

@keyframes starting {
  0% {
    border-color: #1AB58E;
    box-shadow: 0 0 1px #1AB58E, inset 0 0 2px #1AB58E;
  }
  100% {
    border-color: #1AB58E;
    box-shadow: 0 0 10px #1AB58E, inset 0 0 5px #1AB58E;
  }
}

@keyframes restarting {
  0% {
    border-color: #13c2c2;
    box-shadow: 0 0 1px #13c2c2, inset 0 0 2px #13c2c2;
  }
  100% {
    border-color: #13c2c2;
    box-shadow: 0 0 10px #13c2c2, inset 0 0 5px #13c2c2;
  }
}

@keyframes running {
  0% {
    border-color: #52c41a;
    box-shadow: 0 0 1px #52c41a, inset 0 0 2px #52c41a;
  }
  100% {
    border-color: #52c41a;
    box-shadow: 0 0 10px #52c41a, inset 0 0 5px #52c41a;
  }
}

@keyframes failing {
  0% {
    border-color: #fa541c;
    box-shadow: 0 0 1px #fa541c, inset 0 0 2px #fa541c;
  }
  100% {
    border-color: #fa541c;
    box-shadow: 0 0 10px #fa541c, inset 0 0 5px #fa541c;
  }
}


@keyframes cancelling {
  0% {
    border-color: #faad14;
    box-shadow: 0 0 1px #faad14, inset 0 0 2px #faad14;
  }
  100% {
    border-color: #faad14;
    box-shadow: 0 0 10px #faad14, inset 0 0 5px #faad14;
  }
}

@keyframes reconciling {
  0% {
    border-color: #eb2f96;
    box-shadow: 0 0 1px #eb2f96, inset 0 0 2px #eb2f96;
  }
  100% {
    border-color: #eb2f96;
    box-shadow: 0 0 10px #eb2f96, inset 0 0 5px #eb2f96;
  }
}

</style>
