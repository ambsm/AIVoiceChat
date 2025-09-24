<template>
  <span class="streaming-text-container">
    <span v-if="isStreaming" class="streaming-text">
      {{ displayText }}<span class="typing-cursor">|</span>
    </span>
    <span v-else>{{ text }}</span>
  </span>
</template>

<script>
export default {
  name: 'StreamingText',
  props: {
    text: {
      type: String,
      default: ''
    },
    isStreaming: {
      type: Boolean,
      default: false
    },
    speed: {
      type: Number,
      default: 50 // 打字速度（毫秒）
    }
  },
  data() {
    return {
      displayText: '',
      currentIndex: 0,
      typeTimer: null
    }
  },
  watch: {
    text: {
      handler(newText) {
        if (this.isStreaming) {
          // 流式模式下直接显示文本
          this.displayText = newText
        } else {
          // 非流式模式下使用打字效果
          this.startTyping(newText)
        }
      },
      immediate: true
    },
    isStreaming(newVal) {
      if (!newVal && this.text) {
        // 停止流式后开始打字效果
        this.startTyping(this.text)
      }
    }
  },
  beforeDestroy() {
    if (this.typeTimer) {
      clearInterval(this.typeTimer)
    }
  },
  methods: {
    startTyping(text) {
      this.displayText = ''
      this.currentIndex = 0
      
      if (this.typeTimer) {
        clearInterval(this.typeTimer)
      }
      
      this.typeTimer = setInterval(() => {
        if (this.currentIndex < text.length) {
          this.displayText += text[this.currentIndex]
          this.currentIndex++
        } else {
          clearInterval(this.typeTimer)
          this.typeTimer = null
        }
      }, this.speed)
    }
  }
}
</script>

<style scoped>
.streaming-text-container {
  position: relative;
  display: inline;
}

.streaming-text {
  position: relative;
}

.typing-cursor {
  animation: blink 1s infinite;
  color: #667eea;
  font-weight: bold;
  margin-left: 2px;
}

@keyframes blink {
  0%, 50% {
    opacity: 1;
  }
  51%, 100% {
    opacity: 0;
  }
}
</style>