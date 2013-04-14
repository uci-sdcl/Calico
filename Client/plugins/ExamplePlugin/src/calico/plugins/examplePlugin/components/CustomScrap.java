package calico.plugins.examplePlugin.components;

import calico.components.CGroup;
import calico.inputhandlers.CalicoInputManager;
import calico.networking.netstuff.CalicoPacket;
import calico.plugins.examplePlugin.ExamplePluginNetworkCommands;
import calico.plugins.examplePlugin.inputhandlers.CustomScrapInputHandler;

public class CustomScrap extends CGroup {

	public CustomScrap(long uuid, long cuid, long puid) {
		super(uuid, cuid, puid);
		
		//override load signature of scrap
		networkLoadCommand = calico.plugins.examplePlugin.ExamplePluginNetworkCommands.CUSTOM_SCRAP_LOAD;
	}
	
	@Override
	public void setInputHandler()
	{	
		CalicoInputManager.addCustomInputHandler(this.uuid, new CustomScrapInputHandler(this.uuid));	
	}	
	
	/**
	 * Serialize this activity node in a packet
	 */
	@Override
	public CalicoPacket[] getUpdatePackets(long uuid, long cuid, long puid,
			int dx, int dy, boolean captureChildren) {
		
		//Creates the packet for saving this CGroup
		CalicoPacket packet = super.getUpdatePackets(uuid, cuid, puid, dx, dy,
				captureChildren)[0];

		//Appends my own informations to the end of the packet
		//Example: packet.putDouble(responseTime);

		return new CalicoPacket[] { packet };

	}
	
	@Override
	public int get_signature() {
		//Return 0 since this is client side only
		return 0;
	}
	
	
}
