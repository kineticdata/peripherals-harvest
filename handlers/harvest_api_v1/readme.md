# Harvest API V1
Harvest REST API V1 Client

## Info Values
[account_id] The id associated with the Harvest account.  Provided by Harvest.

[access_token] The Personal Access Token created to access Harvest resources.  Create token at [Harvest Developer Tools](https://id.getharvest.com/developers).

[api_location] The prefix value of the api. ex: https://api.harvestapp.com/v2

[enable_debug_logging] Sets logging for handler execution.  Set to true or yes for debug level logging.

## Parameters
[Error Handling]
  Select between returning an error message, or raising an exception.

[Method]
  HTTP Method to use for the Harvest API call being made.

  Options are:
   - GET
   - POST
   - PUT
   - PATCH
   - DELETE

[Path]
  The relative API path (to the `api_location` info value) that will be called.
  This value should begin with a forward slash `/`.

[Body]
  The body content (JSON) that will be sent for POST, PUT, and PATCH requests.

## Results
[Response Body]
  The returned value from the Rest Call (JSON format)

