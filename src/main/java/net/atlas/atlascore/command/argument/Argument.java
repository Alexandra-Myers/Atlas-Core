package net.atlas.atlascore.command.argument;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.atlas.atlascore.util.Codecs;

import java.util.*;
import java.util.function.Supplier;

public record Argument<T>(String name, T data, Class<T> clazz) {
    /**
     * Produces a set of {@link Arguments} from a {@link CommandContext} to make it possible to use the opts arguments as you need them.
     *
     * @param context The command context for this command.
     * @param trueArguments The names representing the arguments as they are inside Brigadier. Example: {"argument", "argument2", etc.}
     * @return A newly constructed instance of {@link Arguments} which can be used to get opts arguments for the command.
     * @param <S> The command source type for the {@link CommandContext}.
     */
    public static <S> Arguments argumentMap(OptsArgument optsArgument, CommandContext<S> context, String[] trueArguments) throws CommandSyntaxException {
        List<Argument<?>> arguments = new ArrayList<>();
        for (String argument : trueArguments) {
            if (Codecs.hasArgument(context, argument)) {
                Argument<?> arg = optsArgument.getArgument(context, argument);
                arguments.add(arg);
            }
        }
        return new Arguments(arguments);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Argument<?> argument)) return false;
        return Objects.equals(name, argument.name) && Objects.equals(data, argument.data) && Objects.equals(clazz, argument.clazz);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, data, clazz);
    }

    public record Arguments(List<Argument<?>> arguments) {
        public static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER = new HashMap<>();

        static {
            PRIMITIVE_TO_WRAPPER.put(boolean.class, Boolean.class);
            PRIMITIVE_TO_WRAPPER.put(byte.class, Byte.class);
            PRIMITIVE_TO_WRAPPER.put(short.class, Short.class);
            PRIMITIVE_TO_WRAPPER.put(char.class, Character.class);
            PRIMITIVE_TO_WRAPPER.put(int.class, Integer.class);
            PRIMITIVE_TO_WRAPPER.put(long.class, Long.class);
            PRIMITIVE_TO_WRAPPER.put(float.class, Float.class);
            PRIMITIVE_TO_WRAPPER.put(double.class, Double.class);
        }

        /**
         * Gets the argument matching the name.
         *
         * @param name The name of the argument you wish to retrieve.
         * @return The argument to be retrieved.
         */
        public Argument<?> get(String name) {
            return arguments.stream().filter(argument -> argument.name.hashCode() == name.hashCode()).findFirst().orElse(null);
        }

        /**
         * Returns an argument's value for the given name.
         *
         * @param name The name of the argument you wish to retrieve.
         * @param clazz The class which you wish to retrieve the argument as, which must be assignable from the argument's class.
         * @throws IllegalArgumentException If the argument does not exist or does not match the provided type.
         * @return The type-casted form of the argument's data.
         * @param <V> The type to return the result as.
         */
        @SuppressWarnings("unchecked")
        public <V> V getArgument(final String name, final Class<V> clazz) {
            final Argument<?> argument = get(name);

            if (argument == null) throw new IllegalArgumentException("No such argument '" + name + "' exists on this command");
            if (!PRIMITIVE_TO_WRAPPER.getOrDefault(clazz, clazz).isAssignableFrom(argument.clazz)) throw new IllegalArgumentException("The class obtained for an argument must match the class of the argument!\nFound: " + clazz.getSimpleName() + "\nExpected: " + argument.clazz.getSimpleName());

            return (V) argument.data;
        }

        /**
         * Returns an argument's value for the given name.
         *
         * @param name The name of the argument you wish to retrieve.
         * @param defaultVal The default value for the argument, which must match the type of the argument.
         * @throws IllegalArgumentException If the argument does not exist or does not match the provided type.
         * @return The type-casted form of the argument's data or a fallback if absent.
         * @param <V> The type to return the result as.
         */
        @SuppressWarnings("unchecked")
        public <V> V getArgumentOrDefault(final String name, V defaultVal) {
            Class<V> clazz = (Class<V>) defaultVal.getClass();
            final Argument<?> argument = get(name);

            if (argument == null) return defaultVal;
            if (!PRIMITIVE_TO_WRAPPER.getOrDefault(clazz, clazz).isAssignableFrom(argument.clazz)) throw new IllegalArgumentException("The class obtained for an argument must match the class of the argument!\nFound: " + clazz.getSimpleName() + "\nExpected: " + argument.clazz.getSimpleName());

            return (V) argument.data;
        }

        /**
         * Returns an argument's value for the given name.
         *
         * @param name The name of the argument you wish to retrieve.
         * @param clazz The class which you wish to retrieve the argument as, which must be assignable from the argument's class.
         * @param defaultVal A supplier for the fallback value of the argument.
         * @throws IllegalArgumentException If the argument does not exist or does not match the provided type.
         * @return The type-casted form of the argument's data or a fallback if absent.
         * @param <V> The type to return the result as.
         */
        @SuppressWarnings("unchecked")
        public <V> V getArgumentOrElseGet(final String name, Class<V> clazz, Supplier<V> defaultVal) {
            final Argument<?> argument = get(name);

            if (argument == null) return defaultVal.get();
            if (!PRIMITIVE_TO_WRAPPER.getOrDefault(clazz, clazz).isAssignableFrom(argument.clazz)) throw new IllegalArgumentException("The class obtained for an argument must match the class of the argument!\nFound: " + clazz.getSimpleName() + "\nExpected: " + argument.clazz.getSimpleName());

            return (V) argument.data;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Arguments other)) return false;
            return Objects.equals(arguments, other.arguments);
        }

        @Override
        public int hashCode() {
            return Objects.hash(arguments);
        }
    }
}
