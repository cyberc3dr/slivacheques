package ru.sliva.userbot

import it.tdlight.client.*
import it.tdlight.jni.TdApi.*
import kotlinx.coroutines.*

fun <T : Update> SimpleTelegramClient.addSuspendUpdateHandler(
    javaClass: Class<T>,
    block: suspend CoroutineScope.(T) -> Unit
) {
    addUpdateHandler(javaClass) { update ->
        GlobalScope.launch(Dispatchers.Default) {
            block.invoke(this, update)
        }
    }
}

fun SimpleTelegramClient.addBotCommandHandler(
    commandName: String,
    block: suspend CoroutineScope.(Message) -> Unit
) {
    addSuspendUpdateHandler(UpdateNewMessage::class.java) { update ->
        val message = update.message

        val content = message.content

        val sender = message.senderId

        if (content is MessageText &&
            content.text.text.startsWith("/$commandName", ignoreCase = true) &&
            sender is MessageSenderUser &&
            me.id != sender.userId
        ) {
            block.invoke(this, message)
        }
    }
}

fun SimpleTelegramClient.addUserBotCommandHandler(
    commandName: String,
    block: suspend CoroutineScope.(Message, List<String>) -> Unit
) {
    addSuspendUpdateHandler(UpdateNewMessage::class.java) { update ->
        val message = update.message

        val content = message.content

        val sender = message.senderId

        if (content is MessageText &&
            content.text.text.startsWith("${Bot.commandPrefix}$commandName", ignoreCase = true) &&
            sender is MessageSenderUser &&
            me.id == sender.userId
        ) {
            val args = content.text.text.split(" ").drop(1)

            block.invoke(this, message, args)
        }
    }
}

fun Message.edit(newText: String, vararg entities: TextEntity) : EditMessageText {
    return EditMessageText().apply {
        chatId = this@edit.chatId
        messageId = id
        inputMessageContent = InputMessageText().apply {
            text = FormattedText(newText, entities)
        }
    }
}

fun Message.delete() : DeleteMessages {
    return DeleteMessages().apply {
        chatId = this@delete.chatId
        messageIds = longArrayOf(id)
    }
}