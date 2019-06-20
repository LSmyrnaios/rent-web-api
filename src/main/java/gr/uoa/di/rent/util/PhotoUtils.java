package gr.uoa.di.rent.util;

import gr.uoa.di.rent.controllers.FileController;
import gr.uoa.di.rent.controllers.HotelController;
import gr.uoa.di.rent.exceptions.FileNotFoundException;
import gr.uoa.di.rent.exceptions.NotAuthorizedException;
import gr.uoa.di.rent.exceptions.UserNotExistException;
import gr.uoa.di.rent.models.Hotel;
import gr.uoa.di.rent.models.Room;
import gr.uoa.di.rent.models.User;
import gr.uoa.di.rent.repositories.HotelRepository;
import gr.uoa.di.rent.repositories.RoomRepository;
import gr.uoa.di.rent.repositories.UserRepository;
import gr.uoa.di.rent.security.Principal;
import gr.uoa.di.rent.services.FileStorageService;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PhotoUtils {

    private static final Logger logger = LoggerFactory.getLogger(PhotoUtils.class);

    private static String genericHotelPhotoName = "generic_hotel_photo.jpg";

    private static String genericRoomPhotoName = "generic_room_photo.jpg";


    public static List<ResponseEntity<?>> handleUploadOfMultipleHotelOrRoomPhotos(Principal principal, MultipartFile[] files, Long hotelId, Long roomId,
                                                                                  FileController fileController, HotelRepository hotelRepository, RoomRepository roomRepository, UserRepository userRepository,
                                                                                  Boolean isForRoomPhotos) {

        List<ResponseEntity<?>> responses = new ArrayList<>();

        if ( files == null || files.length == 0 ) {
            String errorMessage = "No files received!";
            logger.error(errorMessage);
            responses.add(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage));
            return responses;
        }

        // Check if the hotel exists.
        Optional<Hotel> hotel_opt = hotelRepository.findById(hotelId);
        if ( !hotel_opt.isPresent() ) {
            String errorMessage = "No hotel exists with id = " + hotelId;
            logger.error(errorMessage);
            responses.add(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage));
            return responses;
        }
        Hotel hotel = hotel_opt.get();  // Used also for file-insertion later.

        // Check if the provider which will have a new photo for its hotel, exists or not.
        Long providerId = hotel.getBusiness().getProvider().getId();

        // If current provider is not Admin and the given "hotelId" doesn't belong to a hotel owned by the current provider requesting, then return error.
        if ( !principal.getUser().getId().equals(providerId) && !principal.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")) ) {
            throw new NotAuthorizedException("You are not authorized to update the data of another user!");
        }

        Room room = null;
        if ( isForRoomPhotos ) {
            if (roomRepository == null ) {
                String errorMessage = "The received \"roomRepository\" was null!";
                logger.error(errorMessage);
                responses.add(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage));
                return responses;
            }

            // Check if the room exists.
            Optional<Room> room_opt = roomRepository.findById(roomId);
            if ( !room_opt.isPresent() ) {
                String errorMessage = "No room exists with id = " + roomId;
                logger.error(errorMessage);
                responses.add(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage));
                return responses;
            }
            room = room_opt.get();
        }

        // Get the provider from the database in order to get passed to the "uploadFile()".
        User user = userRepository.findById(providerId)
                .orElseThrow(() -> new UserNotExistException("User with id <" + providerId + "> does not exist!"));

        String baseDownloadURI = HotelController.hotelBaseURI + hotelId;
        if ( isForRoomPhotos ) {
            baseDownloadURI += "/rooms/" + roomId;
        }
        baseDownloadURI += "/photos/";

        for ( int i = 0 ; i < files.length ; i++ )
        {
            MultipartFile file = files[i];

            if ( file == null ) {
                logger.warn("Found a null-file in files-list..! Continuing with the next..");
                continue;
            }

            // Change the filename to have an incremental value, specific for this hotel or room.
            String fileName = file.getOriginalFilename();
            if ( fileName == null ) {
                String errorMessage = "Failure when retrieving the filename of the incoming file!";
                logger.error(errorMessage);
                responses.add(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage));
                continue;
            }

            String fileExtension = FilenameUtils.getExtension(fileName)
                    .toLowerCase(); // Linux are case insensitive, so make all file-extensions to lowerCase.

            fileName = (i+1) + "." + fileExtension;

            String fileDownloadURI = baseDownloadURI + fileName;

            // Send file to be stored. We set a new principal in order for the following method to know the user-provider who will have a new photo for its hotel, who provider, might not be the current user (the current user might be the admin who changes the photo of a hotel).
            responses.add(fileController.uploadFile(Principal.getInstance(user), file, fileName, null, fileDownloadURI, hotel, room));
        }

        return responses;
    }


    public static ResponseEntity<?> handleDownloadOfHotelOrRoomPhoto(HttpServletRequest request, String file_name, Long hotelId, Long roomId,
                                                                     HotelRepository hotelRepository, RoomRepository roomRepository,
                                                                     FileStorageService fileStorageService, FileController fileController)
    {
        // First check if the hotel exists.
        Optional<Hotel> hotel_opt = hotelRepository.findById(hotelId);
        if ( !hotel_opt.isPresent() ) {
            String errorMsg = "No hotel exists with id = " + hotelId;
            logger.error(errorMsg);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMsg);
        }
        Hotel hotel = hotel_opt.get();  // Used also for file-insertion later.

        boolean isForRoomPhoto = (roomId != null);
        if ( isForRoomPhoto ) {
            if (roomRepository == null ) {
                String errorMessage = "The received \"roomRepository\" was null!";
                logger.error(errorMessage);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
            }

            // Check if the requested room exists.
            Optional<Room> room_opt = roomRepository.findById(roomId);
            if ( !room_opt.isPresent() ) {
                String errorMessage = "No room exists with id = " + roomId;
                logger.error(errorMessage);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
            }
        }

        User provider = hotel.getBusiness().getProvider();

        // Set the file-location.
        String roleNameDirectory = UsersControllerUtil.getRoleNameDirectory(provider);
        String fileStoragePath = Paths.get(fileStorageService.getFileStorageLocation() + java.io.File.separator + roleNameDirectory).toString();

        String fileFullPath = fileStoragePath + java.io.File.separator + provider.getId() + java.io.File.separator  + "hotels" + java.io.File.separator + hotelId + java.io.File.separator;

        if ( isForRoomPhoto ) {
            fileFullPath += "rooms" + java.io.File.separator + roomId + java.io.File.separator;
        }
        fileFullPath += "photos" + java.io.File.separator + file_name;

        Resource resource;
        try {
            resource = fileStorageService.loadFileAsResource(fileFullPath);
        } catch (FileNotFoundException fnfe) {
            // Load the genericHotelPhoto.
            String genericPhoto;
            if ( isForRoomPhoto ) {
                logger.warn("The photo of hotel with id <" + hotel.getId()  +">, with fileName: \"" + file_name + "\" was not found in storage! Returning the \"genericHotelPhoto\"..");
                genericPhoto = genericRoomPhotoName;
            } else {    // We have a room.
                logger.warn("The photo of room <" + roomId + "> of hotel <" + hotel.getId()  +">, with fileName: \"" + file_name + "\" was not found in storage! Returning the \"genericHotelPhoto\"..");
                genericPhoto = genericHotelPhotoName;
            }
            fileFullPath = AppConstants.localImageDirectory + java.io.File.separator + genericPhoto;
            try {
                resource = fileStorageService.loadFileAsResource(fileFullPath);
            } catch (FileNotFoundException fnfe2) {
                logger.error("The \"" + genericPhoto + "\" was not found in storage! Returning the \"imageNotFoundPhoto\"..");
                // Loading the "image_not_found", so that the user will be notified that sth's wrong with the storage of its picture, even though one was given.
                fileFullPath = AppConstants.localImageDirectory + java.io.File.separator + AppConstants.imageNotFoundName;
                try {
                    resource = fileStorageService.loadFileAsResource(fileFullPath);
                } catch (FileNotFoundException fnfe3) {
                    String errorMsg = "The \"" + AppConstants.imageNotFoundName + "\" was not found in storage! 404";
                    logger.error(errorMsg);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMsg);
                }
            }
        }

        return fileController.GetFileResponse(request, resource);
    }

}
