package org.spongepowered.common.config.category;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class ServicesCategory extends ConfigCategory {

    @Setting(value = "service-plugin",
            comment = "Services specified here can be implemented by plugins. To ensure that a"
                    + "specific plugin implements a given service, set the relevant option to its"
                    + "plugin ID. If you wish to use Sponge's default for a given service, use"
                    + "'sponge' as the ID.\n\n"
                    + "If the plugin ID is unknown, or the option is set to '?', all plugins will"
                    + "be given the opportunity to register their service. If multiple plugins"
                    + "attempt to register, one will be picked in an implementation dependent way."
                    + "If no plugins attempt to register a service, the Sponge default will be used"
                    + "if one exists.\n\n"
                    + "No Sponge default service exists for the Economy service.")
    private ServicePluginSubCategory servicePlugin = new ServicePluginSubCategory();

    public ServicePluginSubCategory getServicePlugin() {
        return this.servicePlugin;
    }

    @ConfigSerializable
    public static class ServicePluginSubCategory {

        @Setting("ban-service")
        private String banService = "?";

        @Setting("economy-service")
        private String economyService = "?";

        @Setting("pagination-service")
        private String paginationService = "?";

        @Setting("permission-service")
        private String permissionService = "?";

        @Setting("sql-service")
        private String sqlService = "?";

        @Setting("user-storage-service")
        private String userStorageService = "?";

        @Setting("whitelist-service")
        private String whitelistService = "?";

        public String getBanService() {
            return this.banService;
        }

        public String getEconomyService() {
            return this.economyService;
        }

        public String getPaginationService() {
            return this.paginationService;
        }

        public String getPermissionService() {
            return this.permissionService;
        }

        public String getSqlService() {
            return this.sqlService;
        }

        public String getUserStorageService() {
            return this.userStorageService;
        }

        public String getWhitelistService() {
            return this.whitelistService;
        }
    }

}
