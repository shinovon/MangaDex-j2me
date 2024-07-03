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
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemRegistry;
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
	private static final int RUN_CHAPTER_VIEW = 5;
//	private static final int RUN_BOOKMARKS = 6;
	private static final int RUN_DOWNLOAD_CHAPTER = 7;
	static final int RUN_PRELOADER = 8;
	private static final int RUN_CHANGE_CHAPTER = 9;
//	private static final int RUN_DISPOSE_VIEW = 10;
	private static final int RUN_AUTH = 11;
	private static final int RUN_FOLLOW = 12;
	
	private static final int LIST_UPDATES = 1;
	private static final int LIST_RECENT = 2;
	private static final int LIST_SEARCH = 3;
	private static final int LIST_ADVANCED_SEARCH = 4;
	private static final int LIST_RELATED = 5;
	private static final int LIST_FOLLOWED = 6;
	
	private static final String SETTINGS_RECORDNAME = "mangaDsets";
	private static final String AUTH_RECORDNAME = "mangaDauth";
	
	private static final String APIURL = "https://api.mangadex.org/";
	private static final String COVERSURL = "https://uploads.mangadex.org/covers/";
	private static final String AUTHURL = "https://auth.mangadex.org/realms/mangadex/protocol/openid-connect/token";

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
	
	private static final String[] LANGUAGES = {
			"en", "ru"
	};
	
	private static String[][] tags;

	static String[] L;

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
	private static Command authCmd;
	private static Command libraryCmd;
	
	private static Command advSubmitCmd;
	private static Command authSubmitCmd;
	
	private static Command mangaItemCmd;
	private static Command chaptersCmd;
	private static Command tagItemCmd;
//	private static Command saveCmd;
	private static Command showCoverCmd;
	private static Command chapterCmd;
	private static Command relatedCmd;
	private static Command downloadCmd;
	private static Command openFromPageCmd;
	private static Command downloadCoverCmd;
	private static Command showLinkCmd;
	private static Command pathCmd;
	private static Command followCmd, unfollowCmd;

	private static Command prevPageCmd;
	private static Command nextPageCmd;
	private static Command gotoPageCmd;
	private static Command toggleOrderCmd;

	static Command goCmd;
	static Command cancelCmd;
	private static Command openCmd;
	private static Command continueCmd;
	
	private static Command dirOpenCmd;
	private static Command dirSelectCmd;
	
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
	private static Form loadingForm;
	
	private static TextField searchField;

	// элементы расширенного поиска
	private static TextField advTitleField;
	private static TextField advYearField;
	private static ChoiceGroup advStatusChoice;
	private static ChoiceGroup advDemographicChoice;
	private static ChoiceGroup advRatingChoice;
	private static ChoiceGroup advSortChoice;
	private static TextField advIncludeField;
	private static ChoiceGroup advInclusionChoice;
	private static TextField advExcludeField;
	private static ChoiceGroup advExclusionChoice;
	
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
	private static ChoiceGroup keepCoversChoice;
	private static ChoiceGroup chapterCacheChoice;
	private static ChoiceGroup cachingPolicyChoice;
	private static ChoiceGroup keepBitmapChoice;
	private static ChoiceGroup jpegChoice;
	private static ChoiceGroup onlineChoice;
	private static TextField tagsFilterField;
	
	private static Alert downloadAlert;
	private static Gauge downloadIndicator;
	
	// логин
	private static Form authForm;
	private static TextField loginField;
	private static TextField passwordField;
	private static TextField clientField;
	private static TextField clientSecretField;
	
	// трединг
	private static int run;
	private static boolean running;
	
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
	private static boolean mangaFollowed;
	private static StringItem followBtn;

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
	private static String chapterGroup;
	private static int chapterDir;
	private static String chapterNextNum;
	private static String chapterNextId;
	private static ViewCommon view; // канва
	
	private static Object coverLoadLock = new Object();
	private static Vector coversToLoad = new Vector();
	private static Hashtable mangaCoversCache = new Hashtable();
	private static Object coverParseLock = new Object();
	private static boolean coversParsed;
	
	private static String version;
	
	private static Image coverPlaceholder;
	
	// файлы
	private static List fileList;
	private static String curDir;
	private static Vector rootsList;
	
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
	static int viewMode; // 0 - auto, 1 - swr, 2 - hwa
	static int cachingPolicy = 0; // 0 - disabled, 1 - keep loaded, 2 - preload
	static boolean keepBitmap;
	static boolean invertPan; // unused
	static boolean chapterFileCache;
	private static String chapterLangFilter = "";
	private static boolean keepListCovers = true;
	private static boolean dataSaver = true;
	private static boolean symbianJrt;
	private static boolean useLoadingForm;
	static boolean onlineResize = true;
	private static String tagsFilter = "";

	// auth
	private static String clientId;
	private static String clientSecret;
	private static String accessToken;
	private static String refreshToken;
	private static String username;
	private static String password;
	private static long accessTokenTime;
	private static long refreshTokenTime;
	private static int runAfterAuth;

	public MangaApp() {}

	protected void destroyApp(boolean unconditional) {}

	protected void pauseApp() {}

	protected void startApp() {
		if (midlet != null) return;
		midlet = this;
		
		version = getAppProperty("MIDlet-Version");
		display = Display.getDisplay(this);
		
		loadingForm = new Form("MangaDex");
		display.setCurrent(loadingForm);
		
		// определения дефолтного пути куда будет скачиваться манга
		String p = System.getProperty("microedition.platform");
		if (symbianJrt = p != null && p.indexOf("platform=S60") != -1) { // 9.3 и выше
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
		
		// 9.2 workaround
		useLoadingForm = !symbianJrt &&
				(System.getProperty("com.symbian.midp.serversocket.support") != null ||
				System.getProperty("com.symbian.default.to.suite.icon") != null);
		
		onlineResize = loadingForm.getWidth() < 320;
		
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
			chapterFileCache = j.getBoolean("chapterCache", chapterFileCache);
			cachingPolicy = j.getInt("cachingPolicy", cachingPolicy);
			keepBitmap = j.getBoolean("keepBitmap", keepBitmap);
			keepListCovers = j.getBoolean("keepListCovers", keepListCovers);
			dataSaver = j.getBoolean("dataSaver", dataSaver);
			onlineResize = j.getBoolean("onlineResize", onlineResize);
			tagsFilter = j.getString("tagsFilter", tagsFilter);
		} catch (Exception e) {}
		
		// загрузка локализации
		(L = new String[128])[0] = "MangaDex";
		try {
			loadLocale(lang);
		} catch (Exception e) {
			try {
				loadLocale(lang = "en");
			} catch (Exception e2) {
				// crash on fail
				throw new RuntimeException(e2.toString());
			}
		}
		
		loadingForm.append(L[Loading]);
		
		// загрузка авторизации
		try {
			RecordStore r = RecordStore.openRecordStore(AUTH_RECORDNAME, false);
			JSONObject j = JSONObject.parseObject(new String(r.getRecord(1), "UTF-8"));
			r.closeRecordStore();
			
			accessToken = j.getNullableString("accessToken");
			refreshToken = j.getNullableString("refreshToken");
			clientId = j.getNullableString("clientId");
			clientSecret = j.getNullableString("clientSecret");
			username = j.getNullableString("username");
			password = j.getNullableString("password");
			accessTokenTime = j.getLong("accessTime", 0);
			refreshTokenTime = j.getLong("refreshTime", 0);
//			start(RUN_AUTH);
		} catch (Exception e) {}
		
		// команды
		
		exitCmd = new Command(L[Exit], Command.EXIT, 2);
		backCmd = new Command(L[Back], Command.EXIT, 2);
		settingsCmd = new Command(L[Settings], Command.SCREEN, 3);
		authCmd = new Command(L[Authorization], Command.SCREEN, 4);
		aboutCmd = new Command(L[About], Command.SCREEN, 5);
		
		searchCmd = new Command(L[Search], Command.ITEM, 1);
		updatesCmd = new Command(L[Updates], Command.ITEM, 1);
//		bookmarksCmd = new Command(L[Bookmarks], Command.ITEM, 1);
		advSearchCmd = new Command(L[AdvSearch], Command.ITEM, 1);
		recentCmd = new Command(L[Recent], Command.ITEM, 1);
		randomCmd = new Command(L[Random], Command.ITEM, 1);
		libraryCmd = new Command(L[Library], Command.ITEM, 1);
		
		advSubmitCmd = new Command(L[Search], Command.OK, 1);
		authSubmitCmd = new Command("Login", Command.OK, 1);
		
		mangaItemCmd = new Command(L[Open], Command.ITEM, 1);
		chaptersCmd = new Command(L[Chapters], Command.SCREEN, 2);
		tagItemCmd = new Command(L[Tag], Command.ITEM, 1);
//		saveCmd = new Command(L[AddToFavorite], Command.SCREEN, 3);
		showCoverCmd = new Command(L[ShowCover], Command.ITEM, 1);
		chapterCmd = new Command(L[Open], Command.ITEM, 1);
		relatedCmd = new Command(L[Related], Command.ITEM, 1);
		downloadCmd = new Command(L[Download], Command.ITEM, 3);
		openFromPageCmd = new Command(L[OpenFromPage], Command.ITEM, 4);
		downloadCoverCmd = new Command(L[DownloadCover], Command.ITEM, 2);
		showLinkCmd = new Command(L[ShowLink], Command.SCREEN, 5);
		pathCmd = new Command(L[SpecifyPath], Command.ITEM, 1);
		followCmd = new Command(L[Follow], Command.ITEM, 1);
		unfollowCmd = new Command(L[Unfollow], Command.ITEM, 1);
		
		nextPageCmd = new Command(L[NextPage], Command.SCREEN, 2);
		prevPageCmd = new Command(L[PrevPage], Command.SCREEN, 3);
		gotoPageCmd = new Command(L[GoToPage], Command.SCREEN, 4);
		toggleOrderCmd = new Command(L[ToggleOrder], Command.SCREEN, 5);

		goCmd = new Command(L[Go], Command.OK, 1);
		cancelCmd = new Command(L[Cancel], Command.CANCEL, 2);
		openCmd = new Command(L[Open], Command.OK, 1);
		continueCmd = new Command(L[Continue], Command.OK, 1);
		
		dirOpenCmd = new Command(L[Open], Command.ITEM, 1);
		dirSelectCmd = new Command(L[Select], Command.SCREEN, 2);
		
		// главная форма
		
		Form f = new Form(L[0]);
		f.addCommand(exitCmd);
		f.addCommand(settingsCmd);
		f.addCommand(aboutCmd);
		f.addCommand(authCmd);
		f.setCommandListener(this);
		
		// лого, не грузить если выключены обложки
		if (coverLoading != 3)
		try {
			f.append(new ImageItem(null, Image.createImage("/md.png"), Item.LAYOUT_LEFT, null));
		} catch (Exception ignored) {}
		
		StringItem s;
		
		s = new StringItem(null, L[0]);
		s.setFont(largefont);
		s.setLayout(Item.LAYOUT_VCENTER | Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER);
		f.append(s);

		// поиск
		searchField = new TextField("", "", 200, TextField.NON_PREDICTIVE);
		searchField.addCommand(searchCmd);
		searchField.setItemCommandListener(this);
		f.append(searchField);
		
		s = new StringItem(null, L[Search], StringItem.BUTTON);
		s.setFont(Font.getDefaultFont());
		s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
		s.addCommand(searchCmd);
		s.setDefaultCommand(searchCmd);
		s.setItemCommandListener(this);
		f.append(s);
		
		// есть авторизация, добавляем доп кнопки
		if (refreshToken != null) {
			// библиотека
			s = new StringItem(null, L[Library], StringItem.BUTTON);
			s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
			s.addCommand(libraryCmd);
			s.setDefaultCommand(libraryCmd);
			s.setItemCommandListener(this);
			f.append(s);
		}
		
		// последние созданные
		s = new StringItem(null, L[RecentlyAdded], StringItem.BUTTON);
		s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
		s.addCommand(recentCmd);
		s.setDefaultCommand(recentCmd);
		s.setItemCommandListener(this);
		f.append(s);
		
		// последние обновленные
		s = new StringItem(null, L[LatestUpdates], StringItem.BUTTON);
		s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
		s.addCommand(updatesCmd);
		s.setDefaultCommand(updatesCmd);
		s.setItemCommandListener(this);
		f.append(s);
		
		// расширенный поиск
		s = new StringItem(null, L[AdvancedSearch], StringItem.BUTTON);
		s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
		s.addCommand(advSearchCmd);
		s.setDefaultCommand(advSearchCmd);
		s.setItemCommandListener(this);
		f.append(s);
		
		// рандом
		s = new StringItem(null, L[Random], StringItem.BUTTON);
		s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
		s.addCommand(randomCmd);
		s.setDefaultCommand(randomCmd);
		s.setItemCommandListener(this);
		f.append(s);
		
		display.setCurrent(mainForm = f);
		
		// запустить тред обложек
		start(RUN_COVERS);
		
		// второй тред обложек если симбиан
		if (coverLoading != 1 && (symbianJrt || coverLoading == 2)) {
			start(RUN_COVERS);
			start(RUN_COVERS);
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
			lang = LANGUAGES[langChoice.getSelectedIndex()];
			listLimit = (itemsLimitChoice.getSelectedIndex() + 1) * 8;
			chaptersLimit = (chaptersLimitChoice.getSelectedIndex() + 1) * 8;
			chaptersOrderDef = chaptersOrderChoice.isSelected(1);
			downloadPath = downloadPathField.getString();
			coverSize = coverSizeGauge.getValue();
			viewMode = viewModeChoice.getSelectedIndex();
			chapterLangFilter = chapterLangField.getString();
			keepListCovers = keepCoversChoice.isSelected(0);
			cachingPolicy = cachingPolicyChoice.getSelectedIndex();
			chapterFileCache = chapterCacheChoice.isSelected(0);
			keepBitmap = keepBitmapChoice.isSelected(0);
			dataSaver = jpegChoice.isSelected(0);
			onlineResize = onlineChoice.isSelected(0);
			tagsFilter = tagsFilterField.getString();
			
			try {
				RecordStore.deleteRecordStore(SETTINGS_RECORDNAME);
			} catch (Exception e) {}
			try {
				JSONObject j = new JSONObject();
				j.put("proxy", proxyUrl);
				j.put("coverLoading", coverLoading);
				j.put("lang", lang);
				if (contentFilter != null) {
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
				j.put("chapterCache", chapterFileCache);
				j.put("cachingPolicy", cachingPolicy);
				j.put("keepBitmap", keepBitmap);
				j.put("keepListCovers", keepListCovers);
				j.put("dataSaver", dataSaver);
				j.put("onlineResize", onlineResize);
				j.put("tagsFilter", tagsFilter);
				
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
				
				// язык интерфейса
				langChoice = new ChoiceGroup(L[InterfaceLanguage], ChoiceGroup.POPUP, new String[] {
						"English", "Русский"
				}, null);
				langChoice.setSelectedIndex("ru".equals(lang) ? 1 : 0, true);
				f.append(langChoice);
				
				String[] n = new String[] { "8", "16", "24", "32", "40" };
				String[] on_off = new String[] { L[Enabled], L[Disabled] };
				
				// колво тайтлов на страницу
				itemsLimitChoice = new ChoiceGroup(L[ItemsPerPage], ChoiceGroup.POPUP, n, null);
				itemsLimitChoice.setSelectedIndex(Math.max(0, Math.min((listLimit / 8) - 1, 4)), true);
				f.append(itemsLimitChoice);
				
				// колво глав на страницу
				chaptersLimitChoice = new ChoiceGroup(L[ChaptersPerPage], ChoiceGroup.POPUP, n, null);
				chaptersLimitChoice.setSelectedIndex(Math.max(0, Math.min((chaptersLimit / 8) - 1, 4)), true);
				f.append(chaptersLimitChoice);
				
				// порядок глав
				chaptersOrderChoice = new ChoiceGroup(L[ChaptersOrder], ChoiceGroup.POPUP, new String[] {
						L[Descending], L[Ascending]
				}, null);
				chaptersOrderChoice.setSelectedIndex(chaptersOrderDef ? 1 : 0, true);
				f.append(chaptersOrderChoice);
				
				// фильтр языков
				chapterLangField = new TextField(L[ChapterLanguageFilter], chapterLangFilter, 200, TextField.NON_PREDICTIVE);
				f.append(chapterLangField);
				
				StringItem s = new StringItem(null, L[Example].concat(": en,ru"));
				s.setFont(smallfont);
				s.setLayout(Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
				f.append(s);
				
				// загрузка обложек
				coversChoice = new ChoiceGroup(L[CoversLoading], ChoiceGroup.POPUP, new String[] {
						L[Auto], L[SingleThread], L[MultiThread], L[Disabled]
				}, null);
				coversChoice.setSelectedIndex(coverLoading, true);
				f.append(coversChoice);
				
				// размер обложек
				coverSizeGauge = new Gauge(L[CoversSize], true, 25, coverSize);
				f.append(coverSizeGauge);
				
				// фильтр содержимого
				contentFilterChoice = new ChoiceGroup(L[ContentFilter], ChoiceGroup.MULTIPLE, new String[] {
						L[Safe], L[Suggestive], L[Erotica], L[Pornographic]
				}, null);
				contentFilterChoice.setSelectedFlags(contentFilter);
				f.append(contentFilterChoice);
				
				tagsFilterField = new TextField("Exclude tags", tagsFilter, 100, TextField.NON_PREDICTIVE);
				f.append(tagsFilterField);
				
				// путь скачивания
				downloadPathField = new TextField(L[DownloadPath], downloadPath, 200, TextField.URL);
				downloadPathField.addCommand(pathCmd);
				downloadPathField.setItemCommandListener(this);
				f.append(downloadPathField);
				
				s = new StringItem("", "...", StringItem.BUTTON);
				s.addCommand(pathCmd);
				s.setDefaultCommand(pathCmd);
				s.setItemCommandListener(this);
				s.setLayout(Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
				f.append(s);
				
				// прокси
				proxyField = new TextField(L[ProxyURL], proxyUrl, 200, TextField.URL);
				f.append(proxyField);
				
				jpegChoice = new ChoiceGroup(L[ImageQuality], ChoiceGroup.POPUP, new String[] {
						"JPEG", "PNG"
				}, null);
				jpegChoice.setSelectedIndex(dataSaver ? 0 : 1, true);
				f.append(jpegChoice);
				
				onlineChoice = new ChoiceGroup("Server-side resizing", ChoiceGroup.POPUP, on_off, null);
				onlineChoice.setSelectedIndex(onlineResize ? 0 : 1, true);
				f.append(onlineChoice);
				
				// режим просмотра
				viewModeChoice = new ChoiceGroup(L[ViewMode], ChoiceGroup.POPUP, new String[] {
						L[Auto], "SWR", "HWA"
				}, null);
				viewModeChoice.setSelectedIndex(viewMode, true);
				f.append(viewModeChoice);
				
				// хранить обложки
				keepCoversChoice = new ChoiceGroup(L[KeepCoversInLists], ChoiceGroup.POPUP, on_off, null);
				keepCoversChoice.setSelectedIndex(keepListCovers ? 0 : 1, true);
				f.append(keepCoversChoice);
				
				// поведение кэширования
				cachingPolicyChoice = new ChoiceGroup(L[ChapterCaching], ChoiceGroup.POPUP, new String[] {
						L[Disabled], "Keep already loaded", L[Preload]
				}, null);
				cachingPolicyChoice.setSelectedIndex(cachingPolicy, true);
				f.append(cachingPolicyChoice);
				
				// кэшировать главу
				chapterCacheChoice = new ChoiceGroup(L[CacheChapterToFile], ChoiceGroup.POPUP, on_off, null);
				chapterCacheChoice.setSelectedIndex(chapterFileCache ? 0 : 1, true);
				f.append(chapterCacheChoice);
				
				// хранить оригиналы страниц
				keepBitmapChoice = new ChoiceGroup(L[KeepOriginalPages], ChoiceGroup.POPUP, on_off, null);
				keepBitmapChoice.setSelectedIndex(keepBitmap ? 0 : 1, true);
				f.append(keepBitmapChoice);
				
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
		if (c == followCmd || c == unfollowCmd) {
			if (running) return;
			d.setTicker(new Ticker(L[Loading]));
			
			runAfterAuth = RUN_FOLLOW;
			start(RUN_AUTH);
			return;
		}
		if (d instanceof Alert) {
			if (c == continueCmd) {
				// согласние на переключение диалога
				display(loadingAlert(), view);
				if (chapterDir == -1) chapterPage = -1;
				chapterId = chapterNextId;
				chapterNextId = null;
				start(RUN_CHAPTER_VIEW);
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
			Form f = chaptersForm != null ? chaptersForm : mangaForm != null ? mangaForm :listForm != null ? listForm : mainForm;
			if (c == openCmd) {
				// открыть главу с конкретной страницы
				int n = Integer.parseInt(((TextBox) d).getString());
				if (n < 1) {
					display(f);
					return;
				}
				chapterPage = n;
				
				start(RUN_CHAPTER_VIEW);
				return;
			}
			if (c == goCmd) {
				gotoPage(f, ((TextBox) d).getString());
			}
			display(f);
			return;
		}
		// фм
		if (d == fileList) {
			if (c == backCmd) {
				if (curDir == null) {
					fileList = null;
					display(settingsForm);
				} else {
					if (curDir.indexOf("/") == -1) {
						fileList = new List("", List.IMPLICIT);
						fileList.addCommand(backCmd);
						fileList.setTitle("");
						fileList.addCommand(List.SELECT_COMMAND);
						fileList.setSelectCommand(List.SELECT_COMMAND);
						fileList.setCommandListener(this);
						for(int i = 0; i < rootsList.size(); i++) {
							String s = (String) rootsList.elementAt(i);
							if (s.startsWith("file:///")) s = s.substring(8);
							if (s.endsWith("/")) s = s.substring(0, s.length() - 1);
							fileList.append(s, null);
						}
						curDir = null;
						display(fileList);
						return;
					}
					String sub = curDir.substring(0, curDir.lastIndexOf('/'));
					String fn = "";
					if (sub.indexOf('/') != -1) {
						fn = sub.substring(sub.lastIndexOf('/') + 1);
					} else {
						fn = sub;
					}
					curDir = sub;
					showFileList(sub + "/", fn);
				}
			}
			if (c == dirOpenCmd || c == List.SELECT_COMMAND) {
				String f = curDir != null ? curDir.concat("/") : "";
				String is = fileList.getString(fileList.getSelectedIndex());
				if ("- ".concat(L[Select]).equals(is)) {
					fileList = null;
					// папка скачивания выбрана
					downloadPathField.setString(f.substring(0, f.length() - 1));
					display(settingsForm);
					curDir = null;
					return;
				}
				
				showFileList((curDir = f.concat(is)).concat("/"), is);
				return;
			}
			if (c == dirSelectCmd) {
				fileList = null;
				curDir = null;
				display(settingsForm);
			}
			return;
		}
		if (d == authForm) {
			if (c == backCmd) {
				loginField = passwordField = clientField = clientSecretField = null;
				authForm = null;
				display(mainForm);
				return;
			}
			if (c == authSubmitCmd) {
				if (running) return;
				start(RUN_AUTH);
				return;
			}
		}
		if (c == authCmd) {
			Form f = new Form("");
			f.addCommand(backCmd);
			f.setCommandListener(this);
			
			loginField = new TextField("Login", username != null ? username : "", 100, TextField.NON_PREDICTIVE);
			f.append(loginField);
			
			passwordField = new TextField("Password",
					password != null ? password : "", 100, TextField.NON_PREDICTIVE);
			f.append(passwordField);
			clientField = new TextField("Client ID",
					clientId != null ? clientId : "personal-client-", 100, TextField.NON_PREDICTIVE);
			f.append(clientField);
			
			clientSecretField = new TextField("Client secret",
					clientSecret != null ? clientSecret : "", 100, TextField.NON_PREDICTIVE);
			f.append(clientSecretField);
			
			StringItem s = new StringItem("", "Login", StringItem.BUTTON);
			s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER);
			s.addCommand(authSubmitCmd);
			s.setDefaultCommand(authSubmitCmd);
			s.setItemCommandListener(this);
			f.append(s);
			
			s = new StringItem(null, "\nSee https://api.mangadex.org/docs/02-authentication/personal-clients/");
			f.append(s);
			
			display(authForm = f);
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
			if (accessToken != null) {
				runAfterAuth = RUN_MANGA;
				start(RUN_MANGA);
				return;
			}
			start(RUN_MANGA);
			return;
		}
		if (c == searchCmd || c == updatesCmd || c == recentCmd || c == libraryCmd) {
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
			
			display(listForm = f);
			
			if (c == libraryCmd) {
				listMode = LIST_FOLLOWED;
				runAfterAuth = RUN_MANGAS;
				start(RUN_AUTH);
				return;
			}
			
			listMode = c == searchCmd ? LIST_SEARCH : c == recentCmd ? LIST_RECENT : LIST_UPDATES;
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
				Alert a = new Alert(L[ExternalLink], "This chapter links to external source, open it?", null, AlertType.WARNING);
				a.addCommand(openCmd);
				a.addCommand(backCmd);
				a.setCommandListener(this);
				return;
			}
			
			chapterPage = 1;
			
			start(RUN_CHAPTER_VIEW);
			return;
		}
		if (c == downloadCmd) {
			// скачать главу
			if (running) return;
			if ((chapterId = (String) chapterItems.get(item)) == null)
				return;
			if (chapterId.startsWith("http")) {
				// внешний источник
				display(errorAlert("Can't download, external link!"), chaptersForm);
				return;
			}

			Alert a = new Alert(mangaForm.getTitle(), L[Initializing], null, null);
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
				display(errorAlert("External link!"), chaptersForm);
				return;
			}
			
			TextBox t = new TextBox(L[PageNumber], "", 3, TextField.NUMERIC);
			t.addCommand(openCmd);
			t.addCommand(cancelCmd);
			t.setCommandListener(this);
			display(t);
			return;
		}
		if (c == showCoverCmd) {
			if (running) return;
			if (viewMode == 1) {
				view = new ViewCommon(-2, false);
			} else if (viewMode == 2) {
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
		if (c == advSearchCmd) {
			// форма адвансед поиска
			Form f = new Form(L[AdvancedSearch]);
			f.addCommand(advSubmitCmd);
			f.addCommand(backCmd);
			f.setCommandListener(this);
			
			TextField t;
			ChoiceGroup g;
			StringItem s;
			
			t = new TextField(L[Title], "", 200, TextField.ANY);
			f.append(advTitleField = t);
			
			t = new TextField(L[Year], "", 4, TextField.NUMERIC);
			f.append(advYearField = t);
			
			
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
			
			s = new StringItem(null, "\nTags");
			s.setFont(smallboldfont);
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
			f.append(s);
			
			// теги
			String[] and_or = new String[] { "And", "Or" };
			
			t = new TextField("Include tags", "", 200, TextField.NON_PREDICTIVE);
			f.append(advIncludeField = t);
			
			g = new ChoiceGroup("Inclusion mode", ChoiceGroup.POPUP, and_or, null);
			f.append(advInclusionChoice = g);
			
			t = new TextField("Exclude tags", "", 200, TextField.NON_PREDICTIVE);
			f.append(advExcludeField = t);
			
			g = new ChoiceGroup("Exclusion mode", ChoiceGroup.POPUP, and_or, null);
			g.setSelectedIndex(1, true);
			f.append(advExclusionChoice = g);
			
			s = new StringItem(null, "\nTags list:");
			s.setFont(smallfont);
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
			f.append(s);
			
			String[][] tags;
			StringBuffer sb = new StringBuffer();
			try {
				// Format
				tags = tags("format");
				for (int i = 0; i < tags.length; i++) {
					sb.append(tags[i][1]).append("; ");
				}
				sb.setLength(sb.length() - 2);
				
				s = new StringItem("Format", sb.toString());
				s.setFont(smallfont);
				s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				f.append(s);
				
				// Genre
				sb.setLength(0);
				tags = tags("genre");
				for (int i = 0; i < tags.length; i++) {
					sb.append(tags[i][1]).append(", ");
				}
				sb.setLength(sb.length() - 2);
				
				s = new StringItem("Genre", sb.toString());
				s.setFont(smallfont);
				s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				f.append(s);

				// Theme
				sb.setLength(0);
				tags = tags("theme");
				for (int i = 0; i < tags.length; i++) {
					sb.append(tags[i][1]).append(", ");
				}
				sb.setLength(sb.length() - 2);
				
				s = new StringItem("Theme", sb.toString());
				s.setFont(smallfont);
				s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				f.append(s);

				// Content
				sb.setLength(0);
				tags = tags("content");
				for (int i = 0; i < tags.length; i++) {
					sb.append(tags[i][1]).append(", ");
				}
				sb.setLength(sb.length() - 2);
				
				s = new StringItem("Content", sb.toString());
				s.setFont(smallfont);
				s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				f.append(s);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			// сортир
			g = new ChoiceGroup(L[SortBy], ChoiceGroup.EXCLUSIVE, new String[] {
					L[Default], L[BestMatch], // relevance
					"Latest Upload", "Oldest Upload", // latestUploadedChapter
					"Title Ascending", "Title Descending", // title
					"Highest Rating", "Lowest Rating", // rating
					"Most Follows", "Fewest Follows", // followedCount
					"Recently Added", "Oldest Added", // createdAt
					"Year Ascending", "Year Descending" // year
			}, null);
			f.append(advSortChoice = g);
			
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
		if (c == pathCmd) {
			showFileList(2);
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
		System.out.println("run ".concat(n(run)));
		running = run != RUN_COVERS && run != RUN_PRELOADER;
		switch (run) {
		case RUN_MANGAS: { // поиск и список манг
			boolean temp;
			Form f = (temp = listMode == LIST_RELATED) ? tempListForm : listForm;
			
			if (useLoadingForm && !temp) display(loadingForm);
			
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
				if (listMode != LIST_ADVANCED_SEARCH && listMode != LIST_FOLLOWED) {
					if (contentFilter != null)
					for (int i = 0; i < CONTENT_RATINGS.length; i++) {
						if (!contentFilter[i]) continue;
						sb.append("&contentRating[]=").append(CONTENT_RATINGS[i]);
					}
					
					if (tagsFilter != null && tagsFilter.length() > 0) {
						tagsParam(clearTag(tagsFilter), tags(null), sb, false);
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
					// статус
					advStatusChoice.getSelectedFlags(sel);
					for (int i = 0; i < MANGA_STATUSES.length; i++) {
						if (!sel[i]) continue;
						sb.append("&status[]=").append(MANGA_STATUSES[i]);
					}
					// демография какая-то
					advDemographicChoice.getSelectedFlags(sel);
					for (int i = 0; i < MANGA_DEMOGRAPHIC.length; i++) {
						if (!sel[i]) continue;
						sb.append("&publicationDemographic[]=").append(MANGA_DEMOGRAPHIC[i]);
					}

					// возрастной рейтинг
					advRatingChoice.getSelectedFlags(sel);
					for (int i = 0; i < CONTENT_RATINGS.length && i < advRatingChoice.size(); i++) {
						if (!sel[i]) continue;
						sb.append("&contentRating[]=").append(CONTENT_RATINGS[i]);
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

					// фильтр по тегам
					String[][] tags = null;
					String inc = clearTag(advIncludeField.getString().trim());
					String exc = clearTag(advExcludeField.getString().trim());
					
					if (inc.length() > 0 || exc.length() > 0) {
						tags = tags(null);
					}
					
					if (inc.length() > 0) {
						tagsParam(inc, tags, sb, true);
						sb.append("&includedTagsMode=").append(advInclusionChoice.isSelected(0) ? "and" : "or");
					}
					
					if (inc.length() > 0) {
						tagsParam(inc, tags, sb, false);
						sb.append("&excludedTagsMode=").append(advExclusionChoice.isSelected(0) ? "and" : "or");
					}
					
					break;
				}
				case LIST_RELATED: {
					f.setTitle(L[Related]);
					int l = relatedManga.size();
					for (int i = 0; i < l; i++) {
						sb.append("&ids[]=").append(((JSONObject) relatedManga.elementAt(i)).getString("id"));
					}
					break;
				}
				case LIST_FOLLOWED: {
					f.setTitle(L[Library]);
					sb.insert(0, "user/follows/");
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
				for (int i = 0; i < l && listForm != null; i++) {
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
				if (listForm == f)
					display(f);
			} catch (Exception e) {
				e.printStackTrace();
				if (listForm == f)
					display(errorAlert(e.toString()), f);
			}
			f.setTicker(null);
			break;
		}
		case RUN_MANGA: { // открыта манга
			if (useLoadingForm) display(loadingForm);
			
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
				// фильтровать рандом
				if ("random".equals(id)) {
					if (contentFilter != null)
					for (int i = 0; i < CONTENT_RATINGS.length; i++) {
						if (!contentFilter[i]) continue;
						sb.append("&contentRating[]=").append(CONTENT_RATINGS[i]);
					}

					if (tagsFilter != null && tagsFilter.length() > 0) {
						tagsParam(clearTag(tagsFilter), tags(null), sb, false);
					}
				}
				
				JSONObject j = api(sb.toString()).getObject("data");
				JSONObject attributes = j.getObject("attributes");
				JSONArray relationships = j.getArray("relationships");
				coverItem.setAltText(mangaId = id = j.getString("id"));
				
				mangaFollowed = false;
				
				if (accessToken != null)
				try {
					mangaFollowed = api("user/follows/manga/".concat(id)).getString("result").equals("ok");
				} catch (Exception e) {}
				
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
				
				if (accessToken != null) {
					s = new StringItem(null, L[mangaFollowed ? Unfollow : Follow], StringItem.BUTTON);
					s.setFont(Font.getDefaultFont());
					s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					s.addCommand(followCmd);
					s.addCommand(unfollowCmd);
					s.setDefaultCommand(mangaFollowed ? unfollowCmd : followCmd);
					s.setItemCommandListener(this);
					f.append(followBtn = s);
				}
				
//				s = new StringItem(null, L[Save], StringItem.BUTTON);
//				s.setFont(Font.getDefaultFont());
//				s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
//				s.addCommand(saveCmd);
//				s.setDefaultCommand(saveCmd);
//				s.setItemCommandListener(this);
//				f.append(s);
				
				// если обложка потерялась, поставить ее в очередь
				if (thumb == null || thumb == coverPlaceholder) {
					scheduleCover(coverItem, id);
				}
				if (mangaForm == f)
					display(f);
			} catch (Exception e) {
				e.printStackTrace();
				if (mangaForm == f)
					display(errorAlert(e.toString()), f);
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
										sb.append("&ids[]=").append(cover);
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
						} catch (Exception e) {} 
					}
				}
			} catch (Throwable e) {}
			return;
		}
		case RUN_CHAPTERS: { // главы манги
			if (useLoadingForm) display(loadingForm);
			
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
				
				// фильтр по языкам
				if (chapterLangFilter != null && chapterLangFilter.length() > 0) {
					String[] s = split(chapterLangFilter, ',');
					for (int i = 0; i < s.length; i++) {
						sb.append("&translatedLanguage[]=").append(s[i].trim());
					}
				}
				
				// фильтр по рейтингу
				if (contentFilter != null) {
					for (int i = 0; i < CONTENT_RATINGS.length; i++) {
						if (!contentFilter[i]) continue;
						sb.append("&contentRating[]=").append(CONTENT_RATINGS[i]);
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
				for (int i = 0; i < l && chaptersForm != null; i++) {
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
						} else if ("scanlation_group".equals(type)) {
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
				if (chaptersForm == f)
					display(f);
			} catch (Exception e) {
				e.printStackTrace();
				if (chaptersForm == f)
					display(errorAlert(e.toString()), f);
			}
			f.setTicker(null);
			break;
		}
		case RUN_CHAPTER_VIEW: { // просмотр главы
			String id = chapterId;
			if (mangaId == null || id == null) break;
			
			Form f = chaptersForm != null ? chaptersForm : mainForm;
			
			display(loadingAlert(), f);
			try {
				loadChapterInfo(id);
				
				int n = chapterPage;
				chapterPages = chapterFilenames.size();
				if (n == -1) { // последняя страницы
					n = chapterPages - 1;
				} else {
					n = Math.min(n, chapterPages) - 1;
				}
				if (viewMode == 1) {
					view = new ViewCommon(n, false);
				} else if (viewMode == 2) {
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
		case RUN_DOWNLOAD_CHAPTER: { // скачать главу
			Form f = chaptersForm;
			
			if (downloadPath == null || downloadPath.trim().length() == 0) {
				display(errorAlert("No download path set!"), f);
				break;
			}
			
			if (chapterFilenames == null) {
				downloadAlert.setString(L[Fetching]);
				try {
					loadChapterInfo(chapterId);
				} catch (Exception e) {
					display(errorAlert(e.toString()), f);
					downloadIndicator = null;
					downloadAlert = null;
					break;
				}
			}
			
			int l = chapterFilenames.size();
			
			downloadIndicator.setMaxValue(l * 2 + 2);
			
			FileConnection fc = null;
			HttpConnection hc;
			InputStream in;
			OutputStream out;
			String n, tn, folder = null;
			try {
				// создание папок
				fc = (FileConnection) Connector.open(folder = "file:///".concat(downloadPath).concat("/"));
				try {
					fc.mkdir();
				} catch (IOException e) {
				} finally {
					fc.close();
				}

				fc = (FileConnection) Connector.open(folder = folder
						.concat(safeFileName(mangaForm.getTitle(), mangaId).concat("/")));
				try {
					fc.mkdir();
				} catch (IOException e) {
				} finally {
					fc.close();
				}

				int i;
				String v = chapterVolume;
				if (v != null) {
					while (v.substring(0, (i = v.indexOf('.')) != -1 ? i : v.length()).length() < 2)
						v = "0".concat(v);
				}
				
				String c = chapterNum;
				if (c != null) {
					while (c.substring(0, (i = c.indexOf('.')) != -1 ? i : c.length()).length() < 3)
						c = "0".concat(c);
				}
				
				fc = (FileConnection) Connector.open(folder = folder
						.concat((v != null ? "Vol. " + v : "") + (c != null ? "Ch. " + c : "Oneshot") + " " + chapterLang + "/"));
				try {
					fc.mkdir();
				} catch (IOException e) {
				} finally {
					fc.close();
				}
				
				Thread.sleep(100);
				
				for (i = 0; i < l && downloadIndicator != null; i++) {
					if (downloadAlert != null)
						downloadAlert.setString(L[Preparing] + " (" + (i+1) + "/" + (l) + ")");
					downloadIndicator.setValue(i * 2 + 1);
					n = (String) chapterFilenames.elementAt(i);
					tn = Integer.toString(i + 1).concat(".jpg");
					while (tn.length() < 7) tn = "0".concat(tn);
					fc = (FileConnection) Connector.open(folder.concat(tn));
					try {
						if (!fc.exists()) fc.create();
						Thread.sleep(100);
						hc = open(proxyUrl(chapterBaseUrl + "/data-saver/" + chapterHash + '/' + n));
						try {
							if (hc.getResponseCode() != 200) {
								throw new IOException("Bad response");
							}
							long fl = hc.getLength();
							if (fl > 0) {
								long al = fc.availableSize();
								if (al < (fl * 2) || al < 1024 * 1024) {
									fc.delete();
									throw new Exception("Out of memory space");
								}
							}
							in = hc.openInputStream();
							if (downloadAlert != null)
								downloadAlert.setString(L[Downloading] + " (" + (i+1) + "/" + (l) + ")");
							downloadIndicator.setValue(i * 2 + 2);
							try {
								out = fc.openDataOutputStream();
								try {
									int r;
									byte[] buf = new byte[symbianJrt ? 64 * 1024 : 32 * 1024];
									while ((r = in.read(buf)) != -1) {
										out.write(buf, 0, r);
										out.flush();
									}
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
				e.printStackTrace();
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
		case RUN_CHANGE_CHAPTER: { // смена главы во время просмотра
			int found = 0;
			try {
				String curCh = chapterNum;
				if (curCh == null)
					curCh = "0";
				curChA = curCh.indexOf('.');
				curChB = 0;
				if (curChA == -1) {
					curChA = Integer.parseInt(curCh);
				} else {
					curChB = Integer.parseInt(curCh.substring(curChA + 1));
					curChA = Integer.parseInt(curCh.substring(0, curChA));
				}
				
				s: {
					StringBuffer sb = new StringBuffer("manga/").append(mangaId)
							.append("/aggregate?");
					int ol = sb.length();

					sb.append("translatedLanguage[]=").append(chapterLang);
					if (chapterGroup != null)
						sb.append("&groups[]=").append(chapterGroup);

					if ((found = searchNextChapter(sb.toString(), 1)) > 0)
						break s;
					
					if (chapterGroup != null) {
						sb.setLength(ol);
						sb.append("translatedLanguage[]=").append(chapterLang);
						if ((found = searchNextChapter(sb.toString(), 2)) > 0)
							break s;
					}

//					sb.setLength(ol);
//					if ((found = searchNextChapter(sb.toString(), 3)) > 0)
//						break s;
				}
				if (found == 0) {
//					display(errorAlert("Next chapter not found!"), view);
//					start(RUN_DISPOSE_VIEW);
					view = null;
					chapterFilenames = null;
					display(chaptersForm != null ? chaptersForm : mangaForm);
				} else if (found > 1) {
					Alert a;
//					if (found == 2 || found == 4) {
						a = new Alert(L[ChapterGap],
								replaceOnce(L[ThereIsGap], "%", chapterNum + " -> " + chapterNextNum),
								null, AlertType.WARNING);
//					} else {
//						a = new Alert("", "Language change, continue?", null, AlertType.WARNING);
//					}
					a.addCommand(cancelCmd);
					a.addCommand(continueCmd);
					a.setCommandListener(this);
					display(a, view);
				} else {
//					start(RUN_DISPOSE_VIEW);
					chapterId = chapterNextId;
					if (chapterDir == -1) chapterPage = -1;
					MangaApp.run = RUN_CHAPTER_VIEW;
					run();
					return;
				}
			} catch (Exception e) {
				e.printStackTrace();
				display(errorAlert(e.toString()), view);
			}
			break;
		}
//		case RUN_DISPOSE_VIEW: {
			// тихо очистить кэш главы
//			if (!chapterFileCache) break;
//			
//			FileConnection fc = null;
//			try {
//				String folder = getFolderName();
//				fc = (FileConnection) Connector.open(folder);
//				Vector list = new Vector();
//				Enumeration e = fc.list();
//				while (e.hasMoreElements()) list.addElement(e.nextElement());
//				
//				int l = list.size();
//				for (int i = 0; i < l; i++) {
//					fc.setFileConnection((i == 0 ? "./" : "../").concat((String) list.elementAt(i)));
//					fc.delete();
//				}
//				fc.delete();
//			} catch (Exception ignored) {
//			} finally {
//				if (fc != null) try {
//					fc.close();
//				} catch (Exception e) {}
//			}
//			break;
//		}
		case RUN_AUTH: { // авторизация
			auth: {
				Displayable f = display.getCurrent();
				
				try {
					// проверка времени жизни токенов
					long now = System.currentTimeMillis();
					if (now - accessTokenTime > 900 * 1000L)
						accessToken = null;
					if (now - refreshTokenTime > 7776000 * 1000L)
						refreshToken = null;
					
					if (clientField != null) {
						clientId = clientField.getString();
						clientSecret = clientSecretField.getString();
					}
					
					StringBuffer p = new StringBuffer("client_id=")
							.append(url(clientId))
							.append("&client_secret=")
							.append(url(clientSecret))
							;
					
					if (loginField != null && loginField.getString().trim().length() > 0) {
						// первый логин или перелогин
						p.append("&grant_type=password&username=")
						.append(url(username = loginField.getString()))
						.append("&password=")
						.append(url(password = passwordField.getString()))
						;
					} else if(refreshToken != null && accessToken == null) {
						// refresh
						p.append("&grant_type=refresh_token&refresh_token=")
						.append(url(refreshToken))
						;
					} else if(accessToken == null && username != null && password != null) {
						// рефреш токен умер, перелогиниваемся
						p.append("&grant_type=password&username=")
						.append(url(username))
						.append("&password=")
						.append(url(password))
						;
					} else break auth;
					
					Alert a = loadingAlert();
					a.setString(L[Authorizing]);
					display(a, f);
					
					byte[] data = p.toString().getBytes();
					
					HttpConnection h = (HttpConnection) open(proxyUrl(AUTHURL));
					try {
						h.setRequestMethod("POST");
						h.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
						h.setRequestProperty("Content-length", Integer.toString(data.length));
						OutputStream out = h.openOutputStream();
						out.write(data);
						out.flush();
						out.close();
						InputStream in = h.openInputStream();
						try {
							JSONObject j = JSONObject.parseObject(new String(readBytes(in, 0, 2048, 2048)));
							if (j.has("access_token")) {
								accessToken = j.getString("access_token");
								accessTokenTime = System.currentTimeMillis();
							}
							if (j.has("refresh_token")) {
								refreshToken = j.getString("refresh_token");
								refreshTokenTime = System.currentTimeMillis();
							}
							if (j.has("error")) {
								throw new Exception("Auth error: ".concat(j.getString("error")));
							}
						} finally {
							in.close();
						}
					} finally {
						h.close();
					}
				} catch (Exception e) {
					e.printStackTrace();
					display(errorAlert(e.toString()), f);
					writeAuth();
					break;
				}
				writeAuth();
				display(f);
			}
			// задача которая должна быть выполнена после проверки авторизации
			if (runAfterAuth != 0) {
				MangaApp.run = runAfterAuth;
				runAfterAuth = 0;
				run();
				return;
			}
			break;
		}
		case RUN_FOLLOW: { // добавить/убрать из библиотеки
			Form f = mangaForm;
			try {
				// подменит метод прокси, потому что в ж2ме нельзя
				api("manga/".concat(mangaId).concat("/follow;_method=").concat(mangaFollowed ? "DELETE" : "POST"));
				followBtn.setText(L[(mangaFollowed = !mangaFollowed) ? Unfollow : Follow]);
			} catch (Exception e) {
				e.printStackTrace();
			}
			f.setTicker(null);
			break;
		}
		}
		running = false;
	}

	// запустить задачу
	Thread start(int i) {
		Thread t = null;
		try {
			synchronized(this) {
				run = i;
				(t = new Thread(this)).start();
				wait();
			}
		} catch (Exception e) {}
		return t;
	}

	// получение инфы о главе для скачивания или просмотра
	private void loadChapterInfo(String id) throws IOException {
		JSONObject j;
		
		try {
			j = api("chapter?ids[]=" + id).getArray("data").getObject(0);
			
			JSONObject att = j.getObject("attributes");
			chapterVolume = att.getString("volume");
			chapterNum = att.getString("chapter");
			chapterLang = att.getString("translatedLanguage");
			
			JSONArray relations = j.getArray("relationships");
			int l = relations.size();
			for (int i = 0; i < l; i++) {
				JSONObject r = relations.getObject(i);
				if (!"scanlation_group".equals(j.getString("type"))) continue;
				chapterGroup = r.getString("id");
			}
		} catch (Exception e) {}
		
		// получение ссылок на страницы https://api.mangadex.org/docs/04-chapter/retrieving-chapter/
		j = api("at-home/server/" + id);
		chapterBaseUrl = j.getString("baseUrl");
		chapterFilenames = new Vector();
		
		j = j.getObject("chapter");
		chapterHash = j.getString("hash");
		
		JSONArray data;
		// жпег, если нет то пнг
		if (dataSaver && j.has("dataSaver")) data = j.getArray("dataSaver");
		else data = j.getArray("data");
		
		int l = data.size();
		for (int i = 0; i < l; i++) {
			String n = data.getString(i);
			chapterFilenames.addElement(n);
		}
	}
	
	// запись состояния авторизации
	private void writeAuth() {
		try {
			RecordStore.deleteRecordStore(AUTH_RECORDNAME);
		} catch (Exception e) {}
		try {
			JSONObject j = new JSONObject();
			
			j.put("accessToken", accessToken);
			j.put("refreshToken", refreshToken);
			j.put("clientId", clientId);
			j.put("clientSecret", clientSecret);
			j.put("username", username);
			j.put("password", password);
			j.put("accessTime", accessTokenTime);
			j.put("refreshTime", refreshTokenTime);
			
			byte[] b = j.toString().getBytes("UTF-8");
			RecordStore r = RecordStore.openRecordStore(AUTH_RECORDNAME, true);
			r.addRecord(b, 0, b.length);
			r.closeRecordStore();
		} catch (Exception e) {}
	}

	private void tagsParam(String c, String[][] tags, StringBuffer sb, boolean inc) {
		String[] s = split(c, ',');
		String n;
		for (int i = 0; i < s.length; i++) {
			n = s[i].trim();
			for (int k = 0; k < tags.length; k++) {
				if (!tags[k][1].equalsIgnoreCase(n)) continue;
				sb.append(inc ? "&includedTags[]=" : "&excludedTags[]=").append(tags[k][0]);
				break;
			}
		}
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
			chaptersOffset = Math.max(0,
					Math.min(Math.min(page - 1, chaptersTotal / chaptersLimit) * chaptersLimit, chaptersTotal));
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
				throw new IOException("HTTP ".concat(Integer.toString(r)));
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
		if (j.has("ja")) return j.getString("ja");
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
		Displayable p = display.getCurrent();
		display.setCurrent(d);
		if (!keepListCovers && p != null && (p == listForm || p == tempListForm)) {
			try {
				// обнуление обложек
				int l = ((Form) p).size();
				for (int i = 0; i < l; i++) {
					Item item = ((Form) p).get(i);
					if (!(item instanceof ImageItem)) continue;
					((ImageItem) item).setImage(coverPlaceholder);
				}
			} catch (Exception e) {}
		}
		if (d == chaptersForm || d == mangaForm) {
//			midlet.start(RUN_DISPOSE_VIEW);
			view = null;
			chapterFilenames = null;
			return;
		}
		if (coverLoading == 3 || p == mainForm || p == loadingForm || p == d) return;
		if (d == listForm/*|| d == tempListForm*/) {
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
		a.setTimeout(30000);
		return a;
	}
	
	private static void showFileList(String f, String title) {
		fileList = new List(title, List.IMPLICIT);
		fileList.setTitle(title);
		fileList.addCommand(backCmd);
		fileList.addCommand(List.SELECT_COMMAND);
		fileList.setSelectCommand(List.SELECT_COMMAND);
		fileList.setCommandListener(midlet);
		fileList.addCommand(dirSelectCmd);
		fileList.append("- ".concat(L[Select]), null);
		try {
			FileConnection fc = (FileConnection) Connector.open("file:///" + f);
			Enumeration list = fc.list();
			while(list.hasMoreElements()) {
				String s = (String) list.nextElement();
				if(s.endsWith("/")) {
					fileList.append(s.substring(0, s.length() - 1), null);
				}
			}
			fc.close();
		} catch (Exception e) {
		}
		display(fileList);
	}
	
	private static void showFileList(int mode) {
		fileList = new List("", List.IMPLICIT);
		
		if(rootsList == null) {
			rootsList = new Vector();
			try {
				Enumeration roots = FileSystemRegistry.listRoots();
				while(roots.hasMoreElements()) {
					String s = (String) roots.nextElement();
					if(s.startsWith("file:///")) s = s.substring(8);
					rootsList.addElement(s);
				}
			} catch (Exception e) {}
		}
		
		for(int i = 0; i < rootsList.size(); i++) {
			String s = (String) rootsList.elementAt(i);
			if(s.startsWith("file:///")) s = s.substring(8);
			if(s.endsWith("/")) s = s.substring(0, s.length() - 1);
			fileList.append(s, null);
		}
		fileList.addCommand(List.SELECT_COMMAND);
		fileList.setSelectCommand(List.SELECT_COMMAND);
		fileList.addCommand(backCmd);
		fileList.setCommandListener(midlet);
		display(fileList);
	}
	
	// возвращает массив массивов из [ид,название]
	private String[][] tags(String group) throws IOException {
		JSONStream s = JSONStream.getStream(getClass().getResourceAsStream("tags"));
		try {
			if (group == null) {
				// все сразу
				if (tags != null)
					return tags;
				
				JSONObject j = s.nextObject();
				JSONArray f = j.getArray("format"),
						g = j.getArray("genre"),
						t = j.getArray("theme"),
						m = j.getArray("content");
				String[][] res = new String[f.size() + g.size() + t.size() + m.size()][2];
				int k = 0;
				int l = f.size();
				for (int i = 0; i < l; i++) {
					JSONArray b = f.getArray(i);
					res[k][0] = b.getString(0);
					res[k++][1] = clearTag(b.getString(1));
				}
				l = g.size();
				for (int i = 0; i < l; i++) {
					JSONArray b = g.getArray(i);
					res[k][0] = b.getString(0);
					res[k++][1] = clearTag(b.getString(1));
				}
				l = t.size();
				for (int i = 0; i < l; i++) {
					JSONArray b = t.getArray(i);
					res[k][0] = b.getString(0);
					res[k++][1] = clearTag(b.getString(1));
				}
				l = m.size();
				for (int i = 0; i < l; i++) {
					JSONArray b = m.getArray(i);
					res[k][0] = b.getString(0);
					res[k++][1] = clearTag(b.getString(1));
				}
				return tags = res;
			}
			s.expectNextTrim('{');
			if (!s.jumpToKey(group))
				return null;
			JSONArray a = s.nextArray();
			int l = a.size();
			String[][] res = new String[l][2];
			for (int i = 0; i < l; i++) {
				JSONArray b = a.getArray(i);
				res[i][0] = b.getString(0);
				res[i][1] = b.getString(1);
			}
			return res;
		} catch (Exception e) {
		} finally {
			s.close();
		}
		return null;
	}
	
	// view

	private int lastA, curChA, curChB;
	
	// 1 - perfect match, 2 - big gap, 3 - different language, 4 - both
	private int searchNextChapter(String url, int type) throws IOException {
		JSONObject volumes = api(url).getObject("volumes");
		
		boolean dir = chapterDir == 1;
		String curCh = chapterNum;
		if (!dir && curCh == null)
			return 0;
		if (curCh == null)
			curCh = "none";
		
		String curVolume = chapterVolume;
		if (curVolume == null)
			curVolume = "none";

		String lastCh = null;
		JSONObject vol = volumes.getNullableObject(curVolume);
		if (vol == null || (lastCh = getNextChapter(vol, curCh, dir)) == null) {
			if (dir) {
				int n = chapterVolume == null ? 1 : Integer.parseInt(curVolume) + 1;
				if ((vol = volumes.getNullableObject(Integer.toString(n))) != null) {
					lastCh = getNextChapter(vol, curCh, true);
				} else if ((vol = volumes.getNullableObject("none")) != null) {
					lastCh = getNextChapter(vol, curCh, true);
					n = 0;
				}
				if (lastCh == null && n > 0 && (vol = volumes.getNullableObject("none")) != null) {
					lastCh = getNextChapter(vol, curCh, true);
					n = 0;
				}
			} else {
				int n;
				if (chapterVolume == null) {
					Enumeration keys = volumes.keys();
					n = -1;
					while (keys.hasMoreElements()) {
						int t = Integer.parseInt((String) keys.nextElement());
						if (t > n) n = t;
					}
					if (n != -1)
						vol = volumes.getNullableObject(Integer.toString(n));
				} else {
					n = Integer.parseInt(curVolume) - 1;
					vol = volumes.getNullableObject(n < 1 ? "none" : Integer.toString(n));
				}
				if (vol != null) {
					lastCh = getNextChapter(vol, curCh, false);
				}
				if (lastCh == null && n > 0 && (vol = volumes.getNullableObject("none")) != null) {
					lastCh = getNextChapter(vol, curCh, false);
					n = 0;
				}
			}
		}
		if (lastCh != null) {
			chapterNextNum = lastCh;
			if (Math.abs(lastA - curChA) > 1)
				return type == 3 ? 4 : 2;
			return type == 3 ? 3 : 1;
		}
		return 0;
	}

	private String getNextChapter(JSONObject vol, String curCh, boolean dir) {
		vol = vol.getObject("chapters");
		Enumeration keys = vol.keys();

		String lastCh = null;
		String lastChId = null;
		int curChA = this.curChA,
				lastA = curChA,
				lastB = curChB;
		while (keys.hasMoreElements()) {
			String ch = (String) keys.nextElement();
			if (ch.equals(curCh)) continue;
			String id = vol.getObject(ch).getString("id");
			int chA, chB = 0;
			if ("none".equals(ch)) {
				ch = "0";
				chA = 0;
			} else {
				chA = ch.indexOf('.');
				if (chA == -1) {
					chA = Integer.parseInt(ch);
				} else {
					chB = Integer.parseInt(ch.substring(chA + 1));
					chA = Integer.parseInt(ch.substring(0, chA));
				}
			}
			
			if (dir) {
				if ((chA == curChA && chB > curChB && (lastA != chA || chB > lastB)) ||
						(chA == lastA && chA != curChA && chB < lastB) ||
						(chA > curChA && (lastCh == null || (chA < lastA && lastA != curChA)))
						) {
					lastA = chA;
					lastB = chB;
					lastCh = ch;
					lastChId = id;
					continue;
				}
			} else {
				if ((chA == curChA && chB < curChB && (lastA != chA || chB < lastB)) ||
						(chA < curChA && (lastCh == null || (chA > lastA && lastA != curChA))) ||
						(chA == lastA && chA != curChA && chB > lastB)
						) {
					lastA = chA;
					lastB = chB;
					lastCh = ch;
					lastChId = id;
					continue;
				}
			}
		}
		this.lastA = lastA;
		chapterNextId = lastChId;
		return lastCh;
	}

	static void changeChapter(int d) {
		if (running) return;
		chapterDir = d;
		display(loadingAlert(), view);
		
		midlet.start(RUN_CHANGE_CHAPTER);
	}

	/**
	 * Caches an image.
	 * 
	 * @param a Data to write.
	 * @param i Number of the image, [1; pages].
	 */
	public static void cachePage(byte[] a, int i) { // from njtai
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

	public static byte[] readCachedPage(int i) { // from njtai too
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
			if (fc != null)
				try {
					fc.close();
				} catch (IOException e) {}
		}
		try {
			fc = (FileConnection) Connector.open(folder = folder.concat(mangaId).concat("/"));
			fc.mkdir();
		} catch (IOException e) {
		} finally {
			if (fc != null)
				try {
					fc.close();
				} catch (IOException e) {}
		}
		try {
			fc = (FileConnection) Connector.open(folder = folder.concat(chapterId).concat("/"));
			fc.mkdir();
		} catch (IOException e) {
		} finally {
			if (fc != null)
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

	public static byte[] getPage(int n, String t) throws IOException {
		return get(proxyUrl(chapterBaseUrl + (dataSaver ? "/data-saver/" : "/data/") +
				chapterHash + '/' + chapterFilenames.elementAt(n) + t));
	}

	public static byte[] getCover(String t) throws Exception {
		if (t == null) t = ".512.jpg";
		else t = ".512.jpg".concat(t);
		return get(proxyUrl(COVERSURL + mangaId + '/' +
				getCover((String) mangaCoversCache.get(mangaId), false) + t));
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
			if (count + readLen > buf.length) {
				byte[] newbuf = new byte[count + expandSize];
				System.arraycopy(buf, 0, newbuf, 0, count);
				buf = newbuf;
			}
			System.arraycopy(readBuf, 0, buf, count, readLen);
			count += readLen;
		}
		if (buf.length == count) {
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
			if ((r = hc.getResponseCode()) >= 400) {
				throw new IOException("HTTP ".concat(Integer.toString(r)));
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
			if ((i = hc.getResponseCode()) >= 400) {
				throw new IOException("HTTP ".concat(Integer.toString(i)));
			}
			String r;
			while(i >= 300) {
				if (++k > 3) {
					throw new IOException("Too many redirects!");
				}
				if ((r = hc.getHeaderField("Location")).startsWith("/")) {
					r = url.substring(0, (j = url.indexOf("//") + 2)) + url.substring(j, url.indexOf("/", j)) + r;
				}
				hc.close();
				hc = open(r);
				if ((i = hc.getResponseCode()) >= 400) {
					throw new IOException("HTTP ".concat(Integer.toString(i)));
				}
			}
			in = hc.openInputStream();
			byte[] buf = new byte[(i = (int) hc.getLength()) <= 0 ? 1024 : i];
			i = 0;
			while((j = in.read(buf, i, buf.length - i)) != -1) {
				if ((i += j) == buf.length) {
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
		if (accessToken != null && url.indexOf("api.mangadex.org") != -1) {
			hc.setRequestProperty("Authorization", "Bearer ".concat(accessToken));
		}
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
		long d = (now - t) / 1000L;
		boolean ru = "ru".equals(lang);
		
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
		
		if (d < 30 * 24 * 60 * 60) {
			d /= 24 * 60 * 60L;
			if (d == 1 || (ru && d % 10 == 1 && d % 100 != 11))
				return Integer.toString((int) d).concat(L[DayAgo]);
			if (ru && (d % 10 > 4 || d % 10 < 2))
				return Integer.toString((int) d).concat(L[DaysAgo2]);
			return Integer.toString((int) d).concat(L[DaysAgo]);
		}
		
		if (d < 365 * 24 * 60 * 60) {
			d /= 30 * 24 * 60 * 60L;
			if (d == 1)
				return Integer.toString((int) d).concat(L[MonthAgo]);
			if (ru && (d % 10 > 4 || d % 10 < 2))
				return Integer.toString((int) d).concat(L[MonthsAgo2]);
			return Integer.toString((int) d).concat(L[MonthsAgo]);
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
		if (date.indexOf('T') != -1) {
			String[] dateSplit = split(date.substring(0, date.indexOf('T')), '-');
			String[] timeSplit = split(date.substring(date.indexOf('T')+1), ':');
			String second = split(timeSplit[2], '.')[0];
			int i = second.indexOf('+');
			if (i == -1) {
				i = second.indexOf('-');
			}
			if (i != -1) {
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
		if (i == -1)
			i = date.lastIndexOf('-');
		if (i == -1)
			return null;
		return date.substring(i);
	}

	// получение оффсета таймзоны даты в миллисекундах
	static int parseTimeZone(String date) {
		int i = date.lastIndexOf('+');
		boolean m = false;
		if (i == -1) {
			i = date.lastIndexOf('-');
			m = true;
		}
		if (i == -1)
			return 0;
		date = date.substring(i + 1);
		int offset = date.lastIndexOf(':');
		offset = (Integer.parseInt(date.substring(0, offset)) * 3600000) +
				(Integer.parseInt(date.substring(offset + 1)) * 60000);
		return m ? -offset : offset;
	}
	
	static String n(int n) {
		if (n < 10) {
			return "0".concat(Integer.toString(n));
		} else return Integer.toString(n);
	}
	
	static String[] split(String str, char d) {
		int i = str.indexOf(d);
		if (i == -1)
			return new String[] {str};
		Vector v = new Vector();
		v.addElement(str.substring(0, i));
		while(i != -1) {
			str = str.substring(i + 1);
			if ((i = str.indexOf(d)) != -1)
				v.addElement(str.substring(0, i));
			i = str.indexOf(d);
		}
		v.addElement(str);
		String[] r = new String[v.size()];
		v.copyInto(r);
		return r;
	}
	
	private static String replaceOnce(String str, String hay, String ned) {
		int idx = str.indexOf(hay);
		if (idx != -1) {
			str = str.substring(0, idx) + ned + str.substring(idx+hay.length());
		}
		return str;
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

		s = t.toString().trim();
		if (s.length() == 0)
			return alt;
		return s;
	}

	
	private static String clearTag(String s) {
		StringBuffer t = new StringBuffer();
		int l = s.length();
		for (int i = 0; i < l; i++) {
			char c = s.charAt(i);
			if (c != ' ' && c != '\'' && c != '-') {
				t.append(c);
			}
		}

		return t.toString().toLowerCase().trim();
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
