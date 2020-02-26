<template>
  <a-card :bordered="false">
    <div class="table-page-search-wrapper">
      <a-form layout="inline">
        <a-row :gutter="48">
          <div :class="advanced ? null: 'fold'">
            <a-col :md="12" :sm="24">
              <a-form-item
                label="用户名"
                :labelCol="{span: 4}"
                :wrapperCol="{span: 18, offset: 2}">
                <a-input v-model="queryParams.username"/>
              </a-form-item>
            </a-col>
            <a-col :md="12" :sm="24">
              <span class="table-page-search-bar">
                <a-button
                  type="primary"
                  shape="circle"
                  icon="search"
                  @click="search">
                </a-button>
                <a-button
                  type="primary"
                  shape="circle"
                  icon="rest"
                  @click="reset">
                </a-button>

              </span>
            </a-col>
          </div>
        </a-row>
      </a-form>
    </div>

    <div>
      <a-table
        :columns="columns"
        :dataSource="dataSource"
        :pagination="pagination"
        :loading="loading"
        :scroll="{ x: 900 }"
        @change="handleTableChange">
        <template slot="username" slot-scope="text, record">
          <template v-if="record.id === user.id">
            {{ record.username }}&nbsp;&nbsp;<a-tag color="pink">current</a-tag>
          </template>
          <template v-else>
            {{ record.username }}
          </template>
        </template>
        <template slot="operation" slot-scope="text, record">
          <a-icon
            v-permit="'user:kickout'"
            type="poweroff"
            style="color: #f95476"
            @click="takeOut(record)"
            title="踢出"></a-icon>
          <a-badge v-no-permit="'user:kickout'" status="warning" text="无权限"></a-badge>
        </template>
      </a-table>
    </div>
  </a-card>
</template>

<script>
import { mapState, mapMutations } from 'vuex'
import { kickout } from '@/api/passport'

export default {
  name: 'Online',
  data () {
    return {
      advanced: false,
      dataSource: [],
      queryParams: {},
      pagination: {
        defaultPageSize: 10000000,
        hideOnSinglePage: true,
        indentSize: 100
      },
      loading: false
    }
  },
  computed: {
    columns () {
      return [{
        title: '用户名',
        dataIndex: 'username',
        scopedSlots: { customRender: 'username' }
      }, {
        title: '登录时间',
        dataIndex: 'loginTime'
      }, {
        title: '登录IP',
        dataIndex: 'ip'
      }, {
        title: '登录地点',
        dataIndex: 'loginAddress'
      }, {
        title: '操作',
        dataIndex: 'operation',
        scopedSlots: { customRender: 'operation' },
        fixed: 'right',
        width: 120
      }]
    },
    ...mapState({
      user: state => state.user.info
    })
  },
  mounted () {
    this.fetch()
  },
  methods: {
    ...mapMutations({
      setEmpty: 'SET_EMPTY'
    }),
    search () {
      this.fetch({
        ...this.queryParams
      })
    },
    takeOut (record) {
      const that = this
      this.$confirm({
        title: '此操作将踢出该用户?是否继续',
        content: '当您点击确定按钮后，该用户将剔出登录',
        okText: '确定',
        okType: 'danger',
        cancelText: '取消',
        centered: true,
        onOk () {
          kickout({
            id: record.id
          }).then((resp) => {
            if (that.user.id === record.id) {
              that.setEmpty()
              location.reload()
            } else {
              that.search()
            }
            that.$message.success('踢出成功!')
          })
        },
        onCancel () {
          that.$message.info('已取消')
        }
      })
    },
    reset () {
      // 重置查询参数
      this.queryParams = {}
      this.fetch()
    },
    handleTableChange (pagination, filters, sorter) {
      this.fetch({
        ...this.queryParams
      })
    },
    fetch (params = {}) {
      this.loading = true
      this.$get('online', {
        ...params
      }).then((resp) => {
        const data = resp.data
        this.loading = false
        this.dataSource = data
      })
    }
  }
}
</script>
