<template>
  <a-drawer
    title="修改定时任务"
    :maskClosable="false"
    width=650
    placement="right"
    :closable="false"
    @close="onClose"
    :visible="jobEditVisiable"
    style="height: calc(100% - 55px);overflow: auto;padding-bottom: 53px;">
    <a-form :form="form">
      <a-form-item label='Bean名称' v-bind="formItemLayout">
        <a-input style="width: 100%"
                 v-decorator="['beanName',
                   {rules: [
                    { required: true, message: 'Bean名称不能为空'},
                    { max: 50, message: '长度不能超过50个字符'}
                  ]}]"/>
      </a-form-item>
      <a-form-item label='方法名称' v-bind="formItemLayout">
        <a-input v-decorator="['methodName',
                   {rules: [
                    { required: true, message: '方法名称不能为空'},
                    { max: 50, message: '长度不能超过50个字符'}
                  ]}]"/>
      </a-form-item>
      <a-form-item label='方法参数' v-bind="formItemLayout">
        <a-input v-decorator="['params',
                   {rules: [
                    { max: 50, message: '长度不能超过50个字符'}
                  ]}]"/>
      </a-form-item>
      <a-form-item label='Cron表达式'
                   v-bind="formItemLayout"
                   :validateStatus="validateStatus"
                   :help="help">
        <a-input @blur="checkCron" v-decorator="['cronExpression']">
          <a-icon slot="addonAfter" type="read" style="cursor: pointer" @click="open"/>
        </a-input>
      </a-form-item>
      <a-form-item label='备注信息' v-bind="formItemLayout">
        <a-textarea
          :rows="4"
          v-decorator="[
          'remark',
          {rules: [
            { max: 100, message: '长度不能超过100个字符'}
          ]}]">
        </a-textarea>
      </a-form-item>
    </a-form>
    <div class="drawer-bootom-button">
      <a-button style="margin-right: .8rem" @click="onClose">取消</a-button>
      <a-button @click="handleSubmit" type="primary" :loading="loading">提交</a-button>
    </div>
  </a-drawer>
</template>
<script>
const formItemLayout = {
  labelCol: { span: 4 },
  wrapperCol: { span: 18 }
}
export default {
  name: 'JobEdit',
  props: {
    jobEditVisiable: {
      default: false
    }
  },
  data () {
    return {
      loading: false,
      formItemLayout,
      form: this.$form.createForm(this),
      validateStatus: '',
      help: '',
      job: {}
    }
  },
  methods: {
    reset () {
      this.loading = false
      this.validateStatus = this.help = ''
      this.job = {}
      this.form.resetFields()
    },
    onClose () {
      this.reset()
      this.$emit('close')
    },
    open () {
      window.open('http://cron.qqe2.com/')
    },
    setFormValues ({...job}) {
      this.job.jobId = job.jobId
      this.job.status = job.status
      let fields = ['beanName', 'methodName', 'params', 'cronExpression', 'remark']
      Object.keys(job).forEach((key) => {
        if (fields.indexOf(key) !== -1) {
          this.form.getFieldDecorator(key)
          let obj = {}
          obj[key] = job[key]
          this.form.setFieldsValue(obj)
        }
      })
    },
    handleSubmit () {
      if (this.validateStatus !== 'success') {
        this.checkCron()
      }
      this.form.validateFields((err, values) => {
        if (!err) {
          let job = this.form.getFieldsValue()
          job.jobId = this.job.jobId
          job.status = this.job.status
          this.$put('job', {
            ...job
          }).then(() => {
            this.reset()
            this.$emit('success')
          }).catch(() => {
            this.loading = false
          })
        }
      })
    },
    checkCron () {
      let cron = this.form.getFieldValue('cronExpression')
      if (cron.length) {
        this.$get('job/cron/check?cron=' + cron).then((r) => {
          if (!r.data) {
            this.validateStatus = 'error'
            this.help = '请填写合法的Cron表达式'
          } else {
            this.validateStatus = 'success'
            this.help = ''
          }
        })
      } else {
        this.validateStatus = 'error'
        this.help = '请填写Cron表达式'
      }
    }
  }
}
</script>
