package gr.uoa.di.rent.controllers;

import gr.uoa.di.rent.exceptions.ApiError;
import gr.uoa.di.rent.exceptions.NotAuthorizedException;
import gr.uoa.di.rent.models.*;
import gr.uoa.di.rent.payload.requests.ReservationRequest;
import gr.uoa.di.rent.payload.requests.RoomRequest;
import gr.uoa.di.rent.payload.requests.filters.PagedRoomsFilter;
import gr.uoa.di.rent.payload.responses.PagedResponse;
import gr.uoa.di.rent.payload.responses.RoomResponse;
import gr.uoa.di.rent.repositories.*;
import gr.uoa.di.rent.security.CurrentUser;
import gr.uoa.di.rent.security.Principal;
import gr.uoa.di.rent.services.FileStorageService;
import gr.uoa.di.rent.util.ModelMapper;
import gr.uoa.di.rent.util.PaginatedResponseUtil;
import gr.uoa.di.rent.util.PhotoUtils;
import gr.uoa.di.rent.util.UriBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static java.time.temporal.ChronoUnit.DAYS;

@RestController
@Validated
@RequestMapping("/hotels/{hotelId}/rooms")
public class RoomController {

    private static final Logger logger = LoggerFactory.getLogger(RoomController.class);

    private final RoomRepository roomRepository;

    private final HotelRepository hotelRepository;

    private final CalendarRepository calendarRepository;

    private final TransactionRepository transactionRepository;

    private final UserRepository userRepository;

    private final FileStorageService fileStorageService;

    private final FileController fileController;

    private final FileRepository fileRepository;

    private final AtomicInteger counter = new AtomicInteger();


    public RoomController(RoomRepository roomRepository, HotelRepository hotelRepository, UserRepository userRepository, FileStorageService fileStorageService,
                          CalendarRepository calendarRepository, TransactionRepository transactionRepository, FileController fileController, FileRepository fileRepository) {
        this.roomRepository = roomRepository;
        this.hotelRepository = hotelRepository;
        this.calendarRepository = calendarRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.fileController = fileController;
        this.fileStorageService = fileStorageService;
        this.fileRepository = fileRepository;
    }


    @GetMapping("")
    public ResponseEntity<?> getHotelRooms(@PathVariable(value = "hotelId") Long hotelId, PagedRoomsFilter pagedRoomsFilter) {

        // Check if the given hotel exists.
        Optional<Hotel> hotel_opt = hotelRepository.findById(hotelId);
        if ( !hotel_opt.isPresent() ) {
            logger.warn("No hotel exists with id = " + hotelId);
            return ResponseEntity.badRequest().build();
        }
        Hotel hotel = hotel_opt.get();

        Pageable pageable = PaginatedResponseUtil.getPageable(pagedRoomsFilter);

        Page<Room> rooms = roomRepository.findAllByHotel_id(hotelId, pageable);
        if (rooms.getNumberOfElements() == 0) {
            return ResponseEntity.status(HttpStatus.OK).body(new PagedResponse<>(Collections.emptyList(), rooms.getNumber(),
                    rooms.getSize(), rooms.getTotalElements(), rooms.getTotalPages(), rooms.isLast()));
        }

        List<Room> roomResponses = rooms.map(ModelMapper::mapRoomtoRoomResponse).getContent();

        // Set the hotelPhotosUrls.
        List<String> photosUrls = PhotoUtils.getPhotoUrls(hotel, fileRepository, true);
        for ( Room room : roomResponses )
            room.setPhotosUrls(photosUrls);

        return ResponseEntity.status(HttpStatus.OK).body(new PagedResponse<>(roomResponses, rooms.getNumber(),
                rooms.getSize(), rooms.getTotalElements(), rooms.getTotalPages(), rooms.isLast()));
    }


    @PostMapping("")
    @PreAuthorize("hasRole('PROVIDER') or hasRole('ADMIN')")
    public ResponseEntity<?> insertRooms(
            @PathVariable(value = "hotelId") Long hotelId,
            @Valid @CurrentUser Principal current_user,
            @Valid @RequestBody List<RoomRequest> requests) {

        // Check if the given hotel exists.
        Optional<Hotel> hotel_opt = hotelRepository.findById(hotelId);
        if ( !hotel_opt.isPresent() )
            return ResponseEntity.badRequest().body("No hotel exists with id = " + hotelId);

        Hotel hotel = hotel_opt.get();

        // Check if the current-user is the hotel-owner or if it's the admin, otherwise throw a "NotAuthorizedException".
        if ( !current_user.getUser().getId().equals(hotel.getBusiness().getProvider().getId())
                && !current_user.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")) )
            throw new NotAuthorizedException("You are not authorized to add rooms in hotel <" + hotel.getName() + "> !");

        hotel.setNumberOfRooms(hotel.getNumberOfRooms() + requests.size());

        for ( RoomRequest req : requests ) {
            // Check if room# already exists.
            Integer roomNumber = req.getRoomNumber();
            Optional<Room> room_opt = roomRepository.findByHotelAndRoomNumber(hotel, roomNumber);
            if ( room_opt.isPresent() ) {
                logger.warn("The room with number <" + roomNumber + "> already exists inside the hotel with id <" + hotelId + ">! Will not insert it twice..");
                continue;
            }

            Room r = req.asRoom(hotel);

            roomRepository.save(r);
        }

        URI uri = UriBuilder.constructUri(hotel, "/rooms");
        if ( uri == null )
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unable to construct the rooms-URI for hotel with id: <" + hotel.getId() + ">!");
        else
            return ResponseEntity.created(uri).body("Rooms successfully created!");
    }


    @GetMapping("/{roomId:[\\d]+}")
    public ResponseEntity<?> getHotelRoom(@PathVariable(value = "hotelId") Long hotelId, @PathVariable(value = "roomId") Long roomId) {

        // Check if the given hotel exists.
        Optional<Hotel> hotel_opt = hotelRepository.findById(hotelId);
        if ( !hotel_opt.isPresent() ) {
            logger.warn("No hotel exists with id = " + hotelId);
            return ResponseEntity.badRequest().build();
        }

        // Get the requested room.
        Optional<Room> room_opt = roomRepository.findById(roomId);
        if ( !room_opt.isPresent() ) {
            logger.warn("No room with id = " + roomId + " was found in hotel with id = " + hotelId);
            return ResponseEntity.notFound().build();
        }
        Room room = room_opt.get();

        // Find and set the photo_urls of this room.
        room.setPhotosUrls(PhotoUtils.getPhotoUrls(hotel_opt.get(), fileRepository, true));

        return ResponseEntity.ok(new RoomResponse(room));
    }


    @PostMapping("/{roomId:[\\d]+}/reservation")
    @PreAuthorize("hasRole('USER') or hasRole('PROVIDER')")
    public ResponseEntity<?> reservation(
            @CurrentUser Principal principal, @PathVariable(value = "hotelId") Long hotelId,
            @PathVariable(value = "roomId") Long roomId, @Valid @RequestBody ReservationRequest reservationRequest
    ) {
        // Check if hotel exists
        Optional<Hotel> hotel_opt = hotelRepository.findById(hotelId);
        if (!hotel_opt.isPresent())
            return ResponseEntity.badRequest().body(
                    new ApiError(
                            HttpStatus.BAD_REQUEST,
                            "Hotel does not exist!",
                            Collections.singletonList("No hotel was found with id " + hotelId)));

        // Check if room exists.
        Optional<Room> room_opt = roomRepository.findById(roomId);
        if (!room_opt.isPresent())
            return ResponseEntity.badRequest().body(
                    new ApiError(
                            HttpStatus.BAD_REQUEST,
                            "The requested room was not found in the requested hotel!",
                            Collections.singletonList("Room with id " + roomId + " was not found in hotel with id " + hotelId)));

        Room room = room_opt.get();

        // Check if valid date format given
        if (reservationRequest.getEndDate().isBefore(reservationRequest.getStartDate()))
            return ResponseEntity.badRequest().body(
                    new ApiError(
                            HttpStatus.BAD_REQUEST,
                            "Check-out day cannot be before Check-in day!",
                            Collections.singletonList("Check-out day " + reservationRequest.getEndDate() +
                                    " cannot be before Check-in day " + reservationRequest.getStartDate())));

        // Check if reservation is for 0 days   ( ex:  from  30/6/2019 to  30/6/2019 )
        if (reservationRequest.getEndDate().isEqual(reservationRequest.getStartDate()))
            return ResponseEntity.badRequest().body(
                    new ApiError(
                            HttpStatus.BAD_REQUEST,
                            "Cannot reserve a room for 0 Days!",
                            Collections.singletonList("Check-out date " + reservationRequest.getEndDate() +
                                    " is the same as Check-in date " + reservationRequest.getStartDate())));

        // Check calendar (if already reserved one of these days)
        if (!isAvailable(reservationRequest.getStartDate(), reservationRequest.getEndDate(), roomId))
            return ResponseEntity.badRequest().body(
                    new ApiError(
                            HttpStatus.BAD_REQUEST,
                            "The requested room is not available these days!",
                            Collections.singletonList("Room " + roomId + " of hotel " + hotelId +
                                    " is not available all the days between " + reservationRequest.getStartDate() + " and " +
                                    reservationRequest.getEndDate())));

        int total_price = (int) (room.getPrice() * DAYS.between(reservationRequest.getStartDate(), reservationRequest.getEndDate()));

        User currentUser = principal.getUser();

        Transaction transaction = new Transaction(currentUser, hotel_opt.get().getBusiness(), total_price);

        Calendar calendar = new Calendar(
                reservationRequest.getStartDate(),
                reservationRequest.getEndDate(),
                null, room
        );
        Reservation reservation = new Reservation(room, null, calendar);

        calendar.setReservation(reservation);

        Transaction transaction_s = transactionRepository.save(transaction);
        reservation.setTransaction(transaction_s);

        calendarRepository.save(calendar);

        //execute sql query to subtract money from user, and add said money to business' wallet + admin's business wallet
        hotelRepository.transferMoney(currentUser.getId(), hotelId, (double) total_price);

        return ResponseEntity.ok().body("Room Successfully Booked!");
    }

    private boolean isAvailable(LocalDate startDate, LocalDate endDate, Long roomID) {
        List<Calendar> calendars = calendarRepository.getOverlappingCalendars(startDate, endDate, roomID);
        return calendars.isEmpty();
    }


    /* PHOTOS */

    @PostMapping("/photos")
    @PreAuthorize("hasRole('PROVIDER') or hasRole('ADMIN')")
    public List<ResponseEntity<?>> uploadRoomsPhoto(@Valid @CurrentUser Principal principal, @RequestParam("file") MultipartFile file,
                                                     @PathVariable(value = "hotelId") Long hotelId) {

        return PhotoUtils.handleUploadOfMultipleHotelOrRoomPhotos(principal, file, hotelId, fileController, userRepository, hotelRepository, true);
    }

    @GetMapping("/photos/{file_name:(?:[\\d]+.[\\w]{2,4})}")
    // Maybe no authorization should exist here as the hotel_photo should be public.
    public ResponseEntity<?> getRoomPhoto(@PathVariable(value = "hotelId") Long hotelId, @PathVariable(value = "file_name") String file_name, HttpServletRequest request) {

        return PhotoUtils.handleDownloadOfHotelOrRoomPhoto(request, file_name, hotelId, hotelRepository, fileStorageService, fileController, true);
    }

}
