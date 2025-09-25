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
          <span class="character-avatar" style="width:32px;height:32px;border-radius:50%;overflow:hidden;display:inline-block;">
            <img v-if="currentCharacter.image" :src="currentCharacter.image" :alt="currentCharacter.name" class="header-character-image" style="width:100%;height:100%;object-fit:cover;border-radius:50%;" />
            <span v-else>{{ currentCharacter.avatar }}</span>
          </span>
          <div class="character-details">
            <h3 class="character-name">{{ currentCharacter.name }}</h3>
            <p class="chat-status">{{ chatStatus }}</p>
          </div>
        </div>
      </div>
      <div class="header-right">
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
            <div class="welcome-avatar" :style="{ width: '80px', height: '80px', borderRadius: '50%', overflow: 'hidden' }">
              <img v-if="currentCharacter.image" :src="currentCharacter.image" :alt="currentCharacter.name" class="character-image" :style="{ width: '100%', height: '100%', objectFit: 'cover', borderRadius: '50%' }" />
              <span v-else>{{ currentCharacter.avatar }}</span>
            </div>
            <div class="welcome-text">
              <h4>你好！我是{{ currentCharacter.name }}</h4>
              <p>{{ currentCharacter.description }}</p>
              <p>🎤 请点击下方的录音按钮开始语音对话</p>
            </div>
          </div>
          
          <!-- 语音聊天消息列表 -->
          <div 
            v-for="(message, index) in messages" 
            :key="index"
            :class="['message-item', message.role === 'user' ? 'user-message' : 'ai-message']"
          >
            <div class="message-avatar" :style="{ width: '48px', height: '48px', borderRadius: '50%', overflow: 'hidden' }">
              <span v-if="message.role === 'user'">👤</span>
              <span v-else-if="currentCharacter.image" style="width:48px;height:48px;border-radius:50%;overflow:hidden;display:block;">
                <img :src="currentCharacter.image" :alt="currentCharacter.name" class="message-character-image" style="width:100%;height:100%;object-fit:cover;border-radius:50%;" />
              </span>
              <span v-else>{{ currentCharacter.avatar }}</span>
            </div>
            <div class="message-content">
              <div class="voice-message-bubble">
                <div v-if="message.type === 'voice'" class="voice-controls">
                  <div class="voice-info">
                    <i class="el-icon-microphone"></i>
                    <span>{{ message.role === 'user' ? '你的语音' : 'AI回复' }}</span>
                    <span class="voice-duration">{{ message.duration || '0:00' }}</span>
                  </div>
                  <div class="voice-actions">
                    <el-button 
                      @click="playAudio(message.audioUrl, index)" 
                      :type="playingIndex === index ? 'success' : 'primary'"
                      size="mini"
                      circle
                      :disabled="!message.audioUrl"
                    >
                      <i :class="playingIndex === index ? 'el-icon-video-pause' : 'el-icon-video-play'"></i>
                    </el-button>
                  </div>
                </div>
                <div v-else class="text-message">
                  <StreamingText 
                    :text="message.content"
                    :is-streaming="isLoading && index === messages.length - 1"
                    :speed="30"
                  />
                </div>
                <div class="message-time">{{ formatTime(message.timestamp) }}</div>
              </div>
            </div>
          </div>
          
          <!-- 加载中消息 -->
          <div v-if="isLoading" class="message-item ai-message">
            <div class="message-avatar" :style="{ width: '48px', height: '48px', borderRadius: '50%', overflow: 'hidden' }">
              <span v-if="currentCharacter.image" style="width:48px;height:48px;border-radius:50%;overflow:hidden;display:block;">
                <img :src="currentCharacter.image" :alt="currentCharacter.name" class="message-character-image" style="width:100%;height:100%;object-fit:cover;border-radius:50%;" />
              </span>
              <span v-else>{{ currentCharacter.avatar }}</span>
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
              v-for="(item, index) in chatHistory" 
              :key="index"
              class="history-message-item"
            >
              <div class="history-voice-pair">
                <!-- 用户语音 -->
                <div class="history-voice-controls user-voice">
                  <div class="history-voice-info">
                    <i class="el-icon-microphone"></i>
                    <span>你的语音</span>
                    <span class="history-voice-duration">{{ historyDurations[`user-${chatId}-${index}`] || '0:00' }}</span>
                  </div>
                  <div class="history-voice-actions">
                    <el-button 
                      @click="playAudio(item.userVoice, 'user-' + index)" 
                      :type="playingIndex === 'user-' + index ? 'success' : 'primary'"
                      size="mini"
                      circle
                      :disabled="!item.userVoice"
                    >
                      <i :class="playingIndex === 'user-' + index ? 'el-icon-video-pause' : 'el-icon-video-play'"></i>
                    </el-button>
                  </div>
                </div>
                
                <!-- AI回复语音 -->
                <div class="history-voice-controls ai-voice">
                  <div class="history-voice-info">
                    <i class="el-icon-microphone"></i>
                    <span>AI回复</span>
                    <span class="history-voice-duration">{{ historyDurations[`ai-${chatId}-${index}`] || '0:00' }}</span>
                  </div>
                  <div class="history-voice-actions">
                    <el-button 
                      @click="playAudio(item.agentVoice, 'ai-' + index)" 
                      :type="playingIndex === 'ai-' + index ? 'success' : 'primary'"
                      size="mini"
                      circle
                      :disabled="!item.agentVoice"
                    >
                      <i :class="playingIndex === 'ai-' + index ? 'el-icon-video-pause' : 'el-icon-video-play'"></i>
                    </el-button>
                  </div>
                </div>
              </div>
              <div class="history-divider"></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部语音输入区域 -->
    <div class="voice-input-area">
      <div class="voice-controls">
        <div class="voice-status">
          <span v-if="!isRecording && !audioBlob">🎤 点击录音按钮开始语音对话</span>
          <span v-else-if="isRecording">🔴 正在录音中... {{ recordingTime }}s</span>
          <span v-else-if="audioBlob">✅ 录音完成，点击发送或重新录制</span>
        </div>
        
        <div class="voice-buttons">
          <el-button 
            v-if="!isRecording && !audioBlob"
            @click="startRecording" 
            type="danger" 
            class="record-btn"
            size="large"
            circle
            :disabled="isLoading"
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
      
      // 语音相关
      isRecording: false,
      recordingTime: 0,
      mediaRecorder: null,
      audioBlob: null,
      recordingTimer: null,
      
      // 音频播放
      currentAudio: null,
      playingIndex: -1,
      
      // 历史记录
      showHistoryPanel: false,
      historyLoading: false,
      chatHistory: [],
      // 添加历史记录时长缓存
      historyDurations: {},
      
      // 角色数据
      charactersData: {
        'voice-ai': {
          id: 'voice-ai',
          name: 'AI语音助手',
          avatar: '🎤',
          description: '智能语音AI助手，支持流畅的语音对话交互'
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
      
      // 加载角色信息
      this.loadCharacters()
      
      // 如果是查看历史记录模式
      if (this.$route.query.viewHistory) {
        this.loadChatHistory()
      }
    },
    
    generateChatId() {
      return 'chat_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
    },
    
    focusInput() {
      // 语音模式下不需要焦点输入框
    },
    
    goBack() {
      this.$router.push({ name: 'Home' })
    },
    
    // 语音聊天相关方法
    
    addMessage(role, content) {
      this.messages.push({
        role,
        content,
        type: 'text',
        timestamp: new Date()
      })
      this.scrollToBottom()
    },
    
    addVoiceMessage(role, audioUrl, duration, description) {
      this.messages.push({
        role,
        type: 'voice',
        audioUrl,
        duration,
        content: description || '语音消息',
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
        // 如果已经有音频在播放，切换播放/暂停状态
        if (this.currentAudio) {
          if (this.currentAudio.paused) {
            this.currentAudio.play()
            return
          } else {
            this.currentAudio.pause()
            return
          }
        }
        
        const audioUrl = URL.createObjectURL(this.audioBlob)
        const audio = new Audio(audioUrl)
        // 保存音频对象以便控制播放/暂停
        this.currentAudio = audio
        
        // 监听音频结束事件
        audio.onended = () => {
          this.currentAudio = null
        }
        
        audio.onerror = () => {
          this.$message.error('播放录音失败')
          this.currentAudio = null
        }
        
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
        
        // 添加用户语音消息
        const userMessageIndex = this.messages.length
        this.addVoiceMessage('user', null, null, '你的语音')
        
        // 添加AI消息占位符
        const aiMessageIndex = this.messages.length
        this.addVoiceMessage('assistant', null, null, 'AI回复')
        
        // 调用语音聊天API
        const response = await chatService.sendVoiceMessage(
          this.chatId, 
          audioFile
        )
        
        // 处理后端返回的音频链接
        if (response && typeof response === 'object') {
          // 更新用户语音消息的链接
          if (response.userVoice && this.messages[userMessageIndex]) {
            this.messages[userMessageIndex].audioUrl = response.userVoice
            this.messages[userMessageIndex].duration = await this.getAudioDuration(response.userVoice)
          }
          
          // 更新AI语音消息的链接
          if (response.agentVoice && this.messages[aiMessageIndex]) {
            this.messages[aiMessageIndex].audioUrl = response.agentVoice
            this.messages[aiMessageIndex].duration = await this.getAudioDuration(response.agentVoice)
            
            // 自动播放AI回复
            setTimeout(() => {
              this.playAudio(response.agentVoice, aiMessageIndex)
            }, 500)
          }
        } else {
          // 处理错误情况
          this.messages[aiMessageIndex].content = '抱歉，语音处理失败。'
          this.messages[aiMessageIndex].type = 'text'
        }
        
        this.cancelRecording()
        
      } catch (error) {
        console.error('发送语音消息失败:', error)
        // 更新错误消息
        const aiMessageIndex = this.messages.length - 1
        if (this.messages[aiMessageIndex] && this.messages[aiMessageIndex].role === 'assistant') {
          this.messages[aiMessageIndex].content = '抱歉，语音处理出现问题，请稍后再试。'
          this.messages[aiMessageIndex].type = 'text'
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
    
    // 音频播放相关方法
    async playAudio(audioUrl, messageIndex) {
      if (!audioUrl) return
      
      try {
        // 如果点击的是正在播放的音频，则暂停播放
        if (this.playingIndex === messageIndex) {
          if (this.currentAudio) {
            if (this.currentAudio.paused) {
              // 如果已暂停则继续播放
              await this.currentAudio.play()
            } else {
              // 如果正在播放则暂停
              this.currentAudio.pause()
            }
          }
          return
        }
        
        // 停止当前播放的音频
        if (this.currentAudio) {
          this.currentAudio.pause()
          this.currentAudio = null
          this.playingIndex = -1
        }
        
        // 创建新的音频对象
        this.currentAudio = new Audio(audioUrl)
        this.playingIndex = messageIndex
        
        // 监听音频事件
        this.currentAudio.onended = () => {
          this.playingIndex = -1
          this.currentAudio = null
        }
        
        this.currentAudio.onerror = () => {
          this.$message.error('音频播放失败')
          this.playingIndex = -1
          this.currentAudio = null
        }
        
        // 开始播放
        await this.currentAudio.play()
        
      } catch (error) {
        console.error('播放音频失败:', error)
        this.$message.error('音频播放失败')
        this.playingIndex = -1
        this.currentAudio = null
      }
    },
    
    // 获取音频时长
    async getAudioDuration(audioUrl) {
      return new Promise((resolve) => {
        const audio = new Audio(audioUrl)
        audio.onloadedmetadata = () => {
          const duration = audio.duration
          const minutes = Math.floor(duration / 60)
          const seconds = Math.floor(duration % 60)
          resolve(`${minutes}:${seconds.toString().padStart(2, '0')}`)
        }
        audio.onerror = () => {
          resolve('0:00')
        }
      })
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
        // 使用新的语音历史记录接口
        const history = await chatService.getVoiceChatHistory(this.chatId)
        this.chatHistory = history || []
        
        // 为历史记录面板获取音频时长
        for (let i = 0; i < this.chatHistory.length; i++) {
          const item = this.chatHistory[i]
          // 生成唯一标识符
          const userKey = `user-${this.chatId}-${i}`
          const aiKey = `ai-${this.chatId}-${i}`
          
          // 获取用户语音时长
          if (item.userVoice && !this.historyDurations[userKey]) {
            this.historyDurations[userKey] = await this.getAudioDuration(item.userVoice)
          }
          
          // 获取AI语音时长
          if (item.agentVoice && !this.historyDurations[aiKey]) {
            this.historyDurations[aiKey] = await this.getAudioDuration(item.agentVoice)
          }
        }
        
        // 如果在查看历史模式，将历史消息加载到当前消息列表
        if (this.$route.query.viewHistory) {
          // 清空当前消息
          this.messages = []
          
          // 将历史记录转换为消息格式
          for (const item of this.chatHistory) {
            // 添加用户语音消息
            if (item.userVoice) {
              const duration = await this.getAudioDuration(item.userVoice)
              this.addVoiceMessage('user', item.userVoice, duration, '你的语音')
            }
            
            // 添加AI语音消息
            if (item.agentVoice) {
              const duration = await this.getAudioDuration(item.agentVoice)
              this.addVoiceMessage('assistant', item.agentVoice, duration, 'AI回复')
            }
          }
          
          this.scrollToBottom()
        }
        
      } catch (error) {
        console.error('加载聊天历史失败:', error)
        this.chatHistory = []
        this.$message.error('加载聊天历史失败')
      } finally {
        this.historyLoading = false
      }
    },
    
    async loadCharacters() {
      try {
        // 从后端获取角色分页数据
        const characterData = await chatService.getCharacterPage(1, 100)
        console.log('获取到的角色数据:', characterData)
        
        // 处理角色数据
        if (characterData && Array.isArray(characterData.records)) {
          // 将角色数据转换为以id为键的对象
          const charactersMap = {}
          characterData.records.forEach(character => {
            charactersMap[character.id] = {
              id: character.id,
              name: character.name || '未知角色',
              avatar: character.avatar || '🤖',
              image: character.image, // 添加image字段
              description: character.description || '暂无描述'
            }
          })
          this.charactersData = { ...this.charactersData, ...charactersMap }
        }
      } catch (error) {
        console.error('加载角色信息失败:', error)
        // 出错时不影响现有角色数据
      }
    }
  },
  
  beforeDestroy() {
    // 清理录音相关资源
    if (this.isRecording) {
      this.stopRecording()
    }
    clearInterval(this.recordingTimer)
    
    // 清理音频播放资源
    if (this.currentAudio) {
      this.currentAudio.pause()
      this.currentAudio = null
    }
  }
}
</script>

<style scoped>
.chat-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f5; /* 与 Home 一致的浅灰背景 */
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

.header-character-image {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
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
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;
  display: flex;
  justify-content: center;
  align-items: center;
}

.welcome-avatar .character-image {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  object-position: center;
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
  width: 48px; /* 固定消息头像大小 */
  height: 48px;
  border-radius: 50%;
  overflow: hidden;
  -webkit-clip-path: circle(50% at 50% 50%);
  clip-path: circle(50% at 50% 50%);
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 28px;
  margin-right: 12px;
  align-self: flex-end;
}

/* 确保嵌套的 span/img 继承容器尺寸，避免原图撑大 */
.message-avatar > span { display: block; width: 100%; height: 100%; border-radius: 50%; }
.message-avatar img { display: block; width: 100%; height: 100%; object-fit: cover; border-radius: 50%; }

/* 提升优先级，强制圆形（适配 scoped 环境与第三方样式） */
::v-deep .message-avatar,
::v-deep .message-avatar > span,
::v-deep .message-avatar img {
  border-radius: 50% !important;
}

/* 用户消息的头像应该显示在右侧 */
.user-message .message-avatar {
  order: 2;
  margin-left: 10px;
  margin-right: 0;
}

/* AI消息的头像保持在左侧 */
.ai-message .message-avatar {
  order: 1;
}

.user-message .message-content {
  order: 1;
}

.ai-message .message-content {
  order: 2;
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
  background: #667eea; /* 去除渐变，使用纯色 */
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

.history-message-item {
  margin-bottom: 15px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 8px;
}

.history-voice-pair {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.history-voice-controls {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-radius: 6px;
}

.user-voice {
  background: #e3f2fd;
}

.ai-voice {
  background: #f5f5f5;
}

.history-voice-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.history-voice-duration {
  font-size: 12px;
  opacity: 0.7;
  margin-left: auto;
}

.history-voice-actions {
  margin-left: 15px;
}

.voice-message-bubble {
  padding: 16px;
  border-radius: 16px;
  position: relative;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.user-message .voice-message-bubble {
  background: #667eea; /* 去除渐变，使用纯色 */
  color: white;
  border-bottom-right-radius: 4px;
}

.ai-message .voice-message-bubble {
  background: white;
  color: #333;
  border-bottom-left-radius: 4px;
}

.voice-controls {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.voice-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.voice-duration {
  font-size: 12px;
  opacity: 0.7;
  margin-left: auto;
}

.voice-actions {
  margin-left: 15px;
}

.text-message {
  font-size: 16px;
  line-height: 1.5;
}

.voice-input-area {
  background: white;
  border-top: 1px solid #e0e0e0;
  padding: 30px 20px;
  position: relative;
}

.voice-input-area::before {
  content: '';
  position: absolute;
  top: -20px;
  left: 50%;
  transform: translateX(-50%);
  width: 60px;
  height: 4px;
  background: linear-gradient(45deg, #667eea, #764ba2);
  border-radius: 2px;
}

.voice-controls {
  max-width: 800px;
  margin: 0 auto;
  text-align: center;
}

.voice-status {
  margin-bottom: 25px;
  font-size: 16px;
  color: #666;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 500;
}

.voice-buttons {
  display: flex;
  justify-content: center;
  gap: 20px;
  align-items: center;
  flex-wrap: wrap;
}

.record-btn, .stop-btn, .play-btn {
  width: 80px;
  height: 80px;
  font-size: 32px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  transition: all 0.3s ease;
}

.record-btn {
  background: #ff4757; /* 纯色 */
  border-color: #ff4757;
  animation: pulse 2s infinite;
}

.record-btn:hover, .record-btn:focus {
  background: #ff3838;
  transform: scale(1.05);
}

.stop-btn {
  background: #2ed573; /* 纯色 */
  border-color: #2ed573;
  animation: recording-pulse 1s infinite;
}

.play-btn {
  background: #5352ed; /* 纯色 */
  border-color: #5352ed;
}

.send-voice-btn, .cancel-btn {
  height: 50px;
  padding: 0 25px;
  border-radius: 25px;
  font-size: 16px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.send-voice-btn {
  background: #667eea; /* 纯色 */
  border-color: #667eea;
}

.send-voice-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 25px rgba(102, 126, 234, 0.4);
}

@keyframes pulse {
  0% {
    box-shadow: 0 4px 20px rgba(255, 71, 87, 0.3);
  }
  50% {
    box-shadow: 0 4px 30px rgba(255, 71, 87, 0.6);
  }
  100% {
    box-shadow: 0 4px 20px rgba(255, 71, 87, 0.3);
  }
}

@keyframes recording-pulse {
  0% {
    box-shadow: 0 4px 20px rgba(46, 213, 115, 0.4);
  }
  50% {
    box-shadow: 0 4px 30px rgba(46, 213, 115, 0.7);
  }
  100% {
    box-shadow: 0 4px 20px rgba(46, 213, 115, 0.4);
  }
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

.message-character-image {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  object-position: center;
}

.welcome-avatar .character-image {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  object-position: center;
}

.header-character-image {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  object-position: center;
}

/* 覆盖可能来自其他页面的渐变背景 */
:root, html, body, #app, .chat-container, .chat-main, .chat-messages {
  background: #f5f5f5 !important;
}

</style>

<style>
/* 非 scoped 全局兜底，确保移除任何外部渐变背景 */
html, body, #app {
  background: #f5f5f5 !important;
}
</style>