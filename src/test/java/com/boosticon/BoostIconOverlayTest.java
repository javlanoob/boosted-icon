package com.boosticon;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class BoostIconOverlayTest
{
	private static final char EMPTY = '.';
	private static final char BLACK = 'b';
	private static final char LIGHT = '#';

	@Test
	public void outlinesTheLightPartsOfASprite()
	{
		assertEquals(
			Arrays.asList(
				"bbb",
				"b#b",
				"bbb"),
			rows(BoostIconOverlay.outlined(sprite("#"))));
	}

	/**
	 * The skill icons come with a dark edge of their own, and a second pixel of black around that is the
	 * outline looking twice as thick in the places the artwork had already done the job.
	 */
	@Test
	public void leavesThePartsTheArtworkAlreadyOutlined()
	{
		assertEquals(
			Arrays.asList(
				"...",
				".b.",
				"..."),
			rows(BoostIconOverlay.outlined(sprite("b"))));

		assertEquals(
			Arrays.asList(
				"bbb.",
				"b#b.",
				"bbb."),
			rows(BoostIconOverlay.outlined(sprite("#b"))));
	}

	private static BufferedImage sprite(String... rows)
	{
		BufferedImage sprite = new BufferedImage(rows[0].length(), rows.length, BufferedImage.TYPE_INT_ARGB);

		for (int y = 0; y < rows.length; y++)
		{
			for (int x = 0; x < rows[y].length(); x++)
			{
				sprite.setRGB(x, y, pixel(rows[y].charAt(x)));
			}
		}

		return sprite;
	}

	private static int pixel(char symbol)
	{
		switch (symbol)
		{
			case BLACK:
				return Color.BLACK.getRGB();
			case LIGHT:
				return Color.WHITE.getRGB();
			default:
				return 0;
		}
	}

	private static List<String> rows(BufferedImage image)
	{
		List<String> rows = new ArrayList<>();

		for (int y = 0; y < image.getHeight(); y++)
		{
			StringBuilder row = new StringBuilder();

			for (int x = 0; x < image.getWidth(); x++)
			{
				row.append(symbol(image.getRGB(x, y)));
			}

			rows.add(row.toString());
		}

		return rows;
	}

	private static char symbol(int pixel)
	{
		if ((pixel >>> 24) == 0)
		{
			return EMPTY;
		}

		return (pixel & 0xffffff) == 0 ? BLACK : LIGHT;
	}
}
