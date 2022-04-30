<template>
  <a-card
    :body-style="{padding: '24px 32px'}"
    :bordered="false"
    class="app_controller">
    <a-form
      @submit="handleSubmit"
      :form="form"
      v-if="app != null">

      <a-form-item
        label="Development Mode"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">

        <a-alert
          v-if="app['jobType'] === 1"
          type="info" >
          <template slot="message">
            <a-icon type="code" style="color: #108ee9"/>&nbsp;Custom Code
          </template>
        </a-alert>
        <a-alert
          v-else
          type="info">
          <template slot="message">
            <svg-icon name="fql" style="color: #108ee9"/>&nbsp;Flink SQL
          </template>
        </a-alert>
      </a-form-item>

      <a-form-item
        label="Execution Mode"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-select
          placeholder="Execution Mode"
          v-decorator="[ 'executionMode' ]"
          @change="handleChangeMode">
          <a-select-option
            v-for="(o,index) in executionModes"
            :key="`execution_mode_${index}`"
            :disabled="o.disabled"
            :value="o.value">
            {{ o.mode }}
          </a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item
        label="Flink Version"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-select
          placeholder="Flink Version"
          v-decorator="[ 'versionId', {rules: [{ required: true, message: 'Flink Version is required' }] }]"
          @change="handleFlinkVersion">>
          <a-select-option
            v-for="(v,index) in flinkEnvs"
            :key="`flink_version_${index}`"
            :value="v.id">
            {{ v.flinkName }}
          </a-select-option>
        </a-select>
      </a-form-item>

      <template v-if="executionMode === 1">
        <a-form-item
          label="Flink Cluster"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
          <a-select
            placeholder="Flink Cluster"
            v-decorator="[ 'flinkClusterId', {rules: [{ required: true, message: 'Flink Cluster is required' }] }]">>
            <a-select-option
              v-for="(v,index) in getExecutionCluster(executionMode)"
              :key="`cluster_${index}`"
              :value="v.id">
              {{ v.clusterName }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </template>

      <template v-if="executionMode === 3">
        <a-form-item
          label="Yarn Session ClusterId"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
          <a-select
            allowClear
            placeholder="Please enter Yarn Session clusterId"
            v-decorator="[ 'yarnSessionClusterId', {rules: [{ required: true, message: 'Flink Cluster is required' }] }]">>
            <a-select-option
              v-for="(v,index) in getExecutionCluster(executionMode)"
              :key="`cluster_${index}`"
              :value="v.clusterId">
              {{ v.clusterName }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </template>

      <template v-if="(executionMode == null && (app.executionMode === 5 || app.executionMode === 6)) || (executionMode !== null && (executionMode === 5 || executionMode === 6))">
        <a-form-item
          label="Kubernetes Namespace"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
          <a-input
            type="text"
            placeholder="default"
            allowClear
            v-decorator="[ 'k8sNamespace']">
            <a-dropdown slot="addonAfter" placement="bottomRight">
              <a-menu slot="overlay" trigger="['click', 'hover']">
                <a-menu-item v-for="item in historyRecord.k8sNamespace" :key="item" @click="handleSelectHistoryK8sNamespace(item)" style="padding-right: 60px">
                  <a-icon type="plus-circle"/>{{ item }}
                </a-menu-item>
              </a-menu>
              <a-icon type="history"/>
            </a-dropdown>
          </a-input>
        </a-form-item>

        <a-form-item
          v-if="app.executionMode === 6 || executionMode === 6"
          label="Kubernetes ClusterId"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
          <a-input
            type="text"
            placeholder="Please enter Kubernetes clusterId"
            allowClear
            v-decorator="[ 'clusterId', {rules: [{ required: true, message: 'Kubernetes clusterId is required' }] }]">
            <template v-if="(executionMode == null && app.executionMode === 5) || (executionMode !== null && executionMode === 5)">
              <a-dropdown slot="addonAfter" placement="bottomRight">
                <a-menu slot="overlay" trigger="['click', 'hover']">
                  <a-menu-item v-for="item in historyRecord.k8sSessionClusterId" :key="item" @click="handleSelectHistoryK8sSessionClusterId(item)" style="padding-right: 60px">
                    <a-icon type="plus-circle"/>{{ item }}
                  </a-menu-item>
                </a-menu>
                <a-icon type="history"/>
              </a-dropdown>
            </template>
          </a-input>
        </a-form-item>

        <a-form-item
          v-if="app.executionMode === 5 || executionMode === 5"
          label="Kubernetes ClusterId"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
          <a-select
            allowClear
            placeholder="Please enter Kubernetes clusterId"
            v-decorator="[ 'clusterId', {rules: [{ required: true, message: 'Flink Cluster is required' }] }]">>
            <a-select-option
              v-for="(v,index) in getExecutionCluster(executionMode)"
              :key="`cluster_${index}`"
              :value="v.clusterId">
              {{ v.clusterName }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </template>

      <template v-if="(executionMode == null && app.executionMode === 6) || executionMode === 6">
        <a-form-item
          label="Flink Base Docker Image"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
          <a-input
            type="text"
            placeholder="Please enter the tag of Flink base docker image"
            allowClear
            v-decorator="[ 'flinkImage', {rules: [{ required: true, message: 'Flink Base Docker Image is required' }] }]">
            <a-dropdown slot="addonAfter" placement="bottomRight">
              <a-menu slot="overlay" trigger="['click', 'hover']">
                <a-menu-item v-for="item in historyRecord.flinkImage" :key="item" @click="handleSelectHistoryFlinkImage(item)" style="padding-right: 60px">
                  <a-icon type="plus-circle"/>{{ item }}
                </a-menu-item>
              </a-menu>
              <a-icon type="history"/>
            </a-dropdown>
          </a-input>
        </a-form-item>

        <a-form-item
          label="Rest-Service Exposed Type"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
          <a-select
            placeholder="kubernetes.rest-service.exposed.type"
            v-decorator="[ 'k8sRestExposedType' ]">
            <a-select-option
              v-for="(o,index) in k8sRestExposedType"
              :key="`k8s_rest_exposed_type_${index}`"
              :value="o.order">
              {{ o.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </template>

      <template v-if="app.jobType === 2">

        <a-form-item
          v-if="flinkSqlHistory.length > 1"
          label="History Version"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 17} }"
          class="form-required">
          <a-select
            @change="handleChangeSQL"
            v-model="defaultFlinkSqlId"
            style="width: calc(100% - 50px)">
            <a-select-option
              v-for="(ver,index) in flinkSqlHistory"
              :value="ver.id"
              :key="`sql_${index}`">
              <div>
                <a-button
                  type="primary"
                  shape="circle"
                  size="small">
                  {{ ver.version }}
                </a-button>
                <a-tag
                  color="green"
                  style=";margin-left: 5px;"
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
          <a-button
            type="primary"
            style="margin-left: 10px; width: 40px;"
            @click="handleCompactSQL"
            icon="swap">
          </a-button>
        </a-form-item>

        <a-form-item
          label="Flink SQL"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 17} }"
          class="form-required"
          style="margin-top: 10px">

          <div class="sql-box" id="flink-sql" :class="'syntax-' + controller.flinkSql.success"></div>

          <a-button-group class="flinksql-tool">
            <a-button
              class="flinksql-tool-item"
              type="primary"
              size="small"
              icon="check"
              @click="handleVerifySql">Verify
            </a-button>
            <a-button
              class="flinksql-tool-item"
              size="small"
              type="default"
              icon="thunderbolt"
              @click.native="handleFormatSql">Format
            </a-button>
            <a-button
              class="flinksql-tool-item"
              type="default"
              size="small"
              icon="fullscreen"
              @click="handleBigScreenOpen">Full Screen
            </a-button>
          </a-button-group>

          <p class="conf-desc" style="margin-bottom: -25px;margin-top: -5px">
            <span class="sql-desc" v-if="!controller.flinkSql.success">
              {{ controller.flinkSql.errorMsg }}
            </span>
            <span v-else class="sql-desc" style="color: green">
              <span v-if="controller.flinkSql.verified">
                successful
              </span>
            </span>
          </p>
        </a-form-item>

        <a-form-item
          label="Dependency"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 17} }"
          style="margin-top: 10px">
          <a-tabs type="card" v-model="controller.activeTab">
            <a-tab-pane
              key="pom"
              tab="Maven pom">
              <div class="pom-box syntax-true" style="height: 300px"></div>
              <a-button
                type="primary"
                class="apply-pom"
                @click="handleApplyPom()">
                Apply
              </a-button>
            </a-tab-pane>
            <a-tab-pane
              key="jar"
              tab="Upload Jar">
              <template v-if="(executionMode == null && (app.executionMode === 5 || app.executionMode === 6)) || (executionMode !== null && (executionMode === 5 || executionMode === 6))">
                <a-select
                  mode="multiple"
                  placeholder="Search History Uploads"
                  :value="selectedHistoryUploadJars"
                  style="width: 100%;"
                  :showArrow="true"
                  @change="handleSearchHistoryUploadJars"
                  @select="addHistoryUploadJar"
                  @deselect="deleteHistoryUploadJar">
                  <a-select-option v-for="item in filteredHistoryUploadJarsOptions" :key="item" :value="item">
                    <a-icon slot="suffixIcon" type="file-done"/>
                    {{ item }}
                  </a-select-option>
                </a-select>
              </template>

              <a-upload-dragger
                name="file"
                :multiple="true"
                @change="handleUploadJar"
                :showUploadList="loading"
                :customRequest="handleCustomRequest"
                :beforeUpload="handleBeforeUpload">
                <div :style="{height: (executionMode === 5 || executionMode === 6) ? '234px' : '266px'}">
                  <p
                    class="ant-upload-drag-icon"
                    style="padding-top: 40px">
                    <a-icon
                      type="inbox"
                      :style="{ fontSize: '70px' }" />
                  </p>
                  <p
                    class="ant-upload-text"
                    style="height: 45px">
                    Click or drag jar to this area to upload
                  </p>
                  <p
                    class="ant-upload-hint"
                    style="height: 45px">
                    Support for a single or bulk upload. You can upload a local jar here to support for current Job.
                  </p>
                </div>
              </a-upload-dragger>
            </a-tab-pane>
          </a-tabs>

          <div
            v-if="dependency.length > 0 || uploadJars.length > 0"
            class="dependency-box">
            <a-alert
              class="dependency-item"
              v-for="(value, index) in dependency"
              :key="`dependency_${index}`"
              type="info">
              <template slot="message">
                <a-space class="tag-dependency-pom">
                  <a-tag class="tag-dependency" color="#2db7f5">POM</a-tag>
                  <span @click="handleEditPom(value)">
                    {{ value.artifactId }}-{{ value.version }}.jar
                  </span>
                  <a-icon type="close" class="icon-close" @click="handleRemovePom(value)"/>
                </a-space>
              </template>
            </a-alert>
            <a-alert
              class="dependency-item"
              v-for="(value, index) in uploadJars"
              :key="`jars_${index}`"
              type="info">
              <template slot="message">
                <a-space>
                  <a-tag class="tag-dependency" color="#108ee9">JAR</a-tag>
                  {{ value }}
                  <a-icon type="close" class="icon-close" @click="handleRemoveJar(value)"/>
                </a-space>
              </template>
            </a-alert>
          </div>
        </a-form-item>

        <a-form-item
          label="Application Conf"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 17} }"
          v-show="(executionMode == null && app.executionMode !== 5 && app.executionMode !== 6) || (executionMode !== null && executionMode !== 5 && executionMode !== 6)">
          <a-switch
            checked-children="ON"
            un-checked-children="OFF"
            @click="handleSQLConf"
            v-model="isSetConfig"
            v-decorator="[ 'config' ]" />
          <a-icon
            v-if="isSetConfig"
            type="setting"
            style="margin-left: 10px"
            theme="twoTone"
            two-tone-color="#4a9ff5"
            @click="handleSQLConf(true)" />
        </a-form-item>
      </template>

      <template v-else>
        <a-form-item
          label="Project"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
          <a-alert
            :message="app['projectName']"
            type="info" />
        </a-form-item>

        <a-form-item
          label="Application"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
          <a-alert
            :message="app['module']"
            type="info" />
        </a-form-item>

        <a-form-item
          label="Application conf"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
          <a-input-group
            compact>
            <a-select
              style="width: 25%"
              default-value="1"
              @change="handleChangeStrategy">
              <a-select-option
                value="1">
                use existing
              </a-select-option>
              <a-select-option
                value="2">
                reselect
              </a-select-option>
            </a-select>

            <a-select
              v-if="strategy === 1"
              style="width: calc(75% - 50px)"
              @change="handleChangeConfig"
              v-model="defaultConfigId">
              <a-select-option
                v-for="(ver,index) in configVersions"
                :value="ver.id"
                :key="`config_${index}`">
                <div>
                  <a-button
                    type="primary"
                    shape="circle"
                    size="small">
                    {{ ver.version }}
                  </a-button>
                  <a-tag
                    color="green"
                    style=";margin-left: 5px;"
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

            <a-tree-select
              style="width: calc(75% - 50px)"
              v-if="strategy === 2"
              :dropdown-style="{ maxHeight: '400px', overflow: 'auto' }"
              :tree-data="configSource"
              placeholder="Please select config"
              tree-default-expand-all
              @change="handleChangeNewConfig"
              v-decorator="[ 'config', {rules: [{ required: true}]} ]">
            </a-tree-select>

            <a-button
              :disabled="strategy === 1? false: (this.form.getFieldValue('config')? false : true)"
              type="primary"
              style="margin-left: 10px; width: 40px;"
              @click="handleEditConfig()"
              icon="setting"
              theme="twoTone"
              two-tone-color="#4a9ff5">
            </a-button>

          </a-input-group>
        </a-form-item>

        <a-form-item
          v-show="strategy === 1 && configVersions.length>1"
          label="Compare conf"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
          <a-input-group
            compact>
            <a-select
              style="width: calc(100% - 50px)"
              v-decorator="[ 'compare_conf' ]"
              placeholder="Please select the configuration version to compare"
              mode="multiple"
              @change="handleChangeConfCompact"
              class="version-select"
              :max-tag-count="selectTagCount.count2">
              <a-select-option
                v-for="(ver,index) in configVersions"
                :value="ver.id"
                :key="`compare_${index}`">
                <div>
                  <a-button
                    type="primary"
                    shape="circle"
                    size="small">
                    {{ ver.version }}
                  </a-button>
                  <a-tag
                    color="green"
                    style=";margin-left: 5px;"
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
            <a-button
              :type="compareDisabled? 'default' :'primary'"
              :disabled="compareDisabled"
              style="margin-left: 10px; width: 40px;"
              :style="{'color':compareDisabled?null:'#fff'}"
              @click="handleCompactConf"
              icon="swap">
            </a-button>
          </a-input-group>
        </a-form-item>
      </template>

      <a-form-item
        label="Use System Hadoop Conf"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }"
        v-show="(executionMode == null && app.executionMode === 6) || executionMode === 6">
        <a-space>
          <a-switch
            checked-children="ON"
            un-checked-children="OFF"
            v-model="useSysHadoopConf"
            @change="handleUseSysHadoopConf"/>
          <a-popover title="Tips">
            <template slot="content">
              <p>Automatically copy configuration files from system environment parameters</p>
              <p><b>HADOOP_CONF_PATH</b> and <b>HIVE_CONF_PATH</b> to Flink Docker image</p>
            </template>
            <a-icon
              type="question-circle"
              style="margin-left: 10px"/>
          </a-popover>
          <transition name="slide-fade">
            <a-button
              icon="eye"
              size="small"
              v-if="useSysHadoopConf == true"
              @click="showSysHadoopConfDrawer">view</a-button>
          </transition>
        </a-space>

        <a-drawer
          title="System Hadoop Conifguration"
          placement="right"
          :width="800"
          :closable="false"
          item-layout="vertical"
          :visible="hadoopConfDrawer.visual"
          @close="closeSysHadoopConfDrawer">
          <a-tabs tabPosition="top">
            <a-tab-pane key="hadoop" tab="Hadoop">
              <template
                v-if="hadoopConfDrawer.content.hadoop == null || Object.keys(hadoopConfDrawer.content.hadoop).length === 0 || hadoopConfDrawer.content.hadoop.size === 0">
                <a-empty/>
              </template>
              <template v-else>
                <a-tabs tabPosition="left">
                  <a-tab-pane v-for="(content, fname) in hadoopConfDrawer.content.hadoop" :key="fname" :tab="fname">
                    <pre style="font-size: 12px; margin-top: 20px; margin-bottom: 20px">{{ content }}</pre>
                  </a-tab-pane>
                </a-tabs>
              </template>
            </a-tab-pane>
            <a-tab-pane key="hive" tab="Hive">
              <template
                v-if="hadoopConfDrawer.content.hive == null || Object.keys(hadoopConfDrawer.content.hive).length === 0 || hadoopConfDrawer.content.hive.size === 0">
                <a-empty/>
              </template>
              <template v-else>
                <a-tabs tabPosition="left">
                  <a-tab-pane v-for="(content, fname) in hadoopConfDrawer.content.hive" :key="fname" :tab="fname">
                    <pre style="font-size: 12px; margin-top: 20px; margin-bottom: 20px">{{ content }}</pre>
                  </a-tab-pane>
                </a-tabs>
              </template>
            </a-tab-pane>
          </a-tabs>
        </a-drawer>
      </a-form-item>

      <a-form-item
        label="Application Name"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-input
          type="text"
          placeholder="Please enter Application Name"
          v-decorator="['jobName',{ rules: [{ validator: handleCheckJobName,trigger:'submit',required: true } ]}]" />
      </a-form-item>

      <a-form-item
        label="Resolve Order"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-select
          placeholder="classloader.resolve-order"
          v-decorator="[ 'resolveOrder', {rules: [{ required: true, message: 'Resolve Order is required' }] }]">
          <a-select-option
            v-for="(o,index) in resolveOrder"
            :key="`resolve_order_${index}`"
            :value="o.order">
            {{ o.name }}
          </a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item
        label="Parallelism"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-input-number
          :min="1"
          :step="1"
          placeholder="The parallelism with which to run the program"
          v-decorator="['parallelism']" />
      </a-form-item>

      <a-form-item
        label="Task Slots"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-input-number
          :min="1"
          :step="1"
          placeholder="Number of slots per TaskManager"
          v-decorator="['slot']" />
      </a-form-item>

      <a-form-item
        label="Kubernetes Pod Template"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }"
        v-show="(executionMode == null && app.executionMode === 6) || executionMode === 6">
        <a-tabs type="card" v-model="controller.podTemplateTab">
          <a-tab-pane
            key="pod-template"
            tab="Pod Template"
            forceRender>
            <a-button-group class="pod-template-tool">
              <a-button
                size="small"
                type="primary"
                icon="history"
                class="pod-template-tool-item"
                @click="showPodTemplateDrawer('ptVisual')">History
              </a-button>
              <a-button
                type="default"
                size="small"
                icon="copy"
                class="pod-template-tool-item"
                @click="handleGetInitPodTemplate('ptVisual')">Init Content
              </a-button>
              <a-button
                type="default"
                size="small"
                icon="share-alt"
                class="pod-template-tool-item"
                @click="showTemplateHostAliasDrawer('ptVisual')">Host Alias
              </a-button>
              <a-button
                type="default"
                size="small"
                icon="hdd"
                disabled
                ghost
                class="pod-template-tool-item">PVC
              </a-button>
            </a-button-group>
            <a-drawer
              title="Pod Template History"
              placement="right"
              :width="700"
              item-layout="vertical"
              :closable="false"
              :visible="this.podTemplateDrawer.ptVisual"
              @close="closePodTemplateDrawer('ptVisual')">
              <template>
                <a-empty v-if="historyRecord.podTemplate == null || historyRecord.podTemplate.length == 0"/>
                <a-card
                  title="pod-template.yaml"
                  size="small"
                  hoverable
                  style="margin-bottom: 8px"
                  v-for="(item,index) in historyRecord.podTemplate"
                  :key="index">
                  <a slot="extra" @click="handleChoicePodTemplate('ptVisual', item)">Choice</a>
                  <pre style="font-size: 12px">{{ item }}</pre>
                </a-card>
              </template>
            </a-drawer>
            <a-drawer
              title="Pod Template HostAlias"
              placement="right"
              :width="500"
              item-layout="vertical"
              :closable="false"
              :visible="podTemplateHostAliasDrawer.ptVisual"
              @close="closeTemplateHostAliasDrawer('ptVisual')">
              <template>
                <a-row>
                  <p class="conf-desc">
                    <span class="note-info" style="margin-bottom: 12px">
                      <a-tag color="#2db7f5" class="tag-note">Note</a-tag>
                      Enter the host-ip mapping value in the format <b>[hostname:ip]</b>, e.g: chd01.streamx.com:192.168.112.233
                    </span>
                  </p>
                </a-row>
                <a-row>
                  <a-select
                    mode="multiple"
                    placeholder="Search System Hosts"
                    :value="selectedPodTemplateHostAlias"
                    style="width: 100%;"
                    :showArrow="true"
                    @change="handleSelectedTemplateHostAlias">
                    <a-select-option v-for="item in filteredPodTemplateHostAliasOptions" :key="item" :value="item">
                      <a-icon slot="suffixIcon" type="plus-circle"/>
                      {{ item }}
                    </a-select-option>
                  </a-select>
                </a-row>
                <a-row style="margin-top: 30px">
                  <a-card
                    title="preview"
                    size="small"
                    hoverable>
                    <pre style="font-size: 12px">{{ hostAliasPreview }}</pre>
                  </a-card>
                </a-row>
              </template>
              <div class="pod-template-tool-drawer-submit-cancel">
                <a-button :style="{ marginRight: '8px' }" @click="closeTemplateHostAliasDrawer('ptVisual')">
                  Cancel
                </a-button>
                <a-button type="primary" @click="handleSubmitHostAliasToPodTemplate('ptVisual')">
                  Submit
                </a-button>
              </div>
            </a-drawer>
            <div class="pod-template-box syntax-true" />
          </a-tab-pane>

          <a-tab-pane
            key="jm-pod-template"
            tab="JM Pod Template"
            forceRender>
            <a-button-group class="pod-template-tool">
              <a-button
                size="small"
                type="primary"
                icon="history"
                class="pod-template-tool-item"
                @click="showPodTemplateDrawer('jmPtVisual')">History
              </a-button>
              <a-button
                type="default"
                size="small"
                icon="copy"
                class="pod-template-tool-item"
                @click="handleGetInitPodTemplate('jmPtVisual')">Init Content
              </a-button>
              <a-button
                type="default"
                size="small"
                icon="share-alt"
                class="pod-template-tool-item"
                @click="showTemplateHostAliasDrawer('jmPtVisual')">Host Alias
              </a-button>
              <a-button
                type="default"
                size="small"
                icon="hdd"
                disabled
                ghost
                class="pod-template-tool-item">PVC
              </a-button>
            </a-button-group>
            <a-drawer
              title="JobManager Pod Template History"
              placement="right"
              :width="700"
              item-layout="vertical"
              :closable="false"
              :visible="this.podTemplateDrawer.jmPtVisual"
              @close="closePodTemplateDrawer('jmPtVisual')">
              <template>
                <a-empty v-if="historyRecord.jmPodTemplate == null || historyRecord.jmPodTemplate.length == 0"/>
                <a-card
                  title="jm-pod-template.yaml"
                  size="small"
                  hoverable
                  style="margin-bottom: 8px"
                  v-for="(item,index) in historyRecord.jmPodTemplate"
                  :key="index">
                  <a slot="extra" @click="handleChoicePodTemplate('jmPtVisual', item)">Choice</a>
                  <pre style="font-size: 12px">{{ item }}</pre>
                </a-card>
              </template>
            </a-drawer>
            <a-drawer
              title="JM Pod Template HostAlias"
              placement="right"
              :width="500"
              item-layout="vertical"
              :closable="false"
              :visible="podTemplateHostAliasDrawer.jmPtVisual"
              @close="closeTemplateHostAliasDrawer('jmPtVisual')">
              <template>
                <a-row>
                  <p class="conf-desc">
                    <span class="note-info" style="margin-bottom: 12px">
                      <a-tag color="#2db7f5" class="tag-note">Note</a-tag>
                      Enter the host-ip mapping value in the format <b>[hostname:ip]</b>, e.g: chd01.streamx.com:192.168.112.233
                    </span>
                  </p>
                </a-row>
                <a-row>
                  <a-select
                    mode="multiple"
                    placeholder="Search System Hosts"
                    :value="selectedPodTemplateHostAlias"
                    style="width: 100%;"
                    :showArrow="true"
                    @change="handleSelectedTemplateHostAlias">
                    <a-select-option v-for="item in filteredPodTemplateHostAliasOptions" :key="item" :value="item">
                      <a-icon slot="suffixIcon" type="plus-circle"/>
                      {{ item }}
                    </a-select-option>
                  </a-select>
                </a-row>
                <a-row style="margin-top: 30px">
                  <a-card
                    title="preview"
                    size="small"
                    hoverable>
                    <pre style="font-size: 12px">{{ hostAliasPreview }}</pre>
                  </a-card>
                </a-row>
              </template>
              <div class="pod-template-tool-drawer-submit-cancel">
                <a-button :style="{ marginRight: '8px' }" @click="closeTemplateHostAliasDrawer('jmPtVisual')">
                  Cancel
                </a-button>
                <a-button type="primary" @click="handleSubmitHostAliasToPodTemplate('jmPtVisual')">
                  Submit
                </a-button>
              </div>
            </a-drawer>
            <div class="jm-pod-template-box syntax-true" />
          </a-tab-pane>

          <a-tab-pane
            key="tm-pod-template"
            tab="TM Pod Template"
            forceRender>
            <a-button-group class="pod-template-tool">
              <a-button
                size="small"
                type="primary"
                icon="history"
                class="pod-template-tool-item"
                @click="showPodTemplateDrawer('tmPtVisual')">History
              </a-button>
              <a-button
                type="default"
                size="small"
                icon="copy"
                class="pod-template-tool-item"
                @click="handleGetInitPodTemplate('tmPtVisual')">Init Content
              </a-button>
              <a-button
                type="default"
                size="small"
                icon="share-alt"
                class="pod-template-tool-item"
                @click="showTemplateHostAliasDrawer('tmPtVisual')">Host Alias
              </a-button>
              <a-button
                type="default"
                size="small"
                icon="hdd"
                disabled
                ghost
                class="pod-template-tool-item">PVC
              </a-button>
            </a-button-group>
            <a-drawer
              title="TaskManager Pod Template History"
              placement="right"
              :width="700"
              item-layout="vertical"
              :closable="false"
              :visible="this.podTemplateDrawer.tmPtVisual"
              @close="closePodTemplateDrawer('tmPtVisual')">
              <template>
                <a-empty v-if="historyRecord.tmPodTemplate == null || historyRecord.tmPodTemplate.length == 0"/>
                <a-card
                  title="tm-pod-template.yaml"
                  size="small"
                  hoverable
                  style="margin-bottom: 8px"
                  v-for="(item,index) in historyRecord.tmPodTemplate"
                  :key="index">
                  <a slot="extra" @click="handleChoicePodTemplate('tmPtVisual', item)">Choice</a>
                  <pre style="font-size: 12px">{{ item }}</pre>
                </a-card>
              </template>
            </a-drawer>
            <a-drawer
              title="Pod Template HostAlias"
              placement="right"
              :width="500"
              item-layout="vertical"
              :closable="false"
              :visible="podTemplateHostAliasDrawer.tmPtVisual"
              @close="closeTemplateHostAliasDrawer('tmPtVisual')">
              <template>
                <a-row>
                  <p class="conf-desc">
                    <span class="note-info" style="margin-bottom: 12px">
                      <a-tag color="#2db7f5" class="tag-note">Note</a-tag>
                      Enter the host-ip mapping value in the format <b>[hostname:ip]</b>, e.g: chd01.streamx.com:192.168.112.233
                    </span>
                  </p>
                </a-row>
                <a-row>
                  <a-select
                    mode="multiple"
                    placeholder="Search System Hosts"
                    :value="selectedPodTemplateHostAlias"
                    style="width: 100%;"
                    :showArrow="true"
                    @change="handleSelectedTemplateHostAlias">
                    <a-select-option v-for="item in filteredPodTemplateHostAliasOptions" :key="item" :value="item">
                      <a-icon slot="suffixIcon" type="plus-circle"/>
                      {{ item }}
                    </a-select-option>
                  </a-select>
                </a-row>
                <a-row style="margin-top: 30px">
                  <a-card
                    title="preview"
                    size="small"
                    hoverable>
                    <pre style="font-size: 12px">{{ hostAliasPreview }}</pre>
                  </a-card>
                </a-row>
              </template>
              <div class="pod-template-tool-drawer-submit-cancel">
                <a-button :style="{ marginRight: '8px' }" @click="closeTemplateHostAliasDrawer('tmPtVisual')">
                  Cancel
                </a-button>
                <a-button type="primary" @click="handleSubmitHostAliasToPodTemplate('tmPtVisual')">
                  Submit
                </a-button>
              </div>
            </a-drawer>
            <div class="tm-pod-template-box syntax-true" />
          </a-tab-pane>
        </a-tabs>
      </a-form-item>

      <a-form-item
        label="Fault Restart Size"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }"
        v-show="(executionMode === null && app.executionMode !== 5 && app.executionMode !== 6) || (executionMode !== null && executionMode !== 5 && executionMode !== 6)">
        <a-input-number
          :min="1"
          :step="1"
          placeholder="restart max size"
          v-decorator="['restartSize']" />
      </a-form-item>

      <a-form-item
        label="CheckPoint Failure Options"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }"
        v-show="(executionMode === null && app.executionMode !== 5 && app.executionMode !== 6) || (executionMode !== null && executionMode !== 5 && executionMode !== 6)">
        <a-input-group compact>
          <a-input-number
            :min="1"
            :step="1"
            placeholder="checkpoint failure rate interval"
            v-decorator="['cpMaxFailureInterval',{ rules: [ { validator: handleCheckCheckPoint , trigger:'change'} ]}]"
            style="width: calc(33% - 70px)"/>
          <a-button style="width: 70px">
            minute
          </a-button>
          <a-input-number
            :min="1"
            :step="1"
            placeholder="max failures per interval"
            v-decorator="['cpFailureRateInterval',{ rules: [ { validator: handleCheckCheckPoint , trigger: 'change'} ]}]"
            style="width: calc(33% - 70px); margin-left: 1%"/>
          <a-button style="width: 70px">
            count
          </a-button>
          <a-select
            placeholder="trigger action"
            allowClear
            v-decorator="['cpFailureAction',{ rules: [ { validator: handleCheckCheckPoint , trigger: 'change'} ]}]"
            style="width: 32%;margin-left: 1%">
            <a-select-option
              v-for="(o,index) in cpTriggerAction"
              :key="`cp_trigger_${index}`"
              :value="o.value">
              <a-icon :type="o.value === 1?'alert':'sync'"/> {{ o.name }}
            </a-select-option>
          </a-select>
        </a-input-group>

        <p class="conf-desc" style="margin-bottom: -15px;margin-top: -3px">
          <span class="note-info" style="margin-bottom: 12px">
            <a-tag color="#2db7f5" class="tag-note">Note</a-tag>
            Operation after checkpoint failure, e.g:<br>
            Within <span class="note-elem">5 minutes</span>(checkpoint failure rate interval), if the number of checkpoint failures reaches <span class="note-elem">10</span> (max failures per interval),action will be triggered(alert or restart job)
          </span>
        </p>
      </a-form-item>

      <a-form-item
        label="Alert Email List"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-input
          type="text"
          placeholder="Please enter email,separate multiple emails with comma(,)"
          allowClear
          v-decorator="[ 'alertEmail',{ rules: [ { validator: handleCheckAlertEmail} ]} ]">
          <svg-icon name="mail" slot="prefix"/>
        </a-input>
      </a-form-item>

      <a-form-item
        v-if="1===2"
        label="Configuration"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-select
          show-search
          allow-clear
          mode="multiple"
          :max-tag-count="selectTagCount.count1"
          placeholder="Please select parameter"
          @change="handleChangeConf"
          v-decorator="['configuration']">
          <a-select-option
            v-for="(conf,index) in configuration"
            :key="`configuration_${index}`"
            :value="conf.key">
            {{ conf.name }}
          </a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item
        class="conf-item"
        v-for="(conf,index) in hasOptions(configItems)"
        :key="`config_items_${index}`"
        :label="conf.name"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-input
          v-if="conf.type === 'input'"
          type="text"
          :placeholder="conf.placeholder"
          v-decorator="[`${conf.name}`,{ rules:[{ validator: conf.validator, trigger:'submit'} ]}]" />
        <a-switch
          v-if="conf.type === 'switch'"
          disabled
          checked-children="ON"
          un-checked-children="OFF"
          v-model="switchDefaultValue"
          v-decorator="[`${conf.name}`]" />
        <a-input-number
          v-if="conf.type === 'number'"
          :min="conf.min"
          :max="conf.max"
          :default-value="conf.defaultValue"
          :step="conf.step"
          v-decorator="[`${conf.name}`,{ rules:[{ validator: conf.validator, trigger:'submit'} ]}]" />
        <span
          v-if="conf.type === 'switch'"
          class="conf-switch">({{ conf.placeholder }})</span>
        <p
          class="conf-desc">
          {{ conf | description }}
        </p>
      </a-form-item>

      <a-form-item
        label="Total Memory Options"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-select
          show-search
          allow-clear
          mode="multiple"
          :max-tag-count="totalTagCount"
          placeholder="Please select the resource parameters to set"
          @change="handleChangeProcess"
          v-decorator="['totalOptions']">
          <a-select-opt-group
            label="process memory(进程总内存)">
            <a-select-option
              v-for="(conf,index) in dynamicOptions('process-memory')"
              :key="`process_memory_${index}`"
              :value="conf.key">
              {{ conf.name }}
            </a-select-option>
          </a-select-opt-group>
          <a-select-opt-group
            label="total memory(Flink 总内存)">
            <a-select-option
              v-for="(conf,index) in dynamicOptions('total-memory')"
              :key="`total_memory_${index}`"
              :value="conf.key">
              {{ conf.name }}
            </a-select-option>
          </a-select-opt-group>
        </a-select>
        <p class="conf-desc" style="margin-bottom: -15px;margin-top: -3px">
          <span class="note-info">
            <a-tag color="#2db7f5" class="tag-note">Note</a-tag>
            Explicitly configuring both <span class="note-elem">total process memory</span> and <span class="note-elem">total Flink memory</span> is not recommended. It may lead to deployment failures due to potential memory configuration conflicts. Configuring other memory components also requires caution as it can produce further configuration conflicts,
            The easiest way is to set <span class="note-elem">total process memory</span>
          </span>
        </p>
      </a-form-item>

      <a-form-item
        class="conf-item"
        v-for="(conf,index) in hasOptions(totalItems)"
        :key="`total_items_${index}`"
        :label="conf.name.replace(/.memory/g,'').replace(/\./g,' ')"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-input-number
          v-if="conf.type === 'number'"
          :min="conf.min"
          :max="conf.max"
          :default-value="conf.defaultValue"
          :step="conf.step"
          v-decorator="[`${conf.key}`,{ rules:[{ validator: conf.validator, trigger:'submit'} ]}]" />
        <span
          v-if="conf.type === 'switch'"
          class="conf-switch">({{ conf.placeholder }})</span>
        <p
          class="conf-desc">
          {{ conf | description }}
        </p>
      </a-form-item>

      <a-form-item
        label="JM Memory Options"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-select
          show-search
          allow-clear
          mode="multiple"
          :max-tag-count="jmMaxTagCount"
          placeholder="Please select the resource parameters to set"
          @change="handleChangeJmMemory"
          v-decorator="['jmOptions']">
          <a-select-option
            v-for="(conf,index) in dynamicOptions('jobmanager-memory')"
            :key="`jm_memory_${index}`"
            :value="conf.key">
            {{ conf.name }}
          </a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item
        class="conf-item"
        v-for="(conf,index) in hasOptions(jmMemoryItems)"
        :key="`jm_memory_items_${index}`"
        :label="conf.name.replace(/jobmanager.memory./g,'')"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-input-number
          v-if="conf.type === 'number'"
          :min="conf.min"
          :max="conf.max"
          :default-value="conf.defaultValue"
          :step="conf.step"
          v-decorator="[`${conf.key}`,{ rules:[{ validator: conf.validator, trigger:'submit'} ]}]" />
        <span
          v-if="conf.type === 'switch'"
          class="conf-switch">({{ conf.placeholder }})</span>
        <p
          class="conf-desc">
          {{ conf | description }}
        </p>
      </a-form-item>

      <a-form-item
        label="TM Memory Options"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-select
          show-search
          allow-clear
          mode="multiple"
          :max-tag-count="tmMaxTagCount"
          placeholder="Please select the resource parameters to set"
          @change="handleChangeTmMemory"
          v-decorator="['tmOptions']">
          <a-select-option
            v-for="(conf,index) in dynamicOptions('taskmanager-memory')"
            :key="`tm_memory_${index}`"
            :value="conf.key">
            {{ conf.name }}
          </a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item
        class="conf-item"
        v-for="(conf,index) in hasOptions(tmMemoryItems)"
        :key="`tm_memory_items_${index}`"
        :label="conf.name.replace(/taskmanager.memory./g,'')"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-input-number
          v-if="conf.type === 'number'"
          :min="conf.min"
          :max="conf.max"
          :default-value="conf.defaultValue"
          :step="conf.step"
          v-decorator="[`${conf.key}`,{ rules:[{ validator: conf.validator, trigger:'submit'} ]}]" />
        <span
          v-if="conf.type === 'switch'"
          class="conf-switch">({{ conf.placeholder }})</span>
        <p
          class="conf-desc">
          {{ conf | description }}
        </p>
      </a-form-item>

      <template v-if="executionMode === 4">
        <a-form-item
          label="Yarn Queue"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
          <a-input
            type="text"
            allowClear
            placeholder="Please enter yarn queue"
            v-decorator="[ 'yarnQueue']">
          </a-input>
        </a-form-item>
      </template>

      <a-form-item
        label="Dynamic Option"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-textarea
          rows="4"
          name="dynamicOptions"
          placeholder="$key=$value,If there are multiple parameters,you can new line enter them (-D <arg>)"
          v-decorator="['dynamicOptions']" />
        <p class="conf-desc">
          <span class="note-info">
            <a-tag color="#2db7f5" class="tag-note">Note</a-tag>
            It works the same as <span class="note-elem">-D$property=$value</span> in CLI mode, Allows specifying multiple generic configuration options. The available options can be found
            <a href="https://ci.apache.org/projects/flink/flink-docs-stable/ops/config.html" target="_blank">here</a>
          </span>
        </p>
      </a-form-item>

      <a-form-item
        label="Program Args"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-textarea
          rows="4"
          name="args"
          placeholder="<arguments>"
          v-decorator="['args']" />
      </a-form-item>

      <a-form-item
        label="Description"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-textarea
          rows="4"
          name="description"
          placeholder="Please enter description"
          v-decorator="['description']" />
      </a-form-item>

      <a-form-item
        :wrapper-col="{ span: 24 }"
        style="text-align: center">
        <a-button
          @click="handleReset(true)">
          Reset
        </a-button>
        <a-button
          html-type="submit"
          type="primary"
          :loading="submitting"
          :disabled="submitting"
          style="margin-left: 15px">
          Submit
        </a-button>
      </a-form-item>
    </a-form>

    <a-modal
      v-model="controller.visiable.bigScreen"
      width="100%"
      class="bigScreen-sql-model"
      :body-style="controller.modal.bigScreen.style"
      :destroy-on-close="controller.modal.destroyOnClose"
      @ok="handleBigScreenOk">
      <template slot="title">
        <svg-icon name="flinksql" size="middle"/>&nbsp; {{ controller.modal.bigScreen.title }}
      </template>
      <template slot="closeIcon">
        <a-icon
          type="fullscreen-exit"
          @click="handleBigScreenClose"/>
      </template>
      <template slot="footer">
        <span style="color: red;float: left">
          <ellipsis :length="200">
            {{ controller.flinkSql.errorMsg }}
          </ellipsis>
        </span>
        <a-button
          type="primary"
          icon="thunderbolt"
          @click.native="handleFormatSql">Format
        </a-button>
        <a-button
          type="primary"
          icon="fullscreen"
          @click="handleBigScreenOk">Apply
        </a-button>
      </template>
      <div class="sql-box" id="big-sql" style="width: 100%;" :class="'syntax-' + controller.flinkSql.success"></div>
    </a-modal>

    <a-modal
      v-model="compareVisible"
      on-ok="handleCompareOk"
      v-if="compareVisible">
      <template
        slot="title">
        <a-icon
          type="swap"
          style="color: #4a9ff5"/>
        Compare Flink SQL
      </template>
      <a-form
        @submit="handleCompareOk"
        :form="formCompare">
        <a-form-item
          v-if="flinkSqlHistory.length > 1"
          label="Version"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 17} }"
          class="form-required">
          <a-select
            v-decorator="[ 'compare_sql' ]"
            placeholder="Please select the sql version to compare"
            mode="multiple"
            @change="handleChangeSQLCompact"
            class="version-select"
            :max-tag-count="2">
            <a-select-option
              v-for="(ver,index) in flinkSqlHistory"
              :value="ver.id"
              :key="`sql_${index}`">
              <div>
                <a-button
                  type="primary"
                  shape="circle"
                  size="small">
                  {{ ver.version }}
                </a-button>
                <a-tag
                  color="green"
                  style="margin-left: 5px;"
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
      <template
        slot="footer">
        <a-button
          @click="handleCompareCancel">
          Close
        </a-button>
        <a-button
          type="compare"
          @click="handleCompareOk">
          Compare
        </a-button>
      </template>
    </a-modal>

    <Mergely
      ref="mergely"
      @close="handleEditConfClose"
      @ok="handleEditConfOk"
      :visiable="controller.visiable.mergely" />

    <Different ref="different"/>

  </a-card>
</template>

<script>
  const Base64 = require('js-base64').Base64
  import Ellipsis from '@/components/Ellipsis'
  import { listConf } from '@api/project'
  import { get, update, checkName, name, readConf, upload } from '@api/application'
  import { history as confHistory, get as getVer, template, sysHadoopConf  } from '@api/config'
  import { get as getSQL, history as sqlhistory } from '@/api/flinkSql'
  import { mapActions, mapGetters } from 'vuex'
  import Mergely from './Mergely'
  import Different from './Different'
  import configOptions from './Option'
  import SvgIcon from '@/components/SvgIcon'
  import { toPomString } from './Pom'
  import {list as listFlinkEnv} from '@/api/flinkEnv'
  import {list as listFlinkCluster} from '@/api/flinkCluster'
  import {checkHadoop} from '@/api/setting'
  import {
    uploadJars as histUploadJars,
    k8sNamespaces as histK8sNamespaces,
    sessionClusterIds as histSessionClusterIds,
    flinkBaseImages as histFlinkBaseImages,
    flinkPodTemplates as histPodTemplates,
    flinkJmPodTemplates as histJmPodTemplates,
    flinkTmPodTemplates as histTmPodTemplates
  } from '@/api/flinkHistory'

  import {
    initFlinkSqlEditor,
    initPodTemplateEditor,
    disposeEditor,
    verifySQL,
    bigScreenOpen,
    bigScreenOk,
    applyPom,
    formatSql,
    updateDependency, checkPomScalaVersion
  } from './AddEdit'

  import {
    sysHosts,
    initPodTemplate,
    completeHostAliasToPodTemplate,
    extractHostAliasFromPodTemplate,
    previewHostAlias
  } from '@/api/flinkPodtmpl'



  export default {
    name: 'EditStreamX',
    components: { Mergely, Different, Ellipsis, SvgIcon },
    data() {
      return {
        strategy: 1,
        app: null,
        compareDisabled: true,
        compareVisible: false,
        selectTagCount: {
          count1: 1,
          count2: 2
        },
        resolveOrder: [
          { name: 'parent-first', order: 0 },
          { name: 'child-first', order: 1 }
        ],
        k8sRestExposedType: [
          {name: 'LoadBalancer', order: 0},
          {name: 'ClusterIP', order: 1},
          {name: 'NodePort', order: 2}
        ],
        executionModes: [
          {mode: 'remote (standalone)', value: 1, disabled: false},
          {mode: 'yarn application', value: 4, disabled: false},
          {mode: 'yarn session', value: 3, disabled: false},
          {mode: 'kubernetes session', value: 5, disabled: false},
          {mode: 'kubernetes application', value: 6, disabled: false},
          {mode: 'yarn per-job (deprecated, please use yarn-application mode)', value: 2, disabled: false}
        ],
        cpTriggerAction: [
          { name: 'alert', value: 1 },
          { name: 'restart', value: 2 }
        ],
        runMaxTagCount: 1,
        totalTagCount: 1,
        jmMaxTagCount: 1,
        tmMaxTagCount: 1,
        switchDefaultValue: true,
        compareConf: [],
        compareSQL: [],
        defaultConfigId: null,
        defaultFlinkSqlId: null,
        defaultOptions: {},
        isSetConfig: false,
        useSysHadoopConf: false,
        configOverride: null,
        configId: null,
        versionId: null,
        flinkEnvs: [],
        flinkClusters: [],
        configVersions: [],
        flinkSqlHistory: [],
        flinkSql: {},
        configSource: [],
        configItems: [],
        totalItems: [],
        jmMemoryItems: [],
        tmMemoryItems: [],
        form: null,
        formCompare: null,
        dependency: [],
        uploadJars: [],
        options: configOptions,
        optionsKeyMapping: {},
        optionsValueMapping: {},
        loading: false,
        submitting: false,
        executionMode: null,
        validateAgain: false,
        podTemplate: null,
        jmPodTemplate: null,
        tmPodTemplate: null,
        configuration: [
          { key: 'tc', name: ' time characteristic' },
          { key: 'cp', name: ' checkpoints' },
          { key: 'rs', name: ' restart strategy' },
          { key: 'sb', name: ' state backend' }
        ],
        controller: {
          activeTab: 'pom',
          podTemplateTab: 'pod-template',
          tagCount: {
            total: 1,
            run: 1,
            jm: 1,
            tm: 1
          },
          visiable: {
            mergely: false,
            bigScreen: false
          },
          modal: {
            destroyOnClose: true,
            bigScreen: {
              style: {
                height: null,
                padding: '5px'
              },
              title: 'Flink SQL'
            }
          },
          editor: {
            flinkSql: null,
            bigScreen: null,
            pom: null,
            podTemplate: null,
            jmPodTemplate: null,
            tmPodTemplate: null
          },
          flinkSql: {
            value: null,
            errorLine: null,
            errorColumn: null,
            errorMsg: null,
            errorStart: null,
            errorEnd: null,
            verified: false,
            success: true
          },
          dependency: {
            pom: new Map(),
            jar: new Map()
          },
          pom: {
            value: null,
            error: null,
            defaultValue: ''
          }
        },
        renderFlinkSql: true,
        selectedHistoryUploadJars: [],
        historyRecord: {
          uploadJars: [],
          k8sNamespace: [],
          k8sSessionClusterId: [],
          flinkImage: [],
          podTemplate:[],
          jmPodTemplate:[],
          tmPodTemplate:[]
        },
        podTemplateDrawer: {
          ptVisual: false,
          jmPtVisual: false,
          tmPtVisual: false
        },
        podTemplateHostAliasDrawer: {
          ptVisual: false,
          jmPtVisual: false,
          tmPtVisual: false,
        },
        hadoopConfDrawer: {
          visual: false,
          content: {}
        },
        sysHostsAlias: [],
        selectedPodTemplateHostAlias: [],
        hostAliasPreview: '',
      }
    },

    computed: {
      dynamicOptions() {
        return function(group) {
          return this.options.filter(x => x.group === group)
        }
      },
      hasOptions() {
        return function(items) {
          return this.options.filter(x => items.includes(x.key))
        }
      },
      myTheme() {
        return this.$store.state.app.theme
      },
      filteredHistoryUploadJarsOptions() {
        return this.historyRecord.uploadJars.filter(o =>
          !this.selectedHistoryUploadJars.includes(o) && !this.controller.dependency.jar.has(o))
      },
      filteredPodTemplateHostAliasOptions() {
        return this.sysHostsAlias.filter(o => !this.selectedPodTemplateHostAlias.includes(o))
      }

    },

    mounted() {
      const appId = this.applicationId()
      if (appId) {
        this.handleGet(appId)
        this.CleanAppId()
      } else {
        this.$router.back(-1)
      }
    },

    beforeMount() {
      this.form = this.$form.createForm(this)
      this.formCompare = this.$form.createForm(this)
      this.optionsKeyMapping = new Map()
      this.optionsValueMapping = new Map()
      this.options.forEach((item, index, array) => {
        console.table(index)
        this.optionsKeyMapping.set(item.key, item)
        this.optionsValueMapping.set(item.name, item.key)
        this.form.getFieldDecorator(item.key, { initialValue: item.defaultValue, preserve: true })
      })
      listFlinkEnv().then((resp)=>{
        this.flinkEnvs = resp.data
      })
      listFlinkCluster().then((resp)=>{
        this.flinkClusters = resp.data
      })
      // load history config records
      histUploadJars().then((resp) => {
        this.historyRecord.uploadJars = resp.data
      })
      histK8sNamespaces().then((resp) => {
        this.historyRecord.k8sNamespace = resp.data
      })
      histSessionClusterIds({'executionMode': 5}).then((resp) => {
        this.historyRecord.k8sSessionClusterId = resp.data
      })
      histFlinkBaseImages().then((resp) => {
        this.historyRecord.flinkImage = resp.data
      })
    },

    filters: {
      description(option) {
        if (option.unit) {
          return option.description + ' (Unit ' + option.unit + ')'
        } else {
          return option.description
        }
      }
    },

    methods: {
      ...mapActions(['CleanAppId']),
      ...mapGetters(['applicationId']),
      filterOption(input, option) {
        return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
      },

      handleGet(appId) {
        get({ id: appId }).then((resp) => {
          this.app = resp.data
          if (this.app.jobType === 2) {
            sqlhistory({ id: appId }).then((resp) => {
              this.flinkSqlHistory = resp.data
            })
          }
          if (this.app.config && this.app.config.trim() !== '') {
            this.configOverride = Base64.decode(this.app.config)
            this.isSetConfig = true
          }
          this.defaultOptions = JSON.parse(this.app.options || '{}')
          this.configId = this.app.configId
          this.executionMode = this.app.executionMode
          this.versionId = this.app.versionId || null
          this.defaultFlinkSqlId = this.app.sqlId || null
          this.handleReset()
          this.handleListConfVersion()
          this.handleConfList()
        }).catch((error) => {
          this.$message.error(error.message)
        })
      },

      handleCheckYarnSessionClusterId(rule, value, callback) {
        if (value === null || value === undefined || value === '') {
          callback(new Error('Yarn session clusterId is required'))
        } else {
          if (!value.startsWith('application')) {
            callback(new Error("Yarn session clusterId is invalid, clusterId must start with 'application'.Please check"))
          } else {
            callback()
          }
        }
      },

      handleFlinkVersion(id) {
        this.versionId = id
        this.scalaVersion = this.flinkEnvs.find(v => v.id === id).scalaVersion
        this.handleCheckPomScalaVersion()
      },

      handleChangeMode(mode) {
        this.executionMode = mode
        this.handleReset()
      },

      handleChangeConf(item) {
        this.configItems = item
      },

      handleChangeJmMemory(item) {
        this.jmMemoryItems = item
      },

      handleChangeTmMemory(item) {
        this.tmMemoryItems = item
      },

      handleChangeProcess(item) {
        this.totalItems = item
      },

      getExecutionCluster(executionMode){
        return this.flinkClusters.filter(o => o.executionMode === executionMode && o.clusterState === 1)
      },

      handleChangeNewConfig(confFile) {
        name({
          config: confFile
        }).then((resp) => {
          this.form.setFieldsValue({ 'jobName': resp.data })
        }).catch((error) => {
          this.$message.error(error.message)
        })
        readConf({
          config: confFile
        }).then((resp) => {
          this.configOverride = Base64.decode(resp.data)
        }).catch((error) => {
          this.$message.error(error.message)
        })
      },

      handleCheckExecMode(rule, value, callback) {
        if (value === null || value === undefined || value === '') {
          callback(new Error('Execution Mode is required'))
        } else {
          if (value === 2 || value === 3 || value === 4) {
            checkHadoop().then((resp) => {
              if (resp.data) {
                callback()
              } else {
                callback(new Error('Hadoop environment initialization failed, please check the environment settings'))
              }
            }).catch((err) => {
              callback(new Error('Hadoop environment initialization failed, please check the environment settings'))
            })
          }
        }
      },

      handleCheckJobName(rule, value, callback) {
        if (!value) {
          callback(new Error('application name is required'))
        } else {
          checkName({
            id: this.app.id,
            jobName: value
          }).then((resp) => {
            const exists = parseInt(resp.data)
            if (exists === 0) {
              callback()
            } else if (exists === 1) {
              callback(new Error('application name must be unique. The application name already exists'))
            } else if (exists === 2) {
              callback(new Error('The application name is already running in yarn,cannot be repeated. Please check'))
            } else if (exists === 3) {
              callback(new Error('The application name is already running in k8s,cannot be repeated. Please check'))
            } else {
              callback(new Error('The application name is invalid.characters must be (Chinese|English|"-"|"_"),two consecutive spaces cannot appear.Please check'))
            }
          })
        }
      },

      handleCheckCheckPoint (rule, value, callback) {
        const cpMaxFailureInterval =  this.form.getFieldValue('cpMaxFailureInterval') || null
        const cpFailureRateInterval = this.form.getFieldValue('cpFailureRateInterval') || null
        const cpFailureAction = this.form.getFieldValue('cpFailureAction') || null
        if( cpMaxFailureInterval != null && cpFailureRateInterval != null && cpFailureAction != null ) {
          callback()
          if (!this.validateAgain) {
            this.validateAgain = true
            this.form.validateFields(['cpMaxFailureInterval', 'cpFailureRateInterval','cpFailureAction'])
            this.validateAgain = false
          }
        } else if(cpMaxFailureInterval == null && cpFailureRateInterval == null && cpFailureAction == null) {
          callback()
          if (!this.validateAgain) {
            this.validateAgain = true
            this.form.validateFields(['cpMaxFailureInterval', 'cpFailureRateInterval','cpFailureAction'])
            this.validateAgain = false
          }
        } else {
          callback(new Error('checkPoint failure options must be all required or all empty'))
          if (!this.validateAgain) {
            this.validateAgain = true
            this.form.validateFields(['cpMaxFailureInterval', 'cpFailureRateInterval','cpFailureAction'])
            this.validateAgain = false
          }
        }
      },

      handleCheckAlertEmail(rule, value, callback) {
        const cpMaxFailureInterval =  this.form.getFieldValue('cpMaxFailureInterval')
        const cpFailureRateInterval = this.form.getFieldValue('cpFailureRateInterval')
        const cpFailureAction = this.form.getFieldValue('cpFailureAction')

        if( cpMaxFailureInterval != null && cpFailureRateInterval != null && cpFailureAction != null ) {
          if( cpFailureAction === 1) {
            const alertEmail = this.form.getFieldValue('alertEmail')
            if (alertEmail == null || alertEmail.trim() === '') {
              callback(new Error('checkPoint Failure trigger is alert,alertEmail must not be empty'))
            } else {
              callback()
            }
          } else {
            callback()
          }
        } else {
          callback()
        }
      },

      handleFormatSql() {
        formatSql(this)
      },

      handleVerifySql() {
        verifySQL(this)
      },

      handleBigScreenOpen() {
        bigScreenOpen(this)
      },

      handleBigScreenOk() {
        bigScreenOk(this)
      },

      handleBigScreenClose() {
      },

      handleInitDependency() {
        this.controller.dependency.jar = new Map()
        this.controller.dependency.pom = new Map()
        this.handleDependencyJsonToPom(this.flinkSql.dependency,this.controller.dependency.pom,this.controller.dependency.jar)
        this.handleUpdateDependency()
      },

      handleCheckPomScalaVersion() {
        checkPomScalaVersion(this)
      },

      handleDependencyJsonToPom (json,pomMap,jarMap) {
        if (json != null && json.trim() !== '') {
          const deps = JSON.parse(json)
          const pom = deps.pom
          if (pom && pom.length > 0) {
            pom.forEach(x => {
              const groupId = x.groupId
              const artifactId = x.artifactId
              const version = x.version
              const exclusions = x.exclusions || []

              const id = groupId + '_' + artifactId
              const mvnPom = {
                'groupId': groupId,
                'artifactId': artifactId,
                'version': version
              }
              if (exclusions != null && exclusions.length > 0) {
                exclusions.forEach(e => {
                  if (e != null && e.length > 0) {
                    const e_group = e.groupId
                    const e_artifact = e.artifactId
                    mvnPom.exclusions.put({
                      'groupId': e_group,
                      'artifactId': e_artifact
                    })
                  }
                })
              }
              pomMap.set(id, mvnPom)
            })
          }
          const jar = deps.jar
          if (jar != null && jar.length > 0) {
            jar.forEach(x => {
              jarMap.set(x, x)
            })
          }
        }
      },

      handleSQLConf(checked) {
        if (checked) {
          if (this.configOverride != null) {
            this.controller.visiable.mergely = true
            this.$refs.mergely.set(this.configOverride)
          } else {
            template({}).then((resp) => {
              const sqlJobConfig = Base64.decode(resp.data)
              this.controller.visiable.mergely = true
              this.$refs.mergely.set(sqlJobConfig)
            }).catch((error) => {
              this.$message.error(error.message)
            })
          }
        } else {
          this.controller.visiable.mergely = false
          this.configOverride = null
          this.isSetConfig = false
        }
      },

      handleApplyPom() {
        applyPom(this)
      },

      handleK8sPodTemplateEditor(){
        this.$nextTick(() => {
          initPodTemplateEditor(this)
        })
      },

      handleEditPom(pom) {
        const pomString = toPomString(pom)
        this.activeTab = 'pom'
        this.controller.editor.pom.getModel().setValue(pomString)
      },

      handleUploadJar(info) {
        const status = info.file.status
        if (status === 'done') {
          this.loading = false
        } else if (status === 'error') {
          this.loading = false
          this.$message.error(`${info.file.name} file upload failed.`)
        }
      },

      handleBeforeUpload(file) {
        if (file.type !== 'application/java-archive') {
          if (!/\.(jar|JAR)$/.test(file.name)) {
            this.loading = false
            this.$message.error('Only jar files can be uploaded! please check your file.')
            return false
          }
        }
        this.loading = true
        return true
      },

      handleSearchHistoryUploadJars(selectedItems) {
        this.selectedHistoryUploadJars = selectedItems
      },

      addHistoryUploadJar(item) {
        this.controller.dependency.jar.set(item, item)
        this.handleUpdateDependency()
      },

      deleteHistoryUploadJar(item){
        this.controller.dependency.jar.delete(item)
        this.handleUpdateDependency()
      },

      handleCustomRequest(data) {
        const formData = new FormData()
        formData.append('file', data.file)
        upload(formData).then((response) => {
          this.loading = false
          this.controller.dependency.jar.set(data.file.name, data.file.name)
          this.handleUpdateDependency()
        }).catch((error) => {
          this.$message.error(error.message)
          this.loading = false
        })
      },

      handleRemovePom(pom) {
        const id = pom.groupId + '_' + pom.artifactId
        this.controller.dependency.pom.delete(id)
        this.handleUpdateDependency()
      },

      handleRemoveJar(jar) {
        this.controller.dependency.jar.delete(jar)
        this.selectedHistoryUploadJars.splice(this.selectedHistoryUploadJars.indexOf(jar), 1)
        this.handleUpdateDependency()
      },

      handleUpdateDependency() {
        updateDependency(this)
      },

      handleChangeStrategy(v) {
        this.strategy = parseInt(v)
      },

      handleChangeConfig(v) {
        getVer({ id: v }).then((resp) => {
          this.configOverride = Base64.decode(resp.data.content)
          this.configId = resp.data.id
        })
      },

      handleChangeSQL(v) {
        getSQL({ id: v }).then((resp) => {
          this.flinkSql = resp.data
          this.controller.flinkSql.value = Base64.decode(this.flinkSql.sql)
          this.controller.editor.flinkSql.getModel().setValue(this.controller.flinkSql.value)
          this.handleInitDependency()
        })
      },

      handleEditConfig() {
        this.controller.visiable.mergely = true
        this.$refs.mergely.set(this.configOverride)
      },

      handleEditConfClose() {
        this.controller.visiable.mergely = false
        if (this.configOverride == null) {
          this.isSetConfig = false
        }
      },

      handleEditConfOk(value) {
        if (value == null || value.trim() === '') {
          this.isSetConfig = false
          this.configOverride = null
        } else {
          this.isSetConfig = true
          this.configOverride = value
        }
      },

      handleSubmit(e) {
        e.preventDefault()
        this.form.validateFields((err, values) => {
          if (!err) {
            if (!this.submitting) {
              if (this.app.jobType === 1) {
                this.handleSubmitCustomJob(values)
              } else {
                if (this.app.jobType === 2) {
                  verifySQL(this,(success) => {
                    if (success) {
                      this.handleSubmitSQL(values)
                    }
                  })
                }
              }
            }
          }
        })
      },

      handleYarnQueue(values) {
        if ( this.executionMode === 4 ) {
          const queue = values['yarnQueue']
          if (queue != null && queue !== '' && queue !== undefined) {
            return queue
          }
          return null
        }
      },

      handleFormValue(values) {
        const options = {}
        for (const k in values) {
          const v = values[k]
          if (v != null && v !== '' && v !== undefined) {
            if (k === 'parallelism') {
              options['parallelism.default'] = v
            } else if (k === 'slot') {
              options['taskmanager.numberOfTaskSlots'] = v
            } else {
              if (this.configItems.includes(k)) {
                options[k] = v
              } else if (this.totalItems.includes(k) || this.jmMemoryItems.includes(k) || this.tmMemoryItems.includes(k)) {
                const opt = this.optionsKeyMapping.get(k)
                const unit = opt['unit'] || ''
                const name = opt['name']
                if (typeof v === 'string') {
                  options[name] = v.replace(/[k|m|g]b$/g, '') + unit
                } else if (typeof v === 'number') {
                  options[name] = v + unit
                } else {
                  options[name] = v
                }
              }
            }
          }
        }
        return options
      },

      handleSubmitCustomJob(values) {
        const options = this.handleFormValue(values)
        const format = this.strategy === 1 ? this.app.format : (this.form.getFieldValue('config').endsWith('.properties') ? 2 : 1)
        let config = this.configOverride || this.app.config
        if (config != null && config.trim() !== '') {
          config = Base64.encode(config)
        } else {
          config = null
        }
        const configId = this.strategy === 1 ? this.configId : null
        const params = {
          id: this.app.id,
          jobName: values.jobName,
          format: format,
          configId: configId,
          versionId: values.versionId,
          config: config,
          args: values.args,
          options: JSON.stringify(options),
          yarnQueue: this.handleYarnQueue(values),
          dynamicOptions: values.dynamicOptions,
          cpMaxFailureInterval: values.cpMaxFailureInterval || null,
          cpFailureRateInterval: values.cpFailureRateInterval || null,
          cpFailureAction: values.cpFailureAction || null,
          resolveOrder: values.resolveOrder,
          k8sRestExposedType: values.k8sRestExposedType,
          executionMode: values.executionMode,
          restartSize: values.restartSize,
          alertEmail: values.alertEmail || null,
          description: values.description,
          k8sNamespace: values.k8sNamespace || null,
          clusterId: values.clusterId || null,
          flinkClusterId: values.flinkClusterId || null,
          flinkImage: values.flinkImage || null,
          yarnSessionClusterId: values.yarnSessionClusterId || null
        }
        if (params.executionMode === 6) {
          params.k8sPodTemplate = this.podTemplate
          params.k8sJmPodTemplate = this.jmPodTemplate
          params.k8sTmPodTemplate = this.tmPodTemplate
          params.k8sHadoopIntegration = this.useSysHadoopConf
        }
        this.handleUpdateApp(params)
      },

      handleSubmitSQL(values) {
        const options = this.handleFormValue(values)
        //触发一次pom确认操作.
        this.handleApplyPom()
        // common params...
        const dependency = {}
        if (this.dependency !== null && this.dependency.length > 0) {
          dependency.pom = this.dependency
        }
        if (this.uploadJars != null && this.uploadJars.length > 0) {
          dependency.jar = this.uploadJars
        }

        let config = this.configOverride
        if (config != null && config.trim() !== '') {
          config = Base64.encode(config)
        } else {
          config = null
        }

        const params = {
          id: this.app.id,
          sqlId: this.defaultFlinkSqlId || null,
          flinkSql: this.controller.flinkSql.value,
          config: config,
          format: this.isSetConfig ? 1 : null,
          versionId: values.versionId,
          jobName: values.jobName,
          args: values.args || null,
          dependency: dependency.pom === undefined && dependency.jar === undefined ? null : JSON.stringify(dependency),
          options: JSON.stringify(options),
          yarnQueue: this.handleYarnQueue(values),
          cpMaxFailureInterval: values.cpMaxFailureInterval || null,
          cpFailureRateInterval: values.cpFailureRateInterval || null,
          cpFailureAction: values.cpFailureAction || null,
          dynamicOptions: values.dynamicOptions || null,
          resolveOrder: values.resolveOrder,
          k8sRestExposedType: values.k8sRestExposedType,
          restartSize: values.restartSize,
          alertEmail: values.alertEmail|| null,
          executionMode: values.executionMode,
          description: values.description || null,
          k8sNamespace: values.k8sNamespace || null,
          clusterId: values.clusterId || null,
          flinkClusterId: values.flinkClusterId || null,
          flinkImage: values.flinkImage || null,
          yarnSessionClusterId: values.yarnSessionClusterId || null
        }
        if (params.executionMode === 6) {
          params.k8sPodTemplate = this.podTemplate
          params.k8sJmPodTemplate = this.jmPodTemplate
          params.k8sTmPodTemplate = this.tmPodTemplate
          params.k8sHadoopIntegration = this.useSysHadoopConf
        }
        this.handleUpdateApp(params)
      },

      handleUpdateApp(params) {
        this.submitting = true
        update(params).then((resp) => {
          this.submitting = false
          const updated = resp.data
          if (updated) {
            this.$router.push({ path: '/flink/app' })
          } else {
            console.log(updated)
          }
        }).catch((error) => {
          this.submitting = false
          this.$message.error(error.message)
        })
      },

      handleListConfVersion() {
        confHistory({ id: this.app.id }).then((resp) => {
          resp.data.forEach((value, index) => {
            if (value.effective) {
              this.defaultConfigId = value.id
            }
          })
          this.configVersions = resp.data
        })
      },

      handleConfList() {
        listConf({
          id: this.app.projectId,
          module: this.app.module
        }).then((resp) => {
          this.configSource = resp.data
        }).catch((error) => {
          this.$message.error(error.message)
        })
      },

      handleChangeConfCompact() {
        this.$nextTick(() => {
          const conf = this.form.getFieldValue('compare_conf')
          if (conf.length > 2) {
            while (conf.length > 2) {
              conf.shift()
            }
            this.form.setFieldsValue({ 'compare_conf': conf })
          }
          this.compareConf = conf
          this.compareDisabled = conf.length !== 2
        })
      },

      handleChangeSQLCompact() {
        this.$nextTick(() => {
          const sqls = this.formCompare.getFieldValue('compare_sql')
          if (sqls.length > 2) {
            while (sqls.length > 2) {
              sqls.shift()
            }
            this.formCompare.setFieldsValue({ 'compare_sql': sqls })
          }
          this.compareSQL = sqls
        })
      },

      handleCompactConf() {
        getVer({
          id: this.compareConf[0]
        }).then((resp) => {
          const conf1 = Base64.decode(resp.data.content)
          const ver1 = resp.data.version
          getVer({
            id: this.compareConf[1]
          }).then((resp) => {
            const conf2 = Base64.decode(resp.data.content)
            const ver2 = resp.data.version
            this.confVisiable = true
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

      handleCompactSQL() {
        this.compareVisible = true
      },

      handleCompareCancel () {
        this.compareVisible = false
      },

      handleCompareOk() {
        getSQL({ id: this.compareSQL.join(',') }).then((resp) => {
          const obj1 = resp.data[0]
          const obj2 = resp.data[1]
          const sql1 = Base64.decode(obj1.sql)
          const sql2 = Base64.decode(obj2.sql)

          const pomMap1 = new Map()
          const jarMap1 = new Map()
          this.handleDependencyJsonToPom(obj1.dependency,pomMap1,jarMap1)
          let pom1 = ''
          let jar1 = ''
          pomMap1.forEach((v,k,map)=> pom1 += toPomString(v)  + '\n\n')
          jarMap1.forEach((v,k,map) => jar1 += v + '\n')

          const pomMap2 = new Map()
          const jarMap2 = new Map()
          this.handleDependencyJsonToPom(obj2.dependency,pomMap2,jarMap2)
          let pom2 = ''
          let jar2 = ''
          pomMap2.forEach((v,k,map) => pom2 += toPomString(v) + '\n\n')
          jarMap2.forEach((v,k,map) => jar2 += v + '\n')

          this.handleCompareCancel()

          this.$refs.different.different([{
              name: 'Flink SQL',
              format: 'sql',
              original: sql1,
              modified: sql2,
            }, {
              name: 'Dependency',
              format: 'xml',
              original: pom1,
              modified: pom2,
            }, {
              name: ' Jar ',
              format: 'text',
              original: jar1,
              modified: jar2,
            }],
            obj1.version,
            obj2.version
          )
        })
      },

      handleUseSysHadoopConf(value) {
        this.useSysHadoopConf = value
      },

      handleSelectHistoryK8sNamespace(value) {
        this.form.setFieldsValue({'k8sNamespace': value})
      },

      handleSelectHistoryK8sSessionClusterId(value) {
        this.form.setFieldsValue({'clusterId': value})
      },

      handleSelectHistoryFlinkImage(value) {
        this.form.setFieldsValue({'flinkImage': value})
      },

      showPodTemplateDrawer(visualType) {
        this.podTemplateDrawer[visualType] = true
        switch (visualType) {
          case 'ptVisual':
            if (this.historyRecord.podTemplate == null || this.historyRecord.podTemplate.length === 0) {
              histPodTemplates().then((resp) => {
                this.historyRecord.podTemplate = resp.data
              })
            }
            break
          case 'jmPtVisual':
            if (this.historyRecord.jmPodTemplate == null || this.historyRecord.jmPodTemplate.length === 0) {
              histJmPodTemplates().then((resp) => {
                this.historyRecord.jmPodTemplate = resp.data
              })
            }
            break
          case 'tmPtVisual':
            if (this.historyRecord.tmPodTemplate == null || this.historyRecord.tmPodTemplate.length === 0){
              histTmPodTemplates().then((resp) => {
                this.historyRecord.tmPodTemplate = resp.data
              })
            }
            break
        }
      },

      closePodTemplateDrawer(visualType) {
        this.podTemplateDrawer[visualType] = false
      },

      handleChoicePodTemplate(visualType, content) {
        switch (visualType) {
          case 'ptVisual':
            this.podTemplate = content
            this.controller.editor.podTemplate.setValue(content)
            break
          case 'jmPtVisual':
            this.jmPodTemplate = content
            this.controller.editor.jmPodTemplate.setValue(content)
            break
          case 'tmPtVisual':
            this.tmPodTemplate = content
            this.controller.editor.tmPodTemplate.setValue(content)
            break
        }
        this.closePodTemplateDrawer(visualType)
      },

      showSysHadoopConfDrawer() {
        this.hadoopConfDrawer.visual = true
        if (this.hadoopConfDrawer.content == null
          || Object.keys(this.hadoopConfDrawer.content).length === 0
          || this.hadoopConfDrawer.content.size === 0) {
          sysHadoopConf().then((resp) => {
            this.hadoopConfDrawer.content = resp.data
          })
        }
      },

      closeSysHadoopConfDrawer(){
        this.hadoopConfDrawer.visual = false
      },

      handleGetInitPodTemplate(visualType) {
        initPodTemplate().then((resp) => {
          const content = resp.data
          if (content != null && content !== '') {
            switch (visualType) {
              case 'ptVisual':
                this.podTemplate = content
                this.controller.editor.podTemplate.setValue(content)
                break
              case 'jmPtVisual':
                this.jmPodTemplate = content
                this.controller.editor.jmPodTemplate.setValue(content)
                break
              case 'tmPtVisual':
                this.tmPodTemplate = content
                this.controller.editor.tmPodTemplate.setValue(content)
                break
            }
          }
        }).catch((error) => {
          this.$message.error(error.message)
        })
      },

      showTemplateHostAliasDrawer(visualType) {
        this.podTemplateHostAliasDrawer[visualType] = true
        sysHosts().then((resp) => {
          this.sysHostsAlias = resp.data
        })
        let tmplContent = ''
        switch (visualType) {
          case 'ptVisual':
            tmplContent = this.podTemplate
            break
          case 'jmPtVisual':
            tmplContent = this.jmPodTemplate
            break
          case 'tmPtVisual':
            tmplContent = this.tmPodTemplate
            break
        }
        if (tmplContent !== '') {
          const param = {}
          param['podTemplate'] = tmplContent
          extractHostAliasFromPodTemplate(param).then((resp) => {
            this.selectedPodTemplateHostAlias = resp.data
            this.handleRefreshHostAliasPreview()
          }).catch(err => {
            this.selectedPodTemplateHostAlias = []
          })
        }
      },

      closeTemplateHostAliasDrawer(visualType) {
        this.podTemplateHostAliasDrawer[visualType] = false
        this.selectedPodTemplateHostAlias = []
        this.hostAliasPreview = ''
      },

      handleSelectedTemplateHostAlias(items) {
        this.selectedPodTemplateHostAlias = items
        this.handleRefreshHostAliasPreview()
      },

      handleSubmitHostAliasToPodTemplate(visualType) {
        const param = {
          hosts: this.selectedPodTemplateHostAlias.join(','),
        }
        switch (visualType) {
          case 'ptVisual':
            param['podTemplate'] = this.podTemplate
            break
          case 'jmPtVisual':
            param['podTemplate'] = this.jmPodTemplate
            break
          case 'tmPtVisual':
            param['podTemplate'] = this.tmPodTemplate
            break
        }
        completeHostAliasToPodTemplate(param).then((resp) => {
          const content = resp.data
          if (content != null && content !== '') {
            switch (visualType) {
              case 'ptVisual':
                this.podTemplate = content
                this.controller.editor.podTemplate.setValue(content)
                break
              case 'jmPtVisual':
                this.jmPodTemplate = content
                this.controller.editor.jmPodTemplate.setValue(content)
                break
              case 'tmPtVisual':
                this.tmPodTemplate = content
                this.controller.editor.tmPodTemplate.setValue(content)
                break
            }
          }
        }).catch((error) => {
          this.$message.error(error.message)
        })
        this.closeTemplateHostAliasDrawer(visualType)
      },

      handleRefreshHostAliasPreview() {
        previewHostAlias({hosts: this.selectedPodTemplateHostAlias.join(',')})
          .then((resp) => {
            this.hostAliasPreview = resp.data
          })
      },

      handleReset() {
        this.$nextTick(() => {
          this.form.setFieldsValue({
            'jobName': this.app.jobName,
            'args': this.app.args,
            'description': this.app.description,
            'dynamicOptions': this.app.dynamicOptions,
            'resolveOrder': this.app.resolveOrder,
            'versionId': this.app.versionId || null,
            'k8sRestExposedType': this.app.k8sRestExposedType,
            'executionMode': this.executionMode || this.app.executionMode,
            'yarnQueue': this.app.yarnQueue,
            'restartSize': this.app.restartSize,
            'alertEmail': this.app.alertEmail,
            'cpMaxFailureInterval': this.app.cpMaxFailureInterval,
            'cpFailureRateInterval': this.app.cpFailureRateInterval,
            'cpFailureAction': this.app.cpFailureAction,
            'clusterId': this.app.clusterId,
            'flinkClusterId': this.app.flinkClusterId,
            'flinkImage': this.app.flinkImage,
            'k8sNamespace': this.app.k8sNamespace,
            'resource': this.app.resourceFrom,
            'yarnSessionClusterId': this.app.yarnSessionClusterId
          })
        })

        this.handleInitEditor()

        let parallelism = null
        let slot = null
        this.totalItems = []
        this.jmMemoryItems = []
        this.tmMemoryItems = []
        const fieldValueOptions = {}
        for (const k in this.defaultOptions) {
          const v = this.defaultOptions[k]
          const key = this.optionsValueMapping.get(k)
          fieldValueOptions[key] = v
          if (k === 'jobmanager.memory.flink.size' || k === 'taskmanager.memory.flink.size' || k === 'jobmanager.memory.process.size' || k === 'taskmanager.memory.process.size') {
            this.totalItems.push(key)
          } else {
            if (k.startsWith('jobmanager.memory.')) {
              this.jmMemoryItems.push(key)
            }
            if (k.startsWith('taskmanager.memory.')) {
              this.tmMemoryItems.push(key)
            }
            if (k === 'taskmanager.numberOfTaskSlots') {
              slot = parseInt(v)
            }
            if (k === 'parallelism.default') {
              parallelism = parseInt(v)
            }
          }
        }
        this.$nextTick(() => {
          this.form.setFieldsValue({ 'parallelism': parallelism })
          this.form.setFieldsValue({ 'slot': slot })
          this.form.setFieldsValue({ 'totalOptions': this.totalItems })
          this.form.setFieldsValue({ 'jmOptions': this.jmMemoryItems })
          this.form.setFieldsValue({ 'tmOptions': this.tmMemoryItems })
          this.form.setFieldsValue(fieldValueOptions)
        })
      },

      handleInitEditor() {
        disposeEditor(this)
        this.$nextTick(()=> {
          if (this.app.jobType === 2) {
            this.flinkSql.sql = this.app.flinkSql || null
            this.flinkSql.dependency = this.app.dependency || null
            initFlinkSqlEditor(this, this.controller.flinkSql.value || Base64.decode(this.flinkSql.sql))
            this.handleInitDependency()
          }
          this.selectedHistoryUploadJars = []
          if (this.executionMode === 6 || this.app.executionMode === 6) {
            this.podTemplate = this.app.k8sPodTemplate
            this.jmPodTemplate = this.app.k8sJmPodTemplate
            this.tmPodTemplate = this.app.k8sTmPodTemplate
            initPodTemplateEditor(this)
            this.useSysHadoopConf = this.app.k8sHadoopIntegration
          }
        })
      }
    },

    watch: {
      myTheme() {
        if (this.app.jobType === 2) {
          this.controller.editor.flinkSql.updateOptions({
            theme: this.ideTheme()
          })
        }
        if (this.controller.editor.podTemplate) {
          this.controller.editor.podTemplate.updateOptions({
            theme: this.ideTheme()
          })
        }
        if (this.controller.editor.jmPodTemplate) {
          this.controller.editor.jmPodTemplate.updateOptions({
            theme: this.ideTheme()
          })
        }
        if (this.controller.editor.tmPodTemplate) {
          this.controller.editor.tmPodTemplate.updateOptions({
            theme: this.ideTheme()
          })
        }
        this.$refs.mergely.theme()
        this.$refs.different.theme()
      }
    },
  }
</script>
<style lang='less'>
  @import "AddEdit";
</style>
