/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2022 Servoy BV

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

package com.servoy.j2db.server.ngclient.property;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.awt.Point;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.sablo.InMemPackageReader;

import com.servoy.eclipse.model.DeveloperPersistIndex;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.util.ElementUtil;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.FlattenedForm;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.ServoyException;

/**
 * Testing if the list of uuids is created correctly for override layouts.
 * @author emera
 */
public class Inheritance_UUIDSList_Test extends AbstractSolutionTest
{

	private WebComponent buttonA;
	private WebComponent buttonB;
	private WebComponent buttonC;

	@Override
	protected InMemPackageReader getTestComponents() throws IOException
	{
		InputStream is = getClass().getResourceAsStream("inheritance.manifest");
		byte[] bytes = new byte[is.available()];
		is.read(bytes);
		String manifest = new String(bytes);
		is.close();

		is = getClass().getResourceAsStream("inheritance_button.spec");
		bytes = new byte[is.available()];
		is.read(bytes);
		String comp1 = new String(bytes);
		is.close();

		HashMap<String, String> components = new HashMap<>();
		components.put("inheritance-button", comp1);
		InMemPackageReader inMemPackageReader = new InMemPackageReader(manifest, components);
		return inMemPackageReader;
	}

	@Override
	protected void fillTestSolution() throws ServoyException
	{
		try
		{

			Form parent = solution.createNewForm(validator, null, "parent", null, false, null);
			parent.setResponsiveLayout(true);

			LayoutContainer row = parent.createNewLayoutContainer();
			row.setCssClasses("row");
			LayoutContainer column = row.createNewLayoutContainer();
			column.setCssClasses("column");
			buttonA = column.createNewWebComponent("buttonA", "button");
			buttonA.setProperty("text", "Button A");
			buttonB = column.createNewWebComponent("buttonB", "button");
			buttonB.setProperty("text", "Button B");
			buttonC = column.createNewWebComponent("buttonC", "button");
			buttonC.setProperty("text", "Button C");

			buttonA.setLocation(new Point(1, 1));
			buttonB.setLocation(new Point(2, 2));
			buttonC.setLocation(new Point(3, 3));

			Form child = solution.createNewForm(validator, null, "child", null, false, null);
			child.setResponsiveLayout(true);
			child.setExtendsID(parent.getID());
		}
		catch (JSONException e)
		{
			e.printStackTrace();
			throw new ServoyException();
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.ngclient.property.AbstractSolutionTest#setupData()
	 */
	@Override
	protected void setupData() throws ServoyException
	{
		// TODO Auto-generated method stub

	}

	@Test
	public void testOverrideSingleComponent() throws JSONException, RepositoryException
	{
		FlattenedSolution fs = client.getFlattenedSolution();
		Assert.assertNotNull(fs);
		Form child = fs.getFlattenedForm(fs.getForm("child"));
		Assert.assertNotNull(child);
		//WebComponent buttonA = child.getWebComponents().next(); //TODO check if buttonA
		LayoutContainer column = (LayoutContainer)buttonA.getParent();

		WebComponent buttonA_ = (WebComponent)ElementUtil.getOverridePersist(PersistContext.create(fs.searchPersist(buttonA), child), fs);
		buttonA_.setProperty("text", "override A");
		assertEquals("Should set extends id for the component", buttonA.getID(), buttonA_.getExtendsID());
		assertEquals("Should not override the parent container (column)", column, buttonA_.getRealParent());
		//TODO assertEquals("Override property (text)", "override A", buttonA_.getProperty("text"));
		//TODO assertEquals("Parent text property should not change", "Button A", buttonA.getProperty("text"));

		assertNull("The form's list of uuids should be null", child.getSortedChildren());
	}

	@Test
	public void testInsertComponentsAndSync() throws JSONException, RepositoryException
	{
		FlattenedSolution fs = client.getFlattenedSolution();
		Form child = fs.getFlattenedForm(fs.getForm("child"));
		Assert.assertNotNull(child.getLayoutContainers());
		LayoutContainer column = child.getLayoutContainers().next().getLayoutContainers().next();
		Assert.assertFalse(column.getListeners().isPresent());

		LayoutContainer column_ = (LayoutContainer)ElementUtil.getOverridePersist(PersistContext.create(column, child), fs);
		assertEquals("Should set extends id for the column", column.getID(), column_.getExtendsID());
		assertEquals("Should not override the parent container (row)", column.getParent(), column_.getRealParent());
		assertNull("The form's list of uuids should be null", child.getSortedChildren());
		assertNull("The parent column's list of uuids should be null", column.getSortedChildren());
		CopyOnWriteArrayList<String> sortedChildren = column_.getSortedChildren();
		assertNotNull("The parent column's list of uuids should NOT be null", sortedChildren);
		assertEquals(buttonA.getUUID().toString(), sortedChildren.get(0));
		assertEquals(buttonB.getUUID().toString(), sortedChildren.get(1));
		assertEquals(buttonC.getUUID().toString(), sortedChildren.get(2));
		Assert.assertTrue(column.getListeners().isPresent());
		Assert.assertTrue(!column.getListeners().get().isEmpty());
		Assert.assertTrue("Override persist should listen to its parent", column.getListeners().get().contains(column_));

		//add to the end -> parent: A, B, C
		//					child:  A, B, C, Z
		WebComponent buttonZ = column_.createNewWebComponent("ButtonZ", "button");
		buttonZ.setLocation(new Point(4, 4));
		column_.addChild(buttonZ);
		sortedChildren = column_.getSortedChildren();
		assertEquals(4, sortedChildren.size());
		assertEquals(buttonA.getUUID().toString(), sortedChildren.get(0));
		assertEquals(buttonZ.getUUID().toString(), sortedChildren.get(3));

		//add X in the beginning -> parent:   A, B, C
		//					        child: X, A, B, C, Z
		WebComponent buttonX = column_.createNewWebComponent("ButtonX", "button");
		column_.addChild(buttonX);
		column_.insertBeforeUUID(buttonX.getUUID(), buttonA);
		sortedChildren = column_.getSortedChildren();
		assertEquals(5, sortedChildren.size());
		assertEquals(buttonX.getUUID().toString(), sortedChildren.get(0));
		assertEquals(buttonA.getUUID().toString(), sortedChildren.get(1));

		//override B -> parent:    A, B,  C
		//			    child:  X, A, B_, C, Z
		WebComponent buttonB_ = (WebComponent)ElementUtil.getOverridePersist(PersistContext.create(fs.searchPersist(buttonB), child), fs);
		buttonB_.setProperty("text", "override B");
		sortedChildren = column_.getSortedChildren();
		assertEquals("uuids list size should not change on override", 5, sortedChildren.size());
		assertTrue(!sortedChildren.contains(buttonB.getUUID().toString()));
		assertEquals(buttonB_.getUUID().toString(), sortedChildren.get(2));

		//add Y between A and B_ -> parent:    A,    B,  C
		//			                child:  X, A, Y, B_, C, Z
		WebComponent buttonY = column_.createNewWebComponent("ButtonY", "button");
		column_.addChild(buttonY);
		column_.insertBeforeUUID(buttonY.getUUID(), buttonB_);
		sortedChildren = column_.getSortedChildren();
		assertEquals(6, sortedChildren.size());
		assertEquals(buttonY.getUUID().toString(), sortedChildren.get(2));
		assertEquals(buttonB_.getUUID().toString(), sortedChildren.get(3));

		//add V between B_ and C -> parent:    A,    B,     C
		//			                child:  X, A, Y, B_, V, C, Z
		assertEquals(sortedChildren.get(4), buttonC.getUUID().toString());
		WebComponent buttonV = column_.createNewWebComponent("ButtonV", "button");
		column_.addChild(buttonV);
		column_.insertBeforeUUID(buttonV.getUUID(), buttonC);
		sortedChildren = column_.getSortedChildren();
		assertEquals(7, sortedChildren.size());
		assertEquals(buttonV.getUUID().toString(), sortedChildren.get(4));
		assertEquals(buttonC.getUUID().toString(), sortedChildren.get(5));

		//insert D into parent before A -> parent:    D, A,    B,     C
		//			                       child:  X, D, A, Y, B_, V, C, Z
		WebComponent buttonD = column.createNewWebComponent("ButtonD", "button");
		buttonD.setLocation(new Point(1, 1));
		buttonA.setLocation(new Point(2, 2));
		buttonB.setLocation(new Point(3, 3));
		buttonC.setLocation(new Point(4, 4));
		column.addChild(buttonD);
		List<String> parentUUIDS = PersistHelper.getSortedChildren(column, (Form)column.getAncestor(IRepository.FORMS), fs);
		assertEquals(buttonD.getUUID().toString(), parentUUIDS.get(0));
		assertEquals(buttonA.getUUID().toString(), parentUUIDS.get(1));
		assertEquals(buttonB.getUUID().toString(), parentUUIDS.get(2));
		assertEquals(buttonC.getUUID().toString(), parentUUIDS.get(3));
		sortedChildren = column_.getSortedChildren();
		assertEquals(8, sortedChildren.size());
		assertEquals(buttonX.getUUID().toString(), sortedChildren.get(0));
		assertEquals(buttonD.getUUID().toString(), sortedChildren.get(1));
		assertEquals(buttonA.getUUID().toString(), sortedChildren.get(2));

		//insert E into parent before B -> parent:    D, A,    E, B,     C
		//			                       child:  X, D, A, Y, E, B_, V, C, Z
		assertEquals(sortedChildren.get(4), buttonB_.getUUID().toString());
		WebComponent buttonE = column.createNewWebComponent("ButtonE", "button");
		buttonE.setLocation(new Point(3, 3));
		buttonB.setLocation(new Point(4, 4));
		buttonC.setLocation(new Point(5, 5));
		column.addChild(buttonE);
		sortedChildren = column_.getSortedChildren();
		assertEquals(9, sortedChildren.size());
		assertEquals(buttonE.getUUID().toString(), sortedChildren.get(4));
		assertEquals(buttonB_.getUUID().toString(), sortedChildren.get(5));

		//override A -> parent:    D, A,     E, B,     C
		//			    child:  X, D, A_, Y, E, B_, V, C, Z
		WebComponent buttonA_ = (WebComponent)ElementUtil.getOverridePersist(PersistContext.create(fs.searchPersist(buttonA), child), fs);
		buttonA_.setProperty("text", "override A");
		sortedChildren = column_.getSortedChildren();
		assertEquals("uuids list size should not change on override", 9, sortedChildren.size());
		assertTrue(!sortedChildren.contains(buttonA.getUUID().toString()));
		assertEquals(buttonA_.getUUID().toString(), sortedChildren.get(2));

		//test sync (remove listeners for a while)
		column.removeSuperListener(column_);
		Assert.assertFalse("Child column listener to parent should be disabled", column.getListeners().isPresent());

		//remove B, C from parent column
		//add alpha before A, delta before E             -> parent:    D, alpha, A,     E, delta
		//			                                        child:  X, D,        A_, Y, E,        B_, V, C, Z
		assertEquals(5, column.getAllObjectsAsList().size());
		column.removeChild(buttonB);
		column.removeChild(buttonC);

		sortedChildren = column_.getSortedChildren();
		assertEquals("uuids size should not change in child because listener is disabled", 9, sortedChildren.size());
		assertEquals(buttonB_.getUUID().toString(), sortedChildren.get(5));
		assertEquals(buttonC.getUUID().toString(), sortedChildren.get(7));
		//TODO force the flattened column_ to still have B and C? check what happens in the real scenario

		WebComponent buttonAlpha = column.createNewWebComponent("Button alpha", "button");
		buttonAlpha.setLocation(new Point(2, 2));
		buttonA.setLocation(new Point(3, 3));
		buttonE.setLocation(new Point(4, 4));
		column.addChild(buttonAlpha);
		WebComponent buttonDelta = column.createNewWebComponent("Button delta", "button");
		buttonDelta.setLocation(new Point(5, 5));
		column.addChild(buttonDelta);
		assertEquals(5, column.getAllObjectsAsList().size());

		sortedChildren = column_.getSortedChildren();
		assertTrue(sortedChildren.contains(buttonB_.getUUID().toString()));
		assertTrue(sortedChildren.contains(buttonC.getUUID().toString()));
		assertFalse(sortedChildren.contains(buttonAlpha.getUUID().toString()));
		assertFalse(sortedChildren.contains(buttonDelta.getUUID().toString()));
		//                    parent:    D, alpha, A,     E, delta
		//			SYNC->    child:  X, D, alpha, A_, Y, E, delta, V, Z
		DeveloperPersistIndex.syncChildrenUUIDS(fs, column, column_);
		sortedChildren = column_.getSortedChildren();
		assertTrue(sortedChildren.contains(buttonA_.getUUID().toString()));
		assertEquals(9, sortedChildren.size());
		assertFalse(sortedChildren.contains(buttonB_.getUUID().toString()));
		assertFalse(sortedChildren.contains(buttonC.getUUID().toString()));
		assertEquals(buttonAlpha.getUUID().toString(), sortedChildren.get(2));
		assertEquals(buttonDelta.getUUID().toString(), sortedChildren.get(6));
	}

	@Test
	public void testReorderingTopContainers() throws JSONException, RepositoryException
	{
		FlattenedSolution fs = client.getFlattenedSolution();
		Form child = fs.getForm("child");
		FlattenedForm flattened_child = (FlattenedForm)fs.getFlattenedForm(child);
		Form parent = fs.getFlattenedForm(fs.getForm("parent"));
		Assert.assertNotNull(flattened_child.getLayoutContainers());
		Assert.assertFalse(parent.getListeners().isPresent());
		LayoutContainer row = (flattened_child.getLayoutContainers().next());

		//add new row after the existing one
		LayoutContainer row_2 = child.createNewLayoutContainer();
		row_2.setLocation(new Point(20, 20));
		child.addChild(row_2);
		flattened_child.reload();
		List<String> containers = PersistHelper.getSortedChildren(flattened_child, child, fs);
		assertEquals(2, containers.size());
		assertEquals(row.getUUID().toString(), containers.get(0));
		assertEquals(row_2.getUUID().toString(), containers.get(1));
		assertNull("nothing is overridden yet, should not have uuids", flattened_child.getSortedChildren());
		assertFalse("nothing is overridden, should not listen", flattened_child.getListeners().isPresent());

		assertEquals(2, flattened_child.getAllObjectsAsList().size());
		assertEquals(1, child.getAllObjectsAsList().size());
		//add row before (need to override row from the parent)
		assertEquals(20, ((LayoutContainer)child.getAllObjectsAsList().get(0)).getLocation().x);
		LayoutContainer row_ = (LayoutContainer)ElementUtil.getOverridePersist(PersistContext.create(row, child), fs);

		CopyOnWriteArrayList<String> sortedChildren = flattened_child.getSortedChildren();
		assertEquals(2, flattened_child.getAllObjectsAsList().size());
		assertNull("parent should not have uuids list", parent.getSortedChildren());
		row_.setLocation(new Point(20, 20));//override the row of the parent because we need to change the location property
		row_2.setLocation(new Point(30, 30));
		LayoutContainer row_0 = child.createNewLayoutContainer();
		row_0.setLocation(new Point(11, 11));
		child.addChild(row_0);
		flattened_child.reload();

		assertEquals(3, flattened_child.getAllObjectsAsList().size());
		assertEquals("should contain the overriden and its own children", 3, child.getAllObjectsAsList().size());
		assertEquals(3, child.getSortedChildren().size());

		sortedChildren = flattened_child.getSortedChildren();
		assertNotNull(sortedChildren);
		assertTrue(parent.getListeners().isPresent());
		assertTrue(parent.getListeners().get().contains(child));
		assertTrue(row.getListeners().isPresent());
		assertTrue(row.getListeners().get().contains(row_));

		assertEquals(3, flattened_child.getAllObjectsAsList().size());
		assertEquals(3, sortedChildren.size());
		assertTrue(sortedChildren.contains(row_.getUUID().toString()));
		assertTrue(sortedChildren.contains(row_0.getUUID().toString()));
		assertTrue(sortedChildren.contains(row_2.getUUID().toString()));
		assertFalse(sortedChildren.contains(row.getUUID().toString()));
		assertEquals(row_0.getUUID().toString(), sortedChildren.get(0));
		assertEquals(row_.getUUID().toString(), sortedChildren.get(1));
		assertEquals(row_2.getUUID().toString(), sortedChildren.get(2));
	}

	//TODO test for listeners (e.g. overriden is listener instead of parent, removed is not listener)
	//TODO test remove own, overridden (remove of inherited as is from the parent should not be allowed)

	@Test
	public void test3FormsHierarchy() throws JSONException, RepositoryException
	{
		FlattenedSolution fs = client.getFlattenedSolution();
		Form child = fs.getFlattenedForm(fs.getForm("child"));

		Form child2 = solution.createNewForm(validator, null, "child2", null, false, null);
		child2.setResponsiveLayout(true);
		child2.setExtendsID(child.getID());

		FlattenedForm flattened_child2 = (FlattenedForm)fs.getFlattenedForm(child2);
		assertNotNull(flattened_child2.getLayoutContainers());
		assertFalse(child.getListeners().isPresent());

		Form parent = fs.getFlattenedForm(fs.getForm("parent"));
		assertFalse(parent.getListeners().isPresent());
		LayoutContainer row = flattened_child2.getLayoutContainers().next();

		LayoutContainer row_2 = child2.createNewLayoutContainer();
		row_2.setLocation(new Point(20, 20));
		child2.addChild(row_2);
		flattened_child2.reload();
		List<String> containers = PersistHelper.getSortedChildren(flattened_child2, child2, fs);
		assertEquals(2, containers.size());
		assertEquals(row.getUUID().toString(), containers.get(0));
		assertEquals(row_2.getUUID().toString(), containers.get(1));
		assertNull("nothing is overridden yet, should not have uuids", flattened_child2.getSortedChildren());
		assertFalse("nothing is overridden, should not listen", flattened_child2.getListeners().isPresent());
		assertFalse(parent.getListeners().isPresent());
		assertFalse(child.getListeners().isPresent());

		LayoutContainer row_ = (LayoutContainer)ElementUtil.getOverridePersist(PersistContext.create(row, child2), fs);

		assertEquals(2, flattened_child2.getAllObjectsAsList().size());
		row_.setLocation(new Point(20, 20));
		row_2.setLocation(new Point(30, 30));
		LayoutContainer row_0 = child2.createNewLayoutContainer();
		row_0.setLocation(new Point(11, 11));
		child2.addChild(row_0);
		flattened_child2.reload();
		//assertTrue(parent.getListeners().isPresent());
		assertTrue(child.getListeners().isPresent());
		assertEquals(3, flattened_child2.getAllObjectsAsList().size());
		assertNotNull(child2.getSortedChildren());
		assertEquals(3, child2.getSortedChildren().size());
	}
}