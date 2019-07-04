package gr.uoa.di.rent.controllers;

import gr.uoa.di.rent.exceptions.NotAuthorizedException;
import gr.uoa.di.rent.models.Business;
import gr.uoa.di.rent.models.User;
import gr.uoa.di.rent.payload.requests.ProviderApplicationRequest;
import gr.uoa.di.rent.payload.responses.BusinessResponse;
import gr.uoa.di.rent.repositories.BusinessRepository;
import gr.uoa.di.rent.repositories.UserRepository;
import gr.uoa.di.rent.security.CurrentUser;
import gr.uoa.di.rent.security.Principal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@Validated
@RequestMapping("/businesses")
public class BusinessController {

    private static final Logger logger = LoggerFactory.getLogger(BusinessController.class);

    private final BusinessRepository businessRepository;

    private final UserRepository userRepository;


    public BusinessController(BusinessRepository businessRepository, UserRepository userRepository) {
        this.businessRepository = businessRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("")
    @PreAuthorize("hasRole('PROVIDER') or hasRole('ADMIN')")
    List<Business> findAll() {
        return businessRepository.findAll();
    }

    @PostMapping("")
    @PreAuthorize("hasRole('PROVIDER') or hasRole('ADMIN')")
    Business newBusiness(@CurrentUser Principal principal,
                         @Valid @RequestBody ProviderApplicationRequest providerApplicationRequest) {
        // No check is needed to verify that the current user is a provider, since common user don't have access to this endpoint.
        return businessRepository.save(new Business(
                providerApplicationRequest.getCompany_name(),
                providerApplicationRequest.getEmail(),
                providerApplicationRequest.getCompany_address(),
                providerApplicationRequest.getTax_number(),
                providerApplicationRequest.getTax_office(),
                providerApplicationRequest.getOwner_name(),
                providerApplicationRequest.getOwner_surname(),
                providerApplicationRequest.getOwner_patronym(),
                providerApplicationRequest.getId_card_number(),
                providerApplicationRequest.getId_card_date_of_issue(),
                providerApplicationRequest.getResidence_address(),
                principal.getUser(),
                null
        ));
    }

    @GetMapping("/byProviderId/{providerId:[\\d]+}")
    @PreAuthorize("hasRole('PROVIDER') or hasRole('ADMIN')")
    ResponseEntity<?> getBusinessByProviderId(@Valid @CurrentUser Principal principal, @PathVariable Long providerId) {

        // If current user is not Admin and the given "userId" is not the same as the current user requesting, then return error.
        if ( !principal.getUser().getId().equals(providerId) && !principal.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")) ) {
            throw new NotAuthorizedException("You are not authorized to get the business-data of another provider!");
        }

        // Get the provider by the providerId.
        User provider = userRepository.findById(providerId).orElse(null);
        if (provider == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Provider not found!");
        }

        Business business = provider.getBusiness();
        if ( business == null )
            return ResponseEntity.status(HttpStatus.CONFLICT).body("No business was found for provider with username: " + provider.getUsername());

        return ResponseEntity.ok(new BusinessResponse(business));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PROVIDER') or hasRole('ADMIN')")
    Business saveOrUpdate(@RequestBody Business newBusiness, @PathVariable Long id) {
        return businessRepository.findById(id)
                .map(x -> businessRepository.save(x))
                .orElseGet(() -> businessRepository.save(newBusiness));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('PROVIDER') or hasRole('ADMIN')")
    Business patch(@RequestBody Map<String, String> update, @PathVariable Long id) {
        return businessRepository.findById(id)
                .map(x -> {
                    String business_name = update.get("business_name");
                    if (!StringUtils.isEmpty(business_name)) {
                        x.setBusiness_name(business_name);
                        return businessRepository.save(x);
                    } else {
                        throw new RuntimeException("Field " + update.keySet() + " update is not allow.");
                    }

                })
                .orElseGet(() -> {
                    throw new RuntimeException("Business [" + id + "] not found!");
                });
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PROVIDER') or hasRole('ADMIN')")
    void deleteBusiness(@PathVariable Long id) {
        businessRepository.deleteById(id);
    }
}
