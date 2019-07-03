package gr.uoa.di.rent.repositories;

import gr.uoa.di.rent.models.Hotel;
import gr.uoa.di.rent.payload.requests.filters.PagedHotelsFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, Long>, CustomHotelRepository {

    List<Hotel> findAll();

    Hotel findById(long id);

    Hotel findByNameOrEmail(String name, String email);

    @Transactional
    @Modifying
    @Query(value = "BEGIN TRANSACTION;\n" +
            "update wallets\n" +
            "set balance = balance - :amount\n" +
            "from users u\n" +
            "where u.id = :userID\n" +
            "  and u.wallet = wallets.id;\n" +
            "\n" +
            "update wallets\n" +
            "set balance = balance + :amount * 99 / 100\n" +
            "from hotels h,\n" +
            "     businesses b\n" +
            "where h.id = :hotelID\n" +
            "  and h.business = b.id\n" +
            "  and b.wallet = wallets.id;\n" +
            "\n" +
            "update wallets\n" +
            "set balance = balance + :amount * 1 / 100\n" +
            "from businesses b\n" +
            "where b.id = 1\n" +
            "  and b.wallet = wallets.id;\n" +
            "\n" +
            "COMMIT;", nativeQuery = true)
    void transferMoney(
            @Param("userID") Long userID,
            @Param("hotelID") Long hotelID,
            @Param("amount") Double amount);


    @Query(value = "SELECT h.*\n" +
            "FROM hotels as h,\n" +
            "       hotel_amenities as ha,\n" +
            "       amenities as a\n" +
            "       WHERE ha.amenity_id = a.id\n" +
            "       and ha.hotel_id = h.id\n" +
            "       AND a.name in :amenities\n" +
            "       GROUP BY h.id\n" +
            "       HAVING COUNT(DISTINCT a.id) = :amenities_count",
            countQuery = "SELECT COUNT(h.*)\n" +
                    "FROM hotels as h,\n" +
                    "       hotel_amenities as ha,\n" +
                    "       amenities as a\n" +
                    "       WHERE ha.amenity_id = a.id\n" +
                    "       AND ha.hotel_id = h.id\n" +
                    "       AND a.name in :amenities\n" +
                    "       GROUP BY h.id\n" +
                    "       HAVING COUNT(DISTINCT a.id) = :amenities_count",
            nativeQuery = true)
    Page<Hotel> findAllHotelsByAmenities(@Param("amenities") List<String> amenities,
                                         @Param("amenities_count") int amenities_count,
                                         Pageable pageable);

    @Query(value =
            "SELECT h.*\n" +
                    "FROM hotels h\n" +
                    "WHERE (point(:longitude, :latitude) <@> point(lng, lat)) < :radius_km / 1.61",
            countQuery = "SELECT COUNT(h.*)\n" +
                    "FROM hotels h\n" +
                    "WHERE (point(:longitude, :latitude) <@> point(lng, lat)) < :radius_km / 1.61",
            nativeQuery = true)
    Page<Hotel> findByLocationAndRadius(
            @Param("longitude") double longitude, @Param("latitude") double latitude,
            @Param("radius_km") double radius_km, Pageable pageable);

    @Query(value =
            "SELECT h.*\n" +
                    "FROM hotels h,\n" +
                    "     hotel_amenities ha,\n" +
                    "     amenities a\n" +
                    "WHERE h.id in\n" +
                    "      (\n" +
                    "          SELECT hotelsfilter.id\n" +
                    "          FROM hotels hotelsfilter\n" +
                    "          WHERE hotelsfilter.id in\n" +
                    "              -- 1) GEOLOCATION RADIUS SEARCH\n" +
                    "                (\n" +
                    "                    SELECT h1.id\n" +
                    "                    FROM hotels h1\n" +
                    "                    WHERE (point(:#{#filter.lng}, :#{#filter.lat}) <@> point(lng, lat)) < :#{#filter.radius} / 1.61\n" +
                    "                )\n" +
                    "            -- 2) VISITORS + CALENDAR AVAILABILITY + PRICE RANGE\n" +
                    "            AND hotelsfilter.id in (\n" +
                    "              SELECT DISTINCT h2.id\n" +
                    "              FROM hotels h2,\n" +
                    "                   rooms r1\n" +
                    "              WHERE r1.hotel = h2.id\n" +
                    "                AND r1.id in (\n" +
                    "                  SELECT r.id\n" +
                    "                  FROM rooms r\n" +
                    "                  WHERE r.capacity >= :#{#filter.visitors}\n" +
                    "                    AND r.price >= :#{#filter.minPrice}\n" +
                    "                    AND r.price <= :#{#filter.maxPrice}\n" +
                    "                    AND r.id NOT IN (\n" +
                    "                      SELECT r.id\n" +
                    "                      FROM calendars c\n" +
                    "                      WHERE c.room = r.id\n" +
                    "                        AND (((c.start_date <= :#{#filter.startDate} AND :#{#filter.endDate} <= c.end_date)\n" +
                    "                          OR (c.start_date <= :#{#filter.endDate} AND :#{#filter.endDate} <= c.end_date)\n" +
                    "                          OR (:#{#filter.startDate} < end_date AND :#{#filter.endDate} >= c.end_date)))\n" +
                    "\n" +
                    "                      LIMIT 1 --- necessary maybe not\n" +
                    "                  ))))\n" +
                    "  -- 3) AMENITY SEARCH\n" +
                    "  AND ha.amenity_id = a.id\n" +
                    "  AND ha.hotel_id = h.id\n" +
                    "  AND a.name in :amenities\n" +
                    "GROUP BY h.id\n" +
                    "HAVING COUNT(DISTINCT a.id) = :amenities_count\n"
            , nativeQuery = true)
    Page<Hotel> findWithFilters(
            @Param("filter") PagedHotelsFilter filter,
            @Param("amenities") List<String> amenities, @Param("amenities_count") int amenities_count,
            Pageable pageable);

    @Query(value =
            "SELECT MIN(price), MAX(price)\n" +
                    "FROM rooms\n" +
                    "WHERE (id, price) in (SELECT r.id, r.price\n" +
                    "                      FROM rooms r,\n" +
                    "                           hotels h\n" +
                    "                      WHERE h.id in (SELECT h.id\n" +
                    "                                     FROM hotels h,\n" +
                    "                                          hotel_amenities ha,\n" +
                    "                                          amenities a\n" +
                    "                                     WHERE h.id in\n" +
                    "                                           (\n" +
                    "                                               SELECT hotelsfilter.id\n" +
                    "                                               FROM hotels hotelsfilter\n" +
                    "                                               WHERE hotelsfilter.id in (\n" +
                    "                                                   SELECT h1.id\n" +
                    "                                                   FROM hotels h1\n" +
                    "                                                   WHERE (point(:#{#filter.lng}, :#{#filter.lat}) <@> point(lng, lat)) < :#{#filter.radius} / 1.61\n" +
                    "                                               )\n" +
                    "                                           )\n" +
                    "                                           AND ha.amenity_id = a.id\n" +
                    "                                           AND ha.hotel_id = h.id\n" +
                    "                                           AND a.name in :amenities\n" +
                    "                                     GROUP BY h.id\n" +
                    "                                     HAVING COUNT(DISTINCT a.id) = :amenities_count\n" +
                    "                                     ORDER BY h.id\n" +
                    "                      )\n" +
                    "                        AND r.hotel = h.id\n" +
                    "                        AND r.id in\n" +
                    "                            (SELECT r.id\n" +
                    "                             FROM rooms r\n" +
                    "                             WHERE r.capacity >= :#{#filter.visitors}\n" +
                    "                               AND r.id NOT IN (\n" +
                    "                                 SELECT r.id\n" +
                    "                                 FROM calendars c\n" +
                    "                                 WHERE c.room = r.id\n" +
                    "                                   AND (((c.start_date <= :#{#filter.startDate} AND :#{#filter.endDate} <= c.end_date)\n" +
                    "                                     OR (c.start_date <= :#{#filter.endDate} AND :#{#filter.endDate} <= c.end_date)\n" +
                    "                                     OR (:#{#filter.startDate} < end_date AND :#{#filter.endDate} >= c.end_date)))\n" +
                    "                                 LIMIT 1\n" +
                    "                             )))"
            , nativeQuery = true)
    List<Object[]> findFloorAndCeilPrices(
            @Param("filter") PagedHotelsFilter filter,
            @Param("amenities") List<String> amenities, @Param("amenities_count") int amenities_count);

}

//    //Update pending provider to true.
//    @Transactional
//    @Modifying
//    @Query(value="UPDATE users SET pending_provider = true WHERE id = :user_id", nativeQuery = true)
//    int updatePendingProvider(@Param("user_id") Long user_id);