<template>
  <a-card
    :body-style="{padding: '24px 32px'}"
    :bordered="false"
    class="app_controller">
    <a-form
      @submit="handleSubmit"
      :form="form">
      <a-form-item
        label="Development Mode"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-select
          placeholder="Please select Development Mode"
          @change="handleChangeJobType"
          v-decorator="[ 'jobType' , {rules: [{ required: true, message: 'Job Type is required' }]} ]">
          <a-select-option
            value="customcode">
            <a-icon type="code" style="color: #108ee9"/>
            Custom Code
          </a-select-option>
          <a-select-option
            value="sql">
            <svg-icon name="fql" style="color: #108ee9"/>
            Flink SQL
          </a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item
        label="Execution Mode"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-select
          placeholder="Execution Mode"
          v-decorator="[ 'executionMode', {rules: [{ required: true, validator: handleCheckExecMode }] }]"
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
            :key="`version_${index}`"
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
            allowClear
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

      <template v-if="executionMode === 5|| executionMode === 6">
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
                <a-menu-item
                  v-for="item in historyRecord.k8sNamespace"
                  :key="item"
                  @click="handleSelectHistoryK8sNamespace(item)"
                  style="padding-right: 60px">
                  <a-icon type="plus-circle"/>
                  {{ item }}
                </a-menu-item>
              </a-menu>
              <a-icon type="history"/>
            </a-dropdown>
          </a-input>
        </a-form-item>

        <a-form-item
          v-if="executionMode === 6"
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
          v-if="executionMode === 5"
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

      <template v-if="executionMode === 6">
        <a-form-item
          label="Flink Base Docker Image"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
          <a-input
            type="text"
            placeholder="Please enter the tag of Flink base docker image, such as: flink:1.13.0-scala_2.11-java8"
            allowClear
            v-decorator="[ 'flinkImage', {rules: [{ required: true, message: 'Flink Base Docker Image is required' }] }]">
            <a-dropdown slot="addonAfter" placement="bottomRight">
              <a-menu slot="overlay" trigger="['click', 'hover']">
                <a-menu-item
                  v-for="item in historyRecord.flinkImage"
                  :key="item"
                  @click="handleSelectHistoryFlinkImage(item)"
                  style="padding-right: 60px">
                  <a-icon type="plus-circle"/>
                  {{ item }}
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

      <template v-if="jobType === 'sql'">
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

              <template v-if="executionMode == 5 || executionMode == 6">
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
                :customRequest="handleCustomDepsRequest"
                :beforeUpload="handleBeforeUpload">
                <div style="height: 266px">
                  <p
                    class="ant-upload-drag-icon"
                    style="padding-top: 40px">
                    <a-icon
                      type="inbox"
                      :style="{ fontSize: '70px' }"/>
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
              type="info"
              @click="handleEditPom(value)">
              <template slot="message">
                <a-space @click="handleEditPom(value)" class="tag-dependency-pom">
                  <a-tag class="tag-dependency" color="#2db7f5">POM</a-tag>
                  {{ value.artifactId }}-{{ value.version }}.jar
                  <a-icon type="close" class="icon-close" @click="handleRemovePom(value)"/>
                </a-space>
              </template>
            </a-alert>
            <a-alert
              class="dependency-item"
              v-for="(value, index) in uploadJars"
              :key="`upload_jars_${index}`"
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
          v-show="executionMode !== 5 && executionMode !== 6">
          <a-switch
            checked-children="ON"
            un-checked-children="OFF"
            v-model="isSetConfig"
            v-decorator="[ 'config' ]"/>
          <a-icon
            v-if="isSetConfig"
            type="setting"
            style="margin-left: 10px"
            theme="twoTone"
            two-tone-color="#4a9ff5"
            @click="handleSQLConf(true)"/>
        </a-form-item>
      </template>

      <template v-else>
        <a-form-item
          label="Resource From"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
          <a-select
            placeholder="Please select resource from"
            @change="handleChangeResourceFrom"
            v-decorator="[ 'resourceFrom' , {rules: [{ required: true, message: 'resource from is required' }]} ]">
            <a-select-option value="cvs">
              <svg-icon role="img" name="github"/>
              CICD <span class="gray">(build from CVS)</span>
            </a-select-option>
            <a-select-option value="upload">
              <svg-icon role="img" name="upload"/>
              Upload <span class="gray">(upload local job)</span>
            </a-select-option>
          </a-select>
        </a-form-item>

        <template v-if="resourceFrom === 'upload'">
          <a-form-item
            label="Upload Job Jar"
            :label-col="{lg: {span: 5}, sm: {span: 7}}"
            :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
            <a-upload-dragger
              name="file"
              :multiple="true"
              @change="handleUploadJob"
              :showUploadList="loading"
              :customRequest="handleCustomJobRequest"
              :beforeUpload="handleBeforeUpload">
              <div style="height: 266px">
                <p
                  class="ant-upload-drag-icon"
                  style="padding-top: 40px">
                  <a-icon
                    type="inbox"
                    :style="{ fontSize: '70px' }"/>
                </p>
                <p
                  class="ant-upload-text"
                  style="height: 45px">
                  Click or drag jar to this area to upload
                </p>
                <p
                  class="ant-upload-hint"
                  style="height: 45px">
                  Support for a single upload. You can upload a local jar here to support for current Job.
                </p>
              </div>
            </a-upload-dragger>

            <a-alert
              v-show="uploadJar"
              class="uploadjar-box"
              type="info">
              <template slot="message">
                <span class="tag-dependency-pom">
                  {{ uploadJar }}
                </span>
              </template>
            </a-alert>

          </a-form-item>

          <a-form-item
            label="Program Main"
            :label-col="{lg: {span: 5}, sm: {span: 7}}"
            :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
            <a-input
              type="text"
              allowClear
              placeholder="Please enter Main class"
              v-decorator="[ 'mainClass', {rules: [{ required: true, message: 'Program Main is required' }]} ]">
            </a-input>
          </a-form-item>
        </template>
        <template v-else>
          <a-form-item
            label="Project"
            :label-col="{lg: {span: 5}, sm: {span: 7}}"
            :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
            <a-select
              show-search
              option-filter-prop="children"
              :filter-option="filterOption"
              placeholder="Please select Project"
              @change="handleChangeProject"
              v-decorator="[ 'project', {rules: [{ required: true }]} ]">
              <a-select-option
                v-for="p in projectList"
                :key="p.id"
                :value="p.id">
                {{ p.name }}
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item
            label="Module"
            :label-col="{lg: {span: 5}, sm: {span: 7}}"
            :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
            <a-select
              label="Module"
              show-search
              option-filter-prop="children"
              :filter-option="filterOption"
              placeholder="Please select module of this project"
              @change="handleChangeModule"
              v-decorator="[ 'module', {rules: [{ required: true }]} ]">
              <a-select-option
                v-for="name in moduleList"
                :key="name"
                :value="name">
                {{ name }}
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item
            label="Application Type"
            :label-col="{lg: {span: 5}, sm: {span: 7}}"
            :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
            <a-select
              placeholder="Please select Application type"
              @change="handleChangeAppType"
              v-decorator="[ 'appType', {rules: [{ required: true, message: 'Application Type is required'}]} ]">
              <a-select-option
                value="1">
                StreamX Flink
              </a-select-option>
              <a-select-option
                value="2">
                Apache Flink
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item
            v-if="appType === 2"
            label="Program Jar"
            :label-col="{lg: {span: 5}, sm: {span: 7}}"
            :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
            <a-select
              placeholder="Please select Program Jar"
              @change="handleChangeJars"
              v-decorator="[ 'jar', {rules: [{ required: true,message: 'Program Jar is required' }] }]">
              <a-select-option
                v-for="(jar,index) in jars"
                :key="`program_jar_${index}`"
                :value="jar">
                {{ jar }}
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item
            v-if="appType === 2"
            label="Program Main"
            :label-col="{lg: {span: 5}, sm: {span: 7}}"
            :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
            <a-input
              type="text"
              allowClear
              placeholder="Please enter Main class"
              v-decorator="[ 'mainClass', {rules: [{ required: true, message: 'Program Main is required' }]} ]">
            </a-input>
          </a-form-item>

          <a-form-item
            v-if="appType === 1"
            label="Application conf"
            :label-col="{lg: {span: 5}, sm: {span: 7}}"
            :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
            <a-tree-select
              :dropdown-style="{ maxHeight: '400px', overflow: 'auto' }"
              :tree-data="configSource"
              placeholder="Please select config"
              tree-default-expand-all
              @change="handleJobName"
              v-decorator="[ 'config', {rules: [{ required: true, validator: handleCheckConfig }]} ]">
              <template
                slot="suffixIcon"
                v-if="this.form.getFieldValue('config')">
                <a-icon
                  type="setting"
                  theme="twoTone"
                  two-tone-color="#4a9ff5"
                  @click.stop="handleEditConfig()"
                  title="edit config"/>
              </template>
            </a-tree-select>
          </a-form-item>

        </template>
      </template>

      <a-form-item
        label="Use System Hadoop Conf"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }"
        v-show="executionMode === 6">
        <a-switch
          checked-children="ON"
          un-checked-children="OFF"
          v-model="useSysHadoopConf"
          @change="handleUseSysHadoopConf"/>
        <a-space>
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
              @click="showSysHadoopConfDrawer">view
            </a-button>
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
          placeholder="Please enter jobName"
          allowClear
          v-decorator="['jobName',{ rules: [{ validator: handleCheckJobName,required: true}]}]"/>
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
          v-decorator="['parallelism']"/>
      </a-form-item>

      <a-form-item
        label="Task Slots"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-input-number
          :min="1"
          :step="1"
          placeholder="Number of slots per TaskManager"
          v-decorator="['slot']"/>
      </a-form-item>

      <a-form-item
        label="Kubernetes Pod Template"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }"
        v-show="executionMode === 6">
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
        v-show="executionMode !== 5 && executionMode !== 6">
        <a-input-number
          :min="1"
          :step="1"
          placeholder="restart max size"
          v-decorator="['restartSize']"/>
      </a-form-item>

      <a-form-item
        label="CheckPoint Failure Options"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }"
        v-show="executionMode !== 5 && executionMode !== 6">
        <a-input-group compact>
          <a-input-number
            :min="1"
            :step="1"
            placeholder="checkpoint failure rate interval"
            allow-clear
            v-decorator="['cpMaxFailureInterval',{ rules: [ { validator: handleCheckCheckPoint} ]}]"
            style="width: calc(33% - 70px)"/>
          <a-button style="width: 70px">
            minute
          </a-button>
          <a-input-number
            :min="1"
            :step="1"
            placeholder="max failures per interval"
            v-decorator="['cpFailureRateInterval',{ rules: [ { validator: handleCheckCheckPoint} ]}]"
            style="width: calc(33% - 70px); margin-left: 1%"/>
          <a-button style="width: 70px">
            count
          </a-button>
          <a-select
            placeholder="trigger action"
            v-decorator="['cpFailureAction',{ rules: [ { validator: handleCheckCheckPoint} ]}]"
            allow-clear
            style="width: 32%;margin-left: 1%">
            <a-select-option
              v-for="(o,index) in cpTriggerAction"
              :key="`cp_trigger_${index}`"
              :value="o.value">
              <a-icon :type="o.value === 1?'alert':'sync'"/>
              {{ o.name }}
            </a-select-option>
          </a-select>
        </a-input-group>

        <p class="conf-desc" style="margin-bottom: -15px;margin-top: -3px">
          <span class="note-info" style="margin-bottom: 12px">
            <a-tag color="#2db7f5" class="tag-note">Note</a-tag>
            Operation after checkpoint failure, e.g:<br>
            Within <span class="note-elem">5 minutes</span>(checkpoint failure rate interval), if the number of checkpoint failures reaches <span
              class="note-elem">10</span> (max failures per interval),action will be triggered(alert or restart job)
          </span>
        </p>
      </a-form-item>

      <!--告警方式-->
      <template>
        <a-form-item
          v-if="1===2"
          label="Fault Alert Type"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
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
              <svg-icon role="img" v-if="o.value === 2" name="sms"/>
              <svg-icon role="img" v-if="o.value === 3" name="dingding"/>
              <svg-icon role="img" v-if="o.value === 4" name="wechat"/>
              {{ o.name }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item
          label="Alert Email"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
          <a-input
            type="text"
            placeholder="Please enter email,separate multiple emails with comma(,)"
            allowClear
            v-decorator="[ 'alertEmail' ]">
            <svg-icon name="mail" slot="prefix"/>
          </a-input>
        </a-form-item>

        <a-form-item
          v-if="alertType.indexOf(2)>-1"
          label="SMS"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
          <a-input
            type="text"
            placeholder="Please enter mobile number"
            allowClear
            v-decorator="[ 'alertSms', {rules: [{ required: true, message: 'mobile number is required' }]} ]"/>
        </a-form-item>

        <a-form-item
          v-if="alertType.indexOf(2)>-1"
          label="SMS Template"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
          <a-textarea
            rows="4"
            placeholder="Please enter sms template"
            v-decorator="['alertSmsTemplate', {rules: [{ required: true, message: 'SMS Template is required' }]} ]"/>
        </a-form-item>

        <a-form-item
          v-if="alertType.indexOf(3)>-1"
          label="DingTask Url"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
          <a-input
            type="text"
            placeholder="Please enter DingTask Url"
            allowClear
            v-decorator="[ 'alertDingURL', {rules: [{ required: true, message: 'DingTask Url is required' }]} ]"/>
        </a-form-item>

        <a-form-item
          v-if="alertType.indexOf(3)>-1"
          label="DingTask User"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
          <a-input
            type="text"
            placeholder="Please enter DingTask receive user"
            allowClear
            v-decorator="[ 'alertDingUser', {rules: [{ required: true, message: 'DingTask receive user is required' }]} ]"/>
        </a-form-item>

      </template>

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
          allowClear
          v-decorator="[`${conf.name}`,{ rules:[{ validator: conf.validator } ]}]"/>
        <a-switch
          v-if="conf.type === 'switch'"
          disabled
          checked-children="ON"
          un-checked-children="OFF"
          v-model="switchDefaultValue"
          v-decorator="[`${conf.name}`]"/>
        <a-input-number
          v-if="conf.type === 'number'"
          :min="conf.min"
          :max="conf.max"
          :default-value="conf.defaultValue"
          :step="conf.step"
          v-decorator="[`${conf.name}`,{ rules:[{ validator: conf.validator } ]}]"/>
        <span
          v-if="conf.type === 'switch'"
          class="conf-switch">
          ({{ conf.placeholder }})
        </span>
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
          :max-tag-count="controller.tagCount.total"
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
        <p class="conf-desc" style="margin-top: -3px">
          <span class="note-info">
            <a-tag color="#2db7f5" class="tag-note">Note</a-tag>
            Explicitly configuring both <span class="note-elem">total process memory</span> and <span
              class="note-elem">total Flink memory</span> is not recommended. It may lead to deployment failures due to potential memory configuration conflicts. Configuring other memory components also requires caution as it can produce further configuration conflicts,
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
          v-decorator="[`${conf.key}`,{ rules:[{ validator: conf.validator } ]}]"/>
        <span
          v-if="conf.type === 'switch'"
          class="conf-switch">
          ({{ conf.placeholder }})
        </span>
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
          :max-tag-count="controller.tagCount.jm"
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
          v-decorator="[`${conf.key}`,{ rules:[{ validator: conf.validator } ]}]"/>
        <span
          v-if="conf.type === 'switch'"
          class="conf-switch">
          ({{ conf.placeholder }})
        </span>
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
          :max-tag-count="controller.tagCount.tm"
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
          v-decorator="[`${conf.key}`,{ rules:[{ validator: conf.validator } ]}]"/>
        <span
          v-if="conf.type === 'switch'"
          class="conf-switch">({{ conf.placeholder }})</span>
        <p class="conf-desc">
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
          v-decorator="['dynamicOptions']"/>
        <p class="conf-desc">
          <span class="note-info">
            <a-tag color="#2db7f5" class="tag-note">Note</a-tag>
            It works the same as <span class="note-elem">-D$property=$value</span> in CLI mode, Allows specifying multiple generic configuration options. The available options can be found
            <a href="https://ci.apache.org/projects/flink/flink-docs-stable/ops/config.html" target="_blank">here</a>
          </span>
        </p>
      </a-form-item>

      <a-form-item
        v-if="jobType === 'customcode'"
        label="Program Args"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-textarea
          rows="4"
          name="args"
          placeholder="<arguments>"
          v-decorator="['args']"/>
      </a-form-item>

      <a-form-item
        label="Description"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-textarea
          rows="4"
          name="description"
          placeholder="Please enter description for this application"
          v-decorator="['description']"/>
      </a-form-item>

      <a-form-item
        :wrapper-col="{ span: 24 }"
        style="text-align: center">
        <a-button
          @click="handleGoBack">
          Cancel
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
        <a-icon type="fullscreen-exit" @click="handleBigScreenClose"/>
      </template>
      <template slot="footer">
        <span style="color: red;float: left">
          <ellipsis :length="200">
            {{ controller.flinkSql.errorMsg }}
          </ellipsis>
        </span>
        <a-button
          type="primary"
          title="Format SQL"
          @click="handleFormatSql">
          <a-icon type="align-left"/>
        </a-button>
        <a-button
          type="primary"
          @click="handleBigScreenOk">
          Apply
        </a-button>
      </template>
      <div class="sql-box" id="big-sql" style="width: 100%;" :class="'syntax-' + controller.flinkSql.success"></div>
    </a-modal>

    <Mergely
      ref="confEdit"
      @close="handleEditConfClose"
      @ok="handleEditConfOk"
      :visiable="controller.visiable.conf"/>
  </a-card>
</template>

<script>
  import Ellipsis from '@/components/Ellipsis'
  import {jars, listConf, modules, select} from '@api/project'
  import {create, checkName, main, name, readConf, upload} from '@api/application'
  import {list as listFlinkEnv} from '@/api/flinkEnv'
  import {list as listFlinkCluster} from '@/api/flinkCluster'
  import {template} from '@api/config'
  import {checkHadoop} from '@api/setting'
  import Mergely from './Mergely'
  import configOptions from './Option'
  import SvgIcon from '@/components/SvgIcon'
  import { sysHadoopConf } from '@api/config'

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
    sysHosts,
    initPodTemplate,
    completeHostAliasToPodTemplate,
    extractHostAliasFromPodTemplate,
    previewHostAlias
  } from '@/api/flinkPodtmpl'

  import {
    applyPom,
    bigScreenOk,
    bigScreenOpen,
    checkPomScalaVersion,
    formatSql,
    initFlinkSqlEditor,
    initPodTemplateEditor,
    disposeEditor,
    updateDependency,
    verifySQL
  } from './AddEdit'

  import {toPomString} from './Pom'
  import storage from '@/utils/storage'

  const Base64 = require('js-base64').Base64

  export default {
    name: 'AppAdd',
    components: {Mergely, Ellipsis, SvgIcon},
    data() {
      return {
        jobType: 'sql',
        resourceFrom: null,
        tableEnv: 1,
        projectList: [],
        projectId: null,
        versionId: null,
        scalaVersion: null,
        uploadJar: null,
        module: null,
        moduleList: [],
        flinkEnvs: [],
        flinkClusters: [],
        jars: [],
        resolveOrder: [
          {name: 'parent-first', order: 0},
          {name: 'child-first', order: 1}
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
          {name: 'alert', value: 1},
          {name: 'restart', value: 2}
        ],
        app: null,
        bigScreenVisible: false,
        appType: 0,
        switchDefaultValue: true,
        config: null,
        isSetConfig: false,
        useSysHadoopConf: false,
        alert: true,
        alertTypes: [
          {name: 'E-mail', value: 1, disabled: false},
          {name: 'SMS', value: 2, disabled: true},
          {name: 'Ding Ding Task', value: 3, disabled: true},
          {name: 'Wechat', value: 4, disabled: true},
        ],
        alertType: [],
        configOverride: null,
        configSource: [],
        configItems: [],
        totalItems: [],
        jmMemoryItems: [],
        tmMemoryItems: [],
        form: null,
        options: configOptions,
        optionsKeyMapping: {},
        dependency: [],
        uploadJars: [],
        loading: false,
        submitting: false,
        executionMode: null,
        exclusions: new Map(),
        podTemplate: '',
        jmPodTemplate: '',
        tmPodTemplate: '',
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
            conf: false,
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
            defaultValue: '',
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
          tmPtVisual: false,
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

    mounted() {
      this.select()
    },

    beforeMount() {
      this.handleInitForm()
      this.handleInitEditor()
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

    computed: {
      dynamicOptions() {
        return function (group) {
          return this.options.filter(x => x.group === group)
        }
      },
      hasOptions() {
        return function (items) {
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

    watch: {
      myTheme() {
        if (this.jobType === 'sql') {
          if (this.controller.editor.flinkSql) {
            this.controller.editor.flinkSql.updateOptions({
              theme: this.ideTheme()
            })
          }
          if (this.controller.editor.bigScreen) {
            this.controller.editor.bigScreen.updateOptions({
              theme: this.ideTheme()
            })
          }
          if (this.controller.editor.pom) {
            this.controller.editor.pom.updateOptions({
              theme: this.ideTheme()
            })
          }
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
        this.$refs.confEdit.theme()
      }
    },

    methods: {
      filterOption(input, option) {
        return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
      },

      getExecutionCluster(executionMode){
        return this.flinkClusters.filter(o => o.executionMode === executionMode)
      },

      select() {
        select().then((resp) => {
          this.projectList = resp.data
        }).catch((error) => {
          this.$message.error(error.message)
        })
      },

      handleInitForm() {
        this.form = this.$form.createForm(this)
        this.optionsKeyMapping = new Map()
        this.options.forEach((item, index, array) => {
          this.optionsKeyMapping.set(item.key, item)
          this.form.getFieldDecorator(item.key, {initialValue: item.defaultValue, preserve: true})
        })
        this.form.getFieldDecorator('resolveOrder', {initialValue: 0})
        this.form.getFieldDecorator('k8sRestExposedType', {initialValue: 0})
        this.form.getFieldDecorator('restartSize', {initialValue: 0})
        this.executionMode = null
        listFlinkEnv().then((resp) => {
          if (resp.data.length > 0) {
            this.flinkEnvs = resp.data
            const v = this.flinkEnvs.filter((v) => {
              return v.isDefault
            })[0]
            this.form.getFieldDecorator('versionId', {initialValue: v.id})
            this.versionId = v.id
            this.scalaVersion = v.scalaVersion
          }
        })
        listFlinkCluster().then((resp) => {
          this.flinkClusters = resp.data
        })
        // load history config records
        this.handleReloadHistoryUploads()
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

      handleChangeJobType(value) {
        this.jobType = value
        this.handleInitForm()
        if (this.jobType === 'sql') {
          this.form.getFieldDecorator('jobType', {initialValue: 'sql'})
          this.form.getFieldDecorator('tableEnv', {initialValue: '1'})
          this.handleInitEditor()
        } else {
          this.form.getFieldDecorator('jobType', {initialValue: 'customcode'})
          this.form.getFieldDecorator('resourceFrom', {initialValue: 'cvs'})
          this.controller.editor.flinkSql.getModel().setValue(this.controller.flinkSql.defaultValue)
        }
      },

      handleChangeResourceFrom(value) {
        this.resourceFrom = value
      },

      handleInitEditor() {
        if (this.jobType === 'sql') {
          disposeEditor(this)
          this.$nextTick(()=> {
            initFlinkSqlEditor(this, this.controller.flinkSql.value)
            this.selectedHistoryUploadJars = []
            if (this.executionMode === 6) {
              initPodTemplateEditor(this)
            }
          })
        }
      },

      handleTableEnv(value) {
        this.tableEnv = value
      },

      handleChangeProject(value) {
        this.projectId = value
        modules({
          id: value
        }).then((resp) => {
          this.moduleList = resp.data
        }).catch((error) => {
          this.$message.error(error.message)
        })
      },

      handleChangeMode(mode) {
        this.executionMode = mode
        this.handleInitEditor()
      },

      handleFlinkVersion(id) {
        this.versionId = id
        this.scalaVersion = this.flinkEnvs.find(v => v.id === id).scalaVersion
        this.handleCheckPomScalaVersion()
      },

      handleChangeJmMemory(value) {
        this.jmMemoryItems = value
      },

      handleChangeTmMemory(value) {
        this.tmMemoryItems = value
      },

      handleChangeProcess(value) {
        this.totalItems = value
      },

      handleChangeAlert() {
        this.alert = !this.alert
        if (!this.alert) {
          this.alertType = []
        }
      },

      handleChangeAlertType(value) {
        this.alertType = value
      },

      handleClusterId(value) {
        if (this.executionMode === 6) {
          this.form.setFieldsValue({jobName: value.target.value})
        }
      },

      handleJobName(confFile) {
        name({
          config: confFile
        }).then((resp) => {
          this.form.setFieldsValue({'jobName': resp.data})
        }).catch((error) => {
          this.$message.error(error.message)
        })
      },

      handleSQLConf(checked) {
        if (checked) {
          if (this.configOverride != null) {
            this.controller.visiable.conf = true
            this.$refs.confEdit.set(this.configOverride)
          } else {
            template({}).then((resp) => {
              this.controller.visiable.conf = true
              const sqlJobConfig = Base64.decode(resp.data)
              this.$refs.confEdit.set(sqlJobConfig)
            }).catch((error) => {
              this.$message.error(error.message)
            })
          }
        } else {
          this.controller.visiable.conf = false
          this.configOverride = null
          this.isSetConfig = false
        }
      },

      handleCheckPomScalaVersion() {
        checkPomScalaVersion(this)
      },

      handleApplyPom() {
        applyPom(this)
      },

      handleEditPom(pom) {
        const pomString = toPomString(pom)
        this.activeTab = 'pom'
        this.editor.pom.getModel().setValue(pomString)
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

      handleUploadJob(info) {
        const status = info.file.status
        if (status === 'done') {
          this.loading = false
        } else if (status === 'error') {
          this.loading = false
          this.$message.error(`${info.file.name} file upload failed.`)
        }
      },

      handleCustomJobRequest(data) {
        const formData = new FormData()
        formData.append('file', data.file)
        upload(formData).then((resp) => {
          if (resp.status == 'error') {
            this.$swal.fire({
              title: 'Failed',
              icon: 'error',
              width: this.exceptionPropWidth(),
              html: '<pre class="propException">' + resp['exception'] + '</pre>',
              focusConfirm: false
            })
          } else {
            this.loading = false
            const path = resp.data
            this.uploadJar = data.file.name
            main({
              jar: path
            }).then((resp) => {
              this.form.setFieldsValue({'mainClass': resp.data})
            }).catch((error) => {
              this.$message.error(error.message)
            })
          }
        }).catch((error) => {
          this.$message.error(error.message)
          this.loading = false
        })
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

      handleCustomDepsRequest(data) {
        const formData = new FormData()
        formData.append('file', data.file)
        upload(formData).then((resp) => {
          this.loading = false
          this.controller.dependency.jar.set(data.file.name, data.file.name)
          this.handleUpdateDependency()
        }).catch((error) => {
          this.$message.error(error.message)
          this.loading = false
        })
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
        bigScreenOk(this, function () {
          //设置小输入框的值.
          this.controller.editor.flinkSql.getModel().setValue(this.controller.flinkSql.value)
        })
      },

      handleBigScreenClose() {
      },

      handleChangeModule(module) {
        this.module = module
        this.form.resetFields(['appType', 'config', 'jobName'])
        this.appType = 0
      },

      handleChangeAppType(val) {
        this.appType = parseInt(val)
        this.handleConfOrJar()
      },

      handleConfOrJar() {
        if (this.module && this.appType) {
          this.form.resetFields(['config', 'jobName'])
          this.configOverride = null
          if (this.appType === 1) {
            listConf({
              id: this.projectId,
              module: this.module
            }).then((resp) => {
              this.configSource = resp.data
            }).catch((error) => {
              this.$message.error(error.message)
            })
          } else {
            jars({
              id: this.projectId,
              module: this.module
            }).then((resp) => {
              this.jars = resp.data
            }).catch((error) => {
              this.$message.error(error.message)
            })
          }
        }
      },

      handleChangeJars(jar) {
        main({
          projectId: this.projectId,
          module: this.module,
          jar: jar
        }).then((resp) => {
          this.form.setFieldsValue({'mainClass': resp.data})
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
          } else {
            callback()
          }
        }
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

      handleCheckKubernetesClusterId(rule, value, callback) {
        const clusterIdReg = /^[a-z]([-a-z0-9]*[a-z0-9])?$/
        if (value === null || value === undefined || value === '') {
          callback(new Error('Kubernetes clusterId is required'))
        } else {
          if (!clusterIdReg.test(value)) {
            callback(new Error("Kubernetes clusterId is invalid, clusterId must consist of lower case alphanumeric characters or '-', start with an alphabetic character, and end with an alphanumeric character.Please check"))
          } else {
            callback()
          }
        }
      },

      handleCheckJobName(rule, value, callback) {
        if (value === null || value === undefined || value === '') {
          callback(new Error('Application Name is required'))
        } else {
          checkName({jobName: value}).then((resp) => {
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

      handleCheckCheckPoint(rule, value, callback) {
        const cpMaxFailureInterval = this.form.getFieldValue('cpMaxFailureInterval')
        const cpFailureRateInterval = this.form.getFieldValue('cpFailureRateInterval')
        const cpFailureAction = this.form.getFieldValue('cpFailureAction')

        if (cpMaxFailureInterval != null && cpFailureRateInterval != null && cpFailureAction != null) {
          if (cpFailureAction === 1) {
            const alertEmail = this.form.getFieldValue('alertEmail')
            if (alertEmail == null) {
              this.form.setFields({
                alertEmail: {
                  errors: [new Error('checkPoint Failure trigger is alert,alertEmail must not be empty')]
                }
              })
              callback(new Error('trigger action is alert,alertEmail must not be empty'))
            } else {
              callback()
            }
          } else {
            callback()
          }
        } else if (cpMaxFailureInterval == null && cpFailureRateInterval == null && cpFailureAction == null) {
          callback()
        } else {
          callback(new Error('options all required or all empty'))
        }
      },

      /**
       * TODO:解析出source和sink,画图!
       */
      handleLeaderLine() {
        const elem = document.querySelectorAll('.CodeMirror-line>span[role=presentation]')
        const keyElem = []
        elem.forEach(x => {
          for (let i = 0; i < x.childNodes.length; i++) {
            const currElem = x.childNodes[i]
            if (currElem.nodeValue != null && currElem.nodeValue.trim() != '') {
              const prev = x.childNodes[i - 1] || null
              if (prev != null && this.hasClass(prev, 'cm-keyword')) {
                keyElem.push(currElem)
              }
            }
          }
        })
        keyElem.forEach(child => {
          $(child).wrap('<span style="color:forestgreen;font-weight: 700"></span>')
        })
      },

      handleCheckConfig(rule, value, callback) {
        if (value) {
          const isProp = value.endsWith('.properties')
          const isYaml = value.endsWith('.yaml') || value.endsWith('.yml')
          if (!isProp && !isYaml) {
            callback(new Error('The configuration file must be (.properties|.yaml|.yml)'))
          } else {
            callback()
          }
        } else {
          callback(new Error('Please select config'))
        }
      },

      handleEditConfig() {
        const config = this.form.getFieldValue('config')
        readConf({
          config: config
        }).then((resp) => {
          const conf = Base64.decode(resp.data)
          this.controller.visiable.conf = true
          this.$refs.confEdit.set(conf)
        }).catch((error) => {
          this.$message.error(error.message)
        })
      },

      handleEditConfClose() {
        this.controller.visiable.conf = false
        if (this.configOverride == null) {
          this.isSetConfig = false
        }
      },

      handleEditConfOk(value) {
        if (value == null || !value.replace(/^\s+|\s+$/gm, '')) {
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
          if (this.jobType === 'sql') {
            if (this.controller.flinkSql.value == null || this.controller.flinkSql.value.trim() === '') {
              this.controller.flinkSql.success = true
              this.controller.flinkSql.errorMsg = null
            } else {
              verifySQL(this, (success) => {
                if (!success) {
                  return
                }
              })
            }
          }
          if (!err) {
            if (!this.submitting) {
              if (this.jobType === 'customcode') {
                this.handleSubmitCustomJob(values)
              } else {
                this.handleSubmitSQL(values)
              }
            }
          }
        })
      },

      handleSubmitCustomJob(values) {
        const options = this.handleFormValue(values)
        if(values.flinkClusterId){
          const cluster = this.flinkClusters.filter(c => c.id === values.flinkClusterId && c.clusterState === 1)[0] || null
          values.clusterId = cluster.id
          values.flinkClusterId = cluster.clusterId
          values.yarnSessionClusterId = cluster.clusterId
        }
        const params = {
          jobType: 1,
          executionMode: values.executionMode,
          versionId: values.versionId,
          projectId: values.project || null,
          module: values.module || null,
          jobName: values.jobName,
          args: values.args,
          options: JSON.stringify(options),
          yarnQueue: this.handleYarnQueue(values),
          cpMaxFailureInterval: values.cpMaxFailureInterval || null,
          cpFailureRateInterval: values.cpFailureRateInterval || null,
          cpFailureAction: values.cpFailureAction || null,
          dynamicOptions: values.dynamicOptions,
          resolveOrder: values.resolveOrder,
          k8sRestExposedType: values.k8sRestExposedType,
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
        console.log('获取params：' +JSON.stringify( params))

        // common params...
        const resourceFrom = values.resourceFrom
        if (resourceFrom != null) {
          if (resourceFrom === 'cvs') {
            params['resourceFrom'] = 1
            params['appType'] = this.appType
            //streamx flink
            if (this.appType === 1) {
              const configVal = this.form.getFieldValue('config')
              params['format'] = configVal.endsWith('.properties') ? 2 : 1
              if (this.configOverride == null) {
                readConf({
                  config: configVal
                }).then((resp) => {
                  params['config'] = resp.data
                  this.handleCreateApp(params)
                }).catch((error) => {
                  this.$message.error(error.message)
                })
              } else {
                params['config'] = Base64.encode(this.configOverride)
                this.handleCreateApp(params)
              }
            } else {
              params['jar'] = this.form.getFieldValue('jar') || null
              params['mainClass'] = this.form.getFieldValue('mainClass') || null
              this.handleCreateApp(params)
            }
          } else {
            // from upload
            params['resourceFrom'] = 2
            params['appType'] = 2
            params['jar'] = this.uploadJar
            params['mainClass'] = this.form.getFieldValue('mainClass') || null
            this.handleCreateApp(params)
          }
        }
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
        if (config != null && config !== undefined && config.trim() != '') {
          config = Base64.encode(config)
        } else {
          config = null
        }
        if(values.flinkClusterId){
          const cluster = this.flinkClusters.filter(c => c.id === values.flinkClusterId && c.clusterState === 1)[0] || null
          values.clusterId = cluster.id
          values.flinkClusterId = cluster.clusterId
          values.yarnSessionClusterId = cluster.clusterId
        }

        const params = {
          jobType: 2,
          executionMode: values.executionMode,
          versionId: values.versionId,
          flinkSql: this.controller.flinkSql.value,
          appType: 1,
          config: config,
          format: this.isSetConfig ? 1 : null,
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
          alertEmail: values.alertEmail,
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
        this.handleCreateApp(params)
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

      handleCreateApp(params) {
        this.submitting = true
        const param = {}
        for (const k in params) {
          const v = params[k]
          if (v != null && v !== undefined) {
            param[k] = v
          }
        }
        const socketId = this.uuid()
        storage.set('DOWN_SOCKET_ID', socketId)
        param.socketId = socketId
        create(param).then((resp) => {
          const created = resp.data
          this.submitting = false
          if (created) {
            this.$router.push({path: '/flink/app'})
          }
        }).catch((error) => {
          this.submitting = false
          this.$message.error(error.message)
        })
      },

      handleGoBack() {
        this.$router.go(-1)
      },

      handleUseSysHadoopConf(value) {
        this.useSysHadoopConf = value
      },

      handleReloadHistoryUploads() {
        this.selectedHistoryUploadJars = []
        histUploadJars().then((resp) => {
          this.historyRecord.uploadJars = resp.data
        })
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

      handleRefreshHostAliasPreview() {historyRecord
        previewHostAlias({hosts: this.selectedPodTemplateHostAlias.join(',')})
          .then((resp) => {
            this.hostAliasPreview = resp.data
          })
      },
    }

  }
</script>

<style lang='less'>
  @import "AddEdit";
</style>
