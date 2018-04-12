package org.cloudfoundry.identity.uaa.integration.util;

import org.cloudfoundry.identity.uaa.ServerRunning;
import org.cloudfoundry.identity.uaa.constants.OriginKeys;
import org.cloudfoundry.identity.uaa.provider.IdentityProvider;
import org.cloudfoundry.identity.uaa.provider.SamlIdentityProviderDefinition;
import org.cloudfoundry.identity.uaa.scim.ScimUser;
import org.cloudfoundry.identity.uaa.test.UaaTestAccounts;
import org.cloudfoundry.identity.uaa.zone.IdentityZone;
import org.cloudfoundry.identity.uaa.zone.IdentityZoneConfiguration;
import org.springframework.security.oauth2.common.util.RandomValueStringGenerator;
import org.springframework.web.client.RestTemplate;

import javax.ws.rs.GET;
import javax.ws.rs.POST;

import static org.junit.Assert.assertNotNull;

public class UaaSamlIdpCreator implements SamlIdentityProviderCreator {
    public UaaSamlIdpCreator() {
//        1. Create an identity zone of the UAA (the idp zone) and create a user in that zone.
        IdentityZoneConfiguration izc = new IdentityZoneConfiguration();
        IdentityZone iz = new IdentityZone();
        iz.setId("idp");
        iz.setSubdomain("");
        iz.setName("");
        iz.

    }

    @Override
    public IdentityProvider<SamlIdentityProviderDefinition> createIdp(String baseUrl) {
        String zoneAdminToken = getZoneAdminToken(baseUrl, ServerRunning.isRunning());
        SamlIdentityProviderDefinition samlIdentityProviderDefinition = createDefinition();
        return createIdentityProvider("UAA test idp", baseUrl);
    }

    private SamlIdentityProviderDefinition createDefinition() {
        SamlIdentityProviderDefinition identityProviderDefinition = new SamlIdentityProviderDefinition();
        identityProviderDefinition.setSkipSslValidation(true);
        identityProviderDefinition.setGroupMappingMode(SamlIdentityProviderDefinition.ExternalGroupMappingMode.EXPLICITLY_MAPPED);
        identityProviderDefinition.setLinkText("Test UAA Saml Idp Login");
        identityProviderDefinition.setShowSamlLink(true);
        identityProviderDefinition.setMetadataTrustCheck(true);
        identityProviderDefinition.setAddShadowUserOnLogin(true);
        identityProviderDefinition.setAssertionConsumerIndex(0);
        identityProviderDefinition.setNameID("uaa-saml-idp");
        identityProviderDefinition.setMetaDataLocation()
        return identityProviderDefinition;
    }

    private void createIdentityProviderRegistrationinSPZone() {

    }

    private IdentityProvider createIdentityProvider(String idpName,
                                                    String baseUrl,
                                                    String spZone) {


//        3. Download UAA IdP saml metadata from GET http://idp.localhost:8080/uaa/saml/idp/metadata
//        4. In the SP zone, create an IdP registration with POST http://sp.<uaa-url>/identity-providers?rawConfig=true
//        5. In the IDP zone, create an SP registration with POST http://idp.<uaa-url>/saml/service-providers with SP metadata from http://sp.<uaa-url>/uaa/saml/metadata

        SamlIdentityProviderDefinition samlIdentityProviderDefinition = createDefinition();
        String zoneAdminToken = getZoneAdminToken(baseUrl, ServerRunning.isRunning());


        IdentityProvider provider = new IdentityProvider();
        provider.setIdentityZoneId(OriginKeys.UAA);
        provider.setType(OriginKeys.SAML);
        provider.setActive(true);
        provider.setConfig(samlIdentityProviderDefinition);
        provider.setOriginKey(samlIdentityProviderDefinition.getIdpEntityAlias());
        provider.setName(idpName);
        provider = IntegrationTestUtils.createOrUpdateProvider(zoneAdminToken,baseUrl,provider);
        assertNotNull(provider.getId());
        return provider;
    }

    private String getZoneAdminToken(String baseUrl, ServerRunning serverRunning) {
        try {
            return IntegrationTestUtils.getZoneAdminToken(baseUrl, serverRunning, OriginKeys.UAA);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
