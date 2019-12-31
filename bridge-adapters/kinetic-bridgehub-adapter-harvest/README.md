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

## Notes
This adapter has been tested with the 1.0.3 bridgehub adapter.
The adapter only supports personal access token authentication at this time.
Pagination and sort order are not supported by the adapter, but Harvest source api behavior is supported.
