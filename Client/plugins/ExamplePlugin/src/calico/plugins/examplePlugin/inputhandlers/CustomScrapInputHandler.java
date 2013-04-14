package calico.plugins.examplePlugin.inputhandlers;

import calico.inputhandlers.CGroupInputHandler;
import calico.inputhandlers.CalicoAbstractInputHandler;
import calico.inputhandlers.InputEventInfo;

public class CustomScrapInputHandler extends CGroupInputHandler  {

	public CustomScrapInputHandler(long u) {
		super(u);
	}
	
//	@Override
	public void actionStrokeToAnotherGroup(long strokeUID, long targetGroup) {
//		 if(CGroupController.groupdb.get(targetGroup) instanceof AnalysisComponent && CGroupController.groupdb.get(this.uuid) instanceof AnalysisComponent){
//             CConnectorController.connectors.put(uuid, new ControlFlow(uuid, CCanvasController.getCurrentUUID(), CalicoDataStore.PenColor, CalicoDataStore.PenThickness, CStrokeController.strokes.get(strokeUID).getRawPolygon()));
//     		CStrokeController.delete(strokeUID);
//         }
//         else{
////        	 CConnectorController.connectors.put(uuid, new CConnector(uuid, CCanvasController.getCurrentUUID(), CalicoDataStore.PenColor, CalicoDataStore.PenThickness, CStrokeController.strokes.get(strokeUID).getRawPolygon()));
//        	 CStrokeController.show_stroke_bubblemenu(strokeUID, false);
//         }
////		CConnectorController.no_notify_create(Calico.uuid(), CCanvasController.getCurrentUUID(), 0l, CalicoDataStore.PenColor, CalicoDataStore.PenThickness, this.uuid, targetGroup, strokeUID);

	}

}
