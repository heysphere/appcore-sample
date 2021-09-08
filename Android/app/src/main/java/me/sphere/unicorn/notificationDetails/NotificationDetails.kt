package me.sphere.unicorn.notificationDetails

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.tooling.preview.Preview
import me.sphere.unicorn.ui.theme.MyTheme

@Composable
fun NotificationDetails() {
    val notificationDetailsViewModel = hiltViewModel<NotificationDetailsViewModel>()
    val state by notificationDetailsViewModel.state.observeAsState()

    NotificationDetails(state = state!!)
}

@Composable
fun NotificationDetails(state: NotificationDetailsState) {
    Surface(color = MaterialTheme.colors.background) {
        Text(text = "Notification Details with id ${state.notificationId} with counter ${state.counter}")
    }
}

@Preview("Light Theme", widthDp = 360, heightDp = 640)
@Composable
fun NotificationDetailsPreviewLight() {
    MyTheme {
        NotificationDetails(
            NotificationDetailsState(
                notificationId = "not_id",
                counter = 66
            )
        )
    }
}

@Preview("Dark Theme", widthDp = 360, heightDp = 640)
@Composable
fun NotificationDetailsPreviewDark() {
    MyTheme(darkTheme = true) {
        NotificationDetails(
            NotificationDetailsState(
                notificationId = "not_id",
                counter = 66
            )
        )
    }
}
