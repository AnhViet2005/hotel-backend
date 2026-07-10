package com.hotel.backend.config;

import com.hotel.backend.model.*;
import com.hotel.backend.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Configuration

public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final AmenityRepository amenityRepository;
    private final HotelRepository hotelRepository;
    private final HotelAmenityRepository hotelAmenityRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final HotelPolicyRepository hotelPolicyRepository;
    private final RoomCalendarRepository roomCalendarRepository;
    private final HotelImageRepository hotelImageRepository;
    private final RoomImageRepository roomImageRepository;
    private final PromotionRepository promotionRepository;
    private final BookingRepository bookingRepository;
    private final BookingRoomRepository bookingRoomRepository;
    private final BookingRoomRateRepository bookingRoomRateRepository;
    private final PaymentRepository paymentRepository;
    private final ReviewRepository reviewRepository;
    private final FavoriteRepository favoriteRepository;
    private final HotelStatisticsRepository statisticsRepository;
    private final PasswordEncoder passwordEncoder;
    private final ContactInfoRepository contactInfoRepository;

    public DataInitializer(RoleRepository roleRepository,
                           UserRepository userRepository,
                           AmenityRepository amenityRepository,
                           HotelRepository hotelRepository,
                           HotelAmenityRepository hotelAmenityRepository,
                           RoomTypeRepository roomTypeRepository,
                           HotelPolicyRepository hotelPolicyRepository,
                           RoomCalendarRepository roomCalendarRepository,
                           HotelImageRepository hotelImageRepository,
                           RoomImageRepository roomImageRepository,
                           PromotionRepository promotionRepository,
                           BookingRepository bookingRepository,
                           BookingRoomRepository bookingRoomRepository,
                           BookingRoomRateRepository bookingRoomRateRepository,
                           PaymentRepository paymentRepository,
                           ReviewRepository reviewRepository,
                           FavoriteRepository favoriteRepository,
                           HotelStatisticsRepository statisticsRepository,
                           PasswordEncoder passwordEncoder,
                           ContactInfoRepository contactInfoRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.amenityRepository = amenityRepository;
        this.hotelRepository = hotelRepository;
        this.hotelAmenityRepository = hotelAmenityRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.hotelPolicyRepository = hotelPolicyRepository;
        this.roomCalendarRepository = roomCalendarRepository;
        this.hotelImageRepository = hotelImageRepository;
        this.roomImageRepository = roomImageRepository;
        this.promotionRepository = promotionRepository;
        this.bookingRepository = bookingRepository;
        this.bookingRoomRepository = bookingRoomRepository;
        this.bookingRoomRateRepository = bookingRoomRateRepository;
        this.paymentRepository = paymentRepository;
        this.reviewRepository = reviewRepository;
        this.favoriteRepository = favoriteRepository;
        this.statisticsRepository = statisticsRepository;
        this.passwordEncoder = passwordEncoder;
        this.contactInfoRepository = contactInfoRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Luôn chạy logic vá phòng cho các khách sạn chưa có phòng
        addRoomTypesToHotelsWithoutRooms();

        // Khởi tạo thông tin liên hệ mặc định nếu chưa có
        if (contactInfoRepository.count() == 0) {
            contactInfoRepository.save(ContactInfo.builder()
                    .email("support@hotel.com")
                    .phone("19001234")
                    .address("123 Đường Lê Lợi, Bến Nghé, Quận 1, TP. Hồ Chí Minh")
                    .companyName("Antigravity Hotel Group")
                    .websiteUrl("https://hotel-booking-inky-eta.vercel.app")
                    .facebookUrl("https://facebook.com/antigravity")
                    .instagramUrl("https://instagram.com/antigravity")
                    .twitterUrl("https://twitter.com/antigravity")
                    .siteName("Antigravity Booking")
                    .siteDescription("Hệ thống đặt phòng khách sạn trực tuyến hàng đầu Việt Nam")
                    .seoKeywords("booking, hotel, hotel booking, đặt phòng khách sạn, du lịch")
                    .commissionRate(10)
                    .build());
            System.out.println(">>> Đã khởi tạo thông tin liên hệ mặc định.");
        }

        // --- PATCH FOR long.le@gmail.com ACCOUNT (Luôn chạy để đảm bảo quyền truy cập) ---
        userRepository.findByEmail("long.le@gmail.com").ifPresent(u -> {
            Role ownerRole = roleRepository.findByRoleName("OWNER").orElse(null);
            if (ownerRole != null) {
                boolean updated = false;
                if (u.getRole() == null || (!"OWNER".equalsIgnoreCase(u.getRole().getRoleName()) && !"ADMIN".equalsIgnoreCase(u.getRole().getRoleName()))) {
                    u.setRole(ownerRole);
                    updated = true;
                }
                if (!Boolean.TRUE.equals(u.getIsActive())) {
                    u.setIsActive(true);
                    updated = true;
                }
                if (updated) {
                    userRepository.save(u);
                    System.out.println(">>> Đã vá tài khoản long.le@gmail.com: Chuyển vai trò sang OWNER và Kích hoạt.");
                }

                // Đảm bảo user này có ít nhất 1 khách sạn để Dashboard có dữ liệu
                if (hotelRepository.findByOwnerId(u.getId()).isEmpty()) {
                    List<Amenity> defaultAmenities = amenityRepository.findAll();
                    createHotel("Long's Luxury Hotel", 
                        "Khách sạn đẳng cấp của Mr. Long tại trung tâm thành phố.",
                        "123 Lê Lợi", "Quận 1", "Bến Thành", "TP. Hồ Chí Minh", "5.0", u, 
                        defaultAmenities.size() > 5 ? defaultAmenities.subList(0, 5) : defaultAmenities,
                        "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb");
                    System.out.println(">>> Đã tạo khách sạn mặc định cho chủ sở hữu: long.le@gmail.com");
                }
            }
        });

        // --- PATCH FOR admin@hotel.com (Đảm bảo tài khoản admin hoạt động với mật khẩu admin123) ---
        Role patchAdminRole = roleRepository.findByRoleName("ADMIN").orElseGet(() -> roleRepository.save(Role.builder().roleName("ADMIN").build()));
        Optional<User> adminOpt = userRepository.findByEmail("admin@hotel.com");
        if (adminOpt.isPresent()) {
            User admin = adminOpt.get();
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            admin.setRole(patchAdminRole);
            admin.setIsActive(true);
            userRepository.save(admin);
            System.out.println(">>> Đã cập nhật mật khẩu cho admin@hotel.com thành admin123");
        } else {
            userRepository.save(User.builder()
                    .fullName("Hệ thống Quản trị")
                    .email("admin@hotel.com")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .role(patchAdminRole)
                    .isActive(true)
                    .build());
            System.out.println(">>> Đã tạo mới tài khoản admin@hotel.com với mật khẩu admin123");
        }

        // --- PATCH FOR owner@hotel.com (Đảm bảo tài khoản owner hoạt động với mật khẩu owner123) ---
        Role patchOwnerRole = roleRepository.findByRoleName("OWNER").orElseGet(() -> roleRepository.save(Role.builder().roleName("OWNER").build()));
        Optional<User> ownerOpt = userRepository.findByEmail("owner@hotel.com");
        if (ownerOpt.isPresent()) {
            User ownerUser = ownerOpt.get();
            ownerUser.setPasswordHash(passwordEncoder.encode("owner123"));
            ownerUser.setRole(patchOwnerRole);
            ownerUser.setIsActive(true);
            userRepository.save(ownerUser);
            System.out.println(">>> Đã cập nhật mật khẩu cho owner@hotel.com thành owner123");
        } else {
            userRepository.save(User.builder()
                    .fullName("Nguyễn Văn Chủ")
                    .email("owner@hotel.com")
                    .passwordHash(passwordEncoder.encode("owner123"))
                    .role(patchOwnerRole)
                    .isActive(true)
                    .build());
            System.out.println(">>> Đã tạo mới tài khoản owner@hotel.com với mật khẩu owner123");
        }

        if (hotelRepository.count() > 0) {
            return; // Dữ liệu mẫu ban đầu đã có
        }

        // 1. Roles
        Role adminRole = roleRepository.findByRoleName("ADMIN").orElseGet(() -> roleRepository.save(Role.builder().roleName("ADMIN").build()));
        Role ownerRole = roleRepository.findByRoleName("OWNER").orElseGet(() -> roleRepository.save(Role.builder().roleName("OWNER").build()));
        Role customerRole = roleRepository.findByRoleName("CUSTOMER").orElseGet(() -> roleRepository.save(Role.builder().roleName("CUSTOMER").build()));

        // 2. Users
        userRepository.save(User.builder()
                .fullName("Hệ thống Quản trị")
                .email("admin@hotel.com")
                .passwordHash(passwordEncoder.encode("admin123"))
                .role(adminRole)
                .isActive(true)
                .build());

        userRepository.save(User.builder()
                .fullName("Quản trị phụ")
                .email("admin2@hotel.com")
                .passwordHash(passwordEncoder.encode("admin456"))
                .role(adminRole)
                .isActive(true)
                .build());

        User owner = userRepository.save(User.builder()
                .fullName("Nguyễn Văn Chủ")
                .email("owner@hotel.com")
                .passwordHash(passwordEncoder.encode("owner123"))
                .role(ownerRole)
                .isActive(true)
                .build());

        List<User> customers = new ArrayList<>();
        String[] customerNames = {"Lê Minh Anh", "Trần Thị Bình", "Phạm Hoàng Gia", "Hoàng Thu Thảo", "Đặng Văn Hùng"};
        for (int i = 0; i < customerNames.length; i++) {
            customers.add(userRepository.save(User.builder()
                    .fullName(customerNames[i])
                    .email("customer" + (i + 1) + "@gmail.com")
                    .passwordHash(passwordEncoder.encode("user123"))
                    .role(customerRole)
                    .isActive(true)
                    .build()));
        }

        // 3. Amenities
        List<Amenity> amenities = Arrays.asList(
                Amenity.builder().amenityName("Free Wi-Fi").iconUrl("Wifi").build(),
                Amenity.builder().amenityName("Hồ bơi").iconUrl("Pool").build(),
                Amenity.builder().amenityName("Phòng Gym").iconUrl("Dumbbell").build(),
                Amenity.builder().amenityName("Nhà hàng").iconUrl("Utensils").build(),
                Amenity.builder().amenityName("Bãi đỗ xe").iconUrl("Car").build(),
                Amenity.builder().amenityName("Spa & Wellness").iconUrl("Flower").build(),
                Amenity.builder().amenityName("Điều hòa").iconUrl("Wind").build(),
                Amenity.builder().amenityName("Quầy Bar").iconUrl("Wine").build(),
                Amenity.builder().amenityName("Đưa đón sân bay").iconUrl("Plane").build(),
                Amenity.builder().amenityName("Bồn tắm").iconUrl("Bath").build()
        );
        amenityRepository.saveAll(amenities);

        // 4. Hotels
        Hotel metropole = createHotel("Sofitel Legend Metropole Hanoi", 
                "Khách sạn lịch sử mang phong cách thuộc địa Pháp, biểu tượng của sự sang trọng tại Hà Nội.",
                "15 Ngô Quyền", "Hoàn Kiếm", "Tràng Tiền", "Hà Nội", "5.0", owner, amenities.subList(0, 8),
                "https://images.unsplash.com/photo-1566073771259-6a8506099945?ixlib=rb-4.0.3&auto=format&fit=crop&w=2000&q=80");
        
        createHotel("Hanoi La Siesta Hotel & Spa",
                "Khách sạn boutique tinh tế tại Phố Cổ với dịch vụ spa đẳng cấp.",
                "94 Mã Mây", "Hoàn Kiếm", "Hàng Buồm", "Hà Nội", "4.5", owner, amenities.subList(0, 5),
                "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb");

        Hotel reverie = createHotel("The Reverie Saigon",
                "Khách sạn xa hoa bậc nhất Việt Nam với thiết kế nội thất Ý lộng lẫy.",
                "22-36 Nguyễn Huệ", "Quận 1", "Bến Nghé", "TP. Hồ Chí Minh", "5.0", owner, amenities,
                "https://images.unsplash.com/photo-1566073771259-6a8506099945");

        Hotel landmark81 = createHotel("Vinpearl Landmark 81, Autograph Collection",
                "Khách sạn cao nhất Việt Nam với tầm nhìn bao quát toàn cảnh thành phố.",
                "720A Điện Biên Phủ", "Bình Thạnh", "Phường 22", "TP. Hồ Chí Minh", "5.0", owner, amenities,
                "https://images.unsplash.com/photo-1582719508461-905c673771fd");

        Hotel intercon = createHotel("InterContinental Danang Sun Peninsula Resort",
                "Khu nghỉ dưỡng biệt lập trên bán đảo Sơn Trà, tuyệt tác kiến trúc của Bill Bensley.",
                "Bãi Bắc, Bán đảo Sơn Trà", "Sơn Trà", "Thọ Quang", "Đà Nẵng", "5.0", owner, amenities,
                "https://images.unsplash.com/photo-1571896349842-33c89424de2d");

        Hotel furama = createHotel("Furama Resort Danang",
                "Khu nghỉ dưỡng biển đẳng cấp mang đậm phong cách văn hóa Chăm Pa.",
                "105 Võ Nguyên Giáp", "Ngũ Hành Sơn", "Khuê Mỹ", "Đà Nẵng", "5.0", owner, amenities,
                "https://images.unsplash.com/photo-1499856871958-5b9627545d1a");

        Hotel imperial = createHotel("The Imperial Hotel Vũng Tàu",
                "Khách sạn 5 sao mang phong cách kiến trúc Phục Hưng độc đáo ngay sát biển.",
                "159 Thùy Vân", "Thắng Tam", "Thắng Tam", "Vũng Tàu", "5.0", owner, amenities,
                "https://images.unsplash.com/photo-1551882547-ff40c0d5b5df");

        Hotel pullman = createHotel("Pullman Vũng Tàu",
                "Khách sạn hiện đại với thiết kế táo bạo, trung tâm hội nghị lớn nhất Vũng Tàu.",
                "15 Thi Sách", "Thắng Tam", "Thắng Tam", "Vũng Tàu", "5.0", owner, amenities,
                "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb");

        Hotel dalatPalace = createHotel("Dalat Palace Heritage Hotel",
                "Khách sạn cổ kính mang vẻ đẹp di sản thời Pháp thuộc, nhìn ra hồ Xuân Hương.",
                "2 Trần Phú", "Phường 3", "Phường 3", "Đà Lạt", "5.0", owner, amenities,
                "https://images.unsplash.com/photo-1520250497591-112f2f40a3f4");

        Hotel colline = createHotel("Hôtel Colline",
                "Tọa lạc ngay trung tâm chợ Đà Lạt với phong cách thanh lịch, trẻ trung.",
                "10 Phan Bội Châu", "Phường 1", "Phường 1", "Đà Lạt", "4.0", owner, amenities.subList(0, 6),
                "https://images.unsplash.com/photo-1566665797739-1674de7a421a");

        Hotel vinpearlNt = createHotel("Vinpearl Resort Nha Trang",
                "Khu nghỉ dưỡng sang trọng trên đảo Hòn Tre với bãi biển riêng thơ mộng.",
                "Đảo Hòn Tre", "Vĩnh Nguyên", "Vĩnh Nguyên", "Nha Trang", "5.0", owner, amenities,
                "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b");

        Hotel interconNt = createHotel("InterContinental Nha Trang",
                "Tọa lạc ngay mặt biển trung tâm thành phố, thiết kế sang trọng đương đại.",
                "32-34 Trần Phú", "Lộc Thọ", "Lộc Thọ", "Nha Trang", "5.0", owner, amenities,
                "https://images.unsplash.com/photo-1631049307264-da0ec9d70304");

        Hotel marriott = createHotel("JW Marriott Phu Quoc Emerald Bay Resort",
                "Khu nghỉ dưỡng theo chủ đề trường đại học giả tưởng độc đáo tại Bãi Khem.",
                "Bãi Khem", "Phú Quốc", "An Thới", "Kiên Giang", "5.0", owner, amenities,
                "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b");

        // 5. Promotions
        promotionRepository.save(Promotion.builder()
                .hotel(metropole)
                .promoCode("SUMMER2024")
                .discountPercent(new BigDecimal("15.00"))
                .maxDiscountAmount(new BigDecimal("1000000"))
                .minOrderValue(new BigDecimal("2000000"))
                .startDate(LocalDateTime.now().minusDays(10))
                .endDate(LocalDateTime.now().plusMonths(3))
                .isActive(true)
                .build());

        promotionRepository.save(Promotion.builder()
                .hotel(intercon)
                .promoCode("HONEYMOON")
                .discountPercent(new BigDecimal("20.00"))
                .maxDiscountAmount(new BigDecimal("2000000"))
                .minOrderValue(new BigDecimal("5000000"))
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusYears(1))
                .isActive(true)
                .build());

        // 6. Room Types & Calendar
        seedRooms(metropole, "Luxury Room", "Phòng sang trọng phong cách cổ điển.", 4500000, 2, 1, 10, 
                "https://images.unsplash.com/photo-1566665797739-1674de7a421a?q=80&w=1000");
        seedRooms(metropole, "Grand Prestige Suite", "Căn hộ cao cấp bậc nhất.", 12000000, 2, 2, 2,
                "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?q=80&w=1000");

        seedRooms(reverie, "Deluxe King Room", "Nội thất hoàng gia Ý.", 6500000, 2, 1, 15,
                "https://images.unsplash.com/photo-1590490360182-c33d57733427?q=80&w=1000");
        seedRooms(reverie, "Panorama Suite", "Tầm nhìn 360 độ ra sông Sài Gòn.", 18000000, 2, 2, 3,
                "https://images.unsplash.com/photo-1631049307264-q=80&w=1000");

        seedRooms(landmark81, "Premier Club", "Tầm nhìn từ độ cao ấn tượng.", 7500000, 2, 1, 20,
                "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?q=80&w=1000");

        seedRooms(intercon, "Resort Classic Oceanview", "View biển Sơn Trà tuyệt đẹp.", 8500000, 2, 1, 20,
                "https://images.unsplash.com/photo-1540541338287-41700207dee6?q=80&w=1000");
        
        seedRooms(furama, "Ocean Studio Suite", "Thiết kế ban công rộng mở ra biển.", 6000000, 2, 2, 15,
                "https://images.unsplash.com/photo-1566665797739-1674de7a421a?q=80&w=1000");

        seedRooms(imperial, "Grand Sea View", "Kiến trúc hoàng gia sang trọng.", 4000000, 2, 1, 30,
                "https://images.unsplash.com/photo-1590490360182-c33d57733427?q=80&w=1000");

        seedRooms(pullman, "Superior City View", "Phòng thiết kế tối giản hiện đại.", 2800000, 2, 1, 40,
                "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?q=80&w=1000");

        seedRooms(dalatPalace, "Luxury Balcony", "Tầm nhìn ra Hồ Xuân Hương.", 5500000, 2, 1, 10,
                "https://images.unsplash.com/photo-1631049307264-da0ec9d70304?q=80&w=1000");

        seedRooms(colline, "Studio Room", "Thanh lịch ngay trung tâm Đà Lạt.", 2000000, 2, 1, 25,
                "https://images.unsplash.com/photo-1540541338287-41700207dee6?q=80&w=1000");

        seedRooms(vinpearlNt, "Villa 3-Bed Ocean", "Biệt thự nghỉ dưỡng cho gia đình.", 15000000, 6, 3, 5,
                "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?q=80&w=1000");

        seedRooms(interconNt, "Club Ocean View", "Đặc quyền Club InterContinental.", 7000000, 2, 1, 15,
                "https://images.unsplash.com/photo-1566665797739-1674de7a421a?q=80&w=1000");

        seedRooms(marriott, "Emerald Bay Front", "Sát bãi biển cát trắng.", 9500000, 2, 1, 25,
                "https://images.unsplash.com/photo-1571896349842-33c89424de2d?q=80&w=1000");

        // 7. Bookings, Payments & Reviews
        createBooking(customers.get(0), metropole, "Confirmed", 3, 4500000 * 3, true);
        createBooking(customers.get(1), reverie, "Completed", 2, 6500000 * 2, true);
        createBooking(customers.get(2), intercon, "Completed", 4, 8500000 * 4, true);
        createBooking(customers.get(3), marriott, "Pending", 2, 9500000 * 2, false);

        // 8. Favorites
        favoriteRepository.save(Favorite.builder()
                .id(new Favorite.FavoriteId(customers.get(0).getId(), metropole.getId()))
                .user(customers.get(0))
                .hotel(metropole)
                .build());
        favoriteRepository.save(Favorite.builder()
                .id(new Favorite.FavoriteId(customers.get(0).getId(), reverie.getId()))
                .user(customers.get(0))
                .hotel(reverie)
                .build());

        // 9. Statistics (Sample for last 7 days)
        for (int i = 0; i < 7; i++) {
            statisticsRepository.save(HotelStatistics.builder()
                    .hotel(metropole)
                    .statDate(LocalDate.now().minusDays(i))
                    .totalBookings(2 + i)
                    .totalRevenue(new BigDecimal(5000000 + i * 1000000))
                    .build());
        }
    }

    private Hotel createHotel(String name, String desc, String addr, String dist, String ward, String city, String star, User owner, List<Amenity> hAmenities, String imageUrl) {
        Hotel hotel = hotelRepository.save(Hotel.builder()
                .hotelName(name)
                .description(desc)
                .addressLine(addr)
                .district(dist)
                .ward(ward)
                .city(city)
                .starRating(new BigDecimal(star))
                .phone("090" + (1000000 + new Random().nextInt(8999999)))
                .email("info@" + name.toLowerCase().replace(" ", "") + ".com")
                .owner(owner)
                .isActive(true)
                .build());

        // Policies
        hotelPolicyRepository.save(HotelPolicy.builder()
                .hotel(hotel)
                .checkInTime(LocalTime.of(14, 0))
                .checkOutTime(LocalTime.of(12, 0))
                .cancellationPolicy("Hủy miễn phí trước 24h.")
                .childrenPolicy("Trẻ em dưới 6 tuổi miễn phí.")
                .petPolicy("Không cho phép thú cưng.")
                .build());

        // Amenities
        for (Amenity a : hAmenities) {
            hotelAmenityRepository.save(HotelAmenity.builder()
                    .id(new HotelAmenity.HotelAmenityId(hotel.getId(), a.getId()))
                    .hotel(hotel)
                    .amenity(a)
                    .isFree(true)
                    .build());
        }

        // Images
        hotelImageRepository.save(HotelImage.builder()
                .hotel(hotel)
                .imageUrl(imageUrl)
                .isPrimary(true)
                .build());

        return hotel;
    }

    private void seedRooms(Hotel hotel, String name, String desc, long price, int adults, int children, int total, String img) {
        RoomType rt = roomTypeRepository.save(RoomType.builder()
                .hotel(hotel)
                .typeName(name)
                .description(desc)
                .basePrice(new BigDecimal(price))
                .maxAdults(adults)
                .maxChildren(children)
                .totalRooms(total)
                .roomSize(30.0 + new Random().nextInt(50))
                .build());

        roomImageRepository.save(RoomImage.builder()
                .roomType(rt)
                .imageUrl(img)
                .isPrimary(true)
                .build());

        List<RoomCalendar> calendars = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 60; i++) {
            calendars.add(RoomCalendar.builder()
                    .roomType(rt)
                    .date(today.plusDays(i))
                    .price(new BigDecimal(price))
                    .totalRooms(total)
                    .bookedRooms(0)
                    .isAvailable(true)
                    .build());
        }
        roomCalendarRepository.saveAll(calendars);
    }

    private void createBooking(User user, Hotel hotel, String status, int nights, long total, boolean withReview) {
        Booking booking = bookingRepository.save(Booking.builder()
                .bookingCode("BK" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12))
                .user(user)
                .hotel(hotel)
                .guestName(user.getFullName())
                .guestEmail(user.getEmail())
                .guestPhone("0987654321")
                .checkIn(LocalDate.now().minusDays(nights + 2))
                .checkOut(LocalDate.now().minusDays(2))
                .subtotal(new BigDecimal(total))
                .totalAmount(new BigDecimal(total))
                .status(Booking.BookingStatus.valueOf(status.toUpperCase()))
                .build());

        if (status.equalsIgnoreCase("COMPLETED") || status.equalsIgnoreCase("CONFIRMED")) {
            paymentRepository.save(Payment.builder()
                    .booking(booking)
                    .amount(new BigDecimal(total))
                    .paymentMethod(Payment.PaymentMethod.VNPAY)
                    .status(Payment.PaymentStatus.SUCCESS)
                    .transactionId("TRX" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .build());

            if (withReview) {
                reviewRepository.save(Review.builder()
                        .user(user)
                        .hotel(hotel)
                        .booking(booking)
                        .rating(new BigDecimal("4.5"))
                        .comment("Dịch vụ tuyệt vời, phòng ốc sạch sẽ và nhân viên rất nhiệt tình!")
                        .isPublished(true)
                        .build());
            }
        }
    }

    @Transactional
    public void addRoomTypesToHotelsWithoutRooms() {
        List<Hotel> hotels = hotelRepository.findAll();
        for (Hotel hotel : hotels) {
            long roomTypeCount = roomTypeRepository.countByHotelId(hotel.getId());
            if (roomTypeCount == 0) {
                // Add default room types
                seedRooms(hotel, "Phòng Standard", "Phòng tiêu chuẩn đầy đủ tiện nghi, phù hợp cho 2 người.", 800000, 2, 1, 10, 
                    "https://images.unsplash.com/photo-1566665797739-1674de7a421a?q=80&w=1000");
                seedRooms(hotel, "Phòng Deluxe", "Phòng sang trọng với tầm nhìn đẹp và nội thất hiện đại.", 1500000, 2, 2, 5, 
                    "https://images.unsplash.com/photo-1590490360182-c33d57733427?q=80&w=1000");
                seedRooms(hotel, "Phòng Suite", "Căn hộ cao cấp với phòng khách riêng biệt và tiện ích đẳng cấp.", 3000000, 2, 2, 2, 
                    "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?q=80&w=1000");
                System.out.println(">>> Đã tự động thêm các loại phòng cho khách sạn: " + hotel.getHotelName());
            }
        }
    }
}
