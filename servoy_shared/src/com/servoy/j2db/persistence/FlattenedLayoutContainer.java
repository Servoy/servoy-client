package com.servoy.j2db.persistence;

import java.awt.Point;
import java.util.List;
import java.util.Map;

import com.servoy.j2db.persistence.StaticContentSpecLoader.TypedProperty;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.UUID;

public class FlattenedLayoutContainer extends LayoutContainer implements IFlattenedPersistWrapper<LayoutContainer>
{

	private static final long serialVersionUID = 1L;

	private final LayoutContainer layoutContainer;
	private final FlattenedForm flattenedForm;

	public FlattenedLayoutContainer(FlattenedForm flattenedForm, LayoutContainer layoutContainer)
	{
		super(layoutContainer.getParent(), layoutContainer.getID(), layoutContainer.getUUID());
		this.layoutContainer = layoutContainer;
		this.flattenedForm = flattenedForm;
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
		getAncestor(IRepository.FORMS);
		List<IPersist> children = PersistHelper.getHierarchyChildren(layoutContainer);
		Map<UUID, IPersist> extendsMap = flattenedForm.getExtendsMap();
		for (IPersist child : children)
		{
			if (child instanceof LayoutContainer)
			{
				internalAddChild(new FlattenedLayoutContainer(flattenedForm, (LayoutContainer)getOverridePersist(child, extendsMap)));
			}
			else
			{
				internalAddChild(getOverridePersist(child, extendsMap));
			}
		}
	}

	private IPersist getOverridePersist(IPersist child, Map<UUID, IPersist> extendsMap)
	{
		if (extendsMap.containsKey(child.getUUID()))
		{
			return getOverridePersist(extendsMap.get(child.getUUID()), extendsMap);
		}
		return child;
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
		if (obj != null && obj.getParent() == this)
		{
			layoutContainer.internalRemoveChild(obj);
		}
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
