package moe.tlaster.kroute.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import moe.tlaster.kroute.processor.ComposableRoute
import moe.tlaster.kroute.processor.Path
import moe.tlaster.kroute.processor.Route

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
        }
    }
}

fun main() {
}

@Route(
    className = "Root"
)
interface IRoot {
    val Home: String
    interface Login {
        val Register: String
        fun Login(username: String, username2: String, password: String? = "123", dsad: String?): String
        fun Login2(username: String, username2: String): String
    }
}

@ComposableRoute(
    route = Root.Home,
)
@Composable
fun Home(
    @Path("userName") username: String?,
) {
    NavHost(
        rememberNavController(),
        startDestination = "asd"
    ) {
        composable(
            "",
            arguments = listOf(
                navArgument("") { type = NavType.StringType; nullable = false }
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "dsa" }
            )
        ) {
        }
    }
}