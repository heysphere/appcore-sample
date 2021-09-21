package me.sphere.unicorn.notificationList

import androidx.compose.foundation.clickable
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import me.sphere.unicorn.ui.theme.MyTheme

@Composable
fun NotificationList(
    openNotificationDetails: (String) -> Unit
) {
    Surface(color = MaterialTheme.colors.background) {
        Text(
            text = "Notification List",
            modifier = Modifier.clickable {
                openNotificationDetails("3333")
            }
        )
    }
}

@Preview("Light Theme", widthDp = 360, heightDp = 640)
@Composable
fun LightPreview() {
    MyTheme {
        NotificationList {}
    }
}

@Preview("Dark Theme", widthDp = 360, heightDp = 640)
@Composable
fun DarkPreview() {
    MyTheme(darkTheme = true) {
        NotificationList{}
    }
}

