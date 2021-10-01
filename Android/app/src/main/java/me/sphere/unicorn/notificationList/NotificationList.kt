package me.sphere.unicorn.notificationList

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.getValue
import me.sphere.unicorn.ui.theme.MyTheme
import me.sphere.unicorn.R
import me.sphere.unicorn.ui.components.InsetAwareTopAppBar

@Composable
fun NotificationList(
    openNotificationDetails: (String) -> Unit
) {
    val notificationListViewModel = hiltViewModel<NotificationListViewModel>()
    val state by notificationListViewModel.stateFlow.collectAsState()

    Scaffold(topBar = {
        InsetAwareTopAppBar(
            titleRes = R.string.notification_list_title,
        )
    }) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        LazyColumn(
            modifier = modifier
        ) {
            state.state?.items?.forEach { notification ->
                item(key = notification.notificationId) {
                    NotificationItem(
                        modifier = modifier,
                        id = notification.notificationId,
                        subjectId = notification.subjectId,
                        unread = notification.unread,
                        repository = notification.repositoryName,
                        description = notification.title,
                        openNotificationDetails = { openNotificationDetails(it) }
                    )
                    Divider()
                }
            }
        }
    }
}

@Composable
private fun NotificationItem(
    modifier: Modifier,
    id: String,
    subjectId: String,
    unread: Boolean,
    repository: String,
    description: String,
    openNotificationDetails: (String) -> Unit
) {
    Row(
        modifier = modifier
            .clickable { openNotificationDetails(id) }
            .padding(horizontal = 8.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(
            modifier = Modifier.size(16.dp),
            onDraw = {
                if (unread)
                    drawCircle(color = Color.Magenta)
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = repository,
                style = MaterialTheme.typography.caption
            )
            Text(
                text = description,
                style = MaterialTheme.typography.body1
            )
        }
        Text(
            text = "#$subjectId",
            style = MaterialTheme.typography.caption,
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
        NotificationList {}
    }
}

