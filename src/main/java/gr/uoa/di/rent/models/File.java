package gr.uoa.di.rent.models;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import gr.uoa.di.rent.models.audit.UserDateAudit;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "files", schema = "rent")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id",
        "uploader_id",
        "filename",
        "filetype",
        "filesize",
        "fileDownloadUri",
        "isForRooms"
})
public class File extends UserDateAudit implements Serializable {

    @Id
    @Column(name = "id")
    @JsonProperty("id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploader", nullable = false)
    @JsonIgnore
    private User uploader;

    @Transient
    @JsonProperty("uploader_id")
    private Long uploader_id;

    @Column(name = "filename", nullable = false)
    @JsonProperty("filename")
    private String filename;

    @Column(name = "filetype", nullable = false, length = 50)
    @JsonProperty("filetype")
    private String filetype;

    @Column(name = "filesize", nullable = false)
    @JsonProperty("filesize")
    private Long filesize;

    @Column(name = "file_download_uri", nullable = false)
    @JsonProperty("fileDownloadUri")
    private String fileDownloadUri;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "hotel")
    @JsonIgnore
    private Hotel hotel;

    @Column(name = "is_for_rooms", nullable = false)
    @JsonProperty("isForRooms")
    private Boolean isForRooms;

    public File() {
    }

    public File(String filename, String filetype, Long filesize, String fileDownloadUri, User uploader, Hotel hotel, boolean isForRooms) {
        this.filename = filename;
        this.filetype = filetype;
        this.filesize = filesize;
        this.fileDownloadUri = fileDownloadUri;
        this.setUploader(uploader); // Sets also the "uploader_id"-field.
        this.hotel = hotel;
        this.isForRooms = isForRooms;
    }

    public Long getId() {
        return id;
    }

    public Long setId() {
        return id;
    }

    public User getUploader() {
        return uploader;
    }

    public void setUploader(User uploader) {
        this.uploader = uploader;
        this.setUploader_id(uploader.getId());
    }

    public Long getUploader_id() {
        return uploader_id;
    }

    public void setUploader_id(Long uploader_id) {
        this.uploader_id = uploader_id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFiletype() {
        return filetype;
    }

    public void setFiletype(String filetype) {
        this.filetype = filetype;
    }

    public Long getFilesize() {
        return filesize;
    }

    public void setFilesize(Long filesize) {
        this.filesize = filesize;
    }

    public String getFileDownloadUri() {
        return fileDownloadUri;
    }

    public void setFileDownloadUri(String fileDownloadUri) {
        this.fileDownloadUri = fileDownloadUri;
    }

    public Hotel getHotel() {
        return hotel;
    }

    public void setHotel(Hotel hotel) {
        this.hotel = hotel;
    }

    public Boolean isForRooms() {
        return isForRooms;
    }

    public void setIsForRooms(Boolean forRooms) {
        isForRooms = forRooms;
    }

    @Override
    public String toString() {
        return "File{" +
                "id=" + id +
                ", uploader_id=" + uploader_id +
                ", filename='" + filename + '\'' +
                ", filetype='" + filetype + '\'' +
                ", filesize=" + filesize +
                ", fileDownloadUri='" + fileDownloadUri + '\'' +
                ", setIsForRooms=" + isForRooms +
                '}';
    }
}
