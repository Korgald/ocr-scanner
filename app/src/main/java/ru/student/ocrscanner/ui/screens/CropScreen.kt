package ru.student.ocrscanner.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.student.ocrscanner.ui.crop.CropViewModel
import ru.student.ocrscanner.ui.crop.NormRect
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private enum class Corner { TL, TR, BL, BR }

// Gesture modes resolved by a single router on top of the whole crop surface.
private enum class Mode { NONE, FRAME, BTN_CANCEL, BTN_CONFIRM }
private data class HandleMode(val corner: Corner)

// Crop screen. ONE gesture router (pointerInput(Unit), all live values via
// rememberUpdatedState so a drag is never interrupted by recomposition).
// Priority on touch-down: handle > button > frame > none. Buttons are
// visual-only (no clickable) and fade out while a handle/frame drag is active
// (9.8 behavior), but still fire on a clean tap because the router dispatches
// them on release-without-move. Rect + bitmap come from the persistent VM
// (9.8), so state survives rotation / minimize / MIUI background kill and is
// cleared only on swipe-from-recents.
@Composable
fun CropScreen(
    onNavigateToResult: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: CropViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val source by viewModel.bitmap.collectAsStateWithLifecycle()
    val nr by viewModel.rect.collectAsStateWithLifecycle()

    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var boxRootPos by remember { mutableStateOf(Offset.Zero) }
    var cancelBounds by remember { mutableStateOf(Rect.Zero) }
    var confirmBounds by remember { mutableStateOf(Rect.Zero) }
    var isDragging by remember { mutableStateOf(false) }

    // Live snapshots for the router coroutine (stable state objects).
    val currentNr by rememberUpdatedState(nr)
    val currentBox by rememberUpdatedState(boxSize)
    val currentBoxRoot by rememberUpdatedState(boxRootPos)
    val currentCancel by rememberUpdatedState(cancelBounds)
    val currentConfirm by rememberUpdatedState(confirmBounds)
    val currentSource by rememberUpdatedState(source)

    val btnAlpha by animateFloatAsState(
        targetValue = if (isDragging) 0f else 1f,
        animationSpec = tween(120),
        label = "btnAlpha"
    )

    val density = LocalDensity.current
    val minPx = with(density) { 40.dp.toPx() }
    val halfTouchPx = with(density) { (48.dp / 2).toPx() }
    val visual = 14.dp

    val borderColor = MaterialTheme.colorScheme.primary
    val scrimColor = Color.Black.copy(alpha = 0.45f)
    val btnCancelColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val btnConfirmColor = MaterialTheme.colorScheme.primary
    val btnConfirmText = MaterialTheme.colorScheme.onPrimary
    val btnCancelText = MaterialTheme.colorScheme.onSurface

    LaunchedEffect(boxSize, source) {
        if (nr == null && boxSize.width > 0 && boxSize.height > 0 && source != null) {
            viewModel.setRect(NormRect(left = 0.04f, top = 0.10f, right = 0.96f, bottom = 0.93f))
        }
    }
    LaunchedEffect(uiState.done) { if (uiState.done) onNavigateToResult() }

    // Confirm: normalized rect -> source pixels via fit math, then crop+ocr.
    fun doConfirm() {
        val cur = viewModel.rect.value ?: return
        val src = viewModel.bitmap.value ?: return
        val bw = boxSize.width.toFloat()
        val bh = boxSize.height.toFloat()
        if (bw <= 0f || bh <= 0f) return
        val scale = min(bw / src.width, bh / src.height)
        val offX = (bw - src.width * scale) / 2f
        val offY = (bh - src.height * scale) / 2f
        val cLeft = cur.left * bw; val cTop = cur.top * bh
        val cRight = cur.right * bw; val cBottom = cur.bottom * bh
        val pxL = (((cLeft - offX) / scale).roundToInt()).coerceIn(0, src.width - 1)
        val pxT = (((cTop - offY) / scale).roundToInt()).coerceIn(0, src.height - 1)
        val pxR = (((cRight - offX) / scale).roundToInt()).coerceIn(pxL + 1, src.width)
        val pxB = (((cBottom - offY) / scale).roundToInt()).coerceIn(pxT + 1, src.height)
        viewModel.confirmCropSource(IntRect(pxL, pxT, pxR, pxB))
    }
    fun doCancel() { viewModel.cancel(); onCancel() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { boxSize = it }
            .onGloballyPositioned { boxRootPos = it.positionInRoot() }
    ) {
        // 1) Source image.
        currentSource?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Crop source",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        val r = nr
        val bw = boxSize.width.toFloat()
        val bh = boxSize.height.toFloat()

        // 2) Scrim outside the rect.
        if (r != null && bw > 0f && bh > 0f) {
            val lPx = r.left * bw; val tPx = r.top * bh
            val rrPx = r.right * bw; val bPx = r.bottom * bh
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(color = scrimColor, topLeft = Offset.Zero, size = Size(size.width, tPx))
                drawRect(color = scrimColor, topLeft = Offset(0f, bPx), size = Size(size.width, size.height - bPx))
                drawRect(color = scrimColor, topLeft = Offset(0f, tPx), size = Size(lPx, bPx - tPx))
                drawRect(color = scrimColor, topLeft = Offset(rrPx, tPx), size = Size(size.width - rrPx, bPx - tPx))
            }
        }

        // 3) Frame outline (visual only).
        if (r != null && bw > 0f && bh > 0f) {
            val lPx = r.left * bw; val tPx = r.top * bh
            val frameW = with(density) { ((r.right - r.left) * bw).toDp() }
            val frameH = with(density) { ((r.bottom - r.top) * bh).toDp() }
            Box(
                modifier = Modifier
                    .offset { IntOffset(lPx.roundToInt(), tPx.roundToInt()) }
                    .size(frameW, frameH)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(color = borderColor, style = Stroke(width = 3f))
                }
            }
            // 4) Corner handle squares (visual only).
            HandleSquare(Corner.TL, r, bw, bh, visual)
            HandleSquare(Corner.TR, r, bw, bh, visual)
            HandleSquare(Corner.BL, r, bw, bh, visual)
            HandleSquare(Corner.BR, r, bw, bh, visual)
        }

        // 5) Action buttons: visual only, bounds reported to router, fade while dragging.
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .graphicsLayer { alpha = btnAlpha },
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .background(btnCancelColor, RoundedCornerShape(28.dp))
                    .onGloballyPositioned { cancelBounds = it.boundsInRoot() },
                contentAlignment = Alignment.Center
            ) { Text("Отмена", color = btnCancelText) }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .background(btnConfirmColor, RoundedCornerShape(28.dp))
                    .onGloballyPositioned { confirmBounds = it.boundsInRoot() },
                contentAlignment = Alignment.Center
            ) { Text("Распознать", color = btnConfirmText) }
        }

        // 6) THE ROUTER: transparent, single pointerInput(Unit), priority
        // handle > button > frame > none. Drag never restarts on recomposition.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    val slop = viewConfiguration.touchSlop
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val local0 = down.position
                        val box = currentBox
                        val bwf = box.width.toFloat()
                        val bhf = box.height.toFloat()
                        val start = currentNr
                        val rootOff = currentBoxRoot
                        val rootPt = Offset(local0.x + rootOff.x, local0.y + rootOff.y)

                        var mode: Any = Mode.NONE
                        var moved = false
                        if (start != null && bwf > 0f && bhf > 0f) {
                            val h = hitHandle(local0, start, bwf, bhf, halfTouchPx)
                            if (h != null) {
                                mode = HandleMode(h)
                            } else if (currentCancel.contains(rootPt)) {
                                mode = Mode.BTN_CANCEL
                            } else if (currentConfirm.contains(rootPt)) {
                                mode = Mode.BTN_CONFIRM
                            } else if (hitFrame(local0, start, bwf, bhf)) {
                                mode = Mode.FRAME
                            }
                        }

                        var prev = local0
                        var keepGoing = true
                        while (keepGoing) {
                            val event = awaitPointerEvent()
                            val ch = event.changes.firstOrNull { it.id == down.id } ?: continue
                            if (ch.pressed) {
                                val pos = ch.position
                                if (!moved && abs(pos.x - local0.x) + abs(pos.y - local0.y) > slop) {
                                    moved = true
                                    if (mode is HandleMode || mode == Mode.FRAME) isDragging = true
                                }
                                when (val m = mode) {
                                    is HandleMode -> {
                                        val newRect = resizeHandle(m.corner, start!!, pos, bwf, bhf, minPx)
                                        viewModel.setRect(newRect)
                                        ch.consume()
                                    }
                                    Mode.FRAME -> {
                                        val newRect = moveFrame(start!!, pos, prev, bwf, bhf)
                                        viewModel.setRect(newRect)
                                        ch.consume()
                                    }
                                    else -> { /* buttons/none: do not consume */ }
                                }
                                prev = pos
                            } else {
                                // Release.
                                if (mode is HandleMode || mode == Mode.FRAME) isDragging = false
                                if (!moved) {
                                    when (mode) {
                                        Mode.BTN_CANCEL -> doCancel()
                                        Mode.BTN_CONFIRM -> doConfirm()
                                        else -> {}
                                    }
                                }
                                keepGoing = false
                            }
                        }
                    }
                }
        )

        // 7) Hint / progress / error on top.
        Text(
            text = "Выделите область текста",
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )

        if (uiState.isProcessing) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        uiState.errorMessage?.let { msg ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = msg, style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = viewModel::clearError) { Text("Закрыть") }
                }
            }
        }
    }
}

// Small visible corner square at the normalized corner position.
@Composable
private fun HandleSquare(corner: Corner, rect: NormRect, bw: Float, bh: Float, sizeDp: Dp) {
    val cx = when (corner) { Corner.TL, Corner.BL -> rect.left; else -> rect.right }
    val cy = when (corner) { Corner.TL, Corner.TR -> rect.top; else -> rect.bottom }
    val px = cx * bw; val py = cy * bh
    val half = with(LocalDensity.current) { (sizeDp / 2).toPx() }
    Box(
        modifier = Modifier
            .offset { IntOffset((px - half).roundToInt(), (py - half).roundToInt()) }
            .size(sizeDp)
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp))
    )
}

// Which corner (if any) is under the local point, within the 48dp touch box.
private fun hitHandle(local: Offset, rect: NormRect, bw: Float, bh: Float, halfTouch: Float): Corner? {
    val corners = listOf(
        Corner.TL to Offset(rect.left * bw, rect.top * bh),
        Corner.TR to Offset(rect.right * bw, rect.top * bh),
        Corner.BL to Offset(rect.left * bw, rect.bottom * bh),
        Corner.BR to Offset(rect.right * bw, rect.bottom * bh)
    )
    for ((c, p) in corners) {
        if (abs(local.x - p.x) <= halfTouch && abs(local.y - p.y) <= halfTouch) return c
    }
    return null
}

private fun hitFrame(local: Offset, rect: NormRect, bw: Float, bh: Float): Boolean {
    val l = rect.left * bw; val t = rect.top * bh
    val r = rect.right * bw; val b = rect.bottom * bh
    return local.x in l..r && local.y in t..b
}

// Resize by dragging one corner; opposite edges pinned from startRect.
private fun resizeHandle(corner: Corner, start: NormRect, pos: Offset, bw: Float, bh: Float, minPx: Float): NormRect {
    val minNW = if (bw > 0f) minPx / bw else 0.05f
    val minNH = if (bh > 0f) minPx / bh else 0.05f
    val nx = (pos.x / bw).coerceIn(0f, 1f)
    val ny = (pos.y / bh).coerceIn(0f, 1f)
    return when (corner) {
        Corner.TL -> NormRect(
            left = nx.coerceIn(0f, max(0f, start.right - minNW)),
            top = ny.coerceIn(0f, max(0f, start.bottom - minNH)),
            right = start.right, bottom = start.bottom
        )
        Corner.TR -> NormRect(
            left = start.left,
            top = ny.coerceIn(0f, max(0f, start.bottom - minNH)),
            right = nx.coerceIn(min(1f, start.left + minNW), 1f),
            bottom = start.bottom
        )
        Corner.BL -> NormRect(
            left = nx.coerceIn(0f, max(0f, start.right - minNW)),
            top = start.top, right = start.right,
            bottom = ny.coerceIn(min(1f, start.top + minNH), 1f)
        )
        Corner.BR -> NormRect(
            left = start.left, top = start.top,
            right = nx.coerceIn(min(1f, start.left + minNW), 1f),
            bottom = ny.coerceIn(min(1f, start.top + minNH), 1f)
        )
    }
}

// Move whole frame by delta between consecutive positions.
private fun moveFrame(start: NormRect, pos: Offset, prev: Offset, bw: Float, bh: Float): NormRect {
    if (bw <= 0f || bh <= 0f) return start
    val dxN = (pos.x - prev.x) / bw
    val dyN = (pos.y - prev.y) / bh
    val wN = start.right - start.left
    val hN = start.bottom - start.top
    val nl = (start.left + dxN).coerceIn(0f, max(0f, 1f - wN))
    val nt = (start.top + dyN).coerceIn(0f, max(0f, 1f - hN))
    return NormRect(nl, nt, nl + wN, nt + hN)
}