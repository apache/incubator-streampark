<script setup lang="ts">
interface Props {
    disabled?: boolean
}

const { disabled = false } = defineProps<Props>()

interface IconList {
    prefix: string
    icons: string[]
    title: string
    total: number
    categories?: Record<string, string[]>
    uncategorized?: string[]
}
const value = defineModel('value', { type: String })

// Ionicons5 via Iconify API (same source as @vicons/ionicons5)
const nameList = ['ion', 'carbon']

async function fetchIconList(name: string): Promise<IconList> {
    return await fetch(`https://api.iconify.design/collection?prefix=${name}`).then((res) =>
        res.json(),
    )
}

async function fetchIconAllList(nameList: string[]) {
    const targets = await Promise.all(nameList.map(fetchIconList))

    const iconList = targets.map((item) => {
        const icons = [
            ...(item.categories ? Object.values(item.categories).flat() : []),
            ...(item.uncategorized ? Object.values(item.uncategorized).flat() : []),
        ]
        return { ...item, icons }
    })

    const svgNames = Object.keys(import.meta.glob('@/assets/svg-icons/*.svg'))
        .map((path) => path.split('/').pop()?.replace('.svg', ''))
        .filter(Boolean) as string[]

    iconList.unshift({
        prefix: 'local',
        title: 'Local Icons',
        icons: svgNames,
        total: svgNames.length,
        uncategorized: svgNames,
    })

    return iconList
}

const iconList = shallowRef<IconList[]>([])

onMounted(async () => {
    iconList.value = await fetchIconAllList(nameList)
})

const currentTab = shallowRef(0)
const currentTag = shallowRef('')

const searchValue = ref('')

const currentPage = shallowRef(1)

function handleChangeTab(index: number) {
    currentTab.value = index
    currentTag.value = ''
    currentPage.value = 1
}

function handleSelectIconTag(icon: string) {
    currentTag.value = currentTag.value === icon ? '' : icon
    currentPage.value = 1
}

const icons = computed(() => {
    if (!iconList.value[currentTab.value]) return []
    const hasTag = !!currentTag.value
    return hasTag
        ? iconList.value[currentTab.value]?.categories?.[currentTag.value] || []
        : iconList.value[currentTab.value].icons || []
})

const filteredIcons = computed(() => {
    return icons.value?.filter((i) => i.includes(searchValue.value)) || []
})

const visibleIcons = computed(() => {
    return filteredIcons.value.slice((currentPage.value - 1) * 200, currentPage.value * 200)
})

const showModal = ref(false)

function handleSelectIcon(icon: string) {
    value.value = icon
    showModal.value = false
}

function clearIcon() {
    value.value = ''
    showModal.value = false
}
</script>

<template>
    <n-input-group disabled>
        <n-button v-if="value" :disabled="disabled" type="primary">
            <template #icon>
                <nova-icon :icon="value" />
            </template>
        </n-button>
        <n-input
            :value="value"
            readonly
            :placeholder="$t('components.iconSelector.inputPlaceholder')"
        />
        <n-button type="primary" ghost :disabled="disabled" @click="showModal = true">
            {{ $t('common.choose') }}
        </n-button>
    </n-input-group>
    <n-modal
        v-model:show="showModal"
        preset="card"
        :title="$t('components.iconSelector.selectorTitle')"
        size="small"
        class="w-800px"
        :bordered="false"
    >
        <template #header-extra>
            <n-button type="warning" size="small" ghost @click="clearIcon">
                {{ $t('components.iconSelector.clearIcon') }}
            </n-button>
        </template>

        <n-tabs
            :value="currentTab"
            type="line"
            animated
            placement="left"
            @update:value="handleChangeTab"
        >
            <n-tab-pane
                v-for="(list, index) in iconList"
                :key="list.prefix"
                :name="index"
                :tab="list.title"
            >
                <n-flex vertical>
                    <n-flex size="small">
                        <n-tag
                            v-for="(_v, k) in list.categories"
                            :key="k"
                            :checked="currentTag === k"
                            round
                            checkable
                            size="small"
                            @update:checked="handleSelectIconTag(k)"
                        >
                            {{ k }}
                        </n-tag>
                    </n-flex>

                    <n-input
                        v-model:value="searchValue"
                        type="text"
                        clearable
                        :placeholder="$t('components.iconSelector.searchPlaceholder')"
                    />

                    <div>
                        <n-flex :size="2">
                            <n-el
                                v-for="icon in visibleIcons"
                                :key="icon"
                                class="hover:(text-[var(--primary-color)] ring-1) ring-[var(--primary-color)] p-1 rounded flex-center"
                                :title="`${list.prefix}:${icon}`"
                                @click="handleSelectIcon(`${list.prefix}:${icon}`)"
                            >
                                <nova-icon :icon="`${list.prefix}:${icon}`" :size="24" />
                            </n-el>
                            <n-empty v-if="visibleIcons.length === 0" class="w-full" />
                        </n-flex>
                    </div>

                    <n-flex justify="center">
                        <n-pagination
                            v-model:page="currentPage"
                            :item-count="filteredIcons.length"
                            :page-size="200"
                        />
                    </n-flex>
                </n-flex>
            </n-tab-pane>
        </n-tabs>
    </n-modal>
</template>
