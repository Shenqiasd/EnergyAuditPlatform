<script setup lang="ts">
const props = defineProps<{
  templateId?: number | string
  versionId?: number | string
  readonly?: boolean
}>()

const emit = defineEmits(['save', 'close'])
</script>

<template>
  <div class="spread-designer-placeholder">
    <el-empty description="SpreadJS 设计器加载中...">
      <template #image>
        <el-icon :size="60"><Document /></el-icon>
      </template>
    </el-empty>
  </div>
</template>

<style scoped lang="scss">
.spread-designer-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
}
</style>
