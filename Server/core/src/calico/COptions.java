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

import java.awt.*;
import java.net.ServerSocket;
import java.nio.ByteOrder;
import org.apache.commons.vfs.*;
import org.apache.commons.vfs.impl.*;

public class COptions
{
	public static class listen
	{
		public static String host = "0.0.0.0";
		public static int port = 27000;
	}
	
	public static class debug
	{
		public static boolean enabled = false;
		public static boolean consistency_debug_enabled = true;
		
		public static class unittests
		{
			public static int bound_width = 800;
			public static int bound_height = 600;
			public static Color[] stroke_colors = {Color.RED, Color.BLUE, Color.BLACK, Color.GREEN, Color.PINK};
		}
	}


	public static class client
	{
		public static class threadopts
		{
			public static long sleeptime = 5L;
		}
		public static class network
		{
			public static long timeout = 15000L;
			public static int cluster_size = 400;// this is the number of coordinates to group together
		}
	}


	public static class admin
	{
		public static String server_signature = "Calico Server API/1.1";
		public static ServerSocket serversocket;
		
		public static class listen
		{
			public static String host = "0.0.0.0";
			public static int port = 27001;
			public static int timeout = 15000;
			public static int buffer = 8192;
			public static boolean tcp_nodelay = true;
			public static boolean stale_conn_check = false;
		}
	}


	public static class canvas
	{
		public static int max_snapshots = 50;
		public static long sleeptime = 10L; // Must be less than 5000
		public static int max_sleep_count = (int) (5000 / sleeptime);
		public static int width = 1600;
		public static int height = 1200;
	}
	
	public static class uuid
	{
		public static int block_size = 300;
		public static int allocation_increment = 500;
		public static int min_size = 500;
	}
	
	public static class server
	{
		public static int tickrate = 66;
		public static String default_email = "";
		public static class backup
		{
			public static boolean enable_autobackup = true;
			public static int write_on_tick = 50; // tickrate * <thisnum>
			public static String backup_file = "backup_auto.csb";
			public static String backup_file_alt = "backup_auto";
		}
		public static class images
		{
			public static String download_folder = "uploads/images/";
		}
		public static class email
		{
			public static String smtpHost = "smtp.gmail.com";
			public static int smtpPort = 465;
			public static String smtpsAuth = "true";
			public static String replyToEmail = "ucicalicodev@gmail.com";
			public static String username = "ucicalicodev@gmail.com";
			public static String password = "calico99";
		}

		public static String plugins = "";
	}
	
	public static class group
	{
		public static int padding = 10;
		public static int text_padding = 0;
		public static Font font = new Font("Helvetica", Font.PLAIN, 14);
	}
	
	public static class stroke
	{
		public static Color default_color = Color.BLACK;
		public static float default_thickness = 1.0f;
	}

	////////////////////////////////////////////////////////////

	// Year-month-day hour:min:sec.mil
	//public static final String LOG_FORMAT_STD = "[%p] %d{ISO8601} %m";
	public static final String LOG_FORMAT_STD = "[%p] %d{yyyy-MM-dd HH:mm:ss.SSS} %m%n";
	public static final String LOG_FORMAT_DATEMSG = "%d{yyyy-MM-dd HH:mm:ss.SSS} %m%n";
	public static final String DATEFILE_FMT = "'.'yyyy-MM-dd";

	
	public static String log_path = "../logs/"; 
	
	public static int GridRows = 7;
	public static int GridCols = 7;
	
	public static String APIDefaultContentType = "text/plain";
	
	
	public static int DuplicateGroupShiftDelta = 20;
	
	
	public static DefaultFileSystemManager fs;
	
	
	public static long ServerStartTime = 0L;
	
	public String testing = "testing";
	
}//

