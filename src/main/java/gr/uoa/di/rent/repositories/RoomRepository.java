package gr.uoa.di.rent.repositories;

import gr.uoa.di.rent.models.Hotel;
import gr.uoa.di.rent.models.Room;
import gr.uoa.di.rent.payload.requests.filters.PagedRoomsFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    // Get hotel page according to the given business
    Page<Room> findAllByHotel_id(Long hotelId, Pageable pageable);

    Optional<Room> findById(Long roomId);

    Optional<Room> findByHotelAndRoomNumber(Hotel hotel, Integer roomNumber);

    @Query(value =
            "SELECT r.*\n" +
                    "FROM rooms r\n" +
                    "WHERE r.hotel = :hotelID\n" +
                    "  AND r.id in\n" +
                    "      (SELECT r.id\n" +
                    "       FROM rooms r\n" +
                    "       WHERE r.capacity >= :#{#filter.visitors}\n" +
                    "         AND r.price >= :#{#filter.minPrice}\n" +
                    "         AND r.price <= :#{#filter.maxPrice}\n" +
                    "         AND r.id NOT IN (\n" +
                    "           SELECT r.id\n" +
                    "           FROM calendars c\n" +
                    "           WHERE c.room = r.id\n" +
                    "             AND (((c.start_date <= :#{#filter.startDate} AND :#{#filter.endDate} <= c.end_date)\n" +
                    "               OR (c.start_date <= :#{#filter.endDate} AND :#{#filter.endDate} <= c.end_date)\n" +
                    "               OR (:#{#filter.startDate} < end_date AND :#{#filter.endDate} >= c.end_date)))\n" +
                    "           LIMIT 1\n" +
                    "       ))\n"
            , nativeQuery = true)
    Page<Room> findRoomsByFilters(
            @Param("filter") PagedRoomsFilter filter,
            @Param("hotelID") Long hotelID,
            Pageable pageable);

}
