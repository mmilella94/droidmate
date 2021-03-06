// DroidMate, an automated execution generator for Android apps.
// Copyright (C) 2012-2018. Saarland University
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

package org.droidmate.command

import com.konradjamrozik.createDirIfNotExists
import org.droidmate.androcov.StatementInstrumenter
import org.droidmate.configuration.ConfigurationWrapper
import org.droidmate.device.android_sdk.AaptWrapper
import org.droidmate.exploration.ExplorationContext
import org.droidmate.misc.SysCmdExecutor
import org.droidmate.tools.ApksProvider

class CoverageCommand @JvmOverloads constructor(cfg: ConfigurationWrapper,
                                                private val instrumenter: StatementInstrumenter = StatementInstrumenter(cfg)) : DroidmateCommand() {
	override fun execute(cfg: ConfigurationWrapper): List<ExplorationContext> {
		val apksProvider = ApksProvider(AaptWrapper(cfg, SysCmdExecutor()))
		val apks = apksProvider.getApks(cfg.apksDirPath, 0, ArrayList(), false)

		if (apks.all { it.instrumented }) {
			log.warn("No non-instrumented apks found. Aborting.")
			return emptyList()
		}

		val originalsDir = cfg.apksDirPath.resolve("originals").toAbsolutePath()
		if (originalsDir.createDirIfNotExists())
			log.info("Created directory to hold original apks, before instrumenting: $originalsDir")

		if (apks.size > 1)
			log.warn("More than one no-instrumented apk on the input dir. Instrumenting only the first one.")

		val apk = apks.first { !it.instrumented }
		instrumenter.instrument(apk, apk.path.parent)
		log.info("Instrumented ${apk.fileName}")
		moveOriginal(apk, originalsDir)

		return emptyList()
	}
}