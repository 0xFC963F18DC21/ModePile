package zeroxfc.nullpo.custom.libs;

import mu.nu.nullpo.game.component.Block;
import mu.nu.nullpo.game.component.Piece;
import mu.nu.nullpo.game.event.EventReceiver;
import mu.nu.nullpo.gui.EffectObject;
import mu.nu.nullpo.gui.sdl.NullpoMinoSDL;
import mu.nu.nullpo.gui.sdl.RendererSDL;
import mu.nu.nullpo.gui.sdl.ResourceHolderSDL;
import mu.nu.nullpo.gui.slick.NullpoMinoSlick;
import mu.nu.nullpo.gui.slick.RendererSlick;
import mu.nu.nullpo.gui.slick.ResourceHolder;
import mu.nu.nullpo.gui.swing.RendererSwing;
import org.apache.log4j.Logger;
import mu.nu.nullpo.gui.swing.ResourceHolderSwing;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import sdljava.video.SDLRect;
import sdljava.video.SDLSurface;
import sdljava.video.SDLVideo;
import zeroxfc.nullpo.custom.libs.backgroundtypes.AnimatedBackgroundHook;

import java.awt.*;
import java.lang.reflect.*;
import java.util.ArrayList;

public class RendererExtension {
	/** Debug logger */
	private static Logger log = Logger.getLogger(RendererExtension.class);

	/** Run class in debug mode? */
	private static final boolean DEBUG = true;

	/** Block break effect types used in addBlockBreakEffect() */
	public static final int TYPE_NORMAL_BREAK = 1, TYPE_GEM_BREAK = 2;

	/** Text alignment option */
	public static final int ALIGN_TOP_LEFT = 0,
	                        ALIGN_TOP_MIDDLE = 1,
	                        ALIGN_TOP_RIGHT = 2,
	                        ALIGN_MIDDLE_LEFT = 3,
	                        ALIGN_MIDDLE_MIDDLE = 4,
	                        ALIGN_MIDDLE_RIGHT = 5,
	                        ALIGN_BOTTOM_LEFT = 6,
	                        ALIGN_BOTTOM_MIDDLE = 7,
	                        ALIGN_BOTTOM_RIGHT = 8;

	/**
	 * Draw piece
	 * @param x X-Coordinate of piece
	 * @param y Y-Coordinate of piece
	 * @param piece The piece to draw
	 * @param scale Scale factor at which the piece is drawn in (0.5f, 1f or 2f)
	 * @param darkness Darkness value (0f = None, negative = lighter, positive = darker)
	 */
	public static void drawPiece(EventReceiver receiver, int x, int y, Piece piece, float scale, float darkness) {
		final int renderer = AnimatedBackgroundHook.getResourceHook();

		Class<?> local;
		Method localMethod;

		switch (renderer) {
			case AnimatedBackgroundHook.HOLDER_SLICK:
				local = RendererSlick.class;
				break;
			case AnimatedBackgroundHook.HOLDER_SWING:
				local = RendererSwing.class;
				break;
			case AnimatedBackgroundHook.HOLDER_SDL:
				local = RendererSDL.class;
				break;
			default:
				return;
		}

		try {
			localMethod = local.getDeclaredMethod("drawPiece", int.class, int.class, Piece.class, float.class, float.class);
			localMethod.setAccessible(true);
			localMethod.invoke(receiver, x, y, piece, scale, darkness);
		} catch (Exception e) {
			if (DEBUG) log.error("Failed to extract and invoke drawPiece() method from renderer.");
		}
	}

	/**
	 * Draw a custom-scaled piece that does not have to have a scale of 0.5f, 1f or 2f.
	 * @param receiver Renderer to draw with
	 * @param x X-coordinate of piece's top-left corner
	 * @param y Y-coordinate of piece's top-left corner
	 * @param piece The piece to draw
	 * @param scale Scale factor at which the piece is drawn in
	 * @param darkness Darkness value (0f = None, negative = lighter, positive = darker)
	 */
	public static void drawScaledPiece(EventReceiver receiver, int x, int y, Piece piece, float scale, float darkness) {
		for(int i = 0; i < piece.getMaxBlock(); i++) {
			int x2 = x + (int)(piece.dataX[piece.direction][i] * 16 * scale);
			int y2 = y + (int)(piece.dataY[piece.direction][i] * 16 * scale);

			Block blkTemp = new Block(piece.block[i]);
			blkTemp.darkness = darkness;

			drawScaledBlock(receiver, x2, y2, blkTemp.color, blkTemp.skin, blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_BONE), blkTemp.darkness, 1f, scale, blkTemp.attribute);
		}
	}

	/**
	 * Draw a custom-scaled piece that does not have to have a scale of 0.5f, 1f or 2f.<br />
	 * <br />
	 * It can be aligned to any corner, side midpoint or centre of its bounding box.
	 * @param receiver Renderer to draw with
	 * @param x X-coordinate of piece's top-left corner
	 * @param y Y-coordinate of piece's top-left corner
	 * @param alignment Alignment setting ID (use the ones in this class)
	 * @param piece The piece to draw
	 * @param scale Scale factor at which the piece is drawn in
	 * @param darkness Darkness value (0f = None, negative = lighter, positive = darker)
	 */
	public static void drawAlignedScaledPiece(EventReceiver receiver, int x, int y, int alignment, Piece piece, float scale, float darkness) {
		final int baseSize = 16 * Math.max(piece.getWidth(), piece.getHeight());
		int offsetX, offsetY;

		switch (alignment) {
			case ALIGN_TOP_MIDDLE:
			case ALIGN_MIDDLE_MIDDLE:
			case ALIGN_BOTTOM_MIDDLE:
				offsetX = (int)(baseSize * 0.5f * scale);
				break;
			case ALIGN_TOP_RIGHT:
			case ALIGN_MIDDLE_RIGHT:
			case ALIGN_BOTTOM_RIGHT:
				offsetX = (int)(baseSize * scale);
				break;
			default:
				offsetX = 0;
				break;
		}

		switch (alignment) {
			case ALIGN_MIDDLE_LEFT:
			case ALIGN_MIDDLE_MIDDLE:
			case ALIGN_MIDDLE_RIGHT:
				offsetY = (int)(baseSize * 0.5f * scale);
				break;
			case ALIGN_BOTTOM_LEFT:
			case ALIGN_BOTTOM_MIDDLE:
			case ALIGN_BOTTOM_RIGHT:
				offsetY = (int)(baseSize * scale);
				break;
			default:
				offsetY = 0;
				break;
		}

		drawScaledPiece(receiver, x - offsetX, y - offsetY, piece, scale, darkness);
	}

	/**
	 * Add block break effect at custom location given
	 * @param receiver Current renderer in game
	 * @param x X-Coordinate of top left corner of 16x16 block
	 * @param y Y-Coordinate of top left corner of 16x16 block
	 * @param blk Block to break
	 */
	public static void addBlockBreakEffect(EventReceiver receiver, int x, int y, Block blk) {
		if (receiver == null || blk == null) return;
		addBlockBreakEffect(receiver, blk.isGemBlock() ? 2 : 1, x, y, blk.getDrawColor());
	}

	/**
	 * Draw a block break effect at a custom position where coordinates stood for the left-hand corner of a block.
	 * @param receiver Current renderer in game
	 * @param effectType Block break effect type
	 * @param x X-Coordinate of top left corner of 16x16 block
	 * @param y Y-Coordinate of top left corner of 16x16 block
	 * @param color Effect colour
	 */
	public static void addBlockBreakEffect(EventReceiver receiver, int effectType, int x, int y, int color) {
		if (receiver == null) return;

		int renderer = AnimatedBackgroundHook.getResourceHook();
		if (renderer == AnimatedBackgroundHook.HOLDER_SWING) return;

		Class<?> local;
		Field effectList;
		ArrayList<EffectObject> list;
		boolean show;

		switch (renderer) {
			case AnimatedBackgroundHook.HOLDER_SLICK:
				local = RendererSlick.class;
				show = NullpoMinoSlick.propConfig.getProperty("option.showlineeffect", true);
				break;
			case AnimatedBackgroundHook.HOLDER_SDL:
				local = RendererSDL.class;
				show = NullpoMinoSDL.propConfig.getProperty("option.showlineeffect", true);
				break;
			default:
				return;
		}

		try {
			if (show) {
				effectList = local.getDeclaredField("effectlist");
				effectList.setAccessible(true);

				list = (ArrayList<EffectObject>)(effectList.get(receiver));

				list.add(new EffectObject(effectType, x, y, color));
				effectList.set(receiver, list);
			}
		} catch (Exception e) {
			if (DEBUG) log.error("Failed to extract, modify and place back effectList.");
		}
	}

	// TODO: Make aligned piece and block draw methods.

	/**
	 * Draws a block that can be scaled to a scale that isn't 0.5f, 1f or 2f.
	 * @param receiver Renderer to draw with
	 * @param x X-coordinate of block top-left corner
	 * @param y Y-coordinate of block top-left corner
	 * @param color Block colour (use colours in <code>Block</code> class)
	 * @param skin Block skin (when in doubt use <code>getSkin()</code> on a <code>GameEngine</code> instance)
	 * @param bone Use bone block skin?
	 * @param darkness Darkness value (0f = None, negative = lighter, positive = darker)
	 * @param alpha Alpha value (transparency; 1f = opaque, 0f = transparent)
	 * @param scale Scale of drawing
	 * @param attr Block attributes (use attrs in <code>Block</code> class and combine with <code>|</code>, or use <code>0</code> for none)
	 */
	public static void drawScaledBlock(EventReceiver receiver, int x, int y, int color, int skin, boolean bone, float darkness, float alpha, float scale, int attr) {
		final int renderer = AnimatedBackgroundHook.getResourceHook();

		if (renderer == AnimatedBackgroundHook.HOLDER_SLICK) {
			// region Slick Case
			Graphics graphics = ResourceHolderCustomAssetExtension.getGraphicsSlick((RendererSlick) receiver);
			if (graphics == null) return;

			if ((color <= Block.BLOCK_COLOR_INVALID)) return;
			if (skin >= ResourceHolder.imgNormalBlockList.size()) skin = 0;

			boolean isSpecialBlocks = (color >= Block.BLOCK_COLOR_COUNT);
			boolean isSticky = ResourceHolder.blockStickyFlagList.get(skin);

			int size = (int) (16 * scale);

			int srcSize;
			if (scale <= 0.5f) {
				srcSize = 8;
			} else if (scale <= 1.0f) {
				srcSize = 16;
			} else {
				srcSize = 32;
			}

			Image img = null;
			if (scale <= 0.5f)
				img = ResourceHolder.imgSmallBlockList.get(skin);
			else if (scale > 1.0f)
				img = ResourceHolder.imgBigBlockList.get(skin);
			else
				img = ResourceHolder.imgNormalBlockList.get(skin);

			int sx = color * srcSize;
			if (bone) sx += 9 * srcSize;
			int sy = 0;
			if (isSpecialBlocks) sx = ((color - Block.BLOCK_COLOR_COUNT) + 18) * srcSize;

			if (isSticky) {
				if (isSpecialBlocks) {
					sx = (color - Block.BLOCK_COLOR_COUNT) * srcSize;
					sy = 18 * srcSize;
				} else {
					sx = 0;
					if ((attr & Block.BLOCK_ATTRIBUTE_CONNECT_UP) != 0) sx |= 0x1;
					if ((attr & Block.BLOCK_ATTRIBUTE_CONNECT_DOWN) != 0) sx |= 0x2;
					if ((attr & Block.BLOCK_ATTRIBUTE_CONNECT_LEFT) != 0) sx |= 0x4;
					if ((attr & Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT) != 0) sx |= 0x8;
					sx *= srcSize;
					sy = color * srcSize;
					if (bone) sy += 9 * srcSize;
				}
			}

			int imageWidth = img.getWidth();
			if ((sx >= imageWidth) && (imageWidth != -1)) sx = 0;
			int imageHeight = img.getHeight();
			if ((sy >= imageHeight) && (imageHeight != -1)) sy = 0;

			Color filter = new Color(Color.white);
			filter.a = alpha;
			if (darkness > 0) {
				filter = filter.darker(darkness);
			}

			graphics.drawImage(img, x, y, x + size, y + size, sx, sy, sx + srcSize, sy + srcSize, filter);

			if (isSticky && !isSpecialBlocks) {
				int d = 16 * size;
				int h = size / 2;
				int d2 = 16 * srcSize;
				int h2 = srcSize / 2;

				if (((attr & Block.BLOCK_ATTRIBUTE_CONNECT_UP) != 0) && ((attr & Block.BLOCK_ATTRIBUTE_CONNECT_LEFT) != 0))
					graphics.drawImage(img, x, y, x + h, y + h, d2, sy, d2 + h2, sy + h2, filter);
				if (((attr & Block.BLOCK_ATTRIBUTE_CONNECT_UP) != 0) && ((attr & Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT) != 0))
					graphics.drawImage(img, x + h, y, x + h + h, y + h, d2 + h2, sy, d2 + h2 + h2, sy + h2, filter);
				if (((attr & Block.BLOCK_ATTRIBUTE_CONNECT_DOWN) != 0) && ((attr & Block.BLOCK_ATTRIBUTE_CONNECT_LEFT) != 0))
					graphics.drawImage(img, x, y + h, x + h, y + h + h, d2, sy + h2, d2 + h2, sy + h2 + h2, filter);
				if (((attr & Block.BLOCK_ATTRIBUTE_CONNECT_DOWN) != 0) && ((attr & Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT) != 0))
					graphics.drawImage(img, x + h, y + h, x + h + h, y + h + h, d2 + h2, sy + h2, d2 + h2 + h2, sy + h2 + h2, filter);
			}

			if (darkness < 0) {
				Color brightfilter = new Color(Color.white);
				brightfilter.a = -darkness;
				graphics.setColor(brightfilter);
				graphics.fillRect(x, y, size, size);
			}

			ResourceHolderCustomAssetExtension.setGraphicsSlick((RendererSlick)receiver, graphics);
			// endregion Slick Case
		} else if (renderer == AnimatedBackgroundHook.HOLDER_SWING) {
			// region Swing Case
			Graphics2D graphics = ResourceHolderCustomAssetExtension.getGraphicsSwing((RendererSwing)receiver);
			if(graphics == null) return;

			if((color <= Block.BLOCK_COLOR_INVALID)) return;
			if(skin >= ResourceHolderSwing.imgNormalBlockList.size()) skin = 0;

			boolean isSpecialBlocks = (color >= Block.BLOCK_COLOR_COUNT);
			boolean isSticky = ResourceHolderSwing.blockStickyFlagList.get(skin);

			int size = (int)(16 * scale);

			int srcSize;
			if (scale <= 0.5f) {
				srcSize = 8;
			} else if (scale <= 1.0f) {
				srcSize = 16;
			} else {
				srcSize = 32;
			}

			java.awt.Image img = null;
			if(scale <= 0.5f)
				img = ResourceHolderSwing.imgSmallBlockList.get(skin);
			else if(scale > 1.0f)
				img = ResourceHolderSwing.imgBigBlockList.get(skin);
			else
				img = ResourceHolderSwing.imgNormalBlockList.get(skin);

			int sx = color * srcSize;
			if (bone) sx += 9 * srcSize;
			int sy = 0;
			if (isSpecialBlocks) sx = ((color - Block.BLOCK_COLOR_COUNT) + 18) * srcSize;

			if (isSticky) {
				if (isSpecialBlocks) {
					sx = (color - Block.BLOCK_COLOR_COUNT) * srcSize;
					sy = 18 * srcSize;
				} else {
					sx = 0;
					if ((attr & Block.BLOCK_ATTRIBUTE_CONNECT_UP) != 0) sx |= 0x1;
					if ((attr & Block.BLOCK_ATTRIBUTE_CONNECT_DOWN) != 0) sx |= 0x2;
					if ((attr & Block.BLOCK_ATTRIBUTE_CONNECT_LEFT) != 0) sx |= 0x4;
					if ((attr & Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT) != 0) sx |= 0x8;
					sx *= srcSize;
					sy = color * srcSize;
					if (bone) sy += 9 * srcSize;
				}
			}

			int imageWidth = img.getWidth(null);
			if((sx >= imageWidth) && (imageWidth != -1)) sx = 0;
			int imageHeight = img.getHeight(null);
			if((sy >= imageHeight) && (imageHeight != -1)) sy = 0;

			Composite backupComposite = graphics.getComposite();

			boolean showbg = AnimatedBackgroundHook.getBGState(receiver);
			if((alpha >= 0f) && (alpha < 1f) && (!showbg)) {
				AlphaComposite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
				graphics.setComposite(composite);
			}

			boolean simpleblock;
			try {
				Class<RendererSwing> rs = RendererSwing.class;
				Field sb = rs.getDeclaredField("simpleblock");
				sb.setAccessible(true);
				simpleblock = sb.getBoolean(receiver);
			} catch (Exception e) {
				if (DEBUG) log.error("Failed to extract boolean from simpleblock field in Swing Renderer");
				return;
			}

			if(simpleblock) {
				switch(color) {
					case Block.BLOCK_COLOR_GRAY:
						graphics.setColor(java.awt.Color.lightGray);
						break;
					case Block.BLOCK_COLOR_RED:
						graphics.setColor(java.awt.Color.red);
						break;
					case Block.BLOCK_COLOR_ORANGE:
						graphics.setColor(java.awt.Color.orange);
						break;
					case Block.BLOCK_COLOR_YELLOW:
						graphics.setColor(java.awt.Color.yellow);
						break;
					case Block.BLOCK_COLOR_GREEN:
						graphics.setColor(java.awt.Color.green);
						break;
					case Block.BLOCK_COLOR_CYAN:
						graphics.setColor(java.awt.Color.cyan);
						break;
					case Block.BLOCK_COLOR_BLUE:
						graphics.setColor(java.awt.Color.blue);
						break;
					case Block.BLOCK_COLOR_PURPLE:
						graphics.setColor(java.awt.Color.magenta);
						break;
					default:
						graphics.setColor(java.awt.Color.white);
						break;
				}
				graphics.drawRect(x, y, size-1, size-1);

				if(showbg) {
					graphics.setColor(java.awt.Color.black);
					graphics.fillRect(x + 1, y + 1, size - 2, size - 2);
				}
			} else {
				graphics.drawImage(img, x, y, x + size, y + size, sx, sy, sx + srcSize, sy + srcSize, null);

				if (isSticky && !isSpecialBlocks) {
					int d = 16 * size;
					int h = size / 2;
					int d2 = 16 * srcSize;
					int h2 = srcSize / 2;

					if (((attr & Block.BLOCK_ATTRIBUTE_CONNECT_UP) != 0) && ((attr & Block.BLOCK_ATTRIBUTE_CONNECT_LEFT) != 0))
						graphics.drawImage(img, x, y, x + h, y + h, d2, sy, d2 + h2, sy + h2, null);
					if (((attr & Block.BLOCK_ATTRIBUTE_CONNECT_UP) != 0) && ((attr & Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT) != 0))
						graphics.drawImage(img, x + h, y, x + h + h, y + h, d2 + h2, sy, d2 + h2 + h2, sy + h2, null);
					if (((attr & Block.BLOCK_ATTRIBUTE_CONNECT_DOWN) != 0) && ((attr & Block.BLOCK_ATTRIBUTE_CONNECT_LEFT) != 0))
						graphics.drawImage(img, x, y + h, x + h, y + h + h, d2, sy + h2, d2 + h2, sy + h2 + h2, null);
					if (((attr & Block.BLOCK_ATTRIBUTE_CONNECT_DOWN) != 0) && ((attr & Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT) != 0))
						graphics.drawImage(img, x + h, y + h, x + h + h, y + h + h, d2 + h2, sy + h2, d2 + h2 + h2, sy + h2 + h2, null);
				}
			}

			graphics.setComposite(backupComposite);

			if( (darkness != 0) || ((alpha >= 0f) && (alpha < 1f) && (showbg)) ) {
				java.awt.Color backupColor = graphics.getColor();

				java.awt.Color filterColor;
				if((alpha >= 0f) && (alpha < 1f) && (showbg)) {
					filterColor = new java.awt.Color(0f, 0f, 0f, alpha);
				} else if(darkness > 0) {
					filterColor = new java.awt.Color(0f, 0f, 0f, darkness);
				} else {
					filterColor = new java.awt.Color(1f, 1f, 1f, -darkness);
				}

				graphics.setColor(filterColor);
				graphics.fillRect(x, y, size, size);
				graphics.setColor(backupColor);
			}
			// endregion Swing Case
		} else if (renderer == AnimatedBackgroundHook.HOLDER_SDL) {
			try {
				// region SDL Case
				SDLSurface graphics = ResourceHolderCustomAssetExtension.getGraphicsSDL((RendererSDL) receiver);
				if (graphics == null) return;

				if (color <= Block.BLOCK_COLOR_INVALID) return;
				if (skin >= ResourceHolderSDL.imgNormalBlockList.size()) skin = 0;

				boolean isSpecialBlocks = (color >= Block.BLOCK_COLOR_COUNT);
				boolean isSticky = ResourceHolderSDL.blockStickyFlagList.get(skin);

				int size = (int) (16 * scale);

				int srcSize;
				if (scale <= 0.5f) {
					srcSize = 8;
				} else if (scale <= 1.0f) {
					srcSize = 16;
				} else {
					srcSize = 32;
				}

				SDLSurface img = null;
				if (scale <= 0.5f)
					img = ResourceHolderSDL.imgSmallBlockList.get(skin);
				else if (scale > 1.0f)
					img = ResourceHolderSDL.imgBigBlockList.get(skin);
				else
					img = ResourceHolderSDL.imgNormalBlockList.get(skin);

				int sx = color * srcSize;
				if (bone) sx += 9 * srcSize;
				int sy = 0;
				if (isSpecialBlocks) sx = ((color - Block.BLOCK_COLOR_COUNT) + 18) * srcSize;

				if (isSticky) {
					if (isSpecialBlocks) {
						sx = (color - Block.BLOCK_COLOR_COUNT) * srcSize;
						sy = 18 * srcSize;
					} else {
						sx = 0;
						if ((attr & Block.BLOCK_ATTRIBUTE_CONNECT_UP) != 0) sx |= 0x1;
						if ((attr & Block.BLOCK_ATTRIBUTE_CONNECT_DOWN) != 0) sx |= 0x2;
						if ((attr & Block.BLOCK_ATTRIBUTE_CONNECT_LEFT) != 0) sx |= 0x4;
						if ((attr & Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT) != 0) sx |= 0x8;
						sx *= srcSize;
						sy = color * srcSize;
						if (bone) sy += 9 * srcSize;
					}
				}

				int imageWidth = img.getWidth();
				if ((sx >= imageWidth) && (imageWidth != -1)) sx = 0;
				int imageHeight = img.getHeight();
				if ((sy >= imageHeight) && (imageHeight != -1)) sy = 0;

				SDLRect rectSrc = new SDLRect(sx, sy, srcSize, srcSize);
				SDLRect rectDst = new SDLRect(x, y, size, size);

				NullpoMinoSDL.fixRect(rectSrc, rectDst);

				if (alpha < 1.0f) {
					int alphalv = (int) (255 * alpha);
					img.setAlpha(SDLVideo.SDL_SRCALPHA | SDLVideo.SDL_RLEACCEL, alphalv);
				} else {
					img.setAlpha(0, 255);
				}

				img.blitSurface(rectSrc, graphics, rectDst);

				if (isSticky && !isSpecialBlocks) {
					int d = 16 * size;
					int h = (size / 2);
					int d2 = 16 * srcSize;
					int h2 = srcSize / 2;

					SDLRect rectDst2 = null;
					SDLRect rectSrc2 = null;

					if (((attr & Block.BLOCK_ATTRIBUTE_CONNECT_UP) != 0) && ((attr & Block.BLOCK_ATTRIBUTE_CONNECT_LEFT) != 0)) {
						rectDst2 = new SDLRect(x, y, h, h);
						rectSrc2 = new SDLRect(d2, sy, h2, h2);
						NullpoMinoSDL.fixRect(rectSrc2, rectDst2);
						img.blitSurface(rectSrc2, graphics, rectDst2);
						//graphics.drawImage(img, x, y, x + h, y + h, d, sy, d + h, sy + h, filter);
					}
					if (((attr & Block.BLOCK_ATTRIBUTE_CONNECT_UP) != 0) && ((attr & Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT) != 0)) {
						rectDst2 = new SDLRect(x + h, y, h, h);
						rectSrc2 = new SDLRect(d2 + h2, sy, h2, h2);
						NullpoMinoSDL.fixRect(rectSrc2, rectDst2);
						img.blitSurface(rectSrc2, graphics, rectDst2);
						//graphics.drawImage(img, x + h, y, x + h + h, y + h, d + h, sy, d + h + h, sy + h, filter);
					}
					if (((attr & Block.BLOCK_ATTRIBUTE_CONNECT_DOWN) != 0) && ((attr & Block.BLOCK_ATTRIBUTE_CONNECT_LEFT) != 0)) {
						rectDst2 = new SDLRect(x, y + h, h, h);
						rectSrc2 = new SDLRect(d2, sy + h2, h2, h2);
						NullpoMinoSDL.fixRect(rectSrc2, rectDst2);
						img.blitSurface(rectSrc2, graphics, rectDst2);
						//graphics.drawImage(img, x, y + h, x + h, y + h + h, d, sy + h, d + h, sy + h + h, filter);
					}
					if (((attr & Block.BLOCK_ATTRIBUTE_CONNECT_DOWN) != 0) && ((attr & Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT) != 0)) {
						rectDst2 = new SDLRect(x + h, y + h, h, h);
						rectSrc2 = new SDLRect(d2 + h2, sy + h2, h2, h2);
						NullpoMinoSDL.fixRect(rectSrc2, rectDst2);
						img.blitSurface(rectSrc2, graphics, rectDst2);
						//graphics.drawImage(img, x + h, y + h, x + h + h, y + h + h, d + h, sy + h, d + h + h, sy + h + h, filter);
					}
				}

				if (darkness > 0) {
					int alphalv = (int) (255 * darkness);
					ResourceHolderSDL.imgBlankBlack.setAlpha(SDLVideo.SDL_SRCALPHA | SDLVideo.SDL_RLEACCEL, alphalv);
					ResourceHolderSDL.imgBlankBlack.blitSurface(new SDLRect(0, 0, size, size), graphics, rectDst);
				} else if (darkness < 0) {
					int alphalv = (int) (255 * -darkness);
					ResourceHolderSDL.imgBlankWhite.setAlpha(SDLVideo.SDL_SRCALPHA | SDLVideo.SDL_RLEACCEL, alphalv);
					ResourceHolderSDL.imgBlankWhite.blitSurface(new SDLRect(0, 0, size, size), graphics, rectDst);
				}
				// endregion SDL Case
			} catch (Exception e) {
				if (DEBUG) log.error("Failed to draw block using SDL renderer:\n" + e.getMessage());
			}
		}
	}

	/**
	 * Draws a block that can be scaled to a scale that isn't 0.5f, 1f or 2f.<br />
	 * <br />
	 * It can be aligned to a corner, the midpoint of one of its sides or its centre.
	 * @param receiver Renderer to draw with
	 * @param x X-coordinate of block anchor point
	 * @param y Y-coordinate of block anchor point
	 * @param alignment Alignment setting ID (use the ones in this class)
	 * @param color Block colour (use colours in <code>Block</code> class)
	 * @param skin Block skin (when in doubt use <code>getSkin()</code> on a <code>GameEngine</code> instance)
	 * @param bone Use bone block skin?
	 * @param darkness Darkness value (0f = None, negative = lighter, positive = darker)
	 * @param alpha Alpha value (transparency; 1f = opaque, 0f = transparent)
	 * @param scale Scale of drawing
	 * @param attr Block attributes (use attrs in <code>Block</code> class and combine with <code>|</code>, or use <code>0</code> for none)
	 */
	public static void drawAlignedScaledBlock(EventReceiver receiver, int x, int y, int alignment, int color, int skin, boolean bone, float darkness, float alpha, float scale, int attr) {
		final int baseSize = 16;
		int offsetX, offsetY;

		switch (alignment) {
			case ALIGN_TOP_MIDDLE:
			case ALIGN_MIDDLE_MIDDLE:
			case ALIGN_BOTTOM_MIDDLE:
				offsetX = (int)(baseSize * 0.5f * scale);
				break;
			case ALIGN_TOP_RIGHT:
			case ALIGN_MIDDLE_RIGHT:
			case ALIGN_BOTTOM_RIGHT:
				offsetX = (int)(baseSize * scale);
				break;
			default:
				offsetX = 0;
				break;
		}

		switch (alignment) {
			case ALIGN_MIDDLE_LEFT:
			case ALIGN_MIDDLE_MIDDLE:
			case ALIGN_MIDDLE_RIGHT:
				offsetY = (int)(baseSize * 0.5f * scale);
				break;
			case ALIGN_BOTTOM_LEFT:
			case ALIGN_BOTTOM_MIDDLE:
			case ALIGN_BOTTOM_RIGHT:
				offsetY = (int)(baseSize * scale);
				break;
			default:
				offsetY = 0;
				break;
		}

		drawScaledBlock(receiver, x - offsetX, y - offsetY, color, skin, bone, darkness, alpha, scale, attr);
	}
}
