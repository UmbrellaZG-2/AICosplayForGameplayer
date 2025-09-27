import { readdirSync } from 'fs';
import { join } from 'path';

// 读取resource/Character目录内容
const characterDir = join(process.cwd(), 'resource', 'Character');

console.log(`检查目录: ${characterDir}`);

try {
  const files = readdirSync(characterDir);
  console.log('找到的文件:');
  files.forEach(file => {
    console.log(`- ${file}`);
  });
} catch (error) {
  console.error('读取目录时出错:', error.message);
}