# NinjaZaloModded (Android, module Xposed/LSPosed)

Ngôn ngữ: Kotlin. Tên APK build ra: `ninjazalo.apk`.

> Dùng mod có thể vi phạm điều khoản của Zalo và có nguy cơ bị khóa tài khoản. Tự chịu rủi ro.

## Giấy phép

AGPL-3.0.

## Trạng thái từng tính năng (đọc kỹ)

Code **chưa được biên dịch hay thử trên thiết bị** (môi trường tạo project không có Android SDK). Nếu build lỗi, gửi log để sửa.

| Tính năng | Trạng thái |
|---|---|
| Chặn endpoint ads / QoS / tracking / Firebase | Hook `java.net.URL.openConnection` và `OkHttpClient.newCall`. |
| Firebase Analytics `logEvent` | Chạy ngay nếu Zalo có Firebase. |
| Anti-Recall / Anti-Delete | Viết lại gói tin, có bộ nhớ đệm tin để hiện lại nội dung, ghi lịch sử `recalled.txt`. **Cần rule trỏ vào đúng method nhận gói tin.** Chưa chạy thử trên Zalo thật. |
| Chặn "Đã xem", không gửi "Đang nhập", ghi đè TTL | Engine sẵn sàng, **cần rule**. |
| Màn hình cài đặt | Mở app NINJAMOD. |
| Không làm | Mở khóa tính năng trả phí, spoof chữ ký, Chat Head, menu tùy biến, Google Drive backup. |

Class của Zalo Android bị obfuscate và đổi theo từng phiên bản, nên module không thể tự biết method nào cần hook. Bạn phải tìm 1 lần cho mỗi phiên bản Zalo.

## Build

Cách 1: Android Studio (JDK 17), mở thư mục này rồi Build > Build APK. Repo `https://api.xposed.info/` cần truy cập được.

Cách 2: đẩy lên GitHub, tab Actions > "Build ninjazalo.apk" > Run workflow, tải artifact `ninjazalo.apk`. Workflow dùng Gradle 8.7, không cần gradle wrapper.

APK được ký bằng debug key. Muốn phát hành, thay `signingConfig` trong `app/build.gradle.kts` bằng keystore của bạn.

## Cài đặt

1. Máy cần root + LSPosed (hoặc môi trường tương đương).
2. Cài `ninjazalo.apk`, bật module trong LSPosed, chọn phạm vi **Zalo** (`com.zing.zalo`, hãy kiểm tra package trên máy bạn nếu khác thì sửa `Keys.TARGET_PKG` và `res/values/arrays.xml`).
3. Mở app **NINJAMOD**, chỉnh cài đặt, Lưu, rồi đóng hẳn Zalo và mở lại.

## Tìm target hook

1. Bật **Ghi log mọi request** rồi dùng Zalo. Xem LSPosed log (tag `[NinjaZaloModded][net]`), copy phần URL quảng cáo/tracking vào **Danh sách chặn**.
2. Bật **Quét class/method khi mở Zalo**, mở Zalo, lấy file `Android/data/com.zing.zalo/files/ninjazalo-scan.txt`. Nếu tên bị obfuscate thì dùng jadx hoặc DexKit trên APK Zalo.
3. Điền method vào **rules.json** trong app (mẫu ở `rules.example.json`):
   - Anti-Recall: method xử lý gói tin chat nhận về. Dùng `"action": "antirecall"` và `"argIndex"` đếm từ 1.
   - Seen: method gửi danh sách tin đã đọc lên server -> `"action": "skip"`, `"configKey": "block_send_seen"`.
   - Typing: -> `"action": "skip"`, `"configKey": "enable_silent_typing"`.
   - TTL: method trả về TTL (int/long) -> `"action": "ttl"`, `"configKey": "enable_msg_ttl_override"`; giá trị TTL nhập trong app.
   - Muốn xem tham số thật: dùng `"action": "log"` rồi xem LSPosed log.
4. Điền **User ID** của bạn để phân biệt tin do chính bạn thu hồi/xóa (mặc định bỏ qua tin của mình).
5. Lịch sử tin thu hồi/xóa nằm ở `Android/data/com.zing.zalo/files/ninjazalo/recalled.txt`.

## Cấu trúc

`MainHook` (khởi tạo) · `Keys` / `HookConfig` (cài đặt) · `HookEngine` (hook theo rules.json) · `AntiRecall` (lọc gói tin) · `NetBlocker` (chặn request) · `Scanner` (tìm target) · `SettingsActivity` (màn hình cài đặt)
