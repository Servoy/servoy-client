package com.servoy.j2db.persistence;

import java.util.Map;

/**
 * Interface for entities that support the attributes property.
 * @author emera
 */
public interface ISupportAttributes
{
	public Map<String, String> getAttributes();

	public void putAttributes(Map<String, String> value);
}
