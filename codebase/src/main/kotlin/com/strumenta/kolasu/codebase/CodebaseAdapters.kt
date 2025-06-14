package com.strumenta.kolasu.codebase

import com.strumenta.kolasu.lionweb.IssueNode
import com.strumenta.kolasu.lionweb.LWNode
import com.strumenta.kolasu.lionweb.LionWebModelConverter
import com.strumenta.kolasu.lionweb.TokensList
import com.strumenta.kolasu.model.Node
import com.strumenta.starlasu.base.CodebaseAccess
import com.strumenta.starlasu.base.CodebaseLanguage
import io.lionweb.lioncore.java.model.ClassifierInstanceUtils
import io.lionweb.lioncore.java.model.impl.DynamicNode
import io.lionweb.lioncore.java.serialization.JsonSerialization
import io.lionweb.lioncore.kotlin.getChildrenByContainmentName
import io.lionweb.lioncore.kotlin.getOnlyChildByContainmentName
import io.lionweb.lioncore.kotlin.getPropertyValueByName
import io.lionweb.lioncore.kotlin.setPropertyValueByName
import kotlin.streams.asSequence

fun <R : Node> deserialize(
    modelConverter: LionWebModelConverter,
    codebase: Codebase<R>,
    codebaseFile: LWNode
): CodebaseFile<R> {
    val relativePath = codebaseFile.getPropertyValueByName("relative_path") as String
    val code = codebaseFile.getPropertyValueByName("code") as String
    val ast = codebaseFile.getOnlyChildByContainmentName("ast")!!
    val compilationUnit = modelConverter.importModelFromLionWeb(ast) as R
    val issues =
        codebaseFile.getChildrenByContainmentName("issues").map { lwIssue ->
            modelConverter.importIssueFromLionweb(lwIssue as IssueNode).second
        }
    val tokens = codebaseFile.getPropertyValueByName("tokens") as TokensList
    return CodebaseFile(codebase, relativePath, code, compilationUnit, tokens, issues)
}

fun <R : Node> serialize(
    modelConverter: LionWebModelConverter,
    id: String,
    languageName: String,
    codebaseFile: CodebaseFile<R>
): LWNode {
    val lwCodebaseFile = DynamicNode(id, CodebaseLanguage.getCodebaseFile())
    lwCodebaseFile.setPropertyValueByName("language_name", languageName)
    lwCodebaseFile.setPropertyValueByName("relative_path", codebaseFile.relativePath)
    lwCodebaseFile.setPropertyValueByName("code", codebaseFile.code)
    lwCodebaseFile.setPropertyValueByName("tokens", codebaseFile.tokens)
    modelConverter.clearNodesMapping()
    val ast = modelConverter.exportModelToLionWeb(codebaseFile.ast)
    ClassifierInstanceUtils.setOnlyChildByContainmentName(lwCodebaseFile, "ast", ast)
    codebaseFile.parsingIssues.forEachIndexed { index, issue ->
        val lwIssue = modelConverter.exportIssueToLionweb(issue, "$id-issue-$index")
        ClassifierInstanceUtils.addChild(lwCodebaseFile, "issues", lwIssue)
    }
    return lwCodebaseFile
}

fun <R : Node> convertCodebase(
    modelConverter: LionWebModelConverter,
    codebaseAccess: CodebaseAccess,
    languagesWeConsider: Set<String>,
    serialization: JsonSerialization
): Codebase<R> {
    return object : Codebase<R> {
        override val name: String
            get() = codebaseAccess.name

        override fun files(): Sequence<CodebaseFile<R>> {
            return codebaseAccess.files().map { fileIdentifier ->
                val serializedFile = codebaseAccess.retrieve(fileIdentifier)
                serialization.deserializeToNodes(serializedFile)[0]
            }.filter { serializedCodebaseFile ->
                val languageName = serializedCodebaseFile!!.getPropertyValueByName("language_name") as String
                languagesWeConsider.contains(languageName)
            }.map { serializedCodebaseFile ->
                deserialize(modelConverter, this, serializedCodebaseFile!!)
            }.asSequence()
        }

        override fun fileByRelativePath(relativePath: String): CodebaseFile<R>? {
            val fileIdentifier = codebaseAccess.fileByRelativePath(relativePath) ?: return null
            val serializedFile = codebaseAccess.retrieve(fileIdentifier) ?: throw IllegalStateException()
            val serializedCodebaseFile = serialization.deserializeToNodes(serializedFile)[0]
            return deserialize(modelConverter, this, serializedCodebaseFile)
        }
    }
}
