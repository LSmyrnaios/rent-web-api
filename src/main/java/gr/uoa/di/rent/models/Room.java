package gr.uoa.di.rent.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import gr.uoa.di.rent.models.audit.UserDateAudit;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

@Entity
@Table(name = "rooms", schema = "rent")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id",
        "roomNumber",
        "hotelId",
        "capacity",
})
public class Room extends UserDateAudit implements Serializable {

    @Id
    @Column(name = "id")
    @JsonProperty("id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_number")
    @JsonProperty("roomNumber")
    private Integer roomNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hotel", nullable = false)
    @JsonIgnore
    private Hotel hotel;

    @Transient
    @JsonProperty("hotelId")
    private Long hotelId;

    @Column(name = "capacity")
    @JsonProperty("capacity")
    private Integer capacity;

    @Column(name = "price", nullable = false)
    @JsonProperty("price")
    private Integer price;

    @OneToMany(mappedBy = "room", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Calendar> calendars;

    @OneToMany(mappedBy = "room", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<File> room_photos;

    public Room() {
    }

    public Room(Integer roomNumber, Hotel hotel, Integer capacity, Integer price) {
        this.roomNumber = roomNumber;
        this.setHotel(hotel);
        this.capacity = capacity;
        this.price = price;
    }

    // Used by the RoomRequest (In which we don't have the hotel-object, just its id)
    public Room(Integer roomNumber, Long hotelId, Integer capacity, Integer price) {
        this.roomNumber = roomNumber;
        this.hotelId = hotelId;
        this.capacity = capacity;
        this.price = price;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(Integer roomNumber) {
        this.roomNumber = roomNumber;
    }

    public Hotel getHotel() {
        return hotel;
    }

    public void setHotel(Hotel hotel) {
        this.hotel = hotel;
        this.hotelId = hotel.getId();
    }

    public Long getHotelId() {
        return hotelId;
    }

    public void setHotelId(Long hotelId) {
        this.hotelId = hotelId;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }

    public List<Calendar> getCalendars() {
        return calendars;
    }

    public void setCalendars(List<Calendar> calendars) {
        this.calendars = calendars;
    }

    public List<File> getRoom_photos() {
        return room_photos;
    }

    public void setRoom_photos(List<File> room_photos) {
        this.room_photos = room_photos;
    }

    @Override
    public String toString() {
        return "Room{" +
                "id=" + id +
                ", roomNumber=" + roomNumber +
                ", hotelId=" + hotelId +
                ", capacity=" + capacity +
                ", calendars=" + calendars +
                '}';
    }
}
