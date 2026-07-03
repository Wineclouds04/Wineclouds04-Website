<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

import AdminShell from './components/AdminShell.vue'

const route = useRoute()
const usesAdminShell = computed(() => Boolean(route.meta.requiresAuth))
</script>

<template>
  <AdminShell v-if="usesAdminShell">
    <RouterView />
  </AdminShell>
  <RouterView v-else />
</template>
