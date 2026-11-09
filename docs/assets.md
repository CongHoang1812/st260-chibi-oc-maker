# Ảnh xuất từ Figma

File Figma: `OUCDw6CTtEW3cw3iPNNcqc` (xem `docs/APP_BRIEF.md`). Figma đổi node nào thì xuất lại đúng file tương ứng.

| File | Node | Cách xuất |
|---|---|---|
| `drawable-xxhdpi/bg_lucky_gift_frame.webp` | `9:184` (ảnh gốc trong node) | Ảnh gốc 1280×1192, cắt bỏ tab tiêu đề phía trên (38dp), xoá vết hai cạnh tab còn sót, thu về 960×780 (320×260dp @3x), WebP lossless. Cửa sổ giữa trong suốt, nền sáng vẽ bằng `bg_lucky_gift_window.xml` |
| `drawable-xxhdpi/bg_lucky_gift_result.webp` | `10:409` | Xuất PNG @3x (nền đục), tự tạo alpha bo góc 40dp, WebP lossless |
| `drawable-nodpi/img_lucky_gift.webp` | `2:631` (ảnh gốc trong `2:628`) | Ảnh gốc 512×512, thu về 192×192, WebP q88 |
| `drawable/bg_lucky_gift_start.xml` | `2:668` | Path SVG của Figma chuyển sang VectorDrawable, bỏ bóng 1dp; chữ START là `@string/lucky_gift_start` |

Vẽ bằng XML theo số đo Figma, không xuất ảnh: `bg_lucky_gift_title.xml` (`2:673`, `10:403`), `bg_lucky_gift_button.xml` (`2:628`), `bg_lucky_gift_window.xml` (`2:647`), `bg_lucky_gift_cell.xml`.

Chưa có trong Figma, đang dựng tạm: `drawable/ic_lock.xml` (icon ô khoá).

## Ảnh dùng chung (giai đoạn 3, 2026-11-10)

Icon và glyph sinh từ SVG của Figma bằng `scripts/svg2vd.py` (viền trong / ngoài qua mask, bóng 1px) và ghép nền nút bằng `scripts/compose_icon.py`. Soát bằng mắt khi không có máy: `scripts/render_vd.py` dựng PNG từ VectorDrawable. Figma đổi glyph thì tải lại SVG của node rồi chạy lại 2 script, không sửa tay phần glyph.

| File | Node | Ghi chú |
|---|---|---|
| `drawable/ic_back.xml` | `35:22` (glyph), nền `35:19` | Nút icon 44dp = nền nút Figma + glyph, màu nền dùng `@color` |
| `drawable/ic_settings.xml` | `2:1006`, nền `2:1004` | như trên |
| `drawable/ic_download.xml` | `2:1773`, nền `2:1770` | như trên |
| `drawable/ic_archive.xml` | `41:23`, nền `2:1430` | như trên (My creation ở Successful) |
| `drawable/ic_info.xml` | `2:442`, nền `2:439` | như trên |
| `drawable/ic_close.xml` | `2:448`, nền `2:446` | như trên (đóng dialog) |
| `drawable/bg_gift_button.xml` | nền `2:496` | Chỉ phần nền nút, dùng cho nút Random cả nhân vật |
| `drawable/ic_share.xml` | `2:1761` | Glyph 24 cho `RoundIconButton` |
| `drawable/ic_home.xml` | `2:1425` | Glyph nhiều màu (mái đỏ) |
| `drawable/ic_rotate_right.xml` | `2:1443` | Re-edit ở Successful |
| `drawable/ic_reset.xml` | `2:482` | Reset ở Customize |
| `drawable/ic_trash.xml` | `2:1767` | Xoá ở chi tiết / My creation |
| `drawable/ic_none.xml`, `drawable/ic_dice.xml` | `2:506`, `2:509` | Glyph cam trong ô None / xúc xắc |
| `drawable/img_add_image.xml` | `2:1198` | Dấu + cam 24dp ở Decor editor |
| `drawable-nodpi/img_dice_gift.webp` | `2:499` (ảnh gốc) | 512 → 96px (32dp @3x) |
| `drawable-nodpi/img_intro_1.webp`, `img_intro_3.webp` | `2:1148`, `2:1160` (ảnh gốc) | Cắt object-cover 360×560, không kèm lớp mờ (layout có `bg_intro_fade`) |
| `drawable-nodpi/img_intro_2.webp` | `2:1155` | Ghép lại @2x: nền blur 1px + thẻ Lucky Gift có bóng; chữ LUCKY GIFT / START nằm trong ảnh minh hoạ |
| `drawable-nodpi/img_permission.webp` | `2:1168` (ảnh gốc) | Khung 300, ảnh 260 đặt ở x 20, @3x; chưa có bóng mờ dưới chân (mix-blend overlay) |
| `drawable-nodpi/img_empty_character.webp` | `2:1532` (ảnh gốc) | 200dp @3x |

Vẽ bằng XML theo số đo Figma, không xuất ảnh: `bg_tavern.xml` (gradient nền chung `#E7F1E5 → #95CBE5`), `bg_intro_fade.xml` (`2:1148`), `bg_part_item.xml` / `bg_part_item_selected.xml` (`2:1200` / `2:1219`), `bg_template_item.xml` (`2:463`), `bg_creation_card.xml` (`2:1560`).

Chưa làm, để cùng màn tương ứng: ảnh Splash (`2:1091`), nút Move panel (`ic_arrow_*`, `ic_rotate_*`, `ic_zoom_*`, Figma `2:931` màu xanh), glyph `ic_flip` / `ic_move` (dùng chung với tay nắm sticker trong `DecorCanvasView`), các khung dialog (`bg_dialog_paper`, `bg_rate_dialog`, `bg_instruction_paper`, chờ Figma).

## Màn Home (2026-11-10)

| File | Node | Ghi chú |
|---|---|---|
| `drawable-nodpi/img_home_bg.webp` | `2:1003` (ảnh nền gốc) | Nền biển, giữ kích thước gốc 736×1312 |
| `drawable-nodpi/img_logo.webp` | `21:29` (ảnh gốc) | Logo CHIBI OC MAKER 1024×384, layout 360×135 fitCenter. Splash có thể dùng lại |
| `drawable-nodpi/img_home_maker.webp` | `2:1012` (ảnh gốc) | Cắt theo khung Figma: ảnh 115.01%, lệch -11.25% / -7.43%, ra 480×480 (160dp @3x) |
| `drawable-nodpi/img_home_cosplay.webp` | `2:1016` (ảnh gốc) | Rộng 100%, cao 128.51%, lấy phần trên |
| `drawable-nodpi/img_home_lucky.webp` | `2:1014` (ảnh gốc) | object-cover cả ảnh |
| `drawable-nodpi/img_home_creation.webp` | `2:1018` (ảnh gốc) | Ảnh 129%, lệch -18.69% / -28.81% |

Đã bỏ `img_home_maker_bg.webp` (thẻ Figma chỉ có 1 ảnh). Dải màu đáy thẻ: `home_fade_*` theo Figma, bắt đầu ở 64.6%.
