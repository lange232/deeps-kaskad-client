package com.zaneschepke.wireguardautotunnel.data.network.panel

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PanelSession(val server: String, val email: String, val token: String)

/**
 * Keeps the panel session between app starts. Stored in app-private
 * SharedPreferences; the token is a 30-day bearer credential, so root can
 * read it — acceptable for a VPN client config token, but do not reuse a
 * valuable password as the panel one.
 */
class PanelSessionStore(context: Context) {

    private val prefs = context.getSharedPreferences("panel_session", Context.MODE_PRIVATE)

    private val _session = MutableStateFlow(load())
    val session: StateFlow<PanelSession?> = _session.asStateFlow()

    private fun load(): PanelSession? {
        val server = prefs.getString(KEY_SERVER, null) ?: return null
        val email = prefs.getString(KEY_EMAIL, null) ?: return null
        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        return PanelSession(server, email, token)
    }

    fun save(session: PanelSession) {
        prefs
            .edit()
            .putString(KEY_SERVER, session.server)
            .putString(KEY_EMAIL, session.email)
            .putString(KEY_TOKEN, session.token)
            .apply()
        _session.value = session
    }

    fun clear() {
        prefs.edit().clear().apply()
        _session.value = null
    }

    private companion object {
        const val KEY_SERVER = "server"
        const val KEY_EMAIL = "email"
        const val KEY_TOKEN = "token"
    }
}
