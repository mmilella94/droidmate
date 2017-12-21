// DroidMate, an automated execution generator for Android apps.
// Copyright (C) 2012-2016 Konrad Jamrozik
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
// email: jamrozik@st.cs.uni-saarland.de
// web: www.droidmate.org
package org.droidmate.device

import org.droidmate.android_sdk.DeviceException
import org.droidmate.android_sdk.IAdbWrapper
import org.droidmate.uiautomator_daemon.DeviceCommand
import org.droidmate.uiautomator_daemon.DeviceResponse

class TcpClients constructor(adbWrapper: IAdbWrapper,
                             deviceSerialNumber: String,
                             monitorSocketTimeout: Int,
                             uiautomatorDaemonSocketTimeout: Int,
                             uiautomatorDaemonTcpPort: Int,
                             uiautomatorDaemonServerStartTimeout: Int,
                             uiautomatorDaemonServerStartQueryDelay: Int) : ITcpClients {
    private val monitorsClient: IMonitorsClient = MonitorsClient(monitorSocketTimeout, deviceSerialNumber, adbWrapper)
    private val uiautomatorClient: IUiautomatorDaemonClient = UiautomatorDaemonClient(
            adbWrapper,
            deviceSerialNumber,
            uiautomatorDaemonTcpPort,
            uiautomatorDaemonSocketTimeout,
            uiautomatorDaemonServerStartTimeout,
            uiautomatorDaemonServerStartQueryDelay)

    override fun anyMonitorIsReachable(): Boolean = monitorsClient.anyMonitorIsReachable()

    override fun closeMonitorServers() {
        monitorsClient.closeMonitorServers()
    }

    override fun getCurrentTime(): List<List<String>> = monitorsClient.getCurrentTime()

    override fun getLogs(): List<List<String>> = monitorsClient.getLogs()

    override fun getPorts(): List<Int> = monitorsClient.getPorts()

    override fun getUiaDaemonThreadIsAlive(): Boolean = uiautomatorClient.getUiaDaemonThreadIsAlive()

    override fun getUiaDaemonThreadIsNull(): Boolean = uiautomatorClient.getUiaDaemonThreadIsNull()

    override fun sendCommandToUiautomatorDaemon(deviceCommand: DeviceCommand): DeviceResponse =
            uiautomatorClient.sendCommandToUiautomatorDaemon(deviceCommand)

    override fun startUiaDaemon() {
        uiautomatorClient.startUiaDaemon()
    }

    override fun waitForUiaDaemonToClose() {
        uiautomatorClient.waitForUiaDaemonToClose()
    }

    override fun forwardPort() {
        this.uiautomatorClient.forwardPort()
    }

    @Throws(DeviceException::class)
    override fun forwardPorts() {
        this.uiautomatorClient.forwardPort()
        this.monitorsClient.forwardPorts()
    }
}