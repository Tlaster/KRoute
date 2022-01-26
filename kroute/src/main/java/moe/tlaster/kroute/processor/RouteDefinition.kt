package moe.tlaster.kroute.processor

import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlin.reflect.KProperty

private const val RouteDivider = "/"

internal interface RouteDefinition {
    val name: String
    val parent: RouteDefinition?
    fun generateRoute(): Taggable
}

internal fun RouteDefinition.parents(): List<RouteDefinition> {
    val list = arrayListOf<RouteDefinition>()
    var p = parent
    while (p != null) {
        list.add(0, p)
        p = p.parent
    }
    return list
}

internal val RouteDefinition.parentPath
    get() = parents()
        .joinToString(RouteDivider) { it.name }

internal data class PrefixRouteDefinition(
    val schema: String,
    val child: NestedRouteDefinition,
    val className: String,
) : RouteDefinition {

    override val name: String
        get() = if (schema.isEmpty()) "" else "$schema:$RouteDivider"
    override val parent: RouteDefinition?
        get() = null

    init {
        child.name = className
        child.parent = this
    }

    override fun generateRoute(): Taggable {
        return child.generateRoute()
    }
}

internal data class NestedRouteDefinition(
    override var name: String,
    override var parent: RouteDefinition? = null,
    val childRoute: ArrayList<RouteDefinition> = arrayListOf(),
) : RouteDefinition {
    override fun generateRoute(): Taggable {
        return TypeSpec.objectBuilder(name)
            .apply {
                childRoute.forEach {
                    it.generateRoute().addTo(this)
                }
            }
            .build()
    }
}

private fun Taggable.addTo(builder: TypeSpec.Builder) {
    when (this) {
        is TypeSpec -> builder.addType(this)
        is FunSpec -> builder.addFunction(this)
        is PropertySpec -> builder.addProperty(this)
    }
}

internal data class ConstRouteDefinition(
    override val name: String,
    override val parent: RouteDefinition? = null,
) : RouteDefinition {
    override fun generateRoute(): Taggable {
        return PropertySpec.builder(name, String::class)
            .addModifiers(KModifier.CONST)
            .initializer("%S + %S + %S", parentPath, RouteDivider, name)
            .build()
    }
}

internal data class FunctionRouteDefinition(
    override val name: String,
    override val parent: RouteDefinition? = null,
    val parameters: List<RouteParameter>,
) : RouteDefinition {
    override fun generateRoute(): Taggable {
        val p = parameters.filter { !it.parameter.type.resolve().isMarkedNullable }
        val query = parameters.filter { it.parameter.type.resolve().isMarkedNullable }
        return TypeSpec.objectBuilder(name)
            .addFunction(
                FunSpec.builder("invoke")
                    .addModifiers(KModifier.OPERATOR)
                    .returns(String::class)
                    .addParameters(
                        parameters.map {
                            ParameterSpec.builder(it.name, it.type)
                                .build()
                        })
                    .addStatement("val path = %S + %S + %S", parentPath, RouteDivider, name)
                    .also {
                        if (p.any()) {
                            it.addStatement(
                                "val params = %S + %P",
                                RouteDivider,
                                p.joinToString(RouteDivider){ if (it.type == ClassName("kotlin", "String")) "\${${encode(it.name)}}" else "\${${it.name}}" },
                            )
                        } else {
                            it.addStatement("val params = \"\"")
                        }
                        if (query.any()) {
                            it.addStatement("val query = \"?\" + %P", query.joinToString("&") {
                                if (it.type == ClassName("kotlin", "String")) {
                                    "${it.name}=\${${encodeNullable(it.name)}}"
                                } else {
                                    "${it.name}=\${${it.name}}"
                                }
                            })
                        } else {
                            it.addStatement("val query = \"\"")
                        }
                    }
                    .addStatement("return path + params + query")
                    .build()
            )
            .addProperty(
                PropertySpec.builder("path", String::class)
                    .addModifiers(KModifier.CONST)
                    .initializer("%S + %S + %S + %S + %S",
                        parentPath,
                        RouteDivider,
                        name,
                        RouteDivider,
                        p.joinToString(RouteDivider) { "{${it.name}}" })
                    .build()
            )
            .build()
    }

    private fun encode(value: String) = "java.net.URLEncoder.encode($value, \"UTF-8\")"
    private fun encodeNullable(value: String) =
        "java.net.URLEncoder.encode(if($value == null) \"\" else $value, \"UTF-8\")"
}

internal data class RouteParameter(
    val name: String,
    val type: TypeName,
    val parameter: KSValueParameter,
)
