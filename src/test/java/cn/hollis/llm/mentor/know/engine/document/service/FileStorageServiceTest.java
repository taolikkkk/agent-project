package cn.hollis.llm.mentor.know.engine.document.service;

import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FileStorageServiceTest {
    @Test
    void sameNamedVersionsKeepSeparateOriginalsAndConvertedFiles() throws Exception {
        MinioClient minio = mock(MinioClient.class);
        when(minio.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        FileStorageService storage = new FileStorageService();
        ReflectionTestUtils.setField(storage, "minioClient", minio);
        ReflectionTestUtils.setField(storage, "bucketName", "knowledge");
        ReflectionTestUtils.setField(storage, "endpoint", "http://localhost:9000");

        String first = storage.uploadFile(new MockMultipartFile("file", "guide.md", "text/markdown", "v1".getBytes()), "guide.md");
        String second = storage.uploadFile(new MockMultipartFile("file", "guide.md", "text/markdown", "v2".getBytes()), "guide.md");
        String convertedFirst = storage.uploadFile("converted/guide.md", "v1".getBytes(), "text/markdown");
        String convertedSecond = storage.uploadFile("converted/guide.md", "v2".getBytes(), "text/markdown");

        assertNotEquals(first, second);
        assertNotEquals(convertedFirst, convertedSecond);
        ArgumentCaptor<PutObjectArgs> objects = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minio, times(4)).putObject(objects.capture());
        assertEquals(4, objects.getAllValues().stream().map(PutObjectArgs::object).distinct().count());
        for (String url : new String[] { first, second, convertedFirst, convertedSecond }) {
            assertTrue(storage.isStoredFile(url));
            assertTrue(url.endsWith("guide.md"));
        }
        String special = storage.uploadFile("converted/中文?#版本.md", "content".getBytes(), "text/markdown");
        assertFalse(special.contains("?"));
        assertFalse(special.contains("#"));
        assertTrue(storage.storedObjectName(special).endsWith("converted/中文?#版本.md"));
    }

    @Test
    void deletionOnlyRemovesObjectsOwnedByCurrentMinioStorage() throws Exception {
        MinioClient minio = mock(MinioClient.class);
        FileStorageService storage = new FileStorageService();
        ReflectionTestUtils.setField(storage, "minioClient", minio);
        ReflectionTestUtils.setField(storage, "bucketName", "knowledge");
        ReflectionTestUtils.setField(storage, "endpoint", "http://localhost:9000");

        assertTrue(storage.deleteStoredFile("http://localhost:9000/knowledge/original/guide.md"));
        assertFalse(storage.deleteStoredFile("https://files.example.com/guide.md"));

        ArgumentCaptor<RemoveObjectArgs> deleted = ArgumentCaptor.forClass(RemoveObjectArgs.class);
        verify(minio).removeObject(deleted.capture());
        assertEquals("knowledge", deleted.getValue().bucket());
        assertEquals("original/guide.md", deleted.getValue().object());
    }
}
