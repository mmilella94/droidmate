package org.droidmate.exploration.statemodel.loader

import com.natpryce.konfig.CommandLineOption
import com.natpryce.konfig.getValue
import com.natpryce.konfig.parseArgs
import com.natpryce.konfig.stringType
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock
import org.droidmate.configuration.ConfigProperties
import org.droidmate.debug.debugT
import org.droidmate.deviceInterface.guimodel.toUUID
import org.droidmate.exploration.statemodel.*
import org.droidmate.exploration.statemodel.features.ModelFeature
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

// TODO constructor parameter to enable compatibility mode
// FIXME watcher state restoration requires eContext.onUpdate function & model.onUpdate currently only onActionUpdate is supported
abstract class ModelParserI<T,S,W>: ParserI<T,Pair<ActionData, StateData>>{
	abstract val config: ModelConfig
	abstract val reader: ContentReader
	abstract val stateParser: StateParserI<S,W>
	abstract val widgetParser: WidgetParserI<W>
	abstract val enablePrint: Boolean

	override val parentJob: Job = Job()
	override val model by lazy{ Model.emptyModel(config) }
	private val jobName by lazy{ "ModelParsing ${config.appName}(${config.baseDir})" }
	protected val actionParseJobName: (List<String>)->String = { actionS ->
		"actionParser ${actionS[ActionData.Companion.ActionDataFields.Action.ordinal]}:${actionS[ActionData.srcStateIdx]}->${actionS[ActionData.resStateIdx]}"}

	fun loadModel(watcher: LinkedList<ModelFeature> = LinkedList()): Model {
		return execute(watcher)
	}

	fun execute(watcher: LinkedList<ModelFeature>): Model{
		// the very first state of any trace is always an empty state which is automatically added on Model initialization
		addEmptyState()
		// start producer who just sends trace paths to the multiple trace processor jobs
		val producer = traceProducer()
		repeat(if(isSequential) 1 else 5)
		{ traceProcessor( producer, watcher ) }  // process up to 5 exploration traces in parallel
		runBlocking(CoroutineName(jobName)) {
			log("wait for children completion")
//			parentJob.joinChildren() } // wait until all traces were processed (the processor adds the trace to the model)
			parentJob.children.forEach {
				it.join()
				it.invokeOnCompletion { exception ->
					if (exception != null) {
						parentJob.cancel(RuntimeException("Error in $it"))
						println("\n---------------------------\n ERROR while parsing model $jobName : $it : ${it.children}")
						exception.printStackTrace()
					}
				}
			}
		}
		clearQueues()
		return model
	}
	private fun clearQueues() {
		stateParser.queue.clear()
		widgetParser.queue.clear()
	}
	abstract fun addEmptyState()

	private fun traceProducer() = produce<Path>(newContext(jobName), capacity = 5) {
		log("TRACE PRODUCER CALL")
		Files.list(Paths.get(config.baseDir.toUri())).use { s ->
			s.filter { it.fileName.toString().startsWith(config[ConfigProperties.ModelProperties.dump.traceFilePrefix]) }
					.also {
						for (p in it) {
							send(p)
						}
					}
		}
	}

	private val modelMutex = Mutex()
	private fun traceProcessor(channel: ReceiveChannel<Path>, watcher: LinkedList<ModelFeature>) = launch(newContext(jobName)){
		log("trace processor launched")
		if(enablePrint) println("trace processor launched")
		channel.consumeEach { tracePath ->
			if(enablePrint) println("\nprocess TracePath $tracePath")
			val traceId = tracePath.fileName.toString().removePrefix(config[ConfigProperties.ModelProperties.dump.traceFilePrefix]).toUUID()
			modelMutex.withLock { model.initNewTrace(watcher, traceId) }
					.let { trace ->
						reader.processLines(tracePath, lineProcessor = processor).let { actionPairs ->  // use maximal parallelism to process the single actions/states
							if (watcher.isEmpty()){
								val resState = getElem(actionPairs.last()).second
								log(" wait for completion of actions")
								trace.updateAll(actionPairs.map { getElem(it).first }, resState)
							}  // update trace actions
							else {
								log(" wait for completion of EACH action")
								actionPairs.forEach { getElem(it).let{ (action,resState) -> trace.update(action, resState) }}
							}
						}
					}
			log("CONSUMED trace $tracePath")
		}
	}

	/** parse the action this function is called in the processor either asynchronous (Deferred) or sequential (blocking) */
	suspend fun parseAction(actionS: List<String>): Pair<ActionData, StateData> {
		if(enablePrint) println("\n\t ---> parse action $actionS")
		val resState = stateParser.processor(actionS).getState()
		val targetWidgetId = widgetParser.fixedWidgetId(actionS[ActionData.widgetIdx])

		val srcId = idFromString(actionS[ActionData.srcStateIdx])
		val srcState = stateParser.queue[srcId]!!.getState()
		val targetWidget = targetWidgetId?.let { tId ->
			srcState.widgets.find { it.id == tId } ?: run{
				log("ERROR target widget $tId cannot be found in src state")
				null
			}
		}
		val fixedActionS = mutableListOf<String>().apply { addAll(actionS) }
		fixedActionS[ActionData.resStateIdx] = resState.stateId.dumpString()
		fixedActionS[ActionData.srcStateIdx] = srcState.stateId.dumpString()  //do NOT use local val srcId as that may be the old id

		if(actionS!=fixedActionS)
			println("id's changed due to automatic repair new action is \n $fixedActionS\n instead of \n $actionS")

		return Pair(ActionData.createFromString(fixedActionS, targetWidget, config[ConfigProperties.ModelProperties.dump.sep]), resState)
				.also { log("\n computed TRACE ${actionS[ActionData.resStateIdx]}: ${it.first.actionString()}") }
	}

	@Suppress("ReplaceSingleLineLet")
	suspend fun S.getState() = this.let{ e ->  stateParser.getElem(e) }

	companion object {

		@JvmStatic fun loadModel(config: ModelConfig, watcher: LinkedList<ModelFeature> = LinkedList(),
		                         autoFix: Boolean = false, sequential: Boolean = false, enablePrint: Boolean = true)
				: Model{
			if(sequential) return debugT("model loading", {
				ModelParserS(config, compatibilityMode = autoFix, enablePrint = enablePrint).loadModel(watcher)
			}, inMillis = true)
			return debugT("model loading", {
				ModelParserP(config, compatibilityMode = autoFix, enablePrint = enablePrint).loadModel(watcher)
			}, inMillis = true)
		}
		/**
		 * helping/debug function to manually load a model.
		 * The directory containing the 'model' folder and the app name have to be specified, e.g.
		 * '--Output-outputDir=pathToModelDir --appName=sampleApp'
		 * --Core-debugMode=true (optional for enabling print-outs)
		 */
		@JvmStatic fun main(args: Array<String>) {
			// stateDiff(args)
			val appName by stringType
			val cfg = parseArgs(args,
					CommandLineOption(ConfigProperties.Output.outputDir), CommandLineOption(ConfigProperties.Core.debugMode),
					CommandLineOption(appName)
			).first
			val config = ModelConfig(cfg[appName], true, cfg = cfg)
			val m =
//				loadModel(config, autoFix = false, sequential = true)
				loadModel(config, autoFix = true, sequential = false, enablePrint = false)
//				debugT("load time parallel", { ModelParserP(config).loadModel() }, inMillis = true)
//				debugT("load time sequentiel", { ModelParserS(config).loadModel() }, inMillis = true)

			/** dump the (repaired) model */ /*
			runBlocking {
				m.P_dumpModel(ModelConfig("repaired-${config.appName}", cfg = cfg))
				m.modelDumpJob.joinChildren()
			}
			// */
			println("model load finished: ${config.appName} $m")
		}
	} /** end COMPANION **/

}

class ModelParserP(override val config: ModelConfig, override val reader: ContentReader = ContentReader(config),
                   override val compatibilityMode: Boolean = false, override val enablePrint: Boolean = true)
	: ModelParserI<Deferred<Pair<ActionData, StateData>>, Deferred<StateData>, Deferred<Widget>>() {
	override val isSequential: Boolean = true //TODO only for debugging

	override val widgetParser by lazy { WidgetParserP(model,parentJob, compatibilityMode) }
	override val stateParser  by lazy { StateParserP(widgetParser, reader, model, parentJob, compatibilityMode)}

	override val processor: suspend(s: List<String>) -> Deferred<Pair<ActionData, StateData>> = { actionS ->
		async(context(actionParseJobName(actionS))) { parseAction(actionS) }
	}

	override fun addEmptyState() {
		StateData.emptyState.let{ stateParser.queue[it.stateId] = async(CoroutineName("empty State")) { it } }
	}

	override suspend fun getElem(e: Deferred<Pair<ActionData, StateData>>): Pair<ActionData, StateData> = e.await()

}

class ModelParserS(override val config: ModelConfig, override val reader: ContentReader = ContentReader(config),
                   override val compatibilityMode: Boolean = false, override val enablePrint: Boolean = true)
	: ModelParserI<Pair<ActionData, StateData>, StateData, Widget >() {
	override val isSequential: Boolean = true

	override val widgetParser by lazy { WidgetParserS(model,parentJob, compatibilityMode) }
	override val stateParser  by lazy { StateParserS(widgetParser, reader, model, parentJob, compatibilityMode)}

	override val processor: suspend (List<String>) -> Pair<ActionData, StateData> = { actionS:List<String> ->
		parseAction(actionS)
	}

	override fun addEmptyState() {
		StateData.emptyState.let{ stateParser.queue[it.stateId] = it }
	}

	override suspend fun getElem(e: Pair<ActionData, StateData>): Pair<ActionData, StateData> = e

}