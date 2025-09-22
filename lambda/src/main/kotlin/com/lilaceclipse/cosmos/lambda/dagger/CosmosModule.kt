package com.lilaceclipse.cosmos.lambda.dagger

import software.amazon.awssdk.services.ec2.Ec2Client
import com.amazonaws.services.ecs.AmazonECS
import com.amazonaws.services.ecs.AmazonECSClientBuilder
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.AmazonSNSClientBuilder
import com.lilaceclipse.cosmos.lambda.util.EnvVarProvider
import dagger.Module
import dagger.Provides
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient

@Module
class CosmosModule {
    @Provides
    fun provideEnvVarProvider(): EnvVarProvider {
        return EnvVarProvider()
    }

    @Provides
    fun provideAmazonSNS(): AmazonSNS {
        return AmazonSNSClientBuilder.defaultClient()
    }

    @Provides
    fun provideEc2Client(): Ec2Client {
        return Ec2Client.create()
    }

    @Provides
    fun provideAmazonECS(): AmazonECS {
        return AmazonECSClientBuilder.defaultClient()
    }

    @Provides
    fun provideDynamoDbEnhancedClient(): DynamoDbEnhancedClient {
        return DynamoDbEnhancedClient.create()
    }
}
