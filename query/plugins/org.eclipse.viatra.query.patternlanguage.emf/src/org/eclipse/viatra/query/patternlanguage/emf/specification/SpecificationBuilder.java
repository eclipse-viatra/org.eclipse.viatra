/*******************************************************************************
 * Copyright (c) 2010-2014, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Zoltan Ujhelyi - initial API and implementation
 *******************************************************************************/
package org.eclipse.viatra.query.patternlanguage.emf.specification;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.viatra.query.patternlanguage.emf.helper.PatternLanguageHelper;
import org.eclipse.viatra.query.patternlanguage.emf.specification.internal.EPMToPBody;
import org.eclipse.viatra.query.patternlanguage.emf.specification.internal.NameToSpecificationMap;
import org.eclipse.viatra.query.patternlanguage.emf.specification.internal.PatternBodyTransformer;
import org.eclipse.viatra.query.patternlanguage.emf.specification.internal.PatternSanitizer;
import org.eclipse.viatra.query.patternlanguage.emf.vql.Annotation;
import org.eclipse.viatra.query.patternlanguage.emf.vql.Modifiers;
import org.eclipse.viatra.query.patternlanguage.emf.vql.Pattern;
import org.eclipse.viatra.query.patternlanguage.emf.vql.PatternBody;
import org.eclipse.viatra.query.runtime.api.IPatternMatch;
import org.eclipse.viatra.query.runtime.api.IQuerySpecification;
import org.eclipse.viatra.query.runtime.api.ViatraQueryMatcher;
import org.eclipse.viatra.query.runtime.localsearch.matcher.integration.LocalSearchBackendFactory;
import org.eclipse.viatra.query.runtime.matchers.ViatraQueryRuntimeException;
import org.eclipse.viatra.query.runtime.matchers.backend.IQueryBackendFactory;
import org.eclipse.viatra.query.runtime.matchers.backend.QueryEvaluationHint;
import org.eclipse.viatra.query.runtime.matchers.psystem.InitializablePQuery;
import org.eclipse.viatra.query.runtime.matchers.psystem.PBody;
import org.eclipse.viatra.query.runtime.matchers.psystem.annotations.PAnnotation;
import org.eclipse.viatra.query.runtime.matchers.psystem.queries.PProblem;
import org.eclipse.viatra.query.runtime.matchers.psystem.queries.PQuery;
import org.eclipse.viatra.query.runtime.matchers.psystem.queries.PQuery.PQueryStatus;
import org.eclipse.viatra.query.runtime.matchers.psystem.rewriters.RewriterException;
import org.eclipse.viatra.query.runtime.rete.matcher.ReteBackendFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;

/**
 * An instance class to initialize {@link PBody} instances from {@link Pattern} definitions. A single instance of this
 * builder is used during construction, that maintains the mapping between {@link Pattern} and {@link PQuery} objects,
 * and can be initialized with a pre-defined set of mappings.</p>
 *
 * <p>
 * The SpecificationBuilder is stateful: it stores all previously built specifications, allowing further re-use.
 *
 * @author Zoltan Ujhelyi
 * @since 2.0
 *
 */
public final class SpecificationBuilder {

    private NameToSpecificationMap patternMap;
    /**
     * This map is used to detect a re-addition of a pattern with a fqn that is used by a previously added pattern.
     */
    private Map<String, Pattern> patternNameMap = new HashMap<>();
    private Multimap<PQuery, IQuerySpecification<?>> dependantQueries = Multimaps.newSetMultimap(
            new HashMap<PQuery, Collection<IQuerySpecification<?>>>(), Sets::newHashSet);
    private PatternSanitizer sanitizer = new PatternSanitizer(/*logger*/ null /* do not log all errors */);

    /**
     * Initializes a query builder with no previously known query specifications
     */
    public SpecificationBuilder() {
        patternMap = new NameToSpecificationMap();
    }

    /**
     * Sets up a query builder with a predefined set of specifications
     */
    public SpecificationBuilder(IQuerySpecification<?>... specifications) {
        patternMap = new NameToSpecificationMap(specifications);
        processPatternSpecifications();
    }

    /**
     * Sets up a query builder with a predefined collection of specifications
     */
    public SpecificationBuilder(
            Collection<? extends IQuerySpecification<? extends ViatraQueryMatcher<? extends IPatternMatch>>> specifications) {
        patternMap = new NameToSpecificationMap(specifications);
        processPatternSpecifications();
    }

    public SpecificationBuilder(NameToSpecificationMap patternMap) {
        this.patternMap = patternMap;
        processPatternSpecifications();
    }

    /**
     * Processes all existing query specifications searching for possible pattern instances, and if found, add it to the
     * {@link #patternNameMap}.
     */
    private void processPatternSpecifications() {
        for (GenericQuerySpecification spec : Iterables.filter(patternMap.values(), GenericQuerySpecification.class)) {
            patternNameMap.put(spec.getFullyQualifiedName(), spec.getInternalQueryRepresentation().getPattern());
        }
    }

    /**
     * Creates a new or returns an existing query specification for the pattern. It is expected, that the builder will
     * not be called with different patterns having the same fqn over its entire lifecycle.
     *
     * @param pattern
     * @throws ViatraQueryRuntimeException
     * @since 2.0
     */
    public IQuerySpecification<? extends ViatraQueryMatcher<? extends IPatternMatch>> getOrCreateSpecification(
            Pattern pattern) {
        return getOrCreateSpecification(pattern, false);
    }

    /**
     * Creates a new or returns an existing query specification for the pattern. It is expected, that the builder will
     * not be called with different patterns having the same fqn over its entire lifecycle.
     *
     * @param pattern
     * @param skipPatternValidation
     *            if set to true, detailed pattern validation is skipped - true for model inferrer; not recommended for
     *            generic API
     * @throws ViatraQueryRuntimeException
     * @since 2.0
     */
    public IQuerySpecification<? extends ViatraQueryMatcher<? extends IPatternMatch>> getOrCreateSpecification(
            Pattern pattern, boolean skipPatternValidation) {
        return getOrCreateSpecification(pattern, Lists.<IQuerySpecification<?>>newArrayList(), skipPatternValidation);
    }

    /**
     * @since 2.0
     */
    public IQuerySpecification<? extends ViatraQueryMatcher<? extends IPatternMatch>> getOrCreateSpecification(
            Pattern pattern, List<IQuerySpecification<?>> createdPatternList, boolean skipPatternValidation) {
        Preconditions.checkArgument(pattern != null && !pattern.eIsProxy(), "Cannot create specification from a null pattern");
        String fqn = PatternLanguageHelper.getFullyQualifiedName(pattern);
        Preconditions.checkArgument(fqn != null && !"".equals(fqn), "Pattern name cannot be empty");
        Preconditions.checkArgument(!patternNameMap.containsKey(fqn) || pattern.equals(patternNameMap.get(fqn)),
                "This builder already contains a different pattern with the fqn %s of the newly added pattern.", fqn);
        IQuerySpecification<?> specification = getSpecification(pattern);
        if (specification == null) {
            specification = buildSpecification(pattern, skipPatternValidation, createdPatternList);
        }
        return specification;
    }

    protected IQuerySpecification<?> buildSpecification(Pattern pattern) {
        return buildSpecification(pattern, false, Lists.<IQuerySpecification<?>>newArrayList());
    }

    protected IQuerySpecification<?> buildSpecification(Pattern pattern, List<IQuerySpecification<?>> newSpecifications) {
        return buildSpecification(pattern, false, newSpecifications);
    }

    protected IQuerySpecification<?> buildSpecification(Pattern pattern, boolean skipPatternValidation, List<IQuerySpecification<?>> newSpecifications) {
        String fqn = PatternLanguageHelper.getFullyQualifiedName(pattern);
        Preconditions.checkArgument(!patternMap.containsKey(fqn), "Builder already stores query with the name of %s",
                fqn);
        if (sanitizer.admit(pattern, skipPatternValidation)) {
            Set<Pattern> newPatterns = Sets.newHashSet(Sets.filter(sanitizer.getAdmittedPatterns(),
                    (Predicate<Pattern>) pattern1 -> {
                        final String name = PatternLanguageHelper.getFullyQualifiedName(pattern1);
                        return !pattern1.eIsProxy() && !"".equals(name)
                               && !patternMap.containsKey(name);
                    }));
            // Initializing new query specifications
            for (Pattern newPattern : newPatterns) {
                String patternFqn = PatternLanguageHelper.getFullyQualifiedName(newPattern);
                GenericEMFPatternPQuery pquery = new GenericEMFPatternPQuery(newPattern, true);
                pquery.setEvaluationHints(buildHints(newPattern));
                GenericQuerySpecification specification = new GenericQuerySpecification(pquery);
                patternMap.put(patternFqn, specification);
                patternNameMap.put(patternFqn, newPattern);
                newSpecifications.add(specification);
            }
            // Updating bodies
            for (Pattern newPattern : newPatterns) {
                String patternFqn = PatternLanguageHelper.getFullyQualifiedName(newPattern);
                GenericQuerySpecification specification = (GenericQuerySpecification) patternMap.get(patternFqn);
                GenericEMFPatternPQuery pQuery = specification.getInternalQueryRepresentation();
                try {
                    buildAnnotations(newPattern, pQuery);
                    buildBodies(newPattern, pQuery);
                } catch (RewriterException e) {
                    pQuery.addError(new PProblem(e, e.getShortMessage()));
                }
                if (!PQueryStatus.ERROR.equals(pQuery.getStatus())) {
                    for (PQuery query : pQuery.getDirectReferredQueries()) {
                        dependantQueries.put(query, specification);
                    }
                }
            }
        } else {
            for (Pattern rejectedPattern : sanitizer.getRejectedPatterns()) {
                String patternFqn = PatternLanguageHelper.getFullyQualifiedName(rejectedPattern);
                if (!patternMap.containsKey(patternFqn)) {
                    GenericQuerySpecification rejected = new GenericQuerySpecification(new GenericEMFPatternPQuery(rejectedPattern, true));
                    for (PProblem problem: sanitizer.getProblemByPattern(rejectedPattern)) 
                        rejected.getInternalQueryRepresentation().addError(problem);
                    patternMap.put(patternFqn, rejected);
                    patternNameMap.put(patternFqn, rejectedPattern);
                    newSpecifications.add(rejected);
                }
            }
        }
        IQuerySpecification<?> specification = patternMap.get(fqn);
        if (specification == null) {
            GenericQuerySpecification erroneousSpecification = new GenericQuerySpecification(new GenericEMFPatternPQuery(pattern, true));
            erroneousSpecification.getInternalQueryRepresentation().addError( new PProblem("Unable to compile pattern due to an unspecified error") );
            patternMap.put(fqn, erroneousSpecification);
            patternNameMap.put(fqn, pattern);
            newSpecifications.add(erroneousSpecification);
            specification = erroneousSpecification;
        }
        return specification;
    }

    protected void buildAnnotations(Pattern pattern, InitializablePQuery query) {
        for (Annotation annotation : pattern.getAnnotations()) {
            PAnnotation pAnnotation = new PAnnotation(annotation.getName());
            for (Entry<String, Object> attribute : 
                PatternLanguageHelper.evaluateAnnotationParametersWithMultiplicity(annotation).entries()) 
            {
                pAnnotation.addAttribute(attribute.getKey(), attribute.getValue());
            }
            query.addAnnotation(pAnnotation);
        }
    }

    /**
     * @throws ViatraQueryRuntimeException
     * @since 2.0
     */
    public Set<PBody> buildBodies(Pattern pattern, InitializablePQuery query) {
        Set<PBody> bodies = getBodies(pattern, query);
        query.initializeBodies(bodies);
        return bodies;
    }

    /**
     * @throws ViatraQueryRuntimeException
     * @since 2.0
     */
    public Set<PBody> getBodies(Pattern pattern, PQuery query) {
        PatternBodyTransformer transformer = new PatternBodyTransformer(pattern);
        Set<PBody> pBodies = Sets.newLinkedHashSet();
        for (PatternBody body : pattern.getBodies()) {
            EPMToPBody acceptor = new EPMToPBody(pattern, query, patternMap);
            PBody pBody = transformer.transform(body, acceptor);
            pBodies.add(pBody);
        }
        return pBodies;
    }

    /**
     * @since 2.0
     */
    public IQuerySpecification<?> getSpecification(Pattern pattern) {
        String fqn = PatternLanguageHelper.getFullyQualifiedName(pattern);
        return getSpecification(fqn);
    }

    public IQuerySpecification<?> getSpecification(String fqn) {
        return patternMap.get(fqn);
    }

    /**
     * Forgets a specification in the builder. </p>
     * <p>
     * <strong>Warning!</strong> Removing a specification does not change any specification created previously, even if
     * they are referring to the old version of the specification. Only use this if you are sure all dependant queries
     * are also removed, otherwise use {@link #forgetSpecificationTransitively(IQuerySpecification)} instead.
     *
     */
    public void forgetSpecification(IQuerySpecification<?> specification) {
        String fqn = specification.getFullyQualifiedName();
        patternMap.remove(fqn);
        if (specification instanceof GenericQuerySpecification) {
            patternNameMap.remove(fqn);
            sanitizer.forgetPattern(((GenericQuerySpecification) specification).getInternalQueryRepresentation().getPattern());
        }
    }

    private void forgetSpecificationTransitively(IQuerySpecification<?> specification,
            Set<IQuerySpecification<?>> forgottenSpecifications) {
        forgetSpecification(specification);
        forgottenSpecifications.add(specification);
        for (IQuerySpecification<?> dependant : dependantQueries.get(specification.getInternalQueryRepresentation())) {
            if (!forgottenSpecifications.contains(dependant)) {
                forgetSpecificationTransitively(dependant, forgottenSpecifications);
            }
        }
        dependantQueries.removeAll(specification.getInternalQueryRepresentation());
    }

    /**
     * Forgets a specification in the builder, and also removes anything that depends on it.
     *
     * @param specification
     * @returns the set of specifications that were removed from the builder
     */
    public Set<IQuerySpecification<?>> forgetSpecificationTransitively(IQuerySpecification<?> specification) {
        Set<IQuerySpecification<?>> forgottenSpecifications = Sets.newHashSet();
        forgetSpecificationTransitively(specification, forgottenSpecifications);
        return forgottenSpecifications;
    }
    
    /**
     * Build a {@link QueryEvaluationHint} based on the pattern modifiers and annotations.
     * @since 1.5
     */
    protected QueryEvaluationHint buildHints(Pattern pattern){
        IQueryBackendFactory backendFactory = null;
        Modifiers modifiers = pattern.getModifiers();
        if (modifiers != null){
            switch(modifiers.getExecution()){
            case INCREMENTAL:
                backendFactory = new ReteBackendFactory();
                break;
            case SEARCH:
                backendFactory = LocalSearchBackendFactory.INSTANCE;
                break;
            case UNSPECIFIED:
            default:
                backendFactory = null;
                break;
            }
        }
        return new QueryEvaluationHint(null, backendFactory);
    }
}
