// Mint 1 JWT HS256 hợp lệ để demo — CÙNG logic với eval/image/run-eval.mjs, tách ra dùng chung
// cho cả RAG lẫn Image (không phụ thuộc thư viện jsonwebtoken, chỉ dùng crypto builtin).
// CHẠY: JWT_SECRET=<>=32-ký-tự node eval/circuit-breaker/mint-jwt.mjs
import crypto from 'node:crypto';

const SECRET = process.env.JWT_SECRET;
if (!SECRET || SECRET.length < 32) { console.error('Thiếu JWT_SECRET (>=32 ký tự) — phải giống service.'); process.exit(1); }

const b64url = (buf) => Buffer.from(buf).toString('base64').replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
const h = b64url(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
const now = Math.floor(Date.now() / 1000);
const p = b64url(JSON.stringify({ sub: '1', email: 'demo@test.com', plan: 'FREE', iat: now, exp: now + 7200 }));
const sig = b64url(crypto.createHmac('sha256', SECRET).update(`${h}.${p}`).digest());
console.log(`${h}.${p}.${sig}`);
