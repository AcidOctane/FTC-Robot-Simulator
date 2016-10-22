/*
 * Copyright (c) 2014, 2015 Qualcomm Technologies Inc
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

package com.qualcomm.robotcore.hardware;

/**
 * Control a DC Motor attached to a DC Motor Controller
 *
 * @see com.qualcomm.robotcore.hardware.DcMotorController
 */
public class DcMotorImpl implements DcMotor {

	// ------------------------------------------------------------------------------------------------
	// State
	// ------------------------------------------------------------------------------------------------

	protected DcMotorController controller = null;
	protected int portNumber = -1;
	protected Direction direction = Direction.FORWARD;
	protected RunMode mode = RunMode.RUN_WITHOUT_ENCODER;

	// ------------------------------------------------------------------------------------------------
	// Construction
	// ------------------------------------------------------------------------------------------------

	/**
	 * Constructor
	 *
	 * @param controller DC motor controller this motor is attached to
	 * @param portNumber portNumber position on the controller
	 */
	public DcMotorImpl(final DcMotorController controller, final int portNumber) {
		this(controller, portNumber, Direction.FORWARD);
	}

	/**
	 * Constructor
	 *
	 * @param controller DC motor controller this motor is attached to
	 * @param portNumber portNumber port number on the controller
	 * @param direction direction this motor should spin
	 */
	public DcMotorImpl(final DcMotorController controller, final int portNumber, final Direction direction) {
		this.controller = controller;
		this.portNumber = portNumber;
		this.direction = direction;
	}

	// ------------------------------------------------------------------------------------------------
	// HardwareDevice interface
	// ------------------------------------------------------------------------------------------------

	@Override
	public Manufacturer getManufacturer() {
		return controller.getManufacturer();
	}

	@Override
	public String getDeviceName() {
		return "DC Motor";
	}

	@Override
	public String getConnectionInfo() {
		return controller.getConnectionInfo() + "; port " + portNumber;
	}

	@Override
	public int getVersion() {
		return 1;
	}

	@Override
	public void resetDeviceConfigurationForOpMode() {
		setDirection(Direction.FORWARD);
	}

	@Override
	public void close() {
		setPowerFloat();
	}

	// ------------------------------------------------------------------------------------------------
	// DcMotor interface
	// ------------------------------------------------------------------------------------------------

	/**
	 * Get DC motor controller
	 *
	 * @return controller
	 */
	@Override
	public DcMotorController getController() {
		return controller;
	}

	/**
	 * Set the direction
	 *
	 * @param direction direction
	 */
	@Override
	synchronized public void setDirection(final Direction direction) {
		this.direction = direction;
	}

	/**
	 * Get the direction
	 *
	 * @return direction
	 */
	@Override
	public Direction getDirection() {
		return direction;
	}

	/**
	 * Get port number
	 *
	 * @return portNumber
	 */
	@Override
	public int getPortNumber() {
		return portNumber;
	}

	/**
	 * Set the current motor power
	 *
	 * @param power from -1.0 to 1.0
	 */
	@Override
	synchronized public void setPower(double power) {
		if (direction == Direction.REVERSE) power = -power;
		// Power must be positive when in RUN_TO_POSITION mode
		if (mode == RunMode.RUN_TO_POSITION) power = Math.abs(power);
		internalSetPower(power);
	}

	protected void internalSetPower(final double power) {
		controller.setMotorPower(portNumber, power);
	}

	@Override
	public void setMaxSpeed(final int encoderTicksPerSecond) {
		controller.setMotorMaxSpeed(portNumber, encoderTicksPerSecond);
	}

	@Override
	public int getMaxSpeed() {
		return controller.getMotorMaxSpeed(portNumber);
	}

	/**
	 * Get the current motor power
	 *
	 * @return scaled from -1.0 to 1.0
	 */
	@Override
	synchronized public double getPower() {
		double power = controller.getMotorPower(portNumber);
		if (direction == Direction.REVERSE) power = -power;
		return power;
	}

	/**
	 * Is the motor busy?
	 *
	 * @return true if the motor is busy
	 */
	@Override
	public boolean isBusy() {
		return controller.isBusy(portNumber);
	}

	@Override
	public synchronized void setZeroPowerBehavior(final ZeroPowerBehavior zeroPowerBehavior) {
		controller.setMotorZeroPowerBehavior(portNumber, zeroPowerBehavior);
	}

	@Override
	public synchronized ZeroPowerBehavior getZeroPowerBehavior() {
		return controller.getMotorZeroPowerBehavior(portNumber);
	}

	/**
	 * Allow motor to float
	 */
	@Override
	@Deprecated
	public synchronized void setPowerFloat() {
		setZeroPowerBehavior(ZeroPowerBehavior.FLOAT);
		setPower(0.0);
	}

	/**
	 * Is motor power set to float?
	 *
	 * @return true of motor is set to float
	 */
	@Override
	public synchronized boolean getPowerFloat() {
		return getZeroPowerBehavior() == ZeroPowerBehavior.FLOAT && getPower() == 0.0;
	}

	/**
	 * Set the motor target position, using an integer. If this motor has been set to REVERSE,
	 * the passed-in "position" value will be multiplied by -1.
	 *
	 * @param position range from Integer.MIN_VALUE to Integer.MAX_VALUE
	 *
	 */
	@Override
	synchronized public void setTargetPosition(int position) {
		if (direction == Direction.REVERSE) position *= -1;
		internalSetTargetPosition(position);
	}

	protected void internalSetTargetPosition(final int position) {
		controller.setMotorTargetPosition(portNumber, position);
	}

	/**
	 * Get the current motor target position. If this motor has been set to REVERSE, the returned
	 * "position" will be multiplied by -1.
	 *
	 * @return integer, unscaled
	 */
	@Override
	synchronized public int getTargetPosition() {
		int position = controller.getMotorTargetPosition(portNumber);
		if (direction == Direction.REVERSE) position *= -1;
		return position;
	}

	/**
	 * Get the current encoder value. If this motor has been set to REVERSE, the returned "position"
	 * will be multiplied by -1.
	 *
	 * @return double indicating current position
	 */
	@Override
	synchronized public int getCurrentPosition() {
		int position = controller.getMotorCurrentPosition(portNumber);
		if (direction == Direction.REVERSE) position *= -1;
		return position;
	}

	/**
	 * Set the current mode
	 *
	 * @param mode run mode
	 */
	@Override
	synchronized public void setMode(RunMode mode) {
		mode = mode.migrate();
		this.mode = mode;
		internalSetMode(mode);
	}

	protected void internalSetMode(final RunMode mode) {
		controller.setMotorMode(portNumber, mode);
	}

	/**
	 * Get the current mode
	 *
	 * @return run mode
	 */
	@Override
	public RunMode getMode() {
		return controller.getMotorMode(portNumber);
	}
}
