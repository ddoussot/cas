package org.apereo.cas.support.saml.services;

import org.apereo.cas.services.DefaultServicesManagerRegisteredServiceLocator;
import org.apereo.cas.support.saml.SamlProtocolConstants;
import org.apereo.cas.support.saml.services.idp.metadata.SamlRegisteredServiceServiceProviderMetadataFacade;
import org.apereo.cas.support.saml.services.idp.metadata.cache.SamlRegisteredServiceCachingMetadataResolver;

import lombok.val;
import org.springframework.core.Ordered;

/**
 * This is {@link SamlIdPServicesManagerRegisteredServiceLocator}.
 *
 * @author Misagh Moayyed
 * @since 6.3.0
 */
public class SamlIdPServicesManagerRegisteredServiceLocator extends DefaultServicesManagerRegisteredServiceLocator {
    public SamlIdPServicesManagerRegisteredServiceLocator(final SamlRegisteredServiceCachingMetadataResolver resolver) {
        setOrder(Ordered.HIGHEST_PRECEDENCE);
        setRegisteredServiceFilter((registeredService, service) -> {
            val isSamlServiceProvider = registeredService.getClass().equals(SamlRegisteredService.class)
                && service.getAttributes().containsKey(SamlProtocolConstants.PARAMETER_SAML_REQUEST);
            
            if (isSamlServiceProvider && registeredService.matches(service.getId())) {
                val samlService = SamlRegisteredService.class.cast(registeredService);
                val adaptor = SamlRegisteredServiceServiceProviderMetadataFacade.get(resolver, samlService, service.getId());
                return adaptor.isPresent();
            }
            return false;
        });
    }
}
