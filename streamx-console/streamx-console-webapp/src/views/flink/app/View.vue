<template>
  <div>
    <a-row :gutter="24">
      <a-col class="gutter-row" :span="6">
        <div class="gutter-box">
          <apexchart
            type="donut"
            width="200"
            :options="chart.type.chartOptions"
            :series="chart.type.series"></apexchart>
          <a-divider style="margin-bottom: 10px"/>
          <div>
            <span>
              Total
              <strong>100</strong>
            </span>
            <a-divider type="vertical"/>
            <span>
              Flink
              <strong>67</strong>
            </span>
            <a-divider type="vertical"/>
            <span>
              Spark
              <strong>33</strong>
            </span>
          </div>
        </div>
      </a-col>
      <a-col class="gutter-row" :span="6">
        <div class="gutter-box">
          <apexchart
            type="donut"
            width="200"
            :options="chart.type.chartOptions"
            :series="chart.type.series"></apexchart>
          <a-divider style="margin-bottom: 10px"/>
          <div>
            <span>
              Total
              <strong>100</strong>
            </span>
            <a-divider type="vertical"/>
            <span>
              Flink
              <strong>67</strong>
            </span>
            <a-divider type="vertical"/>
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
            <a-divider type="vertical"/>
            <span>
              Flink
              <strong>67</strong>
            </span>
            <a-divider type="vertical"/>
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
            <a-divider type="vertical"/>
            <span>
              Flink
              <strong>67</strong>
            </span>
            <a-divider type="vertical"/>
            <span>
              Spark
              <strong>33</strong>
            </span>
          </div>
        </div>
      </a-col>
    </a-row>

    <a-card :bordered="false" style="margin-top: 20px">

      <!-- 表格区域 -->
      <a-table
        ref="TableInfo"
        :columns="columns"
        size="middle"
        rowKey="id"
        class="app_list"
        style="margin-top: -24px"
        :dataSource="dataSource"
        :pagination="pagination"
        :loading="loading"
        :scroll="{ x: 700 }"
        @change="handleTableChange">

        <div
          slot="filterDropdown"
          slot-scope="{ setSelectedKeys, selectedKeys, confirm, clearFilters, column }"
          style="padding: 8px">
          <a-input
            v-ant-ref="c => (searchInput = c)"
            :placeholder="`Search ${column.title}`"
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

        <a-icon
          slot="filterIcon"
          slot-scope="filtered"
          type="search"
          :style="{ color: filtered ? '#108ee9' : undefined }"/>

        <template slot="filterRender" slot-scope="text, record, index, column">
          <!--有条件搜索-->
          <template v-if="searchText && searchedColumn === column.dataIndex">
            <span
              v-if="column.dataIndex === 'jobName'"
              :class="{pointer:record.state === 5}"
              @click="handleView(record)">
              <!--start: record.deploy === 0-->
              <template
                v-if="record.deploy === 0"
                v-for="(fragment, i) in text.trim().substr(0,25).toString().split(new RegExp(`(?<=${searchText})|(?=${searchText})`, 'i'))">
                <mark v-if="fragment.toLowerCase() === searchText.toLowerCase()" :key="i" class="highlight">
                  {{ fragment }}
                </mark>
                <template v-else>
                  {{ fragment }}
                </template>
              </template>
              <!--end: record.deploy === 0-->
              <!--start: record.deploy === 1-->
              <a-badge v-if="record.deploy === 1" dot title="应用已更新,需重新发布">
                <template v-if="text.length>25">
                  <a-tooltip placement="top">
                    <template slot="title">
                      {{ text }}
                    </template>
                    <template
                      v-for="(fragment, i) in text.substr(0,25).toString().split(new RegExp(`(?<=${searchText})|(?=${searchText})`, 'i'))">
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
                  <template
                    v-for="(fragment, i) in text.trim().toString().split(new RegExp(`(?<=${searchText})|(?=${searchText})`, 'i'))">
                    <mark v-if="fragment.toLowerCase() === searchText.toLowerCase()" :key="i" class="highlight">
                      {{ fragment }}
                    </mark>
                    <template v-else>
                      {{ fragment }}
                    </template>
                  </template>
                </template>
              </a-badge>
              <!-- end: record.deploy === 1-->
              <!--start: record.deploy === 2-->
              <a-badge dot color="blue" v-if="record.deploy === 2" title="配置已更新,需重启应用">
                <template v-if="text.length>25">
                  <a-tooltip placement="top">
                    <template slot="title">
                      {{ text }}
                    </template>
                    <template
                      v-for="(fragment, i) in text.trim().substr(0,25).toString().split(new RegExp(`(?<=${searchText})|(?=${searchText})`, 'i'))">
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
                  <template
                    v-for="(fragment, i) in text.trim().toString().split(new RegExp(`(?<=${searchText})|(?=${searchText})`, 'i'))">
                    <mark v-if="fragment.toLowerCase() === searchText.toLowerCase()" :key="i" class="highlight">
                      {{ fragment }}
                    </mark>
                    <template v-else>
                      {{ fragment }}
                    </template>
                  </template>
                </template>
              </a-badge>
              <!-- end: record.deploy === 2-->
              <!-- start: record.deploy === 3-->
              <a-badge dot color="blue" v-if="record.deploy === 3" title="程序已发布完成,需重启应用">
                <template v-if="text.length>25">
                  <a-tooltip placement="top">
                    <template slot="title">
                      {{ text }}
                    </template>
                    <template
                      v-for="(fragment, i) in text.trim().substr(0,25).toString().split(new RegExp(`(?<=${searchText})|(?=${searchText})`, 'i'))">
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
                  <template
                    v-for="(fragment, i) in text.trim().toString().split(new RegExp(`(?<=${searchText})|(?=${searchText})`, 'i'))">
                    <mark v-if="fragment.toLowerCase() === searchText.toLowerCase()" :key="i" class="highlight">
                      {{ fragment }}
                    </mark>
                    <template v-else>
                      {{ fragment }}
                    </template>
                  </template>
                </template>
              </a-badge>
              <!-- end: record.deploy === 3-->
              <a-badge
                class="close-deploy"
                @click.stop="handleCleanDeploy(record)"
                v-if="record.deploy !== 0"
                v-permit="'app:clean'">
                <a-icon slot="count" type="close" style="color: #333"/>
              </a-badge>
            </span>
            <!--其他字段-->
            <span v-else>
              <template
                v-for="(fragment, i) in text.trim().substr(0,25).toString().split(new RegExp(`(?<=${searchText})|(?=${searchText})`, 'i'))">
                <mark v-if="fragment.toLowerCase() === searchText.toLowerCase()" :key="i" class="highlight">
                  {{ fragment }}
                </mark>
                <template v-else>
                  {{ fragment }}
                </template>
              </template>
            </span>
          </template>
          <!--无条件搜索-->
          <template v-else>
            <span
              v-if="column.dataIndex === 'jobName'"
              :class="{pointer:record.state === 5}"
              @click="handleView(record)">
              <a-badge dot title="应用已更新,需重新发布" v-if="record.deploy === 1">
                <ellipsis :length="45" tooltip>
                  {{ text }}
                </ellipsis>
              </a-badge>
              <a-badge dot color="blue" title="配置已更新,需重启应用" v-else-if="record.deploy === 2">
                <ellipsis :length="45" tooltip>
                  {{ text }}
                </ellipsis>
              </a-badge>
              <a-badge dot color="blue" title="程序已发布完成,需重启应用" v-else-if="record.deploy === 3">
                <ellipsis :length="45" tooltip>
                  {{ text }}
                </ellipsis>
              </a-badge>
              <span v-else>
                <ellipsis :length="45" tooltip>
                  {{ text }}
                </ellipsis>
              </span>
              <a-badge
                class="close-deploy"
                @click.stop="handleCleanDeploy(record)"
                v-if="record.deploy !== 0"
                v-permit="'app:clean'">
                <a-icon slot="count" type="close" style="color: #333"/>
              </a-badge>
            </span>
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
            {{ record.endTime }}
          </span>
          <span v-else> - </span>
        </template>

        <template slot="state" slot-scope="state">
          <State :state="state"></State>
        </template>

        <template slot="customOperation">
          Operation
          <a-button
            v-permit="'app:create'"
            type="primary"
            shape="circle"
            icon="plus"
            style="margin-left: 20px; width: 25px;height: 25px;min-width: 25px"
            @click="handleAdd">
          </a-button>
        </template>

        <template slot="operation" slot-scope="text, record">
          <a-icon
            v-if="record.state !== 5"
            v-permit="'app:mapping'"
            type="deployment-unit"
            style="color:#4a9ff5"
            @click="handleMapping(record)">
          </a-icon>
          <icon-font
            type="icon-deploy"
            v-show="record.deploy === 1 && record.state !== 1 "
            v-permit="'app:deploy'"
            style="color:#4a9ff5"
            @click="handleDeploy(record)">
          </icon-font>
          <a-icon
            v-permit="'app:update'"
            type="setting"
            theme="twoTone"
            twoToneColor="#4a9ff5"
            @click="handleEdit(record)"
            title="修改角色">
          </a-icon>
          <a-icon
            type="play-circle"
            v-if="record.state === 0
              || record.state === 2
              || record.state === 7
              || record.state === 9
              || record.state === 10
              || record.state === 11
              || record.state === 13"
            v-permit="'app:start'"
            theme="twoTone"
            twoToneColor="#4a9ff5"
            @click="handleStart(record)">
          </a-icon>
          <a-icon
            type="poweroff"
            title="停止应用"
            v-permit="'app:stop'"
            style="color: #4a9ff5"
            v-show="record.state === 5"
            @click="handleStop(record)">
          </a-icon>

          <a-icon
            type="eye"
            v-permit="'app:detail'"
            theme="twoTone"
            twoToneColor="#4a9ff5"
            @click="handleDetail(record)"
            title="查看">
          </a-icon>

        </template>

      </a-table>

      <a-modal v-model="deployVisible" on-ok="handleDeployOk">
        <template slot="title">
          <a-icon slot="icon" type="upload" style="color: green"/>
          Deploy Application
        </template>
        <template slot="footer">
          <a-button key="back" @click="handleDeployCancel">
            取消
          </a-button>
          <a-button key="submit" type="primary" :loading="loading" @click="handleDeployOk">
            确定
          </a-button>
        </template>
        <a-form @submit="handleDeployOk" :form="formDeploy">
          <a-form-item
            v-if="application && application.state === 5 "
            label="restart"
            :labelCol="{lg: {span: 7}, sm: {span: 7}}"
            :wrapperCol="{lg: {span: 16}, sm: {span: 4} }">
            <a-switch
              checkedChildren="开"
              unCheckedChildren="关"
              checked-children="true"
              un-checked-children="false"
              placeholder="重启应用"
              v-model="restart"
              v-decorator="['restart']"/>
            <span class="conf-switch" style="color:darkgrey"> restart application after deploy</span>
          </a-form-item>
          <a-form-item
            v-if="restart"
            label="Savepoint"
            :labelCol="{lg: {span: 7}, sm: {span: 7}}"
            :wrapperCol="{lg: {span: 16}, sm: {span: 4} }">
            <a-switch
              checkedChildren="开"
              unCheckedChildren="关"
              checked-children="true"
              un-checked-children="false"
              v-model="savePoint"
              v-decorator="['savePoint']"/>
            <span class="conf-switch" style="color:darkgrey"> trigger savePoint before taking stoping </span>
          </a-form-item>
          <a-form-item
            v-if="restart"
            label="allow NonRestored"
            :labelCol="{lg: {span: 7}, sm: {span: 7}}"
            :wrapperCol="{lg: {span: 16}, sm: {span: 4} }">
            <a-switch
              checkedChildren="开"
              unCheckedChildren="关"
              checked-children="true"
              un-checked-children="false"
              v-model="allowNonRestoredState"
              v-decorator="['allowNonRestoredState']"/>
            <span class="conf-switch" style="color:darkgrey"> skip savepoint that cannot be restored </span>
          </a-form-item>
          <a-form-item
            label="backup desc"
            :labelCol="{lg: {span: 7}, sm: {span: 7}}"
            :wrapperCol="{lg: {span: 16}, sm: {span: 4} }">
            <a-textarea
              rows="3"
              placeholder="应用重新发布前会先备份当前的应用,请输入当前应用的备份描述信息,以便回滚版本时找回"
              v-decorator="['description',{ rules: [{ required: true, message: '请输入备份描述' } ]}]">
            </a-textarea>
          </a-form-item>
        </a-form>
      </a-modal>

      <a-modal v-model="startVisible" on-ok="handleStartOk">
        <template slot="title">
          <a-icon slot="icon" type="play-circle" style="color: green"/>
          Start application
        </template>

        <a-form @submit="handleStartOk" :form="formStartCheckPoint">
          <a-form-item
            label="from savepoint"
            :labelCol="{lg: {span: 7}, sm: {span: 7}}"
            :wrapperCol="{lg: {span: 16}, sm: {span: 4} }">
            <a-switch
              checkedChildren="开"
              unCheckedChildren="关"
              checked-children="true"
              un-checked-children="false"
              v-model="savePoint"
              v-decorator="['savePoint']"/>
            <span class="conf-switch" style="color:darkgrey"> restore the job from savepoint</span>
          </a-form-item>

          <a-form-item
            v-if="savePoint && !lastestSavePoint "
            label="savepoint"
            style="margin-bottom: 10px"
            :labelCol="{lg: {span: 7}, sm: {span: 7}}"
            :wrapperCol="{lg: {span: 16}, sm: {span: 4} }">
            <a-select
              mode="combobox"
              allowClear
              v-decorator="['savePointPath',{ rules: [{ required: true } ]}]">
              <a-select-option
                v-for="(k ,i) in historySavePoint"
                :key="i"
                :value="k.savePoint">
                <template>
                  <span style="color:#108ee9">
                    {{ k.savePoint.substr(k.savePoint.lastIndexOf('-') + 1) }}
                  </span>
                  <span style="float: right; color: darkgrey">
                    <a-icon type="clock-circle"/> {{ k.createTime }}
                  </span>
                </template>
              </a-select-option>
            </a-select>
            <span class="conf-switch" style="color:darkgrey"> restore the job from savepoint</span>
          </a-form-item>

          <a-form-item
            label="allow NonRestored"
            :labelCol="{lg: {span: 7}, sm: {span: 7}}"
            :wrapperCol="{lg: {span: 16}, sm: {span: 4} }">
            <a-switch
              checkedChildren="开"
              unCheckedChildren="关"
              checked-children="true"
              un-checked-children="false"
              v-model="allowNonRestoredState"
              v-decorator="['allowNonRestoredState']"/>
            <span class="conf-switch" style="color:darkgrey"> skip savepoint that cannot be restored </span>
          </a-form-item>
        </a-form>

        <template slot="footer">
          <a-button key="back" @click="handleStartCancel">
            取消
          </a-button>
          <a-button key="submit" type="primary" :loading="loading" @click="handleStartOk">
            确定
          </a-button>
        </template>
      </a-modal>
      <a-modal v-model="stopVisible" on-ok="handleStopOk">
        <template slot="title">
          <a-icon slot="icon" type="poweroff" style="color: red"/>
          Stop application
        </template>

        <a-form @submit="handleStopOk" :form="formStopSavePoint">
          <a-form-item
            label="Savepoint"
            :labelCol="{lg: {span: 5}, sm: {span: 5}}"
            :wrapperCol="{lg: {span: 17}, sm: {span: 5} }">
            <a-switch
              checkedChildren="开"
              unCheckedChildren="关"
              checked-children="true"
              un-checked-children="false"
              v-model="savePoint"
              v-decorator="['savePoint']"/>
            <span class="conf-switch" style="color:darkgrey"> trigger savePoint before taking stoping </span>
          </a-form-item>
          <a-form-item
            label="Drain"
            :labelCol="{lg: {span: 5}, sm: {span: 5}}"
            :wrapperCol="{lg: {span: 17}, sm: {span: 5} }">
            <a-switch
              checkedChildren="开"
              unCheckedChildren="关"
              checked-children="true"
              un-checked-children="false"
              placeholder="Send max watermark before taking stoping"
              v-model="drain"
              v-decorator="['drain']"/>
            <span class="conf-switch" style="color:darkgrey"> Send max watermark before stoping</span>
          </a-form-item>
        </a-form>

        <template slot="footer">
          <a-button key="back" @click="handleStopCancel">
            取消
          </a-button>
          <a-button key="submit" type="primary" :loading="loading" @click="handleStopOk">
            确定
          </a-button>
        </template>
      </a-modal>

      <a-modal v-model="mappingVisible" on-ok="handleMappingOk">
        <template slot="title">
          <a-icon slot="icon" type="deployment-unit" style="color: green"/>
          Mapping application
        </template>

        <a-form @submit="handleMappingOk" :form="formMapping">
          <a-form-item
            v-if="mappingVisible"
            label="Application Name"
            :labelCol="{lg: {span: 7}, sm: {span: 7}}"
            :wrapperCol="{lg: {span: 16}, sm: {span: 4} }">
            <a-alert :message="application.jobName" type="info"/>
          </a-form-item>
          <a-form-item
            label="Application Id"
            :labelCol="{lg: {span: 7}, sm: {span: 7}}"
            :wrapperCol="{lg: {span: 16}, sm: {span: 4} }">
            <a-input
              type="text"
              placeholder="请输入 ApplicationId"
              v-decorator="[ 'appId', {rules: [{ required: true, message: '请输入ApplicationId'}]} ]"/>
          </a-form-item>
          <a-form-item
            label="JobId"
            :labelCol="{lg: {span: 7}, sm: {span: 7}}"
            :wrapperCol="{lg: {span: 16}, sm: {span: 4} }">
            <a-input
              type="text"
              placeholder="请输入 JobId"
              v-decorator="[ 'jobId', {rules: [{ required: true, message: '请输入 JobId'}]} ]"/>
          </a-form-item>
        </a-form>

        <template slot="footer">
          <a-button key="back" @click="handleMappingCancel">
            取消
          </a-button>
          <a-button key="submit" type="primary" :loading="loading" @click="handleMappingOk">
            确定
          </a-button>
        </template>
      </a-modal>

    </a-card>
  </div>
</template>
<script>
import Ellipsis from '@/components/Ellipsis'
import RangeDate from '@comp/DateTime/RangeDate'
import State from './State'

import { mapActions } from 'vuex'
import { list, stop, deploy, mapping, start, clean, yarn } from '@api/application'
import { lastest, history } from '@api/savepoint'
import { Icon } from 'ant-design-vue'
const IconFont = Icon.createFromIconfontCN({
  scriptUrl: '//at.alicdn.com/t/font_2006309_bo5pga6ctds.js'
})
export default {
  components: { RangeDate, Ellipsis, State, IconFont },
  data () {
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
      stopVisible: false,
      startVisible: false,
      mappingVisible: false,
      formDeploy: null,
      formStopSavePoint: null,
      formStartCheckPoint: null,
      formMapping: null,
      drain: false,
      savePoint: true,
      restart: false,
      application: null,
      lastestSavePoint: null,
      historySavePoint: null,
      allowNonRestoredState: false,
      searchText: '',
      searchInput: null,
      searchedColumn: '',
      paginationInfo: null,
      pagination: {
        pageSizeOptions: ['10', '20', '30', '40', '100'],
        defaultCurrent: 1,
        defaultPageSize: 10,
        showQuickJumper: true,
        showSizeChanger: true,
        showTotal: (total, range) => `显示 ${range[0]} ~ ${range[1]} 条记录，共 ${total} 条记录`
      },
      seriesSpark3: [{
        data: [400, 12, 400, 243, 404, 433, 145, 210, 321, 100, 213, 89, 254]
      }],

      chartOptionsSpark3: {
        chart: {
          type: 'area',
          height: 140,
          sparkline: {
            enabled: true
          }
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
          }
        },
        yaxis: {
          min: 0
        },
        title: {
          text: '13,965',
          offsetX: 0,
          style: {
            fontSize: '24px'
          }
        },
        subtitle: {
          text: 'Total Project',
          offsetX: 0,
          style: {
            fontSize: '14px'
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
                horizontal: true
              }
            },
            dataLabels: {
              enabled: false
            },
            xaxis: {
              categories: ['South Korea', 'Canada', 'United Kingdom', 'Netherlands', 'Italy']
            }
          }
        },
        type: {
          series: [44, 55],
          chartOptions: {
            chart: {
              width: 240,
              type: 'donut'
            },
            dataLabels: {
              enabled: false
            },
            fill: {
              type: 'gradient'
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
          }
        }
      }
    }
  },

  computed: {
    columns () {
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
          customRender: 'filterRender'
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
          customRender: 'filterRender'
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
        scopedSlots: { customRender: 'duration' },
        width: 150
      }, {
        title: 'End Time',
        dataIndex: 'endTime',
        sorter: true,
        sortOrder: sortedInfo.columnKey === 'endTime' && sortedInfo.order,
        scopedSlots: { customRender: 'endTime' },
        width: 180
      }, {
        title: 'Status',
        dataIndex: 'state',
        width: 120,
        scopedSlots: { customRender: 'state' },
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
          return record.state === value
        },
        sorter: (a, b) => a.state - b.state,
        sortOrder: sortedInfo.columnKey === 'state' && sortedInfo.order
      }, {
        dataIndex: 'operation',
        key: 'operation',
        scopedSlots: { customRender: 'operation' },
        slots: { title: 'customOperation' },
        fixed: 'right',
        width: 150
      }]
    }
  },

  mounted () {
    this.handleYarn()
    this.handleFetch(true)
    const timer = window.setInterval(() => this.handleFetch(false), 2000)
    this.$once('hook:beforeDestroy', () => {
      clearInterval(timer)
    })
  },

  beforeMount () {
    this.formDeploy = this.$form.createForm(this)
    this.formStopSavePoint = this.$form.createForm(this)
    this.formStartCheckPoint = this.$form.createForm(this)
    this.formMapping = this.$form.createForm(this)
  },

  methods: {
    ...mapActions(['SetAppId']),
    onSelectChange (selectedRowKeys) {
      console.log(selectedRowKeys)
      this.selectedRowKeys = selectedRowKeys
    },

    handleDateChange (value) {
      if (value) {
        this.queryParams.dateFrom = value[0]
        this.queryParams.dateTo = value[1]
      }
    },

    handleDeploy (value) {
      this.deployVisible = true
      this.application = value
    },

    handleDeployCancel () {
      this.deployVisible = false
      this.application = null
      this.restart = false
      this.allowNonRestoredState = false
      this.savePoint = true
      this.formDeploy.resetFields()
    },

    handleDeployOk () {
      this.formDeploy.validateFields((err, values) => {
        if (!err) {
          const id = this.application.id
          const savePoint = this.savePoint
          const description = values.description
          const restart = this.restart
          const allowNonRestoredState = this.allowNonRestoredState
          this.handleDeployCancel()
          this.$message.info(
            '已发送部署请求,后台正在执行部署,请耐心等待',
            3
          )
          deploy({
            id: id,
            restart: restart,
            savePointed: savePoint,
            allowNonRestored: allowNonRestoredState,
            backUpDescription: description
          }).then((resp) => {
            console.log(resp)
          })
        }
      })
    },

    handleMapping (app) {
      this.mappingVisible = true
      this.application = app
    },

    handleMappingOk () {
      this.formMapping.validateFields((err, values) => {
        if (!err) {
          this.$message.info(
            '已发送手动映射请求,请稍后',
            3
          )
          const appId = values.appId
          const jobId = values.jobId
          const id = this.application.id
          this.handleMappingCancel()
          mapping({
            id: id,
            appId: appId,
            jobId: jobId
          }).then((resp) => {
            console.log(resp)
          })
        }
      })
    },

    handleMappingCancel () {
      this.mappingVisible = false
      this.application = null
      this.formMapping.resetFields()
    },

    handleStart (app) {
      this.application = app
      lastest({
        appId: this.application.id
      }).then((resp) => {
        this.lastestSavePoint = resp.data || null
        this.startVisible = true
        if (!this.lastestSavePoint) {
          history({
            appId: this.application.id
          }).then((resp) => {
            this.historySavePoint = resp.data || []
          })
        }
      })
    },

    handleStartCancel () {
      this.startVisible = false
      this.allowNonRestoredState = false
      this.formStartCheckPoint.resetFields()
      this.application = null
      this.savePoint = true
    },

    handleStartOk () {
      this.formStartCheckPoint.validateFields((err, values) => {
        if (!err) {
          this.$message.info(
            '已发送启动请求,该应用正在启动中',
            3
          )
          const id = this.application.id
          const savePointed = this.savePoint
          const savePoint = savePointed ? (values.savePointPath || this.lastestSavePoint.savePoint) : null
          const allowNonRestoredState = this.allowNonRestoredState
          this.handleStartCancel()
          start({
            id: id,
            savePointed: savePointed,
            savePoint: savePoint,
            allowNonRestored: allowNonRestoredState
          }).then((resp) => {
            if (resp.data === 'false') {
              this.$message.error(
                '应用启动失败',
                3
              )
            }
          })
        }
      })
    },

    handleStop (value) {
      this.stopVisible = true
      this.application = value
    },

    handleStopCancel () {
      this.stopVisible = false
      this.formStopSavePoint.resetFields()
      this.drain = false
      this.savePoint = true
      this.application = null
    },

    handleStopOk () {
      this.$message.info(
        '已发送停止请求,该应用正在停止',
        3
      )
      const savePoint = this.savePoint
      const drain = this.drain
      const id = this.application.id
      this.handleStopCancel()
      stop({
        id: id,
        savePointed: savePoint,
        drain: drain
      }).then((resp) => {
        console.log(resp)
      })
    },

    handleDetail (app) {
      this.SetAppId(app.id)
      this.$router.push({ 'path': '/flink/app/detail' })
    },

    handleSearch (selectedKeys, confirm, dataIndex) {
      confirm()
      this.searchText = selectedKeys[0]
      this.searchedColumn = dataIndex
      this.queryParams[this.searchedColumn] = this.searchText
      const { sortedInfo } = this
      // 获取当前列的排序和列的过滤规则
      if (sortedInfo) {
        this.queryParams['sortField'] = sortedInfo.field
        this.queryParams['sortOrder'] = sortedInfo.order
      }
      console.log(this.queryParams)
    },

    handleReset (clearFilters) {
      clearFilters()
      this.searchText = ''
      // 重置列排序规则
      this.sortedInfo = null
      // 重置查询参数
      this.queryParams = {}
    },

    handleTableChange (pagination, filters, sorter) {
      this.sortedInfo = sorter
      this.paginationInfo = pagination
      this.queryParams['sortField'] = sorter.field
      this.queryParams['sortOrder'] = sorter.order
      this.handleFetch(true)
    },

    handleFetch (loading) {
      if (loading) this.loading = true
      const params = Object.assign(this.queryParams, {})
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
        const pagination = { ...this.pagination }
        pagination.total = parseInt(resp.data.total)
        this.dataSource = resp.data.records
        this.pagination = pagination
      })
    },

    handleYarn (params = {}) {
      yarn({}).then((resp) => {
        this.yarn = resp.data
      })
    },

    handleView (params) {
      if (params.state === 5) {
        const url = this.yarn + '/proxy/' + params['appId'] + '/'
        window.open(url)
      }
    },

    handleAdd () {
      this.$router.push({ 'path': '/flink/app/add' })
    },

    handleEdit (app) {
      this.SetAppId(app.id)
      if (app.appType == 1) {
        this.$router.push({ 'path': '/flink/app/edit_streamx' })
      } else {
        this.$router.push({ 'path': '/flink/app/edit_flink' })
      }
    },

    handleCleanDeploy (app) {
      clean({
        id: app.id
      }).then((resp) => {
      })
    },

    exportExcel () {

    }

  }
}
</script>

<style scoped>

.ant-upload.ant-upload-drag p.ant-upload-drag-icon .anticon {
  font-size: 100px;
}

.ant-alert.ant-alert-no-icon {
  padding: 6px 15px;
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
  left: 15px;
  font-size: 8px;
  font-weight: bold;
  top: -8px;
}

.app_list >>> .ant-table-thead > tr > td, .app_list >>> .ant-table-tbody > tr > td {
  padding: 9px 9px !important;
}

.app_list >>> .ant-table-thead > tr > th {
  background: unset !important;
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

>>> .ant-badge-dot, .ant-badge {
  right: unset !important;
}

.pointer {
  cursor: pointer;
}
</style>
