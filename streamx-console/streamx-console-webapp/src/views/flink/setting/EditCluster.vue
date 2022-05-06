<template>
  <a-card
    :body-style="{padding: '24px 32px'}"
    :bordered="false"
    class="cluster_controller">
    <a-form
      :form="form">
      <a-form-item
        label="Cluster Name"
        style="margin-bottom: 10px; margin-top:40px"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-input
          type="text"
          placeholder="Please enter cluster name"
          v-decorator="['clusterName',{ rules: [{ required: true } ]}]"/>
      </a-form-item>

      <a-form-item
        label="Execution Mode"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-select
          placeholder="Execution Mode"
          v-decorator="[ 'executionMode', {rules: [{ required: true, validator: handleCheckExecMode }] }]"
          @change="handleChangeMode" >
          <a-select-option
            v-for="(o,index) in executionModes"
            :key="`execution_mode_${index}`"
            :disabled="o.disabled"
            :value="o.value">
            {{ o.mode }}
          </a-select-option>
        </a-select>
      </a-form-item>

      <template>
        <a-form-item
          label="Flink Version"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
          <a-select
            placeholder="Flink Version"
            v-decorator="[ 'versionId', {rules: [{ required: true, message: 'Flink Version is required' }] }]"
            @change="handleFlinkVersion">>
            <a-select-option
              v-for="(v,index) in flinkEnvs"
              :key="`version_${index}`"
              :value="v.id">
              {{ v.flinkName }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </template>

      <template v-if="executionMode === 3">
        <a-form-item
          label="Yarn Queue"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
          <a-input
            type="text"
            allowClear
            placeholder="Please enter yarn queue"
            v-decorator="[ 'yarnQueue']">
          </a-input>
        </a-form-item>
      </template>

      <a-form-item
        label="Address"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <template v-if="executionMode === 1">
          <a-input
            name="address"
            placeholder="Please enter cluster address, multiple addresses use ',' split e.g: http://host:port,http://host1:port2"
            v-decorator="['address',{ rules: [{ required: true } ]}]"/>
        </template>
        <template v-else>
          <a-input
            name="address"
            placeholder="Please enter cluster address,  e.g: http://host:port"
            v-decorator="['address',{ rules: [{ required: false } ]}]"/>
        </template>
      </a-form-item>

      <template v-if="executionMode === 3">
        <a-form-item
          label="Yarn Session ClusterId"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
          <a-input
            type="text"
            allowClear
            placeholder="Please enter Yarn Session clusterId"
            v-decorator="[ 'clusterId', {rules: [{ required: false}] }]">
          </a-input>
        </a-form-item>
      </template>

      <template v-if="executionMode === 5">
        <a-form-item
          label="Kubernetes Namespace"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
          <a-input
            type="text"
            placeholder="default"
            allowClear
            v-decorator="[ 'k8sNamespace']">
            <a-dropdown slot="addonAfter" placement="bottomRight">
              <a-menu slot="overlay" trigger="['click', 'hover']">
                <a-menu-item
                  v-for="item in historyRecord.k8sNamespace"
                  :key="item"
                  @click="handleSelectHistoryK8sNamespace(item)"
                  style="padding-right: 60px">
                  <a-icon type="plus-circle"/>
                  {{ item }}
                </a-menu-item>
              </a-menu>
              <a-icon type="history"/>
            </a-dropdown>
          </a-input>
        </a-form-item>

        <a-form-item
          label="Kubernetes ClusterId"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
          <a-input
            type="text"
            placeholder="Please enter Kubernetes clusterId"
            allowClear
            v-decorator="[ 'clusterId', {rules: [{ required: false}] }]">
            <a-dropdown slot="addonAfter" placement="bottomRight">
              <a-menu slot="overlay" trigger="['click', 'hover']">
                <a-menu-item
                  v-for="item in historyRecord.k8sSessionClusterId"
                  :key="item"
                  @click="handleSelectHistoryK8sSessionClusterId(item)"
                  style="padding-right: 60px">
                  <a-icon type="plus-circle"/>
                  {{ item }}
                </a-menu-item>
              </a-menu>
              <a-icon type="history"/>
            </a-dropdown>
          </a-input>
        </a-form-item>

        <a-form-item
          label="Service Account"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
          <a-input
            type="text"
            placeholder="default"
            allowClear
            v-decorator="[ 'serviceAccount']">
            <a-dropdown slot="addonAfter" placement="bottomRight">
              <a-menu slot="overlay" trigger="['click', 'hover']">
                <a-menu-item
                  v-for="item in historyRecord.serviceAccount"
                  :key="item"
                  @click="handleSelectHistoryServiceAccount(item)"
                  style="padding-right: 60px">
                  <a-icon type="plus-circle"/>
                  {{ item }}
                </a-menu-item>
              </a-menu>
              <a-icon type="history"/>
            </a-dropdown>
          </a-input>
        </a-form-item>

        <a-form-item
          label="Kube Conf File"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
          <a-input
            type="text"
            placeholder="~/.kube/config"
            allowClear
            v-decorator="[ 'kubeConfFile']">
            <a-dropdown slot="addonAfter" placement="bottomRight">
              <a-menu slot="overlay" trigger="['click', 'hover']">
                <a-menu-item
                  v-for="item in historyRecord.kubeConfFile"
                  :key="item"
                  @click="handleSelectHistoryKubeConfFile(item)"
                  style="padding-right: 60px">
                  <a-icon type="plus-circle"/>
                  {{ item }}
                </a-menu-item>
              </a-menu>
              <a-icon type="history"/>
            </a-dropdown>
          </a-input>
        </a-form-item>

        <a-form-item
          label="Flink Base Docker Image"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
          <a-input
            type="text"
            placeholder="Please enter the tag of Flink base docker image, such as: flink:1.13.0-scala_2.11-java8"
            allowClear
            v-decorator="[ 'flinkImage', {rules: [{ required: false, message: 'Flink Base Docker Image is required' }] }]">
            <a-dropdown slot="addonAfter" placement="bottomRight">
              <a-menu slot="overlay" trigger="['click', 'hover']">
                <a-menu-item
                  v-for="item in historyRecord.flinkImage"
                  :key="item"
                  @click="handleSelectHistoryFlinkImage(item)"
                  style="padding-right: 60px">
                  <a-icon type="plus-circle"/>
                  {{ item }}
                </a-menu-item>
              </a-menu>
              <a-icon type="history"/>
            </a-dropdown>
          </a-input>
        </a-form-item>

        <a-form-item
          label="Rest-Service Exposed Type"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
          <a-select
            placeholder="kubernetes.rest-service.exposed.type"
            v-decorator="[ 'k8sRestExposedType' ]">
            <a-select-option
              v-for="(o,index) in k8sRestExposedType"
              :key="`k8s_rest_exposed_type_${index}`"
              :value="o.order">
              {{ o.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </template>

      <template v-if="executionMode === 3 || executionMode === 5">
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
          label="Task Slots"
          :label-col="{lg: {span: 5}, sm: {span: 7}}"
          :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
          <a-input-number
            class="ant-input-number"
            :min="1"
            :step="1"
            placeholder="Number of slots per TaskManager"
            v-decorator="['slot']"/>
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
              Explicitly configuring both <span class="note-elem">total process memory</span> and <span
                class="note-elem">total Flink memory</span> is not recommended. It may lead to deployment failures due to potential memory configuration conflicts. Configuring other memory components also requires caution as it can produce further configuration conflicts,
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
            v-decorator="[`${conf.key}`,{ rules:[{ validator: conf.validator } ]}]"/>
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
            v-decorator="[`${conf.key}`,{ rules:[{ validator: conf.validator } ]}]"/>
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
            v-decorator="[`${conf.key}`,{ rules:[{ validator: conf.validator } ]}]"/>
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
            v-decorator="['dynamicOptions']"/>
          <p class="conf-desc">
            <span class="note-info">
              <a-tag color="#2db7f5" class="tag-note">Note</a-tag>
              It works the same as <span class="note-elem">-D$property=$value</span> in CLI mode, Allows specifying multiple generic configuration options. The available options can be found
              <a href="https://ci.apache.org/projects/flink/flink-docs-stable/ops/config.html" target="_blank">here</a>
            </span>
          </p>
        </a-form-item>
      </template>


      <a-form-item
        label="Description"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-textarea
          rows="4"
          name="description"
          placeholder="Please enter description for this application"
          v-decorator="['description']"/>
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
          key="submit"
          @click="handleSubmitCluster"
          type="primary"
          style="margin-left: 15px">
          Submit
        </a-button>
      </a-form-item>
    </a-form>
  </a-card>
</template>

<script>
  import Ellipsis from '@/components/Ellipsis'
  import {list as listFlinkEnv} from '@/api/flinkEnv'
  import {
    list as listCluster,
    update as updateCluster,
    check as checkCluster,
    get as getCluster
  } from '@/api/flinkCluster'
  import {checkHadoop} from '@api/setting'
  import Mergely from '../app/Mergely.vue'
  import configOptions from '../app/Option'
  import SvgIcon from '@/components/SvgIcon'
  import { sysHadoopConf } from '@api/config'
  import { mapActions, mapGetters } from 'vuex'
  import {
    k8sNamespaces as histK8sNamespaces,
    sessionClusterIds as histSessionClusterIds,
    flinkBaseImages as histFlinkBaseImages
  } from '@/api/flinkHistory'

  const Base64 = require('js-base64').Base64

  export default {
    name: 'EditCluster',
    components: {Mergely, Ellipsis, SvgIcon},
    data() {
      return {
        versionId: null,
        scalaVersion: null,
        flinkEnvs: [],
        clusters: [],
        cluster: null,
        resolveOrder: [
          {name: 'parent-first', order: 0},
          {name: 'child-first', order: 1}
        ],
        k8sRestExposedType: [
          {name: 'LoadBalancer', order: 0},
          {name: 'ClusterIP', order: 1},
          {name: 'NodePort', order: 2}
        ],
        executionModes: [
          {mode: 'remote (standalone)', value: 1, disabled: false},
          {mode: 'yarn session', value: 3, disabled: false},
          {mode: 'kubernetes session', value: 5, disabled: false}
        ],
        useSysHadoopConf: false,
        flinkClusterVisible: false,
        configItems: [],
        totalItems: [],
        jmMemoryItems: [],
        tmMemoryItems: [],
        form: null,
        defaultOptions: {},
        options: configOptions,
        optionsKeyMapping: {},
        optionsValueMapping: {},
        dependency: [],
        loading: false,
        submitting: false,
        executionMode: null,
        controller: {
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
          dependency: {
            pom: new Map(),
            jar: new Map()
          },
          pom: {
            value: null,
            error: null,
            defaultValue: ''
          }
        },
        historyRecord: {
          k8sNamespace: [],
          k8sSessionClusterId: [],
          flinkImage: [],
          podTemplate:[],
          jmPodTemplate:[],
          tmPodTemplate:[]
        },
        hadoopConfDrawer: {
          visual: false,
          content: {}
        }
      }
    },

    mounted() {
      const clusterId = this.clusterId()
      if (clusterId) {
        this.handleGet(clusterId)
        this.CleanClusterId()
      } else {
        this.$router.back(-1)
      }
    },

    beforeMount() {
      this.handleInitForm()
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
        return function (group) {
          return this.options.filter(x => x.group === group)
        }
      },
      hasOptions() {
        return function (items) {
          return this.options.filter(x => items.includes(x.key))
        }
      },
      getExecutionMode(){
        return this.executionMode
      }
    },

    methods: {
      ...mapActions(['CleanClusterId']),
      ...mapGetters(['clusterId']),

      handleGet(clusterId) {
        getCluster({id: clusterId}).then((resp) => {
          this.cluster = resp.data
          this.versionId = this.cluster.versionId || null
          this.executionMode = this.cluster.executionMode
          this.defaultOptions = JSON.parse(this.cluster.options || '{}')
          this.handleReset()
        }).catch((error) => {
          this.$message.error(error.message)
        })
      },
      handleReset() {
        this.$nextTick(() => {
          this.form.setFieldsValue({
            'clusterName': this.cluster.clusterName,
            'executionMode': this.executionMode || this.cluster.executionMode,
            'address': this.cluster.address,
            'clusterId': this.cluster.clusterId,
            'description': this.cluster.description,
            'dynamicOptions': this.cluster.dynamicOptions,
            'resolveOrder': this.cluster.resolveOrder,
            'yarnQueue': this.cluster.yarnQueue,
            'versionId': this.cluster.versionId || null,
            'k8sRestExposedType': this.cluster.k8sRestExposedType,
            'flinkImage': this.cluster.flinkImage,
            'serviceAccount': this.cluster.serviceAccount,
            'kubeConfFile': this.cluster.kubeConfFile,
            'flameGraph': this.cluster.flameGraph,
            'k8sNamespace': this.cluster.k8sNamespace
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
              slot = parseInt(v)
            }
            if (k === 'parallelism.default') {
              parallelism = parseInt(v)
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
      },

      filterOption(input, option) {
        return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
      },

      handleInitForm() {
        this.form = this.$form.createForm(this)
        this.optionsValueMapping = new Map()
        this.optionsKeyMapping = new Map()
        this.options.forEach((item, index, array) => {
          this.optionsKeyMapping.set(item.key, item)
          this.optionsValueMapping.set(item.name, item.key)
          this.form.getFieldDecorator(item.key, { initialValue: item.defaultValue, preserve: true })
        })
        this.form.getFieldDecorator('resolveOrder', {initialValue: 0})
        this.form.getFieldDecorator('k8sRestExposedType', {initialValue: 0})
        this.executionMode = null
        listFlinkEnv().then((resp) => {
          if (resp.data.length > 0) {
            this.flinkEnvs = resp.data
            const v = this.flinkEnvs.filter((v) => {
              return v.isDefault
            })[0]
            this.form.getFieldDecorator('versionId', {initialValue: v.id})
            this.versionId = v.id
            this.scalaVersion = v.scalaVersion
          }
        })
        histK8sNamespaces().then((resp) => {
          this.historyRecord.k8sNamespace = resp.data
        })
        histSessionClusterIds({'executionMode': 5}).then((resp) => {
          this.historyRecord.k8sSessionClusterId = resp.data
        })
        histFlinkBaseImages().then((resp) => {
          this.historyRecord.flinkImage = resp.data
        })
      },

      handleClusterFormVisible(flag) {
        this.clusterId = null
        this.flinkClusterVisible = flag
        this.form.resetFields()
      },

      handleSubmitCluster(e) {
        e.preventDefault()
        this.form.validateFields((err, values) => {
          if (!err) {
            const options = this.handleFormValue(values)
            var params = {}
            if(values.executionMode === 1){
              params = {
                id: this.cluster.id,
                clusterId: values.clusterId || null,
                clusterName: values.clusterName,
                executionMode: values.executionMode,
                versionId: values.versionId,
                address: values.address,
                description: values.description,
              }
            }else if(values.executionMode === 3){
              params = {
                id: this.cluster.id,
                clusterId: values.clusterId || null,
                clusterName: values.clusterName,
                executionMode: values.executionMode,
                versionId: values.versionId,
                options: JSON.stringify(options),
                yarnQueue: this.handleYarnQueue(values),
                dynamicOptions: values.dynamicOptions,
                resolveOrder: values.resolveOrder,
                address: values.address,
                flameGraph: values.flameGraph,
                description: values.description
              }
            }else if(values.executionMode === 5){
              params = {
                id: this.cluster.id,
                clusterId: values.clusterId || null,
                clusterName: values.clusterName,
                executionMode: values.executionMode,
                versionId: values.versionId,
                options: JSON.stringify(options),
                dynamicOptions: values.dynamicOptions,
                resolveOrder: values.resolveOrder,
                k8sRestExposedType: values.k8sRestExposedType,
                k8sNamespace: values.k8sNamespace || null,
                serviceAccount: values.serviceAccount,
                k8sConf: values.kubeConfFile,
                flinkImage: values.flinkImage || null,
                address: values.address,
                flameGraph: values.flameGraph,
                description: values.description
              }
            }else{
              this.$message.error('error executionMode.')
            }
            checkCluster(params).then((resp)=> {
              console.log('resp.data：' + resp.data)
              if (resp.data === 'success') {
                updateCluster(params).then((resp)=>{
                  if (resp.data.status) {
                    this.$emit('changeVisble')
                    this.$swal.fire({
                      icon: 'success',
                      title: values.clusterName.concat(' update successful!'),
                      showConfirmButton: false,
                      timer: 2000
                    })
                    this.$router.push({'path': '/flink/setting?activeKey=cluster'})
                    this.flinkClusterVisible = false
                    this.handleClusterAll()
                  } else {
                    this.$swal.fire(
                      resp.data.msg
                    )
                  }
                })
              } else if(resp.data === 'exists'){
                this.$swal.fire(
                  'Failed',
                  'the cluster name: ' + values.clusterName + ' is already exists,please check',
                  'error'
                )
              } else {
                this.$swal.fire(
                  'Failed',
                  'the address is invalid or connection failure, please check',
                  'error'
                )
              }
            })
          }
        })
      },

      handleClusterAll() {
        listCluster({}).then((resp)=>{
          this.clusters = resp.data
        })
      },

      handleChangeMode(mode) {
        this.executionMode = mode
      },

      handleFlinkVersion(id) {
        this.versionId = id
        this.scalaVersion = this.flinkEnvs.find(v => v.id === id).scalaVersion
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

      handleCheckExecMode(rule, value, callback) {
        if (value === null || value === undefined || value === '') {
          callback(new Error('Execution Mode is required'))
        } else {
          if (value === 2 || value === 3 || value === 4) {
            checkHadoop().then((resp) => {
              if (resp.data) {
                callback()
              } else {
                callback(new Error('Hadoop environment initialization failed, please check the environment settings'))
              }
            }).catch((err) => {
              callback(new Error('Hadoop environment initialization failed, please check the environment settings'))
            })
          } else {
            callback()
          }
        }
      },

      handleCheckYarnSessionClusterId(rule, value, callback) {
        if (value === null || value === undefined || value === '') {
          callback(new Error('Yarn session clusterId is required'))
        } else {
          if (!value.startsWith('application')) {
            callback(new Error("Yarn session clusterId is invalid, clusterId must start with 'application'.Please check"))
          } else {
            callback()
          }
        }
      },

      handleCheckKubernetesClusterId(rule, value, callback) {
        const clusterIdReg = /^[a-z]([-a-z0-9]*[a-z0-9])?$/
        if (value === null || value === undefined || value === '') {
          callback(new Error('Kubernetes clusterId is required'))
        } else {
          if (!clusterIdReg.test(value)) {
            callback(new Error("Kubernetes clusterId is invalid, clusterId must consist of lower case alphanumeric characters or '-', start with an alphabetic character, and end with an alphanumeric character.Please check"))
          } else {
            callback()
          }
        }
      },

      handleYarnQueue(values) {
        if ( this.executionMode === 3 ) {
          const queue = values['yarnQueue']
          if (queue != null && queue !== '' && queue !== undefined) {
            return queue
          }
          return 'default'
        }
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

      handleGoBack() {
        this.$router.push({'path': '/flink/setting?activeKey=cluster'})
        // this.$emit('changeVisble')
      },

      handleUseSysHadoopConf(value) {
        this.useSysHadoopConf = value
      },

      handleSelectHistoryK8sNamespace(value) {
        this.form.setFieldsValue({'k8sNamespace': value})
      },

      handleSelectHistoryServiceAccount(value) {
        this.form.setFieldsValue({'serviceAccount': value})
      },

      handleSelectHistoryKubeConfFile(value) {
        this.form.setFieldsValue({'kubeConfFile': value})
      },

      handleSelectHistoryK8sSessionClusterId(value) {
        this.form.setFieldsValue({'clusterId': value})
      },

      handleSelectHistoryFlinkImage(value) {
        this.form.setFieldsValue({'flinkImage': value})
      },

      showSysHadoopConfDrawer() {
        this.hadoopConfDrawer.visual = true
        if (this.hadoopConfDrawer.content == null
          || Object.keys(this.hadoopConfDrawer.content).length === 0
          || this.hadoopConfDrawer.content.size === 0) {
          sysHadoopConf().then((resp) => {
            this.hadoopConfDrawer.content = resp.data
          })
        }
      },

      closeSysHadoopConfDrawer(){
        this.hadoopConfDrawer.visual = false
      }
    }

  }
</script>

<style lang='less'>
  @import "View";
</style>
