package com.lilaceclipse.cosmos.docker.dagger

import com.amazonaws.services.ec2.AmazonEC2
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder
import com.amazonaws.services.ecs.AmazonECS
import com.amazonaws.services.ecs.AmazonECSClientBuilder
import dagger.Module
import dagger.Provides
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.transfer.s3.S3TransferManager

@Module
class CosmosModule {

    @Provides
    fun provideS3AsyncClient(): S3AsyncClient {
        return S3AsyncClient.create()
    }

    @Provides
    fun provideTransferManager(s3AsyncClient: S3AsyncClient): S3TransferManager {
        return S3TransferManager.builder()
            .s3Client(s3AsyncClient)
            .build()
    }

    @Provides
    fun provideDynamoDbEnhancedClient(): DynamoDbEnhancedClient {
        return DynamoDbEnhancedClient.create()
    }

    @Provides
    fun provideAmazonEC2():  AmazonEC2 {
        return AmazonEC2ClientBuilder.defaultClient()
    }

    @Provides
    fun provideAmazonECS(): AmazonECS {
        return AmazonECSClientBuilder.defaultClient()
    }
}
