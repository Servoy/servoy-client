package com.servoy.j2db.scripting.api.ui;

import com.servoy.j2db.scripting.annotations.ServoyMobile;

@ServoyMobile
public interface HasRuntimeEnabled
{
	public boolean isEnabled();

	public void setEnabled(boolean b);
}
