import com.strumenta.kolasu.lionweb.LWNode
import com.strumenta.kolasu.lionwebclient.FunctionalTestBuildConfig
import io.lionweb.lioncore.java.utils.ModelComparator
import io.lionweb.lioncore.kotlin.repoclient.LionWebClient
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.output.OutputFrame
import org.testcontainers.images.builder.ImageFromDockerfile
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.function.Consumer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

private const val DB_CONTAINER_PORT = 5432

@Testcontainers
abstract class AbstractFunctionalTest {
    @JvmField
    var db: PostgreSQLContainer<*>? = null

    @JvmField
    var modelRepository: GenericContainer<*>? = null

    @BeforeTest
    fun setup() {
        val network = Network.newNetwork()
        db =
            PostgreSQLContainer("postgres:16.1")
                .withNetwork(network)
                .withNetworkAliases("mypgdb")
                .withUsername("postgres")
                .withPassword("lionweb")
                .withExposedPorts(DB_CONTAINER_PORT).apply {
                    this.logConsumers =
                        listOf(
                            object : Consumer<OutputFrame> {
                                override fun accept(t: OutputFrame) {
                                    println("DB: ${t.utf8String.trimEnd()}")
                                }
                            },
                        )
                }
        db!!.start()
        val dbPort = db!!.firstMappedPort
        org.testcontainers.Testcontainers.exposeHostPorts(dbPort)
        modelRepository =
            GenericContainer(
                ImageFromDockerfile()
                    .withFileFromClasspath("Dockerfile", "lionweb-repository-Dockerfile")
                    .withBuildArg("lionwebRepositoryCommitId", FunctionalTestBuildConfig.LIONWEB_REPOSITORY_COMMIT_ID),
            )
                .dependsOn(db)
                .withNetwork(network)
                .withEnv("PGHOST", "mypgdb")
                .withEnv("PGPORT", DB_CONTAINER_PORT.toString())
                .withEnv("PGUSER", "postgres")
                .withEnv("PGDB", "lionweb_test")
                .withExposedPorts(3005).apply {
                    this.logConsumers =
                        listOf(
                            object : Consumer<OutputFrame> {
                                override fun accept(t: OutputFrame) {
                                    println("MODEL REPO: ${t.utf8String.trimEnd()}")
                                }
                            },
                        )
                }
        modelRepository!!.withCommand()
        modelRepository!!.start()

        // Initialization may change in the future (see https://github.com/LionWeb-io/lionweb-repository/issues/61)
        val client = LionWebClient(port = modelRepository!!.firstMappedPort)
        // We need to create the database
        client.createDatabase()
        // We then need to create a default database
        client.createRepository(history = false)
    }

    @AfterTest
    fun teardown() {
        modelRepository!!.stop()
    }

    fun assertLWTreesAreEqual(
        a: LWNode,
        b: LWNode,
    ) {
        val comparison = ModelComparator().compare(a, b)
        assert(comparison.areEquivalent()) {
            "Differences between $a and $b: $comparison"
        }
    }
}
