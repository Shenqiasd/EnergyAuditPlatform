<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { adminMenus } from '@/config/menus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const username = computed(() => userStore.userInfo?.realName || '管理员')

function isActive(path: string): boolean {
  return route.path === path || route.path.startsWith(path + '/')
}
function navigate(path: string) { router.push(path) }
function logout() { userStore.logout(); router.push('/login') }
</script>

<template>
  <div class="main-layout">
    <aside class="sidebar">
      <div class="sidebar__header">
        <div class="sidebar__logo">⚙️</div>
        <div>
          <div class="sidebar__logo-title">能碳审计平台</div>
          <div class="sidebar__logo-sub">管理端</div>
        </div>
      </div>
      <nav class="sidebar__menu">
        <template v-for="group in adminMenus" :key="group.section">
          <div class="sidebar__section">{{ group.section }}</div>
          <div
            v-for="item in group.items"
            :key="item.key"
            class="sidebar__item"
            :class="{ 'is-active': item.path && isActive(item.path) }"
            @click="item.path && navigate(item.path)"
          >
            <span class="sidebar__item-dot"></span>
            <span class="sidebar__item-icon">{{ item.icon }}</span>
            <span class="sidebar__item-text">{{ item.title }}</span>
            <span v-if="item.badge" class="sidebar__item-badge">{{ item.badge }}</span>
          </div>
        </template>
      </nav>
      <div class="sidebar__footer">
        <div class="sidebar__avatar">管</div>
        <div style="flex:1;min-width:0">
          <div class="sidebar__user-name">{{ username }}</div>
          <div class="sidebar__user-sub">系统管理员</div>
        </div>
      </div>
    </aside>
    <div class="layout-content">
      <header class="topbar">
        <div class="breadcrumb">管理后台 / <strong>{{ route.meta.title || '首页' }}</strong></div>
        <div class="topbar-right">
          <el-button link @click="logout" class="topbar-logout">退出</el-button>
        </div>
      </header>
      <main class="layout-main"><router-view /></main>
    </div>
  </div>
</template>

<style lang="scss" scoped>
@use '@/styles/variables' as *;
@use '@/styles/sidebar';
.main-layout { display: flex; height: 100vh; overflow: hidden; background: $sidebar-bg; }
.layout-content { flex: 1; display: flex; flex-direction: column; background: $bg; overflow: hidden; border-radius: 12px 0 0 0; }
.topbar { height: $topbar-height; background: #fff; border-bottom: 1px solid $border; display: flex; align-items: center; justify-content: space-between; padding: 0 24px; flex-shrink: 0; }
.breadcrumb { font-size: 13px; color: $text-tertiary; strong { color: $text-primary; } }
.topbar-right { display: flex; align-items: center; gap: 12px; }
.topbar-logout { font-size: 13px; color: $text-tertiary; }
.layout-main { flex: 1; overflow-y: auto; }
</style>
