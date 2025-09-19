package com.lilaceclipse.cosmos.docker.storage

import com.lilaceclipse.cosmos.docker.config.EnvironmentConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.commons.io.FileUtils
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import javax.inject.Inject

class S3Storage @Inject constructor(
    private val s3Client: S3Client,
    environmentConfig: EnvironmentConfig
) {

    private val log = KotlinLogging.logger {}

    private val serverS3Bucket = environmentConfig.s3Bucket
    private val serverS3KeyPrefix = "servers/"
    private val tmpStorageDir = Paths.get("tmp/cosmos/gameserver/download/")

    private val foldersToExcludeFromUpload = listOf(
        Paths.get("logs"), // Logs are in cloudwatch
        Paths.get("mods"), // Unchanged between runs
        Paths.get("libraries"), // Unchanged between runs
        Paths.get(".fabric"), // Unchanged between runs
        Paths.get("pfm"), // Mainly a cache directory, has many objects which leads to lengthy downloads/uploads
        Paths.get("versions") // Unchanged between runs
    )

    fun downloadMinecraft(storageDir: Path, serverS3KeySuffix: String) {
        log.info { "Starting download" }
        val serverS3Key = serverS3KeyPrefix + serverS3KeySuffix
        
        // Create a synchronous downloader to download a directory recursively
        val downloader = S3DirectoryDownloader(s3Client, serverS3Bucket, serverS3Key, tmpStorageDir)
        downloader.downloadDirectory()
        
        log.info { "Download complete" }
        
        // Directory is placed embedded in the intended storage dir, need to move it one up
        FileUtils.moveDirectory(File("$tmpStorageDir/$serverS3Key"), storageDir.toFile())
    }

    fun uploadMinecraft(storageDir: Path, serverS3KeySuffix: String) {
        log.info { "Starting upload" }
        val serverS3Key = serverS3KeyPrefix + serverS3KeySuffix

        // Delete re-creatable files before upload
        foldersToExcludeFromUpload.forEach { FileUtils.deleteDirectory(storageDir.resolve(it).toFile()) }

        // Create a synchronous uploader to upload a directory recursively
        val uploader = S3DirectoryUploader(s3Client, serverS3Bucket, serverS3Key, storageDir)
        uploader.uploadDirectory()
        
        log.info { "Upload complete" }
    }
    
    // Helper class to handle recursive S3 directory downloads
    private class S3DirectoryDownloader(
        private val s3Client: S3Client,
        private val bucket: String,
        private val keyPrefix: String,
        private val destinationDir: Path
    ) {
        private val log = KotlinLogging.logger {}
        
        fun downloadDirectory() {
            // First list all objects in the directory
            log.info { "Listing objects in S3 bucket $bucket with prefix $keyPrefix" }
            
            val listObjectsRequest = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(keyPrefix)
                .build()
                
            val listObjectsResponse = s3Client.listObjectsV2(listObjectsRequest)
            
            // Download each object
            listObjectsResponse.contents().forEach { s3Object ->
                val key = s3Object.key()
                
                // Compute the local file path
                val relativePath = key.removePrefix(keyPrefix)
                val localFile = destinationDir.resolve(keyPrefix).resolve(relativePath).toFile()
                
                // Create parent directories if needed
                localFile.parentFile.mkdirs()
                
                // Download the file
                if (!s3Object.key().endsWith("/")) {  // Skip directory markers
                    val getObjectRequest = GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build()
                        
                    s3Client.getObject(getObjectRequest).use { response ->
                        response.transferTo(localFile.outputStream())
                    }
                }
            }
        }
    }
    
    // Helper class to handle recursive S3 directory uploads
    private class S3DirectoryUploader(
        private val s3Client: S3Client,
        private val bucket: String,
        private val keyPrefix: String,
        private val sourceDir: Path
    ) {
        private val log = KotlinLogging.logger {}
        
        fun uploadDirectory() {
            // Walk through the directory and upload all files
            val rootDir = sourceDir.toFile()
            rootDir.walkTopDown()
                .filter { it.isFile }
                .forEach { file ->
                    val relativePath = rootDir.toPath().relativize(file.toPath()).toString()
                    val key = if (keyPrefix.endsWith("/")) {
                        keyPrefix + relativePath
                    } else {
                        "$keyPrefix/$relativePath"
                    }
                    
                    // Upload the file
                    val putObjectRequest = PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build()
                        
                    s3Client.putObject(putObjectRequest, RequestBody.fromFile(file))
                    
                    log.info { "Uploaded $relativePath to S3 key $key" }
                }
        }
    }
}