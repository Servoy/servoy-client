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
package com.servoy.j2db.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.servoy.j2db.util.Utils;

/**
 * Method templates, used for defining arguments in form designer (properties) and for creating new methods.
 *
 * @author rgansevles
 *
 */
@SuppressWarnings("nls")
public class MethodTemplate implements IMethodTemplate
{
	public static final int PUBLIC_TAG = 1;
	public static final int PROTECTED_TAG = 2;
	public static final int PRIVATE_TAG = 4;
	public static final int ALL_TAGS = PUBLIC_TAG | PROTECTED_TAG | PRIVATE_TAG;

	private static final String TAG_METHODTEMPLATE = "methodtemplate";
	private static final String ATTR_NAME = "name";
	private static final String ATTR_TYPE = "type";
	private static final String ATTR_ADDTODO = "addtodo";
	private static final String ATTR_PRIVATE = "private";

	private static final String TAG_DESCRIPTION = "description";
	private static final String TAG_ARGUMENTS = "arguments";
	private static final String TAG_RETURN_TYPE = "return";
	private static final String TAG_CODE = "code";

	private static final MethodTemplate DEFAULT_TEMPLATE = new MethodTemplate(null, null, null, null, true);

	private static final Map<Class< ? >, Map<String, MethodTemplate>> CLASS_TEMPLATES = new HashMap<Class< ? >, Map<String, MethodTemplate>>();
	public static final Map<String, MethodTemplate> COMMON_TEMPLATES = new TreeMap<String, MethodTemplate>();
	static
	{
		// Calculation templates
		Map<String, MethodTemplate> calculationTemplates = new HashMap<String, MethodTemplate>();
		CLASS_TEMPLATES.put(ScriptCalculation.class, calculationTemplates);
		calculationTemplates.put("rowBGColorCalculation", //$NON-NLS-1$
			new MethodTemplate("Calculate the row background color", new MethodArgument("rowBGColorCalc", ArgumentType.Color, "row background color"), //
				new MethodArgument[] { new MethodArgument("index", ArgumentType.Number, "row index"), //
					new MethodArgument("selected", ArgumentType.Boolean, "is the row selected"), //
					new MethodArgument("elementType", ArgumentType.String, "element type"), //
					new MethodArgument("dataProviderID", ArgumentType.String, "element data provider"), //
					new MethodArgument("edited", ArgumentType.Boolean, "is the record edited") },
				"\tif (selected)\n\t\treturn '#c4ffff';\n\telse if (index % 2)\n\t\treturn '#f4ffff';\n\telse\n\t\treturn '#FFFFFF';", true));

		// Common method templates

		// valuelist global method

		COMMON_TEMPLATES.put("valueListGlobalMethod", new MethodTemplate(
			"Called when the valuelist needs data, it has 3 modes\nreal and display params both null: return the whole list\nonly display is specified, called by a typeahead, return a filtered list\nonly real value is specified, called when the list doesnt contain the real value for the give record value, this will insert this value into the existing list\n",
			new MethodArgument("getDataSetForValueList", ArgumentType.JSDataSet, //$NON-NLS-1$
				"A dataset with 1 or 2 columns display[,real]"),
			new MethodArgument[] { new MethodArgument("displayValue", ArgumentType.String, "The value of a lookupfield that a user types"), new MethodArgument(
				"realValue", ArgumentType.Object, "The real value for a lookupfield where a display value should be get for"), new MethodArgument("record",
					ArgumentType.JSRecord, "The current record for the valuelist."), new MethodArgument("valueListName", ArgumentType.String,
						"The valuelist name that triggers the method. (This is the FindRecord in find mode, which is like JSRecord has all the columns/dataproviders, but doesn't have its methods)"), new MethodArgument(
							"findMode", ArgumentType.Boolean, "True if foundset of this record is in find mode"), new MethodArgument("rawDisplayValue",
								ArgumentType.Boolean, "The raw displayValue without being converted to lower case") },
			"var args = null;\n" + "var query = datasources.db.example_data.employees.createSelect();\n" + "/** @type  {JSDataSet} */\n" +
				"var result = null;\n" + "if (displayValue == null && realValue == null)\n" +
				"{\n// TODO think about caching this result. can be called often!\n" + "// return the complete list\n" +
				"query.result.add(query.columns.firstname.concat(' ').concat(query.columns.lastname)).add(query.columns.employeeid);\n" +
				"result = databaseManager.getDataSetByQuery(query,100);\n" + "}\n" + "else if (displayValue != null)\n" + "{\n" +
				"// TYPE_AHEAD filter call, return a filtered list\n" + "args = [displayValue + \"%\", displayValue + \"%\"];\n" +
				"query.result.add(query.columns.firstname.concat(' ').concat(query.columns.lastname)).add(query.columns.employeeid).\n" +
				"root.where.add(query.or.add(query.columns.firstname.lower.like(args[0] + '%')).add(query.columns.lastname.lower.like(args[1] + '%')));\n" +
				"result = databaseManager.getDataSetByQuery(query,100);\n" + "}\n" + "else if (realValue != null)\n" + "{\n" +
				"// TODO think about caching this result. can be called often!\n" +
				"// real object not found in the current list, return 1 row with display,realvalue that will be added to the current list\n" +
				"// dont return a complete list in this mode because that will be added to the list that is already there\n" + "args = [realValue];\n" +
				"query.result.add(query.columns.firstname.concat(' ').concat(query.columns.lastname)).add(query.columns.employeeid).\n" +
				"root.where.add(query.columns.employeeid.eq(args[0]));\n" + "result = databaseManager.getDataSetByQuery(query,1);\n" + "}\nreturn result;\n",
			false));
	}

	private final MethodArgument signature;
	private final MethodArgument[] args;
	private final String description;
	private final boolean addTodoBlock;
	private final String defaultMethodCode;
	private boolean privateMethod = false;


	public MethodTemplate(String description, MethodArgument signature, MethodArgument[] args, String defaultMethodCode, boolean addTodoBlock)
	{
		this.signature = signature;
		this.args = args;
		this.description = description;
		this.defaultMethodCode = defaultMethodCode;
		this.addTodoBlock = addTodoBlock;
	}

	public MethodTemplate(IMethodTemplate templ)
	{
		this.signature = new MethodArgument(templ.getName(), templ.getReturnType(), templ.getReturnTypeDescription());
		this.args = new MethodArgument[templ.getArguments().length];
		for (int i = 0; i < templ.getArguments().length; i++)
			args[i] = new MethodArgument(templ.getArguments()[i]);
		this.description = templ.getDescription();
		this.defaultMethodCode = templ.getDefaultMethodCode();
		this.addTodoBlock = templ.addTodoBlock();
	}

	public MethodArgument[] getArguments()
	{
		return args;
	}

	public MethodArgument getSignature()
	{
		return signature;
	}

	public String getName()
	{
		return signature.getName();
	}

	public ArgumentType getReturnType()
	{
		return signature.getType();
	}

	public String getReturnTypeDescription()
	{
		return signature.getDescription();
	}

	public boolean addTodoBlock()
	{
		return addTodoBlock;
	}

	public String getMethodDeclaration(CharSequence name, CharSequence methodCode, String userTemplate)
	{
		return getMethodDeclaration(name, methodCode, PUBLIC_TAG, userTemplate, null);
	}

	/**
	 *
	 * @param name
	 * @param methodCode
	 * @param tagToOutput PUBLIC_TAG/PROTECTED_TAG/PRIVATE_TAG
	 * @return
	 */
	public String getMethodDeclaration(CharSequence name, CharSequence methodCode, int tagToOutput, String userTemplate, Map<String, String> substitutions)
	{
		StringBuilder sb = new StringBuilder();
		if (description != null && description.length() > 0)
		{
			String[] lines = description.split("\n"); //$NON-NLS-1$
			for (int i = 0; i < lines.length; i++)
			{
				sb.append(" * ").append(lines[i]); //$NON-NLS-1$
				if (i == 0 && !lines[i].endsWith(".")) sb.append('.'); //$NON-NLS-1$
				sb.append('\n');
			}
		}
		if (userTemplate != null && userTemplate.length() > 0)
		{
			if (sb.length() > 0) sb.append(" *\n"); //$NON-NLS-1$
			for (String line : userTemplate.split("\n"))
			{
				sb.append(" * ").append(line).append('\n'); //$NON-NLS-1$
			}
		}
		if (args != null && args.length > 0)
		{
			sb.append(" *\n"); //$NON-NLS-1$
			for (MethodArgument element : args)
			{
				sb.append(" * @param "); //$NON-NLS-1$
				if (element.getType() != null && element.getType() != ArgumentType.Object && !"Any".equals(String.valueOf(element.getType())))
				{
					String typeString = String.valueOf(element.getType());
					if (!typeString.startsWith("${") || !typeString.endsWith("}") ||
						(substitutions != null && substitutions.get(typeString.substring(2, typeString.length() - 1)) != null))
					{ // only add curly braces when no variable or variable is really substituted
						sb.append('{').append(typeString).append("} "); //$NON-NLS-1$
					}
				}
				if (element.isOptional()) sb.append("[");
				sb.append(element.getName());
				if (element.isOptional()) sb.append("]");
				if (element.getDescription() != null)
				{
					sb.append(' ').append(element.getDescription());
				}
				sb.append('\n');
			}
		}
		if (signature != null && signature.getType() != null)
		{
			sb.append(" *\n * @return {").append(signature.getType()).append('}'); //$NON-NLS-1$
			if (signature.getDescription() != null)
			{
				sb.append(' ').append(signature.getDescription());
			}
			sb.append('\n');
		}
		if (tagToOutput == PRIVATE_TAG)
		{
			if (sb.length() > 0) sb.append(" *");
			sb.append("\n * @private\n");
		}
		if (tagToOutput == PROTECTED_TAG)
		{
			if (sb.length() > 0) sb.append(" *");
			sb.append("\n * @protected\n");
		}
		if (sb.length() > 0)
		{
			sb.insert(0, "/**\n");
			sb.append(" */\n"); //$NON-NLS-1$
		}

		sb.append("function "); //$NON-NLS-1$
		if (name == null)
		{
			sb.append(signature.getName());
		}
		else
		{
			sb.append(name);
		}
		sb.append('(');
		for (int i = 0; args != null && i < args.length; i++)
		{
			if (i > 0) sb.append(", "); //$NON-NLS-1$
			sb.append(args[i].getName());
		}
		sb.append(")\n{"); //$NON-NLS-1$
		if (methodCode != null)
		{
			if (methodCode.length() > 0)
			{
				sb.append('\n').append(methodCode);
			}
		}
		else
		{
			if (addTodoBlock) sb.append("\n\t// ").append("TODO Auto-generated method stub"); //$NON-NLS-1$
			if (defaultMethodCode != null)
			{
				sb.append("\n\t").append(defaultMethodCode); //$NON-NLS-1$
			}
		}
		sb.append("\n}\n"); //$NON-NLS-1$
		String declaration = sb.toString();

		// replace ${key} with substitutions(key)
		Matcher matcher = Pattern.compile("\\$\\{(\\w+)\\}").matcher(declaration);
		StringBuffer stringBuffer = new StringBuffer(declaration.length());
		while (matcher.find())
		{
			String key = matcher.group(1);
			String value = substitutions == null ? null : substitutions.get(key);
			matcher.appendReplacement(stringBuffer, value == null ? "??" + key + "??" : value);
		}
		matcher.appendTail(stringBuffer);
		declaration = stringBuffer.toString();

		return declaration;
	}

	/**
	 * Get a template for the method key.
	 *
	 * @param context
	 * @param methodKey
	 */
	public static MethodTemplate getTemplate(Class< ? > context, String methodKey)
	{
		MethodTemplate template = null;
		if (methodKey != null)
		{
			Map<String, MethodTemplate> classTemplates = CLASS_TEMPLATES.get(context);
			if (classTemplates != null)
			{
				template = classTemplates.get(methodKey);
			}
			if (template == null)
			{
				template = COMMON_TEMPLATES.get(methodKey);
			}
		}
		return (template == null) ? DEFAULT_TEMPLATE : template;
	}

	/**
	 * Get at template for overriding a method of a super form
	 *
	 * @param context
	 * @param methodKey
	 * @param formalArguments
	 */
	public static MethodTemplate getFormMethodOverrideTemplate(Class< ? > methodClass, String methodKey, MethodArgument[] formalArguments,
		MethodArgument returnType)
	{
		MethodTemplate template = getTemplate(methodClass, methodKey);
		MethodArgument signature = template.signature;
		if (signature == null)
		{
			signature = returnType;
		}
		return new MethodTemplate(template.description, signature, Utils.arrayMerge(template.args, formalArguments), null, true)
		{
			@Override
			public String getMethodDeclaration(CharSequence name, CharSequence methodCode, int tagToOutput, String userTemplate,
				Map<String, String> substitutions)
			{
				CharSequence body;
				if (methodCode == null)
				{
					StringBuilder sb = new StringBuilder();
					sb.append("return _super.").append(name);
					if (getArguments() == null || getArguments().length == 0)
					{
						sb.append(
							".apply(this, arguments); \n/** When the number of arguments that ought to be send into the _super call are known,\n the _super call can also be made like this: _super." +
								name + "(arg1,arg2)*/");
					}
					else
					{
						sb.append('(');
						for (int i = 0; getArguments() != null && i < getArguments().length; i++)
						{
							if (i > 0) sb.append(", ");
							sb.append(getArguments()[i].getName());
						}
						sb.append(')');
					}
					body = sb.toString();
				}
				else
				{
					body = methodCode;
				}
				return super.getMethodDeclaration(name, body, tagToOutput, userTemplate, substitutions);
			}
		};
	}

	public String getDefaultMethodCode()
	{
		return defaultMethodCode;
	}

	public String getDescription()
	{
		return description;
	}

	public boolean hasAddTodoBlock()
	{
		return addTodoBlock;
	}

	public boolean isPrivateMethod()
	{
		return privateMethod;
	}

	public void setPrivateMethod(boolean privateMethod)
	{
		this.privateMethod = privateMethod;
	}

	public Element toXML(Document doc)
	{
		Element root = doc.createElement(TAG_METHODTEMPLATE);
		root.setAttribute(ATTR_NAME, signature.getName());
		if (signature.getType() != null) root.setAttribute(ATTR_TYPE, signature.getType().getName());
		if (addTodoBlock) root.setAttribute(ATTR_ADDTODO, Boolean.TRUE.toString());
		if (privateMethod) root.setAttribute(ATTR_PRIVATE, Boolean.TRUE.toString());

		if (description != null)
		{
			Element descrEl = doc.createElement(TAG_DESCRIPTION);
			descrEl.appendChild(doc.createCDATASection(description));
			root.appendChild(descrEl);
		}
		if (defaultMethodCode != null)
		{
			Element defMetEl = doc.createElement(TAG_CODE);
			defMetEl.appendChild(doc.createCDATASection(defaultMethodCode));
			root.appendChild(defMetEl);
		}
		if (args != null)
		{
			Element argsRoot = doc.createElement(TAG_ARGUMENTS);
			root.appendChild(argsRoot);
			for (MethodArgument marg : args)
			{
				argsRoot.appendChild(marg.toXML(doc));
			}
		}
		return root;
	}

	public static MethodTemplate fromXML(Element root)
	{
		if (!root.getNodeName().equals(TAG_METHODTEMPLATE)) return null;

		String name = root.getAttribute(ATTR_NAME);
		boolean addTodo = Boolean.parseBoolean(root.getAttribute(ATTR_ADDTODO));
		String typeStr = root.getAttribute(ATTR_TYPE);
		ArgumentType type = null;
		if (typeStr != null && typeStr.length() != 0) type = ArgumentType.valueOf(typeStr);

		NodeList nodes = root.getElementsByTagName(TAG_DESCRIPTION);
		String descr = null;
		if (nodes != null && nodes.getLength() > 0)
		{
			Element el = (Element)nodes.item(0);
			descr = el.getTextContent();

		}

		String code = null;
		nodes = root.getElementsByTagName(TAG_CODE);
		if (nodes != null && nodes.getLength() > 0)
		{
			Element el = (Element)nodes.item(0);
			code = el.getTextContent();
		}

		MethodArgument[] arguments = null;
		nodes = root.getElementsByTagName(TAG_ARGUMENTS);
		if (nodes != null && nodes.getLength() > 0)
		{
			Element elArgs = (Element)nodes.item(0);
			NodeList argNodes = elArgs.getElementsByTagName(MethodArgument.TAG_METHODARGUMENT);
			if (argNodes != null && argNodes.getLength() > 0)
			{
				List<MethodArgument> argsList = new ArrayList<MethodArgument>();
				for (int i = 0; i < argNodes.getLength(); i++)
				{
					Element argsElem = (Element)argNodes.item(i);
					MethodArgument arg = MethodArgument.fromXML(argsElem);
					if (arg != null) argsList.add(arg);
				}
				arguments = new MethodArgument[argsList.size()];
				argsList.toArray(arguments);
			}
		}

		MethodTemplate mtempl = new MethodTemplate(descr, new MethodArgument(name, type, null), arguments, code, addTodo);
		String attrPriv = root.getAttribute(ATTR_PRIVATE);
		if (attrPriv != null && Boolean.parseBoolean(attrPriv))
		{
			mtempl.setPrivateMethod(true);
		}
		return mtempl;
	}

	public static void clearTemplates()
	{
		CLASS_TEMPLATES.clear();
		COMMON_TEMPLATES.clear();
	}
}
