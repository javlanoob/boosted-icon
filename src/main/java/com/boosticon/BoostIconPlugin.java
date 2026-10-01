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
import net.runelite.api.Experience;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PartyChanged;
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

	@Inject
	private PartyPanelStats partyPanelStats;

	private final List<StatColumn> columns = new ArrayList<>();

	/**
	 * The levels each party member has shared, held until they share different ones or leave.
	 */
	private final Map<Long, PartyLevels> partyLevels = new HashMap<>();

	/**
	 * The levels last sent to the party, so a tick that changed nothing sends nothing.
	 */
	private int[] sentBoosted;
	private int[] sentReal;

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		wsClient.registerMessage(BoostIconStats.class);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		wsClient.unregisterMessage(BoostIconStats.class);
		partyPanelStats.stop();
		columns.clear();
		partyLevels.clear();
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

		if (config.showSelf())
		{
			addColumn(client.getLocalPlayer(), boosted, real);
		}

		partyPanelStats.listen(config.partyStats());
		share(boosted, real);

		if (!config.partyStats() || !partyService.isInParty())
		{
			partyLevels.clear();
			return;
		}

		PartyMember local = partyService.getLocalMember();
		for (PartyMember member : partyService.getMembers())
		{
			if (local != null && member.getMemberId() == local.getMemberId())
			{
				continue;
			}

			PartyLevels levels = partyLevels.get(member.getMemberId());
			if (levels == null)
			{
				continue;
			}

			addColumn(playerNamed(member.getDisplayName()), levels.getBoosted(), levels.getReal());
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

	/**
	 * Levels belong to the party they were shared with, and a different party is different people.
	 */
	@Subscribe
	public void onPartyChanged(PartyChanged event)
	{
		clientThread.invoke(() ->
		{
			partyLevels.clear();
			forgetSent();
		});

		requestSync();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (BoostIconConfig.GROUP.equals(event.getGroup()) && "partyStats".equals(event.getKey()))
		{
			requestSync();
		}
	}

	@Subscribe
	public void onBoostIconStats(BoostIconStats message)
	{
		int[] boosted = message.getBoosted();
		int[] real = message.getReal();

		if (boosted == null || real == null
			|| boosted.length != STATS.length || real.length != STATS.length)
		{
			return;
		}

		for (int i = 0; i < STATS.length; i++)
		{
			record(message.getMemberId(), STATS[i].getSkill().ordinal(), real[i], boosted[i]);
		}
	}

	@Subscribe
	public void onUserPart(UserPart event)
	{
		clientThread.invoke(() -> partyLevels.remove(event.getMemberId()));
	}

	/**
	 * Somebody has just joined and has nothing of anyone's yet, so the next tick sends again.
	 */
	@Subscribe
	public void onUserSync(UserSync event)
	{
		clientThread.invoke(this::forgetSent);
	}

	/**
	 * One stat of one party member, however it reached us. Levels come from whoever is sharing them and
	 * are taken as nothing more than numbers: a stat with no icon here is dropped, and a level past 99
	 * is a virtual level, which is a level in name only and not something a stat can be boosted above.
	 */
	void record(long memberId, int skillOrdinal, int level, int boostedLevel)
	{
		CombatStat stat = CombatStat.ofSkill(skillOrdinal);
		PartyMember local = partyService.getLocalMember();

		if (stat == null || level < 1 || boostedLevel < 1 || !config.partyStats()
			|| (local != null && local.getMemberId() == memberId))
		{
			return;
		}

		int real = Math.min(level, Experience.MAX_REAL_LEVEL);

		clientThread.invoke(() -> partyLevels
			.computeIfAbsent(memberId, id -> new PartyLevels())
			.set(stat, boostedLevel, real));
	}

	/**
	 * Asks the party for the levels it has now, rather than waiting on everyone's next change.
	 */
	void requestSync()
	{
		if (config.partyStats() && partyService.isInParty())
		{
			partyService.send(new UserSync());
		}
	}

	private void addColumn(Player player, int[] boosted, int[] real)
	{
		if (player == null || player.getLocalLocation() == null)
		{
			return;
		}

		List<StatChange> changes = new ArrayList<>();

		for (int i = 0; i < STATS.length; i++)
		{
			CombatStat stat = STATS[i];

			if (boosted[i] == PartyLevels.UNKNOWN || real[i] == PartyLevels.UNKNOWN)
			{
				continue;
			}

			int change = boosted[i] - real[i];

			if (!stat.isEnabled(config) || change == 0
				|| (change > 0 && !config.showBuffs())
				|| (change < 0 && !config.showDebuffs()))
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
		partyService.send(new BoostIconStats(sentBoosted, sentReal));
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
