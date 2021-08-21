package michael.horse

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.mamoe.mirai.console.command.CommandSenderOnMessage
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.plugin.version
import net.mamoe.mirai.event.events.GroupMessageEvent
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
            var dis = mutableListOf(40, 40, 40, 40, 40)
            var passed = mutableListOf(0, 0, 0, 0, 0)
            var speeds = mutableListOf(0, 0, 0, 0, 0)
            var winner: Int? = null
            fun rendering(_dis: MutableList<Int>): String {
                return (0..4).joinToString("\n") {
                    "${it+1}${" ".repeat(max(0, _dis[it]))}$horse"
                }
            }
            fromEvent.group.sendMessage("""
                赛马比赛即将开始！
                请各位尽快下注
                
                """.trimIndent() + rendering(dis)
            )
            Thread.sleep(3000L)
            while (winner == null) {
                Thread.sleep(1000L)
                for (i in 0..4) {
                    if(Random.nextBoolean()) {
                        if (speeds[i] < 3) {
                            ++speeds[i]
                        }
                    }
                    if(Random.nextBoolean()) {
                        if (speeds[i] > 1) {
                            --speeds[i]
                        }
                    }
                    dis[i] -= speeds[i]
                }
                winner = dis.withIndex().filter { it.value <= 0 }.randomOrNull()?.index
                fromEvent.group.sendMessage(rendering(dis))
            }
            fromEvent.group.sendMessage("${winner+1} 号马率先冲过终点线，获得胜利！")
            running -= fromEvent.group.id
        }
    }
    @SubCommand("帮助", "help")
    @Description("赛马游戏的帮助")
    suspend fun CommandSenderOnMessage<GroupMessageEvent>.help() {
        fromEvent.group.sendMessage("""
        赛马游戏 V${PluginMain.version} 帮助    
        
        游戏共有五个赛道，每个赛道上都有一匹马（$horse）。
        每匹马以随机的速度（1～5个空格）向前进。
        """.trimIndent())
        /*
        碰到道具（$item）后，有以下几种效果：
        1、飞驰：马的速度变为4
        2、顺移：马前进4格
        3、倒退：马后退4格
        4、停滞：马的速度变为0
        */
    }
}