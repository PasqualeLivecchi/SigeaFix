package sigea.main;

import java.lang.annotation.*;
import static java.lang.annotation.ElementType.*;
import javax.inject.Qualifier;

/**
 *
 * @author Pasquale Livecchi
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({FIELD, PARAMETER, METHOD, TYPE})
public @interface Computation {
}
