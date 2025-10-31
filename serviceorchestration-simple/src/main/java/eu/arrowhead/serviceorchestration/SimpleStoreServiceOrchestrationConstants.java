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
package eu.arrowhead.serviceorchestration;

public final class SimpleStoreServiceOrchestrationConstants {

	//=================================================================================================
	// members

	public static final String SYSTEM_NAME = "serviceorchestration-simple";

	// DB
	public static final String DATABASE_ENTITY_PACKAGE = "eu.arrowhead.serviceorchestration.jpa.entity";
	public static final String DATABASE_REPOSITORY_PACKAGE = "eu.arrowhead.serviceorchestration.jpa.repository";

	// HTTP API
	public static final String HTTP_API_BASE_PATH = "/serviceorchestration";
	public static final String HTTP_API_MONITOR_PATH = HTTP_API_BASE_PATH + "/monitor";
	public static final String HTTP_API_GENERAL_MANAGEMENT_PATH = HTTP_API_BASE_PATH + "/general/mgmt";
	public static final String HTTP_API_ORCHESTRATION_PATH = HTTP_API_BASE_PATH + "/orchestration";
	public static final String HTTP_API_ORCHESTRATION_MGMT_PREFIX = HTTP_API_ORCHESTRATION_PATH + "/mgmt";
	public static final String HTTP_API_ORCHESTRATION_STORE_MANAGEMENT_PATH = HTTP_API_ORCHESTRATION_MGMT_PREFIX + "/simple-store";

	// operation
	public static final String HTTP_API_OP_CREATE_PATH = "/create";
	public static final String HTTP_API_OP_QUERY_PATH = "/query";
	public static final String HTTP_API_OP_MODIFY_PRIORITIES_PATH = "/modify-priorities";
	public static final String HTTP_API_OP_REMOVE_PATH = "/remove";
	public static final String HTTP_API_OP_PULL_PATH = "/pull";

	// for ignore related warnings
	public static final String IGNORED_FIELDS = "ignored fields";
	public static final String IGNORED_FLAGS = "ignored flags";
	public static final String FIELD_OPERATIONS = " operations";
	public static final String FIELD_ALIVES_AT = " alives at";
	public static final String FIELD_METADATA_REQ = " metadataRequirements";
	public static final String FIELD_INTF_TEMPLATE_NAMES = " interfaceTemplateNames";
	public static final String FIELD_INTF_ADDRESS_TYPES = " interfaceAddressTypes";
	public static final String FIELD_INTF_PROP_REQ = " interfacePropertyRequirements";
	public static final String FIELD_SECURITY_POLICIES = " securityPolicies";
	public static final String FIELD_PREFFERED_PROVIDERS = " preferredProviders";
	public static final String FIELD_QOS_REQ = " qosRequirements";
	public static final String FIELD_EXCLUSIVITY_DURATION = " exclusivityDuration";

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private SimpleStoreServiceOrchestrationConstants() {
		throw new UnsupportedOperationException();
	}
}
