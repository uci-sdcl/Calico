package calico.plugins.examplePlugin.components.buttons;

import javax.swing.SwingUtilities;

import calico.Calico;
import calico.CalicoDataStore;
import calico.CalicoDraw;
import calico.components.menus.CanvasMenuButton;
import calico.controllers.CCanvasController;
import calico.inputhandlers.InputEventInfo;
import calico.plugins.examplePlugin.ExamplePlugin;
import calico.plugins.examplePlugin.ExamplePluginNetworkCommands;
import calico.plugins.examplePlugin.iconsets.CalicoIconManager;

public class CreateCustomScrapButton extends CanvasMenuButton
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public CreateCustomScrapButton(long c)
	{
		super();
		cuid = c;
		
		iconString = "plugins.exampleplugin.customScrap";
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
			//send command to UI
			long new_uuid = Calico.uuid();
			long cuuid=CCanvasController.getCurrentUUID();
			int x= new Double(Math.random() * 100).intValue() - 50 + CalicoDataStore.ScreenWidth / 3;
			int y= new Double(Math.random() * 100).intValue() - 50 + CalicoDataStore.ScreenHeight / 3;
			ExamplePlugin.UI_send_command(ExamplePluginNetworkCommands.CUSTOM_SCRAP_CREATE,new_uuid, cuuid, x, y);
			
			super.onMouseUp();
		}
	}
	
	/**
	 * Part of scaffolding
	 */
	public void highlight_on()
	{
		if (!isPressed)
		{
			isPressed = true;
			setSelected(true);
			final CanvasMenuButton tempButton = this;
			SwingUtilities.invokeLater(
					new Runnable() { public void run() { 
						double tempX = tempButton.getX();
						double tempY = tempButton.getY();
								
						setImage(CalicoIconManager.getIconImage(iconString));
						tempButton.setX(tempX);
						tempButton.setY(tempY);
					}});
			//CalicoDraw.setNodeX(this, tempX);
			//CalicoDraw.setNodeY(this, tempY);
			//this.repaintFrom(this.getBounds(), this);
			CalicoDraw.repaintNode(this);
		}
	}
	
	/**
	 * Part of scaffolding
	 */
	public void highlight_off()
	{
		if (isPressed)
		{
			isPressed = false;
			setSelected(false);
			final CanvasMenuButton tempButton = this;
			SwingUtilities.invokeLater(
					new Runnable() { public void run() { 
						double tempX = tempButton.getX();
						double tempY = tempButton.getY();
								
						setImage(CalicoIconManager.getIconImage(iconString));
						tempButton.setX(tempX);
						tempButton.setY(tempY);
					}});
			//CalicoDraw.setNodeX(this, tempX);
			//CalicoDraw.setNodeY(this, tempY);
			//this.repaintFrom(this.getBounds(), this);
			CalicoDraw.repaintNode(this);
		}
	}		
}
