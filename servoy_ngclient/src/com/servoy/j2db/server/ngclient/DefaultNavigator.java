package com.servoy.j2db.server.ngclient;

import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.util.UUID;

@SuppressWarnings("nls")
public class DefaultNavigator extends Bean
{

	private static final long serialVersionUID = 1L;

	public final static String BEAN_CLASSNAME = "servoycore-navigator";
	public final static String NAME_PROP_VALUE = "svy_default_navigator";
	public final static String MAXINDEX_PROP = "maxIndex";
	public final static String MININDEX_PROP = "minIndex";
	public final static String CURRENTINDEX_PROP = "currentIndex";
	public final static String FOUNDSET_HAS_MORE_ELEMENTS = "hasMore";
	public final static String SETELECTEDINDEX_FUNCTION_NAME = "setSelectedIndex";

	public final static DefaultNavigator INSTANCE = new DefaultNavigator();

	private DefaultNavigator()
	{
		super(null, 0, null);
		setBeanXML(
			"{" + MAXINDEX_PROP + ":0," + MININDEX_PROP + ":0," + CURRENTINDEX_PROP + ":0," + SETELECTEDINDEX_FUNCTION_NAME + ":'" + UUID.randomUUID() + "'}");
		setName(NAME_PROP_VALUE);
		setBeanClassName(BEAN_CLASSNAME);
	}
}