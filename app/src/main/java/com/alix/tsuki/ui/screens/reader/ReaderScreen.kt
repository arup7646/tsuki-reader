package com.alix.tsuki.ui.screens.reader

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import com.alix.tsuki.data.model.ReaderPage
import com.alix.tsuki.data.model.ReadingDirection
import com.alix.tsuki.ui.screens.reader.components.ReaderBottomBar
import com.alix.tsuki.ui.screens.reader.components.ReaderTopBar
import com.alix.tsuki.ui.screens.reader.components.ZoomableBox
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val manga by viewModel.manga.collectAsState()
    val currentChapter by viewModel.currentChapter.collectAsState()
    val pages by viewModel.pages.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val readingDirection by viewModel.readingDirection.collectAsState()

    var showControls by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Immersive fullscreen mode
    DisposableEffect(showControls) {
        val window = (context as? Activity)?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            if (showControls) {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            } else {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            val win = (context as? Activity)?.window
            if (win != null) {
                val controller = WindowCompat.getInsetsController(win, win.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            pages.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No pages found in comic",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            else -> {
                if (readingDirection == ReadingDirection.VERTICAL) {
                    // Vertical continuous scroll (Webtoon mode)
                    val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentPage)

                    LaunchedEffect(listState) {
                        snapshotFlow { listState.firstVisibleItemIndex }
                            .collect { index ->
                                viewModel.onPageChanged(index)
                            }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { showControls = !showControls }
                    ) {
                        itemsIndexed(pages, key = { _, page -> page.index }) { _, page ->
                            PageItemView(
                                page = page,
                                viewModel = viewModel,
                                isVertical = true,
                                onCenterTap = { showControls = !showControls }
                            )
                        }
                    }
                } else {
                    // Horizontal swipe mode (LTR or RTL)
                    val isRtl = readingDirection == ReadingDirection.RTL
                    val pagerState = rememberPagerState(
                        initialPage = currentPage.coerceIn(0, pages.size - 1),
                        pageCount = { pages.size }
                    )

                    LaunchedEffect(pagerState) {
                        snapshotFlow { pagerState.currentPage }
                            .collect { pageIndex ->
                                viewModel.onPageChanged(pageIndex)
                            }
                    }

                    HorizontalPager(
                        state = pagerState,
                        reverseLayout = isRtl,
                        modifier = Modifier.fillMaxSize()
                    ) { pageIdx ->
                        val page = pages[pageIdx]
                        ZoomableBox(
                            modifier = Modifier.fillMaxSize(),
                            onTapLeft = {
                                scope.launch {
                                    val target = if (isRtl) pagerState.currentPage + 1 else pagerState.currentPage - 1
                                    if (target in 0 until pages.size) {
                                        pagerState.animateScrollToPage(target)
                                    }
                                }
                            },
                            onTapRight = {
                                scope.launch {
                                    val target = if (isRtl) pagerState.currentPage - 1 else pagerState.currentPage + 1
                                    if (target in 0 until pages.size) {
                                        pagerState.animateScrollToPage(target)
                                    }
                                }
                            },
                            onTapCenter = {
                                showControls = !showControls
                            }
                        ) {
                            PageItemView(
                                page = page,
                                viewModel = viewModel,
                                isVertical = false,
                                onCenterTap = { showControls = !showControls }
                            )
                        }
                    }
                }
            }
        }

        // Top overlay bar
        val readerTitle = when {
            manga != null && currentChapter != null -> "${manga?.title}: ${currentChapter?.title}"
            currentChapter != null -> currentChapter?.title ?: "Tsuki Reader"
            else -> manga?.title ?: "Tsuki Reader"
        }

        ReaderTopBar(
            visible = showControls,
            title = readerTitle,
            currentPage = currentPage,
            totalPages = pages.size,
            onBackClick = onBackClick,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Bottom overlay bar
        ReaderBottomBar(
            visible = showControls,
            currentPage = currentPage,
            totalPages = pages.size,
            readingDirection = readingDirection,
            onPageSelected = { selectedPage ->
                viewModel.onPageChanged(selectedPage)
            },
            onDirectionChange = { newDirection ->
                viewModel.setReadingDirection(newDirection)
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun PageItemView(
    page: ReaderPage,
    viewModel: ReaderViewModel,
    isVertical: Boolean,
    onCenterTap: () -> Unit
) {
    val pageFile by produceState<File?>(initialValue = null, key1 = page.index) {
        value = viewModel.getPageFile(page)
    }

    Box(
        modifier = if (isVertical) {
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onCenterTap)
        } else {
            Modifier.fillMaxSize()
        },
        contentAlignment = Alignment.Center
    ) {
        if (pageFile != null && pageFile?.exists() == true) {
            AsyncImage(
                model = pageFile,
                contentDescription = page.displayName,
                contentScale = if (isVertical) ContentScale.FillWidth else ContentScale.Fit,
                modifier = if (isVertical) Modifier.fillMaxWidth() else Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 64.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}
