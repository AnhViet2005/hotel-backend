# Hệ Thống Phân Chia Doanh Thu (Revenue Distribution System)

## Tổng Quan

Hệ thống phân chia doanh thu được thiết kế để theo dõi và quản lý doanh thu từ các đơn đặt phòng khách sạn. Platform nhận 30% hoa hồng, chủ khách sạn nhận 70% từ mỗi đơn thanh toán.

## Cấu Trúc Phân Chia

```
Tổng tiền người dùng thanh toán (Total Amount)
│
├─ 30% → Doanh thu Admin (Platform Revenue)
│        └─ Nhận được từ lúc booking CONFIRMED
│
└─ 70% → Doanh thu Chủ Khách Sạn (Hotel Owner Revenue)
         └─ Nhận được khi booking COMPLETED (sau khi khách hàng checkout)
```

## Các Trường Mới Trong Database

### Bảng `bookings`

Thêm 2 cột mới để theo dõi doanh thu:

| Cột | Kiểu Dữ Liệu | Mô Tả |
|-----|---------------|-------|
| `admin_revenue` | DECIMAL(12,2) | Doanh thu nền tảng (30% của total_amount) |
| `hotel_owner_revenue` | DECIMAL(12,2) | Doanh thu chủ khách sạn (70% của total_amount) |

**Migration SQL:**
```sql
ALTER TABLE bookings ADD COLUMN admin_revenue DECIMAL(12, 2) DEFAULT 0.00 
  COMMENT 'Admin revenue: 30% of total_amount';
ALTER TABLE bookings ADD COLUMN hotel_owner_revenue DECIMAL(12, 2) DEFAULT 0.00 
  COMMENT 'Hotel owner revenue: 70% of total_amount';
```

## Backend Implementation

### 1. Model - Booking.java

```java
@Column(name = "admin_revenue", precision = 12, scale = 2)
private BigDecimal adminRevenue; // 30% của totalAmount

@Column(name = "hotel_owner_revenue", precision = 12, scale = 2)
private BigDecimal hotelOwnerRevenue; // 70% của totalAmount
```

### 2. Service - UserBookingService.java

Khi tạo booking, hệ thống tự động tính toán doanh thu:

```java
// Tính toán doanh thu
BigDecimal adminRevenue = total.multiply(new BigDecimal("0.30"));
BigDecimal hotelOwnerRevenue = total.multiply(new BigDecimal("0.70"));

// Set vào booking
Booking booking = Booking.builder()
    .totalAmount(total)
    .adminRevenue(adminRevenue)
    .hotelOwnerRevenue(hotelOwnerRevenue)
    .build();
```

### 3. Controller - AdminController.java

#### Endpoint 1: `/api/admin/stats` (GET)
Lấy thống kê doanh thu theo vai trò

**Cho Admin:**
- `totalRevenue` = Tổng `adminRevenue` của tất cả CONFIRMED/COMPLETED bookings
- Hiểu rằng: Admin chỉ nhận doanh thu từ lúc booking được xác nhận (CONFIRMED)

**Cho Hotel Owner:**
- `totalRevenue` = Tổng `hotelOwnerRevenue` của tất cả COMPLETED bookings
- Hiểu rằng: Hotel owner chỉ nhận doanh thu khi khách hàng checkout (COMPLETED)

```json
{
  "totalRevenue": 1000000,
  "totalBookings": 10,
  "totalCustomers": 50,
  "totalHotels": 5
}
```

#### Endpoint 2: `/api/admin/revenue-breakdown` (GET)
Lấy chi tiết phân chia doanh thu

```json
{
  "totalAmount": 1000000,
  "adminRevenue": 300000,
  "hotelOwnerRevenue": 700000,
  "revenueDistribution": "Admin: 30% | Hotel Owner: 70%"
}
```

## Frontend Implementation

### 1. API Utility - hotel-admin/src/utils/api.ts

```typescript
export const getRevenueBreakdown = async () => {
  const response = await api.get('/admin/revenue-breakdown');
  return response.data;
};
```

### 2. Component - RevenueBreakdown.tsx

Component hiển thị doanh thu theo dạng visual:
- Thanh tiến trình 30% (Admin) vs 70% (Hotel Owner)
- Hiển thị số tiền cụ thể cho từng bên
- Thông tin giáo dục về cách chia tiền

### 3. Dashboard Page - hotel-admin/src/app/page.tsx

Tích hợp component RevenueBreakdown vào dashboard:

```typescript
const [revenueData, setRevenueData] = useState({
  totalAmount: 0,
  adminRevenue: 0,
  hotelOwnerRevenue: 0,
  revenueDistribution: ""
});

// Fetch data
const revenue = await getRevenueBreakdown();
setRevenueData(revenue);

// Hiển thị
<RevenueBreakdown 
  adminRevenue={revenueData.adminRevenue}
  hotelOwnerRevenue={revenueData.hotelOwnerRevenue}
  totalAmount={revenueData.totalAmount}
/>
```

## Quy Trình Thanh Toán

### Giai đoạn 1: Booking PENDING → CONFIRMED

1. Người dùng tạo booking
2. Hệ thống tính: 
   - `adminRevenue` = totalAmount × 0.30
   - `hotelOwnerRevenue` = totalAmount × 0.70
3. Người dùng thanh toán cọc 30% qua VNPay
4. Booking chuyển thành CONFIRMED
5. **Admin nhận doanh thu 30%** (adminRevenue)

### Giai đoạn 2: Booking CONFIRMED → COMPLETED

1. Khách hàng check-in, ở lại, rồi check-out
2. Người dùng thanh toán 70% còn lại (hoặc qua VNPay hoặc tại quầy)
3. Booking chuyển thành COMPLETED
4. **Hotel Owner nhận doanh thu 70%** (hotelOwnerRevenue)

## Luồng Dữ Liệu

```
Booking Created (PENDING)
    ↓
adminRevenue = totalAmount × 0.30
hotelOwnerRevenue = totalAmount × 0.70
    ↓
Booking CONFIRMED (after deposit payment)
    ↓ Admin's 30% revenue is counted
    ↓
Booking COMPLETED (after remaining payment)
    ↓ Hotel Owner's 70% revenue is counted
    ↓
Revenue statistics updated in admin dashboard
```

## Lợi Ích

### Cho Platform Admin
- Dễ dàng theo dõi hoa hồng từ mỗi booking
- Biết chính xác doanh thu từng giai đoạn (PENDING, CONFIRMED, COMPLETED)
- Báo cáo tài chính chính xác

### Cho Hotel Owner
- Rõ ràng về tỷ lệ hoa hồng platform (30%)
- Chỉ nhận doanh thu khi khách hàng hoàn thành stay (COMPLETED)
- Dễ dàng kiểm toán doanh thu của mình

### Cho Users
- Biết cách tiền được chia giữa platform và hotel
- Tính toàn minh bạch

## Debugging & Monitoring

### 1. Kiểm tra doanh thu trên Dashboard
- Admin dashboard hiển thị "Phân chia doanh thu" 
- Xem visual bars cho 30% vs 70%

### 2. Kiểm tra Database
```sql
-- Xem tất cả doanh thu theo trạng thái
SELECT booking_code, total_amount, admin_revenue, hotel_owner_revenue, status
FROM bookings
WHERE status IN ('CONFIRMED', 'COMPLETED');

-- Tính tổng doanh thu admin
SELECT SUM(admin_revenue) as total_admin_revenue
FROM bookings
WHERE status IN ('CONFIRMED', 'COMPLETED');

-- Tính tổng doanh thu hotel owner
SELECT SUM(hotel_owner_revenue) as total_owner_revenue
FROM bookings
WHERE status = 'COMPLETED';
```

### 3. API Testing
```bash
# Test stats endpoint
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/admin/stats

# Test revenue breakdown
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/admin/revenue-breakdown
```

## Công Thức Tính

### Admin Revenue (Doanh thu Admin)
```
Admin Revenue = Total Amount × 0.30
Thời điểm nhận: CONFIRMED
```

### Hotel Owner Revenue (Doanh thu Chủ Khách Sạn)
```
Hotel Owner Revenue = Total Amount × 0.70
Thời điểm nhận: COMPLETED
```

### Total Revenue (Tổng Doanh Thu)
```
Total Revenue (Admin) = SUM(admin_revenue) where status IN ('CONFIRMED', 'COMPLETED')
Total Revenue (Owner) = SUM(hotel_owner_revenue) where status = 'COMPLETED'
```

## Ví Dụ Thực Tế

### Scenario
- Khách hàng booking 1 phòng với tổng giá 1,000,000 VND
- Tỉnh: Đà Nẵng, Khách sạn: Sun Hotel

### Timeline

| Bước | Hành động | Status | Admin Revenue | Owner Revenue |
|------|----------|--------|---------------|---------------|
| 1 | Tạo booking | PENDING | 0 | 0 |
| 2 | Thanh toán cọc (30%) via VNPay | CONFIRMED | +300,000 | 0 |
| 3 | Khách checkout, thanh toán 70% | COMPLETED | 300,000 | +700,000 |

**Kết quả:** Admin nhận 300,000 VND, Hotel Owner nhận 700,000 VND

## Cài Đặt & Triển Khai

### Database Migration
```sql
-- File: V1_Add_Revenue_Tracking.sql
ALTER TABLE bookings ADD COLUMN admin_revenue DECIMAL(12, 2) DEFAULT 0.00;
ALTER TABLE bookings ADD COLUMN hotel_owner_revenue DECIMAL(12, 2) DEFAULT 0.00;
```

### Build Backend
```bash
cd hotel-backend
mvn clean install
mvn spring-boot:run
```

### Build Admin Frontend
```bash
cd hotel-admin
npm install
npm run dev
```

### Verify
1. Mở http://localhost:3000 (admin dashboard)
2. Đăng nhập với tài khoản admin
3. Xem "Phân chia doanh thu" trên dashboard
4. Kiểm tra `/api/admin/revenue-breakdown` endpoint

## Mở Rộng Trong Tương Lai

- Thêm báo cáo doanh thu theo thời gian (hàng ngày, hàng tuần, hàng tháng)
- Export báo cáo doanh thu (PDF, Excel)
- Thông báo khi đạt mục tiêu doanh thu
- Dashboard cho hotel owner hiển thị 70% doanh thu của họ
- Tích hợp thanh toán tự động cho hotel owner
