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
package calico.components.menus.buttons;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.util.*;

import calico.*;
import calico.components.*;
import calico.components.grid.*;
import calico.components.menus.CanvasMenuButton;
import calico.controllers.CCanvasController;
import calico.iconsets.CalicoIconManager;
import calico.inputhandlers.InputEventInfo;
import calico.modules.*;
import calico.networking.*;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;

import edu.umd.cs.piccolo.*;
import edu.umd.cs.piccolo.util.*;
import edu.umd.cs.piccolo.nodes.*;
import edu.umd.cs.piccolox.nodes.PLine;
import edu.umd.cs.piccolox.pswing.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.imageio.ImageIO;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import edu.umd.cs.piccolo.event.*;

public class EmailButton extends CanvasMenuButton
{
	private static final long serialVersionUID = 1L;
	
	private long cuid = 0L;
	
	
	public EmailButton(long c)
	{
		super();
		cuid = c;
		iconString = "email.canvas";
		try
		{
			setImage(CalicoIconManager.getIconImage(iconString));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
	}

	public void actionMouseClicked(InputEventInfo event)
	{
		if (event.getAction() == InputEventInfo.ACTION_PRESSED)
		{
			super.onMouseDown();
		}
		else if (event.getAction() == InputEventInfo.ACTION_RELEASED && isPressed)
		{

			final Object response = JOptionPane.showInputDialog(CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getComponent(),
					"Please enter the email address(es) you wish to send this canvas to",
					"Email Canvas",
					JOptionPane.QUESTION_MESSAGE, null, null, CalicoDataStore.default_email);
			//			String responseText = response.toString();
			if (response != null)
			{
				Calendar cal = new GregorianCalendar();

				// Get the components of the time
				int hour12 = cal.get(Calendar.HOUR);            // 0..11
				int min = cal.get(Calendar.MINUTE);             // 0..59
				int ampm = cal.get(Calendar.AM_PM);             // 0=AM, 1=PM
				final String time = "" + hour12 + ":" + min + " " + ((ampm==0)?"AM":"PM") + ", " + cal.get(Calendar.MONTH)+1 + "/" + cal.get(Calendar.DAY_OF_MONTH) + "/" + cal.get(Calendar.YEAR);

				// Send the message
				(new Thread(
						new Runnable() { public void run() { 
							try {
								send(CalicoDataStore.email.smtpHost, CalicoDataStore.email.smtpPort, CalicoDataStore.Username + " <" + CalicoDataStore.email.replyToEmail + ">", 
										response.toString(), "Calico Canvas " + CGrid.getCanvasCoord(CCanvasController.getCurrentUUID()) + " - " + time, 
										"Screenshot of Calico Canvas\n\n");
							} catch (AddressException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (MessagingException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}})
						).start();

				CalicoPacket packet = CalicoPacket.getPacket(NetworkCommand.DEFAULT_EMAIL, response.toString());
				packet.rewind();
				PacketHandler.receive(packet);
				Networking.send(packet);


			}
			super.onMouseUp();
		}

		//StatusMessage.popup("Not yet implemented");
	}
	
    public static void send(String smtpHost, int smtpPort,
            String from, String to,
            String subject, String content)
		throws AddressException, MessagingException {
		// Create a mail session
		java.util.Properties props = new java.util.Properties();
		props.put("mail.smtp.host", smtpHost);
		props.put("mail.smtp.port", ""+smtpPort);
		props.put("mail.smtps.auth", CalicoDataStore.email.smtpsAuth);
		Session session = Session.getDefaultInstance(props, null);
		
		// Construct the message
		Message msg = new MimeMessage(session);
		msg.setFrom(new InternetAddress(from));
		msg.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
		msg.setSubject(subject);
//		msg.setText(content);
		
		String attachment = getImageAttachment();
		MimeBodyPart messagePart = new MimeBodyPart();
		messagePart.setText(content);
		             
		
		MimeBodyPart attachmentPart = new MimeBodyPart();
		FileDataSource fileDataSource = new FileDataSource(attachment) {
		   @Override
		   public String getContentType() {
			   return "application/octet-stream";
		   }
		};
		attachmentPart.setDataHandler(new DataHandler(fileDataSource));
		attachmentPart.setFileName(attachment);
		
		Multipart multipart = new MimeMultipart();
		multipart.addBodyPart(messagePart);
		multipart.addBodyPart(attachmentPart);
		
		msg.setContent(multipart);
		
		// Send the message
		Transport t = session.getTransport("smtps");
	    try {
			t.connect(smtpHost, CalicoDataStore.email.username, CalicoDataStore.email.password);
			t.sendMessage(msg, msg.getAllRecipients());
	    } finally {
	    	t.close();
	    	JOptionPane.showMessageDialog(CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getComponent(), "Email sent successfully");
	    }

	}
    
    public static String getImageAttachment()
    {
    	BufferedImage bIMG = new BufferedImage(CalicoDataStore.ScreenWidth, CalicoDataStore.ScreenHeight, BufferedImage.TYPE_INT_ARGB);
    	RenderedImage img = (RenderedImage)CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getLayer().toImage(bIMG, Color.white);
    	String fileName = "CalicoCanvas_" + CGrid.getCanvasCoord(CCanvasController.getCurrentUUID()) + ".png";
    	
    	saveImage(img, fileName);
    	return fileName;
    }
    
	public static void saveImage(RenderedImage img, String fileName) {
		// Write generated image to a file
	    try {
	        // Save as JPEG
	        File file = new File(fileName);
	        ImageIO.write(img, "png", file);
	        System.out.println("Path: " + file.getAbsolutePath());
	    } 

	    catch (IOException e) {
	    	e.printStackTrace();
	    }
	}
	
}
