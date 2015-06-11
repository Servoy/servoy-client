/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.j2db.server.ngclient.component.spec;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.servoy.base.persistence.constants.IRepositoryConstants;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.ContentSpec;
import com.servoy.j2db.persistence.ContentSpec.Element;
import com.servoy.j2db.persistence.IContentSpecConstants;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.ui.IScriptScriptLabelMethods;
import com.servoy.j2db.ui.runtime.IRuntimeCalendar;
import com.servoy.j2db.ui.runtime.IRuntimeCheck;
import com.servoy.j2db.ui.runtime.IRuntimeChecks;
import com.servoy.j2db.ui.runtime.IRuntimeCombobox;
import com.servoy.j2db.ui.runtime.IRuntimeDataButton;
import com.servoy.j2db.ui.runtime.IRuntimeHtmlArea;
import com.servoy.j2db.ui.runtime.IRuntimeImageMedia;
import com.servoy.j2db.ui.runtime.IRuntimePassword;
import com.servoy.j2db.ui.runtime.IRuntimeRadio;
import com.servoy.j2db.ui.runtime.IRuntimeRadios;
import com.servoy.j2db.ui.runtime.IRuntimeTextArea;
import com.servoy.j2db.ui.runtime.IRuntimeTextField;
import com.servoy.j2db.util.IntHashMap;
import com.servoy.j2db.util.Utils;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;

/**
 * @author obuligan
 *
 */
@SuppressWarnings("nls")
public class SpecGenerator
{
	private static final String SERVOYDOC_LOCATION = "../../servoy-eclipse/com.servoy.eclipse.core/src/com/servoy/eclipse/core/doc/servoydoc.xml";
	private static final String COMPONENTS_LOCATION = ".";
	private static final String SPEC_EXTENSION = "spec"; // TIP : the first time you run this tool change "spec" extension to "spec2" to be able to compare with existing manual spec

	// @formatter:off
	private static final List<SpecTemplateModel> specTemplateList = new ArrayList<SpecTemplateModel>();
	static
	{
		specTemplateList.add(new SpecTemplateModel("button", "Button",  "button.gif", IRepository.GRAPHICALCOMPONENTS, IRuntimeDataButton.class, new String[0]));
		specTemplateList.add(new SpecTemplateModel(
			"calendar",
			"Calendar",
			"Calendar_C16.png",
			IRepository.FIELDS,
			IRuntimeCalendar.class,
			new String[] { "{\"name\":\"moment\", \"version\":\"2.6.0\", \"url\": \"servoydefault/calendar/bootstrap-datetimepicker/js/moment.min.js\", \"mimetype\":\"text/javascript\"}","{\"name\":\"moment-jdateformatparser\", \"version\":\"0.1.1\", \"url\":\"servoydefault/calendar/bootstrap-datetimepicker/js/moment-jdateformatparser.js\", \"mimetype\":\"text/javascript\"}", "{\"name\":\"bootstrap-datetimepicker\", \"version\":\"4.7.14\", \"url\":\"servoydefault/calendar/bootstrap-datetimepicker/js/bootstrap-datetimepicker.js\", \"mimetype\":\"text/javascript\"}", "{\"name\":\"bootstrap-datetimepicker\", \"version\":\"4.7.14\", \"url\":\"servoydefault/calendar/bootstrap-datetimepicker/css/bootstrap-datetimepicker.css\", \"mimetype\":\"text/css\"}","{\"name\":\"bootstrap-datetimepicker-servoy-extension\", \"version\":\"1.0\", \"url\":\"servoydefault/calendar/bootstrap-datetimepicker/css/bootstrap-datetimepicker-servoy-extension.css\", \"mimetype\":\"text/css\"}" }));
		specTemplateList.add(new SpecTemplateModel("checkgroup", "Check Group",  null, IRepository.FIELDS, IRuntimeChecks.class, new String[0]));
		specTemplateList.add(new SpecTemplateModel(
			"combobox",
			"Combobox ",

			"SELECT16.png",
			IRepository.FIELDS,
			IRuntimeCombobox.class,
			new String[] { "{\"name\":\"ui-select\", \"version\":\"0.8.0\", \"url\":\"servoydefault/combobox/lib/select.js\", \"mimetype\":\"text/javascript\"},{\"name\":\"ui-select\", \"version\":\"0.8.0\", \"url\":\"servoydefault/combobox/lib/select.css\", \"mimetype\":\"text/css\"}"
			// minified would be "servoydefault/combobox/lib/select2-3.4.5/select2.min.js"
			}));
		specTemplateList.add(new SpecTemplateModel("label", "Label",  "text.gif", IRepository.GRAPHICALCOMPONENTS, IScriptScriptLabelMethods.class, new String[0]));
		specTemplateList.add(new SpecTemplateModel("radiogroup", "Radio group",  null, IRepository.FIELDS, IRuntimeRadios.class, new String[0]));
		specTemplateList.add(new SpecTemplateModel("textfield", "Text Field",  "textinput.png", IRepository.FIELDS, IRuntimeTextField.class, new String[0]));
		specTemplateList.add(new SpecTemplateModel("typeahead", "Type Ahead ",  "bhdropdownlisticon.gif", IRepository.FIELDS, IRuntimeTextField.class, new String[0]));
		specTemplateList.add(new SpecTemplateModel("tabpanel", "Tab Panel",  "tabs.gif", IRepository.TABPANELS, com.servoy.j2db.ui.IScriptTabPanelMethods.class,
			new String[] { "{\"name\":\"accordionpanel\", \"version\":\"1\", \"url\":\"servoydefault/tabpanel/accordionpanel.css\", \"mimetype\":\"text/css\"}" },"servoydefault/tabpanel/tabpanel_server.js"));
		specTemplateList.add(new SpecTemplateModel("password", "Password Field",  "password_field_16.png", IRepository.FIELDS, IRuntimePassword.class, new String[0]));
		specTemplateList.add(new SpecTemplateModel("htmlarea", "Html Area",  "H1_C16.png", IRepository.FIELDS, IRuntimeHtmlArea.class,
			new String[] { "{\"name\":\"tinymce\", \"version\":\"4.0.20\", \"url\":\"servoydefault/htmlarea/lib/tinymce/tinymce.min.js\", \"mimetype\":\"text/javascript\"}", "{\"name\":\"ui-tinymce\", \"version\":\"1\", \"url\":\"servoydefault/htmlarea/lib/ui-tinymce.js\", \"mimetype\":\"text/javascript\"}" }));
		specTemplateList.add(new SpecTemplateModel("htmlview", "Html View",  null, IRepository.FIELDS, IRuntimeHtmlArea.class, new String[0]));
		specTemplateList.add(new SpecTemplateModel("textarea", "Text Area",  "TEXTAREA16.png", IRepository.FIELDS, IRuntimeTextArea.class, new String[0]));
		specTemplateList.add(new SpecTemplateModel("check", "Check",  "CHECKBOX16.png", IRepository.FIELDS, IRuntimeCheck.class, new String[0]));
		specTemplateList.add(new SpecTemplateModel("radio", "Radio",  "RADIO16.png", IRepository.FIELDS, IRuntimeRadio.class, new String[0]));
		specTemplateList.add(new SpecTemplateModel("imagemedia", "Image Media",  "IMG16.png", IRepository.FIELDS, IRuntimeImageMedia.class, new String[0]));
		specTemplateList.add(new SpecTemplateModel(
			"splitpane",
			"Split Pane",

			"split.gif",
			IRepository.TABPANELS,
			com.servoy.j2db.ui.IScriptSplitPaneMethods.class,
			new String[] { "{\"name\":\"bg-splitter\", \"version\":\"1\", \"url\":\"servoydefault/splitpane/bg-splitter/js/splitter.js\", \"mimetype\":\"text/javascript\"}", "{\"name\":\"bg-splitter\", \"version\":\"1\", \"url\":\"servoydefault/splitpane/bg-splitter/css/style.css\", \"mimetype\":\"text/css\"}" },
			"servoydefault/splitpane/splitpane_server.js",
			// @formatter:off
			new ApiMethod[] {
				getApiMethod("getBrowserDividerLocation", "double", null, null, null),
				getApiMethod("getBrowserDividerSize", "int", null, null, null),
				getApiMethod("getDividerLocation", "double", null, null, null),
				getApiMethod("setDividerLocation", "void", Arrays.asList(new String[] { "location" }), Arrays.asList(new String[] { "double" }), Arrays.asList(new String[] { "false" })),
				getApiMethod("getDividerSize", "int", null, null, null),
				getApiMethod("setDividerSize", "void", Arrays.asList(new String[] { "size" }),	Arrays.asList(new String[] { "int" }), Arrays.asList(new String[] { "false" })),
				getApiMethod("getResizeWeight", "double", null, null, null),
				getApiMethod("setResizeWeight", "void", Arrays.asList(new String[] { "resizeWeight" }), Arrays.asList(new String[] { "double" }), Arrays.asList(new String[] { "false" })),
				getApiMethod("getContinuousLayout", "boolean", null, null, null),
				getApiMethod("setContinuousLayout", "void", Arrays.asList(new String[] { "b" }), Arrays.asList(new String[] { "boolean" }),	Arrays.asList(new String[] { "false" })),
				getApiMethod("getRightFormMinSize", "int", null, null, null),
				getApiMethod("setRightFormMinSize",	"void", Arrays.asList(new String[] { "minSize" }), Arrays.asList(new String[] { "int" }), Arrays.asList(new String[] { "false" })),
				getApiMethod("getLeftFormMinSize", "int", null, null, null),
				getApiMethod("setLeftFormMinSize", "void", Arrays.asList(new String[] { "minSize" }), Arrays.asList(new String[] { "int" }), Arrays.asList(new String[] { "false" }))
			}
			// @formatter:on
		));
		specTemplateList.add(new SpecTemplateModel(
			"portal",
			"Portal",
			"portal.gif",
			IRepository.PORTALS,
			com.servoy.j2db.ui.IScriptPortalComponentMethods.class,
			new String[] { "{\"name\":\"ui-grid\", \"version\":\"v3.0.0-rc.12\", \"url\":\"servoydefault/portal/js/ui-grid.js\", \"mimetype\":\"text/javascript\"},"
				+ "\n\t\t\t\t{\"name\":\"ui-grid\", \"version\":\"v3.0.0-rc.12\", \"url\":\"servoydefault/portal/css/ui-grid.min.css\", \"mimetype\":\"text/css\"},"
				+ "\n\t\t\t\t{\"name\":\"svy-portal\", \"version\":\"1\", \"url\":\"servoydefault/portal/portal.css\", \"mimetype\":\"text/css\"}" },
			"servoydefault/portal/portal_server.js"));
		specTemplateList.add(new SpecTemplateModel(
			"spinner",
			"Spinner",
			"spinner.png",
			IRepository.FIELDS,
			com.servoy.j2db.ui.runtime.IRuntimeSpinner.class,
			new String[] { "{\"name\":\"svy-spinner\", \"version\":\"1\", \"url\":\"servoydefault/spinner/spinner.css\", \"mimetype\":\"text/css\"}", "{\"name\":\"font-awesome\", \"version\":\"4.2.0\", \"url\":\"servoydefault/spinner/css/font-awesome.css\", \"mimetype\":\"text/css\"}" }));
		specTemplateList.add(new SpecTemplateModel("listbox", "ListBox", "listbox.png", IRepository.FIELDS, com.servoy.j2db.ui.runtime.IRuntimeListBox.class,
			new String[0]));
		specTemplateList.add(new SpecTemplateModel("rectangle", "Rectangle", "rectangle.gif", IRepository.RECTSHAPES,
			com.servoy.j2db.ui.runtime.IRuntimeRectangle.class, new String[0]));

		//specTemplateList.add(new SpecTemplateModel("navigator","Navigator", IRepository.FIELDS));
	}

	private static ApiMethod getApiMethod(String name, String returnType, List<String> parametersNames, List<String> parameterTypes,
		List<String> optionalParameters)
	{
		return new ApiMethod(name, returnType, parametersNames, parameterTypes, optionalParameters, null);
	}


	// @formatter:on

	private final Configuration cfg;

	public static void main(String[] args)
	{
		SpecGenerator generator = new SpecGenerator();
		generator.generateSpecsForServoyComponents(specTemplateList);

	}

	public SpecGenerator()
	{
		cfg = new Configuration();
		cfg.setTemplateLoader(new ClassTemplateLoader(getClass(), ""));
		cfg.setDefaultEncoding("UTF-8");
		cfg.setLocalizedLookup(false);
		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
		cfg.setIncompatibleImprovements(new Version(2, 3, 20));
	}

	public void generateSpecsForServoyComponents(List<SpecTemplateModel> specTemplateList)
	{

		readApiMethods(specTemplateList);
		readModelAndHandlers(specTemplateList);

		for (SpecTemplateModel componentSpec : specTemplateList)
		{
			try
			{
				String name = componentSpec.getName();
				File file = new File(COMPONENTS_LOCATION + "/war/servoydefault/" + name + "/" + name + "." + SPEC_EXTENSION);
				if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
				FileWriter fw = new FileWriter(file);
				System.out.println("generating file: " + file);
				componentSpec.sortByName();
				generate(componentSpec, fw);
				fw.close();
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}
			catch (UnsupportedEncodingException e)
			{
				e.printStackTrace();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	void readApiMethods(List<SpecTemplateModel> specTemplateList)
	{
		try
		{
			DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = builderFactory.newDocumentBuilder();
			System.out.println("parsing in " + new File(SERVOYDOC_LOCATION).getAbsolutePath());
			Document document = builder.parse(new FileInputStream(SERVOYDOC_LOCATION));

			XPath xPath = XPathFactory.newInstance().newXPath();
			//expressions in the context of function
			XPathExpression parametersExpr = xPath.compile("parameters/parameter");
			XPathExpression argumentTypesExpr = xPath.compile("argumentsTypes/argumentType");
			XPathExpression returnTypeExpr = xPath.compile("return[1]");

			for (SpecTemplateModel specModel : specTemplateList)
			{
				String runtimeComponentExpression = "/servoydoc/runtime/object[@qualifiedName=\"" + specModel.getApiInterface().getName() +
					"\"]/functions/function";
				//read function list  nodes using xpath
				NodeList functionList = (NodeList)xPath.compile(runtimeComponentExpression).evaluate(document, XPathConstants.NODESET);
				for (int i = 0; i < functionList.getLength(); i++)
				{
					Node function = functionList.item(i);
					if (function.getAttributes().getNamedItem("clientSupport").getTextContent().contains("wc"))
					{

						String functionName = function.getAttributes().getNamedItem("name").getTextContent();
						if (overriddenClientSideApi.containsKey(functionName))
						{
							specModel.getApis().add(overriddenClientSideApi.get(functionName)); // could be extended to check if it already exists, or to handle overloading
						}
						else
						{
							Node returnNode = (Node)returnTypeExpr.evaluate(function, XPathConstants.NODE);
							String returnType = specSpecTypeFromDoc(returnNode.getAttributes().getNamedItem("typecode").getTextContent());
							List<String> parameterNames = new ArrayList<>();
							List<String> parameterTypes = new ArrayList<>();
							List<String> optionalParams = new ArrayList<>();

							// read argumentTypes and parameter nodes
							NodeList argumentTypesNodes = (NodeList)argumentTypesExpr.evaluate(function, XPathConstants.NODESET);
							NodeList parametersNamesNodes = (NodeList)parametersExpr.evaluate(function, XPathConstants.NODESET);
							for (int j = 0; j < argumentTypesNodes.getLength() || j < parametersNamesNodes.getLength(); j++)
							{
								Node argType = j > argumentTypesNodes.getLength() ? null : argumentTypesNodes.item(j);
								Node param = j > parametersNamesNodes.getLength() ? null : parametersNamesNodes.item(j);

								// --- read parameter type
								if (argType != null)
								{
									parameterTypes.add(specSpecTypeFromDoc(argType.getAttributes().getNamedItem("typecode").getTextContent()));
								}
								else if (param != null)
								{
									Node node = param.getAttributes().getNamedItem("typecode");
									if (node != null)
									{
										parameterTypes.add(specSpecTypeFromDoc(node.getTextContent()));
									}
									else
									{
										parameterTypes.add(specSpecTypeFromDoc("object"));
									}
								}

								// read parameter name , and add optional if present
								if (param == null)
								{
									parameterNames.add("unnamed_" + j);
								}
								else
								{
									String paramName = param.getAttributes().getNamedItem("name").getTextContent();
									Node node = param.getAttributes().getNamedItem("optional");
									if (node != null && "true".equals(node.getTextContent()))
									{
										optionalParams.add(paramName);
									}
									parameterNames.add(paramName);
								}
							}
							//if there is already added a method with more parameters that it skip it .
							boolean addNewApi = true;
							ApiMethod apiToRemove = null;
							for (ApiMethod api : specModel.getApis())
							{
								if (api.getName().equals(functionName))
								{
									if (api.getParameters().size() > parameterNames.size())
									{
										// method already exists with larger number of parameters
										addNewApi = false;
										break;
									}
									else if (api.getParameters().size() < parameterNames.size())
									{
										//if a method already exists with fewer number of parameters force the rest of new parameters as optional
										for (int ii = api.getParameters().size(); ii < parameterNames.size(); ii++)
										{
											optionalParams.add(parameterNames.get(ii));
										}
										apiToRemove = api;
										break;
									}
								}
							}
							if (apiToRemove != null) specModel.getApis().remove(apiToRemove);
							if (addNewApi && !serverSideApi.contains(functionName)) specModel.getApis().add(
								new ApiMethod(functionName, returnType, parameterNames, parameterTypes, optionalParams, metaDataForApi.get(functionName)));
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}


	private static final HashSet<String> addFindmodeModelEntries = new HashSet<String>();
	private static final HashSet<String> addReadOnlyModelEntries = new HashSet<String>();

	private void readModelAndHandlers(List<SpecTemplateModel> specTemplateList)
	{
		ContentSpec spec = StaticContentSpecLoader.getContentSpec();
		for (SpecTemplateModel componentSpec : specTemplateList)
		{
			List<Element> props = Utils.asList(spec.getPropertiesForObjectType(componentSpec.getRepositoryType()));
			List<Element> model = new ArrayList<Element>();
			List<Element> handlers = new ArrayList<Element>();

			for (Element element : props)
			{
				if (BaseComponent.isEventProperty(element.getName()))
				{
					if (!element.getName().equals(IContentSpecConstants.PROPERTY_ONRENDERMETHODID)) handlers.add(element);
				}
				else if (isAllowedProperty(componentSpec.getName(), element.getName()) && getSpecTypeFromRepoType(componentSpec.getName(), element) != null)
				{
					model.add(element);
				}
			}

			if ("listbox".equals(componentSpec.getName()))
			{
				ContentSpec cs = new ContentSpec();
				model.add(cs.new Element(-1, IRepository.FIELDS, "multiselectListbox", IRepository.BOOLEAN, Boolean.FALSE));
			}
			if ("portal".equals(componentSpec.getName()))
			{
				ContentSpec cs = new ContentSpec();
				model.add(cs.new Element(-1, IRepository.FIELDS, "relatedFoundset", -1, null));
				model.add(cs.new Element(-1, IRepository.FIELDS, "childElements", -1, null));
				model.add(cs.new Element(-1, IRepository.FIELDS, "columnHeaders", -1, null));
				model.add(cs.new Element(-1, IRepository.INTEGER, "headerHeight", IRepository.INTEGER, 32));
			}
			if (componentSpec.getRepositoryType() == IRepository.TABPANELS)
			{
				ContentSpec cs = new ContentSpec();
				Element el = cs.new Element(-1, IRepository.FIELDS, "tabIndex", IRepository.SERVERS, "");
				if (isAllowedProperty(componentSpec.getName(), el.getName()) && getSpecTypeFromRepoType(componentSpec.getName(), el) != null)
				{
					model.add(el);
				}
				el = cs.new Element(-1, IRepository.TABPANELS, "tabs", IRepository.SERVERS, null);
				model.add(el);

				el = cs.new Element(-1, IRepository.TABPANELS, "readOnly", IRepository.BOOLEAN, Boolean.FALSE);
				model.add(el);
			}

			if (addFindmodeModelEntries.contains(componentSpec.getName()))
			{
				ContentSpec cs = new ContentSpec();
				Element el = cs.new Element(-1, IRepository.FIELDS, "findmode", IRepository.STRING, null);
				model.add(el);
			}

			if (addReadOnlyModelEntries.contains(componentSpec.getName()))
			{
				ContentSpec cs = new ContentSpec();
				Element el = cs.new Element(-1, IRepository.FIELDS, "readOnly", IRepository.STRING, null);
				model.add(el);
			}

			componentSpec.setModel(model);
			componentSpec.setHandlers(handlers);
		}
	}

	//@formatter:off
	private static final IntHashMap<String> repoTypeMapping = new IntHashMap<String>();
	private static final Map<String, Map<String, String>> componentRepoTypeMappingExceptions = new HashMap<String, Map<String, String>>();
	private static final Map<String, String> repoTypeMappingExceptions = new HashMap<String, String>();
	private static final List<String> internalProperties = new ArrayList<>();
	private static final Map<String, List<String>> perComponentExceptions = new HashMap<>();
	private static final Map<String, List<String>> perComponentInternalProperties = new HashMap<>();
	private static final List<String> serverSideApi = new ArrayList<>();
	private static final Map<String, ApiMethod> overriddenClientSideApi = new HashMap<>();
	private static final Map<String, List<String>> metaDataForApi = new HashMap<String, List<String>>();
	static
	{
		// general type mappings
		repoTypeMapping.put(IRepository.BOOLEAN, "boolean");
		repoTypeMapping.put(IRepository.STRING, "string");
		repoTypeMapping.put(IRepository.BORDER, "{\"type\":\"border\",\"stringformat\":true}");
		repoTypeMapping.put(IRepository.TABS, "tabs[]");
		repoTypeMapping.put(IRepository.COLOR, "color");
		repoTypeMapping.put(IRepository.INTEGER, "int");
		repoTypeMapping.put(IRepository.FONT, "{\"type\":\"font\",\"stringformat\":true}");
		repoTypeMapping.put(IRepository.POINT, "point");
		repoTypeMapping.put(IRepository.DIMENSION, "dimension");
		repoTypeMapping.put(IRepository.INSETS, "dimension");
		repoTypeMapping.put(IRepositoryConstants.MEDIA, "media");
		repoTypeMapping.put(IRepositoryConstants.SERVERS, "object"); // use SERVERS to generate \"object type\"


		String findModeEditable = "{ \"type\":\"findmode\", \"tags\":{\"scope\":\"private\"}, \"for\" : {\"editable\":true}}";
		String findModeEnabled = "{ \"type\":\"findmode\", \"tags\":{\"scope\":\"private\"}, \"for\" : { \"enabled\":true}}";

		addFindmodeModelEntries.add("portal");
		addFindmodeModelEntries.add("calendar");
		addFindmodeModelEntries.add("check");
		addFindmodeModelEntries.add("checkgroup");

		addFindmodeModelEntries.add("combobox");
		addFindmodeModelEntries.add("htmlarea");
		addFindmodeModelEntries.add("listbox");
		addFindmodeModelEntries.add("password");

		addFindmodeModelEntries.add("radio");
		addFindmodeModelEntries.add("radiogroup");
		addFindmodeModelEntries.add("spinner");
		addFindmodeModelEntries.add("textarea");
		addFindmodeModelEntries.add("textfield");
		addFindmodeModelEntries.add("typeahead");


		String readOnlyEnabled = "{ \"type\" : \"readOnly\", \"oppositeOf\" : \"enabled\"}";
		String readOnlyEditable = "{ \"type\" : \"readOnly\", \"oppositeOf\" : \"editable\"}";


//		addReadOnlyModelEntries.add("button");
		addReadOnlyModelEntries.add("calendar");
		addReadOnlyModelEntries.add("check");
		addReadOnlyModelEntries.add("combobox");
		addReadOnlyModelEntries.add("htmlarea");
		addReadOnlyModelEntries.add("listbox");
		addReadOnlyModelEntries.add("password");
		addReadOnlyModelEntries.add("radio");
		addReadOnlyModelEntries.add("spinner");
		addReadOnlyModelEntries.add("textarea");
		addReadOnlyModelEntries.add("textfield");
		addReadOnlyModelEntries.add("typeahead");
		addReadOnlyModelEntries.add("checkgroup");
//		addReadOnlyModelEntries.add("label");
		addReadOnlyModelEntries.add("radiogroup");
//		addReadOnlyModelEntries.add("tabpanel");
//		addReadOnlyModelEntries.add("htmlview");
		addReadOnlyModelEntries.add("imagemedia");
//		addReadOnlyModelEntries.add("splitpane");
//		addReadOnlyModelEntries.add("portal");
//		addReadOnlyModelEntries.add("rectangle");





		// component specific repository element mapping
		HashMap<String, String> htmlViewRepoTypeMapping = new HashMap<String, String>();
		htmlViewRepoTypeMapping.put(StaticContentSpecLoader.PROPERTY_DATAPROVIDERID.getPropertyName(),
			"{ \"type\" :\"dataprovider\", \"ondatachange\": { \"onchange\":\"onDataChangeMethodID\", \"callback\":\"onDataChangeCallback\"}, \"parsehtml\":true, \"displayTagsPropertyName\" : \"displaysTags\"}");
		htmlViewRepoTypeMapping.put(StaticContentSpecLoader.PROPERTY_STYLECLASS.getPropertyName(), "{ \"type\" :\"styleclass\", \"tags\": { \"scope\" :\"design\" } , \"values\" :[]}");
		htmlViewRepoTypeMapping.put(StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName(), "{\"type\" :\"dimension\",  \"default\" : {\"width\":140, \"height\":140}}");
		componentRepoTypeMappingExceptions.put("htmlview", htmlViewRepoTypeMapping);



		HashMap<String, String> buttonTypeMapping = new HashMap<String, String>();
		buttonTypeMapping.put(StaticContentSpecLoader.PROPERTY_STYLECLASS.getPropertyName(),
			"{ \"type\" :\"styleclass\", \"tags\": { \"scope\" :\"design\" }, \"values\" :[\"btn\",\"btn-default\",\"btn-lg\",\"btn-sm\",\"btn-xs\"]}");
		buttonTypeMapping.put(StaticContentSpecLoader.PROPERTY_VERTICALALIGNMENT.getPropertyName(),
			"{\"type\" :\"int\", \"tags\": { \"scope\" :\"design\" }, \"values\" :[{\"TOP\":1}, {\"CENTER\":0} ,{\"BOTTOM\":3}], \"default\" : 0}");
		buttonTypeMapping.put(StaticContentSpecLoader.PROPERTY_HORIZONTALALIGNMENT.getPropertyName(),
			"{\"type\" :\"int\", \"tags\": { \"scope\" :\"design\" }, \"values\" :[{\"LEFT\":2}, {\"CENTER\":0},{\"RIGHT\":4}], \"default\" : 0}");
		buttonTypeMapping.put(StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName(), "{\"type\" :\"dimension\",  \"default\" : {\"width\":80, \"height\":20}}");
		buttonTypeMapping.put(StaticContentSpecLoader.PROPERTY_FORMAT.getPropertyName(), "{\"for\":[\"dataProviderID\"] , \"type\" :\"format\"}");
		buttonTypeMapping.put(StaticContentSpecLoader.PROPERTY_ENABLED.getPropertyName(), "{ \"type\": \"protected\", \"blockingOn\": false, \"default\": true, \"for\": [\"" + StaticContentSpecLoader.PROPERTY_ONACTIONMETHODID.getPropertyName()+ "\",\""
			+ StaticContentSpecLoader.PROPERTY_ONDOUBLECLICKMETHODID.getPropertyName()+ "\",\""
			+ StaticContentSpecLoader.PROPERTY_ONRIGHTCLICKMETHODID.getPropertyName()+ "\"] }");
		buttonTypeMapping.put(StaticContentSpecLoader.PROPERTY_TEXT.getPropertyName(), "{ \"type\" : \"tagstring\", \"displayTagsPropertyName\" : \"displaysTags\" , \"tags\": { \"directEdit\" : \"true\" } }");
		componentRepoTypeMappingExceptions.put("button", buttonTypeMapping);

		HashMap<String, String> portalTypeMapping = new HashMap<String, String>();
		portalTypeMapping.put(StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName(), "{\"type\" :\"dimension\",  \"default\" : {\"width\":200, \"height\":200}}");
		portalTypeMapping.put("relatedFoundset", "foundset");
		portalTypeMapping.put("headerHeight", "{\"type\" :\"int\",  \"default\" : 32}");
		portalTypeMapping.put("childElements", "{ \"type\" : \"component[]\", \"elementConfig\" : {\"forFoundset\": \"relatedFoundset\"} }");
		portalTypeMapping.put("columnHeaders", "{ \"type\" : \"tagstring[]\", \"tags\" : {\"scope\": \"private\"} }");
		portalTypeMapping.put(StaticContentSpecLoader.PROPERTY_ENABLED.getPropertyName(), "{ \"type\": \"protected\", \"blockingOn\": false, \"default\": true, \"for\": [\"" + StaticContentSpecLoader.PROPERTY_ONDRAGENDMETHODID.getPropertyName()+ "\",\""
			+ StaticContentSpecLoader.PROPERTY_ONDRAGMETHODID.getPropertyName()+ "\",\""
			+ StaticContentSpecLoader.PROPERTY_ONDRAGOVERMETHODID.getPropertyName()+"\",\""
			+ StaticContentSpecLoader.PROPERTY_ONDROPMETHODID.getPropertyName()+ "\"] }");
		portalTypeMapping.put("findmode", findModeEnabled);
		componentRepoTypeMappingExceptions.put("portal", portalTypeMapping);

		HashMap<String, String> calendarTypeMapping = new HashMap<String, String>();
		calendarTypeMapping.put(StaticContentSpecLoader.PROPERTY_STYLECLASS.getPropertyName(),
			"{ \"type\" :\"styleclass\", \"tags\": { \"scope\" :\"design\" }, \"values\" :[\"form-control\", \"input-sm\", \"svy-padding-xs\", \"svy-line-height-normal\"]}");
		calendarTypeMapping.put(StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName(), "{\"type\" :\"dimension\",  \"default\" : {\"width\":140, \"height\":20}}");
		calendarTypeMapping.put(StaticContentSpecLoader.PROPERTY_EDITABLE.getPropertyName(), "{\"type\":\"boolean\", \"default\":true}");
		calendarTypeMapping.put("findmode", findModeEditable);
		calendarTypeMapping.put("readOnly", readOnlyEnabled);
		calendarTypeMapping.put(StaticContentSpecLoader.PROPERTY_FORMAT.getPropertyName(), "{\"for\":[\"dataProviderID\"] , \"type\" :\"format\"}");
		componentRepoTypeMappingExceptions.put("calendar", calendarTypeMapping);

		HashMap<String, String> checkTypeMapping = new HashMap<String, String>();
		checkTypeMapping.put(StaticContentSpecLoader.PROPERTY_STYLECLASS.getPropertyName(), "{ \"type\" :\"styleclass\", \"tags\": { \"scope\" :\"design\" }, \"values\" :[\"checkbox\"]}");
		checkTypeMapping.put(StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName(), "{\"type\" :\"dimension\",  \"default\" : {\"width\":140, \"height\":20}}");
		checkTypeMapping.put("findmode", findModeEditable);
		checkTypeMapping.put("readOnly", readOnlyEnabled);
		componentRepoTypeMappingExceptions.put("check", checkTypeMapping);

		HashMap<String, String> checkGroupTypeMapping = new HashMap<String, String>();
		checkGroupTypeMapping.put(StaticContentSpecLoader.PROPERTY_STYLECLASS.getPropertyName(),
			"{ \"type\" :\"styleclass\", \"tags\": { \"scope\" :\"design\" }, \"values\" :[\"form-control\", \"input-sm\", \"svy-padding-xs\"]}");
		checkGroupTypeMapping.put(StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName(), "{\"type\" :\"dimension\",  \"default\" : {\"width\":140, \"height\":20}}");
		checkGroupTypeMapping.put("findmode", findModeEditable);
		checkGroupTypeMapping.put("readOnly", readOnlyEditable);
		componentRepoTypeMappingExceptions.put("checkgroup", checkGroupTypeMapping);

		HashMap<String, String> comboTypeMapping = new HashMap<String, String>();
		comboTypeMapping.put(StaticContentSpecLoader.PROPERTY_STYLECLASS.getPropertyName(),
			"{ \"type\" :\"styleclass\", \"tags\": { \"scope\" :\"design\" }, \"values\" :[\"form-control\", \"input-sm\", \"svy-padding-xs\", \"select2-container-svy-xs\"]}");
		comboTypeMapping.put(StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName(), "{\"type\" :\"dimension\",  \"default\" : {\"width\":140, \"height\":20}}");
		comboTypeMapping.put(StaticContentSpecLoader.PROPERTY_EDITABLE.getPropertyName(), "{\"type\":\"boolean\", \"default\":true}");
		comboTypeMapping.put("findmode", findModeEditable);
		comboTypeMapping.put("readOnly", readOnlyEnabled);
		componentRepoTypeMappingExceptions.put("combobox", comboTypeMapping);

		HashMap<String, String> htmlAreaMapping = new HashMap<String, String>();
		htmlAreaMapping.put(StaticContentSpecLoader.PROPERTY_STYLECLASS.getPropertyName(), "{ \"type\" :\"styleclass\", \"tags\": { \"scope\" :\"design\" }, \"values\" :[]}");
		htmlAreaMapping.put(StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName(), "{\"type\" :\"dimension\",  \"default\" : {\"width\":370, \"height\":250}}");
		htmlAreaMapping.put("findmode", findModeEditable);
		htmlAreaMapping.put("readOnly", readOnlyEditable);
		componentRepoTypeMappingExceptions.put("htmlarea", htmlAreaMapping);

		HashMap<String, String> imageMediaMapping = new HashMap<String, String>();
		imageMediaMapping.put(StaticContentSpecLoader.PROPERTY_STYLECLASS.getPropertyName(), "{ \"type\" :\"styleclass\", \"tags\": { \"scope\" :\"design\" }, \"values\" :[]}");
		imageMediaMapping.put(StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName(), "{\"type\" :\"dimension\",  \"default\" : {\"width\":140, \"height\":140}}");
		imageMediaMapping.put("readOnly", readOnlyEditable);
		imageMediaMapping.put(StaticContentSpecLoader.PROPERTY_FORMAT.getPropertyName(), "{\"for\":[\"dataProviderID\"] , \"type\" :\"format\"}");
		componentRepoTypeMappingExceptions.put("imagemedia", imageMediaMapping);

		HashMap<String, String> labelMapping = new HashMap<String, String>();
		labelMapping.put(StaticContentSpecLoader.PROPERTY_STYLECLASS.getPropertyName(), "{ \"type\" :\"styleclass\", \"tags\": { \"scope\" :\"design\" }, \"values\" :[]}");
		labelMapping.put(StaticContentSpecLoader.PROPERTY_VERTICALALIGNMENT.getPropertyName(),
			"{\"type\" :\"int\", \"tags\": { \"scope\" :\"design\" }, \"values\" :[{\"TOP\":1}, {\"CENTER\":0} ,{\"BOTTOM\":3}], \"default\" : 0}");
		labelMapping.put(StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName(), "{\"type\" :\"dimension\",  \"default\" : {\"width\":80, \"height\":20}}");
		labelMapping.put(StaticContentSpecLoader.PROPERTY_ENABLED.getPropertyName(), "{ \"type\": \"protected\", \"blockingOn\": false, \"default\": true, \"for\": [\"" + StaticContentSpecLoader.PROPERTY_ONACTIONMETHODID.getPropertyName()+ "\",\""
			+ StaticContentSpecLoader.PROPERTY_ONDOUBLECLICKMETHODID.getPropertyName()+ "\",\""
			+ StaticContentSpecLoader.PROPERTY_ONRIGHTCLICKMETHODID.getPropertyName()+ "\"] }");
		labelMapping.put(StaticContentSpecLoader.PROPERTY_TEXT.getPropertyName(), "{ \"type\" : \"tagstring\", \"displayTagsPropertyName\" : \"displaysTags\" , \"tags\": { \"directEdit\" : \"true\" } }");
		componentRepoTypeMappingExceptions.put("label", labelMapping);

		HashMap<String, String> listboxTypeMapping = new HashMap<String, String>();
		listboxTypeMapping.put(StaticContentSpecLoader.PROPERTY_STYLECLASS.getPropertyName(),
			"{ \"type\" :\"styleclass\", \"tags\": { \"scope\" :\"design\" }, \"values\" :[\"form-control\", \"input-sm\", \"svy-padding-xs\"]}");
		listboxTypeMapping.put(StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName(), "{\"type\" :\"dimension\",  \"default\" : {\"width\":140, \"height\":140}}");
		listboxTypeMapping.put("findmode", findModeEditable);
		listboxTypeMapping.put("readOnly", readOnlyEditable);
		componentRepoTypeMappingExceptions.put("listbox", listboxTypeMapping);

		HashMap<String, String> passwordMapping = new HashMap<String, String>();
		passwordMapping.put(StaticContentSpecLoader.PROPERTY_STYLECLASS.getPropertyName(),
			"{ \"type\" :\"styleclass\", \"tags\": { \"scope\" :\"design\" }, \"values\" :[\"form-control\", \"input-sm\", \"svy-padding-xs\"]}");
		passwordMapping.put(StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName(), "{\"type\" :\"dimension\",  \"default\" : {\"width\":140, \"height\":20}}");
		passwordMapping.put("findmode", findModeEditable);
		passwordMapping.put("readOnly", readOnlyEditable);
		componentRepoTypeMappingExceptions.put("password", passwordMapping);

		HashMap<String, String> radioTypeMapping = new HashMap<String, String>();
		radioTypeMapping.put(StaticContentSpecLoader.PROPERTY_STYLECLASS.getPropertyName(), "{ \"type\" :\"styleclass\", \"tags\": { \"scope\" :\"design\" }, \"values\" :[\"radio\"]}");
		radioTypeMapping.put(StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName(), "{\"type\" :\"dimension\",  \"default\" : {\"width\":140, \"height\":20}}");
		radioTypeMapping.put("findmode", findModeEditable);
		radioTypeMapping.put("readOnly", readOnlyEnabled);
		componentRepoTypeMappingExceptions.put("radio", radioTypeMapping);

		HashMap<String, String> radioGroupTypeMapping = new HashMap<String, String>();
		radioGroupTypeMapping.put(StaticContentSpecLoader.PROPERTY_STYLECLASS.getPropertyName(),
			"{ \"type\" :\"styleclass\", \"tags\": { \"scope\" :\"design\" }, \"values\" :[\"form-control\", \"input-sm\", \"svy-padding-xs\"]}");
		radioGroupTypeMapping.put(StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName(), "{\"type\" :\"dimension\",  \"default\" : {\"width\":140, \"height\":20}}");
		radioGroupTypeMapping.put("findmode", findModeEditable);
		radioGroupTypeMapping.put("readOnly", readOnlyEditable);
		componentRepoTypeMappingExceptions.put("radiogroup", radioGroupTypeMapping);

		HashMap<String, String> spinnerMapping = new HashMap<String, String>();
		spinnerMapping.put(StaticContentSpecLoader.PROPERTY_STYLECLASS.getPropertyName(), "{ \"type\" :\"styleclass\", \"tags\": { \"scope\" :\"design\" }, \"values\" :[]}");
		spinnerMapping.put(StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName(), "{\"type\" :\"dimension\",  \"default\" : {\"width\":140, \"height\":20}}");
		spinnerMapping.put("findmode", findModeEditable);
		spinnerMapping.put("readOnly", readOnlyEditable);
		componentRepoTypeMappingExceptions.put("spinner", spinnerMapping);

		HashMap<String, String> splitpaneMapping = new HashMap<String, String>();
		splitpaneMapping.put(StaticContentSpecLoader.PROPERTY_STYLECLASS.getPropertyName(), "{ \"type\" :\"styleclass\", \"tags\": { \"scope\" :\"design\" }, \"values\" :[]}");
		splitpaneMapping.put(StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName(), "{\"type\" :\"dimension\",  \"default\" : {\"width\":300, \"height\":300}}");
		splitpaneMapping.put(StaticContentSpecLoader.PROPERTY_ENABLED.getPropertyName(), "{ \"type\": \"protected\", \"blockingOn\": false, \"default\": true, \"for\": [\"" + StaticContentSpecLoader.PROPERTY_ONCHANGEMETHODID.getPropertyName()+ "\",\""
			+ StaticContentSpecLoader.PROPERTY_ONTABCHANGEMETHODID.getPropertyName()+ "\"] }");
		splitpaneMapping.put("readOnly", "{ \"type\": \"protected\", \"for\": [\"" + StaticContentSpecLoader.PROPERTY_ONCHANGEMETHODID.getPropertyName()+ "\",\""
			+ StaticContentSpecLoader.PROPERTY_ONTABCHANGEMETHODID.getPropertyName()+ "\"] }");
		componentRepoTypeMappingExceptions.put("splitpane", splitpaneMapping);

		HashMap<String, String> tabpanelMapping = new HashMap<String, String>();
		tabpanelMapping.put(StaticContentSpecLoader.PROPERTY_STYLECLASS.getPropertyName(), "{ \"type\" :\"styleclass\", \"tags\": { \"scope\" :\"design\" }, \"values\" :[]}");
		tabpanelMapping.put(StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName(), "{\"type\" :\"dimension\",  \"default\" : {\"width\":300, \"height\":300}}");
		tabpanelMapping.put(StaticContentSpecLoader.PROPERTY_ENABLED.getPropertyName(), "{ \"type\": \"protected\", \"blockingOn\": false, \"default\": true, \"for\": [\"" + StaticContentSpecLoader.PROPERTY_ONCHANGEMETHODID.getPropertyName()+ "\",\""
			+ StaticContentSpecLoader.PROPERTY_ONTABCHANGEMETHODID.getPropertyName()+ "\"] }");
		tabpanelMapping.put("readOnly", "{ \"type\": \"protected\", \"for\": [\"" + StaticContentSpecLoader.PROPERTY_ONCHANGEMETHODID.getPropertyName()+ "\",\""
			+ StaticContentSpecLoader.PROPERTY_ONTABCHANGEMETHODID.getPropertyName()+ "\"] }");
		componentRepoTypeMappingExceptions.put("tabpanel", tabpanelMapping);

		HashMap<String, String> textareaTypeMapping = new HashMap<String, String>();
		textareaTypeMapping.put(StaticContentSpecLoader.PROPERTY_STYLECLASS.getPropertyName(),
			"{ \"type\" :\"styleclass\", \"tags\": { \"scope\" :\"design\" }, \"values\" :[\"form-control\", \"input-sm\", \"svy-padding-xs\"]}");
		textareaTypeMapping.put(StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName(), "{\"type\" :\"dimension\",  \"default\" : {\"width\":140, \"height\":140}}");
		textareaTypeMapping.put("findmode", findModeEditable);
		textareaTypeMapping.put("readOnly", readOnlyEditable);
		textareaTypeMapping.put(StaticContentSpecLoader.PROPERTY_FORMAT.getPropertyName(), "{\"for\":[\"dataProviderID\"] , \"type\" :\"format\"}");
		componentRepoTypeMappingExceptions.put("textarea", textareaTypeMapping);

		HashMap<String, String> textfieldTypeMapping = new HashMap<String, String>();
		textfieldTypeMapping.put(StaticContentSpecLoader.PROPERTY_STYLECLASS.getPropertyName(),
			"{ \"type\" :\"styleclass\", \"tags\": { \"scope\" :\"design\" }, \"values\" :[\"form-control\", \"input-sm\", \"svy-padding-xs\"]}");
		textfieldTypeMapping.put(StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName(), "{\"type\" :\"dimension\",  \"default\" : {\"width\":140, \"height\":20}}");
		textfieldTypeMapping.put("findmode", findModeEditable);
		textfieldTypeMapping.put("readOnly", readOnlyEditable);
		componentRepoTypeMappingExceptions.put("textfield", textfieldTypeMapping);

		HashMap<String, String> typeaheadTypeMapping = new HashMap<String, String>();
		typeaheadTypeMapping.put(StaticContentSpecLoader.PROPERTY_STYLECLASS.getPropertyName(),
			"{ \"type\" :\"styleclass\", \"tags\": { \"scope\" :\"design\" }, \"values\" :[\"form-control\", \"input-sm\", \"svy-padding-xs\"]}");
		typeaheadTypeMapping.put(StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName(), "{\"type\" :\"dimension\",  \"default\" : {\"width\":140, \"height\":20}}");
		typeaheadTypeMapping.put(StaticContentSpecLoader.PROPERTY_VALUELISTID.getPropertyName(), "{ \"type\" : \"valuelist\", \"tags\": { \"scope\" :\"design\" }, \"for\": \"dataProviderID\", \"default\":\"autoVL\", \"canOptimize\":false}");
		typeaheadTypeMapping.put("findmode", findModeEditable);
		typeaheadTypeMapping.put("readOnly", readOnlyEditable);
		componentRepoTypeMappingExceptions.put("typeahead", typeaheadTypeMapping);

		//speciffic repository element mapping
		repoTypeMappingExceptions.put(StaticContentSpecLoader.PROPERTY_DATAPROVIDERID.getPropertyName(),
			"{ \"type\":\"dataprovider\", \"tags\": { \"scope\" :\"design\" }, \"ondatachange\": { \"onchange\":\"onDataChangeMethodID\", \"callback\":\"onDataChangeCallback\"}, \"displayTagsPropertyName\" : \"displaysTags\"}");
		repoTypeMappingExceptions.put(StaticContentSpecLoader.PROPERTY_FORMAT.getPropertyName(), "{\"for\":[\"valuelistID\",\"dataProviderID\"] , \"type\" :\"format\"}");
		repoTypeMappingExceptions.put(StaticContentSpecLoader.PROPERTY_TEXT.getPropertyName(), "{ \"type\" : \"tagstring\", \"displayTagsPropertyName\" : \"displaysTags\" }");
		repoTypeMappingExceptions.put(StaticContentSpecLoader.PROPERTY_PLACEHOLDERTEXT.getPropertyName(), "{ \"type\" : \"tagstring\", \"displayTagsPropertyName\" : \"displaysTags\" }");
		repoTypeMappingExceptions.put(StaticContentSpecLoader.PROPERTY_TOOLTIPTEXT.getPropertyName(), "{ \"type\" : \"tagstring\", \"displayTagsPropertyName\" : \"displaysTags\" }");
		repoTypeMappingExceptions.put(StaticContentSpecLoader.PROPERTY_DISPLAYSTAGS.getPropertyName(), "{ \"type\" : \"boolean\", \"tags\": { \"scope\" : \"design\" } }");
		repoTypeMappingExceptions.put(StaticContentSpecLoader.PROPERTY_VALUELISTID.getPropertyName(), "{ \"type\" : \"valuelist\", \"tags\": { \"scope\" :\"design\" }, \"for\": \"dataProviderID\"}");
		repoTypeMappingExceptions.put(StaticContentSpecLoader.PROPERTY_ROLLOVERIMAGEMEDIAID.getPropertyName(), "media");
		repoTypeMappingExceptions.put(StaticContentSpecLoader.PROPERTY_IMAGEMEDIAID.getPropertyName(), "media");
		repoTypeMappingExceptions.put(StaticContentSpecLoader.PROPERTY_HORIZONTALALIGNMENT.getPropertyName(),
			"{\"type\" :\"int\", \"tags\": { \"scope\" :\"design\" }, \"values\" :[{\"LEFT\":2}, {\"CENTER\":0},{\"RIGHT\":4}],\"default\" : -1}");
		repoTypeMappingExceptions.put(StaticContentSpecLoader.PROPERTY_TEXTROTATION.getPropertyName(), "{\"type\" :\"int\", \"tags\": { \"scope\" :\"design\" }, \"values\" :[0,90,180,270]}");
		repoTypeMappingExceptions.put(StaticContentSpecLoader.PROPERTY_TABORIENTATION.getPropertyName(),
			"{\"type\" :\"int\", \"tags\": { \"scope\" :\"design\" }, \"values\" :[{\"default\" :0}, {\"TOP\":1}, {\"HIDE\":-1}]}");
		repoTypeMappingExceptions.put(StaticContentSpecLoader.PROPERTY_TABSEQ.getPropertyName(), "{\"type\" :\"tabseq\", \"tags\": { \"scope\" :\"design\" }}");
		repoTypeMappingExceptions.put(StaticContentSpecLoader.PROPERTY_MEDIAOPTIONS.getPropertyName(), "{\"type\" :\"mediaoptions\", \"tags\": { \"scope\" :\"design\" }}");
		repoTypeMappingExceptions.put(StaticContentSpecLoader.PROPERTY_LABELFOR.getPropertyName(), "labelfor");
		repoTypeMappingExceptions.put("tabs", "{\"type\":\"tab[]\", \"droppable\":true}");
		repoTypeMappingExceptions.put("tabIndex", "object");
		repoTypeMappingExceptions.put(StaticContentSpecLoader.PROPERTY_MARGIN.getPropertyName(), "{\"type\" :\"insets\", \"tags\": { \"scope\" :\"design\" }}");
		repoTypeMappingExceptions.put(StaticContentSpecLoader.PROPERTY_ROLLOVERCURSOR.getPropertyName(), "{\"type\" :\"int\", \"tags\": { \"scope\" :\"design\" }}");
		repoTypeMappingExceptions.put(StaticContentSpecLoader.PROPERTY_SCROLLBARS.getPropertyName(), "{\"type\" :\"scrollbars\", \"tags\": { \"scope\" :\"design\" }}");
		repoTypeMappingExceptions.put(StaticContentSpecLoader.PROPERTY_SELECTONENTER.getPropertyName(), "{\"type\" :\"boolean\", \"tags\": { \"scope\" :\"design\" }}");
		repoTypeMappingExceptions.put(StaticContentSpecLoader.PROPERTY_CONTAINSFORMID.getPropertyName(), "form");
		repoTypeMappingExceptions.put(StaticContentSpecLoader.PROPERTY_VISIBLE.getPropertyName(), "\"visible\"");
		repoTypeMappingExceptions.put(StaticContentSpecLoader.PROPERTY_ENABLED.getPropertyName(), "{ \"type\": \"protected\", \"blockingOn\": false, \"default\": true, \"for\": [\"" + StaticContentSpecLoader.PROPERTY_DATAPROVIDERID.getPropertyName()+ "\",\""
																																													+ StaticContentSpecLoader.PROPERTY_ONACTIONMETHODID.getPropertyName()+ "\",\""
																																													+ StaticContentSpecLoader.PROPERTY_ONDATACHANGEMETHODID.getPropertyName()+ "\",\""
																																													+ StaticContentSpecLoader.PROPERTY_ONFOCUSGAINEDMETHODID.getPropertyName()+ "\",\""
																																													+ StaticContentSpecLoader.PROPERTY_ONFOCUSLOSTMETHODID.getPropertyName()+ "\",\""
																																													+ StaticContentSpecLoader.PROPERTY_ONRIGHTCLICKMETHODID.getPropertyName()+ "\"] }");
		repoTypeMappingExceptions.put(StaticContentSpecLoader.PROPERTY_EDITABLE.getPropertyName(), "{ \"type\": \"protected\", \"blockingOn\": false, \"default\": true,\"for\": [\"" + StaticContentSpecLoader.PROPERTY_DATAPROVIDERID.getPropertyName()+ "\",\""
																																													+ StaticContentSpecLoader.PROPERTY_ONDATACHANGEMETHODID.getPropertyName()+ "\",\""
																																													+ StaticContentSpecLoader.PROPERTY_ONFOCUSGAINEDMETHODID.getPropertyName()+ "\",\""
																																													+ StaticContentSpecLoader.PROPERTY_ONFOCUSLOSTMETHODID.getPropertyName()+ "\"] }");
		repoTypeMappingExceptions.put("readOnly", "{ \"type\": \"protected\", \"for\": \"dataProviderID\" }");

		//internal properties (properties that should not be generated for any component)
		internalProperties.add(StaticContentSpecLoader.PROPERTY_EXTENDSID.getPropertyName());
		internalProperties.add(StaticContentSpecLoader.PROPERTY_ANCHORS.getPropertyName());
		internalProperties.add(StaticContentSpecLoader.PROPERTY_NAME.getPropertyName());
//		internalProperties.add(StaticContentSpecLoader.PROPERTY_DISPLAYSTAGS.getPropertyName());
		internalProperties.add(StaticContentSpecLoader.PROPERTY_GROUPID.getPropertyName());
		internalProperties.add(StaticContentSpecLoader.PROPERTY_LOCKED.getPropertyName());
		internalProperties.add(StaticContentSpecLoader.PROPERTY_PRINTSLIDING.getPropertyName());
		internalProperties.add(StaticContentSpecLoader.PROPERTY_ROTATION.getPropertyName());
		internalProperties.add(StaticContentSpecLoader.PROPERTY_CUSTOMPROPERTIES.getPropertyName());
		internalProperties.add(StaticContentSpecLoader.PROPERTY_PRINTSLIDING.getPropertyName());
		internalProperties.add(StaticContentSpecLoader.PROPERTY_FORMINDEX.getPropertyName());
		internalProperties.add(StaticContentSpecLoader.PROPERTY_LABELFOR.getPropertyName());
		internalProperties.add(StaticContentSpecLoader.PROPERTY_DISPLAYTYPE.getPropertyName());
//		internalProperties.add(StaticContentSpecLoader.PROPERTY_SHOWFOCUS.getPropertyName());
		internalProperties.add(StaticContentSpecLoader.PROPERTY_SHOWCLICK.getPropertyName());
		internalProperties.add(StaticContentSpecLoader.PROPERTY_SELECTONENTER.getPropertyName());
		internalProperties.add(StaticContentSpecLoader.PROPERTY_VERTICALALIGNMENT.getPropertyName());
		internalProperties.add(StaticContentSpecLoader.PROPERTY_USERTF.getPropertyName());
		internalProperties.add(StaticContentSpecLoader.PROPERTY_PRINTABLE.getPropertyName());
		internalProperties.add(StaticContentSpecLoader.PROPERTY_SCROLLTABS.getPropertyName());
		internalProperties.add(StaticContentSpecLoader.PROPERTY_CLOSEONTABS.getPropertyName());

		// per component exceptions to internal properties (for ex labelfor should be only for datalabel)
		perComponentExceptions.put(
			"label",
			 Arrays.asList((StaticContentSpecLoader.PROPERTY_LABELFOR.getPropertyName()),
				(StaticContentSpecLoader.PROPERTY_VERTICALALIGNMENT.getPropertyName())));
		perComponentExceptions.put("textfield",  Arrays.asList((StaticContentSpecLoader.PROPERTY_SELECTONENTER.getPropertyName())));
		perComponentExceptions.put("typeahead",  Arrays.asList((StaticContentSpecLoader.PROPERTY_SELECTONENTER.getPropertyName())));
		perComponentExceptions.put("password",  Arrays.asList((StaticContentSpecLoader.PROPERTY_SELECTONENTER.getPropertyName())));
		perComponentExceptions.put("calendar",  Arrays.asList((StaticContentSpecLoader.PROPERTY_SELECTONENTER.getPropertyName())));
		perComponentExceptions.put("button",  Arrays.asList((StaticContentSpecLoader.PROPERTY_VERTICALALIGNMENT.getPropertyName())));
		perComponentInternalProperties.put("portal",  Arrays.asList((StaticContentSpecLoader.PROPERTY_RELATIONNAME.getPropertyName())));
		perComponentInternalProperties.put(
			"htmlview",
			 Arrays.asList((StaticContentSpecLoader.PROPERTY_EDITABLE.getPropertyName()),
				(StaticContentSpecLoader.PROPERTY_VALUELISTID.getPropertyName()), (StaticContentSpecLoader.PROPERTY_FORMAT.getPropertyName()),
				(StaticContentSpecLoader.PROPERTY_PLACEHOLDERTEXT.getPropertyName()), (StaticContentSpecLoader.PROPERTY_SELECTONENTER.getPropertyName())));
		perComponentInternalProperties.put("splitpane",  Arrays.asList("tabIndex"));
		perComponentInternalProperties.put("calendar",  Arrays.asList((StaticContentSpecLoader.PROPERTY_VALUELISTID.getPropertyName()),StaticContentSpecLoader.PROPERTY_SCROLLBARS.getPropertyName()));
		perComponentInternalProperties.put("htmlarea",  Arrays.asList((StaticContentSpecLoader.PROPERTY_VALUELISTID.getPropertyName()),(StaticContentSpecLoader.PROPERTY_FORMAT.getPropertyName())));
		perComponentInternalProperties.put("imagemedia",  Arrays.asList((StaticContentSpecLoader.PROPERTY_VALUELISTID.getPropertyName()),(StaticContentSpecLoader.PROPERTY_FORMAT.getPropertyName())));
		perComponentInternalProperties.put("password", Arrays.asList((StaticContentSpecLoader.PROPERTY_VALUELISTID.getPropertyName()),StaticContentSpecLoader.PROPERTY_FORMAT.getPropertyName(),StaticContentSpecLoader.PROPERTY_SCROLLBARS.getPropertyName()));
		perComponentInternalProperties.put("textarea",  Arrays.asList((StaticContentSpecLoader.PROPERTY_VALUELISTID.getPropertyName())));
		perComponentInternalProperties.put("check",  Arrays.asList((StaticContentSpecLoader.PROPERTY_FORMAT.getPropertyName()),StaticContentSpecLoader.PROPERTY_SCROLLBARS.getPropertyName()));
		perComponentInternalProperties.put("radio",  Arrays.asList((StaticContentSpecLoader.PROPERTY_FORMAT.getPropertyName()),StaticContentSpecLoader.PROPERTY_SCROLLBARS.getPropertyName()));
		perComponentInternalProperties.put("radiogroup",  Arrays.asList((StaticContentSpecLoader.PROPERTY_FORMAT.getPropertyName())));
		perComponentInternalProperties.put("checkgroup",  Arrays.asList((StaticContentSpecLoader.PROPERTY_FORMAT.getPropertyName())));
		perComponentInternalProperties.put("textfield",  Arrays.asList(StaticContentSpecLoader.PROPERTY_SCROLLBARS.getPropertyName()));
		perComponentInternalProperties.put("combobox",  Arrays.asList(StaticContentSpecLoader.PROPERTY_SCROLLBARS.getPropertyName()));
		perComponentInternalProperties.put("spinner",  Arrays.asList(StaticContentSpecLoader.PROPERTY_SCROLLBARS.getPropertyName()));
		perComponentInternalProperties.put("typeahead",  Arrays.asList(StaticContentSpecLoader.PROPERTY_SCROLLBARS.getPropertyName()));

		serverSideApi.add("getAbsoluteFormLocationY");
		serverSideApi.add("getClientProperty");
		serverSideApi.add("getDataProviderID");
		serverSideApi.add("getDesignTimeProperty");
		serverSideApi.add("getElementType");
		//serverSideApi.add("getHeight");
		serverSideApi.add("getLabelForElementNames");
		//serverSideApi.add("getLocationX");
		//serverSideApi.add("getLocationY");
		serverSideApi.add("getName");
		serverSideApi.add("getValueListName");
		//serverSideApi.add("getWidth");
		serverSideApi.add("putClientProperty");
		serverSideApi.add("setLocation");
		serverSideApi.add("setSize");
		serverSideApi.add("setValueListItems");
		serverSideApi.add("getLabelForElementName");

		final String callOnAll = "callOn: 1"; // ALL_RECORDS_IF_TEMPLATE; see globalServoyCustomTypes.spec
		//metaDataForApi.put("setValueListItems", Arrays.asList(new String[] { callOnAll }));
	}
	// @formatter:on

	public static boolean isAllowedProperty(String componentName, String propName)
	{
		if (perComponentExceptions.get(componentName) != null && perComponentExceptions.get(componentName).contains(propName))
		{
			return true;
		}
		if (perComponentInternalProperties.get(componentName) != null && perComponentInternalProperties.get(componentName).contains(propName))
		{
			return false;
		}
		return !internalProperties.contains(propName);
	}

	public static String getSpecTypeFromRepoType(String compName, Element element)
	{
		String ret = null;
		if (componentRepoTypeMappingExceptions.containsKey(compName))
		{
			Map<String, String> typeMapping = componentRepoTypeMappingExceptions.get(compName);
			if (typeMapping.get(element.getName()) != null)
			{
				ret = typeMapping.get(element.getName());
				return ret == null ? element.getName() : ret;
			}
		}
		if (repoTypeMappingExceptions.get(element.getName()) != null)
		{
			//treat exceptions to the typeMapping rule  , ex: dataproviderID is a normal string in IRepository.
			ret = repoTypeMappingExceptions.get(element.getName());
		}
		else
		{
			ret = repoTypeMapping.get(element.getTypeID());
		}
		//no type string found get the name of the element
		return ret == null ? element.getName() : ret;
	}

	private static Map<String, String> docTypeMappingExceptions = new HashMap<String, String>();
	static
	{
		docTypeMappingExceptions.put("[B", "byte");
	}

	private String specSpecTypeFromDoc(String typecode)
	{
		String ret = "";
		String suffix = "";
		int idx = typecode.lastIndexOf(".");
		if (typecode.startsWith("[")) suffix = " []";
		if (typecode.startsWith("[[")) suffix = " [][]";
		if (idx > 0)
		{// ex: java.lang.String   case
			ret = typecode.substring(idx + 1).toLowerCase();
		}
		else
		{
			//ex int
			ret = typecode;
		}

		if (docTypeMappingExceptions.get(ret) != null)
		{
			ret = docTypeMappingExceptions.get(ret);
		}
		ret = ret.replaceAll(";", "");
		return ret + suffix;
	}

	public void generate(Object dataMocel, Writer writer) throws IOException
	{
		Template template = cfg.getTemplate("spec_file.ftl");
		try
		{
			template.process(dataMocel, writer);
		}
		catch (TemplateException e)
		{
			throw new RuntimeException(e);
		}
	}


}
