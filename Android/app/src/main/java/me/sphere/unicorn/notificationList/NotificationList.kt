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
import me.sphere.unicorn.ui.theme.MyTheme
import me.sphere.unicorn.R
import me.sphere.unicorn.ui.components.InsetAwareTopAppBar

@Composable
fun NotificationList(
    openNotificationDetails: (String) -> Unit
) {
    val notificationListViewModel = hiltViewModel<NotificationListViewModel>()
    val state = notificationListViewModel.stateFlow.collectAsState()

    Scaffold(topBar = {
        InsetAwareTopAppBar(
            titleRes = R.string.notification_list_title,
        )
    }) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        LazyColumn(
            modifier = modifier
        ) {
            for (i in 1..25) {
                item {
                    NotificationItem(
                        modifier = modifier,
                        id = i.toString(),
                        owner = "owner",
                        repository = "repostory #$i",
                        description = "some text goes here #$i",
                        openNotificationDetails = { openNotificationDetails(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationItem(
    modifier: Modifier,
    id: String,
    owner: String,
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
                drawCircle(color = Color.Magenta)
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = "$owner/$repository",
                style = MaterialTheme.typography.caption
            )
            Text(
                text = description,
                style = MaterialTheme.typography.body1
            )
        }
        Text(
            text = "#$id",
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

