package com.lilaceclipse.cosmos.lambda.dagger

import com.lilaceclipse.cosmos.lambda.util.EnvVarProvider
import dagger.Module
import dagger.Provides
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.services.ec2.Ec2Client
import software.amazon.awssdk.services.ecs.EcsClient
import software.amazon.awssdk.services.sns.SnsClient

@Module
class CosmosModule {
    @Provides
    fun provideEnvVarProvider(): EnvVarProvider {
        return EnvVarProvider()
    }

    @Provides
    fun provideSnsClient(): SnsClient {
        return SnsClient.builder().build()
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
