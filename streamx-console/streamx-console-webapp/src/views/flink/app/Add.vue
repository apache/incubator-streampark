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
            Custom Code
          </a-select-option>
          <a-select-option
            value="sql">
            Flink SQL
          </a-select-option>
        </a-select>
      </a-form-item>

      <template v-if="jobType === 'sql'">
        <a-form-item
          label="Flink SQL"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 17} }"
          class="form-required"
          style="margin-top: 10px">
          <div class="sql-box" id="flink-sql" :class="'syntax-' + controller.flinkSql.success"></div>
          <p class="conf-desc" style="margin-bottom: -25px;margin-top: -5px">
            <span class="sql-desc" v-if="!controller.flinkSql.success">
              {{ controller.flinkSql.errorMsg }}
            </span>
            <span v-else class="sql-desc" style="color: green">
              successful
            </span>
          </p>
          <a-icon
            class="format-sql"
            type="align-left"
            title="Format SQL"
            @click.native="handleFormatSql"/>

          <a-icon
            class="big-screen"
            type="fullscreen"
            title="Full Screen"
            two-tone-color="#4a9ff5"
            @click="handleBigScreenOpen()" />
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
            </a-tab-pane>
            <a-tab-pane
              key="jar"
              tab="Upload Jar">
              <a-upload-dragger
                name="file"
                :multiple="true"
                @change="handleUploadJar"
                :showUploadList="loading"
                :customRequest="handleCustomRequest"
                :beforeUpload="handleBeforeUpload">
                <div style="height: 266px">
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
          <a-button
            type="primary"
            class="apply-pom"
            @click="handleApplyPom()">
            Apply
          </a-button>

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
                <span @click="handleEditPom(value)" class="tag-dependency-pom">
                  <a-tag class="tag-dependency" color="#2db7f5">POM</a-tag>
                  {{ value.artifactId }}-{{ value.version }}.jar
                </span>
                <a-icon type="close" class="icon-close" @click="handleRemovePom(value)" />
              </template>
            </a-alert>
            <a-alert
              class="dependency-item"
              v-for="(value, index) in uploadJars"
              :key="`upload_jars_${index}`"
              type="info"
              closable>
              <template slot="message">
                <span><a-tag class="tag-dependency" color="#108ee9">JAR</a-tag>{{ value }}</span>
                <a-icon type="close" class="icon-close" @click="handleRemoveJar(value)" />
              </template>
            </a-alert>
          </div>
        </a-form-item>

        <a-form-item
          label="Application conf"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
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
            v-decorator="[ 'mainClass', {rules: [{ required: true, message: 'Program Main is required' }]} ]" />
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
                title="edit config" />
            </template>
          </a-tree-select>
        </a-form-item>
      </template>

      <a-form-item
        label="Application Name"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-input
          type="text"
          placeholder="Please enter jobName"
          allowClear
          v-decorator="['jobName',{ rules: [{ validator: handleCheckJobName,required: true}]}]" />
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
        label="Execution Mode"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-select
          placeholder="Execution Mode"
          v-decorator="[ 'executionMode', {rules: [{ required: true, message: 'Execution Mode is required' }] }]">
          <a-select-option
            v-for="(o,index) in executionMode"
            :key="`execution_mode_${index}`"
            :disabled="o.disabled"
            :value="o.value">
            {{ o.mode }}
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
        label="Fault Restart Size"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-input-number
          :min="1"
          :step="1"
          placeholder="restart max size"
          v-decorator="['restartSize']" />
      </a-form-item>

      <a-form-item
        label="CheckPoint Failure Options"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
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
            v-decorator="[ 'alertSms', {rules: [{ required: true, message: 'mobile number is required' }]} ]" />
        </a-form-item>

        <a-form-item
          v-if="alertType.indexOf(2)>-1"
          label="SMS Template"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
          <a-textarea
            rows="4"
            placeholder="Please enter sms template"
            v-decorator="['alertSmsTemplate', {rules: [{ required: true, message: 'SMS Template is required' }]} ]" />
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
            v-decorator="[ 'alertDingURL', {rules: [{ required: true, message: 'DingTask Url is required' }]} ]" />
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
            v-decorator="[ 'alertDingUser', {rules: [{ required: true, message: 'DingTask receive user is required' }]} ]" />
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
          v-decorator="[`${conf.name}`,{ rules:[{ validator: conf.validator } ]}]" />
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
          v-decorator="[`${conf.name}`,{ rules:[{ validator: conf.validator } ]}]" />
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
          v-decorator="[`${conf.key}`,{ rules:[{ validator: conf.validator } ]}]" />
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
          v-decorator="[`${conf.key}`,{ rules:[{ validator: conf.validator } ]}]" />
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
          v-decorator="[`${conf.key}`,{ rules:[{ validator: conf.validator } ]}]" />
        <span
          v-if="conf.type === 'switch'"
          class="conf-switch">({{ conf.placeholder }})</span>
        <p class="conf-desc">
          {{ conf | description }}
        </p>
      </a-form-item>

      <a-form-item
        label="Dynamic Option"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-textarea
          rows="4"
          name="dynamicOptions"
          placeholder="$key=$value,If there are multiple parameters,you can new line enter them (-D <arg>)"
          v-decorator="['dynamicOptions']" />
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
          v-decorator="['args']" />
      </a-form-item>

      <a-form-item
        label="Description"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-textarea
          rows="4"
          name="description"
          placeholder="Please enter description for this application"
          v-decorator="['description']" />
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
      :visiable="controller.visiable.conf" />
  </a-card>
</template>

<script>
import Ellipsis from '@/components/Ellipsis'
import { jars, listConf, modules, select } from '@api/project'
import { create, upload, exists, main, name, readConf, checkJar } from '@api/application'
import { template } from '@api/config'
import Mergely from './Mergely'
import configOptions from './Option'
const Base64 = require('js-base64').Base64
import SvgIcon from '@/components/SvgIcon'

import {
  initEditor,
  verifySQL,
  bigScreenOpen,
  bigScreenOk,
  bigScreenClose,
  formatSql,
  applyPom,
  updateDependency
} from './AddEdit'

import { toPomString } from './Pom'

export default {
  name: 'AppAdd',
  components: { Mergely,Ellipsis,SvgIcon },
  data() {
    return {
      jobType: 'sql',
      tableEnv: 1,
      projectList: [],
      projectId: null,
      module: null,
      moduleList: [],
      jars: [],
      resolveOrder: [
        { name: 'parent-first', order: 0 },
        { name: 'child-first', order: 1 }
      ],
      executionMode: [
        { mode: 'yarn application', value: 4, disabled: false },
        { mode: 'yarn pre-job', value: 2, disabled: true },
        { mode: 'local', value: 0, disabled: true },
        { mode: 'remote', value: 1, disabled: true },
        { mode: 'yarn-session', value: 3, disabled: true },
        { mode: 'kubernetes', value: 5, disabled: true }
      ],
      cpTriggerAction: [
        { name: 'alert', value: 1 },
        { name: 'restart', value: 2 }
      ],
      app: null,
      bigScreenVisible: false,
      appType: 0,
      switchDefaultValue: true,
      config: null,
      isSetConfig: false,
      alert: true,
      alertTypes: [
        {name: 'E-mail', value: 1, disabled: false},
        {name: 'SMS', value: 2,disabled: true},
        {name: 'Ding Ding Task', value: 3,disabled: true},
        {name: 'Wechat', value: 4,disabled: true},
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
      exclusions: new Map(),
      controller: {
        activeTab: 'pom',
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
          pom: null
        },
        flinkSql: {
          defaultValue: '',
          value: null,
          errorLine: null,
          errorColumn: null,
          errorMsg: null,
          errorStart: null,
          errorEnd: null,
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
      }
    }
  },

  mounted() {
    this.select()
  },

  beforeMount() {
    this.handleInitForm()
    this.handleInitSQLMode()
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
      this.$refs.confEdit.theme()
    }
  },

  methods: {

    filterOption(input, option) {
      return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
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
        this.form.getFieldDecorator(item.key, { initialValue: item.defaultValue, preserve: true })
      })
      this.form.getFieldDecorator('resolveOrder', { initialValue: 0 })
      this.form.getFieldDecorator('executionMode', { initialValue: 4 })
      this.form.getFieldDecorator('restartSize', { initialValue: 0 })
    },

    handleChangeJobType(value) {
      this.jobType = value
      this.handleInitForm()
      if (this.jobType === 'sql') {
        this.handleInitSQLMode()
      } else {
        this.form.getFieldDecorator('jobType', { initialValue: 'customcode' })
        this.controller.editor.flinkSql.getModel().setValue(this.controller.flinkSql.defaultValue)
      }
    },

    handleInitSQLMode() {
      this.form.getFieldDecorator('jobType', { initialValue: 'sql' })
      this.form.getFieldDecorator('tableEnv', { initialValue: '1' })
      this.$nextTick(() => {
        initEditor(this)
      })
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

    handleJobName(confFile) {
      name({
        config: confFile
      }).then((resp) => {
        this.form.setFieldsValue({ 'jobName': resp.data })
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

    handleApplyPom() {
      applyPom(this)
    },

    handleEditPom(pom) {
      const pomString = toPomString(pom)
      this.activeTab = 'pom'
      this.editor.pom.getModel().setValue(pomString)
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
        console.log('upload file type :' + file.type)
        if (!/\.(jar|JAR)$/.test(file.name)) {
          this.loading = false
          this.$message.error('Only jar files can be uploaded! please check your file.')
          return false
        }
      }
      this.loading = true
      return true
    },

    handleCustomRequest(data) {
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

    handleRemovePom(pom) {
      const id = pom.groupId + '_' + pom.artifactId
      this.controller.dependency.pom.delete(id)
      this.handleUpdateDependency()
    },

    handleRemoveJar(jar) {
      this.controller.dependency.jar.delete(jar)
      this.handleUpdateDependency()
    },

    handleUpdateDependency() {
      updateDependency(this)
    },

    handleFormatSql() {
      formatSql(this)
    },

    handleBigScreenOpen() {
      bigScreenOpen(this)
    },

    handleBigScreenOk() {
      bigScreenOk(this,function() {
        //设置小输入框的值.
        this.controller.editor.flinkSql.getModel().setValue(this.controller.flinkSql.value)
      })
    },

    handleBigScreenClose () {
      bigScreenClose(this)
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
        this.form.setFieldsValue({ 'mainClass': resp.data })
      }).catch((error) => {
        this.$message.error(error.message)
      })
    },

    handleCheckJobName(rule, value, callback) {
      if (value === null || value === undefined || value === '') {
        callback(new Error('Application Name is required'))
      } else {
        exists({ jobName: value }).then((resp) => {
          const exists = parseInt(resp.data)
          if (exists === 0) {
            callback()
          } else if (exists === 1) {
            callback(new Error('Application Name must be unique. The application name already exists'))
          } else {
            callback(new Error('The Application Name is already running in yarn,cannot be repeated. Please check'))
          }
        })
      }
    },

    handleCheckCheckPoint (rule, value, callback) {
      const cpMaxFailureInterval =  this.form.getFieldValue('cpMaxFailureInterval')
      const cpFailureRateInterval = this.form.getFieldValue('cpFailureRateInterval')
      const cpFailureAction = this.form.getFieldValue('cpFailureAction')

      if( cpMaxFailureInterval != null && cpFailureRateInterval != null && cpFailureAction != null ) {
        if( cpFailureAction === 1) {
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
      } else if(cpMaxFailureInterval == null && cpFailureRateInterval == null && cpFailureAction == null) {
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
            verifySQL(this,(success) => {
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
      // common params...
      const params = {
        jobType: 1,
        executionMode: values.executionMode,
        projectId: values.project,
        module: values.module,
        appType: this.appType,
        jobName: values.jobName,
        args: values.args,
        options: JSON.stringify(options),
        cpMaxFailureInterval: values.cpMaxFailureInterval || null,
        cpFailureRateInterval: values.cpFailureRateInterval || null,
        cpFailureAction: values.cpFailureAction || null,
        dynamicOptions: values.dynamicOptions,
        resolveOrder: values.resolveOrder,
        restartSize: values.restartSize,
        alertEmail: values.alertEmail || null,
        description: values.description
      }

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
      if (config != null && config != undefined && config.trim() != '') {
        config = Base64.encode(config)
      } else {
        config = null
      }

      const params = {
        jobType: 2,
        executionMode: values.executionMode,
        flinkSql: this.controller.flinkSql.value,
        appType: 1,
        config: config,
        jobName: values.jobName,
        args: values.args || null,
        dependency: dependency.pom === undefined && dependency.jar === undefined ? null : JSON.stringify(dependency),
        options: JSON.stringify(options),
        cpMaxFailureInterval: values.cpMaxFailureInterval || null,
        cpFailureRateInterval: values.cpFailureRateInterval || null,
        cpFailureAction: values.cpFailureAction || null,
        dynamicOptions: values.dynamicOptions || null,
        resolveOrder: values.resolveOrder,
        restartSize: values.restartSize,
        alertEmail: values.alertEmail,
        description: values.description || null
      }
      this.handleCreateApp(params)
    },

    handleFormValue(values) {
      const options = {}
      for (const k in values) {
        const v = values[k]
        if (v != null && v !== '' && v !== undefined ) {
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
      create(param).then((resp) => {
        const created = resp.data
        this.submitting = false
        if (created) {
          this.$router.push({ path: '/flink/app' })
        }
      }).catch((error) => {
        this.submitting = false
        this.$message.error(error.message)
      })
    },

    handleGoBack() {
      this.$router.go(-1)
    }

  }

}
</script>

<style lang='less'>
@import "AddEdit";
</style>
