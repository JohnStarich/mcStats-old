package SolarPrizm.mcStats;

import java.awt.Color;
import java.awt.MouseInfo;
import java.awt.event.MouseEvent;
import java.io.DataOutput;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;

import net.minecraft.client.*;
import net.minecraft.server.*;
import net.minecraft.src.*;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.vector.Vector;
/*
	INCOMPABITBLE WITH THE FOLLOWING:
	Forge
*/


/**
 * TODO:
 * -sword xp gained different per mob
 * -enchantment xp gain
 * -fishing items
 * @author SolarPrizm
 * @version 0.3
 * 
 */
public final class mod_mcStats extends BaseMod
{
	//@MLProp(info="mcLeveling Button", max=1, min=1, name="mcLeveling Menu")
	public static int mainMenuConfig;
	public static Minecraft mc = ModLoader.getMinecraftInstance();
	public static KeyBinding mainMenuButton = new KeyBinding("LevelingMainMenu", mainMenuConfig);
	public static Random rand = new Random();
	public static final boolean testMode = true;
	//public StatSave statSave = new StatSave();
	//private static File file = new File(Minecraft.getMinecraftDir().toString()+"/config/mod_mcStats.properties");
	
	//LEVEL BONUSES:
	public static final byte
	PUNCH_MOB_DROPS_ITEM = 50,	//unarmed - punch a mob to drop items
	CRITICAL_HIT = 10,
	REPAIR_WOOD = 0,
	REPAIR_LEATHER = 0,
	REPAIR_STONE = 0,
	REPAIR_STRING = 5,
	REPAIR_STEEL = 25,
	REPAIR_CHAIN = 50,
	REPAIR_GOLD = 35,
	REPAIR_DIAMOND = 40,
	ABILITY_WAIT_TIME = 1,	//time between raising and lowering tool
	XP_DROP_BONUS = 50,
	ARCHERY_SET_FIRE = 25,
	EXTRA_COMBAT_DAMAGE = 5
	;
	
	private static ArrayList<EventAction> eventActions = new ArrayList<EventAction>();
	
	private static World world = mc.theWorld;
	private static boolean runOnce = true;
	public boolean onTickInGame(float tick, Minecraft minecraft)
	{
		if(mc.theWorld != null && minecraft.isSingleplayer())
		{
			justEnteredWater();
			
			if(testMode)
			{
				if(Keyboard.isKeyDown(Keyboard.KEY_5))
					for(byte i = 0; i < Stat.getLength(); i++)
						Stat.addExperience(i, 5000);
				else if(Keyboard.isKeyDown(Keyboard.KEY_6))
					Stat.createStats();
			}
			
			if(blockPlaceCounter > 0)
				blockPlaceCounter--;
			
//			if(runOnce)
//			{
//				runOnce = false;
//			}
			
			if(loadProperties())
				updateProperties();
			
			//TODO fix?
//			if(!isSwitchableWithHand() && (minecraft == null || (rightClicking() && this.mc.currentScreen == null)))
//			{
//				if(blockPlaceCounter <= 0)
//					doAbilityEvent();
//				return true;
//			}
			if(minecraft.isSingleplayer())
			{
				if(this.mc.currentScreen == null && Keyboard.isKeyDown(Keyboard.KEY_T))
					this.mc.displayGuiScreen(new GuiChat());
				TimedEvent.onUpdate();
				StatData[] array = Stat.getStatDataArray();
				
				for(int t = 0; t < eventActions.size() && !eventActions.isEmpty(); t++)
				{
					EventAction event = eventActions.get(t);
					if(event != null && event.isDone())
					{
						if(event.getLabel().equals("growcrop"))
						{
							getWorldObj().setBlockWithNotify(event.getData(0), event.getData(1), event.getData(2), event.getData(3));
							getWorldObj().setBlockMetadataWithNotify(event.getData(0), event.getData(1), event.getData(2), rand.nextInt(3)+1);
						}
						eventActions.remove(event);
					}
				}
				
//				if(TimedEvent.getCompleted("growcrop"))
//				{
//					getWorldObj().setBlockWithNotify(x, y, z, block.blockID);
//					getWorldObj().setBlockMetadataWithNotify(x, y, z, rand.nextInt(3));
//				}
				
				for(byte i = 0; i < Stat.getLength(); i++)
				{
					String skillName = Stat.getSkillName(i);
					boolean skillReadied = TimedEvent.getCreated(skillName);
					if(skillReadied && !TimedEvent.active(skillName+"On") && !TimedEvent.active(skillName+"Cooldown"))
						sendChat("You raise your "+getTool(skillName)+".", "lime");
					
					if(TimedEvent.getCreated(skillName+"On"))
						sendChat(skillName+" ability activated!", "lime");
					else if(TimedEvent.getCompleted(skillName+"On"))
					{
						TimedEvent.addEvent(skillName+"Cooldown", getCooldown(skillName));
						sendChat(skillName+" ability wore off.", "lime");
					}
					else if(TimedEvent.getCompleted(skillName) && !TimedEvent.active(skillName+"On") && !TimedEvent.active(skillName+"Cooldown"))
						sendChat("You lower your "+getTool(skillName)+".", "lime");
//					else if(TimedEvent.getCreated(skillName+"Cooldown"))
//					{
//						sendChat("You can't use that ability, you are too tired. ("+TimedEvent.timeLeft(skillName+"On")+"s)", "lime");
//					}
					
					if(TimedEvent.getCompleted(skillName+"Cooldown"))
						sendChat(skillName+" Ability refreshed.", "lime");
					
					if(skillReadied)
						doAbilityEvent();
				}
			}
			return true;
		}
		return false;
	}
	
	private static long extraDifficulty = 0;
	
	public static long getExtraDifficulty()
	{
		return extraDifficulty;
	}
	
	public static boolean setExtraDifficulty(long extra)
	{
		if(extra >= 0)
		{
			extraDifficulty = extra; 
			return true;
		}
		else
			return false;
	}
	
	public static void addDifficulty()
	{
		extraDifficulty++;
		updateProperties();
	}
	
	public static void subtractDifficulty()
	{
		if(extraDifficulty - 1 >= 0)
		{
			extraDifficulty--;
			updateProperties();
		}
	}
	
	private static World prevWorld = mc.theWorld;
	private static File file;// = new File(Minecraft.getMinecraftDir().toString()+"/config/mod_mcStats.dat");
	private static boolean loadedProperties = false;
	public static boolean loadProperties()
	{
		try {
			if(!loadedProperties || !mc.theWorld.equals(prevWorld))
			{
				prevWorld = mc.theWorld;
				try
				{
					file = new File(mc.getMinecraftDir()+"/saves/"+mc.getIntegratedServer().getFolderName()+"/mod_mcStats.txt");
					if(file.createNewFile())
					{
						Stat.createStats();
						FileOutputStream out = new FileOutputStream(file);
						ObjectOutputStream objectOut = new ObjectOutputStream(out);
						objectOut.writeObject(Stat.getStatDataArray());
						objectOut.writeUTF(BlockCoord.getCoord().toString());
						objectOut.writeLong(extraDifficulty);
						objectOut.close();
						out.close();
					}
					else
					{
						FileInputStream in = new FileInputStream(file);
						ObjectInputStream objectIn = new ObjectInputStream(in);
						Stat.setStatDataArray((StatData[])objectIn.readObject());
						//BlockCoord.setCoord((ArrayList<BlockCoord>)objectIn.readObject());
						BlockCoord.setCoord(objectIn.readUTF());
						//System.out.println("BlockCoord: "+BlockCoord.getCoord());
						extraDifficulty = objectIn.readLong();
						objectIn.close();
						in.close();
					}
				}
				catch(Exception e){e.printStackTrace();}
				loadedProperties = true;
				return true;
			}
			else return false;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	private static int updateCount = 0;
	public static void updateProperties()
	{
		if(mc.theWorld != null)
		{
			try
			{
				FileOutputStream out = new FileOutputStream(file);
				ObjectOutputStream objectOut = new ObjectOutputStream(out);
				objectOut.writeObject(Stat.getStatDataArray());
				objectOut.writeUTF(BlockCoord.getCoord().toString());
				objectOut.writeLong(extraDifficulty);
				objectOut.close();
				out.close();
				
				System.out.println("Properties successfully updated. "+updateCount);
				updateCount++;
			}
			catch(Exception e){e.printStackTrace();}
		}
	}
	
	/*public static void updateProperties(int type)
	{
		boolean updateBlocks = type == 0;
		boolean updateSkills = type == 1;
		if(mc.theWorld != null && (updateBlocks || updateSkills))
		{
			try
			{
				FileOutputStream out = new FileOutputStream(file);
				ObjectOutputStream objectOut = new ObjectOutputStream(out);
				if(updateSkills)
					objectOut.writeObject(Stat.getStatDataArray());
				else
					objectOut.writeUTF(BlockCoord.getCoord().toString());
				objectOut.close();
				out.close();
				
				System.out.println("Properties successfully updated. "+updateCount);
				updateCount++;
			}
			catch(Exception e){e.printStackTrace();}
		}
	}*/
	
	//TODO fix?
	/*public static boolean prevPlayerAction = false;
	public static boolean blockActivated()
	{
		boolean blockActivated = false;
		net.minecraft.src.PlayerControllerMP controller = mc.playerController;
		BlockCoord coord = controller.getCurrentBlock();
		Block block = coord.getBlockID() > 0 ? Block.blocksList[coord.getBlockID()] : null;
		blockActivated = block.onBlockActivated(mc.theWorld, coord.getX(), coord.getY(), coord.getZ(), mc.thePlayer, (int)0f, 0f, 0f, 0f);
		prevPlayerAction = blockActivated;
		return blockActivated;
	}*/
	
	public static void doAbilityEvent()
	{
		ItemStack itemstack = mc.thePlayer.getCurrentEquippedItem();
		doAbilityEvent(itemstack != null ? mod_mcStats.getSkillNameFromTool(itemstack.itemID) : "Unarmed");
	}
	
	public static String getTranslation(String stringToTranslate)
	{
		String language = mc.gameSettings.language;
		if(language.contains("en"))
			return stringToTranslate;
		if(language.equals("fr_FR"))
		{
			
		}
		else if(language.equals("pt_BR"))
		{
			
		}
		return null;
	}
	
	public static void doAbilityEvent(String skillString)
	{
		if(!TimedEvent.active(skillString+"On") && !TimedEvent.active(skillString+"Cooldown"))
			TimedEvent.addEvent(skillString, mod_mcStats.ABILITY_WAIT_TIME);
		else if(TimedEvent.active(skillString+"Cooldown"))
			sendChat("You can't use that ability, you are too tired. ("+TimedEvent.timeLeft(skillString+"Cooldown")+"s)", "lime");
	}
	
	public static KeyBinding rightClick = mc.gameSettings.keyBindUseItem;
	public static KeyBinding leftClick = mc.gameSettings.keyBindAttack;
	public static boolean rightClicking()//TODO make this more customizable
	{
//		if(Mouse.isButtonDown(1))
//			return true;
//		else
			return false;
	}
	
	public static void sendChat(String string)
	{
		sendChat(string, "f");
	}
	
	public static void sendChat(String string, String colorCode)
	{
		if(colorCode.length() != 1)
		{
			if(colorCode.equalsIgnoreCase("navy"))
				colorCode = "1";
			else if(colorCode.equalsIgnoreCase("green"))
				colorCode = "2";
			else if(colorCode.equalsIgnoreCase("teal"))
				colorCode = "3";
			else if(colorCode.equalsIgnoreCase("red"))
				colorCode = "4";
			else if(colorCode.equalsIgnoreCase("purple"))
				colorCode = "5";
			else if(colorCode.equalsIgnoreCase("orange"))
				colorCode = "6";
			else if(colorCode.equalsIgnoreCase("gray"))
				colorCode = "7";
			else if(colorCode.equalsIgnoreCase("dark-gray") || colorCode.equalsIgnoreCase("darkgray"))
				colorCode = "8";
			else if(colorCode.equalsIgnoreCase("blue"))
				colorCode = "9";
			else if(colorCode.equalsIgnoreCase("black"))
				colorCode = "0";
			else if(colorCode.equalsIgnoreCase("lime") ||colorCode.equalsIgnoreCase("light-green") || colorCode.equalsIgnoreCase("lightgreen"))
				colorCode = "a";
			else if(colorCode.equalsIgnoreCase("aqua") || colorCode.equalsIgnoreCase("lightblue") || colorCode.equalsIgnoreCase("cyan") || colorCode.equalsIgnoreCase("light-blue"))
				colorCode = "b";
			else if(colorCode.equalsIgnoreCase("light-red") || colorCode.equalsIgnoreCase("lightred"))
				colorCode = "c";
			else if(colorCode.equalsIgnoreCase("pink"))
				colorCode = "d";
			else if(colorCode.equalsIgnoreCase("yellow"))
				colorCode = "e";
			else colorCode = "f";
		}
		mc.thePlayer.addChatMessage("¤"+colorCode+string);
		/*
		1 - navy
		2 - green
		3 - teal
		4 - red
		5 - purple
		6 - orange
		7 - gray
		8 - dark-gray
		9  - blue
		0 - black
		a - lime
		b - aqua
		c - light red
		d - pink
		e - yellow
		f - white
		 */
	}
	
	public static float getSwimmingSpeedBoost()
	{
		float boost = Stat.getSkillLevel("Swimming") / 10 / 60f;
		return boost > 0 ? boost : 0;
	}
	
	private static boolean prevInWater = false;
	public static boolean justEnteredWater()
	{
		if(mc.thePlayer.isInsideOfMaterial(Material.water) && !prevInWater)
		{
			prevInWater = true;
			return true;
		}
		else if(!mc.thePlayer.isInsideOfMaterial(Material.water))
			prevInWater = false;
		return false;
	}
	
	private static int prevAir = 300;
	private static int breathCount = 0;
	public static int breathRestore(int air)
	{
/*		if(air == 300)
		{
			air += (int)(Stat.getSkillLevel("Swimming")/100.0*30)+10;
			prevAir = air;
			sendChat("Bonus Air: "+air, "cyan");
		}
		if(air == 301)
			air = 299;
		air += MathHelper.floor_double(Stat.getSkillLevel("Swimming") / 50f);
		System.out.println("Air: "+(air));
		
		if(air <= prevAir)
		{
//			if(breathCount > 0)
//			{
//				breathCount--;
//				return air;// <= 300 ? air + restore : 300;
//			}
//			else
//			{
//				breathCount = 10;
				return air;
//			}
		}
		else
		{
			return prevAir--;
		}*/
		if(air == 300)
		{
			prevAir = 300;
		}
		if(breathCount > 0)
		{
			breathCount--;
			int tempPrevAir = prevAir;
			prevAir = air;
			return tempPrevAir;
		}
		else
		{
			breathCount = (int)(Stat.getSkillLevel("Swimming")/100.0*30);
			prevAir = air;
			return air;
		}
		//return air;
	}
	
	public static float getSpeedBoost(int block)
	{
		ItemStack itemstack = mc.thePlayer.getCurrentEquippedItem();
		String skillString = itemstack != null ? getSkillNameFromTool(itemstack.itemID) : "Unarmed";
		boolean hasXPInfo = ExperienceInfo.getXPInfo(block) != null;
		String xpString = hasXPInfo ? ExperienceInfo.getXPInfo(block).getType() : "";
		boolean shouldNotGiveNormalBonus = !skillString.equals("Unarmed") && (!skillString.equals("Sword") || xpString.equals("Sword")) && TimedEvent.active(skillString+"On");
		if(xpString.equals(skillString) && (TimedEvent.active(skillString) || TimedEvent.active(skillString+"On")))
		{//"On" = ability is on, "Cooldown" = can't use ability, the string itself is equivalent to the tool being lifted
			if(!TimedEvent.active(skillString+"On") && !TimedEvent.active(skillString+"Cooldown") && TimedEvent.active(skillString))
			{
				TimedEvent.addEvent(skillString+"On", getAbilityTime(skillString));
				TimedEvent.removeEvent(skillString);
			}
			if(!shouldNotGiveNormalBonus)
				return (float)(doubleRand((int)Math.pow(Stat.getSkillLevel(skillString), 2.0/5))) / 240F;
		}
		if( (xpString.equals("Digging") && TimedEvent.active("Unarmed")) || (TimedEvent.active(skillString) && skillString.equals(xpString)))
		{
			if(TimedEvent.active("Unarmed") && !TimedEvent.active("UnarmedCooldown"))
				TimedEvent.addEvent("UnarmedOn", getAbilityTime("Unarmed"));
			return 100F;
		}
	//	return (float)(Math.pow(statList.get(getStatLoc(skillString)).skillLevel, 2.0/11)) / 60F;
		return 0;
	}
	
	public static long getCooldown(String skillName)
	{
		return Stat.getLevel(skillName) / 8 + 30;
	//	return 3;
	}
	
	public static long getAbilityTime(String skillString)
	{
		return Stat.getLevel(skillString) / 10 + 3;
	}
	
	public static long getAbilityTime(byte id)
	{
		return Stat.getLevel(id) / 10 + 2;
	}
	
	public static String getSkillNameFromTool(int itemID)
	{
		if(itemID == 0)
			return "";
		
		if(isAxe(itemID))
			return "Woodcutting";
		else if(isPick(itemID))
			return "Mining";
		else if(isHoe(itemID))
			return "Harvesting";
		else if(isShovel(itemID))
			return "Digging";
		else if(isSword(itemID))
			return "Sword";
		else
			return "";
	}

	public static String getTool(String statName)
	{
		if(statName.equals("Mining"))
			return "pickaxe";
		else if(statName.equals("Woodcutting"))
			return "axe";
		else if(statName.equals("Unarmed"))
			return "fist";
		else if(statName.equals("Harvesting"))
			return "hoe";
		else if(statName.equals("Digging"))
			return "shovel";
		else if(statName.equals("Sword"))
			return "sword";
		else
			return "tool";
	}
	
	public void keyboardEvent(KeyBinding keyBinding)
	{
		if(keyBinding == mainMenuButton && mc.currentScreen == null && keyBinding.isPressed())
			mc.displayGuiScreen(new GuiLeveling());
	}
	
	public static ArrayList<Integer> switchWithHand = new ArrayList<Integer>();
	public static boolean isSwitchableWithHand(int i)
	{
		for(Integer integer : switchWithHand)
			if(integer == i)
				return true;
		return false;
	}
	
	//TODO fix?
//	public static boolean isSwitchableWithHand()
//	{
//		return isSwitchableWithHand(mc.playerController.getCurrentBlock().getBlockID());
//	}
	
	public int counter = 0;
	
	private static NBTTagList prevEnchantment;
	private static boolean runOnceWhilePaused = true;
	
	//TODO fix
/*	public boolean onTickInGUI(float tick, Minecraft mc, GuiScreen guiscreen)
	{
		if(mc.theWorld != null && mc.isSingleplayer())
		{
			BlockCoord coord = mc.playerController.getCurrentBlock();
			if(guiscreen.equals(new GuiEnchantment(mc.thePlayer.inventory, getWorldObj(), coord.getX(), coord.getY(), coord.getZ())))
			{
				ItemStack itemstack;
				itemstack = ((GuiEnchantment)guiscreen).inventorySlots.getSlot(0).getStack();
				if(itemstack == null) return false;
				
				NBTTagList curEnchantment = itemstack.getEnchantmentTagList();
				
				if(!prevEnchantment.equals(curEnchantment))
				{
					int xp = 0;
					for(int i = 0; i < curEnchantment.tagCount(); i++)
					{//TODO fix xp values and verify this works
						String string = curEnchantment.tagAt(i).getName();
						xp += ExperienceInfo.getStringInfo(string) != null ? ExperienceInfo.getStringInfo(string).getXP() * curEnchantment.tagAt(i).getId() : 0;
					}
					Stat.addExperience("Sorcery", xp);
				}
				prevEnchantment = curEnchantment;
			}
		}
		return false;
	}*/
		
	public mod_mcStats()
	{
	}
	
	public void takenFromCrafting(EntityPlayer entityplayer, ItemStack itemstack, IInventory iinventory)
	{
		if(mc.isSingleplayer() && itemstack != null)
		{//TODO verify this works on potions
			int id = itemstack.itemID;
			if(id == Item.potion.shiftedIndex)
			{
				if(ItemPotion.isSplash(id))
					Stat.addExperience("Sorcery", 10);
				else
					Stat.addExperience("Sorcery", 5);
			}
			
			if(isPick(id))
				Stat.addExperience("Mining", 2);
			else if(isHoe(id))
				Stat.addExperience("Harvesting", 2);
			else if(isAxe(id))
				Stat.addExperience("Woodcutting", 2);
			else if(isShovel(id))
				Stat.addExperience("Digging", 2);
			else if(id == Item.fishingRod.shiftedIndex)
				Stat.addExperience("Fishing", 2);
			else if(id == Item.bow.shiftedIndex)
				Stat.addExperience("Archery", 2);
			else if(isSword(id))
				Stat.addExperience("Sword", 2);
			
			if(id == Item.appleGold.shiftedIndex)
				Stat.addExperience("Sorcery", 30);
		}
	}
	
	public String getVersion()
	{
		return "v1.4.3 for Minecraft 1.3.2";
	}

	public void load()
	{
		ExperienceInfo.addXPInfo(5, Block.stone.blockID, "Mining");
		ExperienceInfo.addXPInfo(100, Block.oreDiamond.blockID, "Mining");
		ExperienceInfo.addXPInfo(60, Block.oreGold.blockID, "Mining");
		ExperienceInfo.addXPInfo(90, Block.obsidian.blockID, "Mining");
		ExperienceInfo.addXPInfo(5, Block.netherrack.blockID, "Mining");
		ExperienceInfo.addXPInfo(5, Block.cobblestoneMossy.blockID, "Mining");
		ExperienceInfo.addXPInfo(20, Block.oreCoal.blockID, "Mining");
		ExperienceInfo.addXPInfo(5, Block.sandStone.blockID, "Mining");
		ExperienceInfo.addXPInfo(40, Block.glowStone.blockID, "Mining");
		ExperienceInfo.addXPInfo(40, Block.oreRedstone.blockID, "Mining");
		ExperienceInfo.addXPInfo(15, Block.whiteStone.blockID, "Mining");
		ExperienceInfo.addXPInfo(80, Block.oreLapis.blockID, "Mining");
		ExperienceInfo.addXPInfo(25, Block.oreIron.blockID, "Mining");
		ExperienceInfo.addXPInfo(0, Block.cobblestone.blockID, "Mining");
		
		ExperienceInfo.addXPInfo(5, Block.dirt.blockID, "Digging");
		ExperienceInfo.addXPInfo(15, Block.sand.blockID, "Digging");
		ExperienceInfo.addXPInfo(20, Block.gravel.blockID, "Digging");
		ExperienceInfo.addXPInfo(5, Block.grass.blockID, "Digging");
		ExperienceInfo.addXPInfo(5, Block.mycelium.blockID, "Digging");
		ExperienceInfo.addXPInfo(60, Block.slowSand.blockID, "Digging");
		ExperienceInfo.addXPInfo(40, Block.blockClay.blockID, "Digging");
		
		ExperienceInfo.addXPInfo(5, Item.swordWood.shiftedIndex, "Sword");
		ExperienceInfo.addXPInfo(10, Item.swordStone.shiftedIndex, "Sword");
		ExperienceInfo.addXPInfo(15, Item.swordSteel.shiftedIndex, "Sword");
		ExperienceInfo.addXPInfo(10, Item.swordGold.shiftedIndex, "Sword");
		ExperienceInfo.addXPInfo(20, Item.swordDiamond.shiftedIndex, "Sword");
		
		ExperienceInfo.addXPInfo(5, Item.axeWood.shiftedIndex, "Axe");
		ExperienceInfo.addXPInfo(10, Item.axeStone.shiftedIndex, "Axe");
		ExperienceInfo.addXPInfo(15, Item.axeSteel.shiftedIndex, "Axe");
		ExperienceInfo.addXPInfo(10, Item.axeGold.shiftedIndex, "Axe");
		ExperienceInfo.addXPInfo(20, Item.axeDiamond.shiftedIndex, "Axe");
		
		ExperienceInfo.addXPInfo(15, Block.crops.blockID, "Harvesting");
		ExperienceInfo.addXPInfo(15, Block.melon.blockID, "Harvesting");
		ExperienceInfo.addXPInfo(5, Block.cocoaPlant.blockID, "Harvesting");
		ExperienceInfo.addXPInfo(5, Block.pumpkin.blockID, "Harvesting");
		ExperienceInfo.addXPInfo(2, Block.reed.blockID, "Harvesting");
		ExperienceInfo.addXPInfo(40, Block.mushroomRed.blockID, "Harvesting");
		ExperienceInfo.addXPInfo(40, Block.mushroomBrown.blockID, "Harvesting");
		ExperienceInfo.addXPInfo(80, Block.mushroomCapRed.blockID, "Harvesting");
		ExperienceInfo.addXPInfo(80, Block.mushroomCapBrown.blockID, "Harvesting");
		ExperienceInfo.addXPInfo(15, Block.netherStalk.blockID, "Harvesting");
		ExperienceInfo.addXPInfo(20, Block.cactus.blockID, "Harvesting");
		
		ExperienceInfo.addXPInfo(5, Block.wood.blockID, "Woodcutting");
		ExperienceInfo.addXPInfo(2, Block.planks.blockID, "Woodcutting");
		
		ExperienceInfo.addXPInfo(0, "enchantment.protect", "Sorcery");//
		ExperienceInfo.addXPInfo(0, "enchantment.oxygen", "Sorcery");
		ExperienceInfo.addXPInfo(0, "enchantment.lootBonus", "Sorcery");//
		ExperienceInfo.addXPInfo(0, "enchantment.knockback", "Sorcery");
		ExperienceInfo.addXPInfo(0, "enchantment.fire", "Sorcery");
		ExperienceInfo.addXPInfo(0, "enchantment.durability", "Sorcery");
		ExperienceInfo.addXPInfo(0, "enchantment.digging", "Sorcery");
		ExperienceInfo.addXPInfo(0, "enchantment.damage", "Sorcery");//
		ExperienceInfo.addXPInfo(0, "enchantment.arrowKnockback", "Sorcery");
		ExperienceInfo.addXPInfo(0, "enchantment.arrowInfinite", "Sorcery");
		ExperienceInfo.addXPInfo(0, "enchantment.arrowDamage", "Sorcery");
		ExperienceInfo.addXPInfo(0, "enchantment.arrowFire", "Sorcery");
		ExperienceInfo.addXPInfo(0, "enchantment.waterWorker", "Sorcery");
		//TODO check these work ^ (the sorcery ones)

		//ModLoader.setInGUIHook(this, true, false);
		ModLoader.setInGameHook(this, true, true);
		ModLoader.addLocalization("LevelingMainMenu", "LevelingMainMenu");
		ModLoader.registerKey(this, this.mainMenuButton, false);
		
		switchWithHand.add(Block.blockSteel.blockID);
		switchWithHand.add(Block.trapdoor.blockID);
		switchWithHand.add(Block.fenceGate.blockID);
		switchWithHand.add(Block.doorWood.blockID);
		switchWithHand.add(Block.stoneButton.blockID);
		switchWithHand.add(Block.woodenButton.blockID);
		switchWithHand.add(Block.lever.blockID);
		
		leather.add(Item.helmetLeather.shiftedIndex);
		leather.add(Item.plateLeather.shiftedIndex);
		leather.add(Item.legsLeather.shiftedIndex);
		leather.add(Item.bootsLeather.shiftedIndex);
		
		steel.add(Item.helmetSteel.shiftedIndex);
		steel.add(Item.plateSteel.shiftedIndex);
		steel.add(Item.legsSteel.shiftedIndex);
		steel.add(Item.bootsSteel.shiftedIndex);
		steel.add(Item.swordSteel.shiftedIndex);
		steel.add(Item.pickaxeSteel.shiftedIndex);
		steel.add(Item.axeSteel.shiftedIndex);
		steel.add(Item.shovelSteel.shiftedIndex);
		steel.add(Item.shears.shiftedIndex);

		gold.add(Item.helmetGold.shiftedIndex);
		gold.add(Item.plateGold.shiftedIndex);
		gold.add(Item.legsGold.shiftedIndex);
		gold.add(Item.bootsGold.shiftedIndex);
		gold.add(Item.swordGold.shiftedIndex);
		gold.add(Item.pickaxeGold.shiftedIndex);
		gold.add(Item.axeGold.shiftedIndex);
		gold.add(Item.shovelGold.shiftedIndex);
	
		diamond.add(Item.helmetDiamond.shiftedIndex);
		diamond.add(Item.plateDiamond.shiftedIndex);
		diamond.add(Item.legsDiamond.shiftedIndex);
		diamond.add(Item.bootsDiamond.shiftedIndex);
		diamond.add(Item.swordDiamond.shiftedIndex);
		diamond.add(Item.pickaxeDiamond.shiftedIndex);
		diamond.add(Item.axeDiamond.shiftedIndex);
		diamond.add(Item.shovelDiamond.shiftedIndex);
		
		wood.add(Item.pickaxeWood.shiftedIndex);
		wood.add(Item.axeWood.shiftedIndex);
		wood.add(Item.shovelWood.shiftedIndex);
		wood.add(Item.hoeWood.shiftedIndex);
		wood.add(Item.swordWood.shiftedIndex);
	
		stone.add(Item.pickaxeStone.shiftedIndex);
		stone.add(Item.axeStone.shiftedIndex);
		stone.add(Item.shovelStone.shiftedIndex);
		stone.add(Item.hoeStone.shiftedIndex);
		stone.add(Item.swordStone.shiftedIndex);

		string.add(Item.fishingRod.shiftedIndex);
		string.add(Item.bow.shiftedIndex);
		
		chain.add(Item.helmetChain.shiftedIndex);
		chain.add(Item.plateChain.shiftedIndex);
		chain.add(Item.legsChain.shiftedIndex);
		chain.add(Item.bootsChain.shiftedIndex);
		
		//shovel
		shovel.add(Item.shovelDiamond.shiftedIndex);
		shovel.add(Item.shovelStone.shiftedIndex);
		shovel.add(Item.shovelWood.shiftedIndex);
		shovel.add(Item.shovelGold.shiftedIndex);
		shovel.add(Item.shovelSteel.shiftedIndex);
		
		//sword
		sword.add(Item.swordDiamond.shiftedIndex);
		sword.add(Item.swordStone.shiftedIndex);
		sword.add(Item.swordWood.shiftedIndex);
		sword.add(Item.swordGold.shiftedIndex);
		sword.add(Item.swordSteel.shiftedIndex);
		
		//axe
		axe.add(Item.axeDiamond.shiftedIndex);
		axe.add(Item.axeStone.shiftedIndex);
		axe.add(Item.axeWood.shiftedIndex);
		axe.add(Item.axeGold.shiftedIndex);
		axe.add(Item.axeSteel.shiftedIndex);
		
		//pick
		pickaxe.add(Item.pickaxeDiamond.shiftedIndex);
		pickaxe.add(Item.pickaxeStone.shiftedIndex);
		pickaxe.add(Item.pickaxeWood.shiftedIndex);
		pickaxe.add(Item.pickaxeGold.shiftedIndex);
		pickaxe.add(Item.pickaxeSteel.shiftedIndex);
		
		//hoe
		hoe.add(Item.hoeDiamond.shiftedIndex);
		hoe.add(Item.hoeStone.shiftedIndex);
		hoe.add(Item.hoeWood.shiftedIndex);
		hoe.add(Item.hoeGold.shiftedIndex);
		hoe.add(Item.hoeSteel.shiftedIndex);
		
		//helmet
		helmet.add(Item.helmetDiamond.shiftedIndex);
		helmet.add(Item.helmetChain.shiftedIndex);
		helmet.add(Item.helmetLeather.shiftedIndex);
		helmet.add(Item.helmetGold.shiftedIndex);
		helmet.add(Item.helmetSteel.shiftedIndex);
		
		//plate
		chestplate.add(Item.plateDiamond.shiftedIndex);
		chestplate.add(Item.plateChain.shiftedIndex);
		chestplate.add(Item.plateLeather.shiftedIndex);
		chestplate.add(Item.plateGold.shiftedIndex);
		chestplate.add(Item.plateSteel.shiftedIndex);
		
		//leggings
		leggings.add(Item.legsDiamond.shiftedIndex);
		leggings.add(Item.legsChain.shiftedIndex);
		leggings.add(Item.legsLeather.shiftedIndex);
		leggings.add(Item.legsGold.shiftedIndex);
		leggings.add(Item.legsSteel.shiftedIndex);
		
		//boots
		boots.add(Item.bootsDiamond.shiftedIndex);
		boots.add(Item.bootsChain.shiftedIndex);
		boots.add(Item.bootsLeather.shiftedIndex);
		boots.add(Item.bootsGold.shiftedIndex);
		boots.add(Item.bootsSteel.shiftedIndex);
	}
	
	public static boolean isRepairable(int i)
	{
		return isLeatherBase(i) || isSteelBase(i) || isGoldBase(i) || isDiamondBase(i) || isStringBase(i) || isWoodBase(i) || isStoneBase(i) || isChainBase(i);
	}
	
	public static ArrayList<Integer> leather = new ArrayList<Integer>();
	public static boolean isLeatherBase(int item)
	{
		return leather.contains(new Integer(item));
	}
	
	public static ArrayList<Integer> steel = new ArrayList<Integer>();
	public static boolean isSteelBase(int item)
	{
		return steel.contains(new Integer(item));
	}
	
	public static ArrayList<Integer> gold = new ArrayList<Integer>();
	public static boolean isGoldBase(int item)
	{
		return gold.contains(new Integer(item));
	}
	
	public static ArrayList<Integer> diamond = new ArrayList<Integer>();
	public static boolean isDiamondBase(int item)
	{
		return diamond.contains(new Integer(item));
	}
	
	public static ArrayList<Integer> wood = new ArrayList<Integer>();
	public static boolean isWoodBase(int item)
	{
		return wood.contains(new Integer(item));
	}
	
	public static ArrayList<Integer> stone = new ArrayList<Integer>();
	public static boolean isStoneBase(int item)
	{
		return stone.contains(new Integer(item));
	}
	
	public static ArrayList<Integer> string = new ArrayList<Integer>();
	public static boolean isStringBase(int item)
	{
		return string.contains(new Integer(item));
	}

	public static ArrayList<Integer> chain = new ArrayList<Integer>();
	public static boolean isChainBase(int item)
	{
		return chain.contains(new Integer(item));
	}
	
	public static ArrayList<Integer> shovel = new ArrayList<Integer>();
	public static boolean isShovel(int item)
	{
		return shovel.contains(new Integer(item));
	}
	
	public static ArrayList<Integer> sword = new ArrayList<Integer>();
	public static boolean isSword(int item)
	{
		return sword.contains(new Integer(item));
	}
	
	public static ArrayList<Integer> pickaxe = new ArrayList<Integer>();
	public static boolean isPick(int item)
	{
		return pickaxe.contains(new Integer(item));
	}
	
	public static ArrayList<Integer> axe = new ArrayList<Integer>();
	public static boolean isAxe(int item)
	{
		return axe.contains(new Integer(item));
	}
	
	public static ArrayList<Integer> hoe = new ArrayList<Integer>();
	public static boolean isHoe(int item)
	{
		return hoe.contains(new Integer(item));
	}
	
	public static ArrayList<Integer> helmet = new ArrayList<Integer>();
	public static boolean isHelmet(int item)
	{
		return helmet.contains(new Integer(item));
	}
	
	public static ArrayList<Integer> chestplate = new ArrayList<Integer>();
	public static boolean isChestplate(int item)
	{
		return chestplate.contains(new Integer(item));
	}
	
	public static ArrayList<Integer> leggings = new ArrayList<Integer>();
	public static boolean isLeggings(int item)
	{
		return leggings.contains(new Integer(item));
	}
	
	public static ArrayList<Integer> boots = new ArrayList<Integer>();
	public static boolean isBoots(int item)
	{
		return boots.contains(new Integer(item));
	}
	
	/**
	 * Repairs the player's current item. Assumes the repair item has already been consumed. Bases repair off of how many of material was used to construct it.
	 * @param levelAddedRepairFactor Extra percent damage to repair
	 */
	public static void repairCurrentItem(float levelAddedRepairFactor)//assumes item already consumed
	{
		if(levelAddedRepairFactor == 0)
			levelAddedRepairFactor = (float)Math.sqrt(Stat.getSkillLevel("Repair") / 10);
		
		int curDamage = mc.thePlayer.inventory.getCurrentItem().getItemDamage();
		int maxDamage = mc.thePlayer.inventory.getCurrentItem().getMaxDamage();
		int itemID = mc.thePlayer.inventory.getCurrentItem().itemID;
		byte itemMaterialCount = 1;
		byte xp = 0;
		
		if(itemID == Item.fishingRod.shiftedIndex || itemID == Item.shears.shiftedIndex)
			itemMaterialCount = 2;
		else if(itemID == Item.bow.shiftedIndex)
			itemMaterialCount = 3;
		else if(isPick(itemID))
			itemMaterialCount = 3;
		else if(isShovel(itemID))
			itemMaterialCount = 1;
		else if(isHoe(itemID))
			itemMaterialCount = 2;
		else if(isAxe(itemID))
			itemMaterialCount = 3;
		else if(isSword(itemID))
			itemMaterialCount = 2;
		else if(isHelmet(itemID))
			itemMaterialCount = 5;
		else if(isChestplate(itemID))
			itemMaterialCount = 8;
		else if(isLeggings(itemID))
			itemMaterialCount = 7;
		else if(isBoots(itemID))
			itemMaterialCount = 4;
		
		int repairAmount = (int) (maxDamage / itemMaterialCount * (1 + levelAddedRepairFactor));
		if(curDamage - repairAmount < 0)
			getPlayer().inventory.getCurrentItem().setItemDamage(0);
		else
			getPlayer().inventory.getCurrentItem().setItemDamage(curDamage - repairAmount);
		
		String string;
		switch(rand.nextInt(7))
		{
		case 0 : string = "Reparo!"; break;
		case 1 : string = "Abracadabra!"; break;
		case 2 : string = "Voila!"; break;
		case 3 : string = "Dun dun DUN!!!"; break;
		case 4 : string = "Yes he can!"; break;
		case 5 : string = "Episkey!"; break;
		case 6 : string = "BANG!"; break;
		default: string = "";
		}
		
		if(isWoodBase(itemID))
			xp = 5;
		else if(isLeatherBase(itemID))
			xp = 5;
		else if(isStringBase(itemID))
			xp = 8;
		else if(isStoneBase(itemID))
			xp = 15;
		else if(isSteelBase(itemID))
			xp = 30;
		else if(isGoldBase(itemID))
			xp = 40;
		else if(isDiamondBase(itemID))
			xp = 60;
		else if(isChainBase(itemID))
			xp = 80;
		sendChat(string, "lime");
		Stat.addExperience("Repair", xp+repairAmount/5000);//TODO verify that this repair amount for skill is not too high
	}
	
	public static EntityPlayerMP getPlayer()
	{
		return (EntityPlayerMP)getWorldObj().playerEntities.get(getWorldObj().playerEntities.indexOf(mc.thePlayer));
	}
	
	public static boolean isCurrentCropDone()
	{
		if(!mc.theWorld.isRemote && !mc.thePlayer.capabilities.isCreativeMode)
		{
			//TODO fix?
			//mc.playerController.getCurrentBlock().getBlockMeta();
	//		PlayerControllerSP playerController = (PlayerControllerSP)mc.playerController;
	//		if(playerController.getCurrentBlockMetadata() == 7)
	//			return true;
		}
		return false;
	}
	
	public static void addStat(int b)
	{
	if(mc.isSingleplayer())
	{
		ItemStack heldItem = mc.thePlayer.inventory.getCurrentItem();
		int heldItemID = -1;
		if(heldItem != null)
		{
			heldItemID = heldItem.itemID;
		}
		
		if(b == Block.wood.blockID && isAxe(heldItemID))
		{//TODO properly label the log types for future reference
			/*PlayerControllerSP playerController = (PlayerControllerSP)mc.playerController;
			int metadata = playerController.getCurrentBlockMetadata();
			if (metadata == 1)
			{
				Stat.addExperience("Woodcutting", 5);
				//	return 116;
			}

			else if (metadata == 2)
			{//birch (or aspen)
				Stat.addExperience("Woodcutting", 10);
				//	return 117;
			}

			else if(metadata == 3)
			{
				Stat.addExperience("Woodcutting", 15);	
			}
			
			else//normal wood (lighter brown wood)
			{*/
				Stat.addExperience("Woodcutting", ExperienceInfo.getXPInfo(b).getXP());
			//}
			
			//return metadata != 3 ? 20 : 153;
		}
//		else if(b == Block.melon.blockID || b == Block.crops.blockID)//b == Item.wheat.shiftedIndex)
//		{
//			if(b == Block.crops.blockID)
//			{
//				if(isCurrentCropDone())
//					Stat.addExperience("Harvesting", ExperienceInfo.getXPInfo(b).getXP());
//			}
//			else
//				Stat.addExperience("Harvesting", ExperienceInfo.getXPInfo(b).getXP());
//		}
		else if(b == Block.blockSteel.blockID)
		{
			if(isRepairable(heldItemID) && heldItem.getItemDamage() > 0)
			{
				int curDamage = heldItem.getItemDamage();
				int maxDamage = heldItem.getMaxDamage();
				long skillLevel = Stat.getSkillLevel("Repair");
				if(curDamage == 0)
				{
					sendChat("This item is not damaged, but try again anyway. I'm sure it will work.");
					return;
				}
				
				if(isWoodBase(heldItemID))
				{
					if(skillLevel < REPAIR_WOOD)
					{
						sendChat("Your repair skill must be level "+REPAIR_WOOD+" to repair with wood.", "red");
						sendChat("Your current Repair Level: "+skillLevel, "lightred");
						return;
					}
					
					if(mc.thePlayer.inventory.hasItem(Block.planks.blockID))
					{
						getPlayer().inventory.consumeInventoryItem(Block.planks.blockID);
						repairCurrentItem(0);
					}
					else
						sendChat("You do not enough planks.", "red");
				}
				else if(isLeatherBase(heldItemID))
				{
					if(skillLevel < REPAIR_WOOD)
					{
						sendChat("Your repair skill must be level "+REPAIR_LEATHER+" to repair with leather.", "red");
						sendChat("Your current Repair Level: "+skillLevel, "lightred");
						return;
					}
					
					if(mc.thePlayer.inventory.hasItem(Item.leather.shiftedIndex))
					{
						getPlayer().inventory.consumeInventoryItem(Item.leather.shiftedIndex);
						repairCurrentItem(0);
					}
					else
					{
						sendChat("You do not enough leather.", "red");
					}
				}
				else if(isStringBase(heldItemID))
				{
					if(skillLevel < REPAIR_STRING)
					{
						sendChat("Your repair skill must be level "+REPAIR_STRING+" to repair with string.", "red");
						sendChat("Your current Repair Level: "+skillLevel, "lightred");
						return;
					}
					
					if(mc.thePlayer.inventory.hasItem(Item.silk.shiftedIndex))
					{
						getPlayer().inventory.consumeInventoryItem(Item.silk.shiftedIndex);
						repairCurrentItem(0);
					}
					else
					{
						sendChat("You do not enough string.", "red");
					}
				}
				else if(isStoneBase(heldItemID))
				{
					if(skillLevel < REPAIR_STONE)
					{
						sendChat("Your repair skill must be level "+REPAIR_STONE+" to repair with cobblestone.", "red");
						sendChat("Your current Repair Level: "+skillLevel, "lightred");
						return;
					}
					if(mc.thePlayer.inventory.hasItem(Block.cobblestone.blockID))
					{
						getPlayer().inventory.consumeInventoryItem(Block.cobblestone.blockID);
						repairCurrentItem(0);
					}
					else
					{
						sendChat("You do not enough cobblestone.", "red");
					}
				}
				else if(isSteelBase(heldItemID))
				{
					if(skillLevel < REPAIR_STEEL)
					{
						sendChat("Your repair skill must be level "+REPAIR_STEEL+" to repair with iron.", "red");
						sendChat("Your current Repair Level: "+skillLevel, "lightred");
						return;
					}
					
					if(mc.thePlayer.inventory.hasItem(Item.ingotIron.shiftedIndex))
					{
						getPlayer().inventory.consumeInventoryItem(Item.ingotIron.shiftedIndex);
						repairCurrentItem(0);
					}
					else
					{
						sendChat("You do not enough iron ingots.", "red");
					}
				}
				
				else if(isGoldBase(heldItemID))
				{
					if(skillLevel < REPAIR_GOLD)
					{
						sendChat("Your repair skill must be level "+REPAIR_GOLD+" to repair with gold.", "red");
						sendChat("Your current Repair Level: "+skillLevel, "lightred");
						return;
					}
					
					if(mc.thePlayer.inventory.hasItem(Item.ingotGold.shiftedIndex))
					{
						getPlayer().inventory.consumeInventoryItem(Item.ingotGold.shiftedIndex);
						repairCurrentItem(0);
					}
					else
						sendChat("You do not enough gold ingots.", "red");
				}
				
				else if(isDiamondBase(heldItemID))
				{
					if(skillLevel < REPAIR_DIAMOND)
					{
						sendChat("Your repair skill must be level "+REPAIR_DIAMOND+" to repair with diamond.", "red");
						sendChat("Your current Repair Level: "+skillLevel, "lightred");
						return;
					}
					
					if(mc.thePlayer.inventory.hasItem(Item.diamond.shiftedIndex))
					{
						getPlayer().inventory.consumeInventoryItem(Item.diamond.shiftedIndex);
						repairCurrentItem(0);
					}
					else
						sendChat("You do not enough diamonds.", "red");
				}
				
				else if(isChainBase(heldItemID))
				{
					if(skillLevel < REPAIR_CHAIN)
					{
						sendChat("Your repair skill must be level "+REPAIR_CHAIN+" to repair with fire blocks.", "red");
						sendChat("Your current Repair Level: "+skillLevel, "lightred");
						return;
					}
					
					if(mc.thePlayer.inventory.hasItem(Block.fire.blockID))
					{
						getPlayer().inventory.consumeInventoryItem(Block.fire.blockID);
						repairCurrentItem(0);
					}
					else
						sendChat("You do not enough fire blocks.", "red");
				}
			}
		}
		else if(isPick(heldItemID))
		{
			if(ExperienceInfo.getXPInfo(b) != null && ExperienceInfo.getXPInfo(b).getType().equalsIgnoreCase("Mining"))
				Stat.addExperience("Mining", ExperienceInfo.getXPInfo(b).getXP());
		}
		else if(isShovel(heldItemID))
		{
			if(ExperienceInfo.getXPInfo(b) != null && ExperienceInfo.getXPInfo(b).getType().equalsIgnoreCase("Digging"))
				Stat.addExperience("Digging", ExperienceInfo.getXPInfo(b).getXP());
		}
		else if(heldItemID == Item.fishingRod.shiftedIndex && mc.thePlayer.fishEntity != null)
		{
			Stat.addExperience("Fishing", 5);
		}
	}
	}

	public static Item pickFishingItem()
	{
		Item item;
		int randLevel = (int)randLong(rand, (long)Math.sqrt(randLong(rand, Stat.getSkillLevel("Fishing")+1))+1);
		int xp = 0;
		switch(randLevel)
		{//Higher up = more likely	>>>	XP
		case 0 : item = Item.bone; xp = 20; break;
		case 1 : item = Item.arrow; xp = 20; break;
		case 2 : item = Item.brick; xp = 20; break;
		case 3 : item = Item.glassBottle; xp = 20; break;
		case 4 : item = Item.bowlEmpty; xp = 20; break;
		case 5 : item = Item.goldNugget; xp = 20; break;
		case 6 : item = Item.feather; xp = 20; break;
		case 7 : item = Item.shovelWood; xp = 20; break;
		case 8 : item = Item.map; xp = 20; break;
		case 9 : item = Item.leather; xp = 20; break;
		case 10 : item = Item.rottenFlesh; xp = 20; break;
		case 11 : item = Item.paper; xp = 20; break;
		case 12 : item = Item.slimeBall; xp = 20; break;
		case 13 : item = Item.stick; xp = 20; break;
		case 14 : item = Item.boat; xp = 20; break;
		case 15 : item = Item.expBottle; xp = 20; break;
		case 16 : item = Item.reed; xp = 20; break;
		case 17 : item = Item.cookie; xp = 20; break;
		case 18 : item = Item.flint; xp = 20; break;
		case 19 : item = Item.gunpowder; xp = 20; break;
		case 20 : item = Item.goldNugget; xp = 20; break;
		case 21 : item = Item.hoeWood; xp = 20; break;
		case 22 : item = Item.painting; xp = 20; break;
		case 23 : item = Item.bootsLeather; xp = 20; break;
		case 24 : item = Item.netherStalkSeeds; xp = 20; break;
		case 25 : item = Item.pumpkinSeeds; xp = 20; break;
		case 26 : item = Item.appleRed; xp = 20; break;
		case 27 : item = Item.bucketEmpty; xp = 20; break;
		case 28 : item = Item.potion; xp = 20; break;
		case 29 : item = Item.cauldron; xp = 20; break;
		case 30 : item = Item.coal; xp = 20; break;
		case 31 : item = Item.clay; xp = 20; break;
		case 32 : item = Item.doorWood; xp = 20; break;
		case 33 : item = Item.book; xp = 20; break;
		case 34 : item = Item.helmetLeather; xp = 20; break;
		case 35 : item = Item.bow; xp = 20; break;
		case 36 : item = Item.hoeStone; xp = 20; break;
		case 37 : item = Item.compass; xp = 20; break;
		case 38 : item = Item.bucketWater; xp = 20; break;
		case 39 : item = Item.legsLeather; xp = 20; break;
		case 40 : item = Item.pickaxeWood; xp = 20; break;
		/*case 41 : item = Item.diamond; xp = 20; break;
		case 42 : item = Item.diamond; xp = 20; break;
		case 43 : item = Item.diamond; xp = 20; break;
		case 44 : item = Item.diamond; xp = 20; break;
		case 45 : item = Item.diamond; xp = 20; break;
		case 46 : item = Item.diamond; xp = 20; break;
		case 47 : item = Item.diamond; xp = 20; break;
		case 48 : item = Item.diamond; xp = 20; break;
		case 49 : item = Item.diamond; xp = 20; break;
		case 50 : item = Item.diamond; xp = 20; break;
		case 51 : item = Item.diamond; xp = 20; break;
		case 52 : item = Item.diamond; xp = 20; break;
		case 53 : item = Item.diamond; xp = 20; break;
		case 54 : item = Item.diamond; xp = 20; break;
		case 55 : item = Item.diamond; xp = 20; break;
		case 56 : item = Item.diamond; xp = 20; break;
		case 57 : item = Item.diamond; xp = 20; break;
		case 58 : item = Item.diamond; xp = 20; break;
		case 59 : item = Item.diamond; xp = 20; break;
		case 60 : item = Item.diamond; xp = 20; break;
		case 61 : item = Item.diamond; xp = 20; break;
		case 62 : item = Item.diamond; xp = 20; break;
		case 63 : item = Item.diamond; xp = 20; break;
		case 64 : item = Item.diamond; xp = 20; break;
		case 65 : item = Item.diamond; xp = 20; break;
		case 66 : item = Item.diamond; xp = 20; break;
		case 67 : item = Item.diamond; xp = 20; break;
		case 68 : item = Item.diamond; xp = 20; break;
		case 69 : item = Item.diamond; xp = 20; break;
		case 70 : item = Item.diamond; xp = 20; break;
		case 71 : item = Item.diamond; xp = 20; break;
		case 72 : item = Item.diamond; xp = 20; break;
		case 73 : item = Item.diamond; xp = 20; break;
		case 74 : item = Item.diamond; xp = 20; break;
		case 75 : item = Item.diamond; xp = 20; break;
		case 76 : item = Item.diamond; xp = 20; break;
		case 77 : item = Item.diamond; xp = 20; break;
		case 78 : item = Item.diamond; xp = 20; break;
		case 79 : item = Item.diamond; xp = 20; break;
		case 80 : item = Item.diamond; xp = 20; break;
		case 81 : item = Item.diamond; xp = 20; break;
		case 82 : item = Item.diamond; xp = 20; break;
		case 83 : item = Item.diamond; xp = 20; break;
		case 84 : item = Item.diamond; xp = 20; break;
		case 85 : item = Item.diamond; xp = 20; break;
		case 86 : item = Item.diamond; xp = 20; break;
		case 87 : item = Item.diamond; xp = 20; break;
		case 88 : item = Item.diamond; xp = 20; break;
		case 89 : item = Item.diamond; xp = 20; break;
		case 90 : item = Item.diamond; xp = 20; break;
		case 91 : item = Item.diamond; xp = 20; break;
		case 92 : item = Item.diamond; xp = 20; break;
		case 93 : item = Item.diamond; xp = 20; break;
		case 94 : item = Item.diamond; xp = 20; break;
		case 95 : item = Item.diamond; xp = 20; break;
		case 96 : item = Item.diamond; xp = 20; break;
		case 97 : item = Item.diamond; xp = 20; break;
		case 98 : item = Item.diamond; xp = 20; break;
		case 99 : item = Item.diamond; xp = 20; break;
		case 100 : item = Item.diamond; xp = 20; break;
		//	*/
		default: item = null;
		}
		Stat.addExperience("Fishing", xp);
		return item;
	}
	
	/**
	 * Performs two random functions on the given Integer. Prevents random errors by checking value > 0 and adds 1 to the value each time before randomizing.
	 * @param num The number to randomize twice.
	 * @return The randomized result.
	 */
	public static int doubleRand(int num)
	{
		return num > 0 ? rand.nextInt(rand.nextInt(num+1)+1) : 0;
	}

	public static long getStatCurve(long skillLevel)
	{
		if(skillLevel < 0)
			return 0;
		return (long)((Math.pow(skillLevel, 6.0/5)*4 + 80)*getExtraDifficultyMultiplier());
		//											*10/5
		//return (long)(Math.pow(skillLevel, 6.0/5)	*2)+80;
	}
	
	public static double getExtraDifficultyMultiplier()
	{
		return extraDifficulty / 100.0 + 1;
	}

	public static long curveRand(long i)
	{
		return randLong(rand, i+1) / 2;
	}
	
	public static long randLong(Random rand, long num)
	{
		long bits, val;
		do
		{
			bits = (rand.nextLong() << 1) >>> 1;
			val = bits % num;
		}while(bits-val+(num-1) < 0L);
		return val;
	}
	
	public static long randLong(long num)
	{
		return randLong(rand, num);
	}

	public static int onArrowHit(MovingObjectPosition movingobjectposition, int damage, EntityArrow arrow)
	{
		if(movingobjectposition.entityHit instanceof EntityLiving && arrow.shootingEntity instanceof EntityPlayer)
		{
			EntityLiving e = (EntityLiving)movingobjectposition.entityHit;
			int i = rand.nextInt(e.getMaxHealth() + 1)/5;
			damage *= randLong(Stat.getLevel("Archery")+1)/200.0+1;
			Stat.addExperience("Archery", damage/5+1);
		}
		return damage;
	}
	
	
	/**
	 * When a block is damaged, this method is called and returns the damage boost the tool performs.
	 * @param x
	 * @param y
	 * @param z
	 * @param side
	 * @param blockID
	 * @param block
	 * @return Bonus damage done to the block
	 */
	public static float onDamageBlock(int x, int y, int z, int side, int blockID, Block block)
	{
		float boost = 0f;
		if(mc.thePlayer.isInsideOfMaterial(Material.water))
			boost += getSwimmingSpeedBoost();
		boost += getSpeedBoost(blockID);
		if(boost >= 100F)
		{
			destroyAndDropBlock(getPlayer().getCurrentEquippedItem(), block, x, y, z);
			boost = 0;
		}
		return boost > 0 ? boost : 0f;
	}
	
	public static void onCatchFish(World worldObj, double posX, double posY, double posZ, double motionX, double motionY, double motionZ)
	{
		long fishNum = Stat.getSkillLevel("Fishing") / 10;
    	if(rand.nextInt(200) == 0)
    	{
    		EntitySquid squid = new EntitySquid(getWorldObj());
    		squid.setPosition(posX, posY+3, posZ);
    		squid.motionX = motionX;// < 0 ? motionX - .5 : motionX + .5;
    		squid.motionY = .25;
    		squid.motionZ = motionX;// < 0 ? motionZ - .5 : motionZ + .5;
    		getWorldObj().spawnEntityInWorld(squid);
    	}
    	else
    	{
    		Item itemToDrop = pickFishingItem();
	    	if(itemToDrop != null)
	    		spawnItemWithMotion(itemToDrop, posX, posY, posZ, motionX, motionY, motionZ);
	    	long randFish = fishNum > 1 ? mod_mcStats.randLong(fishNum) : 1;
	    	Stat.addExperience("Fishing", 5*fishNum);
	    	for(int i = 0; i < 3; i++)
	    		spawnItemWithMotion(Item.fishRaw, posX, posY, posZ, motionX, motionY, motionZ);
	    }

	}
	
	private static byte blockPlaceCounter = 0;
	
	public static boolean onRightClick(EntityPlayer entityplayer, World world, ItemStack itemstack, int x, int y, int z, int side, Vec3 vector, boolean blockActivated, float meta1, float meta2, float meta3)
	{
		int blockID = world != null ? world.getBlockId(x, y, z) : 0;
		if(blockID == Block.blockSteel.blockID)
		{
			addStat(blockID);
			return true;
		}
		
		String skillString = itemstack != null ? getSkillNameFromTool(itemstack.itemID) : "Unarmed";
		//TODO fix?
//		if(blockPlaceCounter <= 0 && !isSwitchableWithHand() && !blockActivated)
//			doAbilityEvent(skillString);
//		else
//			blockPlaceCounter--;
		
		if(itemstack != null && itemstack.getItem() instanceof ItemBlock)//world.canPlaceEntityOnSide(blockID, x, y, z, blockActivated, side, entityplayer))//itemstack.tryPlaceItemIntoWorld(entityplayer, world, x, y, z, side, meta1, meta2, meta3))
		{
			BlockCoord.addCoordinatesIfNotDuplicate(x, y, z, side);
            blockPlaceCounter = 5;
            return false;
		}
		return false;
	}
	
	/**
	 * When falling, the damage is sent through this and then used for statistics by Minecraft and applying fall damage.
	 * @param distance The distance fallen.
	 * @param damage2 
	 * @return The amount of fall damage after recalculation. Should not be less than 0.
	 */
	public static float onFall(float distance)
	{
		int damageReduced = 0;//MathHelper.ceiling_float_int(distance - 3.0F);
		Stat.addExperience("Acrobatics", (int)((distance-3f)*10));
		damageReduced += Stat.getLevel("Acrobatics")/15;
		//mc.thePlayer.fallDistance = mc.thePlayer.fallDistance > damageReduced ? mc.thePlayer.fallDistance - damageReduced : 0;
		return distance - damageReduced;
	}
	
	private static byte inWaterCounter = 0;
	private static byte counter1 = 0;
	private static final byte waterMovementTicksSkipped = 8;
	public static void onMove(double motionX, double motionY, double motionZ)
	{
		if(mc.isSingleplayer() && inWaterCounter > 20 && mc.thePlayer != null)
		{
			if(mc.thePlayer.isInsideOfMaterial(Material.water))
			{
				int i = Math.round(MathHelper.sqrt_double(motionX * motionX + motionY * motionY + motionZ * motionZ) * 100F);
				//System.out.println(i);
				if(counter1 > waterMovementTicksSkipped && i - 8 > 0)
				{
					Stat.addExperience("Swimming", i-8);
					counter1 = 0;
				}
			}
			else if(mc.thePlayer.isInWater())
			{
				int j = Math.round(MathHelper.sqrt_double(motionX * motionX + motionZ * motionZ) * 100F);
				//System.out.println(j);
				if(counter1 > waterMovementTicksSkipped && j-10 > 0)
				{
					Stat.addExperience("Swimming", j-10);
					counter1 = 0;
				}
			}
			counter1++;
		}
		if(inWaterCounter > 20 && !(mc.thePlayer.isInWater() || mc.thePlayer.isInsideOfMaterial(Material.water)))
			inWaterCounter = 0;
		else
			inWaterCounter++;
	}
	
	public static void onInteractWithEntity(Entity entity)
	{
		if(mc.thePlayer.getCurrentEquippedItem() != null && isAxe(mc.thePlayer.getCurrentEquippedItem().itemID))
			TimedEvent.addEvent("Woodcutting", ABILITY_WAIT_TIME);
		if(entity instanceof EntityAnimal && !(entity instanceof EntityTameable) && mc.thePlayer.getCurrentEquippedItem() != null)
		{
			if(!((EntityAnimal)entity).isInLove() && mc.thePlayer.getCurrentEquippedItem().itemID == Item.wheat.shiftedIndex)
				Stat.addExperience("Taming", 5);
		}
		else if(entity instanceof EntityTameable && mc.thePlayer.getCurrentEquippedItem() != null)
		{
			if(!((EntityAnimal)entity).isInLove() && mc.thePlayer.getCurrentEquippedItem().itemID == Item.bone.shiftedIndex)
				Stat.addExperience("Taming", 5);
		}
		//TODO fix?
//		else if(mc.thePlayer.getCurrentEquippedItem() == null && !mod_mcStats.isSwitchableWithHand(mc.playerController.getCurrentBlock().getBlockID())
//				&& mc.playerController.getCurrentBlock().getBlockID() != Block.blockSteel.blockID)
//		{
//			TimedEvent.addEvent("Unarmed", ABILITY_WAIT_TIME);
//		}
	}
	
	public static void onAttack(EntityPlayer entityplayer, Entity entity)
	{
		if(entity instanceof EntityLiving && ((EntityLiving)entity).hurtTime < 3)
		{
			if(entityplayer.getCurrentEquippedItem() == null)
			{
				byte id = Stat.getSkillId("Unarmed");
				long skill = Stat.getLevel(id);
				if(TimedEvent.active("Unarmed"))
					TimedEvent.addEvent("UnarmedOn", ABILITY_WAIT_TIME);
				if(TimedEvent.active("UnarmedOn"))
				{
					long randSkill = randLong(randLong(skill / 50 + 1)+1);
					for(long i = 0; i < randSkill; i++)
					{
						entityplayer.attackTargetEntityWithCurrentItem(entity);
						mc.effectRenderer.addEffect(new EntityCrit2FX(mc.theWorld, entity));
						Stat.addExperience(id, 8);
					}
				}
				else if(skill >= EXTRA_COMBAT_DAMAGE && skill >= mod_mcStats.CRITICAL_HIT)//0 is level before can critical hit
				{
					long randSkill = randLong(randLong(skill / 50 + 1)+1);
					for(long i = 0; i < randSkill; i++)
					{
						entityplayer.attackTargetEntityWithCurrentItem(entity);
						mc.effectRenderer.addEffect(new EntityCrit2FX(mc.theWorld, entity));
					}
					if(randSkill > 0)
					{
						sendChat("Critical Hit!", "yellow");
						Stat.addExperience(id, 30);
					}
					else
					{
						Stat.addExperience(id, 5);
					}
				}
				else
					Stat.addExperience(id, 5);
				
				//disarm//DO NOT DELETE YET
				/*if(mod_mcStats.randLong(skill+1) > mod_mcStats.PUNCH_MOB_DROPS_ITEM)
				{
				//	TODO drop items
					ItemStack[] itemstack = ((EntityLiving)entity).getLastActiveItems();
					if(itemstack != null && itemstack[0] != null)
						entity.dropItem(itemstack[0].itemID, 1);
					entity.onKillEntity((EntityLiving)entity);
				}*/
			}
			else //can assume current item is not null
			{
				int curItem = entityplayer.getCurrentEquippedItem().itemID;
				boolean isAxe = isAxe(curItem);
				if(isAxe || isSword(curItem))
				{
					String curSkillString;
					
					if(isAxe)
						curSkillString = "Axe";
					else
						curSkillString = "Sword";
					
					if(isAxe && TimedEvent.active("Woodcutting"))
					{
						TimedEvent.addEvent("AxeOn", getAbilityTime("Axe"));
					}
					else if(!isAxe && TimedEvent.active("Sword"))
					{
						TimedEvent.addEvent("SwordOn", getAbilityTime("Sword"));
					}
					
					if(isAxe)
					{
						if(TimedEvent.active("AxeOn"))
						{
							//TODO fix?
//							if(Stat.getLevel("Axe") >= EXTRA_COMBAT_DAMAGE)
//								((EntityLiving)entity).damageEntity(new DamageSource("player"), (int)(Stat.getLevel("Axe")*1.75/25));
							mc.effectRenderer.addEffect(new EntityCrit2FX(mc.theWorld, entity));
							Stat.addExperience("Axe", ExperienceInfo.getXPInfo(curItem).getXP()*2);
						}
						else
						{
							//TODO fix?
//							if(Stat.getLevel("Axe") >= EXTRA_COMBAT_DAMAGE)
//								((EntityLiving)entity).damageEntity(new DamageSource("player"), (int)(Stat.getLevel("Axe")/25));
							Stat.addExperience("Axe", ExperienceInfo.getXPInfo(curItem).getXP());
						}
					}
					else
					{
						if(TimedEvent.active("SwordOn"))
						{
							//TODO fix?
//							if(Stat.getLevel("Sword") >= EXTRA_COMBAT_DAMAGE)
//								((EntityLiving)entity).damageEntity(new DamageSource("player"), (int)(Stat.getLevel("Sword")*1.75/25));
							mc.effectRenderer.addEffect(new EntityCrit2FX(mc.theWorld, entity));
							Stat.addExperience("Sword", ExperienceInfo.getXPInfo(curItem).getXP()*2);
						}
						else
						{
							//TODO fix?
//							if(Stat.getLevel("Sword") >= EXTRA_COMBAT_DAMAGE)
//								((EntityLiving)entity).damageEntity(new DamageSource("player"), (int)(Stat.getLevel("Sword")/25));
							Stat.addExperience("Sword", ExperienceInfo.getXPInfo(curItem).getXP());//TODO make variable
						}
					}
				}
			}
		}
	}
	
	public static void onBreakBlock(int x, int y, int z, int side, WorldClient world, Block block, int metadata, boolean notify, int blockID)
	{
		boolean isPlaced = BlockCoord.isPlaced(x, y, z);
		if(isPlaced)
			BlockCoord.getCoordinates(x, y, z).removeSelf();
		if(block.blockID != Block.blockSteel.blockID && (!isPlaced || isCrop(block.blockID)))
		{
			if(isCrop(block.blockID))
				addCropStat(block.blockID, isPlaced, x, y, z);
			else
				addStat(block.blockID);
			if(ExperienceInfo.getXPInfo(block.blockID) != null)
			{
				String skillType = ExperienceInfo.getXPInfo(block.blockID).getType();
				long random = randLong(Stat.getLevel(skillType)+1)/50;
				if(TimedEvent.active(ExperienceInfo.getXPInfo(block.blockID).getType()+"On"))
					random *= 2.5;
				for(long i = 0; i < random; i++)
				{
					Stat.addExperience(skillType, ExperienceInfo.getXPInfo(blockID).getXP()/2);
					spawnBlock(block, metadata, x, y, z);
				}
				if(block.blockID == Block.crops.blockID && getWorldObj().getBlockMetadata(x, y, z) == 7)//if is a grown crop
				{
					if(rand.nextInt(30) == 0 || randLong(Stat.getLevel("Harvesting")+1)/10 == Stat.getLevel("Harvesting")/10)
						eventActions.add(new EventAction("growcrop", 1, x, y, z, block.blockID));
				}
			}
		}
	}
	
	public static void addCropStat(int blockID, boolean placed, int x, int y, int z)
	{
		if(blockID == Block.crops.blockID)
		{
			if(mc.theWorld.getBlockMetadata(x, y, z) == 7)
				Stat.addExperience("Harvesting", ExperienceInfo.getXPInfo(blockID).getXP());
		}
		else if(blockID == Block.cocoaPlant.blockID)
		{
			if((mc.theWorld.getBlockMetadata(x, y, z) & 12) >> 2 >= 2)
				Stat.addExperience("Harvesting", ExperienceInfo.getXPInfo(blockID).getXP());				
		}
		else if(blockID == Block.netherStalk.blockID)
		{
			if(mc.theWorld.getBlockMetadata(x, y, z) >= 3)
				Stat.addExperience("Harvesting", ExperienceInfo.getXPInfo(blockID).getXP());				
		}
		else if(blockID == Block.mushroomCapBrown.blockID || blockID == Block.mushroomCapRed.blockID)
		{
			Stat.addExperience("Harvesting", ExperienceInfo.getXPInfo(blockID).getXP());
		}
		else
		{
			if(!placed)
				Stat.addExperience("Harvesting", ExperienceInfo.getXPInfo(blockID).getXP());
		}
	}
	
	public static boolean isCrop(int blockID)
	{
		if(blockID == Block.crops.blockID || blockID == Block.cocoaPlant.blockID || blockID == Block.melon.blockID || blockID == Block.pumpkin.blockID ||
				blockID == Block.reed.blockID || blockID == Block.mushroomBrown.blockID || blockID == Block.mushroomRed.blockID ||
				blockID == Block.mushroomCapBrown.blockID || blockID == Block.mushroomCapRed.blockID || blockID == Block.netherStalk.blockID ||
				blockID == Block.cactus.blockID)
			return true;
		else
			return false;
	}

	public static void onArrowLaunch(EntityArrow arrow)
	{
		if(Stat.getLevel("Archery") >= ARCHERY_SET_FIRE && randLong(Stat.getLevel("Archery")/50+1) > ARCHERY_SET_FIRE)
			arrow.setFire(15);
		long skill = Stat.getLevel("Archery");
		arrow.motionX *= skill/20f+1;
		arrow.motionZ *= skill/20f+1;
	}
	
	public static void spawnRandomItem(Entity entity)
	{
		ItemStack itemstack = new ItemStack(Item.porkRaw);
		EntityItem entityitem2 = new EntityItem(getWorldObj(), entity.posX, entity.posY, entity.posZ, itemstack);
		entityitem2.motionX = (rand.nextFloat()-.5f)/4f;
		entityitem2.motionY = .7f;
		entityitem2.motionZ = (rand.nextFloat()-.5f)/4f;
		entityitem2.delayBeforeCanPickup = 0;
		getWorldObj().spawnEntityInWorld(entityitem2);
		System.out.println("Spawning Item");
		//entityliving.dropItem(Item.porkRaw.shiftedIndex, 2);
    }
	
	public static void spawnItem(Item item, Entity entity)
	{
		spawnItemWithMotion(item, entity.posX, entity.posY, entity.posZ, getMotion(true), getMotion(false), getMotion(true));
	}
	
	public static void spawnBlock(Block block, Entity entity)
	{
		spawnBlockWithMotion(block, entity.posX, entity.posY, entity.posZ, getMotion(true), getMotion(false), getMotion(true));
	}
	
	public static void spawnItem(Item item, double posX, double posY, double posZ)
	{
		spawnItemWithMotion(item, posX, posY, posZ, getMotion(true), getMotion(false), getMotion(true));
	}
	
	public static void spawnBlock(Block block, double posX, double posY, double posZ)
	{
		spawnBlockWithMotion(block, posX, posY, posZ, getMotion(true), getMotion(false), getMotion(true));
	}
	
	public static void spawnBlock(int blockID, int metadata, double posX, double posY, double posZ)
	{
		spawnBlockWithMotion(blockID, metadata, posX, posY, posZ, getMotion(true), getMotion(false), getMotion(true));
	}
	
	public static void spawnBlock(int blockID, double posX, double posY, double posZ)
	{
		spawnBlockWithMotion(blockID, 0, posX, posY, posZ, getMotion(true), getMotion(false), getMotion(true));
	}
	
	public static void spawnBlock(Block block, int metadata, double posX, double posY, double posZ)
	{
		spawnBlockWithMotion(block, metadata, posX, posY, posZ, getMotion(true), getMotion(false), getMotion(true));
	}
	
	public static void spawnBlock(Block block, int metadata, double posX, double posY, double posZ, int quantity)
	{
		spawnBlockWithMotion(block, metadata, posX, posY, posZ, getMotion(true), getMotion(false), getMotion(true), quantity);
	}
	
	public static double getMotion(boolean isLateralMotion)
	{
		if(isLateralMotion)
		{
			//float temp = rand.nextBoolean() ? (rand.nextFloat()-.5f)/1000 : 0f;
			//System.out.println(temp);
			return 0;//temp;
		}
		else
			return .25f;
	}

	public static void spawnBlockWithMotion(Block block, int metadata, double posX, double posY, double posZ, double motionX, double motionY, double motionZ)
	{
		boolean silkTouch = false;
		NBTTagList list = mc.thePlayer.getCurrentEquippedItem() != null ? mc.thePlayer.getCurrentEquippedItem().getEnchantmentTagList() : null;
		if(list != null)
		{
			for(byte i = 0; i < list.tagCount(); i++)
			{
				if(list != null && list.getTagName(i).contains("silk"))
				{
					System.out.println("Silk touch enchantment detected.");
					silkTouch = true;
					break;
				}
			}
		}
		
		Item itemToSpawn = null;
		Block blockToSpawn = null;
		if(silkTouch)
		{//not yet tested
			int pickID = block.idPicked(mc.theWorld, (int)posX, (int)posY, (int)posZ);
			if(pickID > 0)
			{
				if(256 > pickID)//is a block
					blockToSpawn = Block.blocksList[pickID];
				else
					itemToSpawn = Item.itemsList[pickID];
			}
		}
		else
		{
			int dropID = block.idDropped(metadata, rand, 1);
			if(dropID > 0)
			{
				if(256 > dropID)//is a block
					blockToSpawn = Block.blocksList[dropID];
				else
					itemToSpawn = Item.itemsList[dropID];
			}
		}
		ItemStack itemstack = null;
		if(itemToSpawn != null)
			itemstack = new ItemStack(itemToSpawn);
		else if(blockToSpawn != null)
			itemstack = new ItemStack(blockToSpawn);
		
		if(itemstack != null)
		{//System.out.println("Block spawn: attempting now");
			EntityItem entityitem2 = new EntityItem(getWorldObj(), posX, posY, posZ, itemstack);
			entityitem2.motionX = motionX;
			entityitem2.motionY = motionY;
			entityitem2.motionZ = motionZ;
			entityitem2.delayBeforeCanPickup = 10;
			getWorldObj().spawnEntityInWorld(entityitem2);
		}
	}
	
	public static void spawnBlockWithMotion(Block block, int metadata, double posX, double posY, double posZ, double motionX, double motionY, double motionZ, int quantity)
	{
		boolean silkTouch = false;
		NBTTagList list = mc.thePlayer.getCurrentEquippedItem() != null ? mc.thePlayer.getCurrentEquippedItem().getEnchantmentTagList() : null;
		if(list != null)
		{
			for(byte i = 0; i < list.tagCount(); i++)
			{
				if(list != null && list.getTagName(i).contains("silk"))
				{
					System.out.println("Silk touch enchantment detected.");
					silkTouch = true;
					break;
				}
			}
		}
		
		Item itemToSpawn = null;
		Block blockToSpawn = null;
		if(silkTouch)
		{//not yet tested
			int pickID = block.idPicked(mc.theWorld, (int)posX, (int)posY, (int)posZ);
			if(pickID > 0)
			{
				if(256 > pickID)//is a block
					blockToSpawn = Block.blocksList[pickID];
				else
					itemToSpawn = Item.itemsList[pickID];
			}
		}
		else
		{
			int dropID = block.idDropped(metadata, rand, 1);
			if(dropID > 0)
			{
				if(256 > dropID)//is a block
					blockToSpawn = Block.blocksList[dropID];
				else
					itemToSpawn = Item.itemsList[dropID];
			}
		}
		ItemStack itemstack = null;
		if(itemToSpawn != null)
			itemstack = new ItemStack(itemToSpawn);
		else if(blockToSpawn != null)
			itemstack = new ItemStack(blockToSpawn);
		
		if(itemstack != null)
		{//System.out.println("Block spawn: attempting now");
			EntityItem entityitem2 = new EntityItem(getWorldObj(), posX, posY, posZ, itemstack);
			entityitem2.motionX = motionX;
			entityitem2.motionY = motionY;
			entityitem2.motionZ = motionZ;
			entityitem2.delayBeforeCanPickup = 10;
			getWorldObj().spawnEntityInWorld(entityitem2);
		}
	}

	public static void spawnBlockWithMotion(Block block, double posX, double posY, double posZ, double motionX, double motionY, double motionZ)
	{
		EntityItem entityitem2 = new EntityItem(getWorldObj(), posX, posY, posZ, new ItemStack(block));
		entityitem2.motionX = motionX;
		entityitem2.motionY = motionY;
		entityitem2.motionZ = motionZ;
		entityitem2.delayBeforeCanPickup = 10;
		getWorldObj().spawnEntityInWorld(entityitem2);
	}
	
	public static void spawnBlockWithMotion(int blockID, int metadata, double posX, double posY, double posZ, double motionX, double motionY, double motionZ)
	{
		EntityItem entityitem2 = new EntityItem(getWorldObj(), posX, posY, posZ, new ItemStack(Block.blocksList[blockID], 1, metadata));
    	entityitem2.motionX = motionX;
        entityitem2.motionY = motionY;
        entityitem2.motionZ = motionZ;
        getWorldObj().spawnEntityInWorld(entityitem2);
	}
	
	public static void spawnItemWithMotion(Item item, double posX, double posY, double posZ, double motionX, double motionY, double motionZ)
	{
		ItemStack itemstack = (new ItemStack(item)).copy();
		EntityItem entityitem2 = new EntityItem(getWorldObj(), posX, posY, posZ, itemstack);
    	entityitem2.motionX = motionX;
        entityitem2.motionY = motionY;
        entityitem2.motionZ = motionZ;
        getWorldObj().spawnEntityInWorld(entityitem2);
	}
	
	private static World prevWorldObj = mc.theWorld;
	public static World getWorldObj()
	{
		if(MinecraftServer.getServer() != null && mc.thePlayer != null)
			return MinecraftServer.getServer().worldServerForDimension(mc.thePlayer.dimension);
		return prevWorldObj;
	}

	public static short shortenFishingWait(short randomWait)
	{//normally 500
		short temp = (short)(randomWait - Stat.getLevel("Fishing")/10);
		return temp > 10 ? temp : 10;
	}
	
	public static void destroyAndDropBlock(ItemStack itemstack, Block block, int x, int y, int z)
	{
		boolean silkTouch = false;
		if(itemstack != null && itemstack.isItemEnchanted())
		{
			NBTTagList enchantments = itemstack.getEnchantmentTagList();
			for(byte i = 0; i < enchantments.tagCount() && !silkTouch; i++)
			{
				if(enchantments.getTagName(i).contains("silk"))
					silkTouch = true;
			}
		}
		
		World worldObj = getWorldObj();
		int metadata = worldObj.getBlockMetadata(x, y, z);
		
		//BEGIN silk touch checks and spawning
		
		Item itemToSpawn = null;
		Block blockToSpawn = null;
		if(silkTouch)
		{//not yet tested
			int pickID = block.idPicked(mc.theWorld, x, y, z);
			if(pickID > 0)
			{
				if(256 > pickID)//is a block
					blockToSpawn = Block.blocksList[pickID];
				else
					itemToSpawn = Item.itemsList[pickID];
			}
		}
		else
		{
			int dropID = block.idDropped(worldObj.getBlockMetadata(x, y, z), rand, 1);
			if(dropID > 0)
			{
				if(256 > dropID)//is a block
					blockToSpawn = Block.blocksList[dropID];
				else
					itemToSpawn = Item.itemsList[dropID];
			}
		}
		ItemStack itemstackToSpawn = null;
		if(itemToSpawn != null)
			itemstackToSpawn = new ItemStack(itemToSpawn);
		else if(blockToSpawn != null)
			itemstackToSpawn = new ItemStack(blockToSpawn);
		
		if(itemstackToSpawn != null)
		{//System.out.println("Block spawn: attempting now");
			EntityItem entityitem2 = new EntityItem(worldObj, x, y, z, itemstackToSpawn);
//			entityitem2.motionX = motionX;
//			entityitem2.motionY = motionY;
//			entityitem2.motionZ = motionZ;
			entityitem2.delayBeforeCanPickup = 10;
			worldObj.spawnEntityInWorld(entityitem2);
			if(!BlockCoord.isPlaced(x, y, z))
			{
					addStat(itemstackToSpawn.itemID);
				if(ExperienceInfo.getXPInfo(block.blockID) != null && block.blockID != Block.leaves.blockID)
				{
					String skillType = ExperienceInfo.getXPInfo(block.blockID).getType();
					if(skillType.equals(itemstack != null ? getSkillNameFromTool(itemstack.itemID) : "Unarmed"))
					{
						long random = randLong(Stat.getLevel(skillType)+1)/50;
						if(TimedEvent.active(ExperienceInfo.getXPInfo(block.blockID).getType()+"On"))
							random *= 1.5;
						int xp = ExperienceInfo.getXPInfo(block.blockID).getXP();
						for(byte i = 0; i < random; i++)
						{
							spawnBlock(block, metadata, x, y, z);
							Stat.addExperience(skillType, xp/2);
						}
						if(itemstack != null && (int)random > 0)
							itemstack.damageItem((int)(random*rand.nextInt(3)+5), getPlayer());
					}
				}
			}
		}
		
		worldObj.setBlockWithNotify(x, y, z, 0);
	}
}

//}




/*boolean created = file.createNewFile();
raf = new RandomAccessFile(file, "rws");

if(created)
{
	for(byte i = 0; i < Stat.getLength(); i++)
	{//writes 0's for each skill and experience slot
	//	raf.writeUTF(Stat.getSkillName(i));
		raf.writeLong(0);
	//	raf.writeUTF(Stat.getSkillName(i)+"XP");
		raf.writeLong(0);
	}
}
else //write more skills if they didn't exist already
{//TODO figure out how to properly find this out
//	for(byte i = 0; i < Stat.getLength(); i++)
//	{
//		if(raf.readUTF().equals(""))
//			;
//	}
}

if(mc.theWorld != null)
{
	raf.seek(0);
	for(byte i = 0; i < Stat.getLength(); i++)
	{
		Stat.setLevel(i, raf.readLong());
		Stat.setXP(i, raf.readLong());
	}
}*/

/*LevelingProperties levelingProperties = new LevelingProperties();
if(file.createNewFile())
{
	FileOutputStream out = new FileOutputStream(file);
	for(byte i = 0; i < Stat.getLength(); i++)
	{
		levelingProperties.setProperty(Stat.getSkillName(i), "0");
		levelingProperties.setProperty(Stat.getSkillName(i)+"XP", "0");
	}
	levelingProperties.store(out, "");
	out.close();
}
else //check for skills that weren't in there already
{
	FileOutputStream out = new FileOutputStream(file);
	levelingProperties.load(new FileInputStream(file));
	String name;
	for(byte i = 0; i < Stat.getLength(); i++)
	{
		name = Stat.getSkillName(i);
		if(levelingProperties.getProperty(Stat.getSkillName(i), "-1").equals("-1"))
		{System.out.println("Adding skill "+name);
			levelingProperties.setProperty(name, "0");
			levelingProperties.setProperty(name+"XP", "0");
		}
	}
	levelingProperties.store(out, "");
	out.close();
}
if(mc.theWorld != null)
{
	levelingProperties.load(new FileInputStream(file));
	for(byte i = 0; i < Stat.getLength(); i++)
	{
		Stat.setLevel(i, Long.parseLong(levelingProperties.getProperty(Stat.getSkillName(i))));
		Stat.setXP(i, Long.parseLong(levelingProperties.getProperty(Stat.getSkillName(i)+"XP")));
	}
}

//*/

/*LevelingProperties levelingProperties = new LevelingProperties();
try
{
	//file = new File(Minecraft.getMinecraftDir().toString()+"/config/mod_mcStats.properties");
	if(file.createNewFile())
	{System.out.println("Attempting to create new file");
		FileOutputStream out = new FileOutputStream(file);
		for(byte i = 0; i < Stat.getLength(); i++)
		{
			levelingProperties.setProperty(Stat.getSkillName(i), "0");
			levelingProperties.setProperty(Stat.getSkillName(i)+"XP", "0");
		}
		levelingProperties.store(out, "Leveling Properties");
		out.close();
	}
	else// if(levelingProperties.contains("Mining"))// if(mc.theWorld != null)
	{
		FileOutputStream out = new FileOutputStream(file);
		for(byte i = 0; i < Stat.getLength(); i++)
		{
			levelingProperties.setProperty(Stat.getSkillName(i), Long.toString(Stat.getLevel(i)));
			levelingProperties.setProperty(Stat.getSkillName(i)+"XP", Long.toString(Stat.getXP(i)));
		}
		levelingProperties.store(out, "Leveling Properties");
		out.close();
	}
}
catch(IOException ioexception)
{
	ioexception.printStackTrace();
}
//*/

//Stat.addStat("Mining", "Use a pick to mine blocks such as stone, iron, coal, and diamond.; Help Menu in Progress.");
//Stat.addStat("Digging", "Use a shovel to dig blocks like dirt, sand, and gravel.; Help Menu in Progress.");
//Stat.addStat("Fishing", "Fish with a fishing pole. As if you would use something else!; Help Menu in Progress.");
//Stat.addStat("Repair", "Right click an iron block with a damaged item to repair it.; You must have the material used to make it in your inventory.");
//Stat.addStat("Harvesting", "Harvest crops like wheat and melons.; If you use a hoe you can use special abilities.");
//Stat.addStat("Swimming", "Swim around a bit... maybe try drowning.; Help Menu in Progress.");
//Stat.addStat("Taming", "Tame animals for your farm or for pets.; Help Menu in Progress.");
//Stat.addStat("Sorcery", "Make potions and enchant items.; Or make golden apples.; Help Menu in Progress.");
//Stat.addStat("Acrobatics", "Fall a distance and survive.; Help Menu in Progress.");
//Stat.addStat("Archery", "Kill mobs using a bow.; Help Menu in Progress.");
//Stat.addStat("Sword", "Kill mobs with a sword.; Help Menu in Progress.");
//Stat.addStat("Woodcutting", "Cut down trees with an axe.; Help Menu in Progress.");
//Stat.addStat("Unarmed", "Just punch some mobs!; Help Menu in Progress.");



/*for(Stat stat : Stat.statList)
{//if stat doesn't exist yet in the file
	if(!levelingProperties.contains(stat.skillLevel))
	{
		System.out.println("File doesn't contain "+stat.skillName);
		levelingProperties.setProperty(stat.skillName, "0");
		levelingProperties.setProperty(stat.skillName+"XP", "0");
	}
}*/

/*for(Stat stat : Stat.statList)
{
	if(TimedEvent.getCreated(stat.skillName) && !TimedEvent.active(stat.skillName+"Cooldown"))
		this.sendChat("You raise your "+getTool(stat.skillName)+".");
	
	if(TimedEvent.getCreated(stat.skillName+"On"))
		this.sendChat(stat.skillName+" ability activated!");
	else if(TimedEvent.getCompleted(stat.skillName+"On"))
	{
		TimedEvent.addEvent(stat.skillName+"Cooldown", getCooldown(stat.skillName));
		this.sendChat(stat.skillName+" ability wore off.");
	}
	else if(TimedEvent.getCompleted(stat.skillName) && !TimedEvent.active(stat.skillName+"On") && !TimedEvent.active(stat.skillName+"Cooldown"))
		this.sendChat("You lower your "+getTool(stat.skillName)+".");
	
	if(TimedEvent.getCompleted(stat.skillName+"Cooldown"))
	{
		this.sendChat(stat.skillName+" Ability refreshed.");
	}
}*/

/*BACKUP with file encryption
 public static void updateProperties()
{
	LevelingProperties levelingProperties = new LevelingProperties();
	FileEncryption fileEncryption = new FileEncryption(mc.theWorld);
	try
	{
		file = new File(Minecraft.getMinecraftDir().toString()+"/config/mod_mcStats.properties");
		if(file.createNewFile())
		{
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			for(Stat stat : Stat.statList)
			{
				levelingProperties.setProperty(fileEncryption.encrypt(stat.skillName), fileEncryption.encrypt("0"));
				levelingProperties.setProperty(fileEncryption.encrypt(stat.skillName+"XP"), fileEncryption.encrypt("0"));
			}
			levelingProperties.store(fileOutputStream, "Leveling Properties");
			fileOutputStream.close();
		}
		else if(mc.theWorld != null)
		{
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			for(Stat stat : Stat.statList)
			{
				levelingProperties.setProperty(fileEncryption.encrypt(stat.skillName), fileEncryption.encrypt(Integer.toString(stat.skillLevel)));
				levelingProperties.setProperty(fileEncryption.encrypt(stat.skillName+"XP"), fileEncryption.encrypt(Integer.toString(stat.experience)));
			}
			levelingProperties.store(fileOutputStream, "Leveling Properties");
			fileOutputStream.close();
		}
		for(Stat stat : Stat.statList)
		{//if stat doesn't exist yet in the file
			if(!levelingProperties.contains(fileEncryption.encrypt(Integer.toString(stat.skillLevel))))
			{
				levelingProperties.setProperty(fileEncryption.encrypt(stat.skillName), fileEncryption.encrypt("0"));
				levelingProperties.setProperty(fileEncryption.encrypt(stat.skillName+"XP"), fileEncryption.encrypt("0"));
			}
		}
		levelingProperties.load(new FileInputStream(Minecraft.getMinecraftDir().toString()+"/config/mod_mcStats.properties"));
		for(Stat stat : Stat.statList)
		{
			stat.skillLevel = Integer.parseInt(levelingProperties.getProperty(fileEncryption.encrypt(stat.skillName)));
			stat.experience = Integer.parseInt(levelingProperties.getProperty(fileEncryption.encrypt(stat.skillName+"XP")));
		}
	}
	catch(IOException ioexception)
	{
		ioexception.printStackTrace();
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}
 */

/*BACKUP
	public static void updateProperties()
{
	LevelingProperties levelingProperties = new LevelingProperties();
	try
	{
		file = new File(Minecraft.getMinecraftDir().toString()+"/config/mod_mcStats.properties");
		if(file.createNewFile())
		{
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			for(Stat stat : Stat.statList)
			{
				levelingProperties.setProperty(stat.skillName, "0");
				levelingProperties.setProperty(stat.skillName+"XP", "0");
			}
			levelingProperties.store(fileOutputStream, "Leveling Properties");
			fileOutputStream.close();
		}
		else if(mc.theWorld != null)
		{
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			for(Stat stat : Stat.statList)
			{
				levelingProperties.setProperty(stat.skillName, Integer.toString(stat.skillLevel));
				levelingProperties.setProperty(stat.skillName+"XP", Integer.toString(stat.experience));
			}
			levelingProperties.store(fileOutputStream, "Leveling Properties");
			fileOutputStream.close();
		}
		for(Stat stat : Stat.statList)
		{//if stat doesn't exist yet in the file
			if(!levelingProperties.contains(stat.skillLevel))
			{
				levelingProperties.setProperty(stat.skillName, "0");
				levelingProperties.setProperty(stat.skillName+"XP", "0");
			}
		}
		levelingProperties.load(new FileInputStream(Minecraft.getMinecraftDir().toString()+"/config/mod_mcStats.properties"));
		for(Stat stat : Stat.statList)
		{
			stat.skillLevel = Integer.parseInt(levelingProperties.getProperty(stat.skillName));
			stat.experience = Integer.parseInt(levelingProperties.getProperty(stat.skillName+"XP"));
		}
	}
	catch(IOException ioexception)
	{
		ioexception.printStackTrace();
	}
}
 */

/*public static void updateProperties()
{
	LevelingProperties levelingProperties = new LevelingProperties();
	try
	{
		FileOutputStream fileOutputStream = new FileOutputStream(file);
		for(Stat stat : Stat.statList)
		{
			levelingProperties.setProperty(stat.skillName, Integer.toString(stat.skillLevel));
			levelingProperties.setProperty(stat.skillName+"XP", Integer.toString(stat.experience));
		}
		levelingProperties.store(fileOutputStream, "Leveling Properties");
		fileOutputStream.close();

		levelingProperties.load(new FileInputStream(Minecraft.getMinecraftDir().toString()+"/config/mod_mcStats.properties"));
		
		for(Stat stat : Stat.statList)
		{
			stat.skillLevel = Integer.parseInt(levelingProperties.getProperty(stat.skillName));
			stat.experience = Integer.parseInt(levelingProperties.getProperty(stat.skillName+"XP"));
		}
	}
	catch(IOException ioexception)
	{
		ioexception.printStackTrace();
	}
}*/


/*if(Mouse.isInsideWindow() && Mouse.isButtonDown(MouseEvent.BUTTON2))//mc.gameSettings.keyBindUseItem.pressTime != 0)
{
	if(mc.playerController != null && mc.playerController instanceof PlayerControllerSP)
	{
		PlayerControllerSP pcsp = (PlayerControllerSP)mc.playerController;
		pcsp.onPlayerRightClick(mc.thePlayer, null, mc.thePlayer.getCurrentEquippedItem(), 0, 0, 0, 0);
	}
}*/

/*public boolean onTickInGame(float tick, Minecraft mc)
{
	if(ModLoader.isGUIOpen(GuiEnchantment.class))
	{
		
	}
	return true;
}*/

//public static ArrayList<Stat> statList = new ArrayList<Stat>();

/*public static int getStatLoc(Stat stat)
{
	for(Stat stat1 : statList)
		if(stat1.skillName.equals(stat.skillName))
			return statList.indexOf(stat.skillName);
	return -1;
}*/

/*public static int getStatLoc(String statString)
{
	for(int i = 0; i < statList.size(); i++)
		if(statList.get(i).skillName.equals(statString))
			return i;
	return -1;
}*/


/*public boolean hitEntity(ItemStack itemstack, EntityLiving entityliving, EntityLiving entityliving1)
{
	itemstack.damageItem(8, entityliving1);
	EntityLiving.setFire(400);
	return true;
}*/

/*
	public Block getCurrentBlock()
	{
		if(mc.theWorld != null)
		{
			int blockID = mc.theWorld.getBlockId(curBlockX, curBlockY, curBlockZ);
			int metaData = mc.theWorld.getBlockMetadata(curBlockX, curBlockY, curBlockZ);
			Material material = mc.theWorld.getBlockMaterial(curBlockX, curBlockY, curBlockZ);
			
			mc.theWorld.setBlock(curBlockX, curBlockY, curBlockZ, 0);
			//mc.theWorld.worldProvider.getChunkProvider().
			Block block = new Block(blockID, metaData, material);
			return block;
		}
		else return null;
	}
*/


/*public boolean onTickInGame(float tick, Minecraft mc)
{
	EntityPlayer entityplayer = mc.thePlayer;
	double yaw = -Math.sin((double)(entityplayer.rotationYaw)/180.0*Math.PI) * 90.0;
	double pitch = -Math.sin((double)(entityplayer.rotationPitch)/180.0*Math.PI) * 90.0;
//	System.out.println("Yaw: "+yaw+"\nPitch: "+pitch);
	
	//PLAYER CONTROLLER SP!!!!!//
	if(!mc.theWorld.isRemote && mc.thePlayer.info.isCreative)
	{
//		PlayerControllerSP playerController = (PlayerControllerSP)mc.playerController;
//		Block block = playerController.getCurrentBlock();
//		if(prevBlock == null)
//			prevBlock = block;
//		
//		if(!prevBlock.equals(block))
//			System.out.println(block);
		
		
		//mc.theWorld.getEntityPathToXYZ(par1Entity, par2, par3, par4, par5, par6, par7, par8, par9)//canMineBlock(entityplayer, par2, par3, i)
		//worldInfo
		//canMineBlock
		//getEntityPathToXYZ
		
		
		
		//getPosition()
		//getLookVector()
		// -- > make movingObjectPosition
	}
	
	
//	updateProperties();
//  lookVector = entityplayer.getLookVec();
//  Vec3D lookVectorPlusMotion = Vec3D.createVector(entityplayer.posX + entityplayer.motionX, entityplayer.motionY, entityplayer.motionZ);
	
	counter++;
	return true;
}*/

/*public Vec3D lookVector;
public Block prevBlock = null;

public static void onBlockHarvest(Block block)
{
	EntityPlayer entityplayer = mc.thePlayer;
	
	//int direction = (int)((entityplayer.rotationYaw / 360F + 2.5D)*360F) & 3;
	//int direction = (int)(entityplayer.rotationYaw * 4F + .5D * 360F) & 360;
	
	double yaw = -Math.sin((double)(entityplayer.rotationYaw)/180.0*Math.PI) * 90.0;
	double pitch = -Math.sin((double)(entityplayer.rotationPitch)/180.0*Math.PI) * 90.0;
	////int direction = MathHelper.floor_double((double)((entityplayer.rotationYaw * 4F) / 360F) + 0.5D) & 3;
	
	System.out.println("Yaw: "+yaw);
	System.out.println("Pitch: "+pitch);
		
	System.out.println("Block harvested.");
}*/

/*
if(string.equals("enchantment.untouching"))
	xp = 5;
else if(string.contains("enchantment.protect"))
	xp = 5;
else if(string.equals("enchantment.oxygen"))
	xp = 5;
else if(string.contains("enchantment.lootBonus"))
	xp = 5;
else if(string.equals("enchantment.knockback"))
	xp = 5;
else if(string.equals("enchantment.fire"))
	xp = 5;
else if(string.equals("enchantment.durability"))
	xp = 5;
else if(string.equals("enchantment.digging"))
	xp = 5;
else if(string.contains("enchantment.damage"))
	xp = 5;
else if(string.equals("enchantment.arrowKnockback"))
	xp = 5;
else if(string.equals("enchantment.arrowInfinite"))
	xp = 5;
else if(string.equals("enchantment.arrowDamage"))
	xp = 5;
else if(string.equals("enchantment.arrowFire"))
	xp = 5;
else if(string.equals("enchantment.waterWorker"))
	xp = 5;
*/

/*for(Stat stat : statList)
{
	if(stat.experience < statList.get(statList.indexOf(stat)).experience)
	{
		stat.skillLevel = statList.get(statList.indexOf(stat)).skillLevel;
		stat.experience = statList.get(statList.indexOf(stat)).experience;
	}
	else if(stat.experience > statList.get(statList.indexOf(stat)).experience)
	{
		statList.get(statList.indexOf(stat)).skillLevel = stat.skillLevel;
		statList.get(statList.indexOf(stat)).experience = stat.experience;
	}
}*/

/*if(heldItemID == Item.pickaxeWood.shiftedIndex || heldItemID == Item.pickaxeStone.shiftedIndex ||
heldItemID == Item.pickaxeSteel.shiftedIndex || heldItemID == Item.pickaxeGold.shiftedIndex || heldItemID == Item.pickaxeDiamond.shiftedIndex ||
heldItemID == Item.shovelWood.shiftedIndex || heldItemID == Item.shovelStone.shiftedIndex || heldItemID == Item.shovelSteel.shiftedIndex ||
heldItemID == Item.shovelGold.shiftedIndex || heldItemID == Item.shovelDiamond.shiftedIndex || heldItemID == Item.bow.shiftedIndex ||
heldItemID == Item.fishingRod.shiftedIndex || heldItemID == Item.swordDiamond.shiftedIndex || heldItemID == Item.swordGold.shiftedIndex ||
heldItemID == Item.swordSteel.shiftedIndex || heldItemID == Item.swordStone.shiftedIndex || heldItemID == Item.swordWood.shiftedIndex ||
heldItemID == Item.helmetChain.shiftedIndex || heldItemID == Item.helmetGold.shiftedIndex || heldItemID == Item.helmetDiamond.shiftedIndex ||
heldItemID == Item.helmetLeather.shiftedIndex || heldItemID == Item.helmetSteel.shiftedIndex || heldItemID == Item.legsChain.shiftedIndex ||
heldItemID == Item.legsLeather.shiftedIndex || heldItemID == Item.legsDiamond.shiftedIndex || heldItemID == Item.legsGold.shiftedIndex ||
heldItemID == Item.legsSteel.shiftedIndex || heldItemID == Item.plateSteel.shiftedIndex || heldItemID == Item.plateChain.shiftedIndex ||
heldItemID == Item.plateLeather.shiftedIndex || heldItemID == Item.plateDiamond.shiftedIndex ||	heldItemID == Item.plateGold.shiftedIndex ||
heldItemID == Item.bootsChain.shiftedIndex || heldItemID == Item.bootsDiamond.shiftedIndex || heldItemID == Item.bootsGold.shiftedIndex ||
heldItemID == Item.bootsLeather.shiftedIndex || heldItemID == Item.bootsSteel.shiftedIndex)
return true;*/


/*if(b == Block.dirt.blockID)
	Stat.addExperience("Digging", 5);
else if(b == Block.sand.blockID)
	Stat.addExperience("Digging", 15);
else if(b == Block.gravel.blockID)
	Stat.addExperience("Digging", 20);
else if(b == Block.grass.blockID)
	Stat.addExperience("Digging", 5);
else if(b == Block.slowSand.blockID)
	Stat.addExperience("Digging", 60);
else if(b == Block.blockClay.blockID)
	Stat.addExperience("Digging", 40);*/

/*if(b == Block.stone.blockID)
Stat.addExperience("Mining", 5);
else if(b == Block.oreDiamond.blockID)
Stat.addExperience("Mining", 100);
else if(b == Block.oreGold.blockID)
Stat.addExperience("Mining", 60);
else if(b == Block.obsidian.blockID)
Stat.addExperience("Mining", 90);
else if(b == Block.netherrack.blockID)
Stat.addExperience("Mining", 5);
else if(b == Block.cobblestoneMossy.blockID)
Stat.addExperience("Mining", 5);
else if(b == Block.oreCoal.blockID)
Stat.addExperience("Mining", 20);
else if(b == Block.sandStone.blockID)
Stat.addExperience("Mining", 5);
else if(b == Block.glowStone.blockID)
Stat.addExperience("Mining", 40);
else if(b == Block.oreRedstone.blockID)
Stat.addExperience("Mining", 40);
else if(b == Block.whiteStone.blockID)
Stat.addExperience("Mining", 15);
else if(b == Block.oreLapis.blockID)
Stat.addExperience("Mining", 80);
else if(b == Block.oreIron.blockID)
Stat.addExperience("Mining", 25);
*/