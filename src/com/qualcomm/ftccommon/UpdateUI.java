/*
 * Copyright (c) 2015 Qualcomm Technologies Inc
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted (subject to the limitations in the disclaimer below) provided that
 * the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 * 
 * Neither the name of Qualcomm Technologies Inc nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 * 
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
 * LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.qualcomm.ftccommon;

import org.firstinspires.ftc.ftccommon.external.RobotStateMonitor;

import com.qualcomm.robotcore.eventloop.opmode.OpModeManager;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.util.RobotLog;

public class UpdateUI {

	/**
	 * Callback methods
	 */
	public class Callback {

		RobotStateMonitor stateMonitor = null;

		public RobotStateMonitor getStateMonitor() {
			return stateMonitor;
		}

		public void setStateMonitor(final RobotStateMonitor stateMonitor) {
			this.stateMonitor = stateMonitor;
		}

		/**
		 * callback method to restart the robot
		 */
		public void restartRobot() {
			AppUtil.getInstance().showToast(activity.getString(R.string.toastRestartingRobot));

			// this call might be coming from the event loop, so we need to start
			// switch contexts before proceeding.

			final Thread t = new Thread(() -> ThreadPool.logThreadLifeCycle("restart robot UI thunker", new Runnable() {
				@Override
				public void run() {

					// Wait for a second and half; reasons for wait are unclear
					try {
						Thread.sleep(1500);
					} catch (final InterruptedException ignored) {}

					// Actually restart the robot on the UI thread, just as the user would if
					// using the robot controller menus
					RobotLog.v("UpdateUI restarting robot..");
					activity.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							requestRobotRestart();
						}
					});
				}
			}));
			t.start();
		}

		public void updateUi(final String opModeName, final Gamepad[] gamepads) {
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (textGamepad != null) {
						for (int i = 0; i < textGamepad.length && i < gamepads.length; i++) {
							if (gamepads[i].id == Gamepad.ID_UNASSOCIATED) {
								setText(textGamepad[i], "");
							} else {
								setText(textGamepad[i], gamepads[i].toString());
							}
						}
					}

					String opModeShow;
					if (opModeName.equals(OpModeManager.DEFAULT_OP_MODE_NAME)) {
						opModeShow = activity.getString(R.string.defaultOpModeName);
					} else {
						opModeShow = opModeName;
					}
					setText(textOpMode, "Op Mode: " + opModeShow);

					refreshTextErrorMessage();
				}
			});
		}

		public void networkConnectionUpdate(final WifiDirectAssistant.Event event) {

			switch (event) {
				case UNKNOWN:
					updateNetworkConnectionStatus(NetworkStatus.UNKNOWN);
					break;
				case DISCONNECTED:
					updateNetworkConnectionStatus(NetworkStatus.INACTIVE);
					break;
				case CONNECTED_AS_GROUP_OWNER:
					updateNetworkConnectionStatus(NetworkStatus.ENABLED);
					break;
				case ERROR:
					updateNetworkConnectionStatus(NetworkStatus.ERROR);
					break;
				case CONNECTION_INFO_AVAILABLE:
					NetworkConnection networkConnection = controllerService.getNetworkConnection();
					displayDeviceName(networkConnection.getDeviceName());
					updateNetworkConnectionStatus(NetworkStatus.ACTIVE);
					break;
				case AP_CREATED:
					networkConnection = controllerService.getNetworkConnection();
					updateNetworkConnectionStatus(NetworkStatus.CREATED_AP_CONNECTION, networkConnection.getConnectionOwnerName());
					break;
				default:
					break;
			}
		}

		protected void displayDeviceName(final String name) {
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					textDeviceName.setText(name);
				}
			});
		}

		public void updateNetworkConnectionStatus(final NetworkStatus networkStatus) {
			if (UpdateUI.this.networkStatus != networkStatus) {
				UpdateUI.this.networkStatus = networkStatus;
				networkStatusExtra = null;
				if (stateMonitor != null) stateMonitor.updateNetworkStatus(networkStatus, null);
				refreshNetworkStatus();
			}
		}

		public void updateNetworkConnectionStatus(final NetworkStatus networkStatus, @NonNull final String extra) {
			if (UpdateUI.this.networkStatus != networkStatus || !extra.equals(networkStatusExtra)) {
				UpdateUI.this.networkStatus = networkStatus;
				networkStatusExtra = extra;
				if (stateMonitor != null) stateMonitor.updateNetworkStatus(networkStatus, extra);
				refreshNetworkStatus();
			}
		}

		public void updatePeerStatus(final PeerStatus peerStatus) {
			if (UpdateUI.this.peerStatus != peerStatus) {
				UpdateUI.this.peerStatus = peerStatus;
				if (stateMonitor != null) stateMonitor.updatePeerStatus(peerStatus);
				refreshNetworkStatus();
			}
		}

		void refreshNetworkStatus() {
			final String format = activity.getString(R.string.networkStatusFormat);
			final String strNetworkStatus = networkStatus.toString(activity, networkStatusExtra);
			final String strPeerStatus = peerStatus == PeerStatus.UNKNOWN ? "" : String.format(", %s", peerStatus.toString(activity));
			final String message = String.format(format, strNetworkStatus, strPeerStatus);

			// Log if changed
			if (DEBUG || !message.equals(networkStatusMessage)) RobotLog.v(message);
			networkStatusMessage = message;

			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					setText(textNetworkConnectionStatus, message);
				}
			});
		}

		public void updateRobotStatus(@NonNull final RobotStatus status) {
			robotStatus = status;
			if (stateMonitor != null) stateMonitor.updateRobotStatus(robotStatus);
			refreshStateStatus();
		}

		public void updateRobotState(@NonNull final RobotState state) {
			robotState = state;
			if (stateMonitor != null) stateMonitor.updateRobotState(robotState);
			refreshStateStatus();
		}

		protected void refreshStateStatus() {
			final String format = activity.getString(R.string.robotStatusFormat);
			final String state = robotState.toString(activity);
			final String status = robotStatus == RobotStatus.NONE ? "" : String.format(", %s", robotStatus.toString(activity));
			final String message = String.format(format, state, status);

			// Log if changed
			if (DEBUG || !message.equals(stateStatusMessage)) RobotLog.v(message);
			stateStatusMessage = message;

			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					setText(textRobotStatus, message);
					refreshTextErrorMessage();
				}
			});
		}

		public void refreshErrorTextOnUiThread() {
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					refreshTextErrorMessage();
				}
			});
		}

		void refreshTextErrorMessage() {

			final String errorMessage = RobotLog.getGlobalErrorMsg();
			final String warningMessage = RobotLog.getGlobalWarningMessage();

			if (!errorMessage.isEmpty() || !warningMessage.isEmpty()) {
				if (!errorMessage.isEmpty()) {
					final String message = activity.getString(R.string.error_text_error, trimTextErrorMessage(errorMessage));
					setText(textErrorMessage, message);
					if (stateMonitor != null) stateMonitor.updateErrorMessage(message);
				} else {
					final String message = activity.getString(R.string.error_text_warning, trimTextErrorMessage(warningMessage));
					setText(textErrorMessage, message);
					if (stateMonitor != null) stateMonitor.updateWarningMessage(message);
				}
				dimmer.longBright();
			} else {
				setText(textErrorMessage, "");
				if (stateMonitor != null) {
					stateMonitor.updateErrorMessage(null);
					stateMonitor.updateWarningMessage(null);
				}
			}
		}

		String trimTextErrorMessage(final String message) {
			// error text box is larger now; don't bother trimming
			return message;
		}

	}

	public static final boolean DEBUG = false;
	private static final int NUM_GAMEPADS = 2;

	protected TextView textDeviceName;
	protected TextView textNetworkConnectionStatus;
	protected TextView textRobotStatus;
	protected TextView[] textGamepad = new TextView[NUM_GAMEPADS];
	protected TextView textOpMode;
	protected TextView textErrorMessage;
	protected RobotState robotState = RobotState.NOT_STARTED;
	protected RobotStatus robotStatus = RobotStatus.NONE;
	protected NetworkStatus networkStatus = NetworkStatus.UNKNOWN;
	protected String networkStatusExtra = null;
	protected PeerStatus peerStatus = PeerStatus.DISCONNECTED;
	protected String networkStatusMessage = null;
	protected String stateStatusMessage = null;

	Restarter restarter;
	FtcRobotControllerService controllerService;

	Activity activity;
	Dimmer dimmer;

	public UpdateUI(final Activity activity, final Dimmer dimmer) {
		this.activity = activity;
		this.dimmer = dimmer;
	}

	public void setTextViews(final TextView textWifiDirectStatus, final TextView textRobotStatus, final TextView[] textGamepad, final TextView textOpMode, final TextView textErrorMessage, final TextView textDeviceName) {

		textNetworkConnectionStatus = textWifiDirectStatus;
		this.textRobotStatus = textRobotStatus;
		this.textGamepad = textGamepad;
		this.textOpMode = textOpMode;
		this.textErrorMessage = textErrorMessage;
		this.textDeviceName = textDeviceName;

	}

	protected void setText(final TextView textView, String message) {
		// Allow the view to be optional, change view visibility according to whether the message is empty or not
		if (textView != null && message != null) {
			message = message.trim();
			if (message.length() > 0) {
				textView.setText(message);
				textView.setVisibility(View.VISIBLE);
			} else {
				textView.setVisibility(View.INVISIBLE);
				textView.setText(" "); // paranoia: there are rumors of Android not doing a redraw if "" is used
			}
		}
	}

	public void setControllerService(final FtcRobotControllerService controllerService) {
		this.controllerService = controllerService;
	}

	public void setRestarter(final Restarter restarter) {
		this.restarter = restarter;
	}

	private void requestRobotRestart() {
		restarter.requestRestart();
	}

}
