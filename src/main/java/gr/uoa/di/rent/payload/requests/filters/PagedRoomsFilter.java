package gr.uoa.di.rent.payload.requests.filters;

import gr.uoa.di.rent.util.AppConstants;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.FutureOrPresent;
import javax.validation.constraints.Max;
import java.time.LocalDate;

public class PagedRoomsFilter extends PagedResponseFilter {

    /** * * * * * * * * * * * * *
     *     Room Filters         *
     *  * * * * * * * * * * * * */

    @FutureOrPresent
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate = AppConstants.DEFAULT_START_DATE;

    @FutureOrPresent
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate = AppConstants.DEFAULT_END_DATE;

    @Max(AppConstants.MAX_VISITORS_NUMBER)
    private int visitors = AppConstants.DEFAULT_VISITORS_NUMBER;
    private int minPrice = AppConstants.DEFAULT_MIN_ROOM_PRICE;
    private int maxPrice = AppConstants.DEFAULT_MAX_ROOM_PRICE;

    PagedRoomsFilter() {
        super();
    }

    PagedRoomsFilter(int page, int size, String sort_field, String order,
                     @FutureOrPresent LocalDate startDate, @FutureOrPresent LocalDate endDate,
                     @Max(AppConstants.MAX_VISITORS_NUMBER) int visitors, int minPrice, int maxPrice) {
        super(page, size, sort_field, order);
        this.startDate = startDate;
        this.endDate = endDate;
        this.visitors = visitors;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public int getVisitors() {
        return visitors;
    }

    public void setVisitors(int visitors) {
        this.visitors = visitors;
    }

    public int getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(int minPrice) {
        this.minPrice = minPrice;
    }

    public int getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(int maxPrice) {
        this.maxPrice = maxPrice;
    }

    @Override
    public String toString() {
        return "PagedRoomsFilter{" +
                "startDate=" + startDate +
                ", endDate=" + endDate +
                ", visitors=" + visitors +
                ", minPrice=" + minPrice +
                ", maxPrice=" + maxPrice +
                '}';
    }
}
