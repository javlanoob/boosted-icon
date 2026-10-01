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

	/**
	 * How light a pixel has to be to want an outline pixel of its own next to it. The skill icons are
	 * drawn with a dark edge already, and going around that as well is what reads as two pixels thick.
	 */
	private static final int DARK = 48;

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

	/**
	 * The height those were prepared at, since a change to the size setting makes them all over again.
	 */
	private int iconHeight;

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

		// Three at the least, since the outline takes a pixel at the top and another at the bottom
		int height = Math.max(3, BASE_HEIGHT + size);

		for (StatColumn column : columns)
		{
			drawColumn(graphics, column, size, height, right);
		}

		return null;
	}

	private void drawColumn(Graphics2D graphics, StatColumn column, int size, int height, boolean right)
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
			BufferedImage icon = icon(change.getStat(), height);
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

			int top = canvasPoint.getY() - adjustIcon;

			// Already the size it is drawn at, so nothing is scaled here
			graphics.drawImage(
				icon,
				canvasPoint.getX() + (right ? ICON_OFFSET : -ICON_OFFSET),
				top,
				null);

			String label = label(change);
			if (label != null)
			{
				text.setText(label);
				text.setColor(color(change));
				text.setPosition(
					canvasPoint.getX() + size + (right ? TEXT_OFFSET : -TEXT_OFFSET),
					top + (icon.getHeight() + graphics.getFontMetrics().getAscent()) / 2);
				text.render(graphics);
			}

			adjustIcon += icon.getHeight();
		}
	}

	/**
	 * Hitpoints and prayer are coloured by how much of them is left rather than by which way they are
	 * off the full amount, so that a brew reads as plenty and a long fight reads as trouble.
	 */
	private Color pointsColor(StatChange stat)
	{
		int full = stat.getRealLevel();

		if (full < 1)
		{
			return config.buffColor();
		}

		int left = stat.getLevel() * 100 / full;

		if (left < config.criticalPoints())
		{
			return config.debuffColor();
		}

		return left < config.lowPoints() ? config.expiringColor() : config.buffColor();
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
	private BufferedImage icon(CombatStat stat, int height)
	{
		if (height != iconHeight)
		{
			icons.clear();
			iconHeight = height;
		}

		BufferedImage icon = icons.get(stat.getSpriteId());

		if (icon != null)
		{
			return icon;
		}

		BufferedImage sprite = spriteManager.getSprite(stat.getSpriteId(), 0);

		if (sprite == null || sprite.getWidth() < 1 || sprite.getHeight() < 1)
		{
			// Still being loaded, so there is nothing to prepare yet
			return null;
		}

		// Scaled before it is outlined, so the outline is a pixel wide at the size it ends up drawn at
		icon = outlined(scaled(sprite, height - 2));

		icons.put(stat.getSpriteId(), icon);

		return icon;
	}

	/**
	 * The sprite at the height the icons are drawn at, keeping its shape. Drawing a scaled image leaves
	 * it to whatever the graphics is set to, which is nearest neighbour and drops pixels unevenly out of
	 * artwork this small, so it is scaled here instead: smoothly, and once rather than every frame.
	 */
	private static BufferedImage scaled(BufferedImage sprite, int height)
	{
		if (height == sprite.getHeight())
		{
			return sprite;
		}

		int width = Math.max(1, Math.round(sprite.getWidth() * height / (float) sprite.getHeight()));

		return ImageUtil.resizeImage(sprite, width, height);
	}

	/**
	 * Black put in around a sprite, wherever the sprite does not have something dark enough there
	 * already, so the icon ends up with an edge of one pixel all the way round rather than two in the
	 * places the artwork was already outlined.
	 */
	static BufferedImage outlined(BufferedImage sprite)
	{
		int width = sprite.getWidth() + 2;
		int height = sprite.getHeight() + 2;

		// A pixel of room on each side, for the outline to have somewhere to go
		BufferedImage image = ImageUtil.resizeCanvas(sprite, width, height);
		BufferedImage outlined = ImageUtil.resizeCanvas(sprite, width, height);

		for (int x = 0; x < width; x++)
		{
			for (int y = 0; y < height; y++)
			{
				// Read from the sprite throughout, so a pixel just filled in cannot grow the outline
				if (empty(image, x, y) && bordersLight(image, x, y))
				{
					outlined.setRGB(x, y, Color.BLACK.getRGB());
				}
			}
		}

		return outlined;
	}

	private static boolean empty(BufferedImage image, int x, int y)
	{
		return (image.getRGB(x, y) >>> 24) == 0;
	}

	/**
	 * Whether any of the eight pixels around this one is part of the icon and light enough to need an
	 * outline of its own.
	 */
	private static boolean bordersLight(BufferedImage image, int x, int y)
	{
		for (int alongX = Math.max(x - 1, 0); alongX <= Math.min(x + 1, image.getWidth() - 1); alongX++)
		{
			for (int alongY = Math.max(y - 1, 0); alongY <= Math.min(y + 1, image.getHeight() - 1); alongY++)
			{
				int pixel = image.getRGB(alongX, alongY);

				if ((pixel >>> 24) != 0 && !dark(pixel))
				{
					return true;
				}
			}
		}

		return false;
	}

	private static boolean dark(int pixel)
	{
		int lightest = Math.max((pixel >> 16) & 0xff, Math.max((pixel >> 8) & 0xff, pixel & 0xff));

		return lightest < DARK;
	}

	private String label(StatChange change)
	{
		if (change.getStat().isPoints() && config.statText() != StatText.NONE)
		{
			// What is left of them is the news, rather than how far off the full amount that is
			return String.valueOf(change.getLevel());
		}

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
		if (stat.getStat().isPoints())
		{
			return pointsColor(stat);
		}

		int change = stat.getChange();

		if (change < 0)
		{
			return config.debuffColor();
		}

		int threshold = config.buffThreshold();

		return threshold > 0 && change <= threshold ? config.expiringColor() : config.buffColor();
	}
}
