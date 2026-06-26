package ru.kgeu.lk.data.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.kgeu.lk.data.model.UserProfile

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kgeu_lk_prefs")

class SessionStorage(context: Context) {
    private val appContext = context.applicationContext
    private val json = Json { ignoreUnknownKeys = true }

    private val securePrefs = EncryptedSharedPreferences.create(
        appContext,
        "kgeu_lk_secure",
        MasterKey.Builder(appContext).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private object Keys {
        val fingerprint = stringPreferencesKey("fingerprint")
    }

    suspend fun getFingerprint(): String {
        val existing = appContext.dataStore.data.map { it[Keys.fingerprint] }.first()
        if (!existing.isNullOrBlank()) return existing

        val generated = securePrefs.getString("fingerprint", null)
            ?: java.util.UUID.randomUUID().toString().also {
                securePrefs.edit().putString("fingerprint", it).apply()
            }

        appContext.dataStore.edit { prefs -> prefs[Keys.fingerprint] = generated }
        return generated
    }

    fun saveToken(token: String) {
        securePrefs.edit().putString("auth_token", token).apply()
    }

    fun getToken(): String? = securePrefs.getString("auth_token", null)

    fun saveLogin(login: String) {
        securePrefs.edit().putString("login", login).apply()
    }

    fun getLogin(): String? = securePrefs.getString("login", null)

    fun savePassword(password: String) {
        securePrefs.edit().putString("password", password).apply()
    }

    fun getPassword(): String? = securePrefs.getString("password", null)

    fun saveUser(user: UserProfile) {
        securePrefs.edit().putString("user_profile", json.encodeToString(user)).apply()
    }

    fun getUser(): UserProfile? {
        val raw = securePrefs.getString("user_profile", null) ?: return null
        return runCatching { json.decodeFromString<UserProfile>(raw) }.getOrNull()
    }

    fun saveGroupId(groupId: Int) {
        securePrefs.edit().putInt("group_id", groupId).apply()
    }

    fun getGroupId(): Int? {
        val value = securePrefs.getInt("group_id", -1)
        return if (value > 0) value else null
    }

    fun clear() {
        securePrefs.edit().clear().apply()
    }
}
