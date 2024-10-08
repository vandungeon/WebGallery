package com.gallery.demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;

import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "images")
public class DataImage {

    @Id
    @Column(name = "imageId")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "fileName")
    private String name;
    @Column(name = "filePath")
    private String path;
    @Column(name = "thumbnails", length = 512)
    private String thumbnails;

    public DataImage() {}
    public String getName() {
        return name;
    }

    public String getThumbnails() {
        return thumbnails;
    }
    public Long getId() {
        return id;
    }

    public String getPath() {
        return path;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPath(String path) {
        this.path = path;
    }
    public void setThumbnails(String thumbnails) {
        this.thumbnails = thumbnails; // Setter for thumbnail paths
    }
    public DataImage(Long id, String name, String path, String path128, String path256, String path512) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        this.id = id;
        this.name = name;
        this.path = path;
        this.thumbnails = objectMapper.writeValueAsString(Map.of(
                "128x128", path128,
                "256x256", path256,
                "512x512", path512
        ));
    }
    public void setThumbnailPaths(String path128, String path256, String path512) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            this.thumbnails = objectMapper.writeValueAsString(Map.of(
                    "128x128", path128,
                    "256x256", path256,
                    "512x512", path512
            ));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
    public Map<String, String> getThumbnailPaths() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(this.thumbnails, Map.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

}
