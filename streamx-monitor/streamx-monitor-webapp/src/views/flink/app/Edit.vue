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
            v-if="strategy == 1"
            style="width: 75%"
            @change="handleChangeConfig"
            v-model="defaultConfigId">
            <a-select-option v-for="(ver,i) in configVersions" :value="ver.id">
              <div style="padding-left: 5px">
                <a-button type="primary" shape="circle" size="small" style="margin-right: 10px;">
                  {{ver.version}}
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
            v-if="strategy == 2"
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
        label="Application name"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
        <a-input type="text"
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
        label="Slots"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
        <a-input-number
          :min="1"
          :step="1"
          placeholder="Number of slots per TaskManager"
          v-decorator="['slot']" />
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
        class="conf_item"
        v-for="(conf,index) in options"
        v-if="configItems.includes(conf.name)"
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
        <a-button htmlType="submit" type="primary">提交</a-button>
        <a-button style="margin-left: 8px">保存</a-button>
      </a-form-item>

    </a-form>

    <conf ref="confEdit" @close="handleEditConfClose" @ok="handleEditConfOk" :visiable="confEdit.visiable"></Conf>

  </a-card>

</template>

<script>
import {listConf} from '@api/project'
import {get, update, exists, name, readConf} from '@api/application'
import {list as listVer,get as getVer} from '@api/config'
import { mapActions,mapGetters } from 'vuex'
import Conf from './Conf'
import configOptions from './option'
let Base64 = require('js-base64').Base64

export default {
  name: 'AppEdit',
  components: {Conf},
  data() {
    return {
      maxTagCount: 1,
      strategy:1,
      app: null,
      defaultConfigId: null,
      defaultOptions: {},
      configOverride: null,
      configId: null,
      configVersions: [],
      configSource: [],
      configItems: [],
      form: null,
      options: configOptions,
      confEdit:  {
        visiable: false
      }
    }
  },

  mounted() {
    let appId = this.applicationId()
    if(appId) {
      this.handleGet(appId)
      this.CleanAppId()
    }else {
      this.$router.back(-1)
    }
  },

  beforeMount() {
    this.form = this.$form.createForm(this)
    this.options.forEach((item, index, array) => {
      this.form.getFieldDecorator(item.name, {initialValue: item.value, preserve: true})
    })
  },

  methods: {
    ...mapActions(['CleanAppId']),
    ...mapGetters(['applicationId']),
    filterOption(input, option) {
      return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
    },

    handleGet(appId) {
      get({id: appId }).then((resp) => {
        this.app = resp.data
        this.configOverride = Base64.decode(this.app.config)
        this.defaultOptions = JSON.parse(this.app.options)
        this.configId = this.app.configId
        this.handleSetForm()
        this.handleSetOptions()
        this.handleListConfVersion()
        listConf({
          path: this.app["confPath"]
        }).then((resp) => {
          this.configSource = resp.data
        }).catch((error) => {
          this.$message.error(error.message)
        })
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

    handleChangeNewConfig(confFile) {
      name({
        config: confFile
      }).then((resp) => {
        this.form.setFieldsValue({'jobName': resp.data})
      }).catch((error) => {
        this.$message.error(error.message)
      })
      readConf({
        config: confFile
      }).then((resp) => {
        let conf = Base64.decode(resp.data)
        this.configOverride = conf
      }).catch((error) => {
        this.$message.error(error.message)
      })
    },

    handleCheckJobName(rule, value, callback) {
      if (!value) {
        callback(new Error('应用名称不能为空'))
      } else {
        exists({ jobName: value.jobName }).then((resp) => {
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

    handleStrategy (v) {
      this.strategy = v
    },

    handleChangeConfig (v) {
      getVer({
        id: v
      }).then((resp) => {
        this.configOverride = Base64.decode(resp.data.content)
        this.configId = resp.data.id
      })
    },

    handleEditConfig() {
      this.confEdit.visiable = true
      this.$refs.confEdit.set(this.configOverride )
    },

    handleEditConfClose() {
      this.confEdit.visiable = false
    },

    handleEditConfOk(value) {
      this.configOverride = value
    },

    // handler
    handleSubmit: function (e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        if (!err) {
          const options = {}
          let shortOptions = ""
          for (const k in values) {
            if (this.configItems.includes(k)) {
              const v = values[k]
              const option = this.options.filter((elem) => k === elem.name)[0]
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

          if(values.parallelism) {
            options["parallelism"] = values.parallelism
            shortOptions += ' -p ' + values.parallelism
          }

          if(values.slot) {
            options["yarnslots"] = values.slot
            shortOptions += ' -ys ' + values.slot
          }

          let format = this.strategy == 1 ? this.app.format : (this.form.getFieldValue('config').endsWith(".properties") ? 2:1)
          let config = this.configOverride || this.app.config
          let configId = this.strategy == 1 ? this.configId : null
          update({
            id: this.app.id,
            config: Base64.encode(config),
            jobName: values.jobName,
            format: format,
            configId: configId,
            args: values.args,
            options: JSON.stringify(options),
            shortOptions: shortOptions,
            dynamicOptions: values.dynamicOptions,
            description: values.description
          }).then((resp) => {
            const updated = resp.data
            if (updated) {
              this.$router.push({path: '/flink/app'})
            } else {
              console.log(updated)
            }
          }).catch((error) => {
            this.$message.error(error.message)
          })

        }
      })
    },

    handleSetForm() {
      this.$nextTick(()=>{
        this.form.setFieldsValue({
            'jobName': this.app.jobName,
            'args': this.app.args,
            'description': this.app.description,
            'dynamicOptions': this.app.dynamicOptions,
            'slot': this.defaultOptions.yarnslots,
            'parallelism': this.defaultOptions.parallelism
          })
      })
    },

    handleSetOptions() {
      let array = []
      for(let k in this.defaultOptions) {
        if( k!="parallelism" && k != "slot") {
          array.push(k)
        }
      }
      this.configItems = array
      this.form.setFieldsValue(this.defaultOptions)
      this.$nextTick(()=>{
        this.form.setFieldsValue({'options': array})
      })
    },

    handleListConfVersion() {
      listVer({
        id: this.app.id
      }).then((resp) => {
        resp.data.forEach(((value, index) => {
          if(value.actived) {
            this.defaultConfigId = value.id
          }
        }))
        this.configVersions = resp.data
      })
    }

  },

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

</style>
