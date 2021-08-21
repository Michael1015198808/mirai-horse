package michael.horse

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.mamoe.mirai.console.command.CommandSenderOnMessage
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.plugin.version
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.nextEventOrNull
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
            val fast = (0..4).map { Random.nextInt(-5, 6) }
            val stable = (0..4).map { Random.nextInt(-5, 6) }
            val lucky = (0..4).map { Random.nextInt(-5, 6) }
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
                请各位尽快下注。
                
                马的属性如下：
                编号 加速 稳定 运气
                """.trimIndent() +
                (0..4).joinToString("\n") {
                    "   ${it + 1}     ${"%2d".format(fast[it])}      ${"%2d".format(stable[it])}      ${"%2d".format(lucky[it])}"
                }
            )
            Thread.sleep(4000L)
            var receipts = mutableListOf<MessageReceipt<Group>>()
            while (winner == null) {
                var msg = ""
                Thread.sleep(1000L)
                for (i in 0..4) {
                    val rand = Random.nextInt(100)
                    if (rand < 25 - stable[i] && speeds[i] > 1) {
                        --speeds[i]
                    } else if (rand > 75 - fast[i] && speeds[i] < 5) {
                        ++speeds[i]
                    }
                    dis[i] -= speeds[i]
                    if (dis[i] <= item_pos && passed[i] == false) {
                        passed[i] = true
                        msg += "${i + 1}号马吃到道具，"
                        if (Random.nextInt(50) < 25 + lucky[i]) {
                            if(Random.nextBoolean()) {
                                msg += "速度变为6"
                                speeds[i] = 6
                            } else {
                                msg += "前进5格"
                                dis[i] -= 5
                            }
                        } else {
                            if(Random.nextBoolean()) {
                                msg += "后退5格"
                                dis[i] += 5
                            } else {
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
            if (nextEventOrNull<GroupMessageEvent>(3000L) {
                    it.message.contentToString() == "保留记录"
                } == null) {
                receipts.map { it.recall() }
            }
        }
    }
    @SubCommand("帮助", "help")
    @Description("赛马游戏的帮助")
    suspend fun CommandSenderOnMessage<GroupMessageEvent>.help(arg: String = "") {
        val parts = listOf(
            "速度",
            "道具",
            "属性",
        )
        fromEvent.group.sendMessage(
        when(arg) {
            "速度" -> """
                所有马的初始速度为1。
                每回合，每匹马的速度都有25-稳定%的概率 -1（最低减到1）
                每回合，每匹马的速度都有25+加速%的概率 +1（最高加到5）
                其余情况下，马的速度保持不变
            """.trimIndent()
            "道具" -> """
                道具共有4种，效果分别为：
                   1、飞驰：马的速度变为6
                   2、顺移：马前进5格
                   3、倒退：马后退5格
                   4、停滞：马的速度变为0
                
                前两种道具的概率为25+运气%
                后两种道具的概率为25-运气%
            """.trimIndent()
            "属性" -> """
                每匹马有3个属性，在赛马开始前，每个属性在范围[-5, 5]内随机生成。
                每个属性的效果：
                    加速：数值越高，马加速的概率越大。
                    稳定：数值越高，马减速的概率越小。
                    运气：数值越高，马吃到道具获得正面效果的概率越高。
            """.trimIndent()
            ""  -> """
                赛马游戏 V${PluginMain.version} 简明帮助
                
                游戏共有五个赛道，每个赛道上都有一匹马（$horse）。
                每匹马以随机的速度（1～5个空格）向前进。
                碰到道具（$item）后，随机获得一种效果。
                
                对于各条目（${parts.joinToString("、")}）的详细信息，
                请输入"/赛马 帮助 <条目>"（如"/赛马 帮助 速度"）了解其他部分详细内容
                """.trimIndent()
            else -> """
                无法解析 $arg 为合法条目。
                合法条目列表：${parts.joinToString("、")}
            """.trimIndent()
        })
    }
}