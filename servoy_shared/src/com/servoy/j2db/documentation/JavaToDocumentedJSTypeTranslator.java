/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.j2db.documentation;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.print.PrinterJob;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;

import com.servoy.base.scripting.api.IJSController;
import com.servoy.base.scripting.api.IJSDataSet;
import com.servoy.base.scripting.api.IJSFoundSet;
import com.servoy.base.scripting.api.IJSRecord;
import com.servoy.base.solutionmodel.IBaseSMButton;
import com.servoy.base.solutionmodel.IBaseSMComponent;
import com.servoy.base.solutionmodel.IBaseSMField;
import com.servoy.base.solutionmodel.IBaseSMForm;
import com.servoy.base.solutionmodel.IBaseSMLabel;
import com.servoy.base.solutionmodel.IBaseSMMethod;
import com.servoy.base.solutionmodel.IBaseSMPart;
import com.servoy.base.solutionmodel.IBaseSMValueList;
import com.servoy.base.solutionmodel.IBaseSMVariable;
import com.servoy.base.solutionmodel.mobile.IMobileSMForm;
import com.servoy.base.solutionmodel.mobile.IMobileSMLabel;
import com.servoy.j2db.IForm;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.dataprocessing.IFoundSet;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecord;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.JSDataSet;
import com.servoy.j2db.dataprocessing.Record;
import com.servoy.j2db.dataprocessing.RelatedFoundSet;
import com.servoy.j2db.querybuilder.IQueryBuilderCondition;
import com.servoy.j2db.querybuilder.IQueryBuilderLogicalCondition;
import com.servoy.j2db.querybuilder.IQueryBuilderWhereCondition;
import com.servoy.j2db.querybuilder.impl.QBCondition;
import com.servoy.j2db.querybuilder.impl.QBLogicalCondition;
import com.servoy.j2db.querybuilder.impl.QBWhereCondition;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.scripting.IScriptable;
import com.servoy.j2db.scripting.JSMap;
import com.servoy.j2db.scripting.solutionmodel.JSButton;
import com.servoy.j2db.scripting.solutionmodel.JSComponent;
import com.servoy.j2db.scripting.solutionmodel.JSField;
import com.servoy.j2db.scripting.solutionmodel.JSFieldWithConstants;
import com.servoy.j2db.scripting.solutionmodel.JSLabel;
import com.servoy.j2db.scripting.solutionmodel.JSMedia;
import com.servoy.j2db.scripting.solutionmodel.JSMethod;
import com.servoy.j2db.scripting.solutionmodel.JSPart;
import com.servoy.j2db.scripting.solutionmodel.JSVariable;
import com.servoy.j2db.solutionmodel.ISMButton;
import com.servoy.j2db.solutionmodel.ISMComponent;
import com.servoy.j2db.solutionmodel.ISMField;
import com.servoy.j2db.solutionmodel.ISMForm;
import com.servoy.j2db.solutionmodel.ISMLabel;
import com.servoy.j2db.solutionmodel.ISMMedia;
import com.servoy.j2db.solutionmodel.ISMMethod;
import com.servoy.j2db.solutionmodel.ISMPart;
import com.servoy.j2db.solutionmodel.ISMVariable;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.IScriptRenderMethods;
import com.servoy.j2db.ui.IScriptRenderMethodsWithOptionalProps;
import com.servoy.j2db.ui.runtime.IRuntimeComponent;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ServoyException;

/**
 * A translator class capable of translating Java classes (used in javascript as return/parameter types) either to another java class that is
 * ServoyDocumented or scriptable (in case the given class isn't already), or directly to a javascript type name (scripting name for ServoyDocumented annotation).
 * @author acostescu
 */
public class JavaToDocumentedJSTypeTranslator
{

	private final Map<Class< ? >, Class< ? >> javaClassToDocumentedJavaClass = new HashMap<Class< ? >, Class< ? >>();
	private final Map<String, String> javaClassToDocumentedJavaClassWorkarounds = new HashMap<String, String>();

	private final Map<Class< ? >, String> cachedDocumentedJavaClassNames = new ConcurrentHashMap<Class< ? >, String>();
	private final Map<Class< ? >, String> cachedJSTypeNames = new ConcurrentHashMap<Class< ? >, String>();

	JavaToDocumentedJSTypeTranslator()
	{
		initializeClassTranslationMap();
	}

	/**
	 * see {@link #translateJavaClassToJSDocumented(Class)}
	 */
	private void initializeClassTranslationMap()
	{
		// -------------------------------- IMPORTANT -------------------------------------
		// if you CHANGE something in this mappings you might need to update TypeMapper's map as well

		javaClassToDocumentedJavaClass.put(Boolean.class, com.servoy.j2db.documentation.scripting.docs.Boolean.class);
		javaClassToDocumentedJavaClass.put(Boolean.TYPE, com.servoy.j2db.documentation.scripting.docs.Boolean.class);

		javaClassToDocumentedJavaClass.put(Double.class, com.servoy.j2db.documentation.scripting.docs.Number.class);
		javaClassToDocumentedJavaClass.put(Double.TYPE, com.servoy.j2db.documentation.scripting.docs.Number.class);
		javaClassToDocumentedJavaClass.put(Float.class, com.servoy.j2db.documentation.scripting.docs.Number.class);
		javaClassToDocumentedJavaClass.put(Float.TYPE, com.servoy.j2db.documentation.scripting.docs.Number.class);
		javaClassToDocumentedJavaClass.put(Long.class, com.servoy.j2db.documentation.scripting.docs.Number.class);
		javaClassToDocumentedJavaClass.put(Long.TYPE, com.servoy.j2db.documentation.scripting.docs.Number.class);
		javaClassToDocumentedJavaClass.put(Integer.class, com.servoy.j2db.documentation.scripting.docs.Number.class);
		javaClassToDocumentedJavaClass.put(Integer.TYPE, com.servoy.j2db.documentation.scripting.docs.Number.class);
		javaClassToDocumentedJavaClass.put(Byte.class, Byte.TYPE);
//		javaClassToDocumentedJavaClass.put(Byte.TYPE, Byte.TYPE);
		javaClassToDocumentedJavaClass.put(Short.class, com.servoy.j2db.documentation.scripting.docs.Number.class);
		javaClassToDocumentedJavaClass.put(Short.TYPE, com.servoy.j2db.documentation.scripting.docs.Number.class);
		javaClassToDocumentedJavaClass.put(Number.class, com.servoy.j2db.documentation.scripting.docs.Number.class);

		javaClassToDocumentedJavaClass.put(java.util.Date.class, com.servoy.j2db.documentation.scripting.docs.Date.class);
		javaClassToDocumentedJavaClass.put(java.sql.Date.class, com.servoy.j2db.documentation.scripting.docs.Date.class);

		javaClassToDocumentedJavaClass.put(java.lang.Character.class, com.servoy.j2db.documentation.scripting.docs.String.class);
		javaClassToDocumentedJavaClass.put(Character.TYPE, com.servoy.j2db.documentation.scripting.docs.String.class);
		javaClassToDocumentedJavaClass.put(String.class, com.servoy.j2db.documentation.scripting.docs.String.class);
		javaClassToDocumentedJavaClass.put(Dimension.class, com.servoy.j2db.documentation.scripting.docs.String.class); // why not Object?
		javaClassToDocumentedJavaClass.put(Insets.class, com.servoy.j2db.documentation.scripting.docs.String.class); // why not Object?
		javaClassToDocumentedJavaClass.put(Point.class, com.servoy.j2db.documentation.scripting.docs.String.class); // why not Object?
		javaClassToDocumentedJavaClass.put(Color.class, com.servoy.j2db.documentation.scripting.docs.String.class); // why not Object?

		javaClassToDocumentedJavaClass.put(NativeArray.class, com.servoy.j2db.documentation.scripting.docs.Array.class);
		javaClassToDocumentedJavaClass.put(NativeJavaArray.class, com.servoy.j2db.documentation.scripting.docs.Array.class);
		javaClassToDocumentedJavaClass.put(JSONArray.class, com.servoy.j2db.documentation.scripting.docs.Array.class);

		javaClassToDocumentedJavaClass.put(Object.class, com.servoy.j2db.documentation.scripting.docs.Object.class);
		javaClassToDocumentedJavaClass.put(NativeObject.class, com.servoy.j2db.documentation.scripting.docs.Object.class);
		javaClassToDocumentedJavaClass.put(Scriptable.class, com.servoy.j2db.documentation.scripting.docs.Object.class);
		javaClassToDocumentedJavaClass.put(JSMap.class, com.servoy.j2db.documentation.scripting.docs.Object.class);

		javaClassToDocumentedJavaClass.put(Map.class, com.servoy.j2db.documentation.scripting.docs.Object.class);

		javaClassToDocumentedJavaClass.put(org.mozilla.javascript.Function.class, com.servoy.j2db.documentation.scripting.docs.Function.class);

		javaClassToDocumentedJavaClass.put(Exception.class, ServoyException.class);

		javaClassToDocumentedJavaClass.put(IFoundSetInternal.class, FoundSet.class);
		javaClassToDocumentedJavaClass.put(IJSFoundSet.class, FoundSet.class);
		javaClassToDocumentedJavaClass.put(IFoundSet.class, FoundSet.class);
		javaClassToDocumentedJavaClass.put(RelatedFoundSet.class, FoundSet.class);

		javaClassToDocumentedJavaClass.put(IDataSet.class, JSDataSet.class);
		javaClassToDocumentedJavaClass.put(IJSDataSet.class, JSDataSet.class);

		javaClassToDocumentedJavaClass.put(IRecordInternal.class, Record.class);
		javaClassToDocumentedJavaClass.put(IJSRecord.class, Record.class);
		javaClassToDocumentedJavaClass.put(IRecord.class, Record.class);

		javaClassToDocumentedJavaClass.put(IComponent.class, IRuntimeComponent.class);

		javaClassToDocumentedJavaClass.put(ISMMedia.class, JSMedia.class);

		javaClassToDocumentedJavaClass.put(ISMField.class, JSFieldWithConstants.class);
		javaClassToDocumentedJavaClass.put(IBaseSMField.class, JSFieldWithConstants.class);
		javaClassToDocumentedJavaClass.put(JSField.class, JSFieldWithConstants.class);

//		javaClassToDocumentedJavaClass.put(ISMComponent.class, JSComponent.class);
		javaClassToDocumentedJavaClass.put(IBaseSMComponent.class, JSComponent.class);
		javaClassToDocumentedJavaClass.put(ISMComponent.class, JSComponent.class);

		javaClassToDocumentedJavaClass.put(ISMMethod.class, JSMethod.class);
		javaClassToDocumentedJavaClass.put(ISMVariable.class, JSVariable.class);
		javaClassToDocumentedJavaClass.put(IBaseSMMethod.class, JSMethod.class);
		javaClassToDocumentedJavaClass.put(IBaseSMVariable.class, JSVariable.class);

		javaClassToDocumentedJavaClass.put(ISMButton.class, JSButton.class);
		javaClassToDocumentedJavaClass.put(ISMLabel.class, JSLabel.class);
		javaClassToDocumentedJavaClass.put(IBaseSMButton.class, JSButton.class);
		javaClassToDocumentedJavaClass.put(IBaseSMLabel.class, JSLabel.class);
		javaClassToDocumentedJavaClass.put(IMobileSMLabel.class, JSLabel.class);

		javaClassToDocumentedJavaClass.put(ISMPart.class, com.servoy.j2db.scripting.solutionmodel.JSPartWithConstants.class);
		javaClassToDocumentedJavaClass.put(IBaseSMPart.class, com.servoy.j2db.scripting.solutionmodel.JSPartWithConstants.class);
		javaClassToDocumentedJavaClass.put(JSPart.class, com.servoy.j2db.scripting.solutionmodel.JSPartWithConstants.class);

		javaClassToDocumentedJavaClass.put(ISMForm.class, com.servoy.j2db.scripting.solutionmodel.JSForm.class);
		javaClassToDocumentedJavaClass.put(IBaseSMForm.class, com.servoy.j2db.scripting.solutionmodel.JSForm.class);
		javaClassToDocumentedJavaClass.put(IMobileSMForm.class, com.servoy.j2db.scripting.solutionmodel.JSForm.class);

		javaClassToDocumentedJavaClass.put(IBaseSMValueList.class, com.servoy.j2db.scripting.solutionmodel.JSValueList.class);

		javaClassToDocumentedJavaClass.put(IForm.class, com.servoy.j2db.documentation.scripting.docs.Form.class);
		javaClassToDocumentedJavaClass.put(FormScope.class, com.servoy.j2db.documentation.scripting.docs.Form.class);
		javaClassToDocumentedJavaClass.put(IJSController.class, com.servoy.j2db.documentation.scripting.docs.Form.class);

		javaClassToDocumentedJavaClass.put(IQueryBuilderCondition.class, QBCondition.class);
		javaClassToDocumentedJavaClass.put(IQueryBuilderLogicalCondition.class, QBLogicalCondition.class);
		javaClassToDocumentedJavaClass.put(IQueryBuilderWhereCondition.class, QBWhereCondition.class);

		javaClassToDocumentedJavaClass.put(IScriptRenderMethods.class, IScriptRenderMethodsWithOptionalProps.class);

		// this might look strange, it's here to avoid PrinterJob types to be changed to Object, because they are not Scriptable or ServoyDocumented
		// but we still want them to appear in JS with their original name
		javaClassToDocumentedJavaClass.put(PrinterJob.class, com.servoy.j2db.documentation.scripting.docs.PrinterJob.class);

		// (plugin) workarounds - ideally this map should tend to become empty in the future instead of growing
		javaClassToDocumentedJavaClassWorkarounds.put("com.servoy.extensions.plugins.window.menu.AbstractMenuItem", //$NON-NLS-1$
			"com.servoy.extensions.plugins.window.menu.MenuItem"); //$NON-NLS-1$
	}

	/**
	 * Translate some java class that is used in JS code (because it is returned somewhere) into a JS (@ServoyDocumented) documented class.
	 * The method is able to handle arrays.
	 * @param javaClass the initial java class.
	 * @return the class name (Class.getName()) of the translation class. (that should make sense in java-script) If the given class is null it returns null.
	 */
	public String translateJavaClassToJSDocumentedJavaClassName(Class< ? > javaClass)
	{
		if (javaClass == null) return null;

		String cached = cachedDocumentedJavaClassNames.get(javaClass);
		if (cached != null) return cached;

		String translatedClassName;
		Pair<Class< ? >, Integer> classAndArray = splitArrayIfNeeded(javaClass);
		if (classAndArray.getLeft().equals(Byte.TYPE))
		{
			translatedClassName = Byte.TYPE.getName();
		}
		else
		{
			Pair<Class< ? >, String> translatedClassAndName = translateJavaClassToJSDocumented(classAndArray.getLeft());
			translatedClassName = translatedClassAndName.getRight();
		}

		cached = mergeBackArrayTypeIfNeeded(translatedClassName, classAndArray.getRight().intValue());
		cachedDocumentedJavaClassNames.put(javaClass, cached);
		return cached;
	}

	/**
	 * Translate a java class that can be used in JS code into a JS Type name. It first tries to translate the class into a java-script documented
	 * class (@ServoyDocumented) if needed, then it tries to get the scripting name defined in the annotation. If this fails, it will just return the
	 * simple class name of the original or translated class. <BR>
	 * The method is able to handle arrays.
	 * @param javaClass the class to be translated.
	 * @return a java-script type that makes sense for the given java class. If the given class is null it returns null.
	 */
	public String translateJavaClassToJSTypeName(Class< ? > javaClass)
	{
		if (javaClass == null) return null;

		String cached = cachedJSTypeNames.get(javaClass);
		if (cached != null) return cached;

		Pair<Class< ? >, Integer> classAndArray = splitArrayIfNeeded(javaClass);
		String jsType;
		if (classAndArray.getLeft().equals(Byte.TYPE))
		{
			jsType = Byte.TYPE.getName();
		}
		else
		{
			Pair<Class< ? >, String> translatedClassAndName = translateJavaClassToJSDocumented(classAndArray.getLeft());
			if (translatedClassAndName.getLeft() == null)
			{
				// we got to this translation because of a String workaround; just parse the class name and get the class' simple name
				int dotIdx = translatedClassAndName.getRight().lastIndexOf('.');
				if (dotIdx >= 0) jsType = translatedClassAndName.getRight().substring(dotIdx + 1);
				else jsType = translatedClassAndName.getRight();
			}
			else
			{
				// this is the usual case;
				// we have a class; use it's scripting name when it's a @ServoyDocumented class or it's simple class name otherwise
				if (translatedClassAndName.getLeft().isAnnotationPresent(ServoyDocumented.class))
				{
					ServoyDocumented annotation = translatedClassAndName.getLeft().getAnnotation(ServoyDocumented.class);
					jsType = annotation.scriptingName();
					if (jsType == null || jsType.trim().length() == 0) jsType = annotation.publicName();
					if (jsType == null || jsType.trim().length() == 0) jsType = translatedClassAndName.getLeft().getSimpleName();
					if (jsType != null) jsType = jsType.trim();
				}
				else if (translatedClassAndName.getRight() != null && translatedClassAndName.getRight().startsWith("Packages."))
				{
					jsType = translatedClassAndName.getRight();
				}
				else
				{
					jsType = translatedClassAndName.getLeft().getSimpleName();
				}
			}
		}

		cached = mergeBackArrayTypeIfNeeded(jsType, classAndArray.getRight().intValue());
		cachedJSTypeNames.put(javaClass, cached);
		return cached;
	}

	/**
	 * Tries to translate a non-array java type into a JS documented java type.
	 * The method prints warnings in the log for unexpected classes that are exposed to JS.
	 * @param javaClass the class to translate.
	 * @return a Class if possible, if not possible then null class (most of the time is not-null) and an always non-null class name (Class.getName()).
	 */
	private Pair<Class< ? >, String> translateJavaClassToJSDocumented(Class< ? > javaClass)
	{
		Class< ? > cls = javaClass;
		String translatedClassName;

		Class< ? > tmpClass = javaClassToDocumentedJavaClass.get(cls);
		if (tmpClass == null)
		{
			String tmpClassName = javaClassToDocumentedJavaClassWorkarounds.get(cls.getName());
			if (tmpClassName == null)
			{
				if (cls.equals(Void.TYPE)) translatedClassName = Void.TYPE.toString();
				else
				{
					if (cls.isAnnotationPresent(ServoyDocumented.class)) translatedClassName = cls.getName();
					else if (IScriptable.class.isAssignableFrom(cls))
					{
						// makes some sense to be used in JS, but is not documented
						Debug.trace("Undocumented scriptable type exposed to JS: " + cls.getName() + "."); //$NON-NLS-1$ //$NON-NLS-2$
						translatedClassName = cls.getName();
					}
					else
					{
						Debug.trace("Undocumented/non-scriptable type exposed to JS: " + cls.getName() + ". Changed into the rhino 'Packages.' notation"); //$NON-NLS-1$ //$NON-NLS-2$
//						cls = com.servoy.j2db.documentation.scripting.docs.Object.class;
//						translatedClassName = com.servoy.j2db.documentation.scripting.docs.Object.class.getName();
						translatedClassName = "Packages." + cls.getName();
					}
				}
			}
			else
			{
				try
				{
					cls = null; // we only have a workaround string representation of the translation; try to get the class from that
					cls = getClass().getClassLoader().loadClass(tmpClassName);
				}
				catch (ClassNotFoundException e)
				{
					// oh well, we tried, it's probably a plugin class
				}
				translatedClassName = tmpClassName;
			}
		}
		else
		{
			cls = tmpClass;
			translatedClassName = tmpClass.getName();
		}

		return new Pair<Class< ? >, String>(cls, translatedClassName);
	}

	/**
	 * If the class is an array, split it up into array dimension and element type (class).
	 * @return a pair containing the element type and the array's dimension. The array's dimension is 0 and element type is the given type if it's not an array.
	 */
	private Pair<Class< ? >, Integer> splitArrayIfNeeded(Class< ? > javaClass)
	{
		int arrDim = 0;
		Class< ? > c = javaClass;
		while (c.isArray())
		{
			arrDim++;
			c = c.getComponentType();
		}
		return new Pair<Class< ? >, Integer>(c, Integer.valueOf(arrDim));
	}

	/**
	 * If the class was an array that was split, merge it's "name" back based on element type and dimensions.
	 * @return the string representation of
	 */
	private String mergeBackArrayTypeIfNeeded(String javaClassName, int dimensions)
	{
		String rez = javaClassName;
		for (int i = 0; i < dimensions; i++)
			rez += "[]"; //$NON-NLS-1$
		return rez;
	}

}
