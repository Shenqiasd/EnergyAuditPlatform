<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import { getDataByType } from '@/api/dict'
import type { DictData } from '@/api/dict'

const props = defineProps<{
  dictType: string
  value: string | number | null | undefined
}>()

const options = ref<DictData[]>([])

async function loadOptions() {
  if (!props.dictType) return
  try {
    options.value = await getDataByType(props.dictType)
  } catch {}
}

const matched = computed(() => {
  const val = String(props.value ?? '')
  return options.value.find(o => o.dictValue === val)
})

const tagType = computed(() => {
  const css = matched.value?.cssClass
  if (css === 'success' || css === 'warning' || css === 'danger' || css === 'info') return css
  return ''
})

onMounted(loadOptions)
watch(() => props.dictType, loadOptions)
</script>

<template>
  <el-tag v-if="matched" :type="(tagType as any)" size="small">{{ matched.dictLabel }}</el-tag>
  <span v-else>{{ value ?? '-' }}</span>
</template>
