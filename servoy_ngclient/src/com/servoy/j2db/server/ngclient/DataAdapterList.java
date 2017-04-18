package com.servoy.j2db.server.ngclient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.sablo.BaseWebObject;
import org.sablo.Container;
import org.sablo.WebComponent;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebObjectFunctionDefinition;
import org.sablo.specification.WebObjectSpecification.PushToServerEnum;
import org.sablo.specification.property.BrowserConverterContext;
import org.sablo.specification.property.IPropertyConverterForBrowser;
import org.sablo.specification.property.IPropertyType;
import org.sablo.specification.property.types.TypesRegistry;

import com.servoy.base.util.ITagResolver;
import com.servoy.j2db.ApplicationException;
import com.servoy.j2db.BasicFormController;
import com.servoy.j2db.dataprocessing.IDataAdapter;
import com.servoy.j2db.dataprocessing.IModificationListener;
import com.servoy.j2db.dataprocessing.IRecord;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.ModificationEvent;
import com.servoy.j2db.dataprocessing.TagResolver;
import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.ColumnWrapper;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IDataProviderLookup;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.query.QueryAggregate;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.scripting.GlobalScope;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.scripting.ScopesScope;
import com.servoy.j2db.server.ngclient.component.EventExecutor;
import com.servoy.j2db.server.ngclient.property.DataproviderConfig;
import com.servoy.j2db.server.ngclient.property.FoundsetLinkedConfig;
import com.servoy.j2db.server.ngclient.property.FoundsetLinkedTypeSabloValue;
import com.servoy.j2db.server.ngclient.property.FoundsetTypeSabloValue;
import com.servoy.j2db.server.ngclient.property.IDataLinkedPropertyValue;
import com.servoy.j2db.server.ngclient.property.IFindModeAwarePropertyValue;
import com.servoy.j2db.server.ngclient.property.types.DataproviderTypeSabloValue;
import com.servoy.j2db.server.ngclient.property.types.IDataLinkedType.TargetDataLinks;
import com.servoy.j2db.server.ngclient.property.types.NGConversions;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.Utils;


public class DataAdapterList implements IModificationListener, ITagResolver, IDataAdapterList
{

	// properties that are interested in a specific dataproviderID chaning
	protected final Map<String, List<IDataLinkedPropertyValue>> dataProviderToLinkedComponentProperty = new HashMap<>(); // dataProviderID -> [(comp, propertyName)]

	// all data-linked properties - contains 'dataProviderToLinkedComponentProperty' as well as other ones that are interested in any DP change
	protected final List<IDataLinkedPropertyValue> allComponentPropertiesLinkedToData = new ArrayList<>(); // [(comp, propertyName), ...]

	protected final List<IFindModeAwarePropertyValue> findModeAwareProperties = new ArrayList<>();

	private final IWebFormController formController;
	private final EventExecutor executor;
	private final WeakHashMap<IWebFormController, String> visibleChildForms = new WeakHashMap<>();
	private final ArrayList<IWebFormController> parentRelatedForms = new ArrayList<IWebFormController>();

	private IRecordInternal record;
	private boolean findMode = false;
	private boolean settingRecord;

	private boolean isFormScopeListener;
	private boolean isGlobalScopeListener;

	public DataAdapterList(IWebFormController formController)
	{
		this.formController = formController;
		this.executor = new EventExecutor(formController);
	}

	public final INGApplication getApplication()
	{
		return formController.getApplication();
	}

	public final IWebFormController getForm()
	{
		return formController;
	}

	@Override
	public Object executeEvent(WebComponent webComponent, String event, int eventId, Object[] args)
	{
		Object jsRetVal = executor.executeEvent(webComponent, event, eventId, args);
		return NGConversions.INSTANCE.convertRhinoToSabloComponentValue(jsRetVal, null, null, webComponent); // TODO why do handlers not have complete definitions in spec - just like apis? - we don't know types here
	}

	@Override
	public Object executeInlineScript(String script, JSONObject args, JSONArray appendingArgs)
	{
		String decryptedScript = HTMLTagsConverter.decryptInlineScript(script, args);
		if (appendingArgs != null && decryptedScript.endsWith("()"))
		{
			ArrayList<Object> javaArguments = new ArrayList<Object>();
			Object argObj = null;
			BrowserConverterContext dataConverterContext = new BrowserConverterContext((WebFormUI)formController.getFormUI(), PushToServerEnum.allow);
			for (int i = 0; i < appendingArgs.length(); i++)
			{
				try
				{
					argObj = ServoyJSONObject.jsonNullToNull(appendingArgs.get(i));
					if (argObj instanceof JSONObject)
					{
						String typeHint = ((JSONObject)argObj).optString("svyType", null); //$NON-NLS-1$
						if (typeHint != null)
						{
							IPropertyType< ? > propertyType = TypesRegistry.getType(typeHint);
							if (propertyType instanceof IPropertyConverterForBrowser< ? >)
							{
								javaArguments.add(((IPropertyConverterForBrowser< ? >)propertyType).fromJSON(argObj, null,
									null /*
											 * TODO this shouldn't be null! Make this better - maybe parse the type or just instantiate a property description
											 * if we don't want full support for what can be defined in spec file as a type
											 */, dataConverterContext, null));
								continue;
							}
						}
					}
				}
				catch (JSONException e)
				{
					Debug.error(e);
				}
				javaArguments.add(argObj);
			}

			String functionName = decryptedScript.substring(0, decryptedScript.length() - 2);
			int startIdx = functionName.lastIndexOf('.');
			String noPrefixFunctionName = functionName.substring(startIdx > -1 ? startIdx + 1 : 0, functionName.length());


			Scriptable scope = null;
			Function f = null;

			if (functionName.startsWith("forms."))
			{
				FormScope formScope = formController.getFormScope();

				f = formScope.getFunctionByName(noPrefixFunctionName);
				if (f != null && f != Scriptable.NOT_FOUND)
				{
					scope = formScope;
				}
			}
			else if (functionName.startsWith("entity."))
			{
				scope = (Scriptable)formController.getFoundSet();
				f = (Function)scope.getPrototype().get(noPrefixFunctionName, scope);
			}
			else
			{
				ScriptMethod scriptMethod = formController.getApplication().getFlattenedSolution().getScriptMethod(functionName);
				if (scriptMethod != null)
				{
					scope = formController.getApplication().getScriptEngine().getScopesScope().getGlobalScope(scriptMethod.getScopeName());
				}
				if (scope != null)
				{
					f = ((GlobalScope)scope).getFunctionByName(noPrefixFunctionName);
				}
			}

			try
			{
				return formController.getApplication().getScriptEngine().executeFunction(f, scope, scope, javaArguments.toArray(), false, false);
			}
			catch (Exception ex)
			{
				Debug.error(ex);
				return null;
			}
		}
		return decryptedScript != null ? formController.eval(decryptedScript) : null;
	}

	private static boolean containsForm(IWebFormUI parent, IWebFormUI child)
	{
		Object childParentContainer = child.getParentContainer();
		if (childParentContainer instanceof WebFormComponent)
		{
			Container p = ((WebFormComponent)childParentContainer).getParent();
			if (p instanceof IWebFormUI)
			{
				if (p.equals(parent))
				{
					return true;
				}
				else return containsForm(parent, (IWebFormUI)p);
			}
		}

		return false;
	}

	public void updateRelatedVisibleForms(List<Pair<String, String>> oldForms, List<Pair<String, String>> newForms)
	{
		if (oldForms != null)
		{
			for (Pair<String, String> oldForm : oldForms)
			{
				IWebFormController fc = getApplication().getFormManager().getCachedFormController(oldForm.getLeft());
				if (fc != null && !newForms.contains(oldForm) && visibleChildForms.containsKey(fc))
				{
					List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>();
					fc.notifyVisible(false, invokeLaterRunnables);
					Utils.invokeLater(getApplication(), invokeLaterRunnables);
					removeVisibleChildForm(fc, true);
				}
			}
		}
		if (newForms != null)
		{
			for (Pair<String, String> newVisibleForm : newForms)
			{
				if (!oldForms.contains(newVisibleForm))
				{
					// if this form is by itself not visible, then don't touch the form if it is not loaded yet.
					IWebFormController newFormController = formController.isFormVisible() ? getApplication().getFormManager().getForm(newVisibleForm.getLeft())
						: getApplication().getFormManager().getCachedFormController(newVisibleForm.getLeft());
					if (newFormController != null)
					{
						addVisibleChildForm(newFormController, newVisibleForm.getRight(), true);
						if (newVisibleForm.getRight() != null)
						{
							newFormController.loadRecords(record != null
								? record.getRelatedFoundSet(newVisibleForm.getRight(), ((BasicFormController)newFormController).getDefaultSortColumns())
								: null);
						}
						updateParentContainer(newFormController, newVisibleForm.getRight(), formController.isFormVisible());
						List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>();
						newFormController.notifyVisible(formController.isFormVisible(), invokeLaterRunnables);
						Utils.invokeLater(getApplication(), invokeLaterRunnables);
					}
				}
				else
				{
					oldForms.remove(newVisibleForm);
				}
			}
		}
	}

	public void addVisibleChildForm(IWebFormController form, String relation, boolean shouldUpdateParentFormController)
	{
		if (shouldUpdateParentFormController)
		{
			form.setParentFormController(formController);
		}
		else
		{
			form.getFormUI().getDataAdapterList().addParentRelatedForm(getForm());
		}

		if (relation != null)
		{
			for (Entry<IWebFormController, String> relatedFormEntry : visibleChildForms.entrySet())
			{
				IWebFormController relatedForm = relatedFormEntry.getKey();
				String relatedFormRelation = relatedFormEntry.getValue();
				if (relatedFormRelation != null)
				{
					if (relatedFormRelation.startsWith(relation) && relatedFormRelation.length() > relation.length())
					{
						if (!containsForm(form.getFormUI(), relatedForm.getFormUI()))
						{
							form.getFormUI().getDataAdapterList().addVisibleChildForm(relatedForm, relatedFormRelation.substring(relation.length() + 1), false);
						}
					}
					else if (relation.startsWith(relatedFormRelation) && relation.length() > relatedFormRelation.length())
					{
						if (!containsForm(relatedForm.getFormUI(), form.getFormUI()))
						{
							relatedForm.getFormUI().getDataAdapterList().addVisibleChildForm(form, relation.substring(relatedFormRelation.length() + 1), false);
						}
					}
				}
			}
			if (!isGlobalScopeListener)
			{
				Relation relationObj = formController.getApplication().getFlattenedSolution().getRelation(relation);
				if (relationObj != null && relationObj.containsGlobal())
				{
					formController.getApplication().getScriptEngine().getScopesScope().getModificationSubject().addModificationListener(this);
					isGlobalScopeListener = true;
				}
			}
		}
		visibleChildForms.put(form, relation);
	}

	public void removeVisibleChildForm(IWebFormController form, boolean firstLevel)
	{
		if (firstLevel)
		{
			form.setParentFormController(null);
		}
		else
		{
			form.getFormUI().getDataAdapterList().removeParentRelatedForm(getForm());
		}

		if (visibleChildForms.containsKey(form))
		{
			visibleChildForms.remove(form);
			for (Object relWFC : form.getFormUI().getDataAdapterList().getParentRelatedForms().toArray())
			{
				((IWebFormController)relWFC).getFormUI().getDataAdapterList().removeVisibleChildForm(form, false);
			}
		}
	}

	public Map<IWebFormController, String> getRelatedForms()
	{
		return visibleChildForms;
	}

	public void addParentRelatedForm(IWebFormController form)
	{
		if (parentRelatedForms.indexOf(form) == -1) parentRelatedForms.add(form);
	}

	public void removeParentRelatedForm(IWebFormController form)
	{
		parentRelatedForms.remove(form);
	}

	public List<IWebFormController> getParentRelatedForms()
	{
		return parentRelatedForms;
	}

	private void setupModificationListener(String dataprovider)
	{
		if (!isFormScopeListener && (isFormDataprovider(dataprovider) || dataprovider == null))
		{
			formController.getFormScope().getModificationSubject().addModificationListener(this);
			isFormScopeListener = true;
		}
		if (!isGlobalScopeListener && (isGlobalDataprovider(dataprovider) || dataprovider == null))
		{
			formController.getApplication().getScriptEngine().getScopesScope().getModificationSubject().addModificationListener(this);
			isGlobalScopeListener = true;
		}
	}

	private String[] getAllDependantDataProviders(String[] dataproviders)
	{
		ArrayList<String> returnDP = new ArrayList<>();
		for (String dp : dataproviders)
		{
			if (dp != null && !ScopesUtils.isVariableScope(dp))
			{
				int idx = dp.lastIndexOf('.');
				if (idx != -1) //handle related fields
				{
					Relation[] relations = getApplication().getFlattenedSolution().getRelationSequence(dp.substring(0, idx));
					if (relations != null)
					{
						for (int i = 0; i < relations.length; i++)
						{
							Relation relation = relations[i];
							if (Relation.isValid(relation, getApplication().getFlattenedSolution()) && (relation.isGlobal() || i == 0))
							{
								try
								{
									IDataProvider[] dps = relation.getPrimaryDataProviders(getApplication().getFlattenedSolution());
									for (IDataProvider idp : dps)
									{
										returnDP.add(idp.getDataProviderID());
									}
								}
								catch (RepositoryException ex)
								{
									Debug.error(ex);
								}
							}
						}
						continue;
					}
				}
			}
			returnDP.add(dp);
		}

		return returnDP.toArray(new String[returnDP.size()]);
	}

	public void addDataLinkedProperty(IDataLinkedPropertyValue propertyValue, TargetDataLinks targetDataLinks)
	{
		if (targetDataLinks == TargetDataLinks.NOT_LINKED_TO_DATA || targetDataLinks == null) return;

		String[] dataproviders = targetDataLinks.dataProviderIDs;
		if (dataproviders == null)
		{
			dataproviders = new String[] { null };
		}
		else
		{
			dataproviders = getAllDependantDataProviders(dataproviders);
		}
		for (String dpID : dataproviders)
		{
			List<IDataLinkedPropertyValue> allLinksOfDP = dataProviderToLinkedComponentProperty.get(dpID);
			if (allLinksOfDP == null)
			{
				allLinksOfDP = new ArrayList<>();
				dataProviderToLinkedComponentProperty.put(dpID, allLinksOfDP);
			}
			if (allLinksOfDP.remove(propertyValue))
			{
				Debug.error("DAL.addDataLinkedProperty - trying to register the same (equal) property value twice (" + propertyValue +
					"); this means that some code that uses DAL is not working properly (maybe cleanup/detach malfunction); will use latest value... Links: " +
					targetDataLinks);
			}

			allLinksOfDP.add(propertyValue);

			if (formController != null) setupModificationListener(dpID); // see if we need to listen to global/form scope changes
		}

		allComponentPropertiesLinkedToData.add(propertyValue);

	}

	public void removeDataLinkedProperty(IDataLinkedPropertyValue propertyValue)
	{
		Iterator<List<IDataLinkedPropertyValue>> it = dataProviderToLinkedComponentProperty.values().iterator();
		while (it.hasNext())
		{
			List<IDataLinkedPropertyValue> x = it.next();
			if (x.remove(propertyValue))
			{
				if (x.size() == 0) it.remove();
			}
		}
		// TODO keep track & unregister when needed global/form scope listeners: so undo setupModificationListener(dpID);
		allComponentPropertiesLinkedToData.remove(propertyValue);
	}

	public void addFindModeAwareProperty(IFindModeAwarePropertyValue propertyValue)
	{
		findModeAwareProperties.add(propertyValue);
	}

	public void removeFindModeAwareProperty(IFindModeAwarePropertyValue propertyValue)
	{
		findModeAwareProperties.remove(propertyValue);
	}

	public void setRecord(IRecord record, boolean fireChangeEvent)
	{
		// if this is not a change in the record that it should not be needed that it should be pushed again.
		// because all types should just listen to the right stuff.
		if (shouldIgnoreRecordChange(this.record, record)) return;
		if (settingRecord)
		{
			if (record != this.record)
			{
				throw new IllegalStateException("Record " + record + " is being set on DAL when record: " + this.record + " is being processed");
			}
			return;
		}
		try
		{
			settingRecord = true;
			if (this.record != null)
			{
				this.record.removeModificationListener(this);
			}
			this.record = (IRecordInternal)record;

			if (this.record != null)
			{
				pushChangedValues(null, fireChangeEvent);
				this.record.addModificationListener(this);
			}
		}
		finally
		{
			settingRecord = false;
		}
		for (IWebFormController form : visibleChildForms.keySet())
		{
			if (visibleChildForms.get(form) != null)
			{
				form.loadRecords(
					record != null ? record.getRelatedFoundSet(visibleChildForms.get(form), ((BasicFormController)form).getDefaultSortColumns()) : null);
			}
		}

	}

	protected boolean shouldIgnoreRecordChange(IRecord oldRecord, IRecord newRecord)
	{
		if (oldRecord == newRecord) return true;
		return false;
	}

	public IRecordInternal getRecord()
	{
		return record;
	}

	protected boolean isFormDataprovider(String dataprovider)
	{
		if (dataprovider == null) return false;
		FormScope fs = formController.getFormScope();
		return fs.has(dataprovider, fs);
	}

	protected boolean isGlobalDataprovider(String dataprovider)
	{
		if (dataprovider == null) return false;
		ScopesScope ss = formController.getApplication().getScriptEngine().getScopesScope();
		Pair<String, String> scope = ScopesUtils.getVariableScope(dataprovider);
		if (scope.getLeft() != null)
		{
			GlobalScope gs = ss.getGlobalScope(scope.getLeft());
			return gs != null && gs.has(scope.getRight(), gs);
		}

		return false;
	}

	private void pushChangedValues(String dataProvider, boolean fireChangeEvent)
	{
		boolean isFormDP = isFormDataprovider(dataProvider);
		boolean isGlobalDP = isGlobalDataprovider(dataProvider);

		boolean changed = false;
		if (dataProvider == null)
		{
			// announce to all - we don't know exactly what changed; maybe all DPs changed
			for (IDataLinkedPropertyValue x : allComponentPropertiesLinkedToData.toArray(
				new IDataLinkedPropertyValue[allComponentPropertiesLinkedToData.size()]))
			{
				x.dataProviderOrRecordChanged(record, null, isFormDP, isGlobalDP, fireChangeEvent);
			}
		}
		else
		{
			List<IDataLinkedPropertyValue> interestedComponentProperties = dataProviderToLinkedComponentProperty.get(dataProvider);
			if (interestedComponentProperties == null)
			{
				interestedComponentProperties = dataProviderToLinkedComponentProperty.get(null);
			}
			else
			{
				List<IDataLinkedPropertyValue> listenToAllComponentProperties = dataProviderToLinkedComponentProperty.get(null);
				if (listenToAllComponentProperties != null && listenToAllComponentProperties.size() > 0)
				{
					interestedComponentProperties = new ArrayList<IDataLinkedPropertyValue>(interestedComponentProperties);
					for (IDataLinkedPropertyValue dataLink : listenToAllComponentProperties)
					{
						if (!interestedComponentProperties.contains(dataLink)) interestedComponentProperties.add(dataLink);
					}
				}
			}

			if (interestedComponentProperties != null)
			{
				interestedComponentProperties = new ArrayList<IDataLinkedPropertyValue>(interestedComponentProperties);
				for (IDataLinkedPropertyValue x : interestedComponentProperties)
				{
					x.dataProviderOrRecordChanged(record, dataProvider, isFormDP, isGlobalDP, fireChangeEvent);
				}
			}
		}

		if (fireChangeEvent && changed)
		{
			getApplication().getChangeListener().valueChanged();
		}
	}

	@Override
	public void valueChanged(ModificationEvent e)
	{
		if (record != null && e != null && e.getName() != null)
		{
			HashMap<IWebFormController, String> visibleChildFormsCopy = new HashMap<>(visibleChildForms);
			for (Entry<IWebFormController, String> relatedFormEntry : visibleChildFormsCopy.entrySet())
			{
				IWebFormController relatedForm = relatedFormEntry.getKey();
				String relatedFormRelation = relatedFormEntry.getValue();
				boolean depends = false;
				Relation[] relations = getApplication().getFlattenedSolution().getRelationSequence(relatedFormRelation);
				for (int r = 0; !depends && relations != null && r < relations.length; r++)
				{
					try
					{
						IDataProvider[] primaryDataProviders = relations[r].getPrimaryDataProviders(getApplication().getFlattenedSolution());
						for (int p = 0; !depends && primaryDataProviders != null && p < primaryDataProviders.length; p++)
						{
							depends = e.getName().equals(primaryDataProviders[p].getDataProviderID());
						}
					}
					catch (RepositoryException ex)
					{
						Debug.log(ex);
					}
				}
				if (depends)
				{
					relatedForm.loadRecords(record.getRelatedFoundSet(relatedFormRelation, ((BasicFormController)relatedForm).getDefaultSortColumns()));
				}
			}
		}
		if (getForm().isFormVisible())
		{
			pushChangedValues(e.getName(), true);
		}
	}

	public void pushChanges(WebFormComponent webComponent, String beanProperty)
	{
		pushChanges(webComponent, beanProperty, webComponent.getProperty(beanProperty), null);
	}

	@Override
	public void pushChanges(WebFormComponent webComponent, String beanProperty, String foundsetLinkedRowID)
	{
		pushChanges(webComponent, beanProperty, webComponent.getProperty(beanProperty), foundsetLinkedRowID);
	}

	/**
	 * Get the dataProviderID from the runtime property.
	 * NOTE: it's not taken directly from FormElement because 'beanProperty' might contain dots (a dataprovider nested somewhere in another property) - and BaseWebObject deals with that correctly.
	 */
	public String getDataProviderID(WebFormComponent webComponent, String beanProperty)
	{
		Object propertyValue = webComponent.getProperty(beanProperty);
		return getDataProviderID(propertyValue);
	}

	public static String getDataProviderID(Object propertyValue)
	{
		if (propertyValue instanceof DataproviderTypeSabloValue) return ((DataproviderTypeSabloValue)propertyValue).getDataProviderID();
		if (propertyValue instanceof FoundsetLinkedTypeSabloValue &&
			((FoundsetLinkedTypeSabloValue)propertyValue).getWrappedValue() instanceof DataproviderTypeSabloValue)
			return ((DataproviderTypeSabloValue)((FoundsetLinkedTypeSabloValue)propertyValue).getWrappedValue()).getDataProviderID();
		return null;
	}

	public void pushChanges(WebFormComponent webComponent, String beanProperty, Object newValue, String foundsetLinkedRowID)
	{
		// TODO should this all (svy-apply/push) move to DataProviderType client/server side implementation instead of these specialized calls, instanceof checks and string parsing (see getProperty or getPropertyDescription)?

		String dataProviderID = getDataProviderID(webComponent, beanProperty);
		if (dataProviderID == null)
		{
			Debug.log(
				"apply called on a property that is not bound to a dataprovider: " + beanProperty + ", value: " + newValue + " of component: " + webComponent);
			return;
		}

		// Check security
		webComponent.checkPropertyProtection(beanProperty);

		IRecordInternal editingRecord = record;

		if (newValue instanceof FoundsetLinkedTypeSabloValue)
		{
			if (foundsetLinkedRowID != null)
			{
				// find the row of the foundset that changed; we can't use client's index (as server-side indexes might have changed meanwhile on server); so we are doing it based on client sent rowID
				editingRecord = getFoundsetLinkedRecord((FoundsetLinkedTypeSabloValue< ? , ? >)newValue, foundsetLinkedRowID);
				if (editingRecord == null)
				{
					Debug.error("Error pushing data from client to server for foundset linked DP (cannot find record): dp=" + newValue + ", rowID=" +
						foundsetLinkedRowID);
					return;
				}
			} // hmm, this is strange - usually we should always get rowID, even if foundset linked is actually set by developer to a global or form variable - even though there rowID is not actually needed; just treat this as if it is not record linked
			newValue = ((FoundsetLinkedTypeSabloValue)newValue).getWrappedValue();
		}
		if (newValue instanceof DataproviderTypeSabloValue) newValue = ((DataproviderTypeSabloValue)newValue).getValue();

		// TODO should this always be tried? (Calendar field has no push for edit, because it doesn't use svyAutoApply)
		// but what if it was a global or form variable?
		if (editingRecord == null || editingRecord.startEditing())
		{
			Object v;
			// if the value is a map, then it means, that a set of related properties needs to be updated,
			// ex. newValue = {"" : "image_data", "_filename": "pic.jpg", "_mimetype": "image/jpeg"}
			// will update property with "image_data", property_filename with "pic.jpg" and property_mimetype with "image/jpeg"
			if (newValue instanceof HashMap)
			{
				v = ((HashMap< ? , ? >)newValue).get(""); // defining value
				Iterator<Entry< ? , ? >> newValueIte = ((HashMap)newValue).entrySet().iterator();
				while (newValueIte.hasNext())
				{
					Entry< ? , ? > e = newValueIte.next();
					if (!"".equals(e.getKey()))
					{
						try
						{
							com.servoy.j2db.dataprocessing.DataAdapterList.setValueObject(editingRecord, formController.getFormScope(),
								dataProviderID + e.getKey(), e.getValue());
						}
						catch (IllegalArgumentException ex)
						{
							Debug.trace(ex);
							getApplication().handleException(null, new ApplicationException(ServoyException.INVALID_INPUT, ex));
						}
					}
				}
			}
			else
			{
				v = newValue;
			}
			Object oldValue = null;
			Exception setValueException = null;
			try
			{
				oldValue = com.servoy.j2db.dataprocessing.DataAdapterList.setValueObject(editingRecord, formController.getFormScope(), dataProviderID, v);
			}
			catch (IllegalArgumentException e)
			{
				Debug.trace(e);
				getApplication().handleException(null, new ApplicationException(ServoyException.INVALID_INPUT, e));
				setValueException = e;
			}
			Object config = webComponent.getFormElement().getWebComponentSpec().getProperty(beanProperty).getConfig();
			if (config instanceof FoundsetLinkedConfig)
			{
				config = ((FoundsetLinkedConfig)config).getWrappedPropertyDescription().getConfig();
			}
			String onDataChange = ((DataproviderConfig)config).getOnDataChange();
			if (onDataChange != null)
			{
				JSONObject event = EventExecutor.createEvent(onDataChange, editingRecord.getParentFoundSet().getSelectedIndex());
				Object returnValue = null;
				Exception exception = null;
				String onDataChangeCallback = null;
				if (!Utils.equalObjects(oldValue, v) && setValueException == null && webComponent.hasEvent(onDataChange))
				{
					try
					{
						returnValue = webComponent.executeEvent(onDataChange, new Object[] { oldValue, v, event });
					}
					catch (Exception e)
					{
						Debug.error("Error during onDataChange webComponent=" + webComponent, e);
						exception = e;
					}
					onDataChangeCallback = ((DataproviderConfig)webComponent.getFormElement().getWebComponentSpec().getProperty(
						beanProperty).getConfig()).getOnDataChangeCallback();
				}
				else if (setValueException != null)
				{
					returnValue = setValueException.getMessage();
					exception = setValueException;
					onDataChangeCallback = ((DataproviderConfig)webComponent.getFormElement().getWebComponentSpec().getProperty(
						beanProperty).getConfig()).getOnDataChangeCallback();
				}
				if (onDataChangeCallback != null)
				{
					WebObjectFunctionDefinition call = new WebObjectFunctionDefinition(onDataChangeCallback);
					call.addParameter(new PropertyDescription("event", TypesRegistry.getType("object")));
					call.addParameter(new PropertyDescription("returnValue", TypesRegistry.getType("object")));
					call.addParameter(new PropertyDescription("exception", TypesRegistry.getType("object")));
					webComponent.invokeApi(call, new Object[] { event, returnValue, exception == null ? null : exception.getMessage() });
				}
			}
		}
	}

	private IRecordInternal getFoundsetLinkedRecord(FoundsetLinkedTypeSabloValue< ? , ? > foundsetLinkedValue, String foundsetLinkedRowID)
	{
		IRecordInternal recordForRowID = null;

		Pair<String, Integer> splitHashAndIndex = FoundsetTypeSabloValue.splitPKHashAndIndex(foundsetLinkedRowID);
		int index = foundsetLinkedValue.getFoundset().getRecordIndex(splitHashAndIndex.getLeft(), splitHashAndIndex.getRight().intValue());

		if (index >= 0) recordForRowID = foundsetLinkedValue.getFoundset().getRecord(index);
		return recordForRowID;
	}

	public void startEdit(WebFormComponent webComponent, String property, String foundsetLinkedRowID)
	{
		// TODO should this all (startEdit) move to DataProviderType client/server side implementation instead of these specialized calls, instanceof checks and string parsing (see getProperty or getPropertyDescription)?

		String dataProviderID = getDataProviderID(webComponent, property);
		if (dataProviderID == null)
		{
			Debug.log("startEdit called on a property that is not bound to a dataprovider: " + property + " of component: " + webComponent);
			return;
		}
		IRecordInternal recordToUse = record;
		if (!ScopesUtils.isVariableScope(dataProviderID))
		{
			Object propertyValue = webComponent.getProperty(property);

			if (propertyValue instanceof FoundsetLinkedTypeSabloValue)
			{
				if (foundsetLinkedRowID != null)
				{
					// find the row of the foundset that the user started to edit; we can't use client's index (as server-side indexes might have changed meanwhile on server); so we are doing it based on client sent rowID
					recordToUse = getFoundsetLinkedRecord((FoundsetLinkedTypeSabloValue< ? , ? >)propertyValue, foundsetLinkedRowID);
					if (recordToUse == null)
					{
						Debug.error("Error executing startEdit (from client) for foundset linked DP (cannot find record): dp=" + propertyValue + ", rowID=" +
							foundsetLinkedRowID);
						return;
					}
				} // hmm, this is strange - usually we should always get rowID, even if foundset linked is actually set by developer to a global or form variable - even though there rowID is not actually needed; just treat this as if it is not record linked
			}

			if (recordToUse != null)
			{
				int rowIndex = recordToUse.getParentFoundSet().getRecordIndex(recordToUse);
				if (Arrays.binarySearch(recordToUse.getParentFoundSet().getSelectedIndexes(), rowIndex) < 0)
				{
					recordToUse.getParentFoundSet().setSelectedIndex(rowIndex);
				}
				recordToUse.startEditing();
			}
		}
	}

	public String getStringValue(String name)
	{
		String stringValue = TagResolver.formatObject(getValueObject(record, name), getApplication());
		return processValue(stringValue, name, null); // TODO last param ,IDataProviderLookup, should be implemented
	}

	public static String processValue(String stringValue, String dataProviderID, IDataProviderLookup dataProviderLookup)
	{
		if (stringValue == null)
		{
			if ("selectedIndex".equals(dataProviderID) || isCountOrAvgOrSumAggregateDataProvider(dataProviderID, dataProviderLookup)) //$NON-NLS-1$
			{
				return "0"; //$NON-NLS-1$
			}
		}
		return stringValue;
	}

	// helper method; not static because needs form scope
	public Object getValueObject(IRecord recordToUse, String dataProviderId)
	{
//		return record.getValue(dataProviderId);
		return com.servoy.j2db.dataprocessing.DataAdapterList.getValueObject(recordToUse, formController.getFormScope(), dataProviderId); // needed for tagString processing (so not just record values but also variables)
	}

	public boolean isCountOrAvgOrSumAggregateDataProvider(IDataAdapter dataAdapter)
	{
		return isCountOrAvgOrSumAggregateDataProvider(dataAdapter.getDataProviderID(), null);
	}

	private static boolean isCountOrAvgOrSumAggregateDataProvider(String dataProvider, IDataProviderLookup dataProviderLookup)
	{
		try
		{
			if (dataProviderLookup == null)
			{
				return false;
			}
			IDataProvider dp = dataProviderLookup.getDataProvider(dataProvider);
			if (dp instanceof ColumnWrapper)
			{
				dp = ((ColumnWrapper)dp).getColumn();
			}
			if (dp instanceof AggregateVariable)
			{
				int aggType = ((AggregateVariable)dp).getType();
				return aggType == QueryAggregate.COUNT || aggType == QueryAggregate.AVG || aggType == QueryAggregate.SUM;
			}
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}

		return false;
	}


	@Override
	public void setFindMode(boolean findMode)
	{
		if (this.findMode != findMode)
		{
			this.findMode = findMode;
			((BaseWebObject)formController.getFormUI()).setProperty("findmode", findMode);
			for (IFindModeAwarePropertyValue x : findModeAwareProperties)
			{
				x.findModeChanged(findMode);
			}
		}
	}

	public void notifyVisible(boolean b, List<Runnable> invokeLaterRunnables, Set<IWebFormController> childFormsThatWereAlreadyNotified)
	{
		HashMap<IWebFormController, String> childFormsCopy = new HashMap<IWebFormController, String>(visibleChildForms);
		for (IWebFormController relatedController : childFormsCopy.keySet())
		{
			updateParentContainer(relatedController, childFormsCopy.get(relatedController), b);
			if (!childFormsThatWereAlreadyNotified.contains(relatedController)) relatedController.notifyVisible(b, invokeLaterRunnables);
		}
	}

	public boolean stopUIEditing(boolean looseFocus)
	{
		for (IWebFormController relatedController : visibleChildForms.keySet())
		{
			if (!relatedController.stopUIEditing(looseFocus)) return false;
		}
		return true;
	}

	private void updateParentContainer(IWebFormController relatedController, String relationName, boolean visible)
	{
		if (((BasicFormController)relatedController).isDestroyed()) return;
		if (visible)
		{
			WebFormComponent parentContainer = null;
			Collection<WebComponent> components = formController.getFormUI().getComponents();
			for (WebComponent component : components)
			{
				// legacy behavior
				Object tabs = component.getProperty("tabs");
				if (tabs instanceof List && ((List)tabs).size() > 0)
				{
					List tabsList = (List)tabs;
					for (int i = 0; i < tabsList.size(); i++)
					{
						Map<String, Object> tab = (Map<String, Object>)tabsList.get(i);
						if (tab != null)
						{
							String relation = tab.get("relationName") != null ? tab.get("relationName").toString() : null;
							Object form = tab.get("containsFormId");
							if (Utils.equalObjects(form, relatedController.getName()) && Utils.equalObjects(relation, relationName))
							{
								parentContainer = (WebFormComponent)component;
								break;
							}
						}
					}
				}
			}
//			// for non legacy components, wait for client to set correct parent; do not set null
			if (parentContainer != null || !relatedController.getForm().isResponsiveLayout())
			{
				relatedController.getFormUI().setParentContainer(parentContainer);
			}
		}
	}

	public void destroy()
	{
		if (record != null)
		{
			setRecord(null, false);
		}
		if (formController != null && formController.getFormScope() != null)
		{
			formController.getFormScope().getModificationSubject().removeModificationListener(this);
		}
		if (formController != null && formController.getApplication() != null && formController.getApplication().getScriptEngine() != null)
		{
			IExecutingEnviroment er = formController.getApplication().getScriptEngine();
			if (er.getScopesScope() != null)
			{
				er.getScopesScope().getModificationSubject().removeModificationListener(this);
			}
		}
		dataProviderToLinkedComponentProperty.clear();
		allComponentPropertiesLinkedToData.clear();
		findModeAwareProperties.clear();
		parentRelatedForms.clear();
		visibleChildForms.clear();
	}
}
