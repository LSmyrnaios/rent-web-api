package gr.uoa.di.rent.util;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public interface AppConstants {

    int DEFAULT_PAGE_NUMBER = 0;
    int DEFAULT_PAGE_SIZE = 10;
    int MAX_PAGE_SIZE = 50;

    // Used to return both users and provider in paginated responses.
    // Otherwise, if role=2 ,then only the users will be returned. If role=3 then only the providers will be returned. (the role=1 refers to the ADMIN who is never returned)
    int DEFAULT_ROLE = -1;
    String DEFAULT_ORDER = "asc";
    String DEFAULT_FIELD = "id";

    // Default reservation start and end dates
    LocalDate DEFAULT_START_DATE = LocalDate.now().plusDays(1);
    LocalDate DEFAULT_END_DATE = LocalDate.now().plusDays(2);

    // Default visitors number and max visitors number
    int DEFAULT_VISITORS_NUMBER = 1;
    long MAX_VISITORS_NUMBER = 15;

    // Default radius and max radius
    double DEFAULT_RADIUS = 5.0;
    long MAX_RADIUS = 30;

    // Default min and max room price
    int DEFAULT_MIN_ROOM_PRICE = 0;
    int DEFAULT_MAX_ROOM_PRICE = 1000;

    List<String> amenity_names = Arrays.asList(
            "wifi",
            "swimmingPool",
            "gym",
            "spa",
            "bar",
            "restaurant",
            "petsAllowed",
            "parking",
            "roomService"
    );

    String currentDirectory = System.getProperty("user.dir");
    String localResourcesDirectory = currentDirectory + java.io.File.separator + "src" + java.io.File.separator + "main" + java.io.File.separator + "resources";
    String localImageDirectory = localResourcesDirectory + java.io.File.separator + "img";
    String imageNotFoundName = "image_not_found.png";
}
