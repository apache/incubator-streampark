<template>
  <div style="width: 100%">
    <div class="option-area">
       <a-upload
         class="upload-area"
         :fileList="fileList"
         :remove="handleRemove"
         :disabled="fileList.length === 1"
         :beforeUpload="beforeUpload">
        <a-button>
          <a-icon type="upload" /> 选择.xlsx文件
        </a-button>
      </a-upload>
      <div class="button-area">
        <a-button type="primary" @click="downloadTemplate" style="margin-right: .5rem">
          模板下载
        </a-button>
        <a-button @click="exportExcel" style="margin-right: .5rem">
          导出Excel
        </a-button>
        <a-button
          @click="handleUpload"
          :disabled="fileList.length === 0"
          :loading="uploading">
          {{uploading ? '导入中' : '导入Excel' }}
        </a-button>
      </div>
    </div>
    <a-card :bordered="false" class="card-area">
      <!-- 表格区域 -->
      <a-table ref="TableInfo"
               :columns="columns"
               :dataSource="dataSource"
               :pagination="pagination"
               :loading="loading"
               @change="handleTableChange"
               :scroll="{ x: 900 }">
      </a-table>
    </a-card>
    <import-result
      @close="handleClose"
      :importData="importData"
      :errors="errors"
      :times="times"
      :importResultVisible="importResultVisible">
    </import-result>
  </div>
</template>
<script>
import ImportResult from './ImportResult'

export default {
  components: {ImportResult},
  data () {
    return {
      fileList: [],
      importData: [],
      times: '',
      errors: [],
      uploading: false,
      importResultVisible: false,
      dataSource: [],
      paginationInfo: null,
      pagination: {
        pageSizeOptions: ['10', '20', '30', '40', '100'],
        defaultCurrent: 1,
        defaultPageSize: 10,
        showQuickJumper: true,
        showSizeChanger: true,
        showTotal: (total, range) => `显示 ${range[0]} ~ ${range[1]} 条记录，共 ${total} 条记录`
      },
      loading: false
    }
  },
  computed: {
    columns () {
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
    }
  },
  mounted () {
    this.fetch()
  },
  methods: {
    handleClose () {
      this.importResultVisible = false
    },
    downloadTemplate () {
      this.$download('test/template', {}, '导入模板.xlsx')
    },
    exportExcel () {
      this.$export('test/export')
    },
    handleRemove (file) {
      if (this.uploading) {
        this.$message.warning('文件导入中，请勿删除')
        return
      }
      const index = this.fileList.indexOf(file)
      const newFileList = this.fileList.slice()
      newFileList.splice(index, 1)
      this.fileList = newFileList
    },
    beforeUpload (file) {
      this.fileList = [...this.fileList, file]
      return false
    },
    handleUpload () {
      const { fileList } = this
      const formData = new FormData()
      formData.append('file', fileList[0])
      this.uploading = true
      this.$upload('test/import', formData).then((r) => {
        let data = r.data.data
        if (data.data.length) {
          this.fetch()
        }
        this.importData = data.data
        this.errors = data.error
        this.times = data.time / 1000
        this.uploading = false
        this.fileList = []
        this.importResultVisible = true
      }).catch((r) => {
        console.error(r)
        this.uploading = false
        this.fileList = []
      })
    },
    handleTableChange (pagination, filters, sorter) {
      this.paginationInfo = pagination
      this.fetch()
    },
    fetch (params = {}) {
      this.loading = true
      if (this.paginationInfo) {
        this.$refs.TableInfo.pagination.current = this.paginationInfo.current
        this.$refs.TableInfo.pagination.pageSize = this.paginationInfo.pageSize
        params.pageSize = this.paginationInfo.pageSize
        params.pageNum = this.paginationInfo.current
      } else {
        params.pageSize = this.pagination.defaultPageSize
        params.pageNum = this.pagination.defaultCurrent
      }
      this.$get('test', {
        ...params
      }).then((r) => {
        let data = r.data
        const pagination = { ...this.pagination }
        pagination.total = data.total
        this.loading = false
        this.dataSource = data.rows
        this.pagination = pagination
      })
    }
  }
}
</script>

<style lang="less" scoped>
  @import "../../../static/less/Common";
  .option-area {
    display: inline-block;
    width: 100%;
    padding: 0 .9rem;
    margin: .5rem 0;
    .upload-area {
      display: inline;
      float: left;
      width: 50%
    }
    .button-area {
      margin-left: 1rem;
      display: inline;
      float: right;
      width: 30%;
    }
  }
</style>
