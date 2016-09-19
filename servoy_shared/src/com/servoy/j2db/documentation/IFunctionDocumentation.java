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

package com.servoy.j2db.documentation;

import java.util.LinkedHashMap;
import java.util.List;

import org.dom4j.Element;

import com.servoy.base.util.ITagResolver;

/**
 * Function documentation interface.
 */
public interface IFunctionDocumentation
{
	// type codes
	public static final Integer TYPE_UNKNOWN = new Integer(0);
	public static final Integer TYPE_FUNCTION = new Integer(1);
	public static final Integer TYPE_PROPERTY = new Integer(2);
	public static final Integer TYPE_CONSTANT = new Integer(3);
	public static final Integer TYPE_EVENT = new Integer(4);
	public static final Integer TYPE_COMMAND = new Integer(5);
	public static final Integer TYPE_CONSTRUCTOR = new Integer(6);

	// states
	public static final int STATE_DOCUMENTED = 0;
	public static final int STATE_UNDOCUMENTED = 1;
	public static final int STATE_INEXISTENT = 2;

	public Integer getType();

	Class< ? > getReturnedType();

	public boolean isSpecial();

	public int getState();

	public List<IDescriptionDocumentation> getDescriptions();

	public void setDescriptions(List<IDescriptionDocumentation> description);

	public void addDescription(IDescriptionDocumentation description);

	public void addDescription(ClientSupport csp, String text);

	public String getDescription(ClientSupport csp);

	public void addSummary(IDescriptionDocumentation summary);

	public String getSummary(ClientSupport csp);

	public String getMainName();

	public Class< ? >[] getArgumentsTypes();

	public void addSample(ISampleDocumentation sample);

	public String getSample(ClientSupport csp);

	public List<ISampleDocumentation> getSamples();

	public void setSamples(List<ISampleDocumentation> samples);

	public String getFullSignature();

	public String getFullSignature(boolean withNames, boolean withTypes);

	public String getFullJSTranslatedSignature(boolean withNames, boolean withTypes);

	public String getSignature(String prefix);

	public boolean answersTo(String name);

	public boolean answersTo(String name, int argCount);

	public boolean answersTo(String name, Class< ? >[] argsTypes);

	public boolean answersTo(String name, String[] argsTypes);

	public LinkedHashMap<String, IParameterDocumentation> getArguments();

	public boolean isDeprecated();

	public String getDeprecatedText();

	public boolean isVarargs();

	public void addArgument(IParameterDocumentation argDoc);

	public void runResolver(ITagResolver resolver);

	public void setBeingSolved(boolean beingSolved);

	public boolean isBeingSolved();

	public boolean needsRedirect();

	public int getRedirectType();

	public QualifiedDocumentationName getRedirect();

	public Element toXML(boolean hideSample, boolean pretty);

	public QualifiedDocumentationName getCloneDescRedirect();

	public boolean isDocumented();

	public String getReturnDescription();

	public String getSince();

	public String getUntil();

	public ClientSupport getClientSupport();
}
