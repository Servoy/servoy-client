/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 */
package com.servoy.j2db.server.headlessclient.dataui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.ClientProperties;
import org.apache.wicket.protocol.http.request.WebClientInfo;

import com.servoy.j2db.FormController;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IFormManager;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.IDisplayRelatedData;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.ISwingFoundSet;
import com.servoy.j2db.dataprocessing.RelatedFoundSet;
import com.servoy.j2db.dataprocessing.SortColumn;
import com.servoy.j2db.persistence.ISupportScrollbars;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.server.headlessclient.MainPage;
import com.servoy.j2db.server.headlessclient.WebForm;
import com.servoy.j2db.server.headlessclient.yui.YUILoader;
import com.servoy.j2db.ui.IAccessible;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.IFormLookupPanel;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;
import com.servoy.j2db.ui.ISplitPane;
import com.servoy.j2db.ui.IStylePropertyChanges;
import com.servoy.j2db.ui.ISupportWebBounds;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Utils;

/**
 * This class represents a split pane in the web client
 * 
 * @author gboros
 */
public class WebSplitPane extends WebMarkupContainer implements ISplitPane, IDisplayRelatedData, IProviderStylePropertyChanges, IAccessible, ISupportWebBounds,
	ISupportWebTabSeq, ListSelectionListener
{
	private final IApplication application;
	private final int orient;
	private Color background;
	private Border border;
	private Font font;
	private Color foreground;
	private Point location = new Point(0, 0);
	private Dimension size = new Dimension(0, 0);
	private String name;
	private String tooltip;
	private boolean opaque;
	private boolean accessible = true;
	private Map<Object, Object> clientProperties;
	private final ChangesRecorder jsChangeRecorder = new ChangesRecorder(new Insets(0, 0, 0, 0), new Insets(0, 0, 0, 0));
	private final List<ISwingFoundSet> related = new ArrayList<ISwingFoundSet>();
	private int dividerLocation;
	private int dividerSize = 5;
	private boolean continuousLayout;
	private double resizeWeight;
	private int leftFormMinSize, rightFormMinSize;

	protected IRecordInternal parentData;
	private final List<String> allRelationNames = new ArrayList<String>(2);
	private final WebMarkupContainer splitter;
	private final WebMarkupContainer[] splitComponents = new WebMarkupContainer[2];
	private final WebTabHolder[] webTabs = new WebTabHolder[2];

	private int tabSequenceIndex = ISupportWebTabSeq.DEFAULT;
	private int leftPanelLastTabIndex = ISupportWebTabSeq.DEFAULT;
	private boolean sizeChanged = false;

	private final AbstractServoyDefaultAjaxBehavior dividerUpdater = new AbstractServoyDefaultAjaxBehavior()
	{
		@Override
		public void renderHead(IHeaderResponse response)
		{
			super.renderHead(response);
			if (sizeChanged)
			{
				sizeChanged = false;
				response.renderOnLoadJavascript("wicketAjaxGet('" + getCallbackUrl() + "&anchor=true')");
			}
		}

		@Override
		protected void respond(AjaxRequestTarget target)
		{
			if (getComponent().getRequest().getParameter("location") != null) dividerLocation = Utils.getAsInteger(getComponent().getRequest().getParameter(
				"location")); //$NON-NLS-1$
			if (getComponent().getRequest().getParameter("anchor") != null)
			{
				Page page = findPage();
				if (page instanceof MainPage && ((MainPage)page).getController() != null)
				{
					if (Utils.getAsBoolean(((MainPage)page).getController().getApplication().getSettings().getProperty("servoy.webclient.enableAnchors", //$NON-NLS-1$
						Boolean.TRUE.toString())))
					{
						target.appendJavascript("layoutEntirePage();");
					}
				}
				for (int i = 0; i < 2; i++)
				{
					if (webTabs[i] != null && webTabs[i].getPanel().isReady())
					{
						webTabs[i].getPanel().getWebForm().setFormWidth(0);
					}
				}
				WebEventExecutor.generateResponse(target, page);
			}
		}

	};

	public WebSplitPane(IApplication application, String name, int orient)
	{
		super(name);
		this.application = application;
		this.orient = orient;

		setOutputMarkupPlaceholderTag(true);
		add(StyleAttributeModifierModel.INSTANCE);
		add(TooltipAttributeModifier.INSTANCE);
		add(dividerUpdater);

		splitter = new WebMarkupContainer("splitter"); //$NON-NLS-1$
		splitComponents[0] = new WebMarkupContainer("websplit_left"); //$NON-NLS-1$
		splitComponents[1] = new WebMarkupContainer("websplit_right"); //$NON-NLS-1$
		splitComponents[0].add(new Label("webform", new Model<String>(""))); //$NON-NLS-1$ //$NON-NLS-2$
		splitComponents[1].add(new Label("webform", new Model<String>(""))); //$NON-NLS-1$ //$NON-NLS-2$

		splitter.add(splitComponents[0]);
		add(splitter);
		add(splitComponents[1]);
	}

	public Color getBackground()
	{
		return background;
	}

	public Border getBorder()
	{
		return border;
	}

	public Font getFont()
	{
		return font;
	}

	public Color getForeground()
	{
		return foreground;
	}

	public Point getLocation()
	{
		return location;
	}

	public String getName()
	{
		return name;
	}

	public Dimension getSize()
	{
		return size;
	}

	public String getToolTipText()
	{
		return tooltip;
	}

	public boolean isOpaque()
	{
		return opaque;
	}

	public void setBackground(Color background)
	{
		this.background = background;
	}

	public void setBorder(Border border)
	{
		this.border = border;
	}

	public void setComponentEnabled(final boolean enabled)
	{
		if (accessible)
		{
			super.setEnabled(enabled);
			visitChildren(IComponent.class, new IVisitor<Component>()
			{
				public Object component(Component component)
				{
					if (component instanceof IComponent)
					{
						((IComponent)component).setComponentEnabled(enabled);
					}
					else
					{
						component.setEnabled(enabled);
					}
					return CONTINUE_TRAVERSAL;
				}
			});
			jsChangeRecorder.setChanged();
		}
	}

	public void setComponentVisible(boolean visible)
	{
		setVisible(visible);
	}

	public void setCursor(Cursor cursor)
	{
	}

	public void setFont(Font font)
	{
		this.font = font;
	}

	public void setForeground(Color foreground)
	{
		this.foreground = foreground;
	}

	public void setLocation(Point location)
	{
		this.location = location;
	}

	public void setName(String n)
	{
		name = n;
	}

	public void setOpaque(boolean opaque)
	{
		this.opaque = opaque;
	}

	public void setSize(Dimension size)
	{
		this.size = size;
	}

	public void setToolTipText(String tooltip)
	{
		if (Utils.stringIsEmpty(tooltip))
		{
			tooltip = null;
		}
		this.tooltip = tooltip;
	}

	public String[] getAllRelationNames()
	{
		String[] retval = new String[allRelationNames.size()];
		for (int i = 0; i < retval.length; i++)
		{
			Object relationName = allRelationNames.get(i);
			if (relationName != null)
			{
				retval[i] = relationName.toString();
			}
		}
		return retval;
	}

	public List<SortColumn> getDefaultSort()
	{
		// cant return anything because the splitpane shows 2 forms, so which one to get?
		return null;
	}

	public String getSelectedRelationName()
	{
		return null;
	}

	public void notifyVisible(boolean visible, List<Runnable> invokeLaterRunnables)
	{
		for (int tabIdx = 0; tabIdx < 2; tabIdx++)
		{
			notifyVisibleForm(visible, tabIdx, invokeLaterRunnables);
		}
	}

	private boolean notifyVisibleForm(boolean visible, int tabIdx, List<Runnable> invokeLaterRunnables)
	{
		if (webTabs[tabIdx] != null)
		{
			WebTabFormLookup fl = webTabs[tabIdx].getPanel();
			FormController controller = fl.getWebForm().getController();

			//this is not needed when closing
			if (visible && parentData != null)
			{
				showFoundSet(fl, parentData, controller.getDefaultSortColumns());

				// Test if current one is there
				if (fl.isReady())
				{
					if (splitComponents[tabIdx].get(fl.getWebForm().getId()) != null)
					{
						// replace it
						splitComponents[tabIdx].replace(fl.getWebForm());
					}
					else
					{
						// else add it
						splitComponents[tabIdx].add(fl.getWebForm());
					}
					FormController fc = fl.getWebForm().getController();
					if (tabIdx == 1 && webTabs[0] != null) fc.recomputeTabSequence(leftPanelLastTabIndex);
					else fc.recomputeTabSequence(tabSequenceIndex);
				}
			}

			return controller.notifyVisible(visible, invokeLaterRunnables);
		}

		return false;
	}

	public void setRecord(IRecordInternal parentState, boolean stopEditing)
	{
		parentData = parentState;
		for (int tabIdx = 0; tabIdx < 2; tabIdx++)
		{
			if (webTabs[tabIdx] != null)
			{
				WebTabFormLookup fl = webTabs[tabIdx].getPanel();
				showFoundSet(fl, parentState, fl.getDefaultSort(parentData != null || fl.isReady()));
			}
		}
	}

	public boolean isReadOnly()
	{
		boolean isReadOnly = true;
		for (int tabIdx = 0; tabIdx < 2; tabIdx++)
		{
			isReadOnly = isReadOnly && (webTabs[tabIdx] != null ? webTabs[tabIdx].getPanel().isReadOnly() : false);
		}

		return isReadOnly;
	}

	public void setValidationEnabled(boolean mode)
	{
	}

	public boolean stopUIEditing(boolean looseFocus)
	{
		boolean stopUIEditing = true;
		for (int tabIdx = 0; tabIdx < 2; tabIdx++)
		{
			if (webTabs[tabIdx] != null && webTabs[tabIdx].getPanel().isReady())
			{
				stopUIEditing = stopUIEditing && webTabs[tabIdx].getPanel().getWebForm().getController().stopUIEditing(true);
			} // else there is nothing there that could be edited (avoid creating forms again with getWebForm() when the app. is shutting down)
		}
		return stopUIEditing;
	}

	public void destroy()
	{
		deregisterSelectionListeners();
		//TODO should deregister related foundsets??
	}

	public IStylePropertyChanges getStylePropertyChanges()
	{
		return jsChangeRecorder;
	}

	public void setAccessible(boolean b)
	{
		if (!b) setComponentEnabled(b);
		accessible = b;
	}

	public Insets getPaddingAndBorder()
	{
		return jsChangeRecorder.getPaddingAndBorder(size.height, border, new Insets(0, 0, 0, 0), 0, null);
	}

	public Rectangle getWebBounds()
	{
		Dimension d = jsChangeRecorder.calculateWebSize(size.width, size.height, border, new Insets(0, 0, 0, 0), 0, null);
		return new Rectangle(location, d);
	}

	public void valueChanged(ListSelectionEvent e)
	{
		if (parentData != null)
		{
			if (webTabs[0] != null)
			{
				WebTabFormLookup panel = webTabs[0].getPanel();
				showFoundSet(panel, parentData, panel.getDefaultSort(true));
			}
			if (webTabs[1] != null)
			{
				WebTabFormLookup panel = webTabs[1].getPanel();
				showFoundSet(panel, parentData, panel.getDefaultSort(true));
			}
		}
	}

	private void showFoundSet(WebTabFormLookup flp, IRecordInternal parentState, List<SortColumn> sort)
	{
		deregisterSelectionListeners();

		if (!flp.isReady()) return;

		FormController fp = flp.getWebForm().getController();
		if (parentState != null && fp != null && flp.getRelationName() != null)
		{
			IFoundSetInternal relatedFoundset = parentState.getRelatedFoundSet(flp.getRelationName(), sort);
			if (relatedFoundset != null) registerSelectionListeners(parentState, flp.getRelationName());
			fp.loadData(relatedFoundset, null);
		}
	}

	private void registerSelectionListeners(IRecordInternal parentState, String relationName)
	{
		String[] parts = relationName.split("\\."); //$NON-NLS-1$
		IRecordInternal currentRecord = parentState;
		for (int i = 0; currentRecord != null && i < parts.length - 1; i++)
		{
			IFoundSetInternal fs = currentRecord.getRelatedFoundSet(parts[i], null);
			if (fs instanceof ISwingFoundSet)
			{
				related.add((ISwingFoundSet)fs);
				((ISwingFoundSet)fs).getSelectionModel().addListSelectionListener(this);
			}
			currentRecord = (fs == null) ? null : fs.getRecord(fs.getSelectedIndex());
		}
	}

	private void deregisterSelectionListeners()
	{
		for (ISwingFoundSet fs : related)
		{
			fs.getSelectionModel().removeListSelectionListener(this);
		}
		related.clear();
	}

	@Override
	protected void onRender(MarkupStream markupStream)
	{
		super.onRender(markupStream);
		jsChangeRecorder.setRendered();
	}

	@Override
	public void onBeforeRender()
	{
		super.onBeforeRender();
		String dim, pos;
		if (orient == TabPanel.SPLIT_HORIZONTAL)
		{
			dim = "width: "; //$NON-NLS-1$
			pos = "left: "; //$NON-NLS-1$
		}
		else
		{
			dim = "height: "; //$NON-NLS-1$
			pos = "top: "; //$NON-NLS-1$
		}

		if (dividerLocation < leftFormMinSize) dividerLocation = leftFormMinSize;
		splitter.add(new StyleAppendingModifier(new Model<String>(dim + (dividerLocation + dividerSize) + "px"))); //$NON-NLS-1$
		splitComponents[0].add(new StyleAppendingModifier(new Model<String>(dim + dividerLocation + "px"))); //$NON-NLS-1$
		splitComponents[1].add(new StyleAppendingModifier(new Model<String>(pos + (dividerLocation + dividerSize) + "px"))); //$NON-NLS-1$
	}

	@Override
	public void renderHead(HtmlHeaderContainer container)
	{
		super.renderHead(container);
		IHeaderResponse headerResponse = container.getHeaderResponse();
		YUILoader.renderResize(headerResponse);

		String dim, dim_o, pos;
		if (orient == TabPanel.SPLIT_HORIZONTAL)
		{
			dim = "Width"; //$NON-NLS-1$
			dim_o = "height"; //$NON-NLS-1$
			pos = "left"; //$NON-NLS-1$
		}
		else
		{
			dim = "Height"; //$NON-NLS-1$
			dim_o = "width"; //$NON-NLS-1$
			pos = "top"; //$NON-NLS-1$
		}

		Map<String, String> leftPanelOverflow = getFormOverflowStyle(getLeftForm());
		Map<String, String> rightPanelOverflow = getFormOverflowStyle(getRightForm());

		StringBuffer resizeScript = new StringBuffer("var splitter = YAHOO.util.Dom.get('splitter_").append(getMarkupId()).append("');"); //$NON-NLS-1$ //$NON-NLS-2$
		resizeScript.append("var left = YAHOO.util.Dom.get('websplit_left_").append(getMarkupId()).append("');"); //$NON-NLS-1$ //$NON-NLS-2$
		resizeScript.append("YAHOO.util.Dom.setStyle(left, 'background-color', '#FFFFFF');"); //$NON-NLS-1$
		resizeScript.append("YAHOO.util.Dom.setStyle(left, 'overflow-x', '").append(leftPanelOverflow.get("overflow-x")).append("');"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
		resizeScript.append("YAHOO.util.Dom.setStyle(left, 'overflow-y', '").append(leftPanelOverflow.get("overflow-y")).append("');"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		resizeScript.append("var right = YAHOO.util.Dom.get('websplit_right_").append(getMarkupId()).append("');"); //$NON-NLS-1$ //$NON-NLS-2$
		resizeScript.append("YAHOO.util.Dom.setStyle(right, 'overflow-x', '").append(rightPanelOverflow.get("overflow-x")).append("');"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
		resizeScript.append("YAHOO.util.Dom.setStyle(right, 'overflow-y', '").append(rightPanelOverflow.get("overflow-y")).append("');"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		resizeScript.append("var resize = new YAHOO.util.Resize(splitter, { min").append(dim).append(": ").append(dividerSize + leftFormMinSize).append(", max").append(dim).append(": splitter.offsetParent.offset").append(dim).append(" - ").append(rightFormMinSize).append(", ").append(continuousLayout ? "" : "proxy: true, "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ 
		resizeScript.append("handles: ['").append(orient == TabPanel.SPLIT_HORIZONTAL ? "r" : "b").append("']});"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		resizeScript.append("YAHOO.util.Dom.setStyle(splitter, '").append(dim_o).append("', '');"); //$NON-NLS-1$ //$NON-NLS-2$

		String dividerBg = null;
		if (background != null)
		{
			dividerBg = Integer.toHexString(background.getRGB());
			dividerBg = "#" + dividerBg.substring(2, dividerBg.length()); //$NON-NLS-1$
		}

		dim = dim.toLowerCase();
		resizeScript.append("var splitterDivs = splitter.getElementsByTagName('div');"); //$NON-NLS-1$ 
		resizeScript.append(
			"for(var x = 0; x < splitterDivs.length; x++) { if(splitterDivs[x].parentNode == splitter && splitterDivs[x].id.match('yui') != null) { ").append( //$NON-NLS-1$ 
			"YAHOO.util.Dom.setStyle(splitterDivs[x], '").append(dim).append("', '").append(dividerSize).append("px');").append(dividerBg != null ? "YAHOO.util.Dom.setStyle(splitterDivs[x], 'background-color', '" + dividerBg + "');" : "").append("break; } }; "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$

		if (!continuousLayout)
		{
			resizeScript.append("resize.on('startResize', function(ev) {"); //$NON-NLS-1$  			
			resizeScript.append("YAHOO.util.Dom.setStyle(splitter, '").append(dim_o).append("', '');"); //$NON-NLS-1$ //$NON-NLS-2$
			resizeScript.append("YAHOO.util.Dom.setStyle(this.getProxyEl(), '").append(dim_o).append("', '100%');"); //$NON-NLS-1$ //$NON-NLS-2$
			resizeScript.append("YAHOO.util.Dom.setStyle(this.getProxyEl(), 'border', 'none');"); //$NON-NLS-1$
			resizeScript.append("YAHOO.util.Dom.setStyle(this.getProxyEl(), 'padding', 0);"); //$NON-NLS-1$
			resizeScript.append(
				"this.getProxyEl().innerHTML = '<div style = \"filter: alpha(opacity=50); opacity: 0.5; -moz-opacity: 0.5; position: absolute; left: 0; right: 0; top: 0; bottom: 0; border-").append(orient == TabPanel.SPLIT_HORIZONTAL ? "right" : "bottom").append( //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				":").append(dividerSize).append("px solid ").append(dividerBg != null ? dividerBg : "#7D98B8").append("\"></div>';"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$			
			resizeScript.append("});"); //$NON-NLS-1$

		}

		resizeScript.append("resize.on('resize', function(ev) {"); //$NON-NLS-1$ 
		resizeScript.append("var d = ev.").append(dim).append(";"); //$NON-NLS-1$ //$NON-NLS-2$
		resizeScript.append("YAHOO.util.Dom.setStyle(splitter, '").append(dim_o).append("', '');"); //$NON-NLS-1$ //$NON-NLS-2$
		resizeScript.append("var newLeftSize = parseInt(YAHOO.util.Dom.getStyle(splitter, '").append(dim).append("'), 10);"); //$NON-NLS-1$ //$NON-NLS-2$
		resizeScript.append("YAHOO.util.Dom.setStyle(left, '").append(dim).append("', (newLeftSize - ").append(dividerSize).append(") + 'px');"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
		resizeScript.append("YAHOO.util.Dom.setStyle(right, '").append(pos).append("', d + 'px');"); //$NON-NLS-1$ //$NON-NLS-2$
		resizeScript.append("});"); //$NON-NLS-1$ 

		resizeScript.append("resize.on('endResize', function(ev) {"); //$NON-NLS-1$ 
		resizeScript.append("var newLeftSize = parseInt(YAHOO.util.Dom.getStyle(splitter, '").append(dim).append("'), 10);"); //$NON-NLS-1$ //$NON-NLS-2$
		resizeScript.append("YAHOO.util.Dom.setStyle(splitter, '").append(dim_o).append("', '');"); //$NON-NLS-1$ //$NON-NLS-2$
		resizeScript.append("wicketAjaxGet('").append(dividerUpdater.getCallbackUrl()).append("&anchor=true").append("&location=' + (newLeftSize - ").append(dividerSize).append("));"); //$NON-NLS-1$  //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		resizeScript.append("});"); //$NON-NLS-1$

		boolean useAnchors = Utils.getAsBoolean((application.getSettings().getProperty("servoy.webclient.enableAnchors", Boolean.TRUE.toString()))); //$NON-NLS-1$
		if (useAnchors)
		{
			String splitId = getMarkupId();
			resizeScript.append("\nif(typeof(splitPanes) != \"undefined\")\n").append("{\n"); //$NON-NLS-1$ //$NON-NLS-2$
			resizeScript.append("splitPanes['").append(splitId).append("'] = new Array();\n"); //$NON-NLS-1$ //$NON-NLS-2$
			resizeScript.append("splitPanes['").append(splitId).append("']['orient'] = '").append(orient == TabPanel.SPLIT_HORIZONTAL ? "h" : "v").append("';\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			resizeScript.append("splitPanes['").append(splitId).append("']['resize'] = resize;\n"); //$NON-NLS-1$ //$NON-NLS-2$ 
			resizeScript.append("splitPanes['").append(splitId).append("']['splitter'] = splitter;\n"); //$NON-NLS-1$ //$NON-NLS-2$ 
			resizeScript.append("splitPanes['").append(splitId).append("']['left'] = left;\n"); //$NON-NLS-1$ //$NON-NLS-2$
			resizeScript.append("splitPanes['").append(splitId).append("']['right'] = right;\n"); //$NON-NLS-1$ //$NON-NLS-2$
			resizeScript.append("splitPanes['").append(splitId).append("']['currentSize'] = splitter.offsetParent.offset").append(orient == TabPanel.SPLIT_HORIZONTAL ? "Width" : "Height").append(";\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			resizeScript.append("splitPanes['").append(splitId).append("']['resizeWeight'] = ").append(resizeWeight).append(";\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
			resizeScript.append("splitPanes['").append(splitId).append("']['dividerSize'] = ").append(dividerSize).append(";\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			resizeScript.append("splitPanes['").append(splitId).append("']['leftMin'] = ").append(leftFormMinSize).append(";\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			resizeScript.append("splitPanes['").append(splitId).append("']['rightMin'] = ").append(rightFormMinSize).append(";\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			resizeScript.append("splitPanes['").append(splitId).append("']['callback'] = '").append(dividerUpdater.getCallbackUrl()).append("';\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			resizeScript.append("}\n"); //$NON-NLS-1$			
		}


		ClientProperties clp = ((WebClientInfo)Session.get().getClientInfo()).getProperties();
		if (clp.isBrowserKonqueror() || clp.isBrowserSafari()) headerResponse.renderOnLoadJavascript(resizeScript.toString());
		else headerResponse.renderOnDomReadyJavascript(resizeScript.toString());
	}

	private Map<String, String> getFormOverflowStyle(IFormLookupPanel formLookup)
	{
		Map<String, String> formOverflowStyle = new HashMap<String, String>();

		IFormManager fm = application.getFormManager();
		int scrollbars = (formLookup != null) ? ((FormController)fm.getForm(formLookup.getFormName())).getForm().getScrollbars()
			: ISupportScrollbars.SCROLLBARS_WHEN_NEEDED;


		if ((scrollbars & ISupportScrollbars.HORIZONTAL_SCROLLBAR_NEVER) == ISupportScrollbars.HORIZONTAL_SCROLLBAR_NEVER)
		{
			formOverflowStyle.put("overflow-x", "hidden"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		else if ((scrollbars & ISupportScrollbars.HORIZONTAL_SCROLLBAR_ALWAYS) == ISupportScrollbars.HORIZONTAL_SCROLLBAR_ALWAYS)
		{
			formOverflowStyle.put("overflow-x", "scroll"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		else
		{
			formOverflowStyle.put("overflow-x", "auto"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		if ((scrollbars & ISupportScrollbars.VERTICAL_SCROLLBAR_NEVER) == ISupportScrollbars.VERTICAL_SCROLLBAR_NEVER)
		{
			formOverflowStyle.put("overflow-y", "hidden"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		else if ((scrollbars & ISupportScrollbars.VERTICAL_SCROLLBAR_ALWAYS) == ISupportScrollbars.VERTICAL_SCROLLBAR_ALWAYS)
		{
			formOverflowStyle.put("overflow-y", "scroll"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		else
		{
			formOverflowStyle.put("overflow-y", "auto"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		return formOverflowStyle;
	}

	@Override
	public String getMarkupId()
	{
		return WebComponentSpecialIdMaker.getSpecialIdIfAppropriate(this);
	}

	public String js_getToolTipText()
	{
		return tooltip;
	}

	public boolean js_isTransparent()
	{
		return !opaque;
	}

	public void js_setFont(String spec)
	{
		font = PersistHelper.createFont(spec);
		jsChangeRecorder.setFont(spec);
	}

	public void js_setToolTipText(String tooltip)
	{
		setToolTipText(tooltip);
		jsChangeRecorder.setChanged();
	}

	public void js_setTransparent(boolean b)
	{
		opaque = !b;
		jsChangeRecorder.setTransparent(b);
	}

	public int js_getAbsoluteFormLocationY()
	{
		WebDataRenderer parent = findParent(WebDataRenderer.class);
		if (parent != null)
		{
			return parent.getYOffset() + getLocation().y;
		}
		return getLocation().y;
	}

	public String js_getBgcolor()
	{
		return PersistHelper.createColorString(background);
	}

	public Object js_getClientProperty(Object key)
	{
		if (clientProperties == null) return null;
		return clientProperties.get(key);
	}

	public String js_getElementType()
	{
		return "SPLITPANE"; //$NON-NLS-1$
	}

	public String js_getFgcolor()
	{
		return PersistHelper.createColorString(foreground);
	}

	public int js_getHeight()
	{
		return size.height;
	}

	public int js_getLocationX()
	{
		return getLocation().x;
	}

	public int js_getLocationY()
	{
		return getLocation().y;
	}

	public String js_getName()
	{
		String jsName = getName();
		if (jsName != null && jsName.startsWith(ComponentFactory.WEB_ID_PREFIX)) jsName = null;
		return jsName;
	}

	public int js_getWidth()
	{
		return size.width;
	}

	public boolean js_isEnabled()
	{
		return isEnabled();
	}

	public boolean js_isVisible()
	{
		return isVisible();
	}

	public void js_putClientProperty(Object key, Object value)
	{
		if (clientProperties == null)
		{
			clientProperties = new HashMap<Object, Object>();
		}
		clientProperties.put(key, value);
	}

	public void js_setBgcolor(String clr)
	{
		background = PersistHelper.createColor(clr);
		jsChangeRecorder.setBgcolor(clr);
	}

	public void js_setBorder(String spec)
	{
		setBorder(ComponentFactoryHelper.createBorder(spec));
		jsChangeRecorder.setBorder(spec);
	}

	public void js_setEnabled(boolean b)
	{
		setComponentEnabled(b);
	}

	public void js_setFgcolor(String clr)
	{
		foreground = PersistHelper.createColor(clr);
		jsChangeRecorder.setChanged();
	}

	public void js_setLocation(int x, int y)
	{
		location = new Point(x, y);
		jsChangeRecorder.setLocation(x, y);
	}

	public void js_setSize(int width, int height)
	{
		size = new Dimension(width, height);
		jsChangeRecorder.setSize(width, height, border, new Insets(0, 0, 0, 0), 0);
		for (int tabIdx = 0; tabIdx < 2; tabIdx++)
		{
			if (webTabs[tabIdx] != null && webTabs[tabIdx].getPanel().isReady())
			{
				webTabs[tabIdx].getPanel().getWebForm().setFormWidth(0);
			}
		}
	}

	public void js_setVisible(boolean visible)
	{
		setVisible(visible);
		jsChangeRecorder.setVisible(visible);
	}

	public boolean js_isReadOnly()
	{
		return isReadOnly();
	}

	public void js_setReadOnly(boolean b)
	{
		for (int tabIdx = 0; tabIdx < 2; tabIdx++)
		{
			if (webTabs[tabIdx] != null) webTabs[tabIdx].getPanel().setReadOnly(b);
		}
		jsChangeRecorder.setChanged();
	}

	public void setLeftForm(IFormLookupPanel flp)
	{
		allRelationNames.add(0, flp == null ? null : flp.getRelationName());
		webTabs[0] = new WebTabHolder(flp.getFormName(), flp, null, null);
	}

	public IFormLookupPanel getLeftForm()
	{
		return webTabs[0] != null ? webTabs[0].getPanel() : null;
	}

	public void setRightForm(IFormLookupPanel flp)
	{
		allRelationNames.add(1, flp == null ? null : flp.getRelationName());
		webTabs[1] = new WebTabHolder(flp.getFormName(), flp, null, null);
	}

	public IFormLookupPanel getRightForm()
	{
		return webTabs[1] != null ? webTabs[1].getPanel() : null;
	}

	public IFormLookupPanel createFormLookupPanel(String name, String relationName, String formName)
	{
		return new WebTabFormLookup(name, relationName, formName, this, application);
	}

	public boolean js_setLeftForm(Object form, Object relation)
	{
		if (setForm(true, form, relation))
		{
			jsChangeRecorder.setChanged();
			return true;
		}
		else return false;
	}

	public FormScope js_getLeftForm()
	{
		if (webTabs[0] != null) return webTabs[0].getPanel().getWebForm().getController().getFormScope();
		return null;
	}

	public boolean js_setRightForm(Object form, Object relation)
	{
		if (setForm(false, form, relation))
		{
			jsChangeRecorder.setChanged();
			return true;
		}
		else return false;
	}

	public FormScope js_getRightForm()
	{
		if (webTabs[1] != null) return webTabs[1].getPanel().getWebForm().getController().getFormScope();
		return null;
	}

	public void js_setDividerLocation(double locationPos)
	{
		if (locationPos < 0) return;
		if (locationPos < 1) dividerLocation = (int)((orient == TabPanel.SPLIT_HORIZONTAL ? getSize().width : getSize().height) * locationPos);
		else dividerLocation = (int)locationPos;
		jsChangeRecorder.setChanged();
		sizeChanged = true;
	}

	public double js_getDividerLocation()
	{
		return dividerLocation;
	}

	public void js_setDividerSize(int size)
	{
		dividerSize = size < 0 ? 0 : size;
		jsChangeRecorder.setChanged();
		sizeChanged = true;
	}

	public int js_getDividerSize()
	{
		return dividerSize;
	}

	public double js_getResizeWeight()
	{
		return resizeWeight;
	}

	public void js_setResizeWeight(double resizeWeight)
	{
		this.resizeWeight = resizeWeight;
		jsChangeRecorder.setChanged();
	}

	public boolean js_getContinuousLayout()
	{
		return continuousLayout;
	}

	public void js_setContinuousLayout(boolean b)
	{
		continuousLayout = b;
		jsChangeRecorder.setChanged();
	}

	public int js_getRightFormMinSize()
	{
		return rightFormMinSize;
	}

	public void js_setRightFormMinSize(int minSize)
	{
		rightFormMinSize = minSize;
		jsChangeRecorder.setChanged();
	}

	public int js_getLeftFormMinSize()
	{
		return leftFormMinSize;
	}

	public void js_setLeftFormMinSize(int minSize)
	{
		leftFormMinSize = minSize;
		jsChangeRecorder.setChanged();
	}

	private boolean setForm(boolean bLeftForm, Object form, Object relation)
	{
		FormController f = null;
		boolean readOnly = false;
		if (form instanceof FormController)
		{
			f = (FormController)form;
			readOnly = f.isReadOnly();
		}
		if (form instanceof FormController.JSForm)
		{
			f = ((FormController.JSForm)form).getFormPanel();
			readOnly = f.isReadOnly();
		}

		if (f != null)
		{
			String name = f.getName();

			RelatedFoundSet relatedFs = null;
			String relationName = null;
			if (relation instanceof RelatedFoundSet)
			{
				relatedFs = (RelatedFoundSet)relation;
			}
			else if (relation instanceof String)
			{
				relationName = (String)relation;
			}

			if (relatedFs != null)
			{
				relationName = relatedFs.getRelationName();
				if (!relatedFs.getDataSource().equals(f.getDataSource()))
				{
					return false;
				}
				// TODO do this check to check if the parent table has this relation? How to get the parent table 
//				Table parentTable = null;
//				application.getSolution().getRelations(Solution.SOLUTION+Solution.MODULES, parentTable, true, false);
			}

			IFormLookupPanel replacedForm = bLeftForm ? getLeftForm() : getRightForm();
			if (replacedForm != null)
			{
				List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>(0);
				boolean bNotifyVisibleForm = notifyVisibleForm(false, bLeftForm ? 0 : 1, invokeLaterRunnables);
				Utils.invokeLater(application, invokeLaterRunnables);
				if (!bNotifyVisibleForm) return false;
			}

			WebTabFormLookup flp = (WebTabFormLookup)createFormLookupPanel(name, relationName, f.getName());
			flp.setReadOnly(readOnly);

			if (bLeftForm) setLeftForm(flp);
			else setRightForm(flp);
			if (relatedFs != null)
			{
				FormController fp = flp.getWebForm().getController();
				if (fp != null && flp.getRelationName() != null && flp.getRelationName().equals(relationName))
				{
					fp.loadData(relatedFs, null);
				}
			}

			List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>(0);
			boolean bNotifyVisibleForm = notifyVisibleForm(true, bLeftForm ? 0 : 1, invokeLaterRunnables);
			Utils.invokeLater(application, invokeLaterRunnables);
			return bNotifyVisibleForm;
		}
		return false;
	}

	public void setTabIndex(int tabIndex)
	{
		this.tabSequenceIndex = tabIndex;
	}

	public void setFormLastTabIndex(WebForm form, int lastTabIndex)
	{
		if (webTabs[0] != null && form.equals(webTabs[0].getPanel().getWebForm())) leftPanelLastTabIndex = lastTabIndex;
	}

	public void notifyResized()
	{
		for (int i = 0; i < 2; i++)
		{
			if (webTabs[i] != null && webTabs[i].getPanel().isReady())
			{
				WebForm webForm = webTabs[i].getPanel().getWebForm();
				FormController controller = webForm.getController();
				if (controller != null && webForm.isFormWidthHeightChanged())
				{
					controller.notifyResized();
					webForm.clearFormWidthHeightChangedFlag();
				}
			}
		}
	}
}
