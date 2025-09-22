package com.lilaceclipse.cosmos.docker.dagger

import software.amazon.awssdk.services.ec2.Ec2Client
import com.amazonaws.services.ecs.AmazonECS
import com.amazonaws.services.ecs.AmazonECSClientBuilder
import com.amazonaws.services.s3.transfer.TransferManager
import com.amazonaws.services.s3.transfer.TransferManagerBuilder
import dagger.Module
import dagger.Provides
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient

@Module
class CosmosModule {

    @Provides
    fun provideTransferManager() : TransferManager {
        return TransferManagerBuilder.defaultTransferManager()
    }

    @Provides
    fun provideDynamoDbEnhancedClient(): DynamoDbEnhancedClient {
        return DynamoDbEnhancedClient.create()
    }

    @Provides
    fun provideEc2Client(): Ec2Client {
        return Ec2Client.create()
    }

    @Provides
    fun provideAmazonECS(): AmazonECS {
        return AmazonECSClientBuilder.defaultClient()
    }
}
