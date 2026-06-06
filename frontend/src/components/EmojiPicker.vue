<template>
  <el-popover
    ref="popoverRef"
    placement="top"
    :width="340"
    trigger="click"
    :offset="8"
    popper-class="emoji-popover"
  >
    <template #reference>
      <el-button circle size="default" class="emoji-trigger-btn">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10"/>
          <path d="M8 14s1.5 2 4 2 4-2 4-2"/>
          <line x1="9" y1="9" x2="9.01" y2="9"/>
          <line x1="15" y1="9" x2="15.01" y2="9"/>
        </svg>
      </el-button>
    </template>
    <div class="emoji-picker">
      <div class="emoji-categories">
        <span
          v-for="(cat, key) in categories"
          :key="key"
          class="emoji-cat"
          :class="{ active: activeCategory === key }"
          @click="activeCategory = key"
        >{{ cat.icon }}</span>
      </div>
      <div class="emoji-grid">
        <span
          v-for="emoji in currentEmojis"
          :key="emoji"
          class="emoji-item"
          @click="pickEmoji(emoji)"
          :title="emoji"
        >{{ emoji }}</span>
      </div>
    </div>
  </el-popover>
</template>

<script setup>
import { ref, computed } from 'vue'

const emit = defineEmits(['emoji-select'])

const popoverRef = ref(null)
const activeCategory = ref('smileys')

const categories = {
  smileys: { icon: '😀' },
  gestures: { icon: '👍' },
  hearts: { icon: '❤️' },
  nature: { icon: '🌸' },
  objects: { icon: '💡' },
  symbols: { icon: '✅' }
}

const emojisByCategory = {
  smileys: [
    '😀', '😃', '😄', '😁', '😆', '😅', '🤣', '😂',
    '🙂', '😊', '😇', '🥰', '😍', '🤩', '😘', '😗',
    '😋', '😛', '😜', '🤪', '😝', '🤑', '🤗', '🤭',
    '🤔', '🤐', '🤨', '😐', '😑', '😶', '😏', '😒',
    '🙄', '😬', '😌', '😔', '😪', '😴', '😷', '🤒',
    '🤕', '🤢', '🤮', '🥵', '🥶', '😵', '🤯', '🥳',
    '😎', '🤓', '😕', '😟', '😮', '😯', '😲', '😳',
    '🥺', '😦', '😧', '😨', '😰', '😥', '😢', '😭',
    '😱', '😖', '😣', '😞', '😓', '😩', '😫', '🥱'
  ],
  gestures: [
    '👍', '👎', '👌', '✌️', '🤞', '🤟', '🤘', '🤙',
    '👏', '🙌', '👐', '🤲', '🤝', '🙏', '✍️', '💪',
    '🦾', '🖐️', '✋', '🤚', '👋', '🖖', '🤏', '👆',
    '👇', '👉', '👈', '🙋', '🙆', '🙅', '🤷', '🤦'
  ],
  hearts: [
    '❤️', '🧡', '💛', '💚', '💙', '💜', '🖤', '🤍',
    '🤎', '💔', '❣️', '💕', '💞', '💓', '💗', '💖',
    '💘', '💝', '💟', '♥️', '🫶', '💌', '💋', '😻'
  ],
  nature: [
    '⭐', '🌟', '✨', '💫', '🔥', '💥', '💯', '💢',
    '💦', '💨', '🕳️', '🎉', '🎊', '🎈', '🌸', '🌺',
    '🌻', '🌹', '🍀', '🌈', '☀️', '🌙', '⚡', '❄️',
    '🍎', '🍕', '🎂', '☕', '🍺', '🎵', '🎶', '📢'
  ],
  objects: [
    '💡', '🔦', '📱', '💻', '🖥️', '⌨️', '🖱️', '🎮',
    '📷', '🎥', '📹', '🎬', '🎤', '🎧', '📞', '☎️',
    '⏰', '⌚', '🔔', '🔕', '📌', '📍', '✂️', '🔑',
    '💰', '💎', '🎁', '📦', '📄', '📊', '📈', '🛠️'
  ],
  symbols: [
    '✅', '❌', '❓', '❗', '‼️', '⁉️', '➕', '➖',
    '➗', '✖️', '♾️', '💲', '©️', '®️', '™️', '#️⃣',
    '*️⃣', '0️⃣', '1️⃣', '2️⃣', '3️⃣', '4️⃣', '5️⃣', '6️⃣',
    '7️⃣', '8️⃣', '9️⃣', '🔟', '🅰️', '🅱️', '🆎', '🆑'
  ]
}

const currentEmojis = computed(() => {
  return emojisByCategory[activeCategory.value] || emojisByCategory.smileys
})

const pickEmoji = (emoji) => {
  emit('emoji-select', emoji)
  popoverRef.value?.hide()
}
</script>

<style>
.emoji-popover {
  padding: 0 !important;
}
</style>

<style scoped>
.emoji-trigger-btn {
  color: #707579 !important;
  border: none !important;
  background: transparent !important;
}

.emoji-trigger-btn:hover {
  background-color: #F4F4F5 !important;
  color: #3390EC !important;
}

.emoji-picker {
  padding: 8px;
}

.emoji-categories {
  display: flex;
  gap: 2px;
  padding: 4px 4px 8px;
  border-bottom: 1px solid #E8ECEF;
  margin-bottom: 8px;
}

.emoji-cat {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  cursor: pointer;
  font-size: 18px;
  transition: background-color 0.15s;
}

.emoji-cat:hover {
  background-color: #F4F4F5;
}

.emoji-cat.active {
  background-color: #E3F2FD;
}

.emoji-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 2px;
  max-height: 260px;
  overflow-y: auto;
}

.emoji-item {
  font-size: 26px;
  cursor: pointer;
  text-align: center;
  padding: 6px 4px;
  border-radius: 8px;
  transition: background-color 0.15s;
  line-height: 1;
}

.emoji-item:hover {
  background-color: #F4F4F5;
  transform: scale(1.2);
}
</style>
