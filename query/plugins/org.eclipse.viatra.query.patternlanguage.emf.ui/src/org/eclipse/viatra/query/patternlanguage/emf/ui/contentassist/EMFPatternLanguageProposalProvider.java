/*******************************************************************************
 * Copyright (c) 2010-2012, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.patternlanguage.emf.ui.contentassist;

import static org.eclipse.emf.ecore.util.EcoreUtil.getRootContainer;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.viatra.query.patternlanguage.emf.annotations.PatternAnnotationProvider;
import org.eclipse.viatra.query.patternlanguage.emf.vql.Annotation;
import org.eclipse.viatra.query.patternlanguage.emf.vql.ClassType;
import org.eclipse.viatra.query.patternlanguage.emf.vql.PatternLanguagePackage;
import org.eclipse.viatra.query.patternlanguage.emf.vql.PackageImport;
import org.eclipse.viatra.query.patternlanguage.emf.vql.PathExpressionConstraint;
import org.eclipse.viatra.query.patternlanguage.emf.vql.PatternModel;
import org.eclipse.viatra.query.patternlanguage.emf.helper.PatternLanguageHelper;
import org.eclipse.viatra.query.patternlanguage.emf.services.EMFPatternLanguageGrammarAccess;
import org.eclipse.viatra.query.patternlanguage.emf.types.EMFTypeInferrer;
import org.eclipse.viatra.query.patternlanguage.emf.vql.Pattern;
import org.eclipse.viatra.query.patternlanguage.emf.vql.PatternBody;
import org.eclipse.viatra.query.patternlanguage.emf.vql.PatternCall;
import org.eclipse.viatra.query.patternlanguage.emf.vql.Variable;
import org.eclipse.viatra.query.runtime.emf.types.EClassTransitiveInstancesKey;
import org.eclipse.viatra.query.runtime.matchers.context.IInputKey;
import org.eclipse.viatra.query.runtime.matchers.psystem.aggregations.IAggregatorFactory;
import org.eclipse.xtext.Assignment;
import org.eclipse.xtext.CrossReference;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.EnumRule;
import org.eclipse.xtext.Keyword;
import org.eclipse.xtext.RuleCall;
import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.common.types.access.IJvmTypeProvider;
import org.eclipse.xtext.common.types.xtext.ui.ITypesProposalProvider;
import org.eclipse.xtext.common.types.xtext.ui.TypeMatchFilters;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.EObjectDescription;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.IScopeProvider;
import org.eclipse.xtext.ui.editor.contentassist.ConfigurableCompletionProposal;
import org.eclipse.xtext.ui.editor.contentassist.ContentAssistContext;
import org.eclipse.xtext.ui.editor.contentassist.ICompletionProposalAcceptor;
import org.eclipse.xtext.ui.editor.contentassist.ICompletionProposalAcceptor.Delegate;
import org.eclipse.xtext.ui.editor.model.IXtextDocument;
import org.eclipse.xtext.xbase.lib.Pair;
import org.eclipse.xtext.xbase.typesystem.IExpressionScope.Anchor;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

/**
 * see http://www.eclipse.org/Xtext/documentation/latest/xtext.html#contentAssist on how to customize content assistant
 */
public class EMFPatternLanguageProposalProvider extends AbstractEMFPatternLanguageProposalProvider {

    private static final Set<String> FILTERED_KEYWORDS = Sets.newHashSet("pattern");

    @Inject
    private PatternAnnotationProvider annotationProvider;
    @Inject
    IScopeProvider scopeProvider;
    @Inject
    ReferenceProposalCreator crossReferenceProposalCreator;
    @Inject
    IQualifiedNameConverter nameConverter;
    @Inject
    private EMFPatternLanguageGrammarAccess ga;

    @Inject
    private ValidFeatureDescription featureDescriptionPredicate;
    @Inject
    private IJvmTypeProvider.Factory jvmTypeProviderFactory;
    @Inject
    private ITypesProposalProvider typeProposalProvider;
    
    @Inject
    private EMFTypeInferrer typeInferrer;

    @Override
    public void createProposals(ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
        // suppress content assist in comments
        if (context.getCurrentNode().getGrammarElement() == ga.getML_COMMENTRule()
                || context.getCurrentNode().getGrammarElement() == ga.getSL_COMMENTRule()) {
            return;
        }
        super.createProposals(context, acceptor);
    }

    private ICompletionProposal configurePatternProposal(ICompletionProposal original) {
        if (original instanceof ConfigurableCompletionProposal) {
            ConfigurableCompletionProposal prop = (ConfigurableCompletionProposal) original;
            prop.setTextApplier(new PatternImporter());
            
        }
        return original;
    }
    
    @Override
    protected Function<IEObjectDescription, ICompletionProposal> getProposalFactory(String ruleName,
            ContentAssistContext contentAssistContext) {
        Function<IEObjectDescription, ICompletionProposal> factory = super.getProposalFactory(ruleName, contentAssistContext);
        if (contentAssistContext.getCurrentNode().getSemanticElement() instanceof PatternCall && ga.getQualifiedNameRule().getName().equals(ruleName)) {
            factory = Functions.compose(this::configurePatternProposal, factory);
        }
        
        return factory;
    }
    
    @SuppressWarnings("restriction")
    @Override
    public void completeKeyword(Keyword keyword, ContentAssistContext contentAssistContext,
            ICompletionProposalAcceptor acceptor) {
        // ignore keywords in FILTERED set
        if (FILTERED_KEYWORDS.contains(keyword.getValue())) {
            return;
        }
        super.completeKeyword(keyword, contentAssistContext, acceptor);
    }

    @Override
    public void complete_ValueReference(EObject model, RuleCall ruleCall, ContentAssistContext context,
            ICompletionProposalAcceptor acceptor) {
        super.complete_ValueReference(model, ruleCall, context, acceptor);
        if (model instanceof PathExpressionConstraint) {
            PathExpressionConstraint head = (PathExpressionConstraint) model;
            // XXX The following code re-specifies scoping instead of reusing the scope provider
            PatternLanguageHelper.getPathExpressionEMFTailType(head)
                .filter(EEnum.class::isInstance)
                .map(EEnum.class::cast)
                .ifPresent(type -> {
                    // In case of EEnums add Enum Literal constants
                    ContentAssistContext.Builder myContextBuilder = context.copy();
                    myContextBuilder.setMatcher(new EnumPrefixMatcher(type.getName()));

                    for (EEnumLiteral literal : type.getELiterals()) {
                        ICompletionProposal completionProposal = createCompletionProposal("::" + literal.getName(),
                                type.getName() + "::" + literal.getName(), null, myContextBuilder.toContext());
                        acceptor.accept(completionProposal);
                    }
                });
            // XXX The following code re-specifies scoping instead of reusing the scope provider
            // Always refer to existing variables
            PatternBody body = (PatternBody) head.eContainer()/* PatternBody */;
            for (Variable var : body.getVariables()) {
                acceptor.accept(createCompletionProposal(var.getName(), context));
            }
            Pattern pattern = (Pattern) body.eContainer();
            for (Variable var : pattern.getParameters()) {
                acceptor.accept(createCompletionProposal(var.getName(), context));
            }
        }
    }

    /**
     * @since 2.0
     */
    @Override
    public void complete_PackageImport(EObject model, RuleCall ruleCall, ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
        PatternModel pModel = null;
        EObject root = getRootContainer(model);
        if (root instanceof PatternModel) {
            pModel = (PatternModel) root;
        }
        ContentAssistContext.Builder myContextBuilder = context.copy();
        myContextBuilder.setMatcher(new ClassifierPrefixMatcher(context.getMatcher(), getQualifiedNameConverter()));
        ClassType type = null;
        if (model instanceof Variable) {
            type = (ClassType) ((Variable) model).getType();
        } else {
            return;
        }

        ICompositeNode node = NodeModelUtils.getNode(type);
        int offset = node.getOffset();
        Region replaceRegion = new Region(offset, context.getReplaceRegion().getLength()
                + context.getReplaceRegion().getOffset() - offset);
        myContextBuilder.setReplaceRegion(replaceRegion);
        myContextBuilder.setLastCompleteNode(node);
        StringBuilder availablePrefix = new StringBuilder(4);
        for (ILeafNode leaf : node.getLeafNodes()) {
            if (leaf.getGrammarElement() != null && !leaf.isHidden()) {
                if ((leaf.getTotalLength() + leaf.getTotalOffset()) < context.getOffset())
                    availablePrefix.append(leaf.getText());
                else
                    availablePrefix.append(leaf.getText().substring(0, context.getOffset() - leaf.getTotalOffset()));
            }
            if (leaf.getTotalOffset() >= context.getOffset())
                break;
        }
        myContextBuilder.setPrefix(availablePrefix.toString());

        ContentAssistContext myContext = myContextBuilder.toContext();
        for (PackageImport declaration : PatternLanguageHelper.getPackageImportsIterable(pModel)) {
            if (declaration.getEPackage() != null) {
                createClassifierProposals(declaration, model, myContext, acceptor);
            }
        }
    }

    private void createClassifierProposals(PackageImport declaration, EObject model, ContentAssistContext context,
            ICompletionProposalAcceptor acceptor) {
        // String alias = declaration.getAlias();
        // QualifiedName prefix = (!Strings.isEmpty(alias))
        // ? QualifiedName.create(getValueConverter().toString(alias,"ID"))
        // : null;
        boolean createDatatypeProposals = modelOrContainerIs(model, Variable.class);
        boolean createEnumProposals = modelOrContainerIs(model, EnumRule.class);
        boolean createClassProposals = modelOrContainerIs(model, Variable.class);
        Function<IEObjectDescription, ICompletionProposal> factory = getProposalFactory(null, context);
        for (EClassifier classifier : declaration.getEPackage().getEClassifiers()) {
            if (classifier instanceof EDataType && createDatatypeProposals || classifier instanceof EEnum
                    && createEnumProposals || classifier instanceof EClass && createClassProposals) {
                String classifierName = getValueConverter().toString(classifier.getName(), "ID");
                QualifiedName proposalQualifiedName = /* (prefix != null) ? prefix.append(classifierName) : */QualifiedName
                        .create(classifierName);
                IEObjectDescription description = EObjectDescription.create(proposalQualifiedName, classifier);
                ConfigurableCompletionProposal proposal = (ConfigurableCompletionProposal) factory.apply(description);
                if (proposal != null) {
                    /*
                     * if (prefix != null) proposal.setDisplayString(classifier.getName() + " - " + alias);
                     */
                    proposal.setPriority(proposal.getPriority() * 2);
                }
                acceptor.accept(proposal);
            }
        }
    }

    private boolean modelOrContainerIs(EObject model, Class<?>... types) {
        for (Class<?> type : types) {
            if (type.isInstance(model) || type.isInstance(model.eContainer()))
                return true;
        }
        return false;
    }

    /**
     * @since 2.0
     */
    public void complete_RefType(PathExpressionConstraint model, RuleCall ruleCall, ContentAssistContext context,
            ICompletionProposalAcceptor acceptor) {
        EObject reference = NodeModelUtils.findActualSemanticObjectFor(context.getCurrentNode());
        // If the semantic object is a ReferenceType, calculate its index
        int index = model.getEdgeTypes().indexOf(reference);
        final int edgeCount = model.getEdgeTypes().size();
        // If the semantic object for the current node is the constraint, consider the last edge as context for the scope calculation
        if (Objects.equals(model, reference) && edgeCount > 1) {
            index = edgeCount;
        }
        // For the scope calculation, step one step back, to either the previous edge type or the source type
        IScope scope = scopeProvider.getScope(index > 0 ? model.getEdgeTypes().get(index - 1) : model.getSourceType(),
                PatternLanguagePackage.Literals.REFERENCE_TYPE__REFNAME);
        crossReferenceProposalCreator.lookupCrossReference(scope, model,
                PatternLanguagePackage.Literals.REFERENCE_TYPE__REFNAME, acceptor,
                Predicates.<IEObjectDescription> alwaysTrue(),
                getProposalFactory(ruleCall.getRule().getName(), context));
    }

    @Override
    public void completePackageImport_EPackage(EObject model, Assignment assignment, ContentAssistContext context,
            ICompletionProposalAcceptor acceptor) {
        super.completePackageImport_EPackage(model, assignment, context, new StringProposalDelegate(acceptor, context));
    }
    
    /**
     * Remove extra double quote from proposal if the next character is already a double quote.
     * Based on the work of Christian Dietrich in {@link "https://christiandietrich.wordpress.com/2015/03/19/xtext-and-strings-as-cross-references/"} 
     * 
     * @author Abel Hegedus
     *
     */
    static class StringProposalDelegate extends Delegate {
        
        ContentAssistContext ctx;
 
        StringProposalDelegate(ICompletionProposalAcceptor delegate, ContentAssistContext ctx) {
            super(delegate);
            this.ctx = ctx;
        }
 
        @Override
        public void accept(ICompletionProposal proposal) {
            if (proposal instanceof ConfigurableCompletionProposal) {
                ConfigurableCompletionProposal prop = (ConfigurableCompletionProposal) proposal;
                int replacementLength = prop.getReplacementLength();
                int endPos = prop.getReplacementOffset() + replacementLength; 
                IXtextDocument document = ctx.getDocument();
                if (document != null && document.getLength() > endPos) {
                    // We are not at the end of the file
                    try {
                        if ("\"".equals(document.get(endPos, 1))) {
                            /*
                             * XXX Updating replacementLength should be enough, however it does not work, as it will be
                             * rewritten when apply() is called on the proposal
                             */ 
                            String replacementString = prop.getReplacementString();
                            prop.setReplacementString(replacementString.substring(0,replacementString.length()-1));
                        }
                    } catch (BadLocationException e) {
                        // never happens, as we already checked that the given range is valid 
                        throw new IllegalStateException("No content at position inside the document", e);
                    }
                }
            }
            super.accept(proposal);
        }
 
    }
    
    private boolean isReferrable(Variable v) {
        return v!= null && !v.eIsProxy() && 
                !v.getName().startsWith("_") && !PatternLanguageHelper.hasAggregateReference(v);
    }
    
    private boolean isReferrable(IEObjectDescription desc) {
        if (desc != null && EcoreUtil2.isAssignableFrom(PatternLanguagePackage.Literals.VARIABLE, desc.getEClass())) {
            Variable v = (Variable) desc.getEObjectOrProxy();
            return v!= null && !v.eIsProxy() && 
                    !v.getName().startsWith("_") && !PatternLanguageHelper.hasAggregateReference(v);
        }
        return false;
    }
    
    
    /**
     * @since 2.0
     */
    @Override
    public void complete_Annotation(EObject model, RuleCall ruleCall, ContentAssistContext context,
            ICompletionProposalAcceptor acceptor) {
        for (String annotationName : annotationProvider.getAllAnnotationNames()) {
            if (annotationProvider.isDeprecated(annotationName)) {
                continue;
            }
            String prefixedName = String.format("@%s", annotationName);
            String prefix = context.getPrefix();
            ContentAssistContext modifiedContext = context;
            INode lastNode = context.getLastCompleteNode();
            if ("".equals(prefix) && lastNode.getSemanticElement() instanceof Annotation) {
                Annotation previousNode = (Annotation) lastNode.getSemanticElement();
                String annotationPrefix = previousNode.getName();
                if (previousNode.getParameters().isEmpty()
                        && !annotationProvider.getAllAnnotationNames().contains(annotationPrefix)) {
                    modifiedContext = context.copy()
                            .setReplaceRegion(new Region(lastNode.getOffset(), lastNode.getLength() + prefix.length()))
                            .toContext();
                    prefixedName = annotationName;
                }
            }
            ICompletionProposal proposal = createCompletionProposal(prefixedName, prefixedName, null, modifiedContext);
            if (proposal instanceof ConfigurableCompletionProposal) {
                ((ConfigurableCompletionProposal) proposal).setAdditionalProposalInfo(annotationProvider
                        .getAnnotationObject(annotationName));
                ((ConfigurableCompletionProposal) proposal).setHover(getHover());
            }
            acceptor.accept(proposal);
        }
    }

    /**
     * @since 2.0
     */
    @Override
    public void complete_AnnotationParameter(EObject model, RuleCall ruleCall, ContentAssistContext context,
            ICompletionProposalAcceptor acceptor) {
        if (model instanceof Annotation) {
            Annotation annotation = (Annotation) model;
            for (String paramName : annotationProvider.getAnnotationParameters(annotation.getName())) {
                String outputName = String.format("%s = ", paramName);
                ICompletionProposal proposal = createCompletionProposal(outputName, paramName, null, context);
                if (proposal instanceof ConfigurableCompletionProposal) {
                    ((ConfigurableCompletionProposal) proposal).setAdditionalProposalInfo(annotationProvider
                            .getAnnotationParameter(annotation.getName(), paramName));
                    ((ConfigurableCompletionProposal) proposal).setHover(getHover());
                }
                acceptor.accept(proposal);
            }
        }
    }

    /**
     * @since 2.0
     */
    @Override
    public void complete_VariableReference(EObject model, RuleCall ruleCall, ContentAssistContext context,
            ICompletionProposalAcceptor acceptor) {
        IScope scope = scopeProvider.getScope(model, PatternLanguagePackage.Literals.VARIABLE_REFERENCE__VARIABLE);
        crossReferenceProposalCreator.lookupCrossReference(scope, model,
                PatternLanguagePackage.Literals.VARIABLE_REFERENCE__VARIABLE, acceptor,
                this::isReferrable,
                getProposalFactory(ruleCall.getRule().getName(), context));

    }

    /**
     * @since 2.0
     */
    @Override
    protected void createLocalVariableAndImplicitProposals(EObject context, Anchor anchor,
            ContentAssistContext contentAssistContext, ICompletionProposalAcceptor acceptor) {
        String prefix = contentAssistContext.getPrefix();
        if (prefix.length() > 0 && !Character.isJavaIdentifierStart(prefix.charAt(0))) {
            return;
        }

        final PatternBody body = EcoreUtil2.getContainerOfType(context, PatternBody.class);
        for (Variable v : Iterables.filter(body.getVariables(), this::isReferrable)) {
            ICompletionProposal proposal = createCompletionProposal(v.getName(), contentAssistContext);
            acceptor.accept(proposal);
        }

        proposeDeclaringTypeForStaticInvocation(context, null /* ignore */, contentAssistContext, acceptor);
    }

    /**
     * @since 2.0
     */
    @Override
    public void completePatternCall_PatternRef(EObject model, Assignment assignment, ContentAssistContext context,
            ICompletionProposalAcceptor acceptor) {
        /*
         * filter not local, private patterns (private patterns in other resources) this information stored in the
         * userdata of the EObjectDescription EObjectDescription only created for not local eObjects, so check for
         * resource equality is unnecessary.
         */
        lookupCrossReference(((CrossReference) assignment.getTerminal()), context, acceptor,
                Predicates.and(featureDescriptionPredicate, input -> !("true".equals(input.getUserData("private")))));
    }
    
    /**
     * @since 2.0
     */
    @Override
    public void completeAggregatedValue_Aggregator(EObject model, Assignment assignment, ContentAssistContext context,
            ICompletionProposalAcceptor acceptor) {
        final IJvmTypeProvider jvmTypeProvider = jvmTypeProviderFactory
                .createTypeProvider(model.eResource().getResourceSet());
        final JvmType interfaceToImplement = jvmTypeProvider.findTypeByName(IAggregatorFactory.class.getName());
        typeProposalProvider.createSubTypeProposals(interfaceToImplement, this, context,
                PatternLanguagePackage.Literals.AGGREGATED_VALUE__AGGREGATOR, TypeMatchFilters.canInstantiate(),
                acceptor);
    }

    /**
     * @since 2.2
     */
    public void complete_Parameter(Pattern model, RuleCall ruleCall, ContentAssistContext context,
            ICompletionProposalAcceptor acceptor) {
        final Set<String> existingParameterNames = model.getParameters().stream()
                .map(Variable::getName)
                .filter(Objects::nonNull)
                .filter(s -> !s.isEmpty() && !Objects.equals("null", s))
                .collect(Collectors.toSet());
        model.getBodies().stream().flatMap(b -> b.getVariables().stream())
                .filter(v -> !PatternLanguageHelper.isNamedSingleUse(v) && !PatternLanguageHelper.isUnnamedSingleUseVariable(v))
                .filter(v -> !existingParameterNames.contains(v.getName()))
                .map(v -> new Pair<>(v, typeInferrer.getType(v)))
                .distinct()
                .forEach(p -> {
                    String name = p.getKey().getName();
                    IInputKey typeKey = p.getValue();
                    // TODO insert Java type functionality missing; to add that instead of string manipulation we should
                    // directly edit the model to avoid issues like bug
                    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=517254 
                    if (typeKey instanceof EClassTransitiveInstancesKey) {
                        EClassTransitiveInstancesKey type = (EClassTransitiveInstancesKey) typeKey;
                        acceptor.accept(createCompletionProposal(String.format("%s : %s", name, type.getEmfKey().getName()), context));
                    } else {
                        acceptor.accept(createCompletionProposal(name, context));
                   }});
        super.complete_Parameter(model, ruleCall, context, acceptor);
    }
    
}
