// stores/user.ts
import { conversationApi } from '@/api/modules/conversation'
import type { ConversationResponse } from '@/types/api'
import { ElMessage } from 'element-plus'
import { defineStore } from 'pinia'

export const chatHistoryStore = defineStore('chatHistory', {
  state: () => ({
    chatHistory: [] as ConversationResponse[]
  }),

  getters: {
  },

  actions: {
    // 获取历史对话记录
    async getChatHistory() {
      const res = await conversationApi.getConversationList()
      if (res.code == 200) {
        this.chatHistory = res.data
      } else {
        ElMessage.error(res.message)
      }
    },

    // 添加历史对话记录
    async addChatHistory(characterId: number | string) {
      const res = await conversationApi.createConversation({ characterId })
      if (res.code == 200) {
        this.chatHistory.unshift(res.data)
        return res.data.conversationUuid
      } else {
        ElMessage.error(res.message)
        throw Error(res.message)
      }
    },
    // 删除对话
    async deleteChatHistory(conversationUuid: string) {
      const res = await conversationApi.deleteConversation(conversationUuid)
      if (res.code == 200) {
        ElMessage.success('对话已删除')
        // 从列表中移除已删除的对话
        this.chatHistory = this.chatHistory.filter(
          (chat) => chat.conversationUuid !== conversationUuid,
        )
      }
    }
  },
})