# Contributing to Eclipse VIATRA

Thanks for your interest in this project.

## Project description

The Eclipse VIATRA™ framework supports the development of model
transformations with specific focus on event-driven, reactive transformations.
Building upon the incremental query support, VIATRA offers a language to define
transformations and a reactive transformation engine to execute certain
transformations upon changes in the underlying model. The VIATRA project
provides: An incremental query engine together with a graph pattern based
language to specify and execute model queries efficiently. An internal DSL over
the Xtend language to specify both batch and event-driven, reactive
transformations. A rule-based design space exploration framework to explore
design candidates as models satisfying multiple criteria. A model obfuscator to
remove sensitive information from a confidential model (e.g. to create bug
reports). The current VIATRA project is a full rewrite of the previous VIATRA2
framework, now with full compatibility and support for EMF models. The project
features a History wiki page that describes the main differences between the
different versions.  

* https://projects.eclipse.org/projects/modeling.viatra

## Terms of Use

This repository is subject to the Terms of Use of the Eclipse Foundation

* https://www.eclipse.org/legal/termsofuse.php

## Developer resources

VIATRA uses Oomph to set up a development environment in Eclipse
(https://wiki.eclipse.org/VIATRA/DeveloperDocumentation/DevEnvironment)
and uses Maven to build the contents (https://wiki.eclipse.org/VIATRA/DeveloperDocumentation/BuildConfig).

Information regarding source code management, builds, coding standards, and
more.

* https://projects.eclipse.org/projects/modeling.viatra/developer

The project maintains the following source code repositories

* https://github.com/eclipse-viatra/org.eclipse.viatra
* https://github.com/eclipse-viatra/org.eclipse.viatra.examples
* https://github.com/eclipse-viatra/org.eclipse.viatra.modelobfuscator
* https://github.com/eclipse-viatra/viatra-website

## Eclipse Development Process

This Eclipse Foundation open project is governed by the Eclipse Foundation
Development Process and operates under the terms of the Eclipse IP Policy.

* https://eclipse.org/projects/dev_process
* https://www.eclipse.org/org/documents/Eclipse_IP_Policy.pdf

## Eclipse Contributor Agreement

Before your contribution can be accepted by the project team contributors must
electronically sign the Eclipse Contributor Agreement (ECA).

* http://www.eclipse.org/legal/ECA.php

Commits that are provided by non-committers must have a Signed-off-by field in
the footer indicating that the author is aware of the terms by which the
contribution has been provided to the project. The non-committer must
additionally have an Eclipse Foundation account and must have a signed Eclipse
Contributor Agreement (ECA) on file.

For more information, please see the Eclipse Committer Handbook:
https://www.eclipse.org/projects/handbook/#resources-commit

## Contact

Contact the project developers via the project's Github discussion feature:
* https://github.com/eclipse-viatra/org.eclipse.viatra/discussions

Furthermore, a mailing list is maintained by Eclipse Foundation used by the developers of the projects:
* https://dev.eclipse.org/mailman/listinfo/viatra-dev

## Building VIATRA

VIATRA uses Maven/Tycho for continuous integration builds. It is executed in two passes:

1. In the first pass, a selected subset of projects (the so-called "core" projects) are built. These are Eclipse-independent and include the Maven plugin for generating code from VIATRA Query patterns. This is required to allow projects to reuse the latest compiler version; on the other hand, the projects compiled here are pushed into Maven repositories as well.
   * Execution: ```mvn clean install -f releng/org.eclipse.viatra.parent.core/pom.xml```
2. In the second pass, the remaining projects are built, mostly Eclipse-specific. These projects may also use the VIATRA Query Maven compiler to generate pattern matcher code.
  * ```mvn clean install -f releng/org.eclipse.viatra.parent.all/pom.xml```

Important: executing the second pass also rebuilds all project from the first pass. When the core language projects, required for the Maven plugin have not changed since the last local build, the first pass can be skipped.

## Development Environment Setup

### Installation with the Oomph Installer

The manual process of setting up a development environment (described below) can be automated with Oomph.

* Download and run [Eclipse Installer](https://github.com/eclipse-oomph/oomph)
* Use the advanced mode
* Product related settings:
  * Product: Eclipse IDE for Eclipse Committers (or alternatively, Eclipse Modeling Tools)
  * Product Version: Latest Release
  * Java version: Select one accordingly
  * Bundle pool: Recommended
* Project selection:
  * Catalog: Eclipse Projects
  * Select the **VIATRA** project
  * Optionally, also select VIATRA Tooling subproject if you want to install the last VIATRA Query SDK from the Jenkins build (not recommended when updating the compiler capabilities of VIATRA)
  * Do not select the discontinued **VIATRA Core** and **DSE** project, they will be removed in the future
* VIATRA-specific variables (check show all variables if these are missing)  
  * Copyright owner (VIATRA): enter your employer or those who should be mentioned as copyright holders in new file comments (e.g. your employer). You don't have to add your name, as that is already inserted from the ${user} variable by Eclipse.
  * Installation location rule: arbitrary
  * Installation folder name: arbitrary
  * Root install folder: arbitrary
  * Workspace location rule: leave default
  * Git clone location rule: leave default
  * Target Platform: select the latest Platform version available, unless a specific version is necessary
  * Git or Gerrit repository: Git (read-write) is recommended
  * Java installations: make sure to have the minimum Java version required for VIATRA installed locally (in August 2024 Java 11) to avoid linking to incorrect Java versions and selected in the wizard.

After these values are provided, the installer will create a new Eclipse instance where it downloads all dependencies and runs bootstrap launch configurations to ensure all generated code is available in the repository. When the bootstrap launch configs run, you may be asked whether to proceed with launching with projects with errors. You can safely proceed, the executed generator workflows should fix these errors.

### Important settings

#### Editor Settings

Code style: slightly modified built-in Eclipse style

* line width: 120 characters both for code and comments
* using spaces for indentation

Downloadable style available from [viatra-jdt-template.xml](releng/org.eclipse.viatra.setup/viatra-jdt-template.xml)

Comment templates:

* Copyright header for each Java file
* Make default Javadoc empty for overridden methods

Downloadable template available from [](releng/org.eclipse.viatra.setup/viatra-jdt-preferences.xml)


#### Bootstrapping metamodel and grammar code generation

If for any reason the automatic execution of the code generators fail, you can retry them by running the following launch configurations (the displayed order takes their dependencies into consideration):

* GenerateReteRecipeMetamodel.mwe2 (Query component)
* GenerateNotationMetamodel.mwe2 (Addon component, viewers)
* GenerateTraceabilityMetamodel.mwe2 (Transformation component, view model transformation)
* GenerateTransformationTraceMetamodel.mwe2 (Transformation component, debugger)
* GeneratePatternLanguage.mwe2 (Query component)
* GenerateEMFPatternLanguage.mwe2 (Query component)
* GenerateGeneratorModel.mwe2 (Query component)

Explanation: The projects contain both manually written and generated code. They are placed in different source folders: all src folders contain only manually written code, the following folders contain generated code:

* The emf-gen and src-gen folders contain code generated by the EMF metamodel generation workflows.
* The src-gen folders contain code generated by the Xtext code generation workflows.
* The xtend-gen files contain code generated by the Xtend compiler which is run incrementally. These files do have dependencies of the other workflow-generated files: before the workflow is executed, it is normal for these files to be erroneous.

Generated code is not committed into Git, so the generator workflows have to be run during setup or whenever a metamodel, grammar or workflow file changes.

Xtext code generator workflows require about 30-60 seconds to run - be patient.

#### Bootstrapping query code generation

Some components already include VIATRA Query patterns but the generated code is not stored in version control. In order to correctly compile these projects, run the Bootstrap launch configuration and import the following projects:

* org.eclipse.viatra.addon.viewers.runtime
* org.eclipse.viatra.addon.viewers.runtime.zest
* org.eclipse.viatra.integration.uml
* org.eclipse.viatra.query.tooling.ui.retevis
* org.eclipse.viatra.transformation.views

Notes:

* If you create an own launch configuration for bootstrapping query code generation, make sure to exclude these imported projects from it, and either include the metamodel projects required by them in it or import them into the runtime workspace as well.
* If VIATRA SDK was installed using the VIATRA Tooling sub-project, executing these configurations is not necessary, the code will be generated automatically when the queries are changed.