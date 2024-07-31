package com.strumenta.kolasu.lionwebclient

import com.strumenta.kolasu.model.ReferenceValue
import com.strumenta.kolasu.model.SyntheticSource
import com.strumenta.kolasu.model.assignParents
import io.lionweb.lioncore.kotlin.getChildrenByContainmentName
import io.lionweb.lioncore.kotlin.repoclient.testing.AbstractRepoClientFunctionalTest
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.Test
import kotlin.test.assertEquals

@Testcontainers
class TodoFunctionalTest : AbstractRepoClientFunctionalTest() {
    @Test
    fun noPartitionsOnNewModelRepository() {
        val kolasuClient = KolasuClient(port = modelRepository!!.firstMappedPort)
        assertEquals(emptyList(), kolasuClient.getPartitionIDs())
    }

    @Test
    fun storePartitionAndGetItBack() {
        val kolasuClient = KolasuClient(port = modelRepository!!.firstMappedPort, debug = true)
        kolasuClient.registerLanguage(TodoStarLasuLanguageInstance)
        kolasuClient.registerLanguage(todoAccountLanguage)

        assertEquals(emptyList(), kolasuClient.getPartitionIDs())

        // We create an empty partition
        val todoAccount = TodoAccount("my-wonderful-partition")
        val expectedPartitionId = todoAccount.id!!
        // By default the partition IDs are derived from the source
        // todoAccount.source = SyntheticSource("my-wonderful-partition")
        kolasuClient.createPartition(todoAccount)

        val partitionIDs = kolasuClient.getPartitionIDs()
        assertEquals(listOf(todoAccount.id), partitionIDs)

        // Now we want to attach a tree to the existing partition
        val todoProject =
            TodoProject(
                "My errands list",
                mutableListOf(
                    Todo("Buy milk"),
                    Todo("Take the garbage out"),
                    Todo("Go for a walk"),
                ),
            )
        // todoProject.assignParents()

        todoProject.source = SyntheticSource("TODO Project A")
        val todoProjectID =
            kolasuClient.attachAST(
                todoProject,
                containerID = expectedPartitionId,
                containmentName = "projects",
            )

        // I can retrieve the entire partition
        // todoAccount.projects.add(todoProject)
        // todoAccount.assignParents()
        val retrievedTodoAccount = kolasuClient.getLionWebNode(expectedPartitionId)
        assertEquals(1, retrievedTodoAccount.getChildrenByContainmentName("projects").size)
        assertEquals(listOf(todoProjectID), retrievedTodoAccount.getChildrenByContainmentName("projects").map { it.id })

        // I can retrieve just a portion of that partition. In that case the parent of the root of the
        // subtree will appear null
        val expectedProjectId = kolasuClient.idFor(todoProject)
        assertEquals("synthetic_TODO_Project_A", expectedProjectId)
        val retrievedTodoProject = kolasuClient.getAST(expectedProjectId)
        assertEquals(
            TodoProject(
                "My errands list",
                mutableListOf(
                    Todo("Buy milk"),
                    Todo("Take the garbage out"),
                    Todo("Go for a walk"),
                ),
            ).apply { assignParents() },
            retrievedTodoProject,
        )
        assertEquals(null, retrievedTodoProject.parent)
    }

    @Test
    fun checkNodeIDs() {
        val kolasuClient = KolasuClient(port = modelRepository!!.firstMappedPort, debug = true)
        kolasuClient.registerLanguage(TodoStarLasuLanguageInstance)
        kolasuClient.registerLanguage(todoAccountLanguage)

        assertEquals(emptyList(), kolasuClient.getPartitionIDs())

        // We create an empty partition
        val todoAccount = TodoAccount("my-wonderful-partition")
        // By default the partition IDs are derived from the source
        // todoAccount.source = SyntheticSource("my-wonderful-partition")
        kolasuClient.createPartition(todoAccount)

        // Now we want to attach a tree to the existing partition
        val todoProject =
            TodoProject(
                "My errands list",
                mutableListOf(
                    Todo("Buy milk"),
                    Todo("Take the garbage out"),
                    Todo("Go for a walk"),
                ),
            )
        todoProject.assignParents()
        todoProject.source = SyntheticSource("MyProject")
        kolasuClient.attachAST(todoProject, todoAccount, containmentName = "projects")

        // assertEquals("partition_synthetic_my-wonderful-partition", kolasuClient.idFor(todoAccount))
        assertEquals("synthetic_MyProject", kolasuClient.idFor(todoProject))
        assertEquals("synthetic_MyProject_todos", kolasuClient.idFor(todoProject.todos[0]))
        assertEquals("synthetic_MyProject_todos_1", kolasuClient.idFor(todoProject.todos[1]))
        assertEquals("synthetic_MyProject_todos_2", kolasuClient.idFor(todoProject.todos[2]))
    }

    @Test
    fun sourceIsRetrievedCorrectly() {
        val kolasuClient = KolasuClient(port = modelRepository!!.firstMappedPort, debug = true)
        kolasuClient.registerLanguage(TodoStarLasuLanguageInstance)
        kolasuClient.registerLanguage(todoAccountLanguage)

        // We create an empty partition
        val todoAccount = TodoAccount("my-wonderful-partition")
        // todoAccount.source = SyntheticSource("my-wonderful-partition")
        val todoAccountId = kolasuClient.createPartition(todoAccount)

        val todoProject1 =
            TodoProject(
                "My errands list",
                mutableListOf(
                    Todo("Buy milk"),
                    Todo("garbage-out", "Take the garbage out"),
                    Todo("Go for a walk"),
                ),
            )
        todoProject1.assignParents()
        todoProject1.source = SyntheticSource("Project1")
        val todoProject1ID = kolasuClient.attachAST(todoProject1, todoAccount, containmentName = "projects")

        val todoProject2 =
            TodoProject(
                "My other errands list",
                mutableListOf(
                    Todo("BD", "Buy diary"),
                    Todo("WD", "Write in diary", ReferenceValue("BD")),
                    Todo("garbage-in", "Produce more garbage", ReferenceValue("garbage-out")),
                ),
            )
        todoProject2.assignParents()
        todoProject2.source = SyntheticSource("Project2")
        val todoProject2ID = kolasuClient.attachAST(todoProject2, todoAccount, containmentName = "projects")

        val retrievedPartition = kolasuClient.getLionWebNode(todoAccountId)

        // When retrieving the entire partition, the source should be set correctly, producing the right node id
        assertEquals(todoProject1ID, retrievedPartition.getChildrenByContainmentName("projects")[0].id)
        assertEquals(todoProject2ID, retrievedPartition.getChildrenByContainmentName("projects")[1].id)
    }
}
