<template>
  <div class="chat-container">
    <!-- 顶部导航栏 -->
    <div class="chat-header">
      <div class="header-left">
        <el-button 
          @click="goBack" 
          type="text" 
          class="back-btn"
          icon="el-icon-arrow-left"
        >
          返回
        </el-button>
        <div class="character-info">
          <span class="character-avatar">{{ currentCharacter.avatar }}</span>
          <div class="character-details">
            <h3 class="character-name">{{ currentCharacter.name }}</h3>
            <p class="chat-status">{{ chatStatus }}</p>
          </div>
        </div>
      </div>
      <div class="header-right">
        <el-button 
          @click="toggleVoiceMode" 
          :type="isVoiceMode ? 'primary' : 'default'"
          size="small"
          class="voice-toggle-btn"
        >
          <i :class="isVoiceMode ? 'el-icon-microphone' : 'el-icon-chat-dot-round'"></i>
          {{ isVoiceMode ? '语音模式' : '文字模式' }}
        </el-button>
        <el-button 
          @click="showHistoryPanel = !showHistoryPanel" 
          type="default"
          size="small"
          class="history-toggle-btn"
        >
          <i class="el-icon-time"></i>
          历史记录
        </el-button>
      </div>
    </div>

    <div class="chat-main">
      <!-- 聊天消息区域 -->
      <div class="chat-messages" ref="messagesContainer">
        <div class="messages-wrapper">
          <!-- 欢迎消息 -->
          <div v-if="messages.length === 0" class="welcome-message">
            <div class="welcome-avatar">{{ currentCharacter.avatar }}</div>
            <div class="welcome-text">
              <h4>你好！我是{{ currentCharacter.name }}</h4>
              <p>{{ currentCharacter.description }}</p>
              <p>你想和我聊什么呢？</p>
            </div>
          </div>
          
          <!-- 聊天消息列表 -->
          <div 
            v-for="(message, index) in messages" 
            :key="index"
            :class="['message-item', message.role === 'user' ? 'user-message' : 'ai-message']"
          >
            <div class="message-avatar">
              <span v-if="message.role === 'user'">👤</span>
              <span v-else>{{ currentCharacter.avatar }}</span>
            </div>
            <div class="message-content">
              <div class="message-bubble">
                <div class="message-text">
                  <StreamingText 
                    v-if="message.role === 'assistant'"
                    :text="message.content"
                    :is-streaming="isLoading && index === messages.length - 1"
                    :speed="30"
                  />
                  <span v-else>{{ message.content }}</span>
                </div>
                <div class="message-time">{{ formatTime(message.timestamp) }}</div>
              </div>
            </div>
          </div>
          
          <!-- 加载中消息 -->
          <div v-if="isLoading" class="message-item ai-message">
            <div class="message-avatar">
              <span>{{ currentCharacter.avatar }}</span>
            </div>
            <div class="message-content">
              <div class="message-bubble loading-bubble">
                <div class="typing-animation">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 侧边历史记录面板 -->
      <div v-show="showHistoryPanel" class="history-panel">
        <div class="history-header">
          <h4>聊天历史</h4>
          <el-button @click="showHistoryPanel = false" type="text" icon="el-icon-close"></el-button>
        </div>
        <div class="history-content">
          <div v-if="historyLoading" class="loading-text">加载中...</div>
          <div v-else-if="chatHistory.length === 0" class="empty-text">暂无历史记录</div>
          <div v-else class="history-messages">
            <div 
              v-for="(msg, index) in chatHistory" 
              :key="index"
              :class="['history-message', msg.role === 'user' ? 'user' : 'ai']"
            >
              <div class="history-content-text">{{ msg.content }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部输入区域 -->
    <div class="chat-input-area">
      <!-- 文字输入模式 -->
      <div v-if="!isVoiceMode" class="text-input-container">
        <div class="input-wrapper">
          <el-input
            v-model="inputMessage"
            type="textarea"
            :rows="1"
            resize="none"
            placeholder="输入你想说的话..."
            @keydown.enter.prevent="handleEnterKey"
            class="message-input"
            ref="messageInput"
          ></el-input>
          <div class="input-actions">
            <el-button 
              @click="sendMessage" 
              type="primary" 
              :disabled="!inputMessage.trim() || isLoading"
              class="send-btn"
            >
              <i class="el-icon-s-promotion"></i>
              发送
            </el-button>
          </div>
        </div>
      </div>

      <!-- 语音输入模式 -->
      <div v-else class="voice-input-container">
        <div class="voice-controls">
          <div class="voice-status">
            <span v-if="!isRecording && !audioBlob">点击按钮开始录音</span>
            <span v-else-if="isRecording">正在录音中... {{ recordingTime }}s</span>
            <span v-else-if="audioBlob">录音完成，点击发送</span>
          </div>
          
          <div class="voice-buttons">
            <el-button 
              v-if="!isRecording && !audioBlob"
              @click="startRecording" 
              type="danger" 
              class="record-btn"
              size="large"
              circle
            >
              <i class="el-icon-microphone"></i>
            </el-button>
            
            <template v-else-if="isRecording">
              <el-button 
                @click="stopRecording" 
                type="success" 
                class="stop-btn"
                size="large"
                circle
              >
                <i class="el-icon-video-pause"></i>
              </el-button>
            </template>
            
            <template v-else-if="audioBlob">
              <el-button 
                @click="playRecording" 
                type="info" 
                class="play-btn"
                size="large"
                circle
              >
                <i class="el-icon-video-play"></i>
              </el-button>
              
              <el-button 
                @click="sendVoiceMessage" 
                type="primary" 
                class="send-voice-btn"
                size="large"
                :disabled="isLoading"
              >
                <i class="el-icon-s-promotion"></i>
                发送语音
              </el-button>
              
              <el-button 
                @click="cancelRecording" 
                type="default" 
                class="cancel-btn"
                size="large"
              >
                <i class="el-icon-delete"></i>
                重录
              </el-button>
            </template>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { chatService } from '@/services/chatService'
import StreamingText from '@/components/StreamingText.vue'

export default {
  name: 'Chat',
  components: {
    StreamingText
  },
  props: {
    characterId: {
      type: String,
      required: true
    }
  },
  data() {
    return {
      // 聊天状态
      chatId: '',
      isLoading: false,
      chatStatus: '在线',
      
      // 消息相关
      messages: [],
      inputMessage: '',
      
      // 语音相关
      isVoiceMode: false,
      isRecording: false,
      recordingTime: 0,
      mediaRecorder: null,
      audioBlob: null,
      recordingTimer: null,
      
      // 历史记录
      showHistoryPanel: false,
      historyLoading: false,
      chatHistory: [],
      
      // 角色数据
      charactersData: {
        'character-a': {
          id: 'character-a',
          name: '角色A - 智慧导师',
          avatar: '🧙‍♂️',
          description: '拥有丰富知识的智慧导师，可以为你答疑解惑，提供人生指导'
        },
        'character-b': {
          id: 'character-b',
          name: '角色B - 活力伙伴', 
          avatar: '🌟',
          description: '充满活力的年轻伙伴，喜欢聊天、分享生活中的趣事'
        },
        'character-c': {
          id: 'character-c',
          name: '角色C - 专业助手',
          avatar: '👔', 
          description: '专业的工作助手，帮助你解决工作中的问题，提供专业建议'
        }
      }
    }
  },
  
  computed: {
    currentCharacter() {
      return this.charactersData[this.characterId] || {
        id: 'default',
        name: '未知角色',
        avatar: '🤖',
        description: '这是一个神秘的AI角色'
      }
    }
  },
  
  mounted() {
    this.initializeChat()
    this.focusInput()
  },
  
  methods: {
    initializeChat() {
      // 从路由参数获取chatId
      this.chatId = this.$route.query.chatId || this.generateChatId()
      
      // 如果是查看历史记录模式
      if (this.$route.query.viewHistory) {
        this.loadChatHistory()
      }
    },
    
    generateChatId() {
      return 'chat_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
    },
    
    focusInput() {
      this.$nextTick(() => {
        if (this.$refs.messageInput) {
          this.$refs.messageInput.focus()
        }
      })
    },
    
    goBack() {
      this.$router.push({ name: 'Home' })
    },
    
    // 文本聊天相关方法
    handleEnterKey(event) {
      if (!event.shiftKey) {
        this.sendMessage()
      }
    },
    
    async sendMessage() {
      if (!this.inputMessage.trim() || this.isLoading) return
      
      const message = this.inputMessage.trim()
      this.inputMessage = ''
      
      // 添加用户消息
      this.addMessage('user', message)
      
      // 添加AI消息占位符，用于实时更新
      const aiMessageIndex = this.messages.length
      this.addMessage('assistant', '正在思考...')
      
      try {
        this.isLoading = true
        this.chatStatus = '思考中...'
        
        // 调用流式聊天API
        await chatService.sendMessage(
          message, 
          this.chatId, 
          null,
          (chunk, fullResponse) => {
            // 实时更新AI消息内容
            if (this.messages[aiMessageIndex]) {
              this.messages[aiMessageIndex].content = fullResponse
              this.chatStatus = `正在回复... (${fullResponse.length} 字)`
              this.scrollToBottom()
            }
          }
        )
        
      } catch (error) {
        console.error('发送消息失败:', error)
        // 更新错误消息
        if (this.messages[aiMessageIndex]) {
          this.messages[aiMessageIndex].content = '抱歉，网络连接出现问题，请稍后再试。'
        }
        this.$message.error('发送消息失败')
      } finally {
        this.isLoading = false
        this.chatStatus = '在线'
        this.focusInput()
      }
    },
    
    addMessage(role, content) {
      this.messages.push({
        role,
        content,
        timestamp: new Date()
      })
      this.scrollToBottom()
    },
    
    scrollToBottom() {
      this.$nextTick(() => {
        const container = this.$refs.messagesContainer
        if (container) {
          container.scrollTop = container.scrollHeight
        }
      })
    },
    
    formatTime(timestamp) {
      return timestamp.toLocaleTimeString('zh-CN', { 
        hour: '2-digit', 
        minute: '2-digit' 
      })
    },
    
    // 语音聊天相关方法
    toggleVoiceMode() {
      this.isVoiceMode = !this.isVoiceMode
      if (!this.isVoiceMode) {
        this.cancelRecording()
        this.focusInput()
      }
    },
    
    async startRecording() {
      try {
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
        this.mediaRecorder = new MediaRecorder(stream)
        
        const audioChunks = []
        
        this.mediaRecorder.ondataavailable = (event) => {
          audioChunks.push(event.data)
        }
        
        this.mediaRecorder.onstop = () => {
          this.audioBlob = new Blob(audioChunks, { type: 'audio/wav' })
          stream.getTracks().forEach(track => track.stop())
        }
        
        this.mediaRecorder.start()
        this.isRecording = true
        this.recordingTime = 0
        
        // 开始计时
        this.recordingTimer = setInterval(() => {
          this.recordingTime++
          if (this.recordingTime >= 60) { // 最大录音60秒
            this.stopRecording()
          }
        }, 1000)
        
      } catch (error) {
        console.error('启动录音失败:', error)
        this.$message.error('无法访问麦克风，请检查权限设置')
      }
    },
    
    stopRecording() {
      if (this.mediaRecorder && this.isRecording) {
        this.mediaRecorder.stop()
        this.isRecording = false
        clearInterval(this.recordingTimer)
      }
    },
    
    playRecording() {
      if (this.audioBlob) {
        const audioUrl = URL.createObjectURL(this.audioBlob)
        const audio = new Audio(audioUrl)
        audio.play()
      }
    },
    
    async sendVoiceMessage() {
      if (!this.audioBlob || this.isLoading) return
      
      try {
        this.isLoading = true
        this.chatStatus = '处理语音中...'
        
        // 创建音频文件，自动处理为标准格式
        const audioFile = await this.processAudioFile(this.audioBlob)
        
        // 添加用户语音消息提示
        this.addMessage('user', '🎤 发送了一段语音')
        
        // 添加AI消息占位符，用于实时更新
        const aiMessageIndex = this.messages.length
        this.addMessage('assistant', '正在识别语音...')
        
        // 调用流式语音聊天API
        await chatService.sendVoiceMessage(
          this.chatId, 
          audioFile, 
          null,
          (chunk, fullResponse) => {
            // 实时更新AI消息内容
            if (this.messages[aiMessageIndex]) {
              this.messages[aiMessageIndex].content = fullResponse
              this.chatStatus = `正在回复... (${fullResponse.length} 字)`
              this.scrollToBottom()
            }
          }
        )
        
        this.cancelRecording()
        
      } catch (error) {
        console.error('发送语音消息失败:', error)
        // 更新错误消息
        const aiMessageIndex = this.messages.length - 1
        if (this.messages[aiMessageIndex] && this.messages[aiMessageIndex].role === 'assistant') {
          this.messages[aiMessageIndex].content = '抱歉，语音处理出现问题，请稍后再试。'
        } else {
          this.addMessage('assistant', '抱歉，语音处理出现问题，请稍后再试。')
        }
        this.$message.error('发送语音消息失败')
      } finally {
        this.isLoading = false
        this.chatStatus = '在线'
      }
    },
    
    // 音频文件处理 - 自动转换为标准格式
    async processAudioFile(audioBlob) {
      try {
        // 创建标准的WAV文件，自动处理多声道问题
        const audioContext = new (window.AudioContext || window.webkitAudioContext)()
        const arrayBuffer = await audioBlob.arrayBuffer()
        const audioBuffer = await audioContext.decodeAudioData(arrayBuffer)
        
        // 将多声道音频混合为单声道（自动处理）
        const sampleRate = audioBuffer.sampleRate
        const channels = audioBuffer.numberOfChannels
        const length = audioBuffer.length
        
        // 创建单声道缓冲区
        const monoBuffer = new Float32Array(length)
        
        if (channels === 1) {
          // 已经是单声道，直接复制
          monoBuffer.set(audioBuffer.getChannelData(0))
        } else {
          // 多声道混合为单声道
          for (let i = 0; i < length; i++) {
            let sample = 0
            for (let channel = 0; channel < channels; channel++) {
              sample += audioBuffer.getChannelData(channel)[i]
            }
            monoBuffer[i] = sample / channels
          }
        }
        
        // 创建 WAV 文件
        const wavBuffer = this.createWAVFile(monoBuffer, sampleRate)
        const processedBlob = new Blob([wavBuffer], { type: 'audio/wav' })
        
        return new File([processedBlob], 'voice.wav', { type: 'audio/wav' })
        
      } catch (error) {
        console.warn('音频处理失败，使用原始文件:', error)
        // 如果处理失败，返回原始文件
        return new File([audioBlob], 'voice.wav', { type: 'audio/wav' })
      }
    },
    
    // 创建 WAV 文件缓冲区
    createWAVFile(audioData, sampleRate) {
      const length = audioData.length
      const buffer = new ArrayBuffer(44 + length * 2)
      const view = new DataView(buffer)
      
      // WAV 文件头
      const writeString = (offset, string) => {
        for (let i = 0; i < string.length; i++) {
          view.setUint8(offset + i, string.charCodeAt(i))
        }
      }
      
      // RIFF identifier
      writeString(0, 'RIFF')
      // RIFF chunk length
      view.setUint32(4, 36 + length * 2, true)
      // RIFF type
      writeString(8, 'WAVE')
      // format chunk identifier
      writeString(12, 'fmt ')
      // format chunk length
      view.setUint32(16, 16, true)
      // sample format (raw)
      view.setUint16(20, 1, true)
      // channel count
      view.setUint16(22, 1, true)
      // sample rate
      view.setUint32(24, sampleRate, true)
      // byte rate (sample rate * block align)
      view.setUint32(28, sampleRate * 2, true)
      // block align (channel count * bytes per sample)
      view.setUint16(32, 2, true)
      // bits per sample
      view.setUint16(34, 16, true)
      // data chunk identifier
      writeString(36, 'data')
      // data chunk length
      view.setUint32(40, length * 2, true)
      
      // 写入音频数据
      let offset = 44
      for (let i = 0; i < length; i++, offset += 2) {
        const sample = Math.max(-1, Math.min(1, audioData[i]))
        view.setInt16(offset, sample < 0 ? sample * 0x8000 : sample * 0x7FFF, true)
      }
      
      return buffer
    },
    
    cancelRecording() {
      if (this.isRecording) {
        this.stopRecording()
      }
      this.audioBlob = null
      this.recordingTime = 0
      clearInterval(this.recordingTimer)
    },
    
    // 历史记录相关方法
    async loadChatHistory() {
      try {
        this.historyLoading = true
        const history = await chatService.getChatHistory('chat', this.chatId)
        this.chatHistory = history || []
        
        // 如果在查看历史模式，将历史消息加载到当前消息列表
        if (this.$route.query.viewHistory) {
          this.messages = this.chatHistory.map(msg => ({
            ...msg,
            timestamp: new Date()
          }))
          this.scrollToBottom()
        }
        
      } catch (error) {
        console.error('加载聊天历史失败:', error)
        this.chatHistory = []
      } finally {
        this.historyLoading = false
      }
    }
  },
  
  beforeDestroy() {
    // 清理录音相关资源
    if (this.isRecording) {
      this.stopRecording()
    }
    clearInterval(this.recordingTimer)
  }
}
</script>

<style scoped>
.chat-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background: white;
  border-bottom: 1px solid #e0e0e0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.header-left {
  display: flex;
  align-items: center;
}

.back-btn {
  color: #666 !important;
  font-size: 16px;
  margin-right: 15px;
}

.character-info {
  display: flex;
  align-items: center;
}

.character-avatar {
  font-size: 32px;
  margin-right: 12px;
}

.character-details h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.chat-status {
  margin: 2px 0 0 0;
  font-size: 12px;
  color: #999;
}

.header-right {
  display: flex;
  gap: 10px;
}

.voice-toggle-btn, .history-toggle-btn {
  height: 36px;
}

.chat-main {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f9f9f9;
}

.messages-wrapper {
  max-width: 800px;
  margin: 0 auto;
}

.welcome-message {
  display: flex;
  margin-bottom: 30px;
  justify-content: center;
}

.welcome-avatar {
  font-size: 48px;
  margin-right: 20px;
}

.welcome-text {
  background: white;
  padding: 20px;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  max-width: 400px;
}

.welcome-text h4 {
  margin: 0 0 10px 0;
  color: #333;
  font-size: 18px;
}

.welcome-text p {
  margin: 8px 0;
  color: #666;
  line-height: 1.5;
}

.message-item {
  display: flex;
  margin-bottom: 20px;
}

.user-message {
  justify-content: flex-end;
}

.ai-message {
  justify-content: flex-start;
}

.message-avatar {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  margin: 0 10px;
}

.message-content {
  max-width: 70%;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 16px;
  position: relative;
}

.user-message .message-bubble {
  background: linear-gradient(45deg, #667eea, #764ba2);
  color: white;
  border-bottom-right-radius: 4px;
}

.ai-message .message-bubble {
  background: white;
  color: #333;
  border-bottom-left-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.loading-bubble {
  background: white !important;
  color: #333 !important;
}

.message-text {
  font-size: 16px;
  line-height: 1.5;
  word-wrap: break-word;
}

.streaming-indicator {
  color: #999;
  font-style: italic;
}

.streaming-text {
  position: relative;
}

.typing-cursor {
  animation: blink 1s infinite;
  color: #667eea;
  font-weight: bold;
}

@keyframes blink {
  0%, 50% {
    opacity: 1;
  }
  51%, 100% {
    opacity: 0;
  }
}

.message-time {
  font-size: 12px;
  opacity: 0.7;
  margin-top: 5px;
}

.typing-animation {
  display: flex;
  gap: 4px;
  padding: 8px 0;
}

.typing-animation span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #999;
  animation: typing 1.4s infinite;
}

.typing-animation span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-animation span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 60%, 100% {
    transform: translateY(0);
  }
  30% {
    transform: translateY(-10px);
  }
}

.history-panel {
  width: 300px;
  background: white;
  border-left: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  border-bottom: 1px solid #e0e0e0;
}

.history-header h4 {
  margin: 0;
  color: #333;
}

.history-content {
  flex: 1;
  overflow-y: auto;
  padding: 15px;
}

.loading-text, .empty-text {
  text-align: center;
  color: #999;
  padding: 20px;
}

.history-messages {
  max-height: 100%;
}

.history-message {
  margin-bottom: 10px;
  padding: 8px 12px;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.4;
}

.history-message.user {
  background: #e3f2fd;
  text-align: right;
}

.history-message.ai {
  background: #f5f5f5;
}

.history-content-text {
  word-wrap: break-word;
}

.chat-input-area {
  background: white;
  border-top: 1px solid #e0e0e0;
  padding: 20px;
}

.text-input-container {
  max-width: 800px;
  margin: 0 auto;
}

.input-wrapper {
  display: flex;
  gap: 15px;
  align-items: flex-end;
}

.message-input {
  flex: 1;
}

.message-input textarea {
  border-radius: 12px;
  border: 2px solid #e0e0e0;
  padding: 12px 16px;
  font-size: 16px;
  resize: none;
  transition: border-color 0.3s;
}

.message-input textarea:focus {
  border-color: #667eea;
}

.send-btn {
  height: 45px;
  padding: 0 20px;
  border-radius: 12px;
  font-size: 16px;
}

.voice-input-container {
  max-width: 800px;
  margin: 0 auto;
  text-align: center;
}

.voice-status {
  margin-bottom: 20px;
  font-size: 16px;
  color: #666;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.voice-buttons {
  display: flex;
  justify-content: center;
  gap: 15px;
  align-items: center;
}

.record-btn, .stop-btn, .play-btn {
  width: 60px;
  height: 60px;
  font-size: 24px;
}

.record-btn {
  background: #ff4757;
  border-color: #ff4757;
}

.record-btn:hover {
  background: #ff3838;
}

.stop-btn {
  background: #2ed573;
  border-color: #2ed573;
}

.play-btn {
  background: #5352ed;
  border-color: #5352ed;
}

.send-voice-btn {
  height: 45px;
  padding: 0 20px;
  border-radius: 12px;
  font-size: 16px;
}

.cancel-btn {
  height: 45px;
  padding: 0 20px;
  border-radius: 12px;
  font-size: 16px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .chat-header {
    padding: 10px 15px;
  }
  
  .character-name {
    font-size: 16px;
  }
  
  .chat-messages {
    padding: 15px;
  }
  
  .message-content {
    max-width: 85%;
  }
  
  .history-panel {
    width: 250px;
  }
  
  .chat-input-area {
    padding: 15px;
  }
  
  .voice-buttons {
    flex-wrap: wrap;
    gap: 10px;
  }
}
</style>