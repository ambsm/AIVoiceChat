<template>
  <div class="home-container">
    <!-- 顶部导航 -->
    <nav class="navbar">
      <div class="nav-content">
        <h1 class="logo">🤖 AI角色扮演聊天</h1>
        <div class="nav-actions">
          <el-button @click="showHistoryDialog = true" type="text" class="history-btn">
            <i class="el-icon-time"></i>
            聊天历史
          </el-button>
        </div>
      </div>
    </nav>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <div class="hero-section">
        <h2 class="hero-title">AI语音聊天助手</h2>
        <p class="hero-subtitle">与AI进行纯语音对话，体验自然的语音交互</p>
      </div>

      <!-- 角色选择区域 -->
      <div class="characters-grid">
        <div 
          v-for="character in characters" 
          :key="character.id"
          class="character-card card"
          @click="selectCharacter(character)"
        >
          <div class="character-avatar">
            {{ character.avatar }}
          </div>
          <h3 class="character-name">{{ character.name }}</h3>
          <p class="character-desc">{{ character.description }}</p>
          <div class="character-tags">
            <el-tag 
              v-for="tag in character.tags" 
              :key="tag" 
              size="mini" 
              type="info"
              class="character-tag"
            >
              {{ tag }}
            </el-tag>
          </div>
          <el-button 
            type="primary" 
            class="chat-btn btn-primary"
            @click.stop="startChat(character)"
          >
            开始聊天
          </el-button>
        </div>
      </div>
    </div>

    <!-- 聊天历史对话框 -->
    <el-dialog 
      title="聊天历史" 
      :visible.sync="showHistoryDialog"
      width="600px"
      class="history-dialog"
    >
      <div v-if="historyLoading" class="loading-container">
        <el-loading-spinner></el-loading-spinner>
        <p>加载中...</p>
      </div>
      <div v-else-if="chatHistoryList.length === 0" class="empty-history">
        <i class="el-icon-chat-dot-round"></i>
        <p>暂无聊天历史</p>
      </div>
      <div v-else class="history-list">
        <div 
          v-for="chatId in chatHistoryList" 
          :key="chatId"
          class="history-item"
          @click="viewChatHistory(chatId)"
        >
          <div class="history-info">
            <h4>语音会话 {{ chatId.substring(0, 8) }}</h4>
            <p class="history-time">点击查看详情和播放音频</p>
          </div>
          <el-button size="mini" type="primary">查看</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { chatService } from '@/services/chatService'

export default {
  name: 'Home',
  data() {
    return {
      showHistoryDialog: false,
      historyLoading: false,
      chatHistoryList: [],
      characters: [
        {
          id: 'voice-ai',
          name: 'AI语音助手',
          avatar: '🎤',
          description: '智能语音AI助手，支持流畅的语音对话交互',
          tags: ['语音', 'AI', '智能', '交互']
        }
      ]
    }
  },
  methods: {
    selectCharacter(character) {
      console.log('选择角色:', character)
    },
    
    startChat(character) {
      // 生成新的聊天ID
      const chatId = this.generateChatId()
      this.$router.push({
        name: 'Chat',
        params: { 
          characterId: character.id 
        },
        query: {
          chatId: chatId,
          characterName: character.name,
          voiceOnly: true // 标记为纯语音模式
        }
      })
    },
    
    generateChatId() {
      return 'chat_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
    },
    
    async loadChatHistory() {
      try {
        this.historyLoading = true
        // 根据后端接口文档，获取语音聊天历史列表应该使用 'voice' 类型
        // 接口: GET /ai/history/{type} 其中 type = voice
        this.chatHistoryList = await chatService.getChatHistoryList('voice')
      } catch (error) {
        console.error('加载聊天历史失败:', error)
        this.$message.error('加载聊天历史失败')
        this.chatHistoryList = []
      } finally {
        this.historyLoading = false
      }
    },
    
    viewChatHistory(chatId) {
      // 这里可以跳转到聊天页面显示历史记录
      this.$router.push({
        name: 'Chat',
        params: { characterId: 'history' },
        query: { chatId, viewHistory: true }
      })
      this.showHistoryDialog = false
    }
  },
  
  watch: {
    showHistoryDialog(newVal) {
      if (newVal) {
        this.loadChatHistory()
      }
    }
  }
}
</script>

<style scoped>
.home-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.navbar {
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
  padding: 0 20px;
}

.nav-content {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 70px;
}

.logo {
  color: white;
  font-size: 24px;
  font-weight: 600;
  margin: 0;
}

.history-btn {
  color: white !important;
  font-size: 16px;
}

.history-btn:hover {
  color: #f0f0f0 !important;
}

.main-content {
  padding: 40px 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.hero-section {
  text-align: center;
  margin-bottom: 50px;
}

.hero-title {
  color: white;
  font-size: 36px;
  font-weight: 600;
  margin-bottom: 15px;
}

.hero-subtitle {
  color: rgba(255, 255, 255, 0.8);
  font-size: 18px;
  margin: 0;
}

.characters-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 30px;
  margin-top: 40px;
}

.character-card {
  padding: 30px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.character-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
  border-color: rgba(102, 126, 234, 0.3);
}

.character-avatar {
  font-size: 60px;
  margin-bottom: 20px;
}

.character-name {
  color: #333;
  font-size: 22px;
  font-weight: 600;
  margin-bottom: 15px;
}

.character-desc {
  color: #666;
  font-size: 16px;
  line-height: 1.6;
  margin-bottom: 20px;
}

.character-tags {
  margin-bottom: 25px;
}

.character-tag {
  margin: 0 5px 5px 0;
}

.chat-btn {
  width: 120px;
  height: 40px;
  font-size: 16px;
  font-weight: 500;
}

.history-dialog {
  border-radius: 16px;
}

.loading-container {
  text-align: center;
  padding: 40px;
  color: #666;
}

.loading-container p {
  margin-top: 15px;
  font-size: 16px;
}

.empty-history {
  text-align: center;
  padding: 40px;
  color: #999;
}

.empty-history i {
  font-size: 48px;
  margin-bottom: 15px;
  display: block;
}

.history-list {
  max-height: 400px;
  overflow-y: auto;
}

.history-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  border-bottom: 1px solid #eee;
  cursor: pointer;
  transition: background-color 0.2s;
}

.history-item:hover {
  background-color: #f5f5f5;
}

.history-item:last-child {
  border-bottom: none;
}

.history-info h4 {
  margin: 0 0 5px 0;
  color: #333;
  font-size: 16px;
}

.history-time {
  margin: 0;
  color: #666;
  font-size: 14px;
}
</style>