package com.alcolarm.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alcolarm.core.designsystem.theme.ClearSignalColors

@Composable
fun SignalChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        },
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = ClearSignalColors.SurfaceElevated,
            labelColor = ClearSignalColors.OnDark,
            selectedContainerColor = ClearSignalColors.Amber,
            selectedLabelColor = ClearSignalColors.NearBlack,
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) ClearSignalColors.Amber else ClearSignalColors.Outline,
        ),
    )
}
