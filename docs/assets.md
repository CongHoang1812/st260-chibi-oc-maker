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
