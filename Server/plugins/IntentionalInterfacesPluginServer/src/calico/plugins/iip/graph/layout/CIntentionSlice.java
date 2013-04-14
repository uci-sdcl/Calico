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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CIntentionSlice
{
	private static final DecimalFormat WEIGHT_FORMAT = new DecimalFormat("0.00");

	private final long rootCanvasId;
	private final List<Long> canvasIds = new ArrayList<Long>();
	private final List<Arc> arcs = new ArrayList<Arc>();
	private final Map<Long, Integer> arcPositions = new HashMap<Long, Integer>();

	// transitory per layout execution
	private double populationWeight;
	private double maxArcWeight;
	private double assignedWeight;
	private int layoutSpan;
	boolean debug = false;

	CIntentionSlice(long rootCanvasId)
	{
		this.rootCanvasId = rootCanvasId;
	}

	long getRootCanvasId()
	{
		return rootCanvasId;
	}

	void addCanvas(long parentCanvasId, long canvasId, int ringIndex)
	{
		canvasIds.add(canvasId);
		getArc(ringIndex).addCanvas(parentCanvasId, canvasId);
	}

	int getLayoutSpan()
	{
		return layoutSpan;
	}

	int size()
	{
		return canvasIds.size();
	}

	int arcSize(int ringIndex)
	{
		return getArc(ringIndex).canvasCount;
	}

	void setPopulationWeight(int totalInOrbit)
	{
		populationWeight = (canvasIds.size() / (double) totalInOrbit);
	}

	public double getWeight()
	{
		return assignedWeight;
	}

	void setWeight(double weight)
	{
		assignedWeight = weight;

		if (debug)
			System.out.println(String.format("Slice for canvas %d has %d canvases and max weight %s with normalized weight %s%%",
				CIntentionLayout.getCanvasIndex(rootCanvasId), canvasIds.size(), WEIGHT_FORMAT.format(getMaxArcWeight()), toPercent(assignedWeight)));

		for (Arc arc : arcs)
		{
			arc.calculateArcSpanProjection();
			
			if (debug)
				System.out.println(String.format("Slice for canvas %d has projected span %d for ring %d", CIntentionLayout.getCanvasIndex(rootCanvasId),
					arc.arcSpanProjection, arc.ringIndex));
		}
	}

	void setArcWeight(int ringIndex, double weight)
	{
		getArc(ringIndex).setWeight(weight);
	}

	void calculateMaxArcWeight()
	{
		maxArcWeight = 0.0;
		for (Arc arc : arcs)
		{
			if (arc.weight > maxArcWeight)
			{
				maxArcWeight = arc.weight;
			}
		}
	}

	double getMaxArcWeight()
	{
		return maxArcWeight;
	}

	int getMaxProjectedSpan(int ringIndex)
	{
		return getArc(ringIndex).arcSpanProjection;
	}

	int calculateLayoutSpan(int ringSpan)
	{
		return (int) (ringSpan * assignedWeight);
	}

	void layoutArc(CIntentionArcTransformer arcTransformer, int ringIndex, int ringSpan, int arcStart, CIntentionClusterLayout layout, Double parentRingRadius)
	{
		int sliceWidth = calculateLayoutSpan(ringSpan);

		Arc arc = arcs.get(ringIndex);
		if (!arc.isEmpty())
		{
			int arcOccupancySpan = (arc.canvasCount - 1) * CIntentionLayout.INTENTION_CELL_DIAMETER;
			int xArc = arcStart + ((sliceWidth - arcOccupancySpan) / 2);

			List<GroupCollision> calculatedCollisions = new ArrayList<GroupCollision>();
			if (parentRingRadius != null)
			{
				List<GroupCollision> collisionsInEffect = new ArrayList<GroupCollision>();

				double leftBoundary = arcStart;
				CanvasGroup previousGroup = null;
				for (CanvasGroup group : arc.canvasGroups.values())
				{
					group.idealPosition = arcTransformer.calculateIdealPosition(arcPositions.get(group.parentCanvasId), parentRingRadius);

					if (debug)
						System.out.println("Ideal position for group of arc " + ringIndex + " in slice for canvas " + CIntentionLayout.getCanvasIndex(rootCanvasId)
							+ ": " + group.idealPosition + " in (" + arcStart + " - " + (arcStart + sliceWidth) + ")");

					double idealStart = group.idealPosition - (group.getSpan() / 2.0);

					for (int i = (collisionsInEffect.size() - 1); i >= 0; i--)
					{
						GroupCollision collision = collisionsInEffect.get(i);
						if (collision.currentLeftBoundary > idealStart)
						{
							collision.displace(group, collision.currentLeftBoundary - idealStart);
							collision.currentLeftBoundary += group.getSpan();
						}
						else
						{
							collisionsInEffect.remove(i);
							calculatedCollisions.add(collision);
						}
					}

					if (idealStart < leftBoundary)
					{
						if (leftBoundary != arcStart)
						{
							GroupCollision collision = new GroupCollision(previousGroup);
							collision.displace(group, (leftBoundary - idealStart));
							collision.currentLeftBoundary = leftBoundary + group.getSpan();
							collisionsInEffect.add(collision);
						}

						idealStart = leftBoundary;
					}

					leftBoundary = idealStart + group.getSpan();
					previousGroup = group;
				}

				calculatedCollisions.addAll(collisionsInEffect);
				collisionsInEffect.clear();

				for (GroupCollision collision : calculatedCollisions)
				{
					collision.describe();
				}
			}

			Map<CanvasGroup, Integer> displacements = new HashMap<CanvasGroup, Integer>();
			if (parentRingRadius == null)
			{
				displacements = null;
			}
			else
			{
				for (GroupCollision collision : calculatedCollisions)
				{
					if (collision.displacements.size() > 1)
					{
						displacements = null;
						break;
					}

					Displacement displacement = collision.displacements.get(0);
					displacements.put(displacement.displacedGroup, (int) displacement.displacementSpan);
				}
			}

			for (CanvasGroup group : arc.canvasGroups.values())
			{
				if (displacements != null)
				{
					xArc = (int) (group.idealPosition + (CIntentionLayout.INTENTION_CELL_DIAMETER / 2.0) - (group.getSpan() / 2.0));

					Integer displacement = displacements.get(group);
					if (displacement != null)
					{
						xArc += displacement;
					}
				}

				for (Long canvasId : group.groupCanvasIds)
				{
					layout.addCanvas(canvasId, arcTransformer.centerCanvasAt(xArc));
					arcPositions.put(canvasId, xArc);
					xArc += CIntentionLayout.INTENTION_CELL_DIAMETER;
				}
			}
		}

		layoutSpan = sliceWidth;
	}

	private Arc getArc(int ringIndex)
	{
		for (int i = arcs.size(); i <= ringIndex; i++)
		{
			arcs.add(new Arc(i));
		}
		return arcs.get(ringIndex);
	}

	private class Arc
	{
		private int ringIndex;
		private final Map<Long, CanvasGroup> canvasGroups = new LinkedHashMap<Long, CanvasGroup>();
		int canvasCount = 0;
		private double weight;
		private int arcSpanProjection;

		Arc(int ringIndex)
		{
			this.ringIndex = ringIndex;
		}

		void addCanvas(long parentCanvasId, long canvasId)
		{
			canvasCount++;
			getCanvasGroup(parentCanvasId).addCanvas(canvasId);
		}

		boolean isEmpty()
		{
			return canvasGroups.isEmpty();
		}

		void setWeight(double weight)
		{
			this.weight = weight;
		}

		void calculateArcSpanProjection()
		{
			arcSpanProjection = (int) ((canvasCount * CIntentionLayout.INTENTION_CELL_DIAMETER) * (1.0 / assignedWeight));
		}

		private CanvasGroup getCanvasGroup(long parentCanvasId)
		{
			CanvasGroup group = canvasGroups.get(parentCanvasId);
			if (group == null)
			{
				group = new CanvasGroup(parentCanvasId);
				canvasGroups.put(parentCanvasId, group);
			}
			return group;
		}
	}

	private class GroupCollision
	{
		private final CanvasGroup ideallyPlacedGroup;
		private final List<Displacement> displacements = new ArrayList<Displacement>();

		// transitory during computation
		private double currentLeftBoundary;

		GroupCollision(CanvasGroup ideallyPlacedGroup)
		{
			this.ideallyPlacedGroup = ideallyPlacedGroup;
		}

		void displace(CanvasGroup group, double span)
		{
			displacements.add(new Displacement(group, span));
		}

		void describe()
		{
			double totalSpan = 0.0;
			for (Displacement displacement : displacements)
			{
				totalSpan += displacement.displacementSpan;
			}

			if (debug)
				System.out.println("Collision for group with parent " + CIntentionLayout.getCanvasIndex(ideallyPlacedGroup.parentCanvasId) + ": "
					+ displacements.size() + " displacements totaling " + ((int) totalSpan) + " arc pixels.");
		}
	}

	private class Displacement
	{
		private final CanvasGroup displacedGroup;
		private final double displacementSpan;

		Displacement(CanvasGroup displacedGroup, double displacementSpan)
		{
			this.displacedGroup = displacedGroup;
			this.displacementSpan = displacementSpan;
		}
	}

	private class CanvasGroup
	{
		private final long parentCanvasId;
		private final List<Long> groupCanvasIds = new ArrayList<Long>();

		// transitory
		double idealPosition;

		CanvasGroup(long parentCanvasId)
		{
			this.parentCanvasId = parentCanvasId;
		}

		void addCanvas(long canvasId)
		{
			groupCanvasIds.add(canvasId);
		}

		int getSpan()
		{
			return groupCanvasIds.size() * CIntentionLayout.INTENTION_CELL_DIAMETER;
		}
	}

	private static String toPercent(double d)
	{
		return String.valueOf((int) (d * 100.0));
	}
}
