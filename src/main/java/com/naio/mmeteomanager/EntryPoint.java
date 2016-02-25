package com.naio.mmeteomanager;


public class EntryPoint
{
	public static void main(String args[])
	{
		Controller controller = new Controller();
		
		controller.doStuff(args[0], args[1], args[2]);
	}
}
