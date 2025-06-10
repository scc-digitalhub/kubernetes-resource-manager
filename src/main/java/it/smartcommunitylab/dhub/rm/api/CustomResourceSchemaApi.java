// SPDX-License-Identifier: Apache-2.0
package it.smartcommunitylab.dhub.rm.api;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import it.smartcommunitylab.dhub.rm.SystemKeys;
import it.smartcommunitylab.dhub.rm.model.dto.CustomResourceSchemaDTO;
import it.smartcommunitylab.dhub.rm.service.CustomResourceSchemaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.util.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAuthority('ROLE_USER')")
@SecurityRequirement(name = "basicAuth")
@SecurityRequirement(name = "jwtAuth")
@RequestMapping(SystemKeys.API_PATH + "/crs")
@Validated
public class CustomResourceSchemaApi {

    @Autowired
    private CustomResourceSchemaService service;

    @GetMapping
    public Page<CustomResourceSchemaDTO> findAll(
        @RequestParam(required = false) Collection<String> id,
        @RequestParam(required = false) Boolean all,
        Pageable pageable
    ) {
        //TODO aggiungere parametri di ricerca per keyword
        return service.findAll(id, Boolean.TRUE.equals(all), pageable);
    }

    @GetMapping("/{id}")
    public CustomResourceSchemaDTO findById(@PathVariable @Pattern(regexp = SystemKeys.REGEX_SCHEMA_ID) String id) {
        return service.findById(id);
    }

    @PreAuthorize("@authz.canAccess('crs', 'write')")
    @PostMapping
    public CustomResourceSchemaDTO add(
        @RequestParam(required = false) String id,
        @Valid @RequestBody CustomResourceSchemaDTO request
    ) {
        return service.add(id, request);
    }

    @PreAuthorize("@authz.canAccess('crs', 'write')")
    @PutMapping("/{id}")
    public CustomResourceSchemaDTO update(
        @PathVariable @Pattern(regexp = SystemKeys.REGEX_SCHEMA_ID) String id,
        @Valid @RequestBody CustomResourceSchemaDTO request
    ) {
        return service.update(id, request);
    }

    @PreAuthorize("@authz.canAccess('crs', 'write')")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable @Pattern(regexp = SystemKeys.REGEX_SCHEMA_ID) String id) {
        service.delete(id);
    }
}
