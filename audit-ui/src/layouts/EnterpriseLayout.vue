<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { enterpriseMenuItems } from '@/config/menus'
import type { MenuItem } from '@/config/menus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const enterpriseName = computed(() => userStore.userInfo?.enterpriseName || '企业用户')
const auditYear = computed(() => userStore.userInfo?.auditYear || new Date().getFullYear())
const avatarText = computed(() => {
  const name = enterpriseName.value
  return name.length >= 2 ? name.slice(0, 1) + '企' : name.slice(0, 1)
})

/* ── Collapsible group state ── */
const expandedKeys = ref<Set<string>>(new Set(['settings', 'charts', 'audit-report']))

function toggleGroup(key: string) {
  const next = new Set(expandedKeys.value)
  if (next.has(key)) next.delete(key)
  else next.add(key)
  expandedKeys.value = next
}

function isGroupActive(item: MenuItem): boolean {
  if (!item.children) return false
  return item.children.some(c => c.path && (route.path === c.path || route.path.startsWith(c.path + '/')))
}

function isActive(path: string): boolean {
  return route.path === path || route.path.startsWith(path + '/')
}

function navigate(path: string) {
  router.push(path)
}

function handleItemClick(item: MenuItem) {
  if (item.disabled) return
  if (item.children) {
    toggleGroup(item.key)
  } else if (item.path) {
    navigate(item.path)
  }
}

function handleChildClick(child: MenuItem) {
  if (child.disabled) return
  if (child.path) navigate(child.path)
}

function logout() {
  userStore.logout()
  router.push('/login')
}
</script>

<template>
  <div class="main-layout">
    <!-- Sidebar -->
    <aside class="sidebar">
      <div class="sidebar__header">
        <div class="sidebar__logo">🌿</div>
        <div>
          <div class="sidebar__logo-title">能碳审计平台</div>
          <div class="sidebar__logo-sub">企业端</div>
        </div>
      </div>

      <nav class="sidebar__menu">
        <template v-for="item in enterpriseMenuItems" :key="item.key">
          <!-- Standalone item (no children) -->
          <div
            v-if="!item.children"
            class="sidebar__item"
            :class="{ 'is-active': item.path && isActive(item.path) }"
            @click="handleItemClick(item)"
          >
            <span class="sidebar__item-dot"></span>
            <span class="sidebar__item-icon">{{ item.icon }}</span>
            <span class="sidebar__item-text">{{ item.title }}</span>
          </div>

          <!-- Group item (has children) -->
          <template v-else>
            <div
              class="sidebar__group-header"
              :class="{ 'is-group-active': isGroupActive(item) }"
              @click="handleItemClick(item)"
            >
              <span class="sidebar__item-icon">{{ item.icon }}</span>
              <span class="sidebar__item-text">{{ item.title }}</span>
              <span class="sidebar__group-arrow" :class="{ 'is-expanded': expandedKeys.has(item.key) }">
                &#9656;
              </span>
            </div>

            <transition name="menu-slide">
              <div v-show="expandedKeys.has(item.key)" class="sidebar__group-children">
                <template v-for="child in item.children" :key="child.key">
                  <el-tooltip
                    :disabled="!child.disabled || !child.tooltip"
                    :content="child.tooltip ?? ''"
                    placement="right"
                    :show-after="200"
                  >
                    <div
                      class="sidebar__item sidebar__child-item"
                      :class="{
                        'is-active': !child.disabled && child.path && isActive(child.path),
                        'is-disabled': child.disabled,
                      }"
                      @click="handleChildClick(child)"
                    >
                      <span class="sidebar__item-dot"></span>
                      <span class="sidebar__item-icon">{{ child.icon }}</span>
                      <span class="sidebar__item-text">{{ child.title }}</span>
                      <span v-if="child.badge" class="sidebar__item-badge">{{ child.badge }}</span>
                    </div>
                  </el-tooltip>
                </template>
              </div>
            </transition>
          </template>
        </template>
      </nav>

      <div class="sidebar__footer">
        <div class="sidebar__avatar">{{ avatarText }}</div>
        <div style="flex:1;min-width:0">
          <div class="sidebar__user-name">{{ enterpriseName }}</div>
          <div class="sidebar__user-sub">{{ auditYear }} 审计年度</div>
        </div>
      </div>
    </aside>

    <!-- Content -->
    <div class="layout-content">
      <header class="topbar">
        <div class="breadcrumb">
          能碳审计平台 / <strong>{{ route.meta.title || '工作台' }}</strong>
        </div>
        <div class="topbar-right">
          <div class="topbar-chip">🗓 审计年度：{{ auditYear }}</div>
          <el-button link @click="logout" class="topbar-logout">退出</el-button>
        </div>
      </header>
      <main class="layout-main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style lang="scss" scoped>
@use '@/styles/variables' as *;
@use '@/styles/sidebar';

.main-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background: $sidebar-bg;
}

.layout-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: $bg;
  overflow: hidden;
  border-radius: 12px 0 0 0;
}

.topbar {
  height: $topbar-height;
  background: #fff;
  border-bottom: 1px solid $border;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  flex-shrink: 0;
}

.breadcrumb {
  font-size: 13px;
  color: $text-tertiary;
  strong { color: $text-primary; }
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.topbar-chip {
  padding: 5px 14px;
  background: $primary-light;
  border: 1px solid rgba(0, 137, 123, 0.2);
  border-radius: 20px;
  font-size: 12.5px;
  color: $primary;
  font-weight: 500;
}

.topbar-logout {
  font-size: 13px;
  color: $text-tertiary;
}

.layout-main {
  flex: 1;
  overflow-y: auto;
}

/* ── Group header ── */
.sidebar__group-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 12px;
  margin: 1px 8px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13.5px;
  color: $sidebar-text;
  transition: $transition;
  user-select: none;

  &:hover { color: #fff; background: rgba(255, 255, 255, 0.06); }

  &.is-group-active {
    color: rgba(255, 255, 255, 0.85);
  }
}

.sidebar__group-arrow {
  margin-left: auto;
  font-size: 11px;
  transition: transform 0.2s ease;
  color: rgba(255, 255, 255, 0.25);

  &.is-expanded {
    transform: rotate(90deg);
  }
}

.sidebar__group-children {
  overflow: hidden;
}

.sidebar__child-item {
  padding-left: 28px !important;

  &.is-disabled {
    color: #666 !important;
    cursor: not-allowed !important;
    pointer-events: auto; /* allow tooltip hover */

    &:hover {
      background: transparent !important;
      color: #666 !important;
    }

    .sidebar__item-dot { display: none; }
  }
}

/* ── Transition ── */
.menu-slide-enter-active,
.menu-slide-leave-active {
  transition: all 0.2s ease;
  max-height: 300px;
}

.menu-slide-enter-from,
.menu-slide-leave-to {
  max-height: 0;
  opacity: 0;
}
</style>
