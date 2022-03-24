<template>
  <a-card
    :body-style="{padding: '24px 32px'}"
    :bordered="false">
    <a-form
      @submit="handleSubmit"
      :form="form">
      <a-form-item
        label="Project Name"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-input
          type="text"
          placeholder="the project name"
          v-decorator="['name',{ rules: [{ validator: handleCheckName,required: true}]}]" />
      </a-form-item>

      <a-form-item
        label="Project Type"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-select
          show-search
          option-filter-prop="children"
          :filter-option="filterOption"
          placeholder="the project type"
          ref="types"
          @change="handleType"
          v-decorator="[ 'type', {rules: [{ required: true, message: 'Project Type is required'}]} ]">
          <a-select-option
            v-for="p in options.types"
            :disabled="p.id === 2"
            :key="p.id"
            :value="p.id">
            {{ p.name }}
          </a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item
        label="CVS"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-select
          show-search
          option-filter-prop="children"
          :filter-option="filterOption"
          placeholder="CVS"
          ref="repository"
          @change="handleResp"
          v-decorator="[ 'repository', {rules: [{ required: true, message: 'CVS is required'}]} ]">
          <a-select-option
            v-for="p in options.repository"
            :disabled="p.id === 2"
            :key="p.id"
            :value="p.id">
            {{ p.name }}
          </a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item
        label="Repository URL"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-input
          type="text"
          placeholder="The Repository URL for this project"
          @change="handleSchema"
          v-decorator="['url',{ rules: [{ required: true, message: 'Repository URL is required'} ]}]" />
      </a-form-item>

      <a-form-item
        label="UserName"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-input
          type="text"
          placeholder="UserName for this project"
          v-decorator="['userName']" />
      </a-form-item>

      <a-form-item
        label="Password"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-input
          type="password"
          placeholder="Password for this project"
          v-decorator="['password']" />
      </a-form-item>

      <a-form-item
        label="Branches"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-select
          show-search
          placeholder="Select a branche"
          option-filter-prop="children"
          :filter-option="filterOption"
          allow-clear
          @click.native="handleBranches"
          v-decorator="['branches',{ rules: [{ required: true } ]}]">
          <a-select-option
            v-for="(k ,i) in brancheList"
            :key="i"
            :value="k">
            {{ k }}
          </a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item
        label="POM"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-input
          type="text"
          placeholder="By default,lookup pom.xml in root path,You can manually specify the module to compile pom.xml"
          v-decorator="['pom',{ rules: [{ message: 'Specifies the module to compile pom.xml If it is not specified, it is found under the root path pom.xml' } ]}]" />
      </a-form-item>

      <a-form-item
        label="Build Argument"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-textarea
          rows="2"
          name="buildArgs"
          placeholder="Build Argument, e.g: -Pprod"
          v-decorator="['buildArgs']" />
      </a-form-item>

      <a-form-item
        label="Description"
        :label-col="{lg: {span: 5}, sm: {span: 7}}"
        :wrapper-col="{lg: {span: 16}, sm: {span: 17} }">
        <a-textarea
          rows="4"
          name="description"
          placeholder="Description for this project"
          v-decorator="['description']" />
      </a-form-item>

      <a-form-item
        :wrapper-col="{ span: 24 }"
        style="text-align: center">
        <a-button
          @click="handleGoBack">
          Back
        </a-button>
        <a-button
          html-type="submit"
          type="primary"
          style="margin-left: 15px">
          Submit
        </a-button>
      </a-form-item>
    </a-form>
  </a-card>
</template>

<script>

import { create,branches,gitcheck,exists } from '@api/project'

export default {
  name: 'BaseForm',
  data () {
    return {
      brancheList: [],
      searchBranche: false,
      options: {
        repository: [
          { id: 1, name: 'GitHub/GitLab', default: true },
          { id: 2, name: 'Subversion', default: false }
        ],
        types: [
          {id: 1, name: 'apache flink',default: true },
          {id: 2, name: 'apache spark',default: false }
        ]
      }
    }
  },

  beforeMount () {
    this.form = this.$form.createForm(this)
  },
  methods: {

    filterOption (input, option) {
      return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0
    },

    handleResp (selected) {
      this.repository = selected
    },

    handleType (selected) {
      this.types = selected
    },

    handleSchema () {
      console.log(this.url)
    },

    handleCheckName(rule, value, callback) {
      if (value === null || value === undefined || value === '') {
        callback(new Error('The Project Name is required'))
      } else {
        exists({ name: value }).then((resp) => {
          const flag = resp.data
          if (flag) {
            callback(new Error('The Project Name is already exists. Please check'))
          } else {
            callback()
          }
        })
      }
    },

    // handler
    handleSubmit: function (e) {
      e.preventDefault()
      this.form.validateFields((err, values) => {
        if (!err) {
          gitcheck({
            url: values.url,
            branches: values.branches,
            userName: values.userName || null,
            password: values.password || null,
          }).then((resp) => {
            if ( resp.data === 0 ) {
              if (this.brancheList.length === 0) {
                this.handleBranches()
              }
              if (this.brancheList.indexOf(values.branches) === -1) {
                this.$swal.fire(
                  'Failed',
                  'branch [' + values.branches + '] does not exist<br>or authentication error,please check',
                  'error'
                )
              } else {
                create({
                  name: values.name,
                  url: values.url,
                  repository: values.repository,
                  type: values.type,
                  branches: values.branches,
                  userName: values.userName,
                  password: values.password,
                  pom: values.pom,
                  buildArgs: values.buildArgs,
                  description: values.description
                }).then((resp) => {
                  const created = resp.data
                  if (created) {
                    this.$router.push({ path: '/flink/project' })
                  } else {
                    this.$swal.fire(
                      'Failed',
                      'Project save failed ..>﹏<.. <br><br>' + resp['message'],
                      'error'
                    )
                  }
                }).catch((error) => {
                  this.$message.error(error.message)
                })
              }
            } else {
              this.$swal.fire(
                'Failed',
                (resp.data === 1?
                  'not authorized ..>﹏<.. <br><br> userName and password is required'
                  : 'authentication error ..>﹏<.. <br><br> please check userName and password'
                ),
                'error'
              )
            }
          })
        }
      })
    },

    handleBranches() {
      this.searchBranche = true
      const form = this.form
      const url = form.getFieldValue('url')
      if (url) {
        const userName = form.getFieldValue('userName') || null
        const password = form.getFieldValue('password') || null
        const userNull = userName === null || userName === undefined || userName === ''
        const passNull = password === null || password === undefined || password === ''
        if ( (userNull && passNull) || (!userNull && !passNull) ) {
          branches({
            url: url,
            userName: userName ,
            password: password
          }).then((resp) => {
            this.brancheList = resp.data
            this.searchBranche = false
          }).catch((error) => {
            this.searchBranche = false
            this.$message.error(error.message)
          })
        }
      }
    },

    handleGoBack () {
      this.$router.go(-1)
    }
  }
}
</script>

<style scoped>
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
