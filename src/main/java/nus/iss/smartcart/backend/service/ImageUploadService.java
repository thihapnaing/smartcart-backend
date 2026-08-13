package nus.iss.smartcart.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import nus.iss.smartcart.backend.exception.ImageUploadException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

@Service
public class ImageUploadService {

    private final Cloudinary cloudinary;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/png", "image/jpeg");
    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024; //10MB


    public ImageUploadService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public String uploadImage(MultipartFile file) {
        if(file.isEmpty()) {
            throw new ImageUploadException("File is empty");
        }
        if(!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new ImageUploadException("Only PNG and JPEG images are allowed");
        }
        if(file.getSize() > MAX_SIZE_BYTES) {
            throw new ImageUploadException("File size must not exceed 10MB");
        }
        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
            return (String) result.get("secure_url");
        } catch (IOException e) {
            throw new ImageUploadException("Failed to upload image", e);
        }
    }
}
