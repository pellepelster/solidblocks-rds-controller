package de.solidblocks.rds.controller

import de.solidblocks.rds.base.Database
import de.solidblocks.rds.controller.providers.HetznerApi
import de.solidblocks.rds.test.TestDatabaseExtension
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import me.tomsdevsn.hetznercloud.HetznerCloudAPI
import org.assertj.core.api.Assertions
import org.awaitility.kotlin.await
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration

@ExtendWith(TestDatabaseExtension::class)
@EnabledIfEnvironmentVariable(named = "HCLOUD_TOKEN", matches = ".*")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ControllerIntegrationTest {

    private val hetznerApi = HetznerApi(System.getenv("HCLOUD_TOKEN"))

    private val hetznerCloudAPI = HetznerCloudAPI(System.getenv("HCLOUD_TOKEN"))

    var controller: Controller? = null

    @BeforeAll
    fun beforeAll() {
        cleanTestbed()
    }

    @AfterAll
    fun afterAll() {
        cleanTestbed()
        controller?.stop()
    }

    fun cleanTestbed() {
        Assertions.assertThat(hetznerApi.deleteAllSSHKeys()).isTrue
        Assertions.assertThat(hetznerApi.deleteAllVolumes()).isTrue

        hetznerCloudAPI.servers.servers.forEach {
            hetznerCloudAPI.deleteServer(it.id)
        }
    }

    @Test
    fun testManageProviders(database: Database) {
        controller = Controller(database)

        await.until {
            try {
                Given {
                    port(8080)
                } When {
                    get("/api/v1/providers")
                } Extract {
                    this.statusCode()
                } == 200
            } catch (e: Exception) {
                false
            }
        }

        Given {
            port(8080)
        } When {
            get("/api/v1/providers")
        } Then {
            statusCode(200)
            body("providers", empty<String>())
        }

        Given {
            port(8080)
            body(
                """
                {
                    "name": "provider1",
                    "apiKey": "provider1"
                }
                """.trimIndent()
            )
        } When {
            post(
                "/api/v1/providers"
            )
        } Then {
            statusCode(422)
            body("messages[0].attribute", equalTo("apiKey"))
            body("messages[0].code", equalTo("invalid"))
        }

        Given {
            port(8080)
            body(
                """
                {
                    "name": "provider1",
                    "apiKey": "${System.getenv("HCLOUD_TOKEN")}"
                }
                """.trimIndent()
            )
        } When {
            post(
                "/api/v1/providers"
            )
        } Then {
            statusCode(201)
            // body("provider.status", equalTo("UNKNOWN"))
        }

        val providerId: String = Given {
            port(8080)
        } When {
            get("/api/v1/providers")
        } Then {
            statusCode(200)
            body("providers", hasSize<String>(1))
            body("providers[0].name", equalTo("provider1"))
        } Extract {
            path("providers[0].id")
        }

        Given {
            port(8080)
        } When {
            get("/api/v1/providers/$providerId")
        } Then {
            statusCode(200)
            body("provider.name", equalTo("provider1"))
        }

        await.atMost(Duration.ofSeconds(120)).until {
            val name: String = Given {
                port(8080)
            } When {
                get("/api/v1/providers/$providerId")
            } Then {
                statusCode(200)
                body("provider.name", equalTo("provider1"))
            } Extract {
                path("provider.name")
            }

            name == "provider1"
        }

        val instanceId: String = Given {
            port(8080)
            body(
                """
                {
                    "name": "instance1",
                    "provider": "$providerId",
                    "username": "user1",
                    "password": "password"
                }
                """.trimIndent()
            )
        } When {
            post(
                "/api/v1/rds-instances"
            )
        } Then {
            statusCode(201)
            // body("rdsInstance.status", equalTo("UNKNOWN"))
        } Extract {
            path("rdsInstance.id")
        }

        await.atMost(Duration.ofSeconds(120)).until {
            val name: String = Given {
                port(8080)
            } When {
                get("/api/v1/rds-instances/$instanceId")
            } Then {
                statusCode(200)
            } Extract {
                path("rdsInstance.name")
            }

            name == "instance1"
        }
    }
}
