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
package calico.plugins.iip.components.canvas;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import calico.Calico;
import calico.CalicoDataStore;
import calico.CalicoDraw;
import calico.controllers.CCanvasController;
import calico.events.CalicoEventHandler;
import calico.events.CalicoEventListener;
import calico.inputhandlers.CalicoAbstractInputHandler;
import calico.inputhandlers.CalicoInputManager;
import calico.inputhandlers.InputEventInfo;
import calico.inputhandlers.StickyItem;
import calico.networking.netstuff.CalicoPacket;
import calico.plugins.iip.IntentionalInterfacesNetworkCommands;
import calico.plugins.iip.components.CIntentionCell;
import calico.plugins.iip.components.CIntentionType;
import calico.plugins.iip.components.IntentionPanelLayout;
import calico.plugins.iip.controllers.CCanvasLinkController;
import calico.plugins.iip.controllers.CIntentionCellController;
import calico.plugins.iip.controllers.IntentionCanvasController;
import calico.plugins.iip.iconsets.CalicoIconManager;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.nodes.PImage;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.nodes.PText;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PPaintContext;
import edu.umd.cs.piccolox.nodes.PComposite;

/**
 * Panel in the Canvas View containing all the available tags in a list. If the current canvas has been tagged, that
 * tag's row in the panel is highlighted with the tag color as a solid background. When the user clicks on a tag, the
 * panel disappears and the tag is assigned to the canvas via <code>CIntentionCellController</code>.
 * 
 * There is only one instance of the tag panel, and it is moved from canvas to canvas as the user traverses their
 * design.
 * 
 * When tags are added or removed, this tag panel expects to be notified with a call to
 * <code>updateIntentionTypes()</code> so that it can make the visual changes.
 * 
 * Earlier versions of this panel allowed the user to edit the tags in various ways.
 * 
 * @author Byron Hawkins
 */
public class CanvasTagPanel implements StickyItem, PropertyChangeListener, CalicoEventListener
{
	public static CanvasTagPanel getInstance()
	{
		return INSTANCE;
	}

	private static CanvasTagPanel INSTANCE = new CanvasTagPanel();

	public static final double PANEL_COMPONENT_INSET = 5.0;

	public static final double ROW_HEIGHT = 30.0;
	public static final double ROW_TEXT_INSET = 1.0;

	private final PanelNode panel;

	private final Image addButtonImage;
	private final Image removeButtonImage;
	private final Image editButtonImage;
	private final Image paletteButtonImage;
	private final Image closeButtonImage;
	private final Image editTagsButtonImage;
	private final Image doneButtonImage;
	
	private final long uuid;
	private long canvas_uuid;

	private boolean visible;
	private IntentionPanelLayout layout;

	private boolean initialized = false;

	private CanvasTagPanel()
	{
		uuid = Calico.uuid();
		this.canvas_uuid = 0L;

		CalicoInputManager.addCustomInputHandler(uuid, new InputHandler());
		
		addButtonImage = CalicoIconManager.getIconImage("intention.add-button");
		removeButtonImage = CalicoIconManager.getIconImage("intention.remove-button");
		editButtonImage = CalicoIconManager.getIconImage("intention.edit-button");
		paletteButtonImage = CalicoIconManager.getIconImage("intention.palette-button");
		closeButtonImage = CalicoIconManager.getIconImage("intention.close");
		editTagsButtonImage = CalicoIconManager.getIconImage("intention.edit-tags");
		doneButtonImage = CalicoIconManager.getIconImage("intention.done");
		
		IntentionTypeRowEditMode.RENAME.image = editButtonImage;
		IntentionTypeRowEditMode.SET_COLOR.image = paletteButtonImage;
		IntentionTypeRowEditMode.REMOVE.image = removeButtonImage;

		panel = new PanelNode();

		panel.setPaint(Color.white);
		panel.setVisible(visible = false);
		
		CalicoEventHandler.getInstance().addListener(IntentionalInterfacesNetworkCommands.CLINK_CREATE, this, CalicoEventHandler.PASSIVE_LISTENER);
		CalicoEventHandler.getInstance().addListener(IntentionalInterfacesNetworkCommands.CLINK_MOVE_ANCHOR, this, CalicoEventHandler.PASSIVE_LISTENER);

		initialized = true;
	}

	@Override
	public long getUUID()
	{
		return uuid;
	}

	@Override
	public boolean containsPoint(Point p)
	{
		return panel.getBounds().contains(p);
	}

	@Override
	public void propertyChange(PropertyChangeEvent event)
	{
		if (visible && event.getPropertyName().equals(PNode.PROPERTY_CHILDREN))
		{
			updatePanelBounds();
			CalicoDraw.repaint(panel);
//			panel.repaint();
		}
	}

	public boolean isVisible()
	{
		return panel.getVisible();
	}

	public void setVisible(boolean b)
	{
		// if the originating canvas is in the inner circle, don't display this

		
		if (visible == b)
		{
			return;
		}

		visible = b;

		if (b)
		{
			refresh();
		}
		else
		{
			panel.setVisible(false);
		}

		if (b)
		{
			CalicoInputManager.registerStickyItem(this);
			CalicoDraw.repaint(panel);
//			panel.repaint();
		}
		else
		{
			CalicoInputManager.unregisterStickyItem(this);
		}
	}

	public void moveTo(long canvas_uuid)
	{
		this.canvas_uuid = canvas_uuid;

		if (panel.getParent() != null)
		{
			panel.getParent().removePropertyChangeListener(this);
			panel.getParent().removeChild(panel);
		}
		refresh();
		CalicoDraw.addChildToNode(CCanvasController.canvasdb.get(canvas_uuid).getCamera(), panel);
		CCanvasController.canvasdb.get(canvas_uuid).getCamera().addChild(panel);
		CCanvasController.canvasdb.get(canvas_uuid).getCamera().addPropertyChangeListener(this);
	}

	public void refresh()
	{
		if (!visible)
		{
			return;
		}

		if (!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(new Runnable() {
				public void run()
				{
					refresh();
				}
			});
			return;
		}

		panel.refresh();
		updatePanelBounds();
		CalicoDraw.setVisible(panel, true);
//		panel.setVisible(true);
		CalicoDraw.repaint(panel);
//		panel.repaint();
	}
	
	private class TitleRow extends PComposite
	{
		private final PText text = new PText();

		public TitleRow()
		{
			text.setConstrainWidthToTextWidth(true);
			text.setConstrainHeightToTextHeight(true);
			text.setFont(text.getFont().deriveFont(20f));

			addChild(text);
		}

		void tap(Point point)
		{
			CanvasTitleDialog.Action action = CanvasTitleDialog.getInstance().queryUserForLabel(
					CIntentionCellController.getInstance().getCellByCanvasId(canvas_uuid));

			if (action == CanvasTitleDialog.Action.OK)
			{
				CIntentionCellController.getInstance().setCellTitle(CIntentionCellController.getInstance().getCellByCanvasId(canvas_uuid).getId(),
						CanvasTitleDialog.getInstance().getText(), false);
			}
		}

		double getMaxWidth()
		{
			return text.getBounds().width + (2 * PANEL_COMPONENT_INSET);
		}

		void refresh()
		{
			text.setText(CIntentionCellController.getInstance().getCellByCanvasId(canvas_uuid).getTitle());
		}

		@Override
		protected void layoutChildren()
		{
			PBounds bounds = getBounds();

			text.recomputeLayout();
			PBounds textBounds = text.getBounds();
			text.setBounds(bounds.x + PANEL_COMPONENT_INSET, bounds.y + ROW_TEXT_INSET, textBounds.width, textBounds.getHeight());
		}
	}

	public void updateIntentionTypes()
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(new Runnable() {
				public void run()
				{
					updateIntentionTypes();
				}
			});
			return;
		}

		panel.updateIntentionTypes();
		panel.refresh();
		updatePanelBounds();
	}

	private void updatePanelBounds()
	{
		double width = panel.calculateWidth();
		double height = panel.calculateHeight();
		layout.updateBounds(panel, width, height);

		if (visible)
		{
			CalicoDraw.repaint(panel);
//			panel.repaint();
		}
	}

	public void setLayout(IntentionPanelLayout layout)
	{
		this.layout = layout;
	}

	private enum IntentionTypeRowEditMode
	{
		NONE,
		RENAME,
		SET_COLOR,
		REMOVE;

		Image image;
	}
	
	private enum IntentionTypeRowMetaRowMode
	{
		DEFAULT,
		EDIT;
	}

	/**
	 * Represents one tag row panel's Piccolo component hierarchy. Paints the selection highlight in
	 * <code>paint()</code>.
	 * 
	 * @author Byron Hawkins
	 */
	private class IntentionTypeRow extends PComposite
	{
		private final CIntentionType type;
		private final PText label;
		private final PText labelTag;
		private final PImage editButton = new PImage(removeButtonImage);
		private final PImage arrowIcon =
				new PImage(CalicoIconManager.getIconImage(
						"intention.bullet-point"));
		private final double arrowIconWidth = ROW_HEIGHT - (2 * ROW_TEXT_INSET);
		private final Color tagColor;
		private IntentionTypeRowEditMode editMode = IntentionTypeRowEditMode.NONE;

		private boolean selected = false;

		public IntentionTypeRow(CIntentionType type)
		{
			this.type = type;
			label = new PText(type.getDescription() + " ");
			label.setConstrainWidthToTextWidth(true);
			label.setConstrainHeightToTextHeight(true);
			label.setFont(label.getFont().deriveFont(20f));
			
			labelTag = new PText(" ["+type.getName()+"] ");
			labelTag.setConstrainWidthToTextWidth(true);
			labelTag.setConstrainHeightToTextHeight(true);
			labelTag.setFont(label.getFont().deriveFont(20f));
			
			tagColor = type.getColor();

			CalicoDraw.addChildToNode(this, label);
			CalicoDraw.addChildToNode(this, labelTag);
			CalicoDraw.addChildToNode(this, arrowIcon);
			addChild(editButton);
			editButton.setVisible(false);
			
//			addChild(label);
		}

		void tap(Point point)
		{
			if (editButton.getVisible() && editButton.getBounds().contains(point))
			{
				switch (editMode)
				{
				case RENAME:
				{
					JTextField nameText = new JTextField(type.getName());
					JTextField descriptionText = new JTextField(type.getDescription());
					final JComponent[] inputs = new JComponent[] {
							new JLabel("Name: "),
							nameText,
							new JLabel("Description: "),
							descriptionText
					};
					
					int action = JOptionPane.showConfirmDialog(null, inputs, "Please enter name and description", JOptionPane.OK_CANCEL_OPTION);
					if (action == JOptionPane.OK_OPTION)
					{
						if (nameText.getText().compareTo(type.getName()) != 0)
							IntentionCanvasController.getInstance().renameIntentionType(type.getId(), nameText.getText());
						if (descriptionText.getText().compareTo(type.getDescription()) != 0)
							IntentionCanvasController.getInstance().setIntentionTypeDescription(type.getId(), descriptionText.getText());
					}
					
//					IntentionTypeNameDialog.Action action = IntentionTypeNameDialog.getInstance().queryUserForName(type);
//					if (action == IntentionTypeNameDialog.Action.OK)
//					{
//
//
////						IntentionCanvasController.getInstance().renameIntentionType(type.getId(), IntentionTypeNameDialog.getInstance().getText());
//						
//					}
				}
				break;
				case SET_COLOR:
				{
					ColorPaletteDialog.Action action = ColorPaletteDialog.getInstance().queryUserForColor(type);
					if (action == ColorPaletteDialog.Action.OK)
					{
						IntentionCanvasController.getInstance().setIntentionTypeColorIndex(type.getId(), ColorPaletteDialog.getInstance().getColorIndex());
					}
				}
				break;
				case REMOVE:
					int count = CIntentionCellController.getInstance().countIntentionTypeUsage(type.getId());
					if (count > 0)
					{
						int userOption = JOptionPane.showConfirmDialog(CalicoDataStore.calicoObj, "<html>The intention tag '" + type.getName()
								+ "' is currently assigned to " + count + " whiteboards.<br>Are you sure you want to delete it?</html>",
										"Warning - intention tag in use", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

						if (userOption != JOptionPane.YES_OPTION)
						{
							break;
						}
					}

					IntentionCanvasController.getInstance().removeIntentionType(type.getId());
					break;
				}

				panel.activateIntentionRowEditMode(IntentionTypeRowEditMode.NONE);
			}
			else
			{
//				CIntentionCellController.getInstance().toggleCellIntentionType(CIntentionCellController.getInstance().getCellByCanvasId(canvas_uuid).getId(),
//						type.getId(), !selected, false);
				IntentionCanvasController.getInstance().showTagPanel(false);
				CIntentionCellController.getInstance().toggleCellIntentionType(CIntentionCellController.getInstance().getCellByCanvasId(canvas_uuid).getId(),
						type.getId(), !selected, false);
				if (type.getName().compareTo("no tag") != 0)
					IntentionCanvasController.getInstance().linkCanvasToOriginatingContext();
//				IntentionCanvasController.getInstance().collapseLikeIntentionTypes();
				
//				CanvasTitlePanel.getInstance().refresh();
			}
		}

		void activateEditMode(IntentionTypeRowEditMode mode)
		{
			if (editMode == mode)
			{
				mode = IntentionTypeRowEditMode.NONE;
			}

			this.editMode = mode;

			if (mode == IntentionTypeRowEditMode.NONE)
			{
				editButton.setVisible(false);
			}
			else
			{
				editButton.setImage(mode.image);
				editButton.setVisible(true);
			}

			

		}

		double getMaxWidth()
		{
			return arrowIconWidth + label.getBounds().width + labelTag.getBounds().width + (PANEL_COMPONENT_INSET * 3) + editButton.getBounds().width;
		}

		void setSelected(boolean b)
		{
			selected = b;

			CalicoDraw.repaint(this);
//			repaint();
		}

		@Override
		protected void layoutChildren()
		{
			PBounds rowBounds = getBounds();
			PBounds labelBounds = label.getBounds();
			PBounds buttonBounds = editButton.getBounds();

			label.setBounds(rowBounds.x + arrowIconWidth + PANEL_COMPONENT_INSET, rowBounds.y + ROW_TEXT_INSET, label.getWidth(), ROW_HEIGHT - (2 * ROW_TEXT_INSET));
			labelTag.setBounds(rowBounds.x + arrowIconWidth + label.getWidth() + PANEL_COMPONENT_INSET, rowBounds.y + ROW_TEXT_INSET, labelTag.getWidth(), ROW_HEIGHT - (2 * ROW_TEXT_INSET));
			arrowIcon.setBounds(rowBounds.x, rowBounds.y + ROW_TEXT_INSET, arrowIconWidth, arrowIconWidth);
			editButton.setBounds((rowBounds.x + rowBounds.width) - (buttonBounds.width + PANEL_COMPONENT_INSET), rowBounds.y
					+ ((rowBounds.height - buttonBounds.height) / 2.0), buttonBounds.width, buttonBounds.height);
		}

		@Override
		protected void paint(PPaintContext paintContext)
		{
			Graphics2D g = paintContext.getGraphics();
			Color c = g.getColor();
			g.setColor(tagColor);
			Rectangle labelTagBounds = new Rectangle((int)labelTag.getX(), (int)labelTag.getY(), (int)labelTag.getWidth(), (int)labelTag.getHeight());
			g.fillRect(labelTagBounds.x, labelTagBounds.y, labelTagBounds.width, labelTagBounds.height);
			g.setColor(c);
			
			if (selected)
			{
				PBounds bounds = getBounds();
				c = g.getColor();
				g.setColor(type.getColor());
				g.fillRect((int) bounds.x, (int) bounds.y, (int) bounds.width, (int) bounds.height);
				g.setColor(c);
			}

			super.paint(paintContext);
		}
	}
	
	private class MetaButton extends PComposite
	{
		private final PImage icon;

		public MetaButton(Image image)
		{
			icon = new PImage(image);
			addChild(icon);
		}

		@Override
		protected void layoutChildren()
		{
			PBounds bounds = getBounds();
			icon.centerBoundsOnPoint(bounds.x + (bounds.width / 2.0), bounds.y + (bounds.height / 2.0));
		}
	}

	private class MetaRow extends PComposite
	{
		private final MetaButton closeButton = new MetaButton(closeButtonImage);
		private final MetaButton editTagsButton = new MetaButton(editTagsButtonImage);
		
		private final MetaButton addButton = new MetaButton(addButtonImage);
		private final MetaButton removeButton = new MetaButton(removeButtonImage);
		private final MetaButton editButton = new MetaButton(editButtonImage);
		private final MetaButton colorButton = new MetaButton(paletteButtonImage);
		private final MetaButton doneButton = new MetaButton(doneButtonImage);
		
		private IntentionTypeRowMetaRowMode editMode = IntentionTypeRowMetaRowMode.DEFAULT;

		public MetaRow()
		{
			addChild(closeButton);
			addChild(editTagsButton);
			
//			addChild(addButton);
//			addChild(removeButton);
//			addChild(editButton);
//			addChild(colorButton);
		}

		void tap(Point point)
		{
			if (editMode == IntentionTypeRowMetaRowMode.DEFAULT)
			{
				if (closeButton.getBoundsReference().contains(point))
				{
					IntentionCanvasController.getInstance().toggleTagPanelVisibility();
				}
				else if (editTagsButton.getBoundsReference().contains(point))
				{
					panel.activateIntentionRowTagEditMode(IntentionTypeRowMetaRowMode.EDIT);
				}
			}
			else if (editMode == IntentionTypeRowMetaRowMode.EDIT)
			{
				if (point.x < removeButton.getBoundsReference().x)
				{
					JTextField nameText = new JTextField();
					JTextField descriptionText = new JTextField();
					final JComponent[] inputs = new JComponent[] {
							new JLabel("Name: "),
							nameText,
							new JLabel("Description: "),
							descriptionText
					};
					
					int action = JOptionPane.showConfirmDialog(null, inputs, "Please enter name and description", JOptionPane.OK_CANCEL_OPTION);
					if (action == JOptionPane.OK_OPTION)
					{
						IntentionCanvasController.getInstance().addIntentionType(nameText.getText(), descriptionText.getText());
					}
				}
				else if (point.x < editButton.getBoundsReference().x)
				{
					panel.activateIntentionRowEditMode(IntentionTypeRowEditMode.REMOVE);
				}
				else if (point.x < colorButton.getBoundsReference().x)
				{
					panel.activateIntentionRowEditMode(IntentionTypeRowEditMode.RENAME);
				}
				else if (point.x < (colorButton.getBoundsReference().x + colorButton.getBoundsReference().width))
				{
					panel.activateIntentionRowEditMode(IntentionTypeRowEditMode.SET_COLOR);
				}
				else if (doneButton.getBoundsReference().contains(point))
				{
					panel.activateIntentionRowTagEditMode(IntentionTypeRowMetaRowMode.DEFAULT);
				}
			}

		}

		@Override
		protected void layoutChildren()
		{
			PBounds rowBounds = getBounds();
			double x = rowBounds.x;
			
			if (editMode == IntentionTypeRowMetaRowMode.DEFAULT)
			{
				double buttonWidth = (rowBounds.getBounds().width / 2.0);
				
				closeButton.setBounds(x, rowBounds.y, buttonWidth, ROW_HEIGHT);
				editTagsButton.setBounds(x += buttonWidth, rowBounds.y, buttonWidth, ROW_HEIGHT);
			}
			else if (editMode == IntentionTypeRowMetaRowMode.EDIT)
			{
				double buttonWidth = (rowBounds.getBounds().width / 5.0);

				addButton.setBounds(x, rowBounds.y, buttonWidth, ROW_HEIGHT);
				removeButton.setBounds(x += buttonWidth, rowBounds.y, buttonWidth, ROW_HEIGHT);
				editButton.setBounds(x += buttonWidth, rowBounds.y, buttonWidth, ROW_HEIGHT);
				colorButton.setBounds(x += buttonWidth, rowBounds.y, buttonWidth, ROW_HEIGHT);
				doneButton.setBounds(x += buttonWidth, rowBounds.y, buttonWidth, ROW_HEIGHT);
			}
		}

		public void activateEditMode(IntentionTypeRowMetaRowMode mode) {

			if (this.editMode == mode)
				return;
			
			this.editMode = mode;
			
			CalicoDraw.removeAllChildrenFromNode(this);

			if (mode == IntentionTypeRowMetaRowMode.DEFAULT)
			{
				SwingUtilities.invokeLater(
						new Runnable() { public void run() { 
							addChild(closeButton);
							addChild(editTagsButton);
							layoutChildren();
						}});
			}
			else if (mode == IntentionTypeRowMetaRowMode.EDIT)
			{
				SwingUtilities.invokeLater(
						new Runnable() { public void run() { 
							addChild(addButton);
							addChild(removeButton);
							addChild(editButton);
							addChild(colorButton);
							addChild(doneButton);
							layoutChildren();
						}});
			}
			
		}
	}

	/**
	 * Represents the panel in the Piccolo component hierarchy. Automatically sizes to fit on
	 * <code>updateIntentionTypes(). Paints its own outline with rounded corners in <code>paint()</code>.
	 * 
	 * @author Byron Hawkins
	 */
	private class PanelNode extends PComposite
	{
		private final TitleRow titleRow = new TitleRow();
		private final List<IntentionTypeRow> typeRows = new ArrayList<IntentionTypeRow>();
		private final PText titleCaptionP1 = new PText("In relation ");
		private final PText titleCaptionP2 = new PText("to " + "" + ", this canvas is: ");
//		private final PImage arrowIcon =
//				new PImage(CalicoIconManager.getIconImage(
//						"intention.link-canvas"));
		private final double arrowIconWidth = 0; //ROW_HEIGHT - (2 * ROW_TEXT_INSET);
		
		private final MetaRow metaRow = new MetaRow();

		private PPath border;

		public PanelNode()
		{
//			addChild(titleRow);
			addChild(metaRow);
			titleCaptionP1.setFont(titleCaptionP2.getFont().deriveFont(20f));
			titleCaptionP2.setFont(titleCaptionP2.getFont().deriveFont(20f));
		}

		void tap(Point point)
		{
//			if (titleRow.getBoundsReference().contains(point))
//			{
//				titleRow.tap(point);
//			}
//			else 
				if (metaRow.getBoundsReference().contains(point))
			{
				metaRow.tap(point);
			}
			else
				for (IntentionTypeRow row : typeRows)
				{
					if (row.getBoundsReference().contains(point))
					{
						row.tap(point);
						break;
					}
				}
		}
		
		void activateIntentionRowEditMode(IntentionTypeRowEditMode mode)
		{
			for (IntentionTypeRow row : typeRows)
			{
				row.activateEditMode(mode);
			}
		}
		
		void activateIntentionRowTagEditMode(IntentionTypeRowMetaRowMode mode)
		{
			metaRow.activateEditMode(mode);
		}
		
		

		double calculateWidth()
		{
			double width = titleCaptionP1.getWidth() + arrowIconWidth + titleCaptionP2.getWidth();
			for (IntentionTypeRow row : typeRows)
			{
				double rowWidth = row.getMaxWidth() + (PANEL_COMPONENT_INSET * 3);
				if (rowWidth > width)
				{
					width = rowWidth;
				}
			}
			return width;
		}

		double calculateHeight()
		{
			return typeRows.size() * ROW_HEIGHT + ROW_HEIGHT * 3;
		}

		void updateIntentionTypes()
		{
			for (IntentionTypeRow row : typeRows)
			{
				CalicoDraw.removeNodeFromParent(row);
//				removeChild(row);
			}
			typeRows.clear();
			
			CalicoDraw.addChildToNode(this, titleCaptionP1);
//			CalicoDraw.addChildToNode(this, arrowIcon);
			CalicoDraw.addChildToNode(this, titleCaptionP2);

			for (CIntentionType type : IntentionCanvasController.getInstance().getActiveIntentionTypes())
			{
				IntentionTypeRow row = new IntentionTypeRow(type);
				CalicoDraw.addChildToNode(this, row);
//				addChild(row);
				typeRows.add(row);
			}

			CalicoDraw.repaint(this);
//			repaint();
		}

		void refresh()
		{
			if (canvas_uuid == 0L)
			{
				return;
			}
			titleRow.refresh();
			
			/**
			 * In this rather obtuse method call, we use an inline if-statement---
			 * 		if there is an originating canvas, use the name of that canvas
			 * 		if there isn't an originating canvas (in which case this won't even be seen...), assign an empty string
			 */
			if (CIntentionCellController.getInstance().getCIntentionCellParent(canvas_uuid) != 0l
					&& !CIntentionCellController.getInstance().isRootCanvas(CIntentionCellController.getInstance().getCIntentionCellParent(canvas_uuid)))
			{
				titleCaptionP2.setText("to " + CIntentionCellController.getInstance().getCellByCanvasId(
						CIntentionCellController.getInstance().getCIntentionCellParent(canvas_uuid)).getTitle() + ", this canvas is: ");
			}
			else if (IntentionCanvasController.getInstance().getCurrentOriginatingCanvasId() != 0l)
			{
				titleCaptionP2.setText("to " + CIntentionCellController.getInstance().getCellByCanvasId(
								IntentionCanvasController.getInstance().getCurrentOriginatingCanvasId()).getTitle() + ", this canvas is: ");
			}
			else
			{
				titleCaptionP2.setText("to " + "" + ", this canvas is: ");
			}
			
			titleCaptionP2.recomputeLayout();
			layoutChildren();

			CIntentionCell cell = CIntentionCellController.getInstance().getCellByCanvasId(canvas_uuid);
			if (cell != null)
				for (IntentionTypeRow row : typeRows)
				{
					row.setSelected(cell.getIntentionTypeId() == row.type.getId());
				}
		}

		@Override
		protected void layoutChildren()
		{
			if (!initialized)
			{
				return;
			}
			

			

			PBounds bounds = panel.getBounds();
			double y = bounds.y;
			titleRow.setBounds(bounds.x, y, bounds.width, ROW_HEIGHT);
			titleCaptionP1.setBounds(bounds.x, y, titleCaptionP1.getWidth(), ROW_HEIGHT);
//			arrowIcon.setBounds(bounds.x + titleCaptionP1.getWidth(), y, arrowIconWidth, arrowIconWidth);
			titleCaptionP2.setBounds(bounds.x + titleCaptionP1.getWidth() + arrowIconWidth, y, titleCaptionP2.getWidth(), ROW_HEIGHT);
			y += ROW_HEIGHT;			
			for (IntentionTypeRow row : typeRows)
			{
				row.setBounds(bounds.x, y, bounds.width, ROW_HEIGHT);
				y += ROW_HEIGHT;
			}
			metaRow.setBounds(bounds.x, y += ROW_HEIGHT, bounds.width, ROW_HEIGHT);
		}

		@Override
		protected void paint(PPaintContext paintContext)
		{
			super.paint(paintContext);

			Graphics2D g = paintContext.getGraphics();
//			Color c = g.getColor();

			PBounds bounds = getBounds();
//			g.setColor(Color.black);
			g.translate(bounds.x, bounds.y);
//			g.drawRoundRect(0, 0, ((int) bounds.width) - 1, ((int) bounds.height) - 1, 14, 14);

			g.translate(-bounds.x, -bounds.y);
//			g.setColor(c);
		}
	}

	/**
	 * Input only processes tap events, so only the pressed state is tracked.
	 * 
	 * @author Byron Hawkins
	 */
	private enum InputState
	{
		IDLE,
		PRESSED
	}

	/**
	 * Recognizes a tap as a press which is held for less than the <code>tapDuration</code> and does not include a drag
	 * beyond the <code>dragThreshold</code>. The <code>state</code> is voluntarily locked for reading and writing under
	 * <code>stateLock</code>.
	 * 
	 * @author Byron Hawkins
	 */
	private class InputHandler extends CalicoAbstractInputHandler
	{
		private final Object stateLock = new Object();

		private final long tapDuration = 500L;
		private final double dragThreshold = 10.0;

		private InputState state = InputState.IDLE;
		private long pressTime = 0L;
		private Point pressAnchor;

		@Override
		public void actionReleased(InputEventInfo event)
		{
			synchronized (stateLock)
			{
				if ((state == InputState.PRESSED) && ((System.currentTimeMillis() - pressTime) < tapDuration))
				{
					panel.tap(event.getGlobalPoint());
				}
				state = InputState.IDLE;
			}

			pressTime = 0L;

			CalicoInputManager.unlockHandlerIfMatch(uuid);
		}

		@Override
		public void actionDragged(InputEventInfo event)
		{
			if (pressAnchor.distance(event.getGlobalPoint()) < dragThreshold)
			{
				// not a drag, completely ignore this event
				return;
			}

			synchronized (stateLock)
			{
				if (state == InputState.PRESSED)
				{
					state = InputState.IDLE;
					pressTime = 0L;
				}
			}
		}

		@Override
		public void actionPressed(InputEventInfo event)
		{
			synchronized (stateLock)
			{
				state = InputState.PRESSED;

				pressTime = System.currentTimeMillis();
				pressAnchor = event.getGlobalPoint();
			}
		}
	}

	@Override
	public void handleCalicoEvent(int event, CalicoPacket p) {
		
		if (event == IntentionalInterfacesNetworkCommands.CLINK_CREATE
				|| event == IntentionalInterfacesNetworkCommands.CLINK_MOVE_ANCHOR)
		{
			refresh();
		}
		
	}
	
}
