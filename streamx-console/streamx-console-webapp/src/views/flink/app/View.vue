<template>
  <div>
    <a-row :gutter="24" class="dashboard">
      <template v-if="dashBigScreen">
        <a-col class="gutter-row" :span="6">
          <div class="gutter-box">
            <a-card :loading="dashLoading" :bordered="false" class="dash-statistic">
              <a-statistic
                title="Available Task Slots"
                :value="metrics.availableSlot"
                :value-style="{color: '#3f8600', fontSize: '45px', fontWeight: 500, textShadow: '1px 1px 0 rgba(0,0,0,0.2)'}"/>
            </a-card>
            <a-divider style="margin-bottom: 10px"/>
            <div>
              <span>
                Task Slots
                <strong>{{ metrics.totalSlot }}</strong>
              </span>
              <a-divider
                type="vertical"/>
              <span>
                Task Managers
                <strong>{{ metrics.totalTM }}</strong>
              </span>
            </div>
          </div>
        </a-col>
        <a-col class="gutter-row" :span="6">
          <div class="gutter-box">
            <a-card :loading="dashLoading" :bordered="false" class="dash-statistic">
              <a-statistic
                title="Running Jobs"
                :value="metrics['runningJob']"
                :value-style="{color: '#3f8600', fontSize: '45px', fontWeight: 500, textShadow: '1px 1px 0 rgba(0,0,0,0.2)'}"/>
            </a-card>
            <a-divider style="margin-bottom: 10px"/>
            <div>
              <span>
                Total Task
                <strong>{{ metrics.task.total }}</strong>
              </span>
              <a-divider type="vertical"/>
              <span>
                Running Task
                <strong>{{ metrics.task.running }}</strong>
              </span>
            </div>
          </div>
        </a-col>
        <a-col class="gutter-row" :span="6">
          <div class="gutter-box">
            <a-card :loading="dashLoading" :bordered="false" class="dash-statistic">
              <a-statistic
                title="JobManager Memory"
                :value="metrics.jmMemory"
                :precision="0"
                suffix="MB"
                :value-style="{color: '#3f8600', fontSize: '45px', fontWeight: 500, textShadow: '1px 1px 0 rgba(0,0,0,0.2)'}"/>
            </a-card>
            <a-divider style="margin-bottom: 10px"/>
            <div>
              <span>
                Total JobManager Mem
                <strong>{{ metrics.jmMemory }} MB</strong>
              </span>
            </div>
          </div>
        </a-col>
        <a-col
          class="gutter-row"
          :span="6">
          <div class="gutter-box">
            <a-card
              :loading="dashLoading"
              :bordered="false"
              class="dash-statistic">
              <a-statistic
                title="TaskManager Memory"
                :value="metrics.tmMemory"
                :precision="0"
                suffix="MB"
                :value-style="{color: '#3f8600', fontSize: '45px', fontWeight: 500, textShadow: '1px 1px 0 rgba(0,0,0,0.2)'}"/>
            </a-card>
            <a-divider
              style="margin-bottom: 10px"/>
            <div>
              <span>
                Total TaskManager Mem
                <strong>{{ metrics.tmMemory }} MB</strong>
              </span>
            </div>
          </div>
        </a-col>
      </template>
      <template
        v-else>
        <a-col
          class="gutter-row"
          :span="12">
          <a-skeleton
            v-if="dashLoading"
            class="gutter-box"
            :loading="dashLoading"
            active/>
          <div
            class="gutter-box"
            v-if="!dashLoading">
            <a-row
              :gutter="24">
              <a-col
                class="gutter-row"
                :span="12">
                <a-card
                  :bordered="false"
                  class="dash-statistic">
                  <a-statistic
                    title="Available Task Slots"
                    :value="metrics.availableSlot"
                    :value-style="{color: '#3f8600', fontSize: '45px', fontWeight: 500, textShadow: '1px 1px 0 rgba(0,0,0,0.2)'}"/>
                </a-card>
              </a-col>
              <a-col
                class="gutter-row"
                :span="12">
                <a-card
                  :bordered="false"
                  class="dash-statistic stat-right">
                  <a-statistic
                    title="Running Jobs"
                    :value="metrics['runningJob']"
                    :value-style="{color: '#3f8600', fontSize: '45px', fontWeight: 500, textShadow: '1px 1px 0 rgba(0,0,0,0.2)'}"/>
                </a-card>
              </a-col>
            </a-row>
            <a-divider
              style="margin-bottom: 10px"/>
            <div>
              <span>
                Total Task
                <strong>{{ metrics.task.total }}</strong>
              </span>
              <a-divider
                type="vertical"/>
              <span>
                Running Task
                <strong>{{ metrics.task.running }}</strong>
              </span>
              <a-divider
                type="vertical"/>
              <span>
                Task Slots
                <strong>{{ metrics.totalSlot }}</strong>
              </span>
              <a-divider
                type="vertical"/>
              <span>
                Task Managers
                <strong>{{ metrics.totalTM }}</strong>
              </span>
            </div>
          </div>
        </a-col>
        <a-col
          class="gutter-row"
          :span="12">
          <a-skeleton
            v-if="dashLoading"
            class="gutter-box"
            :loading="dashLoading"
            active/>
          <div
            class="gutter-box"
            v-if="!dashLoading">
            <a-row
              :gutter="24">
              <a-col
                class="gutter-row"
                :span="12">
                <a-card
                  :bordered="false"
                  class="dash-statistic">
                  <a-statistic
                    title="JobManager Memory"
                    :value="metrics.jmMemory"
                    :precision="0"
                    suffix="MB"
                    :value-style="{color: '#3f8600', fontSize: '45px', fontWeight: 500, textShadow: '1px 1px 0 rgba(0,0,0,0.2)'}"/>
                </a-card>
              </a-col>
              <a-col
                class="gutter-row"
                :span="12">
                <a-card
                  :bordered="false"
                  class="dash-statistic stat-right">
                  <a-statistic
                    title="TaskManager Memory"
                    :value="metrics.tmMemory"
                    :precision="0"
                    suffix="MB"
                    :value-style="{color: '#3f8600', fontSize: '45px', fontWeight: 500, textShadow: '1px 1px 0 rgba(0,0,0,0.2)'}"/>
                </a-card>
              </a-col>
            </a-row>
            <a-divider
              style="margin-bottom: 10px"/>
            <div>
              <span>
                Total JobManager Mem
                <strong>{{ metrics.jmMemory }} MB</strong>
              </span>
              <a-divider
                type="vertical"/>
              <span>
                Total TaskManager Mem
                <strong>{{ metrics.tmMemory }} MB</strong>
              </span>
            </div>
          </div>
        </a-col>
      </template>
    </a-row>
    <a-card
      :bordered="false"
      style="margin-top: 20px">
      <!-- 表格区域 -->
      <a-table
        ref="TableInfo"
        :columns="columns"
        :expand-icon="handleExpandIcon"
        size="middle"
        row-key="id"
        class="app_list"
        style="margin-top: -24px"
        :data-source="dataSource"
        :pagination="pagination"
        :loading="loading"
        :scroll="{ x: 700 }"
        @change="handleTableChange">
        <a-table
          slot="expandedRowRender"
          class="expanded-table"
          slot-scope="record"
          v-if="record.state === 7"
          row-key="id"
          :columns="innerColumns"
          :data-source="record.expanded"
          :pagination="false"/>
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
          <a-button
            size="small"
            icon="rest"
            style="width: 90px"
            @click="() => handleReset(clearFilters)">
            Reset
          </a-button>
        </div>

        <a-icon
          slot="filterIcon"
          slot-scope="filtered"
          type="search"
          :style="{ color: filtered ? '#108ee9' : undefined }"/>

        <template
          slot="customRender"
          slot-scope="text, record, index, column">
          <!--有条件搜索-->
          <template v-if="searchText && searchedColumn === column.dataIndex">
            <span
              :class="{pointer: record.state === 6 || record.state === 7 || record['optionState'] === 4 }"
              @click="handleView(record)">
              <template
                v-if="record.deploy === 0"
                v-for="(fragment, i) in text
                  .toString()
                  .substr(0,(text.length > 30 ? 30: text.length ))
                  .split(new RegExp(`(?<=${searchText})|(?=${searchText})`, 'i'))">
                <mark
                  v-if="fragment.toLowerCase() === searchText.toLowerCase()"
                  :key="i"
                  class="highlight">
                  {{ fragment }}
                </mark>
                <template v-else>
                  {{ fragment }}
                </template>
              </template>
              <template v-else>
                <a-tooltip placement="top">
                  <template slot="title">
                    {{ text }}
                  </template>
                  <template
                    v-for="(fragment, i) in
                      text
                        .toString()
                        .substr(0,(text.length > 30 ? 30: text.length ))
                        .toString()
                        .split(new RegExp(`(?<=${searchText})|(?=${searchText})`, 'i'))">
                    <mark
                      v-if="fragment.toLowerCase() === searchText.toLowerCase()"
                      :key="i"
                      class="highlight">
                      {{ fragment }}
                    </mark>
                    <template v-else>
                      {{ fragment }}
                    </template>
                  </template>
                </a-tooltip>
              </template>
              <span v-if="text.length>30">
                ...
              </span>
            </span>
          </template>
          <!--无条件搜索-->
          <template v-else>
            <span
              v-if="column.dataIndex === 'jobName'"
              :class="{pointer: record.state === 6 || record.state === 7 || record['optionState'] === 4 }"
              @click="handleView(record)">
              <ellipsis
                :length="30"
                tooltip>
                {{ text }}
              </ellipsis>
            </span>
            <span v-else>
              <ellipsis
                :length="30"
                tooltip>
                {{ text }}
              </ellipsis>
            </span>
          </template>
        </template>

        <template
          slot="jobType"
          slot-scope="text, record">
          <div
            class="app_state">
            <a-tag
              color="#545454"
              v-if="record['jobType'] === 1">
              Custom Code
            </a-tag>
            <a-tag
              color="#0C7EF2"
              v-if="record['jobType'] === 2">
              Flink SQL
            </a-tag>
          </div>
        </template>

        <template
          slot="duration"
          slot-scope="text, record">
          {{ record.duration | duration }}
        </template>

        <template
          slot="task"
          slot-scope="text, record">
          <State
            option="task"
            :data="record"/>
        </template>

        <template
          slot="state"
          slot-scope="text, record">
          <State
            option="state"
            :data="record"/>
        </template>

        <template
          slot="deployState"
          slot-scope="text, record">
          <State
            option="deploy"
            :title="handleDeployTitle(record.deploy)"
            :data="record"/>
        </template>

        <template
          slot="customOperation">
          Operation
          <a-button
            v-permit="'app:create'"
            type="primary"
            shape="circle"
            icon="plus"
            style="margin-left: 20px; width: 25px;height: 25px;min-width: 25px"
            @click="handleAdd"/>
        </template>

        <template
          slot="operation"
          slot-scope="text, record">
          <svg-icon
            name="mapping"
            border
            v-if="record.state !== 7
              && optionApps.deploy.get(record.id) === undefined
              && optionApps.stoping.get(record.id) === undefined
              && optionApps.starting.get(record.id) === undefined
              && record['optionState'] === 0"
            v-permit="'app:mapping'"
            @click.native="handleMapping(record)"/>
          <svg-icon
            name="deploy"
            border
            v-show="(record.deploy === 2 || record.deploy === 3) && record.state !== 1 && (optionApps.deploy.get(record.id) === undefined || record['optionState'] === 0)"
            v-permit="'app:deploy'"
            class="pointer"
            @click.native="handleDeploy(record)"/>
          <!--已经发布完的项目,不允许再次编辑-->
          <svg-icon
            name="edit"
            border
            v-if="record.deploy !== 6"
            v-permit="'app:update'"
            type="setting"
            theme="twoTone"
            two-tone-color="#4a9ff5"
            @click.native="handleEdit(record)"
            title="Update application"/>
          <svg-icon
            name="rollback"
            border
            v-if="record.deploy === 6"
            v-permit="'app:update'"
            @click.native="handleRevoke(record)"
            title="Revoke Deploy"/>
          <a-icon
            v-if="record.state === 1 || record['deploy'] === 1"
            type="sync"
            style="color:#4a9ff5"
            spin
            @click="handleSeeLog(record)"/>
          <svg-icon
            name="play"
            border
            v-show="handleIsStart(record)"
            v-permit="'app:start'"
            @click.native="handleStart(record)"/>
          <svg-icon
            name="shutdown"
            border
            title="Cancel this application"
            v-permit="'app:cancel'"
            v-show="record.state === 7 && record['optionState'] === 0"
            @click.native="handleCancel(record)"/>
          <svg-icon
            name="see"
            border
            v-permit="'app:detail'"
            @click.native="handleDetail(record)"
            title="Detail"/>
          <svg-icon
            name="flame"
            border
            v-if="record.flameGraph"
            v-permit="'app:flameGraph'"
            @click.native="handleFlameGraph(record)"
            title="Detail"/>

          <template v-if="handleCanDelete(record)">
            <a-popconfirm
              title="Are you sure delete this job ?"
              cancel-text="No"
              ok-text="Yes"
              @confirm="handleDelete(record)">
              <svg-icon name="remove" border/>
            </a-popconfirm>
          </template>

        </template>

      </a-table>
      <a-modal
        v-model="deployVisible"
        on-ok="handleDeployOk">
        <template
          slot="title">
          <svg-icon
            slot="icon"
            name="deploy"/>
          Launch Application
        </template>
        <template
          slot="footer">
          <a-button
            key="back"
            @click="handleDeployCancel">
            Cancel
          </a-button>
          <a-button
            key="submit"
            type="primary"
            :loading="loading"
            @click="handleDeployOk">
            Apply
          </a-button>
        </template>
        <a-form
          @submit="handleDeployOk"
          :form="formDeploy">
          <a-form-item
            v-if="application && application.state === 7 "
            label="restart"
            :label-col="{lg: {span: 7}, sm: {span: 7}}"
            :wrapper-col="{lg: {span: 16}, sm: {span: 4} }">
            <a-switch
              checked-children="ON"
              un-checked-children="OFF"
              placeholder="restarting this application"
              v-model="restart"
              v-decorator="['restart']"/>
            <span
              class="conf-switch"
              style="color:darkgrey"> restart application after deploy</span>
          </a-form-item>
          <a-form-item
            v-if="restart"
            label="Savepoint"
            :label-col="{lg: {span: 7}, sm: {span: 7}}"
            :wrapper-col="{lg: {span: 16}, sm: {span: 4} }">
            <a-switch
              checked-children="ON"
              un-checked-children="OFF"
              v-model="savePoint"
              v-decorator="['savePoint']"/>
            <span
              class="conf-switch"
              style="color:darkgrey"> trigger savePoint before taking stoping </span>
          </a-form-item>
          <a-form-item
            v-if="restart"
            label="ignore restored"
            :label-col="{lg: {span: 7}, sm: {span: 7}}"
            :wrapper-col="{lg: {span: 16}, sm: {span: 4} }">
            <a-switch
              checked-children="ON"
              un-checked-children="OFF"
              v-model="allowNonRestoredState"
              v-decorator="['allowNonRestoredState']"/>
            <span
              class="conf-switch"
              style="color:darkgrey"> ignore savepoint then cannot be restored </span>
          </a-form-item>
          <a-form-item
            label="backup desc"
            :label-col="{lg: {span: 7}, sm: {span: 7}}"
            :wrapper-col="{lg: {span: 16}, sm: {span: 4} }">
            <a-textarea
              rows="3"
              placeholder="Before launching the new version, the current task will be backed up. Please enter the backup information of the current task"
              v-decorator="['description',{ rules: [{ required: true, message: 'Please enter a backup description' } ]}]"/>
          </a-form-item>
        </a-form>
      </a-modal>
      <a-modal
        v-model="startVisible"
        on-ok="handleStartOk">
        <template
          slot="title">
          <svg-icon
            slot="icon"
            name="play"/>
          Start application
        </template>

        <a-form
          @submit="handleStartOk"
          :form="formStartCheckPoint">
          <a-form-item
            label="flame Graph"
            :label-col="{lg: {span: 7}, sm: {span: 7}}"
            :wrapper-col="{lg: {span: 16}, sm: {span: 4} }">
            <a-switch
              checked-children="ON"
              un-checked-children="OFF"
              v-model="flameGraph"
              @click="handleCheckFlameGraph()"
              v-decorator="['flameGraph']"/>
            <span
              class="conf-switch"
              style="color:darkgrey"> flame Graph support</span>
          </a-form-item>

          <a-form-item
            label="from savepoint"
            :label-col="{lg: {span: 7}, sm: {span: 7}}"
            :wrapper-col="{lg: {span: 16}, sm: {span: 4} }">
            <a-switch
              checked-children="ON"
              un-checked-children="OFF"
              v-model="savePoint"
              v-decorator="['savePoint']"/>
            <span
              class="conf-switch"
              style="color:darkgrey"> restore the application from savepoint or latest checkpoint</span>
          </a-form-item>

          <a-form-item
            v-if="savePoint && !latestSavePoint "
            label="savepoint"
            style="margin-bottom: 10px"
            :label-col="{lg: {span: 7}, sm: {span: 7}}"
            :wrapper-col="{lg: {span: 16}, sm: {span: 4} }">
            <a-select
              v-if="historySavePoint && historySavePoint.length>0"
              mode="combobox"
              allow-clear
              v-decorator="['savepoint',{ rules: [{ required: true } ]}]">
              <a-select-option
                v-for="(k ,i) in historySavePoint"
                :key="i"
                :value="k.path">
                <template>
                  <span style="color:#108ee9">
                    {{ k.path.substr(k.path.lastIndexOf('-') + 1) }}
                  </span>
                  <span
                    style="float: right; color: darkgrey">
                    <a-icon
                      type="clock-circle"/> {{ k.createTime }}
                  </span>
                </template>
              </a-select-option>
            </a-select>
            <a-input
              v-if="!historySavePoint || (historySavePoint && historySavePoint.length === 0)"
              type="text"
              placeholder="Please enter savepoint manually"
              v-decorator="['savepoint',{ rules: [{ required: true } ]}]"/>
            <span
              class="conf-switch"
              style="color:darkgrey"> restore the application from savepoint or latest checkpoint</span>
          </a-form-item>

          <a-form-item
            v-if="savePoint"
            label="ignore restored"
            :label-col="{lg: {span: 7}, sm: {span: 7}}"
            :wrapper-col="{lg: {span: 16}, sm: {span: 4} }">
            <a-switch
              checked-children="ON"
              un-checked-children="OFF"
              v-model="allowNonRestoredState"
              v-decorator="['allowNonRestoredState']"/>
            <span
              class="conf-switch"
              style="color:darkgrey"> ignore savepoint then cannot be restored </span>
          </a-form-item>
        </a-form>

        <template slot="footer">
          <a-button
            key="back"
            @click="handleStartCancel">
            Cancel
          </a-button>
          <a-button
            key="submit"
            type="primary"
            :loading="loading"
            @click="handleStartOk">
            Apply
          </a-button>
        </template>
      </a-modal>

      <a-modal
        v-model="stopVisible"
        on-ok="handleStopOk">
        <template
          slot="title">
          <svg-icon
            slot="icon"
            name="shutdown"
            style="color: red"/>
          Stop application
        </template>

        <a-form
          @submit="handleStopOk"
          :form="formStopSavePoint">
          <a-form-item
            label="Savepoint"
            :label-col="{lg: {span: 5}, sm: {span: 5}}"
            :wrapper-col="{lg: {span: 17}, sm: {span: 5} }">
            <a-switch
              checked-children="ON"
              un-checked-children="OFF"
              v-model="savePoint"
              v-decorator="['savePoint']"/>
            <span
              class="conf-switch"
              style="color:darkgrey"> trigger savePoint before taking stoping </span>
          </a-form-item>
          <a-form-item
            label="Drain"
            :label-col="{lg: {span: 5}, sm: {span: 5}}"
            :wrapper-col="{lg: {span: 17}, sm: {span: 5} }">
            <a-switch
              checked-children="ON"
              un-checked-children="OFF"
              placeholder="Send max watermark before taking stoping"
              v-model="drain"
              v-decorator="['drain']"/>
            <span
              class="conf-switch"
              style="color:darkgrey"> Send max watermark before stoping</span>
          </a-form-item>
        </a-form>

        <template
          slot="footer">
          <a-button
            key="back"
            @click="handleStopCancel">
            Cancel
          </a-button>
          <a-button
            key="submit"
            type="primary"
            :loading="loading"
            @click="handleStopOk">
            Apply
          </a-button>
        </template>
      </a-modal>

      <a-modal
        v-model="mappingVisible"
        on-ok="handleMappingOk">
        <template
          slot="title">
          <svg-icon
            slot="icon"
            name="mapping"
            style="color: green"/>
          Mapping application
        </template>
        <a-form
          @submit="handleMappingOk"
          :form="formMapping">
          <a-form-item
            v-if="mappingVisible"
            label="Application Name"
            :label-col="{lg: {span: 7}, sm: {span: 7}}"
            :wrapper-col="{lg: {span: 16}, sm: {span: 4} }">
            <a-alert
              :message="application.jobName"
              type="info"/>
          </a-form-item>
          <a-form-item
            label="Application Id"
            :label-col="{lg: {span: 7}, sm: {span: 7}}"
            :wrapper-col="{lg: {span: 16}, sm: {span: 4} }">
            <a-input
              type="text"
              placeholder="ApplicationId"
              v-decorator="[ 'appId', {rules: [{ required: true, message: 'ApplicationId is required'}]} ]"/>
          </a-form-item>
          <a-form-item
            label="JobId"
            :label-col="{lg: {span: 7}, sm: {span: 7}}"
            :wrapper-col="{lg: {span: 16}, sm: {span: 4} }">
            <a-input
              type="text"
              placeholder="JobId"
              v-decorator="[ 'jobId', {rules: [{ required: true, message: 'JobId is required'}]} ]"/>
          </a-form-item>
        </a-form>

        <template slot="footer">
          <a-button
            key="back"
            @click="handleMappingCancel">
            Cancel
          </a-button>
          <a-button
            key="submit"
            type="primary"
            :loading="loading"
            @click="handleMappingOk">
            Apply
          </a-button>
        </template>
      </a-modal>

      <a-modal
        v-model="controller.visible"
        width="65%"
        :body-style="controller.modalStyle"
        :destroy-on-close="controller.modalDestroyOnClose"
        :footer="null"
        @ok="handleCloseWS">
        <template slot="title">
          <a-icon type="code"/>&nbsp; {{ controller.consoleName }}
        </template>
        <template slot="footer">
          <a-button
            key="submit"
            type="primary"
            @click="handleCloseWS">
            Close
          </a-button>
        </template>
        <div
          id="terminal"
          ref="terminal"
          class="terminal"/>
      </a-modal>

    </a-card>
  </div>
</template>
<script>
import Ellipsis from '@/components/Ellipsis'
import State from './State'
import {mapActions} from 'vuex'
import {list, dashboard, cancel, deploy, revoke, mapping, start, clean, yarn, remove} from '@api/application'
import {latest, history} from '@api/savepoint'
import {flamegraph} from '@api/metrics'
import {weburl} from '@api/setting'
import {Terminal} from 'xterm'
import 'xterm/css/xterm.css'
import SockJS from 'sockjs-client'
import {baseUrl} from '@/api/baseUrl'
import Stomp from 'webstomp-client'
import SvgIcon from '@/components/SvgIcon'
import {check} from '@/api/setting'

export default {
  components: {Ellipsis, State, SvgIcon},
  data() {
    return {
      loading: false,
      dashLoading: true,
      dashBigScreen: true,
      dataSource: [],
      metrics: {
        availableSlot: 0,
        totalSlot: 0,
        totalTM: 0,
        jmMemory: 0,
        tmMemory: 0,
        task: {
          total: 0,
          running: 0
        }
      },
      expandedRow: ['appId', 'jmMemory', 'tmMemory', 'totalTM', 'totalSlot', 'availableSlot', 'flinkCommit'],
      queryParams: {},
      sortedInfo: null,
      filteredInfo: null,
      queryInterval: 2000,
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
      flameGraph: false,
      restart: false,
      application: null,
      latestSavePoint: null,
      historySavePoint: null,
      allowNonRestoredState: false,
      searchText: '',
      searchInput: null,
      optionApps: {
        'starting': new Map(),
        'stoping': new Map(),
        'deploy': new Map()
      },
      searchedColumn: null,
      paginationInfo: null,
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
    innerColumns() {
      return [
        {title: 'Application Id', dataIndex: 'appId', key: 'appId', width: 280},
        {title: 'JobManager Memory', dataIndex: 'jmMemory', key: 'jmMemory'},
        {title: 'TaskManager Memory', dataIndex: 'tmMemory', key: 'tmMemory'},
        {title: 'Total TaskManager', dataIndex: 'totalTM', key: 'totalTM'},
        {title: 'Total Slots', dataIndex: 'totalSlot', key: 'totalSlot'},
        {title: 'Available Slots', dataIndex: 'availableSlot', key: 'availableSlot'}
      ]
    },
    columns() {
      let {sortedInfo, filteredInfo} = this
      sortedInfo = sortedInfo || {}
      filteredInfo = filteredInfo || {}
      return [{
        title: 'Application Name',
        dataIndex: 'jobName',
        width: 220,
        scopedSlots: {
          filterDropdown: 'filterDropdown',
          filterIcon: 'filterIcon',
          customRender: 'customRender'
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
            }, 0)
          }
        },
      }, {
        title: 'Job Type',
        dataIndex: 'jobType',
        width: 120,
        scopedSlots: {customRender: 'jobType'},
        filters: [
          {text: 'Custom Code', value: 1},
          {text: 'Flink SQL', value: 2}
        ]
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
      }, {
        title: 'Task',
        dataIndex: 'task',
        scopedSlots: {customRender: 'task'},
        width: 120
      }, {
        title: 'Run Status',
        dataIndex: 'state',
        width: 120,
        scopedSlots: {customRender: 'state'},
        filters: [
          {text: 'ADDED', value: 0},
          {text: 'DEPLOYING', value: 1},
          {text: 'DEPLOYED', value: 2},
          {text: 'CREATED', value: 4},
          {text: 'STARTING', value: 5},
          {text: 'RUNNING', value: 7},
          {text: 'FAILED', value: 9},
          {text: 'CANCELED', value: 11},
          {text: 'FINISHED', value: 12},
          {text: 'SUSPENDED', value: 13},
          {text: 'LOST', value: 15}
        ]
      }, {
        title: 'Deploy Status',
        dataIndex: 'deploy',
        width: 130,
        scopedSlots: {customRender: 'deployState'}
      }, {
        dataIndex: 'operation',
        key: 'operation',
        fixed: 'right',
        scopedSlots: {customRender: 'operation'},
        slots: {title: 'customOperation'},
        width: 200
      }]
    }
  },

  mounted() {
    this.handleYarn()
    this.handleDashboard()
    this.handleFetch(true)
    const timer = window.setInterval(() => {
      this.handleDashboard()
      this.handleFetch(false)
    }, this.queryInterval)
    this.$once('hook:beforeDestroy', () => {
      clearInterval(timer)
    })
    this.handleResize()
  },

  beforeMount() {
    this.formDeploy = this.$form.createForm(this)
    this.formStopSavePoint = this.$form.createForm(this)
    this.formStartCheckPoint = this.$form.createForm(this)
    this.formMapping = this.$form.createForm(this)
    this.dashBigScreen = (document.documentElement.offsetWidth || document.body.offsetWidth) >= 1500
  },

  methods: {
    ...mapActions(['SetAppId']),

    handleResize() {
      const $this = this
      window.onresize = () => {
        $this.dashBigScreen = (document.documentElement.offsetWidth || document.body.offsetWidth) >= 1500
      }
    },

    handleDeploy(app) {
      if (this.optionApps.deploy.get(app.id) === undefined || app['optionState'] === 0) {
        this.deployVisible = true
        this.application = app
      }
    },

    handleDeployTitle (deploy) {
      switch (deploy) {
        case -1:
          return 'dependency changed,but download dependency failed'
        case 1:
          return 'deploying'
        case 2:
          return 'application is updated,need relaunch'
        case 3:
          return 'dependency is updated,need relaunch'
        case 4:
          return 'config is updated,need restart'
        case 5:
          return 'flink sql is updated,need restart'
        case 6:
          return 'application is deployed to workspace,need restart'
        case 7:
          return 'application is rollbacked,need restart'
      }
    },

    handleDeployCancel() {
      this.deployVisible = false
      setTimeout(() => {
        this.application = null
        this.restart = false
        this.allowNonRestoredState = false
        this.savePoint = true
        this.formDeploy.resetFields()
      }, 1000)
    },

    handleDeployOk() {
      this.formDeploy.validateFields((err, values) => {
        if (!err) {
          const id = this.application.id
          const savePoint = this.savePoint
          const description = values.description
          const restart = this.restart
          const allowNonRestoredState = this.allowNonRestoredState
          this.handleDeployCancel()
          this.optionApps.deploy.set(id, new Date().getTime())
          this.handleMapUpdate('deploy')
          this.$swal.fire({
            icon: 'success',
            title: 'The current job is deploying',
            showConfirmButton: false,
            timer: 2000
          }).then((r)=> {
            deploy({
              id: id,
              restart: restart,
              savePointed: savePoint,
              allowNonRestored: allowNonRestoredState,
              backUpDescription: description
            }).then((resp) => {
              if (!restart) {
                this.optionApps.deploy.delete(id)
                this.handleMapUpdate('deploy')
              }
            })
          })
        }
      })
    },

    handleMapping(app) {
      this.mappingVisible = true
      this.application = app
    },

    handleMappingOk() {
      this.formMapping.validateFields((err, values) => {
        if (!err) {
          const appId = values.appId
          const jobId = values.jobId
          const id = this.application.id
          this.handleMappingCancel()
          this.$swal.fire({
            icon: 'success',
            title: 'The current job is mapping',
            showConfirmButton: false,
            timer: 2000
          }).then((r)=> {
            mapping({
              id: id,
              appId: appId,
              jobId: jobId
            }).then((resp) => {
              console.log(resp)
            })
          })
        }
      })
    },

    handleMappingCancel() {
      this.mappingVisible = false
      setTimeout(() => {
        this.application = null
        this.formMapping.resetFields()
      }, 1000)
    },

    handleIsStart(app) {
      const status = app.state === 0 ||
        app.state === 2 ||
        app.state === 9 ||
        app.state === 11 ||
        app.state === 12 ||
        app.state === 13 ||
        app.state === 15 || false

      const optionState = this.optionApps.starting.get(app.id) == undefined || app['optionState'] == 0 || false

      return status && optionState
    },

    handleStart(app) {
      if (this.optionApps.starting.get(app.id) === undefined || app['optionState'] === 0) {
        this.application = app
        latest({
          appId: this.application.id
        }).then((resp) => {
          this.latestSavePoint = resp.data || null
          this.startVisible = true
          if (!this.latestSavePoint) {
            history({
              appId: this.application.id,
              pageNum: 1,
              pageSize: 9999
            }).then((resp) => {
              this.historySavePoint = []
              resp.data.records.forEach(x => {
                if (x.path) {
                  this.historySavePoint.push(x)
                }
              })
            })
          }
        })
      }
    },

    handleStartCancel() {
      this.startVisible = false
      setTimeout(() => {
        this.allowNonRestoredState = false
        this.formStartCheckPoint.resetFields()
        this.application = null
        this.savePoint = true
        this.flameGraph = false
      }, 1000)
    },

    handleStartOk() {
      this.formStartCheckPoint.validateFields((err, values) => {
        if (!err) {
          const id = this.application.id
          const savePointed = this.savePoint
          const flameGraph = this.flameGraph
          const savePoint = savePointed ? (values['savepoint'] || this.latestSavePoint.savePoint) : null
          const allowNonRestoredState = this.allowNonRestoredState
          this.optionApps.starting.set(id, new Date().getTime())
          this.handleMapUpdate('starting')
          this.handleStartCancel()
          this.$swal.fire({
            icon: 'success',
            title: 'The current job is starting',
            showConfirmButton: false,
            timer: 2000
          }).then((r)=> {
            start({
              id: id,
              savePointed: savePointed,
              savePoint: savePoint,
              flameGraph: flameGraph,
              allowNonRestored: allowNonRestoredState
            }).then((resp) => {
              const code = parseInt(resp.data)
              if (code === 0) {
                this.$swal.fire(
                  'Failed',
                  'startup failed, please check the startup log :)',
                  'error'
                )
              } else if (code === -1) {
                this.$swal.fire(
                    'Failed',
                    'startup failed, Maybe FLINK_HOME undefined,please check :)',
                    'error'
                )
              }
            })
          })
        }
      })
    },

    handleCancel(app) {
      if (this.optionApps.stoping.get(app.id) === undefined || app['optionState'] === 0) {
        this.stopVisible = true
        this.application = app
      }
    },

    handleStopCancel() {
      this.stopVisible = false
      setTimeout(() => {
        this.formStopSavePoint.resetFields()
        this.drain = false
        this.savePoint = true
        this.application = null
      }, 1000)
    },

    handleStopOk() {
      const id = this.application.id
      const savePointed = this.savePoint
      const drain = this.drain
      this.optionApps.stoping.set(id, new Date().getTime())
      this.handleMapUpdate('stoping')
      this.handleStopCancel()

      this.$swal.fire({
        icon: 'success',
        title: 'The current job is canceling',
        showConfirmButton: false,
        timer: 2000
      }).then((result) => {
        cancel({
          id: id,
          savePointed: savePointed,
          drain: drain
        }).then((resp) => {
          if (resp.status === 'error') {
            this.$swal.fire(
              'Failed',
              resp.exception,
              'error'
            )
          }
        })
      })
    },

    handleDetail(app) {
      this.SetAppId(app.id)
      this.$router.push({'path': '/flink/app/detail'})
    },

    handleCheckFlameGraph() {
      if (this.flameGraph) {
        weburl({}).then((resp)=>{
          if ( resp.data == null || resp.data === '' ) {
            this.$swal.fire(
              'Failed',
              ' flameGraph enable Failed <br><br> StreamX Webapp address not defined <br><br> please check!',
              'error'
            )
            this.flameGraph = false
          }
        })
      }
    },

    handleFlameGraph(app) {
      flamegraph({
          appId: app.id,
          width: document.documentElement.offsetWidth || document.body.offsetWidth
        },
        (resp) => {
          if (resp != null) {
            const blob = new Blob([resp], {type: 'image/svg+xml'})
            const imageUrl = (window.URL || window.webkitURL).createObjectURL(blob)
            window.open(imageUrl)
          }
        },
        {loading: 'flameGraph generating...', error: 'flameGraph generate failed'}
      )
    },

    handleCanDelete (app) {
      return app.state === 0 ||
        app.state === 2 ||
        app.state === 9 ||
        app.state === 11 ||
        app.state === 12 ||
        app.state === 15 ||
        app.state === 19 || false
    },

    handleDelete(app) {
      remove({
        id: app.id
      }).then((resp)=>{
        this.$swal.fire({
          icon: 'success',
          title: 'delete successful',
          showConfirmButton: false,
          timer: 2000
        }).then((result) => {

        })
      })
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
    },

    handleReset(clearFilters) {
      clearFilters()
      this.searchText = null
      this.searchedColumn = null
      // 重置列排序规则
      this.sortedInfo = null
      // 重置查询参数
      this.queryParams = {}
    },

    handleTableChange(pagination, filters, sorter) {
      this.sortedInfo = sorter
      this.paginationInfo = pagination
      if (filters['jobType']) {
        this.queryParams['jobTypeArray'] = filters['jobType']
      }
      if (filters['state']) {
        this.queryParams['stateArray'] = filters['state']
      }
      if (sorter.field) {
        this.queryParams['sortField'] = sorter.field
      }
      if (sorter.order) {
        this.queryParams['sortOrder'] = sorter.order
      }
      this.handleFetch(true)
    },

    handleFetch(loading) {
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
      list({...params}).then((resp) => {
        this.loading = false
        const pagination = {...this.pagination}
        pagination.total = parseInt(resp.data.total)
        const dataSource = resp.data.records
        const timestamp = new Date().getTime()
        dataSource.forEach(x => {
          x.expanded = [{
            'appId': x.appId,
            'jmMemory': x.jmMemory,
            'tmMemory': x.tmMemory,
            'totalTM': x.totalTM,
            'totalSlot': x.totalSlot,
            'availableSlot': x.availableSlot
          }]
          if (x['optionState'] === 0) {
            if (this.optionApps.starting.get(x.id) !== undefined) {
              if (timestamp - this.optionApps.starting.get(x.id) > this.queryInterval * 2) {
                this.optionApps.starting.delete(x.id)
                this.handleMapUpdate('starting')
              }
            }
            if (this.optionApps.stoping.get(x.id) !== undefined) {
              if (timestamp - this.optionApps.stoping.get(x.id) > this.queryInterval) {
                this.optionApps.stoping.delete(x.id)
                this.handleMapUpdate('stoping')
              }
            }
            if (this.optionApps.deploy.get(x.id) !== undefined) {
              if (timestamp - this.optionApps.deploy.get(x.id) > this.queryInterval) {
                this.optionApps.deploy.delete(x.id)
                this.handleMapUpdate('deploy')
              }
            }
          }
        })
        this.pagination = pagination
        this.dataSource = dataSource
      })
    },

    handleDashboard() {
      dashboard({}).then((resp) => {
        const status = resp.status || 'error'
        if (status === 'success') {
          this.dashLoading = false
          this.metrics = resp.data || {}
        }
      })
    },

    handleExpandIcon(props) {
      if (props.record.state === 7) {
        if (props.expanded) {
          return <a class="expand-icon-open" onClick={(e) => {
            props.onExpand(props.record, e)
          }}>
            <a-icon type="down"/>
          </a>
        } else {
          return <a class="expand-icon-close" onClick={(e) => {
            props.onExpand(props.record, e)
          }}>
            <a-icon type="right"/>
          </a>
        }
      } else {
        return ''
      }
    },

    handleYarn() {
      yarn({}).then((resp) => {
        this.yarn = resp.data
      })
    },

    handleView(params) {
      if (params.state === 6 || params.state === 7 || params['optionState'] === 4) {
        const url = this.yarn + '/proxy/' + params['appId'] + '/'
        window.open(url)
      }
    },

    handleAdd() {
      check().then((resp) => {
        const success = resp.data === true || resp.data === 'true'
        if (success) {
          this.$router.push({'path': '/flink/app/add'})
        } else {
          this.$swal.fire(
            'Failed',
            'Please check "StreamX Console Workspace" is defined and make sure have read and write permissions',
            'error'
          )
        }
      })
    },

    handleEdit(app) {
      this.SetAppId(app.id)
      if (app.appType === 1) {
        this.$router.push({'path': '/flink/app/edit_streamx'})
      } else {
        this.$router.push({'path': '/flink/app/edit_flink'})
      }
    },

    handleRevoke (app) {
      revoke({
        id: app.id
      }).then((resp)=>{

      })
    },

    handleCleanDeploy(app) {
      clean({
        id: app.id
      }).then((resp) => {
      })
    },

    handleMapUpdate(type) {
      const map = this.optionApps[type]
      this.optionApps[type] = new Map(map)
    },

    handleSeeLog(app) {
      this.controller.consoleName = app.jobName + ' Deploying log'
      this.controller.visible = true
      this.$nextTick(function () {
        this.handleOpenWS(app)
      })
    },

    handleOpenWS(app) {
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
      const socket = new SockJS(baseUrl(true).concat('/websocket'))
      this.stompClient = Stomp.over(socket)
      this.stompClient.connect({}, (success) => {
        this.stompClient.subscribe(
            '/resp/mvn',
            (msg) => {
              if(msg.body.startsWith('[Exception]')) {
                this.$swal.fire(
                    'Failed',
                    msg.body,
                    'error'
                )
              }
              this.terminal.writeln(msg.body)
            })
        this.stompClient.send('/req/mvn/' + app.id)
      })
    },

    handleCloseWS() {
      this.stompClient.disconnect()
      this.controller.visible = false
      this.terminal.clear()
      this.terminal.clearSelection()
      this.terminal = null
    }

  }
}
</script>

<style lang="less">
@import "View";
</style>
