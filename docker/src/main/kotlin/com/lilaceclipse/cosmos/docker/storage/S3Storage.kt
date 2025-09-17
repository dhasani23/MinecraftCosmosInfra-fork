package com.lilaceclipse.cosmos.docker.storage

import com.lilaceclipse.cosmos.docker.config.EnvironmentConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.commons.io.FileUtils
import software.amazon.awssdk.transfer.s3.S3TransferManager
import software.amazon.awssdk.transfer.s3.model.DirectoryDownload
import software.amazon.awssdk.transfer.s3.model.DirectoryUpload
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import javax.inject.Inject

class S3Storage @Inject constructor(
    private val transferManager: S3TransferManager,
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
        
        val downloadResult: DirectoryDownload = transferManager.downloadDirectory { builder ->
            builder.destination(tmpStorageDir)
                .bucket(serverS3Bucket)
                .listObjectsV2RequestTransformer { req -> req.prefix(serverS3Key) }
        }
        
        downloadResult.completionFuture().join()
        
        log.info { "Download complete" }
        
        // Directory is placed embedded in the intended storage dir, need to move it one up
        FileUtils.moveDirectory(File("$tmpStorageDir/$serverS3Key"), storageDir.toFile())
    }

    fun uploadMinecraft(storageDir: Path, serverS3KeySuffix: String) {
        log.info { "Starting upload" }
        val serverS3Key = serverS3KeyPrefix + serverS3KeySuffix

        // Delete re-creatable files before upload
        foldersToExcludeFromUpload.forEach { FileUtils.deleteDirectory(storageDir.resolve(it).toFile()) }

        val uploadResult: DirectoryUpload = transferManager.uploadDirectory { builder ->
            builder.source(storageDir)
                .bucket(serverS3Bucket)
                .s3Prefix(serverS3Key)
        }
        
        uploadResult.completionFuture().join()
        
        log.info { "Upload complete" }
    }
}