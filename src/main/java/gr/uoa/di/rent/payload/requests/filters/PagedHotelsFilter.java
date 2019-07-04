package gr.uoa.di.rent.payload.requests.filters;

import gr.uoa.di.rent.util.AppConstants;
import javax.validation.constraints.FutureOrPresent;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

public class PagedHotelsFilter extends PagedRoomsFilter {

    /** * * * * * * * * * * * * *
     *    Hotel Basic Filters   *
     *  * * * * * * * * * * * * */
    @NotNull
    private double lat;
    @NotNull
    private double lng;

    @Max(AppConstants.MAX_RADIUS)
    private double radius = AppConstants.DEFAULT_RADIUS;

    /** * * * * * * * * * * * * *
     *     Amenities Filters    *
     *  * * * * * * * * * * * * */
    private boolean wifi = false;
    private boolean swimmingPool = false;
    private boolean gym = false;
    private boolean spa = false;
    private boolean bar = false;
    private boolean restaurant = false;
    private boolean petsAllowed = false;
    private boolean parking = false;
    private boolean roomService = false;


    public PagedHotelsFilter() {
        super();
    }

    public PagedHotelsFilter(int page, int size, String sort_field, String order,
                             @FutureOrPresent LocalDate startDate, @FutureOrPresent LocalDate endDate, int visitors,
                             @NotNull double lat, @NotNull double lng, double radius,
                             boolean wifi, boolean swimmingPool, boolean gym, boolean spa, boolean bar, boolean restaurant,
                             boolean petsAllowed, boolean parking, boolean roomService, int minPrice, int maxPrice) {
        super(page, size, sort_field, order, startDate, endDate, visitors, minPrice, maxPrice);
        this.lat = lat;
        this.lng = lng;
        this.radius = radius;
        this.wifi = wifi;
        this.swimmingPool = swimmingPool;
        this.gym = gym;
        this.spa = spa;
        this.bar = bar;
        this.restaurant = restaurant;
        this.petsAllowed = petsAllowed;
        this.parking = parking;
        this.roomService = roomService;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public boolean isGym() {
        return gym;
    }

    public void setGym(boolean gym) {
        this.gym = gym;
    }

    public boolean isSpa() {
        return spa;
    }

    public void setSpa(boolean spa) {
        this.spa = spa;
    }

    public boolean isBar() {
        return bar;
    }

    public void setBar(boolean bar) {
        this.bar = bar;
    }

    public boolean isWifi() {
        return wifi;
    }

    public void setWifi(boolean wifi) {
        this.wifi = wifi;
    }

    public boolean isParking() {
        return parking;
    }

    public void setParking(boolean parking) {
        this.parking = parking;
    }

    public boolean isRestaurant() {
        return restaurant;
    }

    public void setRestaurant(boolean restaurant) {
        this.restaurant = restaurant;
    }

    public boolean isPetsAllowed() {
        return petsAllowed;
    }

    public void setPetsAllowed(boolean petsAllowed) {
        this.petsAllowed = petsAllowed;
    }

    public boolean isRoomService() {
        return roomService;
    }

    public void setRoomService(boolean roomService) {
        this.roomService = roomService;
    }

    public boolean isSwimmingPool() {
        return swimmingPool;
    }

    public void setSwimmingPool(boolean swimmingPool) {
        this.swimmingPool = swimmingPool;
    }


    @Override
    public String toString() {
        return "PagedHotelsFilter{" +
                ", lat=" + lat +
                ", lng=" + lng +
                ", radius=" + radius +
                ", wifi=" + wifi +
                ", swimmingPool=" + swimmingPool +
                ", gym=" + gym +
                ", spa=" + spa +
                ", bar=" + bar +
                ", restaurant=" + restaurant +
                ", petsAllowed=" + petsAllowed +
                ", parking=" + parking +
                ", roomService=" + roomService +
                '}';
    }
}
