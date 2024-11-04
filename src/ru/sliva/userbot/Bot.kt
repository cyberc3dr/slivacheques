package ru.sliva.userbot

import it.tdlight.Init
import it.tdlight.client.*
import it.tdlight.jni.TdApi.*
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Paths
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds

object Bot {

    private val clientFactory = SimpleTelegramClientFactory()

    lateinit var bot: SimpleTelegramClient
    lateinit var client: SimpleTelegramClient

    lateinit var clientJob: Job
    lateinit var botJob: Job

    private val apiToken = APIToken(1111, "REDACTED")

    var languageCode = "ru"

    private val settings: TDLibSettings
        get() {
            return TDLibSettings.create(apiToken).apply {
                deviceModel = "SlivaHosting"
                applicationVersion = "1.0"
                systemVersion = "4.16.30-vxCUSTOM"
                systemLanguageCode = languageCode
            }
        }

    var commandPrefix = "."

    var logger: Logger = LoggerFactory.getLogger(javaClass)

    var isRunning = false

    @JvmStatic
    fun main(args: Array<String>): Unit = runBlocking {
        Init.init()

        restart()
    }

    suspend fun restart() = coroutineScope {
        clientJob = launch {
            val settings = this@Bot.settings

            val sessionPath = Paths.get("main-session")
            settings.databaseDirectoryPath = sessionPath.resolve("data")
            settings.downloadedFilesDirectoryPath = sessionPath.resolve("downloads")

            val clientBuilder = clientFactory.builder(settings)

            val authenticationData = AuthenticationSupplier.consoleLogin()

            clientBuilder.addUpdateHandler(UpdateAuthorizationState::class.java) { update ->
                when (update.authorizationState) {
                    is AuthorizationStateReady -> {
                        logger.info("Userbot is ready!")
                        postInitUser()
                    }

                    is AuthorizationStateClosed -> logger.info("Closed user.")
                    is AuthorizationStateClosing -> logger.info("Closing user...")
                    is AuthorizationStateLoggingOut -> logger.info("Logging out user...")
                    else -> {}
                }
            }

            isRunning = true

            client = clientBuilder.build(authenticationData)
        }

//        botJob = launch {
//            val settings = this@Bot.settings
//
//            val sessionPath = Paths.get("bot-session")
//            settings.databaseDirectoryPath = sessionPath.resolve("data")
//            settings.downloadedFilesDirectoryPath = sessionPath.resolve("downloads")
//
//            val clientBuilder = clientFactory.builder(settings)
//
//            val authenticationData = AuthenticationSupplier.bot("REDACTED")
//
//            clientBuilder.addUpdateHandler(UpdateAuthorizationState::class.java) { update ->
//                when (update.authorizationState) {
//                    is AuthorizationStateReady -> {
//                        logger.info("Bot is ready!")
//                        postInitBot()
//                    }
//
//                    is AuthorizationStateClosed -> logger.info("Closed bot.")
//                    is AuthorizationStateClosing -> logger.info("Closing bot...")
//                    is AuthorizationStateLoggingOut -> logger.info("Logging out bot...")
//                    else -> {}
//                }
//            }
//
//            bot = clientBuilder.build(authenticationData)
//        }
    }

    private suspend fun stop(message: Message) {
        isRunning = false

        ChartManager.chartJob.cancel()
        Cheque.job.cancel()

        client.send(message.edit("Выключаем бота..."))

        bot.closeAsync().await()
        botJob.cancel()

        delay(2.seconds)
        client.send(message.edit("Бот выключен.")).await()

        delay(2.seconds)
        client.send(message.edit("Выключаем клиент...")).await()

        client.closeAsync().await()
        clientJob.cancel()
    }

    private suspend fun restart(message: Message) {
        stop(message)

        restart()

        delay(5.seconds)

        client.send(message.edit("Мы снова готовы к работе.")).await()
    }

    fun postInitUser() {
        ChartManager.launchDaemon(client)
        Cheque.launchDaemon()

        client.addUserBotCommandHandler("stop") { message, _ ->
            logger.info("Received stop command, shutting down...")

            stop(message)

            exitProcess(0)
        }

        client.addUserBotCommandHandler("restart") { message, _ ->
            logger.info("Received restart command, restarting...")

            restart(message)
        }

        client.addUserBotCommandHandler("changelang") { message, args ->
            if(args.isNotEmpty()) {
                val locale = args[0]

                client.send(message.edit("Изменяем локаль на $locale")).await()

                languageCode = locale
            }
        }

        client.addUserBotCommandHandler("testcoro") { message, _ ->
            client.send(message.edit("Тест корутинс, ждем 5 сек")).await()

            delay(5.seconds)

            for (i in 5 downTo 1) {
                client.send(message.edit("$i")).await()
                delay(1.seconds)
            }

            client.send(message.edit("Done")).await()
        }

        client.addUserBotCommandHandler("testinline") { message, _ ->
            val req = GetInlineQueryResults().apply {
                botUserId = 5014831088
                query = "mci_57VAoOwxKeXCVYP"
            }

            val inlineResults = client.send(req).await()

            val result = inlineResults.results
                .filterIsInstance<InlineQueryResultArticle>()
                .firstOrNull()

            if (result != null) {
                client.send(message.edit(result.description)).await()
            }
        }

        client.addUserBotCommandHandler("sendpic") { message, _ ->
            val chat = message.chatId

            delay(2.seconds)

            client.send(message.delete()).await()

            val req = SendMessage().apply {
                chatId = chat
                inputMessageContent = InputMessagePhoto().apply {
                    photo = InputFileRemote("https://www.w3.org/Graphics/PNG/text2.png")
                }
            }

            client.send(req).await()
        }

        client.addUserBotCommandHandler("checkcrypto") { message, _ ->
            val chat = message.chatId

            delay(2.seconds)

            client.send(message.delete()).await()

            val queryResult = GetInlineQueryResults().apply {
                botUserId = 5014831088
                query = "1 TON = "
            }

            val inlineResults = client.send(queryResult).await()

            val result = inlineResults.results
                .filterIsInstance<InlineQueryResultArticle>()
                .firstOrNull()

            if(result != null) {
                val sendResult = SendInlineQueryResultMessage().apply {
                    chatId = -1
                    resultId = result.id
                    queryId = inlineResults.inlineQueryId
                }

                val someMessage = client.send(sendResult).await()

                val content = someMessage.content

                if(content is MessageText) {
                    val req = SendMessage().apply {
                        chatId = chat
                        inputMessageContent = InputMessageText().apply {
                            text = FormattedText("Parsed: ${content.text.text}", emptyArray())
                        }
                    }

                    client.send(req).await()
                }

                delay(1.seconds)

                client.send(someMessage.delete()).await()
            }
        }

        XRockCheque("mci_57VAoOwxKeXCVYP")
    }

    fun postInitBot() {
        bot.addBotCommandHandler("testbot") { message ->
            val req = SendMessage().apply {
                chatId = message.chatId
                inputMessageContent = InputMessageText().apply {
                    text = FormattedText("It works!", emptyArray())
                }
            }

            bot.send(req).await()
        }
    }

    fun initBot(clientFactory: SimpleTelegramClientFactory, settings: TDLibSettings) {
        val sessionPath = Paths.get("test-session")
        settings.databaseDirectoryPath = sessionPath.resolve("data")
        settings.downloadedFilesDirectoryPath = sessionPath.resolve("downloads")

        val clientBuilder = clientFactory.builder(settings)

        val authenticationData = AuthenticationSupplier.bot("REDACTED")

        clientBuilder.addUpdateHandler(UpdateAuthorizationState::class.java) { update ->
            when (update.authorizationState) {
                is AuthorizationStateReady -> println("We're ready!")
                is AuthorizationStateClosed -> println("Closed.")
                is AuthorizationStateClosing -> println("Closing...")
                is AuthorizationStateLoggingOut -> println("Logging out...")
                else -> {}
            }
        }

        clientBuilder.addCommandHandler<UpdateNewMessage>("test") { chat, sender, args ->
            val req = SendMessage()

            req.chatId = chat.id

            println(chat.id)

            val ctx = InputMessageText()
            ctx.text = FormattedText("Автопарсинг аргументов: $args", emptyArray())
            req.inputMessageContent = ctx

            bot.sendMessage(req, true)
        }

        clientBuilder.addUpdateHandler(UpdateNewMessage::class.java) { update ->
            val content = update.message.content

            if (content is MessageText) {
                println(content.text.text)
            }
        }

        bot = clientBuilder.build(authenticationData)
    }
}