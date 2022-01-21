package moe.tlaster.kroute.processor

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class Route(
    val schema: String = "",
    val packageName: String = "",
    val className: String = "",
)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class RouteGraph

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class ComposableRoute(
    val route: String = "",
    val deeplink: Array<String> = [],
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