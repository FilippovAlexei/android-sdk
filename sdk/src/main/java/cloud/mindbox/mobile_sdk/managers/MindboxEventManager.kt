package cloud.mindbox.mobile_sdk.managers

import android.content.Context
import cloud.mindbox.mobile_sdk.logOnException
import cloud.mindbox.mobile_sdk.models.*
import cloud.mindbox.mobile_sdk.services.BackgroundWorkManager
import com.google.gson.Gson

internal object MindboxEventManager {

    private val gson = Gson()

    fun appInstalled(context: Context, initData: InitData) {
        runCatching {
            DbManager.addEventToQueue(
                context, Event(
                    eventType = EventType.AppInstalled,
                    body = gson.toJson(initData)
                )
            )
        }.logOnException()
    }

    fun appInfoUpdate(context: Context, initData: UpdateData) {
        runCatching {
            DbManager.addEventToQueue(
                context, Event(
                    eventType = EventType.AppInfoUpdated,
                    body = gson.toJson(initData)
                )
            )
        }.logOnException()
    }

    fun pushDelivered(context: Context, uniqKey: String) {
        runCatching {
            val fields = hashMapOf(
                EventParameters.UNIQ_KEY.fieldName to uniqKey
            )
            DbManager.addEventToQueue(
                context, Event(
                    eventType = EventType.PushDelivered,
                    additionalFields = fields
                )
            )
        }.logOnException()
    }

    fun pushClicked(context: Context, clickData: TrackClickData) {
        runCatching {
            DbManager.addEventToQueue(
                context, Event(
                    eventType = EventType.PushClicked,
                    body = gson.toJson(clickData)
                )
            )
        }.logOnException()
    }

    fun appStarted(context: Context, trackVisitData: TrackVisitData) {
        runCatching {
            DbManager.addEventToQueue(
                context, Event(
                    eventType = EventType.TrackVisit,
                    body = gson.toJson(trackVisitData)
                )
            )
        }.logOnException()
    }

    fun asyncOperation(context: Context, name: String, properties: Map<String, Any?>?) {
        runCatching {
            DbManager.addEventToQueue(
                context, Event(
                    eventType = EventType.AsyncOperation(name),
                    body = properties?.let(gson::toJson)
                )
            )
        }.logOnException()
    }

    fun sendEventsIfExist(context: Context) {
        runCatching {
            val keys = DbManager.getFilteredEventsKeys()

            if (keys.isNotEmpty()) {
                BackgroundWorkManager.startOneTimeService(context)
            }
        }.logOnException()
    }

}
