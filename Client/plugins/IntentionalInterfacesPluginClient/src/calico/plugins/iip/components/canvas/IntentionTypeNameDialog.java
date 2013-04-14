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
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import calico.CalicoDataStore;
import calico.plugins.iip.components.CIntentionCell;
import calico.plugins.iip.components.CIntentionType;

/**
 * Simple dialog containing a text field in which the user enters a new tag name. This feature is obsolete.
 * 
 * @author Byron Hawkins
 */
public class IntentionTypeNameDialog
{
	private static final IntentionTypeNameDialog INSTANCE = new IntentionTypeNameDialog();

	public static IntentionTypeNameDialog getInstance()
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

	private IntentionTypeNameDialog()
	{
		dialog = new JDialog();
		panel = new Panel();

		dialog.setTitle("Enter the name of the intention type");
		dialog.setModal(true);
		dialog.getContentPane().add(panel.dialogPanel);
		dialog.pack();
	}

	public Action queryUserForName(CIntentionType type)
	{
		Rectangle windowBounds = CalicoDataStore.calicoObj.getBounds();
		Rectangle dialogBounds = dialog.getBounds();
		int x = windowBounds.x + ((windowBounds.width - dialogBounds.width) / 2);
		int y = windowBounds.y + ((windowBounds.height - dialogBounds.height) / 2);
		dialog.setLocation(x, y);

		action = Action.CANCEL;

		if (type == null)
		{
			panel.entry.setText("");
		}
		else
		{
			panel.entry.setText(type.getName());
		}
		panel.entry.grabFocus();
		panel.entry.selectAll();
		dialog.setVisible(true);

		return action;
	}

	public Action getAction()
	{
		return action;
	}

	public String getText()
	{
		return panel.entry.getText();
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

		private final JPanel entryPanel;
		private final JTextField entry;

		private final JPanel buttonPanel;
		private final JButton ok;
		private final JButton cancel;

		Panel()
		{
			dialogPanel = new JPanel(new BorderLayout());
			dialogPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

			entryPanel = new JPanel(new BorderLayout(4, 0));
			entryPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));
			entry = new JTextField(20);

			buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
			ok = new JButton("OK");
			cancel = new JButton("Cancel");

			entryPanel.add(entry, BorderLayout.CENTER);

			buttonPanel.add(ok);
			buttonPanel.add(cancel);

			dialogPanel.add(entryPanel, BorderLayout.CENTER);
			dialogPanel.add(buttonPanel, BorderLayout.SOUTH);

			ok.addActionListener(this);
			cancel.addActionListener(this);

			entry.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter");
			entry.getActionMap().put("enter", new EnterAction());
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
}
