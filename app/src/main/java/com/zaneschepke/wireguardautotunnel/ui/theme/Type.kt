package com.zaneschepke.wireguardautotunnel.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.zaneschepke.wireguardautotunnel.R

// Sizes follow the panel's scale (11/12/13/14/16/20 with compact line heights),
// so list rows and settings match the site's density.

val inter = FontFamily(Font(R.font.inter))

val Typography =
    Typography(
        bodyLarge =
            TextStyle(
                fontFamily = inter,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.2.sp,
            ),
        bodySmall =
            TextStyle(
                fontFamily = inter,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.2.sp,
                color = LightGrey,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = inter,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight(400),
                letterSpacing = 0.sp,
            ),
        labelLarge =
            TextStyle(
                fontFamily = inter,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.sp,
            ),
        labelMedium =
            TextStyle(
                fontFamily = inter,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = inter,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                letterSpacing = 0.sp,
            ),
    )
