<template>
    <a-drawer
            title="新增定时任务"
            :maskClosable="false"
            width=650
            placement="right"
            :closable="false"
            @close="onClose"
            :visible="jobAddVisiable"
            style="height: calc(100% - 55px);overflow: auto;padding-bottom: 53px;">
        <a-form :form="form">
            <a-form-item label='Bean名称' v-bind="formItemLayout">
                <a-input style="width: 100%"
                         v-model="job.beanName"
                         v-decorator="['beanName',
                   {rules: [
                    { required: true, message: 'Bean名称不能为空'},
                    { max: 50, message: '长度不能超过50个字符'}
                  ]}]"/>
            </a-form-item>
            <a-form-item label='方法名称' v-bind="formItemLayout">
                <a-input v-model="job.methodName"
                         v-decorator="['methodName',
                   {rules: [
                    { required: true, message: '方法名称不能为空'},
                    { max: 50, message: '长度不能超过50个字符'}
                  ]}]"/>
            </a-form-item>
            <a-form-item label='方法参数' v-bind="formItemLayout">
                <a-input v-model="job.params"
                         v-decorator="['params',
                   {rules: [
                    { max: 50, message: '长度不能超过50个字符'}
                  ]}]"/>
            </a-form-item>
            <a-form-item label='Cron表达式'
                         v-bind="formItemLayout"
                         :validateStatus="validateStatus"
                         :help="help">
                <a-input v-model="job.cronExpression" @blur="checkCron">
                    <a-icon slot="addonAfter" type="read" style="cursor: pointer" @click="open"/>
                </a-input>
            </a-form-item>
            <a-form-item label='备注信息' v-bind="formItemLayout">
                <a-textarea
                        :rows="4"
                        v-model="job.remark"
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
        labelCol: {span: 4},
        wrapperCol: {span: 18}
    }
    export default {
        name: 'JobAdd',
        props: {
            jobAddVisiable: {
                default: false
            }
        },
        data() {
            return {
                loading: false,
                formItemLayout,
                form: this.$form.createForm(this),
                job: {
                    cronExpression: ''
                },
                validateStatus: '',
                help: ''
            }
        },
        methods: {
            reset() {
                this.loading = false
                this.validateStatus = this.help = ''
                this.job = {cronExpression: ''}
                this.form.resetFields()
            },
            onClose() {
                this.reset()
                this.$emit('close')
            },
            open() {
                window.open('http://cron.qqe2.com/')
            },
            handleSubmit() {
                if (this.validateStatus !== 'success') {
                    this.checkCron()
                }
                this.form.validateFields((err, values) => {
                    if (!err) {
                        this.$post('job', {
                            ...this.job
                        }).then(() => {
                            this.reset()
                            this.$emit('success')
                        }).catch(() => {
                            this.loading = false
                        })
                    }
                })
            },
            checkCron() {
                let cron = this.job.cronExpression.trim()
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
