<template>
  <a-popover
    v-model="visible"
    trigger="click"
    placement="bottomRight"
    overlay-class-name="header-notice-wrapper"
    :auto-adjust-overflow="true"
    :arrow-point-at-center="true"
    :overlay-style="{ width: '380px', top: '65px' }">
    <template slot="content">
      <a-spin :spinning="loadding">
        <a-tabs default-active-key="1" @change="handleChange">
          <a-tab-pane
            tab="异常告警"
            key="1">
            <a-list>
              <a-list-item
                v-for="(e,index) in exceptions"
                :key="'message-'.concat(index)">
                <a-list-item-meta
                  :title="e.title"
                  :description="e.createTime"
                  @click="handleAlert(e)">
                  <a-avatar
                    style="background-color: white"
                    slot="avatar"
                    src="https://gw.alipayobjects.com/zos/rmsportal/ThXAXghbEsBCCSDihZxY.png"/>
                </a-list-item-meta>
                <span class="mark-read" @click="handleMarkRead(e.id)">
                  <a-icon type="check" style="color: #d9d9d9;font-size: 20px"/>
                </span>
              </a-list-item>
              <template v-if="total1 > 0">
                <a-pagination
                  size="small"
                  :total="total1"
                  :page-size="pageSize"
                  :default-current="pageNo1"
                  class="pagination"
                  @change="handleNoticePage" />
              </template>
            </a-list>
          </a-tab-pane>
          <a-tab-pane
            tab="通知消息"
            key="2">
            <a-list>
              <a-list-item
                v-for="(msg,index) in message"
                :key="'message-'.concat(index)">
                <a-list-item-meta
                  :title="msg.title"
                  :description="msg.createTime"
                  @click="handleAlert(msg)">
                </a-list-item-meta>
                <span class="mark-read" @click="handleMarkRead(e.id)">
                  <a-icon type="check" style="color: #d9d9d9;font-size: 20px"/>
                </span>
              </a-list-item>
            </a-list>
            <a-pagination
              v-if="total2 > 0"
              size="small"
              :total="total2"
              :page-size="pageSize"
              :default-current="pageNo2"
              class="pagination"
              @change="handleNoticePage" />
          </a-tab-pane>
        </a-tabs>
      </a-spin>
    </template>
    <span
      @click="fetchNotice"
      class="header-notice">
      <a-badge
        :count="noticeType === 1 ? (total1 > 0 ? total1 : total2) : (total2 > 0 ? total2 : total1)">
        <a-icon
          style="font-size: 16px; padding: 4px"
          type="bell"/>
      </a-badge>
    </span>
  </a-popover>
</template>

<script>
import {baseUrl} from '@/api/baseUrl'
import {notice,delnotice} from '@api/metrics'
import store from '@/store'

export default {
  name: 'Notice',
  data() {
    return {
      loadding: false,
      visible: false,
      pageSize: 8,
      pageNo1: 1,
      pageNo2: 1,
      total1: 0,
      total2: 0,
      noticeType: 1,
      message: [],
      exceptions: []
    }
  },
  methods: {
    fetchNotice() {
      if (!this.visible) {
        this.loadding = true
        setTimeout(() => {
          this.loadding = false
        }, 500)
      } else {
        this.loadding = false
      }
      this.visible = !this.visible
    },

    handleNoticePage(pageNo) {
      if (this.noticeType === 1) {
        this.pageNo1 = pageNo
      } else {
        this.pageNo2 = pageNo
      }
      notice({
        type: this.noticeType,
        pageNum: pageNo,
        pageSize: this.pageSize
      }).then((resp)=> {
        if (this.noticeType === 1) {
          this.exceptions = resp.data.records || []
          this.total1 = parseInt(resp.data.total)
        } else {
          this.message = resp.data.records || []
          this.total2 = parseInt(resp.data.total)
        }
      }).then((err)=>{

      })
    },

    handleChange(key) {
      this.noticeType = parseInt(key)
      if (this.noticeType === 1) {
         this.handleNoticePage(this.pageNo1)
      } else {
          this.handleNoticePage(this.pageNo2)
      }
    },

    handleWebSocket() {
      const url = baseUrl().concat('/websocket/' + store.getters.userInfo.userId)

      const socket = this.getSocket(url)

      socket.onmessage = (event) => {
        const message = JSON.parse(event.data)
        if (this.noticeType === 1) {
          this.exceptions.push(message)
          this.total1 += 1
        } else {
          this.message.push(message)
          this.total2 += 1
        }
        this.handleAlert(message)
      }
    },

    handleAlert(message) {
      this.$swal.fire({
        title: message.title,
        icon: 'error',
        width: this.exceptionPropWidth(),
        html: '<pre class="propException">' + message.context + '</pre>',
        focusConfirm: false,
      })
    },

    handleMarkRead(id) {
      delnotice({
        id: id
      }).then((resp)=> {
        if (this.noticeType === 1) {
          this.handleNoticePage(this.pageNo1)
        } else {
          this.handleNoticePage(this.pageNo2)
        }
      }).then((err)=>{

      })
    },

  },

  mounted() {
    this.handleNoticePage(this.pageNo1)
    this.handleWebSocket()
  }

}
</script>

<style lang="css">
.header-notice-wrapper {
  top: 50px !important;
}

</style>
<style lang="less" scoped>
.header-notice {
  display: inline-block;
  transition: all 0.3s;
  span {
    vertical-align: initial;
  }
}
.pagination {
  margin-top: 10px;
  float: right;
}

.mark-read {
  width: 30px;
  height: 30px;
  border: 1px solid #d9d9d9;
  border-radius: 50%;
  padding: 5px
}

</style>
