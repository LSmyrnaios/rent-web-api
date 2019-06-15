package gr.uoa.di.rent.payload.requests;

import gr.uoa.di.rent.models.Room;

import javax.validation.constraints.NotNull;

public class RoomRequest {

    @NotNull
    private Integer room_number;

    @NotNull
    private Integer capacity;

    @NotNull
    private Integer price;

    public RoomRequest() {
    }

    public Room asRoom(Long hotel_id) {
        return new Room(
                this.getRoom_number(),
                hotel_id,
                this.getCapacity(),
                this.getPrice()
        );
    }

    public Integer getRoom_number() {
        return room_number;
    }

    public void setRoom_number(Integer room_number) {
        this.room_number = room_number;
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
}
