/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */

package org.openmrs.module.kenyaemr.chore;

import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.api.PatientService;
import org.openmrs.module.idgen.service.IdentifierSourceService;
import org.openmrs.module.kenyacore.chore.AbstractChore;
import org.openmrs.module.kenyacore.chore.Requires;
import org.openmrs.module.kenyaemr.api.KenyaEmrService;
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;

/**
 * Prior to 13.3.1, the EditPatientFragmentController appears to have sometimes saved a patient without properly saving
 * their required OpenMRS ID / MRN.
 */
@Component("kenyaemr.chore.fixMissingOpenmrsIdentifiers")
@Requires({ VoidDuplicateIdentifiers.class })
public class FixMissingOpenmrsIdentifiers extends AbstractChore {

	@Autowired
	private KenyaEmrService kenyaEmrService;

	@Autowired
	private PatientService patientService;

	@Autowired
	private IdentifierSourceService idgenService;

	/**
	 * @see org.openmrs.module.kenyacore.chore.AbstractChore#perform(java.io.PrintWriter)
	 */
	@Override
	public void perform(PrintWriter output) throws Exception {
		PatientIdentifierType openmrsIdType = MetadataUtils.getPatientIdentifierType(CommonMetadata._PatientIdentifierType.OPENMRS_ID);
		Location defaultLocation = kenyaEmrService.getDefaultLocation();

		int fixed = 0;

		for (Patient patient : patientService.getAllPatients()) {

			// Generate new OpenMRS ID if needed
			if (patient.getPatientIdentifier(openmrsIdType) == null) {
				String generated = idgenService.generateIdentifier(openmrsIdType, FixMissingOpenmrsIdentifiers.class.getSimpleName());
				PatientIdentifier openmrsID = new PatientIdentifier(generated, openmrsIdType, defaultLocation);
				patient.addIdentifier(openmrsID);

				if (!patientHasPreferredId(patient)) {
					openmrsID.setPreferred(true);
				}

				patientService.savePatientIdentifier(openmrsID);
				fixed++;
			}
		}

		output.println("Fixed " + fixed + " missing OpenMRS IDs");
	}

	protected boolean patientHasPreferredId(Patient patient) {
		PatientIdentifier defId = patient.getPatientIdentifier();
		return defId != null && defId.isPreferred();
	}
}
