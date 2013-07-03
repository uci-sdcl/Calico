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

import it.unimi.dsi.fastutil.longs.LongArrayList;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Properties;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.UIManager;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.json.me.JSONArray;
import org.json.me.JSONObject;

import calico.components.CCanvasWatermark;
import calico.components.CSession;
import calico.components.bubblemenu.BubbleMenu;
import calico.components.grid.CGrid;
import calico.controllers.CArrowController;
import calico.controllers.CCanvasController;
import calico.controllers.CConnectorController;
import calico.controllers.CGridController;
import calico.controllers.CGroupController;
import calico.controllers.CHistoryController;
import calico.controllers.CStrokeController;
import calico.events.CalicoEventHandler;
import calico.iconsets.CalicoIconManager;
import calico.input.CInputMode;
import calico.input.CalicoKeyListener;
import calico.inputhandlers.CalicoInputManager;
import calico.inputhandlers.InputQueue;
import calico.networking.Networking;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;
import calico.perspectives.CalicoPerspective;
import calico.perspectives.GridPerspective;
import calico.plugins.CalicoPluginManager;
import calico.utils.Ticker;

public class Calico extends JFrame
{
	private static final long serialVersionUID = 1L;

	public static Logger logger = Logger.getLogger(Calico.class.getName());

	private static Calico instance = null;

	private static Ticker ticker = null;
	
	// MODES
	// public static final int MODE_EXPERT = 1 << 0;
	// public static final int MODE_SCRAP = 1 << 1;
	// public static final int MODE_STROKE = 1 << 2;
	// public static final int MODE_ARROW = 1 << 3;
	// public static final int MODE_DELETE = 1 << 4;
	// public static final int MODE_POINTER = 1 << 5;

	public static boolean isGridLoading = true;

	public GraphicsConfiguration gConf = null;

	public static LongArrayList uuidlist = new LongArrayList();
	
	public static boolean isAllocating = false;

	/**
	 * This generates a 128-bit unique ID.
	 * 
	 * @return the uuid
	 */
	public static long uuid()
	{
		if (uuidlist.size() < 300 && !isAllocating)
		{
			// Request More
			Networking.send(NetworkCommand.UUID_GET_BLOCK);
			isAllocating = true;
		}
		
		if (uuidlist.size() == 0)
		{
			System.out.println("UUID Allocation: Pending");
			int waitTimes = 10;
			int count = 0;
			Networking.ignoreConsistencyCheck = true;
			
			//Will wait 10 seconds, then resend allocation packet. 
			//If no uuid_blocks arrive, program will hang here. It can't do anything without uuid's anyways
			while (true)
			{
				if (count >= waitTimes)
				{
					Networking.send(NetworkCommand.UUID_GET_BLOCK);
					isAllocating = true;
					count = 0;
				}
				
				if (uuidlist.size() > 0)
				{
					Networking.ignoreConsistencyCheck = true;
					System.out.println("UUID Allocation: Complete");
					break;
				}
				count++;
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}

		return uuidlist.removeLong(0);
	}

	public static long numUUIDs()
	{
		return uuidlist.size();
	}

	public static void main(String[] args)
	{
		/*
		 * int pixel = (new Color(100,200,250)).getRGB(); int rp = (pixel & 0x00FF0000)>>16; int gp = (pixel &
		 * 0x0000FF00)>>8; int bp = (pixel & 0x000000FF);
		 */

		String ipaddress = "", port = "";

		if (args.length >= 2)
		{

		}

		if (!CalicoOptions.webstart.isWebstart)
			DOMConfigurator.configure(System.getProperty("log4j.configuration", "conf/log4j.xml"));

		// logger.debug("Color: "+rp+","+gp+","+bp);

		Thread.currentThread().setName("Calico Main");
		
		// We load the conf/calico.conf file
		CalicoOptions.setup();
		CalicoDataStore.setup();
		setPropertiesFromArgs(args);
		try
		{

			// Show the connection screen

			if (CalicoDataStore.RunStressTest)
			{
				System.out.println("Loading stress test");
				Calico.reconnect(CalicoDataStore.ServerHost, CalicoDataStore.ServerPort);
			}
			else if (CalicoDataStore.SkipConnectionScreen)
			{
				Calico.reconnect(CalicoDataStore.ServerHost, CalicoDataStore.ServerPort);
			}
			else
			{
				System.out.println("Loading connection box");
				Calico.getConnection();
			}
			System.out.println("Username: " + CalicoDataStore.Username);
		}
		catch (HeadlessException he)
		{
			logger.fatal("This program cannot be run on a headless system. Please run from a GUI environment.");
			Calico.exit();
		}

		// System.getProperties().list(System.out);

	}

	private static void setPropertiesFromArgs(String[] args)
	{
		System.out.println(Arrays.toString(args));
		for (int i = 0; i < args.length; i++)
		{
			if (args[i].compareTo("-hitachistarboardfix") == 0)
			{
				CalicoDataStore.enableHitachiStarboardFix = true;
				System.out.println("Enabling starboard fix");
			}
			else if (args[i].charAt(0) == '-' && i + 1 < args.length)
			{
				if (args[i].compareTo("-ipaddress") == 0)
				{
					String host = args[++i];
					System.setProperty("calico.host", host);
					CalicoDataStore.ServerHost = host;
					System.out.println("Setting host to " + host);
				}
				else if (args[i].compareTo("-port") == 0)
				{
					int port = Integer.parseInt(args[++i]);
					System.setProperty("calico.port", ((new Integer(port)).toString()));
					CalicoDataStore.ServerPort = port;
					System.out.println("Setting port to " + port);
				}
				else if (args[i].compareTo("-defaultusername") == 0)
				{
					CalicoDataStore.Username = System.getProperty("user.name");
					System.out.println("Setting username to " + System.getProperty("user.name"));
				}
				else if (args[i].compareTo("-skipconnectionscreen") == 0)
				{
					CalicoDataStore.SkipConnectionScreen = true;
					System.out.println("Skipping connection screen");
				}
				else if (args[i].compareTo("-stresstest") == 0)
				{
					CalicoDataStore.RunStressTest = true;
					CalicoDataStore.Username = "StressTester-" + (new Random()).nextInt(1000);
					System.out.println("Setting RunStressTest to " + CalicoDataStore.RunStressTest);
				}
				else if (args[i].compareTo("-stinterval") == 0)
				{
					CalicoDataStore.StressTestInterval = Long.valueOf(args[++i]).intValue();
					System.out.println("Setting StressTestInterval to " + CalicoDataStore.StressTestInterval);
				}
				else if (args[i].compareTo("-resw") == 0)
				{
					CalicoDataStore.ScreenWidth = Integer.parseInt(args[++i]);
					if (CalicoDataStore.ScreenWidth == 0)
						CalicoDataStore.isFullScreen = true;
					System.out.println("Setting ScreenWidth to " + CalicoDataStore.ScreenWidth);
				}
				else if (args[i].compareTo("-resh") == 0)
				{
					CalicoDataStore.ScreenHeight = Integer.parseInt(args[++i]);
					if (CalicoDataStore.ScreenHeight == 0)
						CalicoDataStore.isFullScreen = true;
					System.out.println("Setting ScreenHeight to " + CalicoDataStore.ScreenHeight);
				}

			}
		}
		if (CalicoDataStore.ServerHost == null || CalicoDataStore.ServerPort == 0 || CalicoDataStore.StressTestInterval == Integer.MAX_VALUE
				|| CalicoDataStore.ScreenWidth == 0 || CalicoDataStore.ScreenHeight == 0 || CalicoDataStore.RunStressTest == false)
		{
			// System.out.println("Not running stress test!");
			CalicoDataStore.RunStressTest = false;
		}
		else
		{
			// System.out.println("Running stress test!");
		}
	}

	public Calico(GraphicsConfiguration gConf)
	{
		this.gConf = gConf;
	}

	private void setupCalico()
	{
		if (CalicoDataStore.calicoObj != null)
		{
			CalicoDataStore.calicoObj.dispose();
		}

		CalicoDataStore.calicoObj = this;
		CalicoDataStore.messageHandlerObj = StatusMessageHandler.getInstance();

		try { 
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); 
		} 
		catch(Exception e) 
		{ 
			System.out.println("Error setting Java LAF: " + e); 
		}

		// Run the setup methods
		CArrowController.setup();
		CCanvasController.setup();
		CGroupController.setup();
		CStrokeController.setup();
		CConnectorController.setup();
		CHistoryController.setup();
		BubbleMenu.setup();
		CalicoInputManager.setup();

		// Load the icon theme
		CalicoIconManager.setIconTheme(CalicoOptions.core.icontheme);
		CInputMode.setup();
		CCanvasWatermark.InputModeWatermarks.setup();

		Thread.currentThread().setName("Calico Main 3");

		if (CalicoDataStore.isFullScreen)
		{

			Dimension fullScreen = Toolkit.getDefaultToolkit().getScreenSize();
			setBounds(0, 0, fullScreen.width, fullScreen.height);
			setUndecorated(true);
			Calico.logger.debug("SET W+H (" + fullScreen.width + "," + fullScreen.height + ")");
			CalicoDataStore.ScreenWidth = fullScreen.width;
			CalicoDataStore.ScreenHeight = fullScreen.height;
		}
		else
		{
			setBounds(50, 50, CalicoDataStore.ScreenWidth, CalicoDataStore.ScreenHeight);
		}

		CalicoDataStore.CanvasSnapshotSize.setSize(CalicoDataStore.ScreenWidth / 7, CalicoDataStore.ScreenHeight / 7);

		// Setup the networking system
		Networking.setup();

		if (ticker == null)
		{
			ticker = new Ticker();
			ticker.start();
		}

		CCanvasController.setup();

		// Join!
		Networking.join();

		// Wait to initialize the plugins until after the network connection has been established, because the plugins
		// may need to use network services as they are initializing. They should not wait around for the network to
		// appear. For example, UUID request crashes the entire application if called before networking is up.
		CalicoPluginManager.setup();
		

		if (!CalicoPluginManager.hasPlugin(
				"calico.plugins.iip.IntentionalInterfacesClientPlugin"))
		{
			GridPerspective.getInstance().activate();
			CGridController.getInstance();
			CGrid.loadGrid();
			logger.info("Initialized grid");
		}


		this.addComponentListener(new java.awt.event.ComponentAdapter() {
			public void componentResized(ComponentEvent e)
			{
				// System.out.println(e.paramString());

				// must refer to the content pane, because when Calico runs in a window frame, the frame size will
				// include the fat frame border supplied by the OS, which is not relevant to our coordinate system.
				Dimension dim = Calico.this.getContentPane().getSize();
				// System.out.println(e.getComponent().getSize().toString());

				CalicoDataStore.ScreenWidth = dim.width;
				CalicoDataStore.ScreenHeight = dim.height;

				long cuid = CCanvasController.getCurrentUUID();
				if (cuid != 0L)
				{
					CCanvasController.windowResized();
				}

			}

		});

		// The title and die function
		setTitle(CalicoOptions.core.version);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
		setCursor(getDrawCursor());

		// When the window closes, finalize - which right now is just closing the output stream
		addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(WindowEvent winEvt)
			{
				Calico.exit();
			}
			// windowLostFocus
			// windowGainedFocus
			// windowStateChanged
		});

		Thread inputThread = new Thread(null, new InputQueue(), "InputQueue");
		inputThread.start();

		addKeyListener(new CalicoKeyListener());

		if (!(new File(CalicoOptions.images.download_folder + "/")).exists())
			(new File(CalicoOptions.images.download_folder)).mkdir();
	
	}

	public static void exit()
	{
		Networking.leave();

		System.exit(0);
	}

	private static JTextField nameBox;
	private static JTextField nickBox;
	private static JTextField passBox;
	private static JTextField portBox;
	private static JTextField reswBox;
	private static JTextField reshBox;
	private static JFrame conn;

	private static void getConnection()
	{
		conn = new JFrame();

		conn.setTitle("Calico Session");
		conn.setLocation(new Point(225, 270));
		conn.setLayout(null);
		Properties settings = null;
		if (!CalicoOptions.webstart.isWebstart)
		{
			try
			{
				settings = CalicoOptions.loadPropertyFile(CalicoOptions.core.connection.settings_file);
			}
			catch (Exception e)
			{
				settings = new Properties();
			}
		}
		else
		{
			settings = new Properties();
		}

		String tempHost = System.getProperty("calico.host", settings.getProperty("host", "localhost"));
		String tempPort = System.getProperty("calico.port", settings.getProperty("port", "27000"));

		nameBox = new JTextField(tempHost, 16);
		portBox = new JTextField(tempPort, 5);
		nickBox = new JTextField(settings.getProperty("username", (CalicoOptions.webstart.isWebstart) ? "Table-" : System.getProperty("user.name")), 16);
		passBox = new JTextField(settings.getProperty("password"), 16);
		reswBox = new JTextField(settings.getProperty("resw", "0"), 4);
		reshBox = new JTextField(settings.getProperty("resh", "0"), 4);
		JButton submit = new JButton();
		submit.setText(" Connect ");

		conn.setPreferredSize(new Dimension(430, 225));

		submit.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent ev)
			{
				submitConnectionForm();
			}
		});
		nameBox.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent ev)
			{
				submitConnectionForm();
			}
		});
		nickBox.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent ev)
			{
				submitConnectionForm();
			}
		});
		passBox.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent ev)
			{
				submitConnectionForm();
			}
		});
		portBox.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent ev)
			{
				submitConnectionForm();
			}
		});
		reswBox.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent ev)
			{
				submitConnectionForm();
			}
		});
		reshBox.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent ev)
			{
				submitConnectionForm();
			}
		});

		JLabel label_username = new JLabel("Username:");
		label_username.setPreferredSize(new Dimension(70, 20));

		JLabel label_hostinfo = new JLabel("IP:");
		label_hostinfo.setPreferredSize(new Dimension(70, 20));

		JLabel label_portinfo = new JLabel("Port:");
		label_portinfo.setPreferredSize(new Dimension(30, 20));

		JLabel label_password = new JLabel("Password:");
		label_password.setPreferredSize(new Dimension(70, 20));

		JLabel label_resolution = new JLabel("Resolution:");
		label_resolution.setPreferredSize(new Dimension(80, 20));

		JLabel label_by = new JLabel("X");
		label_by.setPreferredSize(new Dimension(8, 20));

		JLabel label_fs = new JLabel("(Use 0 for fullscreen)");
		label_fs.setPreferredSize(new Dimension(150, 20));

		conn.add(label_username);
		conn.add(label_hostinfo);
		conn.add(label_portinfo);
		conn.add(label_password);
		conn.add(label_resolution);
		conn.add(label_by);
		conn.add(label_fs);
		conn.add(nameBox);
		conn.add(portBox);
		conn.add(nickBox);
		conn.add(passBox);
		conn.add(reswBox);
		conn.add(reshBox);
		conn.add(submit);

		Insets insets = conn.getInsets();

		Dimension size_lu = label_username.getPreferredSize();
		Dimension size_hi = label_hostinfo.getPreferredSize();
		Dimension size_pi = label_portinfo.getPreferredSize();
		Dimension size_pa = label_password.getPreferredSize();
		Dimension size_re = label_resolution.getPreferredSize();
		Dimension size_by = label_by.getPreferredSize();
		Dimension size_fs = label_fs.getPreferredSize();

		Dimension size_nb = nameBox.getPreferredSize();
		Dimension size_pb = portBox.getPreferredSize();
		Dimension size_nib = nickBox.getPreferredSize();
		Dimension size_pab = passBox.getPreferredSize();
		Dimension size_rewb = reswBox.getPreferredSize();
		Dimension size_rehb = reshBox.getPreferredSize();
		Dimension size_sub = submit.getPreferredSize();

		// IP: [ ] PORT: [ ]
		int hpos = 5 + insets.top;

		label_hostinfo.setBounds(5 + insets.left, hpos, size_hi.width, size_hi.height);
		nameBox.setBounds(15 + insets.left + size_hi.width, 5 + insets.top, size_nb.width, size_nb.height);

		label_portinfo.setBounds(insets.left + size_hi.width + size_nb.width + 20, hpos, size_pi.width, size_pi.height);
		portBox.setBounds(insets.left + size_hi.width + size_nb.width + 20 + size_pi.width, hpos, size_pb.width, size_pb.height);

		hpos = hpos + size_pb.height + 5;

		// Username
		label_username.setBounds(5 + insets.left, hpos, size_lu.width, size_lu.height);
		nickBox.setBounds(15 + size_lu.width + insets.left, hpos, size_nib.width, size_nib.height);

		hpos = hpos + size_pb.height + 5;

		// password
		label_password.setBounds(5 + insets.left, hpos, size_pa.width, size_pa.height);
		passBox.setBounds(15 + size_pa.width + insets.left, hpos, size_pab.width, size_pab.height);

		hpos = hpos + size_pb.height + 5;

		// Resolution
		int xpos = 5 + insets.left;
		label_resolution.setBounds(xpos, hpos, size_re.width, size_re.height);
		xpos = xpos + 0 + size_re.width;
		reswBox.setBounds(xpos, hpos, size_rewb.width, size_rewb.height);
		xpos = xpos + 0 + size_rewb.width;
		label_by.setBounds(xpos, hpos + 3, size_by.width, size_by.height);
		xpos = xpos + 0 + size_by.width;
		reshBox.setBounds(xpos, hpos, size_rehb.width, size_rehb.height);
		xpos = xpos + 0 + size_rehb.width;
		label_fs.setBounds(xpos, hpos + 5, size_fs.width, size_fs.height);

		// Submit
		hpos = hpos + size_pb.height + 5;
		submit.setBounds(100, hpos, size_sub.width, size_sub.height);

		if (!CalicoOptions.webstart.isWebstart)
			conn.setAlwaysOnTop(true);
		conn.setVisible(true);
		conn.repaint();
		conn.pack();

	}

	public static void reconnect(String host, int port)
	{
		// CalicoDataStore.SessionName = "default";//sessBox.getText();
		// CalicoDataStore.Username = nickBox.getText();
		// CalicoDataStore.Password = passBox.getText();

		CalicoDataStore.ServerHost = host;
		CalicoDataStore.ServerPort = port;

		// XXX: WE MUST RESET THIS TO 0. Otherwise, the client will just hang there like an idiot
		CGrid.GridRows = 0;
		CGrid.GridCols = 0;

		if (Networking.receivePacketThread != null && !Networking.receivePacketThread.isInterrupted())
		{
			Networking.receivePacketThread.interrupt();
		}

		if (Networking.sendPacketThread != null && !Networking.sendPacketThread.isInterrupted())
		{
			Networking.sendPacketThread.interrupt();
		}

		Networking.recvQueue.clear();
		Networking.sendQueue.clear();

		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice gd = ge.getDefaultScreenDevice();

		if (instance != null)
		{
			instance.dispose();
			instance = null;
		}

		if (ticker != null)
		{
			ticker.interrupt();
			ticker = null;
		}

		instance = new Calico(gd.getDefaultConfiguration());
		instance.setupCalico();
	}

	private static void submitConnectionForm()
	{

		CalicoEventHandler.getInstance().fireEvent(NetworkCommand.STATUS_SENDING_LARGE_FILE_START,
				CalicoPacket.getPacket(NetworkCommand.STATUS_SENDING_LARGE_FILE_START, 0, 1, "Synchronizing with server... "));
		Properties settings = new Properties();

		settings.setProperty("host", nameBox.getText());
		settings.setProperty("port", portBox.getText());
		settings.setProperty("username", nickBox.getText());
		settings.setProperty("password", passBox.getText());
		settings.setProperty("resw", reswBox.getText());
		settings.setProperty("resh", reshBox.getText());

		CalicoOptions.writePropertyFile(settings, CalicoOptions.core.connection.settings_file);

		CalicoDataStore.SessionName = "default";// sessBox.getText();
		CalicoDataStore.Username = nickBox.getText();
		CalicoDataStore.Password = passBox.getText();

		int resw = Integer.parseInt(reswBox.getText());
		int resh = Integer.parseInt(reshBox.getText());

		CalicoDataStore.ServerHost = nameBox.getText();
		CalicoDataStore.ServerPort = Integer.parseInt(portBox.getText());

		if (resw == 0 || resh == 0)
		{
			CalicoDataStore.isFullScreen = true;
		}
		else
		{
			CalicoDataStore.ScreenWidth = resw;
			CalicoDataStore.ScreenHeight = resh;
		}

		/*
		 * GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment(); GraphicsDevice gd =
		 * ge.getDefaultScreenDevice();
		 * 
		 * instance = new Calico(gd.getDefaultConfiguration());
		 * 
		 * instance.setupCalico(); conn.dispose();
		 */
		conn.dispose();
		Calico.reconnect(CalicoDataStore.ServerHost, CalicoDataStore.ServerPort);

	}

	public static Cursor getDrawCursor()
	{
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		BufferedImage image = new BufferedImage(30, 30, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D) image.getGraphics();
		g.setColor(Color.black);
		g.drawRect(0, 0, 1, 1);
		// Image image = CalicoIconManager.getIconImage("cursor.dot");
		Point hotSpot = new Point(0, 0);

		return toolkit.createCustomCursor(image, hotSpot, "DrawCursor");
	}

	private static JFrame sessionPopup;
	private static JTextField newSessionNameTextBox;
	private static JTextField newSessionSizeTextBox;
	private static JList sessionList;

	public static void showSessionPopup()
	{
		sessionPopup = new JFrame();

		HttpClient httpclient = new DefaultHttpClient();
		try
		{
			HttpGet httpget = new HttpGet("http://" + CalicoDataStore.ServerHost + ":27015/api/sessions");

			// System.out.println("executing request " + httpget.getURI());

			// Create a response handler
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			String responseBody = httpclient.execute(httpget, responseHandler);

			JSONObject json = new JSONObject(responseBody);

			CalicoDataStore.sessiondb.clear();
			JSONArray sessionList = json.getJSONArray("sessions");
			for (int i = 0; i < sessionList.length(); i++)
			{
				JSONObject sessionObj = sessionList.getJSONObject(i);
				CalicoDataStore.sessiondb.add(new CSession(sessionObj.getString("name"), CalicoDataStore.ServerHost, sessionObj.getJSONObject("calico_server")
						.getInt("port")));
			}

		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{
			// When HttpClient instance is no longer needed,
			// shut down the connection manager to ensure
			// immediate deallocation of all system resources
			httpclient.getConnectionManager().shutdown();
		}
		/*
		 * CalicoDataStore.sessiondb.add(new CSession("Session1", "calico.ics.uci.edu", 27000));
		 * CalicoDataStore.sessiondb.add(new CSession("Session2", "calico.ics.uci.edu", 27001));
		 * CalicoDataStore.sessiondb.add(new CSession("Session3", "calico.ics.uci.edu", 27002));
		 * CalicoDataStore.sessiondb.add(new CSession("Session4", "calico.ics.uci.edu", 27003));
		 * CalicoDataStore.sessiondb.add(new CSession("Session5", "calico.ics.uci.edu", 27004));
		 * CalicoDataStore.sessiondb.add(new CSession("Session6", "calico.ics.uci.edu", 27005));
		 */
		sessionList = new JList(CalicoDataStore.sessiondb.toArray(new CSession[] {}));
		sessionPopup.setTitle("Calico Session");
		sessionPopup.setLocation(new Point(225, 270));
		// sessionPopup.setLayout(null);

		JScrollPane listScroller = new JScrollPane(sessionList);
		listScroller.setPreferredSize(new Dimension(250, 80));
		listScroller.setAlignmentX(LEFT_ALIGNMENT);
		// ...
		// Lay out the label and scroll pane from top to bottom.
		JPanel listPane = new JPanel();
		listPane.setLayout(new BoxLayout(listPane, BoxLayout.PAGE_AXIS));
		JLabel label = new JLabel("Sessions");
		// ...
		listPane.add(label);
		listPane.add(Box.createRigidArea(new Dimension(0, 5)));
		listPane.add(listScroller);
		listPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JButton cancelButton = new JButton("Cancel");
		JButton createButton = new JButton("Create");
		JButton joinButton = new JButton("Join");
		newSessionNameTextBox = new JTextField("", 16);
		newSessionSizeTextBox = new JTextField("5", 3);

		cancelButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent ev)
			{
				closeSessionPopup();
			}
		});
		joinButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent ev)
			{
				pressJoinSessionButton();
			}
		});
		createButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent ev)
			{
				createNewSession();
			}
		});

		// Lay out the buttons from left to right.
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(cancelButton);
		buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
		buttonPane.add(joinButton);

		JPanel createPane = new JPanel();
		createPane.setLayout(new BoxLayout(createPane, BoxLayout.LINE_AXIS));
		createPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		createPane.add(Box.createHorizontalGlue());
		createPane.add(newSessionNameTextBox);
		createPane.add(newSessionSizeTextBox);
		createPane.add(Box.createRigidArea(new Dimension(10, 0)));
		createPane.add(createButton);

		sessionPopup.add(listPane, BorderLayout.PAGE_START);
		sessionPopup.add(buttonPane, BorderLayout.CENTER);
		sessionPopup.add(createPane, BorderLayout.PAGE_END);

		sessionPopup.setAlwaysOnTop(true);
		sessionPopup.setVisible(true);
		sessionPopup.repaint();
		sessionPopup.pack();
	}

	private static void closeSessionPopup()
	{
		sessionPopup.dispose();
	}

	private static void createNewSession()
	{
		String name = newSessionNameTextBox.getText();
		String grid_size = newSessionSizeTextBox.getText();
		System.out.println("New Session Name: " + newSessionNameTextBox.getText());
		closeSessionPopup();

		HttpClient httpclient = new DefaultHttpClient();
		int port = CalicoDataStore.ServerPort;
		try
		{
			HttpPost httppost = new HttpPost("http://" + CalicoDataStore.ServerHost + ":27015/api/sessions");

			String requestParams = "";

			requestParams += "name=" + URLEncoder.encode(name, "utf-8");
			requestParams += "&rows=" + URLEncoder.encode(grid_size, "utf-8");
			requestParams += "&cols=" + URLEncoder.encode(grid_size, "utf-8");

			InputStreamEntity inputEntity = new InputStreamEntity(new ByteArrayInputStream(requestParams.getBytes()), -1);
			inputEntity.setContentType("application/x-www-form-urlencoded");
			inputEntity.setChunked(false);
			httppost.setEntity(inputEntity);

			// System.out.println("executing request " + httpget.getURI());

			// Create a response handler
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			String responseBody = httpclient.execute(httppost, responseHandler);

			JSONObject sessionObj = new JSONObject(responseBody);
			port = sessionObj.getJSONObject("calico_server").getInt("port");
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{
			// When HttpClient instance is no longer needed,
			// shut down the connection manager to ensure
			// immediate deallocation of all system resources
			httpclient.getConnectionManager().shutdown();
		}
		// reconnect(CalicoDataStore.ServerHost, port);
		showSessionPopup();
	}

	private static void pressJoinSessionButton()
	{
		int selectedIndex = sessionList.getAnchorSelectionIndex();
		CSession selectedSession = CalicoDataStore.sessiondb.get(selectedIndex);

		logger.info("Connecting to selected session: " + selectedSession.toString());
		closeSessionPopup();
		reconnect(selectedSession.getHost(), selectedSession.getPort());

	}

}
