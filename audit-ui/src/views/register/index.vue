<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { submit } from '@/api/registration'

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)
const submitted = ref(false)

const form = reactive({
  enterpriseName: '',
  creditCode: '',
  contactPerson: '',
  contactPhone: '',
  contactEmail: '',
})

const rules: FormRules = {
  enterpriseName: [{ required: true, message: '请输入企业名称', trigger: 'blur' }],
  creditCode: [
    { required: true, message: '请输入统一社会信用代码', trigger: 'blur' },
    { pattern: /^[0-9A-Z]{18}$/, message: '信用代码为18位数字和大写字母', trigger: 'blur' },
  ],
  contactPerson: [{ required: true, message: '请输入联系人姓名', trigger: 'blur' }],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' },
  ],
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await submit(form)
    submitted.value = true
  } catch (e: any) {
    ElMessage.error(e?.message || '提交失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

function goLogin() {
  router.push('/login')
}
</script>

<template>
  <div class="register-wrap">
    <div class="register-left">
      <div class="register-logo">
        <div class="register-logo-icon">🌿</div>
        <div>
          <div class="register-logo-title">能碳审计管理平台</div>
          <div class="register-logo-en">Municipal Industrial Energy Carbon Audit Platform</div>
        </div>
      </div>
      <div class="register-hero">
        <h1>企业注册<br><em>申请入驻</em></h1>
        <p>填写企业基本信息提交注册申请，管理员审核通过后将自动创建企业账号。初始登录密码为统一社会信用代码后6位。</p>
      </div>
      <div class="leaf-tags">
        <span class="leaf-tag">📋 填写信息</span>
        <span class="leaf-tag">⏳ 等待审核</span>
        <span class="leaf-tag">✅ 审核通过</span>
        <span class="leaf-tag">🔑 获取账号</span>
      </div>
    </div>

    <div class="register-right">
      <div class="register-card" v-if="!submitted">
        <div class="card-title">企业注册申请</div>
        <div class="card-sub">请如实填写以下信息，提交后等待管理员审核</div>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="handleSubmit">
          <el-form-item label="企业名称" prop="enterpriseName">
            <el-input v-model="form.enterpriseName" placeholder="请输入企业全称" size="large" />
          </el-form-item>
          <el-form-item label="统一社会信用代码" prop="creditCode">
            <el-input v-model="form.creditCode" placeholder="18位统一社会信用代码" size="large" maxlength="18" />
          </el-form-item>
          <el-form-item label="联系人" prop="contactPerson">
            <el-input v-model="form.contactPerson" placeholder="企业联系人姓名" size="large" />
          </el-form-item>
          <el-form-item label="联系电话" prop="contactPhone">
            <el-input v-model="form.contactPhone" placeholder="联系人手机号码" size="large" maxlength="11" />
          </el-form-item>
          <el-form-item label="联系邮箱（选填）">
            <el-input v-model="form.contactEmail" placeholder="联系人邮箱" size="large" />
          </el-form-item>

          <el-button
            type="primary"
            size="large"
            :loading="loading"
            style="width:100%;margin-top:8px;font-size:15px;font-weight:600;"
            @click="handleSubmit"
          >
            提交注册申请
          </el-button>
        </el-form>

        <div class="card-footer">
          <span>已有账号？</span>
          <a @click.prevent="goLogin" href="#">返回登录</a>
        </div>
      </div>

      <div class="register-card success-card" v-else>
        <div class="success-icon">✅</div>
        <div class="card-title" style="text-align:center">申请已提交</div>
        <div class="card-sub" style="text-align:center;margin-bottom:24px;">
          您的注册申请已成功提交，请等待管理员审核。<br>
          审核通过后，您可使用<strong>统一社会信用代码</strong>作为用户名登录，<br>
          初始密码为信用代码<strong>后6位</strong>。
        </div>
        <el-button type="primary" size="large" style="width:100%;" @click="goLogin">
          返回登录
        </el-button>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
@use '@/styles/variables' as *;

.register-wrap {
  width: 100vw; height: 100vh;
  display: flex;
  background: linear-gradient(160deg, #0d2b26 0%, #1b4a3d 40%, #0a3d2e 100%);
  overflow: hidden;
}

.register-left {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 80px;
}

.register-logo {
  display: flex; align-items: center; gap: 16px;
  margin-bottom: 56px;
  .register-logo-icon {
    width: 52px; height: 52px;
    background: linear-gradient(135deg, #00897B, #43a047);
    border-radius: 14px;
    display: flex; align-items: center; justify-content: center;
    font-size: 26px;
    box-shadow: 0 8px 24px rgba(0,137,123,0.4);
  }
  .register-logo-title { font-size: 21px; color: #fff; font-weight: 700; letter-spacing: 1px; }
  .register-logo-en { font-size: 12px; color: rgba(255,255,255,0.35); margin-top: 3px; }
}

.register-hero {
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
  display: flex; flex-wrap: wrap; gap: 10px;
  .leaf-tag {
    padding: 6px 16px;
    background: rgba(0,137,123,0.15);
    border: 1px solid rgba(0,137,123,0.3);
    border-radius: 20px;
    font-size: 13px; color: #4db6ac;
  }
}

.register-right {
  width: 520px;
  display: flex; align-items: center; justify-content: center;
  background: rgba(255,255,255,0.04);
  border-left: 1px solid rgba(255,255,255,0.07);
  backdrop-filter: blur(12px);
  padding: 40px 0;
  overflow-y: auto;
}

.register-card {
  width: 400px;
  background: #fff;
  border-radius: 20px;
  padding: 40px 36px;
  box-shadow: 0 24px 64px rgba(0,0,0,0.35);

  .card-title { font-size: 22px; font-weight: 700; color: $text-primary; margin-bottom: 6px; }
  .card-sub { font-size: 13px; color: $text-tertiary; margin-bottom: 24px; line-height: 1.6; }
}

.success-card {
  text-align: center;
  .success-icon { font-size: 56px; margin-bottom: 16px; }
}

.card-footer {
  text-align: center; margin-top: 16px;
  font-size: 13px; color: $text-tertiary;
  a { color: $primary; cursor: pointer; margin-left: 4px; }
}

:deep(.el-form-item__label) { font-size: 13px; color: $text-secondary; font-weight: 500; }
:deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 1px #00897B inset !important; }
:deep(.el-form-item) { margin-bottom: 16px; }
</style>
