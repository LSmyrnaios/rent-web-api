package gr.uoa.di.rent.repositories;

import com.google.common.base.CaseFormat;
import gr.uoa.di.rent.models.Hotel;
import gr.uoa.di.rent.payload.requests.filters.PagedHotelsFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;

public class CustomHotelRepositoryImpl implements CustomHotelRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Hotel> findHotelsByFilters(PagedHotelsFilter filter, List<String> amenities, int amenities_count, Pageable pageable) {

        Query query = entityManager.createNativeQuery(
                "SELECT h.*\n" +
                        "FROM hotels h\n" +
                        (amenities_count != 0 ? ", hotel_amenities ha,\n" +
                                "amenities a\n" : " ") +
                        "WHERE h.id in\n" +
                        "      (\n" +
                        "          SELECT hotelsfilter.id\n" +
                        "          FROM hotels hotelsfilter\n" +
                        "          WHERE hotelsfilter.id in\n" +
                        "              -- 1) GEOLOCATION RADIUS SEARCH\n" +
                        "                (\n" +
                        "                    SELECT h1.id\n" +
                        "                    FROM hotels h1\n" +
                        "                    WHERE (point(:lng, :lat) <@> point(lng, lat)) < :radius / 1.61\n" +
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
                        "                  WHERE r.capacity >= :visitors\n" +
                        "                    AND r.price >= :minPrice\n" +
                        "                    AND r.price <= :maxPrice\n" +
                        "                    AND r.id NOT IN (\n" +
                        "                      SELECT r.id\n" +
                        "                      FROM calendars c\n" +
                        "                      WHERE c.room = r.id\n" +
                        "                        AND (((c.start_date <= :startDate AND :endDate <= c.end_date)\n" +
                        "                          OR (c.start_date <= :endDate AND :endDate <= c.end_date)\n" +
                        "                          OR (:startDate < end_date AND :endDate >= c.end_date)))\n" +
                        "\n" +
                        "                      LIMIT 1 --- necessary maybe not\n" +
                        "                  ))))\n" +
                        "  -- 3) AMENITY SEARCH\n" +
                        (amenities_count != 0 ?
                                "  AND ha.amenity_id = a.id\n" +
                                        "  AND ha.hotel_id = h.id\n" +
                                        "  AND a.name in :amenities\n" +
                                        "GROUP BY h.id\n" +
                                        "HAVING COUNT(DISTINCT a.id) = :amenities_count\n" : " ") +
                        "ORDER BY h.\n" +
                        CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, filter.getSort_field()) +
                        " " + filter.getOrder(),
                Hotel.class
        );

        if (amenities_count != 0) {
            query.setParameter("amenities", amenities);
            query.setParameter("amenities_count", amenities_count);
        }
        query.setParameter("startDate", filter.getStartDate());
        query.setParameter("endDate", filter.getEndDate());
        query.setParameter("visitors", filter.getVisitors());
        query.setParameter("minPrice", filter.getMinPrice());
        query.setParameter("maxPrice", filter.getMaxPrice());
        query.setParameter("lat", filter.getLat());
        query.setParameter("lng", filter.getLng());
        query.setParameter("radius", filter.getRadius());
        query.setFirstResult((pageable.getPageNumber()) * pageable.getPageSize());
        query.setMaxResults(pageable.getPageSize());

        List hotels = query.getResultList();

        return new PageImpl<>(hotels, pageable, hotels.size());
    }
}
