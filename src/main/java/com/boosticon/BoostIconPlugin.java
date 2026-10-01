package com.boosticon;

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.party.PartyMember;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.WSClient;
import net.runelite.client.party.events.UserPart;
import net.runelite.client.party.messages.UserSync;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

@PluginDescriptor(
	name = "Boost Icon",
	description = "Icons by your health bar for boosted or drained combat stats",
	tags = {"boost", "drain", "potion", "stats", "combat", "party"}
)
public class BoostIconPlugin extends Plugin
{
	private static final CombatStat[] STATS = CombatStat.values();

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private BoostIconConfig config;

	@Inject
	private BoostIconOverlay overlay;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private PartyService partyService;

	@Inject
	private WSClient wsClient;

	private final List<StatColumn> columns = new ArrayList<>();

	/**
	 * The last levels each party member sent, held until they send different ones or leave.
	 */
	private final Map<Long, StatsUpdate> partyStats = new HashMap<>();

	/**
	 * The levels last sent to the party, so a tick that changed nothing sends nothing.
	 */
	private int[] sentBoosted;
	private int[] sentReal;

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		wsClient.registerMessage(StatsUpdate.class);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		wsClient.unregisterMessage(StatsUpdate.class);
		columns.clear();
		partyStats.clear();
		forgetSent();
	}

	@Provides
	BoostIconConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BoostIconConfig.class);
	}

	/**
	 * The icons to draw this tick, read by the overlay on the same thread that fills them in.
	 */
	List<StatColumn> getColumns()
	{
		return Collections.unmodifiableList(columns);
	}

	/**
	 * Levels arrive with the rest of the tick, and a boost that wears off is a tick's news as well, so
	 * the whole set is read fresh each tick rather than followed through stat change events. Nothing to
	 * get out of step with, and a party member's levels are treated the same way.
	 */
	@Subscribe
	public void onGameTick(GameTick event)
	{
		columns.clear();

		int[] boosted = new int[STATS.length];
		int[] real = new int[STATS.length];

		for (int i = 0; i < STATS.length; i++)
		{
			boosted[i] = client.getBoostedSkillLevel(STATS[i].getSkill());
			real[i] = client.getRealSkillLevel(STATS[i].getSkill());
		}

		addColumn(client.getLocalPlayer(), boosted, real);
		share(boosted, real);

		if (!config.partyStats() || !partyService.isInParty())
		{
			partyStats.clear();
			return;
		}

		PartyMember local = partyService.getLocalMember();
		for (PartyMember member : partyService.getMembers())
		{
			if (local != null && member.getMemberId() == local.getMemberId())
			{
				continue;
			}

			StatsUpdate stats = partyStats.get(member.getMemberId());
			if (stats == null)
			{
				continue;
			}

			addColumn(playerNamed(member.getDisplayName()), stats.getBoosted(), stats.getReal());
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() != GameState.LOGGED_IN)
		{
			columns.clear();
			forgetSent();
		}
	}

	@Subscribe
	public void onStatsUpdate(StatsUpdate message)
	{
		PartyMember local = partyService.getLocalMember();
		if (local != null && local.getMemberId() == message.getMemberId())
		{
			return;
		}

		clientThread.invoke(() -> partyStats.put(message.getMemberId(), message));
	}

	@Subscribe
	public void onUserPart(UserPart event)
	{
		clientThread.invoke(() -> partyStats.remove(event.getMemberId()));
	}

	/**
	 * Somebody has just joined and has nothing of anyone's yet, so the next tick sends again.
	 */
	@Subscribe
	public void onUserSync(UserSync event)
	{
		clientThread.invoke(this::forgetSent);
	}

	private void addColumn(Player player, int[] boosted, int[] real)
	{
		if (player == null || player.getLocalLocation() == null
			|| boosted == null || real == null
			|| boosted.length != STATS.length || real.length != STATS.length)
		{
			return;
		}

		List<StatChange> changes = new ArrayList<>();

		for (int i = 0; i < STATS.length; i++)
		{
			CombatStat stat = STATS[i];
			int change = boosted[i] - real[i];

			if (!stat.isEnabled(config) || change == 0
				|| (change > 0 && !config.showBoosts())
				|| (change < 0 && !config.showDrains()))
			{
				continue;
			}

			changes.add(new StatChange(stat, boosted[i], change));
		}

		if (!changes.isEmpty())
		{
			columns.add(new StatColumn(player, changes));
		}
	}

	private void share(int[] boosted, int[] real)
	{
		if (!config.partyStats() || !partyService.isInParty())
		{
			forgetSent();
			return;
		}

		if (Arrays.equals(boosted, sentBoosted) && Arrays.equals(real, sentReal))
		{
			return;
		}

		sentBoosted = boosted.clone();
		sentReal = real.clone();
		partyService.send(new StatsUpdate(sentBoosted, sentReal));
	}

	private void forgetSent()
	{
		sentBoosted = null;
		sentReal = null;
	}

	private Player playerNamed(String name)
	{
		if (name == null)
		{
			return null;
		}

		String wanted = Text.standardize(name);

		for (Player player : client.getTopLevelWorldView().players())
		{
			String other = player.getName();
			if (other != null && Text.standardize(other).equals(wanted))
			{
				return player;
			}
		}

		return null;
	}
}
