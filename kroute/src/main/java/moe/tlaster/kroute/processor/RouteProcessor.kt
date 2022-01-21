package moe.tlaster.kroute.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.visitor.KSEmptyVisitor
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import java.io.OutputStream

internal class RouteProcessor(
    private val codeGenerator: CodeGenerator,
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val routeSymbol = resolver
            .getSymbolsWithAnnotation(
                Route::class.qualifiedName
                    ?: throw CloneNotSupportedException("Can not get qualifiedName for Route")
            )
            .filterIsInstance<KSClassDeclaration>()
        routeSymbol.forEach { it.accept(RouteVisitor(), routeSymbol.toList()) }
        return emptyList()
    }

    inner class RouteVisitor : KSEmptyVisitor<List<KSClassDeclaration>, Unit>() {
        override fun defaultHandler(node: KSNode, data: List<KSClassDeclaration>) {
            if (node !is KSClassDeclaration) {
                return
            }

            val annotation = node.annotations
                .firstOrNull { it.annotationType.resolve().declaration.qualifiedName?.asString() == Route::class.qualifiedName }
                ?: return

            val schema = annotation.getStringValue(Route::schema.name) ?: ""
            val packageName = annotation.getStringValue(Route::packageName.name)
                ?: node.packageName.asString()
            val className = annotation.getStringValue(Route::className.name)
                ?: node.qualifiedName?.getShortName() ?: "<ERROR>"

            val route = generateRoute(declaration = node)
                .takeIf {
                    it is NestedRouteDefinition
                }?.let {
                    PrefixRouteDefinition(
                        schema = schema,
                        child = it as NestedRouteDefinition,
                        className = className,
                    )
                } ?: return

            val dependencies = Dependencies(
                true,
                *(data.mapNotNull { it.containingFile } + listOfNotNull(node.containingFile)).toTypedArray()
            )
            generateFile(
                dependencies,
                packageName,
                className,
                route.generateRoute()
            )
        }

        @OptIn(KotlinPoetKspPreview::class)
        private fun generateFile(
            dependencies: Dependencies,
            packageName: String,
            className: String,
            route: Taggable
        ) {
            FileSpec.builder(packageName, className)
                .apply {
                    when (route) {
                        is TypeSpec -> addType(route)
                        is FunSpec -> addFunction(route)
                        is PropertySpec -> addProperty(route)
                    }
                }
                .build()
                .writeTo(codeGenerator, dependencies)
        }

        @OptIn(KotlinPoetKspPreview::class)
        private fun generateRoute(
            declaration: KSDeclaration,
            parent: RouteDefinition? = null
        ): RouteDefinition {
            val name = declaration.simpleName.getShortName()
            return when (declaration) {
                is KSClassDeclaration -> {
                    NestedRouteDefinition(
                        name = name,
                        parent = parent,
                    ).also { nestedRouteDefinition ->
                        nestedRouteDefinition.childRoute.addAll(
                            declaration.declarations
                                .filter { it.simpleName.getShortName() != "<init>" }
                                .map { generateRoute(it, nestedRouteDefinition) }
                        )
                    }
                }
                is KSPropertyDeclaration -> {
                    ConstRouteDefinition(name, parent)
                }
                is KSFunctionDeclaration -> {
                    FunctionRouteDefinition(
                        name = name,
                        parent = parent,
                        parameters = declaration.parameters.map {
                            val parameterName = it.name?.getShortName() ?: "_"
                            val parameterType = it.type.toTypeName()
                            RouteParameter(
                                name = parameterName,
                                type = parameterType,
                                parameter = it
                            )
                        },
                    )
                }
                else -> throw NotImplementedError()
            }
        }
    }
}

private fun OutputStream.appendLine(str: String = "") {
    this.write("$str${System.lineSeparator()}".toByteArray())
}

internal fun KSAnnotation.getStringValue(name: String): String? = arguments
    .firstOrNull { it.name?.asString() == name }
    ?.let { it.value as? String? }.takeIf { !it.isNullOrEmpty() }
