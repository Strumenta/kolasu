package com.strumenta.kolasu.kcp

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrClassPublicSymbolImpl
import org.jetbrains.kotlin.ir.util.IdSignature
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
