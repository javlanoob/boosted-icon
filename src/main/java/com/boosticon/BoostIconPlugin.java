package com.boosticon;

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Boost Icon",
	description = "Icons by your health bar for boosted or drained combat stats",
	tags = {"boost", "drain", "potion", "stats", "combat"}
)
public class BoostIconPlugin extends Plugin
{
	private static final CombatStat[] STATS = CombatStat.values();

	@Inject
	private Client client;

	@Inject
	private BoostIconConfig config;

	@Inject
	private BoostIconOverlay overlay;

	@Inject
	private OverlayManager overlayManager;

	private final List<StatChange> changes = new ArrayList<>();

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		changes.clear();
	}

	@Provides
	BoostIconConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BoostIconConfig.class);
	}

	/**
	 * The stats to draw an icon for, in stats tab order. Read by the overlay, which runs on the client
	 * thread between ticks, so the list is only ever replaced in place and never handed out to be held.
	 */
	List<StatChange> getChanges()
	{
		return Collections.unmodifiableList(changes);
	}

	/**
	 * Levels arrive with the rest of the tick, and a boost that wears off is a tick's news as well, so
	 * the whole set is read fresh each tick rather than followed through stat change events. Seven array
	 * reads a tick, and nothing to get out of step with.
	 */
	@Subscribe
	public void onGameTick(GameTick event)
	{
		changes.clear();

		for (CombatStat stat : STATS)
		{
			if (!stat.isEnabled(config))
			{
				continue;
			}

			int level = client.getBoostedSkillLevel(stat.getSkill());
			int change = level - client.getRealSkillLevel(stat.getSkill());

			if (change == 0 || (change > 0 && !config.showBoosts()) || (change < 0 && !config.showDrains()))
			{
				continue;
			}

			changes.add(new StatChange(stat, level, change));
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() != GameState.LOGGED_IN)
		{
			changes.clear();
		}
	}
}
