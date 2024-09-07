### Changes:
***
- Addition of `Context`, which gives details for a certain context.

- Addition of `ContextCondition`, which is a named predicate for a certain context.

- Added `ServerModsRetrievedEvent`, invoked when Atlas Core retrieves the mods on the client, including name, modID, semantic version, and provided mods.
***
### Atlas Config Changes:
***
- Addition of `SyncMode`, defines how the configs will be synched, can be set as a default for a config and changed for particular config holders.

- Addition of `ConfigSide`, defining which side the config belongs to. **ALL** configs must *exist* and be *aware* of each other on both sides, however this controls particular behaviour.

- Added a disconnect when there is a mismatch between the server and the client.

- Added another abstract config base class, `ContextBasedConfig`
  * Note: Define a generic form of the config first before using, no matter what
  * Note 2: Contexts currently only provide the dimension and whether the server is dedicated
  * The primary intended purpose is so that the config can change under particular contexts