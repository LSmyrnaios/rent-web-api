package gr.uoa.di.rent.services;

import gr.uoa.di.rent.exceptions.FileStorageException;
import gr.uoa.di.rent.exceptions.FileNotFoundException;
import gr.uoa.di.rent.models.Hotel;
import gr.uoa.di.rent.models.User;
import gr.uoa.di.rent.properties.FileStorageProperties;
import gr.uoa.di.rent.repositories.FileRepository;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.File;
import java.io.FileFilter;
import java.nio.file.*;

@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);


    private final Path fileStorageLocation;

    private final FileRepository fileRepository;

    @Autowired
    public FileStorageService(FileStorageProperties fileStorageProperties, FileRepository fileRepository) throws FileStorageException {

        this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir()).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new FileStorageException("Could not create the directory where the uploaded files will be stored.", ex);
        }
        this.fileRepository = fileRepository;
    }

    public Path getFileStorageLocation() { return fileStorageLocation; }

    public gr.uoa.di.rent.models.File storeFile(MultipartFile file, String fileName, String innerDir, String fileDownloadUri, User uploader, Hotel hotel, boolean isForRooms)
            throws FileStorageException {

        if ( file == null ) {
            String errMsg = "File was null!";
            logger.error(errMsg);
            throw new FileStorageException(errMsg);
        }

        String file_name;

        if ( fileName != null )
            file_name = StringUtils.cleanPath(fileName);
        else {
            fileName = file.getOriginalFilename();
            if ( fileName == null ) {
                String errMsg = "FileName was null!";
                logger.error(errMsg);
                throw new FileStorageException(errMsg);
            }
            file_name = StringUtils.cleanPath(fileName);
        }

        if ( fileDownloadUri == null )  // If it's not specified by a specific endpoint (e.g. uploadProfilePhoto).
            fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath().path("/files/download/").path(file_name).toUriString();

        gr.uoa.di.rent.models.File objectFile = new gr.uoa.di.rent.models.File(file_name, file.getContentType(), file.getSize(), fileDownloadUri, uploader, hotel, isForRooms);

        Path path;
        try {
            if ( innerDir != null ) {
                path = Paths.get(this.fileStorageLocation.toString() + File.separator + innerDir).normalize();
                Files.createDirectories(path);

                if ( (hotel == null) && innerDir.contains("photos") ) {   // Only the "profile-photos" will enter here.
                    // Make sure we delete any previous profile_photo which may have different file-extension (which will cause nothing to be replaced, just two profile_photos to co-exist).
                    File dir = new File(path.toString());
                    FileFilter fileFilter = new WildcardFileFilter("profile_photo.*");
                    File[] files = dir.listFiles(fileFilter);
                    if ( files != null ) {
                        for (File matchedFile : files)
                            if ( !matchedFile.delete() )
                                logger.error("Could not delete file: " + matchedFile.getAbsolutePath());
                    }
                }
            }
            else
                path = this.fileStorageLocation;

            // Check if the file's name contains invalid characters
            if ( file_name.contains("..") ) {
                throw new FileStorageException("Sorry! Filename contains invalid path sequence: " + file_name);
            }

            // Save the file to the target location (Replacing existing file with the same name)
            Path targetLocation = path.resolve(file_name);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return fileRepository.save(objectFile); // The fileID will be assigned by the database.

        } catch (FileNotFoundException fnfe) {
            throw fnfe; // Avoid creating a new exception-object + keeping the original stack-trace.
        } catch (Exception e) {
            logger.error("e", e);
            throw new FileStorageException("Could not store file: " + file_name + ". Please try again!", e);
        }
    }

    public Resource loadFileAsResource(String fileName) throws FileStorageException {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new FileNotFoundException("File not found " + fileName);
            }
        } catch ( FileNotFoundException fnfe) {
            throw fnfe; // Avoid creating a new exception-object + keeping the original stack-trace.
        } catch (Exception e) {
            logger.error("e", e);
            throw new FileNotFoundException("File not found " + fileName, e);
        }
    }
}
