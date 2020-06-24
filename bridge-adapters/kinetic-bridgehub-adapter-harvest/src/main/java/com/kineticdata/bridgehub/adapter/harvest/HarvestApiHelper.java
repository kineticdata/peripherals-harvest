package com.kineticdata.bridgehub.adapter.harvest;

import com.kineticdata.bridgehub.adapter.BridgeError;
import java.io.IOException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a Rest service helper.
 */
public class HarvestApiHelper {
    private static final Logger LOGGER = 
        LoggerFactory.getLogger(HarvestApiHelper.class);
    
    private final String baseUrl;
    private final String accessToken;
    private final String accountId;
    
    public HarvestApiHelper(String baseUrl, String accessToken, String accountId) {
        
        this.baseUrl = baseUrl;
        this.accessToken = accessToken;
        this.accountId = accountId;
    }
        
    public String executeRequest (String path) throws BridgeError{
        
        String url = baseUrl + path;
        String output;      
        // System time used to measure the request/response time
        long start = System.currentTimeMillis();
        
        try (
            CloseableHttpClient client = HttpClients.createDefault()
        ) {
            HttpResponse response;
            HttpGet get = new HttpGet(url);
            
            // Append HTTP BASIC Authorization header to HttpGet call
            String accessToken = this.accessToken;
            String accountId = this.accountId;
            get.setHeader("Authorization", "Bearer " + accessToken);
            get.setHeader("Harvest-Account-ID", accountId);
            get.setHeader("Content-Type", "application/json");
            get.setHeader("Accept", "application/json");
            
            response = client.execute(get);
            LOGGER.debug("Recieved response from \"{}\" in {}ms.",
                url,
                System.currentTimeMillis()-start);

            int responseCode = response.getStatusLine().getStatusCode();
            LOGGER.trace("Request response code: " + responseCode);
            
            HttpEntity entity = response.getEntity();
            
            // Confirm that response is a JSON object
            output = EntityUtils.toString(entity);
            
            // Handle all other faild repsonses
            if (responseCode >= 400) {
                handleFailedReqeust(responseCode, output);
            }
        }
        catch (IOException e) {
            throw new BridgeError(
                "Unable to make a connection to the Azure service server.", e);
        }
        
        return output;
    }
    
    private void handleFailedReqeust (int responseCode, String output) 
        throws BridgeError {
        
        switch (responseCode) {
            case 400:
                throw new BridgeError(String.format("400: Bad Reqeust Response:"
                    + " %s", output));
            case 401:
                throw new BridgeError("401: Unauthorized");
            case 404:
                throw new BridgeError("404: Page not found");
            case 405:
                throw new BridgeError("405: Method Not Allowed");
            case 500:
                throw new BridgeError("500 Internal Server Error");
            default:
                throw new BridgeError("Unexpected response from server");
        }
    }
        
    private JSONObject parseResponse(String output) throws BridgeError{
        
        JSONObject responseObj = new JSONObject();
        try {
            responseObj = (JSONObject)JSONValue.parseWithException(output);
        } catch (ParseException e){
            // Assume all 200 responses will be JSON format.
            LOGGER.error("There was a parse exception with the response", e);
        } catch (Exception e) {
            throw new BridgeError("An unexpected error has occured ", e);
        }
        
        return responseObj;
    }
}
