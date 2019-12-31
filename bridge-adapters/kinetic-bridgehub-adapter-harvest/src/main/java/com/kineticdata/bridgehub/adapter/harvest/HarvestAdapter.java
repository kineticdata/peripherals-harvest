package com.kineticdata.bridgehub.adapter.harvest;

import com.kineticdata.bridgehub.adapter.BridgeAdapter;
import com.kineticdata.bridgehub.adapter.BridgeError;
import com.kineticdata.bridgehub.adapter.BridgeRequest;
import com.kineticdata.bridgehub.adapter.BridgeUtils;
import com.kineticdata.bridgehub.adapter.Count;
import com.kineticdata.bridgehub.adapter.Record;
import com.kineticdata.bridgehub.adapter.RecordList;
import com.kineticdata.commons.v1.config.ConfigurableProperty;
import com.kineticdata.commons.v1.config.ConfigurablePropertyMap;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import org.apache.http.HttpEntity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.LoggerFactory;

public class HarvestAdapter implements BridgeAdapter {
    /*----------------------------------------------------------------------------------------------
     * PROPERTIES
     *--------------------------------------------------------------------------------------------*/
    
    /** Defines the adapter display name */
    public static final String NAME = "Harvest Application Bridge";
    
    /** Defines the logger */
    protected static final org.slf4j.Logger logger = LoggerFactory.getLogger(HarvestAdapter.class);
    
    /** Adapter version constant. */
    public static String VERSION;
    /** Load the properties version from the version.properties file. */
    static {
        try {
            java.util.Properties properties = new java.util.Properties();
            properties.load(HarvestAdapter.class.getResourceAsStream("/"+HarvestAdapter.class.getName()+".version"));
            VERSION = properties.getProperty("version");
        } catch (IOException e) {
            logger.warn("Unable to load "+HarvestAdapter.class.getName()+" version properties.", e);
            VERSION = "Unknown";
        }
    }
    
    /** Defines the collection of property names for the adapter */
    public static class Properties {
        public static final String PROPERTY_ACCESS_TOKEN = "Access Token";
        public static final String PROPERTY_ACCOUNT_ID = "Account Id";
    }
    
    private HarvestQualificationParser parser;
    
    private final ConfigurablePropertyMap properties = new ConfigurablePropertyMap(
        new ConfigurableProperty(Properties.PROPERTY_ACCESS_TOKEN).setIsRequired(true),
        new ConfigurableProperty(Properties.PROPERTY_ACCOUNT_ID)
    );

    // Local variables to store the property values in
    private String accessToken;
    private String accountId;
    private String harvestEndpoint;
    
    /*---------------------------------------------------------------------------------------------
     * SETUP METHODS
     *-------------------------------------------------------------------------------------------*/
    
    @Override
    public void initialize() throws BridgeError {
        // Initializing the variables with the property values that were passed
        // when creating the bridge so that they are easier to use
        this.accessToken = properties.getValue(Properties.PROPERTY_ACCESS_TOKEN);
        this.accountId = properties.getValue(Properties.PROPERTY_ACCOUNT_ID);
        this.harvestEndpoint = "https://api.harvestapp.com/v2";
        this.parser = new HarvestQualificationParser();
    }

    @Override
    public String getName() {
        return NAME;
    }
    
    @Override
    public String getVersion() {
       return  VERSION;
    }
    
    @Override
    public void setProperties(Map<String,String> parameters) {
        // This should always be the same unless there are special circumstances
        // for changing it
        properties.setValues(parameters);
    }
    
    @Override
    public ConfigurablePropertyMap getProperties() {
        // This should always be the same unless there are special circumstances
        // for changing it
        return properties;
    }
    
    // Structures that are valid to use in the bridge. Used to check against
    // when a method is called to make sure that the Structure the user is
    // attempting to call is valid
    public static final List<String> VALID_STRUCTURES = 
        Arrays.asList(new String[] {
            "Contacts", "Clients", "Invoice Messages", "Invoice Payments",
            "Invoices", "Invoice Item Categories", "Estimate Messages", 
            "Estimates", "Estimate Item Categories", "Expenses", 
            "Expense Categories", "Tasks", "Time Entries", "User Assignments",
            "Task Assignments", "Projects", "Roles", "Billable Rates", 
            "Cost Rates", "Project Assignments", "Users"
        });
    
    /*---------------------------------------------------------------------------------------------
     * IMPLEMENTATION METHODS
     *-------------------------------------------------------------------------------------------*/

    @Override
    public Count count(BridgeRequest request) throws BridgeError {        
        // Check if the inputted structure is valid
        if (!VALID_STRUCTURES.contains(request.getStructure())) {
            throw new BridgeError("Invalid Structure: '"
                + request.getStructure() + "' is not a valid structure");
        }
        
        request.setQuery(substituteQueryParameters(request));
        Map<String, String> parameters = parser.getParameters(request.getQuery());
        Map<String, NameValuePair> parameterMap = buildNameValuePairMap(parameters);
        
        // Log the access
        logger.trace("Counting records");
        logger.trace("  Structure: " + request.getStructure());
        logger.trace("  Query: " + request.getQuery());
       
        // Retrieve the objects based on the structure from the source
        String output = getResource(getUrl(request, parameterMap));
        logger.trace("Count Output: "+output);

        // Parse the response string into a JSONObject
        JSONObject object = (JSONObject)JSONValue.parse(output);
        
        // Get the number of elements in the returned array
        Long tempCount = (Long)object.get("total_entries");
        Integer count = 0;
        // Single results will not have a total_entries property
        if (tempCount == null) {
            Long singleResult = (Long)object.get("id");
            if (singleResult == null) {
                throw new BridgeError("The Count result was unexpected.  Please"
                        + "check query and rerun.");
            } else {
                // If object has id property assume a single result for found
                count = 1;
            }
        } else {
            count = (int)tempCount.intValue();
        }
        
        // Create and return a count object that contains the count
        return new Count(count);
    }

    @Override
    public Record retrieve(BridgeRequest request) throws BridgeError {
        // Check if the inputted structure is valid
        if (!VALID_STRUCTURES.contains(request.getStructure())) {
            throw new BridgeError("Invalid Structure: '"
                + request.getStructure() + "' is not a valid structure");
        }
        
        request.setQuery(substituteQueryParameters(request));
        Map<String, String> parameters = parser.getParameters(request.getQuery());
        Map<String, NameValuePair> parameterMap = buildNameValuePairMap(parameters);
        
        // Log the access
        logger.trace("Retrieving Kinetic Request CE Record");
        logger.trace("  Structure: " + request.getStructure());
        logger.trace("  Query: " + request.getQuery());
        logger.trace("  Fields: " + request.getFieldString());

        // Retrieve the objects based on the structure from the source
        String output = getResource(getUrl(request, parameterMap));
        logger.trace("Retrieve Output: "+output);
        
        // Parse the response string into a JSONObject
        JSONObject obj = (JSONObject)JSONValue.parse(output);
        
        // Check if results are singular or mutiple
        // TODO: support results with a list that has one element
        JSONArray objects = (JSONArray)obj.get(
            request.getStructure().replaceAll(" ", "_").toLowerCase()
        );
        if (objects != null) {
            throw new BridgeError("Multiple results found for retrieve.  This is"
                    + "invalid.");
        }
        
        List<String> fields = request.getFields();
        if (fields == null) { 
            fields = new ArrayList();
        }
        
        if(fields.isEmpty()){
            fields.addAll(obj.keySet());
        }
        
        // If specific fields were specified then we remove all of the 
        // nonspecified properties from the object.
        Set<Object> removeKeySet = new HashSet<Object>();
        for(Object key: obj.keySet()){
            if(fields.contains(key)){
                continue;
            }else{
                logger.trace("Remove Key: "+key);
                removeKeySet.add(key);
            }
        }
        obj.keySet().removeAll(removeKeySet);
        
        Object[] keys = obj.keySet().toArray();
        JSONObject convertedObj = convertValues(obj,keys);
        
        // Create a Record object from the responce JSONObject
        Record record;
        if (convertedObj != null) {
            record = new Record(convertedObj);
        } else {
            record = new Record();
        }
        
        // Return the created Record object
        return record;
    }

    @Override
    public RecordList search(BridgeRequest request) throws BridgeError {
        // Check if the inputted structure is valid
        if (!VALID_STRUCTURES.contains(request.getStructure())) {
            throw new BridgeError("Invalid Structure: '"
                + request.getStructure() + "' is not a valid structure.");
        }
        
        request.setQuery(substituteQueryParameters(request));
        Map<String, String> parameters = addPaginationFromMetadata(
            parser.getParameters(request.getQuery()), request.getMetadata());
        Map<String, NameValuePair> parameterMap = buildNameValuePairMap(parameters);
        
        // Log the access
        logger.trace("Searching Records");
        logger.trace("  Structure: " + request.getStructure());
        if (request.getQuery() != null) {
            logger.trace("  Query: " + request.getQuery());
        }
        if (request.getFieldString() != null) {
            logger.trace("  Fields: " + request.getFieldString());
        }
        
        // Retrieve the objects based on the structure from the source
        String output;
        if (request.getMetadata() != null) {
            output = getResource(getUrl(request, parameterMap));
        } else {
            // TODO: support order and pagination.
            output = getResource(getUrl(request, parameterMap));
        }
        
        logger.trace("Search Output: " + output);
        
        // Parse the response string into a JSONObject
        JSONObject obj = (JSONObject)JSONValue.parse(output);
        
        // Get the array of objects. Each Structure has a different accessor name.
        JSONArray objects = (JSONArray)obj.get(
            request.getStructure().replaceAll(" ", "_").toLowerCase()
        );
        
        // Create a List of records that will be used to make a RecordList object.
        List<Record> recordList = new ArrayList<Record>();
        
        // If the user doesn't enter any values for fields we return all of the fields.
        List<String> fields = request.getFields();
        if (fields == null) { 
            fields = new ArrayList();
        }        

        if(objects.isEmpty() != true){
            JSONObject firstObj = (JSONObject)objects.get(0);

            Object[] keys = firstObj.keySet().toArray();
            
            Set<Object> removeKeySet = new HashSet<Object>();
            
            // If no keys where provided to the search then we return all
            // properties.
            if(fields.isEmpty()){
                fields.addAll(firstObj.keySet());
            } else {
            
                // If specific fields were specified then we remove all of the 
                // nonspecified properties from the object.
                for(Object key: firstObj.keySet()){
                    if(fields.contains(key)){
                        continue;
                    } else {
                        logger.trace("Remove Key: "+key);
                        removeKeySet.add(key);
                    }
                }
            }
            
            // Iterate through the responce objects and make a new Record for each.
            for (Object o : objects) {
                JSONObject object = (JSONObject)o;

                // Remove all keys that are not in the fields list.
                object.keySet().removeAll(removeKeySet);
                
                // Reset keys to the new object's keySet.
                keys = object.keySet().toArray();
                
                JSONObject convertedObj = convertValues(object,keys);
                Record record;
                if (convertedObj != null) {
                    record = new Record(convertedObj);
                } else {
                    record = new Record();
                }
                // Add the created record to the list of records
                recordList.add(record);
            }
        }
        
        Map<String,String> metadata = new LinkedHashMap<String,String>();
        metadata.put("next_page", String.valueOf(obj.get("next_page")));
        
        // Return the RecordList object
        return new RecordList(fields, recordList, metadata);
    }
    
    /*--------------------------------------------------------------------------
     * HELPER METHODS
     *------------------------------------------------------------------------*/
    protected Map<String, NameValuePair> buildNameValuePairMap(Map<String, String> parameters) {
        Map<String, NameValuePair> parameterMap = new HashMap<>();

        parameters.forEach((key, value) -> {
            parameterMap.put(key, new BasicNameValuePair(key, value));
        });
        
        return parameterMap;
    }

    private Map<String, String> addPaginationFromMetadata(Map<String, String> parameters,
        Map<String, String> metadata) {
        
        if (metadata != null) {
            if (metadata.containsKey("page")) {
                parameters.putIfAbsent("page", metadata.get("page"));
            }
            if (metadata.containsKey("per_page")) {
                parameters.putIfAbsent("per_page", metadata.get("per_page"));
            }
        }
        
        return parameters;
    }
    
    protected String getUrl (BridgeRequest request,
        Map<String, NameValuePair> parameters) {
        
        return String.format("%s/%s?%s", this.harvestEndpoint, 
            parser.parsePath(request.getQuery()), 
            URLEncodedUtils.format(parameters.values(), Charset.forName("UTF-8")));
    }
    
    // Count Search and Retrieve get the resoucre the same and use the same
    // output object.
    private String getResource(String url) throws BridgeError {
        
        // Initialize the HTTP Client,Response, and HTTP GET objects
        HttpClient client = new DefaultHttpClient();
        HttpResponse response;
        HttpGet get = new HttpGet(url);

        // Append HTTP BASIC Authorization header to HttpGet call
        String accessToken = this.accessToken;
        String accountId = this.accountId;
        get.setHeader("Authorization", "Bearer " + accessToken);
        get.setHeader("Harvest-Account-ID", accountId);
        get.setHeader("Content-Type", "application/json");
        get.setHeader("Accept", "application/json");
        
        // Make the call to the source to retrieve data and convert the response
        // from a HttpEntity object into a Java String
        String output = "";
        try {
            response = client.execute(get);
            HttpEntity entity = response.getEntity();
            output = EntityUtils.toString(entity);
            int responseCode = response.getStatusLine().getStatusCode();
            logger.trace("Request response code: " + responseCode);
            if(responseCode == 404){
                throw new BridgeError("404 Page not found at "+url+".");
            }else if(responseCode == 401){
                throw new BridgeError("401 Access on valid.");
            }
        } 
        catch (IOException e) {
            logger.error(e.getMessage());
            throw new BridgeError("Unable to make a connection to Kinetic Core.", e);
        }
        return output;
    }
 
    // This method converts non string values to strings.
    private JSONObject convertValues(JSONObject obj,Object[] keys){
        for (Object key : keys){
            Object value = obj.get((String)key);
            if (!(value instanceof String)) {
                logger.trace("Converting: " + String.valueOf(value) + " to a string");
                obj.put((String)key, String.valueOf(value));
            }
        } 
        return obj;
    }
    
    private String substituteQueryParameters(BridgeRequest request) throws BridgeError {
        // Parse the query and exchange out any parameters with their parameter 
        // values. ie. change the query username=<%=parameter["Username"]%> to
        // username=test.user where parameter["Username"]=test.user
        HarvestQualificationParser parser = new HarvestQualificationParser();
        return parser.parse(request.getQuery(),request.getParameters());
    }
}
