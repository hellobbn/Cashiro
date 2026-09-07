package com.ritesh.cashiro.presentation.ui.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import coil3.compose.AsyncImage
import com.ritesh.cashiro.R
import com.ritesh.cashiro.presentation.ui.icons.Edit2
import com.ritesh.cashiro.presentation.ui.icons.Iconax
import com.ritesh.cashiro.presentation.ui.icons.Notification
import com.ritesh.cashiro.presentation.ui.icons.NotificationOutline

@Composable
fun GreetingCard(
    modifier: Modifier = Modifier,
    userName: String,
    profileImageUri: Uri?,
    profileBackgroundColor: Color,
    unreadUpdatesCount: Int,
    onProfileClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    onUpdatesClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile Image
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(profileBackgroundColor)
                .clickable(onClick = onProfileClick),
            contentAlignment = Alignment.Center
        ) {
            if (profileImageUri != null) {
                AsyncImage(
                    model = profileImageUri,
                    contentDescription = stringResource(R.string.profile_desc),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.avatar_1),
                    contentDescription = stringResource(R.string.profile_desc),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // User Info
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = userName,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            
            val greetingRes = remember {
                val hour = LocalTime.now().hour
                when (hour) {
                    in 5..11 -> R.string.greeting_morning
                    in 12..16 -> R.string.greeting_afternoon
                    in 17..21 -> R.string.greeting_evening
                    else -> R.string.greeting_night
                }
            }

            val locale = java.util.Locale.getDefault()
            val monthStatus = remember(locale) {
                val now = LocalDate.now()
                val lastDay = now.withDayOfMonth(now.lengthOfMonth())
                val daysLeft = ChronoUnit.DAYS.between(now, lastDay)
                val monthName = now.month.getDisplayName(java.time.format.TextStyle.FULL, locale)
                
                when {
                    daysLeft == 0L -> R.string.month_status_last_day to listOf(monthName)
                    daysLeft <= 7 -> R.plurals.month_status_days_left_format to listOf(daysLeft.toInt(), monthName)
                    else -> null
                }
            }

            val subtitleText = when {
                monthStatus != null -> {
                    val (resId, args) = monthStatus
                    if (resId == R.string.month_status_last_day) {
                        stringResource(resId, *args.toTypedArray())
                    } else {
                        pluralStringResource(resId, args[0] as Int, *args.toTypedArray())
                    }
                }
                else -> stringResource(greetingRes)
            }

            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            /* Priority Logic:
             1. If unread updates > 0 and (it's not the last day of month OR 50% chance)
             2. If monthStatus is available
             3. Greeting
             */

            /*
            * Planning to add a OTA update sheet and changelog
            * therefore commenting out
            */
//            val showUpdates = unreadUpdatesCount > 0 && (monthStatus == null || Math.random() > 0.5)
//
//            if (showUpdates) {
//                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                    modifier = Modifier.clickable(onClick = onUpdatesClick)
//                ) {
//                    Text(
//                        text = "$unreadUpdatesCount+ unread updates",
//                        style = MaterialTheme.typography.bodyMedium,
//                        color = Color(0xFF4285F4)
//                    )
//                    Spacer(modifier = Modifier.width(4.dp))
//                    Icon(
//                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
//                        contentDescription = null,
//                        tint = Color(0xFF4285F4),
//                        modifier = Modifier.size(16.dp)
//                    )
//                }
//            } else {
//                Text(
//                    text = monthStatus ?: greeting,
//                    style = MaterialTheme.typography.bodyMedium,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
//            }
        }

        // Material icon buttons provide an accessible 48dp touch target and ripple.
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNotificationClick) {
                Icon(
                    imageVector = Iconax.NotificationOutline,
                    contentDescription = stringResource(R.string.notification),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(onClick = onMoreClick) {
                Icon(
                    imageVector = Icons.Rounded.MoreHoriz,
                    contentDescription = stringResource(R.string.more_options),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
