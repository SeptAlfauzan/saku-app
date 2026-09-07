package com.septaalfauzan.saku.di

import android.content.Context
import androidx.test.core.app.ApplicationProvider

object AppModuleHostSetup {
    fun apply() {
        val holderClass = Class.forName("com.septaalfauzan.saku.di.AndroidContextHolder")
        val instance = holderClass.getField("INSTANCE").get(null)
        val field = holderClass.getDeclaredField("applicationContext").apply {
            isAccessible = true
        }
        if (field.get(instance) == null) {
            field.set(
                instance,
                ApplicationProvider.getApplicationContext<Context>(),
            )
        }
    }
}
