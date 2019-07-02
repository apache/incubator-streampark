<template>
    <a-card :bordered="false" class="card-area">
        <div :class="advanced ? 'search' : null">
            <!-- 搜索区域 -->
            <a-form layout="horizontal">
                <div class="fold">
                    <a-row >
                        <a-col :md="8" :sm="24" >
                            <a-form-item
                                    label="myId"
                                    :labelCol="{span: 5}"
                                    :wrapperCol="{span: 15, offset: 1}">
                                <a-input v-model="queryParams.myId"/>
                            </a-form-item>
                        </a-col>
                        <a-col :md="8" :sm="24" >
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

                <a-tag color="cyan" slot="confVersion" slot-scope="text,sparkConf" >{{sparkConf.confVersion}}</a-tag>

                <template slot="history" slot-scope="text,sparkConf">
                    <a-tag color="#87d068" style="border-radius:50%!important;margin:2px;"  v-for="(item,index) in sparkConf.history" :key="index" type="primary" size="small" shape="circle" @click="detailHistory(item.recordId)">{{item.confVersion}}</a-tag>
                </template>

                <template slot="operation" slot-scope="text,sparkConf">
                    <a-icon type="eye" @click="detail(sparkConf.myId)" theme="twoTone" twoToneColor="#42b983" title="配置详情"></a-icon>
                    <a-icon  v-hasPermission="'spark:setting'" theme="twoTone" twoToneColor="#4a9ff5" type="setting" @click="setting(sparkConf)" title="配置文件"></a-icon>
                </template >

            </a-table>
        </div>

        <!-- detail -->
        <conf-detail ref="confDetail" @close="handleConfDetailClose" :visiable="confDetail.visiable"></conf-detail>

    </a-card>
</template>

<script>
    import {mapState} from 'vuex'
    import ConfDetail from './ConfDetail'
    export default {
        name: 'Conf',
        components: { ConfDetail },
        data () {
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
                loading: false,
                confDetail: {
                    visiable: false
                },
            }
        },
        computed: {
            columns () {
                return [{
                    title: 'appName',
                    dataIndex: 'appName'
                }, {
                    title: '配置版本',
                    dataIndex: 'confVersion',
                    scopedSlots: {customRender: 'confVersion'},
                },
                {
                    title: '创建时间',
                    dataIndex: 'createTime'
                },
                {
                    title: '历史版本',
                    dataIndex: 'history',
                    scopedSlots: {customRender: 'history'},
                },
                {
                    title: '操作',
                    dataIndex: 'operation',
                    width: 120,
                    scopedSlots: {customRender: 'operation'},
                    fixed: 'right'
                }]
            },
            ...mapState({
                user: state => state.account.user
            })
        },
        mounted () {
            this.fetch()
        },
        methods: {
            search () {
                this.fetch({
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
                this.paginationInfo = null
                // 重置查询参数
                this.queryParams = {}
                this.fetch()
            },
            handleTableChange (pagination, filters, sorter) {
                this.paginationInfo = pagination
                this.fetch({
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
                this.$post('spark/conf/view', {
                    ...params
                }).then((r) => {
                    let data = r.data
                    const pagination = { ...this.pagination }
                    pagination.total = data.total
                    this.loading = false
                    this.dataSource = data.rows
                    this.pagination = pagination
                })
            },

            detail(myId) {
                this.$post('spark/conf/detail/' + myId, {}).then((r) => {
                    let data = r.data
                    this.confDetail.visiable = true
                    data.data.status = 1
                    this.$refs.confDetail.setDetail(data.data)
                })
            },

            detailHistory(recordId) {
                this.$post('spark/conf/record/' + recordId, {}).then((r) => {
                    let data = r.data
                    this.confDetail.visiable = true
                    data.data.status = 0
                    this.$refs.confDetail.setDetail(data.data)
                })
            },

            handleConfDetailClose() {
                this.confDetail.visiable = false
            },

        }
    }
</script>

<style lang="less" scoped>
    @import "../../../static/less/Common";
</style>
