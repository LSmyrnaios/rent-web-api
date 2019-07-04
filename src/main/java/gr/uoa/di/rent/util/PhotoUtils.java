package gr.uoa.di.rent.util;

import gr.uoa.di.rent.controllers.FileController;
import gr.uoa.di.rent.controllers.HotelController;
import gr.uoa.di.rent.exceptions.FileNotFoundException;
import gr.uoa.di.rent.exceptions.NotAuthorizedException;
import gr.uoa.di.rent.exceptions.UserNotExistException;
import gr.uoa.di.rent.models.File;
import gr.uoa.di.rent.models.Hotel;
import gr.uoa.di.rent.models.User;
import gr.uoa.di.rent.repositories.FileRepository;
import gr.uoa.di.rent.repositories.HotelRepository;
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


    public static ResponseEntity<?> handleUploadOfMultipleHotelOrRoomPhotos(Principal principal, MultipartFile file, Long hotelId, FileController fileController,
                                                                                  UserRepository userRepository, HotelRepository hotelRepository, boolean isForRooms)
    {
        if ( file == null ) {
            String errorMessage = "No file received!";
            logger.error(errorMessage);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
        }

        // Check if the hotel exists.
        Optional<Hotel> hotel_opt = hotelRepository.findById(hotelId);
        if ( !hotel_opt.isPresent() ) {
            String errorMessage = "No hotel exists with id = " + hotelId;
            logger.error(errorMessage);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
        }
        Hotel hotel = hotel_opt.get();  // Used also for file-insertion later.

        // Check if the provider which will have a new photo for its hotel, exists or not.
        Long providerId = hotel.getBusiness().getProvider().getId();

        // If current provider is not Admin and the given "hotelId" doesn't belong to a hotel owned by the current provider requesting, then return error.
        if ( !principal.getUser().getId().equals(providerId) && !principal.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")) ) {
            throw new NotAuthorizedException("You are not authorized to update the data of another user!");
        }

        // Get the provider from the database in order to get passed to the "uploadFile()".
        User user = userRepository.findById(providerId)
                .orElseThrow(() -> new UserNotExistException("User with id <" + providerId + "> does not exist!"));

        String baseDownloadURI = HotelController.hotelBaseURI + hotelId;
        if ( isForRooms ) {
            baseDownloadURI += "/rooms";
        }
        baseDownloadURI += "/photos/";

        // Change the filename to have an incremental value, specific for this hotel or room.
        String fileName = file.getOriginalFilename();
        if ( fileName == null ) {
            String errorMessage = "Failure when retrieving the filename of the incoming file!";
            logger.error(errorMessage);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
        }

        String fileExtension = FilenameUtils.getExtension(fileName)
                .toLowerCase(); // Linux are case insensitive, so make all file-extensions to lowerCase.

        fileName = getPhotosCounter(hotel, isForRooms) + "." + fileExtension;

        String fileDownloadURI = baseDownloadURI + fileName;

        // Send file to be stored. We set a new principal in order for the following method to know the user-provider who will have a new photo for its hotel, who provider, might not be the current user (the current user might be the admin who changes the photo of a hotel).
        return fileController.uploadFile(Principal.getInstance(user), file, fileName, null, fileDownloadURI, hotel, isForRooms);
    }


    public static ResponseEntity<?> handleDownloadOfHotelOrRoomPhoto(HttpServletRequest request, String file_name, Long hotelId, HotelRepository hotelRepository,
                                                                     FileStorageService fileStorageService, FileController fileController, boolean isForRooms)
    {
        // First check if the hotel exists.
        Optional<Hotel> hotel_opt = hotelRepository.findById(hotelId);
        if ( !hotel_opt.isPresent() ) {
            String errorMsg = "No hotel exists with id = " + hotelId;
            logger.error(errorMsg);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMsg);
        }
        Hotel hotel = hotel_opt.get();  // Used also for file-insertion later.

        User provider = hotel.getBusiness().getProvider();

        // Set the file-location.
        String roleNameDirectory = UsersControllerUtil.getRoleNameDirectory(provider);
        String fileStoragePath = Paths.get(fileStorageService.getFileStorageLocation() + java.io.File.separator + roleNameDirectory).toString();

        String fileFullPath = fileStoragePath + java.io.File.separator + provider.getId() + java.io.File.separator  + "hotels" + java.io.File.separator + hotelId + java.io.File.separator;

        if ( isForRooms ) {
            fileFullPath += "rooms" + java.io.File.separator;
        }
        fileFullPath += "photos" + java.io.File.separator + file_name;

        Resource resource;
        try {
            resource = fileStorageService.loadFileAsResource(fileFullPath);
        } catch (FileNotFoundException fnfe) {
            // Load the genericHotelPhoto.
            String genericPhoto;
            if ( isForRooms ) {
                logger.warn("The rooms-photo of hotel with id <" + hotel.getId()  +">, with fileName: \"" + file_name + "\" was not found in storage! Returning the \"genericRoomPhotoName\"..");
                genericPhoto = genericRoomPhotoName;
            } else {    // We have a room.
                logger.warn("The photo of hotel with id <" + hotel.getId()  +">, with fileName: \"" + file_name + "\" was not found in storage! Returning the \"genericHotelPhoto\"..");
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


    private static Integer getPhotosCounter(Hotel hotel, boolean isForRooms)
    {
        Integer photosCounter;

        if ( isForRooms ) {
            photosCounter = hotel.getRoomsPhotosCounter();
            photosCounter ++;
            hotel.setRoomsPhotosCounter(photosCounter);
        } else {
            photosCounter = hotel.getHotelPhotosCounter();
            photosCounter ++;
            hotel.setHotelPhotosCounter(photosCounter);
        }

        return photosCounter;
    }


    public static List<String> getPhotoUrls(Hotel hotel, FileRepository fileRepository, boolean isForRooms)
    {
        List<String> photo_urls = null;
        List<File> photos = fileRepository.findAllByHotelAndIsForRooms(hotel, isForRooms);
        if ( photos != null ) {
            photo_urls = new ArrayList<>();
            for ( File photo : photos )
                photo_urls.add(photo.getFileDownloadUri());
        }
        return photo_urls;
    }

}
