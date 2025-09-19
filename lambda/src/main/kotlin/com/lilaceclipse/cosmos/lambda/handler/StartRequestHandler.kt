package com.lilaceclipse.cosmos.lambda.handler

import com.lilaceclipse.cosmos.common.model.CosmosRequest.StartRequest
import com.lilaceclipse.cosmos.common.model.CosmosResponse
import com.lilaceclipse.cosmos.common.model.CosmosResponse.StartResponse
import com.lilaceclipse.cosmos.common.model.OnlineStatus
import com.lilaceclipse.cosmos.common.model.ServerEntry
import com.lilaceclipse.cosmos.lambda.storage.DynamoStorage
import com.lilaceclipse.cosmos.lambda.util.EnvVarProvider
import com.lilaceclipse.cosmos.lambda.util.SnsUtil
import io.github.oshai.kotlinlogging.KotlinLogging
import software.amazon.awssdk.services.ecs.EcsClient
import software.amazon.awssdk.services.ecs.model.*
import javax.inject.Inject


class StartRequestHandler @Inject constructor(
    private val envVarProvider: EnvVarProvider,
    private val dynamoStorage: DynamoStorage,
    private val ecsClient: EcsClient,
    private val snsUtil: SnsUtil
) {
    private val log = KotlinLogging.logger {}

    fun handleRequest(request: StartRequest): CosmosResponse {
        val serverEntry = dynamoStorage.getServerEntryFromDb(request.serverUUID)

        if (serverEntry.onlineStatus != OnlineStatus.OFFLINE) {
            log.info { "Received request to start service, but it was already running" }
            return StartResponse(
                message = "Cosmos is already started!"
            )
        }

        log.info { "Received request to start service, will now attempt to start" }
        
        val awsVpcConfiguration = AwsVpcConfiguration.builder()
            .assignPublicIp(AssignPublicIp.ENABLED)
            .securityGroups(envVarProvider.securityGroupId)
            .subnets(envVarProvider.subnetId)
            .build()
            
        val networkConfiguration = NetworkConfiguration.builder()
            .awsvpcConfiguration(awsVpcConfiguration)
            .build()
            
        val containerOverride = ContainerOverride.builder()
            .name("cosmos-container")
            .command("--environment", envVarProvider.stage, "--target-server", request.serverUUID)
            .build()
            
        val taskOverride = TaskOverride.builder()
            .containerOverrides(containerOverride)
            .build()
        
        val runTaskRequest = RunTaskRequest.builder()
            .launchType(LaunchType.FARGATE)
            .taskDefinition(envVarProvider.taskDefinitionArn)
            .cluster(envVarProvider.clusterArn)
            .networkConfiguration(networkConfiguration)
            .overrides(taskOverride)
            .build()

        ecsClient.runTask(runTaskRequest)
        snsUtil.sendSmsAlert("Cosmos has started! Check cosmos.lilaceclipse.com for the server IP")
        dynamoStorage.updateServerEntryNonNulls(
            ServerEntry(
            serverId = request.serverUUID,
            onlineStatus = OnlineStatus.CONTAINER_LAUNCHED
        )
        )

        return StartResponse(
            message = "Cosmos will now start, refresh the page shortly to get the IP address!"
        )
    }
}