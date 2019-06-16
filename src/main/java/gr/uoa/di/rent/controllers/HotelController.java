package gr.uoa.di.rent.controllers;

import gr.uoa.di.rent.exceptions.BadRequestException;
import gr.uoa.di.rent.exceptions.NotAuthorizedException;
import gr.uoa.di.rent.exceptions.UserNotExistException;
import gr.uoa.di.rent.models.*;
import gr.uoa.di.rent.payload.requests.HotelRequest;
import gr.uoa.di.rent.payload.requests.filters.PagedHotelsFilter;
import gr.uoa.di.rent.payload.responses.*;
import gr.uoa.di.rent.repositories.*;
import gr.uoa.di.rent.security.CurrentUser;
import gr.uoa.di.rent.security.Principal;
import gr.uoa.di.rent.util.AppConstants;
import gr.uoa.di.rent.util.ModelMapper;
import gr.uoa.di.rent.util.PaginatedResponseUtil;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.lang.reflect.Field;
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

    private final AtomicInteger counter = new AtomicInteger();

    private static String hotelBaseURI = "https://localhost:8443/api/hotels/";

    public HotelController(BusinessRepository businessRepository, HotelRepository hotelRepository, UserRepository userRepository,
                           AmenitiesRepository amenitiesRepository, FileController fileController, FileRepository fileRepository) {
        this.businessRepository = businessRepository;
        this.hotelRepository = hotelRepository;
        this.amenitiesRepository = amenitiesRepository;
        this.fileController = fileController;
        this.userRepository = userRepository;
        this.fileRepository = fileRepository;
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
            PaginatedResponseUtil.validateParameters(pagedHotelsFilters.getPage(), pagedHotelsFilters.getSize(),
                    pagedHotelsFilters.getSort_field(), Hotel.class);
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


    @PostMapping("/{hotelId:[\\d]+}/photos")
    @PreAuthorize("hasRole('PROVIDER') or hasRole('ADMIN')")
    public List<ResponseEntity<?>> uploadHotelPhotos(@RequestParam("files") MultipartFile[] files, @PathVariable(value = "hotelId") Long hotelId, @Valid @CurrentUser Principal principal) {

        List<ResponseEntity<?>> responses = new ArrayList<>();

        if ( files == null || files.length == 0 ) {
            String errorMessage = "No files received!";
            logger.error(errorMessage);
            responses.add(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage));
            return responses;
        }

        // First check if the hotel exists.
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

        // Get the provider from the database in order to get passed to the "uploadFile()".
        User user = userRepository.findById(providerId)
                .orElseThrow(() -> new UserNotExistException("User with id <" + providerId + "> does not exist!"));

        for ( int i = 0 ; i < files.length ; i++ )
        {
            MultipartFile file = files[i];

            if ( file == null ) {
                logger.warn("Found a null-file in files-list..! Continuing with the next..");
                continue;
            }

            // Change the filename to have an incremental value, specific for this hotel.
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

            String fileDownloadURI = hotelBaseURI + hotelId + "/photos/" + fileName;

            // Send file to be stored. We set a new principal in order for the following method to know the user-provider who will have a new photo for its hotel, who provider, might not be the current user (the current user might be the admin who changes the photo of a hotel).
            responses.add(fileController.uploadFile(Principal.getInstance(user), file, fileName, null, fileDownloadURI, hotel, null));
        }

        return responses;
    }


    // TODO - Implement endpoint to return multiple photos of a hotel.

}
