package nus.iss.smartcart.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import nus.iss.smartcart.backend.dto.ImageSearchResponse;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageSearchClient {

    @Value("${ai.python-service.base-url}")
    private String aiBaseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ImageSearchResponse search(MultipartFile image) {

        try (CloseableHttpClient client = HttpClients.createDefault()) {

            HttpPost post =
                    new HttpPost(aiBaseUrl + "/api/image-search");

            HttpEntity entity =
                    MultipartEntityBuilder.create()
                            .addBinaryBody(
                                    "file",
                                    image.getBytes(),
                                    ContentType.DEFAULT_BINARY,
                                    image.getOriginalFilename()
                            )
                            .build();

            post.setEntity(entity);

            System.out.println("================================");
            System.out.println("Calling AI Service...");
            System.out.println(post.getUri());
            System.out.println("================================");

            try (CloseableHttpResponse response =
                         client.execute(post)) {

                String json =
                        EntityUtils.toString(response.getEntity());

                System.out.println("HTTP Status : "
                        + response.getCode());

                System.out.println(json);

                if (response.getCode() != 200) {

                    throw new RuntimeException(
                            "AI Service returned HTTP "
                                    + response.getCode()
                                    + "\n"
                                    + json
                    );

                }

                return objectMapper.readValue(
                        json,
                        ImageSearchResponse.class
                );

            }

        } catch (Exception ex) {

            ex.printStackTrace();

            throw new RuntimeException(ex);

        }

    }

}