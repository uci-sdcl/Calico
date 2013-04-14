package calico.plugins.userlist;

import calico.components.piemenu.PieMenuButton;
import calico.inputhandlers.InputEventInfo;
import calico.plugins.userlist.iconsets.*;

public class MuteAudio extends PieMenuButton //ImageCreate
{
	public static int SHOWON = PieMenuButton.SHOWON_SCRAP_MENU;
	private static long uuid = 0L;
	private boolean capturing = false;

	public MuteAudio()
	{
		// icon will not show if set here
		super("");
		capturing = AudioListener.capturing();
		
		if (!capturing)
			setIcon("plugins.userlist.mic");
		else
			setIcon("plugins.userlist.mute");
	}
	
	public MuteAudio(long u)
	{
		// icon will not show if set here
		super("");
		uuid = u;
		capturing = AudioListener.capturing();
		
		if (!capturing)
			setIcon("plugins.userlist.mic");
		else
			setIcon("plugins.userlist.mute");
	}
	
	private void setIcon(String str)
	{
		iconPath = str;
		try
		{
			iconImage = CalicoIconManager.getIconImage(str);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void onClick(InputEventInfo ev)
	{
		super.onClick(ev);
		
		if(capturing)
			UserListPlugin.stopCapture();
		else
			UserListPlugin.startCapture();
	}
}
