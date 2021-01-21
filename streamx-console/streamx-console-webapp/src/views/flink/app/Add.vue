<template>
  <a-card :body-style="{padding: '24px 32px'}" :bordered="false">
    <a-form @submit="handleSubmit" :form="form">

      <a-form-item
        label="Job Type"
        :labelCol="{lg: {span: 5}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 16}, sm: {span: 17} }">
        <a-select
          placeholder="请选择作业类型"
          @change="handleJobType"
          v-decorator="[ 'jobType' , {rules: [{ required: true }]} ]">
          <a-select-option value="dataStream">
            DateStream
          </a-select-option>
          <a-select-option value="sql">
            Flink SQL
          </a-select-option>
        </a-select>
      </a-form-item>

      <template v-if="jobType === 'sql'">
        <a-form-item
          label="Environment"
          :labelCol="{lg: {span: 5}, sm: {span: 7}}"
          :wrapperCol="{lg: {span: 16}, sm: {span: 17} }">
          <a-select
            placeholder="请选择Table Environment"
            @change="handleTableEnv"
            v-decorator="[ 'tableEnv', {rules: [{ required: true, validator: handleCheckTableEnv }]} ]">
            <a-select-option value="1">
              StreamTableEnvironment
            </a-select-option>
            <a-select-option value="2">
              TableEnvironment
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item
          label="Flink SQL"
          :labelCol="{lg: {span: 5}, sm: {span: 7}}"
          :wrapperCol="{lg: {span: 16}, sm: {span: 17} }">
          <textarea
            ref="flinkSQL"
            placeholder="Flink SQL">
          </textarea>
          <p class="conf-desc" style="color: RED;margin-bottom: -20px">{{ flinkSQLMsg }}</p>
        </a-form-item>

        <a-form-item
          label="Application conf"
          :labelCol="{lg: {span: 5}, sm: {span: 7}}"
          :wrapperCol="{lg: {span: 16}, sm: {span: 17} }">
          <a-input
            type="text"
            placeholder="请手动设置参数"
            @focus="handleSQLConf"
            v-decorator="['config',{ rules: [{ validator: handleCheckJobName,trigger:'submit' } ]}]"/>
          <p class="conf-desc" style="color: RED;margin-bottom: -20px">{{ sqlConfMsg }}</p>
        </a-form-item>
      </template>

      <template v-else>
        <a-form-item
          label="Project"
          :labelCol="{lg: {span: 5}, sm: {span: 7}}"
          :wrapperCol="{lg: {span: 16}, sm: {span: 17} }">
          <a-select
            showSearch
            optionFilterProp="children"
            :filterOption="filterOption"
            placeholder="请选择项目"
            @change="handleProject"
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
          :labelCol="{lg: {span: 5}, sm: {span: 7}}"
          :wrapperCol="{lg: {span: 16}, sm: {span: 17} }">
          <a-select
            label="应用"
            showSearch
            optionFilterProp="children"
            :filterOption="filterOption"
            placeholder="请选择模块"
            @change="handleModule"
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
          :labelCol="{lg: {span: 5}, sm: {span: 7}}"
          :wrapperCol="{lg: {span: 16}, sm: {span: 17} }">
          <a-select
            placeholder="请选择Application type"
            @change="handleAppType"
            v-decorator="[ 'appType', {rules: [{ required: true, message: '请选择模块'}]} ]">
            <a-select-option value="1">
              StreamX Flink
            </a-select-option>
            <a-select-option value="2">
              Apache Flink
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item
          v-if="appType === 2"
          label="Program Jar"
          :labelCol="{lg: {span: 5}, sm: {span: 7}}"
          :wrapperCol="{lg: {span: 16}, sm: {span: 17} }">
          <a-select
            placeholder="请选择jar"
            @change="handleJars"
            v-decorator="[ 'jar', {rules: [{ required: true }] }]">
            <a-select-option
              v-for="(jar,index) in jars"
              :key="index"
              :value="jar">
              {{ jar }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item
          v-if="appType === 2"
          label="Program Main"
          :labelCol="{lg: {span: 5}, sm: {span: 7}}"
          :wrapperCol="{lg: {span: 16}, sm: {span: 17} }">
          <a-input
            type="text"
            placeholder="请输入Main class"
            v-decorator="[ 'mainClass', {rules: [{ required: true, message: '请输入Main class'}]} ]"/>
        </a-form-item>

        <a-form-item
          v-if="appType === 1"
          label="Application conf"
          :labelCol="{lg: {span: 5}, sm: {span: 7}}"
          :wrapperCol="{lg: {span: 16}, sm: {span: 17} }">
          <a-tree-select
            :dropdownStyle="{ maxHeight: '400px', overflow: 'auto' }"
            :treeData="configSource"
            placeholder="请选择配置文件"
            treeDefaultExpandAll
            @change="handleJobName"
            v-decorator="[ 'config', {rules: [{ required: true, validator: handleCheckConfig }]} ]">
            <template slot="suffixIcon" v-if="this.form.getFieldValue('config')">
              <a-icon
                type="setting"
                theme="twoTone"
                twoToneColor="#4a9ff5"
                @click.stop="handleEditConfig()"
                title="编辑配置">
              </a-icon>
            </template>
          </a-tree-select>
        </a-form-item>
      </template>

      <a-form-item
        label="Application Name"
        :labelCol="{lg: {span: 5}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 16}, sm: {span: 17} }">
        <a-input
          type="text"
          placeholder="请输入任务名称"
          v-decorator="['jobName',{ rules: [{ validator: handleCheckJobName,trigger:'submit' } ]}]"/>
      </a-form-item>

      <a-form-item
        label="Parallelism"
        :labelCol="{lg: {span: 5}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 16}, sm: {span: 17} }">
        <a-input-number
          :min="1"
          :step="1"
          placeholder="The parallelism with which to run the program"
          v-decorator="['parallelism']"/>
      </a-form-item>

      <a-form-item
        label="Task Slots"
        :labelCol="{lg: {span: 5}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 16}, sm: {span: 17} }">
        <a-input-number
          :min="1"
          :step="1"
          placeholder="Number of slots per TaskManager"
          v-decorator="['slot']"/>
      </a-form-item>

      <a-form-item
        label="Run Options"
        :labelCol="{lg: {span: 5}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 16}, sm: {span: 17} }">
        <a-select
          showSearch
          allowClear
          mode="multiple"
          :maxTagCount="runMaxTagCount"
          placeholder="请选择要设置的资源参数"
          @change="handleConf"
          v-decorator="['runOptions']">
          <a-select-option
            v-for="(conf,index) in options"
            v-if="conf.group === 'run'"
            :key="index"
            :value="conf.key">
            {{ conf.opt }} ( {{ conf.name }} )
          </a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item
        class="conf-item"
        v-for="(conf,index) in options"
        v-if="configItems.includes(conf.key)"
        :key="index"
        :label="conf.name"
        :labelCol="{lg: {span: 5}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 16}, sm: {span: 17} }">
        <a-input
          v-if="conf.type === 'input'"
          type="text"
          :placeholder="conf.placeholder"
          v-decorator="[`${conf.name}`,{ rules:[{ validator: conf.validator, trigger:'submit'} ]}]"/>
        <a-switch
          v-if="conf.type === 'switch'"
          disabled
          checkedChildren="开"
          unCheckedChildren="关"
          checked-children="true"
          un-checked-children="false"
          v-model="switchDefaultValue"
          v-decorator="[`${conf.name}`]"/>
        <a-input-number
          v-if="conf.type === 'number'"
          :min="conf.min"
          :max="conf.max"
          :defaultValue="conf.defaultValue"
          :step="conf.step"
          v-decorator="[`${conf.name}`,{ rules:[{ validator: conf.validator, trigger:'submit'} ]}]"/>
        <span v-if="conf.type === 'switch'" class="conf-switch">({{ conf.placeholder }})</span>
        <p class="conf-desc">{{ conf | description }}</p>
      </a-form-item>

      <a-form-item
        label="Total Memory Options"
        :labelCol="{lg: {span: 5}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 16}, sm: {span: 17} }">
        <a-select
          showSearch
          allowClear
          mode="multiple"
          :maxTagCount="totalTagCount"
          placeholder="请选择要设置的资源参数"
          @change="handleProcess"
          v-decorator="['totalOptions']">
          <a-select-opt-group label="total memory(Flink 总内存)">
            <a-select-option
              v-for="(conf,index) in options"
              v-if="conf.group === 'total-memory'"
              :key="index"
              :value="conf.key">
              {{ conf.name }}
            </a-select-option>
          </a-select-opt-group>
          <a-select-opt-group label="process memory(进程总内存)">
            <a-select-option
              v-for="(conf,index) in options"
              v-if="conf.group === 'process-memory'"
              :key="index"
              :value="conf.key">
              {{ conf.name }}
            </a-select-option>
          </a-select-opt-group>
        </a-select>
        <p class="conf-desc" style="color: RED;margin-bottom: -20px">注意:不要同时设置 "Flink总内存" 和 "进程总内存",会造成内存配置冲突</p>
      </a-form-item>

      <a-form-item
        class="conf-item"
        v-for="(conf,index) in options"
        v-if="totalItems.includes(conf.key)"
        :key="index"
        :label="conf.name.replace(/.memory/g,'').replace(/\./g,' ')"
        :labelCol="{lg: {span: 5}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 16}, sm: {span: 17} }">
        <a-input-number
          v-if="conf.type === 'number'"
          :min="conf.min"
          :max="conf.max"
          :defaultValue="conf.defaultValue"
          :step="conf.step"
          v-decorator="[`${conf.key}`,{ rules:[{ validator: conf.validator, trigger:'submit'} ]}]"/>
        <span v-if="conf.type === 'switch'" class="conf-switch">({{ conf.placeholder }})</span>
        <p class="conf-desc">{{ conf | description }}</p>
      </a-form-item>

      <a-form-item
        label="JM Memory Options"
        :labelCol="{lg: {span: 5}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 16}, sm: {span: 17} }">
        <a-select
          showSearch
          allowClear
          mode="multiple"
          :maxTagCount="jmMaxTagCount"
          placeholder="请选择要设置的资源参数"
          @change="handleJmMemory"
          v-decorator="['jmOptions']">
          <a-select-option
            v-for="(conf,index) in options"
            v-if="conf.group === 'jobmanager-memory'"
            :key="index"
            :value="conf.key">
            {{ conf.name }}
          </a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item
        class="conf-item"
        v-for="(conf,index) in options"
        v-if="jmMemoryItems.includes(conf.key)"
        :key="index"
        :label="conf.name.replace(/jobmanager.memory./g,'')"
        :labelCol="{lg: {span: 5}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 16}, sm: {span: 17} }">
        <a-input-number
          v-if="conf.type === 'number'"
          :min="conf.min"
          :max="conf.max"
          :defaultValue="conf.defaultValue"
          :step="conf.step"
          v-decorator="[`${conf.key}`,{ rules:[{ validator: conf.validator, trigger:'submit'} ]}]"/>
        <span v-if="conf.type === 'switch'" class="conf-switch">({{ conf.placeholder }})</span>
        <p class="conf-desc">{{ conf | description }}</p>
      </a-form-item>

      <a-form-item
        label="TM Memory Options"
        :labelCol="{lg: {span: 5}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 16}, sm: {span: 17} }">
        <a-select
          showSearch
          allowClear
          mode="multiple"
          :maxTagCount="tmMaxTagCount"
          placeholder="请选择要设置的资源参数"
          @change="handleTmMemory"
          v-decorator="['tmOptions']">
          <a-select-option
            v-for="(conf,index) in options"
            v-if="conf.group === 'taskmanager-memory'"
            :key="index"
            :value="conf.key">
            {{ conf.name }}
          </a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item
        class="conf-item"
        v-for="(conf,index) in options"
        v-if="tmMemoryItems.includes(conf.key)"
        :key="index"
        :label="conf.name.replace(/taskmanager.memory./g,'')"
        :labelCol="{lg: {span: 5}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 16}, sm: {span: 17} }">
        <a-input-number
          v-if="conf.type === 'number'"
          :min="conf.min"
          :max="conf.max"
          :defaultValue="conf.defaultValue"
          :step="conf.step"
          v-decorator="[`${conf.key}`,{ rules:[{ validator: conf.validator, trigger:'submit'} ]}]"/>
        <span v-if="conf.type === 'switch'" class="conf-switch">({{ conf.placeholder }})</span>
        <p class="conf-desc">{{ conf | description }}</p>
      </a-form-item>

      <a-form-item
        label="Dynamic Option"
        :labelCol="{lg: {span: 5}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 16}, sm: {span: 17} }">
        <a-textarea
          rows="4"
          name="dynamicOptions"
          placeholder="$key=$value,多个参数换行 (-D <arg>)"
          v-decorator="['dynamicOptions']"/>
      </a-form-item>

      <a-form-item
        v-if="jobType === 'dataStream'"
        label="Program Args"
        :labelCol="{lg: {span: 5}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 16}, sm: {span: 17} }">
        <a-textarea
          rows="4"
          name="args"
          placeholder="<arguments>"
          v-decorator="['args']"/>
      </a-form-item>

      <a-form-item
        label="Description"
        :labelCol="{lg: {span: 5}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 16}, sm: {span: 17} }">
        <a-textarea
          rows="4"
          name="description"
          placeholder="请输入应用描述"
          v-decorator="['description']"/>
      </a-form-item>

      <a-form-item
        :wrapperCol="{ span: 24 }"
        style="text-align: center">
        <a-button @click="handleGoBack">取消</a-button>
        <a-button htmlType="submit" type="primary" style="margin-left: 15px">提交</a-button>
      </a-form-item>

    </a-form>

    <conf ref="confEdit" @close="handleEditConfClose" @ok="handleEditConfOk" :visiable="confVisiable"></Conf>

  </a-card>
</template>

<script>

import { jars, listConf, modules, select } from '@api/project'
import { create, exists, main, name, readConf } from '@api/application'
import { template } from '@api/config'
import Conf from './Conf'
import configOptions from './option'

import CodeMirror from 'codemirror'
import 'codemirror/theme/darcula.css'
import 'codemirror/theme/cobalt.css'
import 'codemirror/theme/dracula.css'
import 'codemirror/theme/idea.css'
import 'codemirror/theme/erlang-dark.css'
import 'codemirror/theme/rubyblue.css'

import 'codemirror/lib/codemirror.css'
import 'codemirror/mode/shell/shell'
import 'codemirror/mode/sql/sql'
import 'codemirror/addon/hint/show-hint'
import 'codemirror/addon/hint/sql-hint'

const Base64 = require('js-base64').Base64

export default {
  name: 'AppAdd',
  components: { Conf },
  data () {
    return {
      totalTagCount: 1,
      runMaxTagCount: 1,
      jmMaxTagCount: 1,
      tmMaxTagCount: 1,
      jobType: 'sql',
      tableEnv: 1,
      projectList: [],
      projectId: null,
      module: null,
      moduleList: [],
      jars: [],
      app: null,
      flinkSQL: null,
      flinkSQLMsg: null,
      sqlConfMsg: null,
      appType: 0,
      switchDefaultValue: true,
      config: null,
      configOverride: null,
      configSource: [],
      configItems: [],
      totalItems: [],
      jmMemoryItems: [],
      tmMemoryItems: [],
      form: null,
      options: configOptions,
      optionsKeyMapping: {},
      confVisiable: false,
      codeMirror: null
    }
  },

  mounted () {
    this.select()
  },

  beforeMount () {
    this.form = this.$form.createForm(this)
    this.optionsKeyMapping = new Map()
    this.options.forEach((item, index, array) => {
      this.optionsKeyMapping.set(item.key, item)
      this.form.getFieldDecorator(item.key, { initialValue: item.defaultValue, preserve: true })
    })
    this.form.getFieldDecorator('jobType', { initialValue: 'sql' })
    this.form.getFieldDecorator('tableMode', { initialValue: 'streaming' })
    this.$nextTick(() => {
      this.handleCodeMirror()
    })
  },

  filters: {
    description (option) {
      if (option.unit) {
        return option.description + ' (单位' + option.unit + ')'
      } else {
        return option.description
      }
    }
  },

  methods: {

    filterOption (input, option) {
      return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
    },

    select () {
      select().then((resp) => {
        this.projectList = resp.data
      }).catch((error) => {
        this.$message.error(error.message)
      })
    },

    handleJobType (value) {
      this.jobType = value
      if (this.jobType === 'sql') {
        this.$nextTick(() => {
          this.handleCodeMirror()
        })
      } else {
        document.querySelector('.CodeMirror').remove()
        this.codeMirror = null
      }
    },

    handleTableEnv (value) {
      this.tableEnv = value
    },

    handleProject (value) {
      this.projectId = value
      modules({
        id: value
      }).then((resp) => {
        this.moduleList = resp.data
      }).catch((error) => {
        this.$message.error(error.message)
      })
    },

    handleConf (item) {
      this.configItems = item
    },

    handleJmMemory (item) {
      this.jmMemoryItems = item
    },

    handleTmMemory (item) {
      this.tmMemoryItems = item
    },

    handleProcess (item) {
      this.totalItems = item
    },

    handleJobName (confFile) {
      name({
        config: confFile
      }).then((resp) => {
        this.form.setFieldsValue({ 'jobName': resp.data })
      }).catch((error) => {
        this.$message.error(error.message)
      })
    },

    handleSQLConf () {
      template({}).then((resp) => {
        const sqlJobConfig = Base64.decode(resp.data)
        this.confVisiable = true
        this.$refs.confEdit.set(sqlJobConfig)
      }).catch((error) => {
        this.$message.error(error.message)
      })
    },

    handleModule (module) {
      this.module = module
      this.form.resetFields(['appType', 'config', 'jobName'])
      this.appType = 0
    },

    handleAppType (val) {
      this.appType = parseInt(val)
      this.handleConfOrJar()
    },

    handleConfOrJar () {
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

    handleJars (jar) {
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

    handleCheckJobName (rule, value, callback) {
      if (!value) {
        callback(new Error('应用名称不能为空'))
      } else {
        exists({ jobName: value.jobName }).then((resp) => {
          const exists = parseInt(resp.data)
          if (exists === 0) {
            callback()
          } else if (exists === 1) {
            callback(new Error('应用名称必须唯一,该应用名称已经存在'))
          } else {
            callback(new Error('该应用名称已经在yarn中运行,不能重复请检查'))
          }
        })
      }
    },

    handleCheckTableEnv (rule, value, callback) {
      if (!value) {
        callback(new Error('请选择Table Environment'))
      } else {
        callback()
      }
    },

    handleCheckSQL (rule, value, callback) {
      if (!value) {
        callback(new Error('Flink SQL不能为空'))
      } else {
        callback()
      }
    },

    handleCheckConfig (rule, value, callback) {
      if (value) {
        const isProp = value.endsWith('.properties')
        const isYaml = value.endsWith('.yaml') || value.endsWith('.yml')
        if (!isProp && !isYaml) {
          callback(new Error('配置文件必须为.properties 或者.yaml,.yml)'))
        } else {
          callback()
        }
      } else {
        callback(new Error('请选择配置文件'))
      }
    },

    handleEditConfig () {
      const config = this.form.getFieldValue('config')
      readConf({
        config: config
      }).then((resp) => {
        const conf = Base64.decode(resp.data)
        this.confVisiable = true
        this.$refs.confEdit.set(conf)
      }).catch((error) => {
        this.$message.error(error.message)
      })
    },

    handleEditConfClose () {
      this.confVisiable = false
    },

    handleEditConfOk (value) {
      this.configOverride = value
    },

    handleSubmit (e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        if (this.jobType === 'sql') {
          if (this.flinkSQL == null) {
            this.flinkSQLMsg = 'Flink SQL不能为空'
            return
          } else {
            this.flinkSQLMsg = null
          }

          if (this.configOverride == null) {
            this.sqlConfMsg = 'Job参数不能为空'
            return
          }
        }
        if (!err) {
          if (this.jobType === 'dataStream') {
            this.handleSubmitStreaming(values)
          } else {
            this.handleSubmitSQL(values)
          }
        }
      })
    },

    handleSubmitStreaming (values) {
      const options = this.handleFormValue(values)
      // common params...
      const params = {
        jobType: 1,
        projectId: values.project,
        module: values.module,
        appType: this.appType,
        jobName: values.jobName,
        args: values.args,
        options: JSON.stringify(options),
        dynamicOptions: values.dynamicOptions,
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
            this.handleCreate(params)
          }).catch((error) => {
            this.$message.error(error.message)
          })
        } else {
          params['config'] = Base64.enable(this.configOverride)
          this.handleCreate(params)
        }
      } else {
        params['jar'] = this.form.getFieldValue('jar') || null
        params['mainClass'] = this.form.getFieldValue('mainClass') || null
        this.handleCreate(params)
      }
    },

    handleSubmitSQL (values) {
      const options = this.handleFormValue(values)
      options['config'] = Base64.enable(this.configOverride)
      // common params...
      const params = {
        jobType: 2,
        flinkSQL: this.flinkSQL,
        jobName: values.jobName,
        args: values.args,
        options: JSON.stringify(options),
        dynamicOptions: values.dynamicOptions,
        description: values.description
      }
      this.handleCreate(params)
    },

    handleFormValue (values) {
      const options = {}
      for (const k in values) {
        const v = values[k]
        if (v !== '' && v !== undefined) {
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

    handleCodeMirror () {
      this.codeMirror = CodeMirror.fromTextArea(this.$refs.flinkSQL, {
        tabSize: 2,
        styleActiveLine: true,
        lineNumbers: true,
        line: true,
        foldGutter: true,
        styleSelectedText: false,
        matchBrackets: true,
        showCursorWhenSelecting: true,
        extraKeys: { 'Ctrl': 'autocomplete' },
        lint: true,
        readOnly: false,
        autoMatchParens: true,
        mode: 'text/x-mysql',
        theme: 'default',	// 设置主题
        gutters: ['CodeMirror-linenumbers', 'CodeMirror-foldgutter', 'CodeMirror-lint-markers'],
        autoCloseBrackets: true
      })

      this.codeMirror.setSize('auto', '450px')

      this.codeMirror.on('change', (mirror) => {
        this.flinkSQL = mirror.getValue()
      })

      if (this.flinkSQL != null) {
        this.codeMirror.setValue(this.flinkSQL)
        setTimeout(() => {
          this.codeMirror.refresh()
        }, 1)
      }
    },

    handleCreate (params) {
      create(params).then((resp) => {
        const created = resp.data
        if (created) {
          this.$router.push({ path: '/flink/app' })
        } else {
          console.log(created)
        }
      }).catch((error) => {
        this.$message.error(error.message)
      })
    },

    handleGoBack () {
      this.$router.go(-1)
    }

  }

}
</script>

<style scoped>
.ant-list-item-meta-description {
  margin-left: 20px;
}

.ant-list-item-content {
  margin-right: 20px;
}

.conf-item {
  margin-bottom: 0px;
}

.conf-desc {
  color: darkgrey;
  margin-bottom: 0px
}

.conf-switch {
  color: darkgrey;
  margin-left: 5px;
}

.ant-input-number {
  width: 100%;
}

.ant-form-explain {
  margin-top: -5px;
}

>>> .ant-switch-loading, .ant-switch-disabled {
  cursor: not-allowed;
  opacity: unset !important;
}

>>> .CodeMirror {
  border: 1px solid rgba(222,222,222,0.5) !important;
}

>>> .CodeMirror-line, >>>.CodeMirror-code > div {
  height: 20px;
  line-height: 20px;
  font-size: 13px;
  font-weight: 600;
  font-family: 'source-code-pro','Menlo', 'Ubuntu Mono','Consolas', monospace !important;
}

>>> .CodeMirror-linenumber {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', 'Consolas', 'source-code-pro', monospace;
}

>>> .CodeMirror-gutters {
  background: #ebebeb;
  width: 42px;
  border: unset;
}
</style>
