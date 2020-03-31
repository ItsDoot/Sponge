package org.spongepowered.common.command.parameter;

import com.google.common.reflect.TypeToken;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.command.parameter.Parameter;

public class SpongeParameterValueFactory implements Parameter.Factory {

    public static final Parameter.Factory INSTANCE = new SpongeParameterValueFactory();

    private SpongeParameterValueFactory() {
        // no-op
    }

    @Override
    public <T> Parameter.Value.@NonNull Builder<T> createParameterBuilder(@NonNull final TypeToken<T> parameterClass) {
        return new SpongeParameterValueBuilder<>(parameterClass);
    }

}
