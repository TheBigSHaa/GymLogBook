package com.ironlog.app.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ironlog.app.ui.components.foundation.AnimatedStatText
import com.ironlog.app.ui.components.foundation.SurfaceCard

@Composable
fun StatTile(
    label: String,
    value: String,
    sublabel: String,
    modifier: Modifier = Modifier,
) {
    SurfaceCard(
        modifier = modifier.aspectRatio(1f),
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentPadding = PaddingValues(16.dp),
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        AnimatedStatText(
            text = value,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 26.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = sublabel.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 2.sp,
                fontSize = 9.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
