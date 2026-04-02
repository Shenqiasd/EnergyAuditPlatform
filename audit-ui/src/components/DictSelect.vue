<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { getDataByType } from '@/api/dict'
import type { DictData } from '@/api/dict'

const props = defineProps<{
  dictType: string
  placeholder?: string
  multiple?: boolean
  clearable?: boolean
  disabled?: boolean
  valueKey?: string
}>()

const modelValue = defineModel<string | string[] | number | number[]>()

const loading = ref(false)
const options = ref<DictData[]>([])

async function loadOptions() {
  if (!props.dictType) return
  loading.value = true
  try {
    options.value = await getDataByType(props.dictType)
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

onMounted(loadOptions)
watch(() => props.dictType, loadOptions)
</script>

<template>
  <el-select
    v-model="modelValue"
    :multiple="multiple"
    :clearable="clearable !== false"
    :disabled="disabled"
    :placeholder="placeholder || '请选择'"
    :loading="loading"
    style="width:100%"
  >
    <el-option
      v-for="item in options"
      :key="item.dictValue"
      :label="item.dictLabel"
      :value="valueKey === 'label' ? item.dictLabel : item.dictValue"
    />
  </el-select>
</template>
