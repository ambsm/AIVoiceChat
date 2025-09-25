<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script>
export default {
  name: 'App'
}
</script>

<style>
/* 全局兜底：统一移除 body 上的渐变背景 */
html, body, #app {
  background: #f5f5f5 !important;
  min-height: 100vh;
}
</style>