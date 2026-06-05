-- Migration script for adding revenue tracking columns to bookings table
-- Execute this in your database to add the new columns

ALTER TABLE bookings ADD COLUMN admin_revenue DECIMAL(12, 2) DEFAULT 0.00;
ALTER TABLE bookings ADD COLUMN hotel_owner_revenue DECIMAL(12, 2) DEFAULT 0.00;

-- Add comments for clarity
ALTER TABLE bookings MODIFY admin_revenue DECIMAL(12, 2) DEFAULT 0.00 COMMENT 'Admin revenue: 30% of total_amount';
ALTER TABLE bookings MODIFY hotel_owner_revenue DECIMAL(12, 2) DEFAULT 0.00 COMMENT 'Hotel owner revenue: 70% of total_amount';

-- Create index for better query performance on revenue calculations
CREATE INDEX idx_admin_revenue ON bookings(admin_revenue);
CREATE INDEX idx_hotel_owner_revenue ON bookings(hotel_owner_revenue);
