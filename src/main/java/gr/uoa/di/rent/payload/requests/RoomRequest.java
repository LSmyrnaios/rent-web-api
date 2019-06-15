package gr.uoa.di.rent.payload.requests;

import gr.uoa.di.rent.models.Room;

import javax.validation.constraints.NotNull;

public class RoomRequest {

    @NotNull
    private Integer roomNumber;

    @NotNull
    private Integer capacity;

    @NotNull
    private Integer price;

    public RoomRequest() {
    }

    public Room asRoom(Long hotel_id) {
        return new Room(
                this.getRoomNumber(),
                hotel_id,
                this.getCapacity(),
                this.getPrice()
        );
    }

    public Integer getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(Integer roomNumber) {
        this.roomNumber = roomNumber;
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
