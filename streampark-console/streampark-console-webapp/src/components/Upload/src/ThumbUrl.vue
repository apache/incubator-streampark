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
  <span class="thumb">
    <Image v-if="fileUrl" :src="fileUrl" :width="104" />
  </span>
</template>
<script lang="ts">
  import { defineComponent } from 'vue';
  import { propTypes } from '/@/utils/propTypes';
  import { Image } from 'ant-design-vue';

  export default defineComponent({
    components: { Image },
    props: {
      fileUrl: propTypes.string.def(''),
      fileName: propTypes.string.def(''),
    },
  });
</script>
<style lang="less">
  .thumb {
    img {
      position: static;
      display: block;
      cursor: zoom-in;
      border-radius: 4px;
      object-fit: cover;
    }
  }
</style>
