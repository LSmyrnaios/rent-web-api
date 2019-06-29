package gr.uoa.di.rent.repositories;

import gr.uoa.di.rent.models.File;
import gr.uoa.di.rent.models.Hotel;
import gr.uoa.di.rent.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface FileRepository extends JpaRepository<File, Long> {

    List<File> findAll();

    // Get all the files belonging to this user.
    List<File> findAllByUploader(User user);

    // Get all the photos belonging to this hotel.
    List<File> findAllByHotel(Hotel hotel);

    // Get all the photos belonging to this room.
    List<File> findAllByHotelAndIsForRooms(Hotel hotel, Boolean isForRooms);

    @Transactional
    void deleteAllByUploaderAndHotelAndIsForRooms(User user, Hotel hotel, Boolean isForRooms);
}
