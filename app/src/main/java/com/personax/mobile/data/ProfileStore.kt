package com.personax.mobile.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ProfileStore(context: Context) {
    private val prefs = context.getSharedPreferences("personax_profiles", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getProfiles(): MutableList<MobileProfile> {
        val json = prefs.getString("profiles", null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<MobileProfile>>() {}.type
        return try { gson.fromJson(json, type) } catch (e: Exception) { mutableListOf() }
    }

    fun saveProfiles(profiles: List<MobileProfile>) {
        prefs.edit().putString("profiles", gson.toJson(profiles)).apply()
    }

    fun addProfile(profile: MobileProfile) {
        val list = getProfiles()
        list.add(profile)
        saveProfiles(list)
    }

    fun updateProfile(profile: MobileProfile) {
        val list = getProfiles()
        val idx = list.indexOfFirst { it.id == profile.id }
        if (idx >= 0) {
            list[idx] = profile
            saveProfiles(list)
        }
    }

    fun deleteProfile(id: String) {
        val list = getProfiles()
        list.removeAll { it.id == id }
        saveProfiles(list)
    }

    fun getActiveProfile(): MobileProfile? {
        return getProfiles().find { it.isActive }
    }

    fun setActiveProfile(id: String) {
        val list = getProfiles()
        list.forEach { it.isActive = (it.id == id) }
        saveProfiles(list)
    }

    fun getProxyPool(): MutableList<String> {
        val json = prefs.getString("proxy_pool", null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<String>>() {}.type
        return try { gson.fromJson(json, type) } catch (e: Exception) { mutableListOf() }
    }

    fun saveProxyPool(proxies: List<String>) {
        prefs.edit().putString("proxy_pool", gson.toJson(proxies)).apply()
    }
}
