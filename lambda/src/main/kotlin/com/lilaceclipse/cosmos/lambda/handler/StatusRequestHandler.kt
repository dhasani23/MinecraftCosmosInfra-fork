package com.lilaceclipse.cosmos.lambda.handler

import software.amazon.awssdk.services.ec2.Ec2Client
import software.amazon.awssdk.services.ec2.model.DescribeNetworkInterfacesRequest
import com.amazonaws.services.ecs.AmazonECS
import com.amazonaws.services.ecs.model.DescribeTasksRequest
import com.amazonaws.services.ecs.model.ListTasksRequest
import com.amazonaws.services.ecs.model.ListTasksResult
import com.lilaceclipse.cosmos.common.model.CosmosRequest.StatusRequest
import com.lilaceclipse.cosmos.common.model.CosmosResponse
import com.lilaceclipse.cosmos.common.model.CosmosResponse.StatusResponse
import com.lilaceclipse.cosmos.lambda.util.EnvVarProvider
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject

class StatusRequestHandler @Inject constructor(
    private val envVarProvider: EnvVarProvider,
    private val ecsClient: AmazonECS,
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
        when (listTaskResult.taskArns.size) {
            0 -> {
                status = "STOPPED"
            }
            1 -> {
                status = "RUNNING"
                val describeTaskResult = ecsClient.describeTasks(
                    DescribeTasksRequest()
                    .withTasks(listTaskResult.taskArns[0])
                    .withCluster(envVarProvider.clusterArn))

                val elasticNetworkInterface = describeTaskResult
                    .tasks[0]
                    .attachments[0]
                    .details.first { it.name == "networkInterfaceId" }
                    .value

                val describeEniResult = ec2Client.describeNetworkInterfaces(
                    DescribeNetworkInterfacesRequest.builder()
                    .networkInterfaceIds(elasticNetworkInterface)
                    .build())

                ip = describeEniResult.networkInterfaces()[0].association().publicIp()
            }
            else -> status = "ERROR"
        }

        return StatusResponse(
            status = status,
            ip = ip
        )
    }

    private fun listActiveTasks(): ListTasksResult {
        return ecsClient.listTasks(
            ListTasksRequest()
                .withCluster(envVarProvider.clusterArn))
    }
}