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

package eu.arrowhead.serviceorchestration.service.model;

import java.util.List;
import java.util.Map;

public class SimpleOrchestrationRequest {

    //=================================================================================================
    // members

    String serviceDefinition;
    List<String> preferredProviders;
    Map<String, Boolean> orchestrationFlags;

    //=================================================================================================
    // boilerplate

    //-------------------------------------------------------------------------------------------------
    public SimpleOrchestrationRequest() {
    }

    //-------------------------------------------------------------------------------------------------
    public SimpleOrchestrationRequest(final String serviceDefinition, final List<String> preferredProviders, final Map<String, Boolean> orchestrationFlags) {
        this.serviceDefinition = serviceDefinition;
        this.preferredProviders = preferredProviders;
        this.orchestrationFlags = orchestrationFlags;
    }

    //-------------------------------------------------------------------------------------------------
    public String getServiceDefinition() {
        return serviceDefinition;
    }

    //-------------------------------------------------------------------------------------------------
    public void setServiceDefinition(String serviceDefinition) {
        this.serviceDefinition = serviceDefinition;
    }

    //-------------------------------------------------------------------------------------------------
    public Map<String, Boolean> getOrchestrationFlags() {
        return orchestrationFlags;
    }

    //-------------------------------------------------------------------------------------------------
    public void setOrchestrationFlags(Map<String, Boolean> orchestrationFlags) {
        this.orchestrationFlags = orchestrationFlags;
    }

    //-------------------------------------------------------------------------------------------------
    public List<String> getPreferredProviders() {
        return preferredProviders;
    }

    //-------------------------------------------------------------------------------------------------
    public void setPreferredProviders(List<String> preferredProviders) {
        this.preferredProviders = preferredProviders;
    }
}
