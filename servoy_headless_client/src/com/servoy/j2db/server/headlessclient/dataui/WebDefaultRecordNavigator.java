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

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.AjaxCallDecorator;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractWrapModel;
import org.apache.wicket.model.IComponentAssignedModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.IWrapModel;
import org.apache.wicket.model.Model;

import com.servoy.j2db.ISupportNavigator;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.ISaveConstants;
import com.servoy.j2db.server.headlessclient.MainPage;
import com.servoy.j2db.server.headlessclient.WebForm;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;
import com.servoy.j2db.ui.IStylePropertyChanges;
import com.servoy.j2db.util.ISupplyFocusChildren;
import com.servoy.j2db.util.Utils;

/**
 * The default Navigator for the webclient.
 *
 * @author jcompagner,jblok
 */
public class WebDefaultRecordNavigator extends Panel implements IProviderStylePropertyChanges, ISupplyFocusChildren<Component>
{
	private static final long serialVersionUID = 1L;

	public static final int DEFAULT_WIDTH = ISupportNavigator.DEFAULT_NAVIGATOR_WIDTH;
	public static final int DEFAULT_HEIGHT_WEB = 160;

	private final FoundSetIndexModel foundSetIndexModel;

	private String totalRecords;
	protected ChangesRecorder jsChangeRecorder = new ChangesRecorder(null, null);
	private String selectedRecord;

	private final TextField tf;
	private final ServoySubmitLink prevLink;
	private final ServoySubmitLink nextLink;

	public WebDefaultRecordNavigator(WebForm form)
	{
		super("default_navigator");

		boolean useAJAX = Utils.getAsBoolean(form.getController().getApplication().getRuntimeProperties().get("useAJAX"));
		setOutputMarkupPlaceholderTag(true);

		foundSetIndexModel = new FoundSetIndexModel(form);

		add(new Label("firstRecordIndex", foundSetIndexModel));
		tf = new NavigatorTextField("currentRecordIndex", foundSetIndexModel)
		{
			private static final long serialVersionUID = 1L;

			private String inputId;

			/**
			 * @see wicket.markup.html.form.FormComponent#getInputName()
			 */
			@Override
			public String getInputName()
			{
				if (inputId == null)
				{
					Page page = findPage();
					if (page instanceof MainPage)
					{
						inputId = ((MainPage)page).nextInputNameId();
					}
					else
					{
						return super.getInputName();
					}
				}
				return inputId;
			}
		};
		if (useAJAX)
		{
			tf.setOutputMarkupPlaceholderTag(true);
			tf.add(new ServoyAjaxFormComponentUpdatingBehavior("onchange")
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected void onUpdate(AjaxRequestTarget target)
				{
					WebEventExecutor.generateResponse(target, findPage());
				}
			});
			tf.add(new ServoyAjaxFormComponentUpdatingBehavior("onkeydown")
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected void onUpdate(AjaxRequestTarget target)
				{
					WebEventExecutor.generateResponse(target, findPage());
				}

				@Override
				protected IAjaxCallDecorator getAjaxCallDecorator()
				{
					return new AjaxCallDecorator()
					{
						private static final long serialVersionUID = 1L;

						@Override
						public CharSequence decorateScript(CharSequence script)
						{
							return "return testEnterKey(event, function() {" + script + "});";
						}
					};
				}
			});
		}
		add(tf);
		add(new Label("totalRecords", foundSetIndexModel));
		prevLink = new NavigatorServoySubmitLink("prev", useAJAX)
		{
			private static final long serialVersionUID = 1L;

			/**
			 * @see wicket.ajax.markup.html.AjaxFallbackLink#onClick(wicket.ajax.AjaxRequestTarget)
			 */
			@Override
			public void onClick(AjaxRequestTarget target)
			{
				foundSetIndexModel.prev();
				if (target != null)
				{
					Page page = getPage();
					target.addComponent(tf);
					WebEventExecutor.generateResponse(target, page);
				}
			}

		};
		add(prevLink);
		nextLink = new NavigatorServoySubmitLink("next", useAJAX)
		{
			private static final long serialVersionUID = 1L;

			/**
			 * @see wicket.ajax.markup.html.AjaxFallbackLink#onClick(wicket.ajax.AjaxRequestTarget)
			 */
			@Override
			public void onClick(AjaxRequestTarget target)
			{
				foundSetIndexModel.next();
				if (target != null)
				{
					Page page = getPage();
					target.addComponent(tf);
					WebEventExecutor.generateResponse(target, page);
				}
			}
		};
		add(nextLink);
	}

	/**
	 * @see wicket.MarkupContainer#onRender(wicket.markup.MarkupStream)
	 */
	@Override
	protected void onRender(MarkupStream markupStream)
	{
		super.onRender(markupStream);
		IFoundSetInternal fs = foundSetIndexModel.getFoundSet();
		if (fs != null)
		{
			totalRecords = (fs.getSize() == 0 ? "" : (fs.getSize() + (fs.hadMoreRows() ? "+" : "")));
			selectedRecord = (fs.getSelectedIndex() == -1 ? "" : "" + (fs.getSelectedIndex() + 1));
		}
	}

	public IStylePropertyChanges getStylePropertyChanges()
	{
		// the recorder is only used for isChanged, set the correct value before returning the instance
		IFoundSetInternal fs = foundSetIndexModel.getFoundSet();
		if (fs == null)
		{
			jsChangeRecorder.setRendered();
		}
		else if ((fs.getSize() == 0 ? "" : (fs.getSize() + (fs.hadMoreRows() ? "+" : ""))).equals(totalRecords) &&
			((fs.getSelectedIndex() == -1 ? "" : "" + (fs.getSelectedIndex() + 1)).equals(selectedRecord)))
		{
			jsChangeRecorder.setRendered();
		}
		else jsChangeRecorder.setChanged();
		return jsChangeRecorder;
	}

	public Component[] getFocusChildren()
	{
		return new Component[] { prevLink, nextLink, tf };
	}

	private class NavigatorTextField extends TextField implements IProviderStylePropertyChanges
	{
		public NavigatorTextField(String id, IModel object)
		{
			super(id, object);
		}

		private final ChangesRecorder jsChangeRecorder = new ChangesRecorder(null, null);

		public IStylePropertyChanges getStylePropertyChanges()
		{
			return jsChangeRecorder;
		}

		/**
		 * @see org.apache.wicket.MarkupContainer#onRender(org.apache.wicket.markup.MarkupStream)
		 */
		@Override
		protected void onRender(MarkupStream markupStream)
		{
			super.onRender(markupStream);
			getStylePropertyChanges().setRendered();
		}
	}

	private abstract class NavigatorServoySubmitLink extends ServoySubmitLink implements IProviderStylePropertyChanges
	{
		public NavigatorServoySubmitLink(String id, boolean useAJAX)
		{
			super(id, useAJAX);
		}

		private final ChangesRecorder jsChangeRecorder = new ChangesRecorder(null, null);

		public IStylePropertyChanges getStylePropertyChanges()
		{
			return jsChangeRecorder;
		}

		/**
		 * @see org.apache.wicket.MarkupContainer#onRender(org.apache.wicket.markup.MarkupStream)
		 */
		@Override
		protected void onRender(MarkupStream markupStream)
		{
			super.onRender(markupStream);
			getStylePropertyChanges().setRendered();
		}
	}
}

class FoundSetIndexModel extends Model implements IComponentAssignedModel
{
	private static final long serialVersionUID = 1L;

	/** The ListView's list model */
	private final WebForm form;

	/**
	 * Construct
	 *
	 * @param listView The ListView
	 * @param index The index of this model
	 */
	public FoundSetIndexModel(final WebForm listView)
	{
		form = listView;
	}

	public IFoundSetInternal getFoundSet()
	{
		return form.getController().getFormModel();
	}


	/**
	 * @see wicket.model.IAssignmentAwareModel#wrapOnAssignment(wicket.Component)
	 */
	public IWrapModel wrapOnAssignment(Component component)
	{
		return new WrapModel(component);
	}

	public void next()
	{
		IFoundSetInternal fs = form.getController().getFormModel();
		if (fs != null)
		{
			int index = fs.getSelectedIndex();
			if (index + 1 < fs.getSize() && ((form.getController().getApplication().getFoundSetManager().getEditRecordList().stopEditing(false) &
				(ISaveConstants.STOPPED + ISaveConstants.AUTO_SAVE_BLOCKED)) != 0))
			{
				fs.setSelectedIndex(index + 1);
			}
		}
	}

	public void prev()
	{
		IFoundSetInternal fs = form.getController().getFormModel();
		if (fs != null && fs.getSelectedIndex() > 0 && ((form.getController().getApplication().getFoundSetManager().getEditRecordList().stopEditing(false) &
			(ISaveConstants.STOPPED + ISaveConstants.AUTO_SAVE_BLOCKED)) != 0))
		{
			fs.setSelectedIndex(fs.getSelectedIndex() - 1);
		}
	}

	class WrapModel extends AbstractWrapModel
	{
		private static final long serialVersionUID = 1L;
		private final Component component;

		WrapModel(Component component)
		{
			this.component = component;
		}

		/**
		 * @see wicket.model.IWrapModel#getNestedModel()
		 */
		public IModel getWrappedModel()
		{
			return FoundSetIndexModel.this;
		}

		@Override
		public void detach()
		{
			FoundSetIndexModel.this.detach();
		}

		/**
		 * @see wicket.model.IModel#getObject()
		 */
		@Override
		public Object getObject()
		{

			IFoundSetInternal fs = form.getController().getFormModel();
			if (fs != null)
			{
				if ("firstRecordIndex".equals(component.getId()))
				{
					return (fs.getSize() == 0 ? "0" : "1");
				}
				if ("totalRecords".equals(component.getId()))
				{
					return (fs.getSize() == 0 ? "" : (fs.getSize() + (fs.hadMoreRows() ? "+" : "")));
				}
				if ("currentRecordIndex".equals(component.getId()))
				{
					return (fs.getSelectedIndex() == -1 ? "" : "" + (fs.getSelectedIndex() + 1));
				}
			}
			return null;
		}

		/**
		 * @see wicket.model.IModel#setObject(java.lang.Object)
		 */
		@Override
		public void setObject(Object object)
		{

			IFoundSetInternal fs = form.getController().getFormModel();
			if (fs != null)
			{
				if ("currentRecordIndex".equals(component.getId()))
				{
					int index = Utils.getAsInteger(object);
					if (index < 1)
					{
						index = 1;
					}
					else if (index > fs.getSize())
					{
						index = fs.getSize();
					}
					if (index != fs.getSelectedIndex() + 1 &&
						((form.getController().getApplication().getFoundSetManager().getEditRecordList().stopEditing(false) &
							(ISaveConstants.STOPPED + ISaveConstants.AUTO_SAVE_BLOCKED)) != 0))
					{
						fs.setSelectedIndex(index - 1);
					}
				}
			}
		}
	}
}
