package com.example.triprescue.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.example.triprescue.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage ="com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val fontName1 = GoogleFont("Cookie") //naslov, upper bar
val fontName2 = GoogleFont("Plus Jakarta Sans") //ostatak

val fontFamily1 = FontFamily(
    Font(
        googleFont = fontName1,
        fontProvider = provider
    )
)

val fontFamily2 = FontFamily(
    Font(
        googleFont =fontName2,
        fontProvider = provider
    )
)

val Typography = Typography(

    titleLarge = TextStyle(
        fontFamily = fontFamily2,
        fontWeight = FontWeight.Normal,
        fontSize = 30.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp
    ),

    headlineLarge = TextStyle(
        fontFamily = fontFamily1,
        fontWeight = FontWeight.Bold,
        fontSize = 50.sp,
        lineHeight = 40.sp,
        letterSpacing = 3.sp),

    bodyLarge = TextStyle(
        fontFamily = fontFamily2,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = fontFamily2,
        fontSize = 25.sp,
        lineHeight = 35.sp
    ),

    labelSmall = TextStyle(
        fontFamily = fontFamily2,
        fontSize = 20.sp,
        lineHeight = 25.sp
    )
)