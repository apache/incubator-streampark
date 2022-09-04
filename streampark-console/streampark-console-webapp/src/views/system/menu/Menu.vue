<template>
  <a-card
    :bordered="false">
    <div
      class="table-page-search-wrapper">
      <a-form
        layout="inline">
        <a-row
          :gutter="48">
          <div
            :class="advanced ? null: 'fold'">
            <a-col
              :md="8"
              :sm="24">
              <a-form-item
                label="Name"
                :label-col="{span: 4}"
                :wrapper-col="{span: 18, offset: 2}">
                <a-input
                  v-model="queryParams.menuName" />
              </a-form-item>
            </a-col>
            <a-col
              :md="8"
              :sm="24">
              <a-form-item
                label="Create Time"
                :label-col="{span: 4}"
                :wrapper-col="{span: 18, offset: 2}">
                <range-date
                  @change="handleDateChange"
                  ref="createTime" />
              </a-form-item>
            </a-col>
            <a-col
              :md="8"
              :sm="24">
              <span
                class="table-page-search-bar">
                <a-button
                  type="primary"
                  shape="circle"
                  icon="search"
                  @click="search" />
                <a-button
                  type="primary"
                  shape="circle"
                  icon="rest"
                  @click="reset" />
                <a-popconfirm
                  title="请选择创建类型"
                  ok-text="按钮"
                  cancel-text="菜单"
                  @cancel="() => createMenu()"
                  @confirm="() => createButton()">
                  <a-icon
                    slot="icon"
                    type="question-circle-o"
                    style="color: orangered" />
                  <a-button
                    v-permit="'menu:add'"
                    type="primary"
                    shape="circle"
                    icon="plus" />
                </a-popconfirm>
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
        :data-source="dataSource"
        :pagination="pagination"
        :loading="loading"
        @change="handleTableChange"
        :scroll="{ x: 1650 }">
        <template
          slot="icon"
          slot-scope="text">
          <a-icon
            :type="text" />
        </template>
        <template
          slot="operation"
          slot-scope="text, record">
          <svg-icon
            v-permit="'menu:update'"
            name="edit"
            border
            @click.native="edit(record)"
            title="修改" />
          <a-badge
            v-noPermit="'menu:update'"
            status="warning"
            text="无权限" />
        </template>
      </a-table>
    </div>

    <!-- 新增菜单 -->
    <menu-add
      @close="handleMenuAddClose"
      @success="handleMenuAddSuccess"
      :menu-add-visiable="menuAddVisiable" />
    <!-- 修改菜单 -->
    <menu-edit
      ref="menuEdit"
      @close="handleMenuEditClose"
      @success="handleMenuEditSuccess"
      :menu-edit-visiable="menuEditVisiable" />
    <!-- 新增按钮 -->
    <button-add
      @close="handleButtonAddClose"
      @success="handleButtonAddSuccess"
      :button-add-visiable="buttonAddVisiable" />
    <!-- 修改按钮 -->
    <button-edit
      ref="buttonEdit"
      @close="handleButtonEditClose"
      @success="handleButtonEditSuccess"
      :button-edit-visiable="buttonEditVisiable" />

  </a-card>
</template>

<script>
import RangeDate from '@/components/DateTime/RangeDate'
import MenuAdd from './MenuAdd'
import MenuEdit from './MenuEdit'
import ButtonAdd from './ButtonAdd'
import ButtonEdit from './ButtonEdit'
import SvgIcon from '@/components/SvgIcon'
import { list } from '@/api/menu'
import storage from '@/utils/storage'
import {USER_NAME} from '@/store/mutation-types'

export default {
  name: 'Menu',
  components: { ButtonAdd, ButtonEdit, RangeDate, MenuAdd, MenuEdit, SvgIcon },
  data () {
    return {
      advanced: false,
      key: +new Date(),
      queryParams: {},
      filteredInfo: null,
      dataSource: [],
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
        }
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
    },
    userName() {
      return storage.get(USER_NAME)
    }
  },
  mounted () {
    this.fetch()
  },
  methods: {
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
    search () {
      const { filteredInfo } = this
      this.fetch({
        ...this.queryParams,
        ...filteredInfo
      })
    },
    reset () {
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
        const data = resp.data
        if (Object.is(data.rows.children, undefined)) {
          this.dataSource = data.rows
        } else {
          this.dataSource = data.rows.children
        }
      })
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
