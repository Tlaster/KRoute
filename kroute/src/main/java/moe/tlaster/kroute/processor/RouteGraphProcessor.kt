package moe.tlaster.kroute.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.visitor.KSEmptyVisitor
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.writeTo
import kotlin.reflect.KClass

class RouteGraphProcessor(
    private val codeGenerator: CodeGenerator
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val routeSymbol = resolver
            .getSymbolsWithAnnotation(
                ComposableRoute::class.qualifiedName
                    ?: throw CloneNotSupportedException("Can not get qualifiedName for ComposableRoute")
            )
            .filterIsInstance<KSFunctionDeclaration>()
        routeSymbol.forEach { it.accept(RouteGraphVisitor(), routeSymbol.toList()) }
        return emptyList()
    }

    inner class RouteGraphVisitor: KSEmptyVisitor<List<KSFunctionDeclaration>, Unit>() {
        @OptIn(KotlinPoetKspPreview::class, KspExperimental::class)
        override fun defaultHandler(node: KSNode, data: List<KSFunctionDeclaration>) {
            if (node !is KSFunctionDeclaration) {
                return
            }
            val dependencies = Dependencies(
                true,
                *(data.mapNotNull { it.containingFile }).toTypedArray()
            )
            FileSpec.builder(node.packageName.asString(), "RouteGraph")
                .addImport("androidx.navigation.compose", "composable")
                .addFunction(
                    FunSpec.builder("route")
//                        .receiver(Class.forName("androidx.navigation.NavGraphBuilder"))
                        .also { builder ->
                            data.forEach { ksFunctionDeclaration ->
                                val annotation = ksFunctionDeclaration.getAnnotationsByType(ComposableRoute::class).first()
                                builder.beginControlFlow("composable(route = %S)", annotation.route)
                                builder.addStatement(ksFunctionDeclaration.simpleName.asString() + "()")
                                builder.endControlFlow()
                            }
                        }
                        .build()
                )
                .build()
                .writeTo(codeGenerator, dependencies)
        }
    }
}