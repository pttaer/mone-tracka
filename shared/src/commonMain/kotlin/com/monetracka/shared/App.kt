package com.monetracka.shared

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.monetracka.shared.ui.home.HomeScreen
import com.monetracka.shared.ui.theme.MoneTrackaTheme

@Composable
fun App() {
    MoneTrackaTheme {
        Navigator(HomeScreen()) { navigator ->
            SlideTransition(navigator)
        }
    }
}
