package calico.plugins.palette;

import calico.clients.Client;
import calico.clients.ClientManager;
import calico.events.CalicoEventHandler;
import calico.events.CalicoEventListener;
import calico.networking.netstuff.CalicoPacket;
import calico.plugins.AbstractCalicoPlugin;

public class UserListPlugin extends AbstractCalicoPlugin
	implements CalicoEventListener
{
	
	public UserListPlugin()
	{
		super();
		PluginInfo.name = "UserList";
	}
	
	public void onPluginStart()
	{
		for (Integer event : this.getNetworkCommands())
		{
			CalicoEventHandler.getInstance().addListener(event.intValue(), this, CalicoEventHandler.ACTION_PERFORMER_LISTENER);
		}
	}
	
	@Override
	public void handleCalicoEvent(int event, CalicoPacket p, Client client)
	{
		switch (event)
		{
			case UserListNetworkCommands.AUDIO_START:
				if (client != null)
					ClientManager.send_except(client, p);
				break;
			case UserListNetworkCommands.AUDIO_END:
				if (client != null)
					ClientManager.send_except(client, p);
				break;
			case UserListNetworkCommands.PEN_START:
				if (client != null)
					ClientManager.send_except(client, p);
				break;
			case UserListNetworkCommands.PEN_END:
				if (client != null)
					ClientManager.send_except(client, p);
				break;
		}
	}
	
	public Class<?> getNetworkCommandsClass()
	{
		return UserListNetworkCommands.class;
	}
}
