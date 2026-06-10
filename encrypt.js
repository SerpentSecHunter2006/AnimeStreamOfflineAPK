const crypto = require('crypto');
const fs = require('fs');

const algorithm = 'aes-256-cbc';
// 32 byte key
const key = Buffer.from('serpent_sec_hunter_anime_stream_', 'utf8');
// 16 byte IV
const iv = Buffer.from('1234567890123456', 'utf8');

const inputPath = 'e:/AnimeStreamOffline/all_episodes_combined.json';
const outputPath = 'app/src/main/assets/episodes.enc';

console.log('Mulai mengenkripsi...');

const input = fs.createReadStream(inputPath);
const output = fs.createWriteStream(outputPath);

const cipher = crypto.createCipheriv(algorithm, key, iv);

input.pipe(cipher).pipe(output);

output.on('finish', () => {
    console.log('Berhasil dienkripsi ke ' + outputPath);
});
input.on('error', (err) => console.error('Error membaca input:', err));
output.on('error', (err) => console.error('Error menulis output:', err));
