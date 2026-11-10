# App brief: ST260_Chibi OC Maker

Nguồn sự thật của app này. Skill `maker-reskin` đọc file này trước khi làm bất cứ việc gì. Mục nào chưa có thì để trống, điền dần khi có (Figma, tính năng mới có thể đến sau).

## Định danh (script đã điền)

| Mục | Giá trị |
|---|---|
| Tên app | ST260_Chibi OC Maker |
| Mã dự án | ST260 |
| Package / applicationId | `com.conght.chibiocmaker` |
| Tiền tố class / theme | `ST260ChibiOCMaker` |
| APP_KEY trên server (đuôi API) | `ST279_AnimeOCMaker` |
| Máy chủ chính | (giữ như app nguồn) |
| Máy chủ dự phòng | (giữ như app nguồn) |

`APP_KEY` quyết định cả endpoint dữ liệu `api/app/<APP_KEY>` lẫn thư mục ảnh `public/app/<APP_KEY>/`. Đổi đuôi API = đổi `APP_KEY` trong `core/common/AppConfig.kt`.

## Bạn cần điền

### Thiết kế

- Figma URL: https://www.figma.com/design/OUCDw6CTtEW3cw3iPNNcqc/ST260_Chibi-OC-Maker?node-id=0-1&t=52aLSuHSoKfvx6EQ-1
- fileKey: `OUCDw6CTtEW3cw3iPNNcqc`
- Page chứa màn hình: `UI` (`0:1`). Page khác: `Logo Ft Sc` (`2:1817`, logo, feature graphic, ảnh store), `nope` (`73:18`, ảnh rời, không có frame màn hình)
- Chủ đề app (vd. anime, mèo, công chúa):
- Danh từ thay cho "D&D" trong chữ hiển thị:
- Font chính (tên + nguồn, cần giấy phép dùng thương mại): Rammetto One (Google Fonts, SIL OFL, `docs/licenses/OFL-rammetto_one.txt`), file `res/font/rammetto_one.ttf`, family `@font/app_font`. Chữ nội dung dialog: Figma dùng Inter → font hệ thống `sans-serif`.

### API

- Định dạng: `giống app mẫu` / `khác` (chạy `probe_api.py` nếu chưa chắc)
- Tài liệu API (đường dẫn file hoặc link):
- JSON mẫu thật (đường dẫn file):
- Có cần token / header không:
- Endpoint thêm cho tính năng mới (nếu có):

### Phát hành

- URL chính sách bảo mật:
- 
- Email góp ý:
- Ngôn ngữ (mặc định en, hi, es, fr, pt, in):
- Quảng cáo / Firebase (có / không, mã gì):

## Tính năng / màn hình mới

Những thứ app mẫu chưa có. Mỗi dòng một việc; Claude lập kế hoạch và hỏi trước khi làm (xem `references/new-feature.md` trong skill).

| Tính năng / màn | Mô tả (mở từ đâu, làm gì, lưu gì) | Figma node | Dữ liệu (API / local) | Trạng thái |
|---|---|---|---|---|
| Lucky Gift (vòng quay trong Customize) | 2/3 item của mỗi tab part bị khoá. Bấm nút quà ở Customize mở overlay LUCKY GIFT, quay để mở khoá part bất kỳ thuộc tab bất kỳ. Quay luôn, chưa cần xem quảng cáo. Sau khi quay đếm ngược 30:00, vẫn chạy tiếp khi thoát app. Code chưa có phần này và chưa có khái niệm khoá / mở phụ kiện | Start `2:551`, Congratulations `10:185`, Free `2:677` (có icon video, hiện chưa dùng quảng cáo), đếm ngược `2:805`; ảnh khung `9:184`, nền sáng `9:99`; đếm ngược ở nút quà cũng có trong `2:931` | Item lấy từ API có sẵn; part đã mở (theo template) và mốc quay kế tiếp lưu DataStore (`settings`) | xong logic + test; chưa chạy trên máy; ô khoá chờ Figma |

## Bỏ hoặc đổi so với app mẫu

Tính năng có sẵn nhưng app này không dùng hoặc hoạt động khác. Để trống nghĩa là giữ nguyên.

-

## Bảng màn hình ↔ Figma

Điền node id khi bắt đầu giai đoạn giao diện. Trạng thái: `chưa` / `đang làm` / `xong` / `chờ Figma`.

| Màn | Layout | Figma node | Trạng thái |
|---|---|---|---|
| Splash | `fragment_splash` | `2:1091` | xong layout (chưa xem trên máy) |
| Language | `fragment_language`, `item_language` | Lần đầu `2:1094`, từ Setting `35:18` | xong layout (chưa xem trên máy) |
| Intro | `fragment_intro`, `item_intro_page` | `2:1141`, `2:1149`, `2:1159` | xong layout (chưa xem trên máy) |
| Permission | `fragment_permission`, `item_permission_row` | `2:1167` | xong layout (chưa xem trên máy) |
| Home | `fragment_home` | `2:1003` | xong layout (chưa xem trên máy) |
| Setting | `fragment_setting` | `2:1020` | xong layout (chưa xem trên máy) |
| Chọn template | `fragment_template_select`, `item_template` | `2:457` | xong layout (chưa xem trên máy) |
| Customize + Move | `fragment_customize`, `view_move_panel`, `item_part`, `item_none_part`, `item_dice_part`, `item_color_dot`, `item_category_tab` | Customize `2:469`, Move `2:931` | xong layout (chưa xem trên máy) |
| Decor editor | `fragment_decor_editor`, `item_decor_cell`, `item_none_decor`, `item_sub_tab`, `item_font_chip`, `item_color_chip` | Nền ảnh `2:1182`, nền màu `2:1240`, sticker `2:1296`, speech `2:1358`, text `2:1452` | xong layout (chưa xem trên máy) |
| Successful | `fragment_success` | Maker `2:1421`, Cosplay `2:280`; thẻ điểm sao `2:321`, `2:330` | xong layout (chưa xem trên máy) |
| My creation + Detail | `fragment_my_creation`, `fragment_creation_detail`, `item_creation` | Character: rỗng `2:1520`, danh sách `2:1548`, chọn nhiều `2:1656`, chi tiết `2:1746`. Design: rỗng `2:1534`, danh sách `2:1614`, chọn nhiều `2:1705`, chi tiết `2:1775` | xong layout (chưa xem trên máy) |
| Cosplay | `fragment_cosplay_start`, `fragment_customize` (mode COSPLAY), `dialog_cosplay_score`, `dialog_instruction` | Start `2:186`, Instruction `2:423`, chơi `2:207`, điểm số `2:339` | xong layout (chưa xem trên máy) |
| Lucky roll | `fragment_lucky_roll` | Mặc định `2:155`, sau khi Random `2:169` | xong layout (chưa xem trên máy) |
| Dialog | `view_paper_dialog`, `dialog_confirm` (Exit / Reset / Random / Delete / No internet), `dialog_color_picker`, `dialog_rate_us`, `dialog_pack_name`, `dialog_loading` | Exit `258:18`, Reset `258:41`, Random `258:52`, No internet `258:63`, Delete `258:74`, Color picker `258:106`, Rate us 0–5 sao `258:108` `258:166` `258:186` `258:206` `258:226` `258:246` (section `258:406`). Chưa có: Pack name, Loading (dựng từ khung chung) | xong layout (chưa xem trên máy) |
| Trạng thái tải / lỗi | `view_error_state`, `item_skeleton_*`, `view_skeleton_image` | Không có frame, dựng từ token và component sẵn có | chưa |
| Lucky Gift (overlay trong Customize) | `dialog_lucky_gift`, `item_lucky_gift_cell`; nút quà `lucky_gift` / `btn_lucky_gift` trong `fragment_customize`; icon khoá `iv_lock` trong `item_part` | Start `2:551`, Congratulations `10:185`, đếm ngược `2:805`, nút quà `2:628` | xong bản đầu, chưa đối chiếu trên máy; ô khoá chờ Figma |

Màn mới thêm dòng vào cuối bảng này.

Ghi chú khi ghép frame:

- Tên frame trong Figma không đáng tin: nhiều frame trùng tên hoặc sai tên (`customzie`, `background / add img` trong section creation). Dùng node id ở bảng trên.
- Frame bị chồng, có vẻ là bản cũ, không dùng: `2:146` (bị `2:155` che, chữ còn ghi "your pony", thiếu nút Random), `2:1042` (bị `35:18` che, thiếu nút Save).
- `2:1091` tên là `home` nhưng là màn Splash (người dùng đã xác nhận).
- `35:18` có nút Save ở dưới danh sách; `fragment_language` hiện chỉ có `btn_check`, không có nút Save. Khi làm màn Language: đổi giao diện theo Figma, giữ `btn_check` để không đổi logic (người dùng đã đồng ý).

## Tiến độ giao diện chung (giai đoạn 3)

- Token (2026-11-09): xong. Màu theo Figma (`colors.xml`), font Rammetto One, dim dialog 0.5. Font mặc định của chữ trong Decor editor vẫn là Londrina (id `londrina` đã lưu trong thiết kế của người dùng).
- Component (2026-11-09): xong `WoodButtonDrawable` / `WoodButton` / `RoundIconButton` (nút Figma: khung trắng 4dp, lõi cam bo 12, bóng đáy), `PaperDialogContainer` (thẻ kem + tab tiêu đề cam, Figma 11:429). Chưa kiểm tra trên máy.
- Ảnh dùng chung (2026-11-10): xong nền chung, icon nút, glyph, khung ô, ảnh Intro / Permission / trạng thái rỗng (chi tiết ở `docs/assets.md`). Ảnh Splash, Move panel để làm cùng màn.
- Chưa làm: từng màn.

## Quyết định đã chốt

Ghi lại quyết định không suy ra được từ code, để phiên làm việc sau không phải hỏi lại.

- Token và component chung (2026-11-09), người dùng đã duyệt:
  - Dialog chung (Exit / Reset / Delete / Random / No internet, Rate us, Pack name, Loading, Color picker): đã có Figma (xem bảng màn hình). Không có nút X tròn.
  - Nút phụ (No / Cancel): làm theo design, Figma chỉ có nút cam nên cả hai nút đều cam, không dùng kiểu xám.
  - Dim mọi dialog 0.5; trạng thái nhấn thu nhỏ 95%, vô hiệu mờ 50%; toast nền cam chữ trắng; nền splash hệ thống `#FED2A5`; skeleton nền sáng `#D3E8F2`.
  - Cỡ chữ tiêu đề màn lấy số đo khi làm từng màn. Chữ tràn nút / tab (Rammetto rộng, nhất là fr) xử lý khi kiểm tra trên máy; tiếng Hindi dùng font hệ thống.

- Figma chưa có các dialog chung (dòng Dialog trong bảng màn hình); người dùng sẽ bổ sung sau, không tự đoán giao diện. Instruction và điểm số Cosplay đã có frame nên ghi ở dòng Cosplay. (2026-11-09)
- Có làm Lucky Gift: khoá 2/3 item mỗi tab part, quay để mở part bất kỳ của tab bất kỳ, chưa cần quảng cáo, đếm ngược 30:00 chạy tiếp khi thoát app. (2026-11-09)
- Panel Move theo Figma 2:931 (2026-11-10), người dùng đã chọn:
  - Bỏ nút zoom +/-; zoom bằng hai ngón tay trên nhân vật.
  - Thêm nút RESET: đưa vị trí / góc xoay / cỡ của bộ phận đang chọn về mặc định (`CustomizeViewModel.resetPartTransform`).
  - Đang Move vẫn hiện Home, NEXT, hàng công cụ, Random và nút quà như Figma.
  - Nút Back ở Customize dùng glyph Home như Figma, hành vi vẫn là quay lại (hỏi Exit nếu đã sửa).
- Quy tắc Lucky Gift (2026-11-09):
  - Mỗi lượt mở 5 part.
  - Mỗi tab mở 1/3 đầu danh sách, khoá 2/3 sau; part mặc định luôn mở.
  - Xúc xắc và Random cả nhân vật chỉ ra part đã mở.
  - Cosplay không khoá. Lucky roll vẫn random mọi part.
  - Khi Edit, part đang mặc được giữ; đổi sang part khác thì không chọn lại được nếu nó còn khoá.
  - Mở khoá theo từng template, đếm ngược chung toàn app.
  - Bấm ô khoá: không chọn ô đó, không có gì xảy ra. Chỉ mở overlay Lucky Gift bằng nút quà.
  - Hết đếm ngược hiện lại START, tạm chưa dùng frame Free.
  - Đã mở hết part thì ẩn nút quà.
  - Figma chưa có trạng thái ô khoá: dựng tạm bằng icon khoá và làm mờ ô, chờ Figma bổ sung.
- Decor editor theo Figma (2026-11-10):
  - Tab chính chuyển xuống thanh đáy (nút 40, chọn = cam), tab con nền đổi thứ tự thành Image, Color như Figma; mở editor vào Image trước.
  - Sticker / Speech: Figma không có hàng tab con, chỉ hiện khi API trả từ 2 pack trở lên.
  - Nút xác nhận (`btn_text_done`) cạnh ô nhập chữ đã có trong Figma (`282:83`): ô nhập và nút cao 36, dấu ✓ cam.
  - Tab Speech có hàng font + màu phía trên lưới (Figma `282:43` / `282:66`), font / màu / lưới cách nhau 8; tab Text cách nhau 16.
  - Nền trắng của vùng thiết kế là View riêng sau `DecorCanvasView`, để ảnh xuất ra vẫn trong suốt khi nền None (như trước).
- API ST279 đặt tên decor khác app mẫu (2026-11-10): khoá `sticker` / `speech bubble` và thư mục ảnh `bg/sticker`, `bg/speech bubble` (app mẫu: `stickers` / `speech_bubble`). `DecorConfigDto` đọc cả hai kiểu, `ApiMapper` ghép thư mục theo khoá có dữ liệu. Khi đổi sang API thật thì kiểm tra lại.
- Successful theo Figma (2026-11-10): Maker là ảnh vuông trắng rộng hết màn; Cosplay là thẻ polaroid trên nền cam có tia sáng, ảnh 208 và 3 sao nằm trong thẻ (bỏ huy hiệu sao và viền nâu cũ). Nút Download rộng 328.
- Dialog theo Figma (2026-11-10):
  - Nút dialog là nút viên 124×34 giống tab con editor (style `TabButton`), không phải nút chữ lớn.
  - Choose color đổi thành Yes (trái, chọn màu) / No (phải, đóng) như Figma, cùng thứ tự với các dialog xác nhận (trước là Cancel / Done).
  - Pack name và Loading chưa có Figma: Pack name dùng khung dialog chung (`PaperDialogContainer`), ô nhập giống ô nhập chữ Decor editor; Loading dùng thẻ kem chung.
  - Rate us giữ emoji / chữ / màu sao như cũ (đã khớp Figma), đổi khung sang thẻ kem chung rộng 312.
- My creation theo Figma (2026-11-10):
  - Tab Character / Design là nút viên canh giữa (cam / xám), bỏ kiểu tab giấy nhô lên.
  - Thẻ trắng 160 bo 4, ảnh phủ kín; xoá ở góc trái, sửa ở góc phải (chỉ Character); chọn nhiều thì checkbox ở góc phải.
  - Nút Add to Telegram (xanh dương) / Add to Whatsapp (xanh lá) là 2 variant mới của `WoodButton` (`telegram`, `whatsapp`).
  - Chi tiết dùng bố cục như Successful Maker (ảnh trắng rộng hết màn, Edit rộng 328).
  - Khoảng cách nút trên header `top_bar_action_gap` đổi 16 → 12 theo Figma.
- Onboarding theo Figma (2026-11-10):
  - Splash: ảnh minh hoạ + logo (dùng lại `img_logo` của Home). Đáy theo Figma `282:18`: vòng loading 44 xoay + "This action can contain ads" (Roboto 16, #01394D 80%), bỏ thanh loading và chữ Loading.
  - Hàng danh sách (`ListRow`, dùng chung Language / Permission / Setting): luôn nền trắng, chữ cam, chọn chỉ đổi radio (bỏ kiểu cả hàng tô cam).
  - Language từ Setting dùng cùng nền gradient, bỏ lớp tối; vẫn dùng `btn_check`, không thêm nút Save.
  - Tiêu đề Language / Permission (`Text.Title`) đổi sang cam; chữ Intro xanh đậm `#01394D`.
- Setting / Chọn template theo Figma (2026-11-10): nền gradient chung, bỏ lớp tối. Thẻ template thành ô trắng 160 bo 4, ảnh phủ kín, lưới cách 8 (bỏ khung giấy có bóng cũ).
- Cosplay / Lucky roll theo Figma (2026-11-10):
  - Nút Random có ảnh xúc xắc bên trái chữ: `WoodButton` thêm `app:woodIcon` (icon + chữ canh giữa thành một khối), style `RandomButton`.
  - Màn chơi Cosplay: đồng hồ chỉ còn viên "00:00" như Figma, ẩn thanh tiến trình (`progress_timer` vẫn còn trong layout, ẩn). Ảnh mẫu thành polaroid 80×96 nghiêng -4° lấn ra mép phải, có băng dính hồng góc trên-trái / dưới-phải (Figma `39:26`).
  - Instruction: thẻ kem + tab tiêu đề cam (`bg_dialog_title_tab.xml`), nút X cách thẻ 24. Popup điểm / ảnh mẫu: thẻ polaroid giống Successful Cosplay, % chữ nâu dưới ảnh.
  - Lucky roll: ô vuông rộng hết màn, Random rộng 328.
- Quyền (2026-11-10, đổi 3 lần, đây là bản chốt):
  - Màn Permission: Android 12 trở xuống chỉ dòng Storage, Android 13 trở lên chỉ dòng Notification. Bỏ quyền `READ_MEDIA_*` khỏi manifest (app không đọc ảnh máy).
  - Download (View / Success / My creation): Android 10+ lưu ngay; Android 9 trở xuống xin quyền ghi, từ chối 2 lần thì lần bấm sau hiện dialog mở Settings (`ConfirmDialogs.storageSettings`).
- Nút Next ở Customize khoá (mờ) khi ảnh nhân vật chưa lên đủ: lúc mới vào, đổi bộ phận, random (`CharacterCanvasView.onSettledChanged`).
- Thanh điều hướng hệ thống ẩn ở mọi màn, mọi thiết bị (bỏ `R.bool.hide_navigation_bar`), kể cả dialog, sau khi đóng bàn phím, khi quay lại app. Vuốt từ đáy hiện tạm rồi tự ẩn; lúc splash mở có thể thấy thanh một chút.
- Lucky Gift quay thêm bằng quảng cáo (2026-11-10): đang đếm ngược, nút ở vòng quay thành 🎬 FREE (Figma `2:677`, giờ chỉ hiện ở nút quà ngoài Customize); bấm thì hiện dialog "Watch an ad" (Cancel | Watch Ad). Xem xong được quay ngay một lượt, đồng hồ chạy tiếp, không đặt lại 30:00. Mỗi lượt đếm ngược chỉ được 1 lần quảng cáo; dùng rồi thì nút hiện giờ còn lại (Figma `2:888`), hết giờ mới quay START, lượt đếm ngược mới lại có FREE. Lưu mốc đã dùng ở DataStore (`lucky_gift_ad_used_for`) nên thoát app vẫn giữ. Chưa có Ads SDK nên Watch Ad cho quay luôn. Hiệu ứng quay: 5 cột cuộn kiểu máy slot, dừng lần lượt trái → phải; part trúng luôn dừng ở hàng giữa. Trong nút FREE có ⏱ giờ còn lại ngay dưới chữ (Figma `2:760` / `282:38`). Nút quà ngoài Customize: còn lượt (START / FREE) thì chấm đỏ, đã dùng quảng cáo thì hiện giờ. Lớp tối sau overlay 70% (Figma `2:759`), các dialog khác vẫn 50%.
- Intro: nền dưới ảnh vẽ riêng `#ADD5E5 → #95CBE5` để nối liền với lớp mờ trên mọi chiều cao màn.
- `APP_KEY` `ST279_AnimeOCMaker` chỉ là API mượn tạm vì app chưa có API thật. Chưa cần chạy `probe_api.py`; phải đổi sang API thật và kiểm tra lại trước khi phát hành. (2026-11-09)
