package org.spongepowered.common.event.service;

import com.google.common.reflect.TypeToken;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.service.ProvideServiceEvent;

import java.util.function.Supplier;

// Specialised logic is required for this.
public class ProvideServiceEventImpl<T> implements ProvideServiceEvent<T> {

    private final Cause cause;
    private final TypeToken<T> genericType;
    @Nullable private Supplier<T> serviceFactory;

    public ProvideServiceEventImpl(final Cause cause, final TypeToken<T> genericType) {
        this.cause = cause;
        this.genericType = genericType;
    }

    @Override
    public void suggest(@NonNull final Supplier<T> serviceFactory) {
        this.serviceFactory = serviceFactory;
    }

    @Nullable
    public Supplier<T> getSuggestion() {
        return this.serviceFactory;
    }

    // For resetting the event between plugins.
    public void clear() {
        this.serviceFactory = null;
    }

    @Override
    @NonNull
    public TypeToken<T> getGenericType() {
        return this.genericType;
    }

    @Override
    @NonNull
    public Cause getCause() {
        return this.cause;
    }

}
