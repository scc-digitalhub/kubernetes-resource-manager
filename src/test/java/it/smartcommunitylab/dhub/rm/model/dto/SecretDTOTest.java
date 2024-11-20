package it.smartcommunitylab.dhub.rm.model.dto;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SecretDTOTest {

    @Mock
    Map<String, String> mockData;

    @InjectMocks
    SecretDTO secretDTO;

    @Before
    public void setUp() {
        secretDTO = new SecretDTO();
        secretDTO.setName("TestSecret");
        secretDTO.setData(mockData);
    }

    @Test
    public void testGetName() {
        assertEquals("TestSecret", secretDTO.getName());
    }

    @Test
    public void testSetName() {
        secretDTO.setName("NewName");
        assertEquals("NewName", secretDTO.getName());
    }

    @Test
    public void testGetData() {
        when(mockData.get("key1")).thenReturn("value1");
        assertEquals("value1", secretDTO.getData().get("key1"));
    }

    @Test
    public void testSetData() {
        Map<String, String> testData = new HashMap<>();
        testData.put("key1", "value1");
        testData.put("key2", "value2");

        secretDTO.setData(testData);
        assertEquals("value1", secretDTO.getData().get("key1"));
        assertEquals("value2", secretDTO.getData().get("key2"));
    }
}
