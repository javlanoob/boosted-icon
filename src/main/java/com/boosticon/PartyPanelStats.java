package com.boosticon;

import java.lang.reflect.Field;
import java.util.Collection;
import javax.inject.Inject;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.party.WSClient;
import net.runelite.client.party.messages.PartyMemberMessage;

/**
 * Party Panel already shares every skill's level with the party, so a team mate running that plugin
 * needs nothing else for their boosts and drains to show up here.
 *
 * Its message is another plugin's class, which this one cannot import or name, so it is picked out of
 * the message types the party connection has been told about and read a field at a time. Anything
 * unexpected means no levels rather than an error: the plugin may not be installed at all, and it is
 * free to change messages that are its own.
 */
class PartyPanelStats
{
	private static final String MESSAGES = "messages";
	private static final String MESSAGE = "PartyBatchedChange";
	private static final String STAT_CHANGES = "s";
	private static final String SKILL = "s";
	private static final String LEVEL = "l";
	private static final String BOOSTED_LEVEL = "b";

	private final EventBus eventBus;
	private final WSClient wsClient;
	private final BoostIconPlugin plugin;

	private Class<?> message;
	private EventBus.Subscriber subscriber;

	/**
	 * Where the party connection keeps the message types it has been told about. Found the once, since
	 * the list behind it is read every tick and finding a field costs more than reading one.
	 */
	private Field messages;

	@Inject
	PartyPanelStats(EventBus eventBus, WSClient wsClient, BoostIconPlugin plugin)
	{
		this.eventBus = eventBus;
		this.wsClient = wsClient;
		this.plugin = plugin;
	}

	/**
	 * Asked often, because the other plugin can be turned on, off or updated part way through a
	 * session, and each of those hands out a different class to listen for, or none at all.
	 */
	void listen(boolean wanted)
	{
		Class<?> found = wanted ? registeredMessage() : null;

		if (found == message)
		{
			return;
		}

		stop();

		if (found == null)
		{
			return;
		}

		message = found;
		subscriber = subscribe(found);

		// Everyone sharing levels answers a sync with all of them, so stats that were already boosted
		// before this started listening show up now rather than on their next change
		plugin.requestSync();
	}

	void stop()
	{
		if (subscriber != null)
		{
			eventBus.unregister(subscriber);
			subscriber = null;
		}

		message = null;
	}

	@SuppressWarnings("unchecked")
	private EventBus.Subscriber subscribe(Class<?> type)
	{
		return eventBus.register((Class<Object>) type, this::onMessage, 0f);
	}

	private void onMessage(Object event)
	{
		readLevels(event, plugin::record);
	}

	/**
	 * Every stat in one of the other plugin's changes. A change can carry other things and no stats at
	 * all, and anything that turns out not to be the message it was taken for has no levels to read.
	 */
	static void readLevels(Object event, Levels levels)
	{
		if (!(event instanceof PartyMemberMessage))
		{
			return;
		}

		long memberId = ((PartyMemberMessage) event).getMemberId();

		try
		{
			Object changes = read(event, STAT_CHANGES);

			if (!(changes instanceof Collection))
			{
				// A change that carried no stats, such as an inventory on its own
				return;
			}

			for (Object change : (Collection<?>) changes)
			{
				if (change == null)
				{
					continue;
				}

				levels.record(
					memberId,
					readInt(change, SKILL),
					readInt(change, LEVEL),
					readInt(change, BOOSTED_LEVEL));
			}
		}
		catch (ReflectiveOperationException | RuntimeException e)
		{
			// Not the message it was taken for, so there are no levels in it to read
		}
	}

	private Class<?> registeredMessage()
	{
		try
		{
			if (messages == null)
			{
				messages = field(WSClient.class, MESSAGES);
			}

			Object registered = messages.get(wsClient);

			return registered instanceof Collection ? messageIn((Collection<?>) registered) : null;
		}
		catch (ReflectiveOperationException | RuntimeException e)
		{
			// Another plugin is registering a message of its own as this reads the list, so the next
			// look will see whatever it ends up as
			return null;
		}
	}

	/**
	 * The other plugin's message among the ones the party connection has been told about, picked out by
	 * the name the connection itself tells its messages apart by.
	 */
	static Class<?> messageIn(Collection<?> types)
	{
		for (Object type : types)
		{
			if (type instanceof Class && MESSAGE.equals(((Class<?>) type).getSimpleName()))
			{
				return (Class<?>) type;
			}
		}

		return null;
	}

	private static Object read(Object owner, String name) throws ReflectiveOperationException
	{
		return field(owner.getClass(), name).get(owner);
	}

	private static int readInt(Object owner, String name) throws ReflectiveOperationException
	{
		return field(owner.getClass(), name).getInt(owner);
	}

	private static Field field(Class<?> type, String name) throws ReflectiveOperationException
	{
		Field field = type.getDeclaredField(name);
		field.setAccessible(true);
		return field;
	}

	/**
	 * Where the levels read out of a message go.
	 */
	interface Levels
	{
		void record(long memberId, int skillOrdinal, int level, int boostedLevel);
	}
}
