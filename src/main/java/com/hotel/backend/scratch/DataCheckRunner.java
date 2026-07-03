package com.hotel.backend.scratch;

import com.hotel.backend.repository.BookingRepository;
import com.hotel.backend.repository.HotelRepository;
import com.hotel.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataCheckRunner implements CommandLineRunner {
    private final HotelRepository hotelRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    public DataCheckRunner(HotelRepository hotelRepository, BookingRepository bookingRepository, UserRepository userRepository) {
        this.hotelRepository = hotelRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("======= DATA CHECK =======");
        System.out.println("Hotels count: " + hotelRepository.count());
        System.out.println("Bookings count: " + bookingRepository.count());
        System.out.println("Users count: " + userRepository.count());
        System.out.println("==========================");
    }
}
