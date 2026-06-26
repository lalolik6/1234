package ru.kgeu.lk.data

import android.content.Context
import ru.kgeu.lk.data.api.KgeuApiClient
import ru.kgeu.lk.data.repository.KgeuRepository
import ru.kgeu.lk.data.storage.SessionStorage

class KgeuServiceLocator(context: Context) {
    private val appContext = context.applicationContext
    val sessionStorage = SessionStorage(appContext)
    private val apiClient = KgeuApiClient(sessionStorage)

    val repository = KgeuRepository(apiClient, sessionStorage)
}
