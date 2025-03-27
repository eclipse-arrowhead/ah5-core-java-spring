package eu.arrowhead.serviceregistry.service;

public enum ServiceDiscoveryInterfacePolicy {
	OPEN, EXTENDABLE, RESTRICTED;

	// members

	public static final String RESTRICTED_VALUE = "RESTRICTED"; // right side must be a constant expression
}