<template>
  <div>
    <a-card
      style="margin-top: 24px"
      :bordered="false">
      <div slot="extra">
        <a-radio-group
          button-style="solid"
          default-value="">
          <a-radio-button
            @click="handleQuery()"
            value="">
            All
          </a-radio-button>
          <a-radio-button
            @click="handleQuery(-1)"
            value="-1">
            Not Build
          </a-radio-button>
          <a-radio-button
            @click="handleQuery(0)"
            value="0">
            Building
          </a-radio-button>
          <a-radio-button
            @click="handleQuery(1)"
            value="1">
            Build Success
          </a-radio-button>
          <a-radio-button
            @click="handleQuery(2)"
            value="2">
            Build Failed
          </a-radio-button>
        </a-radio-group>
        <a-input-search
          @search="handleSearch"
          style="margin-left: 16px; width: 272px;" />
      </div>

      <div
        class="operate"
        v-permit="'project:create'">
        <a-button
          type="dashed"
          style="width: 100%"
          icon="plus"
          @click="handleAdd">
          Add New
        </a-button>
      </div>

      <a-list
        size="large"
        :pagination="pagination">
        <a-list-item
          :key="index"
          v-for="(item, index) in dataSource">
          <a-list-item-meta>
            <a-badge class="build-badge" v-if="item.buildState === -2" slot="avatar" count="NEW" title="this project has changed,need rebuild">
              <svg-icon class="avatar" v-if="item.type === 1" name="flink" size="large"></svg-icon>
              <svg-icon class="avatar" v-if="item.type === 2" name="spark" size="large"></svg-icon>
            </a-badge>
            <template v-else>
              <svg-icon class="avatar" v-if="item.type === 1" name="flink" size="large" slot="avatar"></svg-icon>
              <svg-icon class="avatar" v-if="item.type === 2" name="spark" size="large" slot="avatar"></svg-icon>
            </template>
            <a slot="title">
              {{ item.name }}
              <a-badge
                status="processing"
                title="installing"
                v-if="item.buildState === 0" />
            </a>
            <a-popover
              arrow-point-at-center
              trigger="hover"
              slot="description">
              <template slot="content">
                {{ item.url }}
              </template>
              <a-button
                style="border:unset;height:20px;background:unset;margin-left:0;padding-left:0px;">
                <ellipsis
                  :length="controller.ellipsis">
                  {{ item.description }}
                </ellipsis>
              </a-button>
            </a-popover>
          </a-list-item-meta>

          <div class="list-content">
            <div
              class="list-content-item">
              <span>CVS</span>
              <p>
                <a-icon
                  type="github"
                  two-tone-color />
              </p>
            </div>
            <div class="list-content-item">
              <span>Branches</span>
              <p>
                <a-tag
                  color="blue">
                  {{ item.branches }}
                </a-tag>
              </p>
            </div>
            <div
              class="list-content-item"
              style="width: 180px">
              <span>Last Build</span>
              <p v-if="item.lastBuild">
                {{ item.lastBuild }}
              </p>
              <p v-else>
                --
              </p>
            </div>
            <div
              class="list-content-item"
              style="width: 150px">
              <span>Build State</span>
              <p v-if="item.buildState === -1">
                <a-tag color="#C0C0C0">NOT BUILD</a-tag>
              </p>
              <p v-if="item.buildState === -2">
                <a-tag color="#FFA500">NEED REBUILD</a-tag>
              </p>
              <p v-else-if="item.buildState === 0">
                <a-tag color="#1AB58E" class="status-processing-building">BUILDING</a-tag>
              </p>
              <p v-else-if="item.buildState === 1">
                <a-tag color="#52c41a">SUCCESSFUL</a-tag>
              </p>
              <p v-else>
                <a-tag color="#f5222d">FAILED</a-tag>
              </p>
            </div>
          </div>

          <div class="operation">

            <a-tooltip
              title="See Build log"
              v-if="item.buildState === 0">
              <a-button
                shape="circle"
                size="small"
                style="margin-left: 8px"
                @click.native="handleSeeLog(item)"
                class="control-button ctl-btn-color">
                <a-icon
                  spin
                  type="sync"
                  style="color:#4a9ff5"/>
              </a-button>
            </a-tooltip>

            <a-tooltip
              title="Build Project"
              v-if="item.buildState !== 0"
              v-permit="'project:build'">
              <a-popconfirm
                title="Are you sure build this project?"
                cancel-text="No"
                ok-text="Yes"
                @confirm="handleBuild(item)">
                <a-button
                  shape="circle"
                  size="small"
                  style="margin-left: 8px"
                  class="control-button ctl-btn-color">
                  <a-icon type="thunderbolt" />
                </a-button>
              </a-popconfirm>
            </a-tooltip>

            <a-tooltip title="Update Project">
              <a-button
                v-permit="'project:update'"
                @click.native="handleEdit(item)"
                shape="circle"
                size="small"
                style="margin-left: 8px"
                class="control-button ctl-btn-color">
                <a-icon type="edit"/>
              </a-button>
            </a-tooltip>

            <a-tooltip title="Delete Project">
              <a-popconfirm
                title="Are you sure delete this project ?"
                cancel-text="No"
                ok-text="Yes"
                @confirm="handleDelete(item)">
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

          </div>

        </a-list-item>
      </a-list>
    </a-card>

    <a-modal
      v-model="controller.visible"
      width="65%"
      :body-style="controller.modalStyle"
      :destroy-on-close="controller.modalDestroyOnClose"
      :footer="null"
      @cancel="handleClose">
      <template slot="title">
        <svg-icon name="code" />&nbsp;
        {{ controller.consoleName }}
      </template>
      <div
        id="terminal"
        ref="terminal"
        class="terminal" />
    </a-modal>
  </div>
</template>
<script>
import { build, buildlog, list,remove,closebuild } from '@api/project'
import Ellipsis from '@comp/Ellipsis'
import {mapActions} from 'vuex'
import { Terminal } from 'xterm'
import 'xterm/css/xterm.css'

import SvgIcon from '@/components/SvgIcon'
import {baseUrl} from '@/api/baseUrl'
import storage from '@/utils/storage'

export default {
  components: { Ellipsis, SvgIcon },
  data () {
    return {
      loading: false,
      advanced: false,
      dataSource: [],
      queryParams: {},
      sortedInfo: null,
      stompClient: null,
      terminal: null,
      projectId: null,
      socketId: null,
      storageKey: 'BUILD_SOCKET_ID',
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
        showTotal: (total, range) => `display ${range[0]} ~ ${range[1]} record，total ${total} `
      }
    }
  },

  mounted () {
    this.handleFetch(this.queryParams, true)
    const timer = window.setInterval(() => this.handleFetch(this.queryParams, false), 2000)
    this.$once('hook:beforeDestroy', () => {
      clearInterval(timer)
    })
  },

  methods: {
    ...mapActions(['SetProjectId']),

    handleSearch (value) {
      this.paginationInfo = null
      this.handleFetch({
        name: value,
        ...this.queryParams
      }, true)
    },

    handleBuild (record) {
      this.$swal.fire({
        icon: 'success',
        title: 'The current project is building',
        showConfirmButton: false,
        timer: 2000
      }).then((r)=> {
        this.socketId = this.uuid()
        storage.set(this.storageKey,this.socketId)
        build({
          id: record.id,
          socketId: this.socketId
        })
      })
    },

    handleAdd () {
      this.$router.push({ 'path': '/flink/project/add' })
    },

    handleEdit(item) {
      this.SetProjectId(item.id)
      this.$router.push({'path': '/flink/project/edit'})
    },

    handleDelete(item) {
      remove({
        id: item.id
      }).then((resp) => {
        if ( resp.data ) {
          this.$swal.fire({
            icon: 'success',
            title: 'delete successful',
            showConfirmButton: false,
            timer: 2000
          }).then((result) => {
            this.handleFetch(this.queryParams, true)
          })
        } else {
          this.$swal.fire(
            'Failed',
            'Please check if any application belongs to this project',
            'error'
          )
        }
      })
    },

    handleSeeLog (project) {
      this.controller.consoleName = project.name + ' build log'
      this.controller.visible = true
      this.$nextTick(function () {
        this.handleOpenWS(project)
      })
    },

    handleOpenWS (project) {
      this.projectId = project.id
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

      const url = baseUrl().concat('/websocket/' + this.handleGetSocketId())

      const socket = this.getSocket(url)

      socket.onopen = () => {
        buildlog({id:project.id})
      }

      socket.onmessage = (event) => {
        this.terminal.writeln(event.data)
      }

      socket.onclose = () => {
        this.socketId = null
        storage.rm(this.storageKey)
      }

    },

    handleGetSocketId() {
      if (this.socketId == null) {
        return storage.get(this.storageKey) || null
      }
      return this.socketId
    },

    handleClose () {
      closebuild({ id: this.projectId })
      this.stompClient.disconnect()
      this.controller.visible = false
      this.terminal.clear()
      this.terminal.clearSelection()
      this.terminal = null
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

.gutter-box {
  padding: 10px 20px;
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
  width: 120px;
}

.ant-tag {
  border-radius: 0;
  font-weight: 700;
  font-size: 12px;
  text-align: center;
  padding: 0 4px;
  margin-right: 0px;
  cursor: default;
}

.avatar {
  border-radius: 50%;
  background-color: #ebebeb;
  border: 6px solid #ebebeb;
}

.status-processing-building {
  animation: building-color 800ms ease-out infinite alternate;
}

@keyframes building-color {
  0% {
    border-color: #1AB58E;
    box-shadow: 0 0 1px #1AB58E, inset 0 0 2px #1AB58E;
  }
  100% {
    border-color: #1AB58E;
    box-shadow: 0 0 10px #1AB58E, inset 0 0 5px #1AB58E;
  }
}

.build-badge {
  font-size : 12px;
  -webkit-transform : scale(0.84,0.84) ;
  *font-size:10px;
}
</style>
