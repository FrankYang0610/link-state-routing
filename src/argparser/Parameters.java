package argparser;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Parameters {
    int index();

    String name() default "";

    String description() default "";

    boolean required() default true;

    String defaultValue() default "";

    String[] choices() default {};
}
