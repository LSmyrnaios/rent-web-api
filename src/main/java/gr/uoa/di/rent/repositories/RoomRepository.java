package gr.uoa.di.rent.repositories;

import gr.uoa.di.rent.models.Hotel;
import gr.uoa.di.rent.models.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    // Get hotel page according to the given business
    Page<Room> findAllByHotel_id(Long hotelId, Pageable pageable);

    Optional<Room> findById(Long roomId);

    Optional<Room> findByHotelAndRoomNumber(Hotel hotel, Integer roomNumber);

}
