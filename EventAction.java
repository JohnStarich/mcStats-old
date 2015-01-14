package SolarPrizm.mcStats;

import java.util.Date;

public class EventAction
{
	private Integer[] data = new Integer[10];
	private String label;
	private Date endTime;
	
	public EventAction(String title, long time, Integer[] nums)
	{
		data = nums;
		endTime = new Date(new Date().getTime() + 1000*time);
		label = title;
	}
	
	public EventAction(String title, long time, int a, int b, int c, int d)
	{
		data[0] = a;
		data[1] = b;
		data[2] = c;
		data[3] = d;
		
		endTime = new Date(new Date().getTime() + 1000*time);
		label = title;
	}
	
	public boolean isDone()
	{
		return new Date().getTime() > endTime.getTime();
	}
	
	public Integer[] getData()
	{
		return data;
	}
	
	public Integer getData(int location)
	{
		if(location < data.length)
			return data[location];
		else
		{
			System.out.println("ERROR EventAction Selection");
			return 0;
		}
	}
	
	public String getLabel()
	{
		return label;
	}
}
