// Đo accuracy Image Recognition trên bộ 30 ảnh (CLAUDE.md mục 7/11) — SỐ THẬT, không ước lượng.
// Gọi endpoint /recognize THẬT (test cả pipeline: vision -> parse -> normalize), so normalizedName
// model trả về với ground truth trong labels.json. Tính precision/recall/F1 micro + accuracy/nhóm.
//
// CHẠY:
//   1) Bật Image Service: JWT_SECRET=<>=32ký OPENAI_API_KEY=<real> mvnw -pl image spring-boot:run
//   2) node eval/image/run-eval.mjs   (env: JWT_SECRET giống service; IMAGE_URL mặc định :8086)
//
// Ảnh đặt trong eval/image/images/<file> đúng tên trong labels.json. Nhãn empty = expected [].
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';

const DIR = path.dirname(new URL(import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, '$1'));
const IMAGES_DIR = path.join(DIR, 'images');
const BASE = process.env.IMAGE_URL || 'http://localhost:8086';
const SECRET = process.env.JWT_SECRET;
if (!SECRET || SECRET.length < 32) { console.error('Thiếu JWT_SECRET (>=32 ký tự) — phải giống service.'); process.exit(1); }

const b64url = (buf) => Buffer.from(buf).toString('base64').replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
function mintJwt() {
  const h = b64url(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const now = Math.floor(Date.now() / 1000);
  const p = b64url(JSON.stringify({ sub: '1', email: 'eval@test.com', plan: 'FREE', iat: now, exp: now + 7200 }));
  const sig = b64url(crypto.createHmac('sha256', SECRET).update(`${h}.${p}`).digest());
  return `${h}.${p}.${sig}`;
}

const mime = (f) => ({ '.jpg': 'image/jpeg', '.jpeg': 'image/jpeg', '.png': 'image/png', '.webp': 'image/webp' }[path.extname(f).toLowerCase()] || 'application/octet-stream');

async function recognize(token, file) {
  const p = path.join(IMAGES_DIR, file);
  if (!fs.existsSync(p)) return { ok: false, status: 0, ms: 0, body: `THIẾU FILE ${file} trong eval/image/images/` };
  const buf = fs.readFileSync(p);
  const fd = new FormData();
  fd.append('file', new Blob([buf], { type: mime(file) }), file);
  const t0 = Date.now();
  const res = await fetch(`${BASE}/recognize`, { method: 'POST', headers: { Authorization: `Bearer ${token}` }, body: fd });
  const ms = Date.now() - t0;
  const body = await res.text();
  if (!res.ok) return { ok: false, status: res.status, ms, body: body.slice(0, 160) };
  const items = JSON.parse(body).items || [];
  return { ok: true, ms, pred: items.map((x) => x.normalizedName) };
}

const labels = JSON.parse(fs.readFileSync(path.join(DIR, 'labels.json'), 'utf8'));
const real = labels.images.filter((r) => !r.expected.some((e) => String(e).startsWith('<'))); // bỏ slot chưa điền
if (!real.length) { console.error('labels.json chưa điền nhãn thật (còn placeholder <...>).'); process.exit(1); }

const token = mintJwt();
const byCat = {};
let TP = 0, FP = 0, FN = 0, exactMatch = 0, lat = [];

console.log(`Đo ${real.length} ảnh qua ${BASE}/recognize ...\n`);
for (const row of real) {
  const r = await recognize(token, row.file);
  const cat = (byCat[row.category] ??= { n: 0, tp: 0, fp: 0, fn: 0, exact: 0 });
  cat.n++;
  if (!r.ok) { console.log(`${row.file}: LỖI HTTP ${r.status} ${r.body}`); cat.fn += row.expected.length; FN += row.expected.length; continue; }
  lat.push(r.ms);
  const pred = new Set(r.pred), exp = new Set(row.expected);
  const tp = [...pred].filter((x) => exp.has(x)).length;
  const fp = [...pred].filter((x) => !exp.has(x)).length;
  const fn = [...exp].filter((x) => !pred.has(x)).length;
  TP += tp; FP += fp; FN += fn; cat.tp += tp; cat.fp += fp; cat.fn += fn;
  const exact = fp === 0 && fn === 0; if (exact) { exactMatch++; cat.exact++; }
  console.log(`${row.file} [${row.category}] ${exact ? '✓' : '✗'} pred=${JSON.stringify(r.pred)} exp=${JSON.stringify(row.expected)} (${r.ms}ms)`);
}

const pr = (tp, fp) => (tp + fp ? (tp / (tp + fp) * 100).toFixed(1) : 'n/a');
const rc = (tp, fn) => (tp + fn ? (tp / (tp + fn) * 100).toFixed(1) : 'n/a');
const f1 = (tp, fp, fn) => { const p = tp / (tp + fp || 1), r = tp / (tp + fn || 1); return (p + r ? (2 * p * r / (p + r) * 100).toFixed(1) : '0'); };

console.log(`\n===== TỔNG HỢP (micro, ${real.length} ảnh) =====`);
console.log(`Precision=${pr(TP, FP)}%  Recall=${rc(TP, FN)}%  F1=${f1(TP, FP, FN)}%  (TP=${TP} FP=${FP} FN=${FN})`);
console.log(`Exact-match (khớp trọn tập nguyên liệu): ${exactMatch}/${real.length} = ${(exactMatch / real.length * 100).toFixed(1)}%`);
if (lat.length) console.log(`Latency: avg=${(lat.reduce((a, b) => a + b, 0) / lat.length).toFixed(0)}ms  min=${Math.min(...lat)}  max=${Math.max(...lat)}`);
console.log(`\n--- Theo nhóm ---`);
for (const [c, s] of Object.entries(byCat)) {
  const extra = c === 'empty' ? `  (FP=${s.fp}: ảnh trống mà model vẫn 'nhận' ra thứ gì đó)` : '';
  console.log(`${c}: n=${s.n}  P=${pr(s.tp, s.fp)}%  R=${rc(s.tp, s.fn)}%  exact=${s.exact}/${s.n}${extra}`);
}
