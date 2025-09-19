package com.lilaceclipse.cosmos.lambda.util

import io.github.oshai.kotlinlogging.KotlinLogging
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.PublishRequest
import javax.inject.Inject

class SnsUtil @Inject constructor(
    private val snsClient: SnsClient,
    private val envVarProvider: EnvVarProvider
) {
    private val log = KotlinLogging.logger {}

    fun sendSmsAlert(message: String) {
        val request = PublishRequest.builder()
            .topicArn(envVarProvider.smsAlertTopicArn)
            .message(message)
            .build()

        try {
            val result = snsClient.publish(request)
            log.info { "Message ${result.messageId()} published to ${envVarProvider.smsAlertTopicArn}" }
        } catch (e: Exception) {
            log.info { "Exception caught while publishing message to ${envVarProvider.smsAlertTopicArn}" }
            e.printStackTrace()
        }
    }
}