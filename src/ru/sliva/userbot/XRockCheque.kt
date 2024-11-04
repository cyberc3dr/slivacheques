package ru.sliva.userbot

import it.tdlight.jni.TdApi.GetInlineQueryResults
import it.tdlight.jni.TdApi.InlineQueryResultArticle
import it.tdlight.jni.TdApi.MessageText
import it.tdlight.jni.TdApi.SendInlineQueryResultMessage
import kotlinx.coroutines.future.await

/**
 * Class for creating cheques for xRocket Bot (@xRocket)
 *
 * @property id Cheque id
 */
class XRockCheque(private var id: String) : Cheque() {

    override suspend fun update() {
        logger.info("Updating $id...")

        val query = GetInlineQueryResults().apply {
            botUserId = X_ROCKET_ID
            query = id
        }

        val inlineResults = runCatching { client.send(query).await() }
            .getOrNull() ?: return

        val result = inlineResults.results
            .filterIsInstance<InlineQueryResultArticle>()
            .firstOrNull() ?: return drop()

        val sendResult = SendInlineQueryResultMessage().apply {
            chatId = client.me.id
            resultId = result.id
            queryId = inlineResults.inlineQueryId
        }

        val message = client.send(sendResult).await()

        client.send(message.delete()).await()

        val content = message.content as? MessageText ?: return

        val inline = "${result.title}\n${result.description}"

        val data = content.text.text


    }

    companion object {
        /**
         * xRocket Bot id (@xRocket)
         */
        const val X_ROCKET_ID = 5014831088

        private val client = Bot.client
        private val logger = Bot.logger
    }
}