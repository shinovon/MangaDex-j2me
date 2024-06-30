/**
 * Copyright (c) 2024 Arman Jussupgaliyev
 */
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.io.file.FileConnection;
import javax.microedition.lcdui.*;
import javax.microedition.midlet.MIDlet;
import javax.microedition.rms.RecordStore;

import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;
import cc.nnproject.json.JSONStream;

public class MangaApp extends MIDlet implements Runnable, CommandListener, ItemCommandListener, LangConstants {

	private static final int RUN_MANGAS = 1;
	private static final int RUN_MANGA = 2;
	private static final int RUN_COVERS = 3;
	private static final int RUN_CHAPTERS = 4;
	private static final int RUN_CHAPTER = 5;
//	private static final int RUN_BOOKMARKS = 6;
	private static final int RUN_DOWNLOAD_CHAPTER = 7;
	static final int RUN_PRELOADER = 8;
	private static final int RUN_CHANGE_CHAPTER = 9;
	
	private static final int LIST_UPDATES = 1;
	private static final int LIST_RECENT = 2;
	private static final int LIST_SEARCH = 3;
	private static final int LIST_ADVANCED_SEARCH = 4;
	private static final int LIST_RELATED = 5;
	
	private static final String SETTINGS_RECORDNAME = "mangaDsets";
	
	private static final String APIURL = "https://api.mangadex.org/";
	private static final String COVERSURL = "https://uploads.mangadex.org/covers/";

//	private static final Font largeboldfont = Font.getFont(0, Font.STYLE_BOLD, Font.SIZE_LARGE);
	private static final Font largefont = Font.getFont(0, 0, Font.SIZE_LARGE);
	private static final Font medboldfont = Font.getFont(0, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
	private static final Font medfont = Font.getFont(0, 0, Font.SIZE_MEDIUM);
	private static final Font smallboldfont = Font.getFont(0, Font.STYLE_BOLD, Font.SIZE_SMALL);
	private static final Font smallfont = Font.getFont(0, 0, Font.SIZE_SMALL);
	
	private static final String[] MANGA_STATUSES = {
			"ongoing", "completed", "hiatus", "cancelled"
	};
	
	private static final String[] MANGA_DEMOGRAPHIC = {
			"shounen", "shoujo", "josei", "seinen", "none"
	};
	
	private static final String[] CONTENT_RATINGS = {
			"safe", "suggestive", "erotica", "pornographic"
	};

	static String[] L;

	private static boolean started;
	private static Display display;
	public static MangaApp midlet;
	
	// команды
	private static Command exitCmd;
	static Command backCmd;
	private static Command settingsCmd;
	private static Command aboutCmd;
	
	private static Command searchCmd;
	private static Command updatesCmd;
//	private static Command bookmarksCmd;
	private static Command advSearchCmd;
	private static Command recentCmd;
	private static Command randomCmd;
	
	private static Command advSubmitCmd;
	
	private static Command mangaItemCmd;
	private static Command chaptersCmd;
	private static Command tagItemCmd;
//	private static Command saveCmd;
	private static Command showCoverCmd;
	private static Command chapterCmd;
	private static Command chapterPageItemCmd;
	private static Command relatedCmd;
	private static Command downloadCmd;
	private static Command openFromPageCmd;
	private static Command downloadCoverCmd;
	private static Command showLinkCmd;

	private static Command prevPageCmd;
	private static Command nextPageCmd;
	private static Command gotoPageCmd;
	private static Command nPageCmd;
	private static Command toggleOrderCmd;

	static Command goCmd;
	static Command cancelCmd;
	private static Command openCmd;
	private static Command okCmd;
	
	// ui
	private static Form mainForm;
	private static Form listForm;
	static Form mangaForm;
	static Form chaptersForm;
	private static Form searchForm;
	private static Form settingsForm;
//	private static Form bookmarksForm;
//	private static Form viewForm;
	private static Form tempListForm;
	
	private static TextField searchField;

	// элементы расширенного поиска
	private static TextField advTitleField;
	private static TextField advYearField;
	private static ChoiceGroup advStatusChoice;
	private static ChoiceGroup advDemographicChoice;
	private static ChoiceGroup advRatingChoice;
	private static ChoiceGroup advSortChoice;
	
	// элементы настроек
	private static TextField proxyField;
	private static ChoiceGroup coversChoice;
	private static ChoiceGroup contentFilterChoice;
	private static ChoiceGroup langChoice;
	private static ChoiceGroup itemsLimitChoice;
	private static ChoiceGroup chaptersLimitChoice;
	private static ChoiceGroup chaptersOrderChoice;
	private static TextField downloadPathField;
	private static Gauge coverSizeGauge;
	private static ChoiceGroup viewModeChoice;
	private static TextField chapterLangField;
	
	private static Alert downloadAlert;
	private static Gauge downloadIndicator;
	
	// трединг
	private static int run;
	private static boolean running;
	private static int threadsCount;
	
	// список манги
	private static String query;
	private static int listOffset = 0;
	private static int listTotal;
	private static int listMode;
	private static int prevListMode;
	
	// манга
	private static String mangaId;
	private static ImageItem mangaItem;
	private static String mangaLastChapter;
	private static Vector relatedManga = new Vector();

	// список глав
	private static String chapterId;
	private static int chaptersOffset = 0;
	private static int chaptersTotal;
	private static Hashtable chapterItems = new Hashtable();
	private static boolean chaptersOrder;
	
	// для просмотра
	private static String chapterBaseUrl;
	private static String chapterHash;
	private static Vector chapterFilenames;
	static int chapterPages;
	private static int chapterPage;
	private static String chapterVolume;
	private static String chapterNum;
	private static String chapterLang;
//	private static String chapterGroup;
//	private static int chapterDir;
	private static String chapterNext;
	private static ViewCommon view; // канва
	
	private static Object coverLoadLock = new Object();
	private static Vector coversToLoad = new Vector();
	private static Hashtable mangaCoversCache = new Hashtable();
	private static Object coverParseLock = new Object();
	private static boolean coversParsed;
	
	private static String version;
	private static String platform;
	
	private static Image coverPlaceholder;
	
	// настройки
	private static String proxyUrl = "http://nnp.nnchan.ru/hproxy.php?";
	private static int coverLoading = 0; // 0 - auto, 1 - single, 2 - multi thread, 3 - disabled
	private static boolean[] contentFilter = {true, true, true, false};
	private static String lang = "en";
	private static int listLimit = 8;
	private static int chaptersLimit = 32;
	private static boolean chaptersOrderDef = false;
	private static String downloadPath = "E:/MangaDex";
	private static int coverSize = 10;
	static int viewMode;
	static int cachingPolicy;
	static boolean keepBitmap;
	static boolean invertPan;
	static boolean files;
	private static String chapterLangFilter = "";

	public MangaApp() {}

	protected void destroyApp(boolean unconditional) {}

	protected void pauseApp() {}

	protected void startApp() {
		if (started) return;
		started = true;
		
		midlet = this;
		
		version = getAppProperty("MIDlet-Version");
		display = Display.getDisplay(this);
		
		// определения дефолтного пути куда будет скачиваться манга
		String p = platform = System.getProperty("microedition.platform");
		if (p != null && p.indexOf("platform=S60") != -1) { // 9.3 и выше
			downloadPath = "E:/MangaDex";
		} else if ((p = System.getProperty("java.vendor")) != null && p.indexOf("Android") != -1) { // ж2ме лодырь
			downloadPath = "C:/MangaDex";
		} else if (System.getProperty("kemulator.version") != null) { // ннмод
			downloadPath = "root/MangaDex";
		} else {
			// попробовать галерею
			try {
				downloadPath = System.getProperty("fileconn.dir.photos").substring(8).concat("MangaDex");
			} catch (Exception e) {
				downloadPath = "C:/MangaDex";
			}
		}
		
		// загрузка настроек
		try {
			RecordStore r = RecordStore.openRecordStore(SETTINGS_RECORDNAME, false);
			JSONObject j = JSONObject.parseObject(new String(r.getRecord(1), "UTF-8"));
			r.closeRecordStore();
			
			proxyUrl = j.getString("proxy", proxyUrl);
			coverLoading = j.getInt("coverLoading", coverLoading);
			lang = j.getString("lang", lang);
			for (int i = 0; i < 4; i++) {
				contentFilter[i] = j.getBoolean("contentFilter.".concat(Integer.toString(i)), contentFilter[i]);
			}
			listLimit = j.getInt("listLimit", listLimit);
			chaptersLimit = j.getInt("chaptersLimit", chaptersLimit);
			chaptersOrderDef = j.getBoolean("chaptersOrder", chaptersOrderDef);
			downloadPath = j.getString("downloadPath", downloadPath);
			coverSize = j.getInt("coverSize", coverSize);
			viewMode = j.getInt("viewMode", viewMode);
			chapterLangFilter = j.getString("chapterLangFilter", chapterLangFilter);
		} catch (Exception e) {}
		
		// загрузка локализации
		(L = new String[100])[0] = "MangaDex";
		try {
			loadLocale(lang);
		} catch (Exception e) {
			try {
				loadLocale(lang = "en");
			} catch (Exception e2) {
				// crash on fail
				throw new RuntimeException(lang);
			}
		}
		
		// команды
		
		exitCmd = new Command(L[Exit], Command.EXIT, 2);
		backCmd = new Command(L[Back], Command.EXIT, 2);
		settingsCmd = new Command(L[Settings], Command.SCREEN, 3);
		aboutCmd = new Command(L[About], Command.SCREEN, 4);
		
		searchCmd = new Command(L[Search], Command.ITEM, 1);
		updatesCmd = new Command(L[Updates], Command.ITEM, 1);
//		bookmarksCmd = new Command(L[Bookmarks], Command.ITEM, 1);
		advSearchCmd = new Command(L[AdvSearch], Command.ITEM, 1);
		recentCmd = new Command(L[Recent], Command.ITEM, 1);
		randomCmd = new Command(L[Random], Command.ITEM, 1);
		
		advSubmitCmd = new Command(L[Search], Command.OK, 1);
		
		mangaItemCmd = new Command(L[Open], Command.ITEM, 1);
		chaptersCmd = new Command(L[Chapters], Command.SCREEN, 2);
		tagItemCmd = new Command(L[Tag], Command.ITEM, 1);
//		saveCmd = new Command(L[AddToFavorite], Command.SCREEN, 3);
		showCoverCmd = new Command(L[ShowCover], Command.ITEM, 1);
		chapterCmd = new Command(L[Open], Command.ITEM, 1);
		chapterPageItemCmd = new Command(L[ViewPage], Command.ITEM, 1);
		relatedCmd = new Command(L[Related], Command.ITEM, 1);
		downloadCmd = new Command(L[Download], Command.ITEM, 3);
		openFromPageCmd = new Command(L[OpenFromPage], Command.ITEM, 4);
		downloadCoverCmd = new Command(L[DownloadCover], Command.ITEM, 2);
		showLinkCmd = new Command(L[ShowLink], Command.SCREEN, 5);
		
		nextPageCmd = new Command(L[NextPage], Command.SCREEN, 2);
		prevPageCmd = new Command(L[PrevPage], Command.SCREEN, 3);
		gotoPageCmd = new Command(L[GoToPage], Command.SCREEN, 4);
		nPageCmd = new Command(L[Page], Command.ITEM, 2);
		toggleOrderCmd = new Command(L[ToggleOrder], Command.SCREEN, 5);

		goCmd = new Command(L[Go], Command.OK, 1);
		cancelCmd = new Command(L[Cancel], Command.CANCEL, 2);
		openCmd = new Command(L[Open], Command.OK, 1);
		okCmd = new Command(L[Open], Command.OK, 1);
		
		// главная форма
		
		Form f = new Form(L[0]);
		f.addCommand(exitCmd);
		f.addCommand(settingsCmd);
		f.addCommand(aboutCmd);
		f.setCommandListener(this);
		
		if (coverLoading != 3)
		try {
			f.append(new ImageItem(null, Image.createImage("/md.png"), Item.LAYOUT_LEFT, null));
		} catch (Exception ignored) {}
		
		StringItem s;
		
		s = new StringItem(null, L[0]);
		s.setFont(largefont);
		s.setLayout(Item.LAYOUT_VCENTER | Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER);
		f.append(s);
		
		searchField = new TextField("", "", 200, TextField.NON_PREDICTIVE);
		f.append(searchField);
		
		s = new StringItem(null, L[Search], StringItem.BUTTON);
		s.setFont(Font.getDefaultFont());
		s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
		s.addCommand(searchCmd);
		s.setDefaultCommand(searchCmd);
		s.setItemCommandListener(this);
		f.append(s);
		
		s = new StringItem(null, L[RecentlyAdded], StringItem.BUTTON);
		s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
		s.addCommand(recentCmd);
		s.setDefaultCommand(recentCmd);
		s.setItemCommandListener(this);
		f.append(s);
		
		s = new StringItem(null, L[LatestUpdates], StringItem.BUTTON);
		s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
		s.addCommand(updatesCmd);
		s.setDefaultCommand(updatesCmd);
		s.setItemCommandListener(this);
		f.append(s);
		
		s = new StringItem(null, L[AdvancedSearch], StringItem.BUTTON);
		s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
		s.addCommand(advSearchCmd);
		s.setDefaultCommand(advSearchCmd);
		s.setItemCommandListener(this);
		f.append(s);
		
		s = new StringItem(null, L[Random], StringItem.BUTTON);
		s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
		s.addCommand(randomCmd);
		s.setDefaultCommand(randomCmd);
		s.setItemCommandListener(this);
		f.append(s);
		
//		s = new StringItem(null, L[Bookmarks], StringItem.BUTTON);
//		s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
//		s.addCommand(bookmarksCmd);
//		s.setDefaultCommand(bookmarksCmd);
//		s.setItemCommandListener(this);
//		f.append(s);
		
		display.setCurrent(mainForm = f);
		
		// запустить тред обложек
		start(RUN_COVERS);
		
		// второй тред обложек если симбиан
		if (coverLoading != 1 && ((platform != null && platform.indexOf("platform=S60") != -1) || coverLoading == 2)) {
			start(RUN_COVERS);
//			start(RUN_COVERS);
		}
	}

	public void commandAction(Command c, Displayable d) {
		if (d == mangaForm) {
			if (c == chaptersCmd) {
				// открыть список глав
				if (running) return;
				
				Form f = new Form(mangaForm.getTitle());
				f.addCommand(backCmd);
				f.setCommandListener(this);
				f.setTicker(new Ticker(L[Loading]));

				chaptersOffset = 0;
				chaptersOrder = chaptersOrderDef;
				display(chaptersForm = f);
				start(RUN_CHAPTERS);
				return;
			}
			if (c == showLinkCmd) {
				TextBox t = new TextBox("", "https://mangadex.org/title/".concat(mangaId), 200, TextField.URL);
				t.addCommand(backCmd);
				t.setCommandListener(this);
				display(t);
				return;
			}
			if (c == backCmd) {
				// возвращение из манги
				display(tempListForm != null ? tempListForm : listForm != null ? listForm : mainForm);
				mangaForm = null;
				relatedManga.removeAllElements();
				
				if (coverLoading == 3) return;
				return;
			}
		}
		if (d == listForm && c == backCmd) {
			// возвращение из списка манг
			display(searchForm != null ? searchForm : mainForm);
			coversToLoad.removeAllElements();
			mangaCoversCache.clear();
			listForm = null;
			return;
		}
		if (d == searchForm && c == backCmd) {
			// возвращение из поиска
			display(mainForm);
			searchForm = null;
			return;
		}
		if (d == chaptersForm) {
			if (c == toggleOrderCmd) {
				// переключить порядок сортировки глав
				if (running) return;
				
				chaptersOffset = 0;
				chaptersOrder = !chaptersOrder;
				start(RUN_CHAPTERS);
				return;
			}
			if (c == backCmd) {
				// возвращение из списка глав
				display(mangaForm != null ? mangaForm : listForm != null ? listForm : mainForm);
				chaptersForm = null;
				chapterItems.clear();
				return;
			}
		}
		if (d == tempListForm && c == backCmd) {
			// возвращение из времменого списка манги
			display(mangaForm != null ? mangaForm : listForm != null ? listForm : mainForm);
			coversToLoad.removeAllElements();
			listMode = prevListMode;
			tempListForm = null;
			return;
		}
		if (d == downloadAlert) {
			if (c == cancelCmd) {
				downloadIndicator = null;
			}
			return;
		}
//		if (d == bookmarksForm && c == backCmd) {
//			// возвращение из закладок
//			display(mainForm);
//			bookmarksForm = null;
//			return;
//		}
		if (d == settingsForm && c == backCmd) {
			// сохранить настройки
			proxyUrl = proxyField.getString();
			coverLoading = coversChoice.getSelectedIndex();
			contentFilterChoice.getSelectedFlags(contentFilter);
			lang = langChoice.isSelected(1) ? "ru" : "en";
			listLimit = (itemsLimitChoice.getSelectedIndex() + 1) * 8;
			chaptersLimit = (chaptersLimitChoice.getSelectedIndex() + 1) * 8;
			chaptersOrderDef = chaptersOrderChoice.isSelected(1);
			downloadPath = downloadPathField.getString();
			coverSize = coverSizeGauge.getValue();
			viewMode = viewModeChoice.getSelectedIndex();
			chapterLangFilter = chapterLangField.getString();
			
			try {
				RecordStore.deleteRecordStore(SETTINGS_RECORDNAME);
			} catch (Exception e) {}
			try {
				JSONObject j = new JSONObject();
				j.put("proxyUrl", proxyUrl);
//				j.put("timezone", timezone);
				j.put("coverLoading", coverLoading);
				j.put("lang", lang);
				if(contentFilter != null) {
					for (int i = 0; i < 4; i++) {
						j.put("contentFilter.".concat(Integer.toString(i)), contentFilter[i]);
					}
				}
				j.put("listLimit", listLimit);
				j.put("chaptersLimit", chaptersLimit);
				j.put("chaptersOrder", chaptersOrderDef);
				j.put("downloadPath", downloadPath);
				j.put("coverSize", coverSize);
				j.put("viewMode", viewMode);
				j.put("chapterLangFilter", chapterLangFilter);
				
				byte[] b = j.toString().getBytes("UTF-8");
				RecordStore r = RecordStore.openRecordStore(SETTINGS_RECORDNAME, true);
				r.addRecord(b, 0, b.length);
				r.closeRecordStore();
			} catch (Exception e) {}
			
			display(mainForm);
			// пересоздать плейсхолдер на случай если поменяли размер обложек
			makeCoverPlaceholder();
//			settingsForm = null;
			return;
		}
		if (c == settingsCmd) {
			// настройки
			if (settingsForm == null) {
				Form f = new Form(L[Settings]);
				f.addCommand(backCmd);
				f.setCommandListener(this);
				
				langChoice = new ChoiceGroup(L[InterfaceLanguage], ChoiceGroup.POPUP, new String[] {
						"English", "Русский"
				}, null);
				langChoice.setSelectedIndex("ru".equals(lang) ? 1 : 0, true);
				f.append(langChoice);
				
				String[] n = new String[] { "8", "16", "24", "32", "40" };
				
				itemsLimitChoice = new ChoiceGroup(L[ItemsPerPage], ChoiceGroup.POPUP, n, null);
				itemsLimitChoice.setSelectedIndex(Math.max(0, Math.min((listLimit / 8) - 1, 4)), true);
				f.append(itemsLimitChoice);
				
				chaptersLimitChoice = new ChoiceGroup(L[ChaptersPerPage], ChoiceGroup.POPUP, n, null);
				chaptersLimitChoice.setSelectedIndex(Math.max(0, Math.min((chaptersLimit / 8) - 1, 4)), true);
				f.append(chaptersLimitChoice);
				
				chaptersOrderChoice = new ChoiceGroup(L[ChaptersOrder], ChoiceGroup.POPUP, new String[] {
						L[Descending], L[Ascending]
				}, null);
				chaptersOrderChoice.setSelectedIndex(chaptersOrderDef ? 1 : 0, true);
				f.append(chaptersOrderChoice);
				
				chapterLangField = new TextField("Chapter language filter", chapterLangFilter, 200, TextField.NON_PREDICTIVE);
				f.append(chapterLangField);
				
				StringItem s = new StringItem(null, "Example: en,ru");
				s.setFont(smallfont);
				s.setLayout(Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
				f.append(s);
				
				coversChoice = new ChoiceGroup(L[CoversLoading], ChoiceGroup.POPUP, new String[] {
						L[Auto], L[SingleThread], L[MultiThread], L[Disabled]
				}, null);
				coversChoice.setSelectedIndex(coverLoading, true);
				f.append(coversChoice);
				
				coverSizeGauge = new Gauge(L[CoversSize], true, 25, coverSize);
				f.append(coverSizeGauge);
				
				contentFilterChoice = new ChoiceGroup(L[ContentFilter], ChoiceGroup.MULTIPLE, new String[] {
						L[Safe], L[Suggestive], L[Erotica], L[Pornographic]
				}, null);
				contentFilterChoice.setSelectedFlags(contentFilter);
				f.append(contentFilterChoice);
				
				downloadPathField = new TextField(L[DownloadPath], downloadPath, 200, TextField.NON_PREDICTIVE);
				f.append(downloadPathField);
				
				// TODO фм
				
				viewModeChoice = new ChoiceGroup(L[ViewMode], ChoiceGroup.POPUP, new String[] {
						L[Auto], "SWR", "HWA"
				}, null);
				viewModeChoice.setSelectedIndex(viewMode, true);
				f.append(viewModeChoice);
				
				proxyField = new TextField(L[ProxyURL], proxyUrl, 200, TextField.URL);
				f.append(proxyField);
				
				settingsForm = f;
			}
			display(settingsForm);
			return;
		}
		if (c == aboutCmd) {
			// о программе
			Form f = new Form(L[About]);
			f.addCommand(backCmd);
			f.setCommandListener(this);
			
			try {
				f.append(new ImageItem(null, Image.createImage("/d.png"), Item.LAYOUT_LEFT, null));
			} catch (Exception ignored) {}
			
			StringItem s;
			s = new StringItem(null, "MahoDex v" + version);
			s.setFont(largefont);
			s.setLayout(Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_VCENTER | Item.LAYOUT_LEFT);
			f.append(s);
			
			s = new StringItem(null, "Unofficial MangaDex reader client for J2ME");
			s.setFont(Font.getDefaultFont());
			s.setLayout(Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
			f.append(s);

			s = new StringItem("Developer", "shinovon");
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
			f.append(s);

			s = new StringItem("GitHub", "github.com/shinovon");
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
			f.append(s);

			s = new StringItem("Web", "nnp.nnchan.ru");
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
			f.append(s);

			s = new StringItem("Chat", "t.me/nnmidletschat");
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
			f.append(s);
			
			s = new StringItem(null, "\n292 labs (tm)");
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
			f.append(s);
			display(f);
			return;
		}
		if (c == nextPageCmd || c == prevPageCmd) {
			// переключение страниц в списке манг и глав
			if (running) return;
			coversToLoad.removeAllElements();
			
			Form f = (Form) d;
			f.setTicker(new Ticker(L[Loading]));
			if (d == listForm) {
				int o = listOffset;
				if (c == prevPageCmd) {
					o -= listLimit;
					if (o < 0) o = 0;
				} else {
					o = Math.min(o + listLimit, listTotal);
				}
				listOffset = o;
				start(RUN_MANGAS);
			} else if (d == chaptersForm) {
				int o = chaptersOffset;
				if (c == prevPageCmd) {
					o -= chaptersLimit;
					if (o < 0) o = 0;
				} else {
					o = Math.min(o + chaptersLimit, chaptersTotal);
				}
				chaptersOffset = o;
				start(RUN_CHAPTERS);
			}
			return;
		}
		if (c == gotoPageCmd) {
			// диалог страницы
			if (running) return;
			int a, b;
			if (chaptersForm != null) {
				a = (chaptersOffset / chaptersLimit) + 1;
				b = chaptersTotal / chaptersLimit + (chaptersTotal % chaptersLimit != 0 ? 1 : 0);
			} else {
				a = (listOffset / listLimit) + 1;
				b = listTotal / listLimit + (listTotal % listLimit != 0 ? 1 : 0);
			}
			TextBox t = new TextBox(L[PageNumber].concat(" (") + a + '/' + b + ")", "", 10, TextField.NUMERIC);
			t.addCommand(goCmd);
			t.addCommand(cancelCmd);
			t.setCommandListener(this);
			display(t);
			return;
		}
		if (c == advSubmitCmd) {
			// адвансед поиск
			if (running) return;
			coversToLoad.removeAllElements();
			
			// поиск и список манг
			Form f = new Form(L[0]);
			f.addCommand(backCmd);
			f.setCommandListener(this);
			f.setTicker(new Ticker(L[Loading]));
			
			listOffset = 0;
			query = null;
			listMode = LIST_ADVANCED_SEARCH;
			display(listForm = f);
			start(RUN_MANGAS);
		}
		if (d instanceof Alert) {
			if (c == okCmd) {
				// согласние на переключение диалога
				display(loadingAlert(), view);
				chapterId = chapterNext;
				chapterNext = null;
				start(RUN_CHAPTER);
				return;
			}
			// открытие главы по внешней ссылке
			if (c == openCmd) {
				try {
					if (platformRequest(proxyUrl(chapterId)))
						notifyDestroyed();
				} catch (Exception e) {}
			}
			if (view != null) {
				display(view);
				return;
			}
			if (chaptersForm != null) {
				display(chaptersForm);
				return;
			}
		}
		if (d instanceof TextBox) {
			Form f = chaptersForm != null ? chaptersForm : mangaForm != null ? mangaForm : listForm != null ? listForm : mainForm;
			if (c == openCmd) {
				// открыть главу с конкретной страницы
				int n = Integer.parseInt(((TextBox) d).getString());
				if (n < 1) {
					display(f);
					return;
				}
				chapterPage = n;
				
				start(RUN_CHAPTER);
				return;
			}
			if (c == goCmd) {
				gotoPage(f, ((TextBox) d).getString());
			}
			display(f);
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
		if (c == mangaItemCmd || c == randomCmd) {
			// открыть мангу
			if (running) return; // игнорировать запросы, пока что-то еще грузится
			coversToLoad.removeAllElements();
			
			String id = c == randomCmd ? "random" : (mangaItem = (ImageItem) item).getAltText();
			
			Form f = new Form(/*"Manga " + */id);
			f.addCommand(backCmd);
			f.addCommand(showLinkCmd);
			f.setCommandListener(this);
			f.setTicker(new Ticker(L[Loading]));
			
			mangaId = id;
			display(mangaForm = f);
			start(RUN_MANGA);
			return;
		}
		if (c == searchCmd || c == updatesCmd || c == recentCmd) {
			// открыть поиск или список последних обновленных манг
			if (running) return; // игнорировать запросы, пока что-то еще грузится
			coversToLoad.removeAllElements();
			
			// поиск и список манг
			Form f = new Form(L[0]);
			f.addCommand(backCmd);
			f.setCommandListener(this);
			f.setTicker(new Ticker(L[Loading]));
			
			listOffset = 0;
			query = c == searchCmd ? searchField.getString().trim() : null;
			
			listMode = c == searchCmd ? LIST_SEARCH : c == recentCmd ? LIST_RECENT : LIST_UPDATES;
			display(listForm = f);
			start(RUN_MANGAS);
			return;
		}
		if (c == chapterCmd) {
			// просмотр главы
			if (running) return;
			if ((chapterId = (String) chapterItems.get(item)) == null)
				return;
			if (chapterId.startsWith("http")) {
				// внешний источник
				Alert a = new Alert("Warning", "This chapter links to external source, open it?", null, AlertType.WARNING);
				a.addCommand(openCmd);
				a.addCommand(backCmd);
				a.setCommandListener(this);
				return;
			}
			
			chapterPage = 1;
			
			start(RUN_CHAPTER);
			return;
		}
		if (c == downloadCmd) {
			// скачать главу
			if (running) return;
			if ((chapterId = (String) chapterItems.get(item)) == null)
				return;
			if (chapterId.startsWith("http")) {
				// внешний источник
				display(errorAlert("Can't download, external link!"), chaptersForm); // TODO нормальный текст
				return;
			}

			Alert a = new Alert(mangaForm.getTitle(), "Initializing", null, null);
			a.setIndicator(downloadIndicator = new Gauge(null, false, 100, 0));
			a.setTimeout(Alert.FOREVER);
			a.addCommand(cancelCmd);
			a.setCommandListener(this);
			
			display(downloadAlert = a, chaptersForm);
			start(RUN_DOWNLOAD_CHAPTER);
			return;
		}
		if (c == openFromPageCmd) {
			// открыть диалог страницы
			if (running) return;
			if ((chapterId = (String) chapterItems.get(item)) == null)
				return;
			if (chapterId.startsWith("http")) {
				// внешний источник
				display(errorAlert("External link!"), chaptersForm); // TODO нормальный текст
				return;
			}
			
			TextBox t = new TextBox(L[PageNumber], "", 3, TextField.NUMERIC);
			t.addCommand(openCmd);
			t.addCommand(cancelCmd);
			t.setCommandListener(this);
			display(t);
			return;
		}
		if (c == chapterPageItemCmd) {
			try {
				if (platformRequest(proxyUrl(chapterBaseUrl + "/data-saver/" + chapterHash + '/' + ((StringItem) item).getText())))
					notifyDestroyed();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (c == showCoverCmd) {
			if (running) return;
			if (viewMode == 1) {
				view = new ViewCommon(-2, false);
			} else if(viewMode == 2) {
				view = new ViewHWA(-2);
			} else {
				String vram = System.getProperty("com.nokia.gpu.memory.total");
				if (vram != null && !vram.equals("0")) {
					view = new ViewHWA(-2);
				} else {
					view = new ViewCommon(-2, false);
				}
			}
			display(view);
			return;
		}
		if (c == downloadCoverCmd) {
			try {
				String url = proxyUrl(COVERSURL + mangaId + '/' +
						getCover((String) mangaCoversCache.get(mangaId), false) + ".512.jpg");
				
				if (platformRequest(url))
					notifyDestroyed();
			} catch (Exception e) {}
			return;
		}
		if (c == nPageCmd) {
			// перейти на конкретный номер страницы
			if (running) return;
			coversToLoad.removeAllElements();
			
			gotoPage((Form) display.getCurrent(), ((StringItem) item).getText());
			return;
		}
		if (c == advSearchCmd) {
			// форма адвансед поиска
			Form f = new Form(L[AdvancedSearch]);
			f.addCommand(advSubmitCmd);
			f.addCommand(backCmd);
			f.setCommandListener(this);
			
			TextField t;
			
			t = new TextField(L[Title], "", 200, TextField.ANY);
			f.append(advTitleField = t);
			
			t = new TextField(L[Year], "", 4, TextField.NUMERIC);
			f.append(advYearField = t);
			
			ChoiceGroup g;
			
			g = new ChoiceGroup(L[Status], ChoiceGroup.MULTIPLE, new String[] {
					L[Ongoing], L[Completed], L[Hiatus], L[Cancelled]
			}, null);
			f.append(advStatusChoice = g);
			
			g = new ChoiceGroup(L[MagazineDemographic], ChoiceGroup.MULTIPLE, new String[] {
					"Shounen", "Shoujo", "Josei", "Seinen", "None"
			}, null);
			f.append(advDemographicChoice = g);
			
			g = new ChoiceGroup(L[Rating], ChoiceGroup.MULTIPLE, new String[] {
					L[Safe], L[Suggestive], L[Erotica], L[Pornographic]
			}, null);
			// удалить отфильтрованных рейтинги
			if (!contentFilter[3]) g.delete(3);
			if (!contentFilter[2]) g.delete(2);
			if (!contentFilter[1]) g.delete(1);
			f.append(advRatingChoice = g);
			
			g = new ChoiceGroup(L[SortBy], ChoiceGroup.EXCLUSIVE, new String[] {
					"None", "Best Match", // relevance // XXX не переведено
					"Latest Upload", "Oldest Upload", // latestUploadedChapter
					"Title Ascending", "Title Descending", // title
					"Highest Rating", "Lowest Rating", // rating
					"Most Follows", "Fewest Follows", // followedCount
					"Recently Added", "Oldest Added", // createdAt
					"Year Ascending", "Year Descending" // year
			}, null);
			f.append(advSortChoice = g);
			
			// поиска по автору и по тегам видимо не будет, в запросе надо пихать их иды
			
			display(searchForm = f);
			return;
		}
		if (c == relatedCmd) {
			Form f = new Form(L[0]);
			f.addCommand(backCmd);
			f.setCommandListener(this);
			f.setTicker(new Ticker(L[Loading]));
			
			prevListMode = listMode;
			listMode = LIST_RELATED;
			display(tempListForm = f);
			start(RUN_MANGAS);
			return;
		}
//		if (c == bookmarksCmd) {
//			Form f = new Form(L[Bookmarks]);
//			f.addCommand(backCmd);
//			f.setCommandListener(this);
//			
//			display(bookmarksForm = f);
//			start(RUN_BOOKMARKS);
//			return;
//		}
		commandAction(c, display.getCurrent());
	}
	
	// трединг
	public void run() {
		int run;
		synchronized(this) {
			run = MangaApp.run;
			notify();
		}
		running = run != RUN_COVERS && run != RUN_PRELOADER;
		switch (run) {
		case RUN_MANGAS: { // поиск и список манг
			boolean temp;
			Form f = (temp = listMode == LIST_RELATED) ? tempListForm : listForm;
			
			// очистить все старое
			f.deleteAll();
			f.removeCommand(prevPageCmd);
			f.removeCommand(nextPageCmd);
			f.removeCommand(gotoPageCmd);
			
			try {
				StringBuffer sb = new StringBuffer("manga?limit=").append(listLimit);
				
				// пагинация
				if (listOffset > 0 && !temp) {
					sb.append("&offset=").append(listOffset);
					// добавить команду перехода на пред страницу
					f.addCommand(prevPageCmd);
				}

				// фильтр содержимого из настроек, не применяется в расширенном поиске
				if (listMode != LIST_ADVANCED_SEARCH && contentFilter != null) {
					int j = 0;
					for (int i = 0; i < CONTENT_RATINGS.length; i++) {
						if (!contentFilter[i]) continue;
						sb.append("&contentRating[".concat(Integer.toString(j++)).concat("]=")).append(CONTENT_RATINGS[i]);
					}
				}
				
				if (coverLoading != 3) {
					sb.append("&includes[]=cover_art");
				}
				
				switch(listMode) {
				case LIST_UPDATES: { // последние обновленные
					f.setTitle(L[0].concat(" - ").concat(L[Updates])); 
					sb.append("&order[latestUploadedChapter]=desc");
					break;
				}
				case LIST_RECENT: { // последние созданные
					f.setTitle(L[0].concat(" - ").concat(L[Recent]));
					sb.append("&order[createdAt]=desc");
					break;
				}
				case LIST_SEARCH: { // обычный поиск
					f.setTitle(L[0].concat("- ").concat(L[Search]));
					if (query != null)
						sb.append("&title=").append(url(query));
					sb.append("&order[relevance]=desc"); // сортировать по релеванции
					break;
				}
				case LIST_ADVANCED_SEARCH: { // расширенный поиск
					f.setTitle(L[0].concat("- ").concat(L[Search]));
					
					// название
					String t = advTitleField.getString().trim();
					if (t.length() > 0)
						sb.append("&title=").append(url(t));
					
					// год выпуска
					t = advYearField.getString().trim();
					if (t.length() > 0)
						sb.append("&year=").append(t);
					
					boolean[] sel = new boolean[5];
					int j = 0;
					// статус
					advStatusChoice.getSelectedFlags(sel);
					for (int i = 0; i < MANGA_STATUSES.length; i++) {
						if (!sel[i]) continue;
						sb.append("&status[".concat(Integer.toString(j++)).concat("]=")).append(MANGA_STATUSES[i]);
					}
					j = 0;
					// демография какая-то
					advDemographicChoice.getSelectedFlags(sel);
					for (int i = 0; i < MANGA_DEMOGRAPHIC.length; i++) {
						if (!sel[i]) continue;
						sb.append("&publicationDemographic["
								.concat(Integer.toString(j++)).concat("]=")).append(MANGA_DEMOGRAPHIC[i]);
					}

					j = 0;
					// возрастной рейтинг
					advRatingChoice.getSelectedFlags(sel);
					for (int i = 0; i < CONTENT_RATINGS.length && i < advRatingChoice.size(); i++) {
						if (!sel[i]) continue;
						sb.append("&contentRating[".concat(Integer.toString(j++)).concat("]=")).append(CONTENT_RATINGS[i]);
					}
					
					// сортировка
					switch (advSortChoice.getSelectedIndex()) {
					case -1:
					case 0:
						break;
					case 1:
						sb.append("&order[relevance]=desc");
						break;
					case 2:
						sb.append("&order[latestUploadedChapter]=desc");
						break;
					case 3:
						sb.append("&order[latestUploadedChapter]=asc");
						break;
					case 4:
						sb.append("&order[title]=asc");
						break;
					case 5:
						sb.append("&order[title]=desc");
						break;
					case 6:
						sb.append("&order[rating]=desc");
						break;
					case 7:
						sb.append("&order[rating]=asc");
						break;
					case 8:
						sb.append("&order[followedCount]=desc");
						break;
					case 9:
						sb.append("&order[followedCount]=asc");
						break;
					case 10:
						sb.append("&order[createdAt]=desc");
						break;
					case 11:
						sb.append("&order[createdAt]=asc");
						break;
					case 12:
						sb.append("&order[year]=asc");
						break;
					case 13:
						sb.append("&order[year]=desc");
						break;
					}
					
					break;
				}
				case LIST_RELATED: {
					f.setTitle(L[Related]);
					int l = relatedManga.size();
					for (int i = 0; i < l; i++) {
						sb.append("&ids[".concat(Integer.toString(i)).concat("]="))
						.append(((JSONObject) relatedManga.elementAt(i)).getString("id"));
					}
					break;
				}
				}
				
				JSONObject j = api(sb.toString());
				JSONArray data = j.getArray("data");
				int l = data.size();
				
				// команды пагинации
				if (!temp) {
					listTotal = j.getInt("total");
					if (listTotal > 0)
						f.addCommand(gotoPageCmd);
					
					if (listOffset < listTotal - listLimit)
						f.addCommand(nextPageCmd);
				}
				
				ImageItem item;
				for (int i = 0; i < l; i++) {
					JSONObject m = data.getObject(i);
					String id = m.getString("id");
					JSONObject attributes = m.getObject("attributes");
					JSONArray relationships = m.getArray("relationships");
					
					int k = relationships.size();
					// получение айдишника обложки
					for (int p = 0; p < k; p++) {
						JSONObject r = relationships.getObject(p);
						if (!"cover_art".equals(r.getString("type"))) continue;
						if (r.has("attributes")) {
							mangaCoversCache.put(id, r.getObject("attributes").getString("fileName"));
						} else {
							mangaCoversCache.put(id, r.getString("id"));
						}
						break;
					}

					String title = attributes.has("title") ? getTitle(attributes.getObject("title")) : "Unknown";
					item = new ImageItem(title,
							coverLoading != 3 ? coverPlaceholder : null,
							Item.LAYOUT_CENTER | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE,
							id, Item.BUTTON);
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
			String id = mangaId;
			Image thumb = null;
			
			// если уже есть загруженная обложка то берем ее
			if (mangaItem != null) {
				thumb = mangaItem.getImage();
				mangaItem = null;
			}
			
			Form f = mangaForm;
			
			relatedManga.removeAllElements();
			
			// обложка
			ImageItem coverItem = new ImageItem("", thumb, Item.LAYOUT_LEFT, id);
			coverItem.addCommand(showCoverCmd);
			coverItem.addCommand(downloadCoverCmd);
			coverItem.setDefaultCommand(showCoverCmd);
			coverItem.setItemCommandListener(this);
			coverItem.setLayout(Item.LAYOUT_NEWLINE_AFTER);
			f.append(coverItem);
			
			f.addCommand(chaptersCmd);
//			f.addCommand(saveCmd);
			
			try {
				StringBuffer sb = new StringBuffer("manga/").append(id)
						.append("?includes[0]=author&includes[1]=artist");
				if ("random".equals(id) && contentFilter != null) {
					int j = 0;
					for (int i = 0; i < CONTENT_RATINGS.length; i++) {
						if (!contentFilter[i]) continue;
						sb.append("&contentRating[".concat(Integer.toString(j++)).concat("]=")).append(CONTENT_RATINGS[i]);
					}
				}
				
				JSONObject j = api(sb.toString()).getObject("data");
				JSONObject attributes = j.getObject("attributes");
				JSONArray relationships = j.getArray("relationships");
				coverItem.setAltText(mangaId = id = j.getString("id"));
				
				JSONObject author = null,
						artist = null;
				
				int k = relationships.size();
				for (int p = 0; p < k; p++) {
					JSONObject r = relationships.getObject(p);
					String type = r.getString("type");
					if ("cover_art".equals(type)) {
						mangaCoversCache.put(id, r.getString("id"));
					} else if ("manga".equals(type)) {
						relatedManga.addElement(r);
					} else if ("author".equals(type)) {
						author = r;
					} else if ("artist".equals(type)) {
						artist = r;
					}
				}
				
				// сохраняем последнюю главу, если она есть
				mangaLastChapter = attributes.getString("lastChapter", null);
				
				StringItem s;
				String t;
				
				// большое название
				if (attributes.has("title")) {
					f.setTitle(t = getTitle(attributes.getObject("title")));
					s = new StringItem(null, t);
					s.setFont(largefont);
					s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(s);
				}
				
				// маленькое название
				if ((attributes.has("altTitle") && (t = getTitle(attributes.getArray("altTitle"))) != null) ||
						(attributes.has("altTitles") && (t = getTitle(attributes.getArray("altTitles"))) != null)) {
					s = new StringItem(null, t);
					s.setFont(smallfont);
					s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(s);
				}
				
				f.append(new Spacer(10, 16));

				// теги
				s = new StringItem(null, L[Tags]);
				s.setFont(medboldfont);
				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				f.append(s);
				
				JSONArray tags = attributes.getArray("tags");
				int l = tags.size();
				for (int i = 0; i < l; i++) {
					// отображение тегов как кнопки потому что почему нет
					s = new StringItem(null, getName(tags.getObject(i)), StringItem.BUTTON);
					s.setFont(smallfont);
					s.setLayout(Item.LAYOUT_LEFT);
					s.addCommand(tagItemCmd);
					s.setDefaultCommand(tagItemCmd);
					s.setItemCommandListener(this);
					f.append(s);
				}
				
				f.append("\n");

				// рейтинг
				s = new StringItem(null, L[Rating]);
				s.setFont(medboldfont);
				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				f.append(s);
				
				s = new StringItem(null, attributes.getString("contentRating").toUpperCase());
				s.setFont(smallfont);
				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				f.append(s);
				
				// перевод рейтинга для не англ локали
				if (!"en".equals(lang)) {
					t = attributes.getString("contentRating");
					for (int i = 0; i < 4; i ++) { // рейтингов всего 4
						if (!t.equals(CONTENT_RATINGS[i])) continue;
						t = L[Safe + i];
					}
					s.setText(t);
				}

				// статус, год
				s = new StringItem(null, L[Publication]);
				s.setFont(medboldfont);
				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				f.append(s);
				
				s = new StringItem(null, attributes.getString("year") + ", " + attributes.getString("status").toUpperCase());
				s.setFont(smallfont);
				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				f.append(s);
				
				// автор
				t = null;
				if (author != null && (t = getName(author)) != null) {
					s = new StringItem(null, L[Author]);
					s.setFont(medboldfont);
					s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(s);
					
					s = new StringItem(null, t);
					s.setFont(smallfont);
					s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(s);
				}
				
				// художник
				String a;
				if (artist != null && (a = getName(artist)) != null && !a.equals(t)) {
					s = new StringItem(null, L[Artist]);
					s.setFont(medboldfont);
					s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(s);
					
					s = new StringItem(null, a);
					s.setFont(smallfont);
					s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(s);
				}
				
				// описание
				if (attributes.has("description")) {
					s = new StringItem(null, L[Description]);
					s.setFont(medboldfont);
					s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(s);
					
					Object d = attributes.get("description");
					s = new StringItem(null, d instanceof JSONObject ? getTitle((JSONObject) d) : getTitle((JSONArray) d));
					s.setFont(smallfont);
					s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(s);
				}
				
				// добавить список альт тайтлов?
				
				// кнопки
				s = new StringItem(null, L[Chapters], StringItem.BUTTON);
				s.setFont(Font.getDefaultFont());
				s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				s.addCommand(chaptersCmd);
				s.setDefaultCommand(chaptersCmd);
				s.setItemCommandListener(this);
				f.append(s);
				
				if (!relatedManga.isEmpty()) {
					s = new StringItem(null, L[Related], StringItem.BUTTON);
					s.setFont(Font.getDefaultFont());
					s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					s.addCommand(relatedCmd);
					s.setDefaultCommand(relatedCmd);
					s.setItemCommandListener(this);
					f.append(s);
				}
				
//				s = new StringItem(null, L[Save], StringItem.BUTTON);
//				s.setFont(Font.getDefaultFont());
//				s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
//				s.addCommand(saveCmd);
//				s.setDefaultCommand(saveCmd);
//				s.setItemCommandListener(this);
//				f.append(s);
			} catch (Exception e) {
				e.printStackTrace();
				display(errorAlert(e.toString()), f);
			}
			
			// если обложка потерялась, поставить ее в очередь
			if (thumb == null || thumb == coverPlaceholder) {
				scheduleCover(coverItem, id);
			}
			f.setTicker(null);
			break;
		}
		case RUN_COVERS: { // скачиватель обложек, постоянно крутится на фоне
			try {
				if (coverPlaceholder == null && coverLoading != 3)
					makeCoverPlaceholder();
				while (true) {
					synchronized (coverLoadLock) {
						coverLoadLock.wait();
					}
					// подождать перед тем как начать грузить обложки,
					// может сверху что-то не допарсилось и они друг другу будут мешать
//					while (running) Thread.sleep(100);
					Thread.sleep(200);
					while (coversToLoad.size() > 0) {
						Object[] o = null;
						
						// получить ссылки на обложки
						synchronized (coverParseLock) {
							if (!coversParsed) {
								coversParsed = true;
								try {
									int l = coversToLoad.size();
									int j = 0;
									StringBuffer sb = new StringBuffer("cover?limit=32");
									for (int i = 0; i < l; i++) {
										String id = (String) ((Object[]) coversToLoad.elementAt(i))[0];
										if (!mangaCoversCache.containsKey(id)) continue;
										String cover = (String) mangaCoversCache.get(id);
										if (cover.indexOf('.') != -1) continue;
										sb.append("&ids[").append(j++).append("]=").append(cover);
									}
									
									if (j > 0) {
										JSONArray data = api(sb.toString()).getArray("data");
										l = data.size();
										for (int i = 0; i < l; i++) {
											JSONObject c = data.getObject(i);
											mangaCoversCache.put(c.getArray("relationships").getObject(0).get("id"),
													c.getObject("attributes").get("fileName"));
										}
									}
								} catch (Exception e) {}
							}
						}
						
						try {
							synchronized (coverLoadLock) {
								o = (Object[]) coversToLoad.elementAt(0);
								coversToLoad.removeElementAt(0);
							}
						} catch (Exception e) {
							continue;
						}
						
						if (o == null) continue;
						
						String mangaId = (String) o[0];
						ImageItem item = (ImageItem) o[1];
						
						try { 
							String filename = getCover((String) mangaCoversCache.get(mangaId), false);
							
							// картинка с меньшим размером https://api.mangadex.org/docs/03-manga/covers/
							Image img = getImage(proxyUrl(COVERSURL + mangaId + '/' + filename + ".256.jpg"));

							// ресайз обложки
							int h = (int) (getHeight() * coverSize / 25F);
							if (h < 4) h = 4; // защита от иллегал аргумент эксепшна
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
			String id = mangaId;
			Form f = chaptersForm;
			f.deleteAll();
			chapterItems.clear();
			f.removeCommand(prevPageCmd);
			f.removeCommand(nextPageCmd);
			f.addCommand(toggleOrderCmd);
			
			try {
				StringBuffer sb = new StringBuffer("chapter?manga=").append(id)
						.append("&order[chapter]=").append(chaptersOrder ? "asc" : "desc")
						.append("&limit=").append(chaptersLimit)
						.append("&includes[0]=scanlation_group&includes[1]=user")
						;
				
				if (chapterLangFilter != null && chapterLangFilter.length() > 0) {
					String[] s = split(chapterLangFilter, ',');
					for (int i = 0; i < s.length; i++) {
						sb.append("&translatedLanguage[]=").append(s[i].trim());
					}
				}
				
				if (contentFilter != null) {
					int j = 0;
					for (int i = 0; i < CONTENT_RATINGS.length; i++) {
						if (!contentFilter[i]) continue;
						sb.append("&contentRating[".concat(Integer.toString(j++)).concat("]=")).append(CONTENT_RATINGS[i]);
					}
				}
				
				// пагинация
				if (chaptersOffset > 0) {
					sb.append("&offset=").append(chaptersOffset);
					// команда предыдущей страницы
					f.addCommand(prevPageCmd);
				}
				
				JSONObject j = api(sb.toString());
				JSONArray data = j.getArray("data");
				int l = data.size();
				chaptersTotal = j.getInt("total");
				
				// команда пагинирования добавляется только если в ответе что-то есть
				if (chaptersTotal > 0)
					f.addCommand(gotoPageCmd);
				
				// проверка на последнюю страницу
				if (chaptersOffset < chaptersTotal - chaptersLimit)
					f.addCommand(nextPageCmd);
				
				sb.setLength(0);
				// надпись состояния пагинации
				StringItem s;
				s = new StringItem(null, 
						sb.append("Page: ")
						.append((chaptersOffset / chaptersLimit) + 1).append('/')
						.append(chaptersTotal / chaptersLimit + (chaptersTotal % chaptersLimit != 0 ? 1 : 0))
						.toString());
				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				f.append(s);
				
				String lastVolume = null;
				String lastChapter = null;
				for (int i = 0; i < l; i++) {
					JSONObject c = data.getObject(i);
					JSONObject a = c.getObject("attributes");
					JSONArray relationships = c.getArray("relationships");
					JSONObject user = null, scan = null;
					
					int l2 = relationships.size();
					for (int k = 0; k < l2; k++) {
						JSONObject r = relationships.getObject(k);
						String type = r.getString("type");
						if ("user".equals(type)) {
							user = r;
						} else if("scanlation_group".equals(type)) {
							scan = r;
						}
					}
					
					
					String volume = a.getString("volume"),
							chapter = a.getString("chapter"),
							title = a.getString("title"),
							time = a.getString("publishAt"),
							lang = a.getString("translatedLanguage");
					
					boolean vol = false;
					
					// группировка по томам
					if (i == 0 && (lastVolume == null && volume == null)) {
						s = new StringItem(null, "\nNo Volume");
						s.setFont(medfont);
						s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
						f.append(s);
						vol = true;
					} else if ((volume == null && lastVolume != null) ||
							(volume != null && !volume.equals(lastVolume))) {
						s = new StringItem(null, "\n"
								.concat(volume == null ? L[NoVolume] : L[VolumeNo].concat(" ").concat(volume)));
						s.setFont(medfont);
						s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
						f.append(s);
						vol = true;
					}
					
					if (chapter == null) chapter = "0";
					
					// группировка по главам
					if (i == 0 || !chapter.equals(lastChapter) || vol) {
			
						s = new StringItem(null, (vol ? "" : "\n").concat(L[ChapterNo]).concat(" ")
								.concat(chapter).concat(chapter.equals(mangaLastChapter) ? L[END] : ""));
						s.setFont(smallboldfont);
						s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
						f.append(s);
					}
					
					// внешний источник?
					boolean ext = a.has("externalUrl") && !a.isNull("externalUrl");

					// текст ссылки на главу: язык, название, время загрузки
					sb.setLength(0);
					sb.append("• ").append(lang).append(" / ")
					.append(title != null ? title : "Ch. ".concat(chapter)).append(" / ")
					.append(localizeTime(time))
					;
					
					if (scan != null) {
						sb.append('\n').append(getName(scan));
					} else if (user != null) {
						sb.append('\n').append(getName(user));
					}
					sb.append('\n');
					
					s = new StringItem(null, sb.toString());
					s.setFont(smallfont);
					s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					s.addCommand(chapterCmd);
					s.addCommand(downloadCmd);
					s.addCommand(openFromPageCmd);
					s.setDefaultCommand(chapterCmd);
					s.setItemCommandListener(this);
					f.append(s);
					chapterItems.put(s, ext ? a.get("externalUrl") : c.get("id"));
					
					lastVolume = volume;
					lastChapter = chapter;
				}
			} catch (Exception e) {
				e.printStackTrace();
				display(errorAlert(e.toString()), f);
			}
			f.setTicker(null);
			break;
		}
		case RUN_CHAPTER: { // просмотр главы
			String id = chapterId;
			if (mangaId == null || id == null) break;
			
			Form f = chaptersForm != null ? chaptersForm : mainForm;
			display(loadingAlert(), f);
			try {
				// колво и номер страницы
				JSONObject j;
				
				try {
					j = api("chapter?ids[]=" + id).getArray("data").getObject(0);
					
					JSONObject att = j.getObject("attributes");
					chapterVolume = att.getString("volume");
					chapterNum = att.getString("chapter");
					chapterLang = att.getString("translatedLanguage");
					
//					JSONArray relations = j.getArray("relationships");
//					int l = relations.size();
//					for (int i = 0; i < l; i++) {
//						JSONObject r = relations.getObject(i);
//						if (!"scanlation_group".equals(j.getString("type"))) continue;
//						chapterGroup = r.getString("id");
//					}
				} catch (Exception e) {}
				
				// получение ссылок на страницы https://api.mangadex.org/docs/04-chapter/retrieving-chapter/
				j = api("at-home/server/" + id);
				chapterBaseUrl = j.getString("baseUrl");
				chapterFilenames = new Vector();
				
				j = j.getObject("chapter");
				chapterHash = j.getString("hash");
				
				JSONArray data;
				// жпег, если нет то пнг
				if (j.has("dataSaver")) data = j.getArray("dataSaver");
				else data = j.getArray("data");
				
				int l = data.size();
				for (int i = 0; i < l; i++) {
					String n = data.getString(i);
					chapterFilenames.addElement(n);
				}
				
				int n = Math.min(chapterPage, chapterPages = chapterFilenames.size()) - 1;
				if (viewMode == 1) {
					view = new ViewCommon(n, false);
				} else if(viewMode == 2) {
					view = new ViewHWA(n);
				} else {
					String vram = System.getProperty("com.nokia.gpu.memory.total");
					if (vram != null && !vram.equals("0")) {
						view = new ViewHWA(n);
					} else {
						view = new ViewCommon(n, false);
					}
				}
				
				display(view);
			} catch (Exception e) {
				e.printStackTrace();
				display(errorAlert(e.toString()), f);
			}
			break;
		}
//		case RUN_BOOKMARKS: { // загрузить список закладок
//			break;
//		}
		case RUN_DOWNLOAD_CHAPTER: { // скачать главу
			int l = chapterFilenames.size();
			Form f = chaptersForm;
			
			downloadIndicator.setMaxValue(l + 2);
			
			FileConnection fc = null;
			HttpConnection hc;
			InputStream in;
			OutputStream out;
			String n, tn, folder = null;
			try {
				// создание папок
				try {
					fc = (FileConnection) Connector.open(folder = "file:///".concat(downloadPath).concat("/"));
					fc.mkdir();
				} catch (IOException e) {
				} finally {
					if(fc != null) fc.close();
				}
				
				try {
					fc = (FileConnection) Connector.open(folder = folder
							.concat(safeFileName(mangaForm.getTitle(), mangaId)).concat("/"));
					fc.mkdir();
				} catch (IOException e) {
				} finally {
					if(fc != null) fc.close();
				}
				
				try {
					int i;
					String v = chapterVolume;
					while (v.substring(0, (i = v.indexOf('.')) != -1 ? i : v.length()).length() < 2)
						v = "0".concat(v);
					
					String c = chapterNum;
					while (c.substring(0, (i = c.indexOf('.')) != -1 ? i : c.length()).length() < 3)
						c = "0".concat(c);
					
					String s = "Vol. " + v + " Ch. " + c + " " + chapterLang;
					fc = (FileConnection) Connector.open(folder = folder.concat(safeFileName(s, chapterId)).concat("/"));
					fc.mkdir();
				} catch (IOException e) {
				} finally {
					if(fc != null) fc.close();
				}
				
				for (int i = 0; i < l && downloadIndicator != null; i++) {
					if (downloadAlert != null)
						downloadAlert.setString(L[Preparing] + " (" + (i+1) + "/" + (l) + ")");
					downloadIndicator.setValue(i * 2 + 1);
					n = (String) chapterFilenames.elementAt(i);
					tn = Integer.toString(i + 1);
					while (tn.length() < 3) tn = "0".concat(tn);
					fc = (FileConnection) Connector.open(folder + tn + ".jpg");
					try {
						if (!fc.exists()) fc.create();
						hc = open(proxyUrl(chapterBaseUrl + "/data-saver/" + chapterHash + '/' + n));
						try {
							if (hc.getResponseCode() != 200) {
								throw new IOException("Bad response");
							}
							in = hc.openDataInputStream();
							if (downloadAlert != null)
								downloadAlert.setString(L[Downloading] + " (" + (i+1) + "/" + (l) + ")");
							downloadIndicator.setValue(i * 2 + 2);
							try {
								out = fc.openDataOutputStream();
								try {
									int r;
									byte[] buf = new byte[64 * 1024];
									while ((r = in.read(buf)) != -1) {
										out.write(buf, 0, r);
									}
									out.flush();
								} finally {
									out.close();
								}
							} finally {
								in.close();
							}
						} finally {
							hc.close();
						}
					} finally {
						fc.close();
					}
				}
				if (downloadIndicator != null) {
					display(infoAlert(L[Done].concat(" \n").concat(folder)), f);
					downloadIndicator = null;
					downloadAlert = null;
					break;
				}
			} catch (NullPointerException e) {
			} catch (Throwable e) {
				display(errorAlert(e.toString()), f);
				downloadIndicator = null;
				downloadAlert = null;
				break;
			}
			if (downloadIndicator == null) {
				display(infoAlert(L[LoadingAborted]), f);
				downloadAlert = null;
				break;
			} else display(f);
			downloadIndicator = null;
			downloadAlert = null;
			break;
		}
		case RUN_PRELOADER: {
			try {
				view.preload();
			} catch (Exception ignored) {}
			return;
		}
		case RUN_CHANGE_CHAPTER: {
			
			// TODO
			// по параму задать order
			// сначала искать совпадения по языку и по группе 
			// затем просто по языку
			// если нет то пробовать искать en
			// если нет и англ то первый попавшийся
			//
			// если ничего нет то отобразить алерт
			//
			// показывать диалог если сменился язык, при разрывах в номерах главы
			//
			// если все нормально пересоздать вью
			
			// в апи нет парамы offset_from, придется перебирать все главы
			// потом
			
//			String manga = mangaId;
//			String currentChapter = chapterId;
//			String currentChapterNum = chapterNum;
//			JSONArray data;
//			int l;
//			boolean found;
//			try {
//				s: {
//					StringBuffer sb = new StringBuffer("chapter?manga=").append(manga)
//							.append("&limit=30")
//							.append("&order[chapter]=").append(chapterDir == 1 ? "asc" : "desc")
//							;
//					int ol = sb.length();
//	
//					
//					sb.append("translatedLanguage[]=").append(chapterLang);
//					
//					if (chapterGroup != null) {
//						sb.append("groups[]=").append(chapterGroup);
//					}
//					
//					data = api(sb.toString()).getArray("data");
//					l = data.size();
//					
//					for (int i = 0; i < l; i++) {
//						JSONObject j = data.getObject(i);
//					}
//					
//					
//					if (chapterGroup != null) {
//						sb.setLength(ol);
//						sb.append("translatedLanguage[]=").append(chapterLang);
//
//					}
//	
//					sb.setLength(ol);
//				}
			display(view);
//			} catch (Exception e) {
//				display(errorAlert(e.toString()), view);
//			}
			break;
		}
		}
		running = false;
	}

	Thread start(int i) {
		Thread t = null;
		try {
			synchronized(this) {
				run = i;
				(t = new Thread(this, "Run_" + i + "_" + (threadsCount++)))
				.start();
				wait();
			}
		} catch (Exception e) {}
		return t;
	}
	
	// перейти на конкретную страницу
	private void gotoPage(Form f, String t) {
		int page = Integer.parseInt(t);
		if (page <= 0) return;
		f.setTicker(new Ticker(L[Loading]));
		
		if (f == listForm) {
			listOffset = Math.max(0, Math.min(Math.min(page - 1, listTotal / listLimit) * listLimit, listTotal)); 
			start(RUN_MANGAS);
		} else if (f == chaptersForm) {
			chaptersOffset = Math.max(0, Math.min(Math.min(page - 1, chaptersTotal / chaptersLimit) * chaptersLimit, chaptersTotal));
			start(RUN_CHAPTERS);
		}
	}

	// засунуть имагитем в очередь на скачивание обложки
	private static void scheduleCover(ImageItem img, String mangaId) {
		if (coverLoading == 3) return; // выключены
		coversParsed = false;
		synchronized (coverLoadLock) {
			coversToLoad.addElement(new Object[] { mangaId, img });
			coverLoadLock.notifyAll();
		}
	}
	
	// возвращает файлнейм обложки
	private static String getCover(String id, boolean manga) throws Exception {
		if (manga && mangaCoversCache.containsKey(id)) {
			id = (String) mangaCoversCache.get(id);
			manga = false;
		}
		if (id.indexOf('.') != -1) {
			return id;
		}
		
		JSONStream j = null;
		HttpConnection hc = open(proxyUrl(APIURL.concat("cover?" + (manga ? "manga" : "ids") + "[]=" + id)));
		try {
			int r;
			if ((r = hc.getResponseCode()) >= 400) {
				throw new IOException("HTTP " + r);
			}
			j = JSONStream.getStream(hc.openInputStream());
//			String filename = j.getArray("data").getObject(0).getObject("attributes").getString("fileName");
			if (!(j.nextTrim() == '{' &&
					j.jumpToKey("data") &&
					j.nextTrim() == '[' &&
					j.nextTrim() == '{' &&
					j.jumpToKey("attributes") &&
					j.nextTrim() == '{' &&
					j.jumpToKey("fileName")))
				throw new Exception("corrupt");
			mangaCoversCache.put(id, id = j.nextString());
			return id;
		} finally {
			if (j != null)
				try {
					j.close();
				} catch (Exception e) {}
			try {
				hc.close();
			} catch (Exception e) {}
		}
	}

	// название и описание
	private static String getTitle(JSONObject j) {
		if ("ru".equals(lang) && j.has("ru")) return j.getString("ru"); // выф
		if (j.has("en")) return j.getString("en");
		return "";
	}
	
	private static String getName(JSONObject j) {
		if (j.has("attributes")) j = j.getObject("attributes");
		if (j.has("username")) return j.getString("username");
		if (j.has("name")) {
			Object o = j.get("name");
			return o instanceof JSONObject ? getTitle((JSONObject) o) : j.getString("name");
		}
		return null;
	}

	// парсит массив альт тайтлов
	private static String getTitle(JSONArray j) {
		int l = j.size();
		for (int i = 0; i < l; i++) {
			JSONObject t = j.getObject(i);
			if (t.has("ru") && "ru".equals(lang)) return t.getString("ru"); // тоже выф
			if (t.has("en")) return t.getString("en");
		}
		return null;
	}
	
	// загрузка локали
	private void loadLocale(String lang) throws IOException {
		InputStreamReader r = new InputStreamReader(getClass().getResourceAsStream("/l/" + lang), "UTF-8");
		StringBuffer s = new StringBuffer();
		int c;
		int i = 1;
		while ((c = r.read()) > 0) {
			if (c == '\r') continue;
			if (c == '\\') {
				s.append((c = r.read()) == 'n' ? '\n' : (char) c);
				continue;
			}
			if (c == '\n') {
				L[i++] = s.toString();
				s.setLength(0);
				continue;
			}
			s.append((char) c);
		}
		r.close();
	}
	
	private static void makeCoverPlaceholder() {
		if (coverLoading == 3) {
			coverPlaceholder = null;
			return;
		}
		// создаем картинку с каким нибудь заполнением для плейсхолдера
		try {
			float h = getHeight() * coverSize / 25F;
			if (h < 4) h = 4;
			Graphics g = (coverPlaceholder = Image.createImage((int) (h / 1.6F), (int) h)).getGraphics();
			g.setColor(0x333333);
			g.fillRect(0, 0, (int) (h / 1.6F) + 1, (int) h + 1);
		} catch (Exception e) {}
	}
	
	private static int getHeight() {
		return mainForm.getHeight();
	}
	
	static void display(Alert a, Displayable d) {
		if (d == null) {
			display.setCurrent(a);
			return;
		}
		display.setCurrent(a, d);
	}

	static void display(Displayable d) {
		if (d instanceof Alert) {
			display.setCurrent((Alert) d, mainForm);
			return;
		}
		display.setCurrent(d);
		if (d == chaptersForm || d == mangaForm) {
			view = null;
			chapterFilenames = null;
			return;
		}
		if (coverLoading == 3) return;
		if (d == listForm || d == tempListForm) {
			try {
				// докачивание обложек
				int l = ((Form) d).size();
				for (int i = 0; i < l; i++) {
					Item item = ((Form) d).get(i);
					if (!(item instanceof ImageItem)) continue;
					if (((ImageItem) item).getImage() != null
							&& ((ImageItem) item).getImage() != coverPlaceholder) continue;
					scheduleCover((ImageItem) item, ((ImageItem) item).getAltText());
				}
			} catch (Exception e) {}
		}
	}

	private static Alert errorAlert(String text) {
		Alert a = new Alert("");
		a.setType(AlertType.ERROR);
		a.setString(text);
		a.setTimeout(3000);
		return a;
	}
	
	private static Alert infoAlert(String text) {
		Alert a = new Alert("");
		a.setType(AlertType.CONFIRMATION);
		a.setString(text);
		a.setTimeout(1500);
		return a;
	}
	
	private static Alert loadingAlert() {
		Alert a = new Alert("", L[Loading], null, null);
		a.setIndicator(new Gauge(null, false, Gauge.INDEFINITE, Gauge.CONTINUOUS_RUNNING));
		return a;
	}
	
	private static String safeFileName(String s, String alt) {
		if (s == null || s.trim().length() == 0)
			return alt;
		StringBuffer t = new StringBuffer();
		int l = s.length();
		for (int i = 0; (i < l && i < 36); i++) {
			char c = s.charAt(i);
			if (c == ' ' || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '-'
					|| c == '_' || c == '!') {
				t.append(c);
			}
		}

		s = s.toString().trim();
		if (s.length() == 0)
			return alt;
		return s;
	}
	
	// view

	static void changeChapter(int d) {
//		if (running) return;
//		chapterDir = d;
//		display(loadingAlert(), view);
//		
//		midlet.start(RUN_CHANGE_CHAPTER);
	}

	/**
	 * Caches an image.
	 * 
	 * @param a Data to write.
	 * @param i Number of the image, [1; pages].
	 */
	public static void cachePage(byte[] a, int i) {
		synchronized (coverParseLock) {
//			if (dir == null)
//				dir = getWD();
//			if (dir == null) {
//				NJTAI.setScr(prev);
//				NJTAI.pause(100);
//				NJTAI.setScr(folderMissed(), prev);
//				return;
//			}
	
			FileConnection fc = null;
	
			String folder = getFolderName();
	
			DataOutputStream ou = null;
	
			try {
				String n;
				int j = i + 1;
				if (j < 10) {
					n = "00" + j;
				} else if (j < 100) {
					n = "0" + j;
				} else {
					n = "" + j;
				}
				fc = (FileConnection) Connector.open(folder + n + ".jpg");
				if (fc.exists()) {
					fc.close();
					return;
				}
				fc.create();
				ou = fc.openDataOutputStream();
	
				ou.write(a, 0, a.length);
				ou.flush();
				ou.close();
				fc.close();
	
			} catch (Exception e) {
				e.printStackTrace();
				try {
					if (ou != null)
						ou.close();
				} catch (IOException e1) {
				}
				try {
					if (fc != null)
						fc.close();
				} catch (IOException e1) {
				}
			}
		}
	}

	public static byte[] readCachedPage(int i) {
//		if (dir == null)
//			dir = getWD();
//		if (dir == null) {
//			NJTAI.setScr(folderMissed(), prev);
//			return null;
//		}

		FileConnection fc = null;

		String folder = getFolderName();

		DataInputStream di = null;
		ByteArrayOutputStream b = new ByteArrayOutputStream();

		try {
			String n;
			int j = i + 1;
			if (j < 10) {
				n = "00" + j;
			} else if (j < 100) {
				n = "0" + j;
			} else {
				n = "" + j;
			}
			fc = (FileConnection) Connector.open(folder + n + ".jpg", Connector.READ);
			if (!fc.exists()) {
				return null;
			}
			di = fc.openDataInputStream();

			byte[] buf = new byte[1024 * 64];

			int len = 1;
			while ((len = di.read(buf)) != -1) {
				b.write(buf, 0, len);
			}
			di.close();
			fc.close();
			return b.toByteArray();
		} catch (Exception e) {
			e.printStackTrace();
			try {
				if (di != null)
					di.close();
			} catch (IOException e1) {
			}
			try {
				if (fc != null)
					fc.close();
			} catch (IOException e1) {
			}
		}
		return null;
	}

	public static void createFolder() {
		FileConnection fc = null;
		String folder = null;
		try {
			fc = (FileConnection) Connector.open(folder = "file:///".concat(downloadPath).concat("/"));
			fc.mkdir();
		} catch (IOException e) {
		} finally {
			if(fc != null)
				try {
					fc.close();
				} catch (IOException e) {}
		}
		try {
			fc = (FileConnection) Connector.open(folder = folder.concat(mangaId).concat("/"));
			fc.mkdir();
		} catch (IOException e) {
		} finally {
			if(fc != null)
				try {
					fc.close();
				} catch (IOException e) {}
		}
		try {
			fc = (FileConnection) Connector.open(folder = folder.concat(chapterId).concat("/"));
			fc.mkdir();
		} catch (IOException e) {
		} finally {
			if(fc != null)
				try {
					fc.close();
				} catch (IOException e) {}
		}
	}

	private static String getFolderName() {
		return "file:///".concat(downloadPath).concat("/")
				.concat(mangaId).concat("/")
				.concat(chapterId).concat("/");
	}

	public static byte[] getPage(int n) throws IOException {
		return get(proxyUrl(chapterBaseUrl + "/data-saver/" + chapterHash + '/' + chapterFilenames.elementAt(n)));
	}

	public static byte[] getCover() throws Exception {
		return get(proxyUrl(COVERSURL + mangaId + '/' +
				getCover((String) mangaCoversCache.get(mangaId), false) + ".512.jpg"));
	}
	
	// http
	
	private static JSONObject api(String url) throws IOException {
		JSONObject j = JSONObject.parseObject(getUtf(proxyUrl(APIURL.concat(url))));
//		System.out.println(j);
		// хендлить ошибки апи
		if ("error".equals(j.get("result", null))) {
			throw new RuntimeException("API " + j.getArray("errors").getObject(0).toString());
		}
		return j;
	}
	
	private static String proxyUrl(String url) {
		System.out.println(url);
		if (url == null || proxyUrl == null || proxyUrl.length() == 0 || "https://".equals(proxyUrl)) {
			return url;
		}
		return proxyUrl + url(url);
	}

	private static Image getImage(String url) throws IOException {
		byte[] b = get(url);
		return Image.createImage(b, 0, b.length);
	}
	
	private static byte[] readBytes(InputStream inputStream, int initialSize, int bufferSize, int expandSize)
			throws IOException {
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
		long now = System.currentTimeMillis();
		Calendar c = parseDate(date);
		long t = c.getTime().getTime() + c.getTimeZone().getRawOffset() - parseTimeZone(date);
		long d = now - t;
		
		boolean ru = "ru".equals(lang);
		d /= 1000L;
		
		if (d < 60) {
			if (d == 1 || (ru && d % 10 == 1 && d % 100 != 11))
				return Integer.toString((int) d).concat(L[SecondAgo]);
			if (ru && (d % 10 > 4 || d % 10 < 2))
				return Integer.toString((int) d).concat(L[SecondsAgo2]);
			return Integer.toString((int) d).concat(L[SecondsAgo]);
		}
		
		if (d < 60 * 60) {
			d /= 60L;
			if (d == 1 || (ru && d % 10 == 1 && d % 100 != 11))
				return Integer.toString((int) d).concat(L[MinuteAgo]);
			if (ru && (d % 10 > 4 || d % 10 < 2))
				return Integer.toString((int) d).concat(L[MinutesAgo2]);
			return Integer.toString((int) d).concat(L[MinutesAgo]);
		}
		
		if (d < 24 * 60 * 60) {
			d /= 60 * 60L;
			if (d == 1 || (ru && d % 10 == 1 && d % 100 != 11))
				return Integer.toString((int) d).concat(L[HourAgo]);
			if (ru && (d % 10 > 4 || d % 10 < 2))
				return Integer.toString((int) d).concat(L[HoursAgo2]);
			return Integer.toString((int) d).concat(L[HoursAgo]);
		}
		
		if (d < 365 * 24 * 60 * 60) {
			d /= 24 * 60 * 60L;
			if (d == 1 || (ru && d % 10 == 1 && d % 100 != 11))
				return Integer.toString((int) d).concat(L[DayAgo]);
			if (ru && (d % 10 > 4 || d % 10 < 2))
				return Integer.toString((int) d).concat(L[DaysAgo2]);
			return Integer.toString((int) d).concat(L[DaysAgo]);
		}

		d /= 365 * 24 * 60 * 60L;
		if (d == 1) return Integer.toString((int) d).concat(L[YearAgo]);
		if (ru && (d % 10 > 4 || d % 10 < 2))
			return Integer.toString((int) d).concat(L[YearsAgo2]);
		return Integer.toString((int) d).concat(L[YearsAgo]);
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

	static Image resize(Image src_i, int size_w, int size_h) {
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

	/**
	 * Part of tube42 imagelib. Blends 2 colors.
	 * 
	 * @param c1
	 * @param c2
	 * @param value256
	 * @return Blended value.
	 */
	public static final int blend(final int c1, final int c2, final int value256) {

		final int v1 = value256 & 0xFF;
		final int c1_RB = c1 & 0x00FF00FF;
		final int c2_RB = c2 & 0x00FF00FF;

		final int c1_AG = (c1 >>> 8) & 0x00FF00FF;

		final int c2_AG_org = c2 & 0xFF00FF00;
		final int c2_AG = (c2_AG_org) >>> 8;

		// the world-famous tube42 blend with one mult per two components:
		final int rb = (c2_RB + (((c1_RB - c2_RB) * v1) >> 8)) & 0x00FF00FF;
		final int ag = (c2_AG_org + ((c1_AG - c2_AG) * v1)) & 0xFF00FF00;
		return ag | rb;

	}

}
