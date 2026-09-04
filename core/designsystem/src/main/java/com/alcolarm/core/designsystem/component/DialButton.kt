package com.alcolarm.core.designsystem.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alcolarm.core.designsystem.theme.ClearSignalColors

/** Large, stress-ready dial action for the alert screen. */
@Composable
fun DialButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = ClearSignalColors.Success,
            contentColor = ClearSignalColors.NearBlack,
        ),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 24.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Call,
            contentDescription = null,
            modifier = Modifier.size(36.dp),
        )
        Text(
            text = "  $label",
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp),
        )
    }
}
