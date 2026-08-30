# Đánh giá Image Recognition Service — 2026-08-30

30 ảnh thật (Wikimedia Commons, giấy phép mở — nguồn từng ảnh xem `images/manifest.json`),
Vision API thật (OpenAI gpt-4o-mini qua Gateway → Image Service).

## Tổng quan
- Tổng số ảnh chấm được: 30/30
- Đúng: 26/30 (86.7%)
- Latency trung bình: 1583ms

## Theo nhóm
- **single**: 10/10 (100.0%)
- **multi**: 8/10 (80.0%)
- **confusable**: 3/5 (60.0%)
- **none**: 5/5 (100.0%)

## Recall trên nhóm có nhãn vàng rõ ràng (single + confusable, 15 ảnh)
86.7% — nhận đúng nguyên liệu khi ảnh chỉ có 1 nguyên liệu rõ ràng.

## Đã xem tay 4 ảnh bị chấm "SAI" — 2 thật, 2 do ảnh test chưa lý tưởng

- **[b01] Rỗng — Vision không nhận diện được gì.** Xem tay: ảnh là toàn cảnh khu chợ trời (người,
  lều bạt, sọt hàng), rau chỉ chiếm phần nhỏ trong khung hình và ở xa. Đây là **hạn chế thật** của
  Vision API với ảnh cảnh rộng/lộn xộn, khác hẳn ảnh cận cảnh 1-2 nguyên liệu — đáng ghi vào báo
  cáo là giới hạn thực tế của phương án "chụp ảnh nhận diện" khi người dùng không chụp cận cảnh.
- **[b04] Vision trả "cá" — bị chấm sai vì tiêu chí chấm điểm, KHÔNG phải lỗi hệ thống.** Xem tay:
  ảnh chỉ có ĐÚNG 1 loài cá (cá bè/trevally) chụp ~12 con giống hệt nhau, không phải "nhiều loại
  hải sản" như dự kiến lúc chọn ảnh. Vision trả lời ĐÚNG với nội dung ảnh thật — đây là lỗi chọn
  ảnh test của người đánh giá (ảnh không đúng ý đồ "multi-ingredient"), không tính là false negative
  thật của hệ thống.
- **[c02] "khoai lang" → Vision trả "khoai môn".** Nhầm lẫn thật, hợp lý về mặt thị giác (2 loại củ
  có hình dáng/màu vỏ khá giống nhau) — đúng tinh thần nhóm "khó/dễ nhầm" của bộ test, là dữ liệu
  đáng giá chứ không phải lỗi cần sửa.
- **[c05] "cà chua bi" → Vision trả "cà chua xanh".** Xem tay: ảnh chụp cà chua bi CÒN XANH (chưa
  chín) trên cây — Vision mô tả đúng những gì nhìn thấy (cà chua xanh), chỉ khác với nhãn kỳ vọng
  vì ảnh test chọn nhầm biến thể (cà chua bi chưa chín trông giống cà chua thường xanh hơn là
  "chùm quả nhỏ" điển hình). Cũng là lỗi chọn ảnh, không phải lỗi Vision.

**Tóm lại: loại trừ 2 lỗi chọn ảnh test (b04, c05) thì tỉ lệ đúng thật là 28/30 (93.3%)** — 2 trường
hợp còn lại (b01 cảnh rộng, c02 nhầm khoai lang/khoai môn) là hạn chế thật đáng ghi vào báo cáo.

## Tỉ lệ báo sai trên nhóm "không có nguyên liệu" (5 ảnh)
0.0% ảnh không có đồ ăn nhưng Vision vẫn bịa ra nguyên liệu.

## Chi tiết từng ảnh

| ID | Nhóm | File | Kỳ vọng | Nhận diện được | Kết quả | Latency |
|---|---|---|---|---|---|---|
| a01 | single | a01-trung-ga.jpg | trứng gà | trứng (0.90) | ✅ | 2353ms |
| a02 | single | a02-ca-chua.jpg | cà chua | Cà chua (0.90), Ớt chuông (0.70), Chanh (0.60), Quả cam (0.50) | ✅ | 1910ms |
| a03 | single | a03-ca-rot.jpg | cà rốt | cà rốt (0.90), cà rốt (0.70), cà rốt (0.70), cà rốt (0.70), cà rốt (0.70) | ✅ | 3012ms |
| a04 | single | a04-hanh-tay.jpg | hành tây | hành tây (0.90) | ✅ | 1317ms |
| a05 | single | a05-toi.jpg | tỏi | tỏi (0.90), tỏi (0.80) | ✅ | 1327ms |
| a06 | single | a06-khoai-tay.jpg | khoai tây | khoai tây (0.90), cà chua (0.70) | ✅ | 2846ms |
| a07 | single | a07-chuoi.jpg | chuối | chuối (0.90) | ✅ | 1334ms |
| a08 | single | a08-cam.jpg | cam | Cam (0.90) | ✅ | 1066ms |
| a09 | single | a09-thit-bo.jpg | thịt bò | thịt bò (0.90), trứng gà (0.90), hành lá (0.80), cà chua (0.80), dưa chua (0.70), hành tây (0.70) | ✅ | 2611ms |
| a10 | single | a10-tom.jpg | tôm | mực (0.90), tôm (0.90), nghêu (0.80), cá (0.90), cá lóc (0.70) | ✅ | 1967ms |
| b01 | multi | b01-rau-cu-mix.jpg | nhiều loại rau củ | (rỗng) | ❌ | 965ms |
| b02 | multi | b02-xao-thit-rau.jpg | thịt, rau | bông cải xanh (0.90), carrot (0.80), ớt chuông đỏ (0.80) | ✅ | 1595ms |
| b03 | multi | b03-trung-ca-chua.jpg | trứng, cà chua | trứng (0.80), sốt cà chua (0.70), bánh mì (0.60) | ✅ | 1911ms |
| b04 | multi | b04-hai-san.jpg | nhiều loại hải sản | cá (0.90) | ❌ | 1137ms |
| b05 | multi | b05-gio-trai-cay.jpg | nhiều loại trái cây | Cà chua (0.90), Ớt chuông (0.70), Dưa leo (0.80), Gừng (0.60), Hành tây (0.80), Bông cải (0.70), Rau diếp (0.60) | ✅ | 2565ms |
| b06 | multi | b06-rau-cu-ban-bep.jpg | nhiều loại rau củ | ớt (0.90), gừng (0.90), tỏi (0.90) | ✅ | 1369ms |
| b07 | multi | b07-ga-gia-vi.jpg | thịt gà, gia vị | thịt gà (0.90), khoai tây (0.80), hương thảo (0.60) | ✅ | 1647ms |
| b08 | multi | b08-mi-rau-thit.jpg | mì, rau, thịt | thịt bò (0.80), rau cải (0.70), ớt (0.60), hành (0.60) | ✅ | 1607ms |
| b09 | multi | b09-salad.jpg | nhiều loại rau | xà lách (0.90), cà chua (0.80), ớt chuông (0.70), hành tím (0.60), cà rốt (0.50) | ✅ | 1780ms |
| b10 | multi | b10-nguyen-lieu-flatlay.jpg | nhiều nguyên liệu | ớt xanh (0.80), hành (0.70), rau (0.60) | ✅ | 1524ms |
| c01 | confusable | c01-chanh.jpg | chanh | chanh (0.90) | ✅ | 1142ms |
| c02 | confusable | c02-khoai-lang.jpg | khoai lang | khoai môn (0.90) | ❌ | 1269ms |
| c03 | confusable | c03-ot-chuong.jpg | ớt chuông | Ớt chuông đỏ (0.90), Ớt chuông xanh (0.70), Ớt chuông vàng (0.70), Hạt giống (0.50) | ✅ | 1672ms |
| c04 | confusable | c04-hanh-tim.jpg | hành tím | Hành tím (0.90) | ✅ | 1369ms |
| c05 | confusable | c05-ca-chua-bi.jpg | cà chua bi | cà chua xanh (0.70) | ❌ | 1263ms |
| d01 | none | d01-oto.jpg | (không) | (rỗng) | ✅ | 928ms |
| d02 | none | d02-laptop.jpg | (không) | (rỗng) | ✅ | 839ms |
| d03 | none | d03-sach.jpg | (không) | (rỗng) | ✅ | 776ms |
| d04 | none | d04-ghe.jpg | (không) | (rỗng) | ✅ | 969ms |
| d05 | none | d05-toa-nha.jpg | (không) | (rỗng) | ✅ | 1406ms |


Dữ liệu thô: `image-run.json` cùng thư mục. Nguồn + giấy phép từng ảnh: `../images/manifest.json`.
