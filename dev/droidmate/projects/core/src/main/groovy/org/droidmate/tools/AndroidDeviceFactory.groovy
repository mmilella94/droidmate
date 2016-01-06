// Copyright (c) 2012-2015 Saarland University
// All rights reserved.
//
// Author: Konrad Jamrozik, jamrozik@st.cs.uni-saarland.de
//
// This file is part of the "DroidMate" project.
//
// www.droidmate.org

package org.droidmate.tools

import org.droidmate.android_sdk.IAdbWrapper
import org.droidmate.common_android.DeviceCommand
import org.droidmate.common_android.DeviceResponse
import org.droidmate.configuration.Configuration
import org.droidmate.device.AndroidDevice
import org.droidmate.device.IAndroidDevice
import org.droidmate.device.ISerializableTCPClient

class AndroidDeviceFactory implements IAndroidDeviceFactory
{

  private final Configuration                                         cfg
  private final ISerializableTCPClient<DeviceCommand, DeviceResponse> uiautomatorClient
  private final IAdbWrapper                                           adbWrapper

  AndroidDeviceFactory(
    Configuration cfg,
    ISerializableTCPClient<DeviceCommand, DeviceResponse> uiautomatorClient,
    IAdbWrapper adbWrapper)
  {
    this.cfg = cfg
    this.uiautomatorClient = uiautomatorClient
    this.adbWrapper = adbWrapper
  }

  @Override
  IAndroidDevice create(String serialNumber)
  {
    return new AndroidDevice(serialNumber, cfg, uiautomatorClient, adbWrapper)
  }
}
