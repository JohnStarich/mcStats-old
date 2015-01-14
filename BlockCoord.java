package SolarPrizm.mcStats;

import net.minecraft.src.*;
import java.io.Serializable;
import java.util.ArrayList;

public final class BlockCoord implements Serializable
{//TODO make BigInteger or long
	private int x, y, z;
	private static ArrayList<BlockCoord> coordinates = new ArrayList<BlockCoord>();
	
	public BlockCoord(int x, int y, int z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public int getX()
	{
		return x;
	}
	
	public int getY()
	{
		return y;
	}
	
	public int getZ()
	{
		return z;
	}
	
	public int getBlockID(World world)
	{
		return world.getBlockId(x, y, z);
	}
	
	public int getBlockMeta(World world)
	{
		return world.getBlockMetadata(x, y, z);
	}
	
	public int getBlockID()
	{
		return mod_mcStats.mc.theWorld.getBlockId(x, y, z);
	}
	
	public int getBlockMeta()
	{
		return mod_mcStats.mc.theWorld.getBlockMetadata(x, y, z);
	}
	
	public BlockCoord(int x, int y, int z, int side)
	{
		if(side == 0)
			y--;
		else if(side == 1)
			y++;
		else if(side == 2)
			z--;
		else if(side == 3)
			z++;
		else if(side == 4)
			x--;
		else if(side == 5)
			x++;

        this.x = x;
        this.y = y;
        this.z = z;
	}
	
	public static ArrayList<BlockCoord> getCoord()
	{
		return coordinates;
	}
	
	public static void setCoord(ArrayList<BlockCoord> array)
	{
		coordinates = array;
	}
	
	public static void setCoord(String string)
	{
		coordinates = new ArrayList<BlockCoord>();
		string = string.substring(1, string.length()-1);
		
		while(string != null && string.length() > 2)
		{
			if(string.substring(0,1).equals("("))
			{
				String coord = string.substring(0, string.indexOf(")")+1);
				if(coord != null && !coord.equals(""))
					BlockCoord.addCoordinatesWithoutNotify(BlockCoord.parseBlockCoord(coord));
				string = string.substring(1);
				string = string.contains("(") ? string.substring(string.indexOf("(")) : "";
			}
		}
	}
	
	/**
	 * Adds Block Coordinates according to the block it was placed on.
	 * @param x The X coordinate of the block right clicked
	 * @param y The Y coordinate
	 * @param z The Z coordinate
	 * @param side The side right clicked
	 */
	public static void addCoordinates(int x, int y, int z, int side)
	{
		if(side == 0)
			y--;
		else if(side == 1)
			y++;
		else if(side == 2)
			z--;
		else if(side == 3)
			z++;
		else if(side == 4)
			x--;
		else if(side == 5)
			x++;

        addCoordinates(x, y, z);
    //	System.out.println("BlockCoord: "+coordinates);
	}
	
	public static void addCoordinates(int x, int y, int z)
	{
		coordinates.add(new BlockCoord(x, y, z));
		mod_mcStats.updateProperties();
	}
	
	public static void addCoordinates(BlockCoord blockcoord)
	{
		coordinates.add(blockcoord);
		mod_mcStats.updateProperties();
	}
	
	public static void addCoordinatesWithoutNotify(int x, int y, int z)
	{
		coordinates.add(new BlockCoord(x, y, z));
	}
	
	public static void addCoordinatesWithoutNotify(BlockCoord blockcoord)
	{
		coordinates.add(blockcoord);
	}
	
	public static BlockCoord getCoordinates(int x, int y, int z)
	{
		for(BlockCoord coord : coordinates)
		{
			if(coord.x == x && coord.y == y && coord.z == z)
				return coord;
		}
		return null;
	}
	
	public static boolean isPlaced(int x, int y, int z)
	{
		for(BlockCoord coord : coordinates)
		{
			if(coord.x == x && coord.y == y && coord.z == z)
				return true;
		}
		return false;
	}
	
	public static void removeDuplicates(int x, int y, int z)
	{
		for(BlockCoord coord : coordinates)
			if(coord.x == x && coord.y == y && coord.z == z)
				coord.removeSelf();
	}
	
	public static void addCoordinatesIfNotDuplicate(int x, int y, int z)
	{
		BlockCoord coord = new BlockCoord(x, y, z);
		if(!isPlaced(x, y, z))
		{
			coordinates.add(coord);
			mod_mcStats.updateProperties();
		//	System.out.println("BlockCoord: "+coordinates);
		}
	}
	
	public static void addCoordinatesIfNotDuplicate(int x, int y, int z, int side)
	{
		if(side == 0)
			y--;
		else if(side == 1)
			y++;
		else if(side == 2)
			z--;
		else if(side == 3)
			z++;
		else if(side == 4)
			x--;
		else if(side == 5)
			x++;

		addCoordinatesIfNotDuplicate(x, y, z);
	}
	
	public void removeSelf()
	{
		coordinates.remove(this);
		mod_mcStats.updateProperties();
	}
	
	public String toString()
	{
		return "("+x+","+y+","+z+")";
	}
	
	public static BlockCoord parseBlockCoord(String string)
	{
		string = string.substring(1,string.length()-1);
		int x = Integer.parseInt(string.substring(0, string.indexOf(",")));
		string = string.substring(string.indexOf(",")+1);
		int y = Integer.parseInt(string.substring(0, string.indexOf(",")));
		string = string.substring(string.indexOf(",")+1);
		int z = Integer.parseInt(string);
		return new BlockCoord(x, y, z);
	}
}
