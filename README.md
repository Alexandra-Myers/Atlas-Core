# Atlas Core
***
### A mod library for Atlas Projects
***
Have you ever wondered what would happen if you threw an idiot in a room and asked them to write a robust library for your Minecraft mod?
...
No?
Well, here's what happens if you do anyways...
***
### Now presenting: Atlas Config
***
That's right, the entire reason I made this was to hold a common config system for all of my mods, though admittedly it blossomed into something else entirely.
***
#### Adding a config
***
You wake up, it's a Saturday, you decide to add a config to your mod.
You decide to use Atlas Core to implement it.
But there's a problem:
"How do I use it?" you ask politely... probably.
Well, don't fret, since the solution is simple:
***
##### Part 1: Adding a config class
***
This part is simple, but robust, and thus takes more steps than you'd probably expect.
Starting off, you want to create a class that extends `AtlasConfig`, like so:
```java
package com.example.examplemod.config;

import net.atlas.atlascore.config.AtlasConfig;
import net.minecraft.resources.ResourceLocation;

public class ExampleConfig extends AtlasConfig {
	public ExampleConfig() {
		super(ResourceLocation.fromNamespaceAndPath("examplemod", "examplemod-config"));
		declareDefaultForMod("examplemod");
	}
}
```
Implement the base implementations of these methods in `AtlasConfig`, like so:
```java
package com.example.examplemod.config;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import net.atlas.atlascore.AtlasCore;
import net.atlas.atlascore.config.AtlasConfig;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.io.PrintWriter;

public class ExampleConfig extends AtlasConfig {
	public ExampleConfig() {
		super(ResourceLocation.fromNamespaceAndPath("examplemod", "examplemod-config"));
		declareDefaultForMod("examplemod");
	}

	@Override
	public void defineConfigHolders() {
		
	}

	@Override
	public @NotNull List<Category> createCategories() {
		List<Category> categoryList = super.createCategories(); // We'll be using this to define the categories for the config, this part is essential for the config screen to show up
		return categoryList;
	}

	@Override
	public void resetExtraHolders() {

	}

	@Override
	public <T> void alertChange(ConfigValue<T> configValue, T t) {

	}

	@Override
	protected void loadExtra(JsonObject jsonObject) {

	}

	@Override
	protected InputStream getDefaultedConfig() {
		return Thread.currentThread().getContextClassLoader().getResourceAsStream("examplemod-config.json"); // We will create this resource later, it is used to produce the base config file, though note that it should generally match the default values for each ConfigHolder
	}

	@Override
	public void saveExtra(JsonWriter jsonWriter, PrintWriter printWriter) {

	}

	@Override
	public void handleExtraSync(AtlasCore.AtlasConfigPacket atlasConfigPacket, LocalPlayer localPlayer, PacketSender packetSender) {

	}

	@Override
	public Screen createScreen(Screen screen) {
		return null; // Change this if you want a custom Screen for your config
	}
}
```
Now you've finished the first step, it's time to go into detail about each of these methods.
***
##### Part 2: Defining special parts of the config
***
It's time to go into detail about each of these methods. let's go into the basics, starting with the things you most certainly need.
`defineConfigHolders()` runs in the constructor of `AtlasConfig` before `load()` is called. Rather than in your constructor, everything which needs to be defined before it is loaded should be called here.
`resetExtraHolders()` is made to reset things which exist outside of the `AtlasConfig` base class's jurisdiction, used when the config is reloaded, whether it be from its default state or from the file.
`reload()` is used in several places, including `reset()` and the `reload` commands, reloading the config from its file.
`reloadFromDefault()` not used by default, but reloads the config from the default resource, does NOT edit the config file stored.
`reset()` is largely internal and used to restore a config directly to the default resource it is built from, and then reloads the config.
`getFormattedName()` returns a `Component` representing the name of this config, formatted however you wish.
`declareDefaultForMod(String modID)` will set this config as the default for the given mod, used to define that the screen for this config should be the default for the mod.
`alertChange(ConfigValue<T> value, T newValue)` is sent by the `ConfigValue<T>` which was updated, is used as a callback for when the `ConfigValue<T>` is set.
`loadExtra(JsonObject object)` and `saveExtra(JsonWriter writer, PrintWriter printWriter)` are used to load/save parts of the config outside of the `AtlasConfig`'s jurisdiction.
`handleExtraSync(AtlasCore.AtlasConfigPacket atlasConfigPacket, LocalPlayer localPlayer, PacketSender packetSender)` is used to finish syncing the data sent over by the server about this config.
This is everything you should need to know about methods themselves, now to get into the base features this system is designed for.
***
##### Part 3: Config Holders and you
***
A `ConfigHolder<T, B extends ByteBuf>` is a representation of a `ConfigValue`, and how to edit, save, load, and sync it.
It has several base implementations, including `EnumHolder<E extends Enum>`, `StringHolder`, `BooleanHolder`, `IntegerHolder`, `DoubleHolder`, and `ColorHolder`.
Before going into the implementation detail itself, it is more important to cover the base implementations directly.
First off: `EnumHolder<E extends Enum>` represents an Enum value in a config. it is constructed using the instance method `<E extends Enum<E>> EnumHolder<E> createEnum(String name, E defaultVal, Class<E> clazz, E[] values, Function<Enum, Component> names)` in `AtlasConfig`.
Let's go into detail on the parameters:
***
1. `name`: The name of the holder, used internally to identify it.
2. `defaultVal`: The default value for this config option.
3. `clazz`: The class that this `EnumHolder<E>` represents, must represent an Enum, and must conform to the same type as the other parameters.
4. `values`: The enum values which can be selected by this enum. In general, should be `Enum.values()` but can be whatever subset you like.
5. `names`: A function which provides the enum value, and produces a `Component` to represent the name of the value.
***
Secondly, we have `StringHolder`, which is less complex, constructed by two methods, `createStringRange(String name, String defaultVal, String... values)`, and `createString(String name, String defaultVal)`
`name` and `defaultVal` are shared across all `ConfigHolder` constructors, so we ignore those.
`values` represents the possible values which this `StringHolder` can be set to, allowing you to confine the user's input to strictly what is needed or accepted.
Thirdly, we have `BooleanHolder` which is only constructed with `createBoolean(String name, boolean defaultVal)`, with which we have nothing to say for.
Fourthly, we have `IntegerHolder` and `DoubleHolder`, which are near identical to each other, aside from type, each being constructed with three methods, for which I will be replacing the type's name with (insert type here):
***
1. `create(insert type here)Unbound(String name, (insert type here) defaultVal)`: Nothing notable, creates an Integer / Double with no restrictions on values.
2. `createInRestrictedValues(String name, (insert type here) defaultVal, (insert type here)... values)`: Similar to `createStringRange`, creates a holder which only accepts the given values.
3. `createInRange(String name, int defaultVal, int min, int max, boolean isSlider)`: Specific to `IntegerHolder`, creates a holder which accepts any value in a specified range and specifies whether it will be a slider or not when viewed in the config's screen.
4. `createInRange(String name, double defaultVal, double min, double max)`: Specific to `DoubleHolder`, creates a holder which accepts any value in a specified range.
***
Finally, there is `ColorHolder`, constructed with `createColor(String name, Integer defaultVal, boolean alpha)`
`name` represents the same concept as the rest, however...
`defaultVal` represents the integer representation of the default color, while inside of the config file it is the hex value prefixed with #.
`alpha` defines whether or not this `ColorHolder` should store argb in hex or just rgb.
