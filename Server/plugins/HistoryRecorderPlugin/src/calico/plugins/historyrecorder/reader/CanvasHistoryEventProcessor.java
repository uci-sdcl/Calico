package calico.plugins.historyrecorder.reader;

import calico.networking.netstuff.CalicoPacket;

public interface CanvasHistoryEventProcessor {

	public void processCanvasState(CalicoPacket p, long time, String clientName, long cuid);
	
}
