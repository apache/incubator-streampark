<template>
  <a-card
    :body-style="{padding: '24px 32px'}"
    :bordered="false"
    class="app_controller">
    <a-form
      @submit="handleSubmit"
      :form="form"
      v-if="app!=null">

      <a-form-item
        label="Development Mode"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-alert
          v-if="app['jobType'] === 1"
          message="customcode"
          type="info" />
        <a-alert
          v-else
          message="flinkSql"
          type="info" />
      </a-form-item>

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
              :key="`jars_${index}`"
              type="info">
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
          @click="handleReset">
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
import Ellipsis from '@/components/Ellipsis'
import { listConf } from '@api/project'
import { get, update, exists, name, readConf, upload } from '@api/application'
import { history as confhistory, get as getVer, template } from '@api/config'
import { get as getSQL, history as sqlhistory } from '@api/flinksql'
import { mapActions, mapGetters } from 'vuex'
import Mergely from './Mergely'
import Different from './Different'
import configOptions from './Option'
import SvgIcon from '@/components/SvgIcon'

const Base64 = require('js-base64').Base64
import {
  initEditor,
  verifySQL,
  bigScreenOpen,
  bigScreenOk,
  bigScreenClose,
  applyPom,
  formatSql,
  updateDependency
} from './AddEdit'

import { toPomString } from './Pom'

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
      configOverride: null,
      configId: null,
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
      validateAgain: false,
      configuration: [
        { key: 'tc', name: ' time characteristic' },
        { key: 'cp', name: ' checkpoints' },
        { key: 'rs', name: ' restart strategy' },
        { key: 'sb', name: ' state backend' }
      ],
      controller: {
        activeTab: 'pom',
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
          pom: null
        },
        flinkSql: {
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
      this.optionsKeyMapping.set(item.key, item)
      this.optionsValueMapping.set(item.name, item.key)
      this.form.getFieldDecorator(item.key, { initialValue: item.defaultValue, preserve: true })
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
        if (this.app.config && this.app.config.trim() != '') {
          this.configOverride = Base64.decode(this.app.config)
          this.isSetConfig = true
        }
        this.defaultOptions = JSON.parse(this.app.options)
        this.configId = this.app.configId
        this.defaultFlinkSqlId = this.app['sqlId'] || null
        this.handleReset()
        this.handleListConfVersion()
        this.handleConfList()
      }).catch((error) => {
        this.$message.error(error.message)
      })
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

    handleCheckJobName(rule, value, callback) {
      if (!value) {
        callback(new Error('application name is required'))
      } else {
        exists({
          id: this.app.id,
          jobName: value
        }).then((resp) => {
          const exists = parseInt(resp.data)
          if (exists === 0) {
            callback()
          } else if (exists === 1) {
            callback(new Error('application name must be unique. The application name already exists'))
          } else {
            callback(new Error('The application name is already running in yarn,cannot be repeated. Please check'))
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

    handleBigScreenOpen() {
      bigScreenOpen(this)
    },

    handleBigScreenOk() {
      bigScreenOk(this)
    },

    handleBigScreenClose() {
      bigScreenClose(this)
    },

    handleInitDependency() {
      this.controller.dependency.jar = new Map()
      this.controller.dependency.pom = new Map()
      this.handleDependencyJsonToPom(this.flinkSql.dependency,this.controller.dependency.pom,this.controller.dependency.jar)
      this.handleUpdateDependency()
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
        this.loading = false
        this.$message.error('You can only upload jar file !')
        return false
      }
      this.loading = true
      return true
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
      if (config != null && config != undefined && config.trim() != '') {
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
        config: config,
        args: values.args,
        options: JSON.stringify(options),
        dynamicOptions: values.dynamicOptions,
        cpMaxFailureInterval: values.cpMaxFailureInterval || null,
        cpFailureRateInterval: values.cpFailureRateInterval || null,
        cpFailureAction: values.cpFailureAction || null,
        resolveOrder: values.resolveOrder,
        executionMode: values.executionMode,
        restartSize: values.restartSize,
        alertEmail: values.alertEmail || null,
        description: values.description
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
        alertEmail: values.alertEmail|| null,
        executionMode: values.executionMode,
        description: values.description || null
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
      confhistory({ id: this.app.id }).then((resp) => {
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

    handleReset() {
      this.$nextTick(() => {
        this.form.setFieldsValue({
          'jobName': this.app.jobName,
          'args': this.app.args,
          'description': this.app.description,
          'dynamicOptions': this.app.dynamicOptions,
          'resolveOrder': this.app.resolveOrder,
          'executionMode': this.app.executionMode,
          'restartSize': this.app.restartSize,
          'alertEmail': this.app.alertEmail,
          'cpMaxFailureInterval': this.app.cpMaxFailureInterval,
          'cpFailureRateInterval': this.app.cpFailureRateInterval,
          'cpFailureAction': this.app.cpFailureAction
        })
        if (this.app.jobType === 2) {
          this.flinkSql.sql = this.app.flinkSql || null
          this.flinkSql.dependency = this.app.dependency || null
          initEditor(this,Base64.decode(this.flinkSql.sql))
          this.handleInitDependency()
        }
      })

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
            parallelism = parseInt(v)
          }
          if (k === 'parallelism.default') {
            slot = parseInt(v)
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
    }

  },

  watch: {
    myTheme() {
      if (this.app.jobType === 2) {
        this.controller.editor.flinkSql.updateOptions({
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
