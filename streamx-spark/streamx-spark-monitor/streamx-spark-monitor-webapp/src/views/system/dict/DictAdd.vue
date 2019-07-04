<template>
    <a-drawer
            title="新增字典"
            :maskClosable="false"
            width=650
            placement="right"
            :closable="false"
            @close="onClose"
            :visible="dictAddVisiable"
            style="height: calc(100% - 55px);overflow: auto;padding-bottom: 53px;">
        <a-form :form="form">
            <a-form-item label='键' v-bind="formItemLayout">
                <a-input-number style="width: 100%"
                                v-model="dict.keyy"
                                v-decorator="['keyy',
                   {rules: [
                    { required: true, message: '不能为空'}
                  ]}]"/>
            </a-form-item>
            <a-form-item label='值' v-bind="formItemLayout">
                <a-input v-model="dict.valuee"
                         v-decorator="['valuee',
                   {rules: [
                    { required: true, message: '不能为空'},
                    { max: 20, message: '长度不能超过20个字符'}
                  ]}]"/>
            </a-form-item>
            <a-form-item label='表名' v-bind="formItemLayout">
                <a-input v-model="dict.tableName"
                         v-decorator="['tableName',
                   {rules: [
                    { required: true, message: '不能为空'},
                    { max: 20, message: '长度不能超过20个字符'}
                  ]}]"/>
            </a-form-item>
            <a-form-item label='字段' v-bind="formItemLayout">
                <a-input v-model="dict.fieldName"
                         v-decorator="['fieldName',
                   {rules: [
                    { required: true, message: '不能为空'},
                    { max: 20, message: '长度不能超过20个字符'}
                  ]}]"/>
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
        labelCol: {span: 3},
        wrapperCol: {span: 18}
    }
    export default {
        name: 'DictAdd',
        props: {
            dictAddVisiable: {
                default: false
            }
        },
        data() {
            return {
                loading: false,
                formItemLayout,
                form: this.$form.createForm(this),
                dict: {}
            }
        },
        methods: {
            reset() {
                this.loading = false
                this.dict = {}
                this.form.resetFields()
            },
            onClose() {
                this.reset()
                this.$emit('close')
            },
            handleSubmit() {
                this.form.validateFields((err, values) => {
                    if (!err) {
                        this.$post('dict', {
                            ...this.dict
                        }).then(() => {
                            this.reset()
                            this.$emit('success')
                        }).catch(() => {
                            this.loading = false
                        })
                    }
                })
            }
        }
    }
</script>
