/*
 * Copyright (c) 2016 Robert Atkinson
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
 * Neither the name of Robert Atkinson nor the names of his contributors may be used to
 * endorse or promote products derived from this software without specific prior
 * written permission.
 * 
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
 * LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESSFOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.qualcomm.robotcore.hardware;

/**
 * The ServoEx interface provides enhanced servo functionality which is available with some
 * hardware devices. The ServoEx interface is typically used as a second interface on an object
 * whose primary interface is Servo or CRServo. To access it, cast your Servo or CRServo object
 * to ServoEx. However, it is perhaps prudent to first test whether the cast will succeed by
 * testing using 'instanceof'.
 * 
 * @see DcMotorEx
 */
public interface ServoEx {
	/**
	 * Sets the PWM range limits for the servo
	 * 
	 * @param range the new PWM range limits for the servo
	 * @see #getPwmRange()
	 */
	void setPwmRange(ServoPwmRange range);

	/**
	 * Returns the current PWM range limits for the servo
	 * 
	 * @return the current PWM range limits for the servo
	 * @see #setPwmRange(ServoPwmRange)
	 */
	ServoPwmRange getPwmRange();

	/**
	 * Individually energizes the PWM for this particular servo.
	 * 
	 * @see #setPwmDisable()
	 * @see #isPwmEnabled()
	 */
	void setPwmEnable();

	/**
	 * Individually denergizes the PWM for this particular servo
	 * 
	 * @see #setPwmEnable()
	 */
	void setPwmDisable();

	/**
	 * Returns whether the PWM is energized for this particular servo
	 * 
	 * @see #setPwmEnable()
	 */
	boolean isPwmEnabled();

	/**
	 * ServoPwmRange instances are used to specify the upper and lower pulse widths
	 * and overall framing rate for a servo.
	 *
	 * @see <a href="http://www.endurance-rc.com/ppmtut.php">Guide to PWM and PPM</a>
	 * @see <a href="https://www.servocity.com/html/hs-485hb_servo.html">HS-485HB servo information</a>
	 */
	class ServoPwmRange {
		/** usFrameDefault is the default frame rate used, in microseconds */
		public static final double usFrameDefault = 20000;

		/** defaultRange is the default PWM range used */
		public final static ServoPwmRange defaultRange = new ServoPwmRange(600, 2400);

		/** usPulseLower is the minimum PWM rate used, in microseconds. This corresponds to a servo position of 0.0. */
		public final double usPulseLower;
		/** usPulseLower is the maximum PWM rate used, in microseconds. This corresponds to a servo position of 1.0. */
		public final double usPulseUpper;
		/** usFrame is the rate, in microseconds, at which the PWM is transmitted. */
		public final double usFrame;

		/**
		 * Creates a new ServoPwmRange with the indicated lower and upper bounds and the default
		 * framing rate.
		 * 
		 * @param usPulseLower the minimum PWM rate used, in microsecond
		 * @param usPulseUpper the maximum PWM rate used, in microseconds
		 */
		public ServoPwmRange(final double usPulseLower, final double usPulseUpper) {
			this(usPulseLower, usPulseUpper, usFrameDefault);
		}

		/**
		 * Creates a new ServoPwmRange with the indicated lower and upper bounds and the specified
		 * framing rate.
		 * 
		 * @param usPulseLower the minimum PWM rate used, in microsecond
		 * @param usPulseUpper the maximum PWM rate used, in microseconds
		 * @param usFrame the framing rate, in microseconds
		 */
		public ServoPwmRange(final double usPulseLower, final double usPulseUpper, final double usFrame) {
			this.usPulseLower = usPulseLower;
			this.usPulseUpper = usPulseUpper;
			this.usFrame = usFrame;
		}

		@Override
		public boolean equals(final Object o) {
			if (o instanceof ServoPwmRange) {
				final ServoPwmRange him = (ServoPwmRange) o;
				return usPulseLower == him.usPulseLower && usPulseUpper == him.usPulseUpper && usFrame == him.usFrame;
			}
			return false;
		}

		@Override
		public int hashCode() {
			return ((Double) usPulseLower).hashCode() ^ ((Double) usPulseUpper).hashCode() ^ ((Double) usFrame).hashCode();
		}
	}
}
