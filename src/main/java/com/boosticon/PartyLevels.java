package com.boosticon;

/**
 * The levels a party member last told the party about. They arrive a stat at a time, as and when each
 * one changes, so every stat is kept until something newer arrives for it.
 */
class PartyLevels
{
	/**
	 * Nothing heard about a stat yet. A level is never zero, so the two cannot be mistaken.
	 */
	static final int UNKNOWN = 0;

	private static final int SIZE = CombatStat.values().length;

	private final int[] boosted = new int[SIZE];
	private final int[] real = new int[SIZE];

	void set(CombatStat stat, int boostedLevel, int realLevel)
	{
		boosted[stat.ordinal()] = boostedLevel;
		real[stat.ordinal()] = realLevel;
	}

	int[] getBoosted()
	{
		return boosted;
	}

	int[] getReal()
	{
		return real;
	}
}
