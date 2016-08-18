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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.Component.IVisitor;
import org.apache.wicket.MarkupContainer;
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
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.protocol.http.ClientProperties;
import org.apache.wicket.protocol.http.request.WebClientInfo;

import com.servoy.base.scripting.api.IJSEvent.EventType;
import com.servoy.j2db.FormController;
import com.servoy.j2db.IForm;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.IDisplay;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IFoundSet;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dnd.IFormDataDragNDrop;
import com.servoy.j2db.scripting.IScriptable;
import com.servoy.j2db.scripting.IScriptableProvider;
import com.servoy.j2db.scripting.JSEvent;
import com.servoy.j2db.server.headlessclient.CloseableAjaxRequestTarget;
import com.servoy.j2db.server.headlessclient.MainPage;
import com.servoy.j2db.server.headlessclient.ServoyForm;
import com.servoy.j2db.server.headlessclient.WebClientSession;
import com.servoy.j2db.server.headlessclient.WebClientsApplication.ModifiedAccessStackPageMap;
import com.servoy.j2db.server.headlessclient.WebForm;
import com.servoy.j2db.server.headlessclient.WebOnRenderHelper;
import com.servoy.j2db.server.headlessclient.WrapperContainer;
import com.servoy.j2db.server.headlessclient.dataui.WebCellBasedView.WebCellBasedViewListViewItem;
import com.servoy.j2db.server.headlessclient.dataui.WebDataCompositeTextField.AugmentedTextField;
import com.servoy.j2db.server.headlessclient.dataui.WebDataImgMediaField.ImageDisplay;
import com.servoy.j2db.server.headlessclient.dnd.DraggableBehavior;
import com.servoy.j2db.server.headlessclient.eventthread.IEventDispatcher;
import com.servoy.j2db.server.headlessclient.eventthread.WicketEvent;
import com.servoy.j2db.ui.BaseEventExecutor;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;
import com.servoy.j2db.ui.ISupportOnRender;
import com.servoy.j2db.ui.ISupportOnRenderCallback;
import com.servoy.j2db.ui.RenderEventExecutor;
import com.servoy.j2db.ui.runtime.IRuntimeComponent;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ISupplyFocusChildren;
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
	private IBehavior updatingBehavior;

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
			else if (component instanceof WebBaseSelectBox.ISelector)
			{
				updatingBehavior = new ServoySelectBoxUpdatingBehavior("onclick", ((WebBaseSelectBox.ISelector)component).getSelectBox(), this, "FormUpdate"); //$NON-NLS-1$ //$NON-NLS-2$
				component.add(updatingBehavior);
			}
			else if (component instanceof WebDataLookupField || component instanceof WebDataComboBox || component instanceof AugmentedTextField ||
				component instanceof WebDataListBox) // these fields can change contents without having focus or should generate dataProvider update without loosing focus; for example calendar&spinner might modify field content without field having focus
			{
				component.add(new ServoyFormComponentUpdatingBehavior("onchange", component, this, "FormUpdate")); //$NON-NLS-1$ //$NON-NLS-2$
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
				!(component instanceof ILabel) && !(component instanceof WebBaseSelectBox.ISelector) && component instanceof FormComponent< ? >)
			{
				component.add(new ServoyActionEventBehavior("onKeyDown", component, this, "ActionCmd")); // please keep the case in the event name //$NON-NLS-1$ //$NON-NLS-2$
			}
			else if (component instanceof WebBaseSelectBox.ISelector)
			{
				((ServoySelectBoxUpdatingBehavior)updatingBehavior).setFireActionCommand(true);
			}
			else
			{
				// for ImageDisplay (that is an input with type='image') 'onclick' cannot be used, as it considered a submit button and any
				// enter inside the input's form will fire the 'onclick' - as workaround, we use 'onmouseup'
				component.add(new ServoyAjaxEventBehavior(component instanceof ImageDisplay ? "onmouseup" : "onclick", "ActionCmd") //$NON-NLS-1$ //$NON-NLS-2$
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
							if (comp instanceof IScriptableProvider && ((IScriptableProvider)comp).getScriptObject() instanceof IRuntimeComponent)
							{
								Object oe = ((IRuntimeComponent)((IScriptableProvider)comp).getScriptObject()).getClientProperty("ajax.enabled");
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
								String functionScript = "if (testDoubleClickId('" + component.getMarkupId() + "')) { " + script + "};";
								return "var actionParam = Servoy.Utils.getActionParams(event,false); " +
									(hasDoubleClickCmd() ? "Servoy.Utils.startClickTimer(function() { " + functionScript +
										" Servoy.Utils.clickTimerRunning = false; return false; });" : functionScript);
							}
						};
					}

					@Override
					protected String getJSEventName()
					{
						String jsEventName = super.getJSEventName();
						return hasDoubleClickCmd() ? jsEventName + "WithDblClick" : jsEventName; //$NON-NLS-1$
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
						return super.generateCallbackScript(partialCall + "+Servoy.Utils.getActionParams(event,false)"); //$NON-NLS-1$
					}

					@SuppressWarnings("nls")
					@Override
					public boolean isEnabled(Component comp)
					{
						if (super.isEnabled(comp))
						{
							if (comp instanceof IScriptableProvider && ((IScriptableProvider)comp).getScriptObject() instanceof IRuntimeComponent)
							{
								Object oe = ((IRuntimeComponent)((IScriptableProvider)comp).getScriptObject()).getClientProperty("ajax.enabled");
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
				String sharedName = "Cmd";
				if (component instanceof SortableCellViewHeader)
				{
					sharedName = null;
				}
				component.add(new ServoyAjaxEventBehavior("oncontextmenu", sharedName, true) //$NON-NLS-1$
				{
					@Override
					protected void onEvent(AjaxRequestTarget target)
					{
						WebEventExecutor.this.onEvent(
							JSEvent.EventType.rightClick,
							target,
							component,
							Utils.getAsInteger(RequestCycle.get().getRequest().getParameter(IEventExecutor.MODIFIERS_PARAMETER)),
							new Point(Utils.getAsInteger(RequestCycle.get().getRequest().getParameter("mx")), //$NON-NLS-1$
								Utils.getAsInteger(RequestCycle.get().getRequest().getParameter("my"))), new Point(Utils.getAsInteger(RequestCycle.get().getRequest().getParameter("glx")), //$NON-NLS-1$
								Utils.getAsInteger(RequestCycle.get().getRequest().getParameter("gly")))); //$NON-NLS-1$
					}

					@Override
					protected CharSequence generateCallbackScript(final CharSequence partialCall)
					{
						return super.generateCallbackScript(partialCall +
							"+Servoy.Utils.getActionParams(event," + ((component instanceof SortableCellViewHeader) ? "true" : "false") + ")"); //$NON-NLS-1$
					}

					@Override
					public boolean isEnabled(Component comp)
					{
						if (super.isEnabled(comp))
						{
							if (comp instanceof IScriptableProvider && ((IScriptableProvider)comp).getScriptObject() instanceof IRuntimeComponent)
							{
								Object oe = ((IRuntimeComponent)((IScriptableProvider)comp).getScriptObject()).getClientProperty("ajax.enabled"); //$NON-NLS-1$
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
		onEvent(type, target, comp, webModifiers, mouseLocation, null);
	}

	public void onEvent(final EventType type, final AjaxRequestTarget target, final Component comp, final int webModifiers, final Point mouseLocation,
		final Point absoluteMouseLocation)
	{
		ServoyForm form = comp.findParent(ServoyForm.class);
		if (form == null)
		{
			return;
		}

		final Page page = form.getPage(); // JS might change the page this form belongs to... so remember it now

		IEventDispatcher<WicketEvent> eventDispatcher = WebClientSession.get().getWebClient().getEventDispatcher();
		if (eventDispatcher != null)
		{
			eventDispatcher.addEvent(new WicketEvent(WebClientSession.get().getWebClient(), new Runnable()
			{
				public void run()
				{
					handleEvent(type, target, comp, webModifiers, mouseLocation, absoluteMouseLocation, page);
				}
			}));
		}
		else
		{
			handleEvent(type, target, comp, webModifiers, mouseLocation, absoluteMouseLocation, page);
		}
		if (target != null)
		{
			generateResponse(target, page);
		}
	}

	/**
	 * @param type
	 * @param target
	 * @param comp
	 * @param webModifiers
	 * @param mouseLocation
	 * @param page
	 */
	private void handleEvent(EventType type, AjaxRequestTarget target, Component comp, int webModifiers, Point mouseLocation, Point absoluteMouseLocation,
		Page page)
	{
		WebClientSession.get().getWebClient().executeEvents(); // process model changes from web components

		Component renderScriptProvider = comp;
		ISupplyFocusChildren< ? > componentWithChildren = renderScriptProvider.findParent(ISupplyFocusChildren.class);
		if (componentWithChildren != null) renderScriptProvider = (Component)componentWithChildren;

		RenderEventExecutor renderEventExecutor = null;
		if (renderScriptProvider instanceof IScriptableProvider)
		{
			IScriptable s = ((IScriptableProvider)renderScriptProvider).getScriptObject();
			if (s instanceof ISupportOnRenderCallback)
			{
				renderEventExecutor = ((ISupportOnRenderCallback)s).getRenderEventExecutor();
				if (!renderEventExecutor.hasRenderCallback()) renderEventExecutor = null;
			}
		}

		if (type == EventType.focusGained || type == EventType.action || type == EventType.focusLost)
		{
			if (type == EventType.focusGained || type == EventType.action)
			{
				((MainPage)page).setFocusedComponent(comp);
			}
			else
			{
				((MainPage)page).setFocusedComponent(null);
			}
			if (renderEventExecutor != null)
			{
				renderEventExecutor.setRenderStateChanged();
				// if component's onRender did not change any properties, don't add it to the target
				if (comp instanceof ISupportOnRender && WebOnRenderHelper.doRender((ISupportOnRender)comp))
				{
					target.addComponent(comp);
				}
			}
		}

		if (type == EventType.focusLost ||
			setSelectedIndex(comp, target, convertModifiers(webModifiers), type == EventType.focusGained || type == EventType.action))
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
						fireRightclickCommand(false, comp, convertModifiers(webModifiers | 8), null, mouseLocation, absoluteMouseLocation);
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
		WebForm parentForm = component.findParent(WebForm.class);
		WebCellBasedView tableView = null;
		if (parentForm != null)
		{
			int parentFormViewType = parentForm.getController().getForm().getView();
			if (parentFormViewType == FormController.TABLE_VIEW || parentFormViewType == FormController.LOCKED_TABLE_VIEW ||
				parentFormViewType == IForm.LIST_VIEW || parentFormViewType == FormController.LOCKED_LIST_VIEW)
			{
				tableView = component.findParent(WebCellBasedView.class);
				if (tableView == null)
				{
					// the component is not part of the table view (it is on other form part), so ignore selection change
					return true;
				}
				else tableView.setSelectionMadeByCellAction();


				if (parentFormViewType == IForm.LIST_VIEW || parentFormViewType == FormController.LOCKED_LIST_VIEW)
				{
					if (component instanceof WebCellBasedViewListViewItem)
					{
						((WebCellBasedViewListViewItem)component).markSelected(target);
					}
					else
					{
						WebCellBasedViewListViewItem listViewItem = component.findParent(WebCellBasedView.WebCellBasedViewListViewItem.class);
						if (listViewItem != null)
						{
							listViewItem.markSelected(target);
						}
					}
				}
			}
		}

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
			if (!(component instanceof WebCellBasedViewListViewItem))
			{
				// update the last rendered value for the events component (if updated)
				((RecordItemModel)someModel).updateRenderedValue(component);
			}

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
					boolean isRightClick = (modifiers != MODIFIERS_UNSPECIFIED) && ((modifiers & Event.ALT_MASK) != 0);

					if (!isRightClick)
					{
						if (!toggle && !extend && tableView != null && tableView.getDragNDropController() != null &&
							Arrays.binarySearch(((FoundSet)fs).getSelectedIndexes(), index) > -1)
						{
							return true;
						}

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
						else if (index != -1 || fs.getSize() == 0)
						{
							fs.setSelectedIndex(index);
						}
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
		WebClientSession webClientSession = WebClientSession.get();
		if (target != null && page instanceof MainPage && webClientSession != null && webClientSession.getWebClient() != null &&
			webClientSession.getWebClient().getSolution() != null)
		{
			if (target instanceof CloseableAjaxRequestTarget && ((CloseableAjaxRequestTarget)target).isClosed())
			{
				return;
			}
			// do executed the events for before generating the response.
			webClientSession.getWebClient().executeEvents();

			if (webClientSession.getWebClient() == null || webClientSession.getWebClient().getSolution() == null)
			{
				// how can web client be null here ?
				return;
			}
			final MainPage mainPage = ((MainPage)page);

			if (mainPage.getPageMap() instanceof ModifiedAccessStackPageMap)
			{
				// at every request mark the pagemap as dirty so lru eviction really works
				((ModifiedAccessStackPageMap)mainPage.getPageMap()).flagDirty();
			}

			// If the main form is switched then do a normal redirect.
			if (mainPage.isMainFormSwitched())
			{
				mainPage.versionPush();
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


				mainPage.addWebAnchoringInfoIfNeeded();

				final Set<WebCellBasedView> tableViewsToRender = new HashSet<WebCellBasedView>();
				final List<String> valueChangedIds = new ArrayList<String>();
				final List<String> invalidValueIds = new ArrayList<String>();
				final Map<WebCellBasedView, List<Integer>> tableViewsWithChangedRowIds = new HashMap<WebCellBasedView, List<Integer>>();

				// first, get all invalidValue & valueChanged components
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
							if (component.getParent().isVisibleInHierarchy())
							{
								// the component will get added to the target & rendered only if it's parent is visible in hierarchy because changed flag is also set (see the visitor below)
								// so we will only list these components if they are visible otherwise ajax timer could end up sending hundreds of id's that don't actually render every 5 seconds
								// because the valueChanged flag is cleared only by onRender
								valueChangedIds.add(component.getMarkupId());
								if (component instanceof MarkupContainer)
								{
									((MarkupContainer)component).visitChildren(IDisplayData.class, new IVisitor<Component>()
									{
										public Object component(Component comp)
										{
											// labels/buttons that don't display data are not changed
											if (!(comp instanceof ILabel))
											{
												valueChangedIds.add(comp.getMarkupId());
											}
											return CONTINUE_TRAVERSAL;
										}
									});
								}
							}
						}
						return CONTINUE_TRAVERSAL;
					}
				});

				// add changed components to target; if a component is changed, the change check won't go deeper in hierarchy
				page.visitChildren(IProviderStylePropertyChanges.class, new Component.IVisitor<Component>()
				{
					public Object component(Component component)
					{
						if (((IProviderStylePropertyChanges)component).getStylePropertyChanges().isChanged())
						{
							if (component.getParent().isVisibleInHierarchy())
							{
								target.addComponent(component);
								generateDragAttach(component, target.getHeaderResponse());

								WebForm parentForm = component.findParent(WebForm.class);
								boolean isDesignMode = parentForm != null && parentForm.isDesignMode();

								if (!component.isVisible() ||
									(component instanceof WrapperContainer && !((WrapperContainer)component).getDelegate().isVisible()))
								{
									((IProviderStylePropertyChanges)component).getStylePropertyChanges().setRendered();
									if (isDesignMode)
									{
										target.appendJavascript("Servoy.ClientDesign.hideSelected('" + component.getMarkupId() + "')");
									}
								}
								else
								{
									if (isDesignMode)
									{
										target.appendJavascript("Servoy.ClientDesign.refreshSelected('" + component.getMarkupId() + "')");
									}
									// some components need to perform js layout tasks when their markup is replaced when using anchored layout
									mainPage.getPageContributor().markComponentForAnchorLayoutIfNeeded(component);
								}

								ListItem<IRecordInternal> row = component.findParent(ListItem.class);
								if (row != null)
								{
									WebCellBasedView wcbv = row.findParent(WebCellBasedView.class);
									if (wcbv != null)
									{
										if (tableViewsWithChangedRowIds.get(wcbv) == null)
										{
											tableViewsWithChangedRowIds.put(wcbv, new ArrayList<Integer>());
										}
										List<Integer> ids = tableViewsWithChangedRowIds.get(wcbv);
										int changedRowIdx = wcbv.indexOf(row);
										if (changedRowIdx >= 0 && !ids.contains(changedRowIdx))
										{
											ids.add(changedRowIdx);
										}
									}
								}
							}
							return IVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
						}
						else if (component instanceof WebCellBasedView) tableViewsToRender.add((WebCellBasedView)component);
						return component.isVisible() ? IVisitor.CONTINUE_TRAVERSAL : IVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
					}
				});

				page.visitChildren(IComponentToRequestAttacher.class, new Component.IVisitor<Component>()
				{
					public Object component(Component component)
					{
						((IComponentToRequestAttacher)component).attachComponents(target);
						return IVisitor.CONTINUE_TRAVERSAL;
					}
				});

				final List<String> visibleEditors = new ArrayList<String>();
				page.visitChildren(WebDataHtmlArea.class, new Component.IVisitor<Component>()
				{
					public Object component(Component component)
					{
						visibleEditors.add(((WebDataHtmlArea)component).getEditorID());
						return IVisitor.CONTINUE_TRAVERSAL;
					}
				});
				StringBuffer argument = new StringBuffer();
				for (String id : visibleEditors)
				{
					argument.append("\"");
					argument.append(id);
					argument.append("\"");
					if (visibleEditors.indexOf(id) != visibleEditors.size() - 1)
					{
						argument.append(",");
					}
				}
				target.prependJavascript("Servoy.HTMLEdit.removeInvalidEditors(" + argument + ");");

				String rowSelectionScript, columnResizeScript;
				for (final WebCellBasedView wcbv : tableViewsToRender)
				{
					if (wcbv.isScrollMode()) wcbv.scrollViewPort(target);
					wcbv.updateRowSelection(target);
					List<Integer> changedIds = tableViewsWithChangedRowIds.get(wcbv);
					List<Integer> selectedIndexesChanged = wcbv.getIndexToUpdate(false);
					List<Integer> mergedIds = selectedIndexesChanged != null ? selectedIndexesChanged : new ArrayList<Integer>();
					if (changedIds != null)
					{
						for (Integer id : changedIds)
						{
							if (!mergedIds.contains(id))
							{
								mergedIds.add(id);
							}
						}
					}
					rowSelectionScript = wcbv.getRowSelectionScript(mergedIds);
					wcbv.clearSelectionByCellActionFlag();
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
					if (comp instanceof WebDataHtmlArea)
					{
						target.appendJavascript("tinyMCE.activeEditor.focus()");
					}
					else
					{
						target.focusComponent(comp);
					}
				}
				else if (mainPage.getAndResetMustFocusNull())
				{
					// This is needed for example when showing a non-modal dialog in IE7 (or otherwise
					// the new window would be displayed in the background).
					target.focusComponent(null);
				}
				if (valueChangedIds.size() > 0)
				{
					argument = new StringBuffer();
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
					target.prependJavascript("storeValueAndCursorBeforeUpdate(" + argument + ");");
					target.appendJavascript("restoreValueAndCursorAfterUpdate();");
				}

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

				if (((WebClientInfo)webClientSession.getClientInfo()).getProperties().isBrowserInternetExplorer() &&
					((WebClientInfo)webClientSession.getClientInfo()).getProperties().getBrowserVersionMajor() < 9)
				{
					target.appendJavascript("Servoy.Utils.checkWebFormHeights();");
				}
				try
				{
					if (isStyleSheetLimitForIE(page.getSession()))
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

	public static boolean isStyleSheetLimitForIE(Session session)
	{
		if (session != null)
		{
			return ((WebClientInfo)session.getClientInfo()).getProperties().isBrowserInternetExplorer() &&
				((WebClientInfo)session.getClientInfo()).getProperties().getBrowserVersionMajor() < 10;
		}
		return false;
	}

	@Override
	protected String getFormName()
	{
		return getFormName(component);
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

	@SuppressWarnings("nls")
	private static void updateDragAttachOutput(Object component, StringBuilder sbAttachDrag, StringBuilder sbAttachDrop, boolean hasDragEvent,
		boolean hasDropEvent)
	{
		StringBuilder sb = null;
		if (hasDragEvent &&
			(component instanceof WebBaseLabel || component instanceof WebBaseButton || component instanceof WebBaseSubmitLink || ((component instanceof IDisplay) && ((IDisplay)component).isReadOnly()))) sb = sbAttachDrag;
		else if (hasDropEvent) sb = sbAttachDrop;

		if (sb != null)
		{
			sb.append('\'');
			sb.append(((Component)component).getMarkupId());
			sb.append("',");
		}
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

		if ((behaviorComponent instanceof IComponent) && !(behaviorComponent instanceof IFormDataDragNDrop))
		{
			behaviorComponent = (Component)component.findParent(IFormDataDragNDrop.class);
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

		if (behaviorComponent instanceof IFormDataDragNDrop)
		{
			final StringBuilder sbAttachDrag = new StringBuilder(100);
			sbAttachDrag.append("Servoy.DD.attachDrag([");
			final StringBuilder sbAttachDrop = new StringBuilder(100);
			sbAttachDrop.append("Servoy.DD.attachDrop([");

			final boolean hasDragEvent = ((IFormDataDragNDrop)behaviorComponent).getDragNDropController().getForm().getOnDragMethodID() > 0 ||
				((IFormDataDragNDrop)behaviorComponent).getDragNDropController().getForm().getOnDragOverMethodID() > 0;
			final boolean hasDropEvent = ((IFormDataDragNDrop)behaviorComponent).getDragNDropController().getForm().getOnDropMethodID() > 0;


			if (component instanceof WebDataRenderer || component instanceof WebCellBasedView)
			{
				if (hasDragEvent) sbAttachDrag.append('\'').append(component.getMarkupId()).append("',");
				if (hasDropEvent) sbAttachDrop.append('\'').append(component.getMarkupId()).append("',");

				if (component instanceof WebDataRenderer)
				{
					Iterator< ? extends Component> dataRendererIte = ((WebDataRenderer)component).iterator();

					Object dataRendererChild;
					while (dataRendererIte.hasNext())
					{
						dataRendererChild = dataRendererIte.next();
						if (dataRendererChild instanceof IWebFormContainer) continue;
						if (dataRendererChild instanceof WrapperContainer) dataRendererChild = ((WrapperContainer)dataRendererChild).getDelegate();
						if (dataRendererChild instanceof IComponent && ((IComponent)dataRendererChild).isEnabled())
						{
							updateDragAttachOutput(dataRendererChild, sbAttachDrag, sbAttachDrop, hasDragEvent, hasDropEvent);
						}
					}
				}
				else if (component instanceof WebCellBasedView)
				{
					ListView<IRecordInternal> table = ((WebCellBasedView)component).getTable();
					table.visitChildren(new IVisitor<Component>()
					{
						public Object component(Component comp)
						{
							if (comp instanceof IComponent && comp.isEnabled())
							{
								updateDragAttachOutput(comp, sbAttachDrag, sbAttachDrop, hasDragEvent, hasDropEvent);
							}
							return null;
						}
					});
				}
			}
			else if (component != null && component.isEnabled())
			{
				updateDragAttachOutput(component, sbAttachDrag, sbAttachDrop, hasDragEvent, hasDropEvent);
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