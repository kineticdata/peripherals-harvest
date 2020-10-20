package com.kineticdata.bridgehub.adapter.harvest;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.JsonPathException;
import com.kineticdata.bridgehub.adapter.BridgeAdapter;
import com.kineticdata.bridgehub.adapter.BridgeError;
import com.kineticdata.bridgehub.adapter.BridgeRequest;
import com.kineticdata.bridgehub.adapter.Count;
import com.kineticdata.bridgehub.adapter.Record;
import com.kineticdata.bridgehub.adapter.RecordList;
import com.kineticdata.commons.v1.config.ConfigurableProperty;
import com.kineticdata.commons.v1.config.ConfigurablePropertyMap;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.LoggerFactory;

public class HarvestAdapter implements BridgeAdapter {
    /*----------------------------------------------------------------------------------------------
     * CONSTRUCTOR
     *--------------------------------------------------------------------------------------------*/
    public HarvestAdapter () {
        // Parse the query and exchange out any parameters with their parameter 
        // values. ie. change the query username=<%=parameter["Username"]%> to
        // username=test.user where parameter["Username"]=test.user 
        parser = new HarvestQualificationParser();
        
    }
    
    /*----------------------------------------------------------------------------------------------
     * STRUCTURES
     *      AdapterMapping( Structure Name, accessor, Path Function)
     *--------------------------------------------------------------------------------------------*/
    public static Map<String,AdapterMapping> MAPPINGS 
        = new HashMap<String,AdapterMapping>() {{
        put("Contacts", new AdapterMapping("Contacts", "contacts",
            HarvestAdapter::pathContacts));
        put("Clients", new AdapterMapping("Clients", "clients",
            HarvestAdapter::pathClients));
        put("Invoices", new AdapterMapping("Invoices", "invoices",
            HarvestAdapter::pathInvoices));
        put("Invoice Item Categories", new AdapterMapping("Invoice Item Categories",
            "invoice_item_categories", HarvestAdapter::pathInvoiceItemCategories));
        put("Estimates", new AdapterMapping("Estimates", "estimates",
            HarvestAdapter::pathEstimates));
        put("Estimate Item Categories", new AdapterMapping("Estimate Item Categories",
            "estimate_item_categories", HarvestAdapter::pathEstimateItemCategories));
//        put("Expenses", new AdapterMapping("Expenses",
//            HarvestAdapter::pathExpenses));
//        put("Expense Categories", new AdapterMapping("Expense Categories",
//            HarvestAdapter::pathExpenseCategories));
        put("Tasks", new AdapterMapping("Tasks", "tasks",
            HarvestAdapter::pathTasks));
        put("Time Entries", new AdapterMapping("Time Entries", "time_entries",
            HarvestAdapter::pathTimeEntries));
        put("User Assignments", new AdapterMapping("User Assignments",
            "user_assignments", HarvestAdapter::pathUserAssignments));
        put("Task Assignments", new AdapterMapping("Task Assignments",
            "task_assignments", HarvestAdapter::pathTaskAssignments));
        put("Projects", new AdapterMapping("Projects", "projects",
            HarvestAdapter::pathProjects));
//        put("Roles", new AdapterMapping("Roles",
//            HarvestAdapter::pathRoles));
//        put("Billable Rates", new AdapterMapping("Billable Rates",
//            HarvestAdapter::pathBillableRates));
//        put("Cost Rates", new AdapterMapping("Cost Rates",
//            HarvestAdapter::pathCostRates));
//        put("Project Assignments", new AdapterMapping("Project Assignments",
//            HarvestAdapter::pathProjectAssignments));
        put("Users", new AdapterMapping("Users", "users",
            HarvestAdapter::pathUsers));
        put("Reports", new AdapterMapping("Reports", "results",
            HarvestAdapter::pathReports));
        put("Adhoc", new AdapterMapping("Adhoc", "",
            HarvestAdapter::pathAdhoc));
    }};

    /*----------------------------------------------------------------------------------------------
     * PROPERTIES
     *--------------------------------------------------------------------------------------------*/

    /** Defines the adapter display name */
    public static final String NAME = "Harvest Bridge";

    /** Defines the LOGGER */
    protected static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(HarvestAdapter.class);

    /** Adapter version constant. */
    public static String VERSION;
    /** Load the properties version from the version.properties file. */
    static {
        try {
            java.util.Properties properties = new java.util.Properties();
            properties
                    .load(HarvestAdapter.class.getResourceAsStream("/" + HarvestAdapter.class.getName() + ".version"));
            VERSION = properties.getProperty("version");
        } catch (IOException e) {
            LOGGER.warn("Unable to load " + HarvestAdapter.class.getName() + " version properties.", e);
            VERSION = "Unknown";
        }
    }

    /** Defines the collection of property names for the adapter */
    public static class Properties {
        public static final String PROPERTY_ACCESS_TOKEN = "Access Token";
        public static final String PROPERTY_ACCOUNT_ID = "Account Id";
    }
    private final ConfigurablePropertyMap properties = new ConfigurablePropertyMap(
            new ConfigurableProperty(Properties.PROPERTY_ACCESS_TOKEN).setIsRequired(true),
            new ConfigurableProperty(Properties.PROPERTY_ACCOUNT_ID));

    // Local variables to store the property values in
    private final HarvestQualificationParser parser;
    private HarvestApiHelper apiHelper;

    private static final String API_PATH = "https://api.harvestapp.com/v2";

    /*---------------------------------------------------------------------------------------------
     * SETUP METHODS
     *-------------------------------------------------------------------------------------------*/

    @Override
    public void initialize() throws BridgeError {
        // Initializing the variables with the property values that were passed
        // when creating the bridge so that they are easier to use
        String accessToken = properties.getValue(Properties.PROPERTY_ACCESS_TOKEN);
        String accountId = properties.getValue(Properties.PROPERTY_ACCOUNT_ID);
        apiHelper = new HarvestApiHelper(API_PATH, accessToken, accountId);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public void setProperties(Map<String, String> parameters) {
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
    /*---------------------------------------------------------------------------------------------
     * IMPLEMENTATION METHODS
     *-------------------------------------------------------------------------------------------*/

    @Override
    public Count count(BridgeRequest request) throws BridgeError {
        // Log the access
        LOGGER.trace("Counting records");
        LOGGER.trace("  Structure: " + request.getStructure());
        if (request.getQuery() != null) {
            LOGGER.trace("  Query: " + request.getQuery());
        }

        // parse Structure
        List<String> structureList = Arrays.asList(request.getStructure().trim()
            .split("\\s*>\\s*"));
        // get Structure model
        AdapterMapping mapping = getMapping(structureList.get(0));
        
        Map<String, String> parameters = getParameters(
            parser.parse(request.getQuery(),request.getParameters()), mapping);
        
        // Path builder functions may mutate the parameters Map;
        String path = mapping.getPathbuilder().apply(structureList, parameters);
        
        Map<String, NameValuePair> parameterMap = buildNameValuePairMap(parameters);
       
        // Retrieve the objects based on the structure from the source
        JSONObject responseObject = apiHelper.executeRequest(getUrl(path, parameterMap));
        
        // Get the number of elements in the returned array
        Long tempCount = (Long)responseObject.get("total_entries");
        Integer count = 0;
        // Single results will not have a total_entries property
        if (tempCount == null) {
            Long singleResult = (Long)responseObject.get("id");
            if (singleResult == null) {
                throw new BridgeError("The Count result was unexpected.  Please"
                        + "check query and rerun.");
            } else {
                // If object has id property assume a single result for found
                count = 1;
            }
        } else {
            count = (int) tempCount.intValue();
        }

        // Create and return a count object that contains the count
        return new Count(count);
    }

    @Override
    public Record retrieve(BridgeRequest request) throws BridgeError {
        // Log the access
        LOGGER.trace("Retrieving Kinetic Request CE Record");
        LOGGER.trace("  Structure: " + request.getStructure());
        if (request.getQuery() != null) {
            LOGGER.trace("  Query: " + request.getQuery());
        }
        if (request.getFieldString() != null) {
            LOGGER.trace("  Fields: " + request.getFieldString());
        }
        
        // parse Structure
        List<String> structureList = Arrays.asList(request.getStructure().trim()
            .split("\\s*>\\s*"));
        // get Structure model
        AdapterMapping mapping = getMapping(structureList.get(0));
        
        Map<String, String> parameters = getParameters(
            parser.parse(request.getQuery(),request.getParameters()), mapping);
        
        // Path builder functions may mutate the parameters Map;
        String path = mapping.getPathbuilder().apply(structureList, parameters);
                
        // Accessor values is either passed as a parameter in the qualification
        // mapping for Adhoc or on the mapping for all other structures.
        String accessor = getAccessor(mapping, parameters);
        
        Map<String, NameValuePair> parameterMap = buildNameValuePairMap(parameters);

        // Retrieve the objects based on the structure from the source
        JSONObject responseObject = apiHelper.executeRequest(getUrl(path, parameterMap));
        
        JSONArray responseArray = new JSONArray();
        if (responseObject.containsKey(accessor)) {
            responseArray = getResponseData(responseObject.get(accessor));
        } else {
            responseArray = getResponseData(responseObject);
        }
        
        Record record = new Record();
        if (responseArray.size() == 1) {
            // Reassign object to single result 
            JSONObject object = (JSONObject)responseArray.get(0);
                
            List<String> fields = getFields(request.getFields() == null ? 
                new ArrayList() : request.getFields(), object);
            record = buildRecord(fields, object);
        } else if (responseArray.isEmpty()) {
            LOGGER.debug("No results found for query: {}", request.getQuery());
        } else {
            throw new BridgeError ("Retrieve must return a single result."
                + " Multiple results found.");
        }

        // Return the created Record object
        return record;
    }

    @Override
    public RecordList search(BridgeRequest request) throws BridgeError {
        // Log the access
        LOGGER.trace("Searching Records");
        LOGGER.trace("  Structure: " + request.getStructure());
        if (request.getQuery() != null) {
            LOGGER.trace("  Query: " + request.getQuery());
        }
        if (request.getFieldString() != null) {
            LOGGER.trace("  Fields: " + request.getFieldString());
        }
        
         // parse Structure
        List<String> structureList = Arrays.asList(request.getStructure().trim()
            .split("\\s*>\\s*"));
        // get Structure model
        AdapterMapping mapping = getMapping(structureList.get(0));
        
        Map<String, String> parameters = addPaginationFromMetadata(getParameters(
            parser.parse(request.getQuery(),request.getParameters()), mapping),
            request.getMetadata());
        
        // Path builder functions may mutate the parameters Map;
        String path = mapping.getPathbuilder().apply(structureList, parameters);
        
        // Accessor values is either passed as a parameter in the qualification
        // mapping for Adhoc or on the mapping for all other structures.
        String accessor = getAccessor(mapping, parameters);
        
        Map<String, NameValuePair> parameterMap = buildNameValuePairMap(parameters);
        
        // Retrieve the objects based on the structure from the source
        JSONObject responseObject = apiHelper.executeRequest(getUrl(path, 
            parameterMap));
        
        JSONArray responseArray = new JSONArray();
        if (responseObject.containsKey(accessor)) {
            responseArray = getResponseData(responseObject.get(accessor));
        } else {
            responseArray = getResponseData(responseObject);
        }
        
        // Create a List of records that will be used to make a RecordList object.
        List<Record> recordList = new ArrayList<Record>();      
        List<String> fields = request.getFields() == null ? new ArrayList() : 
            request.getFields();        
        if(responseArray != null && responseArray.isEmpty() != true){
            fields = getFields(fields, (JSONObject)responseArray.get(0));

            // Iterate through the responce objects and make a new Record for each.
            for (Object o : responseArray) {
                JSONObject obj = (JSONObject)o;
                Record record = buildRecord(fields, obj);
                
                // Add the created record to the list of records
                recordList.add(record);
            }
        }

        Map<String, String> metadata = new LinkedHashMap<String, String>();
        metadata.put("next_page", String.valueOf(responseObject.get("next_page")));

        // Return the RecordList object
        return new RecordList(fields, recordList, metadata);
    }

    /*--------------------------------------------------------------------------
     * HELPER METHODS
     *------------------------------------------------------------------------*/
    protected List<String> getFields(List<String> fields, JSONObject jsonobj) {
        // if no fields were provided then all fields will be returned. 
        if(fields.isEmpty()){
            fields.addAll(jsonobj.keySet());
        }
        
        return fields;
    }
    
    /**
     * Build a Record.  If no fields are provided all fields will be returned.
     * 
     * @param fields
     * @param jsonobj
     * @return Record
     */
    protected Record buildRecord (List<String> fields, JSONObject jsonobj) {
        JSONObject obj = new JSONObject();
        DocumentContext jsonContext = JsonPath.parse(jsonobj); 
        
        fields.stream().forEach(field -> {
            // either use JsonPath or just add the field value.  We're assuming
            // all JsonPath usages will begin with $[ or $.. 
            if (field.startsWith("$.") || field.startsWith("$[")) {
                try {
                    obj.put(field, jsonContext.read(field));
                } catch (JsonPathException e) {
                    // if field is a valid path but object is missing the property
                    // return null for field.  This is consistent with existing 
                    // adapter behavior.
                    if (e.getMessage().startsWith("Missing property")) {
                        obj.put(field, null);
                        LOGGER.debug(String.format("%s was not found, returning"
                            + " null value", field), e);
                    } else {   
                        throw new JsonPathException(String.format("There was an issue"
                            + " reading %s", field), e);
                    }
                }
            } else {
                obj.put(field, jsonobj.get(field));
            }
        });
        
        Record record = new Record(obj, fields);
        return record;
    }
    
        
    protected JSONArray getResponseData(Object responseData) {
        JSONArray responseArray = new JSONArray();
        
        if (responseData instanceof JSONArray) {
            responseArray = (JSONArray)responseData;
        }
        else if (responseData instanceof JSONObject) {
            // It's an object
            responseArray.add((JSONObject)responseData);
        }
        
        return responseArray;
    }
    
    /**
     * Get accessor value. If structure is Adhoc remove accessor from parameters.
     * 
     * @param mapping
     * @param parameters
     * @return 
     */
    private String getAccessor(AdapterMapping mapping, Map<String, String> parameters) {
        String accessor;
        
        if (mapping.getStructure().equals("Adhoc")) {
            accessor = parameters.get("accessor");
            parameters.remove("accessor");
        } else {
            accessor = mapping.getAccessor();
        }
        
        return accessor;
    }
    
    /**
     * This helper is intended to abstract the parser get parameters from the core
     * methods.
     * 
     * @param request
     * @param mapping
     * @return
     * @throws BridgeError
     */
    protected Map<String, String> getParameters(String query, AdapterMapping mapping) throws BridgeError {

        Map<String, String> parameters = new HashMap<>();
        if (mapping.getStructure() == "Adhoc") {
            // Adhoc qualifications are two segments. ie path?queryParameters
            String[] segments = query.split("[?]", 2);

            // getParameters only needs the queryParameters segment
            if (segments.length > 1) {
                parameters = parser.getParameters(segments[1]);
            }
            // Pass the path along to the functional operator
            parameters.put("adapterPath", segments[0]);
        } else {
            parameters = parser.getParameters(query);
        }

        return parameters;
    }

    /**
     * This method checks that the structure on the request matches on in the
     * Mapping internal class. Mappings map directly to the adapters supported
     * Structures.
     * 
     * @param structure
     * @return Mapping
     * @throws BridgeError
     */
    protected AdapterMapping getMapping(String structure) throws BridgeError {
        AdapterMapping mapping = MAPPINGS.get(structure);
        if (mapping == null) {
            throw new BridgeError("Invalid Structure: '" + structure + "' is not a valid structure");
        }
        return mapping;
    }

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
    
    protected String getUrl (String path,
        Map<String, NameValuePair> parameters) {
        
        return String.format("%s?%s", path, 
            URLEncodedUtils.format(parameters.values(), Charset.forName("UTF-8")));
    }
    
    /**
     * Ensure that the sort order list is linked so that order can not be changed.
     * 
     * @param uncastSortOrderItems
     * @return
     * @throws IllegalArgumentException 
     */
    private LinkedHashMap<String, String> 
        getSortOrderItems (Map<String, String> uncastSortOrderItems)
        throws IllegalArgumentException{
        
        /* results of parseOrder does not allow for a structure that 
         * guarantees order.  Casting is required to preserver order.
         */
        if (!(uncastSortOrderItems instanceof LinkedHashMap)) {
            throw new IllegalArgumentException("Sort Order Items was invalid.");
        }
        
        return (LinkedHashMap)uncastSortOrderItems;
    }
    
    /**************************** Path Definitions ****************************/
    protected static String pathContacts(List<String> structureList,
        Map<String, String> parameters) throws BridgeError {
        
        String path = "/contacts";
        if (parameters.containsKey("id")) {
            path = String.format("%s/%s", path, parameters.get("id"));
            parameters.remove("id");
        }
        
        return path;
    }   
    
    protected static String pathClients(List<String> structureList,
        Map<String, String> parameters) throws BridgeError {

        String path = "/clients";
        if (parameters.containsKey("id")) {
            path = String.format("%s/%s", path, parameters.get("id"));
            parameters.remove("id");
        }
        
        return path;
    }
    
    protected static String pathInvoices(List<String> structureList,
        Map<String, String> parameters) throws BridgeError {

        String path = "/clients";
        if (parameters.containsKey("id")) {
            path = String.format("%s/%s", path, parameters.get("id"));
            parameters.remove("id");
        }
        if (structureList.contains("Payments")) {
            path = String.format("%s/%s", path, "payments");
        } else if (structureList.contains("Messages")) {
            path = String.format("%s/%s", path, "messages");
        }
        
        return path;
    }          
       
    protected static String pathInvoiceItemCategories(List<String> structureList,
        Map<String, String> parameters) throws BridgeError {

        String path = "/invoice_item_categories";
        if (parameters.containsKey("id")) {
            path = String.format("%s/%s", path, parameters.get("id"));
            parameters.remove("id");
        }
        
        return path;
    }          
       
    protected static String pathEstimates(List<String> structureList,
        Map<String, String> parameters) throws BridgeError {

        String path = "/estimates";
        if (parameters.containsKey("id")) {
            path = String.format("%s/%s", path, parameters.get("id"));
            parameters.remove("id");
        }
        if (structureList.contains("Messages")) {
            path = String.format("%s/%s", path, "messages");
        }
        
        return path;
    }
    
    protected static String pathEstimateItemCategories(List<String> structureList,
        Map<String, String> parameters) throws BridgeError {

        String path = "/estimate_item_categories";
        if (parameters.containsKey("id")) {
            path = String.format("%s/%s", path, parameters.get("id"));
            parameters.remove("id");
        }
        
        return path;
    }
    
//    protected static String pathExpenses(List<String> structureList,
//        Map<String, String> parameters) throws BridgeError {
//
//        return path;
//    }          
//    protected static String pathExpenseCategories(List<String> structureList,
//        Map<String, String> parameters) throws BridgeError {
//
//        return path;
//    }
    
    protected static String pathTasks(List<String> structureList,
        Map<String, String> parameters) throws BridgeError {

        String path = "/tasks";
        if (parameters.containsKey("id")) {
            path = String.format("%s/%s", path, parameters.get("id"));
            parameters.remove("id");
        }
        
        return path;
    }     
    
    protected static String pathTimeEntries(List<String> structureList,
        Map<String, String> parameters) throws BridgeError {

        String path = "/time_entries";
        if (parameters.containsKey("id")) {
            path = String.format("%s/%s", path, parameters.get("id"));
            parameters.remove("id");
        }
        
        return path;
    }
    
    protected static String pathUserAssignments(List<String> structureList,
        Map<String, String> parameters) throws BridgeError {

        String path = "/user_assignments";
        
        return path;
    } 
    
    protected static String pathTaskAssignments(List<String> structureList,
        Map<String, String> parameters) throws BridgeError {

        String path = "/task_assignments";

        return path;
    }      
            
    protected static String pathProjects(List<String> structureList,
        Map<String, String> parameters) throws BridgeError {

        String path = "/projects";
        if (parameters.containsKey("id")) {
            path = String.format("%s/%s", path, parameters.get("id"));
            parameters.remove("id");
        }
        if (structureList.contains("Task Assignments")) {
            path = String.format("%s/%s", path, "task_assignments");
            if (parameters.containsKey("task_assignment_id")) {
                path = String.format("%s/%s", path, 
                    parameters.get("task_assignment_id"));
                parameters.remove("task_assignment_id");
            }
        }
        if (structureList.contains("User Assignments")) {
            path = String.format("%s/%s", path, "user_assignments");
            if (parameters.containsKey("user_assignment_id")) {
                path = String.format("%s/%s", path, 
                    parameters.get("user_assignment_id"));
                parameters.remove("user_assignment_id");
            }
        }
        
        return path;
    }          
    
//    protected static String pathRoles(List<String> structureList,
//        Map<String, String> parameters) throws BridgeError {
//
//        return path;
//    }          
//    protected static String pathBillableRates(List<String> structureList,
//        Map<String, String> parameters) throws BridgeError {
//
//        return path;
//    }          
//    protected static String pathCostRates(List<String> structureList,
//        Map<String, String> parameters) throws BridgeError {
//
//        return path;
//    }          
//    protected static String pathProjectAssignments(List<String> structureList,
//        Map<String, String> parameters) throws BridgeError {
//
//        return path;
//    }         
    
    protected static String pathUsers(List<String> structureList,
        Map<String, String> parameters) throws BridgeError {
        
        String path = "/users";
        if (parameters.containsKey("id")) {
            path = String.format("%s/%s", path, parameters.get("id"));
            parameters.remove("id");
        }
        
        return path;
    }
    
    protected static String pathReports(List<String> structureList, Map<String, String> parameters) throws BridgeError {
        if (structureList.size() < 2) {
            throw new BridgeError("The Reports structure requires at lease two segments");
        }
        
        String path = "/reports";

        switch (structureList.get(1)) {
            case "Expenses":
                path = path + "/expenses";
                if (parameters.containsKey("report_type")) {
                    path = String.format("%s/%s", path, parameters.get("report_type"));
                    parameters.remove("report_type");
                } else {
                    throw new BridgeError("The Reports > Expenses structure requires"
                        + " a parameter of report_type.  valid report types are:"
                        + " clients, projects, categories, and team.");
                }
                break;
            case "Uninvoiced":
                path = path + "/uninvoiced";
                break;
            case "Time":
                path = path + "/time";
                if (parameters.containsKey("report_type")) {
                   path = String.format("%s/%s", path, parameters.get("report_type"));
                    parameters.remove("report_type");
                } else {
                    throw new BridgeError("The Reports > Time structure requires"
                        + " a parameter of report_type.  valid report types are:"
                        + " clients, projects, tasks, and team.");
                }
                break;
            case "Project Budget":
                path = path + "/project_budget";
                break;
            default:
                throw new BridgeError(String.format("The Reports structure does "
                    + "not have a %s segment.  Valid second segments are Expenses,"
                    + " Uninvoiced, Time, Project Budget.", structureList.get(1)));        
        }
        
        return path;
    }
    
    /**
     * Build path for Adhoc structure.
     * 
     * @param structureList
     * @param parameters
     * @return
     * @throws BridgeError 
     */
    protected static String pathAdhoc(List<String> structureList, 
        Map<String, String> parameters) throws BridgeError {
        
        return parameters.get("adapterPath");
    }

    /**
     * Checks if a parameter exists in the parameters Map.
     * 
     * @param param
     * @param parameters
     * @param structureList
     * @throws BridgeError 
     */
    protected static void checkRequiredParamForStruct(String param,
        Map<String, String> parameters, List<String> structureList)
        throws BridgeError{
        
        if (!parameters.containsKey(param)) {
            String structure = String.join(" > ", structureList);
            throw new BridgeError(String.format("The %s structure requires %s"
                + "parameter.", structure, param));
        }
    }
}
