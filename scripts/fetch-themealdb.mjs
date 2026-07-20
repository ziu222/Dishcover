// Dev-time tool (một lần): fetch TheMealDB → transform sang schema Recipe (CLAUDE.md mục 3.2)
// → ghi recipe/src/main/resources/seed/recipes-themealdb.json (RecipeSeeder tự nạp).
// Chạy: node scripts/fetch-themealdb.mjs
// Dữ liệu output được commit làm snapshot — không phụ thuộc API lúc seed.
import { readFileSync, writeFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, resolve } from "node:path";

const __dir = dirname(fileURLToPath(import.meta.url));
const ROOT = resolve(__dir, "..");
const API = "https://www.themealdb.com/api/json/v1/1";

// ponytail: port normalize từ VietnameseTextNormalizer.java (input TheMealDB là tiếng Anh nên
// phần bỏ dấu hầu như không dùng; giữ y hệt để nhất quán nếu gặp ký tự có dấu).
function normalize(raw) {
  if (!raw) return "";
  return raw.trim().toLowerCase().replace(/đ/g, "d")
    .normalize("NFD").replace(/\p{Diacritic}+/gu, "")
    .replace(/[^a-z0-9\s]/g, " ").trim().replace(/\s+/g, " ");
}

// Build alias index từ catalog (giống IngredientCatalog): aliasKey(normalize) -> entry
const catalog = JSON.parse(
  readFileSync(resolve(ROOT, "common/src/main/resources/ingredient-catalog.json"), "utf8"));
const aliasIndex = new Map();
const byNorm = new Map();
for (const e of catalog) {
  byNorm.set(e.normalizedName, e);
  for (const k of [e.canonicalName, e.normalizedName, ...(e.aliases || [])]) {
    const key = normalize(k);
    if (key && !aliasIndex.has(key)) aliasIndex.set(key, e.normalizedName);
  }
}
const resolveName = (raw) => aliasIndex.get(normalize(raw)) ?? normalize(raw);

// Nguyên liệu phụ (gia vị/thơm) → weight 0.3; còn lại essential weight 1.0
const MINOR = ["salt", "pepper", "oil", "sugar", "water", "garlic", "onion", "sauce", "spice",
  "vinegar", "butter", "stock", "broth", "herb", "cilantro", "parsley", "coriander", "seasoning",
  "paprika", "cumin", "chilli", "chili", "lime", "lemon", "honey", "soy", "mirin", "sesame",
  "ginger", "scallion", "spring onion", "wine", "cornflour", "cornstarch", "flour", "baking",
  "yeast", "vanilla", "cinnamon", "nutmeg", "clove", "bay leaf", "thyme", "basil", "mint", "dill"];
const isMinor = (name) => { const n = name.toLowerCase(); return MINOR.some((m) => n.includes(m)); };

function inferDietaryFlags(ings) {
  const flags = new Set();
  for (const ing of ings) {
    const e = byNorm.get(ing.normalized_name);
    if (!e) continue;
    if (e.allergenGroup === "trung") flags.add("contains_egg");
    if (e.allergenGroup === "ca" || e.allergenGroup === "hai_san") flags.add("contains_seafood");
    if (e.allergenGroup === "sua") flags.add("contains_dairy");
    if (e.allergenGroup === "gluten") flags.add("contains_gluten");
    if (e.category === "thit") flags.add("contains_meat");
  }
  return [...flags];
}

function parseMeasure(measure) {
  const m = (measure || "").trim();
  const num = m.match(/^(\d+(?:[.,/]\d+)?)\s*(.*)$/);
  if (num) return { amount: parseAmount(num[1]), unit: num[2].trim() || null };
  return { amount: null, unit: m || null };
}
function parseAmount(s) {
  if (s.includes("/")) { const [a, b] = s.split("/").map(Number); return b ? +(a / b).toFixed(2) : null; }
  const v = Number(s.replace(",", ".")); return Number.isFinite(v) ? v : null;
}

function splitSteps(instr) {
  let parts = (instr || "").split(/\r?\n+/).map((s) => s.trim()).filter(Boolean);
  if (parts.length < 2) {
    parts = (instr || "").split(/(?<=\.)\s+(?=[A-Z])/).map((s) => s.trim()).filter(Boolean);
  }
  return parts.map((content, i) => ({
    order: i + 1, title: `Bước ${i + 1}`, content, duration_minutes: 0,
  }));
}

function transform(m) {
  const ings = [];
  for (let i = 1; i <= 20; i++) {
    const name = (m["strIngredient" + i] || "").trim();
    if (!name) continue;
    const { amount, unit } = parseMeasure(m["strMeasure" + i]);
    const minor = isMinor(name);
    ings.push({
      ingredient_id: "ing_" + normalize(name).replace(/\s+/g, "_"),
      name, normalized_name: resolveName(name), amount, unit,
      essential: !minor, weight: minor ? 0.3 : 1.0,
    });
  }
  const difficulty = ings.length <= 5 ? "EASY" : ings.length <= 9 ? "MEDIUM" : "HARD";
  const tags = [m.strArea, m.strCategory, ...((m.strTags || "").split(",").map((t) => t.trim()).filter(Boolean))]
    .filter(Boolean);
  return {
    _id: "mealdb_" + m.idMeal,
    name: m.strMeal,
    slug: normalize(m.strMeal).replace(/\s+/g, "-"),
    cook_time_minutes: 30, // TheMealDB không có → ước lượng
    difficulty,
    tags,
    dietary_flags: inferDietaryFlags(ings),
    ingredients: ings,
    steps: splitSteps(m.strInstructions),
    image_url: m.strMealThumb || null,
    video_url: m.strYoutube || null,
    created_at: "2026-07-21T00:00:00Z",
  };
}

const AREAS = { Vietnamese: 27, Chinese: 7, Thai: 7, Japanese: 5, Italian: 6 };
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function main() {
  const seen = new Set();
  const out = [];
  for (const [area, cap] of Object.entries(AREAS)) {
    const list = (await (await fetch(`${API}/filter.php?a=${area}`)).json()).meals || [];
    let n = 0;
    for (const meal of list) {
      if (n >= cap) break;
      if (seen.has(meal.idMeal)) continue;
      seen.add(meal.idMeal);
      const full = (await (await fetch(`${API}/lookup.php?i=${meal.idMeal}`)).json()).meals?.[0];
      if (!full) continue;
      out.push(transform(full));
      n++;
      await sleep(150);
    }
    console.log(`${area}: ${n} món`);
  }
  const coverage = out.flatMap((r) => r.ingredients)
    .filter((i) => byNorm.has(i.normalized_name)).length;
  const total = out.reduce((s, r) => s + r.ingredients.length, 0);
  console.log(`Tổng ${out.length} công thức, ${out.reduce((s, r) => s + r.ingredients.length, 0)} nguyên liệu, ` +
    `khớp catalog ${coverage}/${total} (${Math.round((coverage / total) * 100)}%)`);
  writeFileSync(resolve(ROOT, "recipe/src/main/resources/seed/recipes-themealdb.json"),
    JSON.stringify(out, null, 2) + "\n", "utf8");
}
main();
