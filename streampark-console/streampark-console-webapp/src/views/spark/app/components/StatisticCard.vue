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
<script setup lang="ts" name="StatisticCard">
  import { Card, Statistic, Divider, Skeleton } from 'ant-design-vue';
  defineProps({
    loading: { type: Boolean, default: false },
    statisticProps: {
      type: Object as PropType<Recordable>,
      default: () => ({ title: '', value: 0 }),
    },
    footerList: {
      type: Array as PropType<Array<{ title: string; value: string | number }>>,
      default: () => [],
    },
  });
</script>
<template>
  <div class="gutter-box">
    <Skeleton :loading="loading" active>
      <Card :bordered="false" class="dash-statistic">
        <Statistic
          v-bind="statisticProps"
          :value-style="{
            color: '#52c41a',
            fontSize: '36px',
            fontWeight: 500,
            textShadow: '1px 1px 0 rgba(0,0,0,0.1)',
          }"
        />
      </Card>
      <Divider class="def-margin-bottom" />
      <template v-for="(item, index) in footerList" :key="item.field">
        <span> {{ item.title }} </span>
        <strong class="pl-10px">{{ item.value }}</strong>
        <Divider type="vertical" v-if="index !== footerList.length - 1" />
      </template>
      <template v-if="footerList.length == 0">
        <div class="inline-block h-18px"></div>
      </template>
    </Skeleton>
  </div>
</template>
<style scoped></style>
