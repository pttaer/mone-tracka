package com.monetracka.shared.ui.root

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.monetracka.shared.domain.repository.UserProfileRepository
import com.monetracka.shared.ui.home.HomeScreen
import com.monetracka.shared.ui.onboarding.OnboardingScreen
import org.koin.compose.koinInject

class RootScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val userProfileRepository = koinInject<UserProfileRepository>()

        LaunchedEffect(Unit) {
            val hasCompleted = userProfileRepository.hasCompletedOnboarding()
            if (hasCompleted) {
                navigator.replaceAll(HomeScreen())
            } else {
                navigator.replaceAll(OnboardingScreen())
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF060B11))
        )
    }
}
