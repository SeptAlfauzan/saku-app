package com.septaalfauzan.saku.data.database

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.septaalfauzan.saku.data.repository.RoomNotificationSettingsRepository
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertTrue

class TrackingPipelineSpinTest {

    private fun buildInMemory(): AppDatabase =
        Room.inMemoryDatabaseBuilder<AppDatabase>(
            factory = { AppDatabaseConstructor.initialize() },
        ).setDriver(BundledSQLiteDriver())
            .build()

    @Test
    fun trackingPipelineEmitsFiniteDistinctValues() {
        val db = buildInMemory()
        val repo = RoomNotificationSettingsRepository(db.sourceDao(), db.settingsDao())

        var emissions = 0
        var distinct = 0
        var last: Triple<Boolean, Boolean, List<com.septaalfauzan.saku.domain.model.NotificationSource>>? = null
        val started = CountDownLatch(1)

        val scope = CoroutineScope(Dispatchers.Default)
        val job = scope.launch {
            combine(
                repo.observeTrackingEnabled(),
                repo.observeAutoConfirm(),
                repo.observeSources(),
            ) { tracking, auto, sources -> Triple(tracking, auto, sources) }
                .collect {
                    emissions++
                    if (it != last) {
                        distinct++
                        last = it
                    }
                    started.countDown()
                }
        }

        val reached = started.await(10, TimeUnit.SECONDS)
        Thread.sleep(3_000)
        job.cancel()
        scope.cancel()
        db.close()

        assertTrue(reached, "pipeline never emitted an initial state")
        assertTrue(
            emissions < 50,
            "pipeline re-emitting: emissions=$emissions distinct=$distinct after 3s idle",
        )
    }
}