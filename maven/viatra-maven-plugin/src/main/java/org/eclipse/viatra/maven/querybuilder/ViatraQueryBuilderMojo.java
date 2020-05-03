/*******************************************************************************
 * Copyright (c) 2010-2014, Jozsef Makai, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.viatra.maven.querybuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.eclipse.emf.codegen.ecore.genmodel.GenModelPackage;
import org.eclipse.emf.codegen.ecore.genmodel.GenPackage;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.resource.impl.URIMappingRegistryImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.viatra.maven.querybuilder.helper.Metamodel;
import org.eclipse.viatra.maven.querybuilder.helper.UriMapping;
import org.eclipse.viatra.maven.querybuilder.setup.EMFPatternLanguageMavenStandaloneSetup;
import org.eclipse.viatra.maven.querybuilder.setup.MavenBuilderGenmodelLoader;
import org.eclipse.viatra.maven.querybuilder.setup.MavenGeneratorConfigProvider;
import org.eclipse.viatra.query.patternlanguage.emf.util.EMFPatternLanguageGeneratorConfig;
import org.eclipse.viatra.query.patternlanguage.emf.util.EMFPatternLanguageGeneratorConfig.MatcherGenerationStrategy;
import org.eclipse.xtext.maven.Language;
import org.eclipse.xtext.maven.OutputConfiguration;

import com.google.common.base.Strings;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;

/**
 * Goal which generates Java code from patterns of the VIATRA Query Language.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class ViatraQueryBuilderMojo extends AbstractMojo {

    @Parameter(required = true)
    private List<Metamodel> metamodels;

    /**
     * the project relative path to the output directory
     */
    @Parameter(required = true)
    private String outputDirectory;

    /**
     * whether the output directory should be created if t doesn't already exist.
     */
    @Parameter(required = false)
    private boolean createOutputDirectory = true;

    /**
     * whether existing resources should be overridden.
     */
    @Parameter(required = false)
    private boolean overrideExistingResources = true;

    /**
     * The project itself. This parameter is set by maven.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    /**
     * The plugin descriptor
     */
    @Parameter(defaultValue = "${plugin}", readonly = true, required = true)
    protected PluginDescriptor descriptor;
    
    /**
     * Project classpath.
     */
    @Parameter(defaultValue = "${project.compileClasspathElements}", readonly = true, required = true)
    private List<String> classpathElements;

    /**
     * Location of the generated source files.
     */
    @Parameter(defaultValue = "${project.build.directory}/xtext-temp")
    private String tmpClassDirectory;

    /**
     * File encoding argument for the generator.
     */
    @Parameter(property = "xtext.encoding", defaultValue = "${project.build.sourceEncoding}")
    protected String encoding;

    @Parameter(property="maven.compiler.source", defaultValue="1.8")
    private String compilerSourceLevel;

    @Parameter(property="maven.compiler.target", defaultValue="1.8")
    private String compilerTargetLevel;
    
    /**
     * whether the dependencies of the project should be added to the classpath of the plugin.
     */
    @Parameter(defaultValue = "false")
    private boolean useProjectDependencies;
    
    /**
     * @since 1.7
     */
    @Parameter(defaultValue = "true")
    private boolean useEclipseGeneratorPreferences;
    
    /**
     * @since 1.7
     */
    @Parameter(defaultValue = "NESTED_CLASS")
    private MatcherGenerationStrategy matcherGeneration;
    
    /**
     * @parameter
     * @since 1.7
     */
    @Parameter(defaultValue = "false")
    private boolean generateMatchProcessors;
    
    /**
     * Location of the VIATRA Compiler settings file.
     */
    @Parameter(defaultValue="${basedir}/.settings/org.eclipse.viatra.query.patternlanguage.emf.EMFPatternLanguage.prefs", readonly = true)
    private String propertiesFileLocation;
    
    /**
     * URI mappings to add to the global URI mapping registry.
     */
    @Parameter
    private List<UriMapping> uriMappings;

    public void execute() throws MojoExecutionException, MojoFailureException {

        prepareClasspath();
        
        prepareUriMappings();
        
        registerGenmodelExtension();

        registerMetamodels();

        ResourceOrderingXtextGenerator generator = new ResourceOrderingXtextGenerator();
        provideModelInferrerConfiguration();
        
        setupXtextGenerator(generator);

        generator.execute();

    }

    protected void provideModelInferrerConfiguration() {
        EMFPatternLanguageGeneratorConfig config = new EMFPatternLanguageGeneratorConfig();
        if (useEclipseGeneratorPreferences && propertiesFileLocation != null) {
            File f = new File(propertiesFileLocation);
            if (f.canRead()) {
                Properties vqlCompilerSettings = new Properties();
                try {
                    vqlCompilerSettings.load(new FileInputStream(f));
                    config.parseBuilderConfigurationPropertiesFile(vqlCompilerSettings);
                } catch (IOException e) {
                    getLog().warn(e);
                }
            } else {
                getLog().info(
                        "Can't find VIATRA Generator properties under " + propertiesFileLocation + ", maven defaults are used.");
                config.setMatcherGenerationStrategy(matcherGeneration);
                config.setGenerateMatchProcessors(generateMatchProcessors);
            }
        } else {
            config.setMatcherGenerationStrategy(matcherGeneration);
            config.setGenerateMatchProcessors(generateMatchProcessors);
        }

        MavenGeneratorConfigProvider.setGeneratorConfig(config);
    }
    
    /**
     * The URI converters used by EMF resource sets always delegate to the
     * global URI Mapping Registry, that means we can add user-defined mappings
     * there without knowing about the specific resource set.
     * 
     * @throws MojoExecutionException
     */
    protected void prepareUriMappings() throws MojoExecutionException {
        if(uriMappings == null || uriMappings.isEmpty()) {
            return;
        }
        URIMappingRegistryImpl uriMappingRegistry = URIMappingRegistryImpl.INSTANCE;
        for (UriMapping uriMapping : uriMappings) {
            try {   
                URI sourceUri = URI.createURI(uriMapping.getSourceUri());
                URI targetUri = URI.createURI(uriMapping.getTargetUri());
                uriMappingRegistry.put(sourceUri, targetUri);
            } catch (Exception e) {
                final String msg = String.format("Error while adding URI mapping (source: %s, target: %s)", uriMapping.getSourceUri(), uriMapping.getTargetUri());
                getLog().error(msg);
                throw new MojoExecutionException(msg, e);
            }
        }
    }

    /**
     * The classpath of the Maven plugin does not include the dependencies of the Maven project
     * that the plugin runs on. However, the dependencies of the project are passed by Maven to
     * the plugin in the {@link #classpathElements} list. These URLs can be added to the realm
     * of the plugin.
     * 
     * @throws MojoExecutionException
     */
    protected void prepareClasspath() throws MojoExecutionException {
        if(useProjectDependencies) {
            ClassRealm realm = descriptor.getClassRealm();
            getLog().info("Adding project dependencies to classpath");
            for (String element : classpathElements)
            {
                File elementFile = new File(element);
                try {
                    realm.addURL(elementFile.toURI().toURL());
                } catch (MalformedURLException e) {
                    final String msg = "Error while adding classpath URL " + element;
                    getLog().error(msg);
                    throw new MojoExecutionException(msg, e);
                }
            }
        }
    }

    /**
     * To parameterize the XtextGenerator object accordingly
     * 
     * @param generator
     *            The XtextGenerator object
     */
    private void setupXtextGenerator(ResourceOrderingXtextGenerator generator) {
        generator.setLog(getLog()); // it's needed to give our Logger to the
                                    // other plugin to avoid exceptions

        Language language = new Language();
        language.setSetup(EMFPatternLanguageMavenStandaloneSetup.class.getCanonicalName());

        OutputConfiguration outputConfiguration = new OutputConfiguration();
        outputConfiguration.setOutputDirectory(outputDirectory);
        outputConfiguration.setCreateOutputDirectory(createOutputDirectory);
        outputConfiguration.setOverrideExistingResources(overrideExistingResources);

        List<OutputConfiguration> ocList = new ArrayList<>();
        ocList.add(outputConfiguration);

        language.setOutputConfigurations(ocList);

        List<Language> languageList = new ArrayList<>();
        languageList.add(language);

        generator.setLanguages(languageList);
        generator.setSkip(false);
        generator.setFailOnValidationError(true);
        generator.setProject(project);
        generator.setClasspathElements(classpathElements);
        generator.setTmpClassDirectory(tmpClassDirectory);
        generator.setEncoding(encoding);
        generator.setCompilerSourceLevel(compilerSourceLevel);
        generator.setCompilerTargetLevel(compilerTargetLevel);
    }

    /**
     * Register user given metamodel NSURI (determining from the EPackage) and genmodel URI pairs
     * @throws MojoExecutionException 
     */
    private void registerMetamodels() throws MojoExecutionException {
        for (Metamodel metamodel : metamodels) {
            String fqnOfEPackageClass = metamodel.getPackageClass();
            String genmodelUri = metamodel.getGenmodelUri();
            String metamodelNSURI = null;
            
            if (Strings.isNullOrEmpty(fqnOfEPackageClass) && Strings.isNullOrEmpty(genmodelUri)) {
                final String msg = "For a metamodel definition, either EPackage class of Genmodel URI must be set.";
                getLog().error(msg);
                throw new MojoExecutionException(msg);
            }
            
            if (!Strings.isNullOrEmpty(fqnOfEPackageClass) && !Strings.isNullOrEmpty(genmodelUri)) {
                getLog().warn(String.format("For metamodel %s both EPackage class and genmodel are set. Using EPackage class", fqnOfEPackageClass));
            }
            
            doRegisterMetamodel(fqnOfEPackageClass, genmodelUri, metamodelNSURI);
        }
    }

    /**
     * Helper method to load a single metamodel
     */
    private void doRegisterMetamodel(String fqnOfEPackageClass, String genmodelUri, String metamodelNSURI)
            throws MojoExecutionException {
        if (!Strings.isNullOrEmpty(fqnOfEPackageClass)) {
            /*
             * Note that the side effect of this call is that the EPackage will be added to the Registry 
             */
            loadNSUriFromClass(fqnOfEPackageClass);
        }
        
        if (!Strings.isNullOrEmpty(genmodelUri) && Strings.isNullOrEmpty(fqnOfEPackageClass)) {
            String uriToLoad =  (URI.createURI(genmodelUri).isRelative())
                ? ("file://" + project.getBasedir().getAbsolutePath() + File.separator + genmodelUri)
                : genmodelUri;
            if (Strings.isNullOrEmpty(metamodelNSURI)) {
                try {
                ResourceSet set = new ResourceSetImpl();
                final Resource resource = set.getResource(URI.createURI(uriToLoad), true);
                resource.load(Maps.newHashMap());
                EcoreUtil.resolveAll(resource);
                final Iterator<GenPackage> it = Iterators.filter(resource.getAllContents(), GenPackage.class);
                while (it.hasNext()) {
                    final GenPackage genPackage = it.next();
                    final EPackage ecorePackage = genPackage.getEcorePackage();
                    EPackage.Registry.INSTANCE.put(ecorePackage.getNsURI(), ecorePackage);
                    MavenBuilderGenmodelLoader.addGenmodel(ecorePackage.getNsURI(), uriToLoad);
                }
                } catch (Exception e) {
                    final String msg = "Error while loading metamodel specification from " + uriToLoad;
                    getLog().error(msg);
                    throw new MojoExecutionException(msg, e);
                }
            } else {
                MavenBuilderGenmodelLoader.addGenmodel(metamodelNSURI, uriToLoad);
            }
        }
    }

    /**
     * Returns the nsUri of the EPackage loaded from the class for the given qualified name.<br/>
     * 
     * IMPORTANT: as a side effect, the EPackage will be added to the {@link EPackage.Registry}! 
     * 
     * @param fqnOfEPackageClass the qualified name of the EPackage class
     * @return the nsUri of the EPackage that was loaded
     * @throws MojoExecutionException if the EPackage cannot be loaded or processed
     */
    private String loadNSUriFromClass(String fqnOfEPackageClass) throws MojoExecutionException {
        try {
            Class<?> ePackageClass = Class.forName(fqnOfEPackageClass);
            
            Field instanceField = ePackageClass.getDeclaredField("eINSTANCE");

            Class<?> instanceFieldType = instanceField.getType();

            if (ePackageClass != instanceFieldType) {
                getLog().error(
                        "eINSTANCE is not type of " + fqnOfEPackageClass + " in class " + fqnOfEPackageClass
                                + ". It's type of " + instanceFieldType.getCanonicalName() + ".");
                throw new MojoExecutionException("Execution failed due to wrong type of eINSTANCE");
            }
            
            // This call will load the EPackage into the Registry!  
            EPackage ePackage = (EPackage)instanceField.get(null);
            return ePackage.getNsURI();

        } catch (ClassNotFoundException e) {
            getLog().error("Couldn't find class " + fqnOfEPackageClass + " on the classpath.");
            throw new MojoExecutionException("Execution failed due to wrong classname.", e);
        } catch (NoSuchFieldException e) {
            getLog().error("The " + fqnOfEPackageClass + " class doesn't have eINSTANCE field.");
            throw new MojoExecutionException("Execution failed due to wrong classname.", e);
        } catch (Exception e) {
            throw new MojoExecutionException("Error while loading EPackage " + fqnOfEPackageClass, e);
        }
    }

    /**
     * To register genmodel extension and according factory to the extension factory
     */
    private void registerGenmodelExtension() {
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl());
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("genmodel", new EcoreResourceFactoryImpl());
        GenModelPackage.eINSTANCE.eClass();
    }
}
