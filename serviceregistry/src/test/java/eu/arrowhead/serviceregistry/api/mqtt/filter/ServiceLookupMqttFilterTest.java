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
package eu.arrowhead.serviceregistry.api.mqtt.filter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.mqtt.model.MqttRequestModel;
import eu.arrowhead.dto.MqttRequestTemplate;
import eu.arrowhead.serviceregistry.ServiceRegistrySystemInfo;
import eu.arrowhead.serviceregistry.service.ServiceDiscoveryPolicy;

@ExtendWith(MockitoExtension.class)
public class ServiceLookupMqttFilterTest {

	//=================================================================================================
	// members

	@InjectMocks
	private ServiceLookupMqttFilter filter;

	@Mock
	private ServiceRegistrySystemInfo sysInfo;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testOrder() {
		assertEquals(35, filter.order());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterAttrNotSetBecauseOfPolicyMismatch() {
		when(sysInfo.getServiceDiscoveryPolicy()).thenReturn(ServiceDiscoveryPolicy.OPEN);

		final MqttRequestTemplate template = new MqttRequestTemplate("1", null, "response", 0, Map.of(), "payload");
		final MqttRequestModel request = new MqttRequestModel("test/", "operation", template);

		assertDoesNotThrow(() -> filter.doFilter(null, request));
		assertNull(request.getAttribute("restricted.service.lookup"));

		verify(sysInfo).getServiceDiscoveryPolicy();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterAttrNotSetBecauseOfBaseTopicMismatch() {
		when(sysInfo.getServiceDiscoveryPolicy()).thenReturn(ServiceDiscoveryPolicy.RESTRICTED);

		final MqttRequestTemplate template = new MqttRequestTemplate("1", null, "response", 0, Map.of(), "payload");
		final MqttRequestModel request = new MqttRequestModel("test/", "operation", template);

		assertDoesNotThrow(() -> filter.doFilter(null, request));
		assertNull(request.getAttribute("restricted.service.lookup"));

		verify(sysInfo).getServiceDiscoveryPolicy();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterAttrNotSetBecauseOfOperationMismatch() {
		when(sysInfo.getServiceDiscoveryPolicy()).thenReturn(ServiceDiscoveryPolicy.RESTRICTED);

		final MqttRequestTemplate template = new MqttRequestTemplate("1", null, "response", 0, Map.of(), "payload");
		final MqttRequestModel request = new MqttRequestModel("arrowhead/serviceregistry/service-discovery/", "operation", template);

		assertDoesNotThrow(() -> filter.doFilter(null, request));
		assertNull(request.getAttribute("restricted.service.lookup"));

		verify(sysInfo).getServiceDiscoveryPolicy();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterAttrFalseBecauseOfSysop() {
		when(sysInfo.getServiceDiscoveryPolicy()).thenReturn(ServiceDiscoveryPolicy.RESTRICTED);

		final MqttRequestTemplate template = new MqttRequestTemplate("1", null, "response", 0, Map.of(), "payload");
		final MqttRequestModel request = new MqttRequestModel("arrowhead/serviceregistry/service-discovery/", "lookup", template);
		request.setSysOp(true);

		assertDoesNotThrow(() -> filter.doFilter(null, request));
		assertFalse(Boolean.valueOf(request.getAttribute("restricted.service.lookup")));

		verify(sysInfo).getServiceDiscoveryPolicy();
		verify(sysInfo, never()).hasClientDirectAccess(anyString());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterAttrFalseBecauseOfHasDirectAccess() {
		when(sysInfo.getServiceDiscoveryPolicy()).thenReturn(ServiceDiscoveryPolicy.RESTRICTED);
		when(sysInfo.hasClientDirectAccess("TestConsumer")).thenReturn(true);

		final MqttRequestTemplate template = new MqttRequestTemplate("1", null, "response", 0, Map.of(), "payload");
		final MqttRequestModel request = new MqttRequestModel("arrowhead/serviceregistry/service-discovery/", "lookup", template);
		request.setRequester("TestConsumer");
		request.setSysOp(false);

		assertDoesNotThrow(() -> filter.doFilter(null, request));
		assertFalse(Boolean.valueOf(request.getAttribute("restricted.service.lookup")));

		verify(sysInfo).getServiceDiscoveryPolicy();
		verify(sysInfo).hasClientDirectAccess("TestConsumer");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterAttrTrue() {
		when(sysInfo.getServiceDiscoveryPolicy()).thenReturn(ServiceDiscoveryPolicy.RESTRICTED);
		when(sysInfo.hasClientDirectAccess("TestConsumer")).thenReturn(false);

		final MqttRequestTemplate template = new MqttRequestTemplate("1", null, "response", 0, Map.of(), "payload");
		final MqttRequestModel request = new MqttRequestModel("arrowhead/serviceregistry/service-discovery/", "lookup", template);
		request.setRequester("TestConsumer");
		request.setSysOp(false);

		assertDoesNotThrow(() -> filter.doFilter(null, request));
		assertTrue(Boolean.valueOf(request.getAttribute("restricted.service.lookup")));

		verify(sysInfo).getServiceDiscoveryPolicy();
		verify(sysInfo).hasClientDirectAccess("TestConsumer");
	}
}