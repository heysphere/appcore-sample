package me.sphere.unicorn.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import me.sphere.unicorn.notificationList.NotificationList
import me.sphere.unicorn.notificationDetails.NotificationDetails
import me.sphere.unicorn.ui.theme.RouteArgument.NotificationDetailsArg.Companion.NOTIFICATION_ID_KEY

@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Route.NotificationList.route,
        modifier = modifier,
    ) {
        addNotifications(navController)
    }
}

private fun NavGraphBuilder.addNotifications(
    navHostController: NavHostController
) {
    composable(
        route = Route.NotificationList.route,
    ) {
        NotificationList(
            openNotificationDetails = {
                navHostController.navigate(Route.NotificationDetails.createRoute(it))
            }
        )
    }

    composable(
        route = Route.NotificationDetails.route
    ) {
        NotificationDetails()
    }
}

sealed class Route(val route: String) {

    object NotificationList : Route("notificationList")

    object NotificationDetails : Route("notificationDetails/$NOTIFICATION_ID_KEY") {
        fun createRoute(notificationId: String): String {
            return "notificationDetails/$notificationId"
        }
    }
}

sealed class RouteArgument {

    data class NotificationDetailsArg(
        val notificationId: String
    ) : RouteArgument() {

        companion object {
            const val NOTIFICATION_ID_KEY = "notificationId"

            fun SavedStateHandle.toNotificationDetailsArg(): NotificationDetailsArg {
                val notificationId = requireNotNull(get<String>(NOTIFICATION_ID_KEY))
                return NotificationDetailsArg(notificationId)
            }
        }
    }
}