package calico.plugins.userlist;

import calico.components.menus.CanvasMenuButton;
import calico.inputhandlers.InputEventInfo;
import calico.plugins.userlist.iconsets.CalicoIconManager;

public class UserListButton extends CanvasMenuButton
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public UserListButton(long c)
	{
		super();
		cuid = c;
		
		iconString = "plugins.userlist.menu";
		try
		{
//			Image img = CanvasGenericMenuBar.getTextImage("  UserList  ", 
//				new Font("Verdana", Font.BOLD, 12));
			setImage(CalicoIconManager.getIconImage(iconString));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void actionMouseClicked(InputEventInfo event)
	{	
		if (event.getAction() == InputEventInfo.ACTION_PRESSED)
		{
			super.onMouseDown();
		}
		else if (event.getAction() == InputEventInfo.ACTION_RELEASED && isPressed)
		{
			UserListPlugin.toggleUserList();
			super.onMouseUp();
		}
	}
}
