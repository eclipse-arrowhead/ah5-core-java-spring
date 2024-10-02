package eu.arrowhead.serviceregistry.jpa.entity;

import eu.arrowhead.common.jpa.ArrowheadEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "serviceInterfaceTemplateId", "propertyName" }))
public class ServiceInterfaceTemplateProperty extends ArrowheadEntity {

	//=================================================================================================
	// members

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "serviceInterfaceTemplateId", referencedColumnName = "id", nullable = false)
	private ServiceInterfaceTemplate serviceInterfaceTemplate;

	@Column(nullable = false, length = VARCHAR_SMALL)
	private String propertyName;

	@Column(nullable = false)
	private boolean mandatory = false;

	@Column(nullable = true, length = VARCHAR_LARGE)
	private String validator;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public ServiceInterfaceTemplateProperty() {
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceInterfaceTemplateProperty(final ServiceInterfaceTemplate serviceInterfaceTemplate, final String propertyName, final boolean mandatory, final String validator) {
		this.serviceInterfaceTemplate = serviceInterfaceTemplate;
		this.propertyName = propertyName;
		this.mandatory = mandatory;
		this.validator = validator;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "ServiceInterfaceTemplateProperty [id = " + id + ", serviceInterfaceTemplate = " + serviceInterfaceTemplate + ", propertyName = " + propertyName + ", mandatory = " + mandatory + ", validator = " + validator + "]";
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public ServiceInterfaceTemplate getServiceInterfaceTemplate() {
		return serviceInterfaceTemplate;
	}

	//-------------------------------------------------------------------------------------------------
	public void setServiceInterfaceTemplate(final ServiceInterfaceTemplate serviceInterfaceTemplate) {
		this.serviceInterfaceTemplate = serviceInterfaceTemplate;
	}

	//-------------------------------------------------------------------------------------------------
	public String getPropertyName() {
		return propertyName;
	}

	//-------------------------------------------------------------------------------------------------
	public void setPropertyName(final String propertyName) {
		this.propertyName = propertyName;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isMandatory() {
		return mandatory;
	}

	//-------------------------------------------------------------------------------------------------
	public void setMandatory(final boolean mandatory) {
		this.mandatory = mandatory;
	}

	//-------------------------------------------------------------------------------------------------
	public String getValidator() {
		return validator;
	}

	//-------------------------------------------------------------------------------------------------
	public void setValidator(final String validator) {
		this.validator = validator;
	}
}
