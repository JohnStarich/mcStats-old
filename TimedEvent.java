package SolarPrizm.mcStats;

import net.minecraft.src.*;
import java.util.ArrayList;
import java.util.Date;

public class TimedEvent
{
	private static ArrayList<TimedEvent> events = new ArrayList<TimedEvent>();
	
	private final Date startTime = new Date();
	private final Date endTime;
	private String name;
	private boolean created = true;
	
	public static void addEvent(String eventName, long seconds)
	{
		if(eventName.equals("")) return;
		for(TimedEvent event : events)
		{
			if(event.name.equals(eventName))
			{
			//	System.out.println("Duplicate event not created.");
				return;
			}
		}
		events.add(new TimedEvent(eventName, seconds));
	}
	
	public static boolean isEmpty()
	{
		return events.isEmpty();
	}
	
	public static boolean nameContains(String string)
	{
		for(TimedEvent event : events)
		{
			if(event.name.contains(string))
				return true;
		}
		return false;
	}
	
	public TimedEvent(String eventName, long seconds)
	{
		endTime = new Date((new Date()).getTime() + seconds*1000);
		name = eventName;
		System.out.println("Event created: \""+eventName+"\" ("+seconds+"s)");
	}
	
	public static boolean getCompleted(String name)
	{
		for(TimedEvent event : events)
		{
			if(name.equals(event.name))
				return event.getCompleted();
		}
		return false;
	}
	
	public static boolean active(String name)
	{
		for(TimedEvent event : events)
		{
			if(name.equals(event.name))
				return event.active();
		}
		return false;
	}
	
	public boolean active()
	{
		return timeLeftMil() > 0;
	}
	
	public static long timeElapsed(String name)
	{
		for(TimedEvent event : events)
		{
			if(name.equals(event.name))
				return event.timeElapsed();
		}
		return 0;
	}
	
	public static long timeLeft(String name)
	{
		for(TimedEvent event : events)
		{
			if(name.equals(event.name))
				return event.timeLeft();
		}
		return 0;
	}
	
	public static boolean getCreated(String name)
	{
		for(TimedEvent event : events)
		{
			if(name.equals(event.name))
				return event.getCreated();
		}
		return false;
	}
	
	public boolean getCreated()
	{
		if(created)
		{
			created = false;
			return true;
		}
		
		return false;
	}
	
	public boolean getCompleted()
	{
		if((new Date()).after(endTime))
		{
			System.out.println("Event Removed: \""+name+"\"");
			events.remove(this);
			return true;
		}
		return false;
	}
	
	public long timeElapsed()
	{
		return ((new Date()).getTime() - startTime.getTime()) / 1000;
	}
	
	public long timeElapsedMil()
	{
		return (new Date()).getTime() - startTime.getTime();
	}
	
	public long timeLeft()
	{
		return (endTime.getTime() - (new Date()).getTime()) / 1000;
	}
	
	public long timeLeftMil()
	{
		return endTime.getTime() - (new Date()).getTime();
	}
	
	public static void cleanEvents()
	{
		for(TimedEvent event : events)
		{
			if(event.getCompleted())// || event.name.equals(""))
			{
				events.remove(event);
	//			System.out.println("Removed event \""+event.name+"\"");
			}
		}
	}
	
	private static Date prevDate = new Date();
	private static boolean prevPaused = false;
	
	public static void onUpdate()
	{
		if((prevDate).after(new Date()) && events.size() > 0)
		{
			ModLoader.getMinecraftInstance().thePlayer.addChatMessage("Your system time has been set back. All Events removed.");
			events = new ArrayList<TimedEvent>();
			System.out.println("All events removed because the system time has been set back.");
		}
		prevDate = new Date();
		/*if(!mod_mcLeveling.mc.isGamePaused && prevPaused)
		{
			Date endPause = new Date();
			for(TimedEvent event : events)
			{
				event.addTime(endPause.getTime() - prevDate.getTime());
			}
		}
		prevPaused = mod_mcLeveling.mc.isGamePaused;*/
	}
	
	private void addTime(long time)
	{
		endTime.setTime(endTime.getTime()+time);
	}

	public static TimedEvent get(String string)
	{
		for(TimedEvent event : events)
		{
			if(event.name.equals(string))
				return event;
		}
		return null;
	}

	public static void removeEvent(String skillString)
	{
		for(TimedEvent event : events)
		{
			if(event.name.equals(skillString))
				events.remove(event);
		}
	}
}