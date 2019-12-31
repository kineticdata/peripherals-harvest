# Harvest Bridge Adapter
An adapter for interacting with the Harvest v2 api

## Configuration Values
| Name                    | Description |
| :---------------------- | :------------------------- |
| Access Token            | The personal access token set up for the Harvest account |
| Account Id              | The account id associated to the personal access token |

## Example Configuration
| Name | Value |
| :---- | :--- |
| Access Token | 555555.rt.a.34t3g45yt5h45... |
| Account Id   | 123123 |

## Supported Structures
| Name                    | Description |
| :---------------------- | :------------------------- |
| Contacts                | Get client contacts   |
| Clients                 | Get clients   |
| Invoices                | Get invoices   |
| Invoice > Messages      | Get messages associated with a given invoice   |
| Invoice > Payments      | Get payments associate with a given invoice   |
| Invoice Item Categories | Get invoice item categories   |
| Estimate > Messages     | Get messages associated with a given estimate   |
| Estimates               | Get estimates   |
| Estimate Item Categories| Get estimate item categories |
| Tasks                   | Get tasks |
| Time Entries            | Get time entries |
| User Assignments        | Get projects user assignments, active and archived |
| Task Assignments        | Get task assignments |
| Projects                | Get projects |
<!-- | Roles                   | Get roles in the account |
| Billable Rates          | Get billable rates for the user identified by USER_ID |
| Cost Rates              | Get cost rates for the user identified by USER_ID |
| Project Assignments     | Get active project assignments for the user identified by USER_ID | -->
<!-- | Expenses                | Get expenses |
| Expense Categories      | Get expense categories | -->

## Configuration example
| Structure               | Qualification Mapping      | Description |
| :---------------------- | :------------------------- | :------------------------- |
| Projects                | projects                   | Get a list of projects     |
| Projects                | client_id=2372100 | Only return projects belonging to the client with the given ID|
| Projects                | id=14308069          | Retrieve a single project  |
| Users                   | users/{USER_ID} | Get a user |

## Notes
* This adapter has been tested with the 1.0.3 bridgehub adapter.
* The adapter only supports personal access token authentication at this time.  
    - To configure [personal access token](https://id.getharvest.com/)  
    - Information on [personal access token](https://help.getharvest.com/api-v2/authentication-api/authentication/authentication/#personal-access-tokens)  
* Pagination and sort order are not supported by the adapter, but Harvest source api behavior is supported.  
* From more information about harvest api visit [Harvest API v2 Documentation](https://help.getharvest.com/api-v2/)
* This adapter requires an id parameter to be passed to do a retrieve an element.