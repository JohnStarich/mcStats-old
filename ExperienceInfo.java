package SolarPrizm.mcStats;

import java.util.ArrayList;

public class ExperienceInfo
{
	private int xpValue = 0;
	private int assocID = 0;
	private String type = "";
	private String assocString = "";
	
	public ExperienceInfo(int xp, int associd, String nType)
	{
		xpValue = xp;
		assocID = associd;
		type = nType;
	//	addXPInfo(this);
	}
	
	public ExperienceInfo(int xp, String assocstring, String nType)
	{
		xpValue = xp;
		assocString = assocstring;
		type = nType;
	//	addXPInfo(this);
	}
	
	public int getXP()
	{
		return xpValue;
	}
	
	public int getID()
	{
		return assocID;
	}
	
	public String getString()
	{
		return assocString;
	}
	
	public String getType()
	{
		if(type.equals(""))
			System.out.println("ID detect error. ID="+assocID);
		return type;
	}
	
	public static ArrayList<ExperienceInfo> getInfoForSkill(String skill)
	{
		ArrayList<ExperienceInfo> temp = new ArrayList<ExperienceInfo>();
		for(ExperienceInfo xpinfo : xpInfo)
			if(xpinfo.type.equals(skill))
				temp.add(xpinfo);
		return temp;
	}
	
	private static ArrayList<ExperienceInfo> xpInfo = new ArrayList<ExperienceInfo>();
	
	public static void addXPInfo(ExperienceInfo xpData)
	{
		xpInfo.add(xpData);
	}
	
	public static ExperienceInfo getXPInfo(int associd)
	{
		for(ExperienceInfo xpinfo : xpInfo)
		{
			if(xpinfo.getID() == associd)
				return xpinfo;
		}
		return null;
	}
	
	public static ExperienceInfo getStringInfo(String string)
	{
		for(ExperienceInfo xpinfo : xpInfo)
		{
			if(xpinfo.getString().contains(string))
				return xpinfo;
		}
		return null;
	}

	public static void addXPInfo(int xp, String assocstring, String nType)
	{
		addXPInfo(new ExperienceInfo(xp, assocstring, nType));
	}
	
	public static void addXPInfo(int xp, int blockID, String nType)
	{
		addXPInfo(new ExperienceInfo(xp, blockID, nType));
	}
}