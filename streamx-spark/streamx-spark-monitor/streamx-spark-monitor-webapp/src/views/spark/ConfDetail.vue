<template>
    <a-drawer title="配置详细"
              :maskClosable="false"
              width="calc(100% - 20%)"
              placement="right"
              :closable="false"
              @close="onClose"
              :visible="visiable"
              style="height: calc(100% - 55px);overflow: auto;padding-bottom: 53px;">

        <a-col style="font-size: 1rem">
            <p>
                <a-tag color="blue">
                    <a-icon type="fire"></a-icon>
                    appName
                </a-tag>
                &nbsp;&nbsp;{{detail.appName}}
            </p>
            <p>
                <a-tag color="blue">
                    <a-icon type="setting"></a-icon>
                    配置版本
                </a-tag>
                &nbsp;&nbsp;{{detail.confVersion}}
            </p>
            <p>
                <a-tag color="blue">
                    <a-icon type="schedule"></a-icon>
                    记录时间
                </a-tag>
                &nbsp;&nbsp;{{detail.createTime}}
            </p>
            <p>
                <a-tag color="blue">
                    <a-icon type="question-circle"></a-icon>
                    版本状态
                </a-tag>
                &nbsp;&nbsp;
                <a-tag v-if="detail.status === 1" color="#87d068">线上版本</a-tag>
                <a-tag v-else color="#666">历史版本</a-tag>
                <a-button type="primary" shape="circle" icon="edit" size="small" @click="edit()"></a-button>
                <a-button type="primary" shape="circle" icon="download" size="small" @click="download()"></a-button>
            </p>
        </a-col>

        <a-textarea class="conf" ref="conf" v-model="detail.conf"></a-textarea>

        <div class="drawer-bootom-button" style="z-index: 999">
            <a-button style="margin-right: .8rem" @click="onClose">关闭</a-button>
        </div>
    </a-drawer>
</template>

<script>
    import {mapState, mapMutations} from 'vuex'
    import CodeMirror from 'codemirror'
    import 'codemirror/theme/darcula.css'
    import 'codemirror/lib/codemirror.css'
    import 'codemirror/mode/shell/shell'

    export default {
        name: 'Detail',
        props: {
            visiable: {
                default: false
            }
        },
        data() {
            return {
                codeMirror: null,
                detail: {},
                loading: false
            }
        },
        computed: {
            ...mapState({
                user: state => state.account.user
            })
        },

        methods: {
            ...mapMutations({setConfType: 'spark/setConfType'}),
            ...mapMutations({setRecordId: 'spark/setRecordId'}),
            ...mapMutations({setMyId: 'spark/setMyId'}),
            initCodeMirror() {
                this.codeMirror = CodeMirror.fromTextArea(document.querySelector(".conf"), {
                    tabSize: 2,
                    styleActiveLine: true,
                    lineNumbers: true,
                    line: true,
                    foldGutter: true,
                    styleSelectedText: true,
                    matchBrackets: true,
                    showCursorWhenSelecting: true,
                    extraKeys: {'Ctrl': 'autocomplete'},
                    lint: true,
                    autoMatchParens: true,
                    mode: 'shell',
                    readOnly: true,
                    theme: 'default',	// 设置主题
                    lineWrapping: true, // 代码折叠
                    gutters: ['CodeMirror-linenumbers', 'CodeMirror-foldgutter', 'CodeMirror-lint-markers']
                })
            },
            setDetail(detail) {
                this.detail = detail
                this.$nextTick(() => {
                    if (this.codeMirror == null) {
                        this.initCodeMirror()
                    } else {
                        this.codeMirror.setValue(this.detail.conf)
                        setTimeout(() => {
                            this.codeMirror.refresh()
                        }, 1)
                    }
                })
            },
            onClose() {
                this.loading = false
                this.$emit('close')
            },
            edit() {
                this.setConfType(this.detail.status)
                this.setMyId(this.detail.myId)
                this.setRecordId(null)
                if (this.detail.status === 0) {
                    this.setRecordId(this.detail.recordId)
                }
                this.$router.push({path: '/spark/confEdit'})
            }
        },
    }
</script>

<style lang="less" scoped>
    @import "../../../static/less/Common";
</style>

<style lang="less">
    .CodeMirror {
        border: 1px solid #eee;
        height: auto;
    }

    .CodeMirror-scroll {
        height: auto;
        overflow-y: hidden;
        overflow-x: auto;
    }
</style>

