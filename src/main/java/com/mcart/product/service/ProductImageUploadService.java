package com.mcart.product.service;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.mcart.product.dto.GalleryImageRequest;
import com.mcart.product.dto.ProductRequest;
import com.mcart.product.dto.ProductResponse;
import com.mcart.product.dto.ProductUploadGalleryImageRequest;
import com.mcart.product.dto.ProductUploadRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductImageUploadService {

    private final ProductService productService;

    @Value("${app.catalog.bucket-name:}")
    private String bucketName;

    @Value("${app.catalog.image-prefix:products}")
    private String imagePrefix;

    public Mono<ProductResponse> createProductWithImages(
            ProductUploadRequest request,
            List<FilePart> files
    ) {
        if (bucketName == null || bucketName.isBlank()) {
            return Mono.error(new IllegalStateException("app.catalog.bucket-name must be configured"));
        }

        Map<String, FilePart> fileByName = files.stream()
                .collect(Collectors.toMap(FilePart::filename, f -> f, (a, b) -> a));

        Storage storage = StorageOptions.getDefaultInstance().getService();
        String skuSlug = slug(request.getSku());

        List<Mono<GalleryImageRequest>> uploads = new ArrayList<>();
        for (int i = 0; i < request.getGallery().size(); i++) {
            ProductUploadGalleryImageRequest image = request.getGallery().get(i);
            int index = i + 1;
            uploads.add(uploadGalleryImage(storage, skuSlug, index, image, fileByName));
        }

        log.info("Product image upload (create) filePartCount={} gallerySlots={}", files.size(), request.getGallery().size());
        return Mono.zip(uploads, arr -> Arrays.stream(arr)
                        .map(x -> (GalleryImageRequest) x)
                        .collect(Collectors.toList()))
                .flatMap(gallery -> {
                    ProductRequest createRequest = ProductRequest.builder()
                            .name(request.getName())
                            .description(request.getDescription())
                            .price(request.getPrice())
                            .sku(request.getSku())
                            .stockQuantity(request.getStockQuantity())
                            .categories(request.getCategories())
                            .brand(request.getBrand())
                            .gallery(gallery)
                            .rating(request.getRating())
                            .inStock(request.getInStock())
                            .attributes(request.getAttributes())
                            .build();
                    return productService.createProduct(createRequest);
                });
    }

    public Mono<ProductResponse> appendGalleryImages(
            String productId,
            List<ProductUploadGalleryImageRequest> galleryMeta,
            List<FilePart> files
    ) {
        if (bucketName == null || bucketName.isBlank()) {
            return Mono.error(new IllegalStateException("app.catalog.bucket-name must be configured"));
        }
        if (galleryMeta == null || galleryMeta.isEmpty()) {
            return Mono.error(new IllegalArgumentException("gallery manifest must not be empty"));
        }

        Map<String, FilePart> fileByName = files.stream()
                .collect(Collectors.toMap(FilePart::filename, f -> f, (a, b) -> a));
        Storage storage = StorageOptions.getDefaultInstance().getService();

        log.info("Product image upload (append) productId={} filePartCount={} newSlots={}", productId, files.size(), galleryMeta.size());
        return productService.getProductById(productId)
                .flatMap(product -> {
                    String skuSlug = slug(product.getSku());
                    int existing = product.getGallery() != null ? product.getGallery().size() : 0;
                    List<Mono<GalleryImageRequest>> uploads = new ArrayList<>();
                    for (int i = 0; i < galleryMeta.size(); i++) {
                        int index = existing + i + 1;
                        uploads.add(uploadGalleryImage(storage, skuSlug, index, galleryMeta.get(i), fileByName));
                    }
                    return Mono.zip(uploads, arr -> Arrays.stream(arr)
                                    .map(x -> (GalleryImageRequest) x)
                                    .collect(Collectors.toList()))
                            .flatMap(gallery -> productService.appendGallery(productId, gallery));
                });
    }

    private Mono<GalleryImageRequest> uploadGalleryImage(
            Storage storage,
            String skuSlug,
            int index,
            ProductUploadGalleryImageRequest request,
            Map<String, FilePart> fileByName
    ) {
        FilePart thumbPart = fileByName.get(request.getThumbFile());
        if (thumbPart == null) {
            return Mono.error(new IllegalArgumentException("Missing file part: " + request.getThumbFile()));
        }
        FilePart hdPart = request.getHdFile() == null || request.getHdFile().isBlank()
                ? thumbPart
                : fileByName.get(request.getHdFile());
        if (hdPart == null) {
            return Mono.error(new IllegalArgumentException("Missing file part: " + request.getHdFile()));
        }

        String thumbExt = extension(thumbPart.filename());
        String hdExt = extension(hdPart.filename());
        String thumbObject = String.format("%s/sku-%s/gallery/%d/thumb%s", imagePrefix, skuSlug, index, thumbExt);
        String hdObject = String.format("%s/sku-%s/gallery/%d/hd%s", imagePrefix, skuSlug, index, hdExt);

        Mono<String> thumbUrl = readBytes(thumbPart)
                .flatMap(bytes -> upload(storage, thumbObject, bytes))
                .map(object -> publicUrl(bucketName, object));
        Mono<String> hdUrl = readBytes(hdPart)
                .flatMap(bytes -> upload(storage, hdObject, bytes))
                .map(object -> publicUrl(bucketName, object));

        return Mono.zip(thumbUrl, hdUrl)
                .map(tuple -> GalleryImageRequest.builder()
                        .thumbnailUrl(tuple.getT1())
                        .hdUrl(tuple.getT2())
                        .alt(request.getAlt())
                        .build());
    }

    private Mono<byte[]> readBytes(FilePart filePart) {
        return DataBufferUtils.join(filePart.content())
                .map(buffer -> {
                    byte[] bytes = new byte[buffer.readableByteCount()];
                    buffer.read(bytes);
                    DataBufferUtils.release(buffer);
                    return bytes;
                });
    }

    private Mono<String> upload(Storage storage, String objectName, byte[] bytes) {
        return Mono.fromCallable(() -> {
                    BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, objectName)).build();
                    storage.create(blobInfo, bytes);
                    return objectName;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    private static String extension(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx < 0) return ".jpg";
        return filename.substring(idx).toLowerCase(Locale.ROOT);
    }

    private static String publicUrl(String bucket, String objectName) {
        return "https://storage.googleapis.com/" + bucket + "/" + objectName;
    }

    private static String slug(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC);
        String cleaned = normalized.replaceAll("[^A-Za-z0-9]+", "-").toLowerCase(Locale.ROOT);
        String trimmed = cleaned.replaceAll("(^-|-$)", "");
        return trimmed.isBlank() ? Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8)) : trimmed;
    }
}

