package cloud.mindbox.mobile_sdk.managers

import android.content.Context
import cloud.mindbox.mobile_sdk.Configuration
import cloud.mindbox.mobile_sdk.InitializeMindboxException
import cloud.mindbox.mobile_sdk.Logger
import cloud.mindbox.mobile_sdk.models.*
import cloud.mindbox.mobile_sdk.network.ServiceGenerator
import cloud.mindbox.mobile_sdk.toUrlQueryString
import com.android.volley.NetworkResponse
import com.android.volley.Request
import org.json.JSONObject
import java.util.*

internal object GatewayManager {

    private fun buildEventUrl(
        configuration: Configuration,
        event: Event
    ): String {

        val urlQueries: HashMap<String, String> = hashMapOf(
            UrlQuery.ENDPOINT_ID.value to configuration.endpointId,
            UrlQuery.DEVICE_UUID.value to configuration.deviceUuid,
            UrlQuery.TRANSACTION_ID.value to event.transactionId,
            UrlQuery.DATE_TIME_OFFSET.value to getTimeOffset(event.enqueueTimestamp)
        )

        when (event.eventType) {
            EventType.APP_INFO_UPDATED,
            EventType.APP_INSTALLED -> {
                urlQueries[UrlQuery.OPERATION.value] = event.eventType.operation
            }
            EventType.PUSH_DELIVERED -> {
                urlQueries[UrlQuery.UNIQ_KEY.value] =
                    event.additionalFields?.get(EventParameters.UNIQ_KEY.fieldName) ?: ""
            }
        }

        urlQueries.toUrlQueryString()

        return "https://${configuration.domain}${event.eventType.endpoint}${urlQueries.toUrlQueryString()}"
    }

    fun sendEvent(context: Context, event: Event, isSuccess: (Boolean) -> Unit) {
        try {
            val configuration = DbManager.getConfigurations()

            if (configuration == null) {
                Logger.e(
                    this,
                    "Configuration was not initialized",
                    InitializeMindboxException("Configuration was not initialized")
                )
                isSuccess.invoke(false)
                return
            }

            val requestType: Int = getRequestType(event.eventType)
            val url: String = buildEventUrl(configuration, event)
            val jsonRequest: JSONObject? = convertBodyToJson(event.body)

            val request = MindboxRequest(requestType, url, configuration, jsonRequest,
                {
                    Logger.d(this, "Event from background successful sended")
                    isSuccess.invoke(true)
                }, {
                    try {
                        when (val result = parseResponse(it.networkResponse)) {
                            is MindboxResponse.SuccessResponse<*>,
                            is MindboxResponse.BadRequest -> {
                                Logger.d(this, "Event from background successful sended")
                                isSuccess.invoke(true)
                            }
                            is MindboxResponse.Error -> {
                                Logger.d(
                                    this,
                                    "Sending event from background was failure with code ${result.status}"
                                )
                                isSuccess.invoke(false)
                            }
                        }
                    } catch (e: Exception) {
                        Logger.e(this, "Parsing server response was failure", e)
                        isSuccess.invoke(false)
                    }
                }
            )

            ServiceGenerator.getInstance(context).addToRequestQueue(request)
        } catch (e: Exception) {
            Logger.e(this, "Sending event was failure with exception", e)
            isSuccess.invoke(false)
        }
    }

    private fun getRequestType(eventType: EventType): Int {
        return when (eventType) {
            EventType.APP_INSTALLED,
            EventType.APP_INFO_UPDATED -> Request.Method.POST
            EventType.PUSH_DELIVERED -> Request.Method.GET
        }
    }

    private fun getTimeOffset(timeMls: Long): String {
        return (Date().time - timeMls).toString()
    }

    private fun convertBodyToJson(body: String?): JSONObject? {
        return if (body == null) {
            null
        } else {
            JSONObject(body)
        }
    }

    private fun parseResponse(response: NetworkResponse?): MindboxResponse {
        return when {
            response == null -> MindboxResponse.Error(-1, byteArrayOf())
            response.statusCode < 300 -> {
                MindboxResponse.SuccessResponse(response.data)
            }
            response.statusCode in 400..499 -> {
                // separate condition for removing from the queue
                MindboxResponse.BadRequest(response.statusCode)
            }
            else -> {
                MindboxResponse.Error(response.statusCode, response.data)
            }
        }
    }
}