package me.spjere.appcore.android.preference

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import me.sphere.appcore.PreferenceStore

class PreferenceStoreImpl(
    private val context: Context
) : PreferenceStore {

    private val dataStore by lazy {
        PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile(SPHERE_DATASTORE_PREF)
        }
    }

    override suspend fun get(key: String): String? {
        return dataStore.data
            .map {
                val keyPreference = stringPreferencesKey(key)
                it[keyPreference]
            }
            .catch { emit(null) }
            .first()
    }

    override suspend fun set(key: String, value: String?) {
        dataStore.edit { preferences ->
            val keyPreference = stringPreferencesKey(key)

            if (value == null && preferences.contains(keyPreference)) {
                preferences.remove(keyPreference)
            } else if (value != null) {
                preferences[keyPreference] = value
            }
        }
    }

    suspend fun clearStore() {
        dataStore.edit { preferences ->
            preferences.asMap().entries.forEach { entry ->
                preferences.remove(entry.key)
            }
        }
    }

    companion object {
        private const val SPHERE_DATASTORE_PREF = "SPHERE_DATASTORE_PREF"
    }
}
