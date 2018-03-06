/*******************************************************************************
 * Copyright (c) 2010-2012, Abel Hegedus, Istvan Rath and Daniel Varro
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Abel Hegedus - initial API and implementation
 *******************************************************************************/
package org.eclipse.viatra.addon.querybasedfeatures.runtime.validation;

import java.util.Collection;
import java.util.Optional;

import javax.inject.Inject;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.viatra.addon.querybasedfeatures.runtime.QueryBasedFeatureKind;
import org.eclipse.viatra.addon.querybasedfeatures.runtime.handler.QueryBasedFeatures;
import org.eclipse.viatra.query.patternlanguage.emf.annotations.IPatternAnnotationAdditionalValidator;
import org.eclipse.viatra.query.patternlanguage.emf.annotations.PatternAnnotationParameter;
import org.eclipse.viatra.query.patternlanguage.emf.annotations.PatternAnnotationValidator;
import org.eclipse.viatra.query.patternlanguage.emf.helper.PatternLanguageHelper;
import org.eclipse.viatra.query.patternlanguage.emf.types.EMFTypeSystem;
import org.eclipse.viatra.query.patternlanguage.emf.types.ITypeInferrer;
import org.eclipse.viatra.query.patternlanguage.emf.validation.IIssueCallback;
import org.eclipse.viatra.query.patternlanguage.emf.vql.Annotation;
import org.eclipse.viatra.query.patternlanguage.emf.vql.BoolValue;
import org.eclipse.viatra.query.patternlanguage.emf.vql.Pattern;
import org.eclipse.viatra.query.patternlanguage.emf.vql.PatternLanguagePackage;
import org.eclipse.viatra.query.patternlanguage.emf.vql.StringValue;
import org.eclipse.viatra.query.patternlanguage.emf.vql.ValueReference;
import org.eclipse.viatra.query.patternlanguage.emf.vql.Variable;
import org.eclipse.viatra.query.patternlanguage.emf.vql.VariableValue;
import org.eclipse.viatra.query.runtime.emf.types.EClassTransitiveInstancesKey;
import org.eclipse.viatra.query.runtime.matchers.context.IInputKey;

/**
 * @author Abel Hegedus
 * 
 */
public class QueryBasedFeaturePatternValidator extends PatternAnnotationValidator implements IPatternAnnotationAdditionalValidator {

    private static final String VALIDATOR_BASE_CODE = "org.eclipse.viatra.addon.querybasedfeatures.";
    public static final String GENERAL_ISSUE_CODE = VALIDATOR_BASE_CODE + "general";
    public static final String METAMODEL_ISSUE_CODE = VALIDATOR_BASE_CODE + "faulty_metamodel";
    public static final String PATTERN_ISSUE_CODE = VALIDATOR_BASE_CODE + "faulty_pattern";
    public static final String ANNOTATION_ISSUE_CODE = VALIDATOR_BASE_CODE + "faulty_annotation";

    private static final PatternAnnotationParameter FEATURE_PARAMETER = new PatternAnnotationParameter("feature", 
            PatternAnnotationParameter.STRING,
            "The name of the EStructuralFeature that the query will serve (default: pattern name).",
            /*multiple*/ false,
            /*mandatory*/ false);
    private static final PatternAnnotationParameter SOURCE_PARAMETER = new PatternAnnotationParameter("source",
            PatternAnnotationParameter.VARIABLEREFERENCE,
            "The name of the parameter that is the source of the derived feature (default: type of first parameter).",
            /*multiple*/ false,
            /*mandatory*/ false);
    private static final PatternAnnotationParameter TARGET_PARAMETER = new PatternAnnotationParameter("target", 
            PatternAnnotationParameter.VARIABLEREFERENCE,
            "The name of the parameter that is the target of the derived feature (default: type of second parameter).",
            /*multiple*/ false,
            /*mandatory*/ false);
    private static final PatternAnnotationParameter KIND_PARAMETER = new PatternAnnotationParameter("kind",
            PatternAnnotationParameter.STRING,
            "Possible values: single, many, sum, iteration (default: feature.isMany?many:single)",
            /*multiple*/false,
            /*mandatory*/false);
    
    @Inject
    private ITypeInferrer typeInferrer;

    public QueryBasedFeaturePatternValidator() {
        super("QueryBasedFeature",
                "This annotation is used to mark a pattern as a query definition for a query-based feature.",
                FEATURE_PARAMETER, SOURCE_PARAMETER, TARGET_PARAMETER, KIND_PARAMETER);
    }
    
    @Override
    public Optional<IPatternAnnotationAdditionalValidator> getAdditionalValidator() {
        return Optional.of(this);
    }

    @Override
    public void executeAdditionalValidation(Annotation annotation, IIssueCallback validator) {
        boolean foundErrors = false;

        Pattern pattern = (Pattern) annotation.eContainer();

        foundErrors = checkFeatureUniquenessOnQBFAnnotations(annotation, validator, pattern);
        if(foundErrors) {
            return;
        }
        
        // 1. at least two parameters
        if (pattern.getParameters().size() < 2) {
            validator.error("Query-based feature pattern must have at least two parameters.", pattern,
                    PatternLanguagePackage.Literals.PATTERN__PARAMETERS, PATTERN_ISSUE_CODE);
            return;
        }
        // 2. first parameter or "source" (if set) is EClassifier -> Source
        Variable source = null;
        ValueReference ref = PatternLanguageHelper.getFirstAnnotationParameter(annotation, "source");
        if (ref == null) {
            source = pattern.getParameters().get(0);
        } else if (ref instanceof VariableValue) {
            source = PatternLanguageHelper.getParameterByName(pattern, ((VariableValue) ref).getValue().getVar()).orElse(null);
            if (pattern.getParameters().get(0).equals(source)) {
                validator.warning("The 'source' parameter is not needed if it is the first pattern parameter.", ref,
                        PatternLanguagePackage.Literals.VARIABLE_VALUE__VALUE, ANNOTATION_ISSUE_CODE);
            }
        }
        IInputKey sourceType = null;
        if (source != null) {
            sourceType = typeInferrer.getType(source);
        }
        if (!(sourceType instanceof EClassTransitiveInstancesKey)) {
            validator.error("The 'source' parameter must be EClass.", source,
                    PatternLanguagePackage.Literals.VARIABLE__TYPE, PATTERN_ISSUE_CODE);
            return;
        }
        EClass sourceClass = ((EClassTransitiveInstancesKey) sourceType).getEmfKey();

        // 3. pattern name or "feature" is a feature of Source
        String featureName = null;
        EObject contextForFeature = null;
        EStructuralFeature contextESFForFeature = null;
        ref = PatternLanguageHelper.getFirstAnnotationParameter(annotation, "feature");
        if (ref == null) {
            featureName = pattern.getName();
            contextForFeature = pattern;
            contextESFForFeature = PatternLanguagePackage.Literals.PATTERN__NAME;
        } else if (ref instanceof StringValue) {
            featureName = ((StringValue) ref).getValue();
            contextForFeature = ref;
            contextESFForFeature = PatternLanguagePackage.Literals.STRING_VALUE__VALUE;
        }
        if (featureName == null || featureName.isEmpty()) {
            validator.error("The 'feature' parameter must not be empty.", ref,
                    PatternLanguagePackage.Literals.STRING_VALUE__VALUE, ANNOTATION_ISSUE_CODE);
            return;
        }
        EStructuralFeature feature = null;
        for (EStructuralFeature f : sourceClass.getEStructuralFeatures()) {
            if (featureName.equals(f.getName())) {
                feature = f;
                break;
            }
        }
        if (feature == null) {
            validator.error(String.format("Cannot find feature %s of EClass %s.", featureName, sourceClass.getName()),
                    contextForFeature, contextESFForFeature, ANNOTATION_ISSUE_CODE);
            return;
        } else {
            if (feature instanceof EReference) {
                boolean featureError = false;
                if(!feature.isDerived()) {
                    validator.error(String.format("Feature %s is not derived.",featureName),
                            contextForFeature, contextESFForFeature, METAMODEL_ISSUE_CODE);
                    featureError = true;
                }
                if(!feature.isTransient()) {
                    validator.error(String.format("Feature %s is not transient.",featureName),
                            contextForFeature, contextESFForFeature, METAMODEL_ISSUE_CODE);
                    featureError = true;
                }
                if(!feature.isVolatile()) {
                    validator.error(String.format("Feature %s is not volatile.",featureName),
                            contextForFeature, contextESFForFeature, METAMODEL_ISSUE_CODE);
                    featureError = true;
                }
                if(featureError) {
                    return;
                }
                if (feature.isChangeable()) {
                    validator.warning(String.format("Feature %s is changeable, make sure to implement setter.",featureName),
                            contextForFeature, contextESFForFeature, METAMODEL_ISSUE_CODE);
                }
            }
        }
        EClassifier classifier = feature.getEGenericType().getEClassifier();
        if (classifier == null) {
            validator.error(String.format("Feature %s has no type information set in the metamodel", featureName),
                    contextForFeature, contextESFForFeature, METAMODEL_ISSUE_CODE);
            return;
        }
        // 4. second parameter or "target" (if set) is compatible(?) with feature type -> Target
        Variable target = null;
        ref = PatternLanguageHelper.getFirstAnnotationParameter(annotation, "target");
        if (ref == null) {
            target = pattern.getParameters().get(1);
        } else if (ref instanceof VariableValue) {
            target = PatternLanguageHelper.getParameterByName(pattern, ((VariableValue) ref).getValue().getVar()).orElse(null);
            if (pattern.getParameters().get(1).equals(target)) {
                validator.warning("The 'target' parameter is not needed if it is the second pattern parameter.", ref,
                        PatternLanguagePackage.Literals.VARIABLE_VALUE__VALUE, ANNOTATION_ISSUE_CODE);
            }
        }
        IInputKey targetType = typeInferrer.getType(target);
        
        EClassifier targetClassifier = EMFTypeSystem.EXTRACT_CLASSIFIER.apply(targetType); 
        if (targetType == null) {
            validator.warning("Cannot find target EClassifier", target, PatternLanguagePackage.Literals.VARIABLE__TYPE,
                    PATTERN_ISSUE_CODE);
        }
        
        // 5. "kind" (if set) is valid enum value
        QueryBasedFeatureKind kind = null;
        ref = PatternLanguageHelper.getFirstAnnotationParameter(annotation, "kind");
        if (ref instanceof StringValue) {
            String kindStr = ((StringValue) ref).getValue();
            if (QueryBasedFeatureKind.getStringValue(QueryBasedFeatureKind.SINGLE_REFERENCE).equals(kindStr)) {
                if (feature.getUpperBound() != 1) {
                    validator.error(
                            String.format("Upper bound of feature %s should be 1 for single 'kind'.", featureName),
                            ref, PatternLanguagePackage.Literals.STRING_VALUE__VALUE, METAMODEL_ISSUE_CODE);
                    return;
                }
                kind = QueryBasedFeatureKind.SINGLE_REFERENCE;
            } else if (QueryBasedFeatureKind.getStringValue(QueryBasedFeatureKind.MANY_REFERENCE).equals(kindStr)) {
                if (feature.getUpperBound() != -1 && feature.getUpperBound() < 2) {
                    validator.error(String
                            .format("Upper bound of feature %s should be -1 or larger than 1 for many 'kind'.",
                                    featureName), ref, PatternLanguagePackage.Literals.STRING_VALUE__VALUE,
                                    METAMODEL_ISSUE_CODE);
                    return;
                }
                kind = QueryBasedFeatureKind.MANY_REFERENCE;
            } else if (QueryBasedFeatureKind.getStringValue(QueryBasedFeatureKind.SUM).equals(kindStr)) {
                if (!classifier.equals(EcorePackage.Literals.EINT)) {
                    validator
                            .error(String.format("Type of feature %s should be EInt for %s 'kind'.", featureName,
                                    kindStr), ref, PatternLanguagePackage.Literals.STRING_VALUE__VALUE,
                                    METAMODEL_ISSUE_CODE);
                    return;
                }
                kind = QueryBasedFeatureKind.SUM;
            } else if (QueryBasedFeatureKind.getStringValue(QueryBasedFeatureKind.ITERATION).equals(kindStr)) {
                validator.warning("Don't forget to subclass QueryBasedFeature for iteration 'kind'.", ref,
                        PatternLanguagePackage.Literals.STRING_VALUE__VALUE, ANNOTATION_ISSUE_CODE);
                kind = QueryBasedFeatureKind.ITERATION;
            }
        }
        
        if (!classifier.equals(targetClassifier) && (kind == QueryBasedFeatureKind.SINGLE_REFERENCE || kind == QueryBasedFeatureKind.MANY_REFERENCE)) {
            validator.warning(String.format("The 'target' parameter type %s is not equal to actual feature type %s.",
                    featureName, sourceClass.getName()), target, PatternLanguagePackage.Literals.VARIABLE__TYPE,
                    PATTERN_ISSUE_CODE);
        }
        // 6. keepCache (if set) is correct for the kind
        ref = PatternLanguageHelper.getFirstAnnotationParameter(annotation, "keepCache");
        if (ref instanceof BoolValue) {
            boolean keepCache = PatternLanguageHelper.getValue(ref, Boolean.class); 
            if(!keepCache) {
                if (kind == null) {
                    validator.error("Cacheless behavior only available for single and many kinds.", ref,
                            PatternLanguagePackage.Literals.STRING_VALUE__VALUE, ANNOTATION_ISSUE_CODE);
                } else switch (kind) {
                case SINGLE_REFERENCE:
                case MANY_REFERENCE:
                    // OK
                    break;
                default:
                    validator.error("Cacheless behavior only available for single and many kinds.", ref,
                            PatternLanguagePackage.Literals.STRING_VALUE__VALUE, ANNOTATION_ISSUE_CODE);
                    break;
                }
            }
        }
        
        // 7. if resource is not writable, the generation will fail
        EPackage ePackage = sourceClass.getEPackage();
        URI uri = ePackage.eResource().getURI();
        // only file and platform resource URIs are considered safely writable
        if(!(uri.isFile() || uri.isPlatformResource())) {
            ValueReference useModelCodeRef = PatternLanguageHelper.getFirstAnnotationParameter(annotation, "generateIntoModelCode");
            boolean useModelCode = false;
            if(useModelCodeRef != null){
                useModelCode = PatternLanguageHelper.getValue(useModelCodeRef, Boolean.class);
            }
            String patternFQN = PatternLanguageHelper.getFullyQualifiedName(pattern);
            boolean annotationsOK = QueryBasedFeatures.checkEcoreAnnotation(ePackage, feature, patternFQN, useModelCode);
            
            if(!annotationsOK){
                validator.error(String.format("Ecore package of %s must be writable by Query-based Feature generator, "
                        + "but resource with URI %s is not!", sourceClass.getName(), uri.toString()), annotation,
                        PatternLanguagePackage.Literals.ANNOTATION__NAME, METAMODEL_ISSUE_CODE);
            } else {
                validator.warning(String.format("Resource at URI %s for EPackage of %s is not writable,"
                        + " but it already contains correct annotations.", uri.toString(), sourceClass.getName()), annotation,
                        PatternLanguagePackage.Literals.ANNOTATION__NAME, METAMODEL_ISSUE_CODE);
            }
        }
        

    }

    private boolean checkFeatureUniquenessOnQBFAnnotations(Annotation annotation, IIssueCallback validator, Pattern pattern) {
        Collection<Annotation> qbfAnnotations = PatternLanguageHelper.getAnnotationsByName(pattern, "QueryBasedFeature");
        if(qbfAnnotations.size() > 1) {
            ValueReference feature = PatternLanguageHelper.getFirstAnnotationParameter(annotation, "feature");
            if(feature == null) {
                validator.error("Feature must be specified when multiple QueryBasedFeature annotations are used on a single pattern.", annotation,
                        PatternLanguagePackage.Literals.ANNOTATION__NAME, ANNOTATION_ISSUE_CODE);
                return true;
            } else {
                String featureName = ((StringValue) feature).getValue();
                for (Annotation antn : qbfAnnotations) {
                    ValueReference otherFeature = PatternLanguageHelper.getFirstAnnotationParameter(antn, "feature");
                    if(otherFeature != null) {
                        String otherFeatureName = ((StringValue) otherFeature).getValue();
                        if(featureName.equals(otherFeatureName)) {
                            validator.error("Feature must be unique among multiple QueryBasedFeature annotations used on a single pattern.", annotation,
                                    PatternLanguagePackage.Literals.ANNOTATION__NAME, ANNOTATION_ISSUE_CODE);
                            return true;
                        }
                    }
                }
            }
            
        }
        return false;
    }
    

}
