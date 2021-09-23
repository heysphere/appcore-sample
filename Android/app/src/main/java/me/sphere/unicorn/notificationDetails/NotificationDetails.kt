package me.sphere.unicorn.notificationDetails

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.sphere.unicorn.R
import me.sphere.unicorn.ui.components.InsetAwareTopAppBar
import me.sphere.unicorn.ui.components.TopBarNavigation
import me.sphere.unicorn.ui.theme.MyTheme

@Composable
fun NotificationDetails(onNavigateBack: () -> Unit) {
    val notificationDetailsViewModel = hiltViewModel<NotificationDetailsViewModel>()
    val state by notificationDetailsViewModel.state.observeAsState()

    NotificationDetails(state = state!!, onNavigateBack = onNavigateBack)
}

@Composable
private fun NotificationDetails(state: NotificationDetailsState, onNavigateBack: () -> Unit) {
    Scaffold(topBar = {
        InsetAwareTopAppBar(
            titleRes = R.string.notification_details_title,
            topBarNavigation = TopBarNavigation(
                Icons.Default.ArrowBack,
                onNavigateBack
            )
        )
    }) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        Column(
            modifier = modifier.fillMaxWidth()
                .padding(8.dp, 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NotificationInfoRow(R.string.notification_details_info_reason, "Some Reason")
            NotificationInfoRow(R.string.notification_details_info_repository, "owner/repository")
            NotificationInfoRow(R.string.notification_details_info_pull_request_id, "#123")
            NotificationInfoRow(R.string.notification_details_info_pull_request_title, "This is a very long name asdasdasdsa asdasdasd ")
        }
    }
}

@Composable
private fun NotificationInfoRow(@StringRes titleRes: Int, description: String) {
    Column {
        Row {
            Text(
                text = stringResource(id = titleRes),
                style = MaterialTheme.typography.body1,
                modifier = Modifier.weight(0.35f, true)
            )

            Text(
                text = description,
                style = MaterialTheme.typography.body1,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(0.65f, true)
            )
        }
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
        ) {}
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
        ) {}
    }
}
