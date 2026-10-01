package com.gokova.myanimelist.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gokova.myanimelist.core.datastore.di.SessionDataStore
import com.gokova.myanimelist.core.domain.model.UserAnimeStatistics
import com.gokova.myanimelist.core.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesImpl
    @Inject
    constructor(
        @SessionDataStore private val dataStore: DataStore<Preferences>,
    ) : UserPreferences {
        override val userProfile: Flow<UserProfile?> =
            dataStore.data.map { prefs ->
                val id = prefs[KEY_USER_ID] ?: return@map null
                val name = prefs[KEY_USER_NAME] ?: return@map null
                UserProfile(
                    id = id,
                    name = name,
                    pictureUrl = prefs[KEY_USER_PICTURE],
                    gender = prefs[KEY_USER_GENDER],
                    birthday = prefs[KEY_USER_BIRTHDAY],
                    location = prefs[KEY_USER_LOCATION],
                    joinedAt = prefs[KEY_USER_JOINED_AT],
                    statistics = readStatistics(prefs),
                )
            }

        override suspend fun saveUserProfile(
            profile: UserProfile,
            expectedSessionId: String?,
        ): Boolean {
            var saved = false
            dataStore.edit { prefs ->
                val currentSessionId =
                    if (prefs[AuthPreferencesImpl.KEY_ACCESS_TOKEN] != null) {
                        prefs[AuthPreferencesImpl.KEY_SESSION_ID] ?: "legacy_session"
                    } else {
                        null
                    }

                if (currentSessionId != null &&
                    (expectedSessionId == null || currentSessionId == expectedSessionId)
                ) {
                    prefs[KEY_USER_ID] = profile.id
                    prefs[KEY_USER_NAME] = profile.name
                    setOrRemove(prefs, KEY_USER_PICTURE, profile.pictureUrl)
                    setOrRemove(prefs, KEY_USER_GENDER, profile.gender)
                    setOrRemove(prefs, KEY_USER_BIRTHDAY, profile.birthday)
                    setOrRemove(prefs, KEY_USER_LOCATION, profile.location)
                    setOrRemove(prefs, KEY_USER_JOINED_AT, profile.joinedAt)
                    saveStatistics(prefs, profile.statistics)
                    saved = true
                }
            }
            return saved
        }

        override suspend fun clearUserProfile() {
            dataStore.edit { prefs ->
                clearProfileKeys(prefs)
            }
        }

        private fun setOrRemove(
            prefs: MutablePreferences,
            key: Preferences.Key<String>,
            value: String?,
        ) {
            if (value != null) {
                prefs[key] = value
            } else {
                prefs.remove(key)
            }
        }

        private fun Preferences.readInt(key: Preferences.Key<Int>): Int = this[key] ?: 0

        private fun Preferences.readFloat(key: Preferences.Key<Float>): Float = this[key] ?: 0f

        private fun readStatistics(prefs: Preferences): UserAnimeStatistics? {
            if (prefs[KEY_STATS_PRESENT] != true) return null
            return UserAnimeStatistics(
                numItemsWatching = prefs.readInt(KEY_STATS_WATCHING),
                numItemsCompleted = prefs.readInt(KEY_STATS_COMPLETED),
                numItemsOnHold = prefs.readInt(KEY_STATS_ON_HOLD),
                numItemsDropped = prefs.readInt(KEY_STATS_DROPPED),
                numItemsPlanToWatch = prefs.readInt(KEY_STATS_PLAN_TO_WATCH),
                numItems = prefs.readInt(KEY_STATS_NUM_ITEMS),
                numDaysWatched = prefs.readFloat(KEY_STATS_DAYS_WATCHED),
                numDaysWatching = prefs.readFloat(KEY_STATS_DAYS_WATCHING),
                numDaysCompleted = prefs.readFloat(KEY_STATS_DAYS_COMPLETED),
                numDaysOnHold = prefs.readFloat(KEY_STATS_DAYS_ON_HOLD),
                numDaysDropped = prefs.readFloat(KEY_STATS_DAYS_DROPPED),
                numDays = prefs.readFloat(KEY_STATS_DAYS),
                numEpisodes = prefs.readInt(KEY_STATS_EPISODES),
                numTimesRewatched = prefs.readInt(KEY_STATS_REWATCHED),
                meanScore = prefs.readFloat(KEY_STATS_MEAN_SCORE),
            )
        }

        private fun saveStatistics(
            prefs: MutablePreferences,
            stats: UserAnimeStatistics?,
        ) {
            if (stats != null) {
                prefs[KEY_STATS_PRESENT] = true
                prefs[KEY_STATS_WATCHING] = stats.numItemsWatching
                prefs[KEY_STATS_COMPLETED] = stats.numItemsCompleted
                prefs[KEY_STATS_ON_HOLD] = stats.numItemsOnHold
                prefs[KEY_STATS_DROPPED] = stats.numItemsDropped
                prefs[KEY_STATS_PLAN_TO_WATCH] = stats.numItemsPlanToWatch
                prefs[KEY_STATS_NUM_ITEMS] = stats.numItems
                prefs[KEY_STATS_DAYS_WATCHED] = stats.numDaysWatched
                prefs[KEY_STATS_DAYS_WATCHING] = stats.numDaysWatching
                prefs[KEY_STATS_DAYS_COMPLETED] = stats.numDaysCompleted
                prefs[KEY_STATS_DAYS_ON_HOLD] = stats.numDaysOnHold
                prefs[KEY_STATS_DAYS_DROPPED] = stats.numDaysDropped
                prefs[KEY_STATS_DAYS] = stats.numDays
                prefs[KEY_STATS_EPISODES] = stats.numEpisodes
                prefs[KEY_STATS_REWATCHED] = stats.numTimesRewatched
                prefs[KEY_STATS_MEAN_SCORE] = stats.meanScore
            } else {
                prefs.remove(KEY_STATS_PRESENT)
            }
        }

        companion object {
            private val KEY_USER_ID = longPreferencesKey("user_id")
            private val KEY_USER_NAME = stringPreferencesKey("user_name")
            private val KEY_USER_PICTURE = stringPreferencesKey("user_picture")
            private val KEY_USER_GENDER = stringPreferencesKey("user_gender")
            private val KEY_USER_BIRTHDAY = stringPreferencesKey("user_birthday")
            private val KEY_USER_LOCATION = stringPreferencesKey("user_location")
            private val KEY_USER_JOINED_AT = stringPreferencesKey("user_joined_at")
            private val KEY_STATS_PRESENT = booleanPreferencesKey("stats_present")
            private val KEY_STATS_WATCHING = intPreferencesKey("stats_watching")
            private val KEY_STATS_COMPLETED = intPreferencesKey("stats_completed")
            private val KEY_STATS_ON_HOLD = intPreferencesKey("stats_on_hold")
            private val KEY_STATS_DROPPED = intPreferencesKey("stats_dropped")
            private val KEY_STATS_PLAN_TO_WATCH = intPreferencesKey("stats_plan_to_watch")
            private val KEY_STATS_NUM_ITEMS = intPreferencesKey("stats_num_items")
            private val KEY_STATS_DAYS_WATCHED = floatPreferencesKey("stats_days_watched")
            private val KEY_STATS_DAYS_WATCHING = floatPreferencesKey("stats_days_watching")
            private val KEY_STATS_DAYS_COMPLETED = floatPreferencesKey("stats_days_completed")
            private val KEY_STATS_DAYS_ON_HOLD = floatPreferencesKey("stats_days_on_hold")
            private val KEY_STATS_DAYS_DROPPED = floatPreferencesKey("stats_days_dropped")
            private val KEY_STATS_DAYS = floatPreferencesKey("stats_days")
            private val KEY_STATS_EPISODES = intPreferencesKey("stats_episodes")
            private val KEY_STATS_REWATCHED = intPreferencesKey("stats_rewatched")
            private val KEY_STATS_MEAN_SCORE = floatPreferencesKey("stats_mean_score")

            internal fun clearProfileKeys(prefs: MutablePreferences) {
                prefs.remove(KEY_USER_ID)
                prefs.remove(KEY_USER_NAME)
                prefs.remove(KEY_USER_PICTURE)
                prefs.remove(KEY_USER_GENDER)
                prefs.remove(KEY_USER_BIRTHDAY)
                prefs.remove(KEY_USER_LOCATION)
                prefs.remove(KEY_USER_JOINED_AT)
                prefs.remove(KEY_STATS_PRESENT)
                prefs.remove(KEY_STATS_WATCHING)
                prefs.remove(KEY_STATS_COMPLETED)
                prefs.remove(KEY_STATS_ON_HOLD)
                prefs.remove(KEY_STATS_DROPPED)
                prefs.remove(KEY_STATS_PLAN_TO_WATCH)
                prefs.remove(KEY_STATS_NUM_ITEMS)
                prefs.remove(KEY_STATS_DAYS_WATCHED)
                prefs.remove(KEY_STATS_DAYS_WATCHING)
                prefs.remove(KEY_STATS_DAYS_COMPLETED)
                prefs.remove(KEY_STATS_DAYS_ON_HOLD)
                prefs.remove(KEY_STATS_DAYS_DROPPED)
                prefs.remove(KEY_STATS_DAYS)
                prefs.remove(KEY_STATS_EPISODES)
                prefs.remove(KEY_STATS_REWATCHED)
                prefs.remove(KEY_STATS_MEAN_SCORE)
            }
        }
    }
