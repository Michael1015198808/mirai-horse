package michael.horse

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.mamoe.mirai.console.command.CommandSenderOnMessage
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.plugin.version
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.MessageReceipt
import java.lang.Math.max
import kotlin.random.Random

object HorseCommand: CompositeCommand (
    owner = PluginMain,
    "赛马",
    description = """
        赛马小游戏
        """.trimIndent(),
) {
    var running = mutableSetOf<Long>()
    val mutex = Mutex()
    val horse: String = "\uD83C\uDFC7"
    val item: String = "❓"
    val item_pos: Int = 20 // 道具的位置
    @SubCommand("开始", "start")
    @Description("开始赛马")
    suspend fun CommandSenderOnMessage<GroupMessageEvent>.start() {
        var NotRunning = true
        mutex.withLock {
            if (running.contains(fromEvent.group.id)) {
                NotRunning = false
                running += fromEvent.group.id
            }
        }
        if (NotRunning) {
            var dis = mutableListOf(30, 30, 30, 30, 30)
            var passed = mutableListOf(false, false, false, false, false)
            var speeds = mutableListOf(1, 1, 1, 1, 1)
            var winner: Int? = null
            fun rendering(_dis: MutableList<Int>): String {
                return (0..4).joinToString("\n") {
                    "${it+1}${
                        if(passed[it]) {
                            " ".repeat(max(0, _dis[it]))
                        } else {
                            " ".repeat(item_pos - 4) + item + " ".repeat(_dis[it] - item_pos)
                        }
                    }$horse"
                }
            }
            fromEvent.group.sendMessage("""
                赛马比赛即将开始！
                请各位尽快下注
                """.trimIndent()
            )
            Thread.sleep(4000L)
            var receipts = mutableListOf<MessageReceipt<Group>>()
            while (winner == null) {
                var msg = ""
                Thread.sleep(1000L)
                for (i in 0..4) {
                    if(Random.nextBoolean()) {
                        if (speeds[i] > 1) {
                            --speeds[i]
                        }
                    }
                    if(Random.nextBoolean()) {
                        if (speeds[i] < 5) {
                            ++speeds[i]
                        }
                    }
                    dis[i] -= speeds[i]
                    if (dis[i] <= item_pos && passed[i] == false) {
                        passed[i] = true
                        msg += "${i + 1}号马吃到道具，"
                        when(Random.nextInt(0, 4)) {
                            0 -> {
                                msg += "速度变为7"
                                speeds[i] = 7
                            }
                            1 -> {
                                msg += "前进5格"
                                dis[i] -= 5
                            }
                            2 -> {
                                msg += "后退5格"
                                dis[i] += 5
                            }
                            3 -> {
                                msg += "速度变为0"
                                speeds[i] = 0
                            }
                        }
                        msg += "\n"
                    }
                }
                winner = dis.withIndex().filter { it.value <= 0 }.randomOrNull()?.index
                receipts += fromEvent.group.sendMessage(msg + rendering(dis))
            }
            fromEvent.group.sendMessage("${winner+1} 号马率先冲过终点线，获得胜利！")
            running -= fromEvent.group.id
            Thread.sleep(3000L)
            receipts.map { it.recall() }
        }
    }
    @SubCommand("帮助", "help")
    @Description("赛马游戏的帮助")
    suspend fun CommandSenderOnMessage<GroupMessageEvent>.help() {
        fromEvent.group.sendMessage("""
        赛马游戏 V${PluginMain.version} 帮助    
        
        游戏共有五个赛道，每个赛道上都有一匹马（$horse）。
        每匹马以随机的速度（1～5个空格）向前进。
        碰到道具（$item）后，随机获得以下几种效果中的一种：
            1、飞驰：马的速度变为7
            2、顺移：马前进5格
            3、倒退：马后退5格
            4、停滞：马的速度变为0
        """.trimIndent())
    }
}