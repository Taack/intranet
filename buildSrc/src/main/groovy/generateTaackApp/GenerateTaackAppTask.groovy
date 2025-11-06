package generateTaackApp

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@CompileStatic
abstract class GenerateTaackAppTask extends DefaultTask {

    @OutputDirectory
    abstract DirectoryProperty getOutputDirectory();

    @Input
    abstract Property<String> getAppName()

    @TaskAction
    void generateSources() {
        def p = Paths.get(outputDirectory.get().toString()).toAbsolutePath().parent
        println "Creating new App Module: ${name} in ${p.toString()}."
        createAppFolder(appName.get(), p.toString())
    }

    private static String toPkgName(String appName) {
        appName = appName.uncapitalize()
        appName.replaceAll('([A-Z])','.$1').toLowerCase()
    }

    static void createControllersFolder(String appName, String appPath) {
        String pkgName = toPkgName(appName)
        String pathName = pkgName.replace('.', '/')
        new File("$appPath/grails-app/controllers/$pathName").mkdirs()
        File file = new File("$appPath/grails-app/controllers/$pathName/${appName.capitalize()}Controller.groovy")
        String content = """\
        package $pkgName
         
        import grails.web.api.WebAttributes
        import grails.compiler.GrailsCompileStatic
        import grails.plugin.springsecurity.annotation.Secured
        import taack.render.TaackUiService
        import taack.ui.dsl.UiBlockSpecifier
         
        @GrailsCompileStatic
        @Secured(['ROLE_ADMIN'])
        class ${appName.capitalize()}Controller implements WebAttributes {
            TaackUiService taackUiService
            ${appName.capitalize()}UiService ${appName}UiService
            
            def index() {
                UiBlockSpecifier b = new UiBlockSpecifier().ui {
                  custom 'Hello World!'
                }
                taackUiService.show(b, ${appName}UiService.buildMenu())
            }
        }
         
        """.stripIndent()
        file.write(content)
    }

    static void createDomainFolder(String appName, String appPath) {
        new File("$appPath/grails-app/domain/$appName").mkdirs()
    }

    static void createI18nFolder(String appName, String appPath) {
        new File("$appPath/grails-app/i18n").mkdir()
        new File("$appPath/grails-app/i18n/messages.properties").createNewFile()
    }

    static void createServicesFolder(String appName, String appPath) {
        String pkgName = toPkgName(appName)
        String pathName = pkgName.replace('.', '/')
        new File("$appPath/grails-app/services/$pathName").mkdirs()
        File file = new File("$appPath/grails-app/services/$pathName/${appName.capitalize()}UiService.groovy")
        String content = """\
        package $pkgName
         
        import grails.compiler.GrailsCompileStatic
        import grails.web.api.WebAttributes
        import org.codehaus.groovy.runtime.MethodClosure as MC
        import taack.ui.dsl.UiMenuSpecifier
        import taack.app.TaackApp
        import taack.app.TaackAppRegisterService

        import jakarta.annotation.PostConstruct
 
        @GrailsCompileStatic
        class ${appName.capitalize()}UiService implements WebAttributes {

            static lazyInit = false

            @PostConstruct
            void init() {
                TaackAppRegisterService.register(new TaackApp(${appName.capitalize()}Controller.&index as MC, new String(this.class.getResourceAsStream("/${appName}/${appName}.svg").readAllBytes())))        
            }


            UiMenuSpecifier buildMenu() {
                UiMenuSpecifier m = new UiMenuSpecifier()
                m.ui {
                    menu ${appName.capitalize()}Controller.&index as MC
                }
                m
            }
        }
         
        """.stripIndent()
        file.write(content)
    }

    static void createGrailsAppFolder(String appName, String appPath) {
        createControllersFolder(appName, appPath)
        createDomainFolder(appName, appPath)
        createI18nFolder(appName, appPath)
        createServicesFolder(appName, appPath)
    }

    static void createGrailsPluginFile(String appName, String appPath) {
        String pkgName = toPkgName(appName)
        String pathName = pkgName.replace('.', '/')
        new File("$appPath/src/main/groovy/$pathName").mkdirs()
        File file = new File("$appPath/src/main/groovy/${pathName}/${appName.capitalize()}GrailsPlugin.groovy")
        String content = """\
        package ${pkgName}
         
        import grails.compiler.GrailsCompileStatic
        import grails.plugins.Plugin
         
        @GrailsCompileStatic
        class ${appName.capitalize()}GrailsPlugin extends Plugin {
            // the version or versions of Grails the plugin is designed for
            def grailsVersion = '4.0.3 > *'
            // resources that are excluded from plugin packaging
            def pluginExcludes = [
                   'grails-app/views/error.gsp'
            ]
            
            // TODO Fill in these fields
            def title = "${appName.capitalize()}" // Headline display name of the plugin
            
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
               // TODO Implement post initialization spring config (optional)
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
        }
         
        """.stripIndent()
        file.write(content)
    }

    static void createIcon(String appName, String appPath) {
        File file = new File("$appPath/src/main/resources/$appName/${appName}.svg")
        String content = '''\
        <?xml version='1.0' encoding='iso-8859-1'?>
        <!-- Generator: Adobe Illustrator 19.0.0, SVG Export Plug-In . SVG Version: 6.00 Build 0)  -->
        <svg version='1.1' id='Layer_1' xmlns='http://www.w3.org/2000/svg' xmlns:xlink='http://www.w3.org/1999/xlink' x='0px' y='0px'
        \t viewBox='0 0 512 512' style='enable-background:new 0 0 512 512;' xml:space='preserve'>
        <path style='fill:#1A534A;' d="M511.672,242.96C504.882,107.635,393.011,0,256,0C114.615,0,0,114.616,0,256
        \tc0,117.866,79.662,217.11,188.077,246.873L511.672,242.96z"/>
        <path style='fill:#263230;' d="M512,256c0-4.373-0.111-8.721-0.328-13.04L334.704,65.991l-157.409,190.8L256,335.495L98.601,413.399
        \tl89.474,89.474c21.633,5.939,44.405,9.127,67.923,9.127C397.384,512,512,397.385,512,256z"/>
        <path style='fill:#02C8A7;' d="M367.304,178.087v-33.391c0-61.471-49.833-111.304-111.304-111.304l-44.522,128l44.522,128
        \tC317.471,289.391,367.304,239.558,367.304,178.087z"/>
        <path style='fill:#7DDCD4;' d="M256,289.391v-256c-61.471,0-111.304,49.833-111.304,111.304v33.391
        \tC144.696,239.558,194.529,289.391,256,289.391z"/>
        <path style='fill:#889391;' d="M399.875,340.48c16.963,10.919,31.265,26.001,38.89,42.618
        \tc-23.207,33.313-55.296,59.982-92.828,76.577l-11.13-68.441l11.13-68.441C363.924,323.005,383.191,329.728,399.875,340.48z"/>
        <path style='fill:#616F6D;' d="M345.937,322.794v136.882c-7.224,3.206-14.659,6.022-22.261,8.448l-22.261-72.671l22.261-72.671
        \th21.37C345.347,322.783,345.637,322.783,345.937,322.794z"/>
        <path style='fill:#B0B7B6;' d="M112.131,340.48c-16.963,10.919-31.265,26.001-38.89,42.618
        \tc23.207,33.313,55.296,59.982,92.828,76.577l11.13-68.441l-11.13-68.441C148.083,323.005,128.816,329.728,112.131,340.48z"/>
        <path style='fill:#889391;' d="M166.069,322.794v136.882c7.224,3.206,14.659,6.022,22.261,8.448l22.261-72.671l-22.261-72.671
        \th-21.37C166.659,322.783,166.37,322.783,166.069,322.794z"/>
        <path style='fill:#D8F4F2;' d="M290.285,322.783l-34.288,33.392l32.974,120.005c11.903-1.77,23.496-4.482,34.707-8.057v-145.34
        \tH290.285z"/>
        <path style='fill:#FFFFFF;' d="M221.718,322.783l34.288,33.392L223.033,476.18c-11.903-1.77-23.496-4.482-34.707-8.057v-145.34
        \tH221.718z"/>
        <path style='fill:#02967D;' d="M272.699,367.304l16.273,108.867c-10.763,1.603-21.771,2.438-32.968,2.438l-11.13-122.435
        \tL272.699,367.304z"/>
        <g>
        \t<path style='fill:#02C8A7;' d='M256.003,367.304v111.304c-0.39,0-0.278-65.258-0.145-111.304H256.003z'/>
        \t<path style='fill:#02C8A7;' d='M256.003,322.783v44.522h-0.145C255.925,342.183,256.003,322.783,256.003,322.783z'/>
        \t<path style='fill:#02C8A7;' d="M239.308,367.304l16.696-11.13c0,0-0.39,122.435,0,122.435c-11.208,0-22.205-0.835-32.968-2.438
        \t\tL239.308,367.304z"/>
        </g>
        <polygon style='fill:#0E7563;' points='290.285,322.783 272.699,367.304 256.003,367.304 244.873,346.713 256.003,322.783 '/>
        <path style='fill:#02967D;' d='M221.71,322.783h34.293c0,0-0.078,19.4-0.145,44.522h-16.551L221.71,322.783z'/>
        <g>
        </g>
        <g>
        </g>
        <g>
        </g>
        <g>
        </g>
        <g>
        </g>
        <g>
        </g>
        <g>
        </g>
        <g>
        </g>
        <g>
        </g>
        <g>
        </g>
        <g>
        </g>
        <g>
        </g>
        <g>
        </g>
        <g>
        </g>
        <g>
        </g>
        </svg>
        '''.stripIndent()
        file.write(content)
    }

    static void createSrcFolder(String appName, String appPath) {
        createGrailsPluginFile(appName, appPath)
        new File("$appPath/src/main/resources/$appName").mkdirs()
        createIcon(appName, appPath)
    }

    static void createBuildGradleFile(String appName, String appPath) {
        File file = new File("$appPath/build.gradle")
        String content = '''\
        plugins {
            id 'taack-grails-plugin\'
        }

        grails {
            plugins {
                implementation project(':crew')
            }
        }
         
        '''.stripIndent()
        file.write(content)
    }

    static void createAppFolder(String appName, String destAppPathStr) {
        Path destAppPath = Path.of(destAppPathStr)
        if (!Files.isDirectory(destAppPath)) {
            println "No such file directory: $destAppPathStr"
            return
        }
        Path settingsPath = Path.of("$destAppPathStr/settings.gradle")
        Path serverBuildPath = Path.of("$destAppPathStr/server/build.gradle")
        if (!Files.isRegularFile(settingsPath) || !Files.isRegularFile(serverBuildPath)) {
            println "Invalid destination application path: $destAppPathStr"
            return
        }
        String appPathStr = "$destAppPathStr/app/$appName"
        Path appPath = Path.of(appPathStr)
        if (Files.isDirectory(appPath)) {
            println "App folder already exists: $appPathStr"
            return
        }
        new File("$appPath").mkdirs()
        createGrailsAppFolder(appName, "$appPath")
        createSrcFolder(appName, "$appPath")
        createBuildGradleFile(appName, "$appPath")
        println "Taack application ${appName} created"
    }
}
