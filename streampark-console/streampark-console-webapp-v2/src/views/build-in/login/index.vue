<script setup lang="ts">
defineOptions({ name: 'LoginPage' })
import LoginForm from './components/LoginForm/index.vue'
import TeamSelectModal from './components/TeamSelectModal.vue'
import logoIcon from '@/assets/images/logo.png'

const appName = import.meta.env.VITE_APP_NAME
const productName = computed(() => appName.replace(/^Apache\s+/i, ''))
</script>

<template>
    <n-el class="wh-full flex-center" style="background-color: var(--body-color)">
        <div class="fixed top-40px right-40px text-lg flex gap-2">
            <DarkModeSwitch />
            <LangsSwitch />
        </div>
        <div
            class="p-4xl h-full w-full sm:w-450px sm:h-unset"
            style="background: var(--card-color); box-shadow: var(--box-shadow-1)"
        >
            <div class="w-full flex flex-col items-center">
                <div class="my-20px flex flex-col items-center gap-3">
                    <img :src="logoIcon" alt="StreamPark" class="w-64px h-auto" />
                    <div class="text-center" style="color: var(--n-text-color)">
                        <div class="text-xs font-medium tracking-widest opacity-80">APACHE</div>
                        <div class="text-2xl font-light">
                            {{ productName }}
                        </div>
                    </div>
                </div>
                <LoginForm class="w-85%" />
            </div>
        </div>
        <TeamSelectModal />
    </n-el>
</template>
