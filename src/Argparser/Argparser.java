package Argparser;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Argparser {
    private Argparser() {
    }

    public static <T> T parse(String[] args, Class<T> optionsClass) {
        List<Field> fields = getParameterFields(optionsClass);
        int maxIndex = getMaxIndex(fields);

        if (args.length > maxIndex + 1) {
            throw new IllegalArgumentException("Too many arguments.");
        }

        T options = createInstance(optionsClass);

        for (Field field : fields) {
            Parameters parameters = field.getAnnotation(Parameters.class);
            String value = getValue(args, parameters);

            if (value == null) {
                continue;
            }

            setField(options, field, normalizeValue(value, parameters));
        }

        return options;
    }

    public static String usage(Class<?> optionsClass) {
        Command command = optionsClass.getAnnotation(Command.class);
        String commandName = command == null ? optionsClass.getSimpleName() : command.name();
        StringBuilder text = new StringBuilder("Usage: java ").append(commandName);

        for (Field field : getParameterFields(optionsClass)) {
            Parameters parameters = field.getAnnotation(Parameters.class);
            String name = usageName(parameters);

            if (parameters.required()) {
                text.append(" <").append(name).append(">");
            } else {
                text.append(" [").append(name).append("]");
            }
        }

        return text.toString();
    }

    private static List<Field> getParameterFields(Class<?> optionsClass) {
        List<Field> fields = new ArrayList<Field>();
        Set<Integer> indexes = new HashSet<Integer>();

        for (Field field : optionsClass.getDeclaredFields()) {
            Parameters parameters = field.getAnnotation(Parameters.class);
            if (parameters == null) {
                continue;
            }
            if (parameters.index() < 0) {
                throw new IllegalArgumentException("Bad parameter index.");
            }
            if (!indexes.add(parameters.index())) {
                throw new IllegalArgumentException("Duplicate parameter index (" + parameters.index() + ")");
            }
            fields.add(field);
        }

        Collections.sort(fields, new Comparator<Field>() {
            public int compare(Field left, Field right) {
                return Integer.compare(
                        left.getAnnotation(Parameters.class).index(),
                        right.getAnnotation(Parameters.class).index()
                );
            }
        });

        return fields;
    }

    private static int getMaxIndex(List<Field> fields) {
        int maxIndex = -1;

        for (Field field : fields) {
            int index = field.getAnnotation(Parameters.class).index();
            if (index > maxIndex) {
                maxIndex = index;
            }
        }

        return maxIndex;
    }

    private static <T> T createInstance(Class<T> optionsClass) {
        try {
            Constructor<T> constructor = optionsClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot create options.");
        }
    }

    private static String getValue(String[] args, Parameters parameters) {
        if (args.length > parameters.index()) {
            return args[parameters.index()];
        }

        if (!parameters.defaultValue().isEmpty()) {
            return parameters.defaultValue();
        }

        if (parameters.required()) {
            throw new IllegalArgumentException("Missing argument (" + usageName(parameters) + ")");
        }

        return null;
    }

    private static String normalizeValue(String value, Parameters parameters) {
        String[] choices = parameters.choices();
        if (choices.length == 0) {
            return value;
        }

        for (String choice : choices) {
            if (choice.equalsIgnoreCase(value)) {
                return choice;
            }
        }

        throw new IllegalArgumentException("Bad value (" + value + ")");
    }

    private static void setField(Object options, Field field, String value) {
        try {
            field.setAccessible(true);

            Class<?> type = field.getType();
            if (type == String.class) {
                field.set(options, value);
            } else if (type == int.class || type == Integer.class) {
                field.set(options, Integer.valueOf(value));
            } else if (type == boolean.class || type == Boolean.class) {
                field.set(options, Boolean.valueOf(value));
            } else {
                throw new IllegalArgumentException("Unsupported parameter type (" + field.getName() + ")");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Bad argument (" + field.getName() + ")");
        }
    }

    private static String usageName(Parameters parameters) {
        if (parameters.choices().length > 0) {
            return join(parameters.choices(), "|");
        }
        if (!parameters.name().isEmpty()) {
            return parameters.name();
        }
        return "arg" + parameters.index();
    }

    private static String join(String[] values, String separator) {
        StringBuilder text = new StringBuilder();

        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                text.append(separator);
            }
            text.append(values[i]);
        }

        return text.toString();
    }
}
