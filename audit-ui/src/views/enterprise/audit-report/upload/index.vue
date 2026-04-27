<script setup lang="ts">
</script>

<template>
  <div class="page-container">
    <el-card shadow="never" style="max-width: 600px; width: 100%">
      <template #header>
        <span style="font-size: 15px; font-weight: 600">上传报告</span>
      </template>
      <el-empty description="报告上传功能即将上线，敬请期待">
        <template #image>
          <span style="font-size: 48px">📤</span>
        </template>
      </el-empty>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.page-container {
  padding: 40px 20px;
  display: flex;
  justify-content: center;
}
</style>
