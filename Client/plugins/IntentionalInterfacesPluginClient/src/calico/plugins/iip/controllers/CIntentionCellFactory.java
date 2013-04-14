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
package calico.plugins.iip.controllers;

import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import calico.components.CCanvas;
import calico.controllers.CCanvasController;
import calico.plugins.iip.components.CIntentionCell;
import calico.plugins.iip.components.canvas.CanvasInputProximity;

/**
 * Manages this plugin's role in the 2-phase process of creating a new canvas, which consumers request via
 * <code>createNewCell()</code>. First a new canvas is created by request to the <code>CCanvasController.Factory</code>.
 * When the server responds with the new canvas, this class continues by creating and returning a new CIC.
 * 
 * @author Byron Hawkins
 */
public class CIntentionCellFactory
{
	private static final CIntentionCellFactory INSTANCE = new CIntentionCellFactory();

	public static CIntentionCellFactory getInstance()
	{
		return INSTANCE;
	}

	private final Long2ReferenceOpenHashMap<PendingCell> pendingCellsByCanvasId = new Long2ReferenceOpenHashMap<PendingCell>();

	public CIntentionCell createNewCell()
	{
		return createNewCell(0L, CanvasInputProximity.NONE);
	}

	public CIntentionCell createNewCell(long originatingCanvasId, CanvasInputProximity proximity)
	{
		PendingCell pendingCell = new PendingCell();

		synchronized (pendingCellsByCanvasId)
		{
			long canvasId = CCanvasController.Factory.getInstance().createNewCanvas(originatingCanvasId).uuid;
			pendingCellsByCanvasId.put(canvasId, pendingCell);

			if (originatingCanvasId > 0L)
			{
				IntentionCanvasController.getInstance().canvasCreatedLocally(canvasId, originatingCanvasId, proximity);
			}
		}

		return pendingCell.waitForCell();
	}

	public void cellCreated(CIntentionCell cell)
	{
		synchronized (pendingCellsByCanvasId)
		{
			PendingCell pendingCell = pendingCellsByCanvasId.get(cell.getCanvasId());
			if (pendingCell != null)
			{
				cell.setNew(true);
				pendingCellsByCanvasId.remove(cell.getCanvasId());
				pendingCell.cellArrived(cell);
			}
		}
	}

	/**
	 * Synchronization device to wait for the new canvas to be created on the server.
	 * 
	 * @author Byron Hawkins
	 */
	private class PendingCell
	{
		private CIntentionCell cell = null;

		synchronized void cellArrived(CIntentionCell cell)
		{
			this.cell = cell;
			notify();
		}

		synchronized CIntentionCell waitForCell()
		{
			while (cell == null)
			{
				try
				{
					wait();
				}
				catch (InterruptedException ok)
				{
				}
			}

			return cell;
		}
	}
}
