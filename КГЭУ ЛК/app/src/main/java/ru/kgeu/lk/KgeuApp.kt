package ru.kgeu.lk

import android.app.Application
import ru.kgeu.lk.data.KgeuServiceLocator

class KgeuApp : Application() {
    lateinit var locator: KgeuServiceLocator
        private set

    override fun onCreate() {
        super.onCreate()
        locator = KgeuServiceLocator(this)
    }
}
