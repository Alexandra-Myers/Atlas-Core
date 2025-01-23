### Changes:
***
- Addition of `Context`, which gives details for a certain context.

- Addition of `ContextCondition`, which is a named predicate for a certain context.

- Added `ServerModsRetrievedEvent`, invoked when Atlas Core retrieves the mods on the client, including name, modID, semantic version, and provided mods.

- Addition of Brigadier Opts, (kind of) a partial command library/expansion to Brigadier, but at this point merely a major utility, currently not feature-complete
***
### Atlas Config Changes:
***
- Addition of `TagHolder`, parallel of `ObjectHolder` whose identity is edited in GUI and in commands using SNBT, slightly decreasing ease of use for the user, meant for cases which cannot easily be converted into an `ObjectHolder`.

- Addition of `SyncMode`, defines how the configs will be synched, can be set as a default for a config and changed for particular config holders.

- Addition of `ConfigSide`, defining which side the config belongs to. **ALL** configs must *exist* and be *aware* of each other on both sides, however this controls particular behaviour.

- Movement towards codecs for parsing configs.

- More effective saving system for configs, can handle more types of data.

- Added a disconnect when there is a mismatch between the server and the client.

- Added another abstract config base class, `ContextBasedConfig`
  * Note: Define a generic form of the config first before using, no matter what
  * Note 2: Contexts currently only provide the dimension and whether the server is dedicated
  * The primary intended purpose is so that the config can change under particular contexts
***
### Brigadier Opts
***
- Argument Type `OptsArgument` introduced, allows for arguments to simply be interchangeable.
  * Example: `/pandora {player|effect|invisible}` could be input as any of these combinations or more:
    ```
    /pandora invisible=true
    /pandora effect=in_the_end player=@s 
    /pandora player=@s invisible=false effect=apocalyptic_boom
    ```
- With the advent of these changes, `/atlas_config` has been updated to... not use it, but a relatively similar concept to improve functionality.