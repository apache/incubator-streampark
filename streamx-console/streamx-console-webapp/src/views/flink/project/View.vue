<template>
  <div>
    <a-card
      style="margin-top: 24px"
      :bordered="false">
      <div slot="extra">
        <a-radio-group button-style="solid" default-value="">
          <a-radio-button @click="handleQuery()" value="">全部</a-radio-button>
          <a-radio-button @click="handleQuery(0)" value="0" >构建中</a-radio-button>
          <a-radio-button @click="handleQuery(-1)" value="-1">未构建</a-radio-button>
          <a-radio-button @click="handleQuery(1)" value="1">已构建</a-radio-button>
          <a-radio-button @click="handleQuery(2)" value="2">构建失败</a-radio-button>
        </a-radio-group>
        <a-input-search @search="handleSearch" style="margin-left: 16px; width: 272px;"/>
      </div>

      <div class="operate" v-permit="'project:create'">
        <a-button type="dashed" style="width: 100%" icon="plus" @click="handleAdd">添加</a-button>
      </div>

      <a-list size="large" :pagination="pagination">
        <a-list-item :key="index" v-for="(item, index) in dataSource">
          <a-list-item-meta>
            <icon-font slot="avatar" class="icon-font" type="icon-flink"></icon-font>
            <a slot="title">
              {{ item.name }}
              <a-badge status="processing" title="installing" v-if="item.buildState === 0"/>
            </a>
            <a-popover arrow-point-at-center trigger="hover" slot="description">
              <template slot="content">
                {{ item.url }}
              </template>
              <a-button style="border:unset;height:20px;background:unset;margin-left:0;padding-left:0px;">
                <ellipsis :length="controller.ellipsis">
                  {{ item.description }}
                </ellipsis>
              </a-button>
            </a-popover>
          </a-list-item-meta>

          <div class="list-content">
            <div class="list-content-item">
              <span>托管平台</span>
              <p>
                <a-icon type="github" two-tone-color></a-icon>
              </p>
            </div>
            <div class="list-content-item">
              <span>分支</span>
              <p>
                <a-tag color="blue">{{ item.branches }}</a-tag>
              </p>
            </div>
            <div class="list-content-item">
              <span>构建次数</span>
              <p>21</p>
            </div>
            <div class="list-content-item" style="width: 180px">
              <span>最后构建时间</span>
              <p v-if="item.lastBuild">{{ item.lastBuild }}</p>
              <p v-else>--</p>
            </div>
          </div>

          <div class="operation">
            <a-icon
              v-if="item.buildState === 0"
              type="sync"
              style="color:#4a9ff5"
              spin
              @click="handleSeeLog(item)"/>
            <a-popconfirm
              v-else
              v-permit="'project:build'"
              title="确定要编译该项目吗?"
              cancel-text="No"
              ok-text="Yes"
              @confirm="handleBuild(item)">
              <a-icon type="thunderbolt" theme="twoTone" twoToneColor="#4a9ff5"></a-icon>
            </a-popconfirm>

            <a-icon type="edit" v-permit="'project:update'" theme="twoTone" twoToneColor="#4a9ff5" style="width:30px;"></a-icon>
            <a-icon type="delete" v-permit="'project:delete'" theme="twoTone" twoToneColor="#4a9ff5" style="width:30px;"></a-icon>
          </div>
        </a-list-item>

      </a-list>
    </a-card>

    <a-modal
      v-model="controller.visible"
      width="65%"
      :bodyStyle="controller.modalStyle"
      :destroyOnClose="controller.modalDestroyOnClose"
      @ok="handleClose">
      <template slot="title">
        <a-icon type="code"/>&nbsp; {{ controller.consoleName }}
      </template>
      <template slot="footer">
        <a-button key="submit" type="primary" @click="handleClose">
          确定
        </a-button>
      </template>
      <div id="terminal" ref="terminal" class="terminal"></div>
    </a-modal>
  </div>
</template>
<script>
import RangeDate from '@comp/DateTime/RangeDate'
import { build, list } from '@api/project'
import Ellipsis from '@comp/Ellipsis'
import HeadInfo from '@comp/tools/HeadInfo'
import SockJS from 'sockjs-client'
import Stomp from 'webstomp-client'
import { Terminal } from 'xterm'
import 'xterm/css/xterm.css'

import { Icon } from 'ant-design-vue'
import baseUrl from '@/api/baseUrl'

const IconFont = Icon.createFromIconfontCN({
  scriptUrl: '//at.alicdn.com/t/font_2006309_d0bamxgl4wt.js'
})

export default {
  components: { IconFont, RangeDate, Ellipsis, HeadInfo },
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
        modalStyle: {
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

  },

  mounted () {
    this.handleFetch(this.queryParams, true)
    const timer = window.setInterval(() => this.handleFetch(this.queryParams, false), 2000)
    this.$once('hook:beforeDestroy', () => {
      clearInterval(timer)
    })
  },

  methods: {
    onSelectChange (selectedRowKeys) {
      console.log(selectedRowKeys)
      this.selectedRowKeys = selectedRowKeys
    },

    handleSearch (value) {
      this.paginationInfo = null
      this.handleFetch({
        name: value,
        ...this.queryParams
      }, true)
    },

    handleBuild (record) {
      this.$message.info(
        '已发送编译请求,后台正在更新代码并编译',
        3
      )
      build({
        id: record.id
      }).then(() => {
      })
    },

    handleAdd () {
      this.$router.push({ 'path': '/flink/project/add' })
    },

    handleSeeLog (project) {
      this.controller.consoleName = project.name + '构建日志'
      this.controller.visible = true
      this.$nextTick(function () {
        this.handleOpenWS(project)
      })
    },

    handleOpenWS (project) {
      const rows = parseInt(this.controller.modalStyle.height.replace('px', '')) / 16
      const cols = (document.querySelector('.terminal').offsetWidth - 10) / 8
      this.terminal = new Terminal({
        cursorBlink: true,
        rendererType: 'canvas',
        termName: 'xterm',
        useStyle: true,
        screenKeys: true,
        convertEol: true,
        scrollback: 1000,
        tabstopwidth: 4,
        disableStdin: true,
        rows: parseInt(rows), // 行数
        cols: parseInt(cols),
        fontSize: 14,
        cursorStyle: 'underline', // 光标样式
        theme: {
          foreground: '#AAAAAA', // 字体
          background: '#131D32', // 背景色
          lineHeight: 16
        }
      })
      const container = document.getElementById('terminal')
      this.terminal.open(container, true)
      const socket = new SockJS(baseUrl.concat('/websocket'))
      this.stompClient = Stomp.over(socket)
      this.stompClient.connect({}, (success) => {
        this.stompClient.subscribe('/resp/tail', (msg) => this.terminal.writeln(msg.body))
        this.stompClient.send('/req/tail/' + project.id)
      })
    },

    handleClose () {
      this.stompClient.disconnect()
      this.controller.visible = false
      this.terminal.clear()
      this.terminal.clearSelection()
      this.terminal = null
    },

    reset () {
      // 重置查询参数
      this.queryParams = {}
      this.handleFetch({}, true)
    },

    handleQuery (state) {
      this.queryParams.buildState = state
      this.handleFetch({
        ...this.queryParams
      }, true)
    },

    handleFetch (params, loading) {
      if (loading) {
        this.loading = true
      }
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
        pagination.total = parseInt(resp.data.total)
        this.dataSource = resp.data.records
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

.icon-font {
  font-size: 50px;
}

.gutter-box {
  padding: 10px 20px;
  background: #fff;
  color: rgba(0, 0, 0, 0.65);
  font-size: 14px;
  font-variant: tabular-nums;
  line-height: 1.5;
  list-style: none;
  -webkit-font-feature-settings: 'tnum';
  font-feature-settings: 'tnum';
  position: relative;
  border-radius: 2px;
  transition: all 0.3s;
}

.operation {
  width: 80px;
}

</style>
