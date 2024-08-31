/**
 * Copyright (c) 2021 Fyodor Ryzhov
 * Copyright (c) 2024 Arman Jussupgaliyev
 */
import java.util.Vector;

import javax.microedition.lcdui.*;
import javax.microedition.m3g.*;

/**
 * {@link View} implementation, that uses M3G for real-time scaling.
 * 
 * @author Feodor0090
 */
//from njtai
public class ViewHWA extends ViewCommon {

	/**
	 * Creates the view.
	 * 
	 * @param emo  Object with data.
	 * @param prev Previous screen.
	 * @param page Number of page to start.
	 */
	public ViewHWA(int page) {
		super(page, true);

		// material
		mat = new Material();
		mat.setColor(Material.DIFFUSE, 0xFFFFFFFF); // white
		mat.setColor(Material.SPECULAR, 0xFFFFFFFF); // white
		mat.setShininess(128f);
		mat.setVertexColorTrackingEnable(true);

		// compositing
		cmp = new CompositingMode();
		cmp.setAlphaThreshold(0.0f);
		cmp.setBlending(CompositingMode.ALPHA);

		// pol mode
		pm = new PolygonMode();
		pm.setWinding(PolygonMode.WINDING_CW);
		pm.setCulling(PolygonMode.CULL_NONE);
		pm.setShading(PolygonMode.SHADE_SMOOTH);

		// strip
		ind = new TriangleStripArray(0, new int[] { 4 });

		// quad
		// RT, LT, RB, LB
		short[] vert = { tileSize, 0, 0, 0, 0, 0, tileSize, tileSize, 0, 0, tileSize, 0 };
		short[] uv = { 1, 0, 0, 0, 1, 1, 0, 1 };
		VertexArray vertArray = new VertexArray(vert.length / 3, 3, 2);
		vertArray.set(0, vert.length / 3, vert);
		VertexArray texArray = new VertexArray(uv.length / 2, 2, 2);
		texArray.set(0, uv.length / 2, uv);
		vb = new VertexBuffer();
		vb.setPositions(vertArray, 1.0f, null);
		vb.setTexCoords(0, texArray, 1.0f, null);
		vb.setDefaultColor(-1);
		
		// light
		li = new Light();
		li.setColor(0xffffff); // white light
		li.setIntensity(1f);
		li.setMode(Light.AMBIENT);
		
		// bg
		bg = new Background();
		bg.setColorClearEnable(true);
		bg.setDepthClearEnable(false);
	}

	protected VertexBuffer vb;
	protected Material mat;
	protected CompositingMode cmp;
	protected PolygonMode pm;
	protected TriangleStripArray ind;
	protected Light li;
	protected Background bg;

	Object[][] p = null;
	int iw, ih;

	public static final short tileSize = 512;

	protected void reset() {
		p = null;
	}

	protected void prepare() throws InterruptedException {
		byte[] d = getImage(page, false);
		reset();
		Image i = Image.createImage(d, 0, d.length);
		d = null;
		ih = i.getHeight();
		iw = i.getWidth();
		Vector v = new Vector();
		for (int ix = 0; ix < i.getWidth() + tileSize - 1; ix += tileSize) {
			for (int iy = 0; iy < i.getHeight() + tileSize - 1; iy += tileSize) {
				v.addElement(getTile(i, ix, iy));
			}
		}
		Object[][] tmp = new Object[v.size()][];
		v.copyInto(tmp);
		v = null;
		p = tmp;
		x = iw / 2;
		y = ih / 2;
	}

	protected void resize(int size) {
		// do nothing for now
	}

	public boolean canDraw() {
		return p != null;
	}

	protected void paint(Graphics g) {
		try {
			final Font f = MangaApp.smallfont;
			g.setFont(f);

			// bg fill

			if (p == null) {
				g.setGrayScale(0);
				g.fillRect(0, 0, getWidth(), getHeight());
				paintNullImg(g, f);
				g.setColor(0, 0, 255);
				g.fillRect(0, 0, getWidth(), 4);
				g.drawString(iw + "x" + ih, getWidth() / 2, 4, Graphics.TOP | Graphics.HCENTER);
			} else {
				limitOffset();
				final Graphics3D g3 = Graphics3D.getInstance();
				g3.bindTarget(g, false, Graphics3D.ANTIALIAS);
				try {
					g3.clear(bg);

					setupM3G(g3);
					
					if (p != null) {
						for (int i = 0; i < p.length; i++) {
							if (p[i] == null) break;
							g3.render((Node) p[i][0], (Transform) p[i][1]);
						}
					}
				} catch (Throwable t) {
					t.printStackTrace();
				}
				g3.releaseTarget();
				// touch captions
				if (hasPointerEvents() && touchCtrlShown) {
					drawTouchControls(g, f);
				}
			}
			paintHUD(g, f, !hasPointerEvents(), !cover && (!touchCtrlShown || !hasPointerEvents()));
		} catch (Exception e) {
			e.printStackTrace();

			try {
				MangaApp.display(new Alert("Repaint error", e.toString(), null, AlertType.ERROR), this);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}

	protected void limitOffset() {
		if (x < 0)
			x = 0;
		if (y < 0)
			y = 0;
		if (x > iw)
			x = iw;
		if (y > ih)
			y = ih;
	}

	protected void setupM3G(Graphics3D g3d) {
		Camera cam = new Camera();
		cam.setParallel(Math.max(iw, ih) / zoom, getWidth() / (float) getHeight(), 0.1f, 900f);
		Transform t = new Transform();
		t.postTranslate(x, y, 100);
		t.postRotate(180, 0, 0, -1);
		t.postScale(-1, 1, 1);

		g3d.setCamera(cam, t);
		g3d.resetLights();
		g3d.addLight(li, t);
	}

	

	private Object[] getTile(Image i, int tx, int ty) {
		// cropping
		Image part = Image.createImage(tileSize, tileSize);
		Graphics pg = part.getGraphics();
		pg.setColor(0);
		pg.fillRect(0, 0, tileSize, tileSize);
		pg.drawRegion(i, tx, ty, Math.min(tileSize, i.getWidth() - tx), Math.min(tileSize, i.getHeight() - ty), 0, 0,
				0, 0);
		
		System.gc();

		// appearance
		Image2D image2D = new Image2D(Image2D.RGB, part);
		Texture2D tex = new Texture2D(image2D);
		tex.setFiltering(Texture2D.FILTER_LINEAR, Texture2D.FILTER_LINEAR);
		tex.setWrapping(Texture2D.WRAP_CLAMP, Texture2D.WRAP_CLAMP);
		tex.setBlending(Texture2D.FUNC_MODULATE);
		Appearance ap = new Appearance();
		ap.setTexture(0, tex);
		ap.setMaterial(mat);
		ap.setCompositingMode(cmp);
		ap.setPolygonMode(pm);

		// transform
		Transform t = new Transform();
		t.postTranslate(tx, ty, 0);
		
		return new Object[] {new Mesh(vb, ind, ap), t};
	}

	protected float panDeltaMul() {
		return -super.panDeltaMul() * (ih / (float) getHeight());
	}

}
