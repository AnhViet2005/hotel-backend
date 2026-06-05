# Quick Reference - Hệ Thống Phân Chia Doanh Thu

## 📋 Checklist Triển Khai

### Backend (Java/Spring)
- [x] Thêm `adminRevenue` field vào Booking entity
- [x] Thêm `hotelOwnerRevenue` field vào Booking entity  
- [x] Cập nhật logic tính doanh thu trong `UserBookingService`
- [x] Tạo DTO `RevenueBreakdownResponse`
- [x] Thêm endpoint `/api/admin/revenue-breakdown` 
- [x] Cập nhật `AdminController.getStats()` để sử dụng các field mới
- [ ] **TO DO: Chạy database migration**

### Frontend - Admin Dashboard (Next.js)
- [x] Thêm `getRevenueBreakdown()` function vào API utils
- [x] Tạo `RevenueBreakdown.tsx` component
- [x] Cập nhật dashboard page để hiển thị revenue breakdown
- [x] Import component và fetch data

### Database
- [ ] **TO DO: Execute migration script**

## 🚀 Bước Tiếp Theo

### 1️⃣ Database Migration

```bash
# Nếu dùng Flyway hoặc Liquibase, file migration đã được tạo:
# File: src/main/resources/db/migration/V1_Add_Revenue_Tracking.sql

# Hoặc chạy trực tiếp SQL:
ALTER TABLE bookings ADD COLUMN admin_revenue DECIMAL(12, 2) DEFAULT 0.00;
ALTER TABLE bookings ADD COLUMN hotel_owner_revenue DECIMAL(12, 2) DEFAULT 0.00;
```

### 2️⃣ Build & Run Backend

```bash
cd hotel-backend
mvn clean install
mvn spring-boot:run
```

### 3️⃣ Build & Run Admin Dashboard

```bash
cd hotel-admin  
npm install
npm run dev
```

### 4️⃣ Test API Endpoints

```bash
# Test 1: Get Dashboard Stats
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/admin/stats

# Test 2: Get Revenue Breakdown  
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/admin/revenue-breakdown

# Test 3: Check Recent Bookings (includes revenue info)
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/admin/recent-bookings
```

### 5️⃣ Verify Dashboard

- Truy cập http://localhost:3000/
- Đăng nhập với tài khoản admin
- Xem widget "Phân chia doanh thu" hiển thị 30% vs 70%

## 📊 API Endpoints

| Method | Endpoint | Mô Tả | Response |
|--------|----------|-------|----------|
| GET | `/api/admin/stats` | Lấy thống kê doanh thu | `{ totalRevenue, totalBookings, totalCustomers, totalHotels }` |
| GET | `/api/admin/revenue-breakdown` | Lấy chi tiết phân chia | `{ totalAmount, adminRevenue, hotelOwnerRevenue }` |
| GET | `/api/admin/recent-bookings` | Lấy 5 booking gần nhất | Array of bookings with revenue |

## 💰 Phân Chia Doanh Thu

```
Booking Amount: 1,000,000 VND
├─ Admin (30%):        300,000 VND ✓ Nhận khi CONFIRMED
└─ Hotel Owner (70%):  700,000 VND ✓ Nhận khi COMPLETED
```

## 🔍 Database Queries

### Kiểm tra tổng doanh thu admin
```sql
SELECT SUM(admin_revenue) as total 
FROM bookings 
WHERE status IN ('CONFIRMED', 'COMPLETED');
```

### Kiểm tra tổng doanh thu hotel owner
```sql
SELECT SUM(hotel_owner_revenue) as total 
FROM bookings 
WHERE status = 'COMPLETED';
```

### Kiểm tra doanh thu theo khách sạn
```sql
SELECT 
  h.hotel_name,
  COUNT(b.id) as booking_count,
  SUM(b.hotel_owner_revenue) as hotel_revenue
FROM bookings b
JOIN hotels h ON b.hotel_id = h.id
WHERE b.status = 'COMPLETED'
GROUP BY h.id, h.hotel_name;
```

## 📁 Files Được Tạo/Sửa

### Backend
- ✅ `model/Booking.java` - Thêm 2 fields mới
- ✅ `service/UserBookingService.java` - Cập nhật logic tính doanh thu
- ✅ `dto/RevenueBreakdownResponse.java` - New DTO
- ✅ `controller/AdminController.java` - Thêm endpoint & update stats logic
- ✅ `resources/db/migration/V1_Add_Revenue_Tracking.sql` - Migration

### Frontend (Admin)
- ✅ `utils/api.ts` - Thêm `getRevenueBreakdown()`
- ✅ `components/dashboard/RevenueBreakdown.tsx` - New component
- ✅ `app/page.tsx` - Integrate component

## 🎯 Metrics Để Theo Dõi

| Metric | Admin Thấy | Hotel Owner Thấy | Trigger |
|--------|-----------|-----------------|---------|
| Admin Revenue | Tất cả CONFIRMED/COMPLETED | N/A | Booking CONFIRMED |
| Owner Revenue | Tất cả COMPLETED | Bookings của họ COMPLETED | Booking COMPLETED |
| Total Revenue | Sum of all | Sum of their own | Real-time |

## 🐛 Troubleshooting

### Problem: Revenue không hiển thị
**Solution:** 
- Kiểm tra database đã chạy migration chưa
- Verify booking status là CONFIRMED hoặc COMPLETED
- Check API token có valid không

### Problem: Revenue calculation sai
**Solution:**
- Verify `totalAmount` field đúng
- Check 30% và 70% được tính đúng
- Database columns có giá trị không (không NULL)

### Problem: Endpoint 404
**Solution:**
- Restart backend: `mvn spring-boot:run`
- Check AdminController import `RevenueBreakdownResponse`
- Verify endpoint path `/api/admin/revenue-breakdown`

## 📚 Documentation

Chi tiết đầy đủ xem: [REVENUE_DISTRIBUTION_GUIDE.md](REVENUE_DISTRIBUTION_GUIDE.md)

## ⚙️ Configuration

### Tỷ lệ Admin (có thể điều chỉnh)
- **Hiện tại:** 30%
- **File:** `UserBookingService.java` - dòng tính `adminRevenue`
- **Cách thay đổi:** `multiply(new BigDecimal("0.30"))` → `multiply(new BigDecimal("0.25"))`

### Điều kiện nhận doanh thu
- **Admin:** `status IN ('CONFIRMED', 'COMPLETED')`
- **Owner:** `status = 'COMPLETED'` (chỉ khi khách checkout)

## 📞 Support

Nếu cần hỗ trợ:
1. Kiểm tra logs: `tail -f target/spring.log`
2. Xem database: `SELECT * FROM bookings WHERE id = ?`
3. Test API: Sử dụng Postman/curl commands ở trên
