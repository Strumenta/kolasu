package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.testing.assertASTsAreEqual
import io.lionweb.lioncore.java.serialization.JsonSerialization
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class LionWebModelImporterTest {

    @Test
    fun importSimpleModel() {
        val lwSerialized = """{
  "serializationFormatVersion": "1",
  "languages": [
    {
      "version": "1",
      "key": "com.strumenta.SimpleLang"
    }
  ],
  "nodes": [
    {
      "id": "UNKNOWN_SOURCE-root",
      "concept": {
        "language": "com.strumenta.SimpleLang",
        "version": "1",
        "key": "com.strumenta.SimpleLang-SimpleRoot"
      },
      "properties": [
        {
          "property": {
            "language": "com.strumenta.SimpleLang",
            "version": "1",
            "key": "id"
          },
          "value": "12345"
        }
      ],
      "children": [
        {
          "containment": {
            "language": "com.strumenta.SimpleLang",
            "version": "1",
            "key": "children"
          },
          "children": [
            "UNKNOWN_SOURCE-root",
            "UNKNOWN_SOURCE-root",
            "UNKNOWN_SOURCE-root"
          ]
        }
      ],
      "references": [],
      "parent": null
    },
    {
      "id": "UNKNOWN_SOURCE-root",
      "concept": {
        "language": "com.strumenta.SimpleLang",
        "version": "1",
        "key": "com.strumenta.SimpleLang-SimpleNodeA"
      },
      "properties": [
        {
          "property": {
            "language": null,
            "version": null,
            "key": null
          },
          "value": "A1"
        }
      ],
      "children": [
        {
          "containment": {
            "language": "com.strumenta.SimpleLang",
            "version": "1",
            "key": "child"
          },
          "children": []
        }
      ],
      "references": [
        {
          "reference": {
            "language": "com.strumenta.SimpleLang",
            "version": "1",
            "key": "ref"
          },
          "targets": [
            {
              "resolveInfo": "A1",
              "reference": "UNKNOWN_SOURCE-root"
            }
          ]
        }
      ],
      "parent": "UNKNOWN_SOURCE-root"
    },
    {
      "id": "UNKNOWN_SOURCE-root",
      "concept": {
        "language": "com.strumenta.SimpleLang",
        "version": "1",
        "key": "com.strumenta.SimpleLang-SimpleNodeB"
      },
      "properties": [
        {
          "property": {
            "language": "com.strumenta.SimpleLang",
            "version": "1",
            "key": "value"
          },
          "value": "some magic value"
        }
      ],
      "children": [],
      "references": [],
      "parent": "UNKNOWN_SOURCE-root"
    },
    {
      "id": "UNKNOWN_SOURCE-root",
      "concept": {
        "language": "com.strumenta.SimpleLang",
        "version": "1",
        "key": "com.strumenta.SimpleLang-SimpleNodeA"
      },
      "properties": [
        {
          "property": {
            "language": null,
            "version": null,
            "key": null
          },
          "value": "A3"
        }
      ],
      "children": [
        {
          "containment": {
            "language": "com.strumenta.SimpleLang",
            "version": "1",
            "key": "child"
          },
          "children": [
            "UNKNOWN_SOURCE-root"
          ]
        }
      ],
      "references": [
        {
          "reference": {
            "language": "com.strumenta.SimpleLang",
            "version": "1",
            "key": "ref"
          },
          "targets": [
            {
              "resolveInfo": "A1",
              "reference": "UNKNOWN_SOURCE-root"
            }
          ]
        }
      ],
      "parent": "UNKNOWN_SOURCE-root"
    },
    {
      "id": "UNKNOWN_SOURCE-root",
      "concept": {
        "language": "com.strumenta.SimpleLang",
        "version": "1",
        "key": "com.strumenta.SimpleLang-SimpleNodeB"
      },
      "properties": [
        {
          "property": {
            "language": "com.strumenta.SimpleLang",
            "version": "1",
            "key": "value"
          },
          "value": "some magic value"
        }
      ],
      "children": [],
      "references": [],
      "parent": "UNKNOWN_SOURCE-root"
    }
  ]
}"""
        val lwAST = JsonSerialization.getStandardSerialization().unserializeToNodes(lwSerialized).first()
        val kAST = LionWebModelImporter().import(lwAST)

        val a1 = SimpleNodeA("A1", ReferenceByName("A1"), null)
        a1.ref.referred = a1
        val b2 = SimpleNodeB("some magic value")
        val a3 = SimpleNodeA("A3", ReferenceByName("A1", a1), b2)
        val expectedAST = SimpleRoot(
            12345,
            mutableListOf(
                a1, b2, a3
            )
        )

        assertASTsAreEqual(expectedAST, kAST)
    }
}
