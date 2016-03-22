package com.servoy.j2db.persistence;

import java.awt.Point;
import java.util.List;

import com.servoy.j2db.persistence.StaticContentSpecLoader.TypedProperty;
import com.servoy.j2db.util.PersistHelper;

public class FlattenedLayoutContainer extends LayoutContainer implements IFlattenedPersistWrapper<LayoutContainer>
{

	private static final long serialVersionUID = 1L;

	private final LayoutContainer layoutContainer;

	public FlattenedLayoutContainer(LayoutContainer layoutContainer)
	{
		super(layoutContainer.getParent(), layoutContainer.getID(), layoutContainer.getUUID());
		this.layoutContainer = layoutContainer;
		fill();
	}

	@Override
	public LayoutContainer getWrappedPersist()
	{
		return layoutContainer;
	}

	private void fill()
	{
		internalClearAllObjects();
		List<IPersist> children = PersistHelper.getHierarchyChildren(layoutContainer);
		for (IPersist child : children)
		{
			if (child instanceof LayoutContainer && ((LayoutContainer)child).getExtendsID() > 0)
			{
				internalAddChild(new FlattenedLayoutContainer((LayoutContainer)child));
			}
			else
			{
				internalAddChild(child);
			}
		}
	}

	@Override
	protected void internalRemoveChild(IPersist obj)
	{
		layoutContainer.internalRemoveChild(obj);
		fill();
	}

	@Override
	public void addChild(IPersist obj)
	{
		layoutContainer.addChild(obj);
		fill();
	}

	@Override
	public Field createNewField(Point location) throws RepositoryException
	{
		return layoutContainer.createNewField(location);
	}

	@Override
	public GraphicalComponent createNewGraphicalComponent(Point location) throws RepositoryException
	{
		return layoutContainer.createNewGraphicalComponent(location);
	}

	@Override
	<T> T getTypedProperty(TypedProperty<T> property)
	{
		return layoutContainer.getTypedProperty(property);
	}

	@Override
	<T> void setTypedProperty(TypedProperty<T> property, T value)
	{
		layoutContainer.setTypedProperty(property, value);
	}

	@Override
	public int hashCode()
	{
		return layoutContainer.hashCode();
	}

	@Override
	public List<IPersist> getHierarchyChildren()
	{
		return getAllObjectsAsList();
	}
}
