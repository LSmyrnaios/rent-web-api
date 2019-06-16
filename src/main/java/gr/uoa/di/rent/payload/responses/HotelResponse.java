package gr.uoa.di.rent.payload.responses;

import gr.uoa.di.rent.models.Hotel;

import java.util.List;


public class HotelResponse {

    private Hotel hotel;

    private List<String> hotel_photo_urls;

    public HotelResponse() {
    }

    public HotelResponse(Hotel hotel, List<String> hotel_photo_urls) {
        this.hotel = hotel;
        this.hotel_photo_urls = hotel_photo_urls;
    }

    public Hotel getHotel() {
        return hotel;
    }

    public void setHotel(Hotel hotel) {
        this.hotel = hotel;
    }

    public List<String> getHotel_photo_urls() {
        return hotel_photo_urls;
    }

    public void setHotel_photo_urls(List<String> hotel_photo_urls) {
        this.hotel_photo_urls = hotel_photo_urls;
    }

    @Override
    public String toString() {
        return "HotelResponse{" +
                "hotel=" + hotel +
                ", hotel_photo_urls= [" + hotel_photo_urls + "]" +
                '}';
    }
}
