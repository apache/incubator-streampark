<template>
  <a-card :bordered="false">
    <div class="table-page-search-wrapper">
      <a-form layout="inline">
        <a-row :gutter="48">
          <div :class="advanced ? null: 'fold'">
            <a-col :md="8" :sm="24">
              <a-form-item
                label="操作人"
                :labelCol="{span: 4}"
                :wrapperCol="{span: 18, offset: 2}">
                <a-input v-model="queryParams.username"/>
              </a-form-item>
            </a-col>
            <a-col :md="8" :sm="24">
              <a-form-item
                label="操作描述"
                :labelCol="{span: 4}"
                :wrapperCol="{span: 18, offset: 2}">
                <a-input v-model="queryParams.operation"/>
              </a-form-item>
            </a-col>
            <template v-if="advanced">
              <a-col :md="8" :sm="24">
                <a-form-item
                  label="操作地点"
                  :labelCol="{span: 4}"
                  :wrapperCol="{span: 18, offset: 2}">
                  <a-input v-model="queryParams.location"/>
                </a-form-item>
              </a-col>

              <a-col :md="8" :sm="24">
                <a-form-item
                  label="操作时间"
                  :labelCol="{span: 4}"
                  :wrapperCol="{span: 18, offset: 2}">
                  <range-date @change="handleDateChange" ref="createTime" style="width: 100%"></range-date>
                </a-form-item>
              </a-col>
            </template>
          </div>

          <a-col :md="!advanced && 8 || 24" :sm="24">
            <span class="table-page-search-bar" :style="advanced && { float: 'right', overflow: 'hidden' } || {} ">
              <a-button type="primary"
                        shape="circle"
                        icon="search"
                        @click="search">
              </a-button>
              <a-button type="primary"
                        shape="circle"
                        icon="rest"
                        @click="reset">
              </a-button>

               <a-button type="primary"
                         shape="circle"
                         icon="export"
                         v-permit="'log:export'"
                         @click="exportExcel">
              </a-button>
               <a-button v-permit="'log:delete'"
                         type="primary"
                         shape="circle"
                         icon="minus"
                         @click="batchDelete">
                </a-button>

              <a @click="advanced = !advanced" style="margin-left: 4px">
                {{ advanced ? '收起' : '展开' }}
                <a-icon :type="advanced ? 'up' : 'down'"/>
               </a>
            </span>
          </a-col>
        </a-row>
      </a-form>
    </div>

    <!-- 表格区域 -->
    <a-table ref="TableInfo"
             :columns="columns"
             :dataSource="dataSource"
             :pagination="pagination"
             :loading="loading"
             :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"
             @change="handleTableChange" :scroll="{ x: 1500 }">
      <template slot="method" slot-scope="text, record">
        <a-popover placement="topLeft">
          <template slot="content">
            <div>{{text}}</div>
          </template>
          <p style="margin-bottom: 0" class="ellipsis">{{text}}</p>
        </a-popover>
      </template>
      <template slot="params" slot-scope="text, record">
        <a-popover placement="topLeft">
          <template slot="content">
            <div style="max-width: 300px;">{{text}}</div>
          </template>
          <p style="margin-bottom: 0" class="ellipsis">{{text}}</p>
        </a-popover>
      </template>
    </a-table>
  </a-card>
</template>

<script>
    import RangeDate from '@/components/DateTime/RangeDate'
    import {list,remove,$export} from '@/api/log'

    export default {
        name: 'SystemLog',
        components: {RangeDate},
        data() {
            return {
                advanced: false,
                dataSource: [],
                sortedInfo: null,
                paginationInfo: null,
                selectedRowKeys: [],
                queryParams: {},
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
            columns() {
                let {sortedInfo} = this
                sortedInfo = sortedInfo || {}
                return [{
                    title: '操作人',
                    dataIndex: 'username'
                }, {
                    title: '操作描述',
                    dataIndex: 'operation'
                }, {
                    title: '耗时',
                    dataIndex: 'time',
                    customRender: (text, row, index) => {
                        if (text < 500) {
                            return <a-tag color="green">{text} ms</a-tag>
                        } else if (text < 1000) {
                            return <a-tag color="cyan">{text} ms</a-tag>
                        } else if (text < 1500) {
                            return <a-tag color="orange">{text} ms</a-tag>
                        } else {
                            return <a-tag color="red">{text} ms</a-tag>
                        }
                    },
                    sorter: true,
                    sortOrder: sortedInfo.columnKey === 'time' && sortedInfo.order
                }, {
                    title: '执行方法',
                    dataIndex: 'method',
                    scopedSlots: {customRender: 'method'}
                }, {
                    title: '方法参数',
                    dataIndex: 'params',
                    scopedSlots: {customRender: 'params'},
                    width: 100
                }, {
                    title: 'IP地址',
                    dataIndex: 'ip'
                }, {
                    title: '操作地点',
                    dataIndex: 'location'
                }, {
                    title: '操作时间',
                    dataIndex: 'createTime',
                    sorter: true,
                    sortOrder: sortedInfo.columnKey === 'createTime' && sortedInfo.order
                }]
            }
        },
        mounted() {
            this.fetch()
        },
        methods: {
            onSelectChange(selectedRowKeys) {
                this.selectedRowKeys = selectedRowKeys
            },
            toggleAdvanced() {
                this.advanced = !this.advanced
                if (!this.advanced) {
                    this.queryParams.createTimeFrom = ''
                    this.queryParams.createTimeTo = ''
                    this.queryParams.location = ''
                }
            },
            handleDateChange(value) {
                if (value) {
                    this.queryParams.createTimeFrom = value[0]
                    this.queryParams.createTimeTo = value[1]
                }
            },
            batchDelete() {
                if (!this.selectedRowKeys.length) {
                    this.$message.warning('请选择需要删除的记录')
                    return
                }
                let that = this
                this.$confirm({
                    title: '确定删除所选中的记录?',
                    content: '当您点击确定按钮后，这些记录将会被彻底删除',
                    okText: '确定',
                    okType: 'danger',
                    cancelText: '取消',
                    centered: true,
                    onOk() {
                        let ids = []
                        for (let key of that.selectedRowKeys) {
                            ids.push(that.dataSource[key].id)
                        }
                        remove({
                            ids: ids.join(',')
                        }).then(() => {
                            that.$message.success('删除成功')
                            that.selectedRowKeys = []
                            that.search()
                        })
                    },
                    onCancel() {
                        that.selectedRowKeys = []
                        that.$message.info('已取消删除')
                    }
                })
            },
            exportExcel() {
                let {sortedInfo} = this
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
            search() {
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
            reset() {
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
                if (this.advanced) {
                    this.$refs.createTime.reset()
                }
                this.fetch()
            },
            handleTableChange(pagination, filters, sorter) {
                // 将这两个参数赋值给Vue data，用于后续使用
                this.paginationInfo = pagination
                this.sortedInfo = sorter
                this.fetch({
                    sortField: sorter.field,
                    sortOrder: sorter.order,
                    ...this.queryParams,
                    ...filters
                })
            },
            fetch(params = {}) {
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
                list({
                    ...params
                }).then((resp) => {
                    let data = resp
                    const pagination = {...this.pagination}
                    pagination.total = data.total
                    this.loading = false
                    this.dataSource = data.rows
                    this.pagination = pagination
                })
            }
        }
    }
</script>

<style>
  .ellipsis {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    display: inline-block;
    width: 200px;
  }
</style>
