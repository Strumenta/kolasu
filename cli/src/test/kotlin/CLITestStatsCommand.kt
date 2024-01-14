package com.strumenta.kolasu.cli

import com.strumenta.kolasu.parsing.ParsingResult
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile
import kotlin.test.assertEquals

class CLITestStatsCommand {
    @Test
    fun runStatsOnSimpleFile() {
        val myDir = createTempDirectory()
        val myFile = createTempFile(myDir, "myfile.mylang")
        myDir.toFile().deleteOnExit()
        myFile.toFile().deleteOnExit()

        val parserInstantiator = { file: File ->
            MyDummyParser().apply {
                expectedResults[myFile.toFile()] =
                    ParsingResult(
                        emptyList(),
                        MyCompilationUnit(
                            mutableListOf(
                                MyEntityDecl("Entity1", mutableListOf()),
                                MyEntityDecl(
                                    "Entity2",
                                    mutableListOf(
                                        MyFieldDecl("f1"),
                                        MyFieldDecl("f2"),
                                        MyFieldDecl("f3"),
                                    ),
                                ),
                            ),
                        ),
                    )
            }
        }
        val console = CapturingCliktConsole()
        val cliTool = CLITool(parserInstantiator, console)
        cliTool.parse(arrayOf("stats", myFile.toString()))
        assertEquals(
            """== Stats ==

 [Did processing complete?]
  files processed         : 1
     processing failed    : 0
     processing completed : 1

 [Did processing complete successfully?]
  processing completed with errors    : 0
  processing completed without errors : 1
  total number of errors              : 0""",
            console.stdOutput.trim(),
        )
        assertEquals("", console.errOutput)
    }

    @Test
    fun runStatsOnSimpleFileCsv() {
        val myDir = createTempDirectory()
        val myFile = createTempFile(myDir, "myfile.mylang")
        myDir.toFile().deleteOnExit()
        myFile.toFile().deleteOnExit()

        val parserInstantiator = { file: File ->
            MyDummyParser().apply {
                expectedResults[myFile.toFile()] =
                    ParsingResult(
                        emptyList(),
                        MyCompilationUnit(
                            mutableListOf(
                                MyEntityDecl("Entity1", mutableListOf()),
                                MyEntityDecl(
                                    "Entity2",
                                    mutableListOf(
                                        MyFieldDecl("f1"),
                                        MyFieldDecl("f2"),
                                        MyFieldDecl("f3"),
                                    ),
                                ),
                            ),
                        ),
                    )
            }
        }
        val console = CapturingCliktConsole()
        val cliTool = CLITool(parserInstantiator, console)
        cliTool.parse(arrayOf("stats", myFile.toString(), "--csv"))
        assertEquals(
            """Saving Global Stats to global-stats.csv""",
            console.stdOutput.trim(),
        )
        assertEquals("", console.errOutput)

        val globalStatsFile = File("global-stats.csv")
        assert(globalStatsFile.exists())
        assertEquals(
            """Key,Value
filesProcessed,1
filesWithExceptions,0
filesProcessedSuccessfully,1
filesWithErrors,0
filesWithoutErrors,1
totalErrors,0
""".lines(),
            globalStatsFile.readText().lines(),
        )
        globalStatsFile.delete()
    }

    @Test
    fun runStatsOnSimpleFileNoStats() {
        val myDir = createTempDirectory()
        val myFile = createTempFile(myDir, "myfile.mylang")
        myDir.toFile().deleteOnExit()
        myFile.toFile().deleteOnExit()

        val parserInstantiator = { file: File ->
            MyDummyParser().apply {
                expectedResults[myFile.toFile()] =
                    ParsingResult(
                        emptyList(),
                        MyCompilationUnit(
                            mutableListOf(
                                MyEntityDecl("Entity1", mutableListOf()),
                                MyEntityDecl(
                                    "Entity2",
                                    mutableListOf(
                                        MyFieldDecl("f1"),
                                        MyFieldDecl("f2"),
                                        MyFieldDecl("f3"),
                                    ),
                                ),
                            ),
                        ),
                    )
            }
        }
        val console = CapturingCliktConsole()
        val cliTool = CLITool(parserInstantiator, console)
        cliTool.parse(arrayOf("stats", "--no-stats", myFile.toString()))
        assertEquals("""""", console.stdOutput.trim())
        assertEquals("", console.errOutput)
    }

    @Test
    fun runStatsOnSimpleFileNodeStats() {
        val myDir = createTempDirectory()
        val myFile = createTempFile(myDir, "myfile.mylang")
        myDir.toFile().deleteOnExit()
        myFile.toFile().deleteOnExit()

        val parserInstantiator = { file: File ->
            MyDummyParser().apply {
                expectedResults[myFile.toFile()] =
                    ParsingResult(
                        emptyList(),
                        MyCompilationUnit(
                            mutableListOf(
                                MyEntityDecl("Entity1", mutableListOf()),
                                MyEntityDecl(
                                    "Entity2",
                                    mutableListOf(
                                        MyFieldDecl("f1"),
                                        MyFieldDecl("f2"),
                                        MyFieldDecl("f3"),
                                    ),
                                ),
                            ),
                        ),
                    )
            }
        }
        val console = CapturingCliktConsole()
        val cliTool = CLITool(parserInstantiator, console)
        cliTool.parse(arrayOf("stats", "--no-stats", "--node-stats", myFile.toString()))
        assertEquals(
            """== Node Stats ==

  com.strumenta.kolasu.cli.MyCompilationUnit        : 1
  com.strumenta.kolasu.cli.MyEntityDecl             : 2
  com.strumenta.kolasu.cli.MyFieldDecl              : 3

  total number of nodes                             : 6""",
            console.stdOutput.trim(),
        )
        assertEquals("", console.errOutput)
    }

    @Test
    fun runNodeStatsOnSimpleFileCsv() {
        val myDir = createTempDirectory()
        val myFile = createTempFile(myDir, "myfile.mylang")
        myDir.toFile().deleteOnExit()
        myFile.toFile().deleteOnExit()

        val parserInstantiator = { file: File ->
            MyDummyParser().apply {
                expectedResults[myFile.toFile()] =
                    ParsingResult(
                        emptyList(),
                        MyCompilationUnit(
                            mutableListOf(
                                MyEntityDecl("Entity1", mutableListOf()),
                                MyEntityDecl(
                                    "Entity2",
                                    mutableListOf(
                                        MyFieldDecl("f1"),
                                        MyFieldDecl("f2"),
                                        MyFieldDecl("f3"),
                                    ),
                                ),
                            ),
                        ),
                    )
            }
        }
        val console = CapturingCliktConsole()
        val cliTool = CLITool(parserInstantiator, console)
        cliTool.parse(arrayOf("stats", myFile.toString(), "--no-stats", "--node-stats", "--csv"))
        assertEquals(
            """Saving Node Stats to node-stats.csv""",
            console.stdOutput.trim(),
        )
        assertEquals("", console.errOutput)

        val nodeStatsFile = File("node-stats.csv")
        assert(nodeStatsFile.exists())
        assertEquals(
            """Key,Value
com.strumenta.kolasu.cli.MyCompilationUnit,1
com.strumenta.kolasu.cli.MyEntityDecl,2
com.strumenta.kolasu.cli.MyFieldDecl,3
total,6
""".lines(),
            nodeStatsFile.readText().lines(),
        )
        nodeStatsFile.delete()
    }

    @Test
    fun runNodeStatsOnSimpleFileCsvWithSimpleNames() {
        val myDir = createTempDirectory()
        val myFile = createTempFile(myDir, "myfile.mylang")
        myDir.toFile().deleteOnExit()
        myFile.toFile().deleteOnExit()

        val parserInstantiator = { file: File ->
            MyDummyParser().apply {
                expectedResults[myFile.toFile()] =
                    ParsingResult(
                        emptyList(),
                        MyCompilationUnit(
                            mutableListOf(
                                MyEntityDecl("Entity1", mutableListOf()),
                                MyEntityDecl(
                                    "Entity2",
                                    mutableListOf(
                                        MyFieldDecl("f1"),
                                        MyFieldDecl("f2"),
                                        MyFieldDecl("f3"),
                                    ),
                                ),
                            ),
                        ),
                    )
            }
        }
        val console = CapturingCliktConsole()
        val cliTool = CLITool(parserInstantiator, console)
        cliTool.parse(arrayOf("stats", myFile.toString(), "--no-stats", "--node-stats", "--simple-names", "--csv"))
        assertEquals(
            """Saving Node Stats to node-stats.csv""",
            console.stdOutput.trim(),
        )
        assertEquals("", console.errOutput)

        val nodeStatsFile = File("node-stats.csv")
        assert(nodeStatsFile.exists())
        assertEquals(
            """Key,Value
MyCompilationUnit,1
MyEntityDecl,2
MyFieldDecl,3
total,6
""".lines(),
            nodeStatsFile.readText().lines(),
        )
        nodeStatsFile.delete()
    }

    @Test
    fun runStatsOnSimpleFileNodeStatsSimpleNames() {
        val myDir = createTempDirectory()
        val myFile = createTempFile(myDir, "myfile.mylang")
        myDir.toFile().deleteOnExit()
        myFile.toFile().deleteOnExit()

        val parserInstantiator = { file: File ->
            MyDummyParser().apply {
                expectedResults[myFile.toFile()] =
                    ParsingResult(
                        emptyList(),
                        MyCompilationUnit(
                            mutableListOf(
                                MyEntityDecl("Entity1", mutableListOf()),
                                MyEntityDecl(
                                    "Entity2",
                                    mutableListOf(
                                        MyFieldDecl("f1"),
                                        MyFieldDecl("f2"),
                                        MyFieldDecl("f3"),
                                    ),
                                ),
                            ),
                        ),
                    )
            }
        }
        val console = CapturingCliktConsole()
        val cliTool = CLITool(parserInstantiator, console)
        cliTool.parse(arrayOf("stats", "--no-stats", "--node-stats", "-sn", myFile.toString()))
        assertEquals(
            """== Node Stats ==

  MyCompilationUnit        : 1
  MyEntityDecl             : 2
  MyFieldDecl              : 3

  total number of nodes    : 6""",
            console.stdOutput.trim(),
        )
        assertEquals("", console.errOutput)
    }
}
