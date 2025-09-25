import Vue from 'vue'
import App from './App.vue'
import router from './router'
import { chatService } from './services/chatService'
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import '@/assets/styles/common.css'

Vue.config.productionTip = false

Vue.use(ElementUI)

new Vue({
  router,
  render: h => h(App)
}).$mount('#app')

// 挂载到全局，便于在各处调用
Vue.prototype.$services = { chat: chatService }