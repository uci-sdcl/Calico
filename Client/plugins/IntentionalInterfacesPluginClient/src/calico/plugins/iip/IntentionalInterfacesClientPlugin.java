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
package calico.plugins.iip;

import java.awt.Point;
import java.util.ArrayList;

import javax.swing.SwingUtilities;

import calico.Calico;
import calico.CalicoOptions;
import calico.components.bubblemenu.BubbleMenu;
import calico.components.menus.CanvasMenuBar;
import calico.components.menus.CanvasStatusBar;
import calico.components.menus.buttons.HistoryNavigationBackButton;
import calico.components.menus.buttons.HistoryNavigationForwardButton;
import calico.components.menus.buttons.SpacerButton;
import calico.controllers.CCanvasController;
import calico.controllers.CHistoryController;
import calico.events.CalicoEventHandler;
import calico.events.CalicoEventListener;
import calico.networking.Networking;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;
import calico.perspectives.CalicoPerspective;
import calico.perspectives.CanvasPerspective;
import calico.plugins.CalicoPlugin;
import calico.plugins.iip.components.CCanvasLink;
import calico.plugins.iip.components.CCanvasLinkAnchor;
import calico.plugins.iip.components.CIntentionCell;
import calico.plugins.iip.components.CIntentionType;
import calico.plugins.iip.components.canvas.CanvasTagPanel;
import calico.plugins.iip.components.canvas.CanvasTitlePanel;
import calico.plugins.iip.components.canvas.CopyCanvasButton;
import calico.plugins.iip.components.canvas.NewCanvasButton;
import calico.plugins.iip.components.canvas.TagPanelToolBarButton;
import calico.plugins.iip.components.graph.CIntentionTopology;
import calico.plugins.iip.components.graph.IntentionGraph;
import calico.plugins.iip.controllers.CCanvasLinkController;
import calico.plugins.iip.controllers.CIntentionCellController;
import calico.plugins.iip.controllers.CIntentionCellFactory;
import calico.plugins.iip.controllers.IntentionCanvasController;
import calico.plugins.iip.controllers.IntentionGraphController;
import calico.plugins.iip.controllers.IntentionalInterfacesCanvasContributor;
import calico.plugins.iip.iconsets.CalicoIconManager;
import calico.plugins.iip.inputhandlers.CIntentionCellInputHandler;
import calico.plugins.iip.perspectives.IntentionalInterfacesPerspective;

/**
 * Integration point for the Intention View with the Calico plugin mechanism. All network commands are received and
 * initially processed in <code>handleCalicoEvent()</code>.
 * 
 * @author Byron Hawkins
 */
public class IntentionalInterfacesClientPlugin extends CalicoPlugin implements CalicoEventListener
{
	private static ArrayList<Runnable> eventDispatcherQueue = new ArrayList<Runnable>();
	
	public IntentionalInterfacesClientPlugin()
	{
		super();

		PluginInfo.name = "Intentional Interfaces";
		CalicoIconManager.setIconTheme(this.getClass(), CalicoOptions.core.icontheme);
	}

	/**
	 * Registers for network command notification, initializes controllers, adds buttons to the Canvas View's menu bar.
	 */
	public void onPluginStart()
	{
		CalicoEventHandler.getInstance().addListener(NetworkCommand.VIEWING_SINGLE_CANVAS, this, CalicoEventHandler.PASSIVE_LISTENER);
		CalicoEventHandler.getInstance().addListener(NetworkCommand.CONSISTENCY_FINISH, this, CalicoEventHandler.PASSIVE_LISTENER);
		CalicoEventHandler.getInstance().addListener(NetworkCommand.PRESENCE_CANVAS_USERS, this, CalicoEventHandler.PASSIVE_LISTENER);
		CalicoEventHandler.getInstance().addListener(NetworkCommand.CANVAS_DELETE, this, CalicoEventHandler.PASSIVE_LISTENER);
		
		for (Integer event : this.getNetworkCommands())
		{
			CalicoEventHandler.getInstance().addListener(event.intValue(), this, CalicoEventHandler.ACTION_PERFORMER_LISTENER);
		}

		Networking.send(CalicoPacket.command(NetworkCommand.UUID_GET_BLOCK));

		while (Calico.numUUIDs() == 0)
		{
			try
			{
				Thread.sleep(100L);
			}
			catch (InterruptedException e)
			{
			}
		}

		IntentionalInterfacesCanvasContributor.initialize();
		CCanvasLinkController.initialize();
		CIntentionCellController.initialize();
		IntentionGraphController.initialize();
		IntentionCanvasController.initialize();
		IntentionalInterfacesPerspective.getInstance(); // load the class

		CanvasMenuBar.addMenuButtonPreAppend(NewCanvasButton.class);
//		CanvasMenuBar.addMenuButtonPreAppend(NewCanvasStepInto.class);
		CanvasMenuBar.addMenuButtonPreAppend(CopyCanvasButton.class);
		CanvasMenuBar.addMenuButtonPreAppend(SpacerButton.class);

		CanvasMenuBar.addMenuButtonPreAppend(HistoryNavigationBackButton.class);
		CanvasMenuBar.addMenuButtonPreAppend(HistoryNavigationForwardButton.class);
//		CanvasMenuBar.addMenuButtonPreAppend(ShowIntentionGraphButton.class);
		CanvasMenuBar.addMenuButtonPreAppend(SpacerButton.class);
		
		CanvasStatusBar.addMenuButtonRightAligned(TagPanelToolBarButton.class);

	}

	@Override
	public void handleCalicoEvent(int event, CalicoPacket p)
	{

//		if (IntentionalInterfacesNetworkCommands.Command.forId(event) != null) 
//		logger.debug("RX "+IntentionalInterfacesNetworkCommands.Command.forId(event).toString());

		switch (event)
		{
			case NetworkCommand.VIEWING_SINGLE_CANVAS:
				VIEWING_SINGLE_CANVAS(p);
				return;
			case NetworkCommand.CONSISTENCY_FINISH:
//				CCanvasLinkController.getInstance().initializeArrowColors();
//				CIntentionCellController.getInstance().updateUserLists();
//				calico.plugins.iip.components.graph.IntentionGraph.getInstance().updateZoom();
//				calico.plugins.iip.components.graph.IntentionGraph.getInstance().fitContents();
				return;
			case NetworkCommand.PRESENCE_CANVAS_USERS:
				if (Networking.connectionState != Networking.ConnectionState.Connecting)
					CIntentionCellController.getInstance().updateUserLists();
				return;
			case NetworkCommand.CANVAS_DELETE:
				if (CalicoPerspective.Active.getCurrentPerspective() instanceof CanvasPerspective)
				{
					p.rewind();
					p.getInt();
					long canvasId = p.getLong(); 
					if (canvasId ==  CCanvasController.getCurrentUUID())
						IntentionalInterfacesPerspective.getInstance().displayPerspective(IntentionGraph.WALL);
				}
				if (CalicoPerspective.Active.getCurrentPerspective() instanceof IntentionalInterfacesPerspective)
				{
					p.rewind();
					p.getInt();
					long canvasId = p.getLong(); 
					//If this is true, then someone across the network has deleted the CIC that local user
					//	has select. We must deselect it or all hell breaks loose.
					if (BubbleMenu.activeUUID == CIntentionCellInputHandler.getInstance().getActiveCell()
							&& CIntentionCellController.getInstance().getCellById(CIntentionCellInputHandler.getInstance().getActiveCell()).getCanvasId() == canvasId)
						BubbleMenu.clearMenu();
				}
				return;
		}

		switch (IntentionalInterfacesNetworkCommands.Command.forId(event))
		{
			case WALL_BOUNDS:
				WALL_BOUNDS(p);
				break;
			case CIC_UPDATE_FINISHED:
				CCanvasLinkController.getInstance().initializeArrowColors();
				CIntentionCellController.getInstance().updateUserLists();
				calico.plugins.iip.components.graph.IntentionGraph.getInstance().updateZoom();
				break;
			case CIC_CREATE:
				CIC_CREATE(p);
				break;
			case CIC_MOVE:
				CIC_MOVE(p);
				break;
			case CIC_SET_TITLE:
				CIC_SET_TITLE(p);
				break;
			case CIC_TAG:
				CIC_TAG(p);
				break;
			case CIC_UNTAG:
				CIC_UNTAG(p);
				break;
			case CIC_DELETE:
				CIC_DELETE(p);
				break;
			case CIC_TOPOLOGY:
				CIC_TOPOLOGY(p);
				break;
			case CIT_CREATE:
				CIT_CREATE(p);
				break;
			case CIT_RENAME:
				CIT_RENAME(p);
				break;
			case CIT_SET_COLOR:
				CIT_SET_COLOR(p);
				break;
			case CIT_DELETE:
				CIT_DELETE(p);
				break;
			case CLINK_CREATE:
				CLINK_CREATE(p);
				break;
			case CLINK_MOVE_ANCHOR:
				CLINK_MOVE_ANCHOR(p);
				break;
			case CLINK_LABEL:
				CLINK_LABEL(p);
				break;
			case CLINK_DELETE:
				CLINK_DELETE(p);
				break;
			case CIC_SET_PIN:
				CIC_SET_PIN(p);
				break;
			case EXECUTE_II_EVENT_DISPATCHER_EVENTS:
				EXECUTE_II_EVENT_DISPATCHER_EVENTS(p);
				break;
			case CIT_SET_DESCRIPTION:
				CIT_SET_DESCRIPTION(p);
				break;
		}
	}

	private static void VIEWING_SINGLE_CANVAS(CalicoPacket p)
	{
		p.rewind();
		p.getInt();
		long cuid = p.getLong();

		IntentionCanvasController.getInstance().canvasChanged(cuid);
	}

	private static void CIC_CREATE(CalicoPacket p)
	{
		p.rewind();
		IntentionalInterfacesNetworkCommands.Command.CIC_CREATE.verify(p);

		long uuid = p.getLong();
		long canvas_uuid = p.getLong();
		int x = p.getInt();
		int y = p.getInt();
		String title = p.getString();
		boolean isPinned = p.getBoolean();
		
//		System.out.println("CIC_CREATE, " + uuid + ", " + canvas_uuid + ", " + x + ", " + y);

		CIntentionCell cell = new CIntentionCell(uuid, canvas_uuid, new Point(x, y), title);
		CIntentionCellController.getInstance().addCell(cell);
		CIntentionCellFactory.getInstance().cellCreated(cell);
		cell.setIsPinned(isPinned);
		
//		IntentionGraph.getInstance().repaint();
	}

	private static void CIC_MOVE(CalicoPacket p)
	{
		p.rewind();
		IntentionalInterfacesNetworkCommands.Command.CIC_MOVE.verify(p);

		long uuid = p.getLong();
		CIntentionCell cell = CIntentionCellController.getInstance().getCellById(uuid);

		int x = p.getInt();
		int y = p.getInt();
		cell.setLocation(x, y);
//		System.out.println("CIC_MOVE, " + uuid + ", " + x + ", " + y);

		IntentionGraphController.getInstance().cellMoved(cell.getId(), x, y);
	}

	private static void CIC_SET_TITLE(CalicoPacket p)
	{
		p.rewind();
		IntentionalInterfacesNetworkCommands.Command.CIC_SET_TITLE.verify(p);

		long uuid = p.getLong();
		CIntentionCell cell = CIntentionCellController.getInstance().getCellById(uuid);

		cell.setTitle(p.getString());

		if (CanvasPerspective.getInstance().isActive() && (CCanvasController.getCurrentUUID() == cell.getCanvasId()))
		{
			CanvasTitlePanel.getInstance().refresh();
		}
		IntentionalInterfacesCanvasContributor.getInstance().notifyContentChanged(cell.getCanvasId());
	}

	private static void CIC_TAG(CalicoPacket p)
	{
		p.rewind();
		IntentionalInterfacesNetworkCommands.Command.CIC_TAG.verify(p);

		long uuid = p.getLong();
		long typeId = p.getLong();
		CIntentionCell cell = CIntentionCellController.getInstance().getCellById(uuid);

		cell.setIntentionType(typeId);
		CCanvasLinkController.getInstance().canvasIntentionTypeChanged(cell);

		if (CanvasPerspective.getInstance().isActive() && (CCanvasController.getCurrentUUID() == cell.getCanvasId()))
		{
			CanvasTagPanel.getInstance().refresh();
		}
		IntentionalInterfacesCanvasContributor.getInstance().notifyContentChanged(cell.getCanvasId());
	}

	private static void CIC_UNTAG(CalicoPacket p)
	{
		p.rewind();
		IntentionalInterfacesNetworkCommands.Command.CIC_UNTAG.verify(p);

		long uuid = p.getLong();
		long typeId = p.getLong();
		CIntentionCell cell = CIntentionCellController.getInstance().getCellById(uuid);

		cell.clearIntentionType();

		if (CanvasPerspective.getInstance().isActive() && (CCanvasController.getCurrentUUID() == cell.getCanvasId()))
		{
			CanvasTagPanel.getInstance().refresh();
		}
		IntentionalInterfacesCanvasContributor.getInstance().notifyContentChanged(cell.getCanvasId());
	}

	private static void CIC_DELETE(CalicoPacket p)
	{
		p.rewind();
		IntentionalInterfacesNetworkCommands.Command.CIC_DELETE.verify(p);

		long cellId = p.getLong();

		CIntentionCellController.getInstance().localDeleteCell(cellId);
	}

	private static void CIC_TOPOLOGY(CalicoPacket p)
	{
		p.rewind();
//		System.out.println("Called CIC_TOPOLOGY");
//		(new Exception()).printStackTrace();
		IntentionalInterfacesNetworkCommands.Command.CIC_TOPOLOGY.verify(p);

		CIntentionTopology topology = new CIntentionTopology(p.getString());
		IntentionGraph.getInstance().setTopology(topology);

		if (Networking.connectionState == Networking.ConnectionState.Connected)
		{
//			SwingUtilities.invokeLater(
			IntentionalInterfacesClientPlugin.addNewEventDispatcherEvent(
					new Runnable() { public void run() {
						IntentionGraph.getInstance().updateZoom();
//						IntentionGraph.getInstance().fitContents();
					}});
		}
		
		executeEventDispatcherEvents();
		
	}

	private static void CIT_CREATE(CalicoPacket p)
	{
		p.rewind();
		IntentionalInterfacesNetworkCommands.Command.CIT_CREATE.verify(p);

		long uuid = p.getLong();
		String name = p.getString();
		int colorIndex = p.getInt();
		String description = "";
		//the if statement is there to be compatible with previous saves, given that they may not have this extra value.
		//note to self: something like JSON would have prevented this headache... oh well
		if (p.remaining() > 0)
			description = p.getString();
		CIntentionType type = new CIntentionType(uuid, name, colorIndex, description);

		IntentionCanvasController.getInstance().localAddIntentionType(type);
	}

	private static void CIT_RENAME(CalicoPacket p)
	{
		p.rewind();
		IntentionalInterfacesNetworkCommands.Command.CIT_CREATE.verify(p);

		long uuid = p.getLong();
		String name = p.getString();

		IntentionCanvasController.getInstance().localRenameIntentionType(uuid, name);
	}
	
	private static void CIT_SET_DESCRIPTION(CalicoPacket p)
	{
		
		p.rewind();
		IntentionalInterfacesNetworkCommands.Command.CIT_SET_DESCRIPTION.verify(p);

		long uuid = p.getLong();
		String name = p.getString();
		IntentionCanvasController.getInstance().localSetIntentionTypeDescription(uuid, name);
	}

	private static void CIT_SET_COLOR(CalicoPacket p)
	{
		p.rewind();
		IntentionalInterfacesNetworkCommands.Command.CIT_CREATE.verify(p);

		long uuid = p.getLong();
		int colorIndex = p.getInt();

		IntentionCanvasController.getInstance().localSetIntentionTypeColor(uuid, colorIndex);

		if (IntentionalInterfacesPerspective.getInstance().isActive())
		{
			IntentionGraph.getInstance().repaint();
		}
	}

	private static void CIT_DELETE(CalicoPacket p)
	{
		p.rewind();
		IntentionalInterfacesNetworkCommands.Command.CIT_CREATE.verify(p);

		long uuid = p.getLong();

		IntentionCanvasController.getInstance().localRemoveIntentionType(uuid);
		CIntentionCellController.getInstance().removeIntentionTypeReferences(uuid);

		if (IntentionalInterfacesPerspective.getInstance().isActive())
		{
			IntentionGraph.getInstance().repaint();
		}
	}

	private static CCanvasLinkAnchor unpackAnchor(CalicoPacket p)
	{
		long uuid = p.getLong();
		long canvas_uuid = p.getLong();
		CCanvasLinkAnchor.ArrowEndpointType type = CCanvasLinkAnchor.ArrowEndpointType.values()[p.getInt()];
		int x = p.getInt();
		int y = p.getInt();

		CCanvasLinkAnchor anchor;
		switch (type)
		{
			case FLOATING:
				anchor = new CCanvasLinkAnchor(uuid, x, y);
				break;
			case INTENTION_CELL:
				anchor = new CCanvasLinkAnchor(uuid, canvas_uuid, x, y);
				break;
			default:
				throw new IllegalArgumentException("Unknown link type " + type);
		}

		anchor.setGroupId(p.getLong());

		return anchor;
	}

	private static void CLINK_CREATE(CalicoPacket p)
	{
		p.rewind();
		IntentionalInterfacesNetworkCommands.Command.CLINK_CREATE.verify(p);

		long uuid = p.getLong();
		CCanvasLinkAnchor anchorA = unpackAnchor(p);
		CCanvasLinkAnchor anchorB = unpackAnchor(p);
		String label = p.getString();
		CCanvasLink link = new CCanvasLink(uuid, anchorA, anchorB, label);

		CCanvasLinkController.getInstance().addLink(link);
		IntentionGraphController.getInstance().updateLinkArrow(link);
	}

	private static void CLINK_MOVE_ANCHOR(CalicoPacket p)
	{
		p.rewind();
		IntentionalInterfacesNetworkCommands.Command.CLINK_MOVE_ANCHOR.verify(p);

		long anchor_uuid = p.getLong();
		long canvas_uuid = p.getLong();
		CCanvasLinkAnchor.ArrowEndpointType type = CCanvasLinkAnchor.ArrowEndpointType.values()[p.getInt()];
		int x = p.getInt();
		int y = p.getInt();

		CCanvasLinkController.getInstance().localMoveLinkAnchor(anchor_uuid, canvas_uuid, type, x, y);
	}

	private static void CLINK_LABEL(CalicoPacket p)
	{
		p.rewind();
		IntentionalInterfacesNetworkCommands.Command.CLINK_LABEL.verify(p);

		long uuid = p.getLong();
		CCanvasLink link = CCanvasLinkController.getInstance().getLinkById(uuid);
		link.setLabel(p.getString());

		IntentionGraphController.getInstance().getArrowByLinkId(uuid).redraw();
	}

	private static void CLINK_DELETE(CalicoPacket p)
	{
		p.rewind();
		IntentionalInterfacesNetworkCommands.Command.CLINK_DELETE.verify(p);

		long uuid = p.getLong();
		CCanvasLinkController.getInstance().removeLinkById(uuid);
	}
	
	private static void WALL_BOUNDS(CalicoPacket p)
	{
//		calico.plugins.iip.graph.layout.CIntentionLayout.getTopologyBounds();
		IntentionGraph.getInstance().initializeZoom(p);
	}
	
	private static void CIC_SET_PIN(CalicoPacket p)
	{
		p.rewind();
		IntentionalInterfacesNetworkCommands.Command.CIC_SET_PIN.verify(p);
		
		long cic_id = p.getLong();
		boolean pinValue = p.getInt() != 0;
		
		CIntentionCellController.getInstance().getCellById(cic_id).setIsPinned(pinValue);
	}
	
	private static void EXECUTE_II_EVENT_DISPATCHER_EVENTS(CalicoPacket p)
	{
		p.rewind();
		IntentionalInterfacesNetworkCommands.Command.EXECUTE_II_EVENT_DISPATCHER_EVENTS.verify(p);
		
		executeEventDispatcherEvents();
	}
	
	
	
	public Class<?> getNetworkCommandsClass()
	{
		return IntentionalInterfacesNetworkCommands.class;
	}
	
	public static void addNewEventDispatcherEvent(Runnable e)
	{
		eventDispatcherQueue.add(e);
	}
	
	public static void executeEventDispatcherEvents()
	{
		SwingUtilities.invokeLater(
				new Runnable() { public void run() { 
					while (!eventDispatcherQueue.isEmpty())
					{
						eventDispatcherQueue.get(0).run();
						eventDispatcherQueue.remove(0);
					}
				}});
	}
}
