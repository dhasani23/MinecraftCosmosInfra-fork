package com.lilaceclipse.cosmos.docker.dagger

import dagger.Module
import dagger.Provides
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.services.ec2.Ec2Client
import software.amazon.awssdk.services.ecs.EcsClient
import software.amazon.awssdk.services.s3.S3Client

@Module
class CosmosModule {

    @Provides
    fun provideS3Client(): S3Client {
        return S3Client.builder().build()
    }

    @Provides
    fun provideEc2Client(): Ec2Client {
        return Ec2Client.builder().build()
    }
    
    @Provides
    fun provideEcsClient(): EcsClient {
        return EcsClient.builder().build()
    }

    @Provides
    fun provideDynamoDbEnhancedClient(): DynamoDbEnhancedClient {
        return DynamoDbEnhancedClient.create()
    }
}
