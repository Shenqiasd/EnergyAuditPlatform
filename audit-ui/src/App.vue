<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import ChangePasswordDialog from '@/components/ChangePasswordDialog.vue'

const route = useRoute()
const userStore = useUserStore()

const showChangePassword = computed(() => {
  return route.path !== '/login' && userStore.isLoggedIn && userStore.needChangePassword
})

onMounted(async () => {
  if (userStore.token && !userStore.userInfo) {
    try {
      await userStore.fetchUserInfo()
    } catch {
      userStore.logout()
    }
  }
})

watch(() => userStore.token, async (newToken) => {
  if (newToken && !userStore.userInfo) {
    try {
      await userStore.fetchUserInfo()
    } catch {
      userStore.logout()
    }
  }
})
</script>

<template>
  <router-view />
  <ChangePasswordDialog v-model="showChangePassword" />
</template>
