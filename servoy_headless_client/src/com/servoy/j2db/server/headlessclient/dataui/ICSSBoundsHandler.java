package com.servoy.j2db.server.headlessclient.dataui;

import java.awt.Insets;
import java.awt.Point;

import com.servoy.j2db.persistence.ISupportBounds;

interface ICSSBoundsHandler
{
	public void applyBounds(ISupportBounds component, TextualStyle styleObj, Insets ins, int partStartY, int partEndY, int partWidth,
		boolean enableAnchoring, Point locationModifier);
}
