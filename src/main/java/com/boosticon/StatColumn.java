package com.boosticon;

import java.util.List;
import net.runelite.api.Player;

/**
 * The icons to draw by one player's health bar.
 */
class StatColumn
{
	private final Player player;
	private final List<StatChange> changes;

	StatColumn(Player player, List<StatChange> changes)
	{
		this.player = player;
		this.changes = changes;
	}

	Player getPlayer()
	{
		return player;
	}

	List<StatChange> getChanges()
	{
		return changes;
	}
}
