import org.zkoss.zkgrails.*

class ZkRedisGrailsPlugin {
    // the plugin version
    def version = "1.1-M1"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.3 > *"
    // the other plugins this plugin depends on
    def dependsOn = [zk: version]
    def loadAfter = ['zk']
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    // TODO Fill in these fields
    def author = "Chanwit Kaewkasi"
    def authorEmail = ""
    def title = "Spring Redis Datastore support for ZK and Grails"
    def description = '''\\
Spring Datastore support for ZK and Grails
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/zk-redis"

    static final String GOSIV_CLASS = "zkgrails.redis.OpenSessionInViewFilter"
    def doWithWebDescriptor = { xml ->
        def supportExts = ZkConfigHelper.supportExtensions

        //
        // e.g. ["*.zul", "/zkau/*"]
        //
        def filterUrls = supportExts.collect{ "*." + it } + ["/zkau/*"]

        //
        // e.g. ["*.zul", "*.dsp", "*.zhtml", "*.svg", "*.xml2html"]
        //
        def urls = supportExts.collect{ "*." + it } +
                   ["*.dsp", "*.zhtml", "*.svg", "*.xml2html"]

        // adding GrailsOpenSessionInView
        def filterElements = xml.'filter'[0]
        filterElements + {
            'filter' {
                'filter-name' ("GOSIVFilter")
                'filter-class' (GOSIV_CLASS)
            }
        }
        // filter for each ZK urls
        def filterMappingElements = xml.'filter-mapping'[0]
        filterUrls.each {p ->
            filterMappingElements + {
                'filter-mapping' {
                    'filter-name'("GOSIVFilter")
                    'url-pattern'("${p}")
                }
            }
        }
    }

    def doWithSpring = {
        // TODO Implement runtime spring config (optional)
    }

    def doWithDynamicMethods = { ctx ->
        // TODO Implement registering dynamic methods to classes (optional)
    }

    def doWithApplicationContext = { applicationContext ->
        // TODO Implement post initialization spring config (optional)
    }

    def onChange = { event ->
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }
}
