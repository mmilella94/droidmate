// DroidMate, an automated execution generator for Android apps.
// Copyright (C) 2012-2018 Saarland University
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
// Current Maintainers:
// Nataniel Borges Jr. <nataniel dot borges at cispa dot saarland>
// Jenny Hotzkow <jenny dot hotzkow at cispa dot saarland>
//
// Former Maintainers:
// Konrad Jamrozik <jamrozik at st dot cs dot uni-saarland dot de>
//
// web: www.droidmate.org

package org.droidmate.report.apk.playback

import org.droidmate.exploration.ExplorationContext
import org.droidmate.exploration.modelFeatures.ActionPlaybackFeature
import org.droidmate.exploration.strategy.playback.Playback
import org.droidmate.report.misc.plot
import org.droidmate.misc.withExtension
import java.nio.file.Files
import java.nio.file.Path

/**
 * Report + plot of the number of actions and the number of reproduced actions.
 *
 * Ideally the report should show a line where X and Y are equal. Any difference means that some actions could not
 * be reproduced
 */
class ReproducibilityRate @JvmOverloads constructor(playbackStrategy: Playback,
													private val includePlots: Boolean = true,
													fileName: String = "reproducibilityRate.txt") : PlaybackReport(playbackStrategy, fileName) {
	override fun safeWriteApkReport(data: ExplorationContext, apkReportDir: Path, resourceDir: Path) {
		val reportSubDir = getPlaybackReportDir(apkReportDir)

		val sb = StringBuilder()

		val header = "ActionNr\tReproduced\n"
		sb.append(header)

		var actionNr = 0
		var explored = 0

		val playbackFeature = data.getOrCreateWatcher<ActionPlaybackFeature>()
		val skippedActions = playbackFeature.skippedActions

		playbackFeature.storedModel.let {model ->
			model.getPaths().forEachIndexed { traceIdx, trace ->
				trace.getActions().forEachIndexed { actionIdx, _ ->
					actionNr++

					if (!skippedActions.contains(Pair(traceIdx, actionIdx)))
						explored++

					sb.append("$actionNr\t$explored\n")
				}
			}
		}

		/*playbackStrategy.traces.forEach { trace ->
			trace.getTraceCopy().forEach { traceData ->
				actionNr++

				if (traceData.requested)
					requested++

				if (traceData.explored)
					explored++

				sb.append("$actionNr\t$requested\t$explored\n")
			}
		}*/

		val reportFile = reportSubDir.resolve(fileName)
		Files.write(reportFile, sb.toString().toByteArray())

		if (includePlots) {
			log.info("Writing out plot $")
			this.writeOutPlot(reportFile, resourceDir)
		}
	}

	private fun writeOutPlot(dataFile: Path, resourceDir: Path) {
		val fileName = dataFile.fileName.withExtension("pdf")
		val outFile = dataFile.resolveSibling(fileName)

		plot(dataFile.toAbsolutePath().toString(), outFile.toAbsolutePath().toString(), resourceDir)
	}
}
