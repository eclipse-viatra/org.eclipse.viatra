/**
 * Copyright (c) 2010-2016, Peter Lunk, IncQuery Labs Ltd.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Peter Lunk - initial API and implementation
 */
package org.eclipse.viatra.transformation.debug.communication;

import static org.eclipse.viatra.transformation.debug.communication.DebuggerCommunicationConstants.CURRENTVERSION;
import static org.eclipse.viatra.transformation.debug.communication.DebuggerCommunicationConstants.MBEANNAME;
import static org.eclipse.viatra.transformation.debug.communication.DebuggerCommunicationConstants.SUSPENDED;
import static org.eclipse.viatra.transformation.debug.communication.DebuggerCommunicationConstants.TERMINATED;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.AttributeChangeNotification;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;

import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.viatra.query.runtime.api.ViatraQueryEngine;
import org.eclipse.viatra.query.runtime.api.scope.QueryScope;
import org.eclipse.viatra.query.runtime.emf.EMFScope;
import org.eclipse.viatra.query.runtime.util.ViatraQueryLoggingUtil;
import org.eclipse.viatra.transformation.debug.DebuggerActions;
import org.eclipse.viatra.transformation.debug.TransformationDebugger;
import org.eclipse.viatra.transformation.debug.model.breakpoint.ITransformationBreakpointHandler;
import org.eclipse.viatra.transformation.debug.model.transformationstate.TransformationModelBuilder;
import org.eclipse.viatra.transformation.debug.model.transformationstate.TransformationModelElement;
import org.eclipse.viatra.transformation.debug.model.transformationstate.TransformationState;
import org.eclipse.viatra.transformation.debug.model.transformationstate.TransformationStateBuilder;
import org.eclipse.viatra.transformation.debug.transformationtrace.model.ActivationTrace;
import org.eclipse.viatra.transformation.evm.api.Activation;
import org.eclipse.viatra.transformation.evm.api.RuleSpecification;
import org.eclipse.viatra.transformation.evm.api.event.EventFilter;

public class DebuggerTargetEndpoint extends NotificationBroadcasterSupport implements IDebuggerTargetAgent, DebuggerTargetEndpointMBean{
    private static final String NOTIFICATION_TYPE = "TransformationState";
    private static final String NOTIFICATION_MSG = "State changed";
    private String ID;
    private TransformationDebugger debugger;

    
    private long sequenceNumber = 1;
    //TargetEndpoint
    public DebuggerTargetEndpoint(String ID, TransformationDebugger debugger){
        builder.setID(ID);
        this.ID = ID;
        this.debugger  = debugger;
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName name;
        try {
            name = new ObjectName(MBEANNAME+"_"+CURRENTVERSION+"_"+ID);
            mbs.registerMBean(this, name); 
        } catch (MalformedObjectNameException | InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException e) {
            ViatraQueryLoggingUtil.getDefaultLogger().error(e.getMessage(), e);
        } 
        
        
    }

    @Override
    public String getID() {
        return ID;
    }

    @Override
    public void stepForward() {
       debugger.setDebuggerAction(DebuggerActions.Step);
    }

    @Override
    public void continueExecution() {
        debugger.setDebuggerAction(DebuggerActions.Continue);
    }

    @Override
    public void setNextActivation(ActivationTrace activation) {
        debugger.setNextActivation(activation);
    }

    @Override
    public void addBreakpoint(ITransformationBreakpointHandler breakpoint) throws ViatraDebuggerException {
        debugger.addBreakpoint(breakpoint);
    }

    @Override
    public void removeBreakpoint(ITransformationBreakpointHandler breakpoint) {
        debugger.removeBreakpoint(breakpoint);
        
    }

    @Override
    public void disableBreakpoint(ITransformationBreakpointHandler breakpoint) {
        debugger.disableBreakpoint(breakpoint);
        
    }

    @Override
    public void enableBreakpoint(ITransformationBreakpointHandler breakpoint) {
       debugger.enableBreakpoint(breakpoint);
    }
    
    @Override
    public void disconnect() {
        debugger.disconnect();
    }

    
    
    //TargetAgent
    //Notifications
    TransformationModelBuilder modelBuilder = new TransformationModelBuilder();
    TransformationStateBuilder builder = new TransformationStateBuilder(modelBuilder);
    
    @Override
    public void suspended() {
        modelBuilder.reset();
        builder.setBreakpointHit(null);
        TransformationState state = builder.build();
        try {
            byte[] stateString = serializeObject(state, new ByteArrayOutputStream());
            Notification n = new AttributeChangeNotification(this,
                    sequenceNumber++, System.currentTimeMillis(),
                    NOTIFICATION_MSG, NOTIFICATION_TYPE, NOTIFICATION_TYPE,
                    stateString, stateString);

            sendNotification(n);
        } catch (IOException e) {
            ViatraQueryLoggingUtil.getDefaultLogger().error(e.getMessage(), e);
        }
       
    }

    @Override
    public void breakpointHit(ITransformationBreakpointHandler breakpoint) {
        builder.setBreakpointHit(breakpoint);
    }

    @Override
    public void terminated() throws ViatraDebuggerException {
        Notification n = new Notification(TERMINATED, this, sequenceNumber++);
        sendNotification(n);
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer(); 
        ObjectName name;
        try {
            name = new ObjectName(MBEANNAME+"_"+CURRENTVERSION+"_"+ID);
            mbs.unregisterMBean(name); 
        } catch (MalformedObjectNameException | MBeanRegistrationException | InstanceNotFoundException e) {
            throw new ViatraDebuggerException(e.getMessage());
        } 
        
    }

    @Override
    public void conflictSetChanged(Set<Activation<?>> nextActivations, Set<Activation<?>> conflictingActivations) {
        builder.setActivations(conflictingActivations, nextActivations);
        
    }

    @Override
    public void activationFired(Activation<?> activation) {
        builder.activationFired(activation);
    }

    @Override
    public void activationFiring(Activation<?> activation) {
        builder.activationFiring(activation);
    }

    @Override
    public void addedRule(RuleSpecification<?> specification, EventFilter<?> filter) {
        builder.addRule(specification, filter);
        
    }

    @Override
    public void removedRule(RuleSpecification<?> specification, EventFilter<?> filter) {
        builder.removeRule(specification, filter);
    }

    @Override
    public void nextActivationChanged(Activation<?> activation) {
        builder.nextActivationChanged(activation);
        TransformationState state = builder.build();
        try {
            byte[] stateString = serializeObject(state, new ByteArrayOutputStream());
            Notification n = new AttributeChangeNotification(this,
                    sequenceNumber++, System.currentTimeMillis(),
                    NOTIFICATION_MSG, NOTIFICATION_TYPE, NOTIFICATION_TYPE,
                    stateString, stateString);

            sendNotification(n);
        } catch (IOException e) {
            ViatraQueryLoggingUtil.getDefaultLogger().error(e.getMessage(), e);
        }
    }

    @Override
    public MBeanNotificationInfo[] getNotificationInfo() {
        String[] types = new String[]{
            AttributeChangeNotification.ATTRIBUTE_CHANGE,
            SUSPENDED,
            TERMINATED,
        };

        String name = AttributeChangeNotification.class.getName();
        String description = "An attribute of this MBean has changed";
        MBeanNotificationInfo info = 
                new MBeanNotificationInfo(types, name, description);
        return new MBeanNotificationInfo[]{info};
    }

    @Override
    public List<TransformationModelElement> getRootElements() {
        List<TransformationModelElement> list = new ArrayList<>();
        
        ViatraQueryEngine engine = debugger.getEngine();
        List<EObject> resourceElements = getResourceElements(getResources(engine));
        for (EObject eObject : resourceElements) {
            list.add(modelBuilder.getTransformationElement(eObject));
        }
        return list;
    }

    @Override
    public Map<String, List<TransformationModelElement>> getChildren(TransformationModelElement parent) {
        return modelBuilder.createChildElements(parent);
    }

    @Override
    public Map<String, List<TransformationModelElement>> getCrossReferences(TransformationModelElement parent) {
        return modelBuilder.createCrossReferenceElements(parent);
    }
    
   
    private ResourceSet[] getResources(ViatraQueryEngine engine) {
        List<ResourceSet> retVal = new ArrayList<>();
        if(engine != null){
            QueryScope scope = engine.getScope();
            if (scope instanceof EMFScope) {
                for (Notifier notifier : ((EMFScope) scope).getScopeRoots()) {
                    if (notifier instanceof ResourceSet) {
                        retVal.add((ResourceSet) notifier);
                    }
                }
            }
            return retVal.toArray(new ResourceSet[retVal.size()]);
        }else{
            return new ResourceSet[0];
        }
        
    }
    
    private List<EObject> getResourceElements(ResourceSet[] resourceSets) {
        List<EObject> list = new ArrayList<>();
        for (ResourceSet rs : resourceSets) {
            EList<Resource> resources = rs.getResources();
            for (Resource resource : resources) {
                list.addAll(resource.getContents());
            }
        }
        return list;
    }
    
    private byte[] serializeObject(Object object, ByteArrayOutputStream stream) throws IOException {
        ObjectOutputStream out = new ObjectOutputStream(stream);
        out.writeObject(object);
        byte[] stateString = stream.toByteArray();
        return stateString;
    }
    
    

}
