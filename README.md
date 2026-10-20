# Minecraft Auto Clicker – White Edition

Ứng dụng Android (Kotlin) tự động chạm màn hình, có nút nổi kéo thả và đồng hồ bấm giờ.
Giao diện nền trắng, chữ đen, điểm nhấn xanh lá Minecraft. Không cần API key, server hay Android Studio.

## Tính năng
- Auto click theo mili giây (tối thiểu 20 ms), chế độ **liên tục** hoặc **giới hạn số lần**.
- Nút nổi (BẮT ĐẦU / DỪNG) kéo thả được + dấu **+** để chọn vị trí chạm.
- Dùng `AccessibilityService` + `GestureDescription`; chỉ chạy khi bạn bấm BẮT ĐẦU, tự dừng khi tắt dịch vụ.
- Đồng hồ bấm giờ phút:giây.phần trăm giây, độc lập với auto click, đúng giờ khi app xuống nền.
- Tùy chọn giữ màn hình sáng.
- Android 7.0 (API 24) trở lên.

> Nút nổi dùng cửa sổ overlay của dịch vụ Trợ năng nên **không cần** cấp thêm quyền "Hiển thị trên ứng dụng khác".

## Build APK bằng GitHub Actions (trên điện thoại)
1. Đăng nhập **github.com** bằng trình duyệt điện thoại (nên bật "Trang web cho máy tính" / Desktop site).
2. Bấm **+ → New repository**, đặt tên (ví dụ `mc-auto-clicker`), chọn **Public**, bấm **Create repository**.
3. Giải nén file ZIP trên điện thoại. Trong repo bấm **Add file → Upload files**, chọn **toàn bộ nội dung** bên trong thư mục `MinecraftAutoClicker`
   (gồm `app`, `gradle`, `.github`, `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `README.md`).
   Lưu ý: thư mục `.github` là thư mục ẩn; nếu trình duyệt không chọn được, hãy bật hiển thị file ẩn trong ứng dụng Files,
   hoặc tạo file `.github/workflows/build-apk.yml` thủ công bằng **Add file → Create new file** (gõ đường dẫn đầy đủ vào ô tên) rồi dán nội dung.
4. Chọn nhánh **main** và bấm **Commit changes**. Workflow sẽ tự chạy.
5. Vào tab **Actions** → chọn lần chạy **Build APK** mới nhất (hoặc **Run workflow** để chạy tay). Đợi dấu ✅ (khoảng 3–6 phút).
6. Kéo xuống mục **Artifacts**, bấm **MinecraftAutoClicker-debug-apk** để tải file ZIP, giải nén ra `app-debug.apk`.

## Cài đặt và cấp quyền
1. Mở `app-debug.apk`, cho phép "Cài đặt ứng dụng không rõ nguồn" nếu máy hỏi.
2. Mở app → **MỞ CÀI ĐẶT TRỢ NĂNG** → *Ứng dụng đã tải xuống / Dịch vụ đã cài* → **Minecraft Auto Clicker** → bật.
   (Vivo/Funtouch OS: *Cài đặt → Hỗ trợ tiếp cận → Dịch vụ đã cài đặt*.)
3. Nếu máy báo "Cài đặt bị hạn chế": *Cài đặt → Ứng dụng → Minecraft Auto Clicker → ⋮ → Cho phép cài đặt bị hạn chế*, rồi bật lại.
4. Trong app đặt tốc độ/chế độ → **HIỆN NÚT NỔI & CHỌN VỊ TRÍ** → kéo dấu **+** tới chỗ cần chạm trong Minecraft → bấm **BẮT ĐẦU** trên nút nổi. Bấm **DỪNG** để dừng ngay.

## Cấu trúc
- `app/src/main/java/com/mcclicker/white/` – `MainActivity`, `ClickerService`, `ClickPrefs`
- `app/src/main/res/` – layout, drawable, icon, strings
- `.github/workflows/build-apk.yml` – workflow build (JDK 17, Gradle 8.9, AGP 8.5.2, Kotlin 1.9.24, compileSdk 34)
- Workflow tự tạo `gradlew` và `gradle-wrapper.jar` ở bước "Generate Gradle Wrapper".
