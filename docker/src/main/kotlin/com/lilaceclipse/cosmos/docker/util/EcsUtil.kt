package com.lilaceclipse.cosmos.docker.util


import io.github.oshai.kotlinlogging.KotlinLogging
import software.amazon.awssdk.services.ec2.Ec2Client
import software.amazon.awssdk.services.ec2.model.DescribeNetworkInterfacesRequest
import software.amazon.awssdk.services.ec2.model.Filter
import software.amazon.awssdk.services.ecs.EcsClient
import software.amazon.awssdk.services.ecs.model.DescribeTasksRequest
import java.net.URL
import javax.inject.Inject

class EcsUtil @Inject constructor(
    private val ec2Client: Ec2Client,
    private val ecsClient: EcsClient
) {
    private val log = KotlinLogging.logger {}

    fun getContainerPublicIp(): String {
        // Retrieve container metadata
        val metadataUrl = URL(System.getenv("ECS_CONTAINER_METADATA_URI_V4"))
        val containerMetadata = metadataUrl.readText()


        val taskArnRegex = Regex("\\\"com\\\\.amazonaws\\\\.ecs\\\\.task-arn\\\"\\\\s*:\\\\s*\\\"([^\\\"]+)\\\"")
        val taskArn = taskArnRegex.find(containerMetadata)?.groupValues?.getOrNull(1)

        val clusterName = taskArn?.split("/")?.getOrNull(1)
        val taskId = taskArn?.split("/")?.lastOrNull()

        log.info { "Task ARN: $taskArn" }
        log.info { "Cluster Name: $clusterName" }
        log.info { "Task ID: $taskId" }

        if (taskArn != null && clusterName != null) {
            // Describe the task to get the network interface details using AWS SDK v2
            val describeTasksRequest = DescribeTasksRequest.builder()
                .cluster(clusterName)
                .tasks(taskArn)
                .build()
                
            val describeTasksResult = ecsClient.describeTasks(describeTasksRequest)

            val task = describeTasksResult.tasks().firstOrNull()
            val attachmentDetails = task?.attachments()?.flatMap { it.details() }
            val networkInterfaceId = attachmentDetails?.find { it.name() == "networkInterfaceId" }?.value()

            if (networkInterfaceId != null) {
                // Describe the network interface to get the public IP address using AWS SDK v2
                val filter = Filter.builder()
                    .name("network-interface-id")
                    .values(networkInterfaceId)
                    .build()
                
                val describeNetworkInterfacesRequest = DescribeNetworkInterfacesRequest.builder()
                    .filters(filter)
                    .build()
                    
                val describeNetworkInterfacesResult = ec2Client.describeNetworkInterfaces(describeNetworkInterfacesRequest)

                val publicIp = describeNetworkInterfacesResult.networkInterfaces().firstOrNull()
                    ?.association()
                    ?.publicIp()

                return publicIp ?: throw RuntimeException("No public IP found")
            }
        }
        throw RuntimeException("Failed to get IP!!!")
    }
}