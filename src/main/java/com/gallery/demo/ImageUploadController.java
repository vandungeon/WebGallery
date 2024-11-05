package com.gallery.demo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class ImageUploadController {
    private static final Path UPLOAD_DIR = Paths.get("/app/images");
    private static final Path UPLOAD_THUMBNAILS = Paths.get("/");
    private static final Path THUMBNAIL_DIR_128 = Paths.get("/app/thumbnails", "128x128");
    private static final Path THUMBNAIL_DIR_256 = Paths.get("/app/thumbnails", "256x256");
    private static final Path THUMBNAIL_DIR_512 = Paths.get("/app/thumbnails", "512x512");
    @Autowired
    private RabbitTemplate rabbitTemplate;  // Inject RabbitTemplate here
    @Autowired
    private ImageRepository dataImageRepository;
    @GetMapping("/checkThumbnail/{filename}/{size}")
    public ResponseEntity<Boolean> checkThumbnail(@PathVariable String filename, @PathVariable String size) {
        String thumbnailPath = String.format("%s/%s/%s_%s.jpg", THUMBNAIL_DIR_256, size, filename, size);
        boolean exists = Files.exists(Paths.get(thumbnailPath));
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/uploads/{filename:.+}")
    public ResponseEntity<byte[]> getImage(@PathVariable String filename) throws Exception {
        Path path = Paths.get(UPLOAD_DIR + "/" + filename);
        byte[] imageBytes = Files.readAllBytes(path);

        MediaType mediaType = MediaType.IMAGE_JPEG;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentLength(imageBytes.length);

        return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
    }
   @GetMapping("/uploadsT")
    public ResponseEntity<byte[]> getImageT(@RequestParam String json, @RequestParam String size) throws Exception {

        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, String>>() {}.getType();
        Map<String, String> thumbnailPaths = gson.fromJson(json, type);

        String fullPath = thumbnailPaths.get(size);


        if (fullPath == null || !Files.exists(Paths.get(fullPath))) {
            return ResponseEntity.notFound().build();
        }

        byte[] imageBytes = Files.readAllBytes(Paths.get("" + fullPath));

        MediaType mediaType = MediaType.IMAGE_JPEG;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentLength(imageBytes.length);

        return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/")
    public String index(Model model) {
        try {
            List<DataImage> imageList = dataImageRepository.findAll();
            model.addAttribute("images", imageList);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "index";
    }


@PostMapping("/upload")
public String uploadImage(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
    if (file.isEmpty()) {
        redirectAttributes.addFlashAttribute("message", "Please select a file to upload.");
        return "redirect:/";
    }

    try {
        Path imagePath = Paths.get(UPLOAD_DIR + "/" + file.getOriginalFilename());
        Files.write(imagePath, file.getBytes());

        DataImage dataImage = new DataImage(null, file.getOriginalFilename(), imagePath.toString());
        dataImageRepository.save(dataImage);

        Map<String, Object> message = Map.of(
                "id", dataImage.getId(),
                "filepath", imagePath.toString(),
                "sizes", List.of(128, 256, 512)
        );
        rabbitTemplate.convertAndSend("thumbnailRequestQueue", new ObjectMapper().writeValueAsString(message));

        redirectAttributes.addFlashAttribute("message", "Successfully uploaded '" + file.getOriginalFilename() + "'.");

    } catch (IOException e) {
        e.printStackTrace();
    }

    return "redirect:/";
}


}
