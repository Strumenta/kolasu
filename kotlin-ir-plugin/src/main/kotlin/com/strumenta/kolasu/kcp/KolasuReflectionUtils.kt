package com.strumenta.kolasu.kcp

import com.strumenta.kolasu.model.INode
import com.strumenta.kolasu.model.ReferenceByName
import org.jetbrains.kotlin.backend.jvm.ir.getIoFile
import org.jetbrains.kotlin.backend.jvm.ir.psiElement
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocationWithRange
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperClassifiers
import kotlin.reflect.KClass

val IrDeclarationBase.compilerSourceLocation: CompilerMessageSourceLocation?
    get() {
        val fileText = this.file.getIoFile()!!.readText()
        StringUtil.offsetToLineNumber(fileText, this.startOffset)
        StringUtil.offsetToLineNumber(fileText, this.startOffset)
        StringUtil.offsetToLineNumber(fileText, this.endOffset)
        StringUtil.offsetToLineNumber(fileText, this.endOffset)
        return CompilerMessageLocationWithRange.create(
            this.file.path,
            StringUtil.offsetToLineNumber(fileText, this.startOffset),
            StringUtil.offsetToLineNumber(fileText, this.startOffset),
            StringUtil.offsetToLineNumber(fileText, this.endOffset),
            StringUtil.offsetToLineNumber(fileText, this.endOffset),
            null,
        )
    }

fun IrValueParameter.isVal(): Boolean {
    return this.psiElement!!.firstChild.text == "val"
}

@ObsoleteDescriptorBasedAPI
fun IrValueParameter.isSingleContainment(): Boolean {
    val propertyType = this.type
    return propertyType.isSingleContainment() ?: false
}

@ObsoleteDescriptorBasedAPI
fun IrValueParameter.isSingleAttribute(): Boolean {
    val propertyType = this.type
    return propertyType.isSingleAttribute() ?: false
}

@ObsoleteDescriptorBasedAPI
fun IrProperty.declareSingleContainment(): Boolean {
    val propertyType = this.backingField?.type
    return propertyType?.isSingleContainment() ?: false
}

@ObsoleteDescriptorBasedAPI
fun IrProperty.declareReference(): Boolean {
    val propertyType = this.backingField?.type
    return propertyType?.isReference() ?: false
}

@ObsoleteDescriptorBasedAPI
fun IrType.isSingleContainment(): Boolean {
    return if (this is IrSimpleType) {
        this.isAssignableTo(INode::class)
    } else {
        false
    }
}

@ObsoleteDescriptorBasedAPI
fun IrType.isSingleAttribute(): Boolean {
    return !this.isAssignableTo(Collection::class) && !this.isAssignableTo(INode::class)
}

@ObsoleteDescriptorBasedAPI
fun IrType.isReference(): Boolean {
    return this.isAssignableTo(ReferenceByName::class)
}

@ObsoleteDescriptorBasedAPI
fun IrProperty.declareMultipleContainment(): Boolean {
    val propertyType = this.backingField?.type
    return propertyType?.isAssignableTo(Collection::class) ?: false
}

@ObsoleteDescriptorBasedAPI
fun IrType.isAssignableTo(kClass: KClass<*>): Boolean =
    if (this is IrSimpleType) {
        this.classFqName.toString() == kClass.qualifiedName ||
            this.classifier.descriptor.getAllSuperClassifiers().any {
                it.fqNameOrNull()?.toString() == kClass.qualifiedName
            }
    } else {
        false
    }
