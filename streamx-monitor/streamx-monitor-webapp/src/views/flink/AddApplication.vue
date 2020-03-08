<template>
  <a-card :body-style="{padding: '24px 32px'}" :bordered="false">
    <a-form @submit="handleSubmit" :form="form">
      <a-form-item
        label="项目"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
        <a-select
          showSearch
          optionFilterProp="children"
          :filterOption="filterOption"
          placeholder="请选择项目"
          @change="handleProject"
          v-decorator="[ 'project', {rules: [{ required: true, message: '请选择项目'}]} ]">
          <a-select-option v-for="p in project" :key="p.id" :value="p.id">{{ p.name }}</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item
        label="配置文件"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
        <a-tree-select
          :dropdownStyle="{ maxHeight: '400px', overflow: 'auto' }"
          :treeData="configFileData"
          placeholder="请选择配置文件"
          treeDefaultExpandAll
          @change="handleAppName"
          v-decorator="[ 'configFile', {rules: [{ required: true, message: '请选择配置文件'}]} ]">
          >
        </a-tree-select>
      </a-form-item>
      <a-form-item
        label="作业名称"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
        <a-input type="text" placeholder="请输入任务名称" v-decorator="['appName',{ rules: [{ required: true, message: '请输入任务名称' } ]}]"/>
      </a-form-item>
      <a-form-item
        label="部署模式"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
        <a-select
          showSearch
          optionFilterProp="children"
          :filterOption="filterOption"
          placeholder="请选择部署模式"
          @change="handleMode"
          v-decorator="[ 'mode', {rules: [{ required: true, message: '请选择部署模式'}]} ]">
          <a-select-option v-for="p in deploymentModes" :key="p.id" :value="p.id">{{ p.name }}</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item
        label="资源参数"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
        <a-select
          showSearch
          allowClear
          mode="multiple"
          :maxTagCount="maxTagCount"
          ref="confOpt"
          placeholder="请选择要设置的资源参数"
          @change="handleConf"
          v-decorator="['configOpt']">
          <a-select-option v-for="(conf,index) in configOptions" v-if="conf.group === mode" :key="index" :value="conf.name">{{ conf.title }} ( {{ conf.name }} )</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item
        class="conf_item"
        v-for="(conf,index) in configOptions"
        v-if="configItems.includes(conf.name) && mode == conf.group"
        :key="index"
        :label="conf.title"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
        <a-input v-if="conf.type === 'input'" type="text" :placeholder="conf.placeholder" v-decorator="[`${conf.name}`,{ rules:[{ validator: conf.validator, trigger:'submit'} ]}]"/>
        <a-switch
          v-if="conf.type === 'switch'"
          @change="(x) => handleSwitch(x,conf)"
          checkedChildren="开"
          unCheckedChildren="关"
          v-decorator="[`${conf.name}`]"/>
        <a-input-number v-if="conf.type === 'number'" :min="conf.min" v-decorator="[`${conf.name}`,{ rules:[{ validator: conf.validator, trigger:'submit'} ]}]"/>
        <span v-if="conf.type === 'switch'" class="conf-switch">({{ conf.placeholder }})</span>
        <p class="conf-desc">{{ conf.description }}</p>
      </a-form-item>
      <a-form-item
        label="动态参数"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
        <a-textarea
          rows="4"
          name="dynamicProp"
          placeholder="$key=$value,多个参数换行 (-D <arg>)"
          v-decorator="['dynamicProp']" />
      </a-form-item>
      <a-form-item
        label="运行参数"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
        <a-textarea
          rows="4"
          name="args"
          placeholder="<arguments>"
          v-decorator="['args']" />
      </a-form-item>
      <a-form-item
        label="应用描述"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
        <a-textarea
          rows="4"
          name="description"
          placeholder="请输入应用描述"
          v-decorator="['description']" />
      </a-form-item>
      <a-form-item
        :wrapperCol="{ span: 24 }"
        style="text-align: center">
        <a-button htmlType="submit" type="primary">提交</a-button>
        <a-button style="margin-left: 8px">保存</a-button>
      </a-form-item>
    </a-form>
  </a-card>
</template>

<script>
import { select, fileList } from '@/api/project'
import { create, name } from '@/api/application'

const configOptions = [
  {
    title: '-n',
    name: 'container',
    placeholder: '-n,--container <arg>',
    description: 'TaskManager分配个数',
    group: 'session',
    type: 'input',
    value: '',
    validator: (rule, value, callback) => {
      if (!value || value.length === 0) {
        callback(new Error('TaskManager数量不能为空'))
      } else {
        callback()
      }
    }
  },
  {
    title: '-d',
    name: 'detached',
    placeholder: '-d,--detached',
    description: '以独立模式运行',
    group: 'session',
    type: 'switch',
    value: false,
    validator: (rule, value, callback) => {
      callback()
    }
  },
  {
    title: '-jm',
    name: 'jobManagerMemory',
    placeholder: '-jm,--jobManagerMemory <arg>',
    description: 'JobManager内存大小 (单位: MB)',
    group: 'session',
    type: 'number',
    min: 512,
    value: '',
    validator: (rule, value, callback) => {
      if (!value) {
        callback(new Error('JobManager内存不能为空'))
      } else {
        callback()
      }
    }
  },

  // ------------------------------------------------------- YARN -------------------------------------------------------
  {
    title: '-m',
    name: 'jobmanager',
    placeholder: '-m,--jobmanager <arg>',
    description: 'JobManager 地址(yarn-cluster)',
    group: 'yarn',
    type: 'input',
    value: '',
    validator: (rule, value, callback) => {
      if (!value || value.length === 0) {
        callback(new Error('JobManager不能为空'))
      } else {
        callback()
      }
    }
  },
  {
    title: '-d',
    name: 'detached',
    placeholder: '-d,--detached',
    description: '以独立模式运行',
    group: 'yarn',
    type: 'switch',
    value: false,
    validator: (rule, value, callback) => {
      callback()
    }
  },
  {
    title: '-yjm',
    name: 'yarnjobManagerMemory',
    placeholder: '-yjm,--yarnjobManagerMemory <arg>',
    description: 'JobManager内存大小 (单位: MB)',
    group: 'yarn',
    type: 'number',
    min: 512,
    value: '',
    validator: (rule, value, callback) => {
      if (!value) {
        callback(new Error('JobManager内存不能为空'))
      } else {
        callback()
      }
    }
  },
  {
    title: '-ytm',
    name: 'yarntaskManagerMemory',
    placeholder: '-ytm,--yarntaskManagerMemory <arg>',
    description: 'TaskManager内存大小 (单位: MB)',
    group: 'yarn',
    type: 'number',
    min: 512,
    value: '',
    validator: (rule, value, callback) => {
      if (!value) {
        callback(new Error('TaskManager内存不能为空'))
      } else {
        callback()
      }
    }
  },
  {
    title: '-ys',
    name: 'yarnslots',
    placeholder: '-ys,--yarnslots <arg> ',
    description: '每个TaskManager可分配的slot数量',
    group: 'yarn',
    type: 'number',
    min: 1,
    value: '',
    validator: (rule, value, callback) => {
      if (!value) {
        callback(new Error('slots数量不能为空'))
      } else {
        callback()
      }
    }
  },
  {
    title: '-ynm',
    name: 'yarnname',
    placeholder: '-ynm,--yarnname <arg> ',
    description: '自定义应用程序名称(on YARN)',
    group: 'yarn',
    type: 'input',
    value: '',
    validator: (rule, value, callback) => {
      callback()
    }
  },
  {
    title: '-yat',
    name: 'yarnapplicationType',
    placeholder: '-yat,--yarnapplicationType <arg>',
    group: 'yarn',
    type: 'input',
    value: '',
    validator: (rule, value, callback) => {
      callback()
    }
  },
  {
    title: '-yqu',
    name: 'yarnqueue',
    placeholder: '-yqu,--yarnqueue <arg> ',
    group: 'yarn',
    type: 'input',
    description: '指定应用的运行队列(on YARN)',
    value: '',
    validator: (rule, value, callback) => {
      if (!value) {
        callback(new Error('运行队列不能为空'))
      } else {
        callback()
      }
    }
  },
  {
    title: '-yz',
    name: 'yarnzookeeperNamespace',
    placeholder: '-yz,--yarnzookeeperNamespace <arg>',
    description: 'Namespace to create the Zookeeper sub-paths for high availability mode',
    group: 'yarn',
    type: 'input',
    value: '',
    validator: (rule, value, callback) => {
      callback()
    }
  },
  {
    title: '-ynl',
    name: 'yarnnodeLabel',
    placeholder: '-ynl,--yarnnodeLabel <arg>',
    description: 'Specify YARN node label for the YARN application',
    group: 'yarn',
    type: 'input',
    value: '',
    validator: (rule, value, callback) => {
      callback()
    }
  },
  {
    title: '-sae',
    name: 'shutdownOnAttachedExit',
    placeholder: '-sae,--shutdownOnAttachedExit',
    description: '如果非独立模式提交的任务,当客户端中断,集群执行的job任务也会shutdown',
    group: 'yarn',
    type: 'switch',
    value: '',
    validator: (rule, value, callback) => {
      callback()
    }
  },

  {
    title: '-yq',
    name: 'yarnquery',
    placeholder: '-yq,--yarnquery',
    description: '显示YARN上可用的资源(memory, cores)',
    group: 'yarn',
    type: 'switch',
    value: '',
    validator: (rule, value, callback) => {
      callback()
    }
  },
  {
    title: '-yh',
    name: 'yarnhelp',
    placeholder: '-yh,--yarnhelp',
    description: 'YRAN Session帮助信息',
    group: 'yarn',
    type: 'switch',
    value: '',
    validator: (rule, value, callback) => {
      callback()
    }
  }
]

export default {
  name: 'BaseForm',
  data () {
    return {
      maxTagCount: 1,
      value: 1,
      project: [],
      configFile: null,
      configFileData: [],
      configItems: [],
      form: null,
      configOptions: configOptions,
      mode: 'yarn',
      deploymentModes: [
        { id: 'yarn', name: 'PreJob Cluster', default: true },
        { id: 'session', name: 'Session Cluster', default: false }
      ]
    }
  },
  mounted () {
    this.select()
  },
  beforeMount () {
    this.form = this.$form.createForm(this)
    configOptions.forEach((item, index, array) => {
      this.form.getFieldDecorator(item.name, { initialValue: item.value, preserve: true })
    })
  },
  methods: {
    filterOption (input, option) {
      return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
    },
    select () {
      select().then((resp) => {
        this.project = resp.data
      }).catch((error) => {
        this.$message.error(error.message)
      })
    },
    handleProject (value) {
      fileList({
        id: value
      }).then((resp) => {
        this.configFileData = resp.data
      }).catch((error) => {
        this.$message.error(error.message)
      })
    },
    handleConf (name) {
      this.configItems = name
    },
    handleSwitch (bool, conf) {
      const v = {}
      v[conf.name] = bool
      this.form.setFieldsValue(v)
    },
    handleMode (selectMode) {
      if (this.mode !== selectMode) {
        this.configItems = []
        this.form.resetFields(`configOpt`, [])
      }
      this.mode = selectMode
    },
    handleAppName (confFile) {
      name({
        configFile: confFile
      }).then((resp) => {
        this.form.setFieldsValue({ 'appName': resp.data })
      }).catch((error) => {
        this.$message.error(error.message)
      })
    },
    // handler
    handleSubmit: function (e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        if (!err) {
          const projectId = values.project
          const configFile = values.configFile
          const args = values.args
          const dynamicProp = values.dynamicProp
          const description = values.description
          const config = {}
          for (const k in values) {
            if (this.configItems.includes(k)) {
              const v = values[k]
              if (v !== false && v !== '') {
                config[k] = values[k]
              }
            }
          }
          console.log(config)
          create({
            projectId: projectId,
            configFile: configFile,
            args: args,
            dynamicProp: dynamicProp,
            description: description,
            config: JSON.stringify(config)
          }).then((resp) => {
            const created = resp.data
            if (created) {
              this.$router.push({ path: '/flink/app' })
            } else {
              console.log(created)
            }
          }).catch((error) => {
            this.$message.error(error.message)
          })
        }
      })
    }
  }
}
</script>
<style>
  .ant-list-item-meta-description{
    margin-left: 20px;
  }
  .ant-list-item-content{
    margin-right: 20px;
  }
  .conf_item {
    margin-bottom: 0px;
  }
  .conf-desc {
    color: darkgrey;
    margin-bottom:0px
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
</style>
