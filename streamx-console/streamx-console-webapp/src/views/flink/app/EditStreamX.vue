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
        label="Application"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
        <a-alert :message="app['module']" type="info"/>
      </a-form-item>

      <a-form-item
        label="Application conf"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
        <a-input-group compact>
          <a-select style="width: 25%" default-value="1" @change="handleStrategy">
            <a-select-option value="1">
              use existing
            </a-select-option>
            <a-select-option value="2">
              reselect
            </a-select-option>
          </a-select>

          <a-select
            v-if="strategy === 1"
            style="width: 75%"
            @change="handleChangeConfig"
            v-model="defaultConfigId">
            <a-select-option
              v-for="(ver,index) in configVersions"
              :value="ver.id"
              :key="index">
              <div style="padding-left: 5px">
                <a-button type="primary" shape="circle" size="small" style="margin-right: 10px;">
                  {{ ver.version }}
                </a-button>
                <a-tag color="green" style=";margin-left: 10px;" size="small" v-if="ver.actived">current</a-tag>
              </div>
            </a-select-option>
            <template slot="suffixIcon">
              <a-icon
                type="setting"
                theme="twoTone"
                twoToneColor="#4a9ff5"
                @click.stop="handleEditConfig()"
                title="编辑配置">
              </a-icon>
            </template>
          </a-select>

          <a-tree-select
            style="width: 75%"
            v-if="strategy === 2"
            :dropdownStyle="{ maxHeight: '400px', overflow: 'auto' }"
            :treeData="configSource"
            placeholder="请选择配置文件"
            treeDefaultExpandAll
            @change="handleChangeNewConfig"
            v-decorator="[ 'config', {rules: [{ required: true, message: '请选择配置文件'}]} ]">
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
        </a-input-group>

      </a-form-item>

      <a-form-item
        v-show="strategy === 1 && configVersions.length>1"
        label="Compare conf"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17}}">

        <a-input-group compact>
          <a-select
            style="width: calc(100% - 60px)"
            v-decorator="[ 'compare_conf' ]"
            placeholder="请选择要对比的配置版本"
            mode="multiple"
            @change="handleChangeCompact"
            :maxTagCount="selectTagCount.count2">
            <a-select-option
              v-for="(ver,index) in configVersions"
              :value="ver.id"
              :key="index">
              <div style="padding-left: 5px">
                <a-button type="primary" shape="circle" size="small" style="margin-right: 10px;">
                  {{ ver.version }}
                </a-button>
                <a-tag color="green" style=";margin-left: 10px;" size="small" v-if="ver.actived">current</a-tag>
              </div>
            </a-select-option>
          </a-select>
          <a-button
            :disabled="compareDisabled"
            style="width: 60px"
            @click="handleCompactConf"
            type="primary">
            对比
          </a-button>
        </a-input-group>
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
          v-decorator="['parallelism']" />
      </a-form-item>

      <a-form-item
        label="Task Slots"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
        <a-input-number
          :min="1"
          :step="1"
          placeholder="Number of slots per TaskManager"
          v-decorator="['slot']" />
      </a-form-item>

      <a-form-item
        label="Configuration"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
        <a-select
          showSearch
          allowClear
          mode="multiple"
          :maxTagCount="selectTagCount.count1"
          placeholder="请选择要配置的选项"
          @change="handleConf"
          v-decorator="['configuration']">
          <a-select-option
            v-for="(conf,index) in configuration"
            :key="index"
            :value="conf.key">
            {{ conf.name }}
          </a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item
        label="Run Options"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
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
          :max="conf.max"
          :defaultValue="conf.defaultValue"
          :step="conf.step"
          v-decorator="[`${conf.name}`,{ rules:[{ validator: conf.validator, trigger:'submit'} ]}]"/>
        <span v-if="conf.type === 'switch'" class="conf-switch">({{ conf.placeholder }})</span>
        <p class="conf-desc">{{ conf | description }}</p>
      </a-form-item>

      <a-form-item
        label="Total Memory Options"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
        <a-select
          showSearch
          allowClear
          mode="multiple"
          :maxTagCount="totalTagCount"
          placeholder="请选择要设置的资源参数"
          @change="handleProcess"
          v-decorator="['totalOptions']">
          <a-select-opt-group label="total memory">
            <a-select-option
              v-for="(conf,index) in options"
              v-if="conf.group === 'total-memory'"
              :key="index"
              :value="conf.key">
              {{ conf.name }}
            </a-select-option>
          </a-select-opt-group>
          <a-select-opt-group label="process memory">
            <a-select-option
              v-for="(conf,index) in options"
              v-if="conf.group === 'process-memory'"
              :key="index"
              :value="conf.key">
              {{ conf.name }}
            </a-select-option>
          </a-select-opt-group>
        </a-select>
      </a-form-item>

      <a-form-item
        class="conf-item"
        v-for="(conf,index) in options"
        v-if="totalItems.includes(conf.key)"
        :key="index"
        :label="conf.name.replace(/.memory/g,'').replace(/\./g,' ')"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
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
        label="Jobmanager Memory Options"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
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
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
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
        label="Taskmanager Memory Options"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
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
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
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

    <conf ref="confEdit" @close="handleEditConfClose" @ok="handleEditConfOk" :visiable="confVisiable"></Conf>

  </a-card>

</template>

<script>
import { listConf } from '@api/project'
import { get, update, exists, name, readConf } from '@api/application'
import { list as listVer, get as getVer } from '@api/config'
import { mapActions, mapGetters } from 'vuex'
import Conf from './Conf'
import configOptions from './option'
const Base64 = require('js-base64').Base64

export default {
  name: 'EditStreamX',
  components: { Conf },
  data () {
    return {
      strategy: 1,
      app: null,
      compareDisabled: true,
      selectTagCount: {
        count1: 1,
        count2: 2
      },
      runMaxTagCount: 1,
      totalTagCount: 1,
      jmMaxTagCount: 1,
      tmMaxTagCount: 1,
      switchDefaultValue: true,
      compareConf: [],
      defaultConfigId: null,
      defaultOptions: {},
      configOverride: null,
      configId: null,
      configVersions: [],
      configSource: [],
      configItems: [],
      totalItems: [],
      jmMemoryItems: [],
      tmMemoryItems: [],
      form: null,
      options: configOptions,
      optionsKeyMapping: {},
      optionsValueMapping: {},
      configuration: [
        { key: 'tc', name: ' time characteristic' },
        { key: 'cp', name: ' checkpoints' },
        { key: 'rs', name: ' restart strategy' },
        { key: 'sb', name: ' state backend' }
      ],
      confVisiable: false
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
    this.optionsKeyMapping = new Map()
    this.optionsValueMapping = new Map()
    this.options.forEach((item, index, array) => {
      this.optionsKeyMapping.set(item.key, item)
      this.optionsValueMapping.set(item.name, item.key)
      this.form.getFieldDecorator(item.key, { initialValue: item.defaultValue, preserve: true })
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
    ...mapActions(['CleanAppId']),
    ...mapGetters(['applicationId']),
    filterOption (input, option) {
      return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
    },

    handleGet (appId) {
      get({ id: appId }).then((resp) => {
        this.app = resp.data
        this.configOverride = Base64.decode(this.app.config)
        this.defaultOptions = JSON.parse(this.app.options)
        this.configId = this.app.configId
        this.handleReset()
        this.handleListConfVersion()
        this.handleConfList()
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

    handleChangeNewConfig (confFile) {
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

    handleStrategy (v) {
      this.strategy = parseInt(v)
    },

    handleChangeConfig (v) {
      getVer({ id: v }).then((resp) => {
        this.configOverride = Base64.decode(resp.data.content)
        this.configId = resp.data.id
      })
    },

    handleEditConfig () {
      this.confVisiable = true
      this.$refs.confEdit.set(this.configOverride)
    },

    handleEditConfClose () {
      this.confVisiable = false
    },

    handleEditConfOk (value) {
      this.configOverride = value
    },

    // handler
    handleSubmit: function (e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        if (!err) {
          const options = {}
          for (const k in values) {
            const v = values[k]
            if (v !== '') {
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
                  options[name] = v + unit
                }
              }
            }
          }

          const format = this.strategy === 1 ? this.app.format : (this.form.getFieldValue('config').endsWith('.properties') ? 2 : 1)
          const config = this.configOverride || this.app.config
          const configId = this.strategy === 1 ? this.configId : null

          const params = {
            id: this.app.id,
            config: Base64.encode(config),
            jobName: values.jobName,
            format: format,
            configId: configId,
            args: values.args,
            options: JSON.stringify(options),
            dynamicOptions: values.dynamicOptions,
            description: values.description
          }
          update(params).then((resp) => {
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

    handleListConfVersion () {
      listVer({
        appId: this.app.id,
        pageNo: 1,
        pageSize: 999999
      }).then((resp) => {
        resp.data.records.forEach((value, index) => {
          if (value.actived) {
            this.defaultConfigId = value.id
          }
        })
        this.configVersions = resp.data.records
      })
    },

    handleConfList () {
      listConf({
        id: this.app.projectId,
        module: this.app.module
      }).then((resp) => {
        this.configSource = resp.data
      }).catch((error) => {
        this.$message.error(error.message)
      })
    },

    handleChangeCompact () {
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

    handleCompactConf () {
      getVer({
        id: this.compareConf[0]
      }).then((resp) => {
        const conf1 = Base64.decode(resp.data.content)
        getVer({
          id: this.compareConf[1]
        }).then((resp) => {
          const conf2 = Base64.decode(resp.data.content)
          this.confVisiable = true
          this.$refs.confEdit.compact(conf1, conf2)
        })
      })
    },

    handleReset () {
      this.$nextTick(() => {
        this.form.setFieldsValue({
          'jobName': this.app.jobName,
          'args': this.app.args,
          'description': this.app.description,
          'dynamicOptions': this.app.dynamicOptions,
          'slot': this.defaultOptions.slot,
          'parallelism': this.defaultOptions.parallelism
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
        this.form.setFieldsValue({ 'parallelism': parallelism })
        this.form.setFieldsValue({ 'slot': slot })
        this.form.setFieldsValue({ 'totalOptions': this.totalItems })
        this.form.setFieldsValue({ 'jmOptions': this.jmMemoryItems })
        this.form.setFieldsValue({ 'tmOptions': this.tmMemoryItems })
        this.form.setFieldsValue(fieldValueOptions)
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
