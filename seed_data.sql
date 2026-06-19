-- Tắt kiểm tra khóa ngoại để chèn dữ liệu nhanh
SET FOREIGN_KEY_CHECKS = 0;

-- Xóa dữ liệu cũ nếu đã chạy một nửa
DELETE FROM users WHERE email IN ('nam.nguyen@gmail.com', 'mai.tran@gmail.com', 'long.le@gmail.com', 'anh.pham@gmail.com', 'bao.hoang123@gmail.com', 'thao.dang@gmail.com', 'huy.vu@gmail.com', 'tung.ngo@gmail.com', 'mai.bui@gmail.com');

-- 4 Người dùng (USER - ID 4)
INSERT INTO users (full_name, email, password_hash, phone, is_active, role_id) VALUES
('Nguyễn Văn Nam', 'nam.nguyen@gmail.com', '$2a$10$Re63tML3vxeZFroGZ4Ugj.Cf/ETjq/BlViDZBVuGZXv8sS64XZ9Fi', '0912345678', 1, 4),
('Trần Thị Mai', 'mai.tran@gmail.com', '$2a$10$Re63tML3vxeZFroGZ4Ugj.Cf/ETjq/BlViDZBVuGZXv8sS64XZ9Fi', '0922345678', 1, 4),
('Lê Hoàng Long', 'long.le@gmail.com', '$2a$10$Re63tML3vxeZFroGZ4Ugj.Cf/ETjq/BlViDZBVuGZXv8sS64XZ9Fi', '0932345678', 1, 4),
('Phạm Minh Anh', 'anh.pham@gmail.com', '$2a$10$Re63tML3vxeZFroGZ4Ugj.Cf/ETjq/BlViDZBVuGZXv8sS64XZ9Fi', '0942345678', 1, 4);

-- 5 Chủ khách sạn (OWNER - ID 2)
INSERT INTO users (full_name, email, password_hash, phone, is_active, role_id) VALUES
('Hoàng Gia Bảo', 'bao.hoang123@gmail.com', '$2a$10$Re63tML3vxeZFroGZ4Ugj.Cf/ETjq/BlViDZBVuGZXv8sS64XZ9Fi', '0812345001', 1, 2),
('Đặng Thu Thảo', 'thao.dang@gmail.com', '$2a$10$Re63tML3vxeZFroGZ4Ugj.Cf/ETjq/BlViDZBVuGZXv8sS64XZ9Fi', '0812345002', 1, 2),
('Vũ Đức Huy', 'huy.vu@gmail.com', '$2a$10$Re63tML3vxeZFroGZ4Ugj.Cf/ETjq/BlViDZBVuGZXv8sS64XZ9Fi', '0812345003', 1, 2),
('Ngô Thanh Tùng', 'tung.ngo@gmail.com', '$2a$10$Re63tML3vxeZFroGZ4Ugj.Cf/ETjq/BlViDZBVuGZXv8sS64XZ9Fi', '0812345004', 1, 2),
('Bùi Tuyết Mai', 'mai.bui@gmail.com', '$2a$10$Re63tML3vxeZFroGZ4Ugj.Cf/ETjq/BlViDZBVuGZXv8sS64XZ9Fi', '0812345005', 1, 2);

-- 5 Khách sạn (Cập nhật đúng tên cột)
INSERT INTO hotels (hotel_name, city, address_line, star_rating, base_price, is_active, owner_id, description) 
SELECT 'The Grand Palace', 'Hà Nội', '12 Lý Thường Kiệt, Hoàn Kiếm', 5.0, 2500000, 1, id, 'Khách sạn sang trọng bậc nhất thủ đô.' FROM users WHERE email = 'bao.hoang123@gmail.com';

INSERT INTO hotels (hotel_name, city, address_line, star_rating, base_price, is_active, owner_id, description) 
SELECT 'Sapa Horizon Hotel', 'Lào Cai', '18 Phạm Ngọc Thạch, Sa Pa', 4.8, 1200000, 1, id, 'Tầm nhìn tuyệt đẹp ra dãy Hoàng Liên Sơn.' FROM users WHERE email = 'thao.dang@gmail.com';

INSERT INTO hotels (hotel_name, city, address_line, star_rating, base_price, is_active, owner_id, description) 
SELECT 'Da Nang Riverside', 'Đà Nẵng', '77 Trần Hưng Đạo, Sơn Trà', 4.5, 950000, 1, id, 'Sát bờ sông Hàn, thuận tiện xem cầu Rồng.' FROM users WHERE email = 'huy.vu@gmail.com';

INSERT INTO hotels (hotel_name, city, address_line, star_rating, base_price, is_active, owner_id, description) 
SELECT 'Hoi An Ancient House', 'Quảng Nam', '22 Nguyễn Thái Học, Hội An', 4.7, 1500000, 1, id, 'Phong cách kiến trúc cổ kính giữa lòng phố cổ.' FROM users WHERE email = 'tung.ngo@gmail.com';

INSERT INTO hotels (hotel_name, city, address_line, star_rating, base_price, is_active, owner_id, description) 
SELECT 'Phu Quoc Sunset Resort', 'Kiên Giang', 'Bãi Trường, Dương Tơ', 4.9, 3200000, 1, id, 'Khu nghỉ dưỡng ngắm hoàng hôn đẹp nhất đảo.' FROM users WHERE email = 'mai.bui@gmail.com';

SET FOREIGN_KEY_CHECKS = 1;
