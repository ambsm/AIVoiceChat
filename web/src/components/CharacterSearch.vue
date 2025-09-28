<template>
  <div class="character-search">
    <div class="search-container">
      <el-input
        v-model="searchQuery"
        placeholder="搜索角色，如：李白、苏格拉底、林黛玉..."
        class="search-input"
        @input="handleSearch"
        clearable
      >
        <i slot="prefix" class="el-input__icon el-icon-search"></i>
      </el-input>
      
      <div class="search-suggestions" v-if="showSuggestions && suggestions.length > 0">
        <div 
          v-for="suggestion in suggestions" 
          :key="suggestion"
          class="suggestion-item"
          @click="selectSuggestion(suggestion)"
        >
          {{ suggestion }}
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'CharacterSearch',
  props: {
    characters: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      searchQuery: '',
      showSuggestions: false,
      suggestions: []
    }
  },
  methods: {
    handleSearch() {
      this.showSuggestions = this.searchQuery.length > 0
      this.generateSuggestions()
      this.$emit('search', {
        query: this.searchQuery
      })
    },
    
    generateSuggestions() {
      if (!this.searchQuery) {
        this.suggestions = []
        return
      }
      
      const query = this.searchQuery.toLowerCase()
      const allNames = this.characters.map(char => char.name).filter(Boolean)
      
      this.suggestions = allNames
        .filter(name => name.toLowerCase().includes(query))
        .slice(0, 5)
    },
    
    selectSuggestion(suggestion) {
      this.searchQuery = suggestion
      this.showSuggestions = false
      this.$emit('search', {
        query: this.searchQuery
      })
    }
  },
  mounted() {
    // 点击外部关闭建议列表
    document.addEventListener('click', (e) => {
      if (!this.$el.contains(e.target)) {
        this.showSuggestions = false
      }
    })
  }
}
</script>

<style scoped>
.character-search {
  margin-bottom: 30px;
}

.search-container {
  position: relative;
  max-width: 600px;
  margin: 0 auto 20px auto;
}

.search-input {
  width: 100%;
}

.search-input ::v-deep .el-input__inner {
  height: 50px;
  font-size: 16px;
  border-radius: 25px;
  padding-left: 50px;
  border: 2px solid #e6e6e6;
  transition: all 0.3s ease;
}

.search-input ::v-deep .el-input__inner:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.search-suggestions {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: white;
  border: 1px solid #e6e6e6;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.1);
  z-index: 1000;
  max-height: 200px;
  overflow-y: auto;
}

.suggestion-item {
  padding: 12px 20px;
  cursor: pointer;
  transition: background-color 0.2s ease;
  border-bottom: 1px solid #f5f5f5;
}

.suggestion-item:hover {
  background-color: #f8f9fa;
}

.suggestion-item:last-child {
  border-bottom: none;
}

</style>
