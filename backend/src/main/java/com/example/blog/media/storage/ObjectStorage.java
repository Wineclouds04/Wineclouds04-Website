package com.example.blog.media.storage;

public interface ObjectStorage extends AutoCloseable {

    boolean configured();

    void put(String objectKey, byte[] content, String contentType);

    void delete(String objectKey);

    String publicUrl(String objectKey);

    @Override
    void close();
}
