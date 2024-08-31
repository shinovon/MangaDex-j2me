/**
 * Copyright (c) 2021 Fyodor Ryzhov
 * Copyright (c) 2024 Arman Jussupgaliyev
 */
import java.io.IOException;
import java.util.Vector;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.TextBox;

// from njtai
public class ViewCommon extends Canvas implements Runnable, CommandListener, LangConstants {
	/**
	 * Number of page from zero.
	 */
	protected int page;

	protected byte[][] cache;
	private int[] cacheProgress;

	protected float zoom = 1;
	protected float x = 0;
	protected float y = 0;

	protected Thread loader;
	protected Thread[] preloaders;
	private Vector preloaderQueue;
	protected boolean error;

	int nokiaRam;

	private boolean cacheOk = false;

	static Image slider;
	
	private boolean hwa;
	
	// SWR only
	private Image toDraw;
	private Image orig;

	private boolean firstDraw = true;
	
	boolean cover;
	boolean longscroll;

	private long chapterShown;
	private boolean resizing;
	
	int loaderAction;
	
	boolean menu;


	/**
	 * Creates the view.
	 * 
	 * @param emo  Object with data.
	 * @param prev Previous screen.
	 * @param page Number of page to start.
	 */
	public ViewCommon(int page, boolean hwa) {
		if (page == -2) cover = true;
		this.page = page;
		this.hwa = hwa;
		nokiaRam = (System.getProperty("microedition.platform").indexOf("sw_platform_version=5.") == -1)
				? (15 * 1024 * 1024)
				: (40 * 1024 * 1024);
		reload(0);
		setFullScreenMode(true);
		if (slider == null) {
			try {
				slider = Image.createImage("/slider.png");
			} catch (IOException e) {
				slider = null;
			}
		}
	}

	/**
	 * Loads an image, optionally ignoring the cache.
	 * 
	 * @param n Number of image (not page!) [0; MangaApp.chapterPages)
	 * @return Data of loaded image.
	 * @throws InterruptedException
	 */
	protected final byte[] getImage(int n, boolean forceCacheIgnore) throws InterruptedException {
		if (forceCacheIgnore)
			Thread.sleep(500);
		if (cover) {
			try {
				return MangaApp.getCover(null);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		if (MangaApp.chapterFileCache && !forceCacheIgnore) {
			if (cacheProgress == null) {
				cacheProgress = new int[MangaApp.chapterPages];
			}
			byte[] a = MangaApp.readCachedPage(n);
			if (a != null) {
				cacheProgress[n] = 1;
				return a;
			}
			try {
				a = MangaApp.getPage(n, "");
				if (a == null) {
					error = true;
					repaint();
					return null;
				}

				if (!cacheOk) {
					cacheOk = true;
					MangaApp.createFolder();
//					MangaApp.writeChapterModel();
				}
				
				MangaApp.cachePage(a, n);
				cacheProgress[n] = 1;
				return a;
			} catch (IOException e) {
				cacheProgress[n] = 2;
				e.printStackTrace();
				return null;
			}
		}

		try {
			if (cache == null) {
				cache = new byte[MangaApp.chapterPages][];
				cacheProgress = new int[MangaApp.chapterPages];
			}
			if (forceCacheIgnore) {
				cache[n] = null;
			} else if (cache[n] != null) {
				return cache[n];
			}
			
			synchronized (cache) {
				long ct = System.currentTimeMillis();
				if (ct - lastTime < 350)
					Thread.sleep(ct - lastTime);
				lastTime = ct;
				try {
					byte[] b = MangaApp.getPage(n, "");
					if (b == null) {
						cacheProgress[n] = 2;
						error = true;
						repaint();
						return null;
					}
					cacheProgress[n] = 1;
					return cache[n] = b;
				} catch (IOException e) {
					cacheProgress[n] = 2;
					e.printStackTrace();
					return null;
				}
			}
		} catch (RuntimeException e) {
			return null;
		}

	}
	
	private final byte[] getResizedImage(int n, int size) {
		String s = ";tw="+(getWidth()*size)+(!longscroll?";th="+(getHeight()*size):"");
		if (cover) {
			try {
				return MangaApp.getCover(s);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		try {
			return MangaApp.getPage(n, s);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Releases some images to prevent OOM errors.
	 */
	protected synchronized final void emergencyCacheClear() {
		try {
			if (MangaApp.chapterFileCache) {
				cache = null;
				return;
			}
			if (cache == null) {
				return;
			}
			for (int i = 0; i < page - 1; i++) {
				cache[i] = null;
				cacheProgress[i] = 0;
			}
			for (int i = MangaApp.chapterPages - 1; i > page; i--) {
				if (cache[i] != null) {
					cache[i] = null;
					cacheProgress[i] = 0;
					break;
				}
			}
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Tries to guess, how many pages can be preloaded before problems with free
	 * memory.
	 * 
	 * @return Aproximatry count of pages that is safe to load.
	 */
	protected final int canStorePages() {
		if (MangaApp.chapterFileCache) {
			return 999;
		}
		int f = (int) Runtime.getRuntime().freeMemory();
		int free = 0;
		try {
			String nokiaMem = System.getProperty("com.nokia.memoryramfree");
			free = (int) Math.min(Integer.parseInt(nokiaMem), nokiaRam - (Runtime.getRuntime().totalMemory() - f));
		} catch (Throwable t) {
			free = f;
		}
		free = free - (10 * 1024 * 1024);
		int p = free / (300 * 1024);
		if (p < 0) p = 0;
		return p;
	}

	protected synchronized final void checkCacheAfterPageSwitch() {
		if (MangaApp.chapterFileCache) {
			cache = null;
			return;
		}
		if (cache == null) {
			return;
		}
		if (MangaApp.cachingPolicy == 0) {
			for (int i = 0; i < cache.length; i++) {
				if (i != page) {
					cache[i] = null;
					cacheProgress[i] = 0;
				}
			}
		} else {
			if (canStorePages() <= 2) {
				for (int i = 0; i < page - 1; i++) {
					if (canStorePages() <= 2) {
						cache[i] = null;
						cacheProgress[i] = 0;
					}
				}
				for (int i = MangaApp.chapterPages - 1; i > page; i--) {
					if (canStorePages() == 0) {
						cache[i] = null;
						cacheProgress[i] = 0;
					} else {
						break;
					}
				}
			} else if (MangaApp.cachingPolicy == 2) {
				runPreloader();
			}

		}

	}

	long lastTime = System.currentTimeMillis();

	public final void run() {
		try {
			if (loaderAction == 1) {
				Thread.sleep(100);
			}
			synchronized (this) {
				error = false;
				zoom = 1;
				x = 0;
				y = 0;
				reset();
				chapterShown = System.currentTimeMillis();
				try {
					prepare();
					repaint();
					resize(1);
					zoom = 1;
				} catch (Exception e) {
					error = true;
					e.printStackTrace();
				}
				chapterShown = System.currentTimeMillis();
				repaint();
				runPreloader();
			}
		} catch (OutOfMemoryError e) {
			cache = null;
			MangaApp.display(null, true);
			try {
				Thread.sleep(100);
			} catch (Exception ignored) {}
			MangaApp.display(new Alert("Error", "Not enough memory to continue viewing. Try to disable caching.", null,
					AlertType.ERROR));
			return;
		} catch (Exception e) {}
	}

	private final void runPreloader() {
		if (preloaders == null && MangaApp.cachingPolicy == 2) {
			int count = MangaApp.multiPreloader ? 3 : 1; // TODO
			preloaders = new Thread[count];
			preloaderQueue = new Vector();
			
			int pages = MangaApp.chapterPages;
			for (int i = 0; i < pages; i++) {
				preloaderQueue.addElement(new Integer(i));
			}
			if (cacheProgress == null) {
				cacheProgress = new int[pages];
			}
			if (!MangaApp.chapterFileCache && cache == null) {
				cache = new byte[pages][];
			}
			for (int i = 0; i < count; i++) {
				preloaders[i] = MangaApp.midlet.start(MangaApp.RUN_PRELOADER);
			}
		}
	}

	int preloadProgress = 101;

	public final void preload() throws InterruptedException {
		Thread.sleep(500);
		while (preloaderQueue.size() > 0) {
			int i = -1;
			try {
				if (preloadProgress > 101) return;
				synchronized (preloaderQueue) {
					i = ((Integer) preloaderQueue.elementAt(0)).intValue();
					preloaderQueue.removeElementAt(0);
				}
				if (i < 0) continue;
				if (MangaApp.chapterFileCache) {
					try {
						getImage(i, false);
						repaint();
						Thread.sleep(200);
					} catch (InterruptedException e) {
						e.printStackTrace();
						preloadProgress = 103;
						repaint();
						return;
					} catch (OutOfMemoryError e) {
						cacheProgress[i] = 2;
						preloadProgress = 104;
						repaint();
						return;
					} catch (Throwable e) {
						cacheProgress[i] = 2;
						error = true;
						repaint();
					}
				} else {
					if (cache == null) return;
					if (cache[i] != null) continue;
					try {
						if (cache[i] != null) {
							continue;
						}
						if (canStorePages() < 1) {
							preloadProgress = 102;
							return;
						}
						getImage(i, false);
						Thread.sleep(200);
						repaint();
						Thread.sleep(50);
					} catch (InterruptedException e) {
						e.printStackTrace();
						preloadProgress = 103;
						repaint();
						return;
					} catch (OutOfMemoryError e) {
						emergencyCacheClear();
						preloadProgress = 104;
						repaint();
						return;
					} catch (NullPointerException e) {
						preloadProgress = 100;
						repaint();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}
		}
	}

	protected void limitOffset() {
		if (hwa) return;
		if (zoom == 1) {
			x = 0;
			y = 0;
			if (longscroll) {
				int qh = (toDraw.getHeight() - getHeight()) / 2;
				if (y < -qh) y = -qh;
				if (y > qh) y = qh;
			}
			return;
		}
		
		int hw = (toDraw.getWidth() - getWidth()) / 2;
		int hh = (toDraw.getHeight() - getHeight()) / 2;
		if (hw < 0) hw = 0;
		if (hh < 0) hh = 0;
		if (x < -hw) x = -hw;
		if (x > hw) x = hw;
		if (y < -hh) y = -hh;
		if (y > hh) y = hh;
	}

	/**
	 * Clears any data, used for rendering.
	 */
	protected void reset() {
		if (hwa) return;
		toDraw = null;
		orig = null;
	}

	/**
	 * Implementation must prepare {@link #page} for drawing. No resizing is needed.
	 */
	protected void prepare() throws InterruptedException {
		if (hwa || MangaApp.onlineResize || !MangaApp.keepBitmap) return;
		byte[] data = getImage(page, false);
		int l = -1;
		try {
			l = data.length;
			orig = Image.createImage(data, 0, data.length);
			data = null;
			System.gc();
		} catch (RuntimeException e) {
			e.printStackTrace();
			orig = null;
			System.out.println("Failed to decode an image in preparing. Size=" + l + "bytes");
			if (MangaApp.chapterFileCache) {
				showBrokenNotify();
				try {
					orig = Image.createImage(data = getImage(page, true), 0, data.length);
					data = null;
					System.gc();
				} catch (RuntimeException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	/**
	 * Called when image must change it's zoom.
	 * 
	 * @param size New zoom to apply.
	 */
	protected void resize(int size) {
		if (hwa) return;
		resizing = true;
		try {
			toDraw = null;
			System.gc();
			repaint();
			Image origImg;
			if (!MangaApp.onlineResize && MangaApp.keepBitmap && orig != null && orig.getHeight() != 1 && orig.getWidth() != 1) {
				origImg = orig;
			} else {
				int l = -1;
				byte[] b;
				try {
					b = MangaApp.onlineResize ? getResizedImage(page, size) : getImage(page, false);
					l = b.length;
					origImg = Image.createImage(b, 0, b.length);
					b = null;
					System.gc();
				} catch (RuntimeException e) {
					e.printStackTrace();
					System.out.println("Failed to decode an image in resizing. Size=" + l + "bytes");
					origImg = null;
					if (MangaApp.chapterFileCache) {
						showBrokenNotify();
						try {
							b = getImage(page, true);
							origImg = Image.createImage(b, 0, b.length);
							b = null;
							System.gc();
						} catch (RuntimeException e1) {
							e1.printStackTrace();
						}
					}
				}
			}
			resizing = false;
			if (origImg == null) {
				error = true;
				toDraw = null;
				return;
			}
			if (!MangaApp.onlineResize) {
				int oh = origImg.getHeight(), ow = origImg.getWidth();
				int h = getHeight();
				int w = (int) (((float) h / oh) * ow);
				if (w > getWidth()) {
					w = getWidth();
					h = (int) (((float) w / ow) * oh);
				}
	
				if (!cover && MangaApp.enableLongScroll && (longscroll || oh / ow > 2)) {
					longscroll = true;
					w = getWidth();
					h = (int) (((float) w / ow) * oh);
					if (size == 1 && y == 0)
						y = h / 2;
				}
	
				h = h * size;
				w = w * size;
				toDraw = MangaApp.resize(origImg, w, h);
			} else {
				if (!cover && MangaApp.enableLongScroll) {
					int h, w;
					if (size == 1 && longscroll && y == 0) {
						h = origImg.getHeight();
						w = origImg.getWidth();
						if (h > getHeight())
							y = ((h / (float) w) * getWidth() / 2);
					} else if (!longscroll && (longscroll = (h = origImg.getHeight()) / (w = origImg.getWidth()) > 2)) {
						longscroll = true;
						origImg = null;
						y = (h / (float) w) * getWidth() / 2;
						System.gc();
						byte[] b = getResizedImage(page, size);
						origImg = Image.createImage(b, 0, b.length);
					}
				}
				toDraw = origImg;
			}
		} catch (Throwable e) {
			e.printStackTrace();
			resizing = false;
			error = true;
			toDraw = null;
			return;
		}
	}
	
	protected void paint(Graphics g) {
		if (hwa) return;
		try {
			Font f = MangaApp.smallfont;
			g.setFont(f);
			if (toDraw == null) {
				if (firstDraw) {
					firstDraw = false;
					g.setGrayScale(0);
					g.fillRect(0, 0, getWidth(), getHeight());
				}
				paintNullImg(g, f);
			} else {
				// bg fill
				g.setGrayScale(0);
				g.fillRect(0, 0, getWidth(), getHeight());
				limitOffset();
				if (zoom != 1 || longscroll) {
					g.drawImage(toDraw, (int) x + getWidth() / 2, (int) y + getHeight() / 2,
							Graphics.HCENTER | Graphics.VCENTER);
				} else {
					g.drawImage(toDraw, (getWidth() - toDraw.getWidth()) / 2, (getHeight() - toDraw.getHeight()) / 2,
							0);
				}
			}
			// touch captions
			if (hasPointerEvents() && touchCtrlShown) {
				drawTouchControls(g, f);
			}
			paintHUD(g, f, true, !cover && (!touchCtrlShown || !hasPointerEvents()));
		} catch (Exception e) {
			e.printStackTrace();
			try {
				MangaApp.display(new Alert("Repaint error", e.toString(), null, AlertType.ERROR));
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}

	String[] touchCaps = new String[] { "x1", "x2", "x3", "<-", "goto", "->", MangaApp.L[Back], MangaApp.L[Menu] };

	boolean touchCtrlShown = true;

	protected void reload(int i) {
		reset();
		System.gc();
		if (i == 1) {
			if (loader != null) {
				loader.interrupt();
			}
		}
		loaderAction = i;
		loader = new Thread(this);
		loader.setPriority(9);
		loader.start();
		menu = false;
	}

	/**
	 * Is there something to draw?
	 * 
	 * @return False if view is blocked.
	 */
	public boolean canDraw() {
		return toDraw != null;
	}

	protected final void keyPressed(int k) {
		k = qwertyToNum(k);
		if (k == -7 || k == KEY_NUM9) {
			if (menu) {
				menu = false;
				repaint();
				return;
			}
			try {
				if (loader != null && loader.isAlive()) {
					loader.interrupt();
				}
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
			try {
				if (preloaders != null) {
					for (int i = 0; i < preloaders.length; i++) {
						if (preloaders[i] != null) preloaders[i].interrupt();
					}
				}
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
			MangaApp.display(null, true);
			toDraw = orig = null;
			cache = null;
			return;
		}
		if (k == -6) {
			menu = !menu;
			repaint();
			return;
		}
		if (menu) {
			return;
		}
//		if (!canDraw()) {
//			repaint();
//			return;
//		}

		if (k == KEY_NUM7 || k == -10 || k == 8) {
			if (cover) return;
			TextBox tb = new TextBox("Go to page", "", 7, 2);
			tb.addCommand(MangaApp.goCmd);
			tb.addCommand(MangaApp.cancelCmd);
			tb.setCommandListener(this);
			MangaApp.display(tb);
		}

		if (k == KEY_NUM1) {
			changePage(-1);
		} else if (k == KEY_NUM3 || k == ' ') {
			changePage(1);
		}

		if (!resizing) {
			// zooming via *0#
			if (k == KEY_STAR) {
				zoom = 1;
				MangaApp.midlet.start(MangaApp.RUN_ZOOM_VIEW);
			}
			if (k == KEY_NUM0) {
				zoom = 2;
				MangaApp.midlet.start(MangaApp.RUN_ZOOM_VIEW);
			}
			if (k == KEY_POUND) {
				zoom = 3;
				MangaApp.midlet.start(MangaApp.RUN_ZOOM_VIEW);
			}
	
			
			if ((zoom != 1 || longscroll) && (k == -1 || k == KEY_NUM2 || k == 'w')) {
				// up
				y += getHeight() * panDeltaMul() / 4;
			} else if ((zoom != 1 || longscroll) && (k == -2 || k == KEY_NUM8 || k == 's')) {
				y -= getHeight() * panDeltaMul() / 4;
			} else if (zoom != 1) { // zoom is active
				if (k == -5) {
					zoom++;
					if (zoom > 3)
						zoom = 1;

					MangaApp.midlet.start(MangaApp.RUN_ZOOM_VIEW);
				} else if (k == -3 || k == KEY_NUM4 || k == 'a') {
					x += getWidth() * panDeltaMul() / 4;
				} else if (k == -4 || k == KEY_NUM6 || k == 'd') {
					x -= getWidth() * panDeltaMul() / 4;
				}
			} else if (k == -5) { // zoom inactive
				zoom = 2;
				x = 0;
				y = 0;
				MangaApp.midlet.start(MangaApp.RUN_ZOOM_VIEW);
			} else if (k == -3) {
				changePage(-1);
			} else if (k == -4) {
				changePage(1);
			}
		}

		repaint();
	}

	protected final void keyRepeated(int k) {
		k = qwertyToNum(k);
		if (!canDraw()) {
			repaint();
			return;
		}
		
		if ((zoom != 1 || longscroll) && (k == -1 || k == KEY_NUM2 || k == 'w')) {
			// up
			y += getHeight() * panDeltaMul() / 4;
		} else if ((zoom != 1 || longscroll) && (k == -2 || k == KEY_NUM8 || k == 's')) {
			y -= getHeight() * panDeltaMul() / 4;
		} else if (zoom != 1) { // zoom is active
			if (k == -3 || k == KEY_NUM4 || k == 'a') {
				x += getWidth() * panDeltaMul() / 4;
			} else if (k == -4 || k == KEY_NUM6 || k == 'd') {
				x -= getWidth() * panDeltaMul() / 4;
			}
		}

		repaint();
	}

	protected final void changePage(int delta) {
		if (cover) return;
		if (delta < 0) {
			if (page > 0) {
				page--;
				checkCacheAfterPageSwitch();
				reload(1);
			} else MangaApp.changeChapter(delta);
		} else if (delta > 0) {
			if (page < MangaApp.chapterPages - 1) {
				page++;
				checkCacheAfterPageSwitch();
				reload(1);
			} else MangaApp.changeChapter(delta);
		}
	}

	/**
	 * <ul>
	 * <li>0 - nothing
	 * <li>1 - zoom x1
	 * <li>2 - zoom x2
	 * <li>3 - zoom x3
	 * <li>4 - prev
	 * <li>5 - goto
	 * <li>6 - next
	 * <li>7 - return
	 * <li>8 - zoom slider
	 * </ul>
	 */
	int touchHoldPos = 0;
	int lx, ly;
	int sx, sy;

	protected final void pointerPressed(int tx, int ty) {
		int w = getWidth();
		if (!canDraw() && ty > getHeight() - 50 && tx > w * 2 / 3) {
			keyPressed(-7);
			return;
		}
		touchHoldPos = 0;
		lx = (sx = tx);
		ly = (sy = ty);
		if (!touchCtrlShown)
			return;
		if (ty < 50 && hwa) {
			if (tx > w * 3 / 4) {
				touchHoldPos = 9;
			} else {
				setSmoothZoom(tx, w * 3 / 4);
				touchHoldPos = 8;
			}
		} else if (ty < 50) {
			int b;
			if (tx < w / 4) {
				b = 1;
			} else if (tx < w * 2 / 4) {
				b = 2;
			} else if (tx < w * 3 / 4) {
				b = 3;
			} else {
				b = 9;
			}
			touchHoldPos = b;
		} else if (ty > getHeight() - 50) {
			int b;
			if (tx < w / 4) {
				b = 4;
			} else if (tx < w * 2 / 4) {
				b = 5;
			} else if (tx < w * 3 / 4) {
				b = 6;
			} else {
				b = 7;
			}
			touchHoldPos = b;
		}
		repaint();
	}

	protected final void setSmoothZoom(int dx, int w) {
		dx -= 25;
		w -= 50;
		zoom = 1 + 4f * ((float) dx / w);
		if (zoom < 1.01f)
			zoom = 1;
		if (zoom > 4.99f)
			zoom = 5;
	}

	/**
	 * @return -1 if drag must be inverted, 1 overwise.
	 */
	protected float panDeltaMul() {
		return MangaApp.invertPan ? -1 : 1;
	}

	protected final void pointerDragged(int tx, int ty) {
		if (touchHoldPos == 8) {
			setSmoothZoom(tx, getWidth() * 3 / 4);
			repaint();
			return;
		}
		if (touchHoldPos != 0)
			return;
		x += (tx - lx) * panDeltaMul() / (hwa ? zoom : 1f);
		y += (ty - ly) * panDeltaMul() / (hwa ? zoom : 1f);
		lx = tx;
		ly = ty;
		repaint();
	}

	protected final void pointerReleased(int tx, int ty) {
		if (!touchCtrlShown || touchHoldPos == 0) {
			if (Math.abs(sx - tx) < 10 && Math.abs(sy - ty) < 10) {
				touchCtrlShown = !touchCtrlShown;
			}
		}
		if (touchHoldPos == 8) {
			touchHoldPos = 0;
			repaint();
			return;
		}
		int zone = 0;
		if (ty < 50) {
			int b;
			if (tx < getWidth() / 4) {
				b = 1;
			} else if (tx < getWidth() * 2 / 4) {
				b = 2;
			} else if (tx < getWidth() * 3 / 4) {
				b = 3;
			} else {
				b = 9;
			}
			zone = b;
		} else if (ty > getHeight() - 50) {
			int b;
			if (tx < getWidth() / 4) {
				b = 4;
			} else if (tx < getWidth() * 2 / 4) {
				b = 5;
			} else if (tx < getWidth() * 3 / 4) {
				b = 6;
			} else {
				b = 7;
			}
			zone = b;
		}
		if (zone == touchHoldPos) {
			if (zone >= 1 && zone <= 3 && !resizing) {
				zoom = zone;
				MangaApp.midlet.start(MangaApp.RUN_ZOOM_VIEW);
			} else if (zone == 4) {
				changePage(-1);
			} else if (zone == 5) {
				keyPressed(KEY_NUM7);
			} else if (zone == 6) {
				changePage(1);
			} else if (zone == 7) {
				keyPressed(-7);
			} else if (zone == 9) {
				keyPressed(-6);
			}
		}
		touchHoldPos = 0;
		repaint();
	}

	/**
	 * Listener for textbox.
	 * 
	 * @param c
	 * @param d
	 */
	public final void commandAction(Command c, Displayable d) {
		TextBox tb = (TextBox) d;
		MangaApp.display(this);
		if (c == MangaApp.goCmd) {
			try {
				int n = Integer.parseInt(tb.getString());
				if (n < 1) {
					n = 1;
				} else if (n > MangaApp.chapterPages) {
					n = MangaApp.chapterPages;
				}
				page = n - 1;
				checkCacheAfterPageSwitch();
				reload(2);
			} catch (Exception e) {
				MangaApp.display(new Alert("", e.toString(), null, AlertType.ERROR), this);
			}
		}
		repaint();
	}
	
	protected final void paintHUD(Graphics g, Font f, boolean drawZoom, boolean drawPages) {
		int w = getWidth(), h = getHeight();
		int fh = f.getHeight();
		
		if (menu) {
			g.setColor(0);
			return;
		}
		
		String pageNum = (page + 1) + "/" + MangaApp.chapterPages;
		String zoomN = hwa ? String.valueOf(zoom) : Integer.toString((int) zoom);
		if (zoomN.length() > 3)
			zoomN = zoomN.substring(0, 3);
		zoomN = "x" + zoomN;
//		String prefetch = null;
		// if (preloadProgress == 101) {
//		if (MangaApp.cachingPolicy == 2) {
//			prefetch = (preloadProgress > 0 && preloadProgress < 100)
//					? ((MangaApp.chapterFileCache ? "downloading " : "caching ") + preloadProgress + "%")
//					: null;
//		}

		// BGs
		g.setColor(0);
		if (drawPages) {
			g.fillRect(0, 0, f.stringWidth(pageNum), fh);
		}
		if (drawZoom) {
			g.fillRect(w - f.stringWidth(zoomN), 0, f.stringWidth(zoomN), fh);
		}
//		if (prefetch != null) {
//			g.fillRect(0, h - fh, f.stringWidth(prefetch), fh);
//		}

		// texts
		g.setColor(-1);
		if (drawPages) {
			g.drawString(pageNum, 0, 0, 0);
		}
		if (drawZoom) {
			g.drawString(zoomN, w - f.stringWidth(zoomN), 0, 0);
		}
//		if (prefetch != null) {
//			g.drawString(prefetch, 0, h - fh, 0);
//		}
		
		if (!cover && chapterShown != 0 && (page == 0 || page == MangaApp.chapterPages - 1)) {
			if (System.currentTimeMillis() - chapterShown > 3000L) {
				chapterShown = 0;
				return;
			}
			StringBuffer sb = new StringBuffer();
			if (MangaApp.chapterVolume != null) {
				sb.append("Vol. ").append(MangaApp.chapterVolume).append(' ');
			}
			if (MangaApp.chapterNum != null) {
				sb.append("Ch. ").append(MangaApp.chapterNum);
			}
			g.setColor(0xFF6740);
			g.drawString(sb.toString(), w >> 1, drawPages ? 4 : 50, Graphics.HCENTER | Graphics.TOP);
		}
		// progress
		if (!cover && MangaApp.chapterPages > 0) {
			g.setColor(0x2C2C2C);
			g.fillRect(0, h-2, w, 2);
			if (cacheProgress != null) {
				int tw = w / MangaApp.chapterPages;
				synchronized (cacheProgress) {
					for (int i = 0; i < cacheProgress.length; i++) {
						if (cacheProgress[i] == 0) continue;
						g.setColor(cacheProgress[i] == 1 ? 0x4F4F4F : 0xFF2C2C);
						g.fillRect(w * i / MangaApp.chapterPages, h-2, tw, 2);
					}
				}
			}
			g.setColor(0xFF6740);
			g.fillRect(0, h-2, w * (page + 1) / MangaApp.chapterPages, 2);
		}
	}

	protected final void drawTouchControls(Graphics g, Font f) {
		int w = getWidth(), h = getHeight();
		int fh = f.getHeight();

		// captions
		for (int i = 3; i < 7; i++) {
			if (cover && i != 6) continue;
			fillGrad(g, w * (i - 3) / 4, h - 50, w / 4, 51, 0,
					touchHoldPos == (i + 1) ? 0xFF6740 : 0x222222);
			g.setGrayScale(255);
			g.drawString(i == 4 ? ((page + 1) + "/" + MangaApp.chapterPages) : touchCaps[i], w * (1 + (i - 3) * 2) / 8,
					h - 25 - fh / 2, Graphics.TOP | Graphics.HCENTER);
		}
		g.setColor(-1);
		if (!cover) {
			// hor lines
			g.drawLine(0, h - 50, w, h - 50);
			// vert lines between btns
			g.drawLine(w / 4, h - 50, w / 4, h);
			g.drawLine(w >> 1, h - 50, w * 2 / 4, h);
		} else {
			g.drawLine(w * 3 / 4, h - 50, w, h - 50);
		}
		g.drawLine(w * 3 / 4, h - 50, w * 3 / 4, h);
		
		// menu button
		fillGrad(g, w * 3 / 4, 0, w / 4 + 1, 50, touchHoldPos == 9 ? 0xFF6740 : 0x222222, 0);
		g.setColor(-1);
		g.drawString(touchCaps[7], w * 7 / 8, 25 - fh / 2, Graphics.TOP | Graphics.HCENTER);
		g.drawLine(w * 3 / 4, 0, w * 3 / 4, 50);
		g.drawLine(w * 3 / 4, 50, w, 50);

		if (hwa) {
			drawZoomSlider(g, f);
			return;
		}
		for (int i = 0; i < 3; i++) {
			fillGrad(g, w * i / 4, 0, w / 4 + 1, 50, touchHoldPos == (i + 1) ? 0xFF6740 : 0x222222, 0);
			g.setColor(-1);
			g.drawString(touchCaps[i], w * (1 + i * 2) / 8, 25 - fh / 2, Graphics.TOP | Graphics.HCENTER);
		}
		// bottom hor line
		g.setColor(-1);
		g.drawLine(0, 50, w, 50);
		// vert lines between btns
		g.drawLine(w / 4, 0, w / 4, 50);
		g.drawLine(w * 2 / 4, 0, w * 2 / 4, 50);
		g.drawLine(w * 3 / 4, 0, w * 3 / 4, 50);

	}

	private final void drawZoomSlider(Graphics g, Font f) {
		int w = getWidth() * 3 / 4;
		int px = (int) (25 + ((w - 50) * (zoom - 1) / 4));

		// slider's body
		if (slider == null) {
			for (int i = 0; i < 10; i++) {
				g.setColor(MangaApp.blend(touchHoldPos == 8 ? 0xFF6740 : 0x444444, 0xffffff, i * 255 / 9));
				g.drawRoundRect(25 - i, 25 - i, w - 50 + (i * 2), i * 2, i, i);
			}
		} else {
			int spy = touchHoldPos == 8 ? 20 : 0;
			g.drawRegion(slider, 0, spy, 35, 20, 0, 0, 15, 0);
			g.drawRegion(slider, 35, spy, 35, 20, 0, w - 35, 15, 0);
			g.setClip(35, 0, w - 70, 50);
			for (int i = 35; i < w - 34; i += 20) {
				g.drawRegion(slider, 25, spy, 20, 20, 0, i, 15, 0);
			}
			g.setClip(0, 0, w, getHeight());
		}

		// slider's pin
		for (int i = 0; i < 15; i++) {
			g.setColor(MangaApp.blend(touchHoldPos == 8 ? 0xFF6740 : 0x444444, 0, i * 255 / 14));
			g.fillArc(px - 15 + i, 10 + i, 30 - i * 2, 30 - i * 2, 0, 360);
		}
		g.setColor(touchHoldPos == 8 ? 0xFF6740 : -1);

		g.drawArc(px - 16, 9, 30, 30, 0, 360);

		String ft = String.valueOf(zoom);
		if (ft.length() > 3) {
			ft = ft.substring(0, 3);
		}
		g.setColor(-1);
		g.drawString(ft, px, 25 - f.getHeight() / 2, Graphics.TOP | Graphics.HCENTER);
	}

	protected final void paintNullImg(Graphics g, Font f) {
		int w = getWidth(), h = getHeight();
		int fh = f.getHeight();
		
		String info;
		if (error) {
			g.setGrayScale(0);
			g.fillRect(0, 0, w, h);
			info = "Failed to load image.";
		} else {
			info = "Preparing";
		}
		g.setGrayScale(0);
		int tw = f.stringWidth(info);
		g.fillRect(w / 2 - tw / 2, h / 2, tw,  fh);
		g.setGrayScale(255);
		g.drawString(info, w / 2, h / 2, Graphics.HCENTER | Graphics.TOP);
		if (hasPointerEvents()) {
			// grads
			fillGrad(g, w * 3 / 4, h - 50, w / 4, 51, 0, 0x222222);
			// lines
			g.setGrayScale(255);
			g.drawLine(w * 3 / 4, h - 50, w, h - 50);
			g.drawLine(w * 3 / 4, h - 50, w * 3 / 4, h);
			// captions
			g.setGrayScale(255);
			g.drawString(touchCaps[6], w * 7 / 8, h - 25 - fh / 2, Graphics.TOP | Graphics.HCENTER);
		}
	}

	protected final void showBrokenNotify() {
		Alert a = new Alert("Image file is corrupted",
				"It is recommended to run cache repairer from manga's page. The image will be downloaded again for now.",
				null, AlertType.ERROR);
		a.setTimeout(Alert.FOREVER);
		try {
			MangaApp.display(a);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Fills an opaque gradient on the canvas.
	 * 
	 * @param g  Graphics object to draw in.
	 * @param x  X.
	 * @param y  Y.
	 * @param w  Width.
	 * @param h  Height.
	 * @param c1 Top color.
	 * @param c2 Bottom color.
	 */
	public static void fillGrad(Graphics g, int x, int y, int w, int h, int c1, int c2) {
		for (int i = 0; i < h; i++) {
			g.setColor(MangaApp.blend(c2, c1, i * 255 / h));
			g.drawLine(x, y + i, x + w, y + i);
		}
	}

	/**
	 * Converts qwerty key code to corresponding 12k key code.
	 * 
	 * @param k Original key code.
	 * @return Converted key code.
	 */
	public static int qwertyToNum(int k) {
		char c = (char) k;
		switch (c) {
		case 'r':
		case 'R':
		case 'к':
			return Canvas.KEY_NUM1;

		case 't':
		case 'T':
		case 'е':
			return Canvas.KEY_NUM2;

		case 'y':
		case 'Y':
		case 'н':
			return Canvas.KEY_NUM3;

		case 'f':
		case 'F':
		case 'а':
			return Canvas.KEY_NUM4;

		case 'g':
		case 'G':
		case 'п':
			return Canvas.KEY_NUM5;

		case 'h':
		case 'H':
		case 'р':
			return Canvas.KEY_NUM6;

		case 'v':
		case 'V':
		case 'м':
			return Canvas.KEY_NUM7;

		case 'b':
		case 'B':
		case 'и':
			return Canvas.KEY_NUM8;

		case 'n':
		case 'N':
		case 'т':
			return Canvas.KEY_NUM9;

		case 'm':
		case 'M':
		case 'ь':
			return Canvas.KEY_NUM0;

		default:
			return k;
		}
	}
}
