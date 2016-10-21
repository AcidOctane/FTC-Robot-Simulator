/* Copyright (c) 2015 Qualcomm Technologies Inc

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of Qualcomm Technologies Inc nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. */

package com.qualcomm.ftccommon;

import android.content.Context;

import com.qualcomm.hardware.ArmableUsbDevice;
import com.qualcomm.hardware.HardwareFactory;
import com.qualcomm.robotcore.eventloop.EventLoopManager;
import com.qualcomm.robotcore.eventloop.opmode.OpModeManager;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.DcMotorController;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.ServoController;
import com.qualcomm.robotcore.hardware.VoltageSensor;
import com.qualcomm.robotcore.robocol.TelemetryMessage;
import com.qualcomm.robotcore.robot.RobotState;
import com.qualcomm.robotcore.util.BatteryChecker;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.MovingStatistics;
import com.qualcomm.robotcore.util.RobotLog;

public class FtcEventLoopHandler implements BatteryChecker.BatteryWatcher {

  //------------------------------------------------------------------------------------------------
  // Constants
  //------------------------------------------------------------------------------------------------

  /** This string is sent in the robot battery telemetry payload to indicate
   *  that no voltage sensor is available on the robot. */
  public static final String NO_VOLTAGE_SENSOR = "$no$voltage$sensor$";

  //------------------------------------------------------------------------------------------------
  // State
  //------------------------------------------------------------------------------------------------

  private Context           robotControllerContext;
  private EventLoopManager  eventLoopManager;

  private BatteryChecker    robotControllerBatteryChecker;
  private double            robotControllerBatteryCheckerInterval = 180.0; // in seconds

  private ElapsedTime       robotBatteryTimer         = new ElapsedTime();
  private double            robotBatteryInterval      = 3.00; // in seconds
  private MovingStatistics  robotBatteryStatistics    = new MovingStatistics(10);
  private ElapsedTime       robotBatteryLoggingTimer  = new ElapsedTime(0); // 0 so we get an initial report
  private double            robotBatteryLoggingInterval = robotControllerBatteryCheckerInterval;

  private ElapsedTime       userTelemetryTimer        = new ElapsedTime(0); // 0 so we get an initial report
  private double            userTelemetryInterval     = 0.250; // in seconds
  private final Object      refreshUserTelemetryLock  = new Object();

  private ElapsedTime       updateUITimer             = new ElapsedTime();
  private double            updateUIInterval          = 0.250; // in seconds
  private UpdateUI.Callback callback;

  private HardwareFactory   hardwareFactory = null;
  private HardwareMap       hardwareMap     = null;

  //------------------------------------------------------------------------------------------------
  // Construction
  //------------------------------------------------------------------------------------------------

  public FtcEventLoopHandler(HardwareFactory hardwareFactory, UpdateUI.Callback callback, Context robotControllerContext) {
    this.hardwareFactory        = hardwareFactory;
    this.callback               = callback;
    this.robotControllerContext = robotControllerContext;

    long milliseconds = (long)(robotControllerBatteryCheckerInterval * 1000); //milliseconds
    robotControllerBatteryChecker = new BatteryChecker(robotControllerContext, this, milliseconds);
    robotControllerBatteryChecker.startBatteryMonitoring();
  }

  public void init(EventLoopManager eventLoopManager) {
    this.eventLoopManager = eventLoopManager;
  }

  public EventLoopManager getEventLoopManager() {
    return eventLoopManager;
  }

  public HardwareMap getHardwareMap() throws RobotCoreException, InterruptedException {
    if (hardwareMap==null) {
      hardwareMap = hardwareFactory.createHardwareMap(eventLoopManager);
    }
    return hardwareMap;
  }

  //------------------------------------------------------------------------------------------------
  // Operations
  //------------------------------------------------------------------------------------------------

  public void displayGamePadInfo(String activeOpModeName) {
    if (updateUITimer.time() > updateUIInterval) {
      updateUITimer.reset();

      // Get access to gamepad 1 and 2
      Gamepad gamepads[] = eventLoopManager.getGamepads();
      callback.updateUi(activeOpModeName, gamepads);
    }
  }

  public Gamepad[] getGamepads() {
    return eventLoopManager.getGamepads();
  }

  /**
   * Updates the (indicated) user's telemetry: the telemetry is transmitted if a sufficient
   * interval has passed since the last transmission. If the telemetry is transmitted, the
   * telemetry is cleared and the timer is reset. A battery voltage key may be added to the
   * message before transmission.
   *
   * @param telemetry         the telemetry data to send
   * @param requestedInterval the minimum interval (s) since the last transmission. NaN indicates
   *                          that a default transmission interval should be used
   *
   * @see com.qualcomm.robotcore.eventloop.EventLoop#TELEMETRY_DEFAULT_INTERVAL
   */
  public void refreshUserTelemetry(TelemetryMessage telemetry, double requestedInterval) {
    synchronized (this.refreshUserTelemetryLock) {

      // NaN is an indicator to use the default interval, whereas zero will
      // cause immediate transmission.
      if (Double.isNaN(requestedInterval))
        requestedInterval = userTelemetryInterval;

      // We'll do a transmission just to see the user's new data if a sufficient interval
      // has elapsed since the last time we did.
      boolean transmitBecauseOfUser = userTelemetryTimer.seconds() >= requestedInterval;

      // The modern and legacy motor controllers have *radically* different read times for the battery
      // voltage. For the modern controller, since the ReadWriteRunnable constantly polls this state,
      // the read is always out of data already in cache, and takes about 30 microseconds (as measured).
      // The legacy motor controller, on the other hand, because of the modality of the underlying
      // legacy module, doesn't always automatically read this data. Indeed, if the user is doing mostly
      // writes (as is often the case in OpModes that basically just set the motor power), the legacy
      // module won't be switch to read mode ever, *except* for a voltage request here, and that switch
      // will take tens of milliseconds. To *always* take that timing hit when refreshing user telemetry
      // is unreasonable.
      //
      // Instead, we adopt an adaptive strategy. We keep track of the battery read time statistics
      // and if they're small enough, then we transmit battery data whenever the user's going to
      // send data OR when a sufficiently long interval has elapsed. If the battery read times are
      // too large, then we only do the latter.

      double msThreshold = 2;
      boolean transmitBecauseOfBattery = (robotBatteryTimer.seconds() >= robotBatteryInterval)
              || (transmitBecauseOfUser && robotBatteryStatistics.getMean() < msThreshold);

      if (transmitBecauseOfUser || transmitBecauseOfBattery) {

        if (transmitBecauseOfUser) {
          userTelemetryTimer.reset();
        }

        if (transmitBecauseOfBattery) {
          telemetry.addData(EventLoopManager.ROBOT_BATTERY_LEVEL_KEY, buildRobotBatteryMsg());
          robotBatteryTimer.reset();
          if (robotBatteryLoggingTimer.seconds() > robotBatteryLoggingInterval) {
            RobotLog.i("robot battery read duration: n=%d, mean=%.3fms sd=%.3fms", robotBatteryStatistics.getCount(), robotBatteryStatistics.getMean(), robotBatteryStatistics.getStandardDeviation());
            robotBatteryLoggingTimer.reset();
          }
        }

        // Send if there's anything to send. If we send, then we always clear, as the current
        // data has already been send.
        if (telemetry.hasData()) {
          eventLoopManager.sendTelemetryData(telemetry);
          telemetry.clearData();
        }
      }
    }
  }

  /**
   * Send robot phone power % and robot battery voltage level to Driver station
   */
  public void sendBatteryInfo() {
    float percent = robotControllerBatteryChecker.getBatteryLevel();
    String batteryMessage = buildRobotBatteryMsg();
    if (batteryMessage != null) {
      sendTelemetry(EventLoopManager.RC_BATTERY_LEVEL_KEY, String.valueOf(percent));
      sendTelemetry(EventLoopManager.ROBOT_BATTERY_LEVEL_KEY, batteryMessage);
    }
  }

  /**
   * Build a string which indicates the lowest measured system voltage
   * @return String representing battery voltage
   */
  private String buildRobotBatteryMsg() {

    // Don't do anything if we're really early in the construction cycle
    if (this.hardwareMap==null) return null;

    double minBatteryLevel = Double.POSITIVE_INFINITY;

    // Determine the lowest battery voltage read from all motor controllers.
    //
    // If a voltage sensor becomes disconnected, it has been observed to read as zero.
    // Thus, we must account for that eventuality. While doing so, it's convenient for us
    // to rule out (other) unreasonable voltage levels in order to facilitate later string
    // conversion.
    //
    for (VoltageSensor sensor : this.hardwareMap.voltageSensor) {

      // Read the voltage, keeping track of how long it takes to do so
      long nanoBefore = System.nanoTime();
      double sensorVoltage = sensor.getVoltage();
      long nanoAfter = System.nanoTime();

      if (sensorVoltage >= 1.0 /* an unreasonable value to ever see in practice */) {
        // For valid reads, we add the read-duration to our statistics, in ms.
        robotBatteryStatistics.add((nanoAfter - nanoBefore) / (double) ElapsedTime.MILLIS_IN_NANO);

        // Keep track of the minimum valid value we find
        if (sensorVoltage < minBatteryLevel) {
          minBatteryLevel = sensorVoltage;
        }
      }
    }

    String msg;

    if (minBatteryLevel == Double.POSITIVE_INFINITY) {
      msg = NO_VOLTAGE_SENSOR;

    } else {
      // Convert double voltage into string with *two* decimal places (fast), given the
      // above-maintained fact the voltage is at least 1.0.
      msg = Integer.toString((int)(minBatteryLevel * 100));
      msg = new StringBuilder(msg).insert(msg.length()-2, ".").toString();
    }

    return (msg);
  }

  public void sendTelemetry(String tag, String msg) {
    TelemetryMessage telemetry = new TelemetryMessage();
    telemetry.setTag(tag);
    telemetry.addData(tag, msg);
    if (eventLoopManager != null) {
      eventLoopManager.sendTelemetryData(telemetry);
      telemetry.clearData();
    }
  }

  public void closeMotorControllers() {
    for (DcMotorController controller : hardwareMap.getAll(DcMotorController.class)) {
      controller.close();
    }
  }

  public void closeServoControllers()  {
    for (ServoController controller : hardwareMap.getAll(ServoController.class)) {
      controller.close();
    }
  }

  public void closeAllUsbDevices() {
    for (ArmableUsbDevice device : hardwareMap.getAll(ArmableUsbDevice.class)) {
      device.close();
    }
  }

  public void restartRobot() {
    DbgLog.error("restarting robot...");
    robotControllerBatteryChecker.endBatteryMonitoring();
    callback.restartRobot();
  }

  public String getOpMode(String extra) {
    if (eventLoopManager.state != RobotState.RUNNING) {
      return OpModeManager.DEFAULT_OP_MODE_NAME;
    }
    return extra;
  }

  public void updateBatteryLevel(float percent) {
    sendTelemetry(EventLoopManager.RC_BATTERY_LEVEL_KEY, String.valueOf(percent));
  }

}