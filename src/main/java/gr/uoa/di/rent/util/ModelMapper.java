package gr.uoa.di.rent.util;

import gr.uoa.di.rent.models.Hotel;
import gr.uoa.di.rent.models.Room;
import gr.uoa.di.rent.models.User;

public class ModelMapper {

    public static User mapUserToUserResponse(User user) {
        return user;
       /* return new UserResponse(new User(user.getId(), user.getUsername(), user.getPassword(),
                user.getEmail(), user.getRole(), user.getLocked(), user.getIsPendingProvider(),
                user.getProfile(), null));*/
    }

    public static Hotel mapHoteltoHotelResponse(Hotel hotel) {
        return hotel;
    }

    public static Room mapRoomtoRoomResponse(Room room) {
        return room;
    }
}
