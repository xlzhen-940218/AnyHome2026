package com.draco.anyhome.viewmodels

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.draco.anyhome.models.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SelectActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(application.applicationContext)

    private val allApps = mutableListOf<AppInfo>()

    private val _appList = MutableLiveData<List<AppInfo>>(emptyList())
    val appList: LiveData<List<AppInfo>> = _appList

    private val _isLoading = MutableLiveData(true)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _currentHomePackage = MutableLiveData("")
    val currentHomePackage: LiveData<String> = _currentHomePackage

    private var currentQuery: String = ""

    init {
        updateList()
    }

    /**
     * Get the currently saved home app package name
     */
    fun getSavedHomeApp(): String {
        return sharedPrefs.getString("home_app", "") ?: ""
    }

    /**
     * Set a new home app package name
     */
    fun setHomeApp(packageName: String) {
        sharedPrefs.edit().putString("home_app", packageName).apply()
        _currentHomePackage.value = packageName
        updateHighlightState()
    }

    /**
     * Clear saved home app selection
     */
    fun clearHomeApp() {
        sharedPrefs.edit().remove("home_app").apply()
        _currentHomePackage.value = ""
        updateHighlightState()
    }

    /**
     * Filter list by keyword (matches app label or package name)
     */
    fun filter(query: String) {
        currentQuery = query.trim()
        applyFilter()
    }

    private fun applyFilter() {
        if (currentQuery.isBlank()) {
            _appList.value = allApps.toList()
        } else {
            val lower = currentQuery.lowercase()
            _appList.value = allApps.filter {
                it.label.lowercase().contains(lower) || it.id.lowercase().contains(lower)
            }
        }
    }

    private fun updateHighlightState() {
        val current = getSavedHomeApp()
        for (i in allApps.indices) {
            allApps[i] = allApps[i].copy(isCurrentHome = (allApps[i].id == current))
        }
        applyFilter()
    }

    fun updateList() {
        viewModelScope.launch {
            _isLoading.value = true
            val context = getApplication<Application>().applicationContext
            val packageManager = context.packageManager
            val currentHome = getSavedHomeApp()
            _currentHomePackage.value = currentHome

            val loadedList = withContext(Dispatchers.IO) {
                val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }

                val activities: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.queryIntentActivities(
                        launcherIntent,
                        PackageManager.ResolveInfoFlags.of(0L)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.queryIntentActivities(launcherIntent, 0)
                }

                val result = mutableListOf<AppInfo>()
                for (app in activities) {
                    val pkg = app.activityInfo.packageName
                    if (pkg == context.packageName) continue

                    val label = try {
                        app.activityInfo.loadLabel(packageManager).toString()
                    } catch (e: Exception) {
                        pkg
                    }

                    result.add(
                        AppInfo(
                            label = label,
                            id = pkg,
                            isCurrentHome = (pkg == currentHome)
                        )
                    )
                }

                result.sortBy { it.label.lowercase() }
                result
            }

            allApps.clear()
            allApps.addAll(loadedList)
            applyFilter()
            _isLoading.value = false
        }
    }
}