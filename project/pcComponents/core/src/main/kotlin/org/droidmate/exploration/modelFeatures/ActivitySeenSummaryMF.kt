package org.droidmate.exploration.modelFeatures

import kotlinx.coroutines.CoroutineName
import org.droidmate.device.logcat.ApiLogcatMessage
import org.droidmate.deviceInterface.exploration.isLaunchApp
import org.droidmate.deviceInterface.exploration.isPressBack
import org.droidmate.exploration.ExplorationContext
import java.nio.file.Files
import kotlin.coroutines.CoroutineContext


class ActivitySeenSummaryMF : ModelFeature() {

    override val coroutineContext: CoroutineContext = CoroutineName("ActivitySeenSummaryMF")

    override suspend fun onAppExplorationFinished(context: ExplorationContext) {  /* do nothing [to be overwritten] */
        val sb = StringBuilder()
        val header = "activity\tcount\n"
        sb.append(header)

        val activitySeenMap = HashMap<String, Int>()
        var lastActivity = ""
        var currActivity = context.apk.launchableMainActivityName

        // Always see the main activity
        activitySeenMap.put(currActivity, 1)

        context.explorationTrace.P_getActions().forEach { record ->

            if (record.actionType.isPressBack())
                currActivity = lastActivity
            else if (record.actionType.isLaunchApp())
                currActivity = context.apk.launchableMainActivityName

            if (currActivity == "")
                currActivity = "<DEVICE HOME>"

            val logs = record.deviceLogs.map { ApiLogcatMessage.from(it) }

            logs.filter { it.methodName.toLowerCase().startsWith("startactivit") }
                    .forEach { log ->
                        val intent = log.getIntents()
                        // format is: [ '[data=, component=<HERE>]', 'package ]
                        if (intent.isNotEmpty()) {
                            lastActivity = currActivity
                            currActivity = intent[0].substring(intent[0].indexOf("component=") + 10).replace("]", "")
                        }

                        val count = if (activitySeenMap.containsKey(currActivity))
                            activitySeenMap[currActivity]!!
                        else
                            0

                        activitySeenMap[currActivity] = count + 1
                    }
        }

        activitySeenMap.forEach { activity, count ->
            sb.appendln("$activity\t$count")
        }

        val reportFile = context.getModel().config.baseDir.resolve("activitiesSeen.txt")
        Files.write(reportFile, sb.toString().toByteArray())
    }
}