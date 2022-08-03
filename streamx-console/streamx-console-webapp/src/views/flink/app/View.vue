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
            <a-divider class="def-margin-bottom"/>
            <div>
              <span>
                Task Slots
                <strong>{{ metrics.totalSlot }}</strong>
              </span>
              <a-divider type="vertical"/>
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
            <a-divider class="def-margin-bottom"/>
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
            <a-divider class="def-margin-bottom"/>
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
            <a-divider class="def-margin-bottom"/>
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
            <a-divider class="def-margin-bottom"/>
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
              <a-divider type="vertical"/>
              <span>
                Task Slots
                <strong>{{ metrics.totalSlot }}</strong>
              </span>
              <a-divider type="vertical"/>
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
            <a-divider class="def-margin-bottom"/>
            <div>
              <span>
                Total JobManager Mem
                <strong>{{ metrics.jmMemory }} MB</strong>
              </span>
              <a-divider type="vertical"/>
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

      <div slot="extra">
        <a-input-group compact>
          <a-select placeholder="Team" allowClear @change="handleChangeTeam" style="width: 140px">
            <a-select-option v-for="t in teamData" :key="t.teamId"> {{ t.teamName }} </a-select-option>
          </a-select>
          <a-select placeholder="User" allowClear @change="handleChangeUser" style="margin-left: 16px;width: 120px">
            <a-select-option v-for="u in users" :key="u.userId">
              <span v-if="u.nickName"> {{ u.nickName }} </span>
              <span v-else> {{ u.username }} </span>
            </a-select-option>
          </a-select>
          <a-select placeholder="Type" allowClear @change="handleChangeJobType" style="margin-left: 16px;width: 80px">
            <a-select-option value="1">JAR</a-select-option>
            <a-select-option value="2">SQL</a-select-option>
          </a-select>
          <a-input-search
            placeholder="Search..."
            v-model="searchText"
            @change="handleSearch"
            style="width: 250px;"/>
          <a-button
            type="primary"
            icon="plus"
            style="margin-left: 20px"
            @click="handleAdd">
            Add New
          </a-button>
        </a-input-group>
      </div>

      <!-- 表格区域 -->
      <a-table
        ref="TableInfo"
        :columns="columns"
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
          class="expanded-table"
          slot-scope="record"
          v-if="record.state === 5"
          row-key="id"
          :pagination="false"/>

        <a-icon
          slot="filterIcon"
          slot-scope="filtered"
          type="search"
          :style="{ color: filtered ? '#108ee9' : undefined }"/>

        <template
          slot="jobName"
          slot-scope="text, record">
          <span
            class="app_type app_jar"
            v-if="record['jobType'] === 1">
            JAR
          </span>
          <span
            class="app_type app_sql"
            v-if="record['jobType'] === 2">
            SQL
          </span>

          <span
            class="link"
            :class="{pointer: record.state === 4 || record.state === 5 || record['optionState'] === 4 }"
            @click="handleView(record)">{{ record.jobName }}
          </span>

          <template v-if="record['jobType'] === 1">
            <a-badge
              class="build-badge"
              v-if="record.launch === 5"
              count="NEW"
              title="the associated project has changed and this job need to be rechecked"/>
            <a-badge
              class="build-badge"
              v-else-if="record.launch >= 2"
              count="NEW"
              title="the application has changed."/>
          </template>

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
          slot="launchState"
          slot-scope="text, record">
          <State
            option="launch"
            :title="handleLaunchTitle(record.launch)"
            :data="record"/>
          <a-divider type="vertical" style="margin: 0 4px" v-if="record.buildStatus != null"/>
          <State
            option="build"
            click="openBuildProgressDetailDrawer(record)"
            :data="record"/>
        </template>

        <template
          slot="customOperation">
          Operation
        </template>

        <template
          slot="operation"
          slot-scope="text, record">

          <a-tooltip title="Edit Application">
            <a-button
              v-permit="'app:update'"
              @click.native="handleEdit(record)"
              shape="circle"
              size="small"
              style="margin-left: 10px"
              class="control-button ctl-btn-color">
              <a-icon type="edit"/>
            </a-button>
          </a-tooltip>

          <a-tooltip title="Launch Application">
            <a-button
              v-if="(record.launch === -1 || record.launch === 1 || record.launch === 4) && record['optionState'] === 0"
              @click.native="handleCheckLaunchApp(record)"
              shape="circle"
              size="small"
              class="control-button ctl-btn-color">
              <a-icon type="cloud-upload"/>
            </a-button>
          </a-tooltip>

          <a-tooltip title="Launching Progress Detail">
            <a-button
              v-if="record.launch === -1 || record.launch === 2 || record['optionState'] === 1"
              @click.native="openBuildProgressDetailDrawer(record)"
              shape="circle"
              size="small"
              class="control-button ctl-btn-color">
              <a-icon type="container"/>
            </a-button>
          </a-tooltip>

          <a-tooltip title="Start Application">
            <a-button
              v-show="handleIsStart(record)"
              v-permit="'app:start'"
              @click.native="handleAppCheckStart(record)"
              shape="circle"
              size="small"
              class="control-button ctl-btn-color">
              <a-icon type="play-circle"/>
            </a-button>
          </a-tooltip>

          <a-tooltip title="Cancel Application">
            <a-button
              v-show="record.state === 5 && record['optionState'] === 0"
              v-permit="'app:cancel'"
              @click.native="handleCancel(record)"
              shape="circle"
              size="small"
              class="control-button ctl-btn-color">
              <a-icon type="pause-circle"/>
            </a-button>
          </a-tooltip>

          <a-tooltip title="View Application Detail">
            <a-button
              v-permit="'app:detail'"
              @click.native="handleDetail(record)"
              shape="circle"
              size="small"
              class="control-button ctl-btn-color">
              <a-icon type="eye"/>
            </a-button>
          </a-tooltip>

          <a-tooltip title="View FlameGraph">
            <a-button
              v-if="record.flameGraph"
              v-permit="'app:flameGraph'"
              @click.native="handleFlameGraph(record)"
              shape="circle"
              size="small"
              class="control-button ctl-btn-color">
              <a-icon type="fire"/>
            </a-button>
          </a-tooltip>

          <a-tooltip title="Remapping Application">
            <a-button
              v-if="handleCanRemapping(record)"
              v-permit="'app:mapping'"
              @click.native="handleMapping(record)"
              shape="circle"
              size="small"
              class="control-button ctl-btn-color">
              <a-icon type="deployment-unit"/>
            </a-button>
          </a-tooltip>

          <template v-if="handleCanDelete(record)">
            <a-popconfirm
              title="Are you sure delete this job ?"
              cancel-text="No"
              ok-text="Yes"
              v-permit="'app:delete'"
              @confirm="handleDelete(record)">
              <a-button
                type="danger"
                shape="circle"
                size="small"
                class="control-button">
                <a-icon type="delete"/>
              </a-button>
            </a-popconfirm>
          </template>

          <a-tooltip title="Forced Stop Application">
            <a-button
              type="danger"
              shape="circle"
              size="small"
              v-show="handleCanStop(record)"
              v-permit="'app:cancel'"
              @click.native="handleForcedStop(record)"
              class="control-button">
              <a-icon type="pause-circle"/>
            </a-button>
          </a-tooltip>

        </template>

      </a-table>

      <!-- app building progress detail-->
      <template>
        <a-drawer
          title="Application Launching Progress"
          placement="right"
          width="500"
          :closable="true"
          :visible="appBuildDrawerVisual"
          @close="closeBuildProgressDrawer">
          <!-- status and cost time -->
          <h3>
            <a-icon type="dashboard"/>
            Summary
          </h3>
          <template v-if="appBuildDetail.pipeline != null">
            <a-row>
              <a-progress
                v-if="appBuildDetail.pipeline.hasError"
                :percent="appBuildDetail.pipeline.percent"
                status="exception"/>
              <a-progress
                v-else-if="appBuildDetail.pipeline.percent < 100"
                :percent="appBuildDetail.pipeline.percent"
                status="active"/>
              <a-progress
                v-else
                :percent="appBuildDetail.pipeline.percent"/>
            </a-row>
            <a-row style="margin-top: 10px">
              <template v-if="appBuildDetail.pipeline.pipeStatus == 2">
                <a-tag :color="handleAppBuildStatusColor(appBuildDetail.pipeline.pipeStatus)" class="running-tag">
                  {{ handleAppBuildStatueText(appBuildDetail.pipeline.pipeStatus) }}
                </a-tag>
              </template>
              <template v-else>
                <a-tag :color="handleAppBuildStatusColor(appBuildDetail.pipeline.pipeStatus)">
                  {{ handleAppBuildStatueText(appBuildDetail.pipeline.pipeStatus) }}
                </a-tag>
              </template>
              cost {{ appBuildDetail.pipeline.costSec }} seconds
            </a-row>
          </template>
          <template v-else>
            <a-empty/>
          </template>
          <a-divider/>

          <!-- step detail -->
          <h3>
            <a-icon type="project"/>
            Steps Detail
          </h3>
          <template v-if="appBuildDetail.pipeline != null">
            <a-timeline style="margin-top: 20px">
              <a-timeline-item
                v-for="item in appBuildDetail.pipeline.steps"
                :key="item.seq"
                :color="handleAppBuildStepTimelineColor(item)">
                <!-- step status, desc -->
                <p>
                  <tempalte v-if="item.status == 2">
                    <a-tag :color="handleAppBuildStepTimelineColor(item)" class="running-tag">
                      {{ handleAppBuildStepText(item.status) }}
                    </a-tag>
                    <b>Step-{{ item.seq }}</b> {{ item.desc }}
                  </tempalte>
                  <template v-else>
                    <a-tag :color="handleAppBuildStepTimelineColor(item)">
                      {{ handleAppBuildStepText(item.status) }}
                    </a-tag>
                    <b>Step-{{ item.seq }}</b> {{ item.desc }}
                  </template>
                </p>
                <!-- step info update time --->
                <template v-if="item.status !== 0 && item.status !== 1">
                  <p style="color: gray; font-size: 12px">{{ item.ts }}</p>
                </template>
                <!-- docker resolved detail --->
                <template v-if="appBuildDetail.pipeline.pipeType === 2 && appBuildDetail.docker !== null">
                  <template
                    v-if="item.seq === 5 && appBuildDetail.docker.pull !== null && appBuildDetail.docker.pull.layers !== null">
                    <template v-for="(layer,index) in appBuildDetail.docker.pull.layers">
                      <a-row :key="layer.layerId" style="margin-bottom: 5px;">
                        <a-space size="small">
                          <a-icon type="arrow-right"/>
                          <a-tag color="blue"> {{ layer.layerId }}</a-tag>
                          <a-tag>{{ layer.status }}</a-tag>
                          <template v-if="layer.totalMb != null && layer.totalMb !== 0">
                            <span style="font-size: 12px; text-align: right">
                              {{ layer.currentMb }} / {{ layer.totalMb }} MB</span>
                          </template>
                        </a-space>
                      </a-row>
                      <template v-if="layer.totalMb != null && layer.totalMb !== 0">
                        <a-row :key="index" style="margin-left: 20px; margin-right: 50px; margin-bottom: 15px;">
                          <a-progress
                            :percent="layer.percent"
                            status="active"/>
                        </a-row>
                      </template>
                    </template>
                  </template>

                  <template
                    v-else-if="item.seq === 6 && appBuildDetail.docker.build !== null && appBuildDetail.docker.build.steps != null">
                    <a-list
                      bordered
                      :data-source="appBuildDetail.docker.build.steps"
                      size="small">
                      <a-list-item slot="renderItem" slot-scope="step">
                        <a-space>
                          <a-icon type="arrow-right"/>
                          <span style="font-size: 12px">{{ step }}</span>
                        </a-space>
                      </a-list-item>
                    </a-list>
                  </template>

                  <template
                    v-else-if="item.seq === 7 && appBuildDetail.docker.push !== null && appBuildDetail.docker.push.layers !== null">
                    <template v-for="(layer,index) in appBuildDetail.docker.push.layers">
                      <a-row :key="layer.layerId" style="margin-bottom: 5px;">
                        <a-space size="small">
                          <a-icon type="arrow-right"/>
                          <a-tag color="blue"> {{ layer.layerId }}</a-tag>
                          <a-tag>{{ layer.status }}</a-tag>
                          <template v-if="layer.totalMb != null && layer.totalMb !== 0">
                            <span style="font-size: 12px; text-align: right">
                              {{ layer.currentMb }} / {{ layer.totalMb }} MB</span>
                          </template>
                        </a-space>
                      </a-row>
                      <template v-if="layer.totalMb != null && layer.totalMb !== 0">
                        <a-row :key="index" style="margin-left: 20px; margin-right: 50px; margin-bottom: 15px;">
                          <a-progress
                            :percent="layer.percent"
                            status="active"/>
                        </a-row>
                      </template>
                    </template>
                  </template>

                </template>
              </a-timeline-item>
            </a-timeline>
          </template>
          <template v-else>
            <a-empty/>
          </template>

          <!-- error log drawer-->
          <a-drawer
            title="Error Log"
            placement="right"
            width="800"
            :closable="true"
            :visible="appBuildErrorLogDrawerVisual"
            @close="closeBuildErrorLogDrawer">
            <template
              v-if="appBuildDetail.pipeline != null && appBuildDetail.pipeline.hasError">
              <h3>Error Summary</h3>
              <br/>
              <p>{{ appBuildDetail.pipeline.errorSummary }}</p>
              <a-divider/>
              <h3>Error Stack</h3>
              <br/>
              <pre style="font-size: 12px">{{ appBuildDetail.pipeline.errorStack }}</pre>
            </template>
            <template v-else>
              <a-empty/>
            </template>
          </a-drawer>

          <!-- bottom tools -->
          <div
            v-if="appBuildDetail.pipeline != null && appBuildDetail.pipeline.hasError"
            :style="{
              position: 'absolute',
              bottom: 0,
              width: '100%',
              borderTop: '1px solid #e8e8e8',
              padding: '10px 16px',
              textAlign: 'right',
              left: 0,
              background: '#fff',
              borderRadius: '0 0 4px 4px'}">
            <a-button type="primary" @click.native="openBuildErrorLogDrawer">
              <a-icon type="warning"/>
              Error Log
            </a-button>
          </div>
        </a-drawer>

      </template>

      <a-modal
        title="WARNING"
        okType="danger"
        okText="Yes"
        cancelText="Cancel"
        :visible="forceBuildAppModalVisual"
        @ok="handleLaunchApp(application, true)"
        @cancel="closeCheckForceBuildModel">
        <p>The current launch of the application is in progress.</p>
        <p>are you sure you want to force another build?</p>
      </a-modal>

      <a-modal
        title="WARNING"
        okType="danger"
        okText="Yes"
        cancelText="Cancel"
        :visible="forceStartAppModalVisual"
        @ok="handleStart(application)"
        @cancel="closeForceStartAppModal">
        <template v-if="appBuildDetail.pipeline == null">
          <p>No build record exists for the current application.</p>
        </template>
        <template v-else>
          <p>The current build state of the application is
            <a-tag color="orange">
              {{ handleAppBuildStatueText(appBuildDetail.pipeline.pipeStatus) }}
            </a-tag>
          </p>
        </template>
        <p>Are you sure to force the application to run?</p>
      </a-modal>

      <a-modal
        v-model="startVisible"
        on-ok="handleStartOk">
        <template
          slot="title">
          <svg-icon
            slot="icon"
            name="play"/>
          Start Application
        </template>

        <a-form
          @submit="handleStartOk"
          :form="formStartCheckPoint">
          <a-form-item
            label="flame Graph"
            :label-col="{lg: {span: 7}, sm: {span: 7}}"
            :wrapper-col="{lg: {span: 16}, sm: {span: 4} }"
            v-show="executionMode !== 5 && executionMode !== 6">
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
              v-model="startSavePointed"
              v-decorator="['startSavePointed']"/>
            <span
              class="conf-switch"
              style="color:darkgrey"> restore the application from savepoint or latest checkpoint</span>
          </a-form-item>

          <a-form-item
            class="def-margin-bottom"
            v-if="startSavePointed && !latestSavePoint "
            label="savepoint"
            :label-col="{lg: {span: 7}, sm: {span: 7}}"
            :wrapper-col="{lg: {span: 16}, sm: {span: 4} }">
            <a-select
              v-if="historySavePoint && historySavePoint.length>0"
              mode="combobox"
              allow-clear
              v-decorator="['startSavePoint',{ rules: [{ required: true } ]}]">
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
              v-decorator="['startSavePoint',{ rules: [{ required: true } ]}]"/>
            <span
              class="conf-switch"
              style="color:darkgrey"> restore the application from savepoint or latest checkpoint</span>
          </a-form-item>

          <a-form-item
            v-if="startSavePointed"
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
            :label-col="{lg: {span: 7}, sm: {span: 7}}"
            :wrapper-col="{lg: {span: 16}, sm: {span: 4} }">
            <a-switch
              checked-children="ON"
              un-checked-children="OFF"
              v-model="stopSavePointed"
              v-decorator="['stopSavePointed']"/>
            <span
              class="conf-switch"
              style="color:darkgrey"> trigger savePoint before taking cancel </span>
          </a-form-item>
          <a-form-item
            class="def-margin-bottom"
            label="Custom SavePoint"
            :label-col="{lg: {span: 7}, sm: {span: 7}}"
            :wrapper-col="{lg: {span: 16}, sm: {span: 4} }"
            v-show="stopSavePointed">
            <a-input
              type="text"
              placeholder="Entry the custom savepoint path"
              v-model="customSavepoint"
              v-decorator="['customSavepoint']"/>
            <div style="color:darkgrey">cancel job with savepoint path.</div>
          </a-form-item>
          <a-form-item
            label="Drain"
            :label-col="{lg: {span: 7}, sm: {span: 7}}"
            :wrapper-col="{lg: {span: 16}, sm: {span: 4} }">
            <a-switch
              checked-children="ON"
              un-checked-children="OFF"
              placeholder="Send max watermark before job stopped"
              v-model="drain"
              v-decorator="['drain']"/>
            <span
              class="conf-switch"
              style="color:darkgrey"> Send max watermark before stopped</span>
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
import {
  cancel,
  clean,
  checkSavepointPath,
  dashboard,
  downLog,
  forcedStop,
  list,
  mapping,
  remove,
  revoke,
  start,
  yarn,
  verifySchema
} from '@api/application'
import {history, latest} from '@api/savepoint'
import {flamegraph} from '@api/metrics'
import {weburl} from '@api/setting'
import {build, detail as buildDetail} from '@/api/appBuild'
import {activeURL} from '@/api/flinkCluster'
import {Terminal} from 'xterm'
import 'xterm/css/xterm.css'
import {baseUrl} from '@/api/baseUrl'
import SvgIcon from '@/components/SvgIcon'
import storage from '@/utils/storage'
import {listByUser as getUserTeam} from '@/api/team'
import {list as listUser} from '@/api/user'

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
      teamData: [],
      users: [],
      expandedRow: ['appId', 'jmMemory', 'tmMemory', 'totalTM', 'totalSlot', 'availableSlot', 'flinkCommit'],
      queryParams: {},
      jobType: null,
      teamId: null,
      userId: null,
      sortedInfo: null,
      filteredInfo: null,
      queryInterval: 2000,
      yarn: null,
      stopVisible: false,
      startVisible: false,
      mappingVisible: false,
      formDeploy: null,
      formStopSavePoint: null,
      formStartCheckPoint: null,
      formMapping: null,
      drain: false,
      startSavePointed: true,
      stopSavePointed: true,
      customSavepoint: null,
      flameGraph: false,
      restart: false,
      application: null,
      executionMode: null,
      latestSavePoint: null,
      historySavePoint: null,
      allowNonRestoredState: false,
      searchText: '',
      optionApps: {
        'starting': new Map(),
        'stopping': new Map(),
        'launch': new Map()
      },
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
      },
      socketId: null,
      storageKey: 'DOWN_SOCKET_ID',
      appBuildDetail: {
        pipeline: null,
        docker: null,
      },
      appBuildDrawerVisual: false,
      appBuildErrorLogDrawerVisual: false,
      appBuildDtlReqTimer: null,
      forceBuildAppModalVisual: false,
      forceStartAppModalVisual: false,
    }
  },

  computed: {
    columns() {
      let {sortedInfo, filteredInfo} = this
      sortedInfo = sortedInfo || {}
      filteredInfo = filteredInfo || {}
      return [ {
        title: 'Application Name',
        dataIndex: 'jobName',
        width: 320,
        scopedSlots: {customRender: 'jobName'},
      },  {
        title: 'Flink Version',
        dataIndex: 'flinkVersion',
        width: 130
      }, {
        title: 'Owner',
        dataIndex: 'nickName',
        width: 130
      }, {
        title: 'Run Status',
        dataIndex: 'state',
        width: 120,
        scopedSlots: {customRender: 'state'},
        filters: [
          {text: 'ADDED', value: 0},
          {text: 'STARTING', value: 3},
          {text: 'RUNNING', value: 5},
          {text: 'FAILED', value: 6},
          {text: 'CANCELED', value: 9},
          {text: 'FINISHED', value: 10},
          {text: 'LOST', value: 13},
          {text: 'SILENT', value: 17},
          {text: 'TERMINATED', value: 18}
        ]
      }, {
        title: 'Launch | Build',
        dataIndex: 'launch',
        width: 220,
        scopedSlots: {customRender: 'launchState'}
      }, {
        title: 'Duration',
        dataIndex: 'duration',
        sorter: true,
        sortOrder: sortedInfo.columnKey === 'duration' && sortedInfo.order,
        scopedSlots: {customRender: 'duration'},
        width: 150
      }, {
        title: 'Modified Time',
        dataIndex: 'modifyTime',
        sorter: true,
        sortOrder: sortedInfo.columnKey === 'modifyTime' && sortedInfo.order,
        width: 170
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
    this.handleDashboard()
    listUser({'pageSize': '9999'}).then((resp) => {
      this.users = resp.data.records
    })

    getUserTeam(
      {'pageSize': '9999'}
    ).then((resp) => {
      this.teamData = resp.data.records
    })
    this.handleFetch(true)
    const timer = window.setInterval(() => {
      this.handleDashboard()
      this.handleFetch(false)
    }, this.queryInterval)
    this.$once('hook:beforeDestroy', () => {
      clearInterval(timer)
      clearInterval(this.appBuildDtlReqTimer)
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

    handleLaunchTitle(launch) {
      switch (launch) {
        case -1:
          return 'launch failed'
        case 1:
          return 'current job need relaunch'
        case 2:
          return 'launching'
        case 3:
          return 'launch finished,need restart'
        case 4:
          return 'application is rollbacked,need relaunch'
      }
    },

    handleChangeJobType(jobType) {
      this.jobType = jobType
      this.handleSearch()
    },
    handleChangeTeam(team) {
      this.teamId = team
      this.handleSearch()
    },
    handleChangeUser(user) {
      this.userId = user
      this.handleSearch()
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
          }).then((r) => {
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

    showCheckForceBuildModel() {
      this.forceBuildAppModalVisual = true
    },

    closeCheckForceBuildModel() {
      this.forceBuildAppModalVisual = false
    },


    handleCheckLaunchApp(app) {
      this.application = app
      if (app['appControl']['allowBuild'] === true) {
        this.handleLaunchApp(app, false)
      } else {
        this.showCheckForceBuildModel()
      }
    },

    handleLaunchApp(app, force) {
      this.closeCheckForceBuildModel()
      this.$swal.fire({
        icon: 'success',
        title: 'Current Application is launching',
        showConfirmButton: false,
        timer: 2000
      }).then((e) =>
        build({
          appId: app.id,
          forceBuild: force
        }).then((resp) => {
          if (!resp.data) {
            this.$swal.fire(
              'Failed',
              'lanuch application failed, ' + resp.message.replaceAll(/\[StreamX]/g, ''),
              'error'
            )
          }
        })
      )
    },

    handleFetchBuildDetail(app) {
      buildDetail({
        appId: app.id
      }).then((resp) => {
        this.appBuildDetail.pipeline = resp.data.pipeline
        this.appBuildDetail.docker = resp.data.docker
      })
    },

    openBuildProgressDetailDrawer(app) {
      this.appBuildDrawerVisual = true
      if (this.appBuildDtlReqTimer) {
        clearInterval(this.appBuildDtlReqTimer)
      }
      this.handleFetchBuildDetail(app)
      this.appBuildDtlReqTimer = window.setInterval(() => this.handleFetchBuildDetail(app), 500)
    },

    closeBuildProgressDrawer() {
      this.appBuildDrawerVisual = false
      clearInterval(this.appBuildDtlReqTimer)
      this.appBuildDtlReqTimer = null
      this.appBuildDetail.pipeline = null
      this.appBuildDetail.docker = null
    },

    openBuildErrorLogDrawer() {
      this.appBuildErrorLogDrawerVisual = true
    },

    closeBuildErrorLogDrawer() {
      this.appBuildErrorLogDrawerVisual = false
    },

    handleAppBuildStatusColor(statusCode) {
      switch (statusCode) {
        case 0:
          return '#99A3A4'
        case 1:
          return '#F5B041'
        case 2:
          return '#3498DB'
        case 3:
          return '#2ECC71'
        case 4:
          return '#E74C3C'
        default:
          return '#99A3A4'
      }
    },

    handleAppBuildStatueText(statusCode) {
      switch (statusCode) {
        case 0:
          return 'UNKNOWN'
        case 1:
          return 'PENDING'
        case 2:
          return 'RUNNING'
        case 3:
          return 'SUCCESS'
        case 4:
          return 'FAILURE'
        default:
          return 'UNKNOWN'
      }
    },

    handleAppBuildStepTimelineColor(step) {
      if (step == null) {
        return 'gray'
      }
      switch (step.status) {
        case 0:
        case 1:
          return '#99A3A4'
        case 2:
          return '#3498DB'
        case 3:
          return '#2ECC71'
        case 4:
          return '#E74C3C'
        case  5:
          return '#F5B041'
        default:
          return '#99A3A4'
      }
    },

    handleAppBuildStepText(stepStatus) {
      switch (stepStatus) {
        case 0:
          return 'UNKNOWN'
        case 1:
          return 'WAITING'
        case 2:
          return 'RUNNING'
        case 3:
          return 'SUCCESS'
        case 4:
          return 'FAILURE'
        case 5:
          return 'SKIPPED'
      }
    },

    handleIsStart(app) {
      /**
       * FAILED(7),
       * CANCELED(9),
       * FINISHED(10),
       * SUSPENDED(11),
       * LOST(13),
       * OTHER(15),
       * REVOKED(16),
       * TERMINATED(18),
       * POS_TERMINATED(19),
       * SUCCEEDED(20),
       * KILLED(-9)
       * @type {boolean}
       */
      const status = app.state === 0 ||
        app.state === 7 ||
        app.state === 9 ||
        app.state === 10 ||
        app.state === 11 ||
        app.state === 13 ||
        app.state === 16 ||
        app.state === 18 ||
        app.state === 19 ||
        app.state === 20 ||
        app.state === -9 || false

      /**
       *
       * // 部署失败
       * FAILED(-1),
       * // 完结
       * DONE(0),
       * // 任务修改完毕需要重新发布
       * NEED_LAUNCH(1),
       * // 上线中
       * LAUNCHING(2),
       * // 上线完毕,需要重启
       * NEED_RESTART(3),
       * //需要回滚
       * NEED_ROLLBACK(4),
       * // 项目发生变化,任务需检查(是否需要重新选择jar)
       * NEED_CHECK(5),
       * // 发布的任务已经撤销
       * REVOKED(10);
       */

      const launch = app.launch === 0 || app.launch === 3

      const optionState = !this.optionApps.starting.get(app.id) || app['optionState'] === 0 || false

      return status && launch && optionState
    },

    handleCanRemapping(record) {
      return record.state === 7 &&
        record.state === 0 &&
        record.state === 10 &&
        record.state === 11 &&
        record.state === 13
    },

    showForceStartAppModal() {
      this.forceStartAppModalVisual = true
    },

    closeForceStartAppModal() {
      this.forceStartAppModalVisual = false
    },

    handleAppCheckStart(app) {
      // when then app is building, show forced starting modal
      if (app['appControl']['allowStart'] === false) {
        this.application = app
        this.handleFetchBuildDetail(app)
        this.showForceStartAppModal()
      } else {
        this.handleStart(app)
      }
    },

    handleStart(app) {
      this.closeForceStartAppModal()
      if (app.flinkVersion == null) {
        this.$swal.fire(
          'Failed',
          'please set flink version first.',
          'error'
        )
      } else {
        if (!this.optionApps.starting.get(app.id) || app['optionState'] === 0) {
          this.application = app
          latest({
            appId: this.application.id
          }).then((resp) => {
            this.latestSavePoint = resp.data || null
            this.startVisible = true
            this.executionMode = app.executionMode
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
      }
    },

    handleStartCancel() {
      this.startVisible = false
      setTimeout(() => {
        this.allowNonRestoredState = false
        this.formStartCheckPoint.resetFields()
        this.application = null
        this.startSavePointed = true
        this.flameGraph = false
      }, 1000)
    },

    handleStartOk() {
      this.formStartCheckPoint.validateFields((err, values) => {
        if (!err) {
          const id = this.application.id
          const savePointed = this.startSavePointed
          const flameGraph = this.flameGraph
          const savePointPath = savePointed ? (values['startSavePoint'] || this.latestSavePoint.savePoint) : null
          const allowNonRestoredState = this.allowNonRestoredState
          this.optionApps.starting.set(id, new Date().getTime())
          this.handleMapUpdate('starting')
          this.handleStartCancel()

          this.$swal.fire({
            icon: 'success',
            title: 'The current job is starting',
            showConfirmButton: false,
            timer: 2000
          }).then((r) => {
            start({
              id: id,
              savePointed: savePointed,
              savePoint: savePointPath,
              flameGraph: flameGraph,
              allowNonRestored: allowNonRestoredState
            }).then((resp) => {
              if (!resp.data) {
                this.$swal.fire({
                  title: 'Failed',
                  icon: 'error',
                  width: this.exceptionPropWidth(),
                  html: '<pre class="propException"> startup failed, ' + resp.message.replaceAll(/\[StreamX]/g, '') + '</pre>',
                  showCancelButton: true,
                  confirmButtonColor: '#55BDDDFF',
                  confirmButtonText: 'Detail',
                  cancelButtonText: 'Close'
                }).then((isConfirm) => {
                  if (isConfirm.value) {
                    this.SetAppId(id)
                    this.$router.push({'path': '/flink/app/detail'})
                  }
                })
              }
            })
          })
        }
      })
    },

    handleCancel(app) {
      if (!this.optionApps.stopping.get(app.id) || app['optionState'] === 0) {
        this.stopVisible = true
        this.application = app
      }
    },

    handleStopCancel() {
      this.stopVisible = false
      setTimeout(() => {
        this.formStopSavePoint.resetFields()
        this.drain = false
        this.stopSavePointed = true
        this.application = null
      }, 1000)
    },

    handleStopOk() {
      const customSavePoint = this.customSavepoint
      const id = this.application.id
      const savePointed = this.stopSavePointed
      const drain = this.drain
      this.optionApps.stopping.set(id, new Date().getTime())
      this.handleMapUpdate('stopping')
      this.handleStopCancel()

      const stopReq = {
        id: id,
        savePointed: savePointed,
        drain: drain,
        savePoint: customSavePoint
      }

      if (savePointed) {
        if (customSavePoint != null) {
          verifySchema({
            path: customSavePoint
          }).then(resp => {
            if (resp.data === false) {
              this.$swal.fire(
                'Failed',
                'custom savePoint path is invalid, ' + resp.message,
                'error'
              )
            } else {
              this.handleStopAction(stopReq)
            }
          })
        } else {
          checkSavepointPath({
            id: id
          }).then((resp) => {
            if (resp.data === true) {
              this.handleStopAction(stopReq)
            } else {
              this.$swal.fire(
                'Failed',
                resp.message,
                'error'
              )
            }
          })
        }
      } else {
        this.handleStopAction(stopReq)
      }
    },

    handleStopAction(stopReq) {
      this.$swal.fire({
        icon: 'success',
        title: 'The current job is canceling',
        showConfirmButton: false,
        timer: 2000
      }).then((result) => {
        cancel(stopReq).then((resp) => {
        })
      })
    },

    handleDetail(app) {
      this.SetAppId(app.id)
      this.$router.push({'path': '/flink/app/detail'})
    },

    handleCheckFlameGraph() {
      if (this.flameGraph) {
        weburl({}).then((resp) => {
          if (resp.data == null || resp.data === '') {
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

    handleCanStop(app) {
      const optionTime = new Date(app['optionTime']).getTime()
      const nowTime = new Date().getTime()
      if (nowTime - optionTime >= 60 * 1000) {
        const state = app['optionState']
        if (state === 0) {
          return app.state === 3 || app.state === 4 || app.state === 8 || false
        }
        return true
      }
      return false
    },

    handleForcedStop(app) {
      let option = 'starting'
      const optionState = app['optionState']
      if (optionState === 0) {
        switch (app.state) {
          case 3:
            option = 'starting'
            break
          case 4:
            option = 'restarting'
            break
          case 8:
            option = 'cancelling'
            break
        }
      } else {
        switch (optionState) {
          case 1:
            option = 'launching'
            break
          case 2:
            option = 'cancelling'
            break
          case 3:
            option = 'starting'
            break
          case 4:
            option = 'savePointing'
            break
        }
      }

      this.$swal.fire({
        title: 'Are you sure?',
        text: `current job is ${option}, are you sure forced stop?`,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonText: 'Yes, forced stop!',
        denyButtonText: `No, cancel`,
        confirmButtonColor: '#d33',
        cancelButtonColor: '#3085d6',
      }).then((result) => {
        if (result.isConfirmed) {
          this.$swal.fire('forced stopping', '', 'success')
          forcedStop({
            id: app.id
          }).then((resp) => {
          })
        }
      })
    },

    handleCanDelete(app) {
      return app.state === 0 ||
        app.state === 7 ||
        app.state === 9 ||
        app.state === 10 ||
        app.state === 13 ||
        app.state === 18 ||
        app.state === 19 || false
    },

    handleDelete(app) {
      remove({
        id: app.id
      }).then((resp) => {
        this.$swal.fire({
          icon: 'success',
          title: 'delete successful',
          showConfirmButton: false,
          timer: 2000
        }).then((result) => {

        })
      })
    },

    handleSearch() {
      const {sortedInfo} = this
      // 获取当前列的排序和列的过滤规则
      if (sortedInfo) {
        this.queryParams['sortField'] = sortedInfo.field
        this.queryParams['sortOrder'] = sortedInfo.order
      }
      this.queryParams['jobName'] = this.searchText
      this.queryParams['jobType'] = this.jobType
      this.queryParams['teamId'] = this.teamId
      this.queryParams['userId'] = this.userId
      this.handleFetch(false)
    },

    handleTableChange(pagination, filters, sorter) {
      this.sortedInfo = sorter
      this.paginationInfo = pagination
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
            if (this.optionApps.starting.get(x.id)) {
              if (timestamp - this.optionApps.starting.get(x.id) > this.queryInterval * 2) {
                this.optionApps.starting.delete(x.id)
                this.handleMapUpdate('starting')
              }
            }
            if (this.optionApps.stopping.get(x.id)) {
              if (timestamp - this.optionApps.stopping.get(x.id) > this.queryInterval) {
                this.optionApps.stopping.delete(x.id)
                this.handleMapUpdate('stopping')
              }
            }
            if (this.optionApps.launch.get(x.id)) {
              if (timestamp - this.optionApps.launch.get(x.id) > this.queryInterval) {
                this.optionApps.launch.delete(x.id)
                this.handleMapUpdate('launch')
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

    handleView(app) {
      // 任务正在运行中, 重启中, 正在 savePoint 中
      if (app.state === 4 || app.state === 5 || app['optionState'] === 4) {
        // yarn-per-job|yarn-session|yarn-application
        const executionMode = app['executionMode']
        if (executionMode === 1) {
          activeURL({id: app.flinkClusterId}).then((resp) => {
            const url = resp.data + '/#/job/' + app.jobId + '/overview'
            window.open(url)
          })
        } else if (executionMode === 2 || executionMode === 3 || executionMode === 4) {
          if (this.yarn == null) {
            yarn({}).then((resp) => {
              this.yarn = resp.data
              const url = this.yarn + '/proxy/' + app['appId'] + '/'
              window.open(url)
            })
          } else {
            const url = this.yarn + '/proxy/' + app['appId'] + '/'
            window.open(url)
          }
        } else {
          if (app.flinkRestUrl != null) {
            window.open(app.flinkRestUrl)
          }
        }
      }
    },

    handleAdd() {
      this.$router.push({'path': '/flink/app/add'})
    },

    handleEdit(app) {
      this.SetAppId(app.id)
      if (app.appType === 1) {
        // jobType( 1 custom code 2: flinkSQL)
        this.$router.push({'path': '/flink/app/edit_streamx'})
      } else if (app.appType === 2) { //Apache Flink
        this.$router.push({'path': '/flink/app/edit_flink'})
      }
    },

    handleRevoke(app) {
      revoke({
        id: app.id
      }).then((resp) => {

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

      const url = baseUrl().concat('/websocket/' + this.handleGetSocketId())
      const socket = this.getSocket(url)

      socket.onopen = () => {
        downLog({id: app.id})
      }

      socket.onmessage = (event) => {
        if (event.data.startsWith('[Exception]')) {
          this.$swal.fire({
            title: 'Failed',
            icon: 'error',
            width: this.exceptionPropWidth(),
            html: '<pre class="propException">' + event.data + '</pre>',
            focusConfirm: false,
          })
        } else {
          this.terminal.writeln(event.data)
        }
      }

      socket.onclose = () => {
        this.socketId = null
        storage.rm(this.storageKey)
      }

    },

    handleCloseWS() {
      this.stompClient.disconnect()
      this.controller.visible = false
      this.terminal.clear()
      this.terminal.clearSelection()
      this.terminal = null
    },

    handleGetSocketId() {
      if (this.socketId == null) {
        return storage.get(this.storageKey) || null
      }
      return this.socketId
    },

  }
}
</script>

<style lang="less">
@import "View";
</style>
