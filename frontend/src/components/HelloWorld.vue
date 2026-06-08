<template>
  <div class="hello-world">
    <el-button type="primary">{{ msg }}</el-button>
  </div>
</template>

<script setup>
defineProps({
  msg: {
    type: String,
    default: ''
  }
})
</script>

<style scoped>
.hello-world {
  margin-top: 20px;
}
</style>
