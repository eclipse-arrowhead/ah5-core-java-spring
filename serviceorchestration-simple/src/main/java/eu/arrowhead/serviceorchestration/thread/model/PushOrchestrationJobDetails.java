/*******************************************************************************
 *
 * Copyright (c) 2025 AITIA
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 *
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  	AITIA - implementation
 *  	Arrowhead Consortia - conceptualization
 *
 *******************************************************************************/

package eu.arrowhead.serviceorchestration.thread.model;

import java.util.Set;
import java.util.UUID;

public class PushOrchestrationJobDetails {

    //=================================================================================================
    // members

    private final UUID jobId;
    private final Set<String> warnings;

    //=================================================================================================
    // methods

    //-------------------------------------------------------------------------------------------------
    public PushOrchestrationJobDetails(final UUID jobId, final Set<String> warnings) {
        this.jobId = jobId;
        this.warnings = warnings;
    }

    //-------------------------------------------------------------------------------------------------
    public UUID getJobId() {
        return jobId;
    }

    //-------------------------------------------------------------------------------------------------
    public Set<String> getWarnings() {
        return warnings;
    }

}
