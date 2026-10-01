package com.boosticon;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
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

	/**
	 * How opaque a pixel has to be to count as part of the icon rather than as room around it, so that
	 * the edge of one is in a definite place for the outline to go around.
	 */
	private static final int SOLID = 128;

	private final Client client;
	private final SpriteManager spriteManager;
	private final BoostIconPlugin plugin;

	/**
	 * The outlined icons, by {@link CombatStat} ordinal, since outlining one is work that only has to
	 * happen once, and finding it again is work for every stat of every player, every frame.
	 */
	private final BufferedImage[] icons = new BufferedImage[CombatStat.ALL.length];

	/**
	 * The height those were prepared at, since a change to the size setting makes them all over again.
	 */
	private int iconHeight;

	/**
	 * The font the overlay was last handed and the size it was last grown by, so that growing it again is
	 * work for a change of setting rather than for every frame.
	 */
	private Font given;
	private int givenSize;
	private Font grown;

	/**
	 * How wide and how tall that font draws, which the client works out behind a lock, so it is asked
	 * the once along with the font rather than once a frame.
	 */
	private FontMetrics metrics;

	@Inject
	BoostIconOverlay(Client client, SpriteManager spriteManager, BoostIconPlugin plugin)
	{
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		this.client = client;
		this.spriteManager = spriteManager;
		this.plugin = plugin;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		List<StatColumn> columns = plugin.getColumns();

		if (columns.isEmpty() || plugin.isHidden())
		{
			return null;
		}

		Settings settings = plugin.getSettings();
		int size = settings.size();

		// Three at the least, since the outline takes a pixel at the top and another at the bottom
		int height = Math.max(3, BASE_HEIGHT + size);

		// Measured the once, since every number in every column is drawn in the same font
		useFont(graphics, size);

		// Put back afterwards, since the graphics goes on to the overlays after this one
		Composite composite = graphics.getComposite();
		graphics.setComposite(settings.composite());

		for (StatColumn column : columns)
		{
			drawColumn(graphics, settings, column, height);
		}

		graphics.setComposite(composite);

		return null;
	}

	private void drawColumn(Graphics2D graphics, Settings settings, StatColumn column, int height)
	{
		Player player = column.getPlayer();
		LocalPoint location = player.getLocalLocation();

		if (location == null)
		{
			return;
		}

		Point middle = middleOf(location, anchor(settings, player));

		if (middle == null)
		{
			return;
		}

		boolean right = settings.right();
		int size = settings.size();

		// The column is drawn upwards from here, so lowering it is taking off the height it starts at
		int adjustIcon = 5 - settings.drop();

		for (StatChange change : column.getChanges())
		{
			BufferedImage icon = icon(change.getStat(), height);

			if (icon == null)
			{
				continue;
			}

			// Hung off the middle of where the column goes, which is half an icon over from its left
			int x = middle.getX() - icon.getWidth() / 2;
			int top = middle.getY() - icon.getHeight() / 2 - adjustIcon;

			// Already the size it is drawn at, so nothing is scaled here
			graphics.drawImage(icon, x + (right ? ICON_OFFSET : -ICON_OFFSET), top, null);

			String label = change.label(settings);

			if (label != null)
			{
				// Written rightwards from where it is put, so the icons being on the left means
				// measuring back from the end of the number instead, or a long one runs into its own
				// icon while a short one sits nowhere near it
				int textX = right
					? x + size + TEXT_OFFSET
					: x + icon.getWidth() - size - TEXT_OFFSET - metrics.stringWidth(label);

				drawOutlined(
					graphics,
					label,
					textX,
					top + (icon.getHeight() + metrics.getAscent()) / 2,
					change.color(settings));
			}

			adjustIcon += icon.getHeight();
		}
	}

	/**
	 * The middle of where a column hangs from, on the canvas. The game's own
	 * {@code Perspective.getCanvasImageLocation} works out this same point and then takes half an image
	 * off it, so the point is worked out once for the whole column here and each icon is put against it
	 * by its own size, rather than projecting the one point again for every icon hanging off it.
	 */
	private Point middleOf(LocalPoint location, int anchor)
	{
		WorldView worldView = client.getWorldView(location.getWorldView());

		return worldView == null
			? null
			: Perspective.localToCanvas(client, location, worldView.getPlane(), anchor);
	}

	/**
	 * Puts the graphics onto the font the numbers are drawn in: the one the overlay was handed, grown by
	 * the size setting. Kept from one frame to the next, along with how it measures, since the overlay is
	 * handed the same font before each of them and the answer only changes when the setting does or the
	 * client is put onto a different font.
	 */
	private void useFont(Graphics2D graphics, int size)
	{
		Font font = graphics.getFont();

		if (font != given || size != givenSize)
		{
			given = font;
			givenSize = size;
			grown = font.deriveFont(font.getStyle(), font.getSize() + size);
			metrics = null;
		}

		graphics.setFont(grown);

		if (metrics == null)
		{
			metrics = graphics.getFontMetrics();
		}
	}

	/**
	 * A number with black put in around it, which is what keeps it readable over whatever the player is
	 * standing on. The same five passes the client's own text component makes, written out here because
	 * that one runs the text past a colour tag pattern on the way, and a number has no tags in it.
	 */
	private static void drawOutlined(Graphics2D graphics, String text, int x, int y, Color color)
	{
		graphics.setColor(Color.BLACK);
		graphics.drawString(text, x, y + 1);
		graphics.drawString(text, x, y - 1);
		graphics.drawString(text, x + 1, y);
		graphics.drawString(text, x - 1, y);
		graphics.setColor(color);
		graphics.drawString(text, x, y);
	}

	/**
	 * How far up the player the column is measured from, which is as tall as the player for the health
	 * bar it normally sits by, and nothing at all for the tile they are standing on.
	 */
	private static int anchor(Settings settings, Player player)
	{
		switch (settings.anchor())
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
			Arrays.fill(icons, null);
			iconHeight = height;
		}

		BufferedImage icon = icons[stat.ordinal()];

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

		icons[stat.ordinal()] = icon;

		return icon;
	}

	/**
	 * The sprite at the height the icons are drawn at, keeping its shape, taken a pixel at a time so
	 * that every pixel of it stays the colour it was drawn in. Blending them instead leaves an edge
	 * that fades out over a pixel or two, which is nowhere in particular for an outline to go, and ends
	 * up with the outline showing along some of an icon and not the rest of it.
	 */
	static BufferedImage scaled(BufferedImage sprite, int height)
	{
		if (height == sprite.getHeight())
		{
			return sprite;
		}

		int width = Math.max(1, Math.round(sprite.getWidth() * height / (float) sprite.getHeight()));
		BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

		for (int x = 0; x < width; x++)
		{
			for (int y = 0; y < height; y++)
			{
				// The middle of what the pixel covers of the sprite, so that it lines up the same way
				// in from either edge
				scaled.setRGB(x, y, sprite.getRGB(
					Math.min((x * 2 + 1) * sprite.getWidth() / (width * 2), sprite.getWidth() - 1),
					Math.min((y * 2 + 1) * sprite.getHeight() / (height * 2), sprite.getHeight() - 1)));
			}
		}

		return scaled;
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
		return (image.getRGB(x, y) >>> 24) < SOLID;
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

				if ((pixel >>> 24) >= SOLID && !dark(pixel))
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
}
