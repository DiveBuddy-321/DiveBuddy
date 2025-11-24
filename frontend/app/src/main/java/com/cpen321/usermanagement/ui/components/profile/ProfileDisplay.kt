package com.cpen321.usermanagement.ui.components.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cpen321.usermanagement.R
import com.cpen321.usermanagement.data.remote.api.RetrofitClient
import com.cpen321.usermanagement.ui.components.DetailsRow
import com.cpen321.usermanagement.ui.theme.LocalSpacing

@Composable
fun ProfilePictureDisplay(
    profilePicture: String?,
    modifier: Modifier = Modifier
) {
    if (!profilePicture.isNullOrEmpty()) {
        AsyncImage(
            model = RetrofitClient.getPictureUri(profilePicture),
            contentDescription = stringResource(R.string.profile_picture),
            modifier = modifier.clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_account_circle),
                contentDescription = "Default profile picture",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(120.dp)
            )
        }
    }
}

@Composable
fun ProfileName(name: String) {
    val spacing = LocalSpacing.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.padding(bottom = spacing.medium)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun ProfileLocation(location: String) {
    val spacing = LocalSpacing.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.padding(bottom = spacing.medium)
    ) {
        Text(
            text = "📍 $location",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ProfileDetailsCard(
    age: Int?,
    skillLevel: String?,
    bio: String?
) {
    val spacing = LocalSpacing.current

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
            modifier = Modifier.padding(spacing.medium)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.extraLarge2)
            ) {
                if (age != null) {
                    DetailsRow(icon = "👤", label = "Age", value = "$age")
                }
                if (!skillLevel.isNullOrEmpty()) {
                    DetailsRow(icon = "🤿", label = "Level", value = skillLevel)
                }
            }
            if (!bio.isNullOrEmpty()) {
                DetailsRow(icon = "📖", label = "Bio", value = bio)
            }
        }
    }
}