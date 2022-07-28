<template>
  <div>
    <a-tabs type="card" class="setting" v-model="activeKey">
      <a-tab-pane key="system" tab="System Setting">
        <a-card
          :bordered="false"
          class="system_setting">
          <a-collapse class="collapse" v-model="collapseActive">
            <a-collapse-panel key="1" header="Maven Setting.">
              <a-list>
                <a-list-item v-for="(item,index) in settings" :key="index" v-if="item.key.indexOf('streamx.maven') > -1">
                  <a-list-item-meta style="width: 50%">
                    <svg-icon
                      class="avatar"
                      name="maven"
                      size="large"
                      slot="avatar"
                      v-if="item.key === 'streamx.maven.central.repository'"></svg-icon>
                    <svg-icon
                      class="avatar"
                      name="user"
                      size="large"
                      slot="avatar"
                      v-if="item.key === 'streamx.maven.auth.user'"></svg-icon>
                    <svg-icon
                      class="avatar"
                      name="mvnpass"
                      size="large"
                      slot="avatar"
                      v-if="item.key === 'streamx.maven.auth.password'"></svg-icon>
                    <span slot="title">{{ item.title }}</span>
                    <span slot="description">{{ item.description }}</span>
                  </a-list-item-meta>
                  <div class="list-content" style="width: 50%">
                    <div class="list-content-item" style="width: 100%">
                      <template v-if="item.type === 1">
                        <input
                          :type="item.key === 'streamx.maven.auth.password' ? 'password': 'text'"
                          v-if="item.editable"
                          :value="item.value"
                          :class="item.key.replace(/\./g,'_')"
                          class="ant-input"/>
                        <div v-else style="width: 100%;text-align: right">
                          <span v-if="item.key === 'streamx.maven.auth.password' && item.value !== null"> ******** </span>
                          <span v-else>{{ item.value }}</span>
                        </div>
                      </template>
                      <template v-else>
                        <a-switch
                          checked-children="ON"
                          un-checked-children="OFF"
                          style="float: right;margin-right: 30px"
                          :default-checked="item.value === 'true'"
                          @change="handleSwitch(item)"/>
                      </template>
                    </div>
                  </div>
                  <div slot="actions" v-if="item.type === 1">
                    <a v-if="!item.submitting" @click="handleEdit(item)">Edit</a>
                    <a v-else @click="handleSubmit(item)">Submit</a>
                  </div>
                </a-list-item>
              </a-list>
            </a-collapse-panel>
            <a-collapse-panel key="2" header="Docker Setting.">
              <a-list>
                <a-list-item v-for="(item,index) in settings" :key="index" v-if="item.key.indexOf('docker.register') > -1">
                  <a-list-item-meta style="width: 50%">
                    <svg-icon
                      class="avatar"
                      name="docker"
                      size="large"
                      slot="avatar"
                      v-if="item.key === 'docker.register.address'"></svg-icon>
                    <svg-icon
                      class="avatar"
                      name="namespace"
                      size="large"
                      slot="avatar"
                      v-if="item.key === 'docker.register.namespace'"></svg-icon>
                    <svg-icon
                      class="avatar"
                      name="auth"
                      size="large"
                      slot="avatar"
                      v-if="item.key === 'docker.register.user'"></svg-icon>
                    <svg-icon
                      class="avatar"
                      name="password"
                      size="large"
                      slot="avatar"
                      v-if="item.key === 'docker.register.password'"></svg-icon>
                    <span slot="title">{{ item.title }}</span>
                    <span slot="description">{{ item.description }}</span>
                  </a-list-item-meta>
                  <div class="list-content" style="width: 50%">
                    <div class="list-content-item" style="width: 100%">
                      <template v-if="item.type === 1">
                        <input
                          :type="item.key === 'docker.register.password' ? 'password': 'text'"
                          v-if="item.editable"
                          :value="item.value"
                          :class="item.key.replace(/\./g,'_')"
                          class="ant-input"/>
                        <div v-else style="width: 100%;text-align: right">
                          <span v-if="item.key === 'docker.register.password' && item.value !== null"> ******** </span>
                          <span v-else>{{ item.value }}</span>
                        </div>
                      </template>
                      <template v-else>
                        <a-switch
                          checked-children="ON"
                          un-checked-children="OFF"
                          style="float: right;margin-right: 30px"
                          :default-checked="item.value === 'true'"
                          @change="handleSwitch(item)"/>
                      </template>
                    </div>
                  </div>
                  <div slot="actions" v-if="item.type === 1">
                    <a v-if="!item.submitting" @click="handleEdit(item)">Edit</a>
                    <a v-else @click="handleSubmit(item)">Submit</a>
                  </div>
                </a-list-item>
              </a-list>
            </a-collapse-panel>
            <a-collapse-panel key="3" header="Sender Email Setting.">
              <a-list>
                <a-list-item v-for="(item,index) in settings" :key="index" v-if="item.key.indexOf('alert.email') > -1">
                  <a-list-item-meta style="width: 50%">
                    <svg-icon
                      class="avatar"
                      name="host"
                      size="large"
                      slot="avatar"
                      v-if="item.key === 'alert.email.host'"></svg-icon>
                    <svg-icon
                      class="avatar"
                      name="port"
                      size="large"
                      slot="avatar"
                      v-if="item.key === 'alert.email.port'"></svg-icon>
                    <svg-icon
                      class="avatar"
                      name="mail"
                      size="large"
                      slot="avatar"
                      v-if="item.key === 'alert.email.from'"></svg-icon>
                    <svg-icon
                      class="avatar"
                      name="user"
                      size="large"
                      slot="avatar"
                      v-if="item.key === 'alert.email.userName'"></svg-icon>
                    <svg-icon
                      class="avatar"
                      name="keys"
                      size="large"
                      slot="avatar"
                      v-if="item.key === 'alert.email.password'"></svg-icon>
                    <svg-icon
                      class="avatar"
                      name="ssl"
                      size="large"
                      slot="avatar"
                      v-if="item.key === 'alert.email.ssl'"></svg-icon>
                    <span slot="title">{{ item.title }}</span>
                    <span slot="description">{{ item.description }}</span>
                  </a-list-item-meta>
                  <div class="list-content" style="width: 50%">
                    <div class="list-content-item" style="width: 100%">
                      <template v-if="item.type === 1">
                        <input
                          :type="item.key === 'alert.email.password' ? 'password': 'text'"
                          v-if="item.editable"
                          :value="item.value"
                          :class="item.key.replace(/\./g,'_')"
                          class="ant-input"/>
                        <div v-else style="width: 100%;text-align: right">
                          <span v-if="item.key === 'alert.email.password' && item.value !== null"> ******** </span>
                          <span v-else>{{ item.value }}</span>
                        </div>
                      </template>
                      <template v-else>
                        <a-switch
                          checked-children="ON"
                          un-checked-children="OFF"
                          style="float: right;margin-right: 30px"
                          :default-checked="item.value === 'true'"
                          @change="handleSwitch(item)"/>
                      </template>
                    </div>
                  </div>
                  <div slot="actions" v-if="item.type === 1">
                    <a v-if="!item.submitting" @click="handleEdit(item)">Edit</a>
                    <a v-else @click="handleSubmit(item)">Submit</a>
                  </div>
                </a-list-item>
              </a-list>
            </a-collapse-panel>
            <a-collapse-panel key="4" header="Console Setting.">
              <a-list>
                <a-list-item v-for="(item,index) in settings" :key="index" v-if="item.key.indexOf('streamx.console') > -1">
                  <a-list-item-meta style="width: 50%">
                    <svg-icon
                      class="avatar"
                      name="http"
                      size="large"
                      slot="avatar"
                      v-if="item.key === 'streamx.console.webapp.address'"></svg-icon>
                    <span slot="title">{{ item.title }}</span>
                    <span slot="description">{{ item.description }}</span>
                  </a-list-item-meta>
                  <div class="list-content" style="width: 50%">
                    <div class="list-content-item" style="width: 100%">
                      <template v-if="item.type === 1">
                        <input
                          type="text"
                          v-if="item.editable"
                          :value="item.value"
                          :class="item.key.replace(/\./g,'_')"
                          class="ant-input"/>
                        <div v-else style="width: 100%;text-align: right">
                          <span>{{ item.value }}</span>
                        </div>
                      </template>
                      <template v-else>
                        <a-switch
                          checked-children="ON"
                          un-checked-children="OFF"
                          style="float: right;margin-right: 30px"
                          :default-checked="item.value === 'true'"
                          @change="handleSwitch(item)"/>
                      </template>
                    </div>
                  </div>
                  <div slot="actions" v-if="item.type === 1">
                    <a v-if="!item.submitting" @click="handleEdit(item)">Edit</a>
                    <a v-else @click="handleSubmit(item)">Submit</a>
                  </div>
                </a-list-item>
              </a-list>
            </a-collapse-panel>
          </a-collapse>
        </a-card>
      </a-tab-pane>
      <a-tab-pane key="alert" tab="Alert Setting">
        <a-card
          :bordered="false"
          class="system_setting">
          <div
            v-permit="'project:create'">
            <a-button
              type="dashed"
              style="width: 100%;margin-top: 20px"
              icon="plus"
              @click="handleAlertFormVisible(true)">
              Add New
            </a-button>
          </div>

          <a-list>
            <a-list-item v-for="(item,index) in alerts" :key="index">
              <a-list-item-meta style="width: 40%">
                <svg-icon class="avatar" name="flink" size="large" slot="avatar"></svg-icon>
                <span slot="title">{{ item.alertName }}</span>
              </a-list-item-meta>

              <div class="list-content" style="width: 40%">
                <span slot="title" text-align center>Alert Type</span><br><br>
                <svg-icon role="img" name="mail" size="middle" v-if="computeAlertType(item.alertType).indexOf(1) > -1 "/>
                <svg-icon role="img" name="dingtalk" size="middle" v-if="computeAlertType(item.alertType).indexOf(2) > -1 "/>
                <svg-icon role="img" name="wecom" size="middle" v-if="computeAlertType(item.alertType).indexOf(4) > -1 "/>
                <svg-icon role="img" name="message" size="middle" v-if="computeAlertType(item.alertType).indexOf(8) > -1 "/>
                <svg-icon role="img" name="lark" size="middle" v-if="computeAlertType(item.alertType).indexOf(16) > -1 "/>
              </div>

              <div slot="actions">
                <a-tooltip title="Alert Test">
                  <a-button
                    @click.native="handleTestAlarm(item)"
                    shape="circle"
                    size="large"
                    style="margin-left: 3px"
                    class="control-button ctl-btn-color">
                    <a-icon type="thunderbolt" />
                  </a-button>
                </a-tooltip>
                <a-tooltip title="Edit Alert Config">
                  <a-button
                    @click.native="handleEditAlertConf(item)"
                    shape="circle"
                    size="large"
                    style="margin-left: 3px"
                    class="control-button ctl-btn-color">
                    <a-icon type="edit"/>
                  </a-button>
                </a-tooltip>
                <template>
                  <a-popconfirm
                    title="Are you sure delete this alert conf ?"
                    cancel-text="No"
                    ok-text="Yes"
                    @confirm="handleDeleteAlertConf(item)">
                    <a-button
                      type="danger"
                      shape="circle"
                      size="large"
                      style="margin-left: 3px"
                      class="control-button">
                      <a-icon type="delete"/>
                    </a-button>
                  </a-popconfirm>
                </template>
              </div>
            </a-list-item>
          </a-list>

        </a-card>
      </a-tab-pane>
      <a-tab-pane key="flink" tab="Flink Home">
        <a-card
          :bordered="false"
          class="system_setting">
          <div
            v-permit="'project:create'">
            <a-button
              type="dashed"
              style="width: 100%;margin-top: 20px"
              icon="plus"
              @click="handleFlinkFormVisible(true)">
              Add New
            </a-button>
          </div>
          <a-list>
            <a-list-item v-for="(item,index) in flinks" :key="index">
              <a-list-item-meta style="width: 60%">
                <svg-icon class="avatar" name="flink" size="large" slot="avatar"></svg-icon>
                <span slot="title">{{ item.flinkName }}</span>
                <span slot="description">{{ item.description }}</span>
              </a-list-item-meta>

              <div class="list-content" style="width: 40%">
                <div class="list-content-item" style="width: 60%">
                  <span>Flink Home</span>
                  <p style="margin-top: 10px">
                    {{ item.flinkHome }}
                  </p>
                </div>
                <div
                  class="list-content-item"
                  style="width: 30%">
                  <span>Default</span>
                  <p style="margin-top: 10px">
                    <a-switch :disabled="item.isDefault" @click="handleSetDefault(item)" v-model="item.isDefault">
                      <a-icon slot="checkedChildren" type="check"/>
                      <a-icon slot="unCheckedChildren" type="close"/>
                    </a-switch>
                  </p>
                </div>
              </div>

              <div slot="actions">
                <a @click="handleEditFlink(item)">Edit</a>
                <a-divider type="vertical"/>
                <a @click="handleFlinkConf(item)">Flink Conf</a>
              </div>

            </a-list-item>
          </a-list>
        </a-card>
      </a-tab-pane>
      <a-tab-pane key="cluster" tab="Flink Cluster">
        <a-card
          :bordered="false"
          class="system_setting">
          <div
            v-permit="'project:create'">
            <a-button
              type="dashed"
              style="width: 100%;margin-top: 20px"
              icon="plus"
              @click="handleAdd">
              Add New
            </a-button>
          </div>
          <a-list>
            <a-list-item v-for="(item,index) in clusters" :key="index">
              <a-list-item-meta style="width: 60%">
                <svg-icon class="avatar" name="flink" size="large" slot="avatar"></svg-icon>
                <span slot="title">{{ item.clusterName }}</span>
                <span slot="description">{{ item.description }}</span>
              </a-list-item-meta>
              <div class="list-content" style="width: 10%">
                <div class="list-content-item" style="width: 60%">
                  <span>ExecutionMode</span>
                  <p style="margin-top: 10px">
                    {{ item.executionModeEnum.toLowerCase() }}
                  </p>
                </div>
              </div>
              <div class="list-content" style="width: 15%">
                <div class="list-content-item" style="width: 80%">
                  <span>ClusterId</span>
                  <p style="margin-top: 10px">
                    {{ item.clusterId }}
                  </p>
                </div>
              </div>
              <div class="list-content" style="width: 20%">
                <div class="list-content-item" style="width: 60%">
                  <span>Address</span>
                  <p style="margin-top: 10px">
                    {{ item.address }}
                  </p>
                </div>
              </div>
              <div slot="actions">
                <a-tooltip title="Edit Cluster">
                  <a-button
                    v-if="handleIsStart(item)"
                    v-permit="'app:update'"
                    :disabled="true"
                    @click.native="handleEditCluster(item)"
                    shape="circle"
                    size="large"
                    style="margin-left: 3px"
                    class="control-button ctl-btn-color">
                    <a-icon type="edit"/>
                  </a-button>
                  <a-button
                    v-else
                    v-permit="'app:update'"
                    @click.native="handleEditCluster(item)"
                    shape="circle"
                    size="large"
                    style="margin-left: 3px"
                    class="control-button ctl-btn-color">
                    <a-icon type="edit"/>
                  </a-button>
                </a-tooltip>

                <a-tooltip title="Start Cluster">
                  <a-button
                    v-if="item.executionMode === 3 || item.executionMode === 5"
                    v-show="!handleIsStart(item)"
                    v-permit="'cluster:create'"
                    @click.native="handleDeployCluser(item)"
                    shape="circle"
                    size="large"
                    style="margin-left: 3px"
                    class="control-button ctl-btn-color">
                    <a-icon type="play-circle"/>
                  </a-button>
                  <a-button
                    v-else
                    :disabled="true"
                    v-show="!handleIsStart(item)"
                    v-permit="'cluster:create'"
                    shape="circle"
                    size="large"
                    style="margin-left: 3px"
                    class="control-button ctl-btn-color">
                    <a-icon type="play-circle"/>
                  </a-button>
                </a-tooltip>

                <a-tooltip title="Stop Cluster">
                  <a-button
                    v-if="item.executionMode === 3 || item.executionMode === 5"
                    v-show="handleIsStart(item)"
                    v-permit="'cluster:create'"
                    @click.native="handleShutdownCluster(item)"
                    shape="circle"
                    size="large"
                    style="margin-left: 3px"
                    class="control-button ctl-btn-color">
                    <a-icon type="pause-circle"/>
                  </a-button>
                  <a-button
                    v-else
                    :disabled="true"
                    v-show="handleIsStart(item)"
                    v-permit="'cluster:create'"
                    shape="circle"
                    size="large"
                    style="margin-left: 3px"
                    class="control-button ctl-btn-color">
                    <a-icon type="pause-circle"/>
                  </a-button>
                </a-tooltip>
                <a-tooltip title="View Cluster Detail">
                  <a-button
                    v-if="!handleIsStart(item)"
                    v-permit="'app:detail'"
                    :disabled="true"
                    shape="circle"
                    size="large"
                    style="margin-left: 3px"
                    class="control-button ctl-btn-color">
                    <a-icon type="eye"/>
                  </a-button>
                  <a-button
                    v-if="handleIsStart(item)"
                    v-permit="'app:detail'"
                    shape="circle"
                    size="large"
                    style="margin-left: 3px"
                    class="control-button ctl-btn-color"
                    :href="item.address"
                    target="_blank">
                    <a-icon type="eye"/>
                  </a-button>
                </a-tooltip>
                <template>
                  <a-popconfirm
                    title="Are you sure delete this cluster ?"
                    cancel-text="No"
                    ok-text="Yes"
                    @confirm="handleDelete(item)">
                    <a-button
                      type="danger"
                      shape="circle"
                      size="large"
                      style="margin-left: 3px"
                      class="control-button">
                      <a-icon type="delete"/>
                    </a-button>
                  </a-popconfirm>
                </template>
              </div>
            </a-list-item>
          </a-list>
        </a-card>
      </a-tab-pane>
    </a-tabs>

    <a-drawer
      :mask-closable="false"
      width="calc(100% - 40%)"
      placement="right"
      :visible="flinkConfVisible"
      :centered="true"
      :keyboard="false"
      :body-style="{ paddingBottom: '80px' }"
      title="Flink Conf"
      @close="handleCloseConf()">
      <a-col style="font-size: 0.9rem">
        <div style="padding-bottom: 15px">
          Flink Home: &nbsp;&nbsp; {{ flinkHome }}
        </div>
        <div>
          Flink Conf:
          <div style="padding: 15px 0">
            <div id="conf"></div>
            <a-button
              type="primary"
              style="float:right;margin-top: 10px;margin-right: 130px"
              @click.native="handleSync">
              <a-icon type="sync"/>
              Sync Conf
            </a-button>
          </div>
        </div>
      </a-col>
    </a-drawer>

    <a-modal
      v-model="flinkFormVisible">
      <template
        slot="title">
        <svg-icon
          slot="icon"
          name="flink"/>
        Add Flink
      </template>

      <a-form
        :form="flinkForm">
        <a-form-item
          label="Flink Name"
          style="margin-bottom: 10px"
          :label-col="{lg: {span: 7}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 4} }">
          <a-input
            type="text"
            placeholder="Please enter flink name"
            v-decorator="['flinkName',{ rules: [{ required: true } ]}]"/>
          <span
            class="conf-switch"
            style="color:darkgrey">the flink name, e.g: flink-1.12 </span>
        </a-form-item>

        <a-form-item
          label="Flink Home"
          :label-col="{lg: {span: 7}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 4} }">
          <a-input
            type="text"
            placeholder="Please enter flink home"
            v-decorator="['flinkHome',{ rules: [{ required: true } ]}]"/>
          <span
            class="conf-switch"
            style="color:darkgrey">The absolute path of the FLINK_HOME</span>
        </a-form-item>

        <a-form-item
          label="Description"
          :label-col="{lg: {span: 7}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 4} }">
          <a-textarea
            rows="4"
            name="description"
            placeholder="Please enter description"
            v-decorator="['description']"/>
        </a-form-item>

      </a-form>

      <template slot="footer">
        <a-button
          key="back"
          @click="handleFlinkFormVisible(false)">
          Cancel
        </a-button>
        <a-button
          key="submit"
          @click="handleSubmitFlink"
          type="primary">
          Submit
        </a-button>
      </template>
    </a-modal>


    <a-modal
      v-model="alertFormVisible"
      width="850px"
      class="full-modal">
      <template
        slot="title">
        <svg-icon
          slot="icon"
          size="middle"
          name="alarm"/>
        Alert Setting
      </template>

      <a-form
        :form="alertForm">
        <a-form-item
          label="Alert Name"
          style="margin-bottom: 10px"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 4} }">
          <a-input
            type="text"
            allowClear
            placeholder="Please enter alert name"
            v-decorator="['alertName',{ rules: [{validator: handleCheckAlertName,required: true , message: 'Alert Name is required'}]} ]"/>
          <span
            class="conf-switch"
            style="color:darkgrey">the alert name, e.g: streamx team alert </span>
        </a-form-item>

        <a-form-item
          label="Fault Alert Type"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 4} }">
          <a-select
            placeholder="Alert Type"
            mode="multiple"
            @change="handleChangeAlertType"
            v-decorator="[ 'alertType', {rules: [{ required: true, message: 'Alert Type is required' }] }]">
            <a-select-option
              v-for="(o,index) in alertTypes"
              :key="`alertType_${index}`"
              :disabled="o.disabled"
              :value="o.value">
              <svg-icon role="img" v-if="o.value === 1" name="mail"/>
              <svg-icon role="img" v-if="o.value === 2" name="dingtalk"/>
              <svg-icon role="img" v-if="o.value === 4" name="wecom"/>
              <svg-icon role="img" v-if="o.value === 8" name="message"/>
              <svg-icon role="img" v-if="o.value === 16" name="lark"/>
              {{ o.name }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-divider v-if="alertType.indexOf(1)>-1"><svg-icon role="img" name="mail" size="middle"/>  E-mail </a-divider>
        <a-form-item
          v-if="alertType.indexOf(1)>-1"
          label="Alert Email"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 4} }">
          <a-input
            type="text"
            placeholder="Please enter email,separate multiple emails with comma(,)"
            allowClear
            v-decorator="[ 'alertEmail', {rules: [{ required: true, message: 'email address is required' }]} ]">
          </a-input>
        </a-form-item>

        <a-divider v-if="alertType.indexOf(2)>-1"><svg-icon role="img" name="dingtalk" size="middle"/> Ding Talk </a-divider>

        <a-form-item
          v-if="alertType.indexOf(2)>-1"
          label="DingTalk Url"
          defaultValue="https://oapi.dingtalk.com/robot/send"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 4} }">
          <a-input
            type="text"
            placeholder="Please enter DingTask Url"
            allowClear
            v-decorator="[ 'alertDingURL', {rules: [{ required: false, message: 'DingTalk Url is required' }]} ]"/>
        </a-form-item>

        <a-form-item
          v-if="alertType.indexOf(2)>-1"
          label="Access Token"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 4} }">
          <a-input
            type="text"
            placeholder="Please enter the access token of DingTalk"
            allowClear
            v-decorator="[ 'dingtalkToken', {rules: [{ required: true, message: 'Access token is required' }]} ]"/>
        </a-form-item>

        <a-form-item
          label="Secret Enable"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 17} }"
          v-show="alertType.indexOf(2)>-1">
          <a-tooltip title="DingTalk ecretToken is enable">
            <a-switch
              checked-children="ON"
              un-checked-children="OFF"
              :checked="dingtalkSecretEnable"
              allowClear
              @change="handleSetDingtalkSecretEnable"
              v-decorator="[ 'dingtalkSecretEnable' ]" />
          </a-tooltip>
        </a-form-item>

        <a-form-item
          v-if="alertType.indexOf(2)>-1"
          label="Secret Token"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 4} }">
          <a-input
            :disabled="!dingtalkSecretEnable"
            type="text"
            placeholder="Please enter DingTalk SecretToken"
            allowClear
            v-decorator="[ 'dingtalkSecretToken', {rules: [{ required: dingtalkSecretEnable, message: 'DingTalk SecretToken is required' }]} ]"/>
        </a-form-item>

        <a-form-item
          v-if="alertType.indexOf(2)>-1"
          label="DingTalk User"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 4} }">
          <a-input
            type="text"
            placeholder="Please enter DingTalk receive user"
            allowClear
            v-decorator="[ 'alertDingUser', {rules: [{ required: false, message: 'DingTalk receive user is required' }]} ]"/>
        </a-form-item>

        <a-form-item
          label="At All User"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 17} }"
          v-show="alertType.indexOf(2)>-1">
          <a-tooltip title="Whether Notify All">
            <a-switch
              checked-children="ON"
              un-checked-children="OFF"
              allowClear
              :checked="dingtalkIsAtAll"
              @change="handleDingtalkIsAtAll"
              v-decorator="[ 'dingtalkIsAtAll' ]"/>
          </a-tooltip>
        </a-form-item>

        <a-divider v-if="alertType.indexOf(4)>-1"><svg-icon role="img" name="wecom" size="middle"/> WeChat </a-divider>

        <a-form-item
          v-if="alertType.indexOf(4)>-1"
          label="WeChat token"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 4} }">
          <a-textarea
            rows="4"
            placeholder="Please enter WeChart Token"
            v-decorator="['weToken', {rules: [{ required: true, message: 'WeChat Token is required' }]} ]"/>
        </a-form-item>

        <a-divider v-if="alertType.indexOf(8)>-1"><svg-icon role="img" name="message" size="middle"/> SMS </a-divider>

        <a-form-item
          v-if="alertType.indexOf(8)>-1"
          label="SMS"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 4} }">
          <a-input
            type="text"
            placeholder="Please enter mobile number"
            allowClear
            v-decorator="[ 'alertSms', {rules: [{ required: true, message: 'mobile number is required' }]} ]"/>
        </a-form-item>

        <a-form-item
          v-if="alertType.indexOf(8)>-1"
          label="SMS Template"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 4} }">
          <a-textarea
            rows="4"
            placeholder="Please enter sms template"
            v-decorator="['alertSmsTemplate', {rules: [{ required: true, message: 'SMS Template is required' }]} ]"/>
        </a-form-item>

        <a-divider v-if="alertType.indexOf(16)>-1"><svg-icon role="img" name="lark" size="middle"/> Lark </a-divider>

        <a-form-item
          v-if="alertType.indexOf(16)>-1"
          label="Lark Token"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 4} }">
          <a-input
            type="text"
            placeholder="Please enter the access token of LarkTalk"
            allowClear
            v-decorator="[ 'larkToken', {rules: [{ required: true, message: 'Lark token is required' }]} ]"/>
        </a-form-item>

        <a-form-item
          label="At All User"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 17} }"
          v-show="alertType.indexOf(16)>-1">
          <a-tooltip title="Whether Notify All">
            <a-switch
              checked-children="ON"
              un-checked-children="OFF"
              allowClear
              :checked="larkIsAtAll"
              @change="handleLarkIsAtAll"
              v-decorator="[ 'larkIsAtAll' ]"/>
          </a-tooltip>
        </a-form-item>

        <a-form-item
          label="Secret Enable"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 17} }"
          v-show="alertType.indexOf(16)>-1">
          <a-tooltip title="Lark secretToken is enable">
            <a-switch
              checked-children="ON"
              un-checked-children="OFF"
              :checked="larkSecretEnable"
              allowClear
              @change="handleSetLarkSecretEnable"
              v-decorator="[ 'larkSecretEnable' ]" />
          </a-tooltip>
        </a-form-item>

        <a-form-item
          v-if="alertType.indexOf(16)>-1 && larkSecretEnable === true"
          label="Lark Secret Token"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 4} }">
          <a-input
            type="text"
            placeholder="Please enter Lark SecretToken"
            allowClear
            v-decorator="[ 'larkSecretToken', {rules: [{ required: true, message: 'Lark SecretToken is required' }]} ]"/>
        </a-form-item>

      </a-form>

      <template slot="footer">
        <a-button
          key="back"
          @click="handleAlertFormVisible(false)">
          Cancel
        </a-button>
        <a-button
          key="submit"
          @click="handleSubmitAlertSetting"
          type="primary">
          Submit
        </a-button>
      </template>
    </a-modal>
  </div>
</template>

<script>
import {all, update} from '@api/setting'
import {
  list as listFlink,
  create as createFlink,
  get as getFlink,
  update as updateFlink,
  exists as existsEnv,
  setDefault,
  sync
} from '@/api/flinkEnv'

import {
  list as listCluster,
  create as createCluster,
  get as getCluster,
  update as updateCluster,
  check as checkCluster,
  start as startCluster,
  shutdown as shutdownCluster,
  remove as removeCluster
} from '@/api/flinkCluster'

import {
  add as addAlert,
  exists as existsAlert,
  update as updateAlert,
  get as getAlert,
  listWithOutPage as listWithOutPageAlert,
  remove as removeAlert,
  send as sendAlert
} from '@/api/alert'

import SvgIcon from '@/components/SvgIcon'
import monaco from '@/views/flink/app/Monaco.yaml'
import addCluster from './AddCluster'
import { mapActions } from 'vuex'
import storage from '@/utils/storage'
import cluster from '@/store/modules/cluster'
import { Item } from 'ant-design-vue/es/vc-menu'

export default {
  name: 'Setting',
  components: {SvgIcon, addCluster},
  data() {
    return {
      collapseActive: ['1', '2', '3' , '4'],
      activeKey:'system',
      settings: [],
      flinks: [],
      alerts: [],
      clusters: [],
      cluster: null,
      flinkName: null,
      flinkHome: null,
      flinkConf: null,
      versionId: null,
      alertId: null,
      clusterId: null,
      optionClusters: {
        'starting': new Map(),
        'created': new Map(),
        'stoped': new Map()
      },
      flinkConfVisible: false,
      flinkFormVisible: false,
      alertFormVisible: false,
      flinkClusterVisible: false,
      executionMode: null,
      executionModes: [
        {mode: 'remote (standalone)', value: 1, disabled: false},
        {mode: 'yarn session', value: 3, disabled: false},
        {mode: 'kubernetes session', value: 5, disabled: false}
      ],
      resolveOrder: [
        {name: 'parent-first', order: 0},
        {name: 'child-first', order: 1}
      ],
      alert: true,
      alertTypes: [
        {name: 'E-mail', value: 1, disabled: false},
        {name: 'Ding Talk', value: 2, disabled: false},
        {name: 'Wechat', value: 4, disabled: false},
        {name: 'SMS', value: 8, disabled: true},
        {name: 'Lark', value: 16, disabled: false}
      ],
      alertType: [],
      dingtalkIsAtAll: false,
      larkIsAtAll: false,
      dingtalkSecretEnable: false,
      larkSecretEnable: false,
      totalItems: [],
      editor: null,
      flinkForm: null,
      alertForm: null,
      clusterForm: null,
      buttonAddVisiable: false
    }
  },

  computed: {
    myTheme() {
      return this.$store.state.app.theme
    },
    dynamicOptions() {
      return function (group) {
        return this.options.filter(x => x.group === group)
      }
    },
    getRestUrl(item){
      return item.address
    }
  },

  mounted() {
    this.flinkForm = this.$form.createForm(this)
    this.alertForm = this.$form.createForm(this)
    this.clusterForm = this.$form.createForm(this)
    this.handleSettingAll()
    this.handleFlinkAll()
    this.handleClusterAll()
    this.handleAlertConfigAll()
    this.showtabs()
  },

  methods: {
    ...mapActions(['SetClusterId']),
    showtabs(){
      if(this.$route.query.activeKey!=null){
          this.activeKey = this.$route.query.activeKey
      }
    },
    changeVisble(){
      console.log('---zouguo-')
      this.buttonAddVisiable = false
      this.handleClusterAll()
    },
    handleAdd() {
      this.$router.push({'path': '/flink/setting/add_cluster'})
    },
    handleEditCluster(item) {
      this.SetClusterId(item.id)
      this.$router.push({'path': '/flink/setting/edit_cluster'})
    },
    getOption() {
      return {
        theme: this.ideTheme(),
        language: 'yaml',
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
      }
    },
    handleCheckExecMode(rule, value, callback) {
      if (value === null || value === undefined || value === '') {
        callback(new Error('Execution Mode is required'))
      } else {
        if (value === 3) {
          checkHadoop().then((resp) => {
            if (resp.data) {
              callback()
            } else {
              callback(new Error('Hadoop environment initialization failed, please check the environment settings'))
            }
          }).catch((err) => {
            callback(new Error('Hadoop environment initialization failed, please check the environment settings'))
          })
        } else {
          callback()
        }
      }
    },

    handleCheckAlertName(rule, alertName, callback) {
      if (alertName === null || alertName === undefined || alertName === '') {
        callback(new Error('Alert Name is required'))
      } else {
        if(!this.alertId){
          existsAlert({'alertName': alertName}).then((resp) => {
            if (!resp.data) {
              callback()
            } else {
              callback(new Error('Alert Name must be unique. The alert name already exists'))
            }
          }).catch((err) => {
            callback(new Error('error happened ,caused by: ' + err))
          })
        }else{
          callback()
        }
      }
    },

    handleChangeAlertType(value) {
      this.alertType = value
    },

    handleEditAlertType(value) {
      this.alertType.push(value)
    },

    handleChangeMode(mode) {
      this.executionMode = mode
    },

    handleSettingAll() {
      all({}).then((resp) => {
        this.settings = resp.data
      })
    },

    handleChangeProcess(value) {
      this.totalItems = value
    },
    handleEdit(setting) {
      if (!setting.editable) {
        setting.submitting = true
      }
      setting.editable = !setting.editable
    },

    handleIsStart(item) {
     /**
      集群刚创建但未启动
      CREATED(0),
      集群已启动
      STARTED(1),
      集群已停止
      STOPED(2);
    */
      return this.optionClusters.starting.get(item.id)
    },

    handleDeployCluser(item){
      this.$swal.fire({
        icon: 'success',
        title: 'The current cluster is starting',
        showConfirmButton: false,
        timer: 2000
      }).then((r) => {
          startCluster({id: item.id}).then((resp)=>{
            if(resp.data.status){
              this.optionClusters.starting.set(item.id,new Date().getTime())
              this.handleMapUpdate('starting')
              this.handleClusterAll()
              this.$swal.fire({
                icon: 'success',
                title: 'The current cluster is started',
                showConfirmButton: false,
                timer: 2000
              })
            } else {
              this.$swal.fire({
                  title: 'Failed',
                  icon: 'error',
                  width: this.exceptionPropWidth(),
                  html: '<pre class="propsException">' + resp.data.msg + '</pre>',
                  showCancelButton: true,
                  confirmButtonColor: '#55BDDDFF',
                  confirmButtonText: 'OK',
                  cancelButtonText: 'Close'
                })
            }
        })
      })
    },

    handleShutdownCluster(item){
      this.$swal.fire({
        icon: 'success',
        title: 'The current cluster is canceling',
        showConfirmButton: false,
        timer: 2000
      }).then((result) => {
        shutdownCluster({id: item.id}).then((resp) => {
          if(resp.data.status){
            this.optionClusters.starting.delete(item.id)
            this.handleMapUpdate('starting')
          }else{
            this.$swal.fire({
              title: 'Failed',
              icon: 'error',
              width: this.exceptionPropWidth(),
              html: '<pre class="propsException">' + resp.data.msg + '</pre>',
              showCancelButton: true,
              confirmButtonColor: '#55BDDDFF',
              confirmButtonText: 'OK',
              cancelButtonText: 'Close'
            })
          }
        })
      })
    },

    handleDelete(item){
      removeCluster({
        id: item.id
      }).then((resp) => {
        if(resp.data.status){
          this.optionClusters.starting.delete(item.id)
          this.handleMapUpdate('starting')
          this.handleClusterAll()
        }
      })
    },

    handleMapUpdate(type) {
      const map = this.optionClusters[type]
      this.optionClusters[type] = new Map(map)
    },

    handleSubmit(setting) {
      setting.submitting = false
      setting.editable = false
      const className = setting.key.replace(/\./g, '_')
      const elem = document.querySelector('.' + className)
      const value = elem.value
      update({
        key: setting.key,
        value: value
      }).then((resp) => {
        this.handleSettingAll()
      })
    },

    handleFlinkFormVisible(flag) {
      this.versionId = null
      this.flinkFormVisible = flag
      this.flinkForm.resetFields()
    },

    handleAlertFormVisible(flag) {
      this.alertId = null
      this.alertFormVisible = flag
      this.alertType = []
      this.dingtalkIsAtAll = false
      this.dingtalkSecretEnable = false
      this.larkIsAtAll = false
      this.larkSecretEnable = false
      this.alertForm.resetFields()
    },

    handleEditFlink(item) {
      this.versionId = item.id
      this.flinkFormVisible = true
      this.$nextTick(() => {
        this.flinkForm.setFieldsValue({
          'flinkName': item.flinkName,
          'flinkHome': item.flinkHome,
          'description': item.description || null
        })
      })
    },

    handleTestAlarm(item){
      sendAlert({ id : item.id }).then(resp=>{
        if (resp.data) {
          this.$swal.fire({
            icon: 'success',
            title: 'Test Alert Config  successful!',
            showConfirmButton: false,
            timer: 2000
          })
        }
        this.handleAlertConfigAll()
      }).catch(err => {})
    },

    handleEditAlertConf(item){
      this.alertId = item.id
      this.alertFormVisible = true
      const alertType = this.computeAlertType(item.alertType)
      alertType.forEach((value,i) => {
        this.handleEditAlertType(value)
      })
      var emailParams = {}
      var dingTalkParams = {}
      var weComParams = {}
      var larkParams = {}
      if (alertType.indexOf(1) > -1){
        emailParams = JSON.parse(item.emailParams)
      }
      if (alertType.indexOf(2) > -1) {
        dingTalkParams = JSON.parse(item.dingTalkParams)
        this.dingtalkIsAtAll = dingTalkParams.isAtAll
        this.dingtalkSecretEnable = dingTalkParams.secretEnable
      }
      if (alertType.indexOf(4) > -1) {
        weComParams = JSON.parse(item.weComParams)
      }
      if (alertType.indexOf(16) > -1) {
        larkParams = JSON.parse(item.larkParams)
        this.larkIsAtAll = larkParams.isAtAll
        this.larkSecretEnable = larkParams.secretEnable
      }

      console.log('告警参数：' + JSON.stringify(item))
      this.$nextTick(() => {
        this.alertForm.setFieldsValue({
          'alertName': item.alertName,
          'alertType': alertType,
          'alertEmail': emailParams.contacts,
          'alertDingURL': dingTalkParams.alertDingURL,
          'dingtalkToken': dingTalkParams.token,
          'dingtalkSecretToken': dingTalkParams.secretToken,
          'alertDingUser': dingTalkParams.contacts,
          'dingtalkIsAtAll': dingTalkParams.isAtAll,
          'dingtalkSecretEnable': dingTalkParams.secretEnable,
          'weToken': weComParams.token,
          'larkToken': larkParams.token,
          'larkIsAtAll': larkParams.isAtAll,
          'larkSecretEnable':larkParams.secretEnable,
          'larkSecretToken':larkParams.secretToken
        })
      })
    },

    handleDeleteAlertConf(item) {
      removeAlert({'id': item.id}).then((resp) => {
        if (resp.data) {
          this.$swal.fire({
            icon: 'success',
            title: 'Delete Alert Config  successful!',
            showConfirmButton: false,
            timer: 2000
          })
        } else {
          this.$swal.fire(
            'Failed delete AlertConfig',
            resp['message'].replaceAll(/\[StreamX]/g, ''),
            'error'
          )
        }
        this.handleAlertConfigAll()
      })
    },

    handleClusterFormVisible(flag) {
      this.clusterId = null
      this.flinkClusterVisible = flag
      this.clusterForm.resetFields()
    },
    handleFlinkAll() {
      listFlink({}).then((resp) => {
        this.flinks = resp.data
      })
    },

    handleClusterAll() {
      listCluster({}).then((resp) => {
        this.clusters = resp.data
        let c
        for(c in resp.data){
          const cluster = resp.data[c]
          if(resp.data[c].clusterState === 0){
              this.optionClusters.created.set(cluster.id,new Date().getTime())
          }else if(resp.data[c].clusterState === 1){
              this.optionClusters.starting.set(cluster.id,new Date().getTime())
          }else{
              this.optionClusters.stoped.set(cluster.id,new Date().getTime())
          }
        }
      })
    },


    computeAlertType(level){
        if (level === null) {
            level = 0
        }
        const result = new Array()
        while (level != 0) {
            // 获取最低位的 1
            const code = level & -level
            result.push(code)
            // 将最低位置 0
            level ^= code
        }
        return result
    },

    handleAlertConfigAll() {
      listWithOutPageAlert({}).then((resp) => {
        this.alerts = resp.data
      })
    },

    handleSubmitAlertSetting(e) {
      e.preventDefault()
      this.alertForm.validateFields((err, values) => {
        const param = {
          id: this.alertId,
          alertName: values.alertName,
          userId: storage.get('USER_INFO').userId,
          alertType: eval(values.alertType.join('+')),
          emailParams: {contacts: values.alertEmail},
          dingTalkParams: {
            token: values.dingtalkToken,
            contacts: values.alertDingUser,
            isAtAll: values.dingtalkIsAtAll,
            alertDingURL: values.alertDingURL,
            secretEnable: values.dingtalkSecretEnable,
            secretToken: values.dingtalkSecretToken
          },
          weComParams:{
            token:values.weToken
          },
          larkParams:{
            token: values.larkToken,
            isAtAll: values.larkIsAtAll,
            secretEnable: values.larkSecretEnable,
            secretToken: values.larkSecretToken
          }
        }
        console.log('更新告警参数：' + JSON.stringify(param))
        if (!err) {
          if(!param.id){//添加新告警
            existsAlert({'alertName': param.alertName}).then((resp)=>{
              if(resp.data){
                this.$swal.fire(
                  'Failed create AlertConfig',
                  'alertName ' + param.alertName + ' is already exists!',
                  'error'
                )
              }else{
                addAlert(param).then((resp) => {
                if (!resp.data) {//告警添加失败
                  this.$swal.fire(
                    'Failed create AlertConfig',
                    resp['message'].replaceAll(/\[StreamX]/g, ''),
                    'error'
                  )
                } else {//告警添加成功
                  this.$swal.fire({
                    icon: 'success',
                    title: 'Create AlertConfig successful!',
                    showConfirmButton: false,
                    timer: 2000
                  })
                  this.alertFormVisible = false
                  this.handleAlertConfigAll()
                }
              })
              }
            })
          }else{//根据告警id更新告警参数
            updateAlert(param).then((resp) => {
              if (!resp.data) {//告警更新失败
                this.$swal.fire(
                  'Failed update AlertConfig',
                  resp['message'].replaceAll(/\[StreamX]/g, ''),
                  'error'
                )
              } else {//告警更新成功
                this.$swal.fire({
                  icon: 'success',
                  title: 'Update AlertConfig successful!',
                  showConfirmButton: false,
                  timer: 2000
                })
                this.alertFormVisible = false
                this.handleAlertConfigAll()
              }
            })
          }

        }
      }).catch((err) => {
        callback(new Error('提交表单异常' + err))
      })
      this.alertId = null
    },

    handleSubmitFlink(e) {
      e.preventDefault()
      this.flinkForm.validateFields((err, values) => {
        if (!err) {
          existsEnv({
            id: this.versionId,
            flinkName: values.flinkName,
            flinkHome: values.flinkHome
          }).then((resp) => {
            if (resp.data) {
              if (this.versionId == null) {
                createFlink(values).then((resp) => {
                  if (resp.data) {
                    this.flinkFormVisible = false
                    this.handleFlinkAll()
                  } else {
                    this.$swal.fire(
                      'Failed',
                      resp['message'].replaceAll(/\[StreamX]/g, ''),
                      'error'
                    )
                  }
                })
              } else {
                updateFlink({
                  id: this.versionId,
                  flinkName: values.flinkName,
                  flinkHome: values.flinkHome,
                  description: values.description || null
                }).then((resp) => {
                  if (resp.data) {
                    this.flinkFormVisible = false
                    this.$swal.fire({
                      icon: 'success',
                      title: values.flinkName.concat(' update successful!'),
                      showConfirmButton: false,
                      timer: 2000
                    })
                    this.handleFlinkAll()
                  } else {
                    this.$swal.fire(
                      'Failed',
                      resp['message'].replaceAll(/\[StreamX]/g, ''),
                      'error'
                    )
                  }
                })
              }
            } else {
              if (resp.status === 'error') {
                this.$swal.fire(
                  'Failed',
                  'can no found flink-dist or found multiple flink-dist, FLINK_HOME error.',
                  'error'
                )
              } else {
                this.$swal.fire(
                  'Failed',
                  'flink name is already exists',
                  'error'
                )
              }
            }
          })
        }
      })
    },

    handleSubmitCluster(e) {
      e.preventDefault()
      this.clusterForm.validateFields((err, values) => {
        if (!err) {
          checkCluster({
            id: this.clusterId,
            clusterName: values.clusterName,
            address: values.address
          }).then((resp) => {
            if (resp.data === 'success') {
              if (this.clusterId == null) {
                createCluster({
                  clusterName: values.clusterName,
                  address: values.address,
                  description: values.description || null
                }).then((resp) => {
                  if (resp.data) {
                    this.flinkClusterVisible = false
                    this.handleClusterAll()
                  } else {
                    this.$swal.fire(
                      'Failed',
                      resp['message'].replaceAll(/\[StreamX]/g, ''),
                      'error'
                    )
                  }
                })
              } else {
                updateCluster({
                  id: this.clusterId,
                  clusterName: values.clusterName,
                  address: values.address,
                  description: values.description || null
                }).then((resp) => {
                  if (resp.data) {
                    this.clusterFormVisible = false
                    this.$swal.fire({
                      icon: 'success',
                      title: values.clusterName.concat(' update successful!'),
                      showConfirmButton: false,
                      timer: 2000
                    })
                    this.handleClusterAll()
                  } else {
                    this.$swal.fire(
                      'Failed',
                      resp['message'].replaceAll(/\[StreamX]/g, ''),
                      'error'
                    )
                  }
                })
              }
            } else {
              if (resp.data === 'exists') {
                this.$swal.fire(
                  'Failed',
                  'the cluster name: ' + values.clusterName + ' is already exists,please check',
                  'error'
                )
              } else if (resp.data === 'fail') {
                this.$swal.fire(
                  'Failed',
                  'the address is invalid or connection failure, please check',
                  'error'
                )
              }
            }
          })
        }
      })
    },

    handleFlinkConf(flink) {
      this.flinkConfVisible = true
      this.versionId = flink.id
      this.flinkName = flink.flinkName
      getFlink({id: this.versionId}).then((resp) => {
        this.flinkHome = resp.data.flinkHome
        this.flinkConf = resp.data.flinkConf
        this.handleInitEditor()
      })
    },

    handleInitEditor() {
      if (this.editor == null) {
        this.editor = monaco.editor.create(document.querySelector('#conf'), this.getOption())
        this.$nextTick(() => {
          const elem = document.querySelector('#conf')
          this.handleHeight(elem, 210)
        })
      }
      this.$nextTick(() => {
        this.editor.getModel().setValue(this.flinkConf)
      })
    },

    handleSync() {
      sync({id: this.versionId}).then((resp) => {
        getFlink({id: this.versionId}).then((resp) => {
          this.flinkHome = resp.data.flinkHome
          this.flinkConf = resp.data.flinkConf
          this.handleInitEditor()
        }),
        this.$swal.fire({
          icon: 'success',
          title: this.flinkName.concat(' conf sync successful!'),
          showConfirmButton: false,
          timer: 2000
        })
      })
    },

    handleSetDefault(item) {
      if (item.isDefault) {
        setDefault({id: item.id}).then((resp) => {
          this.$swal.fire({
            icon: 'success',
            title: item.flinkName.concat(' set default successful!'),
            showConfirmButton: false,
            timer: 2000
          })
          this.handleFlinkAll()
        })
      }
    },

    handleSetDingtalkSecretEnable(checked) {
      this.dingtalkSecretEnable = checked
    },

    handleSetLarkSecretEnable(checked) {
      this.larkSecretEnable = checked
    },

    handleDingtalkIsAtAll(checked) {
      this.dingtalkIsAtAll = checked
    },

    handleLarkIsAtAll(checked) {
      this.larkIsAtAll = checked
    },

    handleCloseConf() {
      this.flinkConfVisible = false
    },

    handleHeight(elem, h) {
      const height = document.documentElement.offsetHeight || document.body.offsetHeight
      $(elem).css('height', (height - h) + 'px')
    },

    handleSwitch(setting) {
      update({
        key: setting.key,
        value: setting.value !== 'true'
      }).then((resp) => {
        this.handleSettingAll()
      })
    }
  },

  watch: {
    myTheme() {
      if (this.editor != null) {
        this.editor.updateOptions({
          theme: this.ideTheme()
        })
      }
    }
  },

}
</script>

<style lang="less">
@import "View";

.ant-divider-inner-text {
  .svg-icon-middle {
    vertical-align: top;
  }
}
</style>
