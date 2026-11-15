package com.zaneschepke.wireguardautotunnel.data.network.panel

import android.content.Context
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.util.StringValue
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.delete
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import timber.log.Timber

class PanelApiException(val stringValue: StringValue) : Exception()

/**
 * Client for the deeps kaskad panel API.
 *
 * An account owns several WireGuard configurations, one per device, and every configuration has its
 * own internet exit order. Error texts returned by the panel are meant for the user and are shown
 * as they are; only 401/429 are mapped to fixed local strings.
 *
 * The panel usually serves a self-signed certificate, so the build can pin one: drop the panel's
 * PEM certificate into `app/src/main/res/raw/panel.crt` and only that certificate is trusted for
 * API calls. Without a bundled certificate the default system trust store is used (a panel behind a
 * real CA, e.g. Let's Encrypt, works out of the box).
 */
class PanelApi(context: Context) {

    private val client =
        HttpClient(OkHttp) {
            val trustManager = pinnedTrustManager(context)
            if (trustManager != null) {
                engine {
                    config {
                        val sslContext =
                            SSLContext.getInstance("TLS").apply {
                                init(null, arrayOf(trustManager), null)
                            }
                        sslSocketFactory(sslContext.socketFactory, trustManager)
                    }
                }
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000L
                connectTimeoutMillis = 15_000L
                socketTimeoutMillis = 30_000L
            }
            expectSuccess = false
        }

    private fun pinnedTrustManager(context: Context): X509TrustManager? {
        return try {
            val certificate =
                context.resources.openRawResource(R.raw.panel).use { input ->
                    CertificateFactory.getInstance("X.509").generateCertificate(input)
                }
            val keyStore =
                KeyStore.getInstance(KeyStore.getDefaultType()).apply {
                    load(null, null)
                    setCertificateEntry("panel", certificate)
                }
            val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            factory.init(keyStore)
            factory.trustManagers.filterIsInstance<X509TrustManager>().first()
        } catch (e: Exception) {
            Timber.w("No usable panel certificate bundled, using system trust store")
            null
        }
    }

    private fun apiUrl(server: String, path: String): String {
        val base = server.trim().trimEnd('/')
        require(base.startsWith("https://")) { "panel URL must start with https://" }
        return base + path
    }

    private suspend fun errorDetail(response: HttpResponse): String? = runCatching {
        Json.parseToJsonElement(response.bodyAsText())
            .jsonObject["detail"]
            ?.jsonPrimitive
            ?.contentOrNull
    }
        .getOrNull()
        ?.takeIf { it.isNotBlank() }

    private suspend fun requireOk(response: HttpResponse): HttpResponse {
        if (response.status.value in 200..299) return response
        val detail = errorDetail(response)
        throw when (response.status.value) {
            401 ->
                PanelApiException(StringValue.StringResource(R.string.error_panel_session_expired))
            429 -> PanelApiException(StringValue.StringResource(R.string.error_panel_rate_limited))
            // 400/404/409/500 carry a user-facing explanation; 409 is also the
            // configuration limit, its text names the numbers.
            400,
            404,
            409,
            500 ->
                detail?.let { PanelApiException(StringValue.DynamicString(it)) }
                    ?: PanelApiException(StringValue.StringResource(R.string.error_panel_connect))
            else -> PanelApiException(StringValue.StringResource(R.string.error_panel_connect))
        }
    }

    private fun parseConfig(element: JsonObject): PanelConfig =
        PanelConfig(
            id = element.getValue("id").jsonPrimitive.int,
            name = element.getValue("name").jsonPrimitive.content,
            ip = element["ip"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            enabled = element["enabled"]?.jsonPrimitive?.booleanOrNull == true,
            internet = element["internet"]?.jsonPrimitive?.booleanOrNull == true,
            exits = element["exits"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
            handshake = element["handshake"]?.jsonPrimitive?.long ?: 0L,
            rx = element["rx"]?.jsonPrimitive?.long ?: 0L,
            tx = element["tx"]?.jsonPrimitive?.long ?: 0L,
        )

    private fun parseConfigs(body: String): List<PanelConfig> =
        Json.parseToJsonElement(body)
            .jsonObject["configs"]
            ?.jsonArray
            ?.map { parseConfig(it.jsonObject) }
            .orEmpty()

    private suspend fun requireLoginOk(response: HttpResponse): HttpResponse {
        if (response.status.value in 200..299) return response
        val detail = errorDetail(response)
        throw when (response.status.value) {
            429 -> PanelApiException(StringValue.StringResource(R.string.error_panel_rate_limited))
            401 ->
                PanelApiException(
                    detail?.let { StringValue.DynamicString(it) }
                        ?: StringValue.StringResource(R.string.error_panel_auth),
                )
            else -> PanelApiException(StringValue.StringResource(R.string.error_panel_connect))
        }
    }

    /** Signs in and returns the token together with the account configurations. */
    suspend fun login(server: String, email: String, password: String): PanelLoginResult {
        val response =
            requireLoginOk(
                client.submitForm(
                    url = apiUrl(server, "/api/login"),
                    formParameters =
                        Parameters.build {
                            append("email", email.trim())
                            append("password", password)
                        },
                ),
            )
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        return PanelLoginResult(
            token = body.getValue("token").jsonPrimitive.content,
            email = body["email"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            configs = body["configs"]?.jsonArray?.map { parseConfig(it.jsonObject) }.orEmpty(),
        )
    }

    /** The account configurations with traffic and handshake counters. */
    suspend fun fetchConfigs(server: String, token: String): List<PanelConfig> {
        val response =
            requireOk(
                client.get(apiUrl(server, "/api/me")) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                },
            )
        return parseConfigs(response.bodyAsText())
    }

    /** Creates a configuration; an empty name is generated by the panel. */
    suspend fun createConfig(server: String, token: String, name: String? = null): PanelConfig {
        val response =
            requireOk(
                client.submitForm(
                    url = apiUrl(server, "/api/me/configs"),
                    formParameters =
                        Parameters.build { if (!name.isNullOrBlank()) append("name", name.trim()) },
                ) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                },
            )
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        return parseConfig(body.getValue("config").jsonObject)
    }

    /** Downloads one configuration as a WireGuard config; the first one by default. */
    suspend fun fetchConfig(server: String, token: String, configId: Int? = null): String {
        val path = if (configId == null) "/api/me/conf" else "/api/me/configs/$configId/conf"
        val response =
            requireOk(
                client.get(apiUrl(server, path)) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                },
            )
        return response.bodyAsText()
    }

    /** Turns a configuration on or off; returns the new state. */
    suspend fun toggleConfig(server: String, token: String, configId: Int): Boolean {
        val response =
            requireOk(
                client.post(apiUrl(server, "/api/me/configs/$configId/toggle")) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                },
            )
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        return body["enabled"]?.jsonPrimitive?.booleanOrNull == true
    }

    /** Removes a configuration and its keys; there is no undo. */
    suspend fun deleteConfig(server: String, token: String, configId: Int) {
        requireOk(
            client.delete(apiUrl(server, "/api/me/configs/$configId")) {
                header(HttpHeaders.Authorization, "Bearer $token")
            },
        )
    }

    /** Internet exits granted to the account, in the order of the given configuration. */
    suspend fun fetchExits(server: String, token: String, configId: Int? = null): List<PanelExit> {
        val path = if (configId == null) "/api/me/exits" else "/api/me/exits?config_id=$configId"
        val response =
            requireOk(
                client.get(apiUrl(server, path)) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                },
            )
        return Json.parseToJsonElement(response.bodyAsText()).jsonArray.map { element ->
            val exit = element.jsonObject
            PanelExit(
                id = exit.getValue("id").jsonPrimitive.int,
                name = exit.getValue("name").jsonPrimitive.content,
                comment = exit["comment"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                alive = exit["alive"]?.jsonPrimitive?.booleanOrNull == true,
                rttMs = exit["rtt_ms"]?.jsonPrimitive?.doubleOrNull,
            )
        }
    }

    /** Replaces the configuration's exit order; the panel rejects partial lists. */
    suspend fun setExitOrder(
        server: String,
        token: String,
        configId: Int?,
        exitIds: List<Int>,
    ) {
        requireOk(
            client.submitForm(
                url = apiUrl(server, "/api/me/exits/order"),
                formParameters =
                    Parameters.build {
                        if (configId != null) append("config_id", configId.toString())
                        exitIds.forEach { append("exit_id", it.toString()) }
                    },
            ) {
                header(HttpHeaders.Authorization, "Bearer $token")
            },
        )
    }

    /** Best-effort token revocation; the network result does not matter. */
    suspend fun logout(server: String, token: String) {
        try {
            client.post(apiUrl(server, "/api/logout")) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        } catch (e: Exception) {
            Timber.w(e, "Panel logout failed, clearing the session locally anyway")
        }
    }
}

/** One WireGuard configuration of the account: one device. */
data class PanelConfig(
    val id: Int,
    val name: String,
    val ip: String,
    val enabled: Boolean,
    val internet: Boolean,
    val exits: List<String>,
    val handshake: Long,
    val rx: Long,
    val tx: Long,
)

data class PanelLoginResult(val token: String, val email: String, val configs: List<PanelConfig>)

data class PanelExit(
    val id: Int,
    val name: String,
    val comment: String,
    val alive: Boolean,
    val rttMs: Double?,
)
