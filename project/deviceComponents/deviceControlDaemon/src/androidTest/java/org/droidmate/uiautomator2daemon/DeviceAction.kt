package org.droidmate.uiautomator2daemon

import android.content.Context
import android.net.wifi.WifiManager
import android.support.test.uiautomator.*
import android.util.Log
import org.droidmate.uiautomator_daemon.UiAutomatorDaemonException
import org.droidmate.uiautomator_daemon.UiautomatorDaemonConstants.uiaDaemon_logcatTag
import org.droidmate.uiautomator_daemon.guimodel.*
import android.support.test.uiautomator.UiObject2
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

/**
 * Created by J.H. on 05.02.2018.
 */
const val measurePerformance = true

@Suppress("ConstantConditionIf")
inline fun <T> debugT(msg: String, block: () -> T, timer: (Long) -> Unit = {}, inMillis: Boolean = false): T {
	var res: T? = null
	if (measurePerformance) {
		measureNanoTime {
			res = block.invoke()
		}.let {
			timer(it)
			Log.d(uiaDaemon_logcatTag,"time ${if (inMillis) "${it / 1000000.0} ms" else "${it / 1000.0} ns/1000"} \t $msg")
		}
	} else res = block.invoke()
	return res!!
}
internal sealed class DeviceAction {

	@Throws(UiAutomatorDaemonException::class)
	abstract fun execute(device: UiDevice, context: Context)

	companion object {

		@JvmStatic fun fromAction(a: Action): DeviceAction? = with(a) {
			return when (this) {
				is WaitAction -> DeviceWaitAction(target, criteria)
				is LongClickAction -> DeviceLongClickAction(xPath, resId)
				is CoordinateLongClickAction -> DeviceCoordinateLongClickAction(x, y)
				is SwipeAction -> {
					if (start == null || dst == null) throw NotImplementedError("swipe executions currently only support point to point execution (TODO)")
					else DeviceSwipeAction(start!!, dst!!)
				}
				is TextAction -> DeviceTextAction(xPath, resId, text)
				is ClickAction -> DeviceClickAction(xPath, resId)
				is CoordinateClickAction -> DeviceCoordinateClickAction(x, y)
				is PressBack -> DevicePressBack()
				is PressHome -> DevicePressHome()
				is EnableWifi -> DeviceEnableWifi()
				is LaunchApp -> DeviceLaunchApp(appLaunchIconName)
				is SimulationAdbClearPackage -> {
					null /* There's no equivalent device action */
				}
			}
		}
		const val defaultTimeout: Long = 2000
		private const val waitTimeout: Long = 5000
		@JvmStatic private var time: Long = 0
		@JvmStatic private var cnt = 0
		@JvmStatic private var lastDump:String = "ERROR"

		@JvmStatic protected fun waitForChanges(device: UiDevice, actionSuccessful: Boolean = true) {
			if (actionSuccessful) {
				val preDump = lastDump
				measureTimeMillis {
					//            device.waitForWindowUpdate(null,defaultTimeout)
					measureTimeMillis { device.waitForIdle(defaultTimeout) }.let { Log.d(uiaDaemon_logcatTag, "waited $it millis for IDLE") }
//					do {
//						val res = device.wait(hasInteractive, waitTimeout)  // this seams to sometimes take extremely long maybe because the dump is instable?
//						Log.d(uiaDaemon_logcatTag, "wait-condition: $res")
//						getWindowHierarchyDump(device)
//					}while (res==null && lastDump == preDump) // we wait until we found something to interact with or the dump changed

					// exclude android internal elements
					device.wait(Until.findObject(By.clickable(true).pkg(Pattern.compile("^((?!com.android.systemui).)*$"))), waitTimeout)  // this only checks for clickable but is much more reliable than a custom Search-Condition
					// so if we need more we would have to implement something similar to `Until`
// DEBUG_CODE:
//					val c = device.findObjects(By.clickable(true).pkg(Pattern.compile("^((?!com.android.systemui).)*$"))).size
//					val cc = device.findObjects(By.clickable(true)).size
//					Log.e(uiaDaemon_logcatTag," found $c non-systemui clickable elements out of $cc")

					device.waitForIdle(defaultTimeout)  // even though one interactive element was found, the device may still be rendering the others -> wait for idle
				}.let {
					cnt += 1
					time += it
					Log.d(uiaDaemon_logcatTag, "waited $it millis for UI stabilization on average ${time / cnt} ms dump changed = ${lastDump != preDump}")
				}
			}
		}
		@JvmStatic fun getWindowHierarchyDump(device: UiDevice):String{
			return debugT(" fetching gui Dump ", {
				val os = ByteArrayOutputStream()
				try {
					device.dumpWindowHierarchy(os)
					os.flush()
					lastDump = os.toString(StandardCharsets.UTF_8.name())
					os.close()
				} catch (e: IOException) {
					os.close()
					throw UiAutomatorDaemonException(e)
				}
				lastDump
			}, inMillis = true)
		}
	}
}

private class DevicePressBack : DeviceAction() {
	override fun execute(device: UiDevice, context: Context) {
		waitForChanges(device, device.pressBack())
	}
}

private class DevicePressHome : DeviceAction() {
	override fun execute(device: UiDevice, context: Context) {
		waitForChanges(device, device.pressHome())
	}
}

private class DeviceEnableWifi : DeviceAction() {
	/**
	 * Based on: http://stackoverflow.com/a/12420590/986533
	 */
	override fun execute(device: UiDevice, context: Context) {
		Log.d(uiaDaemon_logcatTag, "Ensuring WiFi is turned on.")
		val wfm = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
		val wifiEnabled = wfm.setWifiEnabled(true)

		if (!wifiEnabled) Log.w(uiaDaemon_logcatTag, "Failed to ensure WiFi is enabled!")
	}
}

private data class DeviceLaunchApp(val appLaunchIconName: String) : DeviceAction() {
	private val appListResId = "com.google.android.googlequicksearchbox:id/apps_list_view"
	override fun execute(device: UiDevice, context: Context) {
		Log.d(uiaDaemon_logcatTag, "Launching app by navigating to and clicking icon with text $appLaunchIconName")

		val clickResult: Boolean
		try {
			val app = navigateToAppLaunchIcon(appLaunchIconName, device)
			Log.v(uiaDaemon_logcatTag, "Pressing the $appLaunchIconName app icon to launch it.")
			clickResult = app.click()
		} catch (e: UiObjectNotFoundException) {
			Log.w(uiaDaemon_logcatTag,
					String.format("Attempt to navigate to and click on the icon labeled '%s' to launch the app threw an exception: %s: %s",
							appLaunchIconName, e.javaClass.simpleName, e.localizedMessage))
			Log.d(uiaDaemon_logcatTag, "Pressing 'home' button after failed app launch.")
			waitForChanges(device, device.pressHome())
			return
		}

		if (clickResult) {
//            waitForChanges(device,clickResult)
			device.waitForIdle(defaultTimeout)
			measureTimeMillis {
				device.wait(hasInteractive, 10000)
			}.let { Log.d(uiaDaemon_logcatTag, "load-time $it millis") }
		} else
			Log.w(uiaDaemon_logcatTag, "A click on the icon labeled '$appLaunchIconName' to launch the app returned false")
	}

	private fun navigateToAppLaunchIcon(appLaunchIconName: String, device: UiDevice): UiObject {
		// Simulate a short press on the HOME button.
		device.pressHome()

		// We’re now in the home screen. Next, we want to simulate
		// a user bringing up the All Apps screen.
		// If you use the uiautomatorviewer tool to capture a snapshot
		// of the Home screen, notice that the All Apps button’s
		// content-description property has the value "Apps".  We can
		// use this property to create a UiSelector to find the button.
		val allAppsButton = device.findObject(UiSelector().description("Apps"))

		// Simulate a click to bring up the All Apps screen.
		allAppsButton.clickAndWaitForNewWindow()


		// In the All Apps screen, the app launch icon is located in
		// the Apps tab. To simulate the user bringing up the Apps tab,
		// we create a UiSelector to find a tab with the text
		// label "Apps".
		val appsTab = device.findObject(UiSelector().text("Apps"))
		if (!appsTab.exists()) Log.w(uiaDaemon_logcatTag, "This device does not have an 'Apps' and a 'Widgets' tab, skipping.")
		// Simulate a click to enter the Apps tab.
		else appsTab.click()


		// Next, in the apps tabs, we can simulate a user swiping until
		// they come to the app launch icon. Since the container view
		// is scrollable, we can use a UiScrollable object.
		var appViews: UiScrollable

		try {
			Log.i(uiaDaemon_logcatTag, "Attempting to locate app list by resourceId.")
			appViews = UiScrollable(UiSelector().resourceId(appListResId))

			// Set the swiping mode to horizontal (the default is vertical)
			appViews.setAsHorizontalList()

			// Create a UiSelector to find the app launch icon and simulate
			// a user click to launch the app.
			return appViews.getChildByText(
					UiSelector().className(android.widget.TextView::class.java.name),
					appLaunchIconName)
		} catch (e: UiObjectNotFoundException) {
			Log.i(uiaDaemon_logcatTag, "It was not possible to locate app list by resourceId, using heuristic.")
			appViews = UiScrollable(UiSelector().scrollable(true))
		}

		// Set the swiping mode to horizontal (the default is vertical)
		appViews.setAsHorizontalList()

		// Create a UiSelector to find the app launch icon and simulate
		// a user click to launch the app.
		return appViews.getChildByText(
				UiSelector().className(android.widget.TextView::class.java.name),
				appLaunchIconName)
	}
}

private data class DeviceSwipeAction(val start: Pair<Int, Int>, val dst: Pair<Int, Int>, val xPath: String = "", val direction: String = "") : DeviceAction() {
	private val x0: Int inline get() = start.first
	private val y0: Int inline get() = start.second
	private val x1: Int inline get() = dst.first
	private val y1: Int inline get() = dst.second
	override fun execute(device: UiDevice, context: Context) {
		Log.d(uiaDaemon_logcatTag, "Swiping from (x,y) coordinates ($x0,$y0) to ($x1,$y1)")
		assert(x0 >= 0 && x0 < device.displayWidth, { "Error on swipe invalid x0:$x0" })
		assert(y0 >= 0 && y0 < device.displayHeight, { "Error on swipe invalid y0:$y0" })
		assert(x1 >= 0 && x1 < device.displayWidth, { "Error on swipe invalid x1:$x1" })
		assert(y1 >= 0 && y1 < device.displayHeight, { "Error on swipe invalid y1:$y1" })

		val success = device.swipe(x0, y0, x1, y1, 35)
		if (!success) Log.e(uiaDaemon_logcatTag, "Swipe failed: from (x,y) coordinates ($x0,$y0) to ($x1,$y1)")
		// we could issue a wait action on failure (e.g. waitExist for widget at position)
	}
}

private data class DeviceWaitAction(private val id: String, private val criteria: WidgetSelector) : DeviceAction() {
	override fun execute(device: UiDevice, context: Context) {
		Log.d(uiaDaemon_logcatTag, "Wait for element to exist" + this.toString())
		when (criteria) {
			WidgetSelector.ResourceId -> findByResId(id)
			WidgetSelector.ClassName -> findByClassName(id)
			WidgetSelector.ContentDesc -> findByDescription(id)
			WidgetSelector.XPath -> findByXPath(id)
		}.let {
			device.findObject(it).let {
				// REMARK this wait is necessary to avoid StackOverflowError in the QueryController, which would happen depending on when the UI view stabilizes
				measureTimeMillis { device.wait(hasInteractive, 20000) }.let { Log.d(uiaDaemon_logcatTag, "waited $it millis for interactive element") }
				var success = false
				measureTimeMillis { success = it.waitForExists(10000) }.let { Log.d(uiaDaemon_logcatTag, "waited for exists $it millis with result $success") }
				if (!success) {
					Log.w(uiaDaemon_logcatTag, "WARN element $id not found")
					val clickable = device.findObjects(By.clickable(true)).map { o -> o.resourceName + ": ${o.visibleCenter}" }
					Log.d(uiaDaemon_logcatTag, "clickable elements: $clickable")
				}
			}
		} // wait up to 10 seconds
	}
}

private sealed class DeviceObjectAction : DeviceAction() {
	abstract val xPath: String
	abstract val resId: String

	protected fun executeAction(device: UiDevice, action: (UiObject) -> Boolean, action2: (UiObject2) -> Unit) {
		Log.d(uiaDaemon_logcatTag, "execute action on target element with resId=$resId xPath=$xPath")
		val success = if (xPath.isNotEmpty()) executeAction(device, action, xPath) else {
			Log.d(uiaDaemon_logcatTag, "select element by resourceId")
			executeAction(device, action, resId, findByResId)
		}
		if (!success) {
			Log.w(uiaDaemon_logcatTag, "action on UiObject failed, try to perform on UiObject2 By.resourceId")
			executeAction2(device, action2, resId)
		}
	}
}

private data class DeviceClickAction(override val xPath: String, override val resId: String) : DeviceObjectAction() {
	override fun execute(device: UiDevice, context: Context) {
		executeAction(device,
				{ o -> o.click() },
				{ o -> o.click() })
		waitForChanges(device)
	}
}

private data class DeviceCoordinateClickAction(val x: Int, val y: Int) : DeviceAction() {
	override fun execute(device: UiDevice, context: Context) {
		Log.d(uiaDaemon_logcatTag, "Clicking coordinates ($x,$y)")
		assert(x >= 0 && x < device.displayWidth, { "Error on coordinate click invalid x:$x" })
		assert(y >= 0 && y < device.displayHeight, { "Error on coordinate click invalid y:$y" })
		device.click(x, y)
		Log.d(uiaDaemon_logcatTag, "Clicked coordinates $x, $y")
		waitForChanges(device)
	}
}

private data class DeviceLongClickAction(override val xPath: String, override val resId: String) : DeviceObjectAction() {
	override fun execute(device: UiDevice, context: Context) {
		executeAction(device, { o -> o.longClick() }, { o -> o.longClick() })
		waitForChanges(device)
	}
}

private data class DeviceCoordinateLongClickAction(val x: Int, val y: Int) : DeviceAction() {
	override fun execute(device: UiDevice, context: Context) {
		Log.d(uiaDaemon_logcatTag, "Long clicking coordinates ($x,$y)")

		assert(x >= 0 && x < device.displayWidth, { "Error on coordinate long click invalid x:$x" })
		assert(y >= 0 && y < device.displayHeight, { "Error on coordinate long click invalid y:$y" })

		device.swipe(x, y, x, y, 100) // 100 ~ 2s. Empirical evaluation.
		Log.d(uiaDaemon_logcatTag, "Long clicked coordinates ($x, $y)")
		waitForChanges(device)
	}
}

// TODO check if this is still an issue at all
// NEED FIX: In some cases the setting of text does open the keyboard and is hiding some widgets
// but these widgets are still in the uiautomator dump. Therefore it may be that DroidMate
// clicks on the keyboard thinking it clicked one of the widgets below it.
// http://stackoverflow.com/questions/17223305/suppress-keyboard-after-setting-text-with-android-uiautomator
// -> It seems there is no reliable way to suppress the keyboard.
private data class DeviceTextAction(override val xPath: String, override val resId: String, val text: String) : DeviceObjectAction() {
	val selector by lazy { if (xPath.isNotEmpty()) findByXPath(xPath) else findByResId(resId) }
	override fun execute(device: UiDevice, context: Context) {
		executeAction(device, { o -> o.setText(text) },
				{ o ->
					@Suppress("UsePropertyAccessSyntax")
					o.setText(text)
				})
		device.findObject(selector.text(text)).waitForExists(defaultTimeout)  // wait until the text is set
	}
}