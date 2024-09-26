package it.smartcommunitylab.dhub.rm.service;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import it.smartcommunitylab.dhub.rm.SystemKeys;
import it.smartcommunitylab.dhub.rm.converter.DTOToSchemaConverter;
import it.smartcommunitylab.dhub.rm.converter.SchemaToDTOConverter;
import it.smartcommunitylab.dhub.rm.model.CustomResourceSchema;
import it.smartcommunitylab.dhub.rm.model.IdAwareCustomResourceDefinition;
import it.smartcommunitylab.dhub.rm.model.dto.CustomResourceSchemaDTO;
import it.smartcommunitylab.dhub.rm.repository.CustomResourceSchemaRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.OngoingStubbing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustomResourceSchemaServiceTest {

    @Mock
    CustomResourceSchemaRepository customResourceSchemaRepository;

    @Mock
    SchemaToDTOConverter schemaToDTOConverter;

    @Mock
    CustomResourceDefinitionService crdService;

    @Mock
    K8SAuthorizationService authService;

    @Mock
    DTOToSchemaConverter dtoToSchemaConverter;

    @InjectMocks
    CustomResourceSchemaService customResourceSchemaService;

    CustomResourceSchemaDTO customResourceSchemaDTO;
    CustomResourceSchema customResourceSchema;
    private final String crdName = "test-crd";
    private final String crdVersion = "v1";

    @BeforeEach
    public void setup() {

        customResourceSchema = new CustomResourceSchema();
        customResourceSchema.setCrdId(crdName);
        customResourceSchema.setVersion(crdVersion);

        customResourceSchemaDTO = new CustomResourceSchemaDTO();
        customResourceSchemaDTO.setCrdId(crdName);
        customResourceSchemaDTO.setVersion(crdVersion);

    }

    @Test
    public void testBootstrapSchemas() throws Exception {
        Constructor<IdAwareCustomResourceDefinition> constructor =
                IdAwareCustomResourceDefinition.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        ObjectMeta meta = new ObjectMeta();
        meta.setName(crdName);
        CustomResourceDefinition customResource = new CustomResourceDefinition();
        customResource.setMetadata(meta);

        IdAwareCustomResourceDefinition crd = new IdAwareCustomResourceDefinition(customResource);

        when(crdService.findAll(any(), eq(true), any())).thenReturn(new PageImpl<>(Collections.singletonList(crd)));

        customResourceSchemaService.bootstrapSchemas();

        verify(crdService).findAll(null, true, PageRequest.ofSize(1000));
    }

    @Test
    public void testFindAll() {

        Pageable pageable = PageRequest.of(0, 10);

        IdAwareCustomResourceDefinition crd = Mockito.mock(IdAwareCustomResourceDefinition.class);
        CustomResourceDefinition crdDefinition = Mockito.mock(CustomResourceDefinition.class);

        when(crd.getCrd()).thenReturn(crdDefinition);
        when(crdDefinition.getMetadata()).thenReturn(Mockito.mock(ObjectMeta.class));
        when(crdDefinition.getMetadata().getName()).thenReturn(crdName);

        Page<IdAwareCustomResourceDefinition> crdPage = new PageImpl<>(Collections.singletonList(crd), pageable, 1);
        Page<CustomResourceSchema> schemaPage = new PageImpl<>(Collections.singletonList(customResourceSchema), pageable, 1);

        when(crdService.findAll(null, false, pageable)).thenReturn(crdPage);
        when(crdService.fetchStoredVersionName(crdDefinition)).thenReturn(crdVersion);
        when(customResourceSchemaRepository.findByCrdIdAndVersion(crdName, crdVersion)).thenReturn(Optional.of(customResourceSchema));
        lenient().when(schemaToDTOConverter.convert(customResourceSchema)).thenReturn(customResourceSchemaDTO);

        Page<CustomResourceSchemaDTO> result1 = customResourceSchemaService.findAll(null, true, pageable);

        Assertions.assertNotNull(result1);
        assertEquals(1, result1.getTotalElements());
        assertEquals(crdName, result1.getContent().get(0).getCrdId());

        //

        when(customResourceSchemaRepository.findAll(pageable)).thenReturn(schemaPage);

        Page<CustomResourceSchemaDTO> result2 = customResourceSchemaService.findAll(null, false, pageable);

        Assertions.assertNotNull(result2);
        assertEquals(1, result2.getTotalElements());
        assertEquals(crdName, result2.getContent().get(0).getCrdId());

        //

        List<String> ids = Collections.singletonList(crdName);
        when(customResourceSchemaRepository.findByIdIn(ids, pageable)).thenReturn(schemaPage);

        Page<CustomResourceSchemaDTO> result3 = customResourceSchemaService.findAll(ids, false, pageable);

        Assertions.assertNotNull(result3);
        assertEquals(1, result3.getTotalElements());
        assertEquals(crdName, result3.getContent().get(0).getCrdId());
    }

    @Test
    public void testFindById(){

        when(customResourceSchemaRepository.findById(crdName))
                .thenReturn(Optional.of(customResourceSchema));
        lenient().when(schemaToDTOConverter.convert(customResourceSchema))
                .thenReturn(customResourceSchemaDTO);

        CustomResourceSchemaDTO result1 = customResourceSchemaService.findById(crdName);

        Assertions.assertNotNull(result1);
        assertEquals(crdName, result1.getCrdId());
        assertEquals(crdVersion, result1.getVersion());

        when(customResourceSchemaRepository.findById(crdName)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> {
            customResourceSchemaService.findById(crdName);
        });
    }

    @Test
    public void testFindByCrdIdAndVersion() {

        when(customResourceSchemaRepository.findByCrdIdAndVersion(crdName, crdVersion))
                .thenReturn(Optional.of(customResourceSchema));
        lenient().when(schemaToDTOConverter.convert(customResourceSchema))
                .thenReturn(customResourceSchemaDTO);

        CustomResourceSchemaDTO result1 = customResourceSchemaService.findByCrdIdAndVersion(crdName, crdVersion);

        Assertions.assertNotNull(result1);
        assertEquals(crdName, result1.getCrdId());
        assertEquals(crdVersion, result1.getVersion());

        when(customResourceSchemaRepository.findByCrdIdAndVersion(crdName, crdVersion))
                .thenReturn(Optional.empty());

        when(crdService.getCrdSchema(crdName, crdVersion))
                .thenReturn(Collections.singletonMap("field", "value"));
        lenient().when(schemaToDTOConverter.convert(any(CustomResourceSchema.class)))
                .thenReturn(customResourceSchemaDTO);

        CustomResourceSchemaDTO result2 = customResourceSchemaService.findByCrdIdAndVersion(crdName, crdVersion);

        Assertions.assertNotNull(result2);
        assertEquals(crdName, result2.getCrdId());
        assertEquals(crdVersion, result2.getVersion());

        when(crdService.getCrdSchema(crdName, crdVersion)).thenReturn(null);

        assertThrows(NoSuchElementException.class, () -> {
            customResourceSchemaService.findByCrdIdAndVersion(crdName, crdVersion);
        });
    }


    @Test
    public void testFindCRDByCrdIdAndVersion() {

        when(customResourceSchemaRepository.findByCrdIdAndVersion(crdName, crdVersion)).thenReturn(Optional.of(customResourceSchema));

        CustomResourceSchema CRDResult = customResourceSchemaService.findCRDByCrdIdAndVersion(crdName, crdVersion);

        Assertions.assertNotNull(CRDResult);
        assertEquals(crdName, CRDResult.getCrdId());
        assertEquals(crdVersion, CRDResult.getVersion());

        //

        when(customResourceSchemaRepository.findByCrdIdAndVersion(crdName, crdVersion))
                .thenReturn(Optional.empty());

        Map<String, Serializable> schemaMap = Collections.singletonMap("field", "value");
        when(crdService.getCrdSchema(crdName, crdVersion))
                .thenReturn(schemaMap);

        CustomResourceSchema result2 = customResourceSchemaService.findCRDByCrdIdAndVersion(crdName, crdVersion);

        Assertions.assertNotNull(result2);
        assertEquals(crdName, result2.getCrdId());
        assertEquals(crdVersion, result2.getVersion());
        assertEquals(schemaMap, result2.getSchema());

        //

        when(crdService.getCrdSchema(crdName, crdVersion))
                .thenReturn(null);

        assertThrows(NoSuchElementException.class, () -> {
            customResourceSchemaService.findCRDByCrdIdAndVersion(crdName, crdVersion);
        });
    }

    @Test
    public void testFindByCrdId(){

        Pageable pageable = PageRequest.of(0, 10);
        List<CustomResourceSchema> schemas = Collections.singletonList(customResourceSchema);
        Page<CustomResourceSchema> schemaPage = new PageImpl<>(schemas, pageable, schemas.size());

        when(customResourceSchemaRepository.findByCrdId(crdName, pageable)).thenReturn(schemaPage);
        lenient().when(schemaToDTOConverter.convert(customResourceSchema)).thenReturn(customResourceSchemaDTO);

        Page<CustomResourceSchemaDTO> result = customResourceSchemaService.findByCrdId(crdName, pageable);
        Assertions.assertNotNull(result);
        assertEquals(1, result.getTotalElements());

        CustomResourceSchemaDTO actualDTO = result.getContent().get(0);
        assertEquals(customResourceSchemaDTO.getCrdId(), actualDTO.getCrdId());
        assertEquals(customResourceSchemaDTO.getVersion(), actualDTO.getVersion());
    }

    @Test
    public void testAdd() {

        CustomResourceSchemaDTO dtoToAdd = new CustomResourceSchemaDTO();
        dtoToAdd.setCrdId("test-crd");
        dtoToAdd.setVersion("v1");

        when(authService.isCrdAllowed("test-crd")).thenReturn(true);
        when(crdService.crdExists("test-crd", "v1")).thenReturn(true);
        when(customResourceSchemaRepository.save(any())).thenAnswer(invocation -> {
            CustomResourceSchema savedSchema = invocation.getArgument(0);
            savedSchema.setId("01");
            return savedSchema;
        });

        CustomResourceSchemaDTO addedDTO = customResourceSchemaService.add(null, dtoToAdd);

        Assertions.assertNotNull(addedDTO);
        assertEquals("test-crd", addedDTO.getCrdId());
        assertEquals("v1", addedDTO.getVersion());

        CustomResourceSchemaDTO addedDtoWithId = customResourceSchemaService.add("5", dtoToAdd);
        Assertions.assertNotNull(addedDtoWithId);

        //

        CustomResourceSchemaDTO dtoNotAllowed = new CustomResourceSchemaDTO();
        dtoNotAllowed.setCrdId("crd-not-allowed");
        dtoNotAllowed.setVersion("v1");

        lenient().when(authService.isCrdAllowed("crd-not-allowed")).thenReturn(false);

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> customResourceSchemaService.add(null, dtoNotAllowed)
        );

        assertEquals(SystemKeys.ERROR_CRD_NOT_ALLOWED, exception.getMessage());

    }

    @Test
    public void testAddIllegalExceptionSchemaExist() {

        CustomResourceSchema crWithId = new CustomResourceSchema();
        crWithId.setId("1");

        when(authService.isCrdAllowed(crdName)).thenReturn(true);

        when(customResourceSchemaRepository.findById("1")).thenReturn(Optional.of(crWithId));

        IllegalArgumentException exception1 = assertThrows(
                IllegalArgumentException.class,
                () -> customResourceSchemaService.add("1", customResourceSchemaDTO)
        );

        assertEquals(SystemKeys.ERROR_SCHEMA_EXISTS, exception1.getMessage());

    }

    @Test
    public void testAddIllegalExceptionNoCrd() {

        CustomResourceSchemaDTO dtoNotExist = new CustomResourceSchemaDTO();
        dtoNotExist.setCrdId("crd-not-exist");

        lenient().when(authService.isCrdAllowed("crd-not-exist")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> customResourceSchemaService.add(null, dtoNotExist)
        );

        assertEquals(SystemKeys.ERROR_K8S_NO_CRD, exception.getMessage());

    }


    @Test
    public void testUpdate() {

        when(customResourceSchemaRepository.findById(crdName)).thenReturn(Optional.of(customResourceSchema));
        when(crdService.crdExists(crdName, crdVersion)).thenReturn(true);
        when(customResourceSchemaRepository.save(customResourceSchema)).thenReturn(customResourceSchema);

        CustomResourceSchemaDTO updatedSchemaDTO = customResourceSchemaService.update(crdName, customResourceSchemaDTO);

        Assertions.assertNotNull(updatedSchemaDTO);
        assertEquals(crdName, updatedSchemaDTO.getCrdId());
        assertEquals(crdVersion, updatedSchemaDTO.getVersion());

        //

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> customResourceSchemaService.update(null, any(CustomResourceSchemaDTO.class))
        );

        assertEquals(SystemKeys.ERROR_NO_SCHEMA, exception.getMessage());

        //

        when(crdService.crdExists(crdName, crdVersion)).thenReturn(false);
        IllegalArgumentException exception1 = assertThrows(
                IllegalArgumentException.class,
                () -> customResourceSchemaService.update(crdName, customResourceSchemaDTO)
        );

        assertEquals(SystemKeys.ERROR_K8S_NO_CRD, exception1.getMessage());

    }



    @Test
    public void testDelete(){

        when(customResourceSchemaRepository.findById(crdName)).thenReturn(Optional.of(customResourceSchema));
        customResourceSchemaService.delete(crdName);

        verify(customResourceSchemaRepository).deleteById(crdName);
    }

}
