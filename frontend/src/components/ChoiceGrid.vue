<template>
  <div class="choice-grid">
    <button
      v-for="option in options"
      :key="option"
      class="choice-item"
      :disabled="disabled"
      @click="emit('select', option)"
    >
      {{ option }}
    </button>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    options: string[]
    disabled?: boolean
  }>(),
  {
    disabled: false,
  },
)

const emit = defineEmits<{
  (event: 'select', option: string): void
}>()
</script>

<style scoped>
.choice-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.choice-item {
  min-height: 64px;
  border: 2px solid var(--wfl-border);
  border-radius: 12px;
  background: #fff;
  font-size: 16px;
  font-weight: 600;
  color: var(--wfl-text);
  cursor: pointer;
  padding: 10px 16px;
  transition: all 0.18s ease;
}

.choice-item:hover:not(:disabled) {
  border-color: var(--wfl-primary);
  color: var(--wfl-primary);
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(79, 70, 229, 0.12);
}

.choice-item:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

@media (max-width: 480px) {
  .choice-grid {
    grid-template-columns: 1fr;
  }
}
</style>

