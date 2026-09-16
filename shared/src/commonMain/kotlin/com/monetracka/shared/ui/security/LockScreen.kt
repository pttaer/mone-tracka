package com.monetracka.shared.ui.security

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.monetracka.shared.domain.repository.UserProfileRepository
import com.monetracka.shared.domain.security.BiometricAuthManager
import com.monetracka.shared.ui.home.HomeScreen
import com.monetracka.shared.ui.theme.MoneTrackaColors
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class LockScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val biometricAuthManager = koinInject<BiometricAuthManager>()
        val userProfileRepository = koinInject<UserProfileRepository>()
        val scope = rememberCoroutineScope()

        val profileState by userProfileRepository.getUserProfile().collectAsState(initial = null)
        val userName = profileState?.userName ?: "User"
        val initials = profileState?.initials ?: "U"

        var authFailed by remember { mutableStateOf(false) }
        var isAuthenticating by remember { mutableStateOf(false) }

        val triggerAuth: () -> Unit = {
            if (!isAuthenticating) {
                isAuthenticating = true
                authFailed = false
                scope.launch {
                    val success = biometricAuthManager.authenticate(
                        title = "Unlock MoneTracka",
                        subtitle = "Confirm your identity to access your finances"
                    )
                    isAuthenticating = false
                    if (success) {
                        navigator.replaceAll(HomeScreen())
                    } else {
                        authFailed = true
                    }
                }
            }
        }

        // Auto-trigger biometric prompt on first entry
        LaunchedEffect(Unit) {
            triggerAuth()
        }

        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MoneTrackaColors.BackgroundLight)
                .padding(horizontal = 28.dp, vertical = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Biometric Shield Glow
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(110.dp)
                        .scale(if (isAuthenticating) pulseScale else 1f)
                        .clip(CircleShape)
                        .background(MoneTrackaColors.MintLight)
                        .border(2.dp, MoneTrackaColors.MintPrimary.copy(alpha = 0.4f), CircleShape)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(MoneTrackaColors.MintPrimary)
                            .shadow(8.dp, CircleShape, spotColor = MoneTrackaColors.MintPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // User Initials Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(MoneTrackaColors.CardWhite)
                        .border(1.dp, MoneTrackaColors.ProgressTrack, RoundedCornerShape(999.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MoneTrackaColors.MintPrimary)
                    ) {
                        Text(
                            text = initials,
                            color = Color(0xFF051A12),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Text(
                        text = userName,
                        color = MoneTrackaColors.TextDark,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "MoneTracka is Locked",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MoneTrackaColors.TextDark,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (authFailed) {
                        "Authentication was cancelled or failed. Tap below to retry."
                    } else {
                        "Biometric security is active to protect your financial records."
                    },
                    fontSize = 13.sp,
                    color = if (authFailed) MoneTrackaColors.CoralDanger else MoneTrackaColors.TextGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Tactile Unlock Button
                Button(
                    onClick = triggerAuth,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MoneTrackaColors.MintPrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = MoneTrackaColors.MintPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Fingerprint",
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isAuthenticating) "Authenticating..." else "Unlock with Biometrics",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
