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
        label="Project"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-alert
          :message="app['projectName']"
          type="info" />
      </a-form-item>

      <a-form-item
        label="Module"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-alert
          :message="app['module']"
          type="info" />
      </a-form-item>

      <a-form-item
        label="Application Type"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-alert
          message="Apache Flink"
          type="info" />
      </a-form-item>

      <a-form-item
        label="Program Jar"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-select
          placeholder="Please select jar"
          @change="handleChangeJars"
          v-decorator="[ 'jar', {rules: [{ required: true }] }]">
          <a-select-option
            v-for="(jar,index) in jars"
            :key="`jars_${index}`"
            :value="jar">
            {{ jar }}
          </a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item
        label="Program Main"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-input
          type="text"
          placeholder="Please enter Main class"
          v-decorator="[ 'mainClass', {rules: [{ required: true}]} ]" />
      </a-form-item>

      <a-form-item
        label="Application Name"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-input
          type="text"
          placeholder="Please enter Application Name"
          v-decorator="['jobName',{ rules: [{ validator: handleCheckJobName,trigger:'submit' } ]}]" />
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
            allowClear
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
  </a-card>
</template>

<script>
import { jars } from '@api/project'
import { get, update, exists, main } from '@api/application'
import { mapActions, mapGetters } from 'vuex'
import configOptions from './Option'

export default {
  name: 'EditFlink',
  data() {
    return {
      strategy: 1,
      app: null,
      switchDefaultValue: true,
      runMaxTagCount: 1,
      totalTagCount: 1,
      jmMaxTagCount: 1,
      tmMaxTagCount: 1,
      defaultOptions: {},
      defaultJar: null,
      configSource: [],
      jars: [],
      validateAgain: false,
      resolveOrder: [
        { name: 'parent-first', order: 0 },
        { name: 'child-first', order: 1 }
      ],
      executionMode: [
        { mode: 'local', value: 0,disabled: true },
        { mode: 'remote', value: 1,disabled: true },
        { mode: 'yarn pre-job', value: 2,disabled: true },
        { mode: 'yarn-session', value: 3,disabled: true },
        { mode: 'yarn application', value: 4,disabled: false },
        { mode: 'kubernetes', value: 5,disabled: true }
      ],
      cpTriggerAction: [
        { name: 'alert', value: 1 },
        { name: 'restart', value: 2 }
      ],
      configItems: [],
      jmMemoryItems: [],
      tmMemoryItems: [],
      totalItems: [],
      form: null,
      options: configOptions,
      optionsKeyMapping: {},
      optionsValueMapping: {},
      loading: false,
      submitting: false,
      confEdit: {
        visiable: false
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
    handleGet(appId) {
      get({ id: appId }).then((resp) => {
        this.app = resp.data
        this.defaultOptions = JSON.parse(this.app.options)
        jars({
          id: this.app.projectId,
          module: this.app.module
        }).then((resp) => {
          this.jars = resp.data
          this.handleReset()
        }).catch((error) => {
          this.$message.error(error.message)
        })
      }).catch((error) => {
        this.$message.error(error.message)
      })
    },

    handleConf(item) {
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

    handleChangeJars(jar) {
      main({
        projectId: this.app.projectId,
        module: this.app.module,
        jar: jar
      }).then((resp) => {
        this.form.setFieldsValue({ 'mainClass': resp.data })
      }).catch((error) => {
        this.$message.error(error.message)
      })
    },

    // handler
    handleSubmit: function(e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        if (!err) {
          if (!this.submitting) {
            const options = this.handleFormValue(values)
            const params = {
              id: this.app.id,
              jobName: values.jobName,
              resolveOrder: values.resolveOrder,
              executionMode: values.executionMode,
              jar: values.jar,
              mainClass: values.mainClass,
              args: values.args,
              options: JSON.stringify(options),
              cpMaxFailureInterval: values.cpMaxFailureInterval || null,
              cpFailureRateInterval: values.cpFailureRateInterval || null,
              cpFailureAction: values.cpFailureAction || null,
              dynamicOptions: values.dynamicOptions,
              restartSize: values.restartSize,
              alertEmail: values.alertEmail || null,
              description: values.description
            }
            this.handleUpdateApp(params)
          }
        }
      })
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

    handleReset() {
      this.handleChangeJars(this.app.jar)
      this.$nextTick(() => {
        this.form.setFieldsValue({
          'jobName': this.app.jobName,
          'args': this.app.args,
          'jar': this.app.jar,
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
        this.form.setFieldsValue({'parallelism': parallelism})
        this.form.setFieldsValue({'slot': slot})
        this.form.setFieldsValue({'totalOptions': this.totalItems})
        this.form.setFieldsValue({'jmOptions': this.jmMemoryItems})
        this.form.setFieldsValue({'tmOptions': this.tmMemoryItems})
        this.form.setFieldsValue(fieldValueOptions)
      })
    }

  }

}
</script>

<style lang='less'>
@import "AddEdit";
</style>
