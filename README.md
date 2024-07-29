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
Let's go into the basics, starting with the things you most certainly need.  
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
`defaultVal` represents the integer representation of the default color, while inside of the config file it is the hex value prefixed with `#`.  
`alpha` defines whether or not this `ColorHolder` should store argb in hex or just rgb.  
Example:
```java
package com.example.examplemod.config;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import net.atlas.atlascore.AtlasCore;
import net.atlas.atlascore.config.AtlasConfig;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExampleConfig extends AtlasConfig {
	public enum ExampleEnum {
		FOO,
		BAR
	}
	public EnumHolder<ExampleEnum> exampleEnum;
	public StringHolder exampleString;
	public BooleanHolder exampleBool;
	public IntegerHolder exampleInt;
	public DoubleHolder exampleDouble;
	public ColorHolder exampleColour;
	private Category example;
	public ExampleConfig() {
		super(ResourceLocation.fromNamespaceAndPath("examplemod", "examplemod-config"));
		declareDefaultForMod("examplemod");
	}

	@Override
	public void defineConfigHolders() {
		exampleEnum = createEnum("exampleEnum", ExampleEnum.FOO, ExampleEnum.class, ExampleEnum.values(), e -> Component.translatable("text.config.atlas-core-config.option.exampleEnum." + e.name().toLowerCase(Locale.ROOT)));
		exampleEnum.tieToCategory(example);
		exampleEnum.setupTooltip(1);
		exampleString = createString("exampleString", "bar");
		exampleString.tieToCategory(example);
		exampleBool = createBoolean("exampleBool", true);
		exampleBool.tieToCategory(example);
		exampleInt = createInRestrictedValues("exampleInt", 50, 0, 25, 50, 75, 100);
		exampleInt.tieToCategory(example);
		exampleDouble = createDoubleUnbound("exampleDouble", 4.05);
		exampleDouble.tieToCategory(example);
		exampleColour = createColor("exampleColour", Integer.valueOf("FFFFFF", 16), false);
		exampleColour.tieToCategory(example);
	}

	@Override
	public @NotNull List<Category> createCategories() {
		List<Category> categoryList = super.createCategories();
		example = new Category(this, "example_category", new ArrayList<>());
		categoryList.add(example);
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
		return Thread.currentThread().getContextClassLoader().getResourceAsStream("examplemod-config.json");
	}

	@Override
	public void saveExtra(JsonWriter jsonWriter, PrintWriter printWriter) {

	}

	@Override
	public void handleExtraSync(AtlasCore.AtlasConfigPacket atlasConfigPacket, LocalPlayer localPlayer, PacketSender packetSender) {

	}

	@Override
	public Screen createScreen(Screen screen) {
		return null;
	}
}
```
Now, aside from the unique parsing and saving we could do, or other handlers for this specific config, this config is done!  
Remember to store the config as a field inside of your mod's main class like so:  
```java
public static ExampleConfig CONFIG = new ExampleConfig();
```
To get specific values from particular `ConfigHolder`s, you need to use `ConfigHolder.get()`, and to set values, though admittedly most setting is handled by the config system to begin with, you use `AtlasConfig.saveConfig()`
***
##### Part 4: The final stretch
***
Once you have your config set up, you need to create the default config file as a resource.  
This file is stored in the root of the `resources` directory for your mod by default, but where it specifically is can be defined by just changing the return of `getDefaultedConfig`  
The config for our example here is as follows:  
```json
{
  "exampleEnum": "foo",
  "exampleString": "bar",
  "exampleBool": true,
  "exampleInt": 50,
  "exampleDouble": 4.05,
  "exampleColour": "#ffffff"
}
```
Now, you're almost done!  
It's time for the language file, which in general is prefixed like this: `text.config.(configpath).` and usually has both a `.title` and `.reset` key.  
Each category also has its own translation key, which is `text.config.(configpath).category.(categoryname)`.  
Each individual option then has its own translation key, formatted as `text.config.(configpath).option.(holdername)`, and each possible value for an `EnumHolder` does as well.  
If the holder has a tooltip, the translation key for the tooltip starts as `text.config.(configpath).option.(holdername).tooltip.0` and increases with each tooltip line, matching the amount of tooltip lines you specified when calling `setupTooltip(int lineCount)`  
In our practical example, this would go in your lang file:  
```json
{
  "text.config.examplemod-config.title": "Example Mod",
  "text.config.examplemod-config.reset": "Reset",
  "text.config.examplemod-config.category.example_category": "Example Category",
  "text.config.examplemod-config.option.exampleEnum": "Example Enum",
  "text.config.examplemod-config.option.exampleEnum.foo": "Foo",
  "text.config.examplemod-config.option.exampleEnum.bar": "Bar",
  "text.config.examplemod-config.option.exampleEnum.tooltip.0": "This is an example enum option!",
  "text.config.examplemod-config.option.exampleString": "Example String",
  "text.config.examplemod-config.option.exampleBool": "Example Boolean",
  "text.config.examplemod-config.option.exampleInt": "Example Integer",
  "text.config.examplemod-config.option.exampleDouble": "Example Double",
  "text.config.examplemod-config.option.exampleColor": "Example Color",
}
```
Now, your config is set up properly, and can be used and edited however you like.
