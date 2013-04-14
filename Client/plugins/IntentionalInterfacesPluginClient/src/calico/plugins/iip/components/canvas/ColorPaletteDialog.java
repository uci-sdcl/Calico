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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import calico.CalicoDataStore;
import calico.CalicoDraw;
import calico.plugins.iip.components.CIntentionCell;
import calico.plugins.iip.components.CIntentionType;

/**
 * Simple popup offering a selection of tag colors to the user. This feature is obsolete. 
 *
 * @author Byron Hawkins
 */
public class ColorPaletteDialog
{
	private static final ColorPaletteDialog INSTANCE = new ColorPaletteDialog();

	public static ColorPaletteDialog getInstance()
	{
		return INSTANCE;
	}

	public enum Action
	{
		OK,
		CANCEL;
	}

	private final JDialog dialog;
	private final Panel panel;

	private Action action;

	private ColorPaletteDialog()
	{
		dialog = new JDialog();
		panel = new Panel();

		dialog.setTitle("Enter the canvas title");
		dialog.setModal(true);
		dialog.getContentPane().add(panel.dialogPanel);
		dialog.pack();
	}

	public Action queryUserForColor(CIntentionType type)
	{
		Rectangle windowBounds = CalicoDataStore.calicoObj.getBounds();
		Rectangle dialogBounds = dialog.getBounds();
		int x = windowBounds.x + ((windowBounds.width - dialogBounds.width) / 2);
		int y = windowBounds.y + ((windowBounds.height - dialogBounds.height) / 2);
		dialog.setLocation(x, y);

		action = Action.CANCEL;

		panel.palette.setSelectedSwatchIndex(type.getColorIndex());
		dialog.setVisible(true);

		return action;
	}

	public Action getAction()
	{
		return action;
	}

	public int getColorIndex()
	{
		return panel.palette.getSelectedSwatchIndex();
	}

	private void closeDialog(Action action)
	{
		this.action = action;
		dialog.setVisible(false);
	}

	private class EnterAction extends AbstractAction
	{
		@Override
		public void actionPerformed(ActionEvent event)
		{
			closeDialog(Action.OK);
		}
	}

	private class Panel implements ActionListener
	{
		private final JPanel dialogPanel;

		private final JPanel palettePanel;
		private final Palette palette;

		private final JPanel buttonPanel;
		private final JButton ok;
		private final JButton cancel;

		Panel()
		{
			dialogPanel = new JPanel(new BorderLayout());
			dialogPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

			palettePanel = new JPanel(new BorderLayout());
			palette = new Palette(3, 3, CIntentionType.AVAILABLE_COLORS);
			palettePanel.add(palette, BorderLayout.CENTER);

			buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
			ok = new JButton("OK");
			cancel = new JButton("Cancel");

			buttonPanel.add(ok);
			buttonPanel.add(cancel);

			dialogPanel.add(palettePanel, BorderLayout.CENTER);
			dialogPanel.add(buttonPanel, BorderLayout.SOUTH);

			ok.addActionListener(this);
			cancel.addActionListener(this);
		}

		@Override
		public void actionPerformed(ActionEvent event)
		{
			if (event.getSource() == ok)
			{
				closeDialog(Action.OK);
			}
			else if (event.getSource() == cancel)
			{
				closeDialog(Action.CANCEL);
			}
		}
	}

	private class Palette extends JComponent implements MouseListener
	{
		private final int SWATCH_SPAN = 24;

		private final Color[][] colors;

		private final int rows;
		private final int columns;

		private int selectedSwatchIndex = 0;

		public Palette(int rows, int columns, Color[] colorList)
		{
			this.rows = rows;
			this.columns = columns;

			colors = new Color[rows][];
			for (int i = 0; i < rows; i++)
			{
				colors[i] = new Color[columns];
				for (int j = 0; j < columns; j++)
				{
					colors[i][j] = colorList[(i * columns) + j];
				}
			}

			Dimension size = new Dimension(columns * SWATCH_SPAN, rows * SWATCH_SPAN);
			setPreferredSize(size);
			setMaximumSize(size);
			setMinimumSize(size);
			setSize(size);
			
			addMouseListener(this);
		}

		void setSelectedSwatchIndex(int index)
		{
			this.selectedSwatchIndex = index;

			SwingUtilities.invokeLater(
					new Runnable() { public void run() { 
						repaint();
					}});
		}
		
		int getSelectedSwatchIndex()
		{
			return selectedSwatchIndex;
		}

		@Override
		protected void paintComponent(Graphics _g)
		{
			super.paintComponent(_g);

			Graphics2D g = (Graphics2D) _g;
			Color c = g.getColor();

			for (int i = 0; i < rows; i++)
			{
				int y = i * SWATCH_SPAN;
				for (int j = 0; j < columns; j++)
				{
					int x = j * SWATCH_SPAN;
					g.setColor(colors[i][j]);
					g.fillRect(x, y, SWATCH_SPAN - 1, SWATCH_SPAN - 1);
				}
			}

			g.setColor(Color.black);
			int x = (selectedSwatchIndex % columns) * SWATCH_SPAN;
			int y = (selectedSwatchIndex / columns) * SWATCH_SPAN;
			g.drawRect(x, y, SWATCH_SPAN - 1, SWATCH_SPAN - 1);

			g.setColor(c);
		}

		@Override
		public void mouseClicked(MouseEvent e)
		{
		}

		@Override
		public void mouseEntered(MouseEvent e)
		{
		}

		@Override
		public void mouseExited(MouseEvent e)
		{
		}

		@Override
		public void mousePressed(MouseEvent e)
		{
		}

		@Override
		public void mouseReleased(MouseEvent e)
		{
			int row = e.getY() / SWATCH_SPAN;
			int column = e.getX() / SWATCH_SPAN;

			System.out.println("Select swatch at " + row + ", " + column);

			setSelectedSwatchIndex((row * columns) + column);
		}
	}
}
