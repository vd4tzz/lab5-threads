package com.example.threadslite.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.threadslite.data.model.User

@Composable
fun UserListItem(
    user: User,
    onClick: (User) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(user) }
            .padding(horizontal = 0.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarImage(
            avatarUrl = user.avatarUrl,
            username  = user.username,
            size      = 46
        )

        Spacer(Modifier.width(14.dp))

        Column(
            modifier            = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text       = "@${user.username.ifBlank { "unknown" }}",
                fontSize   = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color      = Color.Black,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
            if (user.email.isNotBlank()) {
                Text(
                    text     = user.email,
                    fontSize = 12.sp,
                    color    = Color(0xFFAAAAAA),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Text(
            text     = "›",
            fontSize = 20.sp,
            color    = Color(0xFFCCCCCC),
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
fun AvatarImage(
    avatarUrl: String,
    username: String,
    size: Int = 44,
    modifier: Modifier = Modifier
) {
    val sizeDp = size.dp
    if (avatarUrl.isNotBlank()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(avatarUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Avatar of $username",
            contentScale       = ContentScale.Crop,
            modifier           = modifier
                .size(sizeDp)
                .clip(CircleShape)
                .background(Color(0xFFEEEEEE), CircleShape)
        )
    } else {
        val initial = username.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        Box(
            modifier         = modifier
                .size(sizeDp)
                .clip(CircleShape)
                .background(Color(0xFF1A1A1A)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = initial,
                fontSize   = (size / 2.5).sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White
            )
        }
    }
}
