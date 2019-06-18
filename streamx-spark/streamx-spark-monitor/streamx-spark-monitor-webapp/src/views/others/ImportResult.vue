<template>
  <a-modal
    class="import-result"
    title="导入结果"
    v-model="show"
    :centered="true"
    :footer="null"
    :maskClosable="false"
    :width=1000
    @cancel="handleCancel">
    <div class="import-desc">
      <span v-if="importData.length === 0 && errors.length === 0">
        <a-alert
          message="暂无导入记录"
          type="info">
        </a-alert>
      </span>
      <span v-if="importData.length !== 0 && errors.length !== 0">
        <a-alert
          message="部分导入成功"
          type="warning">
          <div slot="description">
            成功导入 <a>{{importData.length}}</a> 条记录，<a>{{errors.length}}</a> 条记录导入失败，共耗时 <a>{{times}}</a> 秒
          </div>
        </a-alert>
      </span>
      <span v-if="importData.length !== 0 && errors.length === 0">
        <a-alert
          message="全部导入成功"
          type="success">
          <div slot="description">
            成功导入 <a>{{importData.length}}</a> 条记录，共耗时 <a>{{times}}</a> 秒
          </div>
        </a-alert>
      </span>
      <span v-if="importData.length === 0 && errors.length !== 0">
        <a-alert
          message="全部导入失败"
          type="error">
          <div slot="description">
            <a>{{errors.length}}</a> 条记录导入失败，共耗时  <a>{{times}}</a> 秒
          </div>
        </a-alert>
      </span>
    </div>
    <a-tabs defaultActiveKey="1">
      <a-tab-pane tab="成功记录" key="1" v-if="importData.length">
        <a-table ref="successTable"
                 :columns="successColumns"
                 :dataSource="importData"
                 :pagination="pagination"
                 :scroll="{ x: 900 }">
        </a-table>
      </a-tab-pane>
      <a-tab-pane tab="失败记录" key="2" v-if="errors.length">
        <a-table ref="errorTable"
                 :columns="errorColumns"
                 :dataSource="errorsData"
                 :pagination="pagination"
                 :scroll="{ x: 900 }">
        </a-table>
      </a-tab-pane>
    </a-tabs>
  </a-modal>
</template>
<script>
export default {
  props: {
    importResultVisible: {
      required: true,
      default: false
    },
    importData: {
      required: true
    },
    errors: {
      required: true
    },
    times: {
      required: true
    }
  },
  data () {
    return {
      pagination: {
        pageSizeOptions: ['5', '10'],
        defaultCurrent: 1,
        defaultPageSize: 5,
        showQuickJumper: true,
        showSizeChanger: true,
        showTotal: (total, range) => `显示 ${range[0]} ~ ${range[1]} 条记录，共 ${total} 条记录`
      }
    }
  },
  computed: {
    errorsData () {
      let arr = []
      for (let i = 0; i < this.errors.length; i++) {
        let error = this.errors[i]
        let e = {}
        for (let field of error.errorFields) {
          e = {...field}
          e.row = error.row
          arr.push(e)
        }
      }
      return arr
    },
    successColumns () {
      return [{
        title: '字段1',
        dataIndex: 'field1'
      }, {
        title: '字段2',
        dataIndex: 'field2'
      }, {
        title: '字段3',
        dataIndex: 'field3'
      }, {
        title: '导入时间',
        dataIndex: 'createTime'
      }]
    },
    errorColumns () {
      return [{
        title: '行',
        dataIndex: 'row',
        customRender: (text, row, index) => {
          return `第 ${text + 1} 行`
        }
      }, {
        title: '列',
        dataIndex: 'cellIndex',
        customRender: (text, row, index) => {
          return `第 ${text + 1} 列`
        }
      }, {
        title: '列名',
        dataIndex: 'column'
      }, {
        title: '错误信息',
        dataIndex: 'errorMessage'
      }]
    },
    show: {
      get: function () {
        return this.importResultVisible
      },
      set: function () {
      }
    }
  },
  methods: {
    handleCancel () {
      this.$emit('close')
    }
  }
}
</script>
<style lang="less">
  .import-result {
    .import-desc {
      margin-bottom: .5rem;
      a {
        font-weight: 600;
      }
    }
  }
</style>
