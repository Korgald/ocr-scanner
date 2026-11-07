package ru.student.ocrscanner.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.student.ocrscanner.ui.theme.OcrGradients
import ru.student.ocrscanner.ui.theme.Rose100
import ru.student.ocrscanner.ui.theme.Wine900

private data class OnboardingPage(
    val title: String,
    val description: String
)

/** Onboarding on the hero gradient with decorative circles. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val pages = listOf(
        OnboardingPage(
            title = "Сканируйте конспекты",
            description = "Наведите камеру на конспект или шпаргалку - приложение снимет кадр для распознавания."
        ),
        OnboardingPage(
            title = "Распознавание без интернета",
            description = "OCR работает прямо на устройстве: кириллица и латиница, полностью офлайн."
        ),
        OnboardingPage(
            title = "Сохраняйте и редактируйте",
            description = "Текст можно править, копировать, делиться и сохранять в заметки с экспортом TXT и PDF."
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OcrGradients.hero)
            .systemBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .size(260.dp)
                .offset(x = (-70).dp, y = (-50).dp)
                .clip(CircleShape)
                .background(Rose100.copy(alpha = 0.10f))
        )
        Box(
            modifier = Modifier
                .size(340.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 90.dp, y = 130.dp)
                .clip(CircleShape)
                .background(Rose100.copy(alpha = 0.08f))
        )
        Box(
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.TopEnd)
                .offset(x = (-30).dp, y = 60.dp)
                .clip(CircleShape)
                .background(Wine900.copy(alpha = 0.25f))
        )

        Column(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = pages[page].title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Rose100,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = pages[page].description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Rose100.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pages.size) { index ->
                    val active = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(if (active) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (active) Rose100 else Rose100.copy(alpha = 0.35f))
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onComplete,
                    colors = ButtonDefaults.textButtonColors(contentColor = Rose100.copy(alpha = 0.8f))
                ) {
                    Text(text = "Пропустить")
                }
                if (pagerState.currentPage < pages.size - 1) {
                    Button(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Rose100,
                            contentColor = Wine900
                        )
                    ) {
                        Text(text = "Далее")
                    }
                } else {
                    Button(
                        onClick = onComplete,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Rose100,
                            contentColor = Wine900
                        )
                    ) {
                        Text(text = "Начать")
                    }
                }
            }
        }
    }
}