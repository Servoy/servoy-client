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

package com.servoy.j2db.server.webclient2.component.spec;

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
	private static final String SERVOYDOC_LOCATION = "../com.servoy.eclipse.core/src/com/servoy/eclipse/core/doc/servoydoc.xml";
	private static final String COMPONENTS_LOCATION = ".";
	private static final String SPEC_EXTENSION = "spec"; // TIP : the first time you run this tool change "spec" extension to "spec2" to be able to compare with existing manual spec

	// @formatter:off
	private static final List<SpecTemplateModel> specTemplateList = new ArrayList<SpecTemplateModel>();
	static
	{
		specTemplateList.add(new SpecTemplateModel("button", "Button", IRepository.GRAPHICALCOMPONENTS, IRuntimeDataButton.class));
		specTemplateList.add(new SpecTemplateModel("calendar", "Calendar", IRepository.FIELDS, IRuntimeCalendar.class));
		specTemplateList.add(new SpecTemplateModel("checkgroup", "Check group", IRepository.FIELDS, IRuntimeChecks.class));
		specTemplateList.add(new SpecTemplateModel("combobox", "Combobox ", IRepository.FIELDS, IRuntimeCombobox.class));
		specTemplateList.add(new SpecTemplateModel("label", "label", IRepository.GRAPHICALCOMPONENTS, IScriptScriptLabelMethods.class));
		specTemplateList.add(new SpecTemplateModel("radiogroup", "Radio group", IRepository.FIELDS, IRuntimeRadio.class));
		specTemplateList.add(new SpecTemplateModel("textfield", "Text field", IRepository.FIELDS, IRuntimeTextField.class));
		specTemplateList.add(new SpecTemplateModel("typeahead", "TypeAhead ", IRepository.FIELDS, IRuntimeTextField.class));
		specTemplateList.add(new SpecTemplateModel("tabpanel", "Tab panel", IRepository.TABPANELS, com.servoy.j2db.ui.IScriptTabPanelMethods.class));
		specTemplateList.add(new SpecTemplateModel("password", "Password field", IRepository.FIELDS, IRuntimePassword.class));
		specTemplateList.add(new SpecTemplateModel("htmlarea", "Html Area", IRepository.FIELDS, IRuntimeHtmlArea.class));
		specTemplateList.add(new SpecTemplateModel("textarea", "Text Area", IRepository.FIELDS, IRuntimeTextArea.class));
		specTemplateList.add(new SpecTemplateModel("check", "Check", IRepository.FIELDS, IRuntimeCheck.class));
		specTemplateList.add(new SpecTemplateModel("radio", "Radio", IRepository.FIELDS, IRuntimeRadio.class));
		specTemplateList.add(new SpecTemplateModel("imagemedia", "Image Media", IRepository.FIELDS, IRuntimeImageMedia.class));
		specTemplateList.add(new SpecTemplateModel("splitpane", "Split Pane", IRepository.FIELDS, com.servoy.j2db.ui.IScriptTabPanelMethods.class));
		specTemplateList.add(new SpecTemplateModel("portal", "Portal", IRepository.FIELDS, com.servoy.j2db.ui.IScriptPortalComponentMethods.class));
		specTemplateList.add(new SpecTemplateModel("accordionpanel", "AccordionPanel", IRepository.FIELDS,
			com.servoy.j2db.ui.IScriptPortalComponentMethods.class));
		specTemplateList.add(new SpecTemplateModel("spinner", "Spinner", IRepository.FIELDS, com.servoy.j2db.ui.IScriptPortalComponentMethods.class));

		//specTemplateList.add(new SpecTemplateModel("navigator","Navigator", IRepository.FIELDS));
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
				String runtimeComponentExpression = "/servoydoc/runtime/object[@qualifiedName='" + specModel.getApiInterface().getName() +
					"']/functions/function";
				//read function list  nodes using xpath
				NodeList functionList = (NodeList)xPath.compile(runtimeComponentExpression).evaluate(document, XPathConstants.NODESET);
				for (int i = 0; i < functionList.getLength(); i++)
				{
					Node function = functionList.item(i);
					if (function.getAttributes().getNamedItem("clientSupport").getTextContent().contains("wc"))
					{
						Node returnNode = (Node)returnTypeExpr.evaluate(function, XPathConstants.NODE);
						String returnType = specSpecTypeFromDoc(returnNode.getAttributes().getNamedItem("typecode").getTextContent());
						String functionName = function.getAttributes().getNamedItem("name").getTextContent();
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
						if (addNewApi) specModel.getApis().add(new ApiMethod(functionName, returnType, parameterNames, parameterTypes, optionalParams));
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	void readModelAndHandlers(List<SpecTemplateModel> specTemplateList)
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
					handlers.add(element);
				}
				else if (isAllowedProperty(componentSpec.getName(), element.getName()) && getSpecTypeFromRepoType(element) != null)
				{
					model.add(element);
				}
			}
			componentSpec.setModel(model);
			componentSpec.setHandlers(handlers);
		}
	}

	//@formatter:off
	private static final IntHashMap<String> repoTypeMapping = new IntHashMap<String>();
	private static final Map<String, String> repoTypeMappingExceptions = new HashMap<String, String>();
	private static final List<String> internalProperties = new ArrayList<>();
	private static final Map<String, List<String>> perComponentExceptions = new HashMap<>();
	static
	{
		// general type mappings
		repoTypeMapping.put(IRepository.BOOLEAN, "boolean");
		repoTypeMapping.put(IRepository.STRING, "string");
		repoTypeMapping.put(IRepository.BORDER, "border");
		repoTypeMapping.put(IRepository.TABS, "tabs[]");
		repoTypeMapping.put(IRepository.COLOR, "color");
		repoTypeMapping.put(IRepository.INTEGER, "int");
		repoTypeMapping.put(IRepository.FONT, "font");
		repoTypeMapping.put(IRepository.POINT, "point");
		repoTypeMapping.put(IRepository.DIMENSION, "dimension");
		repoTypeMapping.put(IRepository.INSETS, "dimension");
		repoTypeMapping.put(IRepositoryConstants.MEDIA, "media");

		//speciffic repository element mapping
		repoTypeMappingExceptions.put("dataProviderID",
			"{ 'type':'dataprovider', 'ondatachange': { 'onchange':'onDataChangeMethodID', 'callback':'onDataChangeCallback'}}");
		repoTypeMappingExceptions.put("format", "{for:'dataProviderID' , type:'format'}");
		repoTypeMappingExceptions.put("text", "tagstring");
		repoTypeMappingExceptions.put("placeholderText", "tagstring");
		repoTypeMappingExceptions.put("toolTipText", "tagstring");
		repoTypeMappingExceptions.put("valuelistID", "{ type: 'valuelist', for: 'dataProviderID'}");
		repoTypeMappingExceptions.put("rolloverImageMediaID", "media");
		repoTypeMappingExceptions.put("imageMediaID", "media");
		repoTypeMappingExceptions.put("horizontalAlignment", "{type:'number', values:[{DEFAULT:-1}, {LEFT:0}, {CENTER:2},{RIGHT:4}]}");
		repoTypeMappingExceptions.put("verticalAlignment", "{type:'number', values:[{DEFAULT:-1}, {TOP:1}, {CENTER:2} ,{BOTTOM:3}]}");
		repoTypeMappingExceptions.put("textRotation", "{type:'number', values:[0,90,180,270]}");
		repoTypeMappingExceptions.put("tabSeq", "tabseq");
		repoTypeMappingExceptions.put("mediaOptions", "mediaoptions");
		repoTypeMappingExceptions.put("labelFor", "bean");

		//internal properties (properties that should not be generated for any component)
		internalProperties.add("extendsID");
		internalProperties.add("anchors");
		internalProperties.add("name");
		internalProperties.add("formIndex");
		internalProperties.add("displaysTags");
		internalProperties.add("groupID");
		internalProperties.add("locked");
		internalProperties.add("printSliding");
		internalProperties.add("rotation");
		internalProperties.add("customProperties");
		internalProperties.add("printSliding");
		internalProperties.add("formIndex");
		internalProperties.add("labelFor");
		internalProperties.add("displayType");


		// per component exceptions to internal properties (for ex labelfor should be only for datalabel)
		perComponentExceptions.put("label", new ArrayList<>(Arrays.asList("labelFor")));
	}

	// @formatter:on
	public static boolean isAllowedProperty(String componentName, String propName)
	{
		if (perComponentExceptions.get(componentName) != null)
		{
			if (perComponentExceptions.get(componentName).contains(propName))
			{
				return true;
			}
		}
		if (internalProperties.contains(propName)) return false;
		return true;
	}

	public static String getSpecTypeFromRepoType(Element element)
	{
		String ret = null;
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
