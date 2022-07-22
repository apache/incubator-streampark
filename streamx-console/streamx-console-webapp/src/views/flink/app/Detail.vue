<template>
  <a-card :bordered="false" style="margin-top: 20px;">

    <a-descriptions
      v-if="app"
      bordered
      size="middle"
      layout="vertical">
      <template slot="title">
        <span class="app-bar">Application Info</span>
        <a-button
          type="primary"
          shape="circle"
          icon="arrow-left"
          @click="handleGoBack()"
          style="float: right;margin-top: -8px"/>
        <a-button
          type="danger"
          icon="cloud"
          @click="handleView"
          :disabled="this.app.state !== 5 || (this.yarn === null && this.app.flinkRestUrl === null)"
          style="float: right;margin-top: -8px;margin-right: 20px">Flink Web UI
        </a-button>
        <a-divider
          style="margin-top: 5px;margin-bottom: -5px"/>
      </template>

      <a-descriptions-item
        label="ID">
        <span
          class="link pointer"
          v-clipboard:copy="app.id"
          v-clipboard:success="handleCopySuccess">
          {{ app.id }}
        </span>
      </a-descriptions-item>
      <a-descriptions-item
        label="Application Name">
        {{ app.jobName }}
      </a-descriptions-item>
      <a-descriptions-item
        label="Development Mode">
        <div class="app_state">
          <a-tag
            color="#545454"
            v-if="app.jobType === 1">
            Custom Code
          </a-tag>
          <a-tag
            color="#0C7EF2"
            v-if="app.jobType === 2">
            Flink SQL
          </a-tag>
        </div>
      </a-descriptions-item>
      <a-descriptions-item
        label="Module"
        v-if="app.jobType!==2">
        {{ app.module }}
      </a-descriptions-item>
      <a-descriptions-item
        label="Project"
        v-if="app.jobType!==2">
        {{ app.projectName }}
      </a-descriptions-item>
      <a-descriptions-item
        label="Application Type">
        <div v-if="app.appType == 1">
          <a-tag
            color="cyan">
            StreamX Flink
          </a-tag>
        </div>
        <div v-else-if="app.appType == 2">
          <a-tag
            color="blue">
            Apache Flink
          </a-tag>
        </div>
      </a-descriptions-item>
      <a-descriptions-item
        label="Status">
        <State
          option="state"
          :data="app"/>
      </a-descriptions-item>
      <a-descriptions-item
        label="Start Time">
        <template v-if="app.startTime">
          <a-icon type="clock-circle"/>
          {{ app.startTime }}
        </template>
      </a-descriptions-item>
      <a-descriptions-item
        v-if="app.endTime"
        label="End Time">
        <a-icon type="clock-circle"/>
        {{ app.endTime }}
      </a-descriptions-item>
      <a-descriptions-item
        v-if="app.duration"
        label="Duration">
        {{ app.duration | duration }}
      </a-descriptions-item>
      <a-descriptions-item
        label="Description"
        :span="3">
        {{ app.description }}
      </a-descriptions-item>

      <a-descriptions-item
        :span="3">
        <template slot="label">
          Rest Api
          <a-tooltip placement="top">
            <template slot="title">
              <span>Rest API external call interface,other third-party systems easy to access streamx</span>
            </template>
            <a-icon type="question-circle" style="color: red"/>
          </a-tooltip>
        </template>
        <a-button
          type="primary"
          shape="round"
          icon="copy"
          size="default"
          style="margin:0 3px;padding: 0 5px"
          @click.native="handleCopyCurl(api.Application.START.toString())">Copy Start cURL
        </a-button>
        <a-button
          type="primary"
          shape="round"
          icon="copy"
          size="default"
          style="margin:0 3px;padding: 0 5px"
          @click.native="handleCopyCurl(api.Application.CANCEL.toString())">Copy Cancel cURL
        </a-button>
        <a-button
          type="link"
          shape="round"
          icon="link"
          size="default"
          @click.native="handleDocPage()"
          style="margin:0 3px;padding: 0 5px">Api Doc Center
        </a-button>
      </a-descriptions-item>

    </a-descriptions>

    <a-divider
      style="margin-top: 20px;margin-bottom: -17px"/>
    <a-tabs
      v-if="app"
      :defaultActiveKey="1"
      style="margin-top: 15px"
      :animated="animated"
      :tab-bar-gutter="tabBarGutter"
      @change="handleChangeTab">
      <a-tab-pane
        key="1"
        tab="Option"
        force-render>
        <a-descriptions
          bordered
          size="middle"
          layout="vertical">
          <a-descriptions-item
            v-for="(v,k) in options"
            :key="k">
            <template
              slot="label">
              {{ k }}
            </template>
            {{ v }}
          </a-descriptions-item>
        </a-descriptions>
      </a-tab-pane>
      <a-tab-pane
        key="2"
        tab="Configuration"
        v-if="app && app.appType == 1 && allConfigVersions != null && allConfigVersions.length > 0">
        <a-descriptions>
          <a-descriptions-item
            class="desc-item">
            <a-table
              ref="TableConf"
              :columns="column.conf"
              size="middle"
              row-key="id"
              style="margin-top: -24px"
              :data-source="configVersions"
              :pagination="pagination.config"
              :loading="pager.config.loading"
              @change="handleTableChange"
              class="detail-table">
              <template
                slot="format"
                slot-scope="text, record">
                <a-tag
                  color="#2db7f5"
                  v-if="record.format == 1">
                  yaml
                </a-tag>
                <a-tag
                  color="#108ee9"
                  v-if="record.format == 2">
                  properties
                </a-tag>
              </template>
              <template
                slot="version"
                slot-scope="text, record">
                <a-button
                  type="primary"
                  shape="circle"
                  size="small"
                  style="margin-right: 10px;">
                  {{ record.version }}
                </a-button>
              </template>
              <template
                slot="effective"
                slot-scope="text, record">
                <a-tag
                  color="green"
                  v-if="record.effective">
                  Effective
                </a-tag>
              </template>
              <template
                slot="effective"
                slot-scope="text, record">
                <a-tag
                  color="cyan"
                  v-if="record.latest">
                  Latest
                </a-tag>
              </template>
              <template
                slot="createTime"
                slot-scope="text, record">
                <a-icon
                  type="clock-circle"/>
                {{ record.createTime }}
              </template>
              <template
                slot="operation"
                slot-scope="text, record">

                <a-tooltip title="View Config Detail">
                  <a-button
                    @click.native="handleConfDetail(record)"
                    shape="circle"
                    size="small"
                    class="control-button ctl-btn-color">
                    <a-icon type="eye"/>
                  </a-button>
                </a-tooltip>

                <a-tooltip title="Compare Config">
                  <a-button
                    v-if="configVersions.length>1"
                    @click.native="handleCompare(record)"
                    shape="circle"
                    size="small"
                    class="control-button ctl-btn-color">
                    <a-icon type="swap"/>
                  </a-button>
                </a-tooltip>

                <a-popconfirm
                  v-if="!record.effective"
                  v-permit="'conf:delete'"
                  title="Are you sure delete this record ?"
                  cancel-text="No"
                  ok-text="Yes"
                  @confirm="handleDeleteConf(record)">
                  <a-button
                    type="danger"
                    shape="circle"
                    size="small"
                    class="control-button">
                    <a-icon type="delete"/>
                  </a-button>
                </a-popconfirm>

              </template>
            </a-table>
          </a-descriptions-item>
        </a-descriptions>
      </a-tab-pane>
      <a-tab-pane
        key="3"
        tab="Flink SQL"
        v-if="app.jobType === 2">
        <div class="sql-box syntax-true" id="flink-sql" style="height: 600px;"></div>
      </a-tab-pane>
      <a-tab-pane
        key="4"
        tab="Savepoints"
        v-if="app && savePoints && savePoints.length>0">
        <a-descriptions>
          <a-descriptions-item
            class="desc-item">
            <a-table
              ref="TableSavePoints"
              :columns="column.savePoints"
              size="middle"
              row-key="id"
              style="margin-top: -24px"
              :data-source="savePoints"
              :pagination="pagination.savePoints"
              :loading="pager.savePoints.loading"
              class="detail-table">
              <template
                slot="triggerTime"
                slot-scope="text, record">
                <a-icon
                  type="clock-circle"/>
                {{ record.triggerTime }}
              </template>
              <template
                slot="type"
                slot-scope="text, record">
                <div
                  class="app_state">
                  <a-tag
                    color="#0C7EF2"
                    v-if="record['type'] === 1">
                    Check Point
                  </a-tag>
                  <a-tag
                    color="#52c41a"
                    v-if="record['type'] === 2">
                    Save Point
                  </a-tag>
                </div>
              </template>
              <template
                slot="latest"
                slot-scope="text, record">
                <a-tag
                  color="green"
                  v-if="record.latest">
                  Latest
                </a-tag>
              </template>
              <template
                slot="operation"
                slot-scope="text, record">

                <a-tooltip title="Copy Path">
                  <a-button
                    shape="circle"
                    size="small"
                    class="control-button ctl-btn-color"
                    v-clipboard:copy="record.path"
                    v-clipboard:success="handleCopySuccess"
                    v-clipboard:error="handleCopyError">
                    <a-icon type="copy"/>
                  </a-button>
                </a-tooltip>

                <a-popconfirm
                  title="Are you sure delete?"
                  cancel-text="No"
                  ok-text="Yes"
                  v-permit="'savepoint:delete'"
                  @confirm="handleDeleteSavePoint(record)">
                  <a-button
                    type="danger"
                    shape="circle"
                    size="small"
                    class="control-button">
                    <a-icon type="delete"/>
                  </a-button>
                </a-popconfirm>
              </template>
            </a-table>
          </a-descriptions-item>
        </a-descriptions>
      </a-tab-pane>
      <a-tab-pane
        key="5"
        tab="Backups"
        v-if="app && backUpList && backUpList.length > 0">
        <a-descriptions>
          <a-descriptions-item>
            <a-table
              ref="TableBackUp"
              :columns="column.backUps"
              size="middle"
              row-key="id"
              style="margin-top: -24px"
              :data-source="backUpList"
              :pagination="pagination.backUp"
              :loading="pager.backUp.loading"
              class="detail-table"
              @change="handleTableChange">
              <template
                slot="version"
                slot-scope="text, record">
                <a-button
                  type="primary"
                  shape="circle"
                  size="small">
                  {{ record.version }}
                </a-button>
              </template>
              <template
                slot="operation"
                v-if="1 === 2"
                slot-scope="text, record">

                <a-tooltip title="Rollback Job">
                  <a-button
                    v-permit="'backup:rollback'"
                    @click.native="handleRollback(record)"
                    shape="circle"
                    size="small"
                    class="control-button ctl-btn-color">
                    <a-icon type="rollback"/>
                  </a-button>
                </a-tooltip>

                <a-popconfirm
                  title="Are you sure delete ?"
                  cancel-text="No"
                  ok-text="Yes"
                  v-permit="'backup:delete'"
                  @confirm="handleDeleteBackUp(record)">
                  <a-button
                    type="danger"
                    shape="circle"
                    size="small"
                    class="control-button">
                    <a-icon type="delete"/>
                  </a-button>
                </a-popconfirm>

              </template>
            </a-table>
          </a-descriptions-item>
        </a-descriptions>
      </a-tab-pane>
      <a-tab-pane
        key="6"
        tab="Option Logs"
        v-if="app && optionLogList && optionLogList.length > 0">
        <a-descriptions>
          <a-descriptions-item>
            <a-table
              ref="TableOptLog"
              :columns="column.optionLog"
              size="middle"
              row-key="id"
              style="margin-top: -24px"
              :data-source="optionLogList"
              :pagination="pagination.optionLog"
              :loading="pager.optionLog.loading"
              @change="handleTableChange"
              class="detail-table">
              <template
                slot="yarnAppId"
                slot-scope="text, record">
                <span class="pointer" @click="handleView(record.yarnAppId)">{{ record.yarnAppId }}</span>
              </template>
              <template
                slot="optionTime"
                slot-scope="text, record">
                <a-icon
                  type="clock-circle"/>
                {{ record.optionTime }}
              </template>
              <template
                slot="success"
                slot-scope="text, record">
                <a-tag
                  class="start-state"
                  color="#52c41a"
                  v-if="record.success">
                  SUCCESS
                </a-tag>
                <a-tag
                  class="start-state"
                  color="#f5222d"
                  v-else>
                  FAILED
                </a-tag>
              </template>
              <template
                slot="operation"
                slot-scope="text, record">
                <a-tooltip title="View Exception" v-if="!record.success">
                  <a-button
                    v-permit="'app:detail'"
                    @click.native="handleException(record)"
                    shape="circle"
                    size="small"
                    class="control-button ctl-btn-color">
                    <a-icon type="eye"/>
                  </a-button>
                </a-tooltip>
              </template>
            </a-table>
          </a-descriptions-item>
        </a-descriptions>
      </a-tab-pane>
    </a-tabs>

    <a-modal
      v-model="compareVisible"
      on-ok="handleCompareOk"
      v-if="compareVisible">
      <template
        slot="title">
        <svg-icon
          name="swap"/>
        Compare Config
      </template>
      <template
        slot="footer">
        <a-button
          key="back"
          @click="handleCompareCancel">
          Close
        </a-button>
        <a-button
          key="submit"
          type="primary"
          @click="handleCompareOk">
          Compare
        </a-button>
      </template>
      <a-form
        @submit="handleCompareOk"
        :form="formCompare">
        <a-form-item
          label="source version"
          :label-col="{lg: {span: 7}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 4} }">
          <a-button
            type="primary"
            shape="circle"
            size="small"
            style="margin-right: 10px;">
            {{ compare.version }}
          </a-button>
          <a-icon
            type="clock-circle"
            style="color:darkgrey"/>
          <span
            style="color:darkgrey">{{ compare.createTime }}</span>
        </a-form-item>
        <a-form-item
          label="target version"
          :label-col="{lg: {span: 7}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 4} }">
          <a-select
            @change="handleCompareTarget">
            <a-select-option
              v-for="(ver,index) in filterNotCurrConfig"
              :value="ver.id"
              :key="index">
              <div
                style="padding-left: 5px">
                <a-button
                  type="primary"
                  shape="circle"
                  size="small"
                  style="margin-right: 10px;">
                  {{ ver.version }}
                </a-button>
                <a-tag
                  color="green"
                  style=";margin-left: 10px;"
                  size="small"
                  v-if="ver.effective">
                  Effective
                </a-tag>
                <a-tag
                  color="cyan"
                  style=";margin-left: 5px;"
                  size="small"
                  v-if="ver.candidate == 1 || ver.candidate == 2">
                  Candidate
                </a-tag>
              </div>
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model="execOption.visible"
      width="80%"
      :body-style="execOption.modalStyle"
      :destroy-on-close="execOption.modalDestroyOnClose"
      @ok="handleExpClose">
      <template
        slot="title">
        <svg-icon
          name="code"
          style="color:RED"/>&nbsp; Exception Info
      </template>
      <template
        slot="footer">
        <a-button
          key="submit"
          type="primary"
          @click="handleExpClose">
          Close
        </a-button>
      </template>
      <div
        id="startExp"
        class="startExp"
        ref="startExp"
        style="height: 100%"/>
    </a-modal>

    <a-modal
      v-model="rollbackVisible"
      on-ok="handleRollbackOk">
      <template
        slot="title">
        <svg-icon
          slot="icon"
          name="rollback"/>
        Rollback Backup
      </template>

      <a-form
        @submit="handleRollbackOk"
        :form="formRollback">
        <a-form-item
          label="Backup"
          :label-col="{lg: {span: 5}, sm: {span: 5}}"
          :wrapper-col="{lg: {span: 17}, sm: {span: 5} }">
          <a-switch
            checked-children="ON"
            un-checked-children="OFF"
            v-model="needBackup"
            v-decorator="['needBackup']"/>
          <span
            class="conf-switch"
            style="color:darkgrey"> current effective job need backed up?</span>
        </a-form-item>

        <a-form-item
          v-if="needBackup"
          label="Backup Desc"
          :label-col="{lg: {span: 7}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 4} }">
          <a-textarea
            rows="3"
            placeholder="The backup description for the application,so that it can be retrieved when the version is rolled back"
            v-decorator="['description',{ rules: [{ message: 'Please enter a backup description' } ]}]"/>
        </a-form-item>
      </a-form>

      <template
        slot="footer">
        <a-button
          key="back"
          @click="handleRollbackCancel">
          Cancel
        </a-button>
        <a-button
          key="submit"
          type="primary"
          @click="handleRollbackOk">
          Rollback
        </a-button>
      </template>
    </a-modal>

    <Mergely
      ref="confEdit"
      @close="handleEditConfClose"
      @ok="handleEditConfOk"
      :visiable="confVisiable"
      :read-only="true"/>

    <Different ref="different"/>

  </a-card>
</template>
<script>
import {mapActions, mapGetters} from 'vuex'
import {backUps, get, optionLog, removeBak, rollback, yarn} from '@api/application'
import State from './State'
import configOptions from './Option'
import {get as getVer, list as listVer, remove as removeConf} from '@api/config'
import {history, remove as removeSp} from '@api/savepoint'
import Mergely from './Mergely'
import Different from './Different'
import * as monaco from 'monaco-editor'
import SvgIcon from '@/components/SvgIcon'
import storage from '@/utils/storage'
import {DEFAULT_THEME} from '@/store/mutation-types'
import {activeURL} from '@/api/flinkCluster'
import {baseUrl} from '@/api/baseUrl'
import api from '@/api/index'
import {copyCurl, check as checkToken} from '@/api/token'

const Base64 = require('js-base64').Base64
configOptions.push(
  {
    key: '-p',
    name: 'parallelism'
  },
  {
    key: '-ys',
    name: 'yarnslots'
  }
)

export default {

  components: {SvgIcon, State, Mergely, Different},
  data() {
    return {
      api,
      app: null,
      options: {},
      defaultConfigId: null,
      allConfigVersions: null,
      configVersions: null,
      savePoints: null,
      confVisiable: false,
      backUpList: null,
      compareVisible: false,
      formCompare: null,
      compare: null,
      optionLogList: null,
      queryParams: {},
      animated: false,
      tabBarGutter: 0,
      backup: null,
      yarn: null,
      needBackup: false,
      rollbackVisible: false,
      formRollback: null,
      pager: {
        config: {
          key: '2',
          info: null,
          loading: false
        },
        savePoints: {
          key: '4',
          info: null,
          loading: false
        },
        backUp: {
          key: '5',
          info: null,
          loading: false
        },
        optionLog: {
          key: '6',
          info: null,
          loading: false
        }
      },
      pagination: {
        config: {
          pageSizeOptions: ['10', '20', '30', '40', '100'],
          defaultCurrent: 1,
          defaultPageSize: 10,
          showQuickJumper: true,
          showSizeChanger: true,
          showTotal: (total, range) => `显示 ${range[0]} ~ ${range[1]} 条记录，共 ${total} 条记录`
        },
        savePoints: {
          pageSizeOptions: ['10', '20', '30', '40', '100'],
          defaultCurrent: 1,
          defaultPageSize: 10,
          showQuickJumper: true,
          showSizeChanger: true,
          showTotal: (total, range) => `显示 ${range[0]} ~ ${range[1]} 条记录，共 ${total} 条记录`
        },
        backUp: {
          pageSizeOptions: ['10', '20', '30', '40', '100'],
          defaultCurrent: 1,
          defaultPageSize: 10,
          showQuickJumper: true,
          showSizeChanger: true,
          showTotal: (total, range) => `显示 ${range[0]} ~ ${range[1]} 条记录，共 ${total} 条记录`
        },
        optionLog: {
          pageSizeOptions: ['10', '20', '30', '40', '100'],
          defaultCurrent: 1,
          defaultPageSize: 10,
          showQuickJumper: true,
          showSizeChanger: true,
          showTotal: (total, range) => `显示 ${range[0]} ~ ${range[1]} 条记录，共 ${total} 条记录`
        }
      },

      editor: {
        option: {
          theme: this.ideTheme(),
          language: 'sql',
          selectOnLineNumbers: false,
          foldingStrategy: 'indentation', // 代码分小段折叠
          overviewRulerBorder: false, // 不要滚动条边框
          autoClosingBrackets: true,
          tabSize: 2, // tab 缩进长度
          readOnly: true,
          inherit: true,
          scrollBeyondLastLine: false,
          lineNumbersMinChars: 5,
          lineHeight: 24,
          automaticLayout: true,
          cursorBlinking: 'line',
          cursorStyle: 'line',
          cursorWidth: 3,
          renderFinalNewline: true,
          renderLineHighlight: 'all',
          quickSuggestionsDelay: 100,  //代码提示延时
          scrollbar: {
            useShadows: false,
            vertical: 'visible',
            horizontal: 'visible',
            horizontalSliderSize: 5,
            verticalSliderSize: 5,
            horizontalScrollbarSize: 15,
            verticalScrollbarSize: 15
          }
        },
        flinkSql: null,
        exception: null
      },

      execOption: {
        modalStyle: {
          height: '600px',
          padding: '5px'
        },
        visible: false,
        modalDestroyOnClose: true,
        content: null
      },
      activeTab: '1',
      column: {
        conf: [
          {
            title: 'Version',
            dataIndex: 'version',
            scopedSlots: {customRender: 'version'}
          },
          {
            title: 'Conf Format',
            dataIndex: 'format',
            scopedSlots: {customRender: 'format'}
          },
          {
            title: 'Effective',
            dataIndex: 'effective',
            scopedSlots: {customRender: 'effective'}
          },
          {
            title: 'Candidate',
            dataIndex: 'candidate',
            scopedSlots: {customRender: 'candidate'}
          },
          {
            title: 'Modify Time',
            dataIndex: 'createTime',
            scopedSlots: {customRender: 'createTime'}
          },
          {
            title: 'Operation',
            dataIndex: 'operation',
            key: 'operation',
            scopedSlots: {customRender: 'operation'},
            fixed: 'right',
            width: 150
          }
        ],
        savePoints: [
          {
            title: 'Path',
            dataIndex: 'path',
            width: '45%'
          },
          {
            title: 'Trigger Time',
            dataIndex: 'triggerTime',
            scopedSlots: {customRender: 'triggerTime'},
            width: 250
          },
          {
            title: 'Type',
            dataIndex: 'type',
            scopedSlots: {customRender: 'type'}
          },
          {
            title: 'Latest',
            dataIndex: 'latest',
            scopedSlots: {customRender: 'latest'}
          },
          {
            title: 'Operation',
            dataIndex: 'operation',
            key: 'operation',
            scopedSlots: {customRender: 'operation'},
            fixed: 'right',
            width: 150
          }
        ],
        backUps: [
          {
            title: 'Save Path',
            dataIndex: 'path',
            width: '40%'
          },
          {
            title: 'Description',
            dataIndex: 'description',
            width: '20%'
          },
          {
            title: 'Version',
            dataIndex: 'version',
            width: '10%',
            scopedSlots: {customRender: 'version'}
          },
          {
            title: 'Backup Time',
            dataIndex: 'createTime',
            scopedSlots: {customRender: 'createTime'}
          },
          {
            title: 'Operation',
            dataIndex: 'operation',
            key: 'operation',
            scopedSlots: {customRender: 'operation'},
            fixed: 'right',
            width: 150
          }
        ],
        optionLog: [
          {
            title: 'Application Id',
            dataIndex: 'yarnAppId',
            width: '40%',
            scopedSlots: {customRender: 'yarnAppId'}
          },
          {
            title: 'Start Status',
            dataIndex: 'success',
            scopedSlots: {customRender: 'success'}
          },
          {
            title: 'Option Time',
            dataIndex: 'optionTime',
            scopedSlots: {customRender: 'optionTime'}
          },
          {
            title: 'Operation',
            dataIndex: 'operation',
            key: 'operation',
            scopedSlots: {customRender: 'operation'},
            fixed: 'right',
            width: 150
          }
        ]
      }
    }
  },

  beforeMount() {
    this.formCompare = this.$form.createForm(this)
    this.formRollback = this.$form.createForm(this)
  },

  computed: {
    filterNotCurrConfig() {
      return this.allConfigVersions.filter(x => x.version !== this.compare.version)
    },

    myTheme() {
      return this.$store.state.app.theme
    }

  },

  mounted() {
    const appId = this.applicationId()
    if (appId) {
      this.CleanAppId()
      this.handleGet(appId)
      const timer = window.setInterval(() => this.handleGet(appId), 5000)
      this.$once('hook:beforeDestroy', () => {
        clearInterval(timer)
      })
    } else {
      this.$router.back(-1)
    }
  },

  methods: {
    ...mapActions(['CleanAppId']),
    ...mapGetters(['applicationId']),

    handleDocPage() {
      const res = baseUrl().split(':')[1] + ':10000/doc.html'
      window.open(res)
    },

    handleCopyCurl(urlPath) {
      checkToken({}).then((resp) => {
        const result = parseInt(resp.data)
        if (result === 0) {
          this.$swal.fire({
            icon: 'error',
            title: 'access token is null,please contact the administrator to add.',
            showConfirmButton: true,
            timer: 3500
          })
        } else if (result === 1) {
          this.$swal.fire({
            icon: 'error',
            title: 'access token is invalid,please contact the administrator.',
            showConfirmButton: true,
            timer: 3500
          })
        } else {
          const params = {
            appId: this.app.id,
            baseUrl: baseUrl(),
            path: urlPath
          }
          copyCurl({...params}).then((resp) => {
            const oTextarea = document.createElement('textarea')
            oTextarea.value = resp.data
            document.body.appendChild(oTextarea)
            // 选择对象
            oTextarea.select()
            document.execCommand('Copy')
            this.$message.success('copy successful')
            oTextarea.remove()
          })
        }
      })
    },

    handleGet(appId) {
      get({id: appId}).then((resp) => {
        if (!this.app) {
          this.app = resp.data
          this.options = JSON.parse(this.app.options || '{}')
          if (this.app.executionMode === 2 || this.app.executionMode === 3 || this.app.executionMode === 4) {
            this.handleYarn()
          }
          this.$nextTick(() => {
            this.handleConfig()
            this.handleFlinkSql()
            this.handleSavePoint()
            this.handleBackUps()
            this.handleOptionLog()
          })
        } else {
          /**
           * 第一次之后,每次轮询只管基本信息的变更
           * @type {any}
           */
          this.app = resp.data
        }
      }).catch((error) => {
        this.$message.error(error.message)
      })
    },
    handleFlinkSql() {
      if (this.app.jobType === 2) {
        this.editor.option.value = Base64.decode(this.app.flinkSql)
      }
    },
    handleConfig() {
      const params = {
        appId: this.app.id
      }
      if (this.pager.config.info) {
        // 如果分页信息不为空，则设置表格当前第几页，每页条数，并设置查询分页参数
        this.$refs.TableConf.pagination.current = this.pager.config.info.current
        this.$refs.TableConf.pagination.pageSize = this.pager.config.info.pageSize
        params.pageSize = this.pager.config.info.pageSize
        params.pageNum = this.pager.config.info.current
      } else {
        // 如果分页信息为空，则设置为默认值
        params.pageSize = this.pagination.config.defaultPageSize
        params.pageNum = this.pagination.config.defaultCurrent
      }
      this.handlePagerLoading()
      listVer({...params}).then((resp) => {
        const pagination = {...this.pagination.config}
        pagination.total = parseInt(resp.data.total)
        resp.data.records.forEach((value, index) => {
          if (value.effective) {
            this.defaultConfigId = value.id
          }
        })
        this.configVersions = resp.data.records
        let pageSize = this.pagination.config.defaultPageSize
        if (this.pager.config.info != null) {
          pageSize = this.pager.config.info.pageSize || pageSize
        }
        if (pagination.total >= pageSize) {
          this.allConfigVersions = this.configVersions
        }
        this.pagination.config = pagination
        this.pager.config.loading = false
      })
    },

    handleYarn() {
      yarn({}).then((resp) => {
        this.yarn = resp.data
      })
    },

    handleView() {
      if (this.app.executionMode === 1) {
        activeURL({id: this.app.id}).then((resp) => {
          const url = resp.data + '/#/job/' + this.app.jobId + '/overview'
          window.open(url)
        })
      } else if (this.app.executionMode === 2 || this.app.executionMode === 3 || this.app.executionMode === 4) {
        if (this.yarn !== null) {
          const url = this.yarn + '/proxy/' + this.app.appId + '/'
          window.open(url)
        }
      } else {
        window.open(this.app.flinkRestUrl)
      }
    },

    handleSavePoint() {
      const params = {
        appId: this.app.id
      }
      if (this.pager.savePoints.info) {
        // 如果分页信息不为空，则设置表格当前第几页，每页条数，并设置查询分页参数
        this.$refs.TableSavePoints.pagination.current = this.pager.savePoints.info.current
        this.$refs.TableSavePoints.pagination.pageSize = this.pager.savePoints.info.pageSize
        params.pageSize = this.pager.savePoints.info.pageSize
        params.pageNum = this.pager.savePoints.info.current
      } else {
        // 如果分页信息为空，则设置为默认值
        params.pageSize = this.pagination.savePoints.defaultPageSize
        params.pageNum = this.pagination.savePoints.defaultCurrent
      }
      this.handlePagerLoading()
      history({...params}).then((resp) => {
        const pagination = {...this.pagination.savePoints}
        pagination.total = parseInt(resp.data.total)
        this.savePoints = resp.data.records
        this.pagination.savePoints = pagination
        this.pager.savePoints.loading = false
      })
    },

    handleBackUps() {
      const params = {
        appId: this.app.id
      }
      if (this.pager.backUp.info) {
        // 如果分页信息不为空，则设置表格当前第几页，每页条数，并设置查询分页参数
        this.$refs.TableBackUp.pagination.current = this.pager.backUp.info.current
        this.$refs.TableBackUp.pagination.pageSize = this.pager.backUp.info.pageSize
        params.pageSize = this.pager.backUp.info.pageSize
        params.pageNum = this.pager.backUp.info.current
      } else {
        // 如果分页信息为空，则设置为默认值
        params.pageSize = this.pagination.backUp.defaultPageSize
        params.pageNum = this.pagination.backUp.defaultCurrent
      }
      this.handlePagerLoading()
      backUps({...params}).then((resp) => {
        const pagination = {...this.pagination.backUp}
        pagination.total = parseInt(resp.data.total)
        this.backUpList = resp.data.records
        this.pagination.backUp = pagination
        this.pager.backUp.loading = false
      })
    },

    handleOptionLog() {
      const params = {
        appId: this.app.id
      }
      if (this.pager.optionLog.info) {
        // 如果分页信息不为空，则设置表格当前第几页，每页条数，并设置查询分页参数
        this.$refs.TableOptLog.pagination.current = this.pager.optionLog.info.current
        this.$refs.TableOptLog.pagination.pageSize = this.pager.optionLog.info.pageSize
        params.pageSize = this.pager.optionLog.info.pageSize
        params.pageNum = this.pager.optionLog.info.current
      } else {
        // 如果分页信息为空，则设置为默认值
        params.pageSize = this.pagination.optionLog.defaultPageSize
        params.pageNum = this.pagination.optionLog.defaultCurrent
      }
      this.handlePagerLoading()
      optionLog({...params}).then((resp) => {
        const pagination = {...this.pagination.optionLog}
        pagination.total = parseInt(resp.data.total)
        this.optionLogList = resp.data.records
        this.pagination.optionLog = pagination
        this.pager.optionLog.loading = false
      })
    },

    handleEditConfClose() {
      this.confVisiable = false
    },

    handleEditConfOk(value) {
      this.configOverride = value
    },

    handleConfDetail(record) {
      this.confVisiable = true
      getVer({
        id: record.id
      }).then((resp) => {
        const text = Base64.decode(resp.data.content)
        this.$refs.confEdit.set(text)
      })
    },

    handleDeleteSavePoint(record) {
      removeSp({
        id: record.id
      }).then((resp) => {
        this.handleSavePoint()
      })
    },

    handleDeleteBackUp(record) {
      removeBak({
        id: record.id
      }).then((resp) => {
        this.handleBackUps()
      })
    },

    handleDeleteConf(record) {
      removeConf({
        id: record.id
      }).then((resp) => {
        this.handleConfig()
      })
    },

    handleCopySuccess() {
      this.$message.success('copied to clipboard successfully')
    },

    handleCopyError() {
      this.$message.error('copied to clipboard failed')
    },

    handleCompare(record) {
      if (this.allConfigVersions == null) {
        listVer({
          appId: this.app.id,
          pageNo: 1,
          pageSize: 999999
        }).then((resp) => {
          this.allConfigVersions = resp.data.records
        })
      }
      this.compareVisible = true
      this.compare = {
        id: record.id,
        version: record.version,
        createTime: record.createTime
      }
    },

    handleCompareTarget(v) {
      this.compare.target = v
    },

    handleCompareCancel() {
      this.compareVisible = false
    },

    handleCompareOk() {
      getVer({
        id: this.compare.id
      }).then((resp) => {
        const conf1 = Base64.decode(resp.data.content)
        const ver1 = resp.data.version
        getVer({
          id: this.compare.target
        }).then((resp) => {
          const conf2 = Base64.decode(resp.data.content)
          const ver2 = resp.data.version
          this.handleCompareCancel()
          this.$refs.different.different([{
              name: 'Configuration',
              format: 'yaml',
              original: conf1,
              modified: conf2,
            }],
            ver1,
            ver2
          )
        })
      })
    },

    handleRollback(backup) {
      this.rollbackVisible = true
      this.backup = backup
    },

    handleRollbackOk() {
      this.formRollback.validateFields((err, values) => {
        if (!err) {
          const id = this.backup.id
          const appId = this.backup.appId
          const sqlId = this.backup.sqlId
          const path = this.backup.path
          this.handleRollbackCancel()
          this.$swal.fire({
            icon: 'success',
            title: 'this backup is rolling back',
            showConfirmButton: false,
            timer: 2000
          }).then((r) => {
            rollback({
              id: id,
              appId: appId,
              sqlId: sqlId,
              path: path,
              backup: values.needBackup,
              description: values.description || null
            })
          })
        }
      })
    },

    handleRollbackCancel() {
      this.rollbackVisible = false
      this.backup = null
    },

    handleException(record) {
      this.execOption.visible = true
      this.execOption.content = record.exception
      this.$nextTick(() => {
        this.handleLogMonaco()
        this.editor.exception = monaco.editor.create(document.querySelector('#startExp'), {
          theme: 'log',
          value: this.execOption.content,
          language: 'log',
          readOnly: true,
          inherit: true,
          scrollBeyondLastLine: false,
          overviewRulerBorder: false, // 不要滚动条边框
          autoClosingBrackets: true,
          tabSize: 2, // tab 缩进长度
          scrollbar: {
            useShadows: false,
            vertical: 'visible',
            horizontal: 'visible',
            horizontalSliderSize: 5,
            verticalSliderSize: 5,
            horizontalScrollbarSize: 15,
            verticalScrollbarSize: 15
          }
        })
      })
    },

    handleExpClose() {
      this.execOption.visible = false
      this.editor.exception.dispose()
    },

    handleGoBack() {
      this.$router.back(-1)
    },

    handleTableChange(pagination, filters, sorter) {
      // 将这两个参数赋值给Vue data，用于后续使用
      switch (this.activeTab) {
        case '2':
          this.pager.config.info = pagination
          this.handleConfig()
          break
        case '4':
          this.pager.savePoints.info = pagination
          this.handleSavePoint()
          break
        case '5':
          this.pager.backUp.info = pagination
          this.handleBackUps()
          break
        case '6':
          this.pager.optionLog.info = pagination
          this.handleOptionLog()
          break
      }
    },

    handleChangeTab(key) {
      this.activeTab = key
      this.$nextTick(() => {
        if (this.activeTab === '3') {
          this.editor.flinkSql = monaco.editor.create(document.querySelector('#flink-sql'), this.editor.option)
          this.editor.flinkSql.updateOptions({
            theme: this.ideTheme()
          })
        } else if (this.editor.flinkSql) {
          this.editor.flinkSql.dispose()
        }
      })
    },

    handlePagerLoading() {
      for (const k in this.pager) {
        this.pager[k]['loading'] = this.pager[k]['key'] === this.activeTab
      }
    },

    handleLogMonaco() {
      monaco.languages.register({id: 'log'})
      monaco.languages.setMonarchTokensProvider('log', {
        tokenizer: {
          root: [
            [/.*\.Exception.*/, 'log-error'],
            [/.*Caused\s+by:.*/, 'log-error'],
            [/\s+at\s+.*/, 'log-info'],
            [/http:\/\/(.*):\d+(.*)\/application_\d+_\d+/, 'yarn-info'],
            [/Container\s+id:\s+container_\d+_\d+_\d+_\d+/, 'yarn-info'],
            [/yarn\s+logs\s+-applicationId\s+application_\d+_\d+/, 'yarn-info'],
            [/\[20\d+-\d+-\d+\s+\d+:\d+:\d+\d+|.\d+]/, 'log-date'],
            [/\[[a-zA-Z 0-9:]+]/, 'log-date'],
          ]
        }
      })

      monaco.editor.defineTheme('log', {
        base: storage.get(DEFAULT_THEME) === 'dark' ? 'vs-dark' : 'vs',
        inherit: true,
        rules: [
          {token: 'log-info', foreground: '808080'},
          {token: 'log-error', foreground: 'ff0000', fontStyle: 'bold'},
          {token: 'log-notice', foreground: 'FFA500'},
          {token: 'yarn-info', foreground: '0066FF', fontStyle: 'bold'},
          {token: 'log-date', foreground: '008800'},
        ]
      })
    }

  },

  watch: {
    myTheme() {
      this.$refs.confEdit.theme()
      this.$refs.different.theme()
      if (this.editor.exception !== null) {
        this.editor.exception.updateOptions({
          theme: this.ideTheme()
        })
      }
      if (this.editor.flinkSql != null) {
        this.editor.flinkSql.updateOptions({
          theme: this.ideTheme()
        })
      }
    }
  },
}
</script>

<style lang="less">
@import "Detail";
</style>
