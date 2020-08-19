<template>
  <div>
    <a-row :gutter="24">
      <a-col class="gutter-row" :span="6">
        <div class="gutter-box">
          <apexchart type="donut" width="200" :options="chart.type.chartOptions" :series="chart.type.series"></apexchart>
          <a-divider style="margin-bottom: 10px"/>
          <div>
            <span>
              Total
              <strong>100</strong>
            </span>
            <a-divider type="vertical" />
            <span>
              Flink
              <strong>67</strong>
            </span>
            <a-divider type="vertical" />
            <span>
              Spark
              <strong>33</strong>
            </span>
          </div>
        </div>
      </a-col>
      <a-col class="gutter-row" :span="6">
        <div class="gutter-box">
          <apexchart type="donut" width="200" :options="chart.type.chartOptions" :series="chart.type.series"></apexchart>
          <a-divider style="margin-bottom: 10px"/>
          <div>
            <span>
              Total
              <strong>100</strong>
            </span>
            <a-divider type="vertical" />
            <span>
              Flink
              <strong>67</strong>
            </span>
            <a-divider type="vertical" />
            <span>
              Spark
              <strong>33</strong>
            </span>
          </div>
        </div>
      </a-col>
      <a-col class="gutter-row" :span="6">
        <div class="gutter-box">
          <apexchart type="area" height="100" :options="chartOptionsSpark3" :series="seriesSpark3"></apexchart>
          <a-divider style="margin-bottom: 10px"/>
          <div>
            <span>
              Total
              <strong>100</strong>
            </span>
            <a-divider type="vertical" />
            <span>
              Flink
              <strong>67</strong>
            </span>
            <a-divider type="vertical" />
            <span>
              Spark
              <strong>33</strong>
            </span>
          </div>
        </div>
      </a-col>
      <a-col class="gutter-row" :span="6">
        <div class="gutter-box">
          <apexchart type="area" height="100" :options="chartOptionsSpark3" :series="seriesSpark3"></apexchart>
          <a-divider style="margin-bottom: 10px"/>
          <div>
            <span>
              Total
              <strong>100</strong>
            </span>
            <a-divider type="vertical" />
            <span>
              Flink
              <strong>67</strong>
            </span>
            <a-divider type="vertical" />
            <span>
              Spark
              <strong>33</strong>
            </span>
          </div>
        </div>
      </a-col>
    </a-row>

    <a-card
      style="margin-top: 24px"
      :bordered="false">
      <div slot="extra">
        <a-radio-group button-style="solid" default-value=''>
          <a-radio-button @click="handleQuery()" value=''>全部</a-radio-button>
          <a-radio-button @click="handleQuery(0)" value="0" >构建中</a-radio-button>
          <a-radio-button @click="handleQuery(-1)" value="-1">未构建</a-radio-button>
          <a-radio-button @click="handleQuery(1)" value="1">已构建</a-radio-button>
          <a-radio-button @click="handleQuery(2)" value="2">构建失败</a-radio-button>
        </a-radio-group>
        <a-input-search @search="handleSearch" style="margin-left: 16px; width: 272px;"/>
      </div>

      <div class="operate">
        <a-button type="dashed" style="width: 100%" icon="plus" @click="handleAdd">添加</a-button>
      </div>

      <a-list size="large" :pagination="pagination">
        <a-list-item :key="index" v-for="(item, index) in dataSource">
          <a-list-item-meta>
            <icon-font slot="avatar" class="icon-font" type="icon-flink"></icon-font>
            <a slot="title">{{ item.name }}</a>
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

          <div slot="actions">
            <a-icon v-if="item.buildState === 0" type="sync" spin @click="handleSeeLog(item)"/>
            <a-popconfirm
              v-else
              title="确定要编译该项目吗?"
              cancel-text="No"
              ok-text="Yes"
              @confirm="handleBuild(item)">
              <a-icon type="thunderbolt"></a-icon>
            </a-popconfirm>
          </div>

          <div slot="actions">
            <a-dropdown>
              <a-menu slot="overlay">
                <a-menu-item><a>编辑项目</a></a-menu-item>
                <a-menu-item><a>删除项目</a></a-menu-item>
              </a-menu>
              <a>更多
                <a-icon type="down"/>
              </a>
            </a-dropdown>
          </div>
        </a-list-item>
      </a-list>
    </a-card>

    <a-modal v-model="controller.visible"
             width="65%"
             :bodyStyle="controller.modalStyle"
             :destroyOnClose='controller.modalDestroyOnClose'
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
import {build, list, remove} from '@api/project'
import VueApexCharts from "vue-apexcharts"
import Ellipsis from '@comp/Ellipsis'
import HeadInfo from "@comp/tools/HeadInfo"
import SockJS from 'sockjs-client'
import Stomp from 'webstomp-client'
import {Terminal} from "xterm"
import "xterm/css/xterm.css"
import "xterm/lib/xterm.js"
import {Icon} from 'ant-design-vue'

const IconFont = Icon.createFromIconfontCN({
  scriptUrl: '//at.alicdn.com/t/font_2006309_d0bamxgl4wt.js'
})

export default {
  components: {IconFont, RangeDate, Ellipsis, HeadInfo, VueApexCharts},
  data() {
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
      },

      seriesSpark3: [{
        data: [400, 12, 400, 243, 404,433,145,210,321,100,213,89,254]
      }],

      chartOptionsSpark3: {
        chart: {
          type: 'area',
          height: 140,
          sparkline: {
            enabled: true
          },
        },
        stroke: {
          curve: 'straight'
        },
        fill: {
          opacity: 0.3
        },
        xaxis: {
          crosshairs: {
            width: 1
          },
        },
        yaxis: {
          min: 0
        },
        title: {
          text: '13,965',
          offsetX: 0,
          style: {
            fontSize: '24px',
          }
        },
        subtitle: {
          text: 'Total Project',
          offsetX: 0,
          style: {
            fontSize: '14px',
          }
        }
      },

      chart: {
        state: {
          series: [{
            data: [400, 430, 448, 470, 540]
          }],
          chartOptions: {
            chart: {
              type: 'bar',
              height: 350
            },
            plotOptions: {
              bar: {
                horizontal: true,
              }
            },
            dataLabels: {
              enabled: false
            },
            xaxis: {
              categories: ['South Korea', 'Canada', 'United Kingdom', 'Netherlands', 'Italy'],
            }
          },
        },
        type: {
          series: [44, 55],
          chartOptions: {
            chart: {
              width: 240,
              type: 'donut',
            },
            dataLabels: {
              enabled: false
            },
            fill: {
              type: 'gradient',
            },
            labels: ['Flink', 'Spark'],
            responsive: [{
              breakpoint: 240,
              options: {
                chart: {
                  width: 240
                },
                legend: {
                  position: 'bottom'
                }
              }
            }]
          },
        }
      }
    }
  },
  computed: {
    columns() {
      let {sortedInfo} = this
      sortedInfo = sortedInfo || {}
      return [
      {
        title: '项目名称',
        dataIndex: 'name'
      },
      {
        title: 'Repository',
        dataIndex: 'repository',
        customRender: (text, row, index) => {
          switch (text) {
            case 1:
              return <a-icon type = "github"></a-icon>
            case 2:
              return <a-icon type = "medium"></a-icon>
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
            <a-popover arrow-point-at-center trigger = "hover" >
              <template slot = "content">
                {{ text }}
              </template>
            <a-button style = "border:unset;height:20px;background:unset;" >
              <ellipsis length = "50" >
                {{ text }}
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
              return <a-tag color = "red"> {{text}} </a-tag>
            default:
              return <a-tag color = "green"> {{text}} </a-tag>
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
        scopedSlots: {customRender: 'operation'},
        fixed: 'right',
        width: 120
      }]
    }
  },

  mounted() {
    this.fetch(this.queryParams, true)
    const timer = window.setInterval(() => this.fetch(this.queryParams, false), 1000)
    this.$once('hook:beforeDestroy', () => {
      clearInterval(timer);
    })
  },

  methods: {
    onSelectChange(selectedRowKeys) {
      console.log(selectedRowKeys)
      this.selectedRowKeys = selectedRowKeys
    },

    handleSearch(value) {
      this.paginationInfo = null
      this.fetch({
        name: value,
        ...this.queryParams
      }, true)
    },

    handleBuild(record) {
      this.$message.info(
        '已发送编译请求,后台正在更新代码并编译,您可以查询编译日志来查看进度',
        3,
      )
      build({
        id: record.id
      }).then(() => {
      })
    },

    handleAdd() {
      this.$router.push({'path': '/flink/project/add'})
    },

    handleSeeLog(project) {
      this.controller.consoleName = project.name + '构建日志'
      this.controller.visible = true
      this.$nextTick(function () {
        this.handleOpenWS(project)
      })
    },

    handleOpenWS(project) {
      const rows = parseInt(this.controller.modalStyle.height.replace("px", '')) / 16
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
      const socket = new SockJS('http://test2:10001/websocket')
      this.stompClient = Stomp.over(socket)
      this.stompClient.connect({}, (success) => {
        this.stompClient.subscribe('/resp/tail', (msg) => this.terminal.writeln(msg.body))
        this.stompClient.send("/req/tail/" + project.id)
      })
    },

    handleClose() {
      this.stompClient.disconnect()
      this.controller.visible = false
      this.terminal.clear()
      this.terminal.clearSelection()
      this.terminal = null
    },

    reset() {
      // 重置查询参数
      this.queryParams = {}
      this.fetch({}, true)
    },

    handleQuery(state) {
      this.queryParams.buildState = state
      this.fetch({
        ...this.queryParams
      },true)
    },

    fetch(params, loading) {
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
        const pagination = {...this.pagination}
        pagination.total = resp.data.total
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

</style>

