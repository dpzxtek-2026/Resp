package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

// Shortcut item representation
data class ShortcutItem(
    val name: String,
    val url: String,
    val iconLetter: String,
    val bgColor: Color
)

@Composable
fun HomeScreen(
    onSearchFocused: () -> Unit,
    onNavigateToUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // 4 High-Octane Cyberpunk-Themed Shortcuts (NASA & unused ones are removed)
    val shortcuts = listOf(
        ShortcutItem("Google", "https://www.google.com", "G", Color(0xFF4285F4)),
        ShortcutItem("YouTube", "https://www.youtube.com", "Y", Color(0xFFEA4335)),
        ShortcutItem("GitHub", "https://github.com", "G", NeonCyan),
        ShortcutItem("Reddit", "https://www.reddit.com", "R", NeonPink)
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack),
        contentPadding = PaddingValues(bottom = 32.dp, top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Pulsing Lightning Bolt Brand Logo Header
        item {
            Spacer(modifier = Modifier.height(48.dp))
            CyberBoltLogo(modifier = Modifier.size(110.dp))
            Spacer(modifier = Modifier.height(20.dp))
            
            // "dpzxbrowse!" neon brand title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "dpzx",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = NeonPink,
                        fontSize = 38.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = "browse!",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = NeonCyan,
                        fontSize = 38.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        // 2. High-Performance Glowing Omnibox / Search Pill
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(54.dp)
                    .clip(RoundedCornerShape(27.dp))
                    .background(CyberSurfaceVariant)
                    .border(
                        1.5.dp,
                        Brush.horizontalGradient(listOf(NeonPink, NeonCyan)),
                        RoundedCornerShape(27.dp)
                    )
                    .clickable { onSearchFocused() }
                    .padding(horizontal = 20.dp)
                    .testTag("home_search_bar"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Search or type URL",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    ),
                    color = CyberMuted,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Search",
                    tint = NeonPink,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(40.dp))
        }

        // 3. Compact row of exactly 4 shortcuts (NASA and extra menus are removed)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (item in shortcuts) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(80.dp)
                            .clickable { onNavigateToUrl(item.url) }
                            .padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(item.bgColor.copy(alpha = 0.15f))
                                .border(1.5.dp, item.bgColor.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.iconLetter,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = item.bgColor
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            ),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = GlowingWhite
                        )
                    }
                }
            }
        }
    }
}

// Custom pulsing lightning bolt logo inside browser home screen
@Composable
fun CyberBoltLogo(modifier: Modifier = Modifier) {
    // Elegant pulsing scaling factor for neon aesthetic
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .border(
                width = 2.dp,
                brush = Brush.verticalGradient(listOf(NeonPink, NeonCyan)),
                shape = CircleShape
            )
            .background(CyberSurface, CircleShape)
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Bolt,
            contentDescription = "dpzxbrowse Lightning Logo",
            tint = NeonLime,
            modifier = Modifier
                .fillMaxSize()
                .size(60.dp)
                .aspectRatio(1f)
        )
    }
}
