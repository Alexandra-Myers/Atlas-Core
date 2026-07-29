package net.atlas.atlascore.util;

import java.util.Objects;

@SuppressWarnings("unused")
public class StringUtils {
    public static String convertCamelCaseToSnakeCase(String id) {
        return convertCamelCaseWithDelimiter(id, "_", true);
    }

    public static String convertSnakeCaseToCamelCase(String id) {
        return convertSnakeCaseWithDelimiter(id, "", false);
    }

    public static String convertToName(String input) {
        return convertSnakeCaseWithDelimiter(convertCamelCaseWithDelimiter(input, " ", false), " ", true);
    }

    public static String convertCamelCaseWithDelimiter(String input, String delimiter, boolean uncapitalize) {
        int firstUpper;
        final int len = input.length();

        /* Now check if there are any characters that need to be changed. */
        while (!Objects.equals(input, input.toLowerCase())) {
            scan:
            {
                for (firstUpper = 0; firstUpper < len; firstUpper++) {
                    char c = input.charAt(firstUpper);
                    if (c != Character.toLowerCase(c)) {
                        String[] split = input.split(String.valueOf(c), 2);
                        char insert = uncapitalize ? Character.toLowerCase(c) : c;
                        if (firstUpper > 0) input = split[0] + delimiter + insert + split[1];
                        else input = insert + split[1];
                        break scan;
                    }
                }
            }
        }
        return input;
    }

    public static String convertSnakeCaseWithDelimiter(String input, String delimiter, boolean capitalizeFirstAppended) {
        int firstUpper;

        /* Now check if there are any characters that need to be changed. */
        StringBuilder result = new StringBuilder();
        String[] each = input.split("_");
        for (String segment : each) {
            if (!segment.isEmpty()) {
                boolean firstFullAppending = result.isEmpty();
                if (!firstFullAppending) result.append(delimiter);
                scan: {
                    for (firstUpper = 0; firstUpper < segment.length(); firstUpper++) {
                        char c = segment.charAt(firstUpper);
                        if (c != Character.toUpperCase(c)) {
                            if (firstUpper != 0) result.append(segment, 0, firstUpper).append(delimiter);
                            result.append(capitalizeFirstAppended || !(firstFullAppending && firstUpper == 0) ? Character.toUpperCase(c) : c).append(segment.substring(Math.min(firstUpper + 1, segment.length())));
                            break scan;
                        }
                    }
                    result.append(segment);
                }
            } else result.append(segment);
        }
        return result.toString();
    }
}
