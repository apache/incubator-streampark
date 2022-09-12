<!--

    Licensed to the Apache Software Foundation (ASF) under one or more
    contributor license agreements.  See the NOTICE file distributed with
    this work for additional information regarding copyright ownership.
    The ASF licenses this file to You under the Apache License, Version 2.0
    (the "License"); you may not use this file except in compliance with
    the License.  You may obtain a copy of the License at

       https://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

-->

<template>
  <div
    class="logo">
    <router-link
      :to="{path:'/flink/app'}">
      <img
        :src="logoImg"

        alt="logo">
    </router-link>
  </div>
</template>

<script>

export default {
  name: 'Logo',
  props: {
    title: {
      type: String,
      default: 'StreamPark',
      required: false
    },
    showTitle: {
      type: Boolean,
      default: true,
      required: false
    }
  },
  data () {
    return {
      logoImg: require('@assets/imgs/logo1.svg')
    }
  },
  methods: {
    collapsed(flag) {
      if(flag) {
        setTimeout(()=>{
          this.logoImg = require('@assets/imgs/logo1.svg')
        },50)
      } else {
        setTimeout(()=>{
          this.logoImg = require('@assets/imgs/logo1.svg')
        },50)
      }
    }
  }
}
</script>
