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
  <div class="relative h-full bg-gradient-primary overflow-auto">
    <div class="flex items-center locale-picker-border absolute right-15 top-10">
      <AppLocalePicker
        class="text-white enter-x locale-picker"
        v-if="getShowLocalePicker"
        :reload="true"
        :showText="false"
      />
    </div>
    <div class="w-full relative h-[calc(100%-70px)] min-h-700px flex items-center section">
      <div class="scribble-box w-[80%] h-full absolute overflow-hidden">
        <figure class="scribble scale-2 !opacity-10 top-50 left-0">
          <SvgIcon name="block" class="text-secondary" :size="200" />
        </figure>

        <figure class="scribble scale-3 !opacity-10 top-50 left-0">
          <SvgIcon name="block" class="text-secondary" :size="200" />
        </figure>

        <figure class="scribble scale-4 !opacity-10 top-50 left-0">
          <SvgIcon name="block" class="text-secondary" :size="200" />
        </figure>

        <figure class="scribble scale-5 !opacity-10 top-50 left-0">
          <SvgIcon name="block" class="text-secondary" :size="200" />
        </figure>
      </div>
      <div class="w-full px-100px mx-auto pb-2">
        <Row :gutter="24">
          <Col :md="12" :span="24" class="self-center pr-5 z-100 -enter-x">
            <LoginSlogan />
          </Col>
          <Col :md="12" :span="24">
            <div
              :class="`${prefixCls}-form`"
              class="relative w-auto m-auto max-w-420px bg-[rgba(0,0,0,0.65)] rounded-2px px-10 py-5 shadow-2xl shadow-blue-500 enter-x z-100"
            >
              <LoginForm />
            </div>
          </Col>
        </Row>
      </div>
    </div>
    <footer class="w-1150px m-auto text-center bg-transparent opacity-60">
      <div class="flex items-center justify-center">
        <a
          :href="TWITTER_URL"
          target="_blank"
          class="text-light-100 mx-3 cursor-pointer hover:text-light-400 dark:text-light-100"
        >
          <Icon icon="hugeicons:new-twitter" />
        </a>
        <div class="mx-3 text-light-100 cursor-pointer">
          <Popover placement="top" trigger="hover" arrow-point-at-center>
            <template #content>
              <img src="/@/assets/images/join_wechat.png" alt="qrcode" class="h-150px w-150px" />
            </template>
            <Icon icon="cib:wechat" />
          </Popover>
        </div>
        <a
          :href="EMAIL_URL"
          target="_blank"
          class="text-light-100 mx-3 cursor-pointer hover:text-light-100 dark:text-light-100"
        >
          <Icon icon="ic:round-email" />
        </a>
        <a
          :href="GITHUB_URL"
          target="_blank"
          class="text-light-100 mx-3 cursor-pointer hover:text-light-100 dark:text-light-100"
        >
          <Icon icon="ant-design:github-filled" />
        </a>
      </div>
      <p class="text-light-100 pt-10px">
        Copyright © 2022-{{ `${new Date().getFullYear()}` }} The Apache Software Foundation. Apache
        StreamPark, StreamPark, and its feather logo are trademarks of The Apache Software
        Foundation.
      </p>
    </footer>
  </div>
</template>
<script lang="ts" setup>
  import LoginForm from './LoginForm.vue';
  import LoginSlogan from './LoginSlogan';
  import { useDesign } from '/@/hooks/web/useDesign';
  import { Row, Popover, Col } from 'ant-design-vue';
  import Icon, { SvgIcon } from '/@/components/Icon';
  import { useLocale } from '/@/locales/useLocale';
  import { AppLocalePicker } from '/@/components/Application';
  import { GITHUB_URL, EMAIL_URL, TWITTER_URL } from '/@/settings/siteSetting';
  defineProps({
    sessionTimeout: {
      type: Boolean,
    },
  });

  // const globSetting = useGlobSetting();
  const { getShowLocalePicker } = useLocale();
  const { prefixCls } = useDesign('login');
  sessionStorage.removeItem('appPageNo');
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

  .@{prefix-cls}-form {
    .ant-input {
      padding-top: 3px;
      padding-bottom: 3px;

      .ant-input-affix-wrapper:hover,
      .ant-input:not(.ant-input-disabled) {
        border-color: rgba(@active-color, 0.95);
      }
    }

    .text-left {
      .ant-btn {
        padding: 0px;
      }
    }
  }

  [data-theme='dark'] {
    .@{prefix-cls}-form {
      .ant-form-item-has-error
        :not(.ant-input-affix-wrapper-disabled):not(
          .ant-input-affix-wrapper-borderless
        ).ant-input-affix-wrapper,
      .ant-input-affix-wrapper,
      .ant-input {
        color: rgba(0, 0, 0, 0.85);
        background-color: white;

        &::placeholder {
          color: #bdbdbe !important;
        }
      }
    }
  }

  .locale-picker-border {
    border: 1px solid rgba(255, 255, 255, 0.6);
    border-radius: 6px;
  }

  .locale-picker {
    padding: 6px;
  }
</style>
