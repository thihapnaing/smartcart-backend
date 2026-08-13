package nus.iss.smartcart.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import nus.iss.smartcart.backend.exception.ImageUploadException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageUploadServiceTest {

    @Mock private Cloudinary cloudinary;
    @Mock private Uploader uploader;

    @InjectMocks private ImageUploadService imageUploadService;

    @Test
    void uploadImage_validFile_returnsSecureUrl() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        Map<String, Object> fakeResult = Map.of("secure_url", "https://res.cloudinary.com/demo/sample.jpg");
        when(uploader.upload(any(), any())).thenReturn(fakeResult);

        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getSize()).thenReturn(1024L);
        when(file.getBytes()).thenReturn(new byte[]{1, 2, 3});

        String response = imageUploadService.uploadImage(file);
        assertEquals("https://res.cloudinary.com/demo/sample.jpg",response);
    }

    @Test
    void uploadImage_emptyFile_throwsImageUploadException() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        assertThrows(ImageUploadException.class, () -> imageUploadService.uploadImage(file));
    }

    @Test
    void uploadImage_invalidContentType_throwsImageUploadException() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("application/pdf");

        assertThrows(ImageUploadException.class, () -> imageUploadService.uploadImage(file));
    }

    @Test
    void uploadImage_fileTooLarge_throwsImageUploadException() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getSize()).thenReturn(11L * 1024 * 1024);

        assertThrows(ImageUploadException.class, () -> imageUploadService.uploadImage(file));
    }
}
