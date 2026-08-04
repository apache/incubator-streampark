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
import type { FormInst, FormRules, TreeInst, TreeOption } from 'naive-ui'
import type { RoleListRecord } from '@/types/api/system/model/roleModel'
import {
    fetchCheckRoleName,
    fetchMenuList,
    fetchRoleCreate,
    fetchRoleMenu,
    fetchRoleUpdate,
} from '@/service'
import { FormTypeEnum } from '../../shared/constants'
import {
    collectLeafKeys,
    filterCheckedLeafKeys,
    getPermissionMenuId,
    resetPermissionIdMap,
    transformMenuTree,
    type MenuTreeNode,
} from '../../shared/utils'

const props = defineProps<{
    show: boolean
    formType: FormTypeEnum
    record?: RoleListRecord | null
}>()

const emit = defineEmits<{
    'update:show': [value: boolean]
    success: []
}>()

const { t } = useI18n()

const formRef = ref<FormInst | null>(null)
const treeRef = ref<TreeInst | null>(null)
const submitting = ref(false)
const treeData = ref<TreeOption[]>([])
const checkedKeys = ref<string[]>([])
const rawMenuNodes = ref<MenuTreeNode[]>([])
const leafKeys = ref<string[]>([])

const formModel = ref({
    roleId: '',
    roleName: '',
    description: '',
})

const isCreate = computed(() => props.formType === FormTypeEnum.Create)

const drawerTitle = computed(
    () =>
        ({
            [FormTypeEnum.Create]: t('system.role.form.create'),
            [FormTypeEnum.Edit]: t('system.role.form.edit'),
            [FormTypeEnum.View]: t('system.role.form.view'),
        })[props.formType],
)

const rules = computed<FormRules>(() => ({
    roleName: isCreate.value
        ? [
              {
                  required: true,
                  trigger: 'blur',
                  asyncValidator: async (_rule, value: string) => {
                      if (!value) throw new Error(t('system.role.form.empty'))
                      if (value.length > 255) throw new Error(t('system.role.form.roleNameLen'))
                      const result = await fetchCheckRoleName({ roleName: value })
                      if (!result.isSuccess || !result.data)
                          throw new Error(t('system.role.form.exist'))
                  },
              },
          ]
        : [],
}))

async function ensureMenuTree() {
    if (treeData.value.length) return
    resetPermissionIdMap()
    const result = await fetchMenuList()
    if (!result.isSuccess) throwApiFailure(result, t('sys.api.apiRequestFailed'))
    rawMenuNodes.value = result.data?.rows?.children ?? []
    leafKeys.value = collectLeafKeys(rawMenuNodes.value)
    treeData.value = transformMenuTree(rawMenuNodes.value, t)
}

watch(
    () => [props.show, props.formType, props.record] as const,
    async ([show, formType, record]) => {
        if (!show) return
        formModel.value = {
            roleId: record?.roleId ?? '',
            roleName: record?.roleName ?? '',
            description: record?.description ?? '',
        }
        checkedKeys.value = []
        await ensureMenuTree()
        if (formType !== FormTypeEnum.Create && record?.roleId) {
            const menuResult = await fetchRoleMenu({ roleId: record.roleId })
            if (menuResult.isSuccess && menuResult.data)
                checkedKeys.value = filterCheckedLeafKeys(menuResult.data, leafKeys.value)
        }
        nextTick(() => formRef.value?.restoreValidation())
    },
    { immediate: true },
)

function closeDrawer() {
    emit('update:show', false)
}

async function handleSubmit() {
    await formRef.value?.validate()
    const appViewId = getPermissionMenuId('app:view')
    const checked = treeRef.value?.getCheckedData()?.keys ?? checkedKeys.value
    const indeterminate = treeRef.value?.getIndeterminateData()?.keys ?? []
    const menuIds = [...new Set([...checked, ...indeterminate])]
    if (appViewId && !menuIds.includes(appViewId)) {
        window.$message?.warning(t('system.role.form.noViewPermission'))
        return
    }
    if (!menuIds.length) {
        window.$message?.warning(t('system.role.form.menuIdRequired'))
        return
    }

    submitting.value = true
    try {
        const payload = {
            ...formModel.value,
            menuId: menuIds.join(','),
        }
        const result = isCreate.value
            ? await fetchRoleCreate(payload as any)
            : await fetchRoleUpdate(payload as any)
        if (!result.isSuccess) throwApiFailure(result, t('sys.api.apiRequestFailed'))
        closeDrawer()
        emit('success')
    } catch (e: any) {
        if (e?.message) window.$message?.error(e.message)
    } finally {
        submitting.value = false
    }
}
</script>

<template>
    <n-drawer
        :show="show"
        :width="480"
        placement="right"
        @update:show="emit('update:show', $event)"
    >
        <n-drawer-content :title="drawerTitle" closable>
            <n-form ref="formRef" :model="formModel" :rules="rules" label-placement="top">
                <n-form-item :label="t('system.role.form.roleName')" path="roleName">
                    <n-input v-model:value="formModel.roleName" :disabled="!isCreate" />
                </n-form-item>
                <n-form-item :label="t('common.description')" path="description">
                    <n-input v-model:value="formModel.description" type="textarea" :rows="3" />
                </n-form-item>
                <n-form-item :label="t('system.role.assignment')" required>
                    <n-tree
                        v-if="treeData.length"
                        ref="treeRef"
                        v-model:checked-keys="checkedKeys"
                        :data="treeData"
                        checkable
                        cascade
                        block-line
                        default-expand-all
                    />
                </n-form-item>
            </n-form>
            <template #footer>
                <n-space justify="end">
                    <n-button class="e2e-role-pop-cancel" @click="closeDrawer">
                        {{ t('common.cancelText') }}
                    </n-button>
                    <n-button
                        class="e2e-role-pop-ok"
                        type="primary"
                        :loading="submitting"
                        @click="handleSubmit"
                    >
                        {{ t('common.submitText') }}
                    </n-button>
                </n-space>
            </template>
        </n-drawer-content>
    </n-drawer>
</template>
