<template>
  <a-card :bordered="false">
    <div class="table-page-search-wrapper">
      <a-form layout="inline">
        <a-row :gutter="48">
          <div class="fold">
            <a-col :md="8" :sm="24">
              <a-form-item
                label="名称"
                :labelCol="{span: 4}"
                :wrapperCol="{span: 18, offset: 2}">
                <a-input v-model="queryParams.deptName"/>
              </a-form-item>
            </a-col>

            <a-col :md="8" :sm="24">
              <a-form-item
                label="创建时间"
                :labelCol="{span: 4}"
                :wrapperCol="{span: 18, offset: 2}">
                <range-date @change="handleDateChange" ref="createTime"></range-date>
              </a-form-item>
            </a-col>

            <a-col :md="8" :sm="24">
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
                <a-button
                  type="primary"
                  shape="circle"
                  icon="export"
                  v-permit="'dept:export'"
                  @click="exportExcel">
                </a-button>
                <a-button
                  v-permit="'dept:add'"
                  type="primary"
                  shape="circle"
                  icon="plus"
                  @click="add">
                </a-button>
                <a-button
                  v-permit="'dept:delete'"
                  type="primary"
                  shape="circle"
                  icon="minus"
                  @click="batchDelete">
                </a-button>
              </span>
            </a-col>
          </div>
        </a-row>
      </a-form>
    </div>

    <div>
      <!-- 表格区域 -->
      <a-table
        :columns="columns"
        :dataSource="dataSource"
        :pagination="pagination"
        :loading="loading"
        :scroll="{ x: 900 }"
        :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"
        @change="handleTableChange" >
        <template slot="operation" slot-scope="text, record">
          <a-icon
            v-permit="'dept:update'"
            type="setting"
            theme="twoTone"
            twoToneColor="#4a9ff5"
            @click="edit(record)"
            title="修改">
          </a-icon>
          <a-badge v-noPermit="'dept:update'" status="warning" text="无权限"></a-badge>
        </template>
      </a-table>
    </div>
    <!-- 新增部门 -->
    <dept-add
      @success="handleDeptAddSuccess"
      @close="handleDeptAddClose"
      :deptAddVisiable="deptAddVisiable">
    </dept-add>
    <!-- 修改部门 -->
    <dept-edit
      ref="deptEdit"
      @success="handleDeptEditSuccess"
      @close="handleDeptEditClose"
      :deptEditVisiable="deptEditVisiable">
    </dept-edit>
  </a-card>
</template>

<script>
import RangeDate from '@/components/DateTime/RangeDate'
import DeptAdd from './DeptAdd'
import DeptEdit from './DeptEdit'
import { list, remove, $export } from '@/api/dept'

export default {
  name: 'Dept',
  components: { DeptAdd, DeptEdit, RangeDate },
  data () {
    return {
      advanced: false,
      dataSource: [],
      selectedRowKeys: [],
      queryParams: {},
      sortedInfo: null,
      pagination: {
        defaultPageSize: 10000000,
        hideOnSinglePage: true,
        indentSize: 100
      },
      loading: false,
      deptAddVisiable: false,
      deptEditVisiable: false
    }
  },
  computed: {
    columns () {
      let { sortedInfo } = this
      sortedInfo = sortedInfo || {}
      return [{
        title: '名称',
        dataIndex: 'text'
      }, {
        title: '排序',
        dataIndex: 'order'
      }, {
        title: '创建时间',
        dataIndex: 'createTime',
        sorter: true,
        sortOrder: sortedInfo.columnKey === 'createTime' && sortedInfo.order
      }, {
        title: '修改时间',
        dataIndex: 'modifyTime',
        sorter: true,
        sortOrder: sortedInfo.columnKey === 'modifyTime' && sortedInfo.order
      }, {
        title: '操作',
        dataIndex: 'operation',
        scopedSlots: { customRender: 'operation' },
        fixed: 'right',
        width: 120
      }]
    }
  },
  mounted () {
    this.fetch()
  },
  methods: {
    onSelectChange (selectedRowKeys) {
      this.selectedRowKeys = selectedRowKeys
    },
    handleDeptAddClose () {
      this.deptAddVisiable = false
    },
    handleDeptAddSuccess () {
      this.deptAddVisiable = false
      this.$message.success('新增部门成功')
      this.fetch()
    },
    add () {
      this.deptAddVisiable = true
    },
    handleDeptEditClose () {
      this.deptEditVisiable = false
    },
    handleDeptEditSuccess () {
      this.deptEditVisiable = false
      this.$message.success('修改部门成功')
      this.fetch()
    },
    edit (record) {
      this.deptEditVisiable = true
      this.$refs.deptEdit.setFormValues(record)
    },
    handleDateChange (value) {
      if (value) {
        this.queryParams.createTimeFrom = value[0]
        this.queryParams.createTimeTo = value[1]
      }
    },
    batchDelete () {
      if (!this.selectedRowKeys.length) {
        this.$message.warning('请选择需要删除的记录')
        return
      }
      const that = this
      that.$confirm({
        title: '确定删除所选中的记录?',
        content: '当您点击确定按钮后，这些记录将会被彻底删除',
        okText: '确定',
        okType: 'danger',
        cancelText: '取消',
        onOk () {
          remove({
            deptIds: that.selectedRowKeys.join(',')
          }).then(() => {
            that.$message.success('删除成功')
            that.selectedRowKeys = []
            that.fetch()
          })
        },
        onCancel () {
          that.selectedRowKeys = []
          that.$message.info('已取消删除')
        }
      })
    },
    exportExcel () {
      const { sortedInfo } = this
      let sortField, sortOrder
      // 获取当前列的排序和列的过滤规则
      if (sortedInfo) {
        sortField = sortedInfo.field
        sortOrder = sortedInfo.order
      }
      $export({
        sortField: sortField,
        sortOrder: sortOrder,
        ...this.queryParams
      })
    },
    search () {
      const { sortedInfo } = this
      let sortField, sortOrder
      // 获取当前列的排序和列的过滤规则
      if (sortedInfo) {
        sortField = sortedInfo.field
        sortOrder = sortedInfo.order
      }
      this.fetch({
        sortField: sortField,
        sortOrder: sortOrder,
        ...this.queryParams
      })
    },
    reset () {
      // 取消选中
      this.selectedRowKeys = []
      // 重置列排序规则
      this.sortedInfo = null
      // 重置查询参数
      this.queryParams = {}
      // 清空时间选择
      this.$refs.createTime.reset()
      this.fetch()
    },
    handleTableChange (pagination, filters, sorter) {
      this.sortedInfo = sorter
      this.fetch({
        sortField: sorter.field,
        sortOrder: sorter.order,
        ...this.queryParams,
        ...filters
      })
    },
    fetch (params = {}) {
      this.loading = true
      list({
        ...params
      }).then((resp) => {
        this.loading = false
        this.dataSource = resp.rows.children
      })
    }
  }
}
</script>
