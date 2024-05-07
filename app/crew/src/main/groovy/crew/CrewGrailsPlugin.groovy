package crew

import grails.compiler.GrailsCompileStatic
import grails.plugins.Plugin
import org.codehaus.groovy.runtime.MethodClosure
import org.taack.Attachment
import taack.ui.TaackPlugin
import taack.ui.TaackPluginConfiguration

/*
TODO: put user extra configuration accessible to server to centralize configuration
 */
@GrailsCompileStatic
class CrewGrailsPlugin extends Plugin implements TaackPlugin {
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "4.0.3 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    // TODO Fill in these fields
    def title = "Crew" // Headline display name of the plugin

    def profiles = ['web']

    Closure doWithSpring() {
        { ->
            // TODO Implement runtime spring config (optional)
        }
    }

    void doWithDynamicMethods() {
        // TODO Implement registering dynamic methods to classes (optional)
    }

    void doWithApplicationContext() {
//        for (GrailsClass cls in grailsApplication.getArtefacts("Domain")) {
//            cls.clazz.declaredFields.each {
//                Annotation[] annotations = it.getDeclaredAnnotations()
////                println "${cls.clazz}: ${it}: ${annotations}"
//                for (Annotation annotation : annotations) {
//                    if (annotation instanceof AttachmentTypes) {
//                        println "AUO 42763 " + annotation.value()
//                    }
//                }
//
//            }
//        }
    }

    void onChange(Map<String, Object> event) {
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    void onConfigChange(Map<String, Object> event) {
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    void onShutdown(Map<String, Object> event) {
        // TODO Implement code that is executed when the application shuts down (optional)
    }

    static final List<TaackPluginConfiguration.PluginRole> crewPluginRoles = [
            new TaackPluginConfiguration.PluginRole("ROLE_CREW_ADMIN", TaackPluginConfiguration.PluginRole.RoleRanking.DIRECTOR),
            new TaackPluginConfiguration.PluginRole("ROLE_CREW_MANAGER", TaackPluginConfiguration.PluginRole.RoleRanking.MANAGER),
            new TaackPluginConfiguration.PluginRole("ROLE_CREW_USER", TaackPluginConfiguration.PluginRole.RoleRanking.USER),
    ]

    static final List<TaackPluginConfiguration.TaackLinkClass> linkClasses = [
            new TaackPluginConfiguration.TaackLinkClass(Attachment.class, 'publicName', AttachmentController.&showAttachment as MethodClosure)
    ]

    static final TaackPluginConfiguration crewPluginConfiguration = new TaackPluginConfiguration("Crew",
            "/crew/crew.svg", "crew",
            new TaackPluginConfiguration.IPluginRole() {
                @Override
                List<TaackPluginConfiguration.PluginRole> getPluginRoles() {
                    crewPluginRoles
                }
            })

    static final List<TaackPluginConfiguration.PluginRole> attPluginRoles = [
            new TaackPluginConfiguration.PluginRole("ROLE_ATT_ADMIN", TaackPluginConfiguration.PluginRole.RoleRanking.DIRECTOR),
            new TaackPluginConfiguration.PluginRole("ROLE_ATT_MANAGER", TaackPluginConfiguration.PluginRole.RoleRanking.MANAGER),
            new TaackPluginConfiguration.PluginRole("ROLE_ATT_USER", TaackPluginConfiguration.PluginRole.RoleRanking.USER),
    ]

    static final TaackPluginConfiguration attPluginConfiguration = new TaackPluginConfiguration("Attachment",
            "/att/att.svg", "attachment",
            new TaackPluginConfiguration.IPluginRole() {
                @Override
                List<TaackPluginConfiguration.PluginRole> getPluginRoles() {
                    attPluginRoles
                }
            },
            new TaackPluginConfiguration.ITaackLinkClass() {
                @Override
                List<TaackPluginConfiguration.TaackLinkClass> getTaackLinkClasses() {
                    linkClasses
                }
            })

    @Override
    List<TaackPluginConfiguration> getTaackPluginControllerConfigurations() {
        [crewPluginConfiguration, attPluginConfiguration]
    }
}
