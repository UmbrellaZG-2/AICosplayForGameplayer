<template>
  <div class="avatar-test-container">
    <h2>Avatar Path Test</h2>
    <div class="avatar-test">
      <p>Test 1: Using /Character alias (direct path)</p>
      <img 
        src="/Character/赛马娘无声铃鹿.jpg" 
        alt="赛马娘无声铃鹿" 
        style="width: 100px; height: 100px; object-fit: cover; border-radius: 50%" 
        @error="handleTestError('Test 1 failed')"
      />
    </div>
    <div class="avatar-test">
      <p>Test 2: Using import statement</p>
      <img 
        :src="importedAvatar" 
        alt="赛马娘无声铃鹿 (imported)" 
        style="width: 100px; height: 100px; object-fit: cover; border-radius: 50%" 
        @error="handleTestError('Test 2 failed')"
      />
    </div>
    <div class="avatar-test">
      <p>Test 3: Using relative path</p>
      <img 
        src="/resource/Character/赛马娘无声铃鹿.jpg" 
        alt="赛马娘无声铃鹿 (relative)" 
        style="width: 100px; height: 100px; object-fit: cover; border-radius: 50%" 
        @error="handleTestError('Test 3 failed')"
      />
    </div>
    <div v-if="errors.length > 0">
      <h3>Errors:</h3>
      <ul>
        <li v-for="(error, index) in errors" :key="index">{{ error }}</li>
      </ul>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import importedAvatar from '../../resource/Character/赛马娘无声铃鹿.jpg';

const errors = ref([]);

const handleTestError = (message) => {
  errors.value.push(message);
  console.error(message);
};
</script>

<style scoped>
.avatar-test-container {
  padding: 20px;
  max-width: 600px;
  margin: 0 auto;
}

.avatar-test {
  margin-bottom: 20px;
}

ul {
  color: red;
}
</style>