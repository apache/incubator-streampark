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
    <div class="container relative h-full py-2">
      <div class="flex h-full">
        <div class="flex w-full h-full py-5 xl:h-auto xl:py-0 xl:my-0">
          <div
            :class="`${prefixCls}-form`"
            class="relative w-auto px-8 bg-[rgba(0,20,25,0.6)] py-12 mx-auto my-auto shadow-md enter-y"
          >
            <LoginForm/>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script lang="ts" setup>
import LoginForm from './LoginForm.vue';
import ForgetPasswordForm from './ForgetPasswordForm.vue';
import {useDesign} from '/@/hooks/web/useDesign';

defineProps({
  sessionTimeout: {
    type: Boolean,
  },
});

// const globSetting = useGlobSetting();
const {prefixCls} = useDesign('login');
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
    height: 130px;
    margin: auto;
  }

  .signin-form {
    .ant-input {
      padding-top: 5px;
      padding-bottom: 5px;

      .ant-input-affix-wrapper:hover, .ant-input:not(.ant-input-disabled) {
        border-color: rgba(@active-color, .95);
      }
    }


    .signin-btn {
      .ant-btn {
        margin-top: 30px;
        height: 40px;
        background: rgba(@active-color, .40);
        border: unset;
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

  .@{logo-prefix-cls} {
    position: absolute;
    top: 12px;
    height: 30px;

    &__title {
      font-size: 16px;
      color: #fff;
    }

    img {
      width: 180px;
    }
  }

  .container {
    .@{logo-prefix-cls} {
      display: flex;
      width: 60%;
      height: 80px;

      &__title {
        font-size: 24px;
        color: #fff;
      }

      img {
        width: 180px;
      }
    }
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

  input:not([type='checkbox']) {
    min-width: 360px;

    @media (max-width: @screen-xl) {
      min-width: 320px;
    }

    @media (max-width: @screen-lg) {
      min-width: 260px;
    }

    @media (max-width: @screen-md) {
      min-width: 240px;
    }

    @media (max-width: @screen-sm) {
      min-width: 160px;
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
