package me.sphere.unicorn

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import me.sphere.unicorn.ui.theme.AppNavigation
import me.sphere.unicorn.ui.theme.MyTheme

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyTheme {
                val navHostController = rememberNavController()
                AppNavigation(navController = navHostController, modifier = Modifier)
            }
        }
    }
}
