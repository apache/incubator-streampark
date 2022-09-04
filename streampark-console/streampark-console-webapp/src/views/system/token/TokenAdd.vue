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
      Add Account Token
    </template>
    <a-form
      :form="form">
      <a-form-item
        label="User"
        v-bind="formItemLayout"
        :validate-status="validateStatus"
        :help="help">
        <a-select
          showSearch
          placeholder="Select a user"
          optionFilterProp="children"
          v-decorator="['userId',{rules: [{ required: true }]}]">
          <a-select-option v-for="i in dataSource" :key="i.userId" :value="i.userId">{{ i.username }}</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item
        label="Description"
        v-bind="formItemLayout"
        :validate-status="validateStatus">
        <a-textarea
          :rows="4"
          v-decorator="[
            'description',
            {rules: [
              { max: 50, message: 'max 50 characters'}
            ]}]" />
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
import {getNoTokenUser} from '@/api/user'

import moment from 'moment'
import message from 'ant-design-vue'

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
        if (tokenInfo.userId == '' || tokenInfo.userId == null) {
          message.error('user is empty !')
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
            this.fetch()
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

      getNoTokenUser().then((resp) => {
        this.dataSource = resp.data
        // 数据加载完毕，关闭loading
        this.loading = false
      })
    }

  }
}
</script>
