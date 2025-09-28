import api from '@/utils/api'

// 统一处理：无论是否包含 code，只要有 data 字段就取 data
function handleResultResponse(res) {
  if (res && typeof res === 'object' && Object.prototype.hasOwnProperty.call(res, 'data')) {
    return res.data
  }
  return res
}

export const chatService = {
  // 生成会话 ID
  async generateChatId(characterId) {
    // 全路径：/chat-session/generateChatId
    const res = await api.get('/chat-session/generateChatId', { params: { CharacterId: characterId } })
    // 拦截器返回的是 res.data，因此这里的 res 就是 { code, msg, data }
    if (res && typeof res.data === 'string') return res.data
    return ''
  },
  // 文本聊天（保留以备将来使用）
  async sendMessage(prompt, chatId, files = null) {
    const formData = new FormData()
    
    if (files) {
      files.forEach(file => {
        formData.append('files', file)
      })
    }
    
    const config = {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      params: {
        prompt,
        chatId
      }
    }
    
    const response = await api.post('/ai/chat', formData, config)
    return handleResultResponse(response)
  },

  // 语音聊天 - 支持返回音频链接
  async sendVoiceMessage(chatId, audioFile, path = null) {
    const formData = new FormData()
    // 根据后端接口，音频文件应该通过prompt参数传递
    formData.append('prompt', audioFile)
    
    // 添加其他参数
    const params = new URLSearchParams({
      chatId,
      ...(path && { path })
    })
    
    try {
      console.log('发送语音聊天请求:', {
        chatId,
        audioFileSize: audioFile.size,
        audioFileName: audioFile.name,
        audioFileType: audioFile.type,
        parameterName: 'prompt', // 音频文件通过prompt参数传递
        url: `${api.defaults.baseURL}/ai/voiceChat?${params}`
      })
      
      const response = await fetch(`${api.defaults.baseURL}/ai/voiceChat?${params}`, {
        method: 'POST',
        body: formData
      })
      
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      
      // 解析返回的JSON数据
      const result = await response.json()
      console.log('语音聊天响应:', result)
      
      return handleResultResponse(result)
      
    } catch (error) {
      console.error('语音聊天请求失败:', error)
      // 如果流式请求失败，回退到普通请求
      const fallbackFormData = new FormData()
      fallbackFormData.append('prompt', audioFile) // 音频文件通过prompt参数传递
      
      const config = {
        headers: {
          'Content-Type': 'multipart/form-data'
        },
        params: {
          chatId,
          ...(path && { path })
        }
      }
      const response = await api.post('/ai/voiceChat', fallbackFormData, config)
      return handleResultResponse(response)
    }
  },

  // 获取聊天历史列表
  async getChatHistoryList(type = 'chat') {
    // 根据后端接口文档: GET /ai/history/{type}
    // type 可以是 chat, service, pdf, voice 等
    const response = await api.get(`/ai/history/${type}`)
    return handleResultResponse(response)
  },

  // 获取具体聊天历史
  async getChatHistory(type = 'chat', chatId) {
    // 根据后端接口文档: GET /ai/history/{type}/{chatId}
    const response = await api.get(`/ai/history/${type}/${chatId}`)
    return handleResultResponse(response)
  },
  
  // 获取语音聊天历史记录
  async getVoiceChatHistory(chatId) {
    // 根据后端接口文档: GET /ai/history/voice/{chatId}
    const response = await api.get(`/ai/history/voice/${chatId}`)
    return handleResultResponse(response)
  },
  
  // 获取角色信息分页数据
  async getCharacterPage(currentPage = 1, pageSize = 10) {
    const params = {
      currentPage,
      pageSize
    }
    
    const response = await api.get('/character/getPage', { params })
    return handleResultResponse(response)
  },

  // 创建人物（依据 openapi：GET /character/create 使用 query 参数）
  async createCharacter({ id, name, description, image, promt, voiceModel, voice }) {
    const params = { id, name, description, image, promt, voiceModel, voice }
    const response = await api.get('/character/create', { params })
    return handleResultResponse(response)
  },

  // 修改人物（与后端一致，使用 query 参数）
  async updateCharacter({ id, name, description, image, promt, voiceModel, voice }) {
    const params = { id, name, description, image, promt, voiceModel, voice }
    const response = await api.get('/character/update', { params })
    return handleResultResponse(response)
  },

  // 删除人物（query: id）
  async deleteCharacter(id) {
    const response = await api.get('/character/delete', { params: { id } })
    return handleResultResponse(response)
  },

  // 上传文件，返回图片 URL（等价于 uploadToOss）
  async uploadFile(file) {
    const form = new FormData()
    form.append('file', file)
    const response = await api.post('/oss/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    return handleResultResponse(response) // 期望为字符串 URL
  },

  // 根据 openapi: 获取 TTS 模型列表
  async getTTSModels() {
    const response = await api.get('/chat-session/tts/models')
    return handleResultResponse(response)
  },

  // 根据 openapi: 获取指定模型的音色列表
  async getVoicesByModel(model) {
    const response = await api.get(`/chat-session/tts/voices/${encodeURIComponent(model)}`)
    return handleResultResponse(response)
  },

  // 根据 openapi: OSS 上传（multipart/form-data, field: file）
  async uploadToOss(file) {
    const form = new FormData()
    form.append('file', file)
    const response = await api.post('/oss/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    return handleResultResponse(response)
  },

  // 获取聊天会话列表，通过chatName获取角色信息
  async getChatSessionList(chatName) {
    const params = chatName ? { chatName } : {}
    const response = await api.get('/chat-session/getChatSessionList', { params })
    return handleResultResponse(response)
  }
}