package moe.tlaster.kroute.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import moe.tlaster.kroute.processor.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            App()
        }
    }
}


@Composable
fun App() {
    MaterialTheme {
        Route()
    }
}

@Composable
fun Route() {
    val controller = rememberNavController()
    NavHost(controller, startDestination = Root.Home) {
        generatedRoute(controller)
    }
}

@Route(
    className = "Root"
)
interface RootRoute {
    val Home: String
    fun Detail(id: String): String
    fun Item(index: Long?): String
}

@RouteGraphDestination(
    route = Root.Home,
)
@Composable
fun Home(
    @Navigate(Root.Detail.path) goDetail: (id: String) -> Unit,
    navController: NavController,
) {
    Scaffold {
        Column {
            var text by remember {
                mutableStateOf("")
            }
            TextField(text, onValueChange = { text = it })
            Button(onClick = {
                goDetail.invoke(text)
            }) {
                Text("Click me!")
            }
        }
    }
}

@RouteGraphDestination(
    route = Root.Detail.path
)
@Composable
fun Detail(
    @Path("id") id: String?,
    @Back onBack: () -> Unit,
) {
    if (id == null) {
        return
    }
    Scaffold {
        Column {
            Text(id)
            Button(onClick = { onBack.invoke() }) {
                Text("Back")
            }
        }
    }
}

