package com.doitsh.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.doitsh.app.ui.theme.SpacingTokens

data class NavBarItem(
    val label: String,
    val icon: ImageVector
)

/**
 * M3 Expressive floating bottom bar:
 * - Left: a pill (capsule) container with icon-only nav buttons
 * - Right: a separate rounded-square action button
 * The entire group is horizontally centered on screen.
 */
@Composable
fun FloatingBottomBar(
    navItems: List<NavBarItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(bottom = SpacingTokens.Small),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: pill-shaped nav container
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainer,
                shadowElevation = 4.dp,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .height(56.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    navItems.forEachIndexed { index, item ->
                        PillIcon(
                            icon = item.icon,
                            selected = index == selectedIndex,
                            onClick = { onItemSelected(index) }
                        )
                    }
                }
            }

            // Right: standalone rounded-square action button, larger than the
            // capsule (large-FAB scale) and vertically centered on its axis
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 4.dp,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                IconButton(
                    onClick = onAddClick,
                    modifier = Modifier.size(96.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PillIcon(
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(52.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}

// Default nav items: Today, Projects, Inbox, More
val DefaultNavItems = listOf(
    NavBarItem(label = "Today", icon = Icons.Filled.Today),
    NavBarItem(label = "Projects", icon = Icons.Filled.Folder),
    NavBarItem(label = "Inbox", icon = Icons.Filled.Inbox),
    NavBarItem(label = "More", icon = Icons.Filled.MoreHoriz)
)
