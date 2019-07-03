package gr.uoa.di.rent.repositories;

import gr.uoa.di.rent.models.Hotel;
import gr.uoa.di.rent.payload.requests.filters.PagedHotelsFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CustomHotelRepository {
    Page<Hotel> findHotelsByFilters(PagedHotelsFilter filter, List<String> amenities,
                                    int amenities_count, Pageable pageable);
}
