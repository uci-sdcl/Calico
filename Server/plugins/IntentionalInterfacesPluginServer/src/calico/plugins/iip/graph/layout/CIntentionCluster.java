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

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import calico.components.CCanvas;
import calico.controllers.CCanvasController;
import calico.plugins.iip.CCanvasLink;
import calico.plugins.iip.CCanvasLinkAnchor;
import calico.plugins.iip.CIntentionCell;
import calico.plugins.iip.controllers.CCanvasLinkController;
import calico.plugins.iip.controllers.CIntentionCellController;

class CIntentionCluster
{
	static double getUnitSpan()
	{
		return new CIntentionCluster(-1L).getOccupiedSpan();
	}
	
	//git commit test
	private static final SliceSorter SLICE_SORTER = new SliceSorter();
	static final int RING_SEPARATION = 20 + CIntentionLayout.INTENTION_CELL_DIAMETER;
	static final Dimension CLUSTER_UNIT_SIZE = new Dimension(CIntentionLayout.INTENTION_CELL_SIZE.width /*+ 20*/, CIntentionLayout.INTENTION_CELL_SIZE.height /*+ 20*/);

	private final List<CIntentionRing> rings = new ArrayList<CIntentionRing>();
	private final List<Double> ringRadii = new ArrayList<Double>();
	private final Map<Long, CIntentionSlice> slicesByRootCanvasId = new LinkedHashMap<Long, CIntentionSlice>();
	private final long rootCanvasId;

	// transitory values per layout execution
	private final Point location = new Point();
	private final Dimension layoutSize = new Dimension();

	private boolean populated = false;

	public CIntentionCluster(long rootCanvasId)
	{
		this.rootCanvasId = rootCanvasId;
	}

	private void initializeRings()
	{
		rings.clear();
		ringRadii.clear();
	}

	long getRootCanvasId()
	{
		return rootCanvasId;
	}

	double getOccupiedSpan()
	{
		if (!populated)
			populateCluster();

		double clusterRadius;
		getRingRadii();
		if (ringRadii.isEmpty())
		{
			clusterRadius = CIntentionLayout.INTENTION_CELL_DIAMETER;
		}
		else
		{
			clusterRadius = ringRadii.get(ringRadii.size() - 1) + (CIntentionLayout.INTENTION_CELL_DIAMETER / 2.0);
		}
		return 2 * clusterRadius;
	}

	Point getLocation()
	{
		return location;
	}

	void reset()
	{
		populated = false;
	}

	void describeMaxProjectedSpans(StringBuilder buffer)
	{
		buffer.append("[");
		for (CIntentionRing ring : rings)
		{
			int maxProjectedSpan = 0;
			for (CIntentionSlice slice : slicesByRootCanvasId.values())
			{
				if (slice.getMaxProjectedSpan(ring.getIndex()) > maxProjectedSpan)
				{
					maxProjectedSpan = slice.getMaxProjectedSpan(ring.getIndex());
				}
			}

			buffer.append(ring.getIndex());
			buffer.append(": ");
			buffer.append(maxProjectedSpan);
			buffer.append("; ");
		}
		buffer.append("]");
	}

	void populateCluster()
	{
		if (populated)
			return;

		initializeRings();
		int totalInOrbit = 0;

		List<CIntentionSlice> slices = new ArrayList<CIntentionSlice>();
		for (long anchorId : CCanvasLinkController.getInstance().getAnchorIdsForCanvasId(rootCanvasId))
		{
			long linkedCanvasId = CCanvasLinkController.getInstance().getOpposite(anchorId).getCanvasId();
			if (CIntentionCellController.getInstance().getCellByCanvasId(linkedCanvasId).isPinned())
				continue;
			if (linkedCanvasId < 0L)
			{
				continue;
			}

			CIntentionSlice slice = new CIntentionSlice(linkedCanvasId);
			slices.add(slice);

			traverseAndPopulate(-1L, linkedCanvasId, 0, slice);
			totalInOrbit += slice.size();
		}

		slicesByRootCanvasId.clear();
//		Collections.sort(slices, SLICE_SORTER);
		for (CIntentionSlice slice : slices)
		{
			slicesByRootCanvasId.put(slice.getRootCanvasId(), slice);
		}

		weighSlices(totalInOrbit);
		populated = true;
	}

	List<Double> getRingRadii()
	{
		if (ringRadii.size() < rings.size())
		{
			ringRadii.clear();
			double lastRingRadius = 0.0;
			for (CIntentionRing ring : rings)
			{
				int ringSpan = 0;
				for (CIntentionSlice slice : slicesByRootCanvasId.values())
				{
					if (slice.getMaxProjectedSpan(ring.getIndex()) > ringSpan)
					{
						ringSpan = slice.getMaxProjectedSpan(ring.getIndex());
					}
				}

				double ringRadius = ringSpan / (2 * Math.PI);
				if (ringRadius < (lastRingRadius + RING_SEPARATION))
				{
					ringRadius = (lastRingRadius + RING_SEPARATION);
					ringSpan = (int) (2 * Math.PI * ringRadius);
				}

				ringRadii.add(ringRadius);
				lastRingRadius = ringRadius;
			}
		}
		return ringRadii;
	}

	Dimension getLayoutSize()
	{
		return layoutSize;
	}

	void setLocation(Point newLocation)
	{
		location.setLocation(newLocation);
	}

	CIntentionClusterLayout layoutClusterAsCircles(Point clusterCenter)
	{
		if (!populated)
			populateCluster();
		
		CIntentionClusterLayout layout = new CIntentionClusterLayout(this);

		layout.addCanvas(rootCanvasId, CIntentionLayout.centerCanvasAt(clusterCenter.x, clusterCenter.y));

		getRingRadii(); // make sure they match the rings
		for (int i = 0; i < ringRadii.size(); i++)
		{
			double ringRadius = ringRadii.get(i);
			int ringSpan = (int) (2 * Math.PI * ringRadius);

			int sliceStart = 0;
			CIntentionArcTransformer arcTransformer = null;
			for (CIntentionSlice slice : slicesByRootCanvasId.values())
			{
				if (arcTransformer == null)
				{
					arcTransformer = new CIntentionArcTransformer(clusterCenter, ringRadius, ringSpan, slice.calculateLayoutSpan(ringSpan));
				}
				slice.layoutArc(arcTransformer, i, ringSpan, sliceStart, layout, (i == 0) ? null : ringRadii.get(i - 1));
				sliceStart += slice.getLayoutSpan();
			}
		}

		if (ringRadii.isEmpty())
		{
			layoutSize.setSize(CIntentionLayout.INTENTION_CELL_DIAMETER, CIntentionLayout.INTENTION_CELL_DIAMETER);
		}
		else
		{
			layoutSize.setSize((int) (ringRadii.get(ringRadii.size() - 1) * 2), (int) (ringRadii.get(ringRadii.size() - 1) * 2));
		}

		return layout;
	}
	
	public List<CIntentionCell> getAllCanvasesInCluster()
	{
		List<CIntentionCell> set = new ArrayList<CIntentionCell>();
		for (long anchorId : CCanvasLinkController.getInstance().getAnchorIdsForCanvasId(rootCanvasId))
		{
			long linkedCanvasId = CCanvasLinkController.getInstance().getOpposite(anchorId).getCanvasId();

			if (linkedCanvasId < 0L)
			{
				continue;
			}

			set.add(CIntentionCellController.getInstance().getCellByCanvasId(linkedCanvasId));

			getAllCanvasesInCluster(linkedCanvasId, set);
		}
		
		return set;
	}
	
	private void getAllCanvasesInCluster(long canvasId, List<CIntentionCell> set)
	{
		set.add(CIntentionCellController.getInstance().getCellByCanvasId(canvasId));
		
		for (long anchorId : CCanvasLinkController.getInstance().getAnchorIdsForCanvasId(canvasId))
		{
			CCanvasLinkAnchor anchor = CCanvasLinkController.getInstance().getAnchor(anchorId);
			CCanvasLink link = CCanvasLinkController.getInstance().getLink(anchor.getLinkId());
			if (link.getAnchorB().getId() == anchorId)
			{
				continue;
			}
			long linkedCanvasId = CCanvasLinkController.getInstance().getOpposite(anchorId).getCanvasId();

			if (linkedCanvasId < 0L)
			{
				continue; // this is not a canvas, nothing is here
			}
			getAllCanvasesInCluster(linkedCanvasId, set);
		}
		
	}

	private void traverseAndPopulate(long parentCanvasId, long canvasId, int ringIndex, CIntentionSlice slice)
	{
		CIntentionRing ring = getRing(ringIndex);
		ring.addCanvas(canvasId);

		slice.addCanvas(parentCanvasId, canvasId, ringIndex);

		for (long anchorId : CCanvasLinkController.getInstance().getAnchorIdsForCanvasId(canvasId))
		{
			CCanvasLinkAnchor anchor = CCanvasLinkController.getInstance().getAnchor(anchorId);
			CCanvasLink link = CCanvasLinkController.getInstance().getLink(anchor.getLinkId());
			if (link.getAnchorB().getId() == anchorId)
			{
				continue;
			}
			long linkedCanvasId = CCanvasLinkController.getInstance().getOpposite(anchorId).getCanvasId();
			if (CIntentionCellController.getInstance().getCellByCanvasId(linkedCanvasId).isPinned())
				continue;
			if (linkedCanvasId < 0L)
			{
				continue; // this is not a canvas, nothing is here
			}
			traverseAndPopulate(canvasId, linkedCanvasId, ringIndex + 1, slice);
		}
	}

	private void weighSlices(int totalInOrbit)
	{
		if (totalInOrbit == 0)
		{
			return;
		}

		for (CIntentionSlice slice : slicesByRootCanvasId.values())
		{
			slice.setPopulationWeight(totalInOrbit);
		}

		double minimumRingRadius = 0.0;
		double equalSliceWeight = 1.0 / (double) slicesByRootCanvasId.size();
		for (CIntentionRing ring : rings)
		{
			minimumRingRadius += RING_SEPARATION;
			double minimumRingSpan = 2 * Math.PI * minimumRingRadius;
			int maxCellsInMinRingSpan = (int) (minimumRingSpan / CIntentionLayout.INTENTION_CELL_DIAMETER);
			boolean ringCrowded = ring.size() > maxCellsInMinRingSpan;

			int maxCellsInEqualSliceSpan = (maxCellsInMinRingSpan / slicesByRootCanvasId.size());
			boolean equalSlicesCrowded = false;
			for (CIntentionSlice slice : slicesByRootCanvasId.values())
			{
				if (slice.arcSize(ring.getIndex()) > maxCellsInEqualSliceSpan)
				{
					equalSlicesCrowded = true;
					break;
				}
			}

			for (CIntentionSlice slice : slicesByRootCanvasId.values())
			{
				double arcWeight;
				if (ringCrowded)
				{
					arcWeight = slice.arcSize(ring.getIndex()) / (double) ring.size();
				}
				else if (equalSlicesCrowded)
				{
					arcWeight = (slice.arcSize(ring.getIndex()) * CIntentionLayout.INTENTION_CELL_DIAMETER) / minimumRingSpan;
				}
				else
				{
					arcWeight = equalSliceWeight;
				}
				slice.setArcWeight(ring.getIndex(), arcWeight);
			}
		}

		double sumOfMaxWeights = 0.0;
		for (CIntentionSlice slice : slicesByRootCanvasId.values())
		{
			slice.calculateMaxArcWeight();
			sumOfMaxWeights += slice.getMaxArcWeight();
		}

		double reductionRatio = 1.0 / Math.max(1.0, sumOfMaxWeights);
		for (CIntentionSlice slice : slicesByRootCanvasId.values())
		{
			slice.setWeight(slice.getMaxArcWeight() * reductionRatio);
		}

		// what percentage of the minimum ring span is occupied by slice a? If it is less than the weighted percentage,
		// then it only needs that much.

		// Distributions:
		// 1. weighted
		// 2. equal
		// 3. by occupancy at minimum ring size

		// the idea is to choose a distribution per ring, normalize each one, and then balance maximi per slice
	}

	private CIntentionRing getRing(int ringIndex)
	{
		for (int i = rings.size(); i <= ringIndex; i++)
		{
			rings.add(new CIntentionRing(i));
		}
		return rings.get(ringIndex);
	}

	private static class SliceSorter implements Comparator<CIntentionSlice>
	{
		public int compare(CIntentionSlice first, CIntentionSlice second)
		{
			CCanvas firstCanvas = CCanvasController.canvases.get(first.getRootCanvasId());
			CCanvas secondCanvas = CCanvasController.canvases.get(second.getRootCanvasId());

			return firstCanvas.getIndex() - secondCanvas.getIndex();
		}
	}
}
