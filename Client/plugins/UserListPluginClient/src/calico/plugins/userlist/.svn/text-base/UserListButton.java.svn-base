package calico.plugins.userlist;

import calico.components.menus.CanvasMenuButton;
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
		
		try
		{
//			Image img = CanvasGenericMenuBar.getTextImage("  UserList  ", 
//				new Font("Verdana", Font.BOLD, 12));
			setImage(CalicoIconManager.getIconImage("plugins.userlist.menu"));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void actionMouseClicked()
	{	
		UserListPlugin.toggleUserList();
	}
}
