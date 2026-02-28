package org.geogebra.common.gpad;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Collects XML settings data during SAX-style parsing for later conversion
 * to GPAD @@env format.  Each top-level XML element (kernel, gui, algebraView,
 * spreadsheetView, etc.) is stored as a map of child-tag to attributes,
 * preserving the structure needed by {@link XmlSettingsToGpadEnvConverter}.
 */
public class XmlSettingsCollector {

	// kernel child elements: tag -> attrs
	private final List<TagAttrs> kernelChildren = new ArrayList<>();

	// gui child elements (only font, labelingStyle)
	private final List<TagAttrs> guiChildren = new ArrayList<>();

	// algebraView child elements: mode, auxiliary
	private final List<TagAttrs> algebraViewChildren = new ArrayList<>();

	// spreadsheetView: top-level attrs + child elements
	private LinkedHashMap<String, String> spreadsheetViewAttrs;
	private final List<TagAttrs> spreadsheetViewChildren = new ArrayList<>();

	// keyboard attrs (single tag)
	private LinkedHashMap<String, String> keyboardAttrs;

	// probabilityCalculator child elements
	private final List<TagAttrs> probCalcChildren = new ArrayList<>();

	// scripting attrs (single tag)
	private LinkedHashMap<String, String> scriptingAttrs;

	// tableview attrs (single tag)
	private LinkedHashMap<String, String> tableviewAttrs;

	// euclidianView (ev1, ev2) child elements
	private final List<TagAttrs> ev1Children = new ArrayList<>();
	private final List<TagAttrs> ev2Children = new ArrayList<>();

	// euclidianView3D child elements
	private final List<TagAttrs> ev3dChildren = new ArrayList<>();

	private boolean hasKernel;
	private boolean hasGui;
	private boolean hasAlgebraView;
	private boolean hasSpreadsheetView;
	private boolean hasKeyboard;
	private boolean hasProbCalc;
	private boolean hasScripting;
	private boolean hasTableview;
	private boolean hasEv1;
	private boolean hasEv2;
	private boolean hasEv3d;

	// perspective data (single "tmp" perspective)
	private boolean hasPerspective;
	private final List<PaneData> perspectivePanes = new ArrayList<>();
	private final List<ViewData> perspectiveViews = new ArrayList<>();
	private LinkedHashMap<String, String> perspectiveToolbarAttrs;
	private LinkedHashMap<String, String> perspectiveInputAttrs;

	// app / subApp from the <geogebra> root element
	private String appCode;
	private String subAppCode;

	/** A simple pair of tag name and its attributes. */
	public static class TagAttrs {
		public final String tag;
		public final LinkedHashMap<String, String> attrs;

		public TagAttrs(String tag, LinkedHashMap<String, String> attrs) {
			this.tag = tag;
			this.attrs = new LinkedHashMap<>(attrs);
		}
	}

	// --- kernel ---

	public void setHasKernel(boolean v) { hasKernel = v; }
	public boolean hasKernel() { return hasKernel; }

	public void addKernelChild(String tag, LinkedHashMap<String, String> attrs) {
		kernelChildren.add(new TagAttrs(tag, attrs));
	}

	public List<TagAttrs> getKernelChildren() { return kernelChildren; }

	// --- gui ---

	public void setHasGui(boolean v) { hasGui = v; }
	public boolean hasGui() { return hasGui; }

	public void addGuiChild(String tag, LinkedHashMap<String, String> attrs) {
		guiChildren.add(new TagAttrs(tag, attrs));
	}

	public List<TagAttrs> getGuiChildren() { return guiChildren; }

	// --- algebraView ---

	public void setHasAlgebraView(boolean v) { hasAlgebraView = v; }
	public boolean hasAlgebraView() { return hasAlgebraView; }

	public void addAlgebraViewChild(String tag, LinkedHashMap<String, String> attrs) {
		algebraViewChildren.add(new TagAttrs(tag, attrs));
	}

	public List<TagAttrs> getAlgebraViewChildren() { return algebraViewChildren; }

	// --- spreadsheetView ---

	public void setHasSpreadsheetView(boolean v) { hasSpreadsheetView = v; }
	public boolean hasSpreadsheetView() { return hasSpreadsheetView; }

	public void setSpreadsheetViewAttrs(LinkedHashMap<String, String> attrs) {
		this.spreadsheetViewAttrs = attrs != null ? new LinkedHashMap<>(attrs) : null;
	}

	public LinkedHashMap<String, String> getSpreadsheetViewAttrs() {
		return spreadsheetViewAttrs;
	}

	public void addSpreadsheetViewChild(String tag, LinkedHashMap<String, String> attrs) {
		spreadsheetViewChildren.add(new TagAttrs(tag, attrs));
	}

	public List<TagAttrs> getSpreadsheetViewChildren() { return spreadsheetViewChildren; }

	// --- keyboard ---

	public void setKeyboardAttrs(LinkedHashMap<String, String> attrs) {
		this.keyboardAttrs = attrs != null ? new LinkedHashMap<>(attrs) : null;
		this.hasKeyboard = attrs != null;
	}

	public boolean hasKeyboard() { return hasKeyboard; }
	public LinkedHashMap<String, String> getKeyboardAttrs() { return keyboardAttrs; }

	// --- probabilityCalculator ---

	public void setHasProbCalc(boolean v) { hasProbCalc = v; }
	public boolean hasProbCalc() { return hasProbCalc; }

	public void addProbCalcChild(String tag, LinkedHashMap<String, String> attrs) {
		probCalcChildren.add(new TagAttrs(tag, attrs));
	}

	public List<TagAttrs> getProbCalcChildren() { return probCalcChildren; }

	// --- scripting ---

	public void setScriptingAttrs(LinkedHashMap<String, String> attrs) {
		this.scriptingAttrs = attrs != null ? new LinkedHashMap<>(attrs) : null;
		this.hasScripting = attrs != null;
	}

	public boolean hasScripting() { return hasScripting; }
	public LinkedHashMap<String, String> getScriptingAttrs() { return scriptingAttrs; }

	// --- tableview ---

	public void setTableviewAttrs(LinkedHashMap<String, String> attrs) {
		this.tableviewAttrs = attrs != null ? new LinkedHashMap<>(attrs) : null;
		this.hasTableview = attrs != null;
	}

	public boolean hasTableview() { return hasTableview; }
	public LinkedHashMap<String, String> getTableviewAttrs() { return tableviewAttrs; }

	// --- euclidianView (ev1) ---

	public void setHasEv1(boolean v) { hasEv1 = v; }
	public boolean hasEv1() { return hasEv1; }

	public void addEv1Child(String tag, LinkedHashMap<String, String> attrs) {
		ev1Children.add(new TagAttrs(tag, attrs));
	}

	public List<TagAttrs> getEv1Children() { return ev1Children; }

	// --- euclidianView (ev2) ---

	public void setHasEv2(boolean v) { hasEv2 = v; }
	public boolean hasEv2() { return hasEv2; }

	public void addEv2Child(String tag, LinkedHashMap<String, String> attrs) {
		ev2Children.add(new TagAttrs(tag, attrs));
	}

	public List<TagAttrs> getEv2Children() { return ev2Children; }

	// --- euclidianView3D ---

	public void setHasEv3d(boolean v) { hasEv3d = v; }
	public boolean hasEv3d() { return hasEv3d; }

	public void addEv3dChild(String tag, LinkedHashMap<String, String> attrs) {
		ev3dChildren.add(new TagAttrs(tag, attrs));
	}

	public List<TagAttrs> getEv3dChildren() { return ev3dChildren; }

	// --- perspective ---

	public void setHasPerspective(boolean v) { hasPerspective = v; }
	public boolean hasPerspective() { return hasPerspective; }

	public void addPerspectivePane(PaneData pane) { perspectivePanes.add(pane); }
	public List<PaneData> getPerspectivePanes() { return perspectivePanes; }

	public void addPerspectiveView(ViewData view) { perspectiveViews.add(view); }
	public List<ViewData> getPerspectiveViews() { return perspectiveViews; }

	public void setPerspectiveToolbarAttrs(LinkedHashMap<String, String> attrs) {
		this.perspectiveToolbarAttrs = attrs != null ? new LinkedHashMap<>(attrs) : null;
	}
	public LinkedHashMap<String, String> getPerspectiveToolbarAttrs() { return perspectiveToolbarAttrs; }

	public void setPerspectiveInputAttrs(LinkedHashMap<String, String> attrs) {
		this.perspectiveInputAttrs = attrs != null ? new LinkedHashMap<>(attrs) : null;
	}
	public LinkedHashMap<String, String> getPerspectiveInputAttrs() { return perspectiveInputAttrs; }

	// --- app / subApp ---

	public void setAppCode(String v) { appCode = v; }
	public String getAppCode() { return appCode; }

	public void setSubAppCode(String v) { subAppCode = v; }
	public String getSubAppCode() { return subAppCode; }

	/** Split pane data within a perspective. */
	public static class PaneData {
		public final String location;
		public final String divider;
		public final String orientation;

		public PaneData(String location, String divider, String orientation) {
			this.location = location;
			this.divider = divider;
			this.orientation = orientation;
		}
	}

	/** View (dock panel) data within a perspective. */
	public static class ViewData {
		public final String id;
		public final String toolbar;
		public final String visible;
		public final String stylebar;
		public final String location;
		public final String size;
		public final String tab;
		public final String plane;

		public ViewData(LinkedHashMap<String, String> attrs) {
			this.id = attrs.get("id");
			this.toolbar = attrs.get("toolbar");
			this.visible = attrs.get("visible");
			this.stylebar = attrs.get("stylebar");
			this.location = attrs.get("location");
			this.size = attrs.get("size");
			this.tab = attrs.get("tab");
			this.plane = attrs.get("plane");
		}
	}

	/**
	 * @return true if any settings were collected
	 */
	public boolean hasAnySettings() {
		return hasKernel || hasGui || hasAlgebraView || hasSpreadsheetView
				|| hasKeyboard || hasProbCalc || hasScripting || hasTableview
				|| hasEv1 || hasEv2 || hasEv3d || hasPerspective
				|| appCode != null;
	}
}
