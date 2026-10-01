package cn.hollis.llm.mentor.know.engine.document.service;

import io.minio.*;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import org.springframework.web.util.UriUtils;

@Service
public class FileStorageService {

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucketName}")
    private String bucketName;

    @Value("${minio.endpoint}")
    private String endpoint;

    // 确保 bucket 存在
    private void createBucketIfNotExists(boolean publicRead) throws Exception {
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());

            // 设置 bucket 策略为公共读
            if (publicRead) {
                String policy = "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:GetObject\"],\"Resource\":[\"arn:aws:s3:::" + bucketName + "/*\"]}]}";
                minioClient.setBucketPolicy(
                        SetBucketPolicyArgs.builder()
                                .bucket(bucketName)
                                .config(policy)
                                .build()
                );
            }
        }
    }

    // 上传文件
    public String uploadFile(MultipartFile file, String objectName) throws Exception {
        createBucketIfNotExists(true);
        objectName = uniqueObjectName(objectName);
        try (InputStream stream = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(stream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        }
        return objectUrl(objectName);

    }

    /**
     * 上传文件
     */
    public String uploadFile(String objectName, byte[] content, String contentType) throws Exception {
        createBucketIfNotExists(true);
        objectName = uniqueObjectName(objectName);
        try (InputStream stream = new ByteArrayInputStream(content)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(stream, content.length, -1)
                            .contentType(contentType)
                            .build()
            );

            return objectUrl(objectName);
        }
    }

    private String uniqueObjectName(String name) {
        // 同名文件与转换产物也保留独立对象，避免新版本覆盖历史版本原文。
        return UUID.randomUUID() + "/" + name.replace('\\', '/').replaceAll("^/+", "");
    }

    private String objectUrl(String objectName) {
        return endpoint.replaceAll("/+$", "") + "/" + bucketName + "/" + UriUtils.encodePath(objectName, StandardCharsets.UTF_8);
    }

    // 下载文件（返回 InputStream）
    public InputStream downloadFile(String objectName) throws Exception {
        GetObjectResponse response = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build());
        return response;
    }

    public boolean isStoredFile(String url) {
        return url != null && url.startsWith(endpoint.replaceAll("/+$", "") + "/" + bucketName + "/");
    }

    public byte[] readStoredFile(String url) throws Exception {
        try (InputStream input = downloadFile(storedObjectName(url))) {
            return input.readAllBytes();
        }
    }

    public String storedObjectName(String url) {
        String prefix = endpoint.replaceAll("/+$", "") + "/" + bucketName + "/";
        if (!isStoredFile(url)) throw new IllegalArgumentException("文件不属于当前文件存储");
        return UriUtils.decode(url.substring(prefix.length()), StandardCharsets.UTF_8);
    }

    // 删除文件
    public void deleteFile(String objectName) throws Exception {
        minioClient.removeObject(RemoveObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .build());
    }

    /**
     * 删除当前 MinIO 存储中的文件；非当前存储生成的地址不处理。
     *
     * @return 是否删除了当前 MinIO 中的对象
     */
    public boolean deleteStoredFile(String url) throws Exception {
        if (!isStoredFile(url)) {
            return false;
        }
        deleteFile(storedObjectName(url));
        return true;
    }

    /**
     * 返回可供浏览器下载的地址。
     * 当前 MinIO 存储的对象使用临时签名，外部地址保持原样。
     */
    public String getDownloadUrl(String url) throws Exception {
        return isStoredFile(url) ? getPresignedUrl(storedObjectName(url)) : url;
    }

    // 生成临时下载链接（带签名，有效期 7 天）
    public String getPresignedUrl(String objectName) throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucketName)
                        .object(objectName)
                        .expiry(7, TimeUnit.DAYS)
                        .build());
    }
}
