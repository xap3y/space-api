package me.xap3y.space.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.dto.S3ObjectInfo;
import me.xap3y.space.dto.S3ObjectMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.core.sync.RequestBody;

import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    public String generatePresignedPutUrl(String key, String contentType) {
        return generatePresignedPutUrl(key, contentType, Duration.ofMinutes(5));
    }

    public String generatePresignedPutUrl(String key, String contentType, Duration duration) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(duration)
                    .putObjectRequest(putObjectRequest)
                    .build();

            PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
            String presignedUrl = presignedRequest.url().toString();

            log.info("Generated presigned PUT URL for key: {}", key);
            return presignedUrl;

        } catch (S3Exception e) {
            log.error("Error generating presigned PUT URL for key {}: {}", key, e.getMessage());
            throw new RuntimeException("Failed to generate presigned URL", e);
        }
    }

    public InputStream getFileAsStream(String key) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key("files/" + key)
                    .build();

            ResponseInputStream<GetObjectResponse> responseInputStream = s3Client.getObject(getObjectRequest);

            log.info("Stream opened for key: {}", key);
            return responseInputStream;

        } catch (S3Exception e) {
            log.error("Error getting file stream from S3 with key {}: {}", key, e.getMessage());
            return null;
        }
    }

    public long getFileSize(String key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            HeadObjectResponse response = s3Client.headObject(headObjectRequest);
            return response.contentLength();

        } catch (S3Exception e) {
            log.error("Error getting file size from S3 with key {}: {}", key, e.getMessage());
            return 0;
        }
    }

    /**
     * Generate presigned URL for GET (download)
     */
    public String generatePresignedGetUrl(String key, Duration duration) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(duration)
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            String presignedUrl = presignedRequest.url().toString();

            log.info("Generated presigned GET URL for key: {}", key);
            return presignedUrl;

        } catch (S3Exception e) {
            log.error("Error generating presigned GET URL for key {}: {}", key, e.getMessage());
            throw new RuntimeException("Failed to generate presigned URL", e);
        }
    }

    /**
     * Upload file to S3
     */
    public void uploadFile(String key, byte[] fileContent, String contentType) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileContent));

            log.info("File uploaded successfully to S3: {}", key);

        } catch (S3Exception e) {
            log.error("Error uploading file to S3 with key {}: {}", key, e.getMessage());
            throw new RuntimeException("Failed to upload file", e);
        }
    }


    /**
     * Delete file from S3
     */
    public void deleteFile(String key) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key("files/" + key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);

            log.info("File deleted successfully from S3: {}", key);

        } catch (S3Exception e) {
            log.error("Error deleting file from S3 with key {}: {}", key, e.getMessage());
            throw new RuntimeException("Failed to delete file", e);
        }
    }

    /**
     * Delete multiple files from S3
     */
    public void deleteFiles(List<String> keys) {
        try {
            List<ObjectIdentifier> objectIdentifiers = keys.stream()
                    .map(key -> ObjectIdentifier.builder().key("files/" + key).build())
                    .toList();

            DeleteObjectsRequest deleteObjectsRequest = DeleteObjectsRequest.builder()
                    .bucket(bucketName)
                    .delete(Delete.builder().objects(objectIdentifiers).build())
                    .build();

            DeleteObjectsResponse response = s3Client.deleteObjects(deleteObjectsRequest);

            log.info("Successfully deleted {} files from S3", response.deleted().size());

        } catch (S3Exception e) {
            log.error("Error deleting multiple files from S3: {}", e.getMessage());
            throw new RuntimeException("Failed to delete files", e);
        }
    }

    /**
     * List all objects in bucket
     */
    public List<S3ObjectInfo> listObjects() {
        try {
            ListObjectsV2Request listObjectsRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(listObjectsRequest);

            List<S3ObjectInfo> objectList = new ArrayList<>();
            if (response.contents() != null) {
                for (S3Object s3Object : response.contents()) {
                    objectList.add(S3ObjectInfo.builder()
                            .key(s3Object.key())
                            .size(s3Object.size())
                            .lastModified(s3Object.lastModified().toString())
                            .build());
                }
            }

            log.info("Listed {} objects from S3 bucket: {}", objectList.size(), bucketName);
            return objectList;

        } catch (S3Exception e) {
            log.error("Error listing objects from S3: {}", e.getMessage());
            throw new RuntimeException("Failed to list objects", e);
        }
    }

    /**
     * List objects with prefix (path-like filtering)
     */
    public List<S3ObjectInfo> listObjectsByPrefix(String prefix) {
        try {
            ListObjectsV2Request listObjectsRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(listObjectsRequest);

            List<S3ObjectInfo> objectList = new ArrayList<>();
            if (response.contents() != null) {
                for (S3Object s3Object : response.contents()) {
                    objectList.add(S3ObjectInfo.builder()
                            .key(s3Object.key())
                            .size(s3Object.size())
                            .lastModified(s3Object.lastModified().toString())
                            .build());
                }
            }

            log.info("Listed {} objects with prefix '{}' from S3 bucket: {}", objectList.size(), prefix, bucketName);
            return objectList;

        } catch (S3Exception e) {
            log.error("Error listing objects with prefix '{}' from S3: {}", prefix, e.getMessage());
            throw new RuntimeException("Failed to list objects", e);
        }
    }

    /**
     * Check if object exists
     */
    public boolean objectExists(String key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.headObject(headObjectRequest);
            return true;

        } catch (NoSuchKeyException e) {
            log.info("Object does not exist in S3: {}", key);
            return false;
        } catch (S3Exception e) {
            log.error("Error checking if object exists in S3 with key {}: {}", key, e.getMessage());
            throw new RuntimeException("Failed to check object existence", e);
        }
    }

    /**
     * Get object metadata
     */
    public S3ObjectMetadata getObjectMetadata(String key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            HeadObjectResponse response = s3Client.headObject(headObjectRequest);

            return S3ObjectMetadata.builder()
                    .key(key)
                    .size(response.contentLength())
                    .contentType(response.contentType())
                    .lastModified(response.lastModified().toString())
                    .eTag(response.eTag())
                    .build();

        } catch (S3Exception e) {
            log.error("Error getting object metadata for key {}: {}", key, e.getMessage());
            throw new RuntimeException("Failed to get object metadata", e);
        }
    }

    /**
     * Copy object within bucket
     */
    public void copyObject(String sourceKey, String destinationKey) {
        try {
            CopyObjectRequest copyObjectRequest = CopyObjectRequest.builder()
                    .sourceBucket(bucketName)
                    .sourceKey(sourceKey)
                    .destinationBucket(bucketName)
                    .destinationKey(destinationKey)
                    .build();

            s3Client.copyObject(copyObjectRequest);

            log.info("Object copied from {} to {} in S3", sourceKey, destinationKey);

        } catch (S3Exception e) {
            log.error("Error copying object from {} to {} in S3: {}", sourceKey, destinationKey, e.getMessage());
            throw new RuntimeException("Failed to copy object", e);
        }
    }

    /**
     * Get media stream from S3 (no path prefix added)
     */
    public InputStream getMediaStream(String key) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            ResponseInputStream<GetObjectResponse> responseInputStream = s3Client.getObject(getObjectRequest);
            log.info("Media stream opened for key: {}", key);
            return responseInputStream;
        } catch (S3Exception e) {
            log.error("Error getting media stream from S3 with key {}: {}", key, e.getMessage());
            return null;
        }
    }

    /**
     * Delete media from S3 (no path prefix added)
     */
    public void deleteMedia(String key) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("Media deleted successfully from S3: {}", key);
        } catch (S3Exception e) {
            log.error("Error deleting media from S3 with key {}: {}", key, e.getMessage());
            throw new RuntimeException("Failed to delete media", e);
        }
    }
}
