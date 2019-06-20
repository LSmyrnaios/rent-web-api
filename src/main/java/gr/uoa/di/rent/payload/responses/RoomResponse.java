package gr.uoa.di.rent.payload.responses;

import gr.uoa.di.rent.models.Room;

import java.util.List;


public class RoomResponse {

    private Room room;

    private List<String> room_photo_urls;

    public RoomResponse() {}

    public RoomResponse(Room room, List<String> room_photo_urls) {
        this.room = room;
        this.room_photo_urls = room_photo_urls;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public List<String> getRoom_photo_urls() {
        return room_photo_urls;
    }

    public void setRoom_photo_urls(List<String> room_photo_urls) {
        this.room_photo_urls = room_photo_urls;
    }

    @Override
    public String toString() {
        return "RoomResponse{" +
                "room=" + room +
                ", room_photo_urls= [" + room_photo_urls + "]" +
                '}';
    }
}
