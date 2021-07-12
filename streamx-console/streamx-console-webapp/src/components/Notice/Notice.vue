<template>
  <a-popover
    v-model="visible"
    trigger="click"
    placement="bottomRight"
    overlay-class-name="header-notice-wrapper"
    :auto-adjust-overflow="true"
    :arrow-point-at-center="true"
    :overlay-style="{ width: '300px', top: '50px' }">
    <template slot="content">
      <a-spin :spinning="loadding">
        <a-tabs default-active-key="1" @change="handleChange">
          <a-tab-pane
            tab="异常告警"
            key="1">
            <a-list>
              <a-list-item
                v-for="(msg,index) in exceptions"
                :key="'message-'.concat(index)">
                <a-list-item-meta
                  :title="msg.title"
                  :description="msg.createTime">
                  <a-avatar
                    style="background-color: white"
                    slot="avatar"
                    src="https://gw.alipayobjects.com/zos/rmsportal/ThXAXghbEsBCCSDihZxY.png"/>
                </a-list-item-meta>
              </a-list-item>
              <a-pagination
                size="small"
                :total="total"
                :page-size="pageSize"
                :default-current="pageNo1"
                class="pagination"
                @change="handleNoticePage" />
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
                  :description="msg.createTime">
                </a-list-item-meta>
              </a-list-item>
            </a-list>
            <a-pagination
              size="small"
              :total="total"
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
        :count="total">
        <a-icon
          style="font-size: 16px; padding: 4px"
          type="bell"/>
      </a-badge>
    </span>
  </a-popover>
</template>

<script>
import SockJS from 'sockjs-client'
import Stomp from 'webstomp-client'
import {baseUrl} from '@/api/baseUrl'
import {notice} from '@api/metrics'

export default {
  name: 'Notice',
  data() {
    return {
      loadding: false,
      visible: false,
      pageSize: 8,
      pageNo1: 1,
      pageNo2: 1,
      total: 0,
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
        this.total = parseInt(resp.data.total)
        if (this.noticeType === 1) {
          this.exceptions = resp.data.records || []
        } else {
          this.message = resp.data.records || []
        }
      }).then((err)=>{

      })
    },

    handleChange(key) {
      this.noticeType = key
      if (this.noticeType === 1) {
         this.handleNoticePage(this.pageNo1)
      } else {
          this.handleNoticePage(this.pageNo2)
      }
    },

    handleWebSocket() {
      const socket = new SockJS(baseUrl(true).concat('/websocket'))
      this.stompClient = Stomp.over(socket)
      this.stompClient.connect({}, (success) => {
        this.stompClient.subscribe('/resp/notice', (resp) => {
          const message = JSON.parse(resp.body)
          this.message.push(message)
          const key = message.id
          this.$notification.open({
            message: message.title,
            description: message.context,
            duration: null,
            style: {
              width: '600px',
              marginLeft: `${380 - 600}px`
            },
            icon: <a-icon type="smile" style="color: #108ee9" />,
            btn: h => {
              return h(
                  'a-button',
                  {
                    props: {
                      type: 'primary',
                      size: 'small',
                    },
                    on: {
                      click: () => this.$notification.close(key),
                    },
                  },
                  'Confirm',
              )
            },
            key,
            onClose: close,
          })
        })
      })
    }
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
</style>
