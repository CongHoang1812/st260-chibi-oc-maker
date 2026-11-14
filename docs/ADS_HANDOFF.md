# Bàn giao gắn ads – ST260 Chibi Dream: Doll Dress Up

Tài liệu ads gốc: sheet "Kịch bản ads 110426" (team MONET).
Package `com.chibi.dream.cute`, nhánh giao diện `reskin/st260-ui`.

Tài liệu có 2 phần:

- **Phần A** cho chủ app: app chạy thế nào khi dùng, viết bằng lời thường, để trả lời câu hỏi của người gắn ads.
- **Phần B** cho người gắn ads: file nào, dòng nào, những chỗ dễ làm hỏng.

Cuối tài liệu là **danh sách câu hỏi còn mở**, mỗi câu ghi rõ ai trả lời.

Số dòng ghi theo code ngày 2026-11-14. Code sửa sau đó thì số dòng có thể lệch, tìm theo tên hàm.

---

## Phần A – App chạy thế nào (cho chủ app)

### Thứ tự màn khi mở app

- **Lần đầu cài app:** Splash → Language → Intro (3 trang) → Permission → Home.
- **Các lần sau:** Splash → Intro → Home. Intro hiện **mỗi lần mở app**, Language và Permission chỉ hiện lần đầu.
- Splash hiện cố định 2,5 giây rồi chuyển màn.

### Home có 4 thẻ và nút Setting

| Thẻ | Đi đến |
|---|---|
| Maker | Chọn nhân vật (Category) → Customize → Background (Decor) → Success |
| Cosplay | Cosplay Chibi → Cosplay Play (10 phút) → chấm điểm → Cosplay Success |
| Lucky (Random) | Màn Random: bấm Random ra nhân vật ngẫu nhiên, bấm Edit để sửa tiếp ở Customize |
| My Creation | Danh sách ảnh đã lưu, chia 2 tab: Character và Design |

- Maker, Cosplay và Lucky cần mạng. Mất mạng thì hiện dialog **No Internet** thay vì chuyển màn.
- Bấm Back ở Home: lúc thì hiện dialog **Rate**, lúc thì thoát app.

### Từng nút làm gì

**Customize** (tạo nhân vật):
- **Back:** nếu đã sửa gì thì hỏi "Thoát không lưu?", chưa sửa thì quay lại luôn.
- **Reset:** hỏi lại trước khi xoá về mặc định.
- **Random All** (xúc xắc to): hiện dialog **Watch Ad**, xem xong thì random cả nhân vật.
- **Ô xúc xắc nhỏ đầu mỗi danh sách:** random riêng bộ phận đó, không hỏi gì.
- **Part có ổ khoá:** bấm vào thì hiện dialog **Watch Ad** để mở khoá.
- **Lucky Gift** (hộp quà): vòng quay mở khoá part. Có lượt quay free, hết lượt free thì xem ads để quay thêm.
- **Next:** lưu nhân vật rồi sang Background.

**Cosplay Play** (dùng chung màn Customize):
- Đếm ngược 10 phút.
- Bấm Next hoặc hết giờ thì chấm điểm và sang Cosplay Success.
- Back luôn hỏi "Thoát?".

**Background (Decor)** (trang trí: nền, sticker, bong bóng thoại, chữ):
- **Back:** nếu đã sửa thì hỏi "Thoát?".
- **Nút Save** (biểu tượng tải xuống, góc phải trên): lưu ảnh rồi sang Success.
- Màn này **không có nút Reset**.

**Success** (lưu xong):
- Có các nút Re-edit, Home, My Creation, Share, Download.
- Back của hệ thống làm giống nút Home.
- **Re-edit:**
  - Sau Maker: quay lại Background.
  - Sau Cosplay: **chơi lại** đề vừa chơi. Đây chính là "Play again" trong tài liệu ads.

**Random (Lucky Roll):**
- Bấm Random là ra nhân vật ngay, hiện không có ads.
- Bấm Edit để sửa nhân vật đó ở Customize.

**Cosplay Chibi:**
- Random đổi đề bài, Cosplay vào chơi, nút (i) mở hướng dẫn.

**My Creation:**
- Bấm vào ảnh để mở Detail.
- Nút bút chì để sửa tiếp, đi thẳng vào Customize.
- Giữ lâu để chọn nhiều ảnh, sau đó Xoá, Share hoặc Download được.
- Tab Design có thêm nút xuất sticker sang Telegram và WhatsApp.

**Detail:**
- Có Back, Xoá, Share, Download, Edit.
- Edit đi vào màn sửa.

### Những chỗ app đã có "Watch Ad"

Hiện **chưa có ads thật**: bấm Watch Ad là được thưởng ngay. Có 3 chỗ:

1. Mở khoá part có ổ khoá (Customize).
2. Quay thêm Lucky Gift khi hết lượt free (Customize).
3. Random All (Customize).

### Những lúc người dùng rời app rồi quay lại

Liên quan ads **open resume**: những lúc này **không được** hiện ads.

- Share ảnh, share app.
- Chọn ảnh từ máy làm nền (Decor).
- Hộp cấp quyền (Permission, Download).
- Mở Cài đặt app.
- Mở Play Store (Rate), gửi email góp ý.
- Xuất sticker sang Telegram / WhatsApp.

---

## Phần B – Chi tiết kỹ thuật (cho người gắn ads)

### Tổng quan dự án

- Kotlin, XML + ViewBinding (không dùng Compose), MVVM, Hilt, Navigation, một Activity.
- Nên làm `AdsManager` dạng `@Singleton` rồi inject vào Fragment. Quy ước chung của dự án ở `CLAUDE.md`.
- Chưa có thư viện ads nào trong `app/build.gradle.kts` và `gradle/libs.versions.toml`.
- Có `app/google-services.json` nhưng **chưa thêm plugin Firebase** vào gradle. Muốn tracking hoặc Remote Config thì phải thêm trước.
- Bản release bật R8: keep rule thêm vào `app/src/main/keepRules/rules.keep` nếu thư viện không tự kèm.
- Quy ước layout:
  - Không hardcode màu, kích thước, chữ: dùng `@color`, `@dimen`, `@string`.
  - Chuỗi mới thêm vào **mọi** `values-*/strings.xml`.
  - Dimen mới thì xem cả `values-sw600dp/` và `values-sw800dp/`.

### Tên màn trong tài liệu ads ↔ code

Đường dẫn tính từ `app/src/main/java/com/chibi/dream/cute/feature/`.

| Tài liệu ads | Fragment | Layout |
|---|---|---|
| Splash | `splash/SplashFragment.kt` | `fragment_splash.xml` |
| Language | `language/LanguageFragment.kt` | `fragment_language.xml` |
| Intro | `intro/IntroFragment.kt`, trang trong `intro/IntroPageAdapter.kt` | `fragment_intro.xml`, `item_intro_page.xml` |
| Permison | `permission/PermissionFragment.kt` | `fragment_permission.xml` |
| Home | `home/HomeFragment.kt` | `fragment_home.xml` |
| Category | `maker/template/TemplateSelectFragment.kt` | `fragment_template_select.xml` |
| Customize | `maker/customize/CustomizeFragment.kt` (mode MAKER) | `fragment_customize.xml` |
| Background | `decor/editor/DecorEditorFragment.kt` | `fragment_decor_editor.xml` |
| Success | `success/SuccessFragment.kt` (source MAKER) | `fragment_success.xml` |
| Random | `luckyroll/LuckyRollFragment.kt` | `fragment_lucky_roll.xml` |
| Cosplay Chibi | `cosplay/CosplayStartFragment.kt` | `fragment_cosplay_start.xml` |
| Cosplay Chibi Play | `CustomizeFragment` với `ARG_MODE = COSPLAY`, destination `cosplayPlayFragment` | `fragment_customize.xml` |
| Cosplay Chibi Success | `SuccessFragment` với source COSPLAY | `fragment_success.xml` |
| My creation | `creation/MyCreationFragment.kt` | `fragment_my_creation.xml` |
| Detail Image | `creation/CreationDetailFragment.kt` | `fragment_creation_detail.xml` |

Luồng điều hướng: `app/src/main/res/navigation/nav_main.xml`.

### Chỗ đặt khung native

Đo theo thiết kế 360 × 800. Nội dung các màn bắt đầu dưới status bar, nên y = y Figma − 44.

| Màn | Đáy hiện tại | Cách đặt |
|---|---|---|
| Language | `rv_languages` kéo tới đáy | Nối đáy `rv_languages` vào đỉnh khung ads. Danh sách vẫn cuộn. Tài liệu: nút "ads" của native đặt phía trên. |
| **Intro** | Ảnh cao 560dp, nút Next / chấm trang ở y 640 | **Không đủ chỗ** cho native vừa. Phải thu nhỏ `intro_image_h` / `intro_controls_top`, hoặc làm layout riêng cho trang 1 và trang 3. Cần chủ app duyệt. |
| **Permission** | Nội dung xếp từ trên: ảnh 300dp → 2 hàng quyền → Continue ở y khoảng 600 | **Không đủ chỗ** cho native to, sẽ đè lên hàng quyền và nút Continue. Phải thu nhỏ `permission_image` và đặt Continue ngay trên khung ads. Cần chủ app duyệt. |
| Home | 2 hàng thẻ kết thúc ở y khoảng 570 | Đặt khung ở đáy, còn khoảng 185dp. |
| Category | `rv_templates` kéo tới đáy | Collapsible ở đáy: nối đáy lưới vào khung. Native nhỏ `native_category`: màn không có tiêu đề, nên đặt dưới hàng nút Back, đẩy `template_grid_top` xuống. |
| Customize / Cosplay Play | `panel` cao cố định 260dp ở đáy, vùng vẽ co giãn | Đặt khung dưới `panel`, nối đáy `panel` vào khung. Vùng vẽ tự nhỏ lại. `reference_card` (ảnh mẫu cosplay) bám đỉnh `panel` nên tự đi theo. |
| Background | `panel` (264 + 56dp) ở đáy, vùng vẽ co giãn | Giống Customize. **Xem mục "Bàn phím ở Decor" bên dưới.** |
| Success / Cosplay Success | Nút Download kết thúc ở y khoảng 480 | Đặt khung ở đáy, còn nhiều chỗ. |
| Random | Nút Random ở y khoảng 480 | Đặt khung ở đáy. |
| Cosplay Chibi | Nút ở y khoảng 450 | Đặt khung ở đáy. |
| My creation | `sticker_bar` (Telegram / WhatsApp) ở đáy, chỉ hiện ở tab Design | Đặt khung ở đáy, nối `sticker_bar` lên trên khung. Native nhỏ `native_creation` "dưới title, trên 2 tab": đẩy `creation_body_top` xuống, 2 tab và lưới đi theo. |
| Detail | Nút Edit ở y khoảng 480 | Đặt khung ở đáy. |

- Customize và Decor dùng `ShimmerFrameLayout` làm khung ngoài cùng, nên đặt khung ads **bên trong** `content`.
- Màu và font cho layout native: `res/values/colors.xml`, font Rammetto One. Tài liệu ads yêu cầu nút CTA gần màu chủ đạo, nền gần nền màn.

### Chỗ gắn inter

Mỗi dòng là một nút trong tài liệu ads. Đường dẫn tính từ `feature/`.

| Màn | Nút trong tài liệu | Code |
|---|---|---|
| Splash | Tải xong → inter → màn tiếp | `splash/SplashFragment.kt:34-49`: đang `delay(SPLASH_MS = 2500)` rồi `navigate`. Đổi thành "chờ ads hoặc hết thời gian". |
| Permission | Continue | `permission/PermissionFragment.kt:57` → `viewModel.completeOnboarding()` |
| Home | My Creation | `home/HomeFragment.kt:41` |
| Category | Back | `maker/template/TemplateSelectFragment.kt:41`. Chỉ có nút Back trên màn, **chưa bắt back hệ thống** nên phải thêm `OnBackPressedCallback`. |
| Category | Bấm item | `maker/template/TemplateSelectFragment.kt:86` (`navigateSafe` sang Customize) |
| Customize | Back | `maker/customize/CustomizeFragment.kt:241` `onBack()`, dùng chung cho nút và back hệ thống. Có dialog xác nhận, kết quả ở dòng 221-223 (`KEY_EXIT`). |
| Customize | Reset | `maker/customize/CustomizeFragment.kt:199-201` hiện dialog, kết quả ở dòng 224-226 (`KEY_RESET`). |
| Background | Save | `decor/editor/DecorEditorFragment.kt:128-133` |
| Background | Reset | **Màn không có nút Reset**: xem câu hỏi mở. |
| Success | Home, My Creation | `success/SuccessFragment.kt:65` `goHome()` (back hệ thống cũng gọi hàm này, dòng 68), `:64` My Creation |
| Random | Random (từ lần thứ 2) | `luckyroll/LuckyRollFragment.kt:51` `requestRoll()` |
| Cosplay Chibi | Back | `cosplay/CosplayStartFragment.kt:41`. **Chưa bắt back hệ thống.** |
| Cosplay Play | Back, Next | Back: `CustomizeFragment.onBack()`, luôn hiện dialog thoát. Next: `CustomizeFragment.kt:185-189`. Sau Next là chấm điểm và popup điểm, rồi mới sang Success. |
| Cosplay Success | Back, Home, Play again | Back và Home: `goHome()`. Play again là `btn_re_edit`: `SuccessFragment.kt:60` `reEdit()`. |
| My creation | Bấm item, Edit | `creation/MyCreationFragment.kt:245` `openDetail()`, `:253` `openEdit()` |
| Detail | Edit | `creation/CreationDetailFragment.kt:53` `openEdit()` |

Các điểm cần biết khi gắn inter:

- **Inter khi có dialog xác nhận** (Back, Reset ở Customize; Back ở Decor): hiện inter **sau khi** người dùng bấm đồng ý, tức là trong listener kết quả. Như vậy người dùng bấm Cancel thì không bị inter.
- **Kiểm tra mạng:** nhiều nút đi qua `runWhenOnline` / `navigateOnline`, mất mạng thì hiện `NoInternetDialogFragment`. Kiểm tra mạng trước, chỉ hiện inter khi việc của nút thật sự chạy.
- **Màn chỉ bắt nút Back trên màn, chưa bắt back hệ thống:** `TemplateSelectFragment`, `CosplayStartFragment`, `LuckyRollFragment`, `CreationDetailFragment`. Nếu có inter khi back thì phải thêm `OnBackPressedCallback`.
- Tài liệu ghi "inter 30s hiện 1 lần": dùng một bộ đếm chung trong `AdsManager`.

### Chỗ gắn reward

Mọi dialog Watch Ad đi qua **một hàm**: `feature/dialog/ConfirmDialogFragment.kt:43-46` `finish()`, đã có `TODO(ADS)` ở đó. Khi `confirmed && ARG_WATCH_AD` thì hiện reward. Xem hết thì `sendResult(true)`, tắt giữa chừng thì `sendResult(false)`.

| Dialog | Tạo ở | Nhận kết quả |
|---|---|---|
| Random All (`reward_random`) | `ConfirmDialogs.random()` | `CustomizeFragment.kt:227-229` → `viewModel.randomAll()` |
| Mở khoá part | `ConfirmDialogs.unlockPart()` | `CustomizeViewModel.kt:545` `onUnlockPartResult()`, có `TODO(ADS)` ở dòng 550 |
| Lucky Gift quay thêm | `ConfirmDialogs.luckyGiftAd()` | `maker/customize/LuckyGiftDialogFragment.kt:59-61` → `spinLuckyGift(withAd = true)` |

- Màn Random có cờ `AppConfig.LUCKY_ROLL_REQUIRES_AD` (đang `false`) để bắt xem reward trước khi random. Tài liệu ads dùng inter ở đây, không phải reward.
- **Lỗi có sẵn:** nếu bật cờ này lên `true` thì listener nhận kết quả dialog ở `LuckyRollFragment.kt:44-46` đang bị comment, nên random sẽ không chạy. Phải bỏ comment.

### Open resume

- Gắn ở mức app (`Application` hoặc `ProcessLifecycleOwner`), tài liệu ghi "khi người dùng quay lại app".
- **Không hiện** khi quay về từ các chỗ sau:

| Chỗ | Code |
|---|---|
| Share ảnh | `core/ui/ShareImages.kt:36` |
| Share app, mở link | `feature/setting/SettingFragment.kt:66, 76` |
| Chọn ảnh từ máy | `feature/decor/editor/DecorEditorFragment.kt:72, 439` |
| Cấp quyền | `feature/permission/PermissionFragment.kt:26, 30`, `core/ui/GalleryPermission.kt:28` |
| Mở Cài đặt app | `core/ui/GalleryPermission.kt:56` |
| Rate / email | `feature/dialog/RateUsDialogFragment.kt:134` |
| Sticker Telegram / WhatsApp | `core/ui/StickerIntents.kt:40`, `feature/creation/MyCreationFragment.kt:60` |

- Cũng không hiện ngay sau khi inter hoặc reward vừa đóng, và khi đang ở Splash.

### Những chỗ dễ làm hỏng

1. **Đổi ngôn ngữ không khởi động lại Activity.** Manifest khai báo `configChanges="locale|layoutDirection"`. `MainActivity.onConfigurationChanged` tự inflate lại màn đang hiện, và `LanguageFragment.onConfirmed` đổi ngôn ngữ **sau khi** chuyển màn. Ads đã gắn vào view cũ sẽ mất, nên phải gắn lại trong `onViewCreated`. **Không bỏ** khai báo `configChanges` (bỏ thì màn hình nháy đen).
2. **Thanh điều hướng luôn ẩn.** Người dùng vuốt từ đáy thì thanh hiện tạm và **đè lên** nội dung (`core/ui/WindowExt.kt` `applyNavigationBarMode`). `MainActivity` không chừa đáy cho thanh này. Khung ads sát đáy có thể bị che hoặc bị bấm nhầm, nên chừa một khoảng nhỏ.
3. **Bàn phím ở Decor.** Khi gõ chữ, `DecorEditorFragment` đẩy `panel` lên trên bàn phím (khoảng dòng 247-255). Hàm này tính theo khoảng cách từ `panel` tới đáy màn. Có khung ads dưới `panel` thì phép tính sai, nên ẩn khung ads khi bàn phím mở, hoặc trừ chiều cao khung ads trong phép tính.
4. **Inset trên.** Mỗi màn tự cộng status bar bằng `applyStatusBarPadding()` lên `content`. Native đặt ở đỉnh màn (Category, My creation) phải nằm trong `content` để không bị status bar che.
5. **Dialog trong app** dùng `BaseDialogFragment` toàn màn hình. Native collapsible khi mở rộng sẽ nằm dưới dialog, đây là bình thường.
6. **Tracking event:** tài liệu ghi `click_item_<position>` và `click_item_<position>_done`. `position` là khoá template trong API (`data1`…`data5`), tức `CharacterTemplate.id`.

---

## Câu hỏi còn mở

| # | Câu hỏi | Ai trả lời |
|---|---|---|
| 1 | Dùng thư viện ads nào: AdMob trực tiếp hay thư viện riêng của team? Có mediation không? | MONET |
| 2 | ID ads thật cho từng vị trí. Cột "Ad ID" trong sheet đang ghi mô tả chứ chưa có ID. | MONET |
| 3 | "Inter 30s hiện 1 lần" là đếm chung mọi inter hay riêng từng vị trí? Inter splash có tính vào không? | MONET |
| 4 | "Native cl2 20s hiện 1 lần" nghĩa là tự làm mới sau 20 giây? | MONET |
| 5 | Có bật / tắt từng vị trí ads bằng Remote Config không? | MONET |
| 6 | Background: tài liệu ghi inter khi bấm Reset, nhưng màn không có nút Reset. Bỏ dòng này hay phải thêm nút? | MONET + chủ app |
| 7 | Background: "Sửa lại ảnh maker sang phải to" là sửa gì: dời nhân vật sang phải, hay phóng to nhân vật? | MONET |
| 8 | Customize: hiểu "Bỏ random all" (dòng inter) là **không** gắn inter vào Random All, vì nút này đã dùng reward (`reward_random`, dialog Watch Ad đã có). Hiểu vậy có đúng không? | MONET |
| 9 | Dòng Random trong sheet ghi trạng thái "thiếu": còn thiếu gì? | MONET |
| 10 | Intro và Permission phải thu nhỏ ảnh để có chỗ cho native. Ai sửa layout, có cần designer duyệt không? | Chủ app |
| 11 | Splash chờ ads tối đa bao lâu, hết giờ thì vào app luôn? | Chủ app (thường 5–10 giây) |
| 12 | Language mở lại từ Setting có hiện native không? | Chủ app |
| 13 | Xem reward mà tắt giữa chừng thì không thưởng, hiện thông báo gì? | Chủ app |
| 14 | Làm trên nhánh nào? Nên tách nhánh riêng từ `reskin/st260-ui` để tránh xung đột với phần giao diện. | Chủ app + người gắn ads |
