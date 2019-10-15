package com.strumenta.kolasu.generation

import com.google.gson.GsonBuilder
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.Point
import com.strumenta.kolasu.model.Position
import com.strumenta.kolasu.model.processProperties
import com.strumenta.kolasu.validation.Error
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import com.sun.xml.internal.ws.addressing.EndpointReferenceUtil.transform
import javax.xml.transform.Transformer
import com.sun.org.apache.xerces.internal.impl.xs.opti.SchemaDOM.indent



class XMLGenerator {

    fun generateXML(root: Node): Document {
        val documentFactory = DocumentBuilderFactory.newInstance()
        val documentBuilder = documentFactory.newDocumentBuilder()
        val document = documentBuilder.newDocument()
        document.appendChild(root.toXML("root", document))
        return document
    }

    fun generateXML(result: com.strumenta.kolasu.Result<out Node>): Document {
        val documentFactory = DocumentBuilderFactory.newInstance()
        val documentBuilder = documentFactory.newDocumentBuilder()
        val document = documentBuilder.newDocument()
        val root = document.createElement("result")
        root.addListOfElements("errors", result.errors.map { it.toXML() }, document)
        root.addChildPossiblyEmpty("root", result.root, document)
        document.appendChild(root)
        return document
    }

    fun generateString(root: Node): String {
        val document = generateXML(root)
        return document.toXmlString()
    }

    private fun Document.toXmlString() : String {
        val  tf = TransformerFactory.newInstance()
        tf.setAttribute("indent-number", 4)
        val transformer = tf.newTransformer()
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")

        val writer = StringWriter()

        transformer.transform(DOMSource(this), StreamResult(writer))

        val xmlString = writer.buffer.toString();
        return xmlString
    }

    fun generateString(result: com.strumenta.kolasu.Result<out Node>): String {
        val document = generateXML(result)
        return document.toXmlString()
    }

    fun generateFile(root: Node, file: File) {
        File(file.toURI()).writeText(generateString(root))
    }

    fun generateFile(result: com.strumenta.kolasu.Result<out Node>, file: File) {
        File(file.toURI()).writeText(generateString(result))
    }
}

private fun Element.addChildPossiblyEmpty(role: String, node: Node?, document: Document) {
    if (node == null) {
        this.addNullChild(role, document)
    } else {
        this.addChild(node.toXML(role, document))
    }
}

private fun Element.addListOfNodes(listName: String, elements: Iterable<Node>, document: Document) {
    elements.forEach {
        addChild(it.toXML(listName, document))
    }
}

private fun Element.addListOfElements(listName: String, elements: Iterable<Element>, document: Document) {
    val listElement = document.createElement(listName)
    elements.forEach {
        listElement.addChild(it)
    }
    this.appendChild(listElement)
}

private fun Element.addChild(child: Element) {
    this.appendChild(child)
}

private fun Element.addNullChild(role: String, document : Document) {
    val element = document.createElement(role)
    this.appendChild(element)
}

private fun Node.toXML(role: String, document : Document): Element {
    val element = document.createElement(role)
    element.setAttribute("type", this.javaClass.simpleName)
    this.position?.let {
        element.addChild(it.toXML(document = document))
    }
    this.processProperties {
        if (it.value == null) {
            element.addNullChild(it.name, document)
        } else if (it.multiple) {
            if (it.provideNodes) {
                element.addListOfNodes(it.name, (it.value as Collection<*>).map { it as Node }, document)
            } else {
                element.addAttributesList(it.name, it.value as Collection<*>, document)
            }
        } else {
            if (it.provideNodes) {
                element.addChild((it.value as Node).toXML(it.name, document))
            } else {
                element.addAttribute(it.name, it.value, document)
            }
        }
    }
    return element
}

private fun Element.addAttribute(role: String, value: Any, document: Document) {
    this.setAttribute(role, value.toString())
}

private fun Element.addAttributesList(role: String, values: Collection<*>, document: Document) {
    TODO()
}

private fun Any?.toXML(): Element {
//    return when (this) {
//        null -> JsonNull.INSTANCE
//        is String -> JsonPrimitive(this)
//        is Number -> JsonPrimitive(this)
//        is Boolean -> JsonPrimitive(this)
//        else -> JsonPrimitive(this.toString())
//    }
    TODO()
}

private fun Error.toXML(): Element {
//    return jsonObject(
//        "type" to this.type.name,
//        "message" to this.message,
//        "position" to this.position?.toJson()
//    )
    TODO()
}

private fun Position.toXML(role: String = "position", document: Document): Element {
    val xmlNode = document.createElement(role)
    xmlNode.setAttribute("description", this.toString())
    xmlNode.addChild(this.start.toXML("start", document))
    xmlNode.addChild(this.end.toXML("end", document))
    return xmlNode
}

private fun Point.toXML(role: String, document: Document): Element {
    val xmlNode = document.createElement(role)
    xmlNode.setAttribute("line", this.line.toString())
    xmlNode.setAttribute("column", this.column.toString())
    return xmlNode
}
