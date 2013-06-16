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
package calico.networking.netstuff;

import java.awt.event.MouseEvent;
import java.lang.reflect.*;

import it.unimi.dsi.fastutil.ints.*;

/**
 * This holds all the byte values for all the network commands
 *
 * @author Mitch Dempsey
 */
public class NetworkCommand
{
	public static Int2ObjectAVLTreeMap<NetCommandFormat> formats = new Int2ObjectAVLTreeMap<NetCommandFormat>();

	/*
	 * COMMAND FORMAT SYNTAX
	 * 
	 * S = string
	 * s = short
	 * 
	 * C = color
	 * c = char
	 * 
	 * L = long
	 * 
	 * I = 32-bit integer (signed)
	 * i = 16-bit integer (unsigned, 0-65535)
	 * 
	 * B = boolean
	 * b = byte
	 * 
	 * f = float
	 * 
	 * d = double
	 * 
	 */
	
	

	public static class ClickTrack
	{
		public static final int EXIT	= 1;

		public static final int ARROW_UP	= 2;
		public static final int ARROW_RIGHT	= 3;
		public static final int ARROW_LEFT	= 4;
		public static final int ARROW_DOWN	= 5;

		public static final int UNDO	= 6;
		public static final int REDO	= 7;

		public static final int RETURN_TO_GRID	= 8;
		
		public static final int CANVAS_CELL	= 9;
		
		//public static final int CLICKNAME	= 1000;
		//public static final int CLICKNAME	= 1000;
		//public static final int CLICKNAME	= 1000;
		//public static final int CLICKNAME	= 1000;
		//public static final int CLICKNAME	= 1000;
		//public static final int CLICKNAME	= 1000;
		//public static final int CLICKNAME	= 1000;
		//public static final int CLICKNAME	= 1000;
		//public static final int CLICKNAME	= 1000;
		//public static final int CLICKNAME	= 1000;
		//public static final int CLICKNAME	= 1000;
		//public static final int CLICKNAME	= 1000;
		//public static final int CLICKNAME	= 1000;
	}
	public static final int ACTION_PRESSED	= MouseEvent.MOUSE_PRESSED;
	public static final int ACTION_DRAGGED	= MouseEvent.MOUSE_DRAGGED;
	public static final int ACTION_RELEASED	= MouseEvent.MOUSE_RELEASED;
	public static final int ACTION_CLICKED	= MouseEvent.MOUSE_CLICKED;
	public static final int ACTION_SCROLL	= MouseEvent.MOUSE_WHEEL;
	
	// Join/leave
	public static final int JOIN					= 150;	// JOIN <NICKNAME> <PASSWORD>
	public static final int HEARTBEAT				= 152;	// HEARTBEAT
	public static final int LEAVE					= 153;	// LEAVE

	public static final int ACK						= 154;	// ACKNOWLEDGEMENT (Sent from server->client)

	public static final int SESSION_START			= 156; // <SESSION> <RandomString>
	public static final int DEFAULT_EMAIL			= 157; // Default email
	public static final int SERVER_HTTP_PORT		= 158;
	public static final int SERVER_EMAIL_SETTINGS	= 159;

	// Messages
	public static final int STATUS_MESSAGE			= 100; // <MESSAGE>
	public static final int ERROR_MESSAGE			= 101; // <MESSAGE>
	public static final int STATUS_POPUP			= 102; // <MESSAGE>
	public static final int ERROR_POPUP				= 103; // <MESSAGE>
	public static final int STATUS_SENDING_LARGE_FILE	= 104;
	public static final int STATUS_SENDING_LARGE_FILE_START	= 105;
	public static final int STATUS_SENDING_LARGE_FILE_FINISHED	= 106;

	// BG Elements
	public static final int BGE_START				= 200; // <UUID> <PARENT> <X> <Y>
	public static final int BGE_APPEND				= 201; // <UUID> <X> <Y>
	public static final int BGE_FINISH				= 202; // <UUID>
	public static final int BGE_COORDS				= 203; // <UUID> <COORDCOUNT(s)> <X1> <Y1> ... <XN> <YN>
	public static final int BGE_DELETE				= 204; // <UUID>
	public static final int BGE_COLOR				= 205; // <UUID> <RED> <GREEN> <BLUE>
	public static final int BGE_PARENT				= 206; // <UUID> <PARENT_UUID=0>
	public static final int BGE_MOVE				= 209; // <UUID> X Y
	public static final int BGE_CONSISTENCY			= 212;
	public static final int BGE_RELOAD_START		= 213;// UUID <CANVAS> <PARENT> <RED> <GREEN> <BLUE>
	public static final int BGE_RELOAD_COORDS		= 214;// UUID <POINTCT> <X1> <Y1> .. <Xn> <Yn>  (maximum of maybe 10-15 points)
	public static final int BGE_RELOAD_FINISH		= 215; //UUID
	public static final int BGE_MOVE_START			= 216;//UUID
	public static final int BGE_MOVE_FINISH			= 217;//UUID

	public static final int STROKE_RELOAD_START		= 218;// UUID <CANVAS> <PARENT> <RED> <GREEN> <BLUE>
	public static final int STROKE_RELOAD_COORDS	= 219;// UUID <POINTCT> <X1> <Y1> .. <Xn> <Yn>  (maximum of maybe 10-15 points)
	public static final int STROKE_RELOAD_FINISH	= 220; //UUID
	public static final int STROKE_RELOAD_REMOVE	= 221;// UUID
	public static final int STROKE_RELOAD_POSITION	= 222;// UUID X Y
	public static final int STROKE_START			= 223; // UUID CUID PUID RED GREEN BLUE
	public static final int STROKE_APPEND			= 224; // UUID NUMCOORDS X1 Y1 XN YN
	public static final int STROKE_FINISH			= 225; // UUID
	public static final int STROKE_SET_COLOR		= 226; // UUID RED GREEN BLUE
	public static final int STROKE_SET_PARENT		= 227; // UUID PUID
	public static final int STROKE_MOVE				= 228; // UUID XDELTA YDELTA
	public static final int STROKE_DELETE			= 229; // UUID
	public static final int STROKE_LOAD				= 230; // <---- (USE THIS FOR LOADING)  UUID CUID PUID <COLOR> <NUMCOORDS> x1 y1 ... xn yn <---------------
	public static final int STROKE_HASH_CHECK		= 231; // UUID <SIZE> <HASH_BYTES>
	public static final int STROKE_REQUEST_HASH_CHECK		= 232; // UUID (0L for all)
	public static final int STROKE_MAKE_SCRAP				= 233;
	public static final int STROKE_MAKE_SHRUNK_SCRAP		= 234;
	public static final int STROKE_DELETE_AREA				= 235;
	public static final int STROKE_ROTATE					= 236;
	public static final int STROKE_SCALE					= 237;
	public static final int STROKE_SET_AS_POINTER	= 238;
	public static final int STROKE_HIDE				= 239;
	public static final int STROKE_UNHIDE			= 240;
	
	public static final int ERASE_START 			= 290;
	public static final int ERASE_END				= 291;

	// Scraps
	public static final int SCRAP_START				= 300; // UUID PARENTUUID X Y
	public static final int SCRAP_APPEND			= 301; // UUID X Y
	public static final int SCRAP_FINISH			= 302; // UUID
	public static final int SCRAP_DELETE			= 303; // UUID
	public static final int SCRAP_MOVE				= 304; // UUID SHIFT_X SHIFT_Y
	public static final int SCRAP_DROP				= 305; // UUID
	public static final int SCRAP_COORDS			= 306; // UUID
	public static final int SCRAP_PARENT			= 307; // NOT USED
	public static final int SCRAP_FINISHMOVE		= 308; // UUID
	public static final int SCRAP_CHILD_BGE_ADD		= 309; // UUID
	public static final int SCRAP_CHILD_BGE_CLEAR	= 310; // UUID
	public static final int SCRAP_CHILD_SCRAP_ADD	= 311; // UUID
	public static final int SCRAP_CHILD_SCRAP_CLEAR	= 312; // UUID
	public static final int SCRAP_PARENTS_CLEAR		= 313;//
	public static final int SCRAP_STARTMOVE			= 314;//
	
	
	// Canvases
	public static final int CANVAS_SET              = 400; // <UUID> <- this erases? the canvas and then prepares for a new input
    public static final int CANVAS_REDRAW           = 401; // <UUID> (server->client)
    public static final int CANVAS_NUMBER           = 402; // <UUID> <NUMBER> (server->client)
    public static final int CANVAS_NUMBER_END       = 403; //
    public static final int CANVAS_LIST             = 404; // CtS |
    public static final int CANVAS_INFO             = 405; // StC | UID "Coord" XposOnGrid YposOnGrid
    public static final int CANVAS_UPDATE           = 406; // CtS | UUID    (This requests an update on the specific canvas)
    public static final int CANVAS_UNDO				= 407;// UUID
    public static final int CANVAS_REDO				= 408;//UUID
    public static final int CANVAS_RELOAD_START		= 409; // UUID
    public static final int CANVAS_RELOAD_FINISH	= 410; // UUID
    public static final int CANVAS_RELOAD_STROKES	= 411; // UUID COUNT SUUID1...
    public static final int CANVAS_RELOAD_GROUPS	= 412; // UUID COUNT GUUID1...
    public static final int CANVAS_RELOAD_ARROWS	= 413; // UUID COUNT AUUID1...
    public static final int CANVAS_CLEAR			= 414; // UUID
    public static final int CANVAS_CREATE			= 415; // UUID
    public static final int CANVAS_COPY				= 416; // UUID COPY_TO_UUID
    public static final int CANVAS_CLEAR_FOR_SC		= 417; // UUID (clear for state change)
    public static final int CANVAS_SC_FINISH		= 418; // UUID (state change has completed)
    public static final int CANVAS_LOCK				= 419;
    public static final int CANVAS_LOAD				= 420;
    public static final int CANVAS_DELETE 			= 421; // UUID
    public static final int CANVAS_SET_DIMENSIONS 	= 422;
    public static final int CANVAS_LOAD_PROGRESS = 423;
	
	// Consistency
	public static final int CONSISTENCY_CHECK		= 510; // Requests that the server send you a consistency check.
	public static final int CONSISTENCY_FINISH		= 511; // The consistency check is done, redraw the screen (S->C)
	public static final int CONSISTENCY_CHECK_CONTINUE = 513;
	public static final int CONSISTENCY_FAILED		= 514;
	public static final int CONSISTENCY_RESYNC_CANVAS  = 515;
	public static final int CONSISTENCY_DEBUG		= 516;
	public static final int CONSISTENCY_RESYNCED    = 517;
	
	public static final int GRID_SIZE				= 512; // <ROWS> <COLS> (this is server->client)

	// Server info
	public static final int CLIENT_LIST				= 600;
	public static final int CLIENT_INFO				= 601; // CLIENTID USERNAME


	// Chunking data
	public static final int CHUNK_START				= 700; // <CHUNK_UUID> <CHUNK_COUNT>
	public static final int CHUNK_PART				= 701; // <CHUNK_UUID> <CHUNK_PART_ID> <DATA>
	public static final int CHUNK_FINISH			= 702; // <CHUNK_UUID>
	public static final int CHUNK_DATA				= 703;

	// ARROWS
	public static final int ARROW_CREATE			= 800; // UUID CANVASUID ARROW_TYPE RED GREEN BLUE ANCHOR_A_TYPE ANCHOR_A_UUID ANCHOR_A_X ANCHOR_A_Y   ANCHOR_B_TYPE ANCHOR_B_UUID ANCHOR_B_X ANCHOR_B_Y
	public static final int ARROW_DELETE 			= 801;		// <UUID>
	public static final int ARROW_SET_TYPE			= 802;// UUID ARROW_TYPE
	public static final int ARROW_SET_COLOR			= 803;// UUID RED GREEN BLUE
	
	// CONNECTORS
	public static final int CONNECTOR_LOAD					= 810; 
	public static final int CONNECTOR_DELETE				= 811; 
	public static final int CONNECTOR_LINEARIZE				= 812; 
	public static final int CONNECTOR_MOVE_ANCHOR   		= 813;
	public static final int CONNECTOR_MOVE_ANCHOR_START   	= 814;
	public static final int CONNECTOR_MOVE_ANCHOR_END    	= 815;

	// Click Tracking
	public static final int CLICK_TRACK = 900; // CLICK ID
	
	// Compositional Notations
	public static final int ELEMENT_ADD = 950;
	public static final int ELEMENT_REMOVE = 951;


	// Calico Tree
	public static final int TREE_ADD	= 1000;//UUID PARENT
	public static final int TREE_MOVE	= 1001;//UUID NEWPARENT
	public static final int TREE_DELETE = 1002;//UUID DELCHILDREN=0/1
	public static final int TREE_GET	= 1003;//
	
	
	public static final int SESSION_LIST		= 1100;//
	public static final int SESSION_LIST_RESP	= 1101;//
	public static final int SESSION_LIST_END	= 1102;//
	public static final int SESSION_LISTING		= 1103;// NUM_SESSIONS (<SESSIONID> <NAME> <ROWS> <COLS>)*
	public static final int SESSION_INFO		= 1104; // ID, NAME, HOST, PORT

	// UUID 
	public static final int UUID_GET_BLOCK 	= 1200;//
	public static final int UUID_BLOCK 		= 1201;// <START> <END>
	
	public static final int AUTH_OK			= 1300;
	public static final int AUTH_FAIL		= 1301;
	
	public static final int ERROR_INVALID_SESSION = 1400;

	// GROUPS
	public static final int GROUP_START			= 1500;// UUID CANVASUID PARENT_UID PERM
	public static final int GROUP_APPEND		= 1501;// UUID X Y
	public static final int GROUP_MOVE			= 1502;// UUID Xdelta Ydelta
	public static final int GROUP_DELETE		= 1503;// UUID (deletes group+children)
	public static final int GROUP_DROP			= 1504;// UUID (drops the group, but keeps children)
	public static final int GROUP_FINISH		= 1505;// UUID
	public static final int GROUP_SET_PARENT	= 1511;// UUID PARENT_UUID (or 0 if false)
	public static final int GROUP_SET_CHILDREN	= 1512;// UUID BGE_CHILD_COUNT GROUP_CHILD_COUNT BGE_UUID1..BGE_UUIDN GRP_UUID1..GRP_UUIDN (or 0 if false)
	public static final int GROUP_MOVE_START	= 1513;// UUID this is used to signal a move has completed,( used for undo/redo)
	public static final int GROUP_MOVE_END		= 1514;// UUID <TOTALX> <TOTALY>
	public static final int GROUP_SET_PERM		= 1515;// UUID <PERM=1>
	public static final int GROUP_RECTIFY		= 1516;// UUID
	public static final int GROUP_CIRCLIFY		= 1517;// UUID
	public static final int GROUP_CHILDREN_COLOR= 1518;// UUID RED GREEN BLUE
	
	public static final int GROUP_RELOAD_START	= 1519;// UUID CANVASUID PARENTUUID PERM
	public static final int GROUP_RELOAD_FINISH	= 1520;// UUID (After this, the client should reload/repaint the group)
	public static final int GROUP_RELOAD_COORDS	= 1521;// UUID COORD_COUNT X1 Y1 ... Xn Yn  (Sent multiple times)
	public static final int GROUP_RELOAD_CHILDREN = 1522; // UUID <CHILDCT> <C_UUID1> .. <C_UUIDn>
	public static final int GROUP_RELOAD_POSITION = 1523; // UUID X Y
	public static final int GROUP_RELOAD_REMOVE = 1524; // UUID
	public static final int GROUP_DUPLICATE = 1525;//UUID
	public static final int GROUP_APPEND_CLUSTER = 1526; /// UUID <COUNT> <X> <Y>
	public static final int GROUP_SET_CHILD_GROUPS = 1527; // UUID NUM <child_uuid> ... <child_uuid>
	public static final int GROUP_SET_CHILD_STROKES = 1528; // UUID NUM <child_uuid> ... <child_uuid>
	public static final int GROUP_LOAD				= 1529;// UUID CUID PUID PERM NUMCOORDS X Y ... 
	public static final int GROUP_SET_CHILD_ARROWS = 1530; // UUID NUM <child_uuid> ... <child_uuid>
	public static final int GROUP_HASH_CHECK		= 1531; // UUID <SIZE> <HASH_BYTES>
	public static final int GROUP_REQUEST_HASH_CHECK		= 1532; // UUID
	public static final int GROUP_COPY_TO_CANVAS	= 1533; // UUID <TO_CANVAS_UUID> <NEW_UUID=0L> SHIFT_X SHIFT_Y (if new_uuid==0L, then we just generate a uuid for it)
	public static final int GROUP_SET_TEXT = 1534;
	public static final int GROUP_SHRINK_TO_CONTENTS = 1535;
	public static final int GROUP_IMAGE_DOWNLOAD = 1536;	
	public static final int GROUP_IMAGE_LOAD = 1537;
	public static final int GROUP_ROTATE = 1538;
	public static final int GROUP_SCALE = 1539;
	public static final int GROUP_CREATE_TEXT_GROUP = 1540;
	public static final int GROUP_MAKE_RECTANGLE = 1541;
	public static final int GROUP_COPY_WITH_MAPPINGS = 1542;


	public static final int BACKUP_FILE_INFO	= 1600; // 
	public static final int BACKUP_FILE_START	= 1601;
	public static final int BACKUP_FILE_END		= 1602;
	public static final int BACKUP_FILE_ATTR	= 1603; // KEY, VALUE (strings?)
	

	public static final int DEBUG_UNITTEST_START = 1700;
	public static final int DEBUG_UNITTEST_END = 1701;
	public static final int DEBUG_PACKETSIZE = 1702; // used for testing largest packet (SIZE LONG1 ... LONGN) [this is meant to test the limits of the client/server memory]
	public static final int DEBUG_SEND_PACKETSIZE = 1703;// initiates the packetsize 
	
	public static final int UDP_CHALLENGE = 1800; // LONG - crc32
	
	public static final int PLUGIN_EVENT = 1900;// <EventName> <data>
	


	public static final int LIST_CREATE = 2000;
	public static final int LIST_LOAD = 2001;
	public static final int LIST_CHECK_SET = 2002;
	
	public static final int CANVASVIEW_SCRAP_LOAD = 2003;
	
	public static final int IMAGE_TRANSFER = 2100;
	public static final int IMAGE_CLUSTER = 2101;
	public static final int IMAGE_TRANSFER_FILE = 2102;
	
	 
	

//	public static final int PALETTE_PACKET = 2200;
//	public static final int PALETTE_SUB_GROUP = 2201;
//	public static final int PALETTE_PASTE = 2202;
//	public static final int PALETTE_PASTE_ITEM = 2203;
//	public static final int PALETTE_SWITCH_VISIBLE_PALETTE = 2204;
//	public static final int PALETTE_HIDE_MENU_BAR_ICONS = 2205;
//	public static final int PALETTE_SHOW_MENU_BAR_ICONS = 2206;
//	public static final int PALETTE_ITEM_LOAD = 2207;
//	public static final int PALETTE_LOAD = 2208;
//	public static final int PALETTE_DELETE= 2209;
	

	// presence  
	public static final int PRESENCE_VIEW_CANVAS = 3001; // CUID
	public static final int PRESENCE_LEAVE_CANVAS = 3002; // CUID
	public static final int PRESENCE_CANVAS_RESET = 3003; // cuid
	public static final int PRESENCE_CANVAS_USERS = 3004;// CUID, NUMUSERS, USERID... USERIDn
	
	public static final int HISTORY_ACTION = 4000;
	
	public static final int VIEWING_SINGLE_CANVAS = 5000;
	
	
	public static NetCommandFormat getFormat(int type)
	{
		if(formats.size()==0)
		{
			
			try
			{
				Field[] fields = NetworkCommand.class.getDeclaredFields();
				if(fields.length>0)
				{
					for(int i=0;i<fields.length;i++)
					{
						if(!fields[i].getName().equals("formats"))
						{
							int intval = ((Integer)fields[i].get(null)).intValue();
							//System.out.println("NETWORK: "+fields[i].getName()+"="+intval);
							formats.put(intval, new NetCommandFormat(fields[i].getName(),""));
						}
						//
					}
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			
			formats.put(JOIN,new NetCommandFormat("JOIN","SS"));
			formats.put(DEFAULT_EMAIL,new NetCommandFormat("DEFAULT_EMAIL","S"));
			formats.put(SERVER_HTTP_PORT,new NetCommandFormat("SERVER_HTTP_PORT","S"));
			formats.put(SERVER_EMAIL_SETTINGS, new NetCommandFormat("SERVER_EMAIL_SETTINGS", "SISSSS"));
			formats.put(HEARTBEAT,new NetCommandFormat("HEARTBEAT","LI"));
			
			formats.put(GROUP_START,new NetCommandFormat("GROUP_START","LLLI"));
			formats.put(GROUP_APPEND,new NetCommandFormat("GROUP_APPEND","Lii"));
			formats.put(GROUP_MOVE,new NetCommandFormat("GROUP_MOVE","LII"));
			formats.put(GROUP_DELETE,new NetCommandFormat("GROUP_DELETE","L"));
			formats.put(GROUP_DROP,new NetCommandFormat("GROUP_DROP","L"));
			formats.put(GROUP_FINISH,new NetCommandFormat("GROUP_FINISH","LB"));
			formats.put(GROUP_SET_CHILDREN,new NetCommandFormat("GROUP_SET_CHILDREN","LII"));
			formats.put(GROUP_SET_PARENT,new NetCommandFormat("GROUP_SET_PARENT","LL"));
			formats.put(GROUP_MOVE_START,new NetCommandFormat("GROUP_MOVE_START","L"));
			formats.put(GROUP_MOVE_END,new NetCommandFormat("GROUP_MOVE_END","LII"));
			formats.put(GROUP_SET_PERM,new NetCommandFormat("GROUP_SET_PERM","LI"));
			formats.put(GROUP_RECTIFY,new NetCommandFormat("GROUP_RECTIFY","L"));
			formats.put(GROUP_CIRCLIFY,new NetCommandFormat("GROUP_CIRCLIFY","L"));
			formats.put(GROUP_CHILDREN_COLOR,new NetCommandFormat("GROUP_CHILDREN_COLOR","LIII"));
			

			formats.put(GROUP_RELOAD_START,new NetCommandFormat("GROUP_RELOAD_START","LLLI"));
			formats.put(GROUP_RELOAD_FINISH,new NetCommandFormat("GROUP_RELOAD_FINISH","L"));
			formats.put(GROUP_RELOAD_COORDS,new NetCommandFormat("GROUP_RELOAD_COORDS","LIII"));
			formats.put(GROUP_RELOAD_CHILDREN,new NetCommandFormat("GROUP_RELOAD_CHILDREN","LIL"));
			formats.put(GROUP_RELOAD_POSITION,new NetCommandFormat("GROUP_RELOAD_POSITION","LII"));
			formats.put(GROUP_RELOAD_REMOVE,new NetCommandFormat("GROUP_RELOAD_REMOVE","L"));
			formats.put(GROUP_DUPLICATE,new NetCommandFormat("GROUP_DUPLICATE","L"));
			formats.put(GROUP_APPEND_CLUSTER,new NetCommandFormat("GROUP_APPEND_CLUSTER","LiII"));
			formats.put(GROUP_SET_CHILD_GROUPS,new NetCommandFormat("GROUP_SET_CHILD_GROUPS","Li"));
			formats.put(GROUP_SET_CHILD_STROKES,new NetCommandFormat("GROUP_SET_CHILD_STROKES","Li"));
			formats.put(GROUP_SET_CHILD_ARROWS,new NetCommandFormat("GROUP_SET_CHILD_ARROWS","Li"));
			formats.put(GROUP_REQUEST_HASH_CHECK,new NetCommandFormat("GROUP_REQUEST_HASH_CHECK","L"));
			formats.put(GROUP_LOAD,new NetCommandFormat("GROUP_LOAD","LLLBiIIBddd"));
			formats.put(GROUP_HASH_CHECK,new NetCommandFormat("GROUP_HASH_CHECK","Li"));
			formats.put(GROUP_COPY_TO_CANVAS,new NetCommandFormat("GROUP_COPY_TO_CANVAS","LLLII"));
			formats.put(GROUP_SET_TEXT, new NetCommandFormat("GROUP_SET_TEXT", "LS"));
			formats.put(GROUP_SHRINK_TO_CONTENTS, new NetCommandFormat("GROUP_SHRINK_TO_CONTENTS", "L"));
			formats.put(GROUP_IMAGE_DOWNLOAD, new NetCommandFormat("GROUP_IMAGE_DOWNLOAD", "LLSII"));
			formats.put(GROUP_IMAGE_LOAD, new NetCommandFormat("GROUP_IMAGE_LOAD", "LLLSIIIIBiII"));
			formats.put(GROUP_ROTATE, new NetCommandFormat("GROUP_ROTATE", "Ld"));
			formats.put(GROUP_SCALE, new NetCommandFormat("GROUP_SCALE", "Ldd"));
			formats.put(GROUP_CREATE_TEXT_GROUP, new NetCommandFormat("GROUP_CREATE_TEXT_GROUP", "LLSII"));
			formats.put(GROUP_MAKE_RECTANGLE, new NetCommandFormat("GROUP_MAKE_RECTANGLE", "LIIII"));

			formats.put(GRID_SIZE,new NetCommandFormat("GRID_SIZE","II"));
			
			//formats.put(UUID_GET_BLOCK,new NetCommandFormat("UUID_GET_BLOCK",""));
			formats.put(UUID_BLOCK,new NetCommandFormat("UUID_BLOCK","ILL"));
			
			//formats.put(CANVAS_LIST,new NetCommandFormat("CANVAS_LIST",""));
			formats.put(CANVAS_INFO,new NetCommandFormat("CANVAS_INFO","LSII"));
			formats.put(CANVAS_UPDATE,new NetCommandFormat("CANVAS_UPDATE","L"));
			formats.put(CANVAS_UNDO,new NetCommandFormat("CANVAS_UNDO","L"));
			formats.put(CANVAS_REDO,new NetCommandFormat("CANVAS_REDO","L"));
			formats.put(CANVAS_RELOAD_START,new NetCommandFormat("CANVAS_RELOAD_START","L"));
			formats.put(CANVAS_RELOAD_FINISH,new NetCommandFormat("CANVAS_RELOAD_FINISH","L"));
			formats.put(CANVAS_RELOAD_STROKES,new NetCommandFormat("CANVAS_RELOAD_STROKES","LIL"));
			formats.put(CANVAS_RELOAD_GROUPS,new NetCommandFormat("CANVAS_RELOAD_GROUPS","LIL"));
			formats.put(CANVAS_RELOAD_ARROWS,new NetCommandFormat("CANVAS_RELOAD_ARROWS","LIL"));
			formats.put(CANVAS_CLEAR_FOR_SC, new NetCommandFormat("CANVAS_CLEAR_FOR_SC","L"));
			formats.put(CANVAS_SC_FINISH, new NetCommandFormat("CANVAS_SC_FINISH", "L"));
			formats.put(CANVAS_LOCK, new NetCommandFormat("CANVAS_LOCK", "LBSL"));
			formats.put(CANVAS_SET_DIMENSIONS, new NetCommandFormat("CANVAS_SET_DIMENSIONS", "II"));			
			formats.put(CANVAS_LOAD_PROGRESS, new NetCommandFormat("CANVAS_LOAD_PROGRESS", "II"));

			formats.put(STATUS_MESSAGE,new NetCommandFormat("STATUS_MESSAGE","S"));
			formats.put(ERROR_MESSAGE,new NetCommandFormat("ERROR_MESSAGE","S"));
			formats.put(ERROR_POPUP,new NetCommandFormat("ERROR_POPUP","S"));

			formats.put(CLICK_TRACK,new NetCommandFormat("CLICK_TRACK","I"));
			

			formats.put(BGE_APPEND,new NetCommandFormat("BGE_APPEND","LII"));
			formats.put(BGE_COLOR,new NetCommandFormat("BGE_COLOR","LIII"));
			formats.put(BGE_COORDS,new NetCommandFormat("BGE_COORDS","LIII"));
			formats.put(BGE_DELETE,new NetCommandFormat("BGE_DELETE","L"));
			formats.put(BGE_FINISH,new NetCommandFormat("BGE_FINISH","L"));
			formats.put(BGE_MOVE,new NetCommandFormat("BGE_MOVE","LII"));
			formats.put(BGE_START,new NetCommandFormat("BGE_START","LLL"));
			formats.put(BGE_PARENT,new NetCommandFormat("BGE_PARENT","LL"));
			formats.put(BGE_CONSISTENCY,new NetCommandFormat("BGE_CONSISTENCY","L"));
			formats.put(BGE_RELOAD_START,new NetCommandFormat("BGE_RELOAD_START","LLLIII"));
			formats.put(BGE_RELOAD_COORDS,new NetCommandFormat("BGE_RELOAD_COORDS","LIII"));
			formats.put(BGE_RELOAD_FINISH,new NetCommandFormat("BGE_RELOAD_FINISH","L"));


			formats.put(STROKE_RELOAD_START,new NetCommandFormat("STROKE_RELOAD_START","LLLIII"));
			formats.put(STROKE_RELOAD_COORDS,new NetCommandFormat("STROKE_RELOAD_COORDS","LIII"));
			formats.put(STROKE_RELOAD_FINISH,new NetCommandFormat("STROKE_RELOAD_FINISH","L"));
			formats.put(STROKE_RELOAD_REMOVE,new NetCommandFormat("STROKE_RELOAD_REMOVE","L"));
			formats.put(STROKE_RELOAD_POSITION,new NetCommandFormat("STROKE_RELOAD_POSITION","LII"));

			formats.put(STROKE_START,new NetCommandFormat("STROKE_START","LLLIII"));
			formats.put(STROKE_APPEND,new NetCommandFormat("STROKE_APPEND","LiII"));
			formats.put(STROKE_FINISH,new NetCommandFormat("STROKE_FINISH","L"));
			formats.put(STROKE_SET_COLOR,new NetCommandFormat("STROKE_SET_COLOR","LIII"));
			formats.put(STROKE_SET_PARENT,new NetCommandFormat("STROKE_SET_PARENT","LL"));
			formats.put(STROKE_MOVE,new NetCommandFormat("STROKE_MOVE","LII"));
			formats.put(STROKE_DELETE,new NetCommandFormat("STROKE_DELETE","L"));
			formats.put(STROKE_LOAD,new NetCommandFormat("STROKE_LOAD","LLLCidddII"));
			formats.put(STROKE_HASH_CHECK,new NetCommandFormat("STROKE_HASH_CHECK","L"));
			formats.put(STROKE_MAKE_SCRAP, new NetCommandFormat("STROKE_MAKE_SCRAP", "LL"));
			formats.put(STROKE_MAKE_SHRUNK_SCRAP, new NetCommandFormat("STROKE_MAKE_SHRUNK_SCRAP", "LL"));
			formats.put(STROKE_DELETE_AREA, new NetCommandFormat("STROKE_DELETE_AREA", "LL"));
			formats.put(STROKE_ROTATE, new NetCommandFormat("STROKE_ROTATE", "Ld"));
			formats.put(STROKE_SCALE, new NetCommandFormat("STROKE_SCALE", "Ldd"));
			formats.put(STROKE_SET_AS_POINTER, new NetCommandFormat("STROKE_SET_AS_POINTER", "L"));
			formats.put(STROKE_HIDE, new NetCommandFormat("STROKE_HIDE", "LB"));
			formats.put(STROKE_UNHIDE, new NetCommandFormat("STROKE_UNHIDE", "L"));
			
			formats.put(ERASE_START, new NetCommandFormat("ERASE_START", "L"));
			formats.put(ERASE_END, new NetCommandFormat("ERASE_END", "LB"));

			formats.put(PLUGIN_EVENT,new NetCommandFormat("PLUGIN_EVENT","S"));
			
			
			
			//formats.put(AUTH_OK,new NetCommandFormat("AUTH_OK",""));
			
			formats.put(CONSISTENCY_CHECK,new NetCommandFormat("CONSISTENCY_CHECK",""));
			formats.put(CONSISTENCY_FINISH,new NetCommandFormat("CONSISTENCY_FINISH",""));
			formats.put(CONSISTENCY_CHECK_CONTINUE,new NetCommandFormat("CONSISTENCY_CHECK_CONTINUE","L"));
			formats.put(CONSISTENCY_FAILED,new NetCommandFormat("CONSISTENCY_FAILED",""));
			formats.put(CONSISTENCY_RESYNC_CANVAS,new NetCommandFormat("CONSISTENCY_RESYNC_CANVAS","L"));
			
			
			formats.put(ARROW_CREATE,new NetCommandFormat("ARROW_CREATE","LLICILIIILII"));
			formats.put(ARROW_DELETE,new NetCommandFormat("ARROW_DELETE","L"));
			formats.put(ARROW_SET_TYPE,new NetCommandFormat("ARROW_SET_TYPE","LI"));
			formats.put(ARROW_SET_COLOR,new NetCommandFormat("ARROW_SET_COLOR","LIII"));
			

			formats.put(BACKUP_FILE_INFO,new NetCommandFormat("BACKUP_FILE_INFO","L"));
			formats.put(BACKUP_FILE_START,new NetCommandFormat("BACKUP_FILE_START",""));
			formats.put(BACKUP_FILE_END,new NetCommandFormat("BACKUP_FILE_END",""));
			formats.put(BACKUP_FILE_ATTR,new NetCommandFormat("BACKUP_FILE_ATTR","SS"));
			
			//formats.put(STATUS_MESSAGE,new NetCommandFormat("STATUS_MESSAGE","s"));
			//formats.put(STATUS_MESSAGE,new NetCommandFormat("STATUS_MESSAGE","s"));
			//formats.put(STATUS_MESSAGE,new NetCommandFormat("STATUS_MESSAGE","s"));
			formats.put(LIST_CREATE,new NetCommandFormat("LIST_CREATE","LLLLI"));
			formats.put(LIST_LOAD,new NetCommandFormat("LIST_LOAD","LLLBiII"));
			formats.put(LIST_CHECK_SET, new NetCommandFormat("LIST_CHECK_SET", "LLLLB"));
			formats.put(CANVASVIEW_SCRAP_LOAD, new NetCommandFormat("CANVASVIEW_SCRAP_LOAD", "LLLBiIIBddd"));
			
		}
		
		if(type>0 && formats.containsKey(type))
		{
			return formats.get(type);	
		}
		else
		{
			return new NetCommandFormat("UNKNOWN_"+type,"");
		}
		
	}
	

}
