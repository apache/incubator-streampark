<template>
    <a-card :bordered="false" class="card-area">
        <div :class="advanced ? 'search' : null">
            <!-- 搜索区域 -->
            <a-form layout="horizontal">
                <div class="fold">
                    <a-row>
                        <a-col :md="8" :sm="24">
                            <a-form-item
                                    label="appId"
                                    :labelCol="{span: 5}"
                                    :wrapperCol="{span: 15, offset: 1}">
                                <a-input v-model="queryParams.appId"/>
                            </a-form-item>
                        </a-col>
                        <a-col :md="8" :sm="24">
                            <a-form-item
                                    label="appName"
                                    :labelCol="{span: 5}"
                                    :wrapperCol="{span: 15, offset: 1}">
                                <a-input v-model="queryParams.appName"/>
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
            <!-- 表格区域 -->
            <a-table :columns="columns"
                     :dataSource="dataSource"
                     :pagination="pagination"
                     :loading="loading"
                     :scroll="{ x: 900 }"
                     @change="handleTableChange">
                <template slot="status" slot-scope="text,record">
                    <a-tag v-if="record.status === -1" color="#F56C6C">失&nbsp;联</a-tag>
                    <a-tag v-if="record.status === 0" color="#67C23A">运行中</a-tag>
                    <a-tag v-if="record.status === 1" color="#303133">停&nbsp;止</a-tag>
                    <a-tag v-if="record.status === 2" color="#409EFF">启动中</a-tag>
                    <a-tag v-if="record.status === 3" color="#E6A23C">启动失败</a-tag>
                    <a-tag v-if="record.status === 4" color="#409EFF">停止中</a-tag>
                    <a-tag v-if="record.status === 5" color="#E6A23C">停止失败</a-tag>
                </template>
                <template slot="operation" slot-scope="text,record">

                    <a-popconfirm v-if="record.status === -1||record.status === 1||record.status === 3" v-hasPermission="'spark:start'" title="要启动该任务吗？" okText="启动" cancelText="取消" @confirm="start(record)">
                        <a-icon slot="icon" type="question-circle-o" style="color: green" />
                        <!--停止,启动失败-->
                        <a>
                            <a-icon type="play-circle" theme="twoTone" twoToneColor="#52c41a" title="启动"></a-icon>
                        </a>
                    </a-popconfirm>

                    <a-popconfirm v-if="record.status === 0||record.status === 5" v-hasPermission="'spark:stop'" title="要停止该任务吗？" okText="停止" cancelText="取消" @confirm="stop(record)">
                        <a-icon slot="icon" type="question-circle-o" style="color: red" />
                        <!--正常运行-->
                        <a>
                            <a-icon type="pause-circle" theme="outlined" title="停止"></a-icon>
                        </a>
                    </a-popconfirm>

                    <a-popconfirm v-if="record.status === 1" v-hasPermission="'spark:delete'" title="确定要删除该任务吗？" okText="删除" cancelText="取消" @confirm="remove(record)">
                        <a-icon slot="icon" type="question-circle-o" style="color: red" />
                        <!--停止-->
                        <a>
                            <a-icon type="delete" theme="twoTone" twoToneColor="#eb2f96" title="删除"></a-icon>
                        </a>
                    </a-popconfirm>
                    <a v-if="record.status == 0" v-hasPermission="'spark:track'" :href="record.trackUrl" target="_blank">
                        <a-icon type="fire" theme="twoTone"></a-icon>
                    </a>
                    <a-icon v-hasPermission="'spark:setting'" type="setting" @click="setting(record)" title="配置文件"></a-icon>
                </template>
            </a-table>
        </div>
    </a-card>

</template>

<script>
    import {mapState} from 'vuex'

    export default {
        name: 'Online',
        data() {
            return {
                advanced: false,
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
                queryParams: {},
                loading: false
            }
        },
        computed: {
            columns() {
                return [
                    {
                        title: 'appId',
                        dataIndex: 'appId',
                        scopedSlots: {customRender: 'appId'}
                    }, {
                        title: 'appName',
                        dataIndex: 'appName'
                    }, {
                        title: '配置版本',
                        dataIndex: 'confVersion'
                    }, {
                        title: '运行状态',
                        dataIndex: 'status',
                        scopedSlots: {customRender: 'status'},
                    },
                    {
                        title: '启动时间',
                        dataIndex: 'modifyTime'
                    },
                    {
                        title: '操作',
                        dataIndex: 'operation',
                        width: 180,
                        scopedSlots: {customRender: 'operation'},
                        fixed: 'right'
                    }]
            },
            ...mapState({
                user: state => state.account.user
            })
        },
        mounted() {
            this.fetch()
        },
        methods: {
            search() {
                this.fetch({
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
                this.paginationInfo = null
                // 重置查询参数
                this.queryParams = {}
                this.fetch()
            },
            handleTableChange(pagination, filters, sorter) {
                this.paginationInfo = pagination
                this.fetch({
                    ...this.queryParams
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
                this.$post('spark/monitor', {
                    ...params
                }).then((r) => {
                    let data = r.data
                    const pagination = {...this.pagination}
                    pagination.total = data.total
                    this.loading = false
                    this.dataSource = data.rows
                    this.pagination = pagination
                })
            },
            remove(params = {}) {
                this.$delete('spark/delete/' + params.myId, {
                }).then((r) => {
                    this.fetch({
                        ...this.queryParams
                    })
                })
            },
            stop(params = {}) {
                this.$post('spark/stop/' + params.myId, {
                }).then((r) => {
                    if(r.data.code === 0) {
                        this.$message.success('该任务正在停止中');
                    }else {
                        this.$message.error('该任务停止失败');
                    }
                    this.fetch({
                        ...this.queryParams
                    })
                })
            },
            start(params = {}) {
                this.$post('spark/start/' + params.myId, {
                }).then((r) => {
                    if(r.data.code === 0) {
                        this.$message.success('该任务正在启动中');
                    }else {
                        this.$message.error('该任务启动失败');
                    }
                    this.fetch({
                        ...this.queryParams
                    })
                })
            },
            setting(params = {}) {

            },
        }
    }
</script>

<style lang="less" scoped>
    @import "../../../static/less/Common";
</style>
