package de.solidblocks.rds.controller.controllers

import de.solidblocks.rds.base.Utils
import de.solidblocks.rds.controller.model.Constants.CA_CLIENT_PRIVATE_KEY
import de.solidblocks.rds.controller.model.Constants.CA_CLIENT_PUBLIC_KEY
import de.solidblocks.rds.controller.model.Constants.CA_SERVER_PRIVATE_KEY
import de.solidblocks.rds.controller.model.Constants.CA_SERVER_PUBLIC_KEY
import de.solidblocks.rds.controller.model.controllers.ControllerEntity
import de.solidblocks.rds.controller.model.controllers.ControllersRepository
import mu.KotlinLogging
import java.util.*

class ControllersManager(private val repository: ControllersRepository) {

    private val logger = KotlinLogging.logger {}

    private val DEFAULT_CONTROLLER_NAME = "default"

    init {
        ensureDefaultController()
    }

    private fun ensureDefaultController() {
        if (!repository.exists(DEFAULT_CONTROLLER_NAME)) {

            val serverCa = Utils.generateCAKeyPAir()
            val clientCa = Utils.generateCAKeyPAir()

            repository.create(
                DEFAULT_CONTROLLER_NAME,
                mapOf(
                    CA_SERVER_PRIVATE_KEY to serverCa.privateKey,
                    CA_SERVER_PUBLIC_KEY to serverCa.publicKey,

                    CA_CLIENT_PRIVATE_KEY to clientCa.privateKey,
                    CA_CLIENT_PUBLIC_KEY to clientCa.publicKey,
                )
            )
        }
    }

    fun defaultController(): ControllerEntity {
        ensureDefaultController()
        return repository.read(DEFAULT_CONTROLLER_NAME)!!
    }

    fun readInternal(id: UUID) = repository.read(id)
}
