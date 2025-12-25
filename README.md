## AutoTap Native Android App

Đây là project Android **native** (Kotlin) tách riêng, dùng để chạy **auto tap ngoài ứng dụng** bằng:

- **AccessibilityService** (mô phỏng gesture toàn hệ thống trên Android).
- **Overlay (bong bóng nổi)** hiển thị toolbar và các dot – tương tự logic trong app Flutter cũ, nhưng chạy ở cấp hệ thống.

### Tính năng chính (skeleton)

- Model `Dot` với các thuộc tính:
  - `actionIntervalTime` – chu kỳ tap (ms).
  - `holdTime` – thời gian giữ (ms).
  - `antiDetection` – bán kính jitter (random trong hình tròn).
  - `startDelay` – delay trước khi bắt đầu (ms).
  - `x`, `y` – toạ độ màn hình (px).
- `AutoTapAccessibilityService`:
  - Nhận danh sách dot.
  - Chạy vòng lặp auto tap với jitter giống logic `DotCubit` trong Flutter.
- `OverlayService`:
  - Tạo toolbar nổi có nút **Start/Stop** và **Add Dot**.
  - Gửi cấu hình dots + lệnh start/stop xuống `AutoTapAccessibilityService`.

### Cách mở project

- Mở folder `new` bằng **Android Studio** (*Open an Existing Project*).
- Chờ Gradle sync (project dùng Gradle Kotlin DSL).

### Quyền cần bật trên máy

1. **Overlay / Draw over other apps**:
   - Ứng dụng cần quyền vẽ nổi trên các app khác để hiển thị toolbar.
2. **Accessibility Service**:
   - Vào *Settings → Accessibility*.
   - Tìm app này trong danh sách dịch vụ trợ năng và **Enable**.

Không có hai quyền này thì app **không thể** tap ra ngoài ứng dụng.

### Ghi chú

- Đây là skeleton hoàn chỉnh về mặt cấu trúc & logic; bạn có thể:
  - Chỉnh sửa UI trong `view_toolbar.xml`.
  - Bổ sung màn hình cấu hình profile/dots nếu cần.
  - Mở rộng phần lưu trữ (Room, SharedPreferences, v.v.).


