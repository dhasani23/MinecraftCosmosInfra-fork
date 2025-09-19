package com.lilaceclipse.cosmos.lambda.handler

import com.lilaceclipse.cosmos.common.model.CosmosRequest.StatusRequest
import com.lilaceclipse.cosmos.common.model.CosmosResponse
import com.lilaceclipse.cosmos.common.model.CosmosResponse.StatusResponse
import com.lilaceclipse.cosmos.lambda.util.EnvVarProvider
import io.github.oshai.kotlinlogging.KotlinLogging
import software.amazon.awssdk.services.ec2.Ec2Client
import software.amazon.awssdk.services.ec2.model.DescribeNetworkInterfacesRequest
import software.amazon.awssdk.services.ecs.EcsClient
import software.amazon.awssdk.services.ecs.model.DescribeTasksRequest
import software.amazon.awssdk.services.ecs.model.ListTasksRequest
import javax.inject.Inject

class StatusRequestHandler @Inject constructor(
    private val envVarProvider: EnvVarProvider,
    private val ecsClient: EcsClient,
    private val ec2Client: Ec2Client
) {
    private val log = KotlinLogging.logger {}

    fun handleRequest(request: StatusRequest): CosmosResponse {
        // This code assumes only one task
        var status: String // RUNNING, STARTING, STOPPED, ERROR
        var ip = ""

        log.info { "Fetching active tasks" }
        val listTaskResult = listActiveTasks()

        log.info { "Determining status" }
        when (listTaskResult.taskArns().size) {
            0 -> {
                status = "STOPPED"
            }
            1 -> {
                status = "RUNNING"
                val describeTaskRequest = DescribeTasksRequest.builder()
                    .tasks(listTaskResult.taskArns().get(0))
                    .cluster(envVarProvider.clusterArn)
                    .build()
                    
                val describeTaskResult = ecsClient.describeTasks(describeTaskRequest)

                val elasticNetworkInterface = describeTaskResult
                    .tasks().get(0)
                    .attachments().get(0)
                    .details().first { it.name() == "networkInterfaceId" }
                    .value()

                val describeEniRequest = DescribeNetworkInterfacesRequest.builder()
                    .networkInterfaceIds(elasticNetworkInterface)
                    .build()
                    
                val describeEniResult = ec2Client.describeNetworkInterfaces(describeEniRequest)

                ip = describeEniResult.networkInterfaces().get(0).association().publicIp()
            }
            else -> status = "ERROR"
        }

        return StatusResponse(
            status = status,
            ip = ip
        )
    }

    private fun listActiveTasks() = ecsClient.listTasks(
        ListTasksRequest.builder()
            .cluster(envVarProvider.clusterArn)
            .build()
    )
}