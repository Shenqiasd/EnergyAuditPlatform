<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

type Portal = 'enterprise' | 'admin' | 'auditor'
const activePortal = ref<Portal>('enterprise')

const form = reactive({ username: '', password: '' })
const loading = ref(false)

const portals = [
  { key: 'enterprise', label: '企业端' },
  { key: 'admin',      label: '管理端' },
  { key: 'auditor',    label: '审核端' },
] as const

const portalRedirect: Record<Portal, string> = {
  enterprise: '/enterprise/dashboard',
  admin:      '/admin/dashboard',
  auditor:    '/auditor/dashboard',
}

async function handleLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    await userStore.login({ username: form.username, password: form.password, portal: activePortal.value })
    router.push(portalRedirect[activePortal.value])
  } catch (e: any) {
    ElMessage.error(e?.message || '登录失败，请检查账号密码')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-wrap">
    <!-- Left panel -->
    <div class="login-left">
      <div class="login-logo">
        <div class="login-logo-icon">🌿</div>
        <div>
          <div class="login-logo-title">能碳审计管理平台</div>
          <div class="login-logo-en">Municipal Industrial Energy Carbon Audit Platform</div>
        </div>
      </div>

      <div class="login-hero">
        <h1>推动工业企业<br><em>绿色低碳</em>转型</h1>
        <p>服务政府能源审计与碳排放管理，以数字化手段驱动工业节能降碳，助力城市"双碳"战略落地。</p>
      </div>

      <div class="leaf-tags">
        <span class="leaf-tag">🏭 工业能源审计</span>
        <span class="leaf-tag">🌍 碳排放管理</span>
        <span class="leaf-tag">📊 节能分析</span>
        <span class="leaf-tag">📑 报告生成</span>
      </div>

      <div class="login-stats">
        <div class="stat-item">
          <div class="stat-num">2,841</div>
          <div class="stat-label">接入企业数</div>
        </div>
        <div class="stat-item">
          <div class="stat-num">12.4万</div>
          <div class="stat-label">累计节能量(吨标煤)</div>
        </div>
        <div class="stat-item">
          <div class="stat-num">8.6万</div>
          <div class="stat-label">碳减排量(tCO₂)</div>
        </div>
      </div>
    </div>

    <!-- Right panel: Login card -->
    <div class="login-right">
      <div class="login-card">
        <div class="card-title">欢迎登录</div>
        <div class="card-sub">选择登录身份开始使用</div>

        <!-- Portal tabs -->
        <div class="portal-tabs">
          <div
            v-for="p in portals"
            :key="p.key"
            class="portal-tab"
            :class="{ 'is-active': activePortal === p.key }"
            @click="activePortal = p.key as Portal"
          >{{ p.label }}</div>
        </div>

        <!-- Form -->
        <el-form @submit.prevent="handleLogin">
          <el-form-item>
            <div class="form-label">账号</div>
            <el-input
              v-model="form.username"
              placeholder="统一社会信用代码或用户名"
              size="large"
            />
          </el-form-item>
          <el-form-item>
            <div class="form-label">密码</div>
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              show-password
              size="large"
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            style="width:100%;margin-top:4px;font-size:15px;font-weight:600;"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form>

        <div class="card-footer">
          <router-link to="/register">申请注册</router-link>
          <span>·</span>
          <a href="#">忘记密码</a>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
@use '@/styles/variables' as *;

.login-wrap {
  width: 100vw; height: 100vh;
  display: flex;
  background: linear-gradient(160deg, #0d2b26 0%, #1b4a3d 40%, #0a3d2e 100%);
  overflow: hidden;
}

.login-left {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 80px;
  position: relative;
  z-index: 1;
}

.login-logo {
  display: flex; align-items: center; gap: 16px;
  margin-bottom: 56px;
  .login-logo-icon {
    width: 52px; height: 52px;
    background: linear-gradient(135deg, #00897B, #43a047);
    border-radius: 14px;
    display: flex; align-items: center; justify-content: center;
    font-size: 26px;
    box-shadow: 0 8px 24px rgba(0,137,123,0.4);
  }
  .login-logo-title { font-size: 21px; color: #fff; font-weight: 700; letter-spacing: 1px; }
  .login-logo-en    { font-size: 12px; color: rgba(255,255,255,0.35); margin-top: 3px; }
}

.login-hero {
  margin-bottom: 32px;
  h1 {
    font-size: 42px; color: #fff; font-weight: 700;
    line-height: 1.25; margin-bottom: 20px;
    em { color: #4db6ac; font-style: normal; }
  }
  p {
    font-size: 15px; color: rgba(255,255,255,0.5);
    line-height: 1.9; max-width: 420px;
  }
}

.leaf-tags {
  display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 48px;
  .leaf-tag {
    padding: 6px 16px;
    background: rgba(0,137,123,0.15);
    border: 1px solid rgba(0,137,123,0.3);
    border-radius: 20px;
    font-size: 13px; color: #4db6ac;
  }
}

.login-stats {
  display: flex; gap: 40px;
  padding-top: 32px;
  border-top: 1px solid rgba(255,255,255,0.08);
  .stat-item {
    .stat-num   { font-size: 28px; color: #4db6ac; font-weight: 700; }
    .stat-label { font-size: 12px; color: rgba(255,255,255,0.35); margin-top: 4px; }
  }
}

.login-right {
  width: 480px;
  display: flex; align-items: center; justify-content: center;
  background: rgba(255,255,255,0.04);
  border-left: 1px solid rgba(255,255,255,0.07);
  backdrop-filter: blur(12px);
}

.login-card {
  width: 360px;
  background: #fff;
  border-radius: 20px;
  padding: 48px 40px;
  box-shadow: 0 24px 64px rgba(0,0,0,0.35);

  .card-title { font-size: 24px; font-weight: 700; color: $text-primary; margin-bottom: 6px; }
  .card-sub   { font-size: 14px; color: $text-tertiary; margin-bottom: 28px; }
}

.portal-tabs {
  display: flex; gap: 4px;
  background: #f4f6f4; padding: 4px; border-radius: 10px;
  margin-bottom: 28px;
  .portal-tab {
    flex: 1; padding: 9px 0; text-align: center;
    font-size: 13px; font-weight: 500;
    border-radius: 7px; cursor: pointer;
    color: $text-tertiary; transition: $transition;
    &.is-active {
      background: #fff; color: $primary;
      box-shadow: 0 2px 8px rgba(0,0,0,0.08);
    }
  }
}

.form-label {
  font-size: 13px; color: $text-secondary;
  font-weight: 500; margin-bottom: 6px;
}

.card-footer {
  text-align: center; margin-top: 20px;
  font-size: 13px; color: $text-tertiary;
  display: flex; justify-content: center; gap: 8px;
  a { color: $primary; }
}

// Override Element Plus input border color on focus
:deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 1px #00897B inset !important; }
:deep(.el-form-item) { margin-bottom: 18px; }
</style>
