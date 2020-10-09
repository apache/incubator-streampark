<template>
  <a-card :body-style="{padding: '24px 32px'}" :bordered="false">
    <a-form @submit="handleSubmit" :form="form" v-if="app!=null">
      <a-form-item
        label="Project"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
        <a-alert :message="app['projectName']" type="info"/>
      </a-form-item>

      <a-form-item
        label="Module"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
        <a-alert :message="app['module']" type="info"/>
      </a-form-item>

      <a-form-item
        label="Application Type"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
        <a-alert message="Apache Flink" type="info"/>
      </a-form-item>

      <a-form-item
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
        label="Program Main"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
        <a-input
          type="text"
          placeholder="请输入Main class"
          v-decorator="[ 'mainClass', {rules: [{ required: true, message: '请输入Main class'}]} ]"/>
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
        label="Task Slots"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
        <a-input-number
          :min="1"
          :step="1"
          placeholder="Number of slots per TaskManager"
          v-decorator="['yarnslots']"/>
      </a-form-item>

      <a-form-item
        label="Run Options"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
        <a-select
          showSearch
          allowClear
          mode="multiple"
          :maxTagCount="selectTagCount"
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
        class="conf_item"
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
        <a-button @click="handleReset">重置</a-button>
        <a-button htmlType="submit" type="primary" style="margin-left: 15px">提交</a-button>
      </a-form-item>

    </a-form>

  </a-card>

</template>

<script>
import { jars } from '@api/project'
import { get, update, exists, main } from '@api/application'
import { mapActions, mapGetters } from 'vuex'
import configOptions from './option'

export default {
  name: 'EditFlink',
  data () {
    return {
      strategy: 1,
      app: null,
      switchDefaultValue: true,
      selectTagCount: 1,
      defaultOptions: {},
      defaultJar: null,
      configSource: [],
      jars: [],
      configItems: [],
      form: null,
      options: configOptions,
      confEdit: {
        visiable: false
      }
    }
  },

  mounted () {
    const appId = this.applicationId()
    if (appId) {
      this.handleGet(appId)
      this.CleanAppId()
    } else {
      this.$router.back(-1)
    }
  },

  beforeMount () {
    this.form = this.$form.createForm(this)
    this.options.forEach((item, index, array) => {
      this.form.getFieldDecorator(item.name, { initialValue: item.value, preserve: true })
    })
  },

  methods: {
    ...mapActions(['CleanAppId']),
    ...mapGetters(['applicationId']),
    handleGet (appId) {
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

    handleConf (name) {
      this.configItems = name
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

    handleJars (jar) {
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
    handleSubmit: function (e) {
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

          const parallelism = this.form.getFieldValue('parallelism') || null
          const yarnslots = this.form.getFieldValue('yarnslots') || null
          if (parallelism) {
            options['parallelism'] = parallelism
          }

          if (yarnslots) {
            options['yarnslots'] = yarnslots
          }

          update({
            id: this.app.id,
            jobName: values.jobName,
            jar: values.jar,
            mainClass: values.mainClass,
            args: values.args,
            options: JSON.stringify(options),
            dynamicOptions: values.dynamicOptions,
            description: values.description
          }).then((resp) => {
            const updated = resp.data
            if (updated) {
              this.$router.push({ path: '/flink/app' })
            } else {
              console.log(updated)
            }
          }).catch((error) => {
            this.$message.error(error.message)
          })
        }
      })
    },

    handleReset () {
      this.handleJars(this.app.jar)
      this.$nextTick(() => {
        this.form.setFieldsValue({
          'jobName': this.app.jobName,
          'args': this.app.args,
          'jar': this.app.jar,
          'description': this.app.description,
          'dynamicOptions': this.app.dynamicOptions,
          'yarnslots': this.defaultOptions.yarnslots,
          'parallelism': this.defaultOptions.parallelism
        })
      })
      const array = []
      for (const k in this.defaultOptions) {
        if (k !== 'parallelism' && k !== 'yarnslots') {
          array.push(k)
        }
      }
      this.configItems = array
      this.form.setFieldsValue(this.defaultOptions)
      this.$nextTick(() => {
        this.form.setFieldsValue({ 'options': array })
      })
    }

  }

}
</script>

<style scoped>
.ant-list-item-meta-description {
  margin-left: 20px;
}

.ant-alert.ant-alert-no-icon {
  padding: 6px 15px;
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

.ant-tag {
  font-size: 12px;
  line-height: 1.5;
  margin-right: 5px;
  line-height: 20px;
}

>>> .ant-select-selection__choice {
  border: none !important;
}

>>> .ant-switch-loading, .ant-switch-disabled {
  cursor: not-allowed;
  opacity: unset !important;
}

</style>
