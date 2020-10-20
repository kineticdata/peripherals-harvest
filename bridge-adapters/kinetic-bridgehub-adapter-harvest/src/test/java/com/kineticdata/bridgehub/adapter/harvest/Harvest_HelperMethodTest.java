/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.kineticdata.bridgehub.adapter.harvest;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPathException;
import com.kineticdata.bridgehub.adapter.BridgeError;
import com.kineticdata.bridgehub.adapter.Record;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author chadrehm
 */
public class Harvest_HelperMethodTest { 
    @Test
    public void test_get_parameters() throws BridgeError {
        HarvestAdapter helper = new HarvestAdapter();
        
        AdapterMapping mapping = helper.getMapping("Contacts");
        
        Map<String, String> parameters = 
            helper.getParameters("foo=bar&fizz=buzz", mapping);
        
        Map<String, String> parametersControl = new HashMap<String, String>() {{
            put("foo","bar");
            put("fizz","buzz");
        }};
        
        assertTrue(parameters.equals(parametersControl));
        
        mapping = helper.getMapping("Adhoc");
        
        parameters = helper.getParameters("_noOp_?foo=bar&fizz=buzz", mapping);
        
        parametersControl.put("adapterPath", "_noOp_");
        
        assertTrue(parameters.equals(parametersControl));
    }
    
    @Test
    public void test_get_mapping_error() throws BridgeError {
        BridgeError error = null;
        HarvestAdapter helper = new HarvestAdapter();
        
        try {
            helper.getMapping("Foo");
        } catch (BridgeError e) {
            error = e;
        }
                
        assertNotNull(error);
    }
    
    @Test
    public void test_build_record() {
        HarvestAdapter helper = new HarvestAdapter();
        ObjectMapper mapper = new ObjectMapper();

        List<String> list = new ArrayList();
        list.add("$.custom_fields[*].kind");
        list.add("FirstName");
        
        String jsonString = "{\"FirstName\": \"Foo\", \"custom_fields\": [{" +
            "\"kind\": \"Total activities\", \"value\": 0," +
            "\"fieldType\": \"integer\"}]}";
        JSONObject jsonobj = (JSONObject)JSONValue.parse(jsonString);
        
        String recordControl = "{\"FirstName\":\"Foo\",\"$.custom_fields[*].kind\":"
            + "[\"Total activities\"]}";
        
        Record record = helper.buildRecord(list, jsonobj);
        String recordString = ((JSONObject)record.getRecord()).toJSONString();
        
        IOException ioError = null;
        try {
            assertEquals(mapper.readTree(recordString), mapper.readTree(recordControl));
        } catch (IOException e) {
            ioError = e;
        }
        assertNull(ioError);
        
        // jsonPath is valid but property does not exist
        recordControl = "{\"FirstName\":\"Foo\",\"$.custom_fields[*].kind\":null}";
        
        jsonobj.remove("custom_fields");
        record = helper.buildRecord(list, jsonobj);
        recordString = ((JSONObject)record.getRecord()).toJSONString();
        
        try {
            assertEquals(mapper.readTree(recordString), mapper.readTree(recordControl));
        } catch (IOException e) {
            ioError = e;
        }
        assertNull(ioError);
        
        // jsonPath is invalid
        list.add("$[custom_fields]");
        
        JsonPathException error = null;
        try {
            helper.buildRecord(list, jsonobj);
        } catch (JsonPathException e) {
            error = e;
        }
        
        assertNotNull(error);
    }
}
