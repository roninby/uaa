package org.cloudfoundry.identity.uaa.integration.util;

import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.identity.uaa.provider.IdentityProvider;
import org.cloudfoundry.identity.uaa.provider.SamlIdentityProviderDefinition;
import org.cloudfoundry.identity.uaa.provider.SamlIdentityProviderDefinition.ExternalGroupMappingMode;
import org.cloudfoundry.identity.uaa.provider.saml.idp.SamlServiceProvider;
import org.cloudfoundry.identity.uaa.provider.saml.idp.SamlServiceProviderDefinition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class SamlIntegrationTestUtils {
    private static final String IDP_METADATA_PATH = "/saml/idp/metadata";
    private static final String SP_METADATA_PATH = "/saml/metadata";
    private static final String SAML_SERVICE_PROVIDERS_PATH = "/saml/service-providers";

    private static final String SAML_NAME_ID = "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress";

    private static final String ADMIN_CLIENT = "admin";
    private static final String ADMIN_CLIENT_SECRET = "adminsecret";

    public static String getIdpMetadata(String uaaUrl, String zoneSubdomain) throws Exception {
        return getMetadata(uaaUrl, IDP_METADATA_PATH, zoneSubdomain);
    }

    public static String getSpMetadata(String uaaUrl, String zoneSubdomain) throws Exception {
        return getMetadata(uaaUrl, SP_METADATA_PATH, zoneSubdomain);
    }

    public static IdentityProvider createUaaSamlIdentityProvider(String idpName,
                                                                 String uaaUrl,
                                                                 String metadataZone,
                                                                 String zoneSubdomain) throws Exception {

        SamlIdentityProviderDefinition definition = new SamlIdentityProviderDefinition();
        definition.setAddShadowUserOnLogin(true);
        definition.setStoreCustomAttributes(true);
        definition.setMetaDataLocation(
            SamlIntegrationTestUtils.getIdpMetadata(uaaUrl, metadataZone));
        definition.setNameID(SAML_NAME_ID);
        definition.setAssertionConsumerIndex(0);
        definition.setMetadataTrustCheck(false);
        definition.setShowSamlLink(true);
        definition.setLinkText("Login with UAA SAML");
        definition.setGroupMappingMode(
            ExternalGroupMappingMode.EXPLICITLY_MAPPED);
        definition.setSkipSslValidation(false);

        IdentityProvider<SamlIdentityProviderDefinition> identityProvider = new IdentityProvider<>();
        identityProvider.setConfig(definition);
        identityProvider.setOriginKey(idpName);
        identityProvider.setName(idpName);
        identityProvider.setActive(true);
        identityProvider.setIdentityZoneId(zoneSubdomain);

        String token = IntegrationTestUtils.getClientCredentialsToken(uaaUrl, ADMIN_CLIENT, ADMIN_CLIENT_SECRET);
        return IntegrationTestUtils.createOrUpdateProvider(token, uaaUrl, identityProvider);
    }

    public static SamlServiceProvider createServiceProvider(String spName,
                                                            String entityId,
                                                            RestTemplate template,
                                                            String uaaUrl,
                                                            String metadataZone,
                                                            String zoneSubdomain) throws Exception {

        String token = IntegrationTestUtils.getClientCredentialsToken(uaaUrl, ADMIN_CLIENT, ADMIN_CLIENT_SECRET);
        MultiValueMap<String, String> headers = createHeadersWithToken(token, zoneSubdomain);

        SamlServiceProvider serviceProvider = generateServiceProviderConfig(spName, entityId);
        serviceProvider.getConfig().setMetaDataLocation(getSpMetadata(uaaUrl, metadataZone));

        HttpEntity request = new HttpEntity<>(serviceProvider, headers);

        ResponseEntity<SamlServiceProvider> responseEntity =
            template.exchange(uaaUrl + SAML_SERVICE_PROVIDERS_PATH, HttpMethod.POST, request, SamlServiceProvider.class);

        return responseEntity.getBody();
    }

    private static String getMetadata(String uaaUrl,
                                      String metadataPath,
                                      String zoneSubdomain) throws Exception {

        RestTemplate adminClient = IntegrationTestUtils.getClientCredentialsTemplate(
            IntegrationTestUtils.getClientCredentialsResource(uaaUrl, new String[0], "admin", "adminsecret"));

        // TODO: support https URLs
        if (!StringUtils.isBlank(zoneSubdomain)) {
            uaaUrl = uaaUrl.replaceAll("https?://", "http://" + zoneSubdomain + ".");
        }

        ResponseEntity<String> responseEntity =
            adminClient.getForEntity(uaaUrl + metadataPath, String.class);

        return responseEntity.getBody();
    }

    private static MultiValueMap<String, String> createHeadersWithToken(String token, String zoneSubdomain) {
        MultiValueMap<String,String> headers = new LinkedMultiValueMap<>();
        headers.add("Accept", APPLICATION_JSON_VALUE);
        headers.add("Authorization", "bearer " + token);
        headers.add("Content-Type", APPLICATION_JSON_VALUE);
        if (!StringUtils.isBlank(zoneSubdomain)) {
            headers.add("X-Identity-Zone-Subdomain", zoneSubdomain);
        }
        return headers;
    }

    private static SamlServiceProvider generateServiceProviderConfig(String name, String entityId) {
        SamlServiceProvider config = new SamlServiceProvider();
        config.setName(name);
//        config.setEntityId(entityId);
        config.setConfig(new SamlServiceProviderDefinition());
        return config;
    }
}
