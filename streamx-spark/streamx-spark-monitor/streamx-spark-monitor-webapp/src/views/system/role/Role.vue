<template>
  <a-card :bordered="false" class="card-area">
    <div :class="advanced ? 'search' : null">
      <!-- 搜索区域 -->
      <a-form layout="horizontal">
        <div :class="advanced ? null: 'fold'">
          <a-row >
            <a-col :md="12" :sm="24" >
              <a-form-item
                label="角色"
                :labelCol="{span: 5}"
                :wrapperCol="{span: 18, offset: 1}">
                <a-input v-model="queryParams.roleName"/>
              </a-form-item>
            </a-col>
            <a-col :md="12" :sm="24" >
              <a-form-item
                label="创建时间"
                :labelCol="{span: 5}"
                :wrapperCol="{span: 18, offset: 1}">
                <range-date @change="handleDateChange" ref="createTime"></range-date>
              </a-form-item>
            </a-col>
          </a-row>
        </div>
        <span style="float: right; margin-top: 3px;">
          <a-button type="primary" @click="search">查询</a-button>
          <a-button style="margin-left: 8px" @click="reset">重置</a-button>
        </span>
      </a-form>
    </div>
    <div>
      <div class="operator">
        <a-button v-hasPermission="'role:add'" ghost type="primary" @click="add">新增</a-button>
        <a-button v-hasPermission="'role:delete'" @click="batchDelete">删除</a-button>
        <a-dropdown v-hasPermission="'role:export'">
          <a-menu slot="overlay">
            <a-menu-item key="export-data" @click="exprotExccel">导出Excel</a-menu-item>
          </a-menu>
          <a-button>
            更多操作 <a-icon type="down" />
          </a-button>
        </a-dropdown>
      </div>
      <!-- 表格区域 -->
      <a-table ref="TableInfo"
               :columns="columns"
               :dataSource="dataSource"
               :pagination="pagination"
               :loading="loading"
               :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"
               :scroll="{ x: 900 }"
               @change="handleTableChange">
        <template slot="remark" slot-scope="text, record">
          <a-popover placement="topLeft">
            <template slot="content">
              <div style="max-width: 200px">{{text}}</div>
            </template>
            <p style="width: 200px;margin-bottom: 0">{{text}}</p>
          </a-popover>
        </template>
        <template slot="operation" slot-scope="text, record">
          <a-icon v-hasPermission="'role:update'" type="setting" theme="twoTone" twoToneColor="#4a9ff5" @click="edit(record)" title="修改角色"></a-icon>
          &nbsp;
          <a-icon type="eye" theme="twoTone" twoToneColor="#42b983" @click="view(record)" title="查看"></a-icon>
        </template>
      </a-table>
      <!-- 角色信息查看 -->
      <role-info
        @close="handleRoleInfoClose"
        :roleInfoVisiable="roleInfo.visiable"
        :roleInfoData="roleInfo.data">
      </role-info>
      <!-- 新增角色 -->
      <role-add
        @close="handleRoleAddClose"
        @success="handleRoleAddSuccess"
        :roleAddVisiable="roleAdd.visiable">
      </role-add>
      <!-- 修改角色 -->
      <role-edit
        ref="roleEdit"
        :roleInfoData="roleInfo.data"
        @close="handleRoleEditClose"
        @success="handleRoleEditSuccess"
        :roleEditVisiable="roleEdit.visiable">
      </role-edit>
    </div>
  </a-card>
</template>

<script>
import RangeDate from '@/components/datetime/RangeDate'
import RoleAdd from './RoleAdd'
import RoleInfo from './RoleInfo'
import RoleEdit from './RoleEdit'

export default {
  name: 'Role',
  components: {RangeDate, RoleInfo, RoleAdd, RoleEdit},
  data () {
    return {
      advanced: false,
      roleInfo: {
        visiable: false,
        data: {}
      },
      roleAdd: {
        visiable: false
      },
      roleEdit: {
        visiable: false
      },
      queryParams: {
        createTimeFrom: '',
        createTimeTo: ''
      },
      dataSource: [],
      sortedInfo: null,
      paginationInfo: null,
      selectedRowKeys: [],
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
      let { sortedInfo } = this
      sortedInfo = sortedInfo || {}
      return [{
        title: '角色',
        dataIndex: 'roleName'
      }, {
        title: '描述',
        dataIndex: 'remark',
        scopedSlots: { customRender: 'remark' },
        width: 350
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
        scopedSlots: { customRender: 'operation' }
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
    add () {
      this.roleAdd.visiable = true
    },
    handleRoleAddClose () {
      this.roleAdd.visiable = false
    },
    handleRoleAddSuccess () {
      this.roleAdd.visiable = false
      this.$message.success('新增角色成功')
      this.search()
    },
    view (record) {
      this.roleInfo.data = record
      this.roleInfo.visiable = true
    },
    handleRoleInfoClose () {
      this.roleInfo.visiable = false
    },
    edit (record) {
      this.$refs.roleEdit.setFormValues(record)
      this.roleInfo.data = record
      this.roleEdit.visiable = true
    },
    handleRoleEditSuccess () {
      this.roleEdit.visiable = false
      this.$message.success('修改角色成功')
      this.search()
    },
    handleRoleEditClose () {
      this.roleEdit.visiable = false
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
      let that = this
      this.$confirm({
        title: '确定删除所选中的记录?',
        content: '当您点击确定按钮后，这些记录将会被彻底删除',
        centered: true,
        onOk () {
          let roleIds = []
          for (let key of that.selectedRowKeys) {
            roleIds.push(that.dataSource[key].roleId)
          }
          that.$delete('role/' + roleIds.join(',')).then(() => {
            that.$message.success('删除成功')
            that.selectedRowKeys = []
            that.search()
          })
        },
        onCancel () {
          that.selectedRowKeys = []
        }
      })
    },
    exprotExccel () {
      let {sortedInfo} = this
      let sortField, sortOrder
      // 获取当前列的排序和列的过滤规则
      if (sortedInfo) {
        sortField = sortedInfo.field
        sortOrder = sortedInfo.order
      }
      this.$export('role/excel', {
        sortField: sortField,
        sortOrder: sortOrder,
        ...this.queryParams
      })
    },
    search () {
      let {sortedInfo} = this
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
      // 重置分页
      this.$refs.TableInfo.pagination.current = this.pagination.defaultCurrent
      if (this.paginationInfo) {
        this.paginationInfo.current = this.pagination.defaultCurrent
        this.paginationInfo.pageSize = this.pagination.defaultPageSize
      }
      // 重置列排序规则
      this.sortedInfo = null
      // 重置查询参数
      this.queryParams = {}
      // 清空时间选择
      this.$refs.createTime.reset()
      this.fetch()
    },
    handleTableChange (pagination, filters, sorter) {
      // 将这两个参数赋值给Vue data，用于后续使用
      this.paginationInfo = pagination
      this.sortedInfo = sorter
      this.fetch({
        sortField: sorter.field,
        sortOrder: sorter.order,
        ...this.queryParams
      })
    },
    fetch (params = {}) {
      this.loading = true
      if (this.paginationInfo) {
        // 如果分页信息不为空，则设置表格当前第几页，每页条数，并设置查询分页参数
        this.$refs.TableInfo.pagination.current = this.paginationInfo.current
        this.$refs.TableInfo.pagination.pageSize = this.paginationInfo.pageSize
        params.pageSize = this.paginationInfo.pageSize
        params.pageNum = this.paginationInfo.current
      } else {
        // 如果分页信息为空，则设置为默认值
        params.pageSize = this.pagination.defaultPageSize
        params.pageNum = this.pagination.defaultCurrent
      }
      this.$get('role', {
        ...params
      }).then((r) => {
        let data = r.data
        const pagination = { ...this.pagination }
        pagination.total = data.total
        this.dataSource = data.rows
        this.pagination = pagination
        this.loading = false
      })
    }
  }
}
</script>

<style lang="less" scoped>
@import "../../../../static/less/Common";
</style>
