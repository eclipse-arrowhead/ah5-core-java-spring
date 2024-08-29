package eu.arrowhead.serviceregistry.service.matching;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import eu.arrowhead.dto.AddressDTO;

 @Component
public class AddressMatching {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	// checks if the two list contain exactly the same addresses (the order does not matter)
	public boolean isAddressListMatching(final List<AddressDTO> addresses1, final List<AddressDTO> addresses2) {

		if (addresses1.size() != addresses2.size()) {
			return false;
		}

		List<AddressDTO> temp1 = new ArrayList<>(addresses1);
		final List<AddressDTO> temp2 = new ArrayList<>(addresses2);

		int temp1Size = temp1.size();
		while (temp1Size != 0) {
			for (final AddressDTO temp2Address : temp2) {
				temp1 = removeMatch(temp1, temp2Address);

				//if the size did not change, there was no match
				if (temp1.size() == temp1Size) {
					return false;
				}
				temp1Size = temp1.size();
			}
		}

		return true;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	// Removes the corresponding address from the list, if it matches the required address,
	// returns the original list, if there was no match
	private List<AddressDTO> removeMatch(final List<AddressDTO> addresses, final AddressDTO required) {
		for (final AddressDTO address : addresses) {
			if (address.equals(required)) {
				addresses.remove(address);
				return addresses;
			}
		}
		return addresses;
	}
}
