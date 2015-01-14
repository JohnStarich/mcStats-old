package SolarPrizm.mcStats;

import java.io.Serializable;
import java.util.ArrayList;

import net.minecraft.client.Minecraft;
import net.minecraft.src.*;

public class Stat
{
	private static StatData[] statList = new StatData[14];
	private static ArrayList<String>[] helpTruncated = new ArrayList[statList.length];
	public static final byte visibleLength = 14;
	
	private static String getID(Minecraft mc)
	{
		return mc.theWorld.getSeed()+mc.thePlayer.username;
	}

	public static StatData[] getStatDataArray()
	{
		return statList;
	}
	
	public static void setStatDataArray(StatData[] data)
	{
		statList = data;
	}
	
	public static boolean getVisible(byte id)
	{
		return id < visibleLength;//statList.length && statList[id].getVisible();
	}
	
	public static long getLevel(byte id)
	{
		if(!Minecraft.getMinecraft().isSingleplayer())
			return 0;
		return statList[id].getLevel();
	}
	
	public static long getXP(byte id)
	{
		if(!Minecraft.getMinecraft().isSingleplayer())
			return 0;
		return statList[id].getXP();
	}
	
	public static void addExperience(String statName, long xp)
	{//TODO does this need to be so complicated?
		if(xp > 0 && Minecraft.getMinecraft().isSingleplayer() && !Minecraft.getMinecraft().thePlayer.capabilities.isCreativeMode)
		{
			long levelIncrease = 0;

			levelIncrease = statList[getSkillId(statName)].addXP(xp);
			if(levelIncrease != 0)
			{
				mod_mcStats.mc.thePlayer.addChatMessage(statName+" Level Up! +"+levelIncrease);
				mod_mcStats.mc.thePlayer.addChatMessage(statName+": "+Stat.getSkillLevel(statName));
			}
			mod_mcStats.updateProperties();
		}
	}
	
	public static void addExperience(byte id, long xp)
	{//TODO does this need to be so complicated?
		if(xp > 0 && Minecraft.getMinecraft().isSingleplayer() && !Minecraft.getMinecraft().thePlayer.capabilities.isCreativeMode)
		{
			long levelIncrease = 0;
			levelIncrease = statList[id].addXP(xp);
			if(levelIncrease != 0)
			{
				mod_mcStats.mc.thePlayer.addChatMessage(getSkillName(id)+" Level Up! +"+levelIncrease);
				mod_mcStats.mc.thePlayer.addChatMessage(getSkillName(id)+": "+Stat.getLevel(id));
			}
			mod_mcStats.updateProperties();
		}
	}
	
	public static long getNextXPLevel(String skillName)
	{
		if(!Minecraft.getMinecraft().isSingleplayer())
			return 10;
		return getNextXPLevel(getSkillLevel(skillName));
	}
	
	public long getLevel(int id)
	{
		return statList[id].getLevel();
	}
	
	public long getXP(int id)
	{
		return statList[id].getXP();
	}
	
	public static long getLevel(String skillName)
	{
		if(!Minecraft.getMinecraft().isSingleplayer())
			return 0;
		return statList[getSkillId(skillName)].getLevel();
	}
	
	public long getXP(String skillName)
	{
		return statList[getSkillId(skillName)].getXP();
	}
	
	public static String getSkillName(byte id)
	{
		switch(id)
		{
		case 0	: return "Mining";
		case 1	: return "Digging";
		case 2	: return "Fishing";
		case 3	: return "Repair";
		case 4	: return "Harvesting";
		case 5	: return "Swimming";
		case 6	: return "Taming";
		case 7	: return "Sorcery";
		case 8	: return "Acrobatics";
		case 9	: return "Archery";
		case 10	: return "Sword";
		case 11	: return "Woodcutting";
		case 12	: return "Unarmed";
		case 13	: return "Axe";
		case 14	: return "Diving";
		default	: return null;
		}
	}
	
	/**
	 * Calculates the next experience level at which the player levels up.
	 * @param level The current skill level.
	 * @return The next required experience level to level up.
	 */
	public static long getNextXPLevel(long level)
	{
		if(!Minecraft.getMinecraft().isSingleplayer())
			return 10;
//		if(level <= 0)
//			return 10;
		return mod_mcStats.getStatCurve(level);
		//return (long)(level*1.8);
	}
	
	public static long getSkillLevel(String skillName)
	{
		if(!Minecraft.getMinecraft().isSingleplayer())
			return 0;
		return statList[getSkillId(skillName)].getLevel();
	}
	
	public static byte getSkillId(String skillName)
	{
		if(skillName.equals("Mining"))
			return 0;
		if(skillName.equals("Digging"))
			return 1;
		if(skillName.equals("Fishing"))
			return 2;
		if(skillName.equals("Repair"))
			return 3;
		if(skillName.equals("Harvesting"))
			return 4;
		if(skillName.equals("Swimming"))
			return 5;
		if(skillName.equals("Taming"))
			return 6;
		if(skillName.equals("Sorcery"))
			return 7;
		if(skillName.equals("Acrobatics"))
			return 8;
		if(skillName.equals("Archery"))
			return 9;
		if(skillName.equals("Sword"))
			return 10;
		if(skillName.equals("Woodcutting"))
			return 11;
		if(skillName.equals("Unarmed"))
			return 12;
		if(skillName.equals("Axe"))
			return 13;
		return 0;
	}
	
	public static String getHelp(byte id)
	{
		switch(id)
		{
		case 0  : return "Use a pick to mine blocks such as stone, iron, coal, and diamond.;  ; Help Menu in Progress.";
		case 1  : return "Use a shovel to dig blocks like dirt, sand, and gravel.; Help Menu in Progress.";
		case 2  : return "Fish with a fishing pole. As if you would use something else!; Help Menu in Progress.";
		case 3  : return "Right click an iron block with a damaged item to repair it.; You must have the material used to make the tool in your inventory when using an anvil (iron block).";
		case 4  : return "Harvest crops like wheat and melons.; If you use a hoe, you can use special abilities (coming soon).";
		case 5  : return "Swim around a bit on the surface and underwater.; By swimming around in the water you gain experience and this can make you swim faster and break blocks faster underwater.";
		case 6  : return "Tame animals for your farm or for pets.; Skill in progress, check for updates on this one on the website.";
		case 7  : return "Make potions and enchant items.; Make magical items like potions, golden apples, or enchant items to gain experience.";
		case 8  : return "Fall a distance and survive.; This reduces the amount of fall damage you take for large falls. Gain experience by taking damage from falling.";
		case 9  : return "Kill mobs using a bow.; At level "+mod_mcStats.ARCHERY_SET_FIRE+" you have a chance to shoot fire arrows.";
		case 10 : return "Kill mobs with a sword.; Use your sword on mobs to gain combat experience.";
		case 11 : return "Cut down trees with an axe.; Coming soon: Cut down trees with a single stroke! Also, in a upcoming update you will receive different experience amounts for different tree types.";
		case 12 : return "Just punch some mobs!; At level "+mod_mcStats.CRITICAL_HIT+" you will have a chance to critical hit a mob.";
		case 13 : return "Hack away at mobs with an axe!; Pound in the heads of yer foes with yer mighty lumberjack axe.";
		default : return "";
		}
	}
	
	private static int xp(int blockID)
	{
		return ExperienceInfo.getXPInfo(blockID) != null ? ExperienceInfo.getXPInfo(blockID).getXP() : 0;
	}
	
	private static int xp(Block block)
	{
		return ExperienceInfo.getXPInfo(block.blockID) != null ? ExperienceInfo.getXPInfo(block.blockID).getXP() : 0;
	}
	
	public static String getXPInformation(byte id)
	{
		String temp = "";
		ArrayList<ExperienceInfo> xpInfo = ExperienceInfo.getInfoForSkill(getSkillName(id));
		for(ExperienceInfo xpinfo : xpInfo)
		{
			String blockName = Block.blocksList[xpinfo.getID()].getBlockName();
			blockName = blockName.substring(blockName.lastIndexOf('.')+1);
			temp += blockName+" / "+xpinfo.getXP()+"; ";
		}
		temp = temp.substring(0, temp.length()-2);
		return temp;
//		switch(id)
//		{
//		case 0	: return "Stone / "+xp(Block.stone)+"; Diamond / "+xp(Block.oreDiamond)+"; Gold / "+xp(Block.oreGold)+"; Obsidian / "+xp(Block.obsidian)+"; Netherrack / "+xp(Block.netherrack)+"; Mossy Cobble / "+xp(Block.cobblestoneMossy)+"; Coal / "+xp(Block.oreCoal)+"; Sandstone / "+xp(Block.sandStone)+"; Glowstone / "+xp(Block.glowStone);
//		case 1	: return "";
//		case 2	: return "";
//		case 3	: return "Wood / "+mod_mcStats.REPAIR_WOOD+"; Leather / "+mod_mcStats.REPAIR_LEATHER+"; String / "+mod_mcStats.REPAIR_STRING+"; Stone / "+mod_mcStats.REPAIR_STONE+"; Iron / "+mod_mcStats.REPAIR_STEEL+"; Gold / "+mod_mcStats.REPAIR_GOLD+"; Diamond / "+mod_mcStats.REPAIR_DIAMOND+"; Chain / "+mod_mcStats.REPAIR_CHAIN;
//		case 4	: return "";
//		case 5	: return "";
//		case 6	: return "";
//		case 7	: return "";
//		case 8	: return "";
//		case 9	: return "";
//		case 10	: return "";
//		case 11	: return "";
//		case 12	: return "";
//		case 13	: return "";
//		default	: return "";
//		}
	}
	
	public static boolean getSkillUsesBlocks(byte id)
	{
		switch(id)
		{
		case 0  : return true;
		case 1  : return true;
		case 11 : return true;
		default : return false;
		}
	}

	public static int getLength()
	{
		if(!Minecraft.getMinecraft().isSingleplayer())
			return 0;
		return statList.length;
	}
	
	public static int getHelpTruncatedSize(byte id)
	{
		if(!Minecraft.getMinecraft().isSingleplayer())
			return 0;
		return helpTruncated[id].size();
	}
	
	public static String getHelpLine(byte id, int line)
	{
		if(!Minecraft.getMinecraft().isSingleplayer())
			return "";
		return helpTruncated[id].get(line);
	}
	
	public static void makeHelpTruncated(byte id, int length)
	{
		if(statList[id].getTruncatedLength() != length)
		{
			if(length > 30)
			{
				String string = getHelp(id)+";  ; "+(id != 3 ? "Block or Item / XP; " : "Repair Type / Level Required")+getXPInformation(id);
				if(getSkillUsesBlocks(id))
					string += ";  ; You cannot gain XP from placed blocks.";
				helpTruncated[id] = new ArrayList<String>();
				while(!string.equals(""))
				{
					if(string.length() > length && string.contains("; ")) //too long && has "; "
					{ //in this case, find out if the "; " comes before the max length or after
						if(length < string.indexOf("; "))
						{
							//make shorter
							while(string.length() > length + 1 && !string.substring(0,length).contains("; "))
							{
								if(string.length() < 10)
								{
									helpTruncated[id].add(string.substring(0, length));
									if(string.length() != length)
										string = string.substring(length);
								}
								else
								{
									int space = string.substring(0, length).lastIndexOf(" ");
									helpTruncated[id].add(string.substring(0, space));
									if(string.length() != length && string.length() != space + 1)
										string = string.substring(space + 1);
								}
							}
							
							//make new line at "; "
							if(string.contains("; "))
							{
								helpTruncated[id].add(string.substring(0, string.indexOf("; ")));
								string = string.substring(string.indexOf("; ") + 2);
							}
						}
						else
						{
							//make new line at "; "
							if(string.contains("; "))
							{
								helpTruncated[id].add(string.substring(0, string.indexOf("; ")));
								string = string.substring(string.indexOf("; ") + 2);
							}
							
							//make shorter
							while(string.length() > length + 1 && !string.substring(0,length).contains("; "))
							{
								if(string.length() < 10)
								{
									helpTruncated[id].add(string.substring(0, length));
									if(string.length() != length)
										string = string.substring(length);
								}
								else
								{
									int space = string.substring(0, length).lastIndexOf(" ");
									helpTruncated[id].add(string.substring(0, space));
									if(string.length() != length && string.length() != space + 1)
										string = string.substring(space + 1);
								}
							}
						}
					}
					else if(string.length() > length + 1) //too long
					{
						if(string.length() < 10)
						{
							helpTruncated[id].add(string.substring(0, length));
							if(string.length() != length)
								string = string.substring(length);
						}
						else
						{
							int space = string.substring(0, length).lastIndexOf(" ");
							helpTruncated[id].add(string.substring(0, space));
							if(string.length() != length && string.length() != space + 1)
								string = string.substring(space + 1);
						}
					}
					else if(string.indexOf("; ") > 0) //has "; "
					{
						helpTruncated[id].add(string.substring(0, string.indexOf("; ")));
						string = string.substring(string.indexOf("; ") + 2);
					}
					else if(string.length() <= length)// && !string.contains("; "))
					{
						helpTruncated[id].add(string);
						string = "";
					}
				}
			}
		}
	}

	private static void setLevel(byte id, long level)
	{
		if(Minecraft.getMinecraft().isSingleplayer())
			statList[id].setLevel(level);
	}

	private static void setXP(byte id, long level)
	{
		if(Minecraft.getMinecraft().isSingleplayer())
			statList[id].setLevel(level);	
	}

	public static void createStats()
	{
		for(byte i = 0; i < statList.length; i++)
		{
			if(i < visibleLength)
				statList[i] = new StatData(0, 0, /*getID(mod_mcStats.mc),*/ true);
			else
				statList[i] = new StatData(0, 0, /*getID(mod_mcStats.mc),*/ false);	
		}
	}

	public static String getID()
	{
		return mod_mcStats.mc.theWorld.getSeed()+mod_mcStats.mc.thePlayer.username;
	}
}

class StatData implements Serializable
{
	private long skillLevel;
	private long experience;
	//private String identifier;
	private transient boolean visible = true;
	private transient int truncatedLength = -1;
	
	public StatData(long s, long xp)//, String id)
	{
		skillLevel = s;
		experience = xp;
	//	identifier = id;
	}
	
	public StatData(long s, long xp, /*String id,*/ boolean v)
	{
		skillLevel = s;
		experience = xp;
		visible = v;
	//	identifier = id;
	}
	
	/*public StatData(String id)
	{
		identifier = id;
		experience = 0;
		skillLevel = 0;
	}*/
	
	/*protected String getID()
	{
		return identifier;
	}*/

	public void reset()
	{
		skillLevel = 0;
		experience = 0;
	}
	
	public void setTruncatedLength(int length)
	{
		truncatedLength = length;
	}
	
	public int getTruncatedLength()
	{
		return truncatedLength;
	}
	
	public boolean getVisible()
	{
		return visible;
	}

	public long getLevel()
	{
		return skillLevel;
	}
	
	public String toString()
	{
		return "level="+skillLevel+" XP="+experience;
	}
	
	void levelUp()
	{
		skillLevel++;
	}
	
	protected long addXP(long xp)
	{
		experience += xp;
		long levelIncrease = 0;
		while(experience >= Stat.getNextXPLevel(skillLevel+levelIncrease))
		{
			experience -= Stat.getNextXPLevel(skillLevel+levelIncrease);
			levelIncrease++;
		}
		skillLevel += levelIncrease;
		return levelIncrease;
	}
	
	protected void setXP(long xp)
	{
		experience = xp;
	}
	
	protected void setLevel(long level)
	{
		level = skillLevel;
	}
	
	public long getXP()
	{
		return experience;
	}
}


/*public class Stat
{
	public long skillLevel = 0, experience = 0;
	public String skillName = "";
	public ArrayList<String> help = new ArrayList<String>();
	public ArrayList<String> helpTruncated;
	public String helpString = "";
	public static ArrayList<Stat> statList = new ArrayList<Stat>();
	
	public Stat(long skillNum)
	{
		skillLevel = skillNum;
	}
	
	public Stat(String skillString)
	{
		skillName = skillString;
	}
	
	public Stat(long skillNum, String skillString)
	{
		skillLevel = skillNum;
		skillName = skillString;
	}
	
	public static void addStat(long skillNum, String skillString, String string)
	{
		statList.add(new Stat(skillNum, skillString, string));
	}
	
	public static void addStat(String skillString, String string)
	{
		statList.add(new Stat(skillString, string));
	}
	
	public static void addStat(Stat stat)
	{
		statList.add(stat);
	}
	
	public Stat(long skillNum, String skillString, ArrayList<String> helpList)
	{
		skillLevel = skillNum;
		skillName = skillString;
		help = helpList;
	}
	
	public Stat(long skillNum, String skillString, String string)
	{
		skillLevel = skillNum;
		skillName = skillString;
		helpString = string;
		while(string.indexOf("; ") > 0)
		{
			help.add(string.substring(0, string.indexOf("; ")));
			string = string.substring(string.indexOf("; ") + 2);
		}
		if(string.length() > 1)
			help.add(string);
	}
	
	public Stat(long e, long skillNum, String skillString, String string)
	{
		skillLevel = skillNum;
		skillName = skillString;
		helpString = string;
		experience = e;
		while(string.indexOf("; ") > 0)
		{
			help.add(string.substring(0, string.indexOf("; ")));
			string = string.substring(string.indexOf("; ") + 2);
		}
		if(string.length() > 1)
			help.add(string);
	}
	
	public Stat(String skillString, String string)
	{
		skillName = skillString;
		helpString = string;
		while(string.indexOf("; ") > 0)
		{
			help.add(string.substring(0, string.indexOf("; ")));
			string = string.substring(string.indexOf("; ") + 2);
		}
		if(string.length() > 1)
			help.add(string);
	}
	
	public String toString(int helpLine)
	{
		if(helpLine >= help.size())
			return "";
		
		if(help.size() != 0)
			return help.get(helpLine);
		else
			return "Coming soon...";
	}
	
	public static Stat getStat(String string)
	{
		for(Stat stat : statList)
		{
			if(stat.skillName.equalsIgnoreCase(string))
				return stat;
		}
		return null;
	}
		
	public String toString(int helpLine, int length)
	{
		makeHelpTruncated(length);
		
		if(helpLine < helpTruncated.size())
			return helpTruncated.get(helpLine);
		
		System.out.println("helpString not caught.");
		return "";
	}
	
	public void makeHelpTruncated(int length)
	{
		if(length > 30)
		{
			String string = helpString;
			helpTruncated = new ArrayList<String>();
			while(!string.equals(""))
			{
				if(string.length() > length && string.contains("; ")) //too long && has "; "
				{ //in this case, find out if the "; " comes before the max length or after
					if(length < string.indexOf("; "))
					{
						//make shorter
						while(string.length() > length + 1 && !string.substring(0,length).contains("; "))
						{
							if(string.length() < 10)
							{
								helpTruncated.add(string.substring(0, length));
								if(string.length() != length)
									string = string.substring(length);
							}
							else
							{
								int space = string.substring(0, length).lastIndexOf(" ");
								helpTruncated.add(string.substring(0, space));
								if(string.length() != length && string.length() != space + 1)
									string = string.substring(space + 1);
							}
						}
						
						//make new line at "; "
						if(string.contains("; "))
						{
							helpTruncated.add(string.substring(0, string.indexOf("; ")));
							string = string.substring(string.indexOf("; ") + 2);
						}
					}
					else
					{
						//make new line at "; "
						if(string.contains("; "))
						{
							helpTruncated.add(string.substring(0, string.indexOf("; ")));
							string = string.substring(string.indexOf("; ") + 2);
						}
						
						//make shorter
						while(string.length() > length + 1 && !string.substring(0,length).contains("; "))
						{
							if(string.length() < 10)
							{
								helpTruncated.add(string.substring(0, length));
								if(string.length() != length)
									string = string.substring(length);
							}
							else
							{
								int space = string.substring(0, length).lastIndexOf(" ");
								helpTruncated.add(string.substring(0, space));
								if(string.length() != length && string.length() != space + 1)
									string = string.substring(space + 1);
							}
						}
					}
				}
				else if(string.length() > length + 1) //too long
				{
					if(string.length() < 10)
					{
						helpTruncated.add(string.substring(0, length));
						if(string.length() != length)
							string = string.substring(length);
					}
					else
					{
						int space = string.substring(0, length).lastIndexOf(" ");
						helpTruncated.add(string.substring(0, space));
						if(string.length() != length && string.length() != space + 1)
							string = string.substring(space + 1);
					}
				}
				else if(string.indexOf("; ") > 0) //has "; "
				{
					helpTruncated.add(string.substring(0, string.indexOf("; ")));
					string = string.substring(string.indexOf("; ") + 2);
				}
				else if(string.length() <= length)// && !string.contains("; "))
				{
					helpTruncated.add(string);
					string = "";
				}
			}
		}
	}

	public String toString()
	{
		return help.toString();
	}
	
	public static void addExperience(String statName, long xp)
	{
		if(statName.equals("Swimming"))
			if(mod_mcStats.rand.nextInt(3) == 0)
			{
				getStat(statName).addExperience(xp);
				return;
			}
		getStat(statName).addExperience(xp);
	}
	
	public static void addExperience(Stat stat, long xp)
	{
		stat.addExperience(xp);
	}

	public void addExperience(long xp)
	{
		if(!mod_mcStats.mc.isMultiplayerWorld() && mod_mcStats.mc.playerController.isNotCreative())
		{
			experience += xp;
			int levelUpCounter = 0;
			while(experience >= getNextXPLevel())
			{
				experience -= getNextXPLevel();
				skillLevel++;
				levelUpCounter++;
			}
			if(levelUpCounter > 0)
				ModLoader.getMinecraftInstance().thePlayer.addChatMessage("Level up! +"+levelUpCounter+"  "+skillName+": "+skillLevel);
		}
	}
	
	public int getNextXPLevel()
	{
		if(skillLevel == 0)
			return 10;
		return mod_mcStats.getStatCurve(skillLevel);
	}
	
	public static long getNextXPLevel(long level)
	{
		if(level <= 0)
			return 10;
		return (long)(level*1.8);
	}
}


/*if(length > 30)
{
	String string = helpString;
	helpTruncated = new ArrayList<String>();
	while(!string.equals(""))//string.indexOf("; ") > -1 || string.length() > length)
	{
		if(string.length() > length && string.contains("; ")) //too long && has "; "
		{ //in this case, find out if the "; " comes before the max length or after
			if(length < string.indexOf("; "))
			{
				//make shorter
				while(string.length() > length + 1 && !string.substring(0,length).contains("; "))
				{
					helpTruncated.add(string.substring(0, length));
					if(string.length() != length)
						string = string.substring(length);
				}
				
				//make new line at "; "
				if(string.contains("; "))
				{
					helpTruncated.add(string.substring(0, string.indexOf("; ")));
					string = string.substring(string.indexOf("; ") + 2);
				}
			}
			else
			{
				//make new line at "; "
				if(string.contains("; "))
				{
					helpTruncated.add(string.substring(0, string.indexOf("; ")));
					string = string.substring(string.indexOf("; ") + 2);
				}
				
				//make shorter
				while(string.length() > length + 1 && !string.substring(0,length).contains("; "))
				{
					helpTruncated.add(string.substring(0, length));
					if(string.length() != length)
						string = string.substring(length);
				}
			}
		}
		else if(string.length() > length + 1) //too long
		{
			helpTruncated.add(string.substring(0, length));
			if(string.length() != length)
				string = string.substring(length);
		}
		else if(string.indexOf("; ") > 0) //has "; "
		{
			helpTruncated.add(string.substring(0, string.indexOf("; ")));
			string = string.substring(string.indexOf("; ") + 2);
		}
		else if(string.length() <= length)// && !string.contains("; "))
		{
			helpTruncated.add(string);
			string = "";
		}
	}
}*/
