package com.zaneschepke.wireguardautotunnel.data.network.panel

import android.content.Context
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.util.StringValue
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber

class PanelApiException(val stringValue: StringValue) : Exception()

/**
 * Client for the deeps kaskad panel API (`/api/login`, `/api/me/conf`).
 *
 * The panel usually serves a self-signed certificate, so the build can pin
 * one: drop the panel's PEM certificate into `app/src/main/res/raw/panel.crt`
 * and only that certificate is trusted for API calls. Without a bundled
 * certificate the default system trust store is used (a panel behind a real
 * CA, e.g. Let's Encrypt, works out of the box).
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
            val factory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
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

    private suspend fun requireOk(response: HttpResponse): HttpResponse {
        when (response.status.value) {
            in 200..299 -> return response
            401 ->
                throw PanelApiException(StringValue.StringResource(R.string.error_panel_auth))
            429 ->
                throw PanelApiException(
                    StringValue.StringResource(R.string.error_panel_rate_limited)
                )
            else ->
                throw PanelApiException(StringValue.StringResource(R.string.error_panel_connect))
        }
    }

    /** Signs in and returns the bearer token. */
    suspend fun login(server: String, email: String, password: String): String {
        val response =
            requireOk(
                client.submitForm(
                    url = apiUrl(server, "/api/login"),
                    formParameters =
                        Parameters.build {
                            append("email", email.trim())
                            append("password", password)
                        },
                )
            )
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        return body.getValue("token").jsonPrimitive.content
    }

    /** Downloads the client's awg0 config as text. */
    suspend fun fetchConfig(server: String, token: String): String {
        val response =
            requireOk(
                client.get(apiUrl(server, "/api/me/conf")) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            )
        return response.bodyAsText()
    }
}
