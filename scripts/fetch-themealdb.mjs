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

// Dạng chế biến khô/xay luôn là gia vị, kể cả khi tên chứa từ của nguyên liệu chính
const SPICE_MARKERS = ["flakes", "powder", "ground", "dried", "paste", "extract", "essence"];
// Tên ghép mà một từ trong đó trùng MINOR nhưng bản thân là nguyên liệu CHÍNH
// (trước đây khớp chuỗi con nên "Butternut Squash" bị coi là "butter" → hạ nhầm weight 0.3)
const MAIN_OVERRIDE = ["peanut butter", "butternut", "sugar snap", "water chestnut", "soya bean",
  "rice flour", "butter bean", "red pepper", "green pepper", "yellow pepper", "bell pepper",
  "spring greens", "onion squash"];

const singular = (w) => (w.length > 3 && w.endsWith("s") ? w.slice(0, -1) : w);

function isMinor(name) {
  const n = name.toLowerCase();
  if (SPICE_MARKERS.some((s) => n.includes(s))) return true;
  if (MAIN_OVERRIDE.some((m) => n.includes(m))) return false;
  const words = n.split(/[^a-z]+/).filter(Boolean).map(singular);
  const joined = words.join(" ");
  // khớp theo RANH GIỚI TỪ, không phải chuỗi con
  return MINOR.some((m) => (m.includes(" ") ? joined.includes(m) : words.includes(m)));
}

// Fallback khi nguyên liệu không có trong catalog (nguyên liệu ngoại chiếm ~60%):
// thà gắn cờ dư còn hơn bỏ sót — bỏ sót nghĩa là gợi ý món có thịt/hải sản cho người ăn chay/dị ứng.
const NON_DAIRY = /coconut|peanut|almond|soya|soy|cocoa|butter bean|shea/i;
const FALLBACK = [
  ["contains_meat", /\b(beef|pork|lamb|mutton|bacon|ham|chicken|turkey|duck|steak|mince|minced|sausage|chorizo|prosciutto|salami|veal|goose|liver|gammon|pancetta)\b/i],
  ["contains_seafood", /\b(fish|prawn|prawns|shrimp|crab|lobster|squid|octopus|clam|mussel|oyster|scallop|anchovy|anchovies|salmon|tuna|cod|haddock|mackerel|sardine|seafood|kipper)\b/i],
  ["contains_egg", /\begg/i],
  ["contains_dairy", /\b(milk|cheese|butter|cream|yoghurt|yogurt|ghee|mascarpone|ricotta|parmesan|mozzarella|creme)\b/i],
  ["contains_gluten", /\b(flour|bread|breadcrumb|breadcrumbs|pasta|spaghetti|lasagne|noodle|noodles|couscous|barley|semolina|pastry|bun|tortilla)\b/i],
  ["contains_nuts", /\b(peanut|peanuts|cashew|cashews|almond|almonds|walnut|walnuts|pistachio|hazelnut|pecan)\b/i],
];

function inferDietaryFlags(ings) {
  const flags = new Set();
  for (const ing of ings) {
    const e = byNorm.get(ing.normalized_name);
    if (e) {
      // Ưu tiên catalog vì chính xác hơn regex
      if (e.allergenGroup === "trung") flags.add("contains_egg");
      if (e.allergenGroup === "ca" || e.allergenGroup === "hai_san") flags.add("contains_seafood");
      if (e.allergenGroup === "sua") flags.add("contains_dairy");
      if (e.allergenGroup === "gluten") flags.add("contains_gluten");
      if (e.allergenGroup === "dau_phong" || e.allergenGroup === "hat") flags.add("contains_nuts");
      if (e.allergenGroup === "me") flags.add("contains_sesame");
      if (e.category === "thit") flags.add("contains_meat");
    }
    // Chạy regex CẢ KHI đã khớp catalog: catalog xếp "Nước dùng gà" vào do_kho nên không ra
    // contains_meat, dù với người ăn chay thì nước dùng gà vẫn là vi phạm. Sản phẩm chế biến
    // (nước dùng, nước mắm...) cần regex bắt theo tên.
    for (const [flag, re] of FALLBACK) {
      if (!re.test(ing.name)) continue;
      if (flag === "contains_dairy" && NON_DAIRY.test(ing.name)) continue; // "coconut milk"/"peanut butter" không phải sữa
      flags.add(flag);
    }
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
