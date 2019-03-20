/*******************************************************************************
 * Copyright (c) 2010-2012, Abel Hegedus, Mark Czotter, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.runtime.api;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.viatra.query.runtime.api.impl.BaseQueryGroup;
import org.eclipse.viatra.query.runtime.registry.IQuerySpecificationRegistry;
import org.eclipse.viatra.query.runtime.registry.IQuerySpecificationRegistryEntry;
import org.eclipse.viatra.query.runtime.registry.IQuerySpecificationRegistryChangeListener;
import org.eclipse.viatra.query.runtime.registry.IRegistryView;
import org.eclipse.viatra.query.runtime.registry.IRegistryViewFilter;
import org.eclipse.viatra.query.runtime.registry.QuerySpecificationRegistry;

/**
 * Package based {@link BaseQueryGroup} implementation. It handles patterns as a group within the same package.
 * 
 * @author Abel Hegedus, Mark Czotter
 * 
 */
public class PackageBasedQueryGroup extends BaseQueryGroup {

    private final Set<IQuerySpecification<?>> querySpecifications = new HashSet<>();
    private final String packageName;
    private final boolean includeSubPackages;
    private IRegistryView view;

    /**
     * Creates a query group with specifications of a given package from the {@link QuerySpecificationRegistry}. Only
     * query specifications with the exact package fully qualified name are included.
     * 
     * @param packageName
     *            that contains the specifications
     */
    public PackageBasedQueryGroup(String packageName) {
        this(packageName, false);
    }

    /**
     * Creates a query group with specifications of a given package from the {@link QuerySpecificationRegistry}.
     * 
     * @param packageName
     *            that contain the specifications
     * @param includeSubPackages
     *            if true all query specifications with package names starting with the given package are included
     */
    public PackageBasedQueryGroup(String packageName, boolean includeSubPackages) {
        super();
        this.packageName = packageName;
        this.includeSubPackages = includeSubPackages;
        IQuerySpecificationRegistry registry = QuerySpecificationRegistry.getInstance();
        view = registry.createView(new PackageNameBasedViewFilter());
        for (IQuerySpecificationRegistryEntry entry : view.getEntries()) {
            this.querySpecifications.add(entry.get());
        }
        SpecificationSetUpdater listener = new SpecificationSetUpdater();
        view.addViewListener(listener);
    }

    @Override
    public Set<IQuerySpecification<?>> getSpecifications() {
        return Collections.unmodifiableSet(new HashSet<>(querySpecifications));
    }

    public String getPackageName() {
        return packageName;
    }

    public boolean isIncludeSubPackages() {
        return includeSubPackages;
    }

    /**
     * Refreshes the pattern group from the query specification registry based on the parameters used during the
     * initialization.
     */
    public void refresh() {
        // do nothing, view is automatically refreshed
    }

    /**
     * Listener to update the specification set
     * 
     * @author Abel Hegedus
     *
     */
    private final class SpecificationSetUpdater implements IQuerySpecificationRegistryChangeListener {
        @Override
        public void entryAdded(IQuerySpecificationRegistryEntry entry) {
            querySpecifications.add(entry.get());
        }
    
        @Override
        public void entryRemoved(IQuerySpecificationRegistryEntry entry) {
            querySpecifications.remove(entry.get());
        }
    }

    /**
     * Registry view filter that checks FQNs against the given package name.
     * 
     * @author Abel Hegedus
     *
     */
    private final class PackageNameBasedViewFilter implements IRegistryViewFilter {
        @Override
        public boolean isEntryRelevant(IQuerySpecificationRegistryEntry entry) {
            String fqn = entry.getFullyQualifiedName();
            if (packageName.length() + 1 < fqn.length()) {
                if (includeSubPackages) {
                    if (fqn.startsWith(packageName + '.')) {
                        return true;
                    }
                } else {
                    String name = fqn.substring(fqn.lastIndexOf('.') + 1, fqn.length());
                    if (fqn.equals(packageName + '.' + name)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

}
