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
package org.droidmate

import com.natpryce.konfig.*
import org.droidmate.ConfigProperties.deploy.installApk
import org.droidmate.ConfigProperties.deploy.installAux
import org.droidmate.command.DroidmateCommand
import org.droidmate.command.ExploreCommand
import org.droidmate.command.InlineCommand
import org.droidmate.configuration.ConfigurationBuilder
import org.droidmate.exploration.strategy.ISelectableExplorationStrategy
import org.droidmate.frontend.DroidmateFrontend
import org.droidmate.frontend.ExceptionHandler
import org.droidmate.logging.LogbackUtilsRequiringLogbackLog
import org.droidmate.misc.DroidmateException
import org.droidmate.report.Reporter
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.FileSystems
import java.time.LocalDate
import java.util.*

@Suppress("MemberVisibilityCanBePrivate")
object ExplorationAPI : ConfigProperties() {
	private val log = LoggerFactory.getLogger(DroidmateFrontend::class.java)

	/**
	 * entry-point to explore an application with a (subset) of default exploration strategies
	 * 1. inline the apks in the directory if they do not end on '-inlined.apk'
	 * 2. run the exploration with the strategies listed in the property 'explorationStrategies'
	 */
	@JvmStatic
	fun main(args: Array<String>) {
		val cmdOptions = arrayOf(CommandLineOption(configPath, short = "config", description = "path to the file containing custom configuration properties")
				, CommandLineOption(ConfigProperties.output.reportOutputDir))

		val (cmd_config, cmd_args) = parseArgs(args, *cmdOptions)
		inlineAndExplore(cmd_config)
	}

	@JvmStatic
	fun inlineAndExplore(cmd_config: Configuration?, strategies: List<ISelectableExplorationStrategy> = emptyList(), reportCreators: List<Reporter> = emptyList()) {
		val config = (   // highest priority overriding lower priority
//            EnvironmentVariables() overriding           // any JVM arguments (i.e. "-DpropName=value")
				ConfigurationProperties.fromResource("defaultConfig.properties") // overwrite any system property by definitions of resource file
//        overriding systemProperties()
				).apply {
			cmd_config?.overriding(this)
			File(this[configPath].path).let { customConfigFile ->
				if (customConfigFile.exists()) ConfigurationProperties.fromFile(customConfigFile) overriding this // highest priority any custom file, e.g given via command line
			}
		}

		var exitStatus = 0

		try {
			println(copyRight)
			validateConfig(config)

			LogbackUtilsRequiringLogbackLog.cleanLogsDir()  // FIXME this logPath crap should use our config properties
			log.info("Bootstrapping DroidMate: building ${org.droidmate.configuration.Configuration::class.java.simpleName} from args " +
					"and instantiating objects for ${DroidmateCommand::class.java.simpleName}.")
			log.info("IMPORTANT: for help on how to configure DroidMate, run it with --help")

//			log.info("inline the apks if necessary")
			val cfg = ConfigurationBuilder().build(ConfigWrapper.createOldConfigArgs(config), FileSystems.getDefault())
//			InlineCommand().execute(cfg)
			cfg.runOnNotInlined = true

			val runStart = Date()
			val command = ExploreCommand.build(cfg, reportCreators = reportCreators)
			log.info("EXPLORATION start timestamp: $runStart")
			log.info("Running in Android $cfg.androidApi compatibility mode (api23+ = version 6.0 or newer).")

			command.execute(cfg)

		} catch (e: Throwable) {
			exitStatus = ExceptionHandler().handle(e)
		}
		System.exit(exitStatus)
	}

	private fun validateConfig(config: Configuration) {
		if (config[logLevel].toUpperCase() !in arrayListOf("TRACE", "DEBUG", "INFO", "WARN", "ERROR")) throw DroidmateException("The $logLevel variable has to be set to TRACE, " +
				"DEBUG, INFO, WARN or ERROR. Instead, it is set to ${config[logLevel]}.")

		if (!config[installApk]) log.warn("DroidMate will not reinstall the target APK(s). If the APK(s) are not previously installed on the device the exploration will fail.")

		if (!config[installAux]) log.warn("DroidMate will not reinstall its auxiliary components (UIAutomator and Monitor). If the they are not previously installed on the device the exploration will fail.")
	}

}

val copyRight = """ |DroidMate, an automated execution generator for Android apps.
                  |Copyright (c) 2012 - ${LocalDate.now().year} Konrad Jamrozik
                  |This program is free software licensed under GNU GPL v3.
                  |
                  |You should have received a copy of the GNU General Public License
                  |along with this program.  If not, see <http://www.gnu.org/licenses/>.
                  |
                  |email: jamrozik@st.cs.uni-saarland.de
                  |web: www.droidmate.org""".trimMargin()
