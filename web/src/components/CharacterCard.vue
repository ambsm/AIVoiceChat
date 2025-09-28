<template>
  <div class="character-card card" @click="$emit('select', character)">
    <div class="character-avatar">
      <img
        v-if="character.image"
        :src="character.image"
        :alt="character.name"
        class="character-image"
      />
      <span v-else>{{ character.avatar || '🤖' }}</span>
    </div>
    <h3 class="character-name">{{ character.name }}</h3>
    <p class="character-desc">{{ character.description }}</p>
    <div class="character-tags">
      <el-tag v-for="tag in tags" :key="tag" size="mini" type="info" class="character-tag">
        {{ tag }}
      </el-tag>
    </div>
    <div class="card-actions">
      <el-button type="primary" class="chat-btn btn-primary" @click.stop="$emit('start', character)">开始聊天</el-button>
      <el-button type="default" size="mini" @click.stop="$emit('edit', character)">修改</el-button>
      <el-button type="danger" size="mini" @click.stop="$emit('delete', character)">删除</el-button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'CharacterCard',
  props: {
    character: { type: Object, required: true }
  },
  computed: {
    tags() {
      const { tags } = this.character || {}
      if (Array.isArray(tags)) return tags
      if (typeof tags === 'string') return tags.split(',').map(s => s.trim()).filter(Boolean)
      return []
    }
  }
}
</script>

<style scoped>
.character-card { width: auto; padding: 30px; text-align: left; cursor: pointer; transition: all .3s ease; border: 2px solid transparent; background: #fff; border-radius: 16px; box-shadow: 0 4px 20px rgba(0,0,0,.1); display:flex; flex-direction:column; height:100%; }
.character-card:hover { transform: translateY(-8px); box-shadow: 0 12px 40px rgba(0,0,0,.15); border-color: rgba(102,126,234,.3); }
.character-avatar { width: 120px; height: 120px; border-radius: 50%; margin: 0 auto 20px auto; display:flex; justify-content:center; align-items:center; overflow:hidden; }
.character-image { width:100%; height:100%; border-radius:50%; object-fit:cover; object-position:center; }
.character-name { color:#333; font-size:22px; font-weight:600; margin-bottom:15px; }
.character-desc { color:#666; font-size:16px; line-height:1.6; margin-bottom:20px; min-height:60px; }
.character-tags { margin-bottom:25px; min-height:30px; }
.character-tag { margin:0 5px 5px 0; }
.chat-btn { width:120px; height:40px; font-size:16px; font-weight:500; }
.card-actions { margin-top:auto; display:flex; justify-content:center; gap:8px; }
</style>


