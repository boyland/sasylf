import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

public class ReflectionUtils {

    public static void printObjectAttributes(Object obj) {
        printObjectAttributes(obj, new HashSet<>(), 0);
    }

    private static void printObjectAttributes(Object obj, Set<Object> visited, int depth) {
        if (obj == null || visited.contains(obj)) {
            return;
        }

        visited.add(obj);

        Class<?> objClass = obj.getClass();
        printIndented("Class: " + objClass.getName(), depth);

        Field[] fields = objClass.getDeclaredFields();
        for (Field field : fields) {
            // Skip static fields
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            field.setAccessible(true);
            try {
                Object value = field.get(obj);
                printIndented(field.getName() + " (" + field.getType().getTypeName() + ") = " + value, depth + 1);
                if (value != null && !isPrimitiveOrWrapper(value.getClass())) {
                    printObjectAttributes(value, visited, depth + 1);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InaccessibleObjectException e) {
                // Skip fields that cannot be made accessible
                System.out.println("Skipping inaccessible field: " + field.getName());
            }
        }
    }

    private static void printIndented(String message, int depth) {
        for (int i = 0; i < depth; i++) {
            System.out.print("  "); // Two spaces for each level of depth
        }
        System.out.println(message);
    }

    private static boolean isPrimitiveOrWrapper(Class<?> type) {
        return type.isPrimitive() ||
                type == Boolean.class ||
                type == Byte.class ||
                type == Character.class ||
                type == Short.class ||
                type == Integer.class ||
                type == Long.class ||
                type == Float.class ||
                type == Double.class ||
                type == String.class;
    }
}