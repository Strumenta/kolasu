package com.strumenta.kolasu.kcp

import com.strumenta.kolasu.language.Concept
import com.strumenta.kolasu.model.NodeLike
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrClassPublicSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.superTypes
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.util.toIrConst
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import kotlin.reflect.KClass

fun IrPluginContext.referenceConstructors(klass: KClass<*>): Collection<IrConstructorSymbol> {
    val classId = ClassId.topLevel(FqName(klass.qualifiedName!!))
    return this.referenceConstructors(classId)
}

fun IrPluginContext.referenceFunctions(
    klass: KClass<*>,
    methodName: String,
): Collection<IrSimpleFunctionSymbol> {
    val classId = ClassId.topLevel(FqName(klass.qualifiedName!!))
    val callableId = CallableId(classId, Name.identifier(methodName))
    return this.referenceFunctions(callableId)
}

val KClass<*>.packageName: String
    get() = this.qualifiedName!!.removeSuffix(".${this.simpleName}")

val KClass<*>.classifierSymbol: IrClassifierSymbol
    get() = IrClassPublicSymbolImpl(IdSignature.CommonSignature(this.packageName, this.simpleName!!, null, 0, null))

val KClass<*>.classId: ClassId
    get() = ClassId(FqName(this.packageName), Name.identifier(this.simpleName!!))

val IrType.isEnum: Boolean
    get() = this.classifierOrNull!!.superTypes().any { it.classFqName!!.asString() == Enum::class.qualifiedName }

fun IrBuilderWithScope.companionGetter(irClass: IrClass): IrExpression {
    val companionClass =
        irClass
            .companionObject() ?: throw IllegalStateException(
            "Cannot find companion object for ${irClass.kotlinFqName}",
        )
    val companionInstance = irGet(companionClass.thisReceiver!!)
    return companionInstance
}

fun IrBuilderWithScope.conceptGetter(nodeSubclass: IrClass): IrExpression {
    val companionClass =
        nodeSubclass
            .companionObject()
            ?: throw IllegalStateException(
                "Cannot find companion object for ${nodeSubclass.kotlinFqName}",
            )
    val conceptField = companionClass.properties.find { it.name.identifier == "concept" }!!
    val conceptInstance: IrExpression =
        irCall(conceptField.getter!!).apply {
            dispatchReceiver = companionGetter(nodeSubclass)
        }
    return conceptInstance
}

fun IrBuilderWithScope.conceptGetter(
    pluginContext: IrPluginContext,
    nodeInstance: IrExpression,
): IrExpression {
    val nodeLike = pluginContext.referenceClass(NodeLike::class.classId)!!

    val conceptField = nodeLike.owner.properties.find { it.name.identifier == "concept" }!!
    val conceptInstance: IrExpression =
        irCall(conceptField.getter!!).apply {
            dispatchReceiver = nodeInstance
        }
    return conceptInstance
}

fun IrBuilderWithScope.propertyByName(
    pluginContext: IrPluginContext,
    nodeSubclass: IrClass,
    propertyName: String,
): IrExpression {
    val propertyMethod = pluginContext.referenceFunctions(Concept::class, "requireProperty").single()
    val propertyNameExpr = propertyName.toIrConst(pluginContext.irBuiltIns.stringType)

    val conceptInstance: IrExpression = conceptGetter(nodeSubclass)

    val property =
        irCall(propertyMethod).apply {
            dispatchReceiver = conceptInstance
            putValueArgument(0, propertyNameExpr)
        }
    return property
}

fun IrBuilderWithScope.referenceByName(
    pluginContext: IrPluginContext,
    nodeSubclass: IrClass,
    referenceName: String,
): IrExpression {
    val attributeMethod = pluginContext.referenceFunctions(Concept::class, "requireReference").single()
    val referenceName = referenceName.toIrConst(pluginContext.irBuiltIns.stringType)

    val conceptInstance: IrExpression = conceptGetter(nodeSubclass)

    val attribute =
        irCall(attributeMethod).apply {
            dispatchReceiver = conceptInstance
            putValueArgument(0, referenceName)
        }
    return attribute
}

fun IrBuilderWithScope.referenceByName(
    pluginContext: IrPluginContext,
    nodeInstance: IrExpression,
    referenceName: String,
): IrExpression {
    val attributeMethod = pluginContext.referenceFunctions(Concept::class, "requireReference").single()
    val referenceName = referenceName.toIrConst(pluginContext.irBuiltIns.stringType)

    val conceptInstance: IrExpression = conceptGetter(pluginContext, nodeInstance)

    val attribute =
        irCall(attributeMethod).apply {
            dispatchReceiver = conceptInstance
            putValueArgument(0, referenceName)
        }
    return attribute
}

val FqName.packageName: String
    get() = this.parent().asString()
