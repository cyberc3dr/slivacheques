package ru.sliva.userbot

import it.tdlight.client.SimpleTelegramClient
import it.tdlight.jni.TdApi.GetInlineQueryResults
import it.tdlight.jni.TdApi.InlineQueryResultArticle
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import ru.sliva.userbot.ChartManager.CryptoType.Companion.toCryptoType
import kotlin.time.Duration.Companion.seconds

object ChartManager {

    private val charts = mutableMapOf<CryptoType, Double>()

    lateinit var chartJob: Job

    fun launchDaemon(client: SimpleTelegramClient) {
        client.addUserBotCommandHandler("printcharts") { message, _ ->
            val chart = charts.entries
                .sortedByDescending { it.value }
                .take(15).joinToString("\n") { "1 ${it.key} = ${it.value.round(2).toPlainString()}$" }

            client.send(message.edit(chart)).await()
        }

        client.addUserBotCommandHandler("getchart") {message, args ->
            val sum = args[0].toDoubleOrNull() ?: run {
                client.send(message.edit("Введите сумму")).await()
                return@addUserBotCommandHandler
            }

            val cryptoType = args[1].toCryptoType() ?: run {
                client.send(message.edit("Введите валидную крипту")).await()
                return@addUserBotCommandHandler
            }

            val chart = getCryptoValue(sum, cryptoType)

            if(chart != null) {
                client.send(message.edit("$sum $cryptoType = ${chart.toPlainString()}$")).await()
            }
        }

        chartJob = GlobalScope.launch {
            delay(5.seconds)

            while(Bot.isRunning) {
                CryptoType.entries.forEach { crypto ->
                    val req = GetInlineQueryResults().apply {
                        botUserId = 5014831088
                        query = "1 $crypto = "
                    }

                    val inlineResults = runCatching { client.send(req).await() }
                        .getOrNull() ?: run {
                        Bot.logger.info("Some error occurred for $crypto")
                        return@forEach
                    }

                    val result = inlineResults.results
                        .filterIsInstance<InlineQueryResultArticle>()
                        .firstOrNull()

                    if(result != null) {
                        val chart = result.title
                            .split(" = ")[1]
                            .removeSuffix("$")
                            .replace(" ", "")
                            .toDoubleOrNull()

                        chart?.let { charts[crypto] = chart }
                    }
                }

                Bot.logger.info("Done scanning cryptos.")

                delay(30.seconds)
            }
        }
    }

    fun getCryptoValue(sum: Double, crypto: CryptoType) : Double? = charts[crypto]?.times(sum)?.round(2)?.toDouble()

    enum class CryptoType(private val token: String? = null) {
        XROCK, TRX, BNB, ETH, BTC, TON, USDT, DOGS, GRBS, RUSD("1RUSD"),
        DRIFT, PUNK, BOLT, SLOW, UNIC, SAU, ANON, VWS, JUSDT("jUSDT"),
        DHD, THNG, LAIKA, DFC, GRAM, NUDES, UP, TAKE, MEOW, SOX, JBCT, TONNEL,
        SHEEP, PLANKTON, TNX, WEB3, MRDN, WALL, KINGY, HYDRA, TINU, TIME, STBL,
        PROTON, JVT, WIF, FID, LAVE, ARBUZ, MARGA, PIZZA, NKOTE("nKOTE"),
        NANO, IVS, ATL, MMM, GOY, CES, CAVI, MUMBA, REDX, BLKC, GEMSTON, NOT,
        JETTON, GGT, RAFF, HEDGE, LKY, SCALE, AMBR, LIFEYT, KFISH("kFISH"),
        DRA, STATHAM, POT, BUFFY, VIRUS, ICTN, JMT, CATS, MORFEY, ALENKA, KKX, GRC, OPEN;

        override fun toString() = token ?: name

        companion object {
            fun String.toCryptoType() = CryptoType.entries.firstOrNull { it.toString().equals(this, ignoreCase = true) }
        }

    }
}