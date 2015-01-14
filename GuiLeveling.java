package SolarPrizm.mcStats;

import java.awt.Color;
import java.awt.MouseInfo;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.plaf.basic.BasicTabbedPaneUI.MouseHandler;

import net.minecraft.src.*;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class GuiLeveling extends GuiScreen
{
	public GuiLeveling()
	{
	}
	
	public int scrollLoc = 0;
	public java.util.Random rand = new java.util.Random();
	public boolean mainStatScreen = true;
	public boolean infoScreen = false;
	public boolean difficultyScreen = false;
	public int currentHelpIndex = 0;
	public int prevScrollLoc = scrollLoc;
	public int mentionScroll = 10;
	public int screenIsUpCounter = 0;
	public int smallButton = 40;
	public double statBoxHorizMargin = 1/8.0, statBoxVertMargin = 1/8.0;
	public int descriptionLength;
	public static boolean prevMentionedScroll = false; 
	private static boolean savedOnOpen = false;
	
	public void initGui()
	{
		controlList.clear();
		descriptionLength = 30;
		int count = 0;
		for(byte i = 0; i < Stat.getLength(); i++)
		{
			if((count-scrollLoc) * 20.0 < (1-statBoxHorizMargin) * height - 40 && ((count-scrollLoc + 4) * 20.0 > statBoxHorizMargin * height))
				controlList.add(new GuiButton(count, (int)(width * statBoxVertMargin) - smallButton - 3, (int)(height*statBoxHorizMargin) + count*20, smallButton, 20, "Info"));
			count++;
		}
		controlList.add(new GuiButton(Stat.getLength(), (int)(width * (1-statBoxVertMargin)) + 3, (int)(height/4.0), smallButton, 20, "Up"));
		controlList.add(new GuiButton(Stat.getLength()+1, (int)(width * (1-statBoxVertMargin)) + 3, (int)(height*3/4.0), smallButton, 20, "Down"));
		controlList.add(new GuiButton(Stat.getLength()+2, width/2 - smallButton/2, (int)(height*(1-statBoxHorizMargin) + 5), smallButton, 20, "Done"));
		
		controlList.add(new GuiButton(Stat.getLength()+3, width/4 - (smallButton+10)/2, (int)(height*(1-statBoxHorizMargin) + 5), smallButton+10, 20, "Difficulty"));
	}
	
	public void drawScreen(int par1, int par2, float par3)
	{
		descriptionLength = (int)(width * statBoxVertMargin);	
		drawDefaultBackground();
		drawGradientRect((int)(width*statBoxVertMargin), (int)(height*statBoxHorizMargin), (int)(width*(1-statBoxVertMargin)), (int)(height*(1-statBoxHorizMargin)), Color.darkGray.getRGB(), Color.black.getRGB());
		drawCenteredString(fontRenderer, "mcStats", width / 2, height/16, Color.RED.getRGB());//0xffffff);
		if(screenIsUpCounter < 50)
			drawString(fontRenderer, "(Game Paused)", width / 16, height / 16, Color.GREEN.getRGB());
		if(TimedEvent.active("scroll"))
			drawString(fontRenderer, "Use your scroll wheel to scroll", width / 2 + smallButton, height * 15 / 16, Color.PINK.getRGB());
//		else
//			TimedEvent.getCompleted("scroll");
		
		if(mainStatScreen)
		{
			//stats
			int countnum = 0;
			boolean flag;
			byte visibleCounter = 0;
			for(byte i = 0; i < Stat.getLength() - visibleCounter; i++)
			{
				if(Stat.getVisible(i))
				{
					flag = (countnum-scrollLoc + 1) * 20.0 > statBoxHorizMargin * height/3.0;
					if((countnum-scrollLoc) * 20.0 < (1-statBoxHorizMargin) * height - 40 + 1 && flag)
					{
						drawString(fontRenderer, String.format("%-12s %10d      XP: %12d / %d", Stat.getSkillName(i)+":", Stat.getLevel(i), Stat.getXP(i), Stat.getNextXPLevel(Stat.getLevel(i))), (int)(width*statBoxVertMargin) + 5, (int)(height*statBoxHorizMargin) + (countnum-scrollLoc)*20, Color.ORANGE.getRGB());//0xffffff);
					}
					if((countnum-scrollLoc) * 20.0 + 10 < (1-statBoxHorizMargin) * height - 40 + 1 && flag)
					{
						String string = Stat.getHelp(i);
						string = string.substring(0, string.indexOf("; "));
						if(string.length() >= descriptionLength)
							string = string.substring(0, descriptionLength-2)+"...";
						
						drawString(fontRenderer, string, (int)(width*statBoxVertMargin) + 10, (int)(height*statBoxHorizMargin) + (countnum-scrollLoc)*20 + 10, Color.lightGray.getRGB());
					}
					countnum++;
				}
				else visibleCounter++;
			}
		}
		else if(infoScreen)
		{
			if(prevScrollLoc + currentHelpIndex < Stat.getLength())
				drawCenteredString(fontRenderer, Stat.getSkillName((byte)(currentHelpIndex+prevScrollLoc))+" Information", width / 2, (int)(height*statBoxHorizMargin) + 2, Color.GREEN.getRGB());
			else
				drawCenteredString(fontRenderer, "No easter egg. Go home fool :P", width / 2, (int)(height*statBoxHorizMargin) + 2, Color.white.getRGB());
			
			byte id = (byte) (currentHelpIndex+prevScrollLoc);
			Stat.makeHelpTruncated(id, descriptionLength);
			int countnum = 0;
			boolean flag;
			for(byte i = 0; i < Stat.getLength(); i++)
			{
				flag = (countnum-scrollLoc + 1) * 10.0 > statBoxHorizMargin * height/3.0;
				if((countnum-scrollLoc) * 10.0 < (1-statBoxHorizMargin) * height - 40 + 1 && flag)
				{
					drawString(fontRenderer, Stat.getHelpLine(id, i), (int)(width*statBoxVertMargin) + 5, (int)(height*statBoxHorizMargin) + (countnum-scrollLoc)*10, Color.white.getRGB());
				}
//				if((countnum-scrollLoc) * 20.0 + 10 < (1-statBoxHorizMargin) * height - 40 + 1 && flag)
//				{	
//					drawString(fontRenderer, Stat.getHelpLine(id, i), (int)(width*statBoxVertMargin) + 10, (int)(height*statBoxHorizMargin) + (countnum-scrollLoc)*20 + 10, Color.lightGray.getRGB());
//				}
				countnum++;
			}
//			int countnum = 0, currentHeight;
//			boolean flag;
//			for(byte i = 0; i < Stat.getHelpTruncatedSize(id); i++)
//			{
//				flag = (countnum-scrollLoc + 1) * 10.0 > statBoxHorizMargin * height/3.0;
//				if((countnum-scrollLoc) * 10.0 < (1-statBoxHorizMargin) * height && flag)
//				{
//					currentHeight = (int)(height*statBoxHorizMargin) + (i+1)*10;
//					drawString(fontRenderer, Stat.getHelpLine(id, i), (int)(width*statBoxVertMargin) + 3, currentHeight, Color.white.getRGB());
//				}
//				countnum++;
//			}
		}
		else if(difficultyScreen)
		{
			drawCenteredString(fontRenderer, "Set Difficulty", width / 2, (int)(height*statBoxHorizMargin) + 2, Color.GREEN.getRGB());
			drawString(fontRenderer, "Increase the difficulty to gain levels more slowly", (int)(width*statBoxVertMargin) + 3, (int)(height*statBoxHorizMargin)+30, Color.white.getRGB());
			drawString(fontRenderer, "using the up and down buttons on the right.", (int)(width*statBoxVertMargin) + 3, (int)(height*statBoxHorizMargin)+40, Color.white.getRGB());
			drawString(fontRenderer, "Difficulty Percent Added: "+mod_mcStats.getExtraDifficulty()+"%", (int)(width*statBoxVertMargin) + 3, (int)(height*statBoxHorizMargin)+60, Color.GREEN.getRGB());
			drawString(fontRenderer, "WARNING: Decreasing difficulty may", (int)(width*statBoxVertMargin) + 3, (int)(height*statBoxHorizMargin)+80, Color.RED.getRGB());
			drawString(fontRenderer, "           result in a jump in levels.", (int)(width*statBoxVertMargin) + 3, (int)(height*statBoxHorizMargin)+90, Color.RED.getRGB());
		}
		super.drawScreen(par1, par2, par3);
	}
	
	protected void actionPerformed(GuiButton button)
	{
		String displayString = button.displayString;
		if(button.id < Stat.getLength())
		{
			mainStatScreen = false;
			infoScreen = true;
			prevScrollLoc = scrollLoc;
			scrollLoc = 0;
			currentHelpIndex = button.id;
		}
		
		if(button.displayString.equals("Difficulty"))
		{
			mainStatScreen = false;
			difficultyScreen = true;
		}
		
		if(displayString.equals("Done"))
		{
			if(mainStatScreen)
				mc.displayGuiScreen(null);
			else
			{
				difficultyScreen = false;
				infoScreen = false;
				mainStatScreen = true;
				prevScrollLoc = scrollLoc;
				scrollLoc = 0;
			}
		}
		
		if(displayString.equals("Up"))
		{
			if(!difficultyScreen)
			{
				scrollLoc--;
				TimedEvent.addEvent("scroll", 10);
			}
			else
				mod_mcStats.addDifficulty();
		}
		if(displayString.equals("Down"))
		{
			if(!difficultyScreen)
			{
				scrollLoc++;
				TimedEvent.addEvent("scroll", 10);
			}
			else
				mod_mcStats.subtractDifficulty();
		}
	}
	
	protected void keyTyped(char c, int i)
	{
		if(i == Keyboard.KEY_ESCAPE || (i == mod_mcStats.mainMenuButton.keyCode && screenIsUpCounter > 10))
		{
			mc.displayGuiScreen(null);
			savedOnOpen = false;
		}
	}
	
	private boolean scrollUp()
	{
		if(scrollLoc + 8 < Stat.getLength())
		{
			scrollLoc++;
			return true;
		}
		return false;
	}
	
	private boolean scrollDown()
	{
		if(scrollLoc != 0)
		{
			scrollLoc--;
			return true;
		}
		return false;
	}
	
	public void updateScreen()
	{
		super.updateScreen();
		screenIsUpCounter++;
		if(!savedOnOpen)
		{
			mod_mcStats.updateProperties();
			savedOnOpen = true;
		}
				
		int wheel = Mouse.getDWheel();
		if(wheel < 0)//scrolling up
			scrollUp();
		if(wheel > 0)//scrolling down
			scrollDown();
		
		for (Iterator iterator = controlList.iterator(); iterator.hasNext();)
		{
			GuiButton button = (GuiButton)iterator.next();
			if(mainStatScreen)
			{
				int length = Stat.getLength();
				if((scrollLoc == 0 && button.id == length/*up button*/) || (scrollLoc + height/40 + 3 > length && button.id == length+1/*Down button*/))
					button.enabled = false;
				else if((button.id + scrollLoc >= length || !Stat.getVisible((byte) (button.id + scrollLoc))) && !(button.id == length/*up button*/ || button.id == length+1/*Down button*/ || button.id == length+2/*Done button*/ || button.id == length+3/*Difficulty button*/))
					button.enabled = false;
				else
					button.enabled = true;
			}
			else if(infoScreen)
			{
				if(button.id < Stat.getLength() + 2 || button.id == Stat.getLength() + 3)
					button.enabled = false;
				else
					button.enabled = true;
			}
			else if(difficultyScreen)
			{
				if(button.id < Stat.getLength() || button.id == Stat.getLength() + 3 || (button.id == Stat.getLength() + 1 && mod_mcStats.getExtraDifficulty() == 0))
					button.enabled = false;
				else
					button.enabled = true;
			}
		}
	}
}