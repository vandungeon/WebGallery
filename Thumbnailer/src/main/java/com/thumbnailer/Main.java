package com.thumbnailer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import net.coobird.thumbnailator.Thumbnails;
import com.rabbitmq.client.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class Main {
    private static final Path THUMBNAIL_DIR_128 = Paths.get("/app/thumbnails", "128x128");
    private static final Path THUMBNAIL_DIR_256 = Paths.get("/app/thumbnails", "256x256");
    private static final Path THUMBNAIL_DIR_512 = Paths.get("/app/thumbnails", "512x512");

    private final ConnectionFactory factory;
    private final ObjectMapper objectMapper;

    public Main() {
        factory = new ConnectionFactory();
        factory.setHost("rabbitmq");
        factory.setPort(5672);
        factory.setUsername("guest");
        factory.setPassword("guest");
        objectMapper = new ObjectMapper();
    }
    public static void main(String[] args) throws Exception {
        Main app = new Main();
        app.startListening();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                app.factory.newConnection().close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
        System.out.println("Hello world!");
        Thread.currentThread().join();
    }

    public void startListening() throws Exception {
        Connection connection = factory.newConnection();  // Persistent connection
        Channel channel = connection.createChannel();
        String queueName = "thumbnailRequestQueue";
        channel.queueDeclare(queueName, true, false, false, null);

        System.out.println("Waiting for messages in " + queueName);

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            try {
                processThumbnailRequest(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        };

        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> { });
    }

    private void processThumbnailRequest(String message) throws IOException {
        JsonNode json = objectMapper.readTree(message);
        Long imageId = json.get("id").asLong();
        String filepath = json.get("filepath").asText();
        JsonNode sizesNode = json.get("sizes");
        System.out.println(message);

        List<Map<String, String>> thumbnailPaths = new ArrayList<>();
        for (JsonNode sizeNode : sizesNode) {
            int size = sizeNode.asInt();
            String thumbnailPath = createThumbnail(filepath, size);
            Map<String, String> thumbnailEntry = new HashMap<>();
            thumbnailEntry.put(String.valueOf(size), thumbnailPath);
            thumbnailPaths.add(thumbnailEntry);
        }

        Map<String, Object> response = Map.of(
                "id", imageId,
                "thumbnails", thumbnailPaths
        );

        String responseMessage = objectMapper.writeValueAsString(response);
        System.out.println(responseMessage);
        sendResponse(responseMessage);
    }

    private void sendResponse(String responseMessage) {
        System.out.println("Starting the process of sending paths back to web: " + responseMessage);
        try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {
            System.out.println("Try successful: " + responseMessage);
            String responseQueue = "thumbnailResponseQueue";
            channel.queueDeclare(responseQueue, true, false, false, null);
            channel.basicPublish("", responseQueue, null, responseMessage.getBytes("UTF-8"));
            System.out.println("Sent response: " + responseMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String createThumbnail(String fullpath, int size) throws IOException {
        Path originalImagePath = Paths.get(fullpath);

        File thumbnailDir = switch (size) {
            case 512 -> THUMBNAIL_DIR_512.toFile();
            case 256 -> THUMBNAIL_DIR_256.toFile();
            case 128 -> THUMBNAIL_DIR_128.toFile();
            default -> throw new IllegalArgumentException("Invalid thumbnail size: " + size);
        };


        if (!thumbnailDir.exists()) {
            thumbnailDir.mkdirs();
        }

        String filename = new File(fullpath).getName();
        Path thumbnailPath = switch (size) {
            case 512 -> THUMBNAIL_DIR_512.resolve(filename);
            case 256 -> THUMBNAIL_DIR_256.resolve(filename);
            case 128 -> THUMBNAIL_DIR_128.resolve(filename);
            default -> throw new IllegalArgumentException("Invalid thumbnail size: " + size);
        };


        Thumbnails.of(originalImagePath.toFile())
                .size(size, size)
                .toFile(thumbnailPath.toFile());

        return thumbnailPath.toString();
    }
}