package ru.sliva.userbot

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

/**
 * Abstract class for creating cheques
 *
 * @property cheques List of all cheques
 */
abstract class Cheque {

    init {
        cheques += this
    }

    /**
     * Drop cheque from list
     */
    open fun drop() {
        cheques -= this
    }

    /**
     * Update cheque
     */
    abstract suspend fun update()

    companion object {
        val cheques = mutableListOf<Cheque>()

        lateinit var job: Job

        /**
         * Launch daemon for updating cheques
         */
        fun launchDaemon() {
            job = GlobalScope.launch {
                delay(5.seconds)

                while(Bot.isRunning) {
                    cheques.forEach {
                        it.update()
                    }

                    delay(30.seconds)
                }
            }
        }
    }
}