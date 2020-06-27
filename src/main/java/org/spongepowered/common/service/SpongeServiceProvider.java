package org.spongepowered.common.service;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.service.ProvideServiceEvent;
import org.spongepowered.api.service.ServiceProvider;
import org.spongepowered.api.service.ServiceRegistration;
import org.spongepowered.api.service.ban.BanService;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.sql.SqlService;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.service.whitelist.WhitelistService;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.config.category.ServicesCategory;
import org.spongepowered.common.event.SpongeEventManager;
import org.spongepowered.common.event.service.ProvideServiceEventImpl;
import org.spongepowered.common.service.ban.SpongeBanService;
import org.spongepowered.common.service.permission.SpongePermissionService;
import org.spongepowered.common.service.sql.SqlServiceImpl;
import org.spongepowered.common.service.user.SpongeUserStorageService;
import org.spongepowered.common.service.whitelist.SpongeWhitelistService;
import org.spongepowered.plugin.PluginContainer;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class SpongeServiceProvider implements ServiceProvider {

    // Contains all the services and defaults.
    // Zidane, I'm sorry for the horrible type signatures. I just wanted to throw this together quickly!
    private static final ImmutableMap<Class<?>, Tuple<Function<ServicesCategory.ServicePluginSubCategory, String>, Supplier<?>>> AVAILABLE_SERVICES =
            ImmutableMap.<Class<?>, Tuple<Function<ServicesCategory.ServicePluginSubCategory, String>, Supplier<?>>>builder()
                    .put(BanService.class, Tuple.of(ServicesCategory.ServicePluginSubCategory::getBanService, SpongeBanService::new))
                    .put(EconomyService.class, Tuple.of(ServicesCategory.ServicePluginSubCategory::getEconomyService, () -> null))
                    // TODO: currently in invalid
                    .put(PaginationService.class, Tuple.of(ServicesCategory.ServicePluginSubCategory::getPaginationService, () -> null))
                    .put(PermissionService.class,
                            Tuple.of(ServicesCategory.ServicePluginSubCategory::getPermissionService, SpongePermissionService::new))
                    .put(SqlService.class, Tuple.of(ServicesCategory.ServicePluginSubCategory::getSqlService, SqlServiceImpl::new))
                    .put(UserStorageService.class,
                            Tuple.of(ServicesCategory.ServicePluginSubCategory::getUserStorageService, SpongeUserStorageService::new))
                    .put(WhitelistService.class,
                            Tuple.of(ServicesCategory.ServicePluginSubCategory::getWhitelistService, SpongeWhitelistService::new))
                    .build();

    /**
     * Discovers services by querying plugins with the
     * {@link ProvideServiceEvent}. To be called at the appropriate moment in
     * the lifecycle.
     *
     * @param servicePluginSubCategory The config sub category
     * @return The completed provider
     */
    public static SpongeServiceProvider discoverServices(final ServicesCategory.ServicePluginSubCategory servicePluginSubCategory) {
        final ImmutableMap.Builder<Class<?>, Registration<?>> services = ImmutableMap.builder();

        // We loop over all available services and try to discover each one.
        // This does NOT support third party service interfaces, only impls.
        for (final Map.Entry<Class<?>, Tuple<Function<ServicesCategory.ServicePluginSubCategory, String>, Supplier<?>>> candidate :
                AVAILABLE_SERVICES.entrySet()) {

            // If the configration file has a specific plugin ID, we look for it. If it's there, we will ONLY query that
            // plugin.
            final Optional<PluginContainer> specificPluginContainer =
                    Sponge.getPluginManager().getPlugin(candidate.getValue().getFirst().apply(servicePluginSubCategory));
            final Collection<PluginContainer> toQuery =
                    specificPluginContainer.<Collection<PluginContainer>>map(ImmutableList::of).orElseGet(Sponge.getPluginManager()::getPlugins);

            Registration<?> registration = null;
            final Iterator<PluginContainer> pluginContainerIterator = toQuery.iterator();
            while (registration == null && pluginContainerIterator.hasNext()) {
                final PluginContainer pluginContainer = pluginContainerIterator.next();
                if (!SpongeImpl.getInternalPlugins().contains(pluginContainer)) { // We don't bother with our internal plugins.
                    registration = getSpecificRegistration(pluginContainer, candidate.getKey());
                }
            }

            if (registration == null) {
                // If we don't have a registration, we try a Sponge one (which is lowest priority)
                registration = createRegistration(candidate.getKey(), candidate.getValue().getSecond(), specificPluginContainer.get());
            }

            // If after all that we have a registration, we... register it.
            if (registration != null) {
                services.put(candidate.getKey(), registration);
                SpongeImpl.getLogger().info("Registered service {} to plugin {}.",
                        registration.clazz.getSimpleName(),
                        registration.pluginContainer.getMetadata().getId());
            }
        }

        // Ta-da.
        return new SpongeServiceProvider(services.build());
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <T> Registration<T> createRegistration(final Class<T> clazz, final Supplier<?> supplier, final PluginContainer container) {
        final T impl = (T) supplier.get();
        if (impl == null) {
            return null;
        }
        return new Registration<>(clazz, (T) supplier.get(), container);
    }

    @Nullable
    private static <T> Registration<T> getSpecificRegistration(final PluginContainer container, final Class<T> service) {
        final ProvideServiceEventImpl<T> event =
                new ProvideServiceEventImpl<>(SpongeImpl.getCauseStackManager().getCurrentCause(), TypeToken.of(service));

        // This is the actual query - a generic event.
        ((SpongeEventManager) Sponge.getEventManager()).post(event, container);
        if (event.getSuggestion() != null) {
            try {
                return new Registration<>(service, event.getSuggestion().get(), container);
            } catch (final Throwable e) { // if the service can't be created
                SpongeImpl.getLogger().error("Could not create service {} from plugin {}.",
                        service.getSimpleName(),
                        container.getMetadata().getId(),
                        e);
                return null;
            }
        }
        SpongeImpl.getLogger().error("Could not create service {} from plugin {}, no service was provided.", service.getSimpleName(),
                container.getMetadata().getId());
        return null;
    }

    // --

    private final Map<Class<?>, Registration<?>> services;

    public SpongeServiceProvider(final Map<Class<?>, Registration<?>> services) {
        this.services = ImmutableMap.copyOf(services);
    }

    @Override
    @NonNull
    @SuppressWarnings("unchecked")
    public <T> Optional<T> provide(@NonNull final Class<T> serviceClass) {
        final Registration<T> registration = (Registration<T>) this.services.get(serviceClass);
        if (registration != null) {
            return Optional.of(registration.service());
        }
        return Optional.empty();
    }

    @Override
    @NonNull
    @SuppressWarnings("unchecked")
    public <T> Optional<ServiceRegistration<T>> registration(@NonNull final Class<T> serviceClass) {
        return Optional.ofNullable((ServiceRegistration<T>) this.services.get(serviceClass));
    }

    @NonNull
    @SuppressWarnings("unchecked")
    private <T> T provideUnchecked(final Class<T> serviceClass) {
        final Registration<T> registration = (Registration<T>) this.services.get(serviceClass);
        if (registration != null) {
            return registration.service();
        }
        throw new IllegalStateException("");
    }

    @Override
    @NonNull
    public BanService banService() {
        return this.provideUnchecked(BanService.class);
    }

    @Override
    @NonNull
    public Optional<EconomyService> economyService() {
        return this.provide(EconomyService.class);
    }

    @Override
    @NonNull
    public PaginationService paginationService() {
        return this.provideUnchecked(PaginationService.class);
    }

    @Override
    @NonNull
    public PermissionService permissionService() {
        return this.provideUnchecked(PermissionService.class);
    }

    @Override
    @NonNull
    public SqlService sqlService() {
        return this.provideUnchecked(SqlService.class);
    }

    @Override
    @NonNull
    public UserStorageService userStorageService() {
        return this.provideUnchecked(UserStorageService.class);
    }

    @Override
    @NonNull
    public WhitelistService whitelistService() {
        return this.provideUnchecked(WhitelistService.class);
    }

    static class Registration<T> implements ServiceRegistration<T> {

        private final Class<T> clazz;
        private final T object;
        private final PluginContainer pluginContainer;

        private Registration(final Class<T> clazz, final T object, final PluginContainer pluginContainer) {
            this.clazz = clazz;
            this.object = Preconditions.checkNotNull(object, "The service must have an implementation!");
            this.pluginContainer = pluginContainer;
        }

        @Override
        @NonNull
        public Class<T> serviceClass() {
            return this.clazz;
        }

        @Override
        @NonNull
        public T service() {
            return this.object;
        }

        @Override
        @NonNull
        public PluginContainer pluginContainer() {
            return this.pluginContainer;
        }
    }

}
