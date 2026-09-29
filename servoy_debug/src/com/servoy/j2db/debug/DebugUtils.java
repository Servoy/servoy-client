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
package com.servoy.j2db.debug;

import java.awt.EventQueue;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

import org.eclipse.dltk.rhino.dbgp.DBGPDebugger;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.RhinoException;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.SpecProviderState;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectSpecification;

import com.servoy.j2db.ClientState;
import com.servoy.j2db.IFormController;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.FoundSetManager;
import com.servoy.j2db.debug.DebugJ2DBClient.DebugSwingFormMananger;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.FlattenedForm;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.Menu;
import com.servoy.j2db.persistence.MenuItem;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Style;
import com.servoy.j2db.persistence.Tab;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.scripting.LazyCompilationScope;
import com.servoy.j2db.server.ngclient.property.types.FormComponentPropertyType;
import com.servoy.j2db.server.ngclient.property.types.MenuPropertyType;
import com.servoy.j2db.server.ngclient.property.types.RelationPropertyType;
import com.servoy.j2db.server.ngclient.property.types.ValueListPropertyType;
import com.servoy.j2db.server.ngclient.template.FormTemplateGenerator;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Utils;

public class DebugUtils
{
	public interface DebugUpdateFormSupport
	{
		public void updateForm(Form form);
	}

	/**
	 * Optional, owner-scoped sink that receives a copy of every stdout/stderr message that is forwarded to the debugger console
	 * ({@link #stdoutToDebugger(IExecutingEnviroment, Object)} / {@link #stderrToDebugger(IExecutingEnviroment, Object)} /
	 * {@link #errorToDebugger(IExecutingEnviroment, String, Object)}). It is used to capture the console output produced while a single
	 * ad-hoc script/method is executed in a running debug client (see the servoy-debug MCP tool).
	 * <p>
	 * Runs are expected to be serialized on the client's single event-dispatch thread, but overlapping/queued scheduling on that thread is
	 * still made safe by keying the sink to an <b>owner token</b>: {@link #setOutputSink(Object, Consumer)} only takes ownership, and both
	 * {@link #captureOutput(Object)} and {@link #removeOutputSink(Object)} act only while the passed owner is still the active one. A stale
	 * or abandoned (timed-out) run therefore cannot steal, clear or write into a newer run's sink. The static fields are guarded by
	 * {@link #SINK_LOCK} so a callback firing on any thread stays consistent.
	 */
	private static final Object SINK_LOCK = new Object();

	private static Object currentSinkOwner;

	private static Consumer<String> currentSink;

	/**
	 * Installs an output sink owned by {@code owner}, replacing any previously installed sink. Must be paired with
	 * {@link #removeOutputSink(Object)} (typically in a finally block) using the same {@code owner}. Any stdout/stderr text forwarded to the
	 * debugger while this owner is active is also passed to {@code sink}.
	 *
	 * @param owner a unique, non-null token identifying this run; used to guard against stale/overlapping runs.
	 * @param sink the consumer to receive captured console text; must not be null.
	 */
	public static void setOutputSink(Object owner, Consumer<String> sink)
	{
		if (owner == null || sink == null) return;
		synchronized (SINK_LOCK)
		{
			currentSinkOwner = owner;
			currentSink = sink;
		}
	}

	/**
	 * Removes the output sink previously installed with {@link #setOutputSink(Object, Consumer)} <b>only if</b> {@code owner} still owns it.
	 * A stale/abandoned run whose ownership has since been taken over by a newer run is a no-op, so it can never clear the newer run's sink.
	 *
	 * @param owner the same token that was passed to {@link #setOutputSink(Object, Consumer)}.
	 */
	public static void removeOutputSink(Object owner)
	{
		if (owner == null) return;
		synchronized (SINK_LOCK)
		{
			if (currentSinkOwner == owner)
			{
				currentSinkOwner = null;
				currentSink = null;
			}
		}
	}

	private static void captureOutput(Object message)
	{
		Consumer<String> sink;
		synchronized (SINK_LOCK)
		{
			sink = currentSink;
		}
		if (sink != null)
		{
			try
			{
				sink.accept(message == null ? "<null>" : message.toString());
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
	}

	public static void errorToDebugger(IExecutingEnviroment engine, String message, Object errorDetail)
	{
		Object detail = errorDetail;
		if (engine instanceof RemoteDebugScriptEngine)
		{
			DBGPDebugger debugger = ((RemoteDebugScriptEngine)engine).getDebugger();
			if (debugger != null)
			{
				RhinoException rhinoException = null;
				if (detail instanceof Exception)
				{
					Throwable exception = (Exception)detail;
					while (exception != null)
					{
						if (exception instanceof RhinoException)
						{
							rhinoException = (RhinoException)exception;
							break;
						}
						exception = exception.getCause();
					}
				}
				String msg = message;
				if (rhinoException != null)
				{
					if (msg == null)
					{
						msg = rhinoException.getLocalizedMessage();
					}
					else msg += '\n' + rhinoException.getLocalizedMessage();
					msg += '\n' + rhinoException.getScriptStackTrace();
				}
				else if (detail instanceof Exception)
				{
					Object e = ((Exception)detail).getCause();
					if (e != null)
					{
						detail = e;
					}

					String stackTrace = null;

					ByteArrayOutputStream bos = null;
					PrintStream ps = null;

					try
					{
						bos = new ByteArrayOutputStream();
						ps = new PrintStream(bos);

						((Exception)detail).printStackTrace(ps);

						ps.flush();
						bos.flush();

						stackTrace = bos.toString();
					}
					catch (Exception ex)
					{
						Debug.error(ex);
					}
					finally
					{
						if (ps != null) ps.close();
						if (bos != null)
						{
							try
							{
								bos.close();
							}
							catch (Exception ex)
							{
								Debug.error(ex);
							}
						}
					}

					if (stackTrace == null) stackTrace = detail.toString();

					msg = ((msg == null) ? "<null>\n > " : msg + "\n > ") + stackTrace;

					if (detail instanceof ServoyException && ((ServoyException)detail).getScriptStackTrace() != null)
					{
						msg += '\n' + ((ServoyException)detail).getScriptStackTrace();
					}
				}
				else if (detail != null)
				{
					msg = ((msg == null) ? "<null>\n" : msg + "\n") + detail;
					String scriptstack = Debug.getScriptStacktraceFromContext(msg);
					if (scriptstack != null) msg += "\n" + scriptstack;
				}
				else
				{
					if (msg == null) msg = "<null>";
					else
					{
						String scriptstack = Debug.getScriptStacktraceFromContext(msg);
						if (scriptstack != null) msg += "\n" + scriptstack;
					}
				}
				captureOutput(msg);
				debugger.outputStdErr(msg.toString() + '\n');
			}
		}
	}

	public static void stderrToDebugger(IExecutingEnviroment engine, Object message)
	{
		if (engine instanceof RemoteDebugScriptEngine)
		{
			DBGPDebugger debugger = ((RemoteDebugScriptEngine)engine).getDebugger();
			if (debugger != null)
			{
				captureOutput(message);
				debugger.outputStdErr((message == null ? "<null>" : message.toString().trim()) + System.lineSeparator());
			}
		}
	}

	public static void stdoutToDebugger(IExecutingEnviroment engine, Object message)
	{
		if (engine instanceof RemoteDebugScriptEngine)
		{
			DBGPDebugger debugger = ((RemoteDebugScriptEngine)engine).getDebugger();
			if (debugger != null)
			{
				captureOutput(message);
				debugger.outputStdOut((message == null ? "<null>" : message.toString().trim()) + System.lineSeparator());
			}
		}
	}


	public static void infoToDebugger(IExecutingEnviroment engine, String message)
	{
		if (engine instanceof RemoteDebugScriptEngine)
		{
			DBGPDebugger debugger = ((RemoteDebugScriptEngine)engine).getDebugger();
			if (debugger != null)
			{
				captureOutput(message);
				debugger.outputStdOut(message + '\n');
			}
		}
	}

	private static boolean isReferenceFormUsedInForm(final ClientState clientState, final Form referenceForm, Form form)
	{
		final boolean[] isReferenceFormUsedInForm = { false };
		form.acceptVisitor(new IPersistVisitor()
		{
			@Override
			public Object visit(IPersist o)
			{
				if (o instanceof WebComponent)
				{
					WebComponent wc = (WebComponent)o;
					WebObjectSpecification spec = FormTemplateGenerator.getWebObjectSpecification(wc);
					Collection<PropertyDescription> properties = spec != null ? spec.getProperties(FormComponentPropertyType.INSTANCE) : null;
					if (properties != null && properties.size() > 0)
					{
						for (PropertyDescription pd : properties)
						{
							Form frm = FormComponentPropertyType.INSTANCE.getForm(wc.getProperty(pd.getName()), clientState.getFlattenedSolution());
							if (referenceForm.equals(frm))
							{
								isReferenceFormUsedInForm[0] = true;
								return IPersistVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
							}
							else
							{
								isReferenceFormUsedInForm[0] = isReferenceFormUsedInForm(clientState, referenceForm, frm);
								if (isReferenceFormUsedInForm[0])
								{
									return IPersistVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
								}
							}
						}
					}
				}
				return IPersistVisitor.CONTINUE_TRAVERSAL;
			}
		});

		return isReferenceFormUsedInForm[0];
	}

	public static Set<IFormController>[] getScopesAndFormsToReload(final ClientState clientState, Collection<IPersist> changes)
	{
		Set<IFormController> scopesToReload = new HashSet<IFormController>();
		final Set<IFormController> formsToReload = new HashSet<IFormController>();

		final SpecProviderState specProviderState = WebComponentSpecProvider.getSpecProviderState();

		final Set<Form> formsUpdated = new HashSet<Form>();
		for (IPersist persist : changes)
		{

			clientState.getFlattenedSolution().updatePersistInSolutionCopy(persist);
			if (persist instanceof ScriptMethod scriptMethod)
			{
				if (persist.getParent() instanceof Form)
				{
					Form form = (Form)persist.getParent();
					List<IFormController> cachedFormControllers = clientState.getFormManager().getCachedFormControllers(form);

					for (IFormController formController : cachedFormControllers)
					{
						scopesToReload.add(formController);
					}
				}
				else if (persist.getParent() instanceof Solution)
				{
					LazyCompilationScope scope = clientState.getScriptEngine().getScopesScope().getGlobalScope(scriptMethod.getScopeName());
					Object oldValue = scope.remove(scriptMethod);
					scope.put((IScriptProvider)persist, scriptMethod);
					if (oldValue instanceof Function oldValueFunction)
					{
						clientState.getEventsManager().updateCallbacks(oldValueFunction, scope.getFunctionByName(scriptMethod.getName()));
					}
				}
				else if (persist.getParent() instanceof TableNode)
				{
					clientState.getFoundSetManager().reloadFoundsetMethod(((TableNode)persist.getParent()).getDataSource(), (IScriptProvider)persist);
				}

				if (clientState instanceof DebugJ2DBClient)
				{
//					((DebugJ2DBClient)clientState).clearUserWindows();  no need for this as window API was refactored and it allows users to clean up dialogs
					((DebugSwingFormMananger)((DebugJ2DBClient)clientState).getFormManager()).fillScriptMenu();
				}
			}
			else if (persist instanceof ScriptVariable)
			{
				ScriptVariable sv = (ScriptVariable)persist;
				if (persist.getParent() instanceof Solution)
				{
					clientState.getScriptEngine().getScopesScope().getGlobalScope(sv.getScopeName()).put(sv);
				}
				if (persist.getParent() instanceof Form)
				{
					Form form = (Form)persist.getParent();
					List<IFormController> cachedFormControllers = clientState.getFormManager().getCachedFormControllers(form);

					for (IFormController formController : cachedFormControllers)
					{
						FormScope scope = formController.getFormScope();
						scope.put(sv);
					}
				}
			}
			else if (persist.getAncestor(IRepository.FORMS) != null)
			{
				final Form form = (Form)persist.getAncestor(IRepository.FORMS);
				if (form != null && form.isFormComponent().booleanValue())
				{
					// if the changed form is a reference form we need to check if that is referenced by a loaded form..
					List<IFormController> cachedFormControllers = clientState.getFormManager().getCachedFormControllers();
					for (IFormController fc : cachedFormControllers)
					{
						fc.getForm().acceptVisitor(new IPersistVisitor()
						{
							@Override
							public Object visit(IPersist o)
							{
								if (o instanceof WebComponent)
								{
									WebComponent wc = (WebComponent)o;
									WebObjectSpecification spec = FormTemplateGenerator.getWebObjectSpecification(wc);
									Collection<PropertyDescription> properties = spec != null ? spec.getProperties(FormComponentPropertyType.INSTANCE) : null;
									if (properties != null && properties.size() > 0)
									{
										Form persistForm = (Form)wc.getAncestor(IRepository.FORMS);
										for (PropertyDescription pd : properties)
										{
											Form frm = FormComponentPropertyType.INSTANCE.getForm(wc.getProperty(pd.getName()),
												clientState.getFlattenedSolution());
											if (frm != null && (form.equals(frm) || FlattenedForm.hasFormInHierarchy(frm, form) ||
												isReferenceFormUsedInForm(clientState, form, frm)) && !formsUpdated.contains(persistForm))
											{
												formsUpdated.add(persistForm);
												List<IFormController> cfc = clientState.getFormManager().getCachedFormControllers(persistForm);

												for (IFormController formController : cfc)
												{
													formsToReload.add(formController);
												}
											}
										}
									}
								}
								return IPersistVisitor.CONTINUE_TRAVERSAL;
							}
						});
					}
				}
				else if (!formsUpdated.contains(form))
				{
					formsUpdated.add(form);
					List<IFormController> cachedFormControllers = clientState.getFormManager().getCachedFormControllers(form);

					for (IFormController formController : cachedFormControllers)
					{
						formsToReload.add(formController);
					}
				}
				if (persist instanceof Form && clientState.getFormManager() instanceof DebugUtils.DebugUpdateFormSupport)
				{
					((DebugUtils.DebugUpdateFormSupport)clientState.getFormManager()).updateForm((Form)persist);
				}
			}
			else if (persist instanceof ScriptCalculation)
			{
				ScriptCalculation sc = (ScriptCalculation)persist;
				if (((RemoteDebugScriptEngine)clientState.getScriptEngine()).recompileScriptCalculation(sc))
				{
					List<String> al = new ArrayList<String>();
					al.add(sc.getDataProviderID());
					try
					{
						String dataSource = clientState.getFoundSetManager().getDataSource(sc.getTable());
						((FoundSetManager)clientState.getFoundSetManager()).getRowManager(dataSource).clearCalcs(null, al);
						((FoundSetManager)clientState.getFoundSetManager()).flushSQLSheet(dataSource);
					}
					catch (Exception e)
					{
						Debug.error(e);
					}
				}
//				if (clientState instanceof DebugJ2DBClient)
//				{
//					((DebugJ2DBClient)clientState).clearUserWindows(); no need for this as window API was refactored and it allows users to clean up dialogs
//				}
			}
			else if (persist instanceof Relation)
			{
				((FoundSetManager)clientState.getFoundSetManager()).flushSQLSheet((Relation)persist);

				List<IFormController> cachedFormControllers = clientState.getFormManager().getCachedFormControllers();

				try
				{
					String primary = ((Relation)persist).getPrimaryDataSource();
					for (IFormController formController : cachedFormControllers)
					{
						if (primary.equals(formController.getDataSource()))
						{
							final IFormController finalController = formController;
							final Relation finalRelation = (Relation)persist;
							formController.getForm().acceptVisitor(new IPersistVisitor()
							{
								@Override
								public Object visit(IPersist o)
								{
									if (o instanceof Tab && Utils.equalObjects(finalRelation.getName(), ((Tab)o).getRelationName()))
									{
										formsToReload.add(finalController);
										return o;
									}
									if (o instanceof Field && ((Field)o).getValuelistID() != null)
									{
										ValueList vl = clientState.getFlattenedSolution().getValueList(((Field)o).getValuelistID());
										if (vl != null && Utils.equalObjects(finalRelation.getName(), vl.getRelationName()))
										{
											formsToReload.add(finalController);
											return o;
										}
									}
									if (o instanceof WebComponent)
									{
										WebComponent webComponent = (WebComponent)o;
										WebObjectSpecification spec = specProviderState == null ? null
											: specProviderState.getWebObjectSpecification(webComponent.getTypeName());
										if (spec != null)
										{
											Collection<PropertyDescription> properties = spec.getProperties(RelationPropertyType.INSTANCE);
											for (PropertyDescription pd : properties)
											{
												if (Utils.equalObjects(webComponent.getFlattenedJson().opt(pd.getName()), finalRelation.getName()))
												{
													formsToReload.add(finalController);
													return o;
												}
											}
										}
									}
									return CONTINUE_TRAVERSAL;
								}
							});
						}
					}
				}
				catch (Exception e)
				{
					Debug.error(e);
				}
			}
			else if (persist instanceof ValueList)
			{
				ComponentFactory.flushValueList(clientState, (ValueList)persist);
				List<IFormController> cachedFormControllers = clientState.getFormManager().getCachedFormControllers();
				for (IFormController formController : cachedFormControllers)
				{
					final IFormController finalController = formController;
					final ValueList finalValuelist = (ValueList)persist;
					formController.getForm().acceptVisitor(new IPersistVisitor()
					{
						@Override
						public Object visit(IPersist o)
						{
							if (o instanceof Field && ((Field)o).getValuelistID() != null &&
								finalValuelist.getUUID().toString().equals(((Field)o).getValuelistID()))
							{
								formsToReload.add(finalController);
								return o;
							}
							if (o instanceof WebComponent)
							{
								WebComponent webComponent = (WebComponent)o;
								WebObjectSpecification spec = specProviderState == null ? null
									: specProviderState.getWebObjectSpecification(webComponent.getTypeName());
								if (spec != null)
								{
									Collection<PropertyDescription> properties = spec.getProperties(ValueListPropertyType.INSTANCE);
									for (PropertyDescription pd : properties)
									{
										if (Utils.equalObjects(webComponent.getFlattenedJson().opt(pd.getName()), finalValuelist.getUUID().toString()))
										{
											formsToReload.add(finalController);
											return o;
										}
									}
								}
							}
							return CONTINUE_TRAVERSAL;
						}
					});
				}
			}
			else if (persist instanceof Menu || persist instanceof MenuItem)
			{
				clientState.getMenuManager().flushMenus();
				final Menu menu = persist instanceof Menu ? (Menu)persist : (Menu)persist.getAncestor(IRepository.MENUS);
				List<IFormController> cachedFormControllers = clientState.getFormManager().getCachedFormControllers();
				for (IFormController formController : cachedFormControllers)
				{
					final IFormController finalController = formController;
					formController.getForm().acceptVisitor(new IPersistVisitor()
					{
						@Override
						public Object visit(IPersist o)
						{
							if (o instanceof WebComponent)
							{
								WebComponent webComponent = (WebComponent)o;
								WebObjectSpecification spec = specProviderState == null ? null
									: specProviderState.getWebObjectSpecification(webComponent.getTypeName());
								if (spec != null)
								{
									Collection<PropertyDescription> properties = spec.getProperties(MenuPropertyType.INSTANCE);
									for (PropertyDescription pd : properties)
									{
										if (Utils.equalObjects(webComponent.getFlattenedJson().opt(pd.getName()), menu.getUUID()))
										{
											formsToReload.add(finalController);
											return o;
										}
									}
								}
							}
							return CONTINUE_TRAVERSAL;
						}
					});
				}
			}
			else if (persist instanceof Style)
			{
				ComponentFactory.flushStyle(null, ((Style)persist));
				List<IFormController> cachedFormControllers = clientState.getFormManager().getCachedFormControllers();

				String styleName = ((Style)persist).getName();
				for (IFormController formController : cachedFormControllers)
				{
					if (styleName.equals(formController.getForm().getStyleName()))
					{
						formsToReload.add(formController);
					}
				}
			}
		}

		return new Set[] { scopesToReload, formsToReload };
	}

	/**
	 * This method must be invoked from the swt thread to deal with mac os 10.8 deadlock problems when the awt thread freezes with the stack :
	 * <p>
	 * 	<i>at apple.awt.CInputMethod.getNativeLocale(Native Method)
	 *	at apple.awt.CToolkit.getDefaultKeyboardLocale(CToolkit.java:1044)</i>
	 * <p>
	 * //https://bugs.eclipse.org/bugs/show_bug.cgi?id=372951#c7
	 * apply workaround from https://bugs.eclipse.org/bugs/show_bug.cgi?id=291326   plus read and dispatch
	 * @param run : run must be <b>final</b>
	 * @throws InvocationTargetException
	 */
	public static void invokeAndWaitWhileDispatchingOnSWT(final Runnable run) throws InterruptedException, InvocationTargetException
	{
		// apply workaround from https://bugs.eclipse.org/bugs/show_bug.cgi?id=291326   plus read and dispatch
		if (EventQueue.isDispatchThread())
		{// called from AWT dispatch thread
			run.run();
		}
		else if (org.eclipse.swt.widgets.Display.getCurrent() == null)
		{// called from non SWT thread
			SwingUtilities.invokeAndWait(run);
		}
		else
		{
			final AtomicBoolean awtFinished = new AtomicBoolean(false);
			final org.eclipse.swt.widgets.Display display = org.eclipse.swt.widgets.Display.getCurrent();
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					// do some AWT stuff here
					try
					{
						run.run();
					}
					finally
					{
						awtFinished.set(true);
						display.asyncExec(new Runnable()
						{
							public void run()
							{
								// deliberately empty, this is only to wake up a
								// potentially waiting SWT-thread below
							}
						});
					}
				}
			});
			while (!awtFinished.get())
			{
				if (!display.readAndDispatch()) display.sleep();
			}
		}
	}
}