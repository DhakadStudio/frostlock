package com.frostlock.app.ui.screens.onboarding

import com.frostlock.app.data.local.datastore.SettingsDataStore
import com.frostlock.app.data.local.db.ProfileDao
import com.frostlock.app.data.local.db.ProfileEntity
import com.frostlock.app.data.local.db.StreakDao
import com.frostlock.app.data.local.db.StreakEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OnboardingViewModelTest {

    private class FakeProfileDao : ProfileDao {
        var savedProfile: ProfileEntity? = null
        var insertedCount = 0
        override fun getProfile(): Flow<ProfileEntity?> = MutableStateFlow(savedProfile)
        override fun getProfileSync(): ProfileEntity? = savedProfile
        override fun insertProfile(profile: ProfileEntity): Long {
            savedProfile = profile
            insertedCount++
            return 1L
        }
    }

    private class FakeStreakDao : StreakDao {
        val insertedStreaks = mutableListOf<StreakEntity>()
        override fun getAllStreaks() = MutableStateFlow(insertedStreaks)
        override fun insertStreak(streak: StreakEntity): Long {
            insertedStreaks.add(streak)
            return 1L
        }
    }

    private lateinit var viewModel: OnboardingViewModel
    private lateinit var fakeProfileDao: FakeProfileDao
    private lateinit var fakeStreakDao: FakeStreakDao
    private lateinit var fakeDataStore: SettingsDataStore

    @Before
    fun setUp() {
        fakeProfileDao = FakeProfileDao()
        fakeStreakDao = FakeStreakDao()
        fakeDataStore = SettingsDataStore(null)
        viewModel = OnboardingViewModel(fakeProfileDao, fakeStreakDao, fakeDataStore).apply {
            testDispatcher = Dispatchers.Unconfined
        }
    }

    @Test
    fun emptyGoalsBlocked() {
        viewModel.onPageChanged(OnboardingPages.GOALS)
        assertFalse(viewModel.canProceed())
    }

    @Test
    fun emptyTriggersBlocked() {
        viewModel.onPageChanged(OnboardingPages.TRIGGERS)
        assertFalse(viewModel.canProceed())
    }

    @Test
    fun reasonUnder10CharactersBlocked() {
        viewModel.onPageChanged(OnboardingPages.REASON)
        viewModel.onReasonChanged("Too short")
        assertFalse(viewModel.canProceed())
    }

    @Test
    fun reasonOver300CharactersBlocked() {
        viewModel.onPageChanged(OnboardingPages.REASON)
        val longReason = "a".repeat(301)
        viewModel.onReasonChanged(longReason)
        assertFalse(viewModel.canProceed())
    }

    @Test
    fun validDataSaves() {
        viewModel.onPageChanged(OnboardingPages.GOALS)
        viewModel.onGoalToggled("Study focus")
        viewModel.onPageChanged(OnboardingPages.TRIGGERS)
        viewModel.onTriggerToggled("Stress")
        viewModel.onPageChanged(OnboardingPages.REASON)
        viewModel.onReasonChanged("This is a valid reason that exceeds 10 characters.")
        assertTrue(viewModel.canProceed())

        viewModel.finishOnboarding {}
        assertTrue(fakeProfileDao.insertedCount > 0)
        assertTrue(fakeStreakDao.insertedStreaks.isNotEmpty())
    }

    @Test
    fun onboardingDoneSetToTrueAfterFinish() {
        viewModel.finishOnboarding {}
        assertTrue(viewModel.uiState.value.isCompleted)
    }
}
