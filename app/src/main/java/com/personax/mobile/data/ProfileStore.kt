package com.personax.mobile.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.net.HttpURLConnection
import java.net.URL

class ProfileStore(context: Context) {
    private val prefs = context.getSharedPreferences("personax_profiles", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val API_BASE = "https://personax.work/mobile"

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

    fun fetchRemoteProxies(): List<String> {
        return try {
            val url = URL("$API_BASE/api/proxies")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            val json = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            val type = object : TypeToken<List<String>>() {}.type
            val proxies: List<String> = gson.fromJson(json, type)
            saveProxyPool(proxies)
            proxies
        } catch (e: Exception) {
            getProxyPool()
        }
    }
}
