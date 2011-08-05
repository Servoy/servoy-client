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

import java.awt.Event;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.Component.IVisitor;
import org.apache.wicket.Page;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.Response;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.AjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.AjaxPostprocessingCallDecorator;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.PageableListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.protocol.http.ClientProperties;
import org.apache.wicket.protocol.http.RequestUtils;
import org.apache.wicket.protocol.http.request.WebClientInfo;

import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.IDisplay;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IFoundSet;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.scripting.IScriptable;
import com.servoy.j2db.scripting.IScriptableProvider;
import com.servoy.j2db.scripting.JSEvent;
import com.servoy.j2db.scripting.JSEvent.EventType;
import com.servoy.j2db.server.headlessclient.MainPage;
import com.servoy.j2db.server.headlessclient.ServoyForm;
import com.servoy.j2db.server.headlessclient.WebClientSession;
import com.servoy.j2db.server.headlessclient.WebClientsApplication.ModifiedAccessStackPageMap;
import com.servoy.j2db.server.headlessclient.WebForm;
import com.servoy.j2db.server.headlessclient.WrapperContainer;
import com.servoy.j2db.server.headlessclient.dataui.WebDataCalendar.DateField;
import com.servoy.j2db.server.headlessclient.dataui.WebDataRadioButton.MyRadioButton;
import com.servoy.j2db.server.headlessclient.dnd.DraggableBehavior;
import com.servoy.j2db.ui.BaseEventExecutor;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.IDataRenderer;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;
import com.servoy.j2db.ui.IScriptBaseMethods;
import com.servoy.j2db.ui.ISupportOnRenderCallback;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

/**
 * The event executor that handles the events for a webclient.
 * 
 * @author jcompagner
 */
public class WebEventExecutor extends BaseEventExecutor
{
	private final Component component;
	private final boolean useAJAX;

	public WebEventExecutor(Component c, boolean useAJAX)
	{
		this.component = c;
		this.useAJAX = useAJAX;

		if (useAJAX && !(component instanceof WebDataHtmlView) && !(component instanceof WebImageBeanHolder) && !(component instanceof ILabel))
		{
			if (component instanceof WebDataRadioChoice || component instanceof WebDataCheckBoxChoice)
			{
				component.add(new ServoyChoiceComponentUpdatingBehavior(component, this));
			}
			else if (component instanceof CheckBox || component instanceof MyRadioButton)
			{
				component.add(new ServoyFormComponentUpdatingBehavior("onclick", component, this)); //$NON-NLS-1$
			}
			else if (component instanceof WebDataLookupField || component instanceof WebDataComboBox || component instanceof DateField) // these fields can change contents without having focus or should generate dataProvider update without loosing focus; for example calendar might modify field content without field having focus
			{
				component.add(new ServoyFormComponentUpdatingBehavior("onchange", component, this)); //$NON-NLS-1$
			}
			else if (!(component instanceof FormComponent< ? >)) // updating FormComponent is handled in focusLost event handler in PageContributor
			{
				Debug.trace("Component didn't get a updating behaviour: " + component); //$NON-NLS-1$
			}
		}
		else if (component instanceof ILabel && useAJAX)
		{
			component.add(new AbstractBehavior()
			{
				private static final long serialVersionUID = 1L;

				@Override
				public void onComponentTag(Component comp, ComponentTag tag)
				{
					CharSequence type = tag.getString("type"); //$NON-NLS-1$
					if (type != null && type.equals("submit")) //$NON-NLS-1$
					{
						// in ajax we can remove the submit. see case 177070
						tag.put("type", "button"); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			});
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.ui.BaseEventExecutor#setValidationEnabled(boolean)
	 */
	@Override
	public void setValidationEnabled(boolean b)
	{
		super.setValidationEnabled(b);
		if (component instanceof IProviderStylePropertyChanges)
		{
			((IProviderStylePropertyChanges)component).getStylePropertyChanges().setChanged();
		}
	}

	/**
	 * @see com.servoy.j2db.ui.BaseEventExecutor#setActionCmd(java.lang.String, Object[])
	 */
	@Override
	public void setActionCmd(String id, Object[] args)
	{
		if (id != null && useAJAX)
		{
			if (!((component instanceof TextField< ? > || component instanceof TextArea< ? >) && component instanceof IDisplay && ((IDisplay)component).isReadOnly()) &&
				!(component instanceof ILabel) && component instanceof FormComponent< ? >)
			{
				component.add(new ServoyActionEventBehavior("onKeyDown", component, this, "ActionCmd")); // please keep the case in the event name //$NON-NLS-1$ //$NON-NLS-2$ 
			}
			else
			{
				component.add(new ServoyAjaxEventBehavior("onclick", "ActionCmd") //$NON-NLS-1$ //$NON-NLS-2$
				{
					private static final long serialVersionUID = 1L;

					@Override
					protected void onEvent(AjaxRequestTarget target)
					{
						WebEventExecutor.this.onEvent(JSEvent.EventType.action, target, component,
							Utils.getAsInteger(RequestCycle.get().getRequest().getParameter(IEventExecutor.MODIFIERS_PARAMETER)),
							new Point(Utils.getAsInteger(RequestCycle.get().getRequest().getParameter("mx")), //$NON-NLS-1$
								Utils.getAsInteger(RequestCycle.get().getRequest().getParameter("my")))); //$NON-NLS-1$

						target.appendJavascript("clearDoubleClickId('" + component.getMarkupId() + "')"); //$NON-NLS-1$ //$NON-NLS-2$
					}

					@Override
					protected CharSequence generateCallbackScript(final CharSequence partialCall)
					{
						return super.generateCallbackScript(partialCall + "+actionParam"); //$NON-NLS-1$
					}

					@Override
					public CharSequence getCallbackUrl(boolean onlyTargetActivePage)
					{
						return super.getCallbackUrl(true);
					}

					@SuppressWarnings("nls")
					@Override
					public boolean isEnabled(Component comp)
					{
						if (super.isEnabled(comp))
						{
							if (comp instanceof IScriptableProvider && ((IScriptableProvider)comp).getScriptObject() instanceof IScriptBaseMethods)
							{
								Object oe = ((IScriptBaseMethods)((IScriptableProvider)comp).getScriptObject()).js_getClientProperty("ajax.enabled");
								if (oe != null) return Utils.getAsBoolean(oe);
							}
							return true;
						}
						return false;
					}

					@Override
					protected IAjaxCallDecorator getAjaxCallDecorator()
					{
						return new AjaxPostprocessingCallDecorator(null)
						{
							private static final long serialVersionUID = 1L;

							@SuppressWarnings("nls")
							@Override
							public CharSequence postDecorateScript(CharSequence script)
							{
								return "var actionParam = Servoy.Utils.getActionParams(event); Servoy.Utils.startClickTimer(function() { if (testDoubleClickId('" +
									component.getMarkupId() + "')) { " + script + "}; Servoy.Utils.clickTimerRunning = false; return false; });";
							}
						};
					}
				});
			}
		}
		super.setActionCmd(id, args);
	}

	@Override
	public void setDoubleClickCmd(String id, Object[] args)
	{
		if (id != null && useAJAX)
		{
			if (component instanceof ILabel)
			{
				component.add(new ServoyAjaxEventBehavior("ondblclick", "Cmd") //$NON-NLS-1$ //$NON-NLS-2$
				{
					@Override
					protected void onEvent(AjaxRequestTarget target)
					{
						WebEventExecutor.this.onEvent(JSEvent.EventType.doubleClick, target, component,
							Utils.getAsInteger(RequestCycle.get().getRequest().getParameter(IEventExecutor.MODIFIERS_PARAMETER)),
							new Point(Utils.getAsInteger(RequestCycle.get().getRequest().getParameter("mx")), //$NON-NLS-1$
								Utils.getAsInteger(RequestCycle.get().getRequest().getParameter("my")))); //$NON-NLS-1$
					}

					@SuppressWarnings("nls")
					@Override
					protected CharSequence generateCallbackScript(final CharSequence partialCall)
					{
						return super.generateCallbackScript(partialCall + "+Servoy.Utils.getActionParams(event)"); //$NON-NLS-1$
					}

					@SuppressWarnings("nls")
					@Override
					public boolean isEnabled(Component comp)
					{
						if (super.isEnabled(comp))
						{
							if (comp instanceof IScriptableProvider && ((IScriptableProvider)comp).getScriptObject() instanceof IScriptBaseMethods)
							{
								Object oe = ((IScriptBaseMethods)((IScriptableProvider)comp).getScriptObject()).js_getClientProperty("ajax.enabled");
								if (oe != null) return Utils.getAsBoolean(oe);
							}
							return true;
						}
						return false;
					}

					@Override
					protected IAjaxCallDecorator getAjaxCallDecorator()
					{
						return new AjaxPostprocessingCallDecorator(null)
						{
							private static final long serialVersionUID = 1L;

							@SuppressWarnings("nls")
							@Override
							public CharSequence postDecorateScript(CharSequence script)
							{
								return "Servoy.Utils.stopClickTimer();" + script + "return !" + IAjaxCallDecorator.WICKET_CALL_RESULT_VAR + ";";
							}
						};
					}
				});
			}
		}
		super.setDoubleClickCmd(id, args);
	}

	@Override
	public void setRightClickCmd(String id, Object[] args)
	{
		if (id != null && useAJAX)
		{
			if (component instanceof ILabel || component instanceof IFieldComponent || component instanceof SortableCellViewHeader)
			{
				component.add(new ServoyAjaxEventBehavior("oncontextmenu") //$NON-NLS-1$
				{
					@Override
					protected void onEvent(AjaxRequestTarget target)
					{
						WebEventExecutor.this.onEvent(JSEvent.EventType.rightClick, target, component,
							Utils.getAsInteger(RequestCycle.get().getRequest().getParameter(IEventExecutor.MODIFIERS_PARAMETER)),
							new Point(Utils.getAsInteger(RequestCycle.get().getRequest().getParameter("mx")), //$NON-NLS-1$
								Utils.getAsInteger(RequestCycle.get().getRequest().getParameter("my")))); //$NON-NLS-1$
					}

					@Override
					protected CharSequence generateCallbackScript(final CharSequence partialCall)
					{
						return super.generateCallbackScript(partialCall + "+Servoy.Utils.getActionParams(event)"); //$NON-NLS-1$
					}

					@Override
					public boolean isEnabled(Component comp)
					{
						if (super.isEnabled(comp))
						{
							if (comp instanceof IScriptableProvider && ((IScriptableProvider)comp).getScriptObject() instanceof IScriptBaseMethods)
							{
								Object oe = ((IScriptBaseMethods)((IScriptableProvider)comp).getScriptObject()).js_getClientProperty("ajax.enabled"); //$NON-NLS-1$
								if (oe != null) return Utils.getAsBoolean(oe);
							}
							return true;
						}
						return false;
					}

					// We need to return false, otherwise the context menu of the browser is displayed.
					@Override
					protected IAjaxCallDecorator getAjaxCallDecorator()
					{
						return new AjaxCallDecorator()
						{
							@Override
							public CharSequence decorateScript(CharSequence script)
							{
								return script + " return false;"; //$NON-NLS-1$
							}
						};
					}
				});
			}
		}
		super.setRightClickCmd(id, args);
	}

	public void onEvent(EventType type, AjaxRequestTarget target, Component comp, int webModifiers)
	{
		onEvent(type, target, comp, webModifiers, null);
	}

	public void onEvent(EventType type, AjaxRequestTarget target, Component comp, int webModifiers, Point mouseLocation)
	{
		ServoyForm form = comp.findParent(ServoyForm.class);
		if (form == null)
		{
			return;
		}

		Page page = form.getPage(); // JS might change the page this form belongs to... so remember it now
		WebClientSession.get().getWebClient().executeEvents(); // process model changes from web components

		boolean compHasOnRender = false;
		if (comp instanceof IScriptableProvider)
		{
			IScriptable s = ((IScriptableProvider)comp).getScriptObject();
			if (s instanceof ISupportOnRenderCallback)
			{
				compHasOnRender = ((ISupportOnRenderCallback)s).getRenderEventExecutor().hasRenderCallback();
			}
		}

		if (compHasOnRender && comp instanceof IProviderStylePropertyChanges)
		{
			if (type == EventType.focusGained && page instanceof MainPage)
			{
				((MainPage)page).setFocusedComponent(comp);
				((IProviderStylePropertyChanges)comp).getStylePropertyChanges().setChanged();
			}
			else if (type == EventType.focusLost)
			{
				((MainPage)page).setFocusedComponent(null);
				((IProviderStylePropertyChanges)comp).getStylePropertyChanges().setChanged();
			}
		}


		if (type == EventType.focusLost || setSelectedIndex(comp, target, convertModifiers(webModifiers), type == EventType.focusGained))
		{
			if (skipFireFocusGainedCommand && type.equals(JSEvent.EventType.focusGained))
			{
				skipFireFocusGainedCommand = false;
			}
			else
			{
				switch (type)
				{
					case action :
						fireActionCommand(false, comp, convertModifiers(webModifiers), mouseLocation);
						break;
					case focusGained :
						fireEnterCommands(false, comp, convertModifiers(webModifiers));
						break;
					case focusLost :
						fireLeaveCommands(comp, false, convertModifiers(webModifiers));
						break;
					case doubleClick :
						fireDoubleclickCommand(false, comp, convertModifiers(webModifiers), mouseLocation);
						break;
					case rightClick :
						// if right click, mark the meta flag as it is on the smart client
						fireRightclickCommand(false, comp, convertModifiers(webModifiers | 8), mouseLocation);
						break;
					case none :
					case dataChange :
					case form :
					case onDrag :
					case onDragOver :
					case onDrop :
				}
			}
		}
		if (target != null)
		{
			generateResponse(target, page);
		}
	}

	/**
	 * Convert JS modifiers to AWT/Swing modifiers (used by Servoy event)
	 * 
	 * @param webModifiers
	 * @return
	 */
	public static int convertModifiers(int webModifiers)
	{
		if (webModifiers == IEventExecutor.MODIFIERS_UNSPECIFIED) return IEventExecutor.MODIFIERS_UNSPECIFIED;

		// see function Servoy.Utils.getModifiers() in servoy.js
		int awtModifiers = 0;
		if ((webModifiers & 1) != 0) awtModifiers |= Event.CTRL_MASK;
		if ((webModifiers & 2) != 0) awtModifiers |= Event.SHIFT_MASK;
		if ((webModifiers & 4) != 0) awtModifiers |= Event.ALT_MASK;
		if ((webModifiers & 8) != 0) awtModifiers |= Event.META_MASK;

		return awtModifiers;
	}


	public void onError(AjaxRequestTarget target, Component comp)
	{
		if (target == null)
		{
			return;
		}
		ServoyForm form = comp.findParent(ServoyForm.class);
		if (form == null)
		{
			return;
		}
		generateResponse(target, form.getPage());
	}

	@SuppressWarnings("nls")
	public static boolean setSelectedIndex(Component component, AjaxRequestTarget target, int modifiers)
	{
		return setSelectedIndex(component, target, modifiers, false);
	}

	/**
	 * @param component
	 */
	@SuppressWarnings("nls")
	public static boolean setSelectedIndex(Component component, AjaxRequestTarget target, int modifiers, boolean bHandleMultiselect)
	{
		//search for recordItem model
		Component recordItemModelComponent = component;
		IModel< ? > someModel = recordItemModelComponent.getDefaultModel();
		while (!(someModel instanceof RecordItemModel))
		{
			recordItemModelComponent = recordItemModelComponent.getParent();
			if (recordItemModelComponent == null) break;
			someModel = recordItemModelComponent.getDefaultModel();
		}

		if (someModel instanceof RecordItemModel)
		{
			// update the last rendered value for the events component (if updated)
			((RecordItemModel)someModel).updateRenderedValue(component);

			IRecordInternal rec = (IRecordInternal)someModel.getObject();
			if (rec != null)
			{
				int index;
				IFoundSetInternal fs = rec.getParentFoundSet();
				if (someModel instanceof FoundsetRecordItemModel)
				{
					index = ((FoundsetRecordItemModel)someModel).getRowIndex();
				}
				else
				{
					index = fs.getRecordIndex(rec); // this is used only on "else", because a "plugins.rawSQL.flushAllClientsCache" could result in index = -1 although the record has not changed (but record & underlying row instances changed)
				}

				if (fs instanceof FoundSet && ((FoundSet)fs).isMultiSelect())
				{
					//set the selected record
					ClientProperties clp = ((WebClientInfo)Session.get().getClientInfo()).getProperties();
					String navPlatform = clp.getNavigatorPlatform();
					int controlMask = (navPlatform != null && navPlatform.toLowerCase().indexOf("mac") != -1) ? Event.META_MASK : Event.CTRL_MASK;

					boolean toggle = (modifiers != MODIFIERS_UNSPECIFIED) && ((modifiers & controlMask) != 0);
					boolean extend = (modifiers != MODIFIERS_UNSPECIFIED) && ((modifiers & Event.SHIFT_MASK) != 0);
					boolean meta = (modifiers != MODIFIERS_UNSPECIFIED) && ((modifiers & Event.META_MASK) != 0);

					if (toggle || extend)
					{
						if (bHandleMultiselect)
						{
							if (toggle)
							{
								int[] selectedIndexes = ((FoundSet)fs).getSelectedIndexes();
								ArrayList<Integer> selectedIndexesA = new ArrayList<Integer>();
								Integer selectedIndex = new Integer(index);

								for (int selected : selectedIndexes)
									selectedIndexesA.add(new Integer(selected));
								if (selectedIndexesA.indexOf(selectedIndex) != -1)
								{
									if (selectedIndexesA.size() > 1) selectedIndexesA.remove(selectedIndex);
								}
								else selectedIndexesA.add(selectedIndex);
								selectedIndexes = new int[selectedIndexesA.size()];
								for (int i = 0; i < selectedIndexesA.size(); i++)
									selectedIndexes[i] = selectedIndexesA.get(i).intValue();
								((FoundSet)fs).setSelectedIndexes(selectedIndexes);
							}
							else if (extend)
							{
								int anchor = ((FoundSet)fs).getSelectedIndex();
								int min = Math.min(anchor, index);
								int max = Math.max(anchor, index);

								int[] newSelectedIndexes = new int[max - min + 1];
								for (int i = min; i <= max; i++)
									newSelectedIndexes[i - min] = i;
								((FoundSet)fs).setSelectedIndexes(newSelectedIndexes);
							}
						}
					}
					else if (!meta) // if left click, just set the new selection
					{
						fs.setSelectedIndex(index);
					}
				}
				else if (!isIndexSelected(fs, index)) fs.setSelectedIndex(index);
				if (!isIndexSelected(fs, index) && !(fs instanceof FoundSet && ((FoundSet)fs).isMultiSelect()))
				{
					// setSelectedIndex failed, probably due to validation failed, do a blur()
					if (target != null) target.appendJavascript("var toBlur = document.getElementById(\"" + component.getMarkupId() +
						"\");if (toBlur) toBlur.blur();");
					return false;
				}
			}
		}
		return true;
	}

	private static boolean isIndexSelected(IFoundSet fs, int index)
	{
		if (fs instanceof FoundSet)
		{
			FoundSet fsObj = (FoundSet)fs;
			for (int selectedIdx : fsObj.getSelectedIndexes())
			{
				if (selectedIdx == index) return true;
			}
		}
		return fs.getSelectedIndex() == index;
	}

	@SuppressWarnings("nls")
	public static void generateResponse(final AjaxRequestTarget target, Page page)
	{
		if (target != null && page instanceof MainPage && WebClientSession.get().getWebClient().getSolution() != null)
		{
			// do executed the events for before generating the response.
			WebClientSession.get().getWebClient().executeEvents();

			final MainPage mainPage = ((MainPage)page);

			if (mainPage.getPageMap() instanceof ModifiedAccessStackPageMap)
			{
				// at every request mark the pagemap as dirty so lru eviction really works
				((ModifiedAccessStackPageMap)mainPage.getPageMap()).flagDirty();
			}
			// PageContributor installs focus/blur event handlers for changed/new components
			target.addListener(mainPage.getPageContributor());

			// If the main form is switched then do a normal redirect.
			if (mainPage.isMainFormSwitched())
			{
				page.ignoreVersionMerge();
				RequestCycle.get().setResponsePage(page);
			}

			else
			{
				page.visitChildren(WebTabPanel.class, new Component.IVisitor<WebTabPanel>()
				{
					public Object component(WebTabPanel component)
					{
						component.initalizeFirstTab();
						return IVisitor.CONTINUE_TRAVERSAL;
					}
				});

				final List<String> recreatedIDs = new ArrayList<String>();
				page.visitChildren(WebForm.class, new Component.IVisitor<WebForm>()
				{
					/**
					 * @see org.apache.wicket.Component.IVisitor#component(org.apache.wicket.Component)
					 */
					public Object component(WebForm component)
					{
						if (component.isVisibleInHierarchy() && component.isUIRecreated())
						{
							recreatedIDs.add(component.getMarkupId());
							return IVisitor.CONTINUE_TRAVERSAL;
						}
						return IVisitor.CONTINUE_TRAVERSAL;
					}
				});
				if (recreatedIDs.size() > 0)
				{
					page.ignoreVersionMerge();
					StringBuffer argument = new StringBuffer();
					argument.append("\"");
					argument.append(RequestUtils.toAbsolutePath(RequestCycle.get().urlFor(page).toString()));
					argument.append("\"");
					argument.append(",");
					for (String id : recreatedIDs)
					{
						argument.append("\"");
						argument.append(id);
						argument.append("\"");
						if (recreatedIDs.indexOf(id) != recreatedIDs.size() - 1)
						{
							argument.append(",");
						}
					}
					target.appendJavascript("Servoy.Utils.redirectKeepingScrolls(" + argument + ");");
					return;
				}

				mainPage.addWebAnchoringInfoIfNeeded(true);

				final Set<WebCellBasedView> tableViewsToRender = new HashSet<WebCellBasedView>();
				final List<String> valueChangedIds = new ArrayList<String>();
				final List<String> invalidValueIds = new ArrayList<String>();
				page.visitChildren(IProviderStylePropertyChanges.class, new Component.IVisitor<Component>()
				{
					public Object component(Component component)
					{
						if (component instanceof IDisplayData && !((IDisplayData)component).isValueValid())
						{
							invalidValueIds.add(component.getMarkupId());
						}
						if (((IProviderStylePropertyChanges)component).getStylePropertyChanges().isValueChanged())
						{
							valueChangedIds.add(component.getMarkupId());
						}
						if (((IProviderStylePropertyChanges)component).getStylePropertyChanges().isChanged())
						{
							if (component.getParent().isVisibleInHierarchy())
							{
								target.addComponent(component);
								generateDragAttach(component, target.getHeaderResponse());
								if (!component.isVisible())
								{
									((IProviderStylePropertyChanges)component).getStylePropertyChanges().setRendered();
								}
							}
							return IVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
						}
						else if (component instanceof WebCellBasedView) tableViewsToRender.add((WebCellBasedView)component);
						return component.isVisible() ? IVisitor.CONTINUE_TRAVERSAL : IVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
					}
				});

				String rowSelectionScript, columnResizeScript;
				for (WebCellBasedView wcbv : tableViewsToRender)
				{
					rowSelectionScript = wcbv.getRowSelectionScript();
					wcbv.updateRowComponentsRenderState(target);
					if (rowSelectionScript != null) target.appendJavascript(rowSelectionScript);
					columnResizeScript = wcbv.getColumnResizeScript();
					if (columnResizeScript != null) target.appendJavascript(columnResizeScript);
				}

				// double check if the page contributor is changed, because the above IStylePropertyChanges ischanged could have altered it.
				if (mainPage.getPageContributor().getStylePropertyChanges().isChanged())
				{
					target.addComponent((Component)mainPage.getPageContributor());
				}
				if (invalidValueIds.size() == 0)
				{
					target.appendJavascript("setValidationFailed(null);"); //$NON-NLS-1$
				}
				else
				{
					target.appendJavascript("setValidationFailed('" + invalidValueIds.get(0) + "');"); //$NON-NLS-1$
				}
				Component comp = mainPage.getAndResetToFocusComponent();
				if (comp != null)
				{
					target.focusComponent(comp);
				}
				else if (mainPage.getAndResetMustFocusNull())
				{
					// This is needed for example when showing a non-modal dialog in IE7 (or otherwise
					// the new window would be displayed in the background).
					target.focusComponent(null);
				}
				StringBuffer argument = new StringBuffer();
				for (String id : valueChangedIds)
				{
					argument.append("\"");
					argument.append(id);
					argument.append("\"");
					if (valueChangedIds.indexOf(id) != valueChangedIds.size() - 1)
					{
						argument.append(",");
					}
				}
				target.prependJavascript("storeValueBeforeUpdate(" + argument + ");");
				target.appendJavascript("restoreValueAfterUpdate();");

				//if we have admin info, show it
				String adminInfo = mainPage.getAdminInfo();
				if (adminInfo != null)
				{
					adminInfo = Utils.stringReplace(adminInfo, "\r", "");
					adminInfo = Utils.stringReplace(adminInfo, "\n", "\\n");
					target.appendJavascript("alert('Servoy admin info : " + adminInfo + "');");
				}

				// If we have a status text, set it.
				String statusText = mainPage.getStatusText();
				if (statusText != null)
				{
					target.appendJavascript("setStatusText('" + statusText + "');");
				}

				String show = mainPage.getShowUrlScript();
				if (show != null)
				{
					target.appendJavascript(show);
				}

				mainPage.renderJavascriptChanges(target);

				try
				{
					if (((WebClientInfo)page.getSession().getClientInfo()).getProperties().isBrowserInternetExplorer())
					{
						target.appendJavascript("testStyleSheets();");
					}
				}
				catch (Exception e)
				{
					Debug.error(e);//cannot retrieve session/clientinfo/properties?
					target.appendJavascript("testStyleSheets();");
				}
			}
		}
	}

	@Override
	protected String getFormName(Object display)
	{
		WebForm form = ((Component)display).findParent(WebForm.class);
		if (form == null)
		{
			return null;
		}
		return form.getController().getName();
	}

	/**
	 * @param component2
	 * @param response
	 */
	@SuppressWarnings("nls")
	public static void generateDragAttach(Component component, IHeaderResponse response)
	{
		DraggableBehavior draggableBehavior = null;
		Component behaviorComponent = component;

		if ((behaviorComponent instanceof IComponent) && !(behaviorComponent instanceof IDataRenderer))
		{
			behaviorComponent = (Component)component.findParent(IDataRenderer.class);
		}
		if (behaviorComponent != null)
		{
			Iterator<IBehavior> behaviors = behaviorComponent.getBehaviors().iterator();
			Object behavior;
			while (behaviors.hasNext())
			{
				behavior = behaviors.next();
				if (behavior instanceof DraggableBehavior)
				{
					draggableBehavior = (DraggableBehavior)behavior;
					break;
				}
			}
		}

		if (draggableBehavior == null) return;

		boolean bUseProxy = draggableBehavior.isUseProxy();
		boolean bResizeProxyFrame = draggableBehavior.isResizeProxyFrame();
		boolean bXConstraint = draggableBehavior.isXConstraint();
		boolean bYConstraint = draggableBehavior.isYConstraint();
		CharSequence dragUrl = draggableBehavior.getCallbackUrl();

		String jsCode = null;

		if (behaviorComponent instanceof IDataRenderer)
		{
			final StringBuilder sbAttachDrag = new StringBuilder(100);
			sbAttachDrag.append("Servoy.DD.attachDrag([");
			final StringBuilder sbAttachDrop = new StringBuilder(100);
			sbAttachDrop.append("Servoy.DD.attachDrop([");

			if (behaviorComponent instanceof WebDataRenderer)
			{
				boolean hasDragEvent = ((WebDataRenderer)behaviorComponent).getDragNDropController().getForm().getOnDragMethodID() > 0 ||
					((WebDataRenderer)behaviorComponent).getDragNDropController().getForm().getOnDragOverMethodID() > 0;
				boolean hasDropEvent = ((WebDataRenderer)behaviorComponent).getDragNDropController().getForm().getOnDropMethodID() > 0;

				if (component instanceof WebDataRenderer)
				{
					if (hasDragEvent) sbAttachDrag.append('\'').append(component.getMarkupId()).append("',");
					if (hasDropEvent) sbAttachDrop.append('\'').append(component.getMarkupId()).append("',");

					Iterator< ? extends Component> dataRendererIte = ((WebDataRenderer)component).iterator();
					Object dataRendererChild;
					while (dataRendererIte.hasNext())
					{
						dataRendererChild = dataRendererIte.next();
						if (dataRendererChild instanceof WebTabPanel || dataRendererChild instanceof WebSplitPane) continue;
						if (dataRendererChild instanceof WrapperContainer) dataRendererChild = ((WrapperContainer)dataRendererChild).getDelegate();
						if (dataRendererChild instanceof IComponent && ((IComponent)dataRendererChild).isEnabled())
						{
							StringBuilder sb = null;
							if (hasDragEvent &&
								(dataRendererChild instanceof WebBaseLabel || ((dataRendererChild instanceof IDisplay) && ((IDisplay)dataRendererChild).isReadOnly()))) sb = sbAttachDrag;
							else if (hasDropEvent) sb = sbAttachDrop;

							if (sb != null)
							{
								sb.append('\'');
								sb.append(((Component)dataRendererChild).getMarkupId());
								sb.append("',");
							}
						}
					}
				}
				else if (component != null && component.isEnabled())
				{
					StringBuilder sb = null;

					if (hasDragEvent && (component instanceof WebBaseLabel || ((component instanceof IDisplay) && ((IDisplay)component).isReadOnly()))) sb = sbAttachDrag;
					else if (hasDropEvent) sb = sbAttachDrop;

					if (sb != null)
					{
						sb.append('\'');
						sb.append(component.getMarkupId());
						sb.append("',");
					}
				}
			}
			else if (behaviorComponent instanceof WebCellBasedView)
			{
				final boolean hasDragEvent = ((WebCellBasedView)behaviorComponent).getDragNDropController().getForm().getOnDragMethodID() > 0 ||
					((WebCellBasedView)behaviorComponent).getDragNDropController().getForm().getOnDragOverMethodID() > 0;
				final boolean hasDropEvent = ((WebCellBasedView)behaviorComponent).getDragNDropController().getForm().getOnDropMethodID() > 0;

				if (component instanceof WebCellBasedView)
				{
					if (hasDragEvent) sbAttachDrag.append('\'').append(component.getMarkupId()).append("',");
					if (hasDropEvent) sbAttachDrop.append('\'').append(component.getMarkupId()).append("',");

					PageableListView<IRecordInternal> table = ((WebCellBasedView)component).getTable();
					table.visitChildren(new IVisitor<Component>()
					{
						public Object component(Component comp)
						{
							if (comp instanceof IComponent && comp.isEnabled())
							{
								StringBuilder sb = null;

								if (hasDragEvent && (comp instanceof WebBaseLabel || ((comp instanceof IDisplay) && ((IDisplay)comp).isReadOnly()))) sb = sbAttachDrag;
								else if (hasDropEvent) sb = sbAttachDrop;

								if (sb != null)
								{
									sb.append('\'');
									sb.append(comp.getMarkupId());
									sb.append("',");
								}
							}
							return null;
						}

					});
				}
				else if (component != null && component.isEnabled())
				{
					StringBuilder sb = null;

					if (hasDragEvent && (component instanceof WebBaseLabel || ((component instanceof IDisplay) && ((IDisplay)component).isReadOnly()))) sb = sbAttachDrag;
					else if (hasDropEvent) sb = sbAttachDrop;

					if (sb != null)
					{
						sb.append('\'');
						sb.append(component.getMarkupId());
						sb.append("',");
					}
				}
			}

			if (sbAttachDrag.length() > 25)
			{
				sbAttachDrag.setLength(sbAttachDrag.length() - 1);
				sbAttachDrag.append("],'");
				sbAttachDrag.append(dragUrl);
				sbAttachDrag.append("', ");
				sbAttachDrag.append(bUseProxy);
				sbAttachDrag.append(", ");
				sbAttachDrag.append(bResizeProxyFrame);
				sbAttachDrag.append(", ");
				sbAttachDrag.append(bXConstraint);
				sbAttachDrag.append(", ");
				sbAttachDrag.append(bYConstraint);
				sbAttachDrag.append(");");

				jsCode = sbAttachDrag.toString();
			}

			if (sbAttachDrop.length() > 25)
			{
				sbAttachDrop.setLength(sbAttachDrop.length() - 1);
				sbAttachDrop.append("],'");
				sbAttachDrop.append(dragUrl);
				sbAttachDrop.append("');");

				if (jsCode != null) jsCode += '\n' + sbAttachDrop.toString();
				else jsCode = sbAttachDrop.toString();
			}

			if (jsCode != null)
			{
				if (response == null)
				{
					jsCode = (new StringBuilder().append("\n<script type=\"text/javascript\">\n").append(jsCode).append("</script>\n")).toString();
					Response cyleResponse = RequestCycle.get().getResponse();
					cyleResponse.write(jsCode);
				}
				else response.renderOnDomReadyJavascript(jsCode);
			}
		}
		else
		//default handling 
		{
			jsCode = "Servoy.DD.attachDrag(['" + component.getMarkupId() + "'],'" + dragUrl + "', " + bUseProxy + ", " + bResizeProxyFrame + ", " +
				bXConstraint + ", " + bYConstraint + ")";
			if (response == null)
			{
				jsCode = (new StringBuilder().append("\n<script type=\"text/javascript\">\n").append(jsCode).append("</script>\n")).toString();
				Response cyleResponse = RequestCycle.get().getResponse();

				cyleResponse.write(jsCode);
			}
			else response.renderOnDomReadyJavascript(jsCode);
		}
	}

	@Override
	protected String getElementName(Object display)
	{
		String name = super.getElementName(display);
		if (name == null && display instanceof SortableCellViewHeader)
		{
			name = ((SortableCellViewHeader)display).getName();
		}
		return name;
	}

	@Override
	protected Object getSource(Object display)
	{
		return display instanceof SortableCellViewHeader ? null : super.getSource(display);
	}
}