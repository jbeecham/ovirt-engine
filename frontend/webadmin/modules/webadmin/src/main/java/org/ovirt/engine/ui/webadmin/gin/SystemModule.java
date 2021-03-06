package org.ovirt.engine.ui.webadmin.gin;

import org.ovirt.engine.ui.common.gin.BaseSystemModule;
import org.ovirt.engine.ui.common.section.DefaultLoginSectionPlace;
import org.ovirt.engine.ui.common.section.DefaultMainSectionPlace;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationMessages;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.ApplicationTemplates;
import org.ovirt.engine.ui.webadmin.place.ApplicationPlaces;
import org.ovirt.engine.ui.webadmin.place.WebAdminPlaceManager;
import org.ovirt.engine.ui.webadmin.system.ApplicationInit;
import org.ovirt.engine.ui.webadmin.system.InternalConfiguration;

import com.google.inject.Singleton;
import com.gwtplatform.mvp.client.proxy.PlaceManager;

/**
 * GIN module containing WebAdmin infrastructure and configuration bindings.
 */
public class SystemModule extends BaseSystemModule {

    @Override
    protected void configure() {
        bindInfrastructure();
        bindConfiguration();
    }

    void bindInfrastructure() {
        bindCommonInfrastructure();
        bind(PlaceManager.class).to(WebAdminPlaceManager.class).in(Singleton.class);
        bind(ApplicationInit.class).asEagerSingleton();
        bind(InternalConfiguration.class).asEagerSingleton();
    }

    void bindConfiguration() {
        bindConstant().annotatedWith(DefaultLoginSectionPlace.class)
                .to(ApplicationPlaces.DEFAULT_LOGIN_SECTION_PLACE);
        bindConstant().annotatedWith(DefaultMainSectionPlace.class)
                .to(ApplicationPlaces.DEFAULT_MAIN_SECTION_PLACE);

        bindResourceConfiguration(ApplicationConstants.class, ApplicationMessages.class,
                ApplicationResources.class, ApplicationTemplates.class);
    }

}
