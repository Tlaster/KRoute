package moe.tlaster.kroute.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.visitor.KSEmptyVisitor
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

@OptIn(KotlinPoetKspPreview::class, KspExperimental::class)
class RouteGraphProcessor(
    private val codeGenerator: CodeGenerator
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver
            .getSymbolsWithAnnotation(
                ComposableRoute::class.qualifiedName
                    ?: throw CloneNotSupportedException("Can not get qualifiedName for ComposableRoute")
            ).filterIsInstance<KSFunctionDeclaration>().toList()
        val ret = symbols.filter {
            try {
                it.getAnnotationsByType(ComposableRoute::class).first().route
                false
            } catch (e: Throwable) {
                true
            }
        }
        val actualSymbols = symbols.filter {
            try {
                it.getAnnotationsByType(ComposableRoute::class).first().route
                true
            } catch (e: Throwable) {
                false
            }
        }
        actualSymbols.forEach {
            it.accept(
                RouteGraphVisitor(),
                actualSymbols.toList()
            )
        }
        return ret
    }

    inner class RouteGraphVisitor : KSEmptyVisitor<List<KSFunctionDeclaration>, Unit>() {
        override fun defaultHandler(node: KSNode, data: List<KSFunctionDeclaration>) {
            if (node !is KSFunctionDeclaration) {
                return
            }
            val dependencies = Dependencies(
                true,
                *(data.mapNotNull { it.containingFile }).toTypedArray()
            )
            FileSpec.builder(node.packageName.asString(), "RouteGraph")
                .addImport("androidx.navigation", "NavType")
                .addImport("androidx.navigation", "navDeepLink")
                .addImport("androidx.navigation", "navArgument")
                .also { fileBuilder ->
                    fileBuilder.addFunction(
                        FunSpec.builder("route")
                            .receiver(ClassName("androidx.navigation", "NavGraphBuilder"))
                            .also { builder ->
                                data.forEach { ksFunctionDeclaration ->
                                    val annotation =
                                        ksFunctionDeclaration.getAnnotationsByType(ComposableRoute::class)
                                            .first()
                                    fileBuilder.addImport(
                                        annotation.packageName,
                                        annotation.simpleName
                                    )
                                    builder.addStatement(
                                        "%N(",
                                        annotation.simpleName,
                                    )
                                    builder.addCode(
                                        buildCodeBlock {
                                            withIndent {
                                                addStatement(
                                                    "route = %S,",
                                                    annotation.route,
                                                )
                                                addStatement("arguments = listOf(")
                                                withIndent {
                                                    ksFunctionDeclaration.parameters.forEach {
                                                        addStatement(
                                                            "navArgument(%S) { type = NavType.%NType; nullable = %L }",
                                                            it.name?.asString() ?: "",
                                                            it.type.resolve().declaration.simpleName.asString(),
                                                            it.isAnnotationPresent(Query::class)
                                                        )
                                                    }
                                                }
                                                addStatement("),")
                                                addStatement("deepLinks = listOf(")
                                                withIndent {
                                                    annotation.deeplink.forEach {
                                                        addStatement(
                                                            "navDeepLink { uriPattern = %S }",
                                                            it
                                                        )
                                                    }
                                                }
                                                addStatement("),")
                                            }
                                        }
                                    )
                                    builder.beginControlFlow(")")
                                    ksFunctionDeclaration.parameters.forEach {
                                        require(it.type.resolve().isMarkedNullable)
                                        if (it.isAnnotationPresent(Path::class)) {
                                            val path = it.getAnnotationsByType(Path::class).first()
                                            builder.addStatement(
                                                "val ${it.name?.asString()} = it.arguments?.get(%S) as? %T",
                                                path.name,
                                                it.type.toTypeName()
                                            )
                                        } else if (it.isAnnotationPresent(Query::class)) {
                                            val query =
                                                it.getAnnotationsByType(Query::class).first()
                                            builder.addStatement(
                                                "val ${it.name?.asString()} = it.arguments?.get(%S) as? %T",
                                                query.name,
                                                it.type.toTypeName()
                                            )
                                        }
                                    }
                                    builder.addCode(
                                        buildCodeBlock {
                                            addStatement(
                                                "%N(",
                                                ksFunctionDeclaration.simpleName.asString()
                                            )
                                            withIndent {
                                                ksFunctionDeclaration.parameters.forEach {
                                                    addStatement(
                                                        "%N = %N,",
                                                        it.name?.asString() ?: "",
                                                        it.name?.asString() ?: ""
                                                    )
                                                }
                                            }
                                            addStatement(")")
                                        }
                                    )
                                    builder.endControlFlow()
                                }
                            }
                            .build()
                    )
                }
                .build()
                .writeTo(codeGenerator, dependencies)
        }
    }
}