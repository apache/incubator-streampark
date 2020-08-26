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
          v-decorator="[ 'projectId', {rules: [{ required: true, message: '请选择项目'}]} ]">
          <a-select-option v-for="p in project" :key="p.id" :value="p.id">{{ p.name }}</a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item
        label="Application"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
        <a-select
          label="应用"
          showSearch
          optionFilterProp="children"
          :filterOption="filterOption"
          placeholder="请选择应用"
          @change="handleApp"
          v-decorator="[ 'module', {rules: [{ required: true, message: '请选择应用'}]} ]">
          <a-select-option v-for="p in appList" :key="p.name" :value="p.path">{{ p.name }}</a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item
        label="配置文件"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
        <a-tree-select
          :dropdownStyle="{ maxHeight: '400px', overflow: 'auto' }"
          :treeData="configSource"
          placeholder="请选择配置文件"
          treeDefaultExpandAll
          @change="handleAppName"
          v-decorator="[ 'config', {rules: [{ required: true, message: '请选择配置文件'}]} ]">
          >
        </a-tree-select>
      </a-form-item>

      <a-form-item
        label="作业名称"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
        <a-input type="text"
                 placeholder="请输入任务名称"
                 v-decorator="['appName',{ rules: [{ validator: handleCheckAppName,trigger:'submit' } ]}]"/>
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
          placeholder="请选择要设置的资源参数"
          @change="handleConf"
          v-decorator="['options']">
          <a-select-option
            v-for="(conf,index) in options"
            v-if="conf.group === mode"
            :key="index"
            :value="conf.name">
            {{ conf.key }} ( {{ conf.name }} )
          </a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item
        class="conf_item"
        v-for="(conf,index) in options"
        v-if="configItems.includes(conf.name) && mode === conf.group"
        :key="index"
        :label="conf.key"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
        <a-input v-if="conf.type === 'input'"
                 type="text"
                 :placeholder="conf.placeholder"
                 v-decorator="[`${conf.name}`,{ rules:[{ validator: conf.validator, trigger:'submit'} ]}]"
        />
        <a-switch
          v-if="conf.type === 'switch'"
          @change="(x) => handleSwitch(x,conf)"
          checkedChildren="开"
          unCheckedChildren="关"
          v-decorator="[`${conf.name}`]"/>
        <a-input-number v-if="conf.type === 'number'"
                        :min="conf.min"
                        v-decorator="[`${conf.name}`,{ rules:[{ validator: conf.validator, trigger:'submit'} ]}]"
        />
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
          v-decorator="['dynamicProp']"/>
      </a-form-item>

      <a-form-item
        label="运行参数"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
        <a-textarea
          rows="4"
          name="args"
          placeholder="<arguments>"
          v-decorator="['args']"/>
      </a-form-item>

      <a-form-item
        label="应用描述"
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
        <a-button htmlType="submit" type="primary">提交</a-button>
        <a-button style="margin-left: 8px">保存</a-button>
      </a-form-item>

    </a-form>
  </a-card>
</template>

<script>
import {select, listApp, listConf} from '@api/project'
import {create, exists, name} from '@api/application'

const configOptions = [
  {
    key: '-n',
    name: 'container',
    placeholder: '-n,--container <arg>',
    description: 'TaskManager分配个数',
    group: 'SESSION',
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
    key: '-d',
    name: 'detached',
    placeholder: '-d,--detached',
    description: '以独立模式运行',
    group: 'SESSION',
    type: 'switch',
    value: false,
    validator: (rule, value, callback) => {
      callback()
    }
  },
  {
    key: '-jm',
    name: 'jobManagerMemory',
    placeholder: '-jm,--jobManagerMemory <arg>',
    description: 'JobManager内存大小 (单位: MB)',
    group: 'SESSION',
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
    key: '-m',
    name: 'jobmanager',
    placeholder: '-m,--jobmanager <arg>',
    description: 'JobManager 地址(yarn-cluster)',
    group: 'PER_JOB',
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
    key: '-d',
    name: 'detached',
    placeholder: '-d,--detached',
    description: '以独立模式运行',
    group: 'PER_JOB',
    type: 'switch',
    value: false,
    validator: (rule, value, callback) => {
      callback()
    }
  },
  {
    key: '-yjm',
    name: 'yarnjobManagerMemory',
    placeholder: '-yjm,--yarnjobManagerMemory <arg>',
    description: 'JobManager内存大小 (单位: MB)',
    group: 'PER_JOB',
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
    key: '-ytm',
    name: 'yarntaskManagerMemory',
    placeholder: '-ytm,--yarntaskManagerMemory <arg>',
    description: 'TaskManager内存大小 (单位: MB)',
    group: 'PER_JOB',
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
    key: '-ys',
    name: 'yarnslots',
    placeholder: '-ys,--yarnslots <arg> ',
    description: '每个TaskManager可分配的slot数量',
    group: 'PER_JOB',
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
    key: '-yat',
    name: 'yarnapplicationType',
    placeholder: '-yat,--yarnapplicationType <arg>',
    group: 'PER_JOB',
    type: 'input',
    value: '',
    validator: (rule, value, callback) => {
      callback()
    }
  },
  {
    key: '-yqu',
    name: 'yarnqueue',
    placeholder: '-yqu,--yarnqueue <arg> ',
    group: 'PER_JOB',
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
    key: '-yz',
    name: 'yarnzookeeperNamespace',
    placeholder: '-yz,--yarnzookeeperNamespace <arg>',
    description: 'Namespace to create the Zookeeper sub-paths for high availability mode',
    group: 'PER_JOB',
    type: 'input',
    value: '',
    validator: (rule, value, callback) => {
      callback()
    }
  },
  {
    key: '-ynl',
    name: 'yarnnodeLabel',
    placeholder: '-ynl,--yarnnodeLabel <arg>',
    description: 'Specify YARN node label for the YARN application',
    group: 'PER_JOB',
    type: 'input',
    value: '',
    validator: (rule, value, callback) => {
      callback()
    }
  },
  {
    key: '-sae',
    name: 'shutdownOnAttachedExit',
    placeholder: '-sae,--shutdownOnAttachedExit',
    description: '如果非独立模式提交的任务,当客户端中断,集群执行的job任务也会shutdown',
    group: 'PER_JOB',
    type: 'switch',
    value: '',
    validator: (rule, value, callback) => {
      callback()
    }
  },

  {
    key: '-yq',
    name: 'yarnquery',
    placeholder: '-yq,--yarnquery',
    description: '显示YARN上可用的资源(memory, cores)',
    group: 'PER_JOB',
    type: 'switch',
    value: '',
    validator: (rule, value, callback) => {
      callback()
    }
  },
  {
    key: '-yh',
    name: 'yarnhelp',
    placeholder: '-yh,--yarnhelp',
    description: 'YRAN Session帮助信息',
    group: 'PER_JOB',
    type: 'switch',
    value: '',
    validator: (rule, value, callback) => {
      callback()
    }
  }
]

export default {
  name: 'BaseForm',
  data() {
    return {
      maxTagCount: 1,
      value: 1,
      project: [],
      appList: [],
      app: null,
      config: null,
      configSource: [],
      configItems: [],
      form: null,
      options: configOptions,
      mode: 'PER_JOB',
      deploymentModes: [
        {id: 'APPLICATION', name: 'Application Mode', default: true},
        {id: 'PER_JOB', name: 'Per-Job', default: false},
        {id: 'SESSION', name: 'Session', default: false}
      ]
    }
  },
  mounted() {
    this.select()
  },
  beforeMount() {
    this.form = this.$form.createForm(this)
    configOptions.forEach((item, index, array) => {
      this.form.getFieldDecorator(item.name, {initialValue: item.value, preserve: true})
    })
  },
  methods: {
    filterOption(input, option) {
      return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
    },
    select() {
      select().then((resp) => {
        this.project = resp.data
      }).catch((error) => {
        this.$message.error(error.message)
      })
    },
    handleProject(value) {
      listApp({
        id: value
      }).then((resp) => {
        this.appList = resp.data
      }).catch((error) => {
        this.$message.error(error.message)
      })
    },
    handleConf(name) {
      this.configItems = name
    },

    handleSwitch(bool, conf) {
      const v = {}
      v[conf.name] = bool
      this.form.setFieldsValue(v)
    },

    handleMode(selectMode) {
      if (this.mode !== selectMode) {
        this.configItems = []
        this.form.resetFields(`options`, [])
      }
      this.mode = selectMode
    },

    handleAppName(confFile) {
      name({
        config: confFile
      }).then((resp) => {
        this.form.setFieldsValue({'appName': resp.data})
      }).catch((error) => {
        this.$message.error(error.message)
      })
    },

    handleApp(app) {
      listConf({
        path: app
      }).then((resp) => {
        this.configSource = resp.data
      }).catch((error) => {
        this.$message.error(error.message)
      })
    },

    handleCheckAppName(rule, value, callback) {
      if (!value) {
        callback(new Error('应用名称不能为空'))
      } else {
        exists({
          appName: values.appName
        }).then((resp) => {
          const exists = parseInt(resp.data)
          if (exists === 0) {
            callback()
          }else if(exists === 1) {
            callback(new Error('应用名称必须唯一,该应用名称已经存在'))
          }else {
            callback(new Error('该应用名称已经在yarn中运行,不能重复请检查'))
          }
        })
      }
    },

    // handler
    handleSubmit: function (e) {
      //e.preventDefault()
      this.form.validateFields((err, values) => {
        if (!err) {
          const options = {}
          let shortOptions = ""
          for (const k in values) {
            if (this.configItems.includes(k)) {
              const v = values[k]
              const option = configOptions.filter((elem) => k === elem.name)[0]
              const key = option.key
              if (v !== false && v !== '') {
                options[k] = v
                shortOptions += key + ' '
                if (option.type !== 'switch') {
                  shortOptions += v + ' '
                }
              }
            }
          }
          create({
            projectId: values.projectId,
            module: values.module,
            deployMode: values.mode,
            config: values.config,
            appName: values.appName,
            args: values.args,
            options: JSON.stringify(options),
            shortOptions: shortOptions,
            dynamicProp: values.dynamicProp,
            description: values.description
          }).then((resp) => {
            const created = resp.data
            if (created) {
              this.$router.push({path: '/flink/app'})
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

<style scoped>
.ant-list-item-meta-description {
  margin-left: 20px;
}

.ant-list-item-content {
  margin-right: 20px;
}

.conf_item {
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
</style>
