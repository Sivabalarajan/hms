package com.android.hms.utils

import com.github.kittinunf.fuel.core.*
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.CoroutineContext
import com.google.auth.oauth2.GoogleCredentials
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.extensions.jsonBody
import java.io.ByteArrayInputStream
import java.io.FileInputStream

object FCMNotification: CoroutineScope {
    private val masterJob = Job()
    override val coroutineContext: CoroutineContext get() = Dispatchers.IO + masterJob

    private const val authorizationKeyValue = "key=AIzaSyAmtHmTDdezZpqhN-7y2yKiyh0v91ckp2M" // "key=AIzaSyCWHGk2w_oXT6Ugsd3pU5wicEW4BGfz7MU"

    private const val contentType = "Content-Type"
    private const val appJson = "application/json"
    private const val authorizationHeader = "Authorization"
    private val fuelManager = FuelManager()
    private val header = mapOf(contentType to appJson, authorizationHeader to authorizationKeyValue)
    // private val authHeader = mapOf(authorizationHeader to authorizationKeyValue)

    init {
//        fuelManager.basePath = "https://fcm.googleapis.com/fcm" // https://fcm.googleapis.com/fcm/send
        fuelManager.basePath = "https://fcm.googleapis.com/v1/projects/hms-app-c182a/messages:send"

        fuelManager.baseHeaders = header
        fuelManager.timeoutInMillisecond = 60000 // 1 minute => 1 * 60 * 1000
        fuelManager.timeoutReadInMillisecond = 60000
    }

    private fun buildJsonString(title: String, message: String, regTokens: List<String>): String {
        val notification = JSONObject()
        notification.put("title", title)
        notification.put("body", message)
        val json = JSONObject()
        json.put("registration_ids", JSONArray(regTokens))
        json.put("content_available", true)
        json.put("notification", notification)
        return json.toString()
    }

    fun sendOld(title: String, message: String, regTokens: List<String>) {
        launch {
            if (regTokens.isEmpty()) return@launch
            val jsonBody = buildJsonString(title, message, regTokens)
            val (request, response, result) = fuelManager.request(Method.POST, "/send") // url)
                .body(jsonBody)
                .header(header)
                .response()
            // .responseJson()

            val (ret, err) = result
            if (ret == null || err != null) CommonUtils.printMessage(err.toString())
            // val retValue = Triple(request, response, result)
            // return Triple(request, response, result)
        }
    }

    // Function to load service account credentials from JSON content and obtain an access token
    private fun getAccessToken(): String {
        val serviceAccountJson = FcmJson.jsonText
        ByteArrayInputStream(serviceAccountJson.toByteArray()).use { stream ->
            val credentials = GoogleCredentials.fromStream(stream)
                .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))
            credentials.refreshIfExpired()
            return credentials.accessToken.tokenValue
        }
    }

    // Function to send a message to multiple devices
    fun send(title: String, body: String, registrationTokens: List<String>, data: Map<String, String>? = null) {
        if (registrationTokens.isEmpty()) return
        launch {
            try {
                val accessToken = getAccessToken()
                val url = "https://fcm.googleapis.com/v1/projects/hms-app-c182a/messages:send"

                registrationTokens.forEach { token ->
                    val messagePayload = buildMessagePayload(token, title, body, data)

                    val (_, response, result) = Fuel.post(url)
                        .header(
                            Headers.CONTENT_TYPE to "application/json; UTF-8",
                            Headers.AUTHORIZATION to "Bearer $accessToken"
                        )
                        .jsonBody(messagePayload)
                        .response()

                    val (ret, err) = result
                    if (ret == null || err != null) CommonUtils.printMessage(err.toString())

                    result.fold(
                        { success -> CommonUtils.printMessage("Message sent successfully: ${response.statusCode} - $success") },
                        { error -> CommonUtils.printMessage("Error sending message to $token: ${response.statusCode} - ${error.message}") }
                    )
                }
            }
            catch (e: Exception) {
                CommonUtils.printMessage("Error in sending FCM messages")
                CommonUtils.printException(e)
            }
        }
    }

    // Function to build the message payload
    private fun buildMessagePayload(registrationToken: String, title: String, body: String, data: Map<String, String>? = null): String {
        val dataFields = data?.entries?.joinToString(",") { "\"${it.key}\": \"${it.value}\"" } ?: ""
        return """
        {
            "message": {
                "token": "$registrationToken",
                "notification": {
                    "title": "$title",
                    "body": "$body"
                },
                "data": { $dataFields }
            }
        }
    """.trimIndent()
    }

    // Function to load service account credentials and obtain an access token
    private fun getAccessToken(serviceAccountPath: String): String {
        FileInputStream(serviceAccountPath).use { stream ->
            val credentials = GoogleCredentials.fromStream(stream)
                .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))
            credentials.refreshIfExpired()
            return credentials.accessToken.tokenValue
        }
    }

    // Example usage
    private fun mainTest() {
        val serviceAccountPath = "path/to/serviceAccountKey.json" // Replace with your service account key file path
        val projectId = "YOUR_PROJECT_ID" // Replace with your Firebase project ID
        val registrationTokens = listOf(
            "DEVICE_REGISTRATION_TOKEN_1", // Replace with actual device registration tokens
            "DEVICE_REGISTRATION_TOKEN_2",
            "DEVICE_REGISTRATION_TOKEN_3"
        )
        val title = "Hello"
        val body = "This is a test notification"
        val data = mapOf("key1" to "value1", "key2" to "value2")

        // sendMessagesToDevices(serviceAccountPath, projectId, registrationTokens, title, body, data)
    }

    object FcmJson {
        const val jsonText = "{\n" +
                "  \"type\": \"service_account\",\n" +
                "  \"project_id\": \"hms-app-c182a\",\n" +
                "  \"private_key_id\": \"ad4b0e612b1958f69a8d187dd8b0715f5e61c832\",\n" +
                "  \"private_key\": \"-----BEGIN PRIVATE KEY-----\\nMIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC6y5iUSbhok3N6\\n0nHxGWpB2Bvcpw0+rJvIsYEkGFFWAaX6fddcJk6yGidvx/+aHUrfFzuLFvlTSQX0\\nlHrGPRiqOrggYBA5u6ZmCuSWvBIxoc7VSxc1Ls35mZx9nfPwWio+z72nnv/e9l8h\\niIYgkSE9XoNs/UW4cGBUgDeZ0MPubjApn0eVPsENZE8KH/dGjRBzLdJq8aJeTofe\\ncXnteiYKEnEhKtDsP7Yy3b5k3e9XlLuLXv/h0rGq8IROkUMbWN9YS+BEXs/8++PO\\nrtV7JpoFMCRqyVau3E/QjxVXiY+cHiIprOsY1f5cYbDTm4AFYxoMTghgUfONhIcO\\np5/K0kW9AgMBAAECggEABkT0qm8NYp7ATqwz6TTb9uwuUzbDU3BiC73zAnRTSbp+\\n1Z9voPojXge4SlPBTAZ7qTn5/gfkkMHWRbua1OVFp9qWRdyziE+4aZuOYuv0OF8H\\nHOvxeKl8SrC393qO4c94GrX71l2YMBi/LKLzM+VrFi8CSwaQVnzL3uPGxsGimD5j\\nCB4yl6Ae/COLRJVd9/STuqN0bNS6InbH2qVCAxm9HpOOnxTl8/gicOvzoyqWEtiO\\nUd2rdB2igcEOR6wiFfLt6mM33NWRqUwhGl1ge18c3kgzSvm6W4JKX5pZ44gEGa2o\\nQvoJJpi+kXUwg15DXqYJ7CsXWwFydGLleIwT5uVDCQKBgQDe6Sku9u0zGx+SFH9R\\n4czqdJUZoIeKPry4MU/1g6M3POqKKnALzaLUdvZTaWrGqdTHzx7MjXAHw/k8nXQZ\\n8sZBd7k1cLs+DURvcdksCvF8av6pqnCMJYWTa2HK+FHxiisHgA0Hw9/EbPLD/hUY\\nPeL9Oh7qBUZVb/dqawtOiwmKgwKBgQDWhgJrjTJiquLnfrOTaNSTZPhz241FDIP+\\nh55kM/WKKWS/mRKLGwSaSlwcLQzgEvpVrqiDkSa5DoqGsAqpFEALuxrO8kqAVM8+\\nWWFdYA3L1LoSVW7vcw6jONHfCEP/Q4LeoJrkVDqqJzusMuo5ZgnwVoMQbLxOV2Zx\\nMwCukdD6vwKBgFLhIDXdC3539DoNMvWP3e/CXvmDTUHiZ6qYcyFWy2WrWYz7kGNj\\nFsqmXJnQ1Sl+VaXg5ZOqdt/8Rnv6EwPe10XaLNuoyYWdyzcgfl24x+a5pWwR+fyx\\nqsOQJnEf+N+0vqDwRGgBtP5Ef9LNcqxrvizm4j2irm7vzW8c07xlR6vfAoGBAKUR\\n9zjFLFkp/3F8WDf8uV41msyjy3+vUr3/o0ydgrnughzAb7lkb9U9lMUFkVu2nrRh\\nsj+S6IYLf2KGgZ16/JFKRyWo4NUA7XufkUElkVgf95H19WgBU+ka5rtFA/WfAEvI\\nr4c0nPOjPtKRVxQVxV4PY8i2sutha219fm/VukXHAoGBAMe1cOd+GgC/4zgkhAW+\\n1yI9bRUxmWaPfuWw1R9ov43mHc4VYzJf4JAz5tYClqsQwoLbP3+KRSMGrLj/XYrH\\njfSN2fw5yS9/1fkxNlyBt5IPB89Mga68ZuQGx19xcGE+Bs+cxbOnyvjIHkBgDxLb\\nBlcFWR3BSKkVea7GhsjtJd1Y\\n-----END PRIVATE KEY-----\\n\",\n" +
                "  \"client_email\": \"firebase-adminsdk-w7aed@hms-app-c182a.iam.gserviceaccount.com\",\n" +
                "  \"client_id\": \"116994422909424537309\",\n" +
                "  \"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\",\n" +
                "  \"token_uri\": \"https://oauth2.googleapis.com/token\",\n" +
                "  \"auth_provider_x509_cert_url\": \"https://www.googleapis.com/oauth2/v1/certs\",\n" +
                "  \"client_x509_cert_url\": \"https://www.googleapis.com/robot/v1/metadata/x509/firebase-adminsdk-w7aed%40hms-app-c182a.iam.gserviceaccount.com\",\n" +
                "  \"universe_domain\": \"googleapis.com\"\n" +
                "}\n"
    }

}
/*    fun execute(url: String, jsonString: String): Triple<Request, Response, Result<FuelJson, FuelError>>? {
        var retValue: Triple<Request, Response, Result<FuelJson, FuelError>>? = null
        try {
            val (request, response, result) = fuelManager.request(Method.POST, url)
                .body(jsonString, charSet)
//                    .header(header)
                .responseJson()

            retValue = Triple(request, response, result)

            val (ret, err) = result
            if (ret == null || err != null)
                CommonUtils.printMessage(err.toString())

            return Triple(request, response, result)
        } catch (e: Exception) {
            CommonUtils.printMessage("Execution in URL: $url")
            CommonUtils.printException(e)
        }
        return retValue
    }

    fun executeAsync(url: String, jsonString: String) {
        launch { execute(url, jsonString) }
    }

    private fun execute(url: String): Triple<Request, Response, Result<FuelJson, FuelError>>? {
        var retValue: Triple<Request, Response, Result<FuelJson, FuelError>>? = null
        try {
            val (request, response, result) = fuelManager.request(Method.GET, url).responseJson() // { _, _, result  // request, response, result
            retValue = Triple(request, response, result)
            val (ret, err) = result
            if (ret == null || err != null)
                CommonUtils.printMessage(err.toString())

            return Triple(request, response, result)
        } catch (e: Exception) {
            CommonUtils.printMessage("Execution in URL: $url")
            CommonUtils.printException(e)
        }
        return retValue
    }

    fun executeAsync(url: String) {
        launch { execute(url) }
    }

    fun getIdAndTime(retValue: String?) : Pair<Long?, Long?> {
        // val retValue = Gson().fromJson(resultValue, String::class.java)
        if (retValue.isNullOrBlank()) return Pair(null, null)
        val result = retValue.split(",")
        if (result.size != 2) return Pair(null, null)
        return Pair(result[0].toLongOrNull(), result[1].toLongOrNull())
    }

    /* fun exist(id: Long, nickname: String): Int {
        val jsonString = "{\"id\":$id,\"nickname\":\"$nickname\"}"
        return exist("/user/existbyidname", jsonString)
    }

    private fun exist(url: String, jsonString: String): Int {
        try {
            val (_, _, result) = execute(url, jsonString) ?: return Globals.gServerIssueCode
            val (ret, _) = result
            return if (ret != null) Gson().fromJson(ret.content, String::class.java).toInt() else Globals.gServerIssueCode
        } catch (e: Exception) {
            CommonUtils.printException(e)
        }
        return Globals.gServerIssueCode
    } */



/* body = Json.obj(
"registration_ids" -> tokens,
"dry_run" -> FcmConfig.dryRun,
"content_available" -> true,
"notification" -> Json.toJson(fcmNotification),
"data" -> Json.toJson(fcmData)
)

def send(tokens: Seq[String]): Unit = Future {
    val maxTokensPerRequest = 999
    val tokenList = tokens.grouped(maxTokensPerRequest).toList
    for (tokensSplit <- tokenList) Future {
        val body = buildBody(tokensSplit)
        if (body != null) {
            val call = wsClient.url(FcmConfig.endpoint)
                .withHttpHeaders(("Authorization", s"key=${FcmConfig.key}"))
            .post(body).zip(Future.successful(tokensSplit))

            call.flatMap { case (response, originalTokens) =>
                handleFcmResponse(response, originalTokens)
            }
        }
    }
} */