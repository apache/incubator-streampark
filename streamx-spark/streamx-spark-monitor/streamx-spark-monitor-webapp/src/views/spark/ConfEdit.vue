<template>
    <div :class="[multiPage === true ? 'multi-page':'single-page', 'not-menu-page']">
        <div v-show="!diffVisible">
            <a-textarea class="conf" ref="conf" v-model="sparkConf.conf"></a-textarea>
            <div class="drawer-bootom-button" style="z-index: 999">
                <a-button style="margin-right: .8rem" @click="codeDiff(true)">提交</a-button>
            </div>
        </div>
        <div v-if="diffVisible" >
            <vue-code-diff :old-string="oldConf" :new-string="conf" :context="20" outputFormat="side-by-side"/>
            <div class="drawer-bootom-button" style="z-index: 999">
                <a-button style="margin-right: .8rem" @click="codeDiff(false)">上一部</a-button>
                <a-button style="margin-right: .8rem" @click="goBack">取消</a-button>
                <a-button style="margin-right: .8rem" @click="onSubmit">提交</a-button>
            </div>
        </div>
        <a-spin v-if="false" tip="生成配置对比中..." :spinning="true" style="width: 100%;height: 400px;z-index:1000;position: absolute"></a-spin>
    </div>
</template>

<script>
    import {mapState} from 'vuex'
    import vueCodeDiff from 'vue-code-diff'
    import CodeMirror from 'codemirror'
    import 'codemirror/theme/darcula.css'
    import 'codemirror/lib/codemirror.css'
    import 'codemirror/mode/shell/shell'

    export default {
        name: 'ConfEdit',
        components: {vueCodeDiff},
        data () {
            return {
                codeMirror:null,
                sparkConf:{},
                conf:'',
                oldConf:'',
                diffVisible:false,
                spinVisible:false,
            }
        },
        computed: {
            ...mapState({
                user: state => state.account.user,
                multiPage: state => state.setting.multipage,
                confType: state => state.spark.confType,
                myId: state => state.spark.myId,
                recordId: state => state.spark.recordId,
            }),
        },

        mounted() {
            this.getConf()
        },

        methods: {
            initCodeMirror () {
                this.codeMirror = CodeMirror.fromTextArea(document.querySelector(".conf"), {
                    tabSize: 2,
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
                }).on('change', cm => {
                   this.conf =  cm.getValue()
                })
            },

            getConf() {
                let prefix = this.confType === 1 ? "spark/conf/detail/":"spark/conf/record/"
                let id = this.confType === 1 ? this.myId : this.recordId
                this.$post(prefix + id, {}).then((r) => {
                    let data = r.data
                    this.sparkConf = data.data
                    this.oldConf = this.sparkConf.conf
                    this.conf = this.sparkConf.conf
                    if ( this.codeMirror==null ) {
                        this.$nextTick(()=>{
                            this.initCodeMirror()
                        })
                    }
                })
            },

            codeDiff(flag) {
                this.diffVisible = flag;
                if (flag) {
                    this.spinVisible = true
                }
                this.$nextTick(()=>{
                    this.spinVisible = false
                })
            },
            goBack() {
                this.$router.push("/spark/conf")
            },
            onSubmit() {
                this.$post("/spark/conf/update", {
                    myId:this.myId,
                    conf:this.conf,
                    userId:this.user.userId
                }).then((r) => {
                    this.$message.success('修改成功')
                    this.goBack()
                })
            }
        }
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
</style>

