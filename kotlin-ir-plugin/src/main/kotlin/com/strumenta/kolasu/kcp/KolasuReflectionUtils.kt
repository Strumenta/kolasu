package com.strumenta.kolasu.kcp

import com.strumenta.kolasu.model.Derived
import com.strumenta.kolasu.model.FeatureType
import com.strumenta.kolasu.model.Multiplicity
import com.strumenta.kolasu.model.NodeLike
import com.strumenta.kolasu.model.ReferenceByName
import org.jetbrains.kotlin.backend.jvm.ir.getIoFile
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
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.hasAnnotation
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
    val text =
        this
            .file
            .getIoFile()!!
            .readText()
            .substring(this.startOffset, this.endOffset)
            .trim()
    return text.startsWith("val ")
}

@ObsoleteDescriptorBasedAPI
fun IrValueParameter.isSingleOrOptionalContainment(): Boolean {
    val propertyType = this.type
    return propertyType.isSingleOrOptionalContainment() ?: false
}

@ObsoleteDescriptorBasedAPI
fun IrValueParameter.isSingleOrOptionalAttribute(): Boolean {
    val propertyType = this.type
    return propertyType.isSingleOrOptionalAttribute() ?: false
}

@ObsoleteDescriptorBasedAPI
fun IrProperty.declareSingleOrOptionalContainment(): Boolean {
    val propertyType = this.backingField?.type
    return propertyType?.isSingleOrOptionalContainment() ?: false
}

@ObsoleteDescriptorBasedAPI
fun IrProperty.declareReference(): Boolean {
    val propertyType = this.backingField?.type
    return propertyType?.isReference() ?: false
}

@ObsoleteDescriptorBasedAPI
fun IrType.isSingleOrOptionalContainment(): Boolean {
    return if (this is IrSimpleType) {
        this.isAssignableTo(NodeLike::class)
    } else {
        false
    }
}

@ObsoleteDescriptorBasedAPI
fun IrType.isMultipleContainment(): Boolean {
    return if (this is IrSimpleType) {
        if (this.isAssignableTo(List::class)) {
            (this.arguments[0] as? IrSimpleType)?.isSingleOrOptionalContainment() ?: false
        } else {
            false
        }
    } else {
        false
    }
}

@ObsoleteDescriptorBasedAPI
fun IrType.isSingleOrOptionalAttribute(): Boolean {
    return !this.isAssignableTo(List::class) && !this.isAssignableTo(NodeLike::class)
}

@ObsoleteDescriptorBasedAPI
fun IrType.isReference(): Boolean {
    return this.isAssignableTo(ReferenceByName::class)
}

@ObsoleteDescriptorBasedAPI
fun IrProperty.declareMultipleContainment(): Boolean {
    val propertyType = this.backingField?.type
    return propertyType?.isAssignableTo(List::class) ?: false
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

@OptIn(ObsoleteDescriptorBasedAPI::class)
fun IrType.featureType(): FeatureType {
    return when {
        isSingleOrOptionalAttribute() -> FeatureType.ATTRIBUTE
        isSingleOrOptionalContainment() -> FeatureType.CONTAINMENT
        isMultipleContainment() -> FeatureType.CONTAINMENT
        else -> TODO()
    }
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
fun IrType.multiplicity(): Multiplicity {
    return when {
        isSingleOrOptionalAttribute() -> Multiplicity.SINGULAR
        isSingleOrOptionalContainment() -> {
            if (this.isNullable()) {
                Multiplicity.OPTIONAL
            } else {
                Multiplicity.SINGULAR
            }
        }
        isAssignableTo(List::class) -> Multiplicity.MANY
        else -> TODO()
    }
}

@ObsoleteDescriptorBasedAPI
fun IrProperty.isDerived(): Boolean {
    return this.hasAnnotation(Derived::class.classId)
}

@ObsoleteDescriptorBasedAPI
fun IrProperty.providesNodes(): Boolean {
    val featureType = this.getter!!.returnType.featureType()
    return featureType in setOf(FeatureType.CONTAINMENT, FeatureType.REFERENCE)
}
