package com.boosticon;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import net.runelite.client.util.ImageUtil;

public class BoostIconOverlay extends Overlay
{
	/**
	 * How far from the middle of your health bar the column of icons sits, and how far past that the
	 * number goes.
	 */
	private static final int ICON_OFFSET = 25;

	private static final int TEXT_OFFSET = 45;

	/**
	 * How tall an icon is drawn before the size setting, which is smaller than the skills tab draws them.
	 */
	private static final int BASE_HEIGHT = 16;

	private final Client client;
	private final SpriteManager spriteManager;
	private final BoostIconConfig config;
	private final BoostIconPlugin plugin;

	/**
	 * Drawn through this rather than straight onto the graphics for the black outline it puts behind the
	 * text, which keeps a number readable over whatever the player is standing on.
	 */
	private final TextComponent text = new TextComponent();

	/**
	 * The outlined icons, kept by sprite, since outlining one is work that only has to happen once.
	 */
	private final Map<Integer, BufferedImage> icons = new HashMap<>();

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

		int anchor = anchor(player);

		// The column is drawn upwards from here, so lowering it is taking off the height it starts at
		int adjustIcon = 5 - config.drop();
		for (StatChange change : column.getChanges())
		{
			BufferedImage icon = icon(change.getStat());
			if (icon == null)
			{
				continue;
			}

			Point canvasPoint = Perspective.getCanvasImageLocation(
				client,
				location,
				icon,
				anchor);

			if (canvasPoint == null)
			{
				return;
			}

			// The skill icons are not all the same shape, so the size goes on the height and the width
			// follows it, rather than squaring everything off
			int height = Math.max(1, BASE_HEIGHT + size);
			int width = Math.max(1, Math.round(icon.getWidth() * height / (float) icon.getHeight()));
			int top = canvasPoint.getY() - adjustIcon;

			graphics.drawImage(
				icon,
				canvasPoint.getX() + (right ? ICON_OFFSET : -ICON_OFFSET),
				top,
				width,
				height,
				null);

			String label = label(change);
			if (label != null)
			{
				text.setText(label);
				text.setColor(color(change));
				text.setPosition(
					canvasPoint.getX() + size + (right ? TEXT_OFFSET : -TEXT_OFFSET),
					top + (height + graphics.getFontMetrics().getAscent()) / 2);
				text.render(graphics);
			}

			adjustIcon += height;
		}
	}

	/**
	 * How far up the player the column is measured from, which is as tall as the player for the health
	 * bar it normally sits by, and nothing at all for the tile they are standing on.
	 */
	private int anchor(Player player)
	{
		switch (config.iconAnchor())
		{
			case BOTTOM:
				return 0;
			case MIDDLE:
				return player.getLogicalHeight() / 2;
			default:
				return player.getLogicalHeight();
		}
	}

	/**
	 * A skill's icon with a black outline around it, which is what keeps it off whatever is behind it.
	 * The outline has to go somewhere, so the sprite is given a pixel of room on each side for it first.
	 */
	private BufferedImage icon(CombatStat stat)
	{
		BufferedImage outlined = icons.get(stat.getSpriteId());

		if (outlined != null)
		{
			return outlined;
		}

		BufferedImage sprite = spriteManager.getSprite(stat.getSpriteId(), 0);

		if (sprite == null || sprite.getWidth() < 1 || sprite.getHeight() < 1)
		{
			// Still being loaded, so there is nothing to outline yet
			return null;
		}

		outlined = ImageUtil.outlineImage(
			ImageUtil.resizeCanvas(sprite, sprite.getWidth() + 2, sprite.getHeight() + 2),
			Color.BLACK);

		icons.put(stat.getSpriteId(), outlined);

		return outlined;
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
