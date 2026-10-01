package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Compact horizontal rounded rectangular swipe-to-stop control.
 *
 * Target layout:
 * ┌─────────────────────────────────────────────┐
 * │  [  >>  ]                  Stop Ringing      │
 * └─────────────────────────────────────────────┘
 *
 * Contains:
 * - A small dark/contrasting rounded slider handle on the LEFT with double chevron ">>"
 * - Text on the RIGHT: "Stop Ringing"
 * - Dragging the slider handle from left to right past the threshold triggers onSwipeComplete.
 * - Accidental taps or incomplete drags spring back to the start without stopping.
 */
@Composable
fun SwipeToStopSlider(
    onSwipeComplete: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Stop Ringing"
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val trackHeight = 58.dp
    val handleWidth = 60.dp
    val handleHeight = 48.dp
    val trackPadding = 5.dp
    val cornerRadius = 29.dp

    val handleWidthPx = with(density) { handleWidth.toPx() }
    val trackPaddingPx = with(density) { trackPadding.toPx() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(trackHeight)
            .shadow(elevation = 3.dp, shape = RoundedCornerShape(cornerRadius))
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color(0xFFE2E8F0)) // Clean contrasting light container
            .border(
                width = 1.dp,
                color = Color(0xFFCBD5E1),
                shape = RoundedCornerShape(cornerRadius)
            )
            .semantics {
                role = Role.Button
                contentDescription = "Swipe slider to stop alarm ringing"
            }
            .testTag("swipe_to_stop_slider")
    ) {
        val totalWidthPx = with(density) { maxWidth.toPx() }
        val maxDragPx = (totalWidthPx - handleWidthPx - (trackPaddingPx * 2)).coerceAtLeast(0f)
        val offsetX = remember { Animatable(0f) }

        val progress = if (maxDragPx > 0) (offsetX.value / maxDragPx).coerceIn(0f, 1f) else 0f

        // Progress fill behind handle
        if (progress > 0.05f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(with(density) { (offsetX.value + handleWidthPx + trackPaddingPx).toDp() })
                    .background(Color(0xFFDC2626).copy(alpha = 0.15f * progress))
            )
        }

        // Text on the RIGHT: "Stop Ringing"
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 28.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = label,
                color = Color(0xFF1E293B), // Dark legible slate
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.alpha((1f - (progress * 1.5f)).coerceIn(0f, 1f))
            )
        }

        // Draggable Handle on the LEFT with double-chevron ">>"
        Box(
            modifier = Modifier
                .padding(start = trackPadding, top = trackPadding, bottom = trackPadding)
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .size(width = handleWidth, height = handleHeight)
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(22.dp))
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF0F172A)) // Dark contrasting rounded handle
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        coroutineScope.launch {
                            val target = (offsetX.value + delta).coerceIn(0f, maxDragPx)
                            offsetX.snapTo(target)
                        }
                    },
                    onDragStopped = {
                        val completionThreshold = maxDragPx * 0.80f
                        if (offsetX.value >= completionThreshold) {
                            coroutineScope.launch {
                                offsetX.animateTo(maxDragPx, tween(120))
                                onSwipeComplete()
                            }
                        } else {
                            // Spring back to start — tapping or small drag does NOT stop!
                            coroutineScope.launch {
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                            }
                        }
                    }
                )
                .testTag("swipe_thumb"),
            contentAlignment = Alignment.Center
        ) {
            // Double-chevron ">>" inside the handle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Double chevron right",
                    tint = Color.White,
                    modifier = Modifier
                        .size(20.dp)
                        .offset(x = (-9).dp)
                )
            }
        }
    }
}
