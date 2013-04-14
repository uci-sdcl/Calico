/*******************************************************************************
 * Copyright (c) 2013, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the
 * distribution.
 * 
 * None of the name of the Regents of the University of California, or the names of its
 * contributors may be used to endorse or promote products derived from this software without specific
 * prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package calico.plugins.iip.components.piemenu;

import java.awt.Point;
import java.util.TimerTask;

import calico.CalicoOptions;
import calico.inputhandlers.CalicoAbstractInputHandler.MenuAnimation;
import edu.umd.cs.piccolo.PLayer;

/**
 * Pie menu animation task.
 *
 * @author Byron Hawkins
 */
public abstract class PieMenuTimerTask extends TimerTask
{
	private PLayer layer;
	private Point point;

	protected abstract void animationCompleted();

	protected void startAnimation(PLayer layer, Point point)
	{
		this.layer = layer;
		this.point = point;

		Animation animation = new Animation();
		animation.start();
	}

	private class Animation extends MenuAnimation
	{
		Animation()
		{
			super(layer, CalicoOptions.pen.press_and_hold_menu_animation_duration, CalicoOptions.core.max_hold_distance, point);

			setStartTime(System.currentTimeMillis());
			setStepRate(CalicoOptions.pen.press_and_hold_menu_animation_tick_rate);
		}

		@Override
		protected void activityStep(long elapsedTime)
		{
			super.activityStep(elapsedTime);

			if (animateStep(elapsedTime))
			{
				terminate();
			}
		}

		@Override
		protected void activityFinished()
		{
			cleanup();
			animationCompleted();
		}
	}
}
