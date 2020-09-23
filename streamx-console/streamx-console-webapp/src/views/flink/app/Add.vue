<template>
  <a-card :body-style="{padding: '24px 32px'}" :bordered="false">
    <a-form @submit="handleSubmit" :form="form">

      <a-form-item
        label="Project"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
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
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
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
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
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
        v-if="appType == 2"
        label="Program Jar"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
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
        v-if="appType == 2"
        label="Program Main"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
        <a-input
          type="text"
          placeholder="请输入Main class"
          v-decorator="[ 'mainClass', {rules: [{ required: true, message: '请输入Main class'}]} ]"/>
      </a-form-item>

      <a-form-item
        v-if="appType == 1"
        label="Application conf"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
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

      <a-form-item
        label="Application Name"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
        <a-input
          type="text"
          placeholder="请输入任务名称"
          v-decorator="['jobName',{ rules: [{ validator: handleCheckJobName,trigger:'submit' } ]}]"/>
      </a-form-item>

      <a-form-item
        label="Parallelism"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
        <a-input-number
          :min="1"
          :step="1"
          placeholder="The parallelism with which to run the program"
          v-decorator="['parallelism']"/>
      </a-form-item>

      <a-form-item
        label="Slots"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
        <a-input-number
          :min="1"
          :step="1"
          placeholder="Number of slots per TaskManager"
          v-decorator="['slot']"/>
      </a-form-item>

      <a-form-item
        label="Run Options"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
        <a-select
          showSearch
          allowClear
          mode="multiple"
          :maxTagCount="maxTagCount"
          placeholder="请选择要设置的资源参数"
          @change="handleConf"
          v-decorator="['options']">
          <a-select-opt-group label="run options">
            <a-select-option
              v-for="(conf,index) in options"
              v-if="conf.group === 'run'"
              :key="index"
              :value="conf.name">
              {{ conf.key }} ( {{ conf.name }} )
            </a-select-option>
          </a-select-opt-group>
          <a-select-opt-group label="yarn-cluster options">
            <a-select-option
              v-for="(conf,index) in options"
              v-if="conf.group === 'yarn-cluster'"
              :key="index"
              :value="conf.name">
              {{ conf.key }} ( {{ conf.name }} )
            </a-select-option>
          </a-select-opt-group>
        </a-select>
      </a-form-item>

      <a-form-item
        class="conf-item"
        v-for="(conf,index) in options"
        v-if="configItems.includes(conf.name)"
        :key="index"
        :label="conf.key"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
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
          v-decorator="[`${conf.name}`,{ rules:[{ validator: conf.validator, trigger:'submit'} ]}]"/>
        <span v-if="conf.type === 'switch'" class="conf-switch">({{ conf.placeholder }})</span>
        <p class="conf-desc">{{ conf.description }}</p>
      </a-form-item>

      <a-form-item
        label="Dynamic Option"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
        <a-textarea
          rows="4"
          name="dynamicOptions"
          placeholder="$key=$value,多个参数换行 (-D <arg>)"
          v-decorator="['dynamicOptions']"/>
      </a-form-item>

      <a-form-item
        label="Program Args"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
        <a-textarea
          rows="4"
          name="args"
          placeholder="<arguments>"
          v-decorator="['args']"/>
      </a-form-item>

      <a-form-item
        label="Description"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
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

    <conf ref="confEdit" @close="handleEditConfClose" @ok="handleEditConfOk" :visiable="confEdit.visiable"></Conf>

  </a-card>
</template>

<script>

import { select, modules, listConf, jars } from '@api/project'
import { create, exists, name, readConf, main } from '@api/application'
import Conf from './Conf'
import configOptions from './option'

const Base64 = require('js-base64').Base64

export default {
  name: 'AppAdd',
  components: { Conf },
  data () {
    return {
      maxTagCount: 1,
      projectList: [],
      projectId: null,
      module: null,
      moduleList: [],
      jars: [],
      app: null,
      appType: 0,
      switchDefaultValue: true,
      config: null,
      configOverride: null,
      configSource: [],
      configItems: [],
      form: null,
      options: configOptions,
      confEdit: {
        type: Boolean,
        default: false
      }
    }
  },

  mounted () {
    this.select()
  },

  beforeMount () {
    this.form = this.$form.createForm(this)
    this.options.forEach((item, index, array) => {
      this.form.getFieldDecorator(item.name, { initialValue: item.value, preserve: true })
    })
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

    handleConf (name) {
      this.configItems = name
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

    handleModule (module) {
      this.module = module
      this.form.resetFields(['appType', 'config', 'jobName'])
      this.appType = 0
    },

    handleAppType (val) {
      this.appType = val
      this.handleConfOrJar()
    },

    handleConfOrJar () {
      if (this.module && this.appType) {
        this.form.resetFields(['config', 'jobName'])
        this.configOverride = null
        if (this.appType == 1) {
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
        this.confEdit.visiable = true
        this.$refs.confEdit.set(conf)
      }).catch((error) => {
        this.$message.error(error.message)
      })
    },

    handleEditConfClose () {
      this.confEdit.visiable = false
    },

    handleEditConfOk (value) {
      this.configOverride = value
    },

    handleSubmit (e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        if (!err) {
          const options = {}
          for (const k in values) {
            if (this.configItems.includes(k)) {
              const v = values[k]
              if (v !== '') {
                options[k] = v
              }
            }
          }

          // common params...
          const params = {
            projectId: values.project,
            module: values.module,
            appType: this.appType,
            jobName: values.jobName,
            args: values.args,
            options: JSON.stringify(options),
            dynamicOptions: values.dynamicOptions,
            description: values.description
          }
          if (this.appType == 1) {
            const configVal = this.form.getFieldValue('config')
            const format = configVal.endsWith('.properties') ? 2 : 1
            params['format'] = format
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
        }
      })
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
</style>
