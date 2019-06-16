package gr.uoa.di.rent.controllers;

import gr.uoa.di.rent.exceptions.UploadFileException;
import gr.uoa.di.rent.models.Hotel;
import gr.uoa.di.rent.models.Room;
import gr.uoa.di.rent.models.User;
import gr.uoa.di.rent.models.File;
import gr.uoa.di.rent.payload.responses.UploadFileResponse;
import gr.uoa.di.rent.security.CurrentUser;
import gr.uoa.di.rent.security.Principal;
import gr.uoa.di.rent.services.FileStorageService;
import gr.uoa.di.rent.util.UsersControllerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/files")
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/upload")
    @PreAuthorize("hasRole('USER')or hasRole('PROVIDER') or hasRole('ADMIN')")
    public ResponseEntity<?> uploadFile(@Valid @CurrentUser Principal principal, @Valid @RequestParam("file") MultipartFile file,
                                        String fileName, String innerDir, String fileDownloadUri, Hotel hotel, Room room) {
        User currentUser = principal.getUser();

        if ( file == null ) {
            String errorMsg = "Received file object was null!";
            logger.error(errorMsg);
            throw new UploadFileException(errorMsg);
        }

        String file_name;
        if ( fileName == null ) {
            file_name = file.getOriginalFilename();
            if ( file_name == null ) {
                String errorMsg = "Failure when retrieving the filename of the incoming file!";
                logger.error(errorMsg);
                throw new UploadFileException(errorMsg);
            }
        } else
            file_name = fileName;

        if ( innerDir == null || innerDir.contains("photos") )
        {
            String role = UsersControllerUtil.getRoleNameDirectory(currentUser);
            String tempInnerDir = innerDir;
            innerDir = java.io.File.separator + role + java.io.File.separator + currentUser.getId() + java.io.File.separator;

            if ( hotel != null ) {
                innerDir += "hotels" + java.io.File.separator + hotel.getId() + java.io.File.separator;

                if ( room != null )
                    innerDir += "room" + java.io.File.separator + room.getId() + java.io.File.separator;

                innerDir += "photos";
            }
            else if ( "photos".equals(tempInnerDir) ) { // Avoid NPE.
                innerDir += tempInnerDir;
            }
        }

        File objectFile = fileStorageService.storeFile(file, file_name, innerDir, fileDownloadUri, currentUser, hotel, room);

        return ResponseEntity.ok(new UploadFileResponse(objectFile));
    }

    @PostMapping("/multiple")
    @PreAuthorize("hasRole('USER')or hasRole('PROVIDER') or hasRole('ADMIN')")
    public List<ResponseEntity<?>> uploadMultipleFiles(@Valid @CurrentUser Principal principal, @RequestParam("files") MultipartFile[] files) {
        return Arrays.asList(files)
                .stream()
                .map(file -> uploadFile(principal, file, file.getOriginalFilename(), null, null, null, null))
                .collect(Collectors.toList());
    }

    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, HttpServletRequest request) {
        // Load file as Resource
        Resource resource = fileStorageService.loadFileAsResource(fileName);

        return GetFileResponse(request, resource);
    }

    public ResponseEntity<Resource> GetFileResponse(HttpServletRequest request, Resource resource) {

        // Try to determine file's content type
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            logger.info("Could not determine file type.");
        }

        // Fallback to the default content type if type could not be determined
        if ( contentType == null ) {
            contentType = "application/octet-stream";
        }
        /*else
            logger.debug("File: \"" + resource.getFilename() + "\" has contentType: \"" + contentType + "\"");*/

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

}
