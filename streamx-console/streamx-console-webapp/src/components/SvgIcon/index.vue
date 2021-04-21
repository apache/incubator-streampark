<template>
  <span :class="'svg-icon-'.concat(size)" class="anticon">
    <analyse v-if="name==='analyse'"></analyse>
    <flink v-if="name==='flink'"></flink>
    <spark v-if="name==='spark'"></spark>
    <fql v-if="name==='fql'"></fql>
    <flinksql v-if="name==='flinksql'"></flinksql>
    <maven v-if="name==='maven'"></maven>
    <workspace v-if="name==='workspace'"></workspace>
    <threshold v-if="name==='threshold'"></threshold>
    <http v-if="name==='http'"></http>
    <deploy v-if="name==='deploy'"></deploy>
    <stream v-if="name==='stream'"></stream>
    <format v-if="name==='format'"></format>
    <dingding v-if="name==='dingding'"></dingding>
    <mail v-if="name==='mail'"></mail>
    <sms v-if="name==='sms'"></sms>
    <wechat v-if="name==='wechat'"></wechat>
  </span>
</template>

<script>
import {analyse,deploy, flink, flinksql, fql, http, maven, spark, stream, workspace, threshold ,format,dingding,mail,sms,wechat } from '@/core/icons'

export default {
  name: 'SvgIcon',
  components: {analyse,flink, flinksql, fql, spark, maven, workspace, http, deploy, stream, threshold ,format,dingding,mail,sms,wechat},
  props: {
    name: {
      type: String,
      default: ''
    },
    size: {
      type: String,
      default: 'default'
    }
  }
}
</script>

<style scoped>
.svg-icon-default > svg {
  height: 1em !important;
  max-width: 15px !important;
  font-weight: normal;
}

.svg-icon-middle > svg {
  max-height: 25px !important;
  max-width: 25px !important;
}

.svg-icon-large > svg {
  max-height: 50px !important;
  max-width: 50px !important;
}
</style>