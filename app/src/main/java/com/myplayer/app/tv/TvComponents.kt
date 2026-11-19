package com.myplayer.app.tv

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.myplayer.app.ui.common.AlbumArtImage

val TvAccentPurple = Color(0xFF7C3AED)
val TvAccentPink = Color(0xFFEC4899)
val TvAccentGreen = Color(0xFF10B981)
val TvAccentBlue = Color(0xFF3B82F6)
val TvAccentAmber = Color(0xFFF59E0B)
val TvCardBg = Color(0xFF161624)
val TvCardFocusedBg = Color(0xFF2B2146)

/**
 * 10-foot D-Pad Focusable Surface for Android TV / Google TV / Fire TV.
 * Scales smoothly on focus with a high-contrast white ring so remote position is always unmistakable.
 */
@Composable
fun TvFocusableSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    focusedScale: Float = 1.05f,
    containerColor: Color = TvCardBg,
    focusedContainerColor: Color = TvCardFocusedBg,
    showFocusRing: Boolean = true,
    onDpadLeft: (() -> Unit)? = null,
    onDpadRight: (() -> Unit)? = null,
    content: @Composable BoxScope.(isFocused: Boolean) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val highlighted = isFocused || isHovered

    val scale by animateFloatAsState(
        targetValue = if (highlighted) focusedScale else 1f,
        animationSpec = tween(130),
        label = "tvScale"
    )
    val bg by animateColorAsState(
        targetValue = if (highlighted) focusedContainerColor else containerColor,
        animationSpec = tween(130),
        label = "tvBg"
    )

    val keyModifier = if (onDpadLeft != null || onDpadRight != null) {
        Modifier.onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown) {
                when (event.key) {
                    Key.DirectionLeft -> {
                        onDpadLeft?.invoke()
                        onDpadLeft != null
                    }
                    Key.DirectionRight -> {
                        onDpadRight?.invoke()
                        onDpadRight != null
                    }
                    else -> false
                }
            } else {
                false
            }
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .zIndex(if (highlighted) 2f else 0f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .onFocusChanged { isFocused = it.isFocused || it.hasFocus }
            .then(keyModifier)
            .clip(shape)
            .background(bg)
            .then(
                if (highlighted && showFocusRing) {
                    Modifier.border(2.5.dp, Color.White, shape)
                } else {
                    Modifier.border(1.dp, Color.White.copy(alpha = 0.09f), shape)
                }
            )
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            ),
        content = { content(highlighted) }
    )
}

@Composable
fun TvSectionHeader(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.62f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun TvPosterCard(
    title: String,
    subtitle: String,
    artworkUrl: String?,
    onClick: () -> Unit,
    badge: String? = null,
    width: Dp = 176.dp,
    isActive: Boolean = false
) {
    TvFocusableSurface(
        onClick = onClick,
        modifier = Modifier.width(width),
        shape = RoundedCornerShape(16.dp),
        containerColor = if (isActive) TvAccentPurple.copy(alpha = 0.25f) else TvCardBg,
        focusedContainerColor = TvCardFocusedBg
    ) { focused ->
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF232338))
            ) {
                AlbumArtImage(
                    uri = artworkUrl,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize()
                )
                if (!badge.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.72f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = badge,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (focused || isActive) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (focused) TvAccentPurple else TvAccentGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isActive) Icons.Rounded.GraphicEq else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TvCompactTrackRow(
    title: String,
    subtitle: String,
    artworkUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    indexBadge: String? = null,
    isPlaying: Boolean = false,
    trailingAction: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TvFocusableSurface(
            onClick = onClick,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            focusedScale = 1.02f,
            containerColor = if (isPlaying) TvAccentPurple.copy(alpha = 0.25f) else TvCardBg,
            focusedContainerColor = TvCardFocusedBg
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!indexBadge.isNullOrBlank()) {
                    Text(
                        text = indexBadge,
                        color = if (isPlaying) Color(0xFFC4B5FD) else Color.White.copy(alpha = 0.55f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.width(30.dp)
                    )
                }
                AlbumArtImage(
                    uri = artworkUrl,
                    contentDescription = title,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = if (isPlaying) Color(0xFFE9D5FF) else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.GraphicEq else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = if (isPlaying) TvAccentGreen else Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        trailingAction?.invoke()
    }
}

@Composable
fun TvActionChip(
    label: String,
    icon: ImageVector? = null,
    selected: Boolean = false,
    accentColor: Color = TvAccentPurple,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TvFocusableSurface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(50),
        focusedScale = 1.06f,
        containerColor = if (selected) accentColor.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f),
        focusedContainerColor = accentColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = label,
                color = Color.White,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun TvToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    icon: ImageVector,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    TvFocusableSurface(
        onClick = onToggle,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        focusedScale = 1.02f,
        containerColor = if (checked) TvAccentPurple.copy(alpha = 0.22f) else TvCardBg,
        focusedContainerColor = TvCardFocusedBg
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (checked) TvAccentPurple else Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    subtitle,
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (checked) TvAccentGreen else Color.White.copy(alpha = 0.14f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (checked) "ON" else "OFF",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * D-Pad friendly stepper card: press Center/OK to cycle next, or press D-Pad Left / Right while focused!
 */
@Composable
fun TvStepperCard(
    title: String,
    subtitle: String,
    valueText: String,
    icon: ImageVector,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    TvFocusableSurface(
        onClick = onNext,
        onDpadLeft = onPrevious,
        onDpadRight = onNext,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        focusedScale = 1.02f,
        containerColor = TvCardBg,
        focusedContainerColor = TvCardFocusedBg
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(TvAccentBlue.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    subtitle,
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Rounded.ChevronLeft, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(valueText, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

fun formatTvTime(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    return "%d:%02d".format(mins, secs)
}
