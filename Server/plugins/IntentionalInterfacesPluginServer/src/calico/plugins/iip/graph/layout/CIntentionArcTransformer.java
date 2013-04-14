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
package calico.plugins.iip.graph.layout;

import java.awt.Point;

class CIntentionArcTransformer
{
	private final Point center;
	private final double radius;
	private final int ringSpan;
	private final double offset;

	CIntentionArcTransformer(Point center, double radius, int ringSpan, int firstArcSpan)
	{
		this.center = center;
		this.radius = radius;
		this.ringSpan = ringSpan;

		offset = -1 * firstArcSpan/2 - ringSpan / 4; //((7 * ringSpan) / 8.0) - (firstArcSpan / 2.0);

		// System.out.println("Offset " + offset + " for radius " + radius + " and ring span " + ringSpan +
		// " and first arc " + firstArcSpan);
	}

	Point centerCanvasAt(int xArc)
	{
		int xShiftedArc = (int) ((xArc + offset) % ringSpan);
		return centerCanvasAt(xShiftedArc, center, radius);
	}

	double calculateIdealPosition(int parentArcPosition, double parentRingRadius)
	{
		return (radius / parentRingRadius) * parentArcPosition;
	}

	private Point centerCanvasAt(int xArc, Point center, double radius)
	{
		double theta = xArc / radius;
		int x = center.x + (int) (radius * Math.cos(theta));
		int y = center.y + (int) (radius * Math.sin(theta));

		// System.out.println(String.format("[%d] (%d, %d) for xArc %d and radius %f",
		// CIntentionLayout.getCanvasIndex(canvasId), x, y, xArc, radius));

		return CIntentionLayout.centerCanvasAt(x, y);
	}
}
