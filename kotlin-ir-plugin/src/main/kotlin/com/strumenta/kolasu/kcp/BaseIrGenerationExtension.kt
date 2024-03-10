package com.strumenta.kolasu.kcp

import com.strumenta.kolasu.model.MPNode
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.getAllSuperclasses
import org.jetbrains.kotlin.ir.util.kotlinFqName

abstract class BaseIrGenerationExtension : IrGenerationExtension {
    fun processMPNodeSubclasses(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext,
        processor: (irClass: IrClass) -> Unit,
    ) {
        moduleFragment.files.forEach { irFile ->
            irFile.declarations.filterIsInstance<IrClass>().forEach { irClass ->
                val isMPNode =
                    irClass.getAllSuperclasses().any {
                        it.kotlinFqName.toString() == MPNode::class.qualifiedName
                    }
                if (isMPNode) {
                    processor.invoke(irClass)
                }
            }
        }
    }
}
