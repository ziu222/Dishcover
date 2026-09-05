// Dev-time tool (một lần): fetch Spoonacular → transform sang schema Recipe (CLAUDE.md mục 3.2)
// → ghi recipe/src/main/resources/seed/recipes-spoonacular.json (RecipeSeeder tự nạp).
// Chạy: SPOONACULAR_API_KEY=xxx node scripts/fetch-spoonacular.mjs
// Khác recipes-vn/au/themealdb.json: nutrition/servings lấy TRỰC TIẾP từ Spoonacular (đáng tin cậy
// hơn tự tính qua IngredientCatalog vì ingredient tiếng Anh, coverage catalog thấp) — xem
// specs/diet-direction-recommendation.md mục 7.4. RecipeSeeder phải tôn trọng field có sẵn, không
// ghi đè (xem sửa ở RecipeSeeder.java cùng đợt).
import { readFileSync, writeFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, resolve } from "node:path";

const __dir = dirname(fileURLToPath(import.meta.url));
const ROOT = resolve(__dir, "..");
const API = "https://api.spoonacular.com/recipes";
const API_KEY = process.env.SPOONACULAR_API_KEY;
if (!API_KEY) {
  console.error("Thiếu SPOONACULAR_API_KEY (đăng ký tại spoonacular.com/food-api).");
  process.exit(1);
}

// ponytail: 3 helper sau đây port nguyên bản từ fetch-themealdb.mjs (cùng logic, nguồn dữ liệu
// tiếng Anh khác nhau nhưng ý nghĩa như nhau) — xem file đó nếu cần đối chiếu lý do thiết kế.
function normalize(raw) {
  if (!raw) return "";
  return raw.trim().toLowerCase().replace(/đ/g, "d")
    .normalize("NFD").replace(/\p{Diacritic}+/gu, "")
    .replace(/[^a-z0-9\s]/g, " ").trim().replace(/\s+/g, " ");
}

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

const MINOR = ["salt", "pepper", "oil", "sugar", "water", "garlic", "onion", "sauce", "spice",
  "vinegar", "butter", "stock", "broth", "herb", "cilantro", "parsley", "coriander", "seasoning",
  "paprika", "cumin", "chilli", "chili", "lime", "lemon", "honey", "soy", "mirin", "sesame",
  "ginger", "scallion", "spring onion", "wine", "cornflour", "cornstarch", "flour", "baking",
  "yeast", "vanilla", "cinnamon", "nutmeg", "clove", "bay leaf", "thyme", "basil", "mint", "dill"];
const SPICE_MARKERS = ["flakes", "powder", "ground", "dried", "paste", "extract", "essence"];
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
  return MINOR.some((m) => (m.includes(" ") ? joined.includes(m) : words.includes(m)));
}

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
      if (e.allergenGroup === "trung") flags.add("contains_egg");
      if (e.allergenGroup === "ca" || e.allergenGroup === "hai_san") flags.add("contains_seafood");
      if (e.allergenGroup === "sua") flags.add("contains_dairy");
      if (e.allergenGroup === "gluten") flags.add("contains_gluten");
      if (e.allergenGroup === "dau_phong" || e.allergenGroup === "hat") flags.add("contains_nuts");
      if (e.allergenGroup === "me") flags.add("contains_sesame");
      if (e.category === "thit") flags.add("contains_meat");
    }
    for (const [flag, re] of FALLBACK) {
      if (!re.test(ing.name)) continue;
      if (flag === "contains_dairy" && NON_DAIRY.test(ing.name)) continue;
      flags.add(flag);
    }
  }
  return [...flags];
}

// diets[] hợp lệ của Spoonacular khớp thẳng "vegetarian"/"vegan" -> dietary_flags (ALLOWED_FLAGS
// có sẵn), còn lại (gluten free/high protein/low carb/paleo/ketogenic...) -> tags.
const DIET_TO_DIETARY_FLAG = new Set(["vegetarian", "vegan"]);

function findNutrient(nutrients, name) {
  return nutrients?.find((n) => n.name === name)?.amount ?? 0;
}

function transform(r) {
  const ings = (r.extendedIngredients || []).map((i) => {
    const name = i.name || i.originalName || "";
    return {
      ingredient_id: "ing_" + normalize(name).replace(/\s+/g, "_"),
      name, normalized_name: resolveName(name),
      amount: i.amount ?? null, unit: i.unit || null,
      essential: !isMinor(name), weight: isMinor(name) ? 0.3 : 1.0,
    };
  });

  const steps = (r.analyzedInstructions?.[0]?.steps || []).map((s) => ({
    order: s.number, title: `Bước ${s.number}`, content: s.step, duration_minutes: 0,
  }));
  if (steps.length === 0) return null; // schema bắt buộc có steps -- loại công thức thiếu hướng dẫn

  const diets = r.diets || [];
  const dietaryFlags = new Set(inferDietaryFlags(ings));
  const tags = new Set(r.dishTypes || []);
  for (const d of diets) {
    if (DIET_TO_DIETARY_FLAG.has(d)) dietaryFlags.add(d);
    else tags.add(d);
  }

  const nutrients = r.nutrition?.nutrients;
  return {
    _id: "spoon_" + r.id,
    name: r.title,
    slug: normalize(r.title).replace(/\s+/g, "-"),
    cook_time_minutes: r.readyInMinutes || 30,
    difficulty: ings.length <= 5 ? "EASY" : ings.length <= 9 ? "MEDIUM" : "HARD",
    tags: [...tags],
    dietary_flags: [...dietaryFlags],
    ingredients: ings,
    steps,
    servings: r.servings || 1,
    nutrition: {
      caloriesPerServing: findNutrient(nutrients, "Calories"),
      proteinPerServing: findNutrient(nutrients, "Protein"),
      carbPerServing: findNutrient(nutrients, "Carbohydrates"),
      fatPerServing: findNutrient(nutrients, "Fat"),
      incomplete: false,
    },
    image_url: r.image || null,
    video_url: null,
    created_at: "2026-09-03T00:00:00Z",
  };
}

// "balanced" không phải giá trị diet hợp lệ của Spoonacular -- nhóm VĐV/nhanh dùng maxReadyTime+sort thay vì diet.
const GROUPS = [
  { label: "chay", params: { diet: "vegetarian" } },
  { label: "gymer/high-protein", params: { diet: "high protein" } },
  { label: "VĐV/nhanh", params: { maxReadyTime: "30", sort: "healthiness" } },
];
const PER_GROUP = 10;
// Xin dư ứng viên: nhiều công thức Spoonacular thiếu analyzedInstructions (bị loại ở transform())
// -- xin ít sẽ hụt số lượng nhóm (đã thấy thật: nhóm "high protein" chỉ còn 2/10 nếu chỉ xin 10).
const SEARCH_POOL = 25;
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function main() {
  const seen = new Set();
  const out = [];
  for (const { label, params } of GROUPS) {
    const qs = new URLSearchParams({ apiKey: API_KEY, number: String(SEARCH_POOL), ...params });
    const list = (await (await fetch(`${API}/complexSearch?${qs}`)).json()).results || [];
    let n = 0;
    for (const item of list) {
      if (n >= PER_GROUP) break;
      if (seen.has(item.id)) continue;
      seen.add(item.id);
      const infoQs = new URLSearchParams({ apiKey: API_KEY, includeNutrition: "true" });
      const full = await (await fetch(`${API}/${item.id}/information?${infoQs}`)).json();
      const recipe = transform(full);
      await sleep(150);
      if (!recipe) continue;
      out.push(recipe);
      n++;
    }
    console.log(`${label}: ${n} món`);
  }
  const coverage = out.flatMap((r) => r.ingredients)
    .filter((i) => byNorm.has(i.normalized_name)).length;
  const total = out.reduce((s, r) => s + r.ingredients.length, 0);
  console.log(`Tổng ${out.length} công thức, ${total} nguyên liệu, ` +
    `khớp catalog ${coverage}/${total} (${total ? Math.round((coverage / total) * 100) : 0}%)`);
  writeFileSync(resolve(ROOT, "recipe/src/main/resources/seed/recipes-spoonacular.json"),
    JSON.stringify(out, null, 2) + "\n", "utf8");
}
main();
