<template>
    <a-drawer :title="title"
              :maskClosable="false"
              width="calc(100% - 35%)"
              placement="right"
              :closable="false"
              @close="onClose"
              :visible="visiable"
              style="height: calc(100% - 55px);overflow: auto;padding-bottom: 53px;">
        <a-textarea class="conf" ref="conf" v-model="detail.conf"></a-textarea>
        <div class="drawer-bootom-button" style="z-index: 999">
            <a-button style="margin-right: .8rem" @click="onClose">关闭</a-button>
        </div>
    </a-drawer>
</template>

<script>

    import {mapState} from 'vuex'
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
        data () {
            return {
                title:'配置详情',
                codeMirror:null,
                detail:{},
                loading:false
            }
        },
        computed: {
            ...mapState({
                user: state => state.account.user
            })
        },

        methods: {
            initCodeMirror () {
                this.codeMirror = CodeMirror.fromTextArea(document.querySelector(".conf"), {
                    tabSize: 4,
                    styleActiveLine: true,
                    lineNumbers: true,
                    line: true,
                    foldGutter: true,
                    styleSelectedText: true,
                    matchBrackets: true,
                    showCursorWhenSelecting: true,
                    extraKeys: { 'Ctrl': 'autocomplete' },
                    lint: true,
                    autoMatchParens: true,
                    mode: 'shell',
                    theme: 'darcula',	// 设置主题
                    lineWrapping: true, // 代码折叠
                    gutters: ['CodeMirror-linenumbers', 'CodeMirror-foldgutter', 'CodeMirror-lint-markers']
                })
            },
            setDetail (detail) {
                this.detail = detail
                this.$nextTick(()=>{
                    if (this.codeMirror == null) {
                        this.initCodeMirror()
                    }
                })
            },
            onClose () {
                this.loading = false
                this.$emit('close')
            },
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

