<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
-->
<script setup lang="ts">
import type { TreeOption } from 'naive-ui'
import type { RoleListRecord } from '@/types/api/system/model/roleModel'
import { fetchMenuList, fetchRoleMenu } from '@/service'
import {
    collectLeafKeys,
    filterCheckedLeafKeys,
    transformMenuTree,
    type MenuTreeNode,
} from '../../shared/utils'

const props = defineProps<{
    show: boolean
    record: RoleListRecord | null
}>()

const emit = defineEmits<{
    'update:show': [value: boolean]
}>()

const { t } = useI18n()
const treeData = ref<TreeOption[]>([])
const checkedKeys = ref<string[]>([])

watch(
    () => [props.show, props.record] as const,
    async ([show, record]) => {
        if (!show || !record) return
        const menuResult = await fetchMenuList()
        const nodes: MenuTreeNode[] = menuResult.data?.rows?.children ?? []
        treeData.value = transformMenuTree(nodes, t)
        const leafKeys = collectLeafKeys(nodes)
        const roleMenuResult = await fetchRoleMenu({ roleId: record.roleId })
        checkedKeys.value =
            roleMenuResult.isSuccess && roleMenuResult.data
                ? filterCheckedLeafKeys(roleMenuResult.data, leafKeys)
                : []
    },
)
</script>

<template>
    <n-drawer
        :show="show"
        :width="480"
        placement="right"
        @update:show="emit('update:show', $event)"
    >
        <n-drawer-content :title="t('system.role.roleInfo')" closable>
            <n-descriptions
                v-if="record"
                :column="1"
                label-placement="left"
                bordered
                class="mb-16px"
            >
                <n-descriptions-item :label="t('system.role.form.roleName')">
                    {{ record.roleName }}
                </n-descriptions-item>
                <n-descriptions-item :label="t('common.description')">
                    {{ record.description || '-' }}
                </n-descriptions-item>
                <n-descriptions-item :label="t('common.createTime')">
                    {{ record.createTime || '-' }}
                </n-descriptions-item>
                <n-descriptions-item :label="t('common.modifyTime')">
                    {{ record.modifyTime || t('system.role.modifyTime') }}
                </n-descriptions-item>
            </n-descriptions>
            <div class="font-medium mb-8px">
                {{ t('system.role.assignment') }}
            </div>
            <n-tree
                v-if="treeData.length"
                v-model:checked-keys="checkedKeys"
                :data="treeData"
                checkable
                cascade
                block-line
                default-expand-all
                disabled
            />
        </n-drawer-content>
    </n-drawer>
</template>
