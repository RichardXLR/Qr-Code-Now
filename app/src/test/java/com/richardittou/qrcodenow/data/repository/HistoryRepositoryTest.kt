package com.richardittou.qrcodenow.data.repository

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.richardittou.qrcodenow.data.database.HistoryDatabase
import com.richardittou.qrcodenow.domain.model.QrContent
import com.richardittou.qrcodenow.domain.model.ScanOrigin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HistoryRepositoryTest {
    private lateinit var database: HistoryDatabase
    private lateinit var repository: HistoryRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, HistoryDatabase::class.java)
            .setDriver(AndroidSQLiteDriver())
            .build()
        repository = HistoryRepositoryImpl(database.historyDao())
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun historySupportsSearchFavoritesAndBatchDeletion() = runTest {
        val firstId = repository.add(QrContent.Text("Primeiro conteúdo"), ScanOrigin.CAMERA)
        val secondId = repository.add(QrContent.Text("Segundo conteúdo pesquisável"), ScanOrigin.GALLERY)
        val thirdId = repository.add(QrContent.Text("Terceiro conteúdo"), ScanOrigin.GENERATED)
        repository.add(QrContent.Text("100% local"), ScanOrigin.GENERATED)

        assertThat(repository.observe().first()).hasSize(4)
        assertThat(repository.observe("pesquisável").first().map { it.id }).containsExactly(secondId)
        assertThat(repository.observe("%").first().map { it.rawContent }).containsExactly("100% local")

        repository.setFavorite(firstId, true)
        val favorites = repository.observe(favoritesOnly = true).first()
        assertThat(favorites.map { it.id }).containsExactly(firstId)
        assertThat(favorites.single().origin).isEqualTo(ScanOrigin.CAMERA.name)

        repository.delete(setOf(firstId, thirdId))
        assertThat(repository.observe().first().map { it.rawContent }).containsExactly("100% local", "Segundo conteúdo pesquisável")

        repository.clear()
        assertThat(repository.observe().first()).isEmpty()
    }
}
