package moe.tlaster.kroute.processor

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class Route(
    val schema: String = "",
    val packageName: String = "",
    val className: String = "",
)

@Retention(AnnotationRetention.SOURCE)
annotation class RoutePackage(
    val packageName: String,
    val functionName: String,
)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class RouteGraphDestination(
    val route: String,
    val deeplink: Array<String> = [],
    val routePackage: RoutePackage = RoutePackage(
        packageName = "androidx.navigation.compose",
        functionName = "composable"
    ),
)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Path(
    val name: String = "",
)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Query(
    val name: String = "",
)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Navigate(
    val target: String,
)


@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Back

