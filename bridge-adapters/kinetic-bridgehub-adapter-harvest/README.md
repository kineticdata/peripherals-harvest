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
| Invoice Messages        | Get messages associated with a given invoice   |
| Invoice Payments        | Get payments associate with a given invoice   |
| Invoices                | Get invoices   |
| Invoice Item Categories | Get invoice item categories   |
| Estimate Messages       | Get messages associated with a given estimate   |
| Estimates               | Get estimates   |
| Estimate Item Categories| Get estimate item categories |
| Expenses                | Get expenses |
| Expense Categories      | Get expense categories |
| Tasks                   | Get tasks |
| Time Entries            | Get time entries |
| User Assignments        | Get projects user assignments, active and archived |
| Task Assignments        | Get task assignments |
| Projects                | Get projects |
| Roles                   | Get roles in the account |
| Billable Rates          | Get billable rates for the user identified by USER_ID |
| Cost Rates              | Get cost rates for the user identified by USER_ID |
| Project Assignments     | Get active project assignments for the user identified by USER_ID |

## configuration example
| Structure               | Qualification Mapping      | Description |
| :---------------------- | :------------------------- | :------------------------- |
| Projects                | projects                   | Get a list of projects     |
| Projects                | projects?client_id=2372100 | Only return projects belonging to the client with the given ID|
| Projects                | projects/14308069          | Retrieve a single project  |
| Users                   | users/{USER_ID}/billable_rates | Get a list of billable rates for the user |
| Users                   | /users/{USER_ID}/billable_rates/{billable_RATE_ID} | Retrieve a billable rate for a user |

## Notes
This adapter has been tested with the 1.0.3 bridgehub adapter.
The adapter only supports personal access token authentication at this time.
Pagination and sort order are not supported by the adapter|    |
| but Harvest source api behavior is supported.
