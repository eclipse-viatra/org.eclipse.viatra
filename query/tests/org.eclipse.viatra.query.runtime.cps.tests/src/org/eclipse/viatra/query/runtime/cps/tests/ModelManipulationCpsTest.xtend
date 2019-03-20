/*******************************************************************************
 * Copyright (c) 2014-2016 Akos Horvath, Abel Hegedus, Akos Menyhert, Tamas Borbas, Marton Bur, Zoltan Ujhelyi, Daniel Segesdi, Gabor Bergmann, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.viatra.query.runtime.cps.tests

import org.eclipse.emf.ecore.util.EcoreUtil
import org.eclipse.viatra.examples.cps.cyberPhysicalSystem.ApplicationInstance
import org.eclipse.viatra.examples.cps.cyberPhysicalSystem.ApplicationType
import org.eclipse.viatra.examples.cps.cyberPhysicalSystem.CyberPhysicalSystem
import org.eclipse.viatra.examples.cps.cyberPhysicalSystem.CyberPhysicalSystemFactory
import org.eclipse.viatra.examples.cps.cyberPhysicalSystem.HostInstance
import org.eclipse.viatra.examples.cps.cyberPhysicalSystem.HostType
import org.eclipse.viatra.examples.cps.cyberPhysicalSystem.State
import org.eclipse.viatra.query.runtime.cps.tests.queries.util.ApplicationInstancesIdentifiersQuerySpecification
import org.eclipse.viatra.query.runtime.cps.tests.queries.util.ApplicationInstancesOfApplicationTypeQuerySpecification
import org.eclipse.viatra.query.runtime.cps.tests.queries.util.ApplicationInstancesQuerySpecification
import org.eclipse.viatra.query.runtime.cps.tests.queries.util.ApplicationTypeWithHostedInstancesQuerySpecification
import org.eclipse.viatra.query.runtime.cps.tests.queries.util.ApplicationTypeWithoutHostedInstanceQuerySpecification
import org.eclipse.viatra.query.runtime.cps.tests.queries.util.ApplicationTypesIdentifiersQuerySpecification
import org.eclipse.viatra.query.runtime.cps.tests.queries.util.CommunicateWithQuerySpecification
import org.eclipse.viatra.query.runtime.cps.tests.queries.util.FinalPatternQuerySpecification
import org.eclipse.viatra.query.runtime.cps.tests.queries.util.HasTheMostHostedApplicationInstancesQuerySpecification
import org.eclipse.viatra.query.runtime.cps.tests.queries.util.HasTheMostHostedApplicationsQuerySpecification
import org.eclipse.viatra.query.runtime.cps.tests.queries.util.HostInstanceWithAtLeastAsMuchTotalRamAsTotalHddQuerySpecification
import org.eclipse.viatra.query.runtime.cps.tests.queries.util.HostInstancesWithZeroTotalRamQuerySpecification
import org.eclipse.viatra.query.runtime.cps.tests.queries.util.HostedApplicationsQuerySpecification
import org.eclipse.viatra.query.runtime.cps.tests.queries.util.InTheCommunicationChainsQuerySpecification
import org.eclipse.viatra.query.runtime.cps.tests.queries.util.InstancesQuerySpecification
import org.eclipse.viatra.query.runtime.cps.tests.queries.util.TransitionsOfApplicationTypeIdentifiersQuerySpecification
import org.eclipse.viatra.query.runtime.cps.tests.queries.util.TransitionsOfApplicationTypeQuerySpecification
import org.eclipse.viatra.query.testing.core.api.ViatraQueryTest
import org.junit.Test
import org.eclipse.viatra.query.testing.core.ModelLoadHelper
import org.eclipse.emf.ecore.resource.ResourceSet
import org.junit.Before
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl
import org.eclipse.viatra.query.runtime.emf.EMFScope

class ModelManipulationCpsTest {
    public static val SNAPSHOT_PATH = "org.eclipse.viatra.query.runtime.cps.tests/models/snapshots/test.snapshot"
    
    
    var ResourceSet set
    var EMFScope scope

    extension ModelLoadHelper = new ModelLoadHelper
    
    @Before
    def void initialize() {
        set = new ResourceSetImpl
        set.loadAdditionalResourceFromUri("org.eclipse.viatra.query.runtime.cps.tests/models/instances/demo.cyberphysicalsystem")
        scope = new EMFScope(set)
    }

    @Test
    def void test_newAppInstance() {
        ViatraQueryTest.test(ApplicationInstancesQuerySpecification.instance)
                        .and(ApplicationInstancesOfApplicationTypeQuerySpecification.instance)
                        .on(scope)
                        .with(BackendType.Rete.newBackendInstance)
                        .with(set.loadExpectedResultsFromUri(SNAPSHOT_PATH))
                        .assumeInputs
                        .assertEqualsThen
                        .modify(ApplicationType,
                            [true],
                            [appType |
                                // MODEL MODIFICATION HERE
                                // add a new Application Instance to all Application Types in the model
                                CyberPhysicalSystemFactory::eINSTANCE.createApplicationInstance => [
                                    it.identifier = appType.identifier+".instNew"
                                    it.type = appType
                                ]
                            ])
                        .with(set.loadExpectedResultsFromUri("org.eclipse.viatra.query.runtime.cps.tests/models/snapshots/test_newAppInstance.snapshot"))
                        .assertEquals
    }

    @Test
    def void test_changeAppInstanceIdentifier() {
        ViatraQueryTest.test(ApplicationInstancesIdentifiersQuerySpecification.instance)
                        .on(scope)
                        .with(BackendType.Rete.newBackendInstance)
                        .with(BackendType.LocalSearch.newBackendInstance)
                        .with(set.loadExpectedResultsFromUri(SNAPSHOT_PATH))
                        .assumeInputs
                        .assertEqualsThen
                        .modify(ApplicationInstance,
                            [it.identifier == "simple.cps.app.FirstAppClass0.inst1"],
                            [appInst |
                                // MODEL MODIFICATION HERE
                                // change the Application Instance "simple.cps.app.FirstAppClass0.inst1" 
                                // identifier to "simple.cps.app.FirstAppClass0.instModified"
                                appInst.identifier = "simple.cps.app.FirstAppClass0.instModified"
                            ])
                        .with(set.loadExpectedResultsFromUri("org.eclipse.viatra.query.runtime.cps.tests/models/snapshots/test_changeAppInstanceIdentifier.snapshot"))
                        .assertEquals
    }

    @Test
    def void test_changeAppTypeIdentifier() {
        ViatraQueryTest.test(ApplicationTypesIdentifiersQuerySpecification.instance)
                        .on(scope)
                        .with(BackendType.Rete.newBackendInstance)
                        .with(BackendType.LocalSearch.newBackendInstance)
                        .with(set.loadExpectedResultsFromUri(SNAPSHOT_PATH))
                        .assumeInputs
                        .assertEqualsThen
                        .modify(ApplicationType,
                            [it.identifier == "simple.cps.app.FirstAppClass0"],
                            [appType |
                                // MODEL MODIFICATION HERE
                                // change the Application Type "simple.cps.app.FirstAppClass0"
                                // identifier to "simple.cps.app.FirstAppClassModified"
                                appType.identifier = "simple.cps.app.FirstAppClassModified"
                            ])
                        .with(set.loadExpectedResultsFromUri("org.eclipse.viatra.query.runtime.cps.tests/models/snapshots/test_changeAppTypeIdentifier.snapshot"))
                        .assertEquals
    }
    
    @Test
    def void test_deleteAppInstance() {
        ViatraQueryTest.test(ApplicationInstancesQuerySpecification.instance)
                        .on(scope)
                        .with(BackendType.Rete.newBackendInstance)
                        .with(BackendType.LocalSearch.newBackendInstance)
                        .with(set.loadExpectedResultsFromUri(SNAPSHOT_PATH))
                        .assumeInputs
                        .assertEqualsThen
                        .modify(ApplicationInstance,
                            [it.identifier == "simple.cps.app.FirstAppClass0.inst0"],
                            [appInst |
                                // MODEL MODIFICATION HERE
                                // delete the Application Instance "simple.cps.app.FirstAppClass0.inst1"
                                EcoreUtil.delete(appInst)
                            ])
                        .with(set.loadExpectedResultsFromUri("org.eclipse.viatra.query.runtime.cps.tests/models/snapshots/test_deleteAppInstance.snapshot"))
                        .assertEquals
    }

    @Test
    def void test_deleteAppType() {
        ViatraQueryTest.test(ApplicationTypesIdentifiersQuerySpecification.instance)
                        .on(scope)
                        .with(BackendType.Rete.newBackendInstance)
                        .with(BackendType.LocalSearch.newBackendInstance)
                        .with(set.loadExpectedResultsFromUri(SNAPSHOT_PATH))
                        .assumeInputs
                        .assertEqualsThen
                        .modify(ApplicationType,
                            [it.identifier == "simple.cps.app.FirstAppClass0"],
                            [appType |
                                // MODEL MODIFICATION HERE
                                // delete the Application Type "simple.cps.app.FirstAppClass0"
                                EcoreUtil.delete(appType)
                            ])
                        .with(set.loadExpectedResultsFromUri("org.eclipse.viatra.query.runtime.cps.tests/models/snapshots/test_deleteAppType.snapshot"))
                        .assertEquals
    }
    
    @Test
    def void test_newHostInstance() {
        ViatraQueryTest.test(CommunicateWithQuerySpecification.instance)
                        .on(scope)
                        .with(BackendType.Rete.newBackendInstance)
                        .with(BackendType.LocalSearch.newBackendInstance)
                        .with(set.loadExpectedResultsFromUri(SNAPSHOT_PATH))
                        .assumeInputs
                        .assertEqualsThen
                        .modify(HostInstance,
                            [it.identifier == "simple.cps.host.FirstHostClass0.inst0"],
                            [hostInst |
                                // MODEL MODIFICATION HERE
                                // add a new Host Instance as communication partner to
                                // Host Instance "simple.cps.host.FirstHostClass0.inst0"
                                CyberPhysicalSystemFactory::eINSTANCE.createHostInstance => [
                                    it.identifier = "simple.cps.host.FirstHostClass0.instNew"
                                    it.nodeIp = "simple.cps.host.FirstHostClass0.instNew"
                                    (hostInst.eContainer as HostType).instances += it
                                    it.communicateWith += hostInst
                                ]
                            ])
                        .with(set.loadExpectedResultsFromUri("org.eclipse.viatra.query.runtime.cps.tests/models/snapshots/test_newHostInstance.snapshot"))
                        .assertEquals
    }
    
    @Test
    def void test_changeAppInstanceAllocationLocation() {
        ViatraQueryTest.test(HostedApplicationsQuerySpecification.instance)
                        .on(scope)
                        .with(BackendType.Rete.newBackendInstance)
                        .with(BackendType.LocalSearch.newBackendInstance)
                        .with(set.loadExpectedResultsFromUri(SNAPSHOT_PATH))
                        .assumeInputs
                        .assertEqualsThen
                        .modify(ApplicationInstance,
                            [it.identifier == "simple.cps.app.FirstAppClass0.inst0"],
                            [appInst |
                                // MODEL MODIFICATION HERE
                                val hostInst = appInst.eResource
                                                        .allContents
                                                        .filter(HostInstance)
                                                        .findFirst[it.identifier == "simple.cps.host.SecondHostClass0.inst0"]
                                appInst.allocatedTo = hostInst
                            ])
                        .with(set.loadExpectedResultsFromUri("org.eclipse.viatra.query.runtime.cps.tests/models/snapshots/test_changeAppInstanceAllocationLocation.snapshot"))
                        .assertEquals
    }
    
    @Test
    def void test_changeAppInstanceType() {
        ViatraQueryTest.test(ApplicationTypeWithHostedInstancesQuerySpecification.instance)
                        .on(scope)
                        .with(BackendType.Rete.newBackendInstance)
                        .with(BackendType.LocalSearch.newBackendInstance)
                        .with(set.loadExpectedResultsFromUri(SNAPSHOT_PATH))
                        .assumeInputs
                        .assertEqualsThen
                        .modify(ApplicationInstance,
                            [it.identifier == "simple.cps.app.FirstAppClass0.inst1"],
                            [appInst |
                                // MODEL MODIFICATION HERE
                                val appType = appInst.eResource
                                                        .allContents
                                                        .filter(ApplicationType)
                                                        .findFirst[it.identifier == "simple.cps.app.SecondAppClass0"]
                                appInst.type = appType
                            ])
                        .with(set.loadExpectedResultsFromUri("org.eclipse.viatra.query.runtime.cps.tests/models/snapshots/test_changeAppInstanceType.snapshot"))
                        .assertEquals
    }
    
    @Test
    def void test_newTransition() {
        ViatraQueryTest.test(TransitionsOfApplicationTypeIdentifiersQuerySpecification.instance)
                        .on(scope)
                        .with(BackendType.Rete.newBackendInstance)
                        .with(BackendType.LocalSearch.newBackendInstance)
                        .with(set.loadExpectedResultsFromUri(SNAPSHOT_PATH))
                        .assumeInputs
                        .assertEqualsThen
                        .modify(State,
                            [it.identifier == "simple.cps.app.FirstAppClass0.sm0.s1"],
                            [state |
                                // MODEL MODIFICATION HERE
                                CyberPhysicalSystemFactory::eINSTANCE.createTransition => [
                                    it.identifier = "simple.cps.app.FirstAppClass0.sm0.s1.tNew"
                                    it.action = "Dummy Action"
                                    state.outgoingTransitions += it
                                ]
                            ])
                        .with(set.loadExpectedResultsFromUri("org.eclipse.viatra.query.runtime.cps.tests/models/snapshots/test_newTransition.snapshot"))
                        .assertEquals
    }
    
    @Test
    def void test_deleteHostType() {
        ViatraQueryTest.test(InTheCommunicationChainsQuerySpecification.instance)
                        .on(scope)
                        .with(BackendType.Rete.newBackendInstance)
                        .with(BackendType.LocalSearch.newBackendInstance)
                        .with(set.loadExpectedResultsFromUri(SNAPSHOT_PATH))
                        .assumeInputs
                        .assertEqualsThen
                        .modify(HostType,
                            [it.identifier == "simple.cps.host.FirstHostClass0"],
                            [ EcoreUtil.delete(it, true) ] )
                        .with(set.loadExpectedResultsFromUri("org.eclipse.viatra.query.runtime.cps.tests/models/snapshots/test_deleteHostType.snapshot"))
                        .assertEquals
    }
    
    @Test
    def void test_newHostInstanceWithMoreRamThanHdd() {
        ViatraQueryTest.test(HostInstanceWithAtLeastAsMuchTotalRamAsTotalHddQuerySpecification.instance)
                        .on(scope)
                        .with(BackendType.Rete.newBackendInstance)
                        .with(BackendType.LocalSearch.newBackendInstance)
                        .with(set.loadExpectedResultsFromUri(SNAPSHOT_PATH))
                        .assumeInputs
                        .assertEqualsThen
                        .modify(HostType,
                            [it.identifier == "simple.cps.host.FirstHostClass0"],
                            [hostType |
                                // MODEL MODIFICATION HERE
                                CyberPhysicalSystemFactory::eINSTANCE.createHostInstance => [
                                    it.identifier = "simple.cps.host.FirstHostClass0.instNew"
                                    it.totalRam = 2
                                    it.totalHdd = 1
                                    hostType.instances += it
                                ]
                            ])
                        .with(set.loadExpectedResultsFromUri("org.eclipse.viatra.query.runtime.cps.tests/models/snapshots/test_newHostInstanceWithMoreRamThanHdd.snapshot"))
                        .assertEquals
    }
    
    @Test
    def void test_newHostInstanceWithMoreHddThanRam() {
        ViatraQueryTest.test(HostInstanceWithAtLeastAsMuchTotalRamAsTotalHddQuerySpecification.instance)
                        .on(scope)
                        .with(BackendType.Rete.newBackendInstance)
                        .with(BackendType.LocalSearch.newBackendInstance)
                        .with(set.loadExpectedResultsFromUri(SNAPSHOT_PATH))
                        .assumeInputs
                        .assertEqualsThen
                        .modify(HostType,
                            [it.identifier == "simple.cps.host.FirstHostClass0"],
                            [hostType |
                                // MODEL MODIFICATION HERE
                                CyberPhysicalSystemFactory::eINSTANCE.createHostInstance => [
                                    it.identifier = "simple.cps.host.FirstHostClass0.instNew"
                                    it.nodeIp = "simple.cps.host.FirstHostClass0.instNew"
                                    it.totalRam = 1
                                    it.totalHdd = 2
                                    hostType.instances += it
                                ]
                            ])
                        .with(set.loadExpectedResultsFromUri("org.eclipse.viatra.query.runtime.cps.tests/models/snapshots/test_newHostInstanceWithMoreHddThanRam.snapshot"))
                        .assertEquals
    }
    
    @Test
    def void test_deleteHostInstanceWithTheMostHostedApplication() {
        ViatraQueryTest.test(FinalPatternQuerySpecification.instance)
                        .on(scope)
                        .with(BackendType.Rete.newBackendInstance)
                        .with(BackendType.LocalSearch.newBackendInstance)
                        .with(set.loadExpectedResultsFromUri(SNAPSHOT_PATH))
                        .assumeInputs
                        .assertEqualsThen
                        .modify(HostInstance,
                            [it.identifier == "simple.cps.host.SecondHostClass0.inst1"],
                            [ EcoreUtil.delete(it) ] )
                        .with(set.loadExpectedResultsFromUri("org.eclipse.viatra.query.runtime.cps.tests/models/snapshots/test_deleteHostInstanceWithTheMostHostedApplication.snapshot"))
                        .assertEquals
    }
    
    @Test
    def void test_deleteAllHostType() {
        ViatraQueryTest.test(ApplicationTypeWithHostedInstancesQuerySpecification.instance)
        .and(HostInstancesWithZeroTotalRamQuerySpecification.instance)
        .and(InTheCommunicationChainsQuerySpecification.instance)
        .and(HasTheMostHostedApplicationsQuerySpecification.instance)
                        .on(scope)
                        .with(BackendType.Rete.newBackendInstance)
                        .with(BackendType.LocalSearch.newBackendInstance)
                        .with(set.loadExpectedResultsFromUri(SNAPSHOT_PATH))
                        .assumeInputs
                        .assertEqualsThen
                        .modify(HostType,
                            [true],
                            [ EcoreUtil.delete(it) ] )
                        .with(set.loadExpectedResultsFromUri("org.eclipse.viatra.query.runtime.cps.tests/models/snapshots/test_deleteAllHostType.snapshot"))
                        .assertEquals
    }
    
    @Test
    def void test_deleteAllHostInstance() {
        ViatraQueryTest.test(ApplicationTypeWithoutHostedInstanceQuerySpecification.instance)
        .and(HasTheMostHostedApplicationsQuerySpecification.instance)
                        .on(scope)
                        .with(BackendType.Rete.newBackendInstance)
                        .with(BackendType.LocalSearch.newBackendInstance)
                        .with(set.loadExpectedResultsFromUri(SNAPSHOT_PATH))
                        .assumeInputs
                        .assertEqualsThen
                        .modify(HostInstance,
                            [true],
                            [ EcoreUtil.delete(it) ] )
                        .with(set.loadExpectedResultsFromUri("org.eclipse.viatra.query.runtime.cps.tests/models/snapshots/test_deleteAllHostInstance.snapshot"))
                        .assertEquals
    }
    
    @Test
    def void test_newComplexStructure() {
        ViatraQueryTest.test(HasTheMostHostedApplicationInstancesQuerySpecification.instance)
        .and(TransitionsOfApplicationTypeQuerySpecification.instance)
        .and(InstancesQuerySpecification.instance)
                        .on(scope)
                        .with(BackendType.Rete.newBackendInstance)
                        .with(BackendType.LocalSearch.newBackendInstance)
                        .with(set.loadExpectedResultsFromUri(SNAPSHOT_PATH))
                        .assumeInputs
                        .assertEqualsThen
                        .modify(CyberPhysicalSystem,
                            [true],
                            [cps |
                                // MODEL MODIFICATION HERE
                                extension val factory =  CyberPhysicalSystemFactory::eINSTANCE
                                createApplicationType => [ appType |
                                    appType.identifier = "simple.cps.app.NewAppClass0"
                                    cps.appTypes += appType
                                    createStateMachine => [ sm |
                                        sm.identifier = '''«appType.identifier».sm0'''
                                        appType.behavior = sm
                                        val s0 = createState => [ state |
                                            state.identifier = '''«sm.identifier».s0'''
                                            sm.states += state
                                            sm.initial = state
                                        ]
                                        val s1 = createState => [ state |
                                            state.identifier = '''«sm.identifier».s1'''
                                            sm.states += state
                                        ]
                                        createTransition => [ t |
                                            t.identifier = '''«s0.identifier».t0'''
                                            s0.outgoingTransitions += t
                                            t.targetState = s1
                                        ]
                                    ]
                                    val hostInstances = cps.hostTypes
                                                           .findFirst[it.identifier=="simple.cps.host.FirstHostClass0"]
                                                           .instances
                                    createApplicationInstance => [ appInst |
                                        appInst.identifier = '''«appType.identifier».inst0'''
                                        appInst.type = appType
                                        appInst.allocatedTo = hostInstances.findFirst[it.identifier.contains("inst0")]
                                    ]
                                    createApplicationInstance => [ appInst |
                                        appInst.identifier = '''«appType.identifier».inst1'''
                                        appInst.type = appType
                                        appInst.allocatedTo = hostInstances.findFirst[it.identifier.contains("inst1")]
                                    ]
                                    createApplicationInstance => [ appInst |
                                        appInst.identifier = '''«appType.identifier».inst2'''
                                        appInst.type = appType
                                        appInst.allocatedTo = hostInstances.findFirst[it.identifier.contains("inst2")]
                                    ]
                                ]
                            ])
                        .with(set.loadExpectedResultsFromUri("org.eclipse.viatra.query.runtime.cps.tests/models/snapshots/test_newComplexStructure.snapshot"))
                        .assertEquals
    }
}