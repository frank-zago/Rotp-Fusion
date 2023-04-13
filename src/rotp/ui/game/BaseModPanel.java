/*
 * Copyright 2015-2020 Ray Fowler
 * 
 * Licensed under the GNU General Public License, Version 3 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *	   https://www.gnu.org/licenses/gpl-3.0.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rotp.ui.game;

import static rotp.model.game.MOO1GameOptions.loadAndUpdateFromFileName;
import static rotp.model.game.MOO1GameOptions.setBaseAndModSettingsToDefault;
import static rotp.model.game.MOO1GameOptions.updateOptionsAndSaveToFileName;
import static rotp.ui.UserPreferences.ALL_GUI_ID;
import static rotp.ui.UserPreferences.GAME_OPTIONS_FILE;
import static rotp.ui.UserPreferences.LAST_OPTIONS_FILE;
import static rotp.ui.UserPreferences.LIVE_OPTIONS_FILE;
import static rotp.ui.UserPreferences.USER_OPTIONS_FILE;
import static rotp.ui.util.InterfaceParam.LABEL_DESCRIPTION;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.LinkedList;

import rotp.model.game.MOO1GameOptions;
import rotp.ui.BasePanel;
import rotp.ui.BaseText;
import rotp.ui.RotPUI;
import rotp.ui.UserPreferences;
import rotp.ui.game.HelpUI.HelpSpec;
import rotp.ui.util.InterfaceParam;
import rotp.util.LabelManager;
import rotp.util.ModifierKeysState;

public abstract class BaseModPanel extends BasePanel
		implements MouseListener, MouseMotionListener {
 
	private static final String setGlobalDefaultKey	= "SETTINGS_GLOBAL_DEFAULT";
	private static final String setLocalDefaultKey	= "SETTINGS_LOCAL_DEFAULT";
	private static final String setGlobalGameKey	= "SETTINGS_GLOBAL_LAST_GAME";
	private static final String setLocalGameKey		= "SETTINGS_LOCAL_LAST_GAME";
	private static final String setGlobalLastKey	= "SETTINGS_GLOBAL_LAST_SET";
	private static final String setLocalLastKey		= "SETTINGS_LOCAL_LAST_SET";
	private static final String setGlobalUserKey	= "SETTINGS_GLOBAL_USER_SET";
	private static final String setLocalUserKey		= "SETTINGS_LOCAL_USER_SET";
	private static final String saveGlobalUserKey	= "SETTINGS_GLOBAL_USER_SAVE";
	private static final String saveLocalUserKey	= "SETTINGS_LOCAL_USER_SAVE";
	private static final String restoreGlobalKey	= "SETTINGS_GLOBAL_RESTORE";
	private static final String restoreLocalKey		= "SETTINGS_LOCAL_RESTORE";
	private static final String guideKey			= "SETTINGS_GUIDE";
	private static final String exitKey		 		= "SETTINGS_EXIT";

	private	  static int	 exitButtonWidth, guideButtonWidth,
							 userButtonWidth, defaultButtonWidth, lastButtonWidth;
	protected static int	 w, h;
	protected static int	 smallButtonMargin;
	protected static int	 smallButtonH;
	public	  static boolean autoGuide	= false; // To disable automated Guide
	public	  static HelpUI  guideUI;

	private final LinkedList<PolyBox>	polyBoxList	= new LinkedList<>();
	private final LinkedList<Box>		boxBaseList	= new LinkedList<>();
	private final LinkedList<Box>		boxHelpList	= new LinkedList<>();
	protected Box	  hoverBox;
	protected PolyBox hoverPolyBox;
	protected Shape	  prevHover;

	LinkedList<InterfaceParam> paramList;
	LinkedList<InterfaceParam> duplicateList;
	LinkedList<InterfaceParam> activeList;
	

	//	protected Font smallButtonFont	= FontManager.current().narrowFont(20);
	protected Font smallButtonFont	= narrowFont(20);
//	protected Box defaultBox		= new Box("MOD_HELP_BUTTON_DEFAULT");
//	protected Box lastBox			= new Box("MOD_HELP_BUTTON_LAST");
//	protected Box userBox			= new Box("MOD_HELP_BUTTON_USER");
	protected Box defaultBox		= new Box(UserPreferences.defaultButtonHelp);
	protected Box lastBox			= new Box(UserPreferences.lastButtonHelp);
	protected Box userBox			= new Box(UserPreferences.userButtonHelp);
	protected Box guideBox			= new Box("SETTINGS_GUIDE_DESC");

	protected boolean globalOptions	= false; // No preferred button and Saved to remnant.cfg

	protected BaseModPanel () {	}
	
	private void localInit(Graphics2D g) {
		Font prevFont = g.getFont();
		g.setFont(smallButtonFont);

		initExitButtonWidth(g);
		initGuideButtonWidth(g);
		initUserButtonWidth(g);
		initDefaultButtonWidth(g);
		initLastButtonWidth(g);

		g.setFont(prevFont);
	}
	private int stringWidth(Graphics2D g, String key) {
		return g.getFontMetrics().stringWidth(LabelManager.current().label(key));
	}
	private int buttonWidth(Graphics2D g, String[] keys) {
		int result = 0;
		for (String key : keys)
			result = max(result, stringWidth(g, key));
		return smallButtonMargin + result;
	}
	
	protected abstract String GUI_ID();
	protected void refreshGui() {}
	protected MOO1GameOptions guiOptions() { return RotPUI.mergedGuiOptions(); }

	protected boolean guiCallFromGame() { return RotPUI.guiCallFromGame(); }
	@Override public void repaintButtons() { repaint(); }
	protected void init() {
		ModifierKeysState.reset();
		w = RotPUI.setupRaceUI().getWidth();
		h = RotPUI.setupRaceUI().getHeight();
		smallButtonMargin = s30;
		smallButtonH	  = s30;
	}
	protected void close() { 
		ModifierKeysState.reset();
		guideUI = null;
		disableGlassPane();
	}

	// ---------- Exit Button
	protected String exitButtonKey() { return exitKey;}
	private void initExitButtonWidth(Graphics2D g) {
		exitButtonWidth = buttonWidth(g, new String[] {exitKey});
	}
	protected int exitButtonWidth(Graphics2D g) {
		if (exitButtonWidth == 0)
			localInit(g);
		return exitButtonWidth;
	}
	protected void doExitBoxAction() {
		buttonClick();
		switch (ModifierKeysState.get()) {
		case CTRL:
		case CTRL_SHIFT: // Restore
			// loadAndUpdateFromFileName(guiOptions(), LIVE_OPTIONS_FILE, ALL_GUI_ID);
			// break;
		default: // Save
			updateOptionsAndSaveToFileName(guiOptions(), LIVE_OPTIONS_FILE, ALL_GUI_ID);
			break; 
		}
		close();
	}
	protected String exitButtonDescKey() {
		return exitButtonKey() + LABEL_DESCRIPTION;
	}

	// ---------- Guide Button
	protected String guideButtonKey() { return guideKey; }
	private void initGuideButtonWidth(Graphics2D g) {
		guideButtonWidth = buttonWidth(g, new String[] {guideKey});
	}
	protected int guideButtonWidth(Graphics2D g) {
		if (guideButtonWidth == 0) 
			localInit(g);
		return guideButtonWidth;
	}
	protected void doGuideBoxAction() {
		buttonClick();
		autoGuide = !autoGuide;
		if (autoGuide)
			loadGuide();
		else
			clearGuide();
		paintComponent(getGraphics());
	}	
	protected String guideButtonDescKey() {
		return guideButtonKey() + LABEL_DESCRIPTION;
	}

	// ---------- User Button
	protected String userButtonKey() {
		switch (ModifierKeysState.get()) {
		case CTRL:		 return saveGlobalUserKey;
		case CTRL_SHIFT: return saveLocalUserKey;
		case SHIFT:		 return setLocalUserKey;
		default:		 return setGlobalUserKey;
		}
	}
	private void initUserButtonWidth(Graphics2D g) {
		userButtonWidth = buttonWidth(g, new String[] {
				saveGlobalUserKey, saveLocalUserKey, setLocalUserKey, setGlobalUserKey});
	}
	protected int userButtonWidth(Graphics2D g) {
		if (userButtonWidth == 0) 
			localInit(g);
		return userButtonWidth;
	}
	protected void doUserBoxAction() {
		buttonClick();
		switch (ModifierKeysState.get()) {
		case CTRL: // saveGlobalUserKey
			updateOptionsAndSaveToFileName(guiOptions(), USER_OPTIONS_FILE, ALL_GUI_ID);
			return;
		case CTRL_SHIFT: // saveLocalUserKey
			updateOptionsAndSaveToFileName(guiOptions(), USER_OPTIONS_FILE, GUI_ID());
			return;
		case SHIFT: // setLocalUserKey
			loadAndUpdateFromFileName(guiOptions(), USER_OPTIONS_FILE, GUI_ID());
			refreshGui();
			return;
		default: // setGlobalUserKey
			loadAndUpdateFromFileName(guiOptions(), USER_OPTIONS_FILE, ALL_GUI_ID);
			refreshGui();
		}
	}	
	protected String userButtonDescKey() {
		return userButtonKey() + LABEL_DESCRIPTION;
	}

	// ---------- Default Button
	protected String defaultButtonKey() {
		if (globalOptions)  // The old ways
			switch (ModifierKeysState.get()) {
			case CTRL:
			case CTRL_SHIFT: return restoreLocalKey;
			default:		 return setLocalDefaultKey;
			}
		else
			switch (ModifierKeysState.get()) {
			case CTRL:		 return restoreGlobalKey;
			case CTRL_SHIFT: return restoreLocalKey;
			case SHIFT:		 return setLocalDefaultKey;
			default:		 return setGlobalDefaultKey;
			}
	}
	private void initDefaultButtonWidth(Graphics2D g) {
		defaultButtonWidth = buttonWidth(g, new String[] {
				restoreGlobalKey, restoreLocalKey, setLocalDefaultKey, setGlobalDefaultKey});
	}
	protected int defaultButtonWidth(Graphics2D g) {
		if (defaultButtonWidth == 0) 
			localInit(g);
		return defaultButtonWidth;
	}
	protected void doDefaultBoxAction() {
		buttonClick();
		switch (ModifierKeysState.get()) {
		case CTRL: // restoreGlobalKey
			loadAndUpdateFromFileName(guiOptions(), LIVE_OPTIONS_FILE, ALL_GUI_ID);		
			break;
		case CTRL_SHIFT: // restoreLocalKey
			loadAndUpdateFromFileName(guiOptions(), LIVE_OPTIONS_FILE, GUI_ID());		
			break;
		case SHIFT: // setLocalDefaultKey
			setBaseAndModSettingsToDefault(guiOptions(), GUI_ID());		
			break; 
		default: // setGlobalDefaultKey
			setBaseAndModSettingsToDefault(guiOptions(), ALL_GUI_ID);		
			break; 
		}
		refreshGui();
	}
	protected String defaultButtonDescKey() {
		return defaultButtonKey() + LABEL_DESCRIPTION;
	}

	// ---------- Last Button
	protected String lastButtonKey() {
		switch (ModifierKeysState.get()) {
		case CTRL:		 return setGlobalGameKey;
		case CTRL_SHIFT: return setLocalGameKey;
		case SHIFT:		 return setLocalLastKey;
		default:		 return setGlobalLastKey;
		}
	}
	private void initLastButtonWidth(Graphics2D g) {
		lastButtonWidth = buttonWidth(g, new String[] {
				setGlobalGameKey, setLocalGameKey, setLocalLastKey, setGlobalLastKey});
	}
	protected int lastButtonWidth(Graphics2D g) {
		if (lastButtonWidth == 0) 
			localInit(g);
		return lastButtonWidth;
	}
	protected void doLastBoxAction() {
		buttonClick();
		switch (ModifierKeysState.get()) {
		case CTRL: // setGlobalGameKey
			loadAndUpdateFromFileName(guiOptions(), GAME_OPTIONS_FILE, ALL_GUI_ID);
			break;
		case CTRL_SHIFT: // setLocalGameKey
			loadAndUpdateFromFileName(guiOptions(), GAME_OPTIONS_FILE, GUI_ID());
			break;
		case SHIFT: // setLocalLastKey
			loadAndUpdateFromFileName(guiOptions(), LAST_OPTIONS_FILE, GUI_ID());
			break;
		default: // setGlobalLastKey
			loadAndUpdateFromFileName(guiOptions(), LAST_OPTIONS_FILE, ALL_GUI_ID);
		}
		refreshGui();
	}
	protected String lastButtonDescKey() {
		return lastButtonKey() + LABEL_DESCRIPTION;
	}

	// ---------- Events management
	@Override public void mouseClicked(MouseEvent e)	{  }
	@Override public void mousePressed(MouseEvent e)	{  }
	@Override public void mouseEntered(MouseEvent e)	{  }
	@Override public void mouseExited(MouseEvent e)		{  }
	@Override public void mouseDragged(MouseEvent e)	{  }
	@Override public void mouseMoved(MouseEvent e)		{
		checkModifierKey(e);		
		int x = e.getX();
		int y = e.getY();
//		Shape prevHover = hoverBox;
		hoverPolyBox	= null;
		hoverBox		= null;

		for (Box box : boxBaseList)
				if (box.contains(x,y)) {
					hoverBox = box;
					break;
				}
		if (hoverBox != prevHover) {
			loadGuide();
			repaint();
			return;
		}
		for (PolyBox box : polyBoxList)
				if (box.contains(x,y)) {
					hoverPolyBox = box;
					break;
				}
		if (hoverPolyBox != prevHover) {
			repaint();
		}
	}
	@Override public void keyPressed(KeyEvent e)		{
		checkModifierKey(e);		
		int k = e.getKeyCode();
		switch(k) {
			case KeyEvent.VK_F1:
				if (showContextualHelp())
					return;
				showHelp(); // Panel Help
				return;
		}
	}
	// ---------- Help management
	protected void loadGuide()							{
		if (hoverBox == null) {
			clearGuide();
			return;
		}
		if (!autoGuide)
			return;
	  	String txt = hoverBox.getGuide();
	  	if (txt == null || txt.isEmpty())
	  		return;
	  	setGuide(hoverBox, txt);
	}
	private boolean showContextualHelp()				{ // Following "F1!
		if (hoverBox == null)
			return false; // ==> panel help
		String txt = hoverBox.getHelp();
	  	if (txt == null || txt.isEmpty())
	  		return false; // ==> panel help
			setGuide(hoverBox, txt);
			showGuide(getGraphics());
	  	return true;
	}
	private void setGuide(Rectangle dest, String text)	{
		int	maxWidth  = scaled(400);
		guideUI = RotPUI.helpUI();
		guideUI.clear();
		HelpSpec sp = guideUI.addBrownHelpText(0, 0, maxWidth, 1, text);
		sp.autoSize(frame().getGraphics());
		sp.autoPosition(dest);
	}
	protected void showGuide(Graphics g)				{
		if (guideUI == null)
			return;
		guideUI.paint(g, false);
	}
	protected void clearGuide()							{
		if (guideUI == null)
			return;
		guideUI.clear();
		guideUI = null;
	}
	// ========== Sub Classes ==========
	//
	public class Box extends Rectangle {
		private InterfaceParam	param;
		private String			label;
		// ========== Constructors ==========
		//
		public Box()						 { boxBaseList.add(this); }
		Box(String label)					 {
			this();
			boxHelpList.add(this);
			this.label = label;
		}
		Box(InterfaceParam param)			 {
			this();
			boxHelpList.add(this);
			this.param = param;
		}
		void param(InterfaceParam param)	 { this.param = param; }
		InterfaceParam param()	 			 { return param; }
		void label(String label)			 { this.label = label; }
		private String getDescription()		 {
			String desc = getParamDescription();
			if (desc.isEmpty())
				return getLabelDescription();
			else
				return desc;
		}
		private String getHelp()			 {
			String help = getParamHelp();
			if (help.isEmpty())
				return getLabelHelp();
			else
				return help;
		}
		String getGuide()					 {
			String guide = getParamGuide();
			if (guide.isEmpty())
				return getLabelHelp();
			else
				return guide;
		}
		private String getLabelDescription() {
			if (label == null)
				return "";
			String descLabel = label + LABEL_DESCRIPTION;
			String desc = text(descLabel);
			if (desc.equals(descLabel))
				return "";
			else
				return desc;
		}
		private String getLabelHelp()		 {
			if (label == null)
				return "";
			String helpLabel = label;
			String help = text(helpLabel);
			if (help.equals(helpLabel))
				return getDescription();
			else
				return help;
		}
		private String getParamDescription() {
			if (param == null)
				return "";
			return param.getGuiDescription();
		}
		private String getParamHelp()		 {
			if (param == null)
				return "";
			return param.getFullHelp();
		}
		private String getParamGuide()		 {
			if (param == null)
				return "";
			return param.getGuide();
		}
	}
	class PolyBox extends Polygon {
		// ========== Constructors ==========
		//
		PolyBox() { polyBoxList.add(this); }
	}

	// ========== Sub Classes ==========
	//
	public class ModText extends BaseText {

		private final Box box = new Box();

		/**
		* @param p		BasePanel
		* @param logo	logoFont
		* @param fSize	fontSize
		* @param x1	xOrig
		* @param y1	yOrig
		* @param c1	enabledC
		* @param c2	disabledC
		* @param c3	hoverC
		* @param c4	depressedC
		* @param c5	shadeC
		* @param i1	bdrStep
		* @param i2	topLBdr
		* @param i3	btmRBdr
		*/
		public ModText(BasePanel p, boolean logo, int fSize, int x1, int y1, Color c1, Color c2, Color c3, Color c4,
				Color c5, int i1, int i2, int i3) {
			super(p, logo, fSize, x1, y1, c1, c2, c3, c4, c5, i1, i2, i3);
		}
		ModText param(InterfaceParam param)	 { box.param(param); return this; }
		ModText label(String label)			 { box.label(label); return this; }
		Box getBox() {
			box.setBounds(bounds());
			return box;
		}
	}
}
