package gr.uoa.di.rent.controllers;

import gr.uoa.di.rent.exceptions.*;
import gr.uoa.di.rent.models.*;
import gr.uoa.di.rent.payload.requests.HotelRequest;
import gr.uoa.di.rent.payload.requests.filters.PagedHotelsFilter;
import gr.uoa.di.rent.payload.responses.*;
import gr.uoa.di.rent.repositories.*;
import gr.uoa.di.rent.repositories.HotelRepository;
import gr.uoa.di.rent.security.CurrentUser;
import gr.uoa.di.rent.security.Principal;
import gr.uoa.di.rent.services.FileStorageService;
import gr.uoa.di.rent.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.net.URI;
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

        Business business = business_opt.get();

        /* Create a hotel object which will belong to that business. */
        Hotel hotel = hotelRequest.asHotel(business.getId());

        // Check if the hotel already exists in the database.
        Hotel hotelInDb = hotelRepository.findByNameOrEmail(hotel.getName(), hotel.getEmail());
        if ( hotelInDb != null ) {
            String errorMsg = "A hotel with the name: \"" + hotel.getName() + "\" or with the email: \"" + hotel.getEmail() + "\", already exists in the database!";
            logger.warn(errorMsg);
            return ResponseEntity.badRequest().body(errorMsg);
        }

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

        URI uri = UriBuilder.constructUri(hotel, null);
        if (uri == null)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unable to construct the hotel-URI");
        else
            return ResponseEntity.created(uri).body(new HotelResponse(hotel));
    }

    @GetMapping("/{hotelId:[\\d]+}")
    public ResponseEntity<?> getHotelByID(@PathVariable(value = "hotelId") Long hotelId) {

        Optional<Hotel> hotel_opt = hotelRepository.findById(hotelId);
        if (!hotel_opt.isPresent()) {
            logger.warn("No hotel exists with id = " + hotelId);
            return ResponseEntity.notFound().build();
        }

        Hotel hotel = hotel_opt.get();

        // Find and set the photo_urls of this hotel and return the related response.
        hotel.setPhotosUrls(PhotoUtils.getPhotoUrls(hotel, fileRepository, false));

        return ResponseEntity.ok(new HotelResponse(hotel));
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

        /* Get All Hotels  */
        Page<Hotel> hotels;
        int floor = AppConstants.MIN_ROOM_PRICE;
        int ceil = AppConstants.MAX_ROOM_PRICE;

        /* If no amenities were given, search only with the basic filters */
        if (!queryAmenities.isEmpty()) {
            hotels = hotelRepository.findHotelsByFilters(pagedHotelsFilters, queryAmenities, queryAmenities.size(), pageable);
            List<Object[]> floorAndCeil = hotelRepository.findFloorAndCeilPrices(pagedHotelsFilters, queryAmenities, queryAmenities.size());

            for (Object[] o : floorAndCeil) {
                if (o[0] != null && o[1] != null) {
                    floor = (int) o[0];
                    ceil = (int) o[1];
                }
            }
        } else {
            hotels = hotelRepository.findHotelsByFilters(pagedHotelsFilters, queryAmenities, queryAmenities.size(), pageable);
        }

        if (hotels.getNumberOfElements() == 0) {
            return new SearchResponse(floor, ceil,
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

        return new SearchResponse(floor, ceil,
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
    public List<ResponseEntity<?>> uploadHotelPhoto(@Valid @CurrentUser Principal principal, @RequestParam("file") MultipartFile file, @PathVariable(value = "hotelId") Long hotelId) {

        return PhotoUtils.handleUploadOfMultipleHotelOrRoomPhotos(principal, file, hotelId, fileController, userRepository, hotelRepository, false);
    }


    @GetMapping("/{hotelId:[\\d]+}/photos/{file_name:(?:[\\d]+.[\\w]{2,4})}")
    // Maybe no authorization should exist here as the hotel_photo should be public.
    public ResponseEntity<?> getHotelPhoto(@PathVariable(value = "hotelId") Long hotelId, @PathVariable(value = "file_name") String file_name, HttpServletRequest request) {

        return PhotoUtils.handleDownloadOfHotelOrRoomPhoto(request, file_name, hotelId, hotelRepository, fileStorageService, fileController, false);
    }
}
