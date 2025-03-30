package org.mitre.synthea.export;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.SingleValidationMessage;
import ca.uhn.fhir.validation.ValidationResult;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mitre.synthea.TestHelper;
import org.mitre.synthea.engine.Generator;
import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.world.agents.Provider;
import org.mitre.synthea.world.agents.ProviderTest;
import org.mitre.synthea.world.concepts.HealthRecord.EncounterType;
import org.mitre.synthea.world.geography.Location;

public class HospitalExporterTestDstu2 {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void testFHIRExport() throws Exception {
    FhirContext ctx = FhirDstu2.getContext();
    FhirValidator validator = ctx.newValidator();
    validator.setValidateAgainstStandardSchema(true);
    validator.setValidateAgainstStandardSchematron(true);

    File tempOutputFolder = tempFolder.newFolder();
    Config.set("exporter.baseDirectory", tempOutputFolder.toString());
    Config.set("exporter.hospital.fhir_dstu2.export", "true");
    Config.set("exporter.fhir.bulk_data", "false");
    Config.set("exporter.fhir.transaction_bundle", "true");
    FhirDstu2.TRANSACTION_BUNDLE = true; // set this manually, in case it has already been loaded.
    TestHelper.loadTestProperties();
    Generator.DEFAULT_STATE = Config.get("test_state.default", "Massachusetts");
    Location location = new Location(Generator.DEFAULT_STATE, null);
    Provider.clear();
    Provider.loadProviders(location, ProviderTest.providerRandom);
    assertNotNull(Provider.getProviderList());
    assertFalse(Provider.getProviderList().isEmpty());

    Provider.getProviderList().get(0).incrementEncounters(EncounterType.WELLNESS, 0);
    HospitalExporterDstu2.export(0L);

    File expectedExportFolder = tempOutputFolder.toPath().resolve("fhir_dstu2").toFile();
    assertTrue(expectedExportFolder.exists() && expectedExportFolder.isDirectory());

    File expectedExportFile = expectedExportFolder.toPath().resolve("hospitalInformation0.json")
        .toFile();
    assertTrue(expectedExportFile.exists() && expectedExportFile.isFile());

    FileReader fileReader = new FileReader(expectedExportFile.getPath());
    BufferedReader bufferedReader = new BufferedReader(fileReader);
    StringBuilder fhirJson = new StringBuilder();
    String line = null;
    while ((line = bufferedReader.readLine()) != null) {
      fhirJson.append(line);
    }
    bufferedReader.close();
    IBaseResource resource = ctx.newJsonParser().parseResource(fhirJson.toString());
    ValidationResult result = validator.validateWithResult(resource);
    if (result.isSuccessful() == false) {
      for (SingleValidationMessage message : result.getMessages()) {
        System.out.println(message.getSeverity().toString() + ": " + message.getMessage());
      }
    }
    assertTrue(result.isSuccessful());
  }

  @Test
  public void testBulkExport() throws Exception {
    File tempOutputFolder = tempFolder.newFolder();
    Config.set("exporter.baseDirectory", tempOutputFolder.toString());
    Config.set("exporter.hospital.fhir_dstu2.export", "true");
    Config.set("exporter.fhir.bulk_data", "true");
    Config.set("exporter.fhir.transaction_bundle", "false");
    FhirStu3.TRANSACTION_BUNDLE = true; // set this manually, in case it has already been loaded.
    TestHelper.loadTestProperties();
    Generator.DEFAULT_STATE = Config.get("test_state.default", "Massachusetts");
    Location location = new Location(Generator.DEFAULT_STATE, null);
    Provider.clear();
    Provider.loadProviders(location, ProviderTest.providerRandom);
    assertNotNull(Provider.getProviderList());
    assertFalse(Provider.getProviderList().isEmpty());

    Provider.getProviderList().get(0).incrementEncounters(EncounterType.WELLNESS, 0);
    Provider.getProviderList().get(0).attributes.put("bed_count", 1);
    HospitalExporterDstu2.export(0L);

    File expectedExportFolder = tempOutputFolder.toPath().resolve("fhir_dstu2").toFile();
    assertTrue(expectedExportFolder.exists() && expectedExportFolder.isDirectory());

    File expectedExportFile = expectedExportFolder.toPath().resolve("Organization.0.ndjson")
        .toFile();
    assertTrue(expectedExportFile.exists() && expectedExportFile.isFile());
  }
}
