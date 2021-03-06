package uk.gov.hmcts.auth.checker.service;

import java.util.Collection;
import java.util.function.Function;
import javax.servlet.http.HttpServletRequest;
import uk.gov.hmcts.auth.checker.RequestAuthorizer;
import uk.gov.hmcts.auth.checker.SubjectResolver;
import uk.gov.hmcts.auth.checker.exceptions.AuthCheckerException;
import uk.gov.hmcts.auth.checker.exceptions.BearerTokenInvalidException;
import uk.gov.hmcts.auth.checker.exceptions.BearerTokenMissingException;
import uk.gov.hmcts.auth.checker.exceptions.UnauthorisedServiceException;
import uk.gov.hmcts.auth.idam.service.token.ServiceTokenInvalidException;
import uk.gov.hmcts.auth.idam.service.token.ServiceTokenParsingException;

import static java.util.stream.Collectors.toSet;

public class ServiceRequestAuthorizer implements RequestAuthorizer<Service> {

    public static final String AUTHORISATION = "ServiceAuthorization";

    private final SubjectResolver<Service> serviceResolver;
    private final Function<HttpServletRequest, Collection<String>> authorizedServicesExtractor;

    public ServiceRequestAuthorizer(SubjectResolver<Service> serviceResolver, Function<HttpServletRequest, Collection<String>> authorizedServicesExtractor) {
        this.serviceResolver = serviceResolver;
        this.authorizedServicesExtractor = authorizedServicesExtractor;
    }

    @Override
    public Service authorise(HttpServletRequest request) throws UnauthorisedServiceException {
        Collection<String> authorizedServices = authorizedServicesExtractor.apply(request).stream().map(String::toLowerCase).collect(toSet());
        if (authorizedServices.isEmpty()) {
            throw new IllegalArgumentException("Must have at least one service defined");
        }

        String bearerToken = request.getHeader(AUTHORISATION);
        if (bearerToken == null) {
            throw new BearerTokenMissingException();
        }

        Service service = getTokenDetails(bearerToken);
        if (!authorizedServices.contains(service.getPrincipal().toLowerCase())) {
            throw new UnauthorisedServiceException();
        }

        return service;
    }

    private Service getTokenDetails(String bearerToken) {
        try {
            return serviceResolver.getTokenDetails(bearerToken);
        } catch (ServiceTokenInvalidException e) {
            throw new BearerTokenInvalidException();
        } catch (ServiceTokenParsingException e) {
            throw new AuthCheckerException("Error parsing JWT token");
        }
    }

}
