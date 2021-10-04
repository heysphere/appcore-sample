package me.sphere.unicorn.notificationList

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.placeholder.placeholder
import me.sphere.appcore.dataSource.PagingStatus
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
        val scrollState = rememberLazyListState()
        val items = state.state?.items
        when (state.state?.status) {
            PagingStatus.LOADING, null ->
                LazyColumn(
                    modifier = modifier,
                    state = scrollState
                ) {
                    repeat((0..20).count()) {
                        item {
                            NotificationItemPlaceholder(
                                modifier
                            )
                        }
                    }
                }
            PagingStatus.HAS_MORE,
            PagingStatus.END_OF_COLLECTION -> {
                LazyColumn(
                    modifier = modifier,
                    state = scrollState
                ) {
                    items?.forEach { notification ->
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
                    if (state.state?.status == PagingStatus.HAS_MORE) {
                        item {
                            NotificationItemPlaceholder(
                                modifier
                            )
                        }
                    }
                }
                val shouldLoadNextPage by remember {
                    derivedStateOf {
                        scrollState.layoutInfo.totalItemsCount > 0 && scrollState.firstVisibleItemIndex >= (scrollState.layoutInfo.totalItemsCount - 10)
                    }
                }
                LaunchedEffect(shouldLoadNextPage) {
                    if (shouldLoadNextPage) {
                        notificationListViewModel.sendAction(NotificationListAction.LoadNextPage)
                    }
                }
            }
            PagingStatus.FAILED -> TODO()
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
    openNotificationDetails: (String) -> Unit,
    textModifier: Modifier = Modifier
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
                style = MaterialTheme.typography.caption,
                modifier = textModifier
            )
            Text(
                text = description,
                style = MaterialTheme.typography.body1,
                modifier = textModifier
            )
        }
        Text(
            text = "#$subjectId",
            style = MaterialTheme.typography.caption,
            modifier = textModifier
        )
    }
}

@Composable
private fun NotificationItemPlaceholder(
    modifier: Modifier,
) {
    NotificationItem(
        modifier = modifier,
        id = "?",
        subjectId = "",
        unread = false,
        repository = "some repo",
        description = "Test description",
        openNotificationDetails = {},
        textModifier = Modifier
            .padding(bottom = 4.dp)
            .placeholder(visible = true, color = Color.LightGray),
    )
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

