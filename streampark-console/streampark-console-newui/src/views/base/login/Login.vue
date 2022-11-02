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
    class="w-full bg-light-200 relative section bg-gradient-primary pb-260px md:pt-130px px-50px overflow-hidden"
  >
    <div class="overlay bg-gradient-primary !opacity-90 -z-1"></div>

    <figure class="scribble scale-4 !opacity-10 top-50 left-0">
      <SvgIcon name="block" class="text-secondary" :size="200" />
    </figure>

    <figure class="scribble scale-5 !opacity-10 top-50 left-0">
      <SvgIcon name="block" class="text-secondary" :size="200" />
    </figure>

    <figure class="scribble scale-6 !opacity-10 top-50 left-0">
      <SvgIcon name="block" class="text-secondary" :size="200" />
    </figure>

    <figure class="scribble scale-7 !opacity-10 top-50 left-0">
      <SvgIcon name="block" class="text-secondary" :size="200" />
    </figure>
    <div class="container px-7 pb-2">
      <Row :gutter="24">
        <Col :md="12" :span="24" class="self-center pr-5 z-100 -enter-x">
          <LoginSlogan />
        </Col>
        <Col :md="12" :span="24">
          <div
            :class="`${prefixCls}-form`"
            class="relative w-auto m-auto max-w-460px px-12 bg-[rgba(0,0,0,0.6)] py-5 shadow-md enter-x z-100"
          >
            <LoginForm />
          </div>
        </Col>
      </Row>
    </div>

    <figure class="absolute -bottom-1 left-0 right-0 top-auto text-light-200 mb-0 z-8">
      <svg
        class="waves"
        xmlns="http://www.w3.org/2000/svg"
        xmlns:xlink="http://www.w3.org/1999/xlink"
        viewBox="0 24 150 28"
        preserveAspectRatio="none"
        shape-rendering="auto"
      >
        <defs>
          <path
            id="gentle-wave"
            d="M-160 44c30 0 58-18 88-18s 58 18 88 18 58-18 88-18 58 18 88 18 v44h-352z"
          />
        </defs>
        <g class="parallax">
          <use xlink:href="#gentle-wave" x="48" y="0" fill="rgba(255,255,255,0.7" />
          <use xlink:href="#gentle-wave" x="48" y="3" fill="rgba(255,255,255,0.5)" />
          <use xlink:href="#gentle-wave" x="48" y="5" fill="rgba(255,255,255,0.3)" />
          <use xlink:href="#gentle-wave" x="48" y="7" fill="#fff" />
        </g>
      </svg>
    </figure>
  </div>
</template>
<script lang="ts" setup>
  import LoginForm from './LoginForm.vue';
  import LoginSlogan from './LoginSlogan';
  import { useDesign } from '/@/hooks/web/useDesign';
  import { Row, Col } from 'ant-design-vue';
  import { SvgIcon } from '/@/components/Icon';
  defineProps({
    sessionTimeout: {
      type: Boolean,
    },
  });

  // const globSetting = useGlobSetting();
  const { prefixCls } = useDesign('login');
  // const title = computed(() => globSetting?.title ?? '');
</script>
<style lang="less">
  @prefix-cls: ~'@{namespace}-login';
  @logo-prefix-cls: ~'@{namespace}-app-logo';
  @countdown-prefix-cls: ~'@{namespace}-countdown-input';
  @active-color: 255, 255, 255;

  input.fix-auto-fill,
  .fix-auto-fill input {
    box-shadow: inherit !important;
  }

  .bg-gradient-primary {
    background-image: linear-gradient(
      130deg,
      #0e18d2 15%,
      #3172f5 40%,
      #3172f5 60%,
      #60cff2 100%
    ) !important;
  }

  .overlay {
    border-radius: inherit;
    height: 100%;
    left: 0;
    opacity: 0.5;
    position: absolute;
    top: 0;
    width: 100%;
  }

  .@{prefix-cls}-form {
    .ant-input {
      padding-top: 3px;
      padding-bottom: 3px;

      .ant-input-affix-wrapper:hover,
      .ant-input:not(.ant-input-disabled) {
        border-color: rgba(@active-color, 0.95);
      }
    }
    .form-title {
      font-family: Poppins, sans-serif !important;
    }
    .text-left {
      .ant-btn {
        padding: 0px;
      }
    }
  }

  .waves {
    position: relative;
    width: 100%;
    height: 24vh;
    margin-bottom: -7px; /* 修复 Safari 等浏览器下方空隙 */
    min-height: 150px;
    max-height: 280px;
    width: 100%;
  }
  .parallax > use {
    animation: move-forever 25s cubic-bezier(0.55, 0.5, 0.45, 0.5) infinite;
  }
  .parallax > use:nth-child(1) {
    animation-delay: -2s;
    animation-duration: 7s;
  }
  .parallax > use:nth-child(2) {
    animation-delay: -3s;
    animation-duration: 8s;
  }
  .parallax > use:nth-child(3) {
    animation-delay: -4s;
    animation-duration: 9s;
  }
  .parallax > use:nth-child(4) {
    animation-delay: -5s;
    animation-duration: 10s;
  }
  @keyframes move-forever {
    0% {
      transform: translate3d(-90px, 0, 0);
    }
    100% {
      transform: translate3d(85px, 0, 0);
    }
  }
</style>
