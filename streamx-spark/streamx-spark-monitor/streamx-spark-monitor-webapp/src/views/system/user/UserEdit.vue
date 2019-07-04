<template>
    <a-drawer
            title="修改用户"
            :maskClosable="false"
            width=650
            placement="right"
            :closable="false"
            @close="onClose"
            :visible="userEditVisiable"
            style="height: calc(100% - 55px);overflow: auto;padding-bottom: 53px;">
        <a-form :form="form">
            <a-form-item label='用户名' v-bind="formItemLayout">
                <a-input readOnly v-decorator="['username']"/>
            </a-form-item>
            <a-form-item label='邮箱' v-bind="formItemLayout">
                <a-input
                        v-decorator="[
          'email',
          {rules: [
            { type: 'email', message: '请输入正确的邮箱' },
            { max: 50, message: '长度不能超过50个字符'}
          ]}
        ]"/>
            </a-form-item>
            <a-form-item label="手机" v-bind="formItemLayout">
                <a-input
                        v-decorator="['mobile', {rules: [
            { pattern: '^0?(13[0-9]|15[012356789]|17[013678]|18[0-9]|14[57])[0-9]{8}$', message: '请输入正确的手机号'}
          ]}]"/>
            </a-form-item>
            <a-form-item label='角色' v-bind="formItemLayout">
                <a-select
                        mode="multiple"
                        :allowClear="true"
                        style="width: 100%"
                        v-decorator="[
            'roleId',
            {rules: [{ required: true, message: '请选择角色' }]}
          ]">
                    <a-select-option v-for="r in roleData" :key="r.roleId.toString()">{{r.roleName}}</a-select-option>
                </a-select>
            </a-form-item>
            <a-form-item label='部门' v-bind="formItemLayout">
                <a-tree-select
                        :allowClear="true"
                        :dropdownStyle="{ maxHeight: '220px', overflow: 'auto' }"
                        :treeData="deptTreeData"
                        @change="onDeptChange"
                        :value="userDept">
                </a-tree-select>
            </a-form-item>
            <a-form-item label='状态' v-bind="formItemLayout">
                <a-radio-group
                        v-decorator="[
            'status',
            {rules: [{ required: true, message: '请选择状态' }]}
          ]">
                    <a-radio value="0">锁定</a-radio>
                    <a-radio value="1">有效</a-radio>
                </a-radio-group>
            </a-form-item>
            <a-form-item label='性别' v-bind="formItemLayout">
                <a-radio-group
                        v-decorator="[
            'sex',
            {rules: [{ required: true, message: '请选择性别' }]}
          ]">
                    <a-radio value="0">男</a-radio>
                    <a-radio value="1">女</a-radio>
                    <a-radio value="2">保密</a-radio>
                </a-radio-group>
            </a-form-item>
        </a-form>
        <div class="drawer-bootom-button">
            <a-button style="margin-right: .8rem" @click="onClose">取消</a-button>
            <a-button @click="handleSubmit" type="primary" :loading="loading">提交</a-button>
        </div>
    </a-drawer>
</template>
<script>
    import {mapState, mapMutations} from 'vuex'

    const formItemLayout = {
        labelCol: {span: 3},
        wrapperCol: {span: 18}
    }
    export default {
        name: 'UserEdit',
        props: {
            userEditVisiable: {
                default: false
            }
        },
        data() {
            return {
                formItemLayout,
                form: this.$form.createForm(this),
                deptTreeData: [],
                roleData: [],
                userDept: [],
                userId: '',
                loading: false
            }
        },
        computed: {
            ...mapState({
                currentUser: state => state.account.user
            })
        },
        methods: {
            ...mapMutations({
                setUser: 'account/setUser'
            }),
            onClose() {
                this.loading = false
                this.form.resetFields()
                this.$emit('close')
            },
            setFormValues({...user}) {
                this.userId = user.userId
                let fields = ['username', 'email', 'status', 'sex', 'mobile']
                Object.keys(user).forEach((key) => {
                    if (fields.indexOf(key) !== -1) {
                        this.form.getFieldDecorator(key)
                        let obj = {}
                        obj[key] = user[key]
                        this.form.setFieldsValue(obj)
                    }
                })
                if (user.roleId) {
                    this.form.getFieldDecorator('roleId')
                    let roleArr = user.roleId.split(',')
                    this.form.setFieldsValue({'roleId': roleArr})
                }
                if (user.deptId) {
                    this.userDept = [user.deptId]
                }
            },
            onDeptChange(value) {
                this.userDept = value
            },
            handleSubmit() {
                this.form.validateFields((err, values) => {
                    if (!err) {
                        this.loading = true
                        let user = this.form.getFieldsValue()
                        user.roleId = user.roleId.join(',')
                        user.userId = this.userId
                        user.deptId = this.userDept
                        this.$put('user', {
                            ...user
                        }).then((r) => {
                            this.loading = false
                            this.$emit('success')
                            // 如果修改用户就是当前登录用户的话，更新其state
                            if (user.username === this.currentUser.username) {
                                this.$get(`user/${user.username}`).then((r) => {
                                    this.setUser(r.data)
                                })
                            }
                        }).catch(() => {
                            this.loading = false
                        })
                    }
                })
            }
        },
        watch: {
            userEditVisiable() {
                if (this.userEditVisiable) {
                    this.$get('role').then((r) => {
                        this.roleData = r.data.rows
                    })
                    this.$get('dept').then((r) => {
                        this.deptTreeData = r.data.rows.children
                    })
                }
            }
        }
    }
</script>
