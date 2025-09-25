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
      <div
        class="characters-grid"
        :style="{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '80px 200px' }"
      >
        <CharacterCard
          v-for="character in characters"
          :key="character.id"
          :character="character"
          @select="selectCharacter"
          @start="startChat"
        />
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
import CharacterCard from '@/components/CharacterCard.vue'
import { chatService } from '@/services/chatService'

export default {
  name: 'Home',
  components: { CharacterCard },
  data() {
    return {
      showHistoryDialog: false,
      historyLoading: false,
      chatHistoryList: [],
      characters: []
    }
  },
  async created() {
    // 页面创建时加载角色信息
    await this.loadCharacters()
  },
  methods: {
    async loadCharacters() {
      try {
        // 从后端获取角色分页数据
        const characterData = await chatService.getCharacterPage(1, 100)
        console.log('获取到的角色数据:', characterData)
        
        // 处理角色数据
        if (characterData && Array.isArray(characterData.records)) {
          this.characters = characterData.records.map(character => ({
            id: character.id,
            name: character.name || '未知角色',
            avatar: character.avatar || '🤖',
            image: character.image,
            description: character.description || '暂无描述',
            tags: character.tags ? character.tags.split(',') : ['AI角色']
          }))
        } else if (characterData && typeof characterData === 'object') {
          // 如果返回的是对象格式，尝试直接使用
          this.characters = [characterData].map(character => ({
            id: character.id,
            name: character.name || '未知角色',
            avatar: character.avatar || '🤖',
            image: character.image,
            description: character.description || '暂无描述',
            tags: character.tags ? character.tags.split(',') : ['AI角色']
          }))
        } else {
          // 如果没有获取到数据，使用默认角色
          this.characters = [
            {
              id: 'voice-ai',
              name: 'AI语音助手',
              avatar: '🎤',
              description: '智能语音AI助手，支持流畅的语音对话交互',
              tags: ['语音', 'AI', '智能', '交互']
            }
          ]
        }
      } catch (error) {
        console.error('加载角色信息失败:', error)
        // 出错时使用默认角色
        this.characters = [
          {
            id: 'voice-ai',
            name: 'AI语音助手',
            avatar: '🎤',
            description: '智能语音AI助手，支持流畅的语音对话交互',
            tags: ['语音', 'AI', '智能', '交互']
          }
        ]
        this.$message.error('加载角色信息失败，使用默认角色')
      }
    },
    
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
  background: #f5f5f5; /* 去除彩色渐变背景 */
  padding: 0 200px; /* 屏幕左右留白 200px */
}

/* 全局兜底，确保没有渐变透出 */
:root, html, body, #app {
  background: #f5f5f5 !important;
}

.navbar {
  background: #ffffff; /* 去除透明以免透出渐变 */
  border-bottom: 1px solid #e6e6e6;
  padding: 0; /* 留白由外层 home-container 控制 */
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
  color: #333;
  font-size: 24px;
  font-weight: 600;
  margin: 0;
}

.history-btn { font-size: 16px; }

.history-btn:hover {
  color: #f0f0f0 !important;
}

.main-content {
  padding: 40px 0; /* 左右由外层控制 */
  width: 100%; /* 占满外层容器，保证左右恒定 200px */
}
@media (max-width: 1024px) { .home-container { padding: 0 80px; } }
@media (max-width: 640px) { .home-container { padding: 0 16px; } }

.hero-section {
  text-align: center;
  margin-bottom: 50px;
}

.hero-title {
  color: #333;
  font-size: 36px;
  font-weight: 600;
  margin-bottom: 15px;
}

.hero-subtitle {
  color: #666;
  font-size: 18px;
  margin: 0;
}

.characters-grid {
  display: grid !important; /* 防止被其他样式覆盖为 block/flex */
  grid-template-columns: repeat(3, minmax(0, 1fr)); /* 固定三列并允许收缩 */
  gap: 24px;
  margin-top: 40px;
}
@media (max-width: 1024px) {
  .characters-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 640px) { .characters-grid { grid-template-columns: 1fr; } }

.character-card {
  width: auto; /* 由网格列控制宽度 */
  padding: 30px;
  text-align: left; /* 文本左对齐 */
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid transparent;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.character-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
  border-color: rgba(102, 126, 234, 0.3);
}

.character-avatar {
  width: 120px; /* 固定头像容器，避免大图撑满页面 */
  height: 120px;
  border-radius: 50%;
  margin: 0 auto 20px auto; /* 水平居中头像 */
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
}
@media (max-width: 480px) {
  .character-avatar { width: 96px; height: 96px; }
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
  min-height: 60px;
}

.character-tags {
  margin-bottom: 25px;
  min-height: 30px;
}

.character-tag {
  margin: 0 5px 5px 0;
}

.chat-btn {
  width: 120px;
  height: 40px;
  font-size: 16px;
  font-weight: 500;
  display: block;
  margin: 12px auto 0; /* 水平居中按钮 */
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

.character-image {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover; /* 充满并裁剪，避免变形与溢出 */
  object-position: center;
}
</style>