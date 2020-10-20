<template>
  <a-card :bordered="false">
    <div class="table-page-search-wrapper">
      <a-form layout="inline">
        <a-row :gutter="48">
          <div :class="advanced ? null: 'fold'">
            <a-col :md="8" :sm="24">
              <a-form-item
                label="Name"
                :labelCol="{span: 4}"
                :wrapperCol="{span: 18, offset: 2}">
                <a-input v-model="queryParams.menuName"/>
              </a-form-item>
            </a-col>
            <a-col :md="8" :sm="24">
              <a-form-item
                label="Create Time"
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

                <a-popconfirm
                  title="请选择创建类型"
                  okText="按钮"
                  cancelText="菜单"
                  @cancel="() => createMenu()"
                  @confirm="() => createButton()">
                  <a-icon slot="icon" type="question-circle-o" style="color: orangered"/>
                  <a-button
                    v-permit="'menu:add'"
                    type="primary"
                    shape="circle"
                    icon="plus">
                  </a-button>
                </a-popconfirm>

                <a-button
                  v-permit="'menu:delete'"
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
        :key="key"
        :dataSource="dataSource"
        :expandIcon="expandIcon"
        :pagination="pagination"
        :loading="loading"
        :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"
        @change="handleTableChange"
        :scroll="{ x: 1650 }" >
        <template slot="icon" slot-scope="text, record">
          <a-icon :type="text"/>
        </template>
        <template slot="operation" slot-scope="text, record">
          <a-icon
            v-permit="'menu:update'"
            type="setting"
            theme="twoTone"
            twoToneColor="#4a9ff5"
            @click="edit(record)"
            title="修改"></a-icon>
          <a-badge v-noPermit="'menu:update'" status="warning" text="无权限"></a-badge>
        </template>
      </a-table>
    </div>

    <!-- 新增菜单 -->
    <menu-add
      @close="handleMenuAddClose"
      @success="handleMenuAddSuccess"
      :menuAddVisiable="menuAddVisiable">
    </menu-add>
    <!-- 修改菜单 -->
    <menu-edit
      ref="menuEdit"
      @close="handleMenuEditClose"
      @success="handleMenuEditSuccess"
      :menuEditVisiable="menuEditVisiable">
    </menu-edit>
    <!-- 新增按钮 -->
    <button-add
      @close="handleButtonAddClose"
      @success="handleButtonAddSuccess"
      :buttonAddVisiable="buttonAddVisiable">
    </button-add>
    <!-- 修改按钮 -->
    <button-edit
      ref="buttonEdit"
      @close="handleButtonEditClose"
      @success="handleButtonEditSuccess"
      :buttonEditVisiable="buttonEditVisiable">
    </button-edit>
  </a-card>
</template>

<script>
import RangeDate from '@/components/DateTime/RangeDate'
import MenuAdd from './MenuAdd'
import MenuEdit from './MenuEdit'
import ButtonAdd from './ButtonAdd'
import ButtonEdit from './ButtonEdit'

import { list, remove, $export } from '@/api/menu'

export default {
  name: 'Menu',
  components: { ButtonAdd, ButtonEdit, RangeDate, MenuAdd, MenuEdit },
  data () {
    return {
      advanced: false,
      key: +new Date(),
      queryParams: {},
      filteredInfo: null,
      dataSource: [],
      selectedRowKeys: [],
      pagination: {
        defaultPageSize: 10000000,
        hideOnSinglePage: true,
        indentSize: 100
      },
      loading: false,
      menuAddVisiable: false,
      menuEditVisiable: false,
      buttonAddVisiable: false,
      buttonEditVisiable: false
    }
  },
  computed: {
    columns () {
      let { filteredInfo } = this
      filteredInfo = filteredInfo || {}
      return [{
        title: 'Name',
        dataIndex: 'text',
        width: 200,
        fixed: 'left'
      }, {
        title: 'Icon',
        dataIndex: 'icon',
        scopedSlots: { customRender: 'icon' }
      }, {
        title: 'Type',
        dataIndex: 'type',
        customRender: (text, row, index) => {
          switch (text) {
            case '0': return <a-tag color = "cyan" >菜单</a-tag>
            case '1': return <a-tag color = "pink"> 按钮 </a-tag>
            default: return text
          }
        },
        filters: [
          { text: 'Button', value: '1' },
          { text: 'Router', value: '0' }
        ],
        filterMultiple: false,
        filteredValue: filteredInfo.type || null,
        onFilter: (value, record) => record.type.includes(value)
      }, {
        title: 'Path',
        dataIndex: 'path'
      }, {
        title: 'Vue Component',
        dataIndex: 'component'
      }, {
        title: 'Permission',
        dataIndex: 'permission'
      }, {
        title: 'Order By',
        dataIndex: 'order'
      }, {
        title: 'Create Time',
        dataIndex: 'createTime'
      }, {
        title: 'Modify Time',
        dataIndex: 'modifyTime'
      }, {
        title: 'Operation',
        dataIndex: 'operation',
        width: 120,
        scopedSlots: { customRender: 'operation' },
        fixed: 'right'
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
    handleMenuEditClose () {
      this.menuEditVisiable = false
    },
    handleMenuEditSuccess () {
      this.menuEditVisiable = false
      this.$message.success('修改菜单成功')
      this.fetch()
    },
    handleButtonEditClose () {
      this.buttonEditVisiable = false
    },
    handleButtonEditSuccess () {
      this.buttonEditVisiable = false
      this.$message.success('修改按钮成功')
      this.fetch()
    },
    edit (record) {
      if (record.type === '0') {
        this.$refs.menuEdit.setFormValues(record)
        this.menuEditVisiable = true
      } else {
        this.$refs.buttonEdit.setFormValues(record)
        this.buttonEditVisiable = true
      }
    },
    handleButtonAddClose () {
      this.buttonAddVisiable = false
    },
    handleButtonAddSuccess () {
      this.buttonAddVisiable = false
      this.$message.success('新增按钮成功')
      this.fetch()
    },
    createButton () {
      this.buttonAddVisiable = true
    },
    handleMenuAddClose () {
      this.menuAddVisiable = false
    },
    handleMenuAddSuccess () {
      this.menuAddVisiable = false
      this.$message.success('新增菜单成功')
      this.fetch()
    },
    createMenu () {
      this.menuAddVisiable = true
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
      this.$confirm({
        title: '确定删除所选中的记录?',
        content: '当您点击确定按钮后，这些记录将会被彻底删除',
        okText: '确定',
        okType: 'danger',
        cancelText: '取消',
        centered: true,
        onOk () {
          remove({
            menuIds: that.selectedRowKeys.join(',')
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
      const { filteredInfo } = this
      $export({
        ...this.queryParams,
        ...filteredInfo
      })
    },
    search () {
      const { filteredInfo } = this
      this.fetch({
        ...this.queryParams,
        ...filteredInfo
      })
    },
    reset () {
      // 取消选中
      this.selectedRowKeys = []
      // 重置列过滤器规则
      this.filteredInfo = null
      // 重置查询参数
      this.queryParams = {}
      // 清空时间选择
      this.$refs.createTime.reset()
      this.fetch()
    },
    handleTableChange (pagination, filters, sorter) {
      // 将这两个个参数赋值给Vue data，用于后续使用
      this.filteredInfo = filters
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
        if (Object.is(resp.rows.children, undefined)) {
          this.dataSource = resp.rows
        } else {
          this.dataSource = resp.rows.children
        }
      })
    },

    expandIcon (props) {
      if (props.record.children && props.record.children.length > 0) {
        if (props.expanded) {
          return <a class="table-expanded-icon" onClick={e => {
            props.onExpand(props.record, e)
          }}><a-icon type="caret-down"/></a>
        } else {
          return <a class="table-expanded-icon" onClick={e => {
            props.onExpand(props.record, e)
          }}><a-icon type="caret-right"/></a>
        }
      } else {
        return <span></span>
      }
    }
  }
}
</script>

<style>

.table-expanded-icon {
  margin-right: 8px;
}

.table-expanded-icon .anticon {
  font-size: 10px;
  color: #666;
}
</style>
