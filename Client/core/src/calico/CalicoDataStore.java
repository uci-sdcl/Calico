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
package calico;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;

import calico.components.CSession;
import calico.controllers.CCanvasController;
import calico.input.CInputMode;



public class CalicoDataStore
{
	// Object Storage

	/**
	 * This is a reference back to the main Calico object
	 */
	public static Calico calicoObj	= null;
	public static StatusMessageHandler messageHandlerObj = null;

	public static int ScreenWidth = 0;
	public static int ScreenHeight = 0;
	public static boolean isFullScreen = false;
	public static final Dimension CanvasSnapshotSize = new Dimension();
	public static int serverScreenWidth = 0;
	public static int serverScreenHeight = 0;

	public static String SessionName = "";
	public static String Username = "SDCL-";
	public static String Password = "";

	public static boolean SkipConnectionScreen = false;
	
	public static String ServerHost = null;
	public static int ServerPort = 0;
	public static int ServerHTTPPort = 0;
	
	public static boolean RunStressTest = false;
	public static long StressTestInterval = Integer.MAX_VALUE;
	public static long timeLastStressPacketSent = 0;
	
	public static String default_email = "";
	
	
	public static boolean enableHitachiStarboardFix = false;
	
	/**
	 * This is the current operating mode we are in
	 */
	public static CInputMode Mode = CInputMode.EXPERT;
	
	/**
	 * This is the current pen color
	 */
	public static Color PenColor = Color.BLACK;
	public static float PenThickness = CalicoOptions.pen.stroke_size;
	
	public static Color LastDrawingColor = Color.BLACK;
	public static Color PointingColor = Color.ORANGE;
	
	public static float LastDrawingThickness = CalicoOptions.pen.stroke_size;
	
	
	// This is used to see when the last mouse/keyboard/finger/whatever was inputted
	// We will probably use this thing for running tasks after a certain amount of time.
	public static long LastUserActionTime = 0L;

	public static boolean initialScreenDisplayed = false;
	
	public static ArrayList<CSession> sessiondb = new ArrayList<CSession>();
	
	
	public static Int2ObjectOpenHashMap<String> clientInfo = new Int2ObjectOpenHashMap<String>();
	
	public static String lastOpenedDirectory = "";
	
	public static class email {
		public static String smtpHost = "smtp.gmail.com";
		public static int smtpPort = 465;
		public static String smtpsAuth = "true";
		public static String replyToEmail = "<ucicalicodev@gmail.com>";
		public static String username = "ucicalicodev@gmail.com";
		public static String password = "calico99";
	}
	
	/**
	 * Sets the default options
	 */
	public static void setup()
	{
		Mode = CalicoOptions.canvas.input.default_mode;
		PenColor = CalicoOptions.stroke.default_color;
		CalicoDataStore.messageHandlerObj = StatusMessageHandler.getInstance();
		
		LastUserActionTime = System.currentTimeMillis();
	}
	
	public static void set_Mode(CInputMode mode)
	{

//		if (Mode == Calico.MODE_EXPERT)
//			LastDrawingColor = PenColor;
//		if (Mode == Calico.MODE_POINTER)
//			PointingColor = PenColor;

		if (mode == CInputMode.EXPERT)
		{
			PenColor = LastDrawingColor;
			PenThickness = LastDrawingThickness;
		}
		if (mode == CInputMode.POINTER)
		{
			PenColor = PointingColor;
			PenThickness = 4.0f;
		}
		if (mode == CInputMode.ARROW)
		{
			PenColor = LastDrawingColor;
		}
		
		Mode = mode;
		CCanvasController.canvasModeChanged();
		
		//Calico.logger.debug("Switching to mode "+Mode+" ("+Mode_Reverse+")");
	}
	
	
	public static void touch_input()
	{
		LastUserActionTime = System.currentTimeMillis();
	}
	
	
}
