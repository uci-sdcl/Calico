package calico.plugins.examplePlugin.controllers;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;

import calico.CalicoDraw;
import calico.components.CGroup;
import calico.controllers.CCanvasController;
import calico.controllers.CGroupController;
import calico.plugins.examplePlugin.components.CustomScrap;
import calico.utils.Geometry;

public class CustomScrapController {

	
	/*************************************************
	 * BEHAVIORS OF THE CONTROLLER
	 * 
	 * The summary of the actions that can be
	 * invoked by the view elements
	 *************************************************/		
	public static void create_custom_scrap(long new_uuid, long cuuid, int x, int y){
		no_notify_create_custom_scrap(new_uuid, cuuid, x, y, "My custom scrap");
	}	
	
	/*************************************************
	 * NO NOTIFY METHODS
	 * 
	 * Methods which use the CGroupController
	 * to create the actual nodes
	 *************************************************/		
	
	public static void no_notify_create_custom_scrap(long uuid, long cuuid, int x, int y, String optText){

		//create shape
		GeneralPath myPolygon = new GeneralPath(new Rectangle(x,y, 120, 60));
		Polygon p = Geometry.getPolyFromPath(myPolygon.getPathIterator(null));	

		//initialize custom scrap
		CGroup group = new CustomScrap(uuid, cuuid, 0);

		//create the scrap
		no_notify_create_custom_scrap_bootstrap(uuid, cuuid, group, p, optText);
	}
	

	
	/*************************************************
	 * UTILITY METHODS
	 *************************************************/		
	public static void no_notify_create_custom_scrap_bootstrap(long uuid, long cuuid, CGroup group, Polygon p, String optText){
		no_notify_start(uuid, cuuid, 0l, true, group);
		CGroupController.setCurrentUUID(uuid);
		create_custom_shape(uuid, p);
		//Set the optional text to identify the scrap
		CGroupController.no_notify_set_text(uuid, optText);
		CGroupController.no_notify_finish(uuid, false, false, true);
		CGroupController.no_notify_set_permanent(uuid, true);
		CGroupController.recheck_parent(uuid);
	}	
	
	//Starts the creation of any of the activity diagram scrap
	public static void no_notify_start(long uuid, long cuid, long puid, boolean isperm, CGroup customScrap)
	{
		if (!CCanvasController.exists(cuid))
			return;
		if(CGroupController.exists(uuid))
		{
			CGroupController.logger.debug("Need to delete group "+uuid);
			//CCanvasController.canvasdb.get(cuid).getLayer().removeChild(groupdb.get(uuid));
			CalicoDraw.removeChildFromNode(CCanvasController.canvasdb.get(cuid).getLayer(), CGroupController.groupdb.get(uuid));
			//CCanvasController.canvasdb.get(cuid).getCamera().repaint();
		}
		
		// Add to the GroupDB
		try {
			CGroupController.groupdb.put(uuid, customScrap);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		CCanvasController.canvasdb.get(cuid).addChildGroup(uuid);
		//CCanvasController.canvasdb.get(cuid).getLayer().addChild(groupdb.get(uuid));
		CalicoDraw.addChildToNode(CCanvasController.canvasdb.get(cuid).getLayer(), CGroupController.groupdb.get(uuid));
		CGroupController.groupdb.get(uuid).drawPermTemp(true);
		//CCanvasController.canvasdb.get(cuid).repaint();
	}	
	
	//Add the points defined in p to the scrap with id uuid
	public static void create_custom_shape(long uuid, Polygon p){
		for (int i = 0; i < p.npoints; i++)
		{
			CGroupController.no_notify_append(uuid, p.xpoints[i], p.ypoints[i]);
			CGroupController.no_notify_append(uuid, p.xpoints[i], p.ypoints[i]);
			CGroupController.no_notify_append(uuid, p.xpoints[i], p.ypoints[i]);
		}
	}
	
}
