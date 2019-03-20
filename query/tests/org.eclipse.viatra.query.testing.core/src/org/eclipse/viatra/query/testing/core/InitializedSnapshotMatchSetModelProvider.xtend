/** 
 * Copyright (c) 2010-2015, Peter Lunk, Grill Balazs, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.viatra.query.testing.core

import org.eclipse.emf.ecore.resource.ResourceSet
import org.eclipse.viatra.query.runtime.api.IPatternMatch
import org.eclipse.viatra.query.runtime.api.IQuerySpecification
import org.eclipse.viatra.query.runtime.api.ViatraQueryMatcher
import org.eclipse.viatra.query.runtime.emf.EMFScope
import org.eclipse.viatra.query.testing.snapshot.MatchSetRecord
import org.eclipse.viatra.query.testing.snapshot.QuerySnapshot

/** 
 * 
 * Fetches Snapshot match elements for a given QuerySpecification based on the specified QuerySnaphot instances.</p>
 * 
 * <b>Note</b> If no query scope is set up explicitly, the resource set of the snapshot model will be used as a test model scope.
 * 
 * @since 1.5.2
 */
class InitializedSnapshotMatchSetModelProvider implements IMatchSetModelProvider {
    final Iterable<QuerySnapshot> qsnap
    
    new(QuerySnapshot ... qsnap){
        this.qsnap = qsnap
    }

    override <Match extends IPatternMatch> MatchSetRecord getMatchSetRecord(EMFScope scope,
        IQuerySpecification<? extends ViatraQueryMatcher<Match>> querySpecification,
        Match filter) {
        
        val FQN = querySpecification.getFullyQualifiedName()
        
        return getMatchSetRecordsFromSnapshot(qsnap, FQN)
    }

    override updatedByModify() {
        false
    }

    override <Match extends IPatternMatch> getMatchSetRecord(ResourceSet resourceSet,
        IQuerySpecification<? extends ViatraQueryMatcher<Match>> querySpecification,
        Match filter) {
        val FQN = querySpecification.getFullyQualifiedName()
        
        return getMatchSetRecordsFromSnapshot(qsnap, FQN)
    }

    override dispose() {}

    private def <Match extends IPatternMatch> MatchSetRecord getMatchSetRecordsFromSnapshot(
        Iterable<QuerySnapshot> snapshot, String FQN) {
        if(snapshot.empty) throw new IllegalArgumentException("The provided scope does not contain a Snapshot model")
        val record = (snapshot).head?.matchSetRecords.findFirst[FQN == it.patternQualifiedName]
        if (record === null)
            throw new IllegalArgumentException("Could not find snapshot for " + FQN + " in the provided scope")
        record
    }

}