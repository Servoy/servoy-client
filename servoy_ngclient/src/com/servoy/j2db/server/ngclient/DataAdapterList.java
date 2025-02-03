package com.servoy.j2db.server.ngclient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.sablo.BaseWebObject;
import org.sablo.Container;
import org.sablo.IWebObjectContext;
import org.sablo.IllegalChangeFromClientException;
import org.sablo.WebComponent;
import org.sablo.specification.PropertyDescriptionBuilder;
import org.sablo.specification.WebObjectApiFunctionDefinition;
import org.sablo.specification.WebObjectFunctionDefinition;
import org.sablo.specification.WebObjectSpecification.PushToServerEnum;
import org.sablo.specification.property.BrowserConverterContext;
import org.sablo.specification.property.IPropertyConverterForBrowser;
import org.sablo.specification.property.IPropertyType;
import org.sablo.specification.property.types.TypesRegistry;

import com.servoy.base.util.ITagResolver;
import com.servoy.j2db.ApplicationException;
import com.servoy.j2db.BasicFormController;
import com.servoy.j2db.FormAndTableDataProviderLookup;
import com.servoy.j2db.dataprocessing.FireCollector;
import com.servoy.j2db.dataprocessing.FoundSetEvent;
import com.servoy.j2db.dataprocessing.IDataAdapter;
import com.servoy.j2db.dataprocessing.IFoundSetEventListener;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IModificationListener;
import com.servoy.j2db.dataprocessing.IRecord;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.ISwingFoundSet;
import com.servoy.j2db.dataprocessing.ModificationEvent;
import com.servoy.j2db.dataprocessing.PrototypeState;
import com.servoy.j2db.dataprocessing.TagResolver;
import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.ColumnWrapper;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IDataProviderLookup;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.LiteralDataprovider;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.query.QueryAggregate;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.scripting.GlobalScope;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.scripting.ScopesScope;
import com.servoy.j2db.server.ngclient.component.EventExecutor;
import com.servoy.j2db.server.ngclient.property.DataproviderConfig;
import com.servoy.j2db.server.ngclient.property.FoundsetLinkedConfig;
import com.servoy.j2db.server.ngclient.property.FoundsetLinkedTypeSabloValue;
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


@SuppressWarnings("nls")
public class DataAdapterList implements IModificationListener, ITagResolver, IDataAdapterList
{

	// properties that are interested in a specific dataproviderID chaning
	protected final Map<String, List<IDataLinkedPropertyValue>> dataProviderToLinkedComponentProperty = new HashMap<>(); // dataProviderID -> [(comp, propertyName)]

	// all data-linked properties - contains 'dataProviderToLinkedComponentProperty' as well as other ones that are interested in any DP change
	protected final List<IDataLinkedPropertyValue> allComponentPropertiesLinkedToData = new ArrayList<>(); // [(comp, propertyName), ...]

	protected final List<IFindModeAwarePropertyValue> findModeAwareProperties = new ArrayList<>();

	private final IWebFormController formController;
	private final EventExecutor executor;
	private final WeakHashMap<IWebFormController, String> visibleChildForms = new WeakHashMap<>(3);
	private final ArrayList<IWebFormController> parentRelatedForms = new ArrayList<IWebFormController>(3);

	private Map<IDataLinkedPropertyValue, Pair<Relation[], List<RelatedListener>>> toWatchRelations;
	private DLPropertyValueFoundsetFoundsetListener maxRecIndexPropertyValueListener;
	private final Map<String, List<Pair<String, String>>> lookupDependency = new HashMap<String, List<Pair<String, String>>>();
	private final List<RelatedFoundsetListenerForChildVisibleForm> nestedRelatedFoundsetListeners = new ArrayList<RelatedFoundsetListenerForChildVisibleForm>();

	private IRecordInternal record;
	private boolean findMode = false;
	private boolean settingRecord;

	private boolean isFormScopeListener;
	private boolean isGlobalScopeListener;


	public DataAdapterList(IWebFormController formController)
	{
		this.formController = formController;
		this.executor = new EventExecutor(formController);

		ITable table = formController.getTable();
		if (table != null)
		{
			for (Column c : table.getColumns())
			{
				ColumnInfo ci = c.getColumnInfo();
				if (ci != null && ci.getAutoEnterType() == ColumnInfo.LOOKUP_VALUE_AUTO_ENTER)
				{
					String lookup = ci.getLookupValue();
					if (lookup != null && lookup.length() != 0 && !ScopesUtils.isVariableScope(lookup))
					{
						int index = lookup.lastIndexOf('.');
						if (index != -1)
						{
							try
							{
								Relation[] relations = formController.getApplication().getFlattenedSolution().getRelationSequence(lookup.substring(0, index));
								if (relations != null && relations.length > 0)
								{
									// add a RelookupAdapter for the last relation
									Relation relation = relations[relations.length - 1];
									IDataProvider[] dps = relation.getPrimaryDataProviders(formController.getApplication().getFlattenedSolution());
									if (dps != null)
									{
										StringBuilder prefix = new StringBuilder();
										for (int r = 0; r < relations.length - 1; r++)
										{
											prefix.append(relations[r].getName());
											prefix.append('.');
										}
										for (IDataProvider dp : dps)
										{
											if (dp instanceof LiteralDataprovider) continue;
											String dataProviderID;
											if (ScopesUtils.isVariableScope(dp.getDataProviderID()))
											{
												dataProviderID = dp.getDataProviderID();
											}
											else
											{
												dataProviderID = prefix.toString() + dp.getDataProviderID();
											}
											List<Pair<String, String>> lookupValueDependency = this.lookupDependency.get(dataProviderID);
											if (lookupValueDependency == null)
											{
												lookupValueDependency = new ArrayList<Pair<String, String>>();
												this.lookupDependency.put(dataProviderID, lookupValueDependency);
											}
											lookupValueDependency.add(new Pair(c.getDataProviderID(), lookup));
										}
									}
								}
							}
							catch (RepositoryException e)
							{
								Debug.error(e);
							}
						}
					}
				}
			}
		}
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

		// FIXME I think the convertRhinoToSabloComponentValue should only happen if
		// call comes from sablo/java (not Rhino - we don't currently have a reverse of IServerRhinoToRhino);
		// and this conversion has to be done before this method is even called... see SVY-18096

		WebObjectFunctionDefinition handlerDef = (webComponent != null ? webComponent.getSpecification().getHandler(event) : null);
		return NGConversions.INSTANCE.convertRhinoToSabloComponentValue(jsRetVal, null, handlerDef != null ? handlerDef.getReturnType() : null,
			(IWebObjectContext)webComponent);
	}

	/**
	 * @param args args to replace in script - used for HTML-triggered executeInlineScript; so calls generated via HTMLTagsConverter.convert(String, IServoyDataConverterContext, boolean) inside some piece of HTML
	 * @param appendingArgs args to append in script execution - used for component/service client side code triggered executeInlineScript.
	 */
	@Override
	public Object executeInlineScript(String script, JSONObject args, JSONArray appendingArgs)
	{
		String decryptedScript = HTMLTagsConverter.decryptInlineScript(script, args, getApplication().getFlattenedSolution());
		if (appendingArgs != null && decryptedScript != null && decryptedScript.endsWith("()"))
		{
			Object[] javaArguments = generateArguments(appendingArgs, formController);

			String functionName = decryptedScript.substring(0, decryptedScript.length() - 2);
			int startIdx = functionName.lastIndexOf('.');
			String noPrefixFunctionName = functionName.substring(startIdx > -1 ? startIdx + 1 : 0, functionName.length());

			Scriptable scope = null;
			Function f = null;

			if (functionName.startsWith("forms."))
			{
				String formName = functionName.substring("forms.".length(), startIdx);
				FormScope formScope = formController.getFormScope();
				// if this form controller doesn't match the formname of the script then take that scope
				// this is a function that is on a form that is not the active window form.
				if (!formController.getName().equals(formName))
				{
					formScope = getApplication().getFormManager().getForm(formName).getFormScope();
				}

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
				return formController.getApplication().getScriptEngine().executeFunction(f, scope, scope, javaArguments, false, false);
			}
			catch (Exception ex)
			{
				Debug.error(ex);
				return null;
			}
		} // else this is a executeInlineScript called from within a piece of HTML that was treated before being sent to client using HTMLTagsConverter.convert(...); all that was needed (including args) was already done inside the HTMLTagsConverter.decryptInlineScript() call above

		return decryptedScript != null ? formController.eval(decryptedScript) : null;
	}

	/**
	 * @param arguments
	 * @return
	 */
	public static Object[] generateArguments(JSONArray arguments, IWebFormController formController)
	{
		// this is an executeInlineScript called from component/service client-side code
		ArrayList<Object> javaArguments = new ArrayList<Object>();
		BrowserConverterContext dataConverterContext = new BrowserConverterContext((WebFormUI)formController.getFormUI(), PushToServerEnum.allow);
		for (int i = 0; i < arguments.length(); i++)
		{
			Object argObj = ServoyJSONObject.jsonNullToNull(arguments.get(i));
			try
			{
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
										 * TODO this shouldn't be null! Make this better - maybe parse the type or just instantiate a property description if we
										 * don't want full support for what can be defined in spec file as a type
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
		return javaArguments.toArray();
	}

	private static boolean containsForm(IWebFormUI parent, IWebFormUI child)
	{
		if (parent == child) return true;
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
					fc.notifyVisible(false, invokeLaterRunnables, true);
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
						newFormController.notifyVisible(formController.isFormVisible(), invokeLaterRunnables, true);
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
		if (form == formController)
		{
			Debug.error("Form " + form + " is added as a visible child form over relation " + relation + " to itself ",
				new RuntimeException("Form " + form + " is added as a visible child form over relation " + relation + " to itself "));
			return;
		}

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
			Relation[] relations = formController.getApplication().getFlattenedSolution().getRelationSequence(relation);
			if (relations != null)
			{
				if (relations.length > 0)
				{
					// if selection changes or the current record changes columns that are used as keys somewhere in the relation sequence then a reload of the nested form's foundset needs to happen
					// the NestedRelatedListener constructor will recursively create one NestedRelatedListener for each intermediat relation in the relation chain
					nestedRelatedFoundsetListeners.add(new RelatedFoundsetListenerForChildVisibleForm(relations, 0, null,
						form, relation, this));
				}
				if (!isGlobalScopeListener)
				{
					for (Relation relationObj : relations)
					{
						if (relationObj != null && relationObj.containsGlobal())
						{
							formController.getApplication().getScriptEngine().getScopesScope().getModificationSubject().addModificationListener(this);
							isGlobalScopeListener = true;
							break;
						}
					}
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
			this.nestedRelatedFoundsetListeners.stream()
				.filter(listener -> Utils.equalObjects(form, listener.leafRelatedFormController))
				.forEach(nestedRelatedListener -> nestedRelatedListener.dispose());
			this.nestedRelatedFoundsetListeners.removeIf(listener -> Utils.equalObjects(form, listener.leafRelatedFormController));
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

	protected void setupModificationListener(String dataprovider)
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

	private String[] getAllDependentDataProviders(String[] dataproviders)
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
										if (!returnDP.contains(idp.getDataProviderID())) returnDP.add(idp.getDataProviderID());
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
			if (!returnDP.contains(dp)) returnDP.add(dp);
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
			dataproviders = getAllDependentDataProviders(dataproviders);
		}
		for (String dpID : dataproviders)
		{
			List<IDataLinkedPropertyValue> allLinksOfDP = dataProviderToLinkedComponentProperty.get(dpID);
			if (allLinksOfDP == null)
			{
				allLinksOfDP = new ArrayList<>();
				dataProviderToLinkedComponentProperty.put(dpID, allLinksOfDP);
				if ("maxRecordIndex".equalsIgnoreCase(dpID) || "lazyMaxRecordIndex".equalsIgnoreCase(dpID))
				{
					if (maxRecIndexPropertyValueListener == null)
					{
						maxRecIndexPropertyValueListener = new DLPropertyValueFoundsetFoundsetListener();
					}
					maxRecIndexPropertyValueListener.addPropertyValueToList(propertyValue);
				}
			}
			if (allLinksOfDP.remove(propertyValue))
			{
				Debug.warn("DAL.addDataLinkedProperty - trying to register the same (equal) property value twice (" + propertyValue +
					"); this means that some code that uses DAL is not working properly (maybe cleanup/detach malfunction); will use latest value... Links: " +
					targetDataLinks);
			}

			allLinksOfDP.add(propertyValue);

			if (formController != null) setupModificationListener(dpID); // see if we need to listen to global/form scope changes
		}

		if (!allComponentPropertiesLinkedToData.contains(propertyValue)) allComponentPropertiesLinkedToData.add(propertyValue);

		if (targetDataLinks.relations != null)
		{
			if (toWatchRelations == null) toWatchRelations = new HashMap<>(3);
			toWatchRelations.put(propertyValue, new Pair<Relation[], List<RelatedListener>>(targetDataLinks.relations, Collections.emptyList()));
			createRelationListeners(propertyValue);
		}
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
		// TODO keep track & unregister when needed global/form scope listeners: so undo setupModificationListener(dpID)? they are only max two listeners and they are removed at destroy anyway, but if there are no DPs needing it anymore...
		allComponentPropertiesLinkedToData.remove(propertyValue);

		// remove any relation listeners that may be set for this property value
		if (toWatchRelations != null)
		{
			Pair<Relation[], List<RelatedListener>> toWatchRelationsForPropertyValue = toWatchRelations.remove(propertyValue);
			if (toWatchRelationsForPropertyValue != null)
			{
				toWatchRelationsForPropertyValue.getRight().forEach(listener -> listener.dispose());
				toWatchRelationsForPropertyValue.getRight().clear();
			}
		}

		if (maxRecIndexPropertyValueListener != null)
		{
			maxRecIndexPropertyValueListener.dispose();
		}
	}

	public void addFindModeAwareProperty(IFindModeAwarePropertyValue propertyValue)
	{
		if (!findModeAwareProperties.contains(propertyValue)) findModeAwareProperties.add(propertyValue);
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
				throw new IllegalStateException(
					"Record " + record + " is being set on DAL when record: " + this.record + " is being processed for form " + getForm());
			}
			return;
		}
		try
		{
			FireCollector fireCollector = FireCollector.getFireCollector();
			try
			{
				settingRecord = true;
				formController.getFormUI().setChanging(true);
				if (this.record != null)
				{
					this.record.removeModificationListener(this);
					this.record.getParentFoundSet().removeAggregateModificationListener(this);
				}
				this.record = (IRecordInternal)record;

				if (this.record != null)
				{
					pushChangedValues(null, fireChangeEvent);
					this.record.addModificationListener(this);
					this.record.getParentFoundSet().addAggregateModificationListener(this);
				}
				createRelationListeners();
				if (record != null) tellNestedRelatedListenersThatMainDALRecordHasChanged(); // if record == null it could be due to a hide of the main form, in which case we want the related forms to not set their foundsets to null, but keep the last related foundset where the form will be shown next (if another foundset/relation is not enforced on that form then)
				if (maxRecIndexPropertyValueListener != null) maxRecIndexPropertyValueListener.setRecord(this.record);
			}
			finally
			{
				formController.getFormUI().setChanging(false);
				settingRecord = false;
				fireCollector.done();
			}
		}
		catch (RuntimeException re)
		{
			throw new RuntimeException("Error setting record " + record + " on form " + getForm() + " on DAL " + this, re);
		}

	}

	private void createRelationListeners()
	{
		if (toWatchRelations != null) toWatchRelations.keySet().forEach(key -> createRelationListeners(key));
	}

	private void createRelationListeners(IDataLinkedPropertyValue propertyValue)
	{
		// first remove the previous ones
		Pair<Relation[], List<RelatedListener>> pair = toWatchRelations.get(propertyValue);
		pair.getRight().forEach(listener -> listener.dispose());
		pair.getRight().clear();

		if (this.record != null)
		{
			Relation[] relationsToWatch = pair.getLeft();
			IRecordInternal current = this.record;
			ArrayList<RelatedListener> listeners = new ArrayList<>(relationsToWatch.length);
			for (Relation element : relationsToWatch)
			{
				IFoundSetInternal related = current.getRelatedFoundSet(element.getName());
				if (related != null)
				{
					// if selection changes or the current record changes then an update should happen.
					listeners.add(new RelatedListener(propertyValue, related));
					current = related.getRecord(related.getSelectedIndex());
					if (current == null) break;
				}
				else break;
			}
			pair.setRight(listeners);
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

	protected final boolean isGlobalDataprovider(String dataprovider)
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

	protected void pushChangedValues(String dataProvider, boolean fireChangeEvent)
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
				if (canFirePropertyValueListener(x))
				{
					x.dataProviderOrRecordChanged(record, null, isFormDP, isGlobalDP, fireChangeEvent);
				}
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
					if (canFirePropertyValueListener(x))
					{
						x.dataProviderOrRecordChanged(record, dataProvider, isFormDP, isGlobalDP, fireChangeEvent);
					}
				}
			}
		}

		if (fireChangeEvent && changed)
		{
			getApplication().getChangeListener().valueChanged();
		}
	}

	protected boolean canFirePropertyValueListener(IDataLinkedPropertyValue propertyValue)
	{
		return true;
	}

	@Override
	public void valueChanged(ModificationEvent e)
	{
		if (!findMode && this.record != null && e.getName() != null && (e.getRecord() == null || e.getRecord() == this.record) &&
			this.lookupDependency.containsKey(e.getName()) &&
			!(this.record instanceof PrototypeState))
		{
			if (this.record.startEditing())
			{
				List<Pair<String, String>> lookupDepencies = this.lookupDependency.get(e.getName());
				for (Pair<String, String> dependency : lookupDepencies)
				{
					Object obj = this.record.getValue(dependency.getRight());
					this.record.setValue(dependency.getLeft(), obj);
				}
			}
		}

		// one of the relations could be changed make sure they are recreated.
		createRelationListeners();
		if (e.getRecord() == null) tellNestedRelatedListenersThatGlobalOrScopeVariableChanged(e.getName()); // if it's a change in the record, they will refresh anyway due to direct foundset listeners added by nestedRelatedFoundsetListeners or due to DAL.setRecord(...)

		pushChangedValues(e.getName(), true);
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

	public void pushChanges(WebFormComponent webComponent, String beanProperty, Object value, String foundsetLinkedRowID)
	{
		// TODO should this all (svy-apply/push) move to DataProviderType client/server side implementation instead of these specialized calls, instanceof checks and string parsing (see getProperty or getPropertyDescription)?
		try
		{
			String dataProviderID = getDataProviderID(webComponent, beanProperty);
			if (dataProviderID == null)
			{
				Debug.log(
					"apply called on a property that is not bound to a dataprovider: " + beanProperty + ", value: " + value + " of component: " + webComponent);
				return;
			}

			Object newValue = value;
			// Check security
			webComponent.checkThatPropertyAllowsUpdateFromClient(beanProperty);

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

			if (editingRecord == null || editingRecord.startEditing() || editingRecord.getParentFoundSet().getColumnIndex(dataProviderID) == -1)
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
					if (value instanceof DataproviderTypeSabloValue) ((DataproviderTypeSabloValue)value).checkValueForChanges(editingRecord);
				}
				catch (IllegalArgumentException e)
				{
					Debug.trace(e);
					getApplication().handleException(null, new ApplicationException(ServoyException.INVALID_INPUT, e));
					setValueException = e;
					webComponent.setInvalidState(true);
				}
				DataproviderConfig dataproviderConfig = getDataproviderConfig(webComponent, beanProperty);
				String onDataChange = dataproviderConfig.getOnDataChange();

				if (onDataChange != null)
				{
					JSONObject event = EventExecutor.createEvent(onDataChange, editingRecord.getParentFoundSet().getSelectedIndex());
					event.put("data", createDataproviderInfo(editingRecord, formController.getFormScope(), dataProviderID));
					Object returnValue = null;
					Exception exception = null;
					String onDataChangeCallback = null;
					if (!Utils.equalObjects(oldValue, v) && setValueException == null && webComponent.hasEvent(onDataChange))
					{
						getApplication().getWebsocketSession().getClientService("$sabloLoadingIndicator").executeAsyncNowServiceCall("showLoading", null); //$NON-NLS-1$ //$NON-NLS-2$
						try
						{
							returnValue = webComponent.executeEvent(onDataChange, new Object[] { oldValue, v, event });
						}
						catch (Exception e)
						{
							Debug.error("Error during onDataChange webComponent=" + webComponent, e);
							exception = e;
						}
						finally
						{
							getApplication().getWebsocketSession().getClientService("$sabloLoadingIndicator").executeAsyncNowServiceCall("hideLoading", null); //$NON-NLS-1$ //$NON-NLS-2$
						}
						onDataChangeCallback = dataproviderConfig.getOnDataChangeCallback();

					}
					else if (setValueException != null)
					{
						returnValue = setValueException.getMessage();
						exception = setValueException;
						onDataChangeCallback = dataproviderConfig.getOnDataChangeCallback();

					}
					else if (webComponent.isInvalidState() && exception == null)
					{
						onDataChangeCallback = dataproviderConfig.getOnDataChangeCallback();
						webComponent.setInvalidState(false);

					}
					if (onDataChangeCallback != null)
					{
						WebObjectApiFunctionDefinition call = createWebObjectFunction(onDataChangeCallback);
						webComponent.invokeApi(call, new Object[] { event, returnValue, exception == null ? null : exception.getMessage() });
					}
				}
			}
		}
		catch (IllegalChangeFromClientException e)
		{
			// we always want to print a warning in the log if a data push was denied
			// due to form becomming hidden, even if it was hidden just milliseconds before
			// the deny; we should know if real dataprovider data was discarded due to this...
			e.setShouldPrintWarningToLog(true);
			throw e;
		}
	}

	private JSONObject createDataproviderInfo(IRecord record, FormScope fs, String dataProviderID)
	{
		Object scope = null;
		String scopeID = null;
		Pair<String, String> glScope = ScopesUtils.getVariableScope(dataProviderID);
		if (glScope.getLeft() != null)
		{
			try
			{
				scope = fs.getFormController().getApplication().getScriptEngine().getScopesScope().getGlobalScope(glScope.getLeft());
				dataProviderID = ScriptVariable.SCOPES_DOT_PREFIX + glScope.getRight();
				scopeID = glScope.getLeft();
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
		else if (fs.has(dataProviderID, fs))
		{
			scope = fs;
			scopeID = "forms." + fs.getScopeName();
		}
		else
		{
			scope = record;
			scopeID = record.getDataSource();
		}

		JSONObject dpInfo = new JSONObject();
		dpInfo.put("dataprovider", dataProviderID);
		dpInfo.put("scope", scope);
		dpInfo.put("scopeid", scopeID);
		return dpInfo;
	}

	static DataproviderConfig getDataproviderConfig(WebFormComponent webComponent, String beanProperty)
	{
		Object config = webComponent.getFormElement().getWebComponentSpec().getProperty(beanProperty).getConfig();
		if (config instanceof FoundsetLinkedConfig)
		{
			config = ((FoundsetLinkedConfig)config).getWrappedPropertyDescription().getConfig();
		}
		return config instanceof DataproviderConfig ? (DataproviderConfig)config : null;
	}

	static WebObjectApiFunctionDefinition createWebObjectFunction(String onDataChangeCallback)
	{
		WebObjectApiFunctionDefinition call = new WebObjectApiFunctionDefinition(onDataChangeCallback);
		call.addParameter(new PropertyDescriptionBuilder().withName("event").withType(TypesRegistry.getType("object")).build());
		call.addParameter(new PropertyDescriptionBuilder().withName("returnValue").withType(TypesRegistry.getType("object")).build());
		call.addParameter(new PropertyDescriptionBuilder().withName("exception").withConfig(TypesRegistry.getType("object")).build());
		return call;
	}

	private IRecordInternal getFoundsetLinkedRecord(FoundsetLinkedTypeSabloValue< ? , ? > foundsetLinkedValue, String foundsetLinkedRowID)
	{
		IRecordInternal recordForRowID = null;

		int index = foundsetLinkedValue.getFoundset().getRecordIndex(foundsetLinkedRowID, foundsetLinkedValue.getRecordIndexHint());
		if (index >= 0) recordForRowID = foundsetLinkedValue.getFoundset().getRecord(index);

		return recordForRowID;
	}

	public void startEdit(WebFormComponent webComponent, String property, String foundsetLinkedRowID)
	{
		// TODO should this all (startEdit) move to DataProviderType client/server side implementation instead of these specialized calls, instanceof checks and string parsing (see getProperty or getPropertyDescription)?

		try
		{
			webComponent.checkThatPropertyAllowsUpdateFromClient(property);
		}
		catch (IllegalChangeFromClientException ex)
		{
			//ignore, this is just to check if we can edit it, if not, do not enter edit mode
			return;
		}
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
		String stringValue = TagResolver.formatObject(getValueObjectForTagResolver(record, name), getApplication());
		ITable table = record != null ? record.getParentFoundSet().getTable() : null;
		FormAndTableDataProviderLookup dataproviderLookup = formController != null ? new FormAndTableDataProviderLookup(
			formController.getApplication().getFlattenedSolution(), formController.getForm(), table != null ? table : formController.getTable()) : null;
		return processValue(stringValue, name, dataproviderLookup);
	}

	public static String processValue(String stringValue, String dataProviderID, IDataProviderLookup dataProviderLookup)
	{
		if (stringValue == null)
		{
			if ("maxRecordIndex".equals(dataProviderID) || "selectedIndex".equals(dataProviderID) || //$NON-NLS-1$
				isCountOrAvgOrSumAggregateDataProvider(dataProviderID, dataProviderLookup))
			{
				return "0"; //$NON-NLS-1$
			}
			if (dataProviderID != null && (dataProviderID.endsWith(".selectedIndex") || dataProviderID.endsWith(".maxRecordIndex")))
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

	protected Object getValueObjectForTagResolver(IRecord recordToUse, String dataProviderId)
	{
		return getValueObject(recordToUse, dataProviderId);
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
		HashMap<IWebFormController, String> childFormsCopy = getVisibleChildFormCopy();
		for (IWebFormController relatedController : childFormsCopy.keySet())
		{
			if (!relatedController.isDestroyed())
			{
				updateParentContainer(relatedController, childFormsCopy.get(relatedController), b);
				if (!childFormsThatWereAlreadyNotified.contains(relatedController)) relatedController.notifyVisible(b, invokeLaterRunnables, false);
			}
		}
		if (!b)
		{
			clearNestedRelatedFoundsetListeners();
			visibleChildForms.clear();
		}
	}

	/**
	 * @return
	 */
	private HashMap<IWebFormController, String> getVisibleChildFormCopy()
	{
		return new HashMap<IWebFormController, String>(visibleChildForms);
	}

	public boolean stopUIEditing(boolean looseFocus)
	{
		for (IWebFormController relatedController : getVisibleChildFormCopy().keySet())
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
					for (Object element : tabsList)
					{
						Map<String, Object> tab = (Map<String, Object>)element;
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
		clearNestedRelatedFoundsetListeners();
		dataProviderToLinkedComponentProperty.clear();
		allComponentPropertiesLinkedToData.clear();
		findModeAwareProperties.clear();
		parentRelatedForms.clear();
		visibleChildForms.clear();
	}

	public void clearNestedRelatedFoundsetListeners()
	{
		for (RelatedFoundsetListenerForChildVisibleForm listener : nestedRelatedFoundsetListeners)
		{
			listener.dispose();
		}
		nestedRelatedFoundsetListeners.clear();
	}

	public void tellNestedRelatedListenersThatMainDALRecordHasChanged()
	{
		for (RelatedFoundsetListenerForChildVisibleForm listener : nestedRelatedFoundsetListeners)
		{
			listener.rootDALRecordHasChanged();
		}
	}

	public void tellNestedRelatedListenersThatGlobalOrScopeVariableChanged(String globalScopeChangedVariableName)
	{
		for (RelatedFoundsetListenerForChildVisibleForm listener : nestedRelatedFoundsetListeners)
		{
			listener.globalOrScopeVariableChanged(globalScopeChangedVariableName);
		}
	}

	@Override
	public String toString()
	{
		return "DAL[form:" + getForm() + ", record:" + getRecord() + "]";
	}

	private static class DLPropertyValueFoundsetFoundsetListener implements IFoundSetEventListener
	{
		private IRecordInternal recordInternal;
		private final List<IDataLinkedPropertyValue> propertyValues = new ArrayList<IDataLinkedPropertyValue>(2);
		private IFoundSetInternal foundset;

		public void setRecord(IRecordInternal recordInternal)
		{
			if (this.recordInternal != recordInternal)
			{
				this.recordInternal = recordInternal;
				if (this.recordInternal != null)
				{
					if (this.foundset != this.recordInternal.getParentFoundSet())
					{
						if (this.foundset != null) foundset.removeFoundSetEventListener(this);
						this.foundset = this.recordInternal.getParentFoundSet();
						this.foundset.addFoundSetEventListener(this);
					}
				}
				else if (this.foundset != null)
				{
					this.foundset.removeFoundSetEventListener(this);
					this.foundset = null;
				}
			}
		}

		public void dispose()
		{
			if (this.foundset != null)
			{
				this.foundset.removeFoundSetEventListener(this);
				this.recordInternal = null;
				this.foundset = null;
			}
		}

		public void addPropertyValueToList(IDataLinkedPropertyValue propertyValue)
		{
			propertyValues.add(propertyValue);
		}

		@Override
		public void foundSetChanged(FoundSetEvent e)
		{
			if (e.getType() == FoundSetEvent.CONTENTS_CHANGED)
			{
				propertyValues.forEach(propertyValue -> propertyValue.dataProviderOrRecordChanged(this.recordInternal, null, false, false, true));
			}
		}
	}

	/**
	 * Class used to listen to related foundset changes in case a related data-provider was registered to this DAL.
	 *
	 * TODO see if it makes sense to merge this class with {@link RelatedFoundsetListenerForChildVisibleForm} class...
	 */
	private class RelatedListener implements ListSelectionListener, IModificationListener, IFoundSetEventListener
	{

		private final IFoundSetInternal related;
		private final IRecordInternal selectedRecord;
		private final IDataLinkedPropertyValue propertyValue;

		public RelatedListener(IDataLinkedPropertyValue propertyValue, IFoundSetInternal related)
		{
			this.propertyValue = propertyValue;
			this.related = related;
			if (this.related instanceof ISwingFoundSet)
			{
				((ISwingFoundSet)this.related).getSelectionModel().addListSelectionListener(this);
			}
			this.related.addFoundSetEventListener(this);
			selectedRecord = this.related.getRecord(this.related.getSelectedIndex());
			if (selectedRecord != null)
			{
				selectedRecord.addModificationListener(this);
			}
		}

		public void dispose()
		{
			if (this.related instanceof ISwingFoundSet)
			{
				((ISwingFoundSet)this.related).getSelectionModel().removeListSelectionListener(this);
			}
			this.related.removeFoundSetEventListener(this);
			if (selectedRecord != null)
			{
				selectedRecord.removeModificationListener(this);
			}
		}

		private void changed()
		{
			propertyValue.dataProviderOrRecordChanged(getRecord(), null, false, false, true);
			createRelationListeners(propertyValue);
		}

		@Override
		public void valueChanged(ListSelectionEvent e)
		{
			changed();
		}

		@Override
		public void valueChanged(ModificationEvent e)
		{
			changed();
		}

		@Override
		public void foundSetChanged(FoundSetEvent e)
		{
			if (this.related.getRecord(this.related.getSelectedIndex()) != this.selectedRecord)
			{
				changed();
			}
		}
	}

	/**
	 * Class used to listen to relation changes in related foundset chains used by related child visible forms.
	 * It will reload records in those related forms when needed.
	 */
	private class RelatedFoundsetListenerForChildVisibleForm implements ListSelectionListener, IFoundSetEventListener
	{

		private final String intermediateRelationName;
		private final Set<String> nextIntermediateRelationScopeKeys = new HashSet<>();
		private final Set<String> nextIntermediateRelationColumnKeys = new HashSet<>();
		private IFoundSetInternal foundsetToListenTo;
		private final IWebFormController leafRelatedFormController;
		private final String fullRelationName;
		private final DataAdapterList dal;
		private IRecordInternal selectedRecord;

		private final RelatedFoundsetListenerForChildVisibleForm nextIntermediateRelationListener;

		/**
		 * This listens to changes in the selected record of foundset determined by "intermediateRelationName" (or DAL foundset if intermediateRelationName is null)
		 * or in columns from the selected record of it that are used as keys in the "relations[currentIntermediateRelationIndex]".<br/><br/>
		 *
		 * If there are such changes, then the given formController will reload it's records based on fullRelationName.
		 *
		 * @param relations the full relation sequence that needs to be listened to; current NestedRelatedListener will only look at relations[currentIntermediateRelationIndex]
		 * @param currentIntermediateRelationIndex the index in "relations" that this NestedRelatedListener creates listeners for
		 * @param intermediateRelationName the relation of the related foundset to look at for selections changes and column changes that might affect; can be null and then we watch the 'dal''s foundset directly (it is for the first relation in the chain);
		 *                                 this is the String equivalent to the relation sequence up to (excluding) relations[currentIntermediateRelationIndex]; initially it is null
		 * @param leafRelatedFormController the form controller that corresponds to given fullRelationName
		 * @param fullRelationName the full relation name of the (end) form for which we are watching selection and column changes
		 * @param dal the DAL of the parent form that sees leafRelatedFormController becoming visible inside it (nested in tab panels etc.)
		 */
		public RelatedFoundsetListenerForChildVisibleForm(Relation[] relations, int currentIntermediateRelationIndex, String intermediateRelationName,
			IWebFormController leafRelatedFormController, String fullRelationName, DataAdapterList dal)
		{
			this.intermediateRelationName = intermediateRelationName;
			IDataProvider[] nIRColumns = null;
			try
			{
				nIRColumns = relations[currentIntermediateRelationIndex].getPrimaryDataProviders(dal.getApplication().getFlattenedSolution());
				for (IDataProvider dp : nIRColumns)
				{
					String dpID = dp.getDataProviderID();
					if (ScopesUtils.isVariableScope(dpID)) nextIntermediateRelationScopeKeys.add(dpID);
					else nextIntermediateRelationColumnKeys.add(dpID);
				}
			}
			catch (RepositoryException e)
			{
				Debug.log(e);
			}

			this.leafRelatedFormController = leafRelatedFormController;
			this.fullRelationName = fullRelationName;
			this.dal = dal;

			addListeners();

			if (currentIntermediateRelationIndex < relations.length - 1 && relations[currentIntermediateRelationIndex + 1] != null)
				nextIntermediateRelationListener = new RelatedFoundsetListenerForChildVisibleForm(relations, currentIntermediateRelationIndex + 1,
					intermediateRelationName == null ? relations[currentIntermediateRelationIndex].getName()
						: intermediateRelationName + "." + relations[currentIntermediateRelationIndex].getName(),
					leafRelatedFormController, fullRelationName, dal);
			else nextIntermediateRelationListener = null;
		}

		private void addListeners()
		{
			if (this.dal.getRecord() != null)
			{
				if (intermediateRelationName != null)
					this.foundsetToListenTo = this.dal.getRecord().getRelatedFoundSet(intermediateRelationName);
				else
					this.foundsetToListenTo = this.dal.getRecord().getParentFoundSet();

				if (this.foundsetToListenTo != null)
				{
					if (this.foundsetToListenTo instanceof ISwingFoundSet && intermediateRelationName != null) // if intermediateRelationName == null then this is the foundet of the DAL itself, and the DAL will notify record selection changed itself, because the foundset itself might change...
					{
						((ISwingFoundSet)this.foundsetToListenTo).getSelectionModel().addListSelectionListener(this);
					}
					selectedRecord = this.foundsetToListenTo.getRecord(this.foundsetToListenTo.getSelectedIndex());
					this.foundsetToListenTo.addFoundSetEventListener(this);
				}
			}
		}

		private void recreateListenersInChain()
		{
			addListeners();
			if (nextIntermediateRelationListener != null) nextIntermediateRelationListener.addListeners();
		}

		public void dispose()
		{
			if (nextIntermediateRelationListener != null) nextIntermediateRelationListener.dispose();

			if (this.foundsetToListenTo != null)
			{
				if (this.foundsetToListenTo instanceof ISwingFoundSet && intermediateRelationName != null)
				{
					((ISwingFoundSet)this.foundsetToListenTo).getSelectionModel().removeListSelectionListener(this);
				}
				this.foundsetToListenTo.removeFoundSetEventListener(this);
				this.foundsetToListenTo = null;
				this.selectedRecord = null;
			}
		}

		public void globalOrScopeVariableChanged(String globalScopeChangedVariableName)
		{
			// a global/scope variable changed
			if (nextIntermediateRelationScopeKeys.contains(globalScopeChangedVariableName))
			{
				if (nextIntermediateRelationListener != null) nextIntermediateRelationListener.refreshListeners();
				reloadRecordsOnEndFormController(); // a global/scope variable that is used as a key in the next intermediate relation has changed; reload of records is needed
			}
			else if (nextIntermediateRelationListener != null) nextIntermediateRelationListener.globalOrScopeVariableChanged(globalScopeChangedVariableName);
		}

		/**
		 * The first NestedRelatedListener in a chain of relations that leads to a related form controller gets it's selection change
		 * from the DAL directly, just in case the foundset of the DAL has changed as well - so the listeners are recreated on the new foundset then.
		 */
		public void rootDALRecordHasChanged()
		{
			this.refreshListeners(); // it is possible that the selected record is from another foundset - so refresh including 'this', not just from 'nextIntermediateRelationListener'
			selectedRecordChanged();
		}

		@Override
		public void valueChanged(ListSelectionEvent e)
		{
			// selected index changed...
			selectedRecordChanged();
		}

		private void selectedRecordChanged()
		{
			reloadRecordsOnEndFormController();
		}

		private void refreshListeners()
		{
			dispose(); // calls it for whole relation chain starting from this NestedRelatedListener's intermediate relation
			recreateListenersInChain(); // calls it for whole relation chain starting from this NestedRelatedListener's intermediate relation
		}

		private void reloadRecordsOnEndFormController()
		{
			leafRelatedFormController.loadRecords(
				(this.dal.getRecord() != null ? this.dal.getRecord().getRelatedFoundSet(fullRelationName,
					((BasicFormController)leafRelatedFormController).getDefaultSortColumns()) : null));
		}

		@Override
		public void foundSetChanged(FoundSetEvent e)
		{
			if (this.foundsetToListenTo != null)
			{
				int selectedIndex = this.foundsetToListenTo.getSelectedIndex();
				if (this.foundsetToListenTo.getRecord(selectedIndex) != this.selectedRecord)
				{
					// selected record has changed, but maybe selected index did not (record deletes/inserts etc. could do that)
					selectedRecordChanged();
				}
				else
				{
					// same selected record; if it was a change that did affect the current record; check
					// changed column(s) vs relation keys; see if columns that are used as keys in relations have changed
					if (this.selectedRecord != null && e != null && e.getType() == FoundSetEvent.CONTENTS_CHANGED &&
						e.getChangeType() == FoundSetEvent.CHANGE_UPDATE && e.getFirstRow() <= selectedIndex && e.getLastRow() >= selectedIndex &&
						(e.getDataProviders() == null ||
							e.getDataProviders().stream().anyMatch((columnName) -> nextIntermediateRelationColumnKeys.contains(columnName))))
					{
						if (nextIntermediateRelationListener != null) nextIntermediateRelationListener.refreshListeners();
						reloadRecordsOnEndFormController(); // a column that is used as a key in the next intermediate relation has changed; reload of records is needed
					}
				}
			}
		}

	}
}
