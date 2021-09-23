package me.sphere.unicorn.ui.components

import androidx.annotation.StringRes
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.insets.statusBarsPadding
import me.sphere.unicorn.R
import me.sphere.unicorn.ui.theme.MyTheme

@Composable
fun InsetAwareTopAppBar(
    modifier: Modifier = Modifier,
    @StringRes titleRes: Int,
    topBarNavigation: TopBarNavigation? = null,
) {
    val backgroundColor = MaterialTheme.colors.surface
    Surface(
        color = MaterialTheme.colors.surface,
        elevation = 4.dp,
        modifier = modifier
    ) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(id = titleRes),
                   // modifier = Modifier
                        //.fillMaxSize()
                       // .padding(bottom = 4.dp, top = 10.dp)
                )
            },
            backgroundColor = Color.Transparent,
            contentColor = contentColorFor(backgroundColor),
            elevation = 0.dp,
            navigationIcon = topBarNavigation?.let(::navigationIcon),
            modifier = Modifier
                .statusBarsPadding()
                .navigationBarsPadding(bottom = false)
        )
    }
}

@Stable
data class TopBarNavigation(
    val navigationIcon: ImageVector,
    val navigateUp: () -> Unit
)

private fun navigationIcon(topBarNavigation: TopBarNavigation) = @Composable {
    IconButton(onClick = topBarNavigation.navigateUp) {
        Icon(
            imageVector = topBarNavigation.navigationIcon,
            contentDescription = stringResource(R.string.navigate_up)
        )
    }
}

@Preview("Light Theme", widthDp = 360, heightDp = 640)
@Composable
fun LightPreview() {
    MyTheme {
        InsetAwareTopAppBar(titleRes = R.string.notification_list_title)
    }
}

@Preview("Dark Theme", widthDp = 360, heightDp = 640)
@Composable
fun DarkPreview() {
    MyTheme(darkTheme = true) {
        InsetAwareTopAppBar(titleRes = R.string.notification_list_title)
    }
}