package gr.uoa.di.rent.controllers;

import gr.uoa.di.rent.exceptions.*;
import gr.uoa.di.rent.models.*;
import gr.uoa.di.rent.payload.requests.HotelRequest;
import gr.uoa.di.rent.payload.requests.filters.PagedHotelsFilter;
import gr.uoa.di.rent.payload.responses.*;
import gr.uoa.di.rent.repositories.*;
import gr.uoa.di.rent.security.CurrentUser;
import gr.uoa.di.rent.security.Principal;
import gr.uoa.di.rent.services.FileStorageService;
import gr.uoa.di.rent.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@RestController
@Validated
@RequestMapping("/hotels")
public class HotelController {

    private static final Logger logger = LoggerFactory.getLogger(HotelController.class);

    private final BusinessRepository businessRepository;

    private final HotelRepository hotelRepository;

    private final AmenitiesRepository amenitiesRepository;

    private final UserRepository userRepository;

    private final FileController fileController;

    private final FileRepository fileRepository;

    private final FileStorageService fileStorageService;

    private final AtomicInteger counter = new AtomicInteger();

    public static String hotelBaseURI = "https://localhost:8443/api/hotels/";
    private static String genericHotelPhotoName = "generic_hotel_photo.jpg";


    public HotelController(BusinessRepository businessRepository, HotelRepository hotelRepository, UserRepository userRepository, FileStorageService fileStorageService,
                           AmenitiesRepository amenitiesRepository, FileController fileController, FileRepository fileRepository) {
        this.businessRepository = businessRepository;
        this.hotelRepository = hotelRepository;
        this.amenitiesRepository = amenitiesRepository;
        this.fileController = fileController;
        this.userRepository = userRepository;
        this.fileRepository = fileRepository;
        this.fileStorageService = fileStorageService;
    }


    @PostMapping("")
    @PreAuthorize("hasRole('PROVIDER') or hasRole('ADMIN')")
    public ResponseEntity<?> createHotel(@Valid @RequestBody HotelRequest hotelRequest, @Valid @CurrentUser Principal principal) {

        User provider = principal.getUser();

        /* Get business for this provider (principal). */
        Optional<Business> business_opt = businessRepository.findById(provider.getBusiness().getId());
        if (!business_opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("No business was found for provider with username: " + provider.getUsername());
        }

        logger.debug(business_opt.get().toString());

        Business business = business_opt.get();

        /* Create a hotel object which will belong to that business. */
        Hotel hotel = hotelRequest.asHotel(business.getId());
        hotel.setBusiness(business);

        List<Room> rooms = new ArrayList<>();
        hotel.setRooms(rooms);

        List<File> hotel_photos = new ArrayList<>();
        hotel.setHotel_photos(hotel_photos);

        /* Get list of amenities from request */
        Collection<Amenity> amenities = amenitiesRepository.findAll()
                .stream()
                .filter(amenity -> hotelRequest.getAmenities().contains(amenity.getName()))
                .collect(Collectors.toList());

        hotel.setAmenities(amenities);

        /* Store the hotel in the database. */
        hotel = hotelRepository.save(hotel);

        return ResponseEntity.ok(new HotelResponse(hotel, null));
    }

    @GetMapping("/{hotelId:[\\d]+}")
    public ResponseEntity<?> getHotelByID(@PathVariable(value = "hotelId") Long hotelId) {

        Optional<Hotel> hotel_opt = hotelRepository.findById(hotelId);
        if ( !hotel_opt.isPresent() ) {
            logger.warn("No hotel exists with id = " + hotelId);
            return ResponseEntity.notFound().build();
        }

        // Find and set the photo_urls of this hotel.
        Hotel hotel = hotel_opt.get();

        List<String> hotel_photo_urls = null;

        List<File> hotel_photos = fileRepository.findAllByHotel(hotel);
        if ( hotel_photos != null ) {
            hotel_photo_urls = new ArrayList<>();
            for ( File hotel_photo : hotel_photos ) {
                hotel_photo_urls.add(hotel_photo.getFileDownloadUri());
            }
        }

        return ResponseEntity.ok(new HotelResponse(hotel, hotel_photo_urls));
    }

    @GetMapping("/search")
    public SearchResponse searchHotels(@Valid PagedHotelsFilter pagedHotelsFilters) {

        try {
            PaginatedResponseUtil
                    .validateParameters(pagedHotelsFilters.getPage(), pagedHotelsFilters.getSize(), pagedHotelsFilters.getSort_field(), Hotel.class);
        } catch (BadRequestException bre) {
            throw bre;
        } catch (Exception e) {
            throw new BadRequestException("Instantiation problem!");
        }

        Sort.Direction sort_order;

        /* Default order is ASC, otherwise DESC */
        if (AppConstants.DEFAULT_ORDER.equals(pagedHotelsFilters.getOrder()))
            sort_order = Sort.Direction.ASC;
        else
            sort_order = Sort.Direction.DESC;

        /* Create a list with all the amenity filters that are to be applied */
        Field[] fields = pagedHotelsFilters.getClass().getDeclaredFields();

        List<String> queryAmenities = new ArrayList<>();
        Arrays.stream(fields)
                .filter(field -> {
                            field.setAccessible(true);
                            try {
                                return field.get(pagedHotelsFilters).equals(true) && AppConstants.amenity_names.contains(field.getName());
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                                throw new BadRequestException("An error occurred accessing a field when trying to create the amenity filter list!");
                            }
                        }
                )
                .map(Field::getName)
                .forEach(queryAmenities::add);

        Pageable pageable = PageRequest.of(pagedHotelsFilters.getPage(), pagedHotelsFilters.getSize(),
                sort_order, pagedHotelsFilters.getSort_field());

        double radius_km = 1000000000000000.0;

        /* Get All Hotels  */
        Page<Hotel> hotels;

        /* If no amenities were given, search only with the basic filters */
        //TODO Add price range and rating filters to the sql query.
        if (!queryAmenities.isEmpty()) {
            hotels = hotelRepository.findWithFilters(
                    pagedHotelsFilters.getStart_date(), pagedHotelsFilters.getEnd_date(),
                    pagedHotelsFilters.getLng(), pagedHotelsFilters.getLat(), radius_km,
                    pagedHotelsFilters.getVisitors(),
                    queryAmenities, queryAmenities.size(),
                    pageable);
        } else {
            hotels = hotelRepository.findAll(pageable);
        }

        if (hotels.getNumberOfElements() == 0) {
            return new SearchResponse(0, 200,
                    new AmenitiesCount(0,
                            0,
                            0,
                            0,
                            0,
                            0,
                            0,
                            0,
                            0),
                    new PagedResponse<>(Collections.emptyList(), hotels.getNumber(), hotels.getSize(),
                            hotels.getTotalElements(), hotels.getTotalPages(), hotels.isLast()));
        }

        List<Hotel> hotelResponses = hotels.map(ModelMapper::mapHoteltoHotelResponse).getContent();

        return new SearchResponse(111, 222,
                new AmenitiesCount(1,
                        2,
                        3,
                        4,
                        4,
                        5,
                        6,
                        7,
                        10),
                new PagedResponse<>(hotelResponses, hotels.getNumber(), hotels.getSize(), hotels.getTotalElements(),
                        hotels.getTotalPages(), hotels.isLast())
        );
    }

    /* PHOTOS */

    @PostMapping("/{hotelId:[\\d]+}/photos")
    @PreAuthorize("hasRole('PROVIDER') or hasRole('ADMIN')")
    public List<ResponseEntity<?>> uploadHotelPhotos(@Valid @CurrentUser Principal principal, @RequestParam("files") MultipartFile[] files, @PathVariable(value = "hotelId") Long hotelId) {

        return PhotoUtils.handleUploadOfMultipleHotelOrRoomPhotos(principal, files, hotelId, null, fileController, hotelRepository, null, userRepository, false);
    }


    @GetMapping("/{hotelId:[\\d]+}/photos/{file_name:(?:[\\d]+.[\\w]{2,4})}")
    // Maybe no authorization should exist here as the hotel_photo should be public.
    public ResponseEntity<?> getHotelPhoto(@PathVariable(value = "hotelId") Long hotelId, @PathVariable(value = "file_name") String file_name, HttpServletRequest request) {

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

        String fileFullPath = fileStoragePath + java.io.File.separator + provider.getId() + java.io.File.separator  + "hotels" + java.io.File.separator + hotelId + java.io.File.separator + "photos" + java.io.File.separator + file_name;

        Resource resource;
        try {
            resource = fileStorageService.loadFileAsResource(fileFullPath);
        } catch (FileNotFoundException fnfe) {
            logger.warn("The photo of hotel with id <" + hotel.getId()  +">, with fileName: \"" + file_name + "\" was not found in storage! Returning the \"genericHotelPhoto\"..");
            // Load the genericHotelPhoto.
            fileFullPath = AppConstants.localImageDirectory + java.io.File.separator + genericHotelPhotoName;
            try {
                resource = fileStorageService.loadFileAsResource(fileFullPath);
            } catch (FileNotFoundException fnfe2) {
                logger.error("The \"" + genericHotelPhotoName + "\" was not found in storage! Returning the \"imageNotFoundPhoto\"..");
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
