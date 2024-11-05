package com.gallery.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ThumbnailService {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ImageRepository dataImageRepository;

    @Autowired
    public ThumbnailService(RabbitTemplate rabbitTemplate, ImageRepository dataImageRepository) {
        this.rabbitTemplate = rabbitTemplate;
        this.dataImageRepository = dataImageRepository;
    }
    @RabbitListener(queues = "${app.rabbitmq.thumbnailResponseQueue}")
    public void handleThumbnailResponse(String message) throws IOException {
        JsonNode json = objectMapper.readTree(message);
        Long imageId = json.get("id").asLong();
        JsonNode thumbnailsNode = json.get("thumbnails");

        Map<String, String> thumbnailPathsMap = new HashMap<>();
        for (JsonNode sizeNode : thumbnailsNode) {
            sizeNode.fields().forEachRemaining(entry -> {
                thumbnailPathsMap.put(entry.getKey(), entry.getValue().asText());
            });
        }
        updateDataImageWithThumbnails(imageId, thumbnailPathsMap);
    }


    private void updateDataImageWithThumbnails(Long imageId, Map<String, String> thumbnailPathsMap) {
        DataImage dataImage = dataImageRepository.findById(imageId).orElse(null);
        if (dataImage != null) {
            dataImage.setThumbnails(thumbnailPathsMap);
            dataImageRepository.save(dataImage);
        } else {
            System.err.println("DataImage not found for id: " + imageId);
        }
    }

}
