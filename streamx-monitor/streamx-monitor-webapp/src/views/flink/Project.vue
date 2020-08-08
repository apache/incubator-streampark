<template>
  <div>
    <a-card :bordered="false">
      <a-row>
        <a-col :sm="8" :xs="24">
          <head-info title="我的待办" content="8个任务" :bordered="true"/>
        </a-col>
        <a-col :sm="8" :xs="24">
          <head-info title="本周任务平均处理时间" content="32分钟" :bordered="true"/>
        </a-col>
        <a-col :sm="8" :xs="24">
          <head-info title="本周完成任务数" content="24个"/>
        </a-col>
      </a-row>
    </a-card>

    <a-card
      style="margin-top: 24px"
      :bordered="false">
      <div slot="extra">
        <a-radio-group>
          <a-radio-button>全部</a-radio-button>
          <a-radio-button>构建中</a-radio-button>
          <a-radio-button>未构建</a-radio-button>
          <a-radio-button>已构建</a-radio-button>
          <a-radio-button>构建失败</a-radio-button>
        </a-radio-group>
        <a-input-search style="margin-left: 16px; width: 272px;" />
      </div>

    <div class="operate">
      <a-button type="dashed" style="width: 100%" icon="plus" @click="handleAdd">添加</a-button>
    </div>
    <a-list size="large" :pagination="{showSizeChanger: true, showQuickJumper: true, pageSize: 5, total: 50}">
      <a-list-item  :key="index" v-for="(item, index) in dataSource">
        <a-list-item-meta>
          <a-avatar slot="avatar" size="large" src="~@/assets/icons/flink.svg"/>
          <a slot="title">{{ item.name }}</a>
          <a-popover arrow-point-at-center trigger="hover" slot="description">
            <template slot="content">
              {{item.url}}
            </template>
            <a-button style="border:unset;height:20px;background:unset;margin-left:0;padding-left:0px;">
              <ellipsis :length="controller.ellipsis">
                {{item.description}}
              </ellipsis>
            </a-button>
          </a-popover>
        </a-list-item-meta>

        <div class="list-content">
          <div class="list-content-item">
            <span>托管平台</span>
            <p><a-icon type="github" two-tone-color></a-icon></p>
          </div>
          <div class="list-content-item">
            <span>分支</span>
            <p><a-tag color="blue">{{ item.branches }}</a-tag></p>
          </div>
          <div class="list-content-item">
            <span>构建次数</span>
            <p>21</p>
          </div>
          <div class="list-content-item">
            <span>最后构建时间</span>
            <p>{{ item.lastBuild }}</p>
          </div>
        </div>

        <div slot="actions" v-show="controller.building || item.buildState == 0">
          <a-icon type="sync" spin @click="handleSeeLog(item)"/>
        </div>

        <div slot="actions">
          <a-dropdown>
            <a-menu slot="overlay">
              <a-menu-item><a>编辑项目</a></a-menu-item>
              <a-menu-item>
                <a-popconfirm
                  title="确定要pull最新代码并重新编译该项目吗?"
                  cancel-text="No"
                  ok-text="Yes"
                  @confirm="handleBuild(item)"
                >
                 <span>更新 & 编译</span>
                </a-popconfirm>
              </a-menu-item>
              <a-menu-item><a>删除项目</a></a-menu-item>
            </a-menu>
            <a>更多<a-icon type="down"/></a>
          </a-dropdown>
        </div>
      </a-list-item>
    </a-list>
    </a-card>

    <a-modal v-model="controller.visible" width="65%" :bodyStyle="controller.modalStyle" :destroyOnClose='controller.modalDestroyOnClose' @ok="handleClose">
      <template slot="title">
        <a-icon type="code" />&nbsp; {{controller.consoleName}}
      </template>
      <template slot="footer">
        <a-button key="submit" type="primary" @click="handleClose">
          确定
        </a-button>
      </template>
      <div id="terminal" ref="terminal" class="terminal"></div>
    </a-modal>

    <!--  <a-card :bordered="false" style="margin-top: 20px;">
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
        </div>-->
      <!-- 表格区域 -->
    <!--
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
      </a-table>-->

  </div>

</template>
<script>
import RangeDate from '@/components/DateTime/RangeDate'
import { build, list, remove } from '@/api/project'
import Ellipsis from '@/components/Ellipsis'
import HeadInfo from "@comp/tools/HeadInfo"
import SockJS from 'sockjs-client'
import Stomp from 'webstomp-client'
import { Terminal } from "xterm"
import "xterm/css/xterm.css"
import "xterm/lib/xterm.js"

export default {
  components: { RangeDate,Ellipsis,HeadInfo },
  data () {
    return {
      loading: false,
      advanced: false,
      dataSource: [],
      selectedRowKeys: [],
      queryParams: {},
      sortedInfo: null,
      stompClient: null,
      terminal: null,
      controller: {
        ellipsis: 100,
        building: false,
        modalStyle : {
          height: '600px',
          padding: '5px'
        },
        visible: false,
        modalDestroyOnClose: true,
        consoleName: null
      },
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
    columns () {
      let { sortedInfo } = this
      sortedInfo = sortedInfo || {}
      return [{
        title: '项目名称',
        dataIndex: 'name'
      }, {
        title: 'Repository',
        dataIndex: 'repository',
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
        title: 'Repository URL',
        dataIndex: 'url',
        customRender: (text, row, index) => {
         return <div>
           <a-popover arrow-point-at-center trigger="hover">
             <template slot="content">
               {{text}}
             </template>
             <a-button style="border:unset;height:20px;background:unset;">
               <ellipsis length="50">
                 {{text}}
               </ellipsis>
             </a-button>
           </a-popover>
         </div>
        },
      },
        {
          title: 'Branches',
          dataIndex: 'branches',
          customRender: (text, row, index) => {
            switch (text) {
              case 'master':
                return <a-tag color="red">{{text}}</a-tag>
              default:
                return <a-tag color="green">{{text}}</a-tag>
            }
          }
        },
        {
        title: 'Last Build',
        dataIndex: 'lastBuild'
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
        icon: <a-icon type="smile" style="color: #108ee9" />
      })
      build({
        id: record.id
      }).then(() => {
      })
      this.controller.building = true
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

    handleAdd () {
      this.$router.push({ 'path': 'addproject' })
    },

    handleSeeLog (project) {
      this.controller.consoleName = project.name + '构建日志'
      this.controller.visible = true
      this.$nextTick(function (){
        this.handleOpenWS(project)
      })
    },

    handleOpenWS(project) {
      const rows = parseInt(this.controller.modalStyle.height.replace("px",'')) / 16
      const cols = (document.querySelector(".terminal").offsetWidth - 10) / 8
      this.terminal = new Terminal({
        cursorBlink: true,
        rendererType: 'canvas',
        termName: "xterm",
        useStyle: true,
        screenKeys: true,
        convertEol: true,
        scrollback: 1000,
        tabstopwidth: 4,
        disableStdin: true,
        rows: parseInt(rows), //行数
        cols: parseInt(cols),
        fontSize: 14,
        cursorStyle: "underline", //光标样式
        theme: {
          foreground: "#AAAAAA", //字体
          background: "#131D32", //背景色
          lineHeight: 16
        }
      })
      const container = document.getElementById("terminal")
      this.terminal.open(container, true)
      const socket = new SockJS('http://localhost:10001/websocket')
      this.stompClient = Stomp.over(socket)
      this.stompClient.connect({}, (success) => {
        this.stompClient.subscribe('/resp/tail', (msg) => this.terminal.writeln(msg.body) )
        this.stompClient.send("/req/tail/"+project.id)
      })
    },

    handleClose() {
      this.stompClient.disconnect()
      this.controller.visible = false
      this.terminal.clear()
      this.terminal.clearSelection()
      this.terminal = null
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
        console.log(this.dataSource)
        this.pagination = pagination
        this.loading = false
      })
    }
  }
}
</script>

<style lang="less" scoped>
.ant-avatar-lg {
  width: 48px;
  height: 48px;
  line-height: 48px;
}

.list-content-item {
  color: rgba(0, 0, 0, .45);
  display: inline-block;
  vertical-align: middle;
  font-size: 14px;
  margin-left: 40px;
  span {
    line-height: 20px;
  }
  p {
    margin-top: 4px;
    margin-bottom: 0;
    line-height: 22px;
  }
}
</style>

