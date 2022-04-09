<template>
  <a-drawer
    :mask-closable="false"
    width="650"
    placement="right"
    :closable="true"
    @close="onClose"
    :visible="visible"
    style="height: calc(100% - 55px);overflow: auto;padding-bottom: 53px;">
    <template slot="title">
      <a-icon type="user"/>
      创建令牌
    </template>
    <a-form
      :form="form">
      <a-form-item
        label="User Name"
        v-bind="formItemLayout"
        :validate-status="validateStatus"
        :help="help">

        <a-select
          showSearch
          placeholder="Select a user"
          optionFilterProp="children"
          v-decorator="['username',{rules: [{ required: true }]}]">

          <a-select-option v-for="i in dataSource" :key="i.userId" :value="i.username">{{ i.username }}</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item
        label="Description"
        v-bind="formItemLayout"
        :validate-status="validateStatus">
        <a-input
          v-decorator="['description',{rules: [
            { max: 100, message: '长度不能超过100个字符'}
          ]}]"/>
      </a-form-item>
      <a-form-item
        label="ExpireTime"
        v-bind="formItemLayout"
        :validate-status="validateStatus"
        :help="help">
        <div>
          <a-date-picker
            :disabledDate="tokenDisabledDate"
            format="YYYY-MM-DD"
            v-decorator="['expireTime',{initialValue: this.dateExpire}]"
            disabled/>
        </div>


      </a-form-item>
    </a-form>
    <div
      class="drawer-bootom-button">
      <a-button
        @click="onClose">
        Cancel
      </a-button>
      <a-button
        @click="handleSubmit"
        type="primary"
        :loading="loading">
        Submit
      </a-button>
    </div>
  </a-drawer>
</template>
<script>
import {create} from '@/api/token'
import {list} from '@/api/user'
import moment from 'moment'
import {message} from 'ant-design-vue'


const formItemLayout = {
  labelCol: {span: 4},
  wrapperCol: {span: 18}
}
export default {
  name: 'TokenAdd',
  props: {
    visible: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      loading: false,
      dataSource: [],
      formItemLayout,
      dateExpire: moment('9999-01-01', 'YYYY-MM-DD'),
      form: this.$form.createForm(this),
      validateStatus: '',
      help: ''
    }
  },
  mounted() {
    this.fetch()
  },
  methods: {

    //expireTime不可选择的日期
    tokenDisabledDate(current) {
      return current && current < moment().endOf('day')
    },

    onClose() {
      this.$emit('close')
    },

    reset() {
      this.validateStatus = ''
      this.help = ''
      this.loading = false
      this.form.resetFields()
    },

    handleSubmit() {


      this.form.validateFields((err, tokenInfo) => {
        if (tokenInfo.username == '' || tokenInfo.username == null) {
          message.error('user name is empty !')
          return
        }

        if (tokenInfo.expireTime == null) {
          message.error('expireTime is null !')
          return
        }
        tokenInfo.expireTime = tokenInfo.expireTime.format('YYYY-MM-DD HH:mm:ss')
        create({
          ...tokenInfo
        }).then((r) => {
          if (r.status === 'success') {
            this.reset()
            this.$emit('success')
          }
        }).catch(() => {
          this.loading = false
        })
      })
    },
    fetch(params = {}) {
      // 显示loading
      this.loading = true

      params.pageSize = 99999
      params.pageNum = 1
      if (params.status != null && params.status.length > 0) {
        params.status = params.status[0]
      } else {
        delete params.status
      }
      if (params.sortField === 'createTime') {
        params.sortField = 'create_time'
      }

      list({...params}).then((resp) => {
        this.dataSource = resp.data.records
        // 数据加载完毕，关闭loading
        this.loading = false
      })
    }
  }
}
</script>
