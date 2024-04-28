package com.strumenta.kolasu.kcp

import com.strumenta.kolasu.language.StarLasuLanguage
import com.strumenta.kolasu.model.MPNode
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.getAllSuperclasses
import org.jetbrains.kotlin.ir.util.kotlinFqName

abstract class BaseIrGenerationExtension : IrGenerationExtension

fun processMPNodeSubclasses(
    moduleFragment: IrModuleFragment,
    mpNodeProcessor: (irClass: IrClass) -> Unit,
    languageProcessor: (irClass: IrClass) -> Unit = {},
) {
    moduleFragment.files.forEach { irFile ->
        irFile.declarations.filterIsInstance<IrClass>().forEach { irClass ->
            val isMPNode =
                irClass.getAllSuperclasses().any {
                    it.kotlinFqName.toString() == MPNode::class.qualifiedName
                }
            val isLanguage =
                irClass.getAllSuperclasses().any {
                    it.kotlinFqName.toString() == StarLasuLanguage::class.qualifiedName
                }
            if (isMPNode) {
                mpNodeProcessor.invoke(irClass)
            } else if (isLanguage) {
                languageProcessor.invoke(irClass)
            }
        }
    }
}
