package de.solidblocks.rds.controller

import de.solidblocks.rds.base.Database
import de.solidblocks.rds.controller.api.ApiHttpServer
import de.solidblocks.rds.controller.configuration.RdsConfigurationManager
import de.solidblocks.rds.controller.controllers.ControllersManager
import de.solidblocks.rds.controller.instances.RdsInstancesManager
import de.solidblocks.rds.controller.instances.api.RdsInstancesApi
import de.solidblocks.rds.controller.model.repositories.*
import de.solidblocks.rds.controller.status.StatusManager
import de.solidblocks.rds.controller.model.status.StatusRepository
import de.solidblocks.rds.controller.providers.ProvidersManager
import de.solidblocks.rds.controller.providers.api.ProvidersApi
import mu.KotlinLogging
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider

class Controller(database: Database) {

    private val logger = KotlinLogging.logger {}

    private val rdsInstancesRepository = RdsInstancesRepository(database.dsl)

    private val rdsConfigurationRepository = RdsConfigurationRepository(database.dsl)

    private val providersRepository = ProvidersRepository(database.dsl)

    private val controllersRepository = ControllersRepository(database.dsl)

    private val logRepository = LogRepository(database.dsl)

    private val statusRepository = StatusRepository(database.dsl)

    private val rdsScheduler = RdsScheduler(database)

    private val controllersManager = ControllersManager(controllersRepository)

    private val statusManager = StatusManager(statusRepository)

    private val providersManager =
        ProvidersManager(providersRepository, rdsInstancesRepository, controllersManager, rdsScheduler, statusManager, logRepository)

    private val instancesManager =
        RdsInstancesManager(rdsInstancesRepository, providersManager, controllersManager, rdsScheduler, statusManager)

    private val configurationManager =
        RdsConfigurationManager(
            rdsConfigurationRepository,
            instancesManager,
            rdsScheduler,
            statusManager
        )

    private val rdsManager = RdsManager(database.dsl, instancesManager, configurationManager, statusManager)

    private val executor = DefaultLockingTaskExecutor(JdbcLockProvider(database.datasource))

    init {
        val httpServer = ApiHttpServer(port = 8080)

        val providersApi = ProvidersApi(httpServer, providersManager, statusManager)
        val instancesApi = RdsInstancesApi(httpServer, rdsManager)

        rdsScheduler.start()
    }

    fun stop() {
        rdsScheduler.stop()
    }
}
