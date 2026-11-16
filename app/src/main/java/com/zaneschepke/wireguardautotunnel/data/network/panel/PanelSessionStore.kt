package com.zaneschepke.wireguardautotunnel.data.network.panel

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PanelSession(val server: String, val email: String, val token: String)

/** What the user typed into the sign-in dialog, kept to refill it after a failure. */
data class PanelLoginDraft(val server: String, val email: String)

/**
 * Keeps the panel session between app starts. Stored in app-private SharedPreferences; the token is
 * a 30-day bearer credential, so root can read it — acceptable for a VPN client config token, but
 * do not reuse a valuable password as the panel one.
 *
 * The selected configuration is stored here too: an account owns several configurations (one per
 * device) and the app works with one of them at a time.
 */
class PanelSessionStore(context: Context) {

    private val prefs = context.getSharedPreferences("panel_session", Context.MODE_PRIVATE)

    private val _session = MutableStateFlow(load())
    val session: StateFlow<PanelSession?> = _session.asStateFlow()

    private val _selectedConfigId = MutableStateFlow(loadSelectedConfigId())
    val selectedConfigId: StateFlow<Int?> = _selectedConfigId.asStateFlow()

    private val _loginDraft = MutableStateFlow(loadLoginDraft())
    val loginDraft: StateFlow<PanelLoginDraft?> = _loginDraft.asStateFlow()

    private fun load(): PanelSession? {
        val server = prefs.getString(KEY_SERVER, null) ?: return null
        val email = prefs.getString(KEY_EMAIL, null) ?: return null
        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        return PanelSession(server, email, token)
    }

    private fun loadSelectedConfigId(): Int? =
        prefs.getInt(KEY_SELECTED_CONFIG, -1).takeIf { it > 0 }

    private fun loadLoginDraft(): PanelLoginDraft? {
        val server = prefs.getString(KEY_LAST_SERVER, null) ?: return null
        val email = prefs.getString(KEY_LAST_EMAIL, null) ?: return null
        return PanelLoginDraft(server, email)
    }

    /**
     * Remembers the address and email typed at sign-in so a typo or a failed attempt does not make
     * the user retype them. The password is never stored.
     */
    fun rememberLogin(server: String, email: String) {
        prefs.edit().putString(KEY_LAST_SERVER, server).putString(KEY_LAST_EMAIL, email).apply()
        _loginDraft.value = PanelLoginDraft(server, email)
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

    fun selectConfig(configId: Int?) {
        prefs.edit().putInt(KEY_SELECTED_CONFIG, configId ?: -1).apply()
        _selectedConfigId.value = configId
    }

    fun clear() {
        val draft = _loginDraft.value
        prefs.edit().clear().apply()
        _session.value = null
        _selectedConfigId.value = null
        // The address and email are not a secret and stay for the next sign-in.
        draft?.let { rememberLogin(it.server, it.email) }
    }

    private companion object {
        const val KEY_SERVER = "server"
        const val KEY_EMAIL = "email"
        const val KEY_TOKEN = "token"
        const val KEY_SELECTED_CONFIG = "selected_config_id"
        const val KEY_LAST_SERVER = "last_server"
        const val KEY_LAST_EMAIL = "last_email"
    }
}
