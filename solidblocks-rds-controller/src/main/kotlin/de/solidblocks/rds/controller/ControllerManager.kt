package de.solidblocks.rds.controller

import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.task.ExecutionContext
import com.github.kagkarlsson.scheduler.task.TaskInstance
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay
import de.solidblocks.rds.base.Database
import de.solidblocks.rds.controller.api.ApiHttpServer
import de.solidblocks.rds.controller.instances.RdsInstancesManager
import de.solidblocks.rds.controller.instances.api.RdsInstancesApi
import de.solidblocks.rds.controller.model.ProvidersRepository
import de.solidblocks.rds.controller.model.RdsInstancesRepository
import de.solidblocks.rds.controller.providers.ProvidersManager
import de.solidblocks.rds.controller.providers.api.ProvidersApi
import mu.KotlinLogging
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor
import net.javacrumbs.shedlock.core.LockConfiguration
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider
import java.time.Duration
import java.time.Instant

class ControllerManager(database: Database) {

    private val logger = KotlinLogging.logger {}

    private val rdsInstancesManager = RdsInstancesManager(RdsInstancesRepository(database.dsl))
    private val providersManager = ProvidersManager(ProvidersRepository(database.dsl), rdsInstancesManager)

    val executor = DefaultLockingTaskExecutor(JdbcLockProvider(database.datasource))

    private var provisionerApplyTask = Tasks.recurring("provisioner-apply-task", FixedDelay.ofSeconds(30))
        .execute { _: TaskInstance<Void>, _: ExecutionContext ->

            executor.executeWithLock<Any>(
                {
                    providersManager.apply()
                },
                LockConfiguration(
                    Instant.now(), "global-apply-task", Duration.ofSeconds(60), Duration.ofSeconds(5)
                )
            )
        }

    private val scheduler = Scheduler.create(database.datasource).startTasks(provisionerApplyTask)
        .deleteUnresolvedAfter(Duration.ofSeconds(60)).threads(5).build()

    init {
        scheduler.start()

        val httpServer = ApiHttpServer(port = 8080)
        val providersApi = ProvidersApi(httpServer, providersManager)
        val rdsInstancesApi = RdsInstancesApi(httpServer, rdsInstancesManager)
    }
}
