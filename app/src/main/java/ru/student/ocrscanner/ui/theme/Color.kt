package ru.student.ocrscanner.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Base wine/plum palette (first reference):
val Wine950 = Color(0xFF1D0A0C)
val Wine900 = Color(0xFF2E1114)
val Wine800 = Color(0xFF3A1417)
val Wine700 = Color(0xFF501B1D)
val Plum600 = Color(0xFF64485C)
val PlumOnLight = Color(0xFF4E3B41)
val Mauve500 = Color(0xFF83677B)
val Mauve300 = Color(0xFFD6BBC6)
val Rose100 = Color(0xFFF6ECEE)
val Rose200 = Color(0xFFE8D5DA)
val Rose300 = Color(0xFFD9C6CC)

// Second reference palette (FCINQ): pink / eggplant / indigo / coral / sage
val Egg900 = Color(0xFF2A1B3D)
val Egg800 = Color(0xFF3A2A55)
val Egg700 = Color(0xFF4A3B62)
val Egg600 = Color(0xFF6E5A8E)
val Ind600 = Color(0xFF44318D)
val Pink500 = Color(0xFFD83F87)
val Pink200 = Color(0xFFFBDCE9)
val Coral400 = Color(0xFFE98074)
val Coral600 = Color(0xFFC65A4E)
val Coral200 = Color(0xFFFFE3DE)
val Sage300 = Color(0xFFA4B3B6)
val Lilac100 = Color(0xFFF3EAF7)
val Lilac200 = Color(0xFFE4D9F5)
val Mist050 = Color(0xFFFAF7FB)
val Mist400 = Color(0xFFADADAD)
val Mist600 = Color(0xFF6E6E6E)
val Mist200 = Color(0xFFD9D9D9)
val Blush050 = Color(0xFFFAF6F7)

// Gradients: hero for onboarding, accent for shutter/fab/hero cards.
object OcrGradients {
    val hero = Brush.linearGradient(listOf(Egg900, Ind600, Pink500))
    val accent = Brush.linearGradient(listOf(Pink500, Ind600))
}