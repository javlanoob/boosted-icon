package com.boosticon;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.TextComponent;

public class BoostIconOverlay extends Overlay
{
	/**
	 * How far from the middle of your health bar the column of icons sits, and how far past that the
	 * number goes.
	 */
	private static final int ICON_OFFSET = 25;

	private static final int TEXT_OFFSET = 45;

	/**
	 * Smallest an icon is drawn, before the size setting is added to it.
	 */
	private static final int BASE_SIZE = 16;

	private final Client client;
	private final SpriteManager spriteManager;
	private final BoostIconConfig config;
	private final BoostIconPlugin plugin;

	/**
	 * Drawn through this rather than straight onto the graphics for the black outline it puts behind the
	 * text, which keeps a number readable over whatever the player is standing on.
	 */
	private final TextComponent text = new TextComponent();

	@Inject
	BoostIconOverlay(Client client, SpriteManager spriteManager, BoostIconConfig config, BoostIconPlugin plugin)
	{
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		this.client = client;
		this.spriteManager = spriteManager;
		this.config = config;
		this.plugin = plugin;
		text.setOutline(true);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		List<StatColumn> columns = plugin.getColumns();

		if (columns.isEmpty())
		{
			return null;
		}

		int size = config.size();
		boolean right = config.iconSide() == IconSide.RIGHT;
		Font font = graphics.getFont();
		graphics.setFont(font.deriveFont(font.getStyle(), font.getSize() + size));

		for (StatColumn column : columns)
		{
			drawColumn(graphics, column, size, right);
		}

		return null;
	}

	private void drawColumn(Graphics2D graphics, StatColumn column, int size, boolean right)
	{
		Player player = column.getPlayer();
		LocalPoint location = player.getLocalLocation();

		if (location == null)
		{
			return;
		}

		// The column is drawn upwards from here, so lowering it is taking off the height it starts at
		int adjustIcon = 5 - config.drop();
		for (StatChange change : column.getChanges())
		{
			BufferedImage icon = spriteManager.getSprite(change.getStat().getSpriteId(), 0);
			if (icon == null)
			{
				continue;
			}

			Point canvasPoint = Perspective.getCanvasImageLocation(
				client,
				location,
				icon,
				player.getLogicalHeight());

			if (canvasPoint == null)
			{
				return;
			}

			graphics.drawImage(
				icon,
				canvasPoint.getX() + (right ? ICON_OFFSET : -ICON_OFFSET),
				canvasPoint.getY() - adjustIcon,
				size + BASE_SIZE,
				size + BASE_SIZE,
				null);

			String label = label(change);
			if (label != null)
			{
				text.setText(label);
				text.setColor(color(change));
				text.setPosition(
					canvasPoint.getX() + size + (right ? TEXT_OFFSET : -TEXT_OFFSET),
					canvasPoint.getY() + size + 11 - adjustIcon);
				text.render(graphics);
			}

			adjustIcon += size + BASE_SIZE;
		}
	}

	private String label(StatChange change)
	{
		switch (config.statText())
		{
			case CHANGE:
				return change.getChange() > 0 ? "+" + change.getChange() : String.valueOf(change.getChange());
			case LEVEL:
				return String.valueOf(change.getLevel());
			default:
				return null;
		}
	}

	/**
	 * The colours the game's own Boosts plugin uses, so a stat reads the same here as it does there:
	 * green while buffed, yellow once the buff is down to its last levels, red while debuffed.
	 */
	private Color color(StatChange stat)
	{
		int change = stat.getChange();

		if (change < 0)
		{
			return config.debuffColor();
		}

		int threshold = config.buffThreshold();

		return threshold > 0 && change <= threshold ? config.expiringColor() : config.buffColor();
	}
}
