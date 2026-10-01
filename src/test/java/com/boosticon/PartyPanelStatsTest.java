package com.boosticon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.runelite.api.Skill;
import net.runelite.client.party.messages.PartyMemberMessage;
import net.runelite.client.party.messages.UserSync;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Party Panel's messages are another plugin's classes, so the shape this one reads them by is written
 * out again here. Anything these get wrong about that shape, the plugin gets wrong too.
 */
public class PartyPanelStatsTest
{
	private static class PartyStatChange
	{
		private int s;
		private int l;
		private int b;
	}

	private static class PartyBatchedChange extends PartyMemberMessage
	{
		private Collection<PartyStatChange> s;
	}

	@Test
	public void findsTheMessageAmongTheOthers()
	{
		assertSame(
			PartyBatchedChange.class,
			PartyPanelStats.messageIn(Arrays.asList(UserSync.class, PartyBatchedChange.class, BoostIconStats.class)));
	}

	@Test
	public void findsNothingWhenThePluginIsNotThere()
	{
		assertNull(PartyPanelStats.messageIn(Arrays.asList(UserSync.class, BoostIconStats.class)));
		assertNull(PartyPanelStats.messageIn(Collections.emptyList()));
	}

	@Test
	public void readsEveryStatInAChange()
	{
		assertEquals(
			Arrays.asList(
				"7 " + Skill.ATTACK.ordinal() + " 99 118",
				"7 " + Skill.DEFENCE.ordinal() + " 85 80"),
			read(change(7L, stat(Skill.ATTACK, 99, 118), stat(Skill.DEFENCE, 85, 80))));
	}

	@Test
	public void readsNothingFromAChangeWithoutStats()
	{
		assertTrue(read(change(7L)).isEmpty());
	}

	@Test
	public void readsNothingFromAMessageOfAnotherShape()
	{
		assertTrue(read(new BoostIconStats(new int[]{99}, new int[]{99})).isEmpty());
		assertTrue(read(new UserSync()).isEmpty());
		assertTrue(read("not a message at all").isEmpty());
	}

	private static List<String> read(Object event)
	{
		List<String> levels = new ArrayList<>();

		PartyPanelStats.readLevels(event, (memberId, skill, level, boostedLevel) ->
			levels.add(memberId + " " + skill + " " + level + " " + boostedLevel));

		return levels;
	}

	private static PartyBatchedChange change(long memberId, PartyStatChange... stats)
	{
		PartyBatchedChange change = new PartyBatchedChange();
		change.setMemberId(memberId);
		change.s = stats.length == 0 ? null : Arrays.asList(stats);
		return change;
	}

	private static PartyStatChange stat(Skill skill, int level, int boostedLevel)
	{
		PartyStatChange stat = new PartyStatChange();
		stat.s = skill.ordinal();
		stat.l = level;
		stat.b = boostedLevel;
		return stat;
	}
}
