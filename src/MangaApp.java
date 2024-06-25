import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.TimeZone;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.lcdui.*;
import javax.microedition.midlet.MIDlet;

import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;
import cc.nnproject.json.JSONStream;

public class MangaApp extends MIDlet implements Runnable, CommandListener, ItemCommandListener {

	private static final int RUN_MANGAS = 1;
	private static final int RUN_MANGA = 2;
	private static final int RUN_COVERS = 3;
	private static final int RUN_CHAPTERS = 4;
	
	private static final String APIURL = "https://api.mangadex.dev/"; // TODO dev домен
	private static final String COVERSURL = "https://uploads.mangadex.org/covers/";
	
	private static final Font largefont = Font.getFont(0, 0, Font.SIZE_LARGE);
	private static final Font medboldfont = Font.getFont(0, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
	private static final Font medfont = Font.getFont(0, 0, Font.SIZE_MEDIUM);
	private static final Font smallboldfont = Font.getFont(0, Font.STYLE_BOLD, Font.SIZE_SMALL);
	private static final Font smallfont = Font.getFont(0, 0, Font.SIZE_SMALL);

	private static boolean started;
	private static Display display;
	
	// команды
	private static Command exitCmd;
	private static Command backCmd;
	private static Command searchCmd;
	private static Command updatesCmd;
	private static Command mangaItemCmd;
	private static Command chaptersCmd;
	private static Command tagItemCmd;
	private static Command addFavoriteCmd;
	private static Command coverItemCmd;
	private static Command chapterCmd;
	
	// ui
	private static Form mainForm;
	private static Form listForm;
	private static Form mangaForm;
	private static Form chaptersForm;
	
	private static TextField searchField;
	
	// трединг
	private static int run;
	private static boolean running;
	
	private static String query;
	private static String currentMangaId;
	private static ImageItem mangaItem;
	
	private static int chaptersLimit = 20;
	private static int chaptersOffset = 0;
	private static int chaptersTotal;
	private static Hashtable chapterItems = new Hashtable();
	
	private static Object coverLoadLock = new Object();
	private static Vector coversToLoad = new Vector();
	
	private static String version;
	
	// настройки
	private static String proxyUrl = "http://nnp.nnchan.ru/hproxy.php?";
	private static String timezone;

	public MangaApp() {
	}

	protected void destroyApp(boolean unconditional) {

	}

	protected void pauseApp() {

	}

	protected void startApp() {
		if (started) return;
		started = false;
		
		version = getAppProperty("MIDlet-Version");
		display = Display.getDisplay(this);
		
		// TODO локализации и загрузка настроек здесь
		
		try {
			// определение таймзоны системы
			int i = TimeZone.getDefault().getRawOffset() / 60000;
			timezone = (i < 0 ? '-' : '+') + n(Math.abs(i / 60)) + ':' + n(Math.abs(i % 60));
		} catch (Exception e) {}
		
		exitCmd = new Command("Exit", Command.EXIT, 2);
		backCmd = new Command("Back", Command.EXIT, 2);
		searchCmd = new Command("Search", Command.ITEM, 1);
		updatesCmd = new Command("Updates", Command.ITEM, 1);
		mangaItemCmd = new Command("Open", Command.ITEM, 1);
		chaptersCmd = new Command("Chapters", Command.ITEM, 1);
		tagItemCmd = new Command("Tag", Command.ITEM, 1);
		addFavoriteCmd = new Command("Add to favorite", Command.ITEM, 1);
		coverItemCmd = new Command("Show cover", Command.ITEM, 1);
		chapterCmd = new Command("Chapter", Command.ITEM, 1);
		
		// главная форма
		
		Form f = new Form("MangaDex");
		f.addCommand(exitCmd);
		f.setCommandListener(this);
		
		StringItem s;
		
		searchField = new TextField("", "", 200, TextField.NON_PREDICTIVE);
		f.append(searchField);
		
		s = new StringItem(null, "Search", StringItem.BUTTON);
		s.setFont(Font.getDefaultFont());
		s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
		s.addCommand(searchCmd);
		s.setDefaultCommand(searchCmd);
		s.setItemCommandListener(this);
		f.append(s);
		
		s = new StringItem(null, "Updates", StringItem.BUTTON);
		s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
		s.addCommand(updatesCmd);
		s.setDefaultCommand(updatesCmd);
		s.setItemCommandListener(this);
		f.append(s);
		
		
		display.setCurrent(mainForm = f);
		
		// запустить тред обложек
		start(RUN_COVERS);
	}

	public void commandAction(Command c, Displayable d) {
		if (d == chaptersForm && c == backCmd) {
			// возвращение из глв
			display(chaptersForm != null ? chaptersForm : listForm != null ? listForm : mainForm);
			chaptersForm = null;
			chapterItems.clear();
			return;
		}
		if (d == mangaForm && c == backCmd) {
			// возвращение из манги
			display(listForm != null ? listForm : mainForm);
			mangaForm = null;
			return;
		}
		if (d == listForm && c == backCmd) {
			// возвращение из списка
			display(mainForm);
			coversToLoad.removeAllElements();
			listForm = null;
			return;
		}
		if (c == backCmd) {
			display(mainForm);
			return;
		}
		if (c == exitCmd) {
			notifyDestroyed();
			return;
		}
	}

	public void commandAction(Command c, Item item) {
		if (c == mangaItemCmd) {
			if (running) return; // игнорировать запросы, пока что-то еще грузится
			coversToLoad.removeAllElements();
			
			String id = (mangaItem = (ImageItem) item).getAltText();
			
			Form f = new Form("Manga " + id);
			f.addCommand(backCmd);
			f.setCommandListener(this);
			
			f.setTicker(new Ticker("Loading..."));
			
			currentMangaId = id;
			display(mangaForm = f);
			start(RUN_MANGA);
			return;
		}
		
		if (c == searchCmd || c == updatesCmd) {
			if (running) return; // игнорировать запросы, пока что-то еще грузится
			coversToLoad.removeAllElements();
			
			// поиск и список манг
			Form f = new Form("MangaDex");
			f.addCommand(backCmd);
			f.setCommandListener(this);
			
			f.setTicker(new Ticker("Loading..."));
			
			query = c == searchCmd ? searchField.getString().trim() : null;
			display(listForm = f);
			start(RUN_MANGAS);
			return;
		}
		if (c == chaptersCmd) {
			// TODO главы
			chaptersOffset = 0;
			
			Form f = new Form(mangaForm.getTitle());
			f.addCommand(backCmd);
			f.setCommandListener(this);
			
			f.setTicker(new Ticker("Loading..."));
			
			display(chaptersForm = f);
			start(RUN_CHAPTERS);
			return;
		}
		if (c == addFavoriteCmd) {
			// TODO список локальных закладок
			
			return;
		}
		if (c == coverItemCmd) {
			// TODO показать обложку
			
			try {
				String url = proxyUrl(COVERSURL + currentMangaId + '/' + getMangaCover(currentMangaId));
				
				if (platformRequest(url))
					notifyDestroyed();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;
		}
		if (c == chapterCmd) {
			// TODO просмотр главы
			if (running) return;
			
			String chapterId = (String) chapterItems.get(item);
			if (chapterId == null) return;
			return;
		}
	}
	
	// трединг
	public void run() {
		int run;
		synchronized(this) {
			run = MangaApp.run;
			notify();
		}
		running = run != RUN_COVERS;
		switch (run) {
		case RUN_MANGAS: { // поиск и список манг
			Form f = listForm;
			try {
				JSONObject j = api("manga" + (query != null ? "?title=" + url(query) : ""));
				JSONArray data = j.getArray("data");
				int l = data.size();
				
				ImageItem item;
				for (int i = 0; i < l; i++) {
					JSONObject m = data.getObject(i);
					String id = m.getString("id");
					
					JSONObject attributes = m.getObject("attributes");

					String title = attributes.has("title") ? getTitle(attributes.getObject("title")) : "Unknown";
					item = new ImageItem(title, null, Item.LAYOUT_EXPAND, id);
					item.addCommand(mangaItemCmd);
					item.setDefaultCommand(mangaItemCmd);
					item.setItemCommandListener(this);
					scheduleCover(item, id);
					f.append(item);
				}
				
			} catch (Exception e) {
				e.printStackTrace();
				display(errorAlert(e.toString()), f);
			}
			f.setTicker(null);
			break;
		}
		case RUN_MANGA: { // открыта манга
			String id = currentMangaId;
			Image thumb = null;
			
			if(mangaItem != null) {
				thumb = mangaItem.getImage();
				mangaItem = null;
			}
			
			Form f = mangaForm;
			
			ImageItem coverItem = new ImageItem("", thumb, Item.LAYOUT_LEFT, id);
			coverItem.addCommand(coverItemCmd);
			coverItem.addCommand(coverItemCmd);
			coverItem.setItemCommandListener(this);
			coverItem.setLayout(Item.LAYOUT_NEWLINE_AFTER);
			f.append(coverItem);
			
			try {
				JSONObject j = api("manga/" + id).getObject("data");	
				JSONObject attributes = j.getObject("attributes");
				
				StringItem s;
				String t;
				
				if (attributes.has("title")) {
					f.setTitle(t = getTitle(attributes.getObject("title")));
					s = new StringItem(null, t);
					s.setFont(largefont);
					s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(s);
				}
				
				if (attributes.has("altTitle") && (t = getTitle(attributes.getArray("altTitle"))) != null) {
					s = new StringItem(null, t);
					s.setFont(smallfont);
					s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(s);
				}

				// теги
				s = new StringItem(null, "Tags");
				s.setFont(medboldfont);
				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				f.append(s);
				
				JSONArray tags = attributes.getArray("tags");
				int l = tags.size();
				for (int i = 0; i < l; i++) {
					// отображение тегов как кнопки потому что почему нет
					s = new StringItem(null, getTitle(tags.getObject(i).getObject("attributes").getObject("name")), StringItem.BUTTON);
					s.setFont(smallfont);
					s.setLayout(Item.LAYOUT_LEFT);
					s.addCommand(tagItemCmd);
					s.setDefaultCommand(tagItemCmd);
					s.setItemCommandListener(this);
					f.append(s);
				}
				
				f.append("\n");

				// статус
				s = new StringItem(null, "Publication");
				s.setFont(medboldfont);
				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				f.append(s);
				
				s = new StringItem(null, attributes.getString("year") + ", " + attributes.getString("status").toUpperCase());
				s.setFont(smallfont);
				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				f.append(s);
				
				// описание
				if (attributes.has("description")) {
					s = new StringItem(null, "Description");
					s.setFont(medboldfont);
					s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(s);
					
					s = new StringItem(null, getTitle(attributes.getObject("description")));
					s.setFont(smallfont);
					s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(s);
				}
				
				// кнопки
				s = new StringItem(null, "Chapters", StringItem.BUTTON);
				s.setFont(Font.getDefaultFont());
				s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				s.addCommand(chaptersCmd);
				s.setDefaultCommand(chaptersCmd);
				s.setItemCommandListener(this);
				f.append(s);
				
				s = new StringItem(null, "Add to favorites", StringItem.BUTTON);
				s.setFont(Font.getDefaultFont());
				s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				s.addCommand(addFavoriteCmd);
				s.setDefaultCommand(addFavoriteCmd);
				s.setItemCommandListener(this);
				f.append(s);

//				f.append(j.format());
			} catch (Exception e) {
				e.printStackTrace();
				display(errorAlert(e.toString()), f);
			}
			
			if (thumb == null) {
				scheduleCover(coverItem, id);
			}
			f.setTicker(null);
			break;
		}
		case RUN_COVERS: { // скачиватель обложек, постоянно крутится на фоне
			try {
				while (true) {
					synchronized (coverLoadLock) {
						coverLoadLock.wait();
					}
					// подождать перед тем как начать грузить обложки, может сверху что-то не допарсилось и они друг другу будут мешать
					Thread.sleep(500);
					while (coversToLoad.size() > 0) {
						int i = 0;
						Object[] o = (Object[]) coversToLoad.elementAt(i);
						coversToLoad.removeElementAt(i);
						String mangaId = (String) o[0];
						ImageItem item = (ImageItem) o[1];
						
						try { 
							String filename = getMangaCover(mangaId);
							
							// картинка с меньшим размером https://api.mangadex.org/docs/03-manga/covers/
							Image img = getImage(proxyUrl(COVERSURL + mangaId + '/' + filename + ".256.jpg"));

							// ресайз обложки
							int h = getHeight() / 3; // TODO константа?
							int w = (int) (((float) h / img.getHeight()) * img.getWidth());
							img = resize(img, w, h);
							
							item.setImage(img);
						} catch (Exception e) {
							e.printStackTrace();
						} 
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;
		}
		case RUN_CHAPTERS: { // главы манги
			String id = currentMangaId;
			Form f = chaptersForm;
			f.deleteAll();
			chapterItems.clear();
			
			try {
				StringBuffer sb = new StringBuffer("chapter?manga=").append(id)
						.append("&order[chapter]=desc")
						.append("&limit=").append(chaptersLimit)
						;
				if (chaptersOffset > 0)
					sb.append("&offset=").append(chaptersOffset);
				
				JSONObject j = api(sb.toString());
				JSONArray data = j.getArray("data");
				int l = data.size();
				chaptersTotal = j.getInt("total");
				
				// TODO pagination
				
				StringItem s;
				
				sb.setLength(0);
				s = new StringItem(null, sb.append("Offset: ").append(Math.min(chaptersOffset + chaptersLimit, chaptersTotal)).append('/').append(chaptersTotal).append("\n\n").toString());
				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				f.append(s);
				
				String lastVolume = null;
				String lastChapter = null;
				for (int i = 0; i < l; i++) {
					JSONObject c = data.getObject(i);
					JSONObject a = c.getObject("attributes");
					
					String volume = a.getString("volume");
					String chapter = a.getString("chapter");
					String title = a.getString("title");
					String time = a.getString("publishAt");
					String lang = a.getString("translatedLanguage");
					
					boolean b = false;
					
					// выглядит страшно
					if (i == 0 && (lastVolume == null && volume == null)) {
						s = new StringItem(null, "No Volume");
						s.setFont(medfont);
						s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
						f.append(s);
						b = true;
					} else if ((volume == null && lastVolume != null) ||
							(volume != null && !volume.equals(lastVolume))) {
						s = new StringItem(null, volume == null ? "\nNo Volume" : "\nVolume ".concat(volume));
						s.setFont(medfont);
						s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
						f.append(s);
						b = true;
					}
					
					if (i == 0 || !chapter.equals(lastChapter) || b) {
						s = new StringItem(null, (b ? "" : "\n").concat("Chapter ").concat(chapter));
						s.setFont(smallboldfont);
						s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
						f.append(s);
					}

					// TODO автор, картинка языка
					sb.setLength(0);
					sb.append("* ").append(lang).append(" / ")
					.append(title != null ? title : "Ch. ".concat(chapter)).append(" / ")
					.append(localizeTime(time));
					
					s = new StringItem(null, sb.toString());
					s.setFont(smallfont);
					s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					s.addCommand(chapterCmd);
					s.setDefaultCommand(chapterCmd);
					s.setItemCommandListener(this);
					f.append(s);
					chapterItems.put(s, c.get("id"));
					
					lastVolume = volume;
					lastChapter = chapter;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			f.setTicker(null);
			break;
		}
		}
		running = false;
	}

	private void start(int i) {
		try {
			synchronized(this) {
				run = i;
				new Thread(this).start();
				wait();
			}
		} catch (Exception e) {}
	}

	private static void scheduleCover(ImageItem img, String mangaId) {
		// засунуть имагитем в очередь на скачивание обложки
		coversToLoad.addElement(new Object[] { mangaId, img });
		synchronized (coverLoadLock) {
			coverLoadLock.notifyAll();
		}
	}
	
	// возвращает файлнейм обложки
	private static String getMangaCover(String mangaId) throws Exception {
		JSONStream j = apiStream("cover?manga[]=" + mangaId);
//		String filename = j.getArray("data").getObject(0).getObject("attributes").getString("fileName");
		try {
			if (!(j.nextTrim() == '{' &&
					j.jumpToKey("data") &&
					j.nextTrim() == '[' &&
					j.nextTrim() == '{' &&
					j.jumpToKey("attributes") &&
					j.nextTrim() == '{' &&
					j.jumpToKey("fileName"))) throw new Exception("corrupt");
			return j.nextString();
		} finally {
			j.close();
		}
	}

	private static String getTitle(JSONObject j) {
		if (j.has("en")) return j.getString("en");
		return "";
	}

	private static String getTitle(JSONArray j) {
		int l = j.size();
		for (int i = 0; i < l; i++) {
			JSONObject t = j.getObject(i);
			if (t.has("en")) return t.getString("en");
		}
		return null;
	}
	
	private static int getHeight() {
		// а что выдает это на форме?
		return display.getCurrent().getHeight();
	}
	
	private static void display(Alert a, Displayable d) {
		if(d == null) {
			display.setCurrent(a);
			return;
		}
		display.setCurrent(a, d);
	}

	private static void display(Displayable d) {
		if(d instanceof Alert) {
			display.setCurrent((Alert) d, mainForm);
			return;
		}
		display.setCurrent(d);
	}

	private static Alert errorAlert(String text) {
		Alert a = new Alert("");
		a.setType(AlertType.ERROR);
		a.setString(text);
		a.setTimeout(2000);
		return a;
	}
	
	// http
	
	private static JSONObject api(String url) throws IOException {
		JSONObject j = JSON.getObject(getUtf(proxyUrl(APIURL.concat(url))));
		System.out.println(j);
		// хендлить ошибки апи
		if ("error".equals(j.get("result", ""))) {
			throw new RuntimeException("API " + j.getArray("errors").getObject(0).toString());
		}
		return j;
	}
	
	private static JSONStream apiStream(String url) throws IOException {
		// коннекшн остается гнить незакрытым, не порядок
		HttpConnection hc = open(proxyUrl(APIURL.concat(url)));
		
		int r;
		if((r = hc.getResponseCode()) >= 400) {
			throw new IOException("HTTP " + r);
		}
		return JSONStream.getStream(hc.openInputStream());
	}
	
	private static String proxyUrl(String url) {
		System.out.println(url);
		if(url == null || proxyUrl == null || proxyUrl.length() == 0 || "https://".equals(proxyUrl)) {
			return url;
		}
		return proxyUrl + url(url);
	}

	private static Image getImage(String url) throws IOException {
		byte[] b = get(url);
		return Image.createImage(b, 0, b.length);
	}
	
	private static byte[] readBytes(InputStream inputStream, int initialSize, int bufferSize, int expandSize) throws IOException {
		if (initialSize <= 0) initialSize = bufferSize;
		byte[] buf = new byte[initialSize];
		int count = 0;
		byte[] readBuf = new byte[bufferSize];
		int readLen;
		while ((readLen = inputStream.read(readBuf)) != -1) {
			if(count + readLen > buf.length) {
				byte[] newbuf = new byte[count + expandSize];
				System.arraycopy(buf, 0, newbuf, 0, count);
				buf = newbuf;
			}
			System.arraycopy(readBuf, 0, buf, count, readLen);
			count += readLen;
		}
		if(buf.length == count) {
			return buf;
		}
		byte[] res = new byte[count];
		System.arraycopy(buf, 0, res, 0, count);
		return res;
	}
	
	private static byte[] get(String url) throws IOException {
		HttpConnection hc = null;
		InputStream in = null;
		try {
			hc = open(url);
			int r;
			if((r = hc.getResponseCode()) >= 400) {
				throw new IOException("HTTP " + r);
			}
			in = hc.openInputStream();
			return readBytes(in, (int) hc.getLength(), 8*1024, 16*1024);
		} finally {
			try {
				if (in != null) in.close();
			} catch (IOException e) {
			}
			try {
				if (hc != null) hc.close();
			} catch (IOException e) {
			}
		}
	}

	private static String getUtf(String url) throws IOException {
		HttpConnection hc = null;
		InputStream in = null;
		try {
			hc = open(url);
			int i, j, k = 0;
			if((i = hc.getResponseCode()) >= 400) {
				throw new IOException("HTTP " + i);
			}
			String r;
			while(i >= 300) {
				if(++k > 3) {
					throw new IOException("Too many redirects!");
				}
				if((r = hc.getHeaderField("Location")).startsWith("/")) {
					r = url.substring(0, (j = url.indexOf("//") + 2)) + url.substring(j, url.indexOf("/", j)) + r;
				}
				hc.close();
				hc = open(r);
				if((i = hc.getResponseCode()) >= 400) {
					throw new IOException("HTTP " + i);
				}
			}
			in = hc.openInputStream();
			byte[] buf = new byte[(i = (int) hc.getLength()) <= 0 ? 1024 : i];
			i = 0;
			while((j = in.read(buf, i, buf.length - i)) != -1) {
				if((i += j) == buf.length) {
					System.arraycopy(buf, 0, buf = new byte[i + 2048], 0, i);
				}
			}
			return new String(buf, 0, i, "UTF-8");
		} finally {
			try {
				if (in != null) in.close();
			} catch (IOException e) {}
			try {
				if (hc != null) hc.close();
			} catch (IOException e) {}
		}
	}
	
	private static HttpConnection open(String url) throws IOException {
		HttpConnection hc = (HttpConnection) Connector.open(url);
		hc.setRequestMethod("GET");
		hc.setRequestProperty("User-Agent", "j2me-client/" + version + " (https://github.com/shinovon)");
		return hc;
	}
	
	public static String url(String url) {
		StringBuffer sb = new StringBuffer();
		char[] chars = url.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			int c = chars[i];
			if (65 <= c && c <= 90) {
				sb.append((char) c);
			} else if (97 <= c && c <= 122) {
				sb.append((char) c);
			} else if (48 <= c && c <= 57) {
				sb.append((char) c);
			} else if (c == 32) {
				sb.append("%20");
			} else if (c == 45 || c == 95 || c == 46 || c == 33 || c == 126 || c == 42 || c == 39 || c == 40
					|| c == 41) {
				sb.append((char) c);
			} else if (c <= 127) {
				sb.append(hex(c));
			} else if (c <= 2047) {
				sb.append(hex(0xC0 | c >> 6));
				sb.append(hex(0x80 | c & 0x3F));
			} else {
				sb.append(hex(0xE0 | c >> 12));
				sb.append(hex(0x80 | c >> 6 & 0x3F));
				sb.append(hex(0x80 | c & 0x3F));
			}
		}
		return sb.toString();
	}

	private static String hex(int i) {
		String s = Integer.toHexString(i);
		return "%".concat(s.length() < 2 ? "0" : "").concat(s);
	}
	
	// date utils
	
	static String localizeTime(String date) {
		Calendar c = getLocalizedCalendar(date);
		long now = System.currentTimeMillis();
		long t = c.getTime().getTime();
		long d = now - t;
		
		d /= 1000L;
		
		if (d < 60) {
			if (d == 1) return Integer.toString((int) d).concat(" second ago");
			return Integer.toString((int) d).concat(" seconds ago");
		}
		
		if (d < 60 * 60) {
			d /= 60L;
			if (d == 1) return Integer.toString((int) d).concat(" minute ago");
			return Integer.toString((int) d).concat(" minutes ago");
		}
		
		if (d < 24 * 60 * 60) {
			d /= 60 * 60L;
			if (d == 1) return Integer.toString((int) d).concat(" hour ago");
			return Integer.toString((int) d).concat(" hours ago");
		}
		
		if (d < 365 * 60 * 60) {
			d /= 24 * 60 * 60L;
			if (d == 1) return Integer.toString((int) d).concat(" day ago");
			return Integer.toString((int) d).concat(" days ago");
		}

		d /= 365 * 60 * 60L;
		if (d == 1) return Integer.toString((int) d).concat(" year ago");
		return Integer.toString((int) d).concat(" years ago");
	}
	
	static Calendar getLocalizedCalendar(String date) {
		Calendar c = parseDate(date);
		c.setTime(new Date(c.getTime().getTime() - parseTimeZone(date) + parseTimeZone(timezone)));
		return c;
	}
	
	// парсер даты ISO 8601 без учета часового пояса
	static Calendar parseDate(String date) {
		Calendar c = Calendar.getInstance();
		if(date.indexOf('T') != -1) {
			String[] dateSplit = split(date.substring(0, date.indexOf('T')), '-');
			String[] timeSplit = split(date.substring(date.indexOf('T')+1), ':');
			String second = split(timeSplit[2], '.')[0];
			int i = second.indexOf('+');
			if(i == -1) {
				i = second.indexOf('-');
			}
			if(i != -1) {
				second = second.substring(0, i);
			}
			c.set(Calendar.YEAR, Integer.parseInt(dateSplit[0]));
			c.set(Calendar.MONTH, Integer.parseInt(dateSplit[1])-1);
			c.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dateSplit[2]));
			c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeSplit[0]));
			c.set(Calendar.MINUTE, Integer.parseInt(timeSplit[1]));
			c.set(Calendar.SECOND, Integer.parseInt(second));
		} else {
			String[] dateSplit = split(date, '-');
			c.set(Calendar.YEAR, Integer.parseInt(dateSplit[0]));
			c.set(Calendar.MONTH, Integer.parseInt(dateSplit[1])-1);
			c.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dateSplit[2]));
		}
		return c;
	}
	
	// отрезать таймзону из даты
	static String getTimeZoneStr(String date) {
		int i = date.lastIndexOf('+');
		if(i == -1)
			i = date.lastIndexOf('-');
		if(i == -1)
			return null;
		return date.substring(i);
	}

	// получение оффсета таймзоны даты в миллисекундах
	static int parseTimeZone(String date) {
		int i = date.lastIndexOf('+');
		boolean m = false;
		if(i == -1) {
			i = date.lastIndexOf('-');
			m = true;
		}
		if(i == -1)
			return 0;
		date = date.substring(i + 1);
		int offset = date.lastIndexOf(':');
		offset = (Integer.parseInt(date.substring(0, offset)) * 3600000) +
				(Integer.parseInt(date.substring(offset + 1)) * 60000);
		return m ? -offset : offset;
	}
	
	static String n(int n) {
		if(n < 10) {
			return "0".concat(Integer.toString(n));
		} else return Integer.toString(n);
	}
	
	static String[] split(String str, char d) {
		int i = str.indexOf(d);
		if(i == -1)
			return new String[] {str};
		Vector v = new Vector();
		v.addElement(str.substring(0, i));
		while(i != -1) {
			str = str.substring(i + 1);
			if((i = str.indexOf(d)) != -1)
				v.addElement(str.substring(0, i));
			i = str.indexOf(d);
		}
		v.addElement(str);
		String[] r = new String[v.size()];
		v.copyInto(r);
		return r;
	}
	
	// image utils

	private static Image resize(Image src_i, int size_w, int size_h) {
		// set source size
		int w = src_i.getWidth();
		int h = src_i.getHeight();

		// no change??
		if (size_w == w && size_h == h)
			return src_i;

		int[] dst = new int[size_w * size_h];

		resize_rgb_filtered(src_i, dst, w, h, size_w, size_h);

		// not needed anymore
		src_i = null;

		return Image.createRGBImage(dst, size_w, size_h, true);
	}

	private static final void resize_rgb_filtered(Image src_i, int[] dst, int w0, int h0, int w1, int h1) {
		int[] buffer1 = new int[w0];
		int[] buffer2 = new int[w0];

		// UNOPTIMIZED bilinear filtering:               
		//         
		// The pixel position is defined by y_a and y_b,
		// which are 24.8 fixed point numbers
		// 
		// for bilinear interpolation, we use y_a1 <= y_a <= y_b1
		// and x_a1 <= x_a <= x_b1, with y_d and x_d defining how long
		// from x/y_b1 we are.
		//
		// since we are resizing one line at a time, we will at most 
		// need two lines from the source image (y_a1 and y_b1).
		// this will save us some memory but will make the algorithm 
		// noticeably slower

		for (int index1 = 0, y = 0; y < h1; y++) {

			final int y_a = ((y * h0) << 8) / h1;
			final int y_a1 = y_a >> 8;
			int y_d = y_a & 0xFF;

			int y_b1 = y_a1 + 1;
			if (y_b1 >= h0) {
				y_b1 = h0 - 1;
				y_d = 0;
			}

			// get the two affected lines:
			src_i.getRGB(buffer1, 0, w0, 0, y_a1, w0, 1);
			if (y_d != 0)
				src_i.getRGB(buffer2, 0, w0, 0, y_b1, w0, 1);

			for (int x = 0; x < w1; x++) {
				// get this and the next point
				int x_a = ((x * w0) << 8) / w1;
				int x_a1 = x_a >> 8;
				int x_d = x_a & 0xFF;

				int x_b1 = x_a1 + 1;
				if (x_b1 >= w0) {
					x_b1 = w0 - 1;
					x_d = 0;
				}

				// interpolate in x
				int c12, c34;
				int c1 = buffer1[x_a1];
				int c3 = buffer1[x_b1];

				// interpolate in y:
				if (y_d == 0) {
					c12 = c1;
					c34 = c3;
				} else {
					int c2 = buffer2[x_a1];
					int c4 = buffer2[x_b1];

					final int v1 = y_d & 0xFF;
					final int a_c2_RB = c1 & 0x00FF00FF;
					final int a_c2_AG_org = c1 & 0xFF00FF00;

					final int b_c2_RB = c3 & 0x00FF00FF;
					final int b_c2_AG_org = c3 & 0xFF00FF00;

					c12 = (a_c2_AG_org + ((((c2 >>> 8) & 0x00FF00FF) - (a_c2_AG_org >>> 8)) * v1)) & 0xFF00FF00
							| (a_c2_RB + ((((c2 & 0x00FF00FF) - a_c2_RB) * v1) >> 8)) & 0x00FF00FF;
					c34 = (b_c2_AG_org + ((((c4 >>> 8) & 0x00FF00FF) - (b_c2_AG_org >>> 8)) * v1)) & 0xFF00FF00
							| (b_c2_RB + ((((c4 & 0x00FF00FF) - b_c2_RB) * v1) >> 8)) & 0x00FF00FF;
				}

				// final result

				final int v1 = x_d & 0xFF;
				final int c2_RB = c12 & 0x00FF00FF;

				final int c2_AG_org = c12 & 0xFF00FF00;
				dst[index1++] = (c2_AG_org + ((((c34 >>> 8) & 0x00FF00FF) - (c2_AG_org >>> 8)) * v1)) & 0xFF00FF00
						| (c2_RB + ((((c34 & 0x00FF00FF) - c2_RB) * v1) >> 8)) & 0x00FF00FF;
			}
		}
	}

}
