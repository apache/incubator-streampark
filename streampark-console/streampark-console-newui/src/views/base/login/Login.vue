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
  <div :class="prefixCls" class="relative w-full h-full px-4">
    <div class="relative h-full">
      <div class="flex h-full">
        <div class="flex w-full h-full py-5 xl:h-auto xl:py-0 xl:my-0">
          <div
            :class="`${prefixCls}-form`"
            class="relative w-auto px-12 bg-[rgba(0,0,0,0.4)] py-5 mx-auto my-auto shadow-md enter-y"
          >
            <LoginForm />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script lang="ts" setup>
  import LoginForm from './LoginForm.vue';
  import { useDesign } from '/@/hooks/web/useDesign';

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

  .@{prefix-cls} {
    min-height: 100%;
    overflow: hidden;
    background: url('/@/assets/images/sign-bg.jpg') no-repeat 50%;
    background-size: cover;

    .ant-input-affix-wrapper {
      border: 1px solid rgba(@active-color, 0.55);
      border-radius: 1px;
      color: rgba(@active-color, 0.65);
      margin-top: 10px;
      background-color: rgba(@active-color, 0.05) !important;
    }

    .signin-title {
      padding-top: 20px;
      font-size: 16px;
      color: rgba(255, 255, 255, 0.65);
      margin-top: 12px;
      margin-bottom: 20px;
    }

    .logo {
      padding-top: 20px;
      height: 130px;
      margin: auto;
    }

    .signin-form {
      .ant-form-item-has-error :not(.ant-input-disabled):not(.ant-input-borderless).ant-input {
        background-color: transparent !important;
      }

      .ant-input {
        padding-top: 3px;
        padding-bottom: 3px;
        color: @white;

        .ant-input-affix-wrapper:hover,
        .ant-input:not(.ant-input-disabled) {
          border-color: rgba(@active-color, 0.95);
        }
      }
      .ant-input-password-icon {
        color: @content-bg !important;
      }
      .signin-btn {
        .ant-btn {
          margin-top: 30px;
          height: 40px;
          background: rgba(@active-color, 0.4);
          border: unset;
        }
      }

      .text-left {
        .ant-btn {
          padding: 0px;
        }
      }
    }

    &::after {
      content: '';
      width: 100%;
      position: absolute;
      left: 0;
      top: 0;
      bottom: -20px;
      background: inherit;
      z-index: 2;
      filter: blur(1px);
    }

    &-sign-in-way {
      .anticon {
        font-size: 22px;
        color: #888;
        cursor: pointer;

        &:hover {
          color: @primary-color;
        }
      }
    }

    input {
      min-width: 300px;
      background: transparent;
      &:autofill {
        background: transparent;
      }
      @media (max-width: @screen-xl) {
        min-width: 260px;
      }

      @media (max-width: @screen-lg) {
        min-width: 200px;
      }

      @media (max-width: @screen-md) {
        min-width: 180px;
      }

      @media (max-width: @screen-sm) {
        min-width: 100px;
      }
    }

    .@{countdown-prefix-cls} input {
      min-width: unset;
    }

    .ant-divider-inner-text {
      font-size: 12px;
      color: @text-color-secondary;
    }
  }
</style>
