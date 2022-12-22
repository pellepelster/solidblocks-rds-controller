package de.solidblocks.rds.controller

import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.task.AbstractTask
import com.github.kagkarlsson.scheduler.task.Task
import com.github.kagkarlsson.scheduler.task.TaskInstance
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import de.solidblocks.rds.base.Database
import de.solidblocks.rds.controller.utils.JacksonSerializer
import java.time.Duration
import java.time.Instant

class RdsScheduler(private val database: Database) {

    private val onetimeTasks = ArrayList<Task<out Any>>()
    private val recurringTasks = ArrayList<RecurringTask<out Any>>()

    private var scheduler: Scheduler? = null

    fun start() {
        scheduler = Scheduler
            .create(database.datasource, onetimeTasks)
            .startTasks(recurringTasks)
            .deleteUnresolvedAfter(Duration.ofSeconds(60))
            .serializer(JacksonSerializer())
            .threads(5).build()
        scheduler!!.start()
    }

    fun stop() {
        scheduler?.stop()
    }

    fun scheduleTask(task: TaskInstance<out Any>) {

        if (scheduler == null) {
            throw RuntimeException("scheduler is not started")
        }

        scheduler!!.schedule(task, Instant.now().plusSeconds(5))
    }

    fun addOneTimeTask(task: AbstractTask<out Any>) {

        if (scheduler != null && scheduler!!.schedulerState.isStarted) {
            throw RuntimeException("could not add new task definition, scheduler is already running")
        }

        onetimeTasks.add(task)
    }

    fun addRecurringTask(task: RecurringTask<out Any>) {

        if (scheduler != null && scheduler!!.schedulerState.isStarted) {
            throw RuntimeException("could not add new task definition, scheduler is already running")
        }

        recurringTasks.add(task)
    }
}
