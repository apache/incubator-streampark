<template>
  <a-card :body-style="{padding: '24px 32px'}" :bordered="false">
    <a-form @submit="handleSubmit" :form="form">
      <a-form-item
        label="项目名称"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
        <a-input type="text"
                 placeholder="请输入项目名称"
                 v-decorator="['name',{ rules: [{ required: true, message: '请输入项目名称' } ]}]"/>
      </a-form-item>
      <a-form-item
        label="托管平台"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
        <a-select
          showSearch
          optionFilterProp="children"
          :filterOption="filterOption"
          placeholder="请选择托管平台"
          ref="repository"
          @change="handleResp"
          v-decorator="[ 'repository', {rules: [{ required: true, message: '请选择托管平台'}]} ]">
          <a-select-option v-for="p in options.repository" :key="p.id" :value="p.id">{{ p.name }}</a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item
        label="Repository URL"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
        <a-input type="text"
                 :addonBefore="schema"
                 placeholder="请输入项目仓库地址"
                 @change="handleSchema"
                 v-decorator="['url',{ rules: [{ required: true, message: '请输入项目仓库地址' } ]}]"/>
      </a-form-item>

      <a-form-item
        label="Branches"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
        <a-input type="text"
                 placeholder="请输入项目分支"
                 defaultValue="master"
                 v-decorator="['branches',{ rules: [{ required: true, message: '请输入项目分支' } ],initialValue:'master'}]"/>
      </a-form-item>

      <a-form-item
        label="用户名"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
        <a-input type="text"
                 placeholder="请输入项目访问的认证用户名"
                 v-decorator="['username',{ rules: [{ message: '请输入项目访问的认证用户名' } ]}]"/>
      </a-form-item>

      <a-form-item
        label="密码"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
        <a-input type="password"
                 placeholder="请输入项目访问的认证密码"
                 v-decorator="['password',{ rules: [{ message: '请输入项目访问的认证密码' } ]}]"/>
      </a-form-item>

      <a-form-item
        label="POM"
        :labelCol="{lg: {span: 7}, sm: {span: 7}}"
        :wrapperCol="{lg: {span: 10}, sm: {span: 17} }">
        <a-input type="text"
                 placeholder="默认在根路径下寻找pom.xml,可手动指定要编译模块的pom.xml相对路径"
                 v-decorator="['pom',{ rules: [{ message: '指定要编译的模块pom.xml的相对路径,如不指定则在根路径下寻找pom.xml' } ]}]"/>
      </a-form-item>

      <a-form-item
        label="应用描述"
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
  </a-card>
</template>

<script>

import {create, name} from '@api/project'

export default {
  name: 'BaseForm',
  data() {
    return {
      schema: 'ssh',
      options: {
        repository: [
          {id: 1, name: 'GitHub/GitLab', default: true},
          {id: 2, name: 'Subversion', default: false}
        ]
      }
    }
  },
  mounted() {
    this.select()
  },
  beforeMount() {
    this.form = this.$form.createForm(this)
  },
  methods: {
    filterOption(input, option) {
      return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
    },
    handleResp(selected) {
      this.repository = selected
    },

    handleSchema() {
      console.log(this.url)
    },
    // handler
    handleSubmit: function (e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        if (!err) {
          create({
            name: values.name,
            url: values.url,
            repository: values.repository,
            branches: values.branches,
            username: values.username,
            password: values.password,
            pom: values.pom,
            description: values.description
          }).then((resp) => {
            const created = resp.data
            if (created) {
              this.$router.push({path: '/flink/project'})
            } else {
              this.$notification.error({
                message: '项目保存失败',
                description: resp['message'] ,
              });
            }
          }).catch((error) => {
            this.$message.error(error.message)
          })
        }
      })
    }
  }
}
</script>
<style>
.ant-list-item-meta-description {
  margin-left: 20px;
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
</style>
