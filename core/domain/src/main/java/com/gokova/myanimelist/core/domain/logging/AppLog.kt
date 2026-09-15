package com.gokova.myanimelist.core.domain.logging

/**
 * Global logging facade providing layer-specific loggers.
 */
object AppLog {
    @Volatile
    var ui: Logger = NoOpLogger
        private set

    @Volatile
    var viewModel: Logger = NoOpLogger
        private set

    @Volatile
    var domain: Logger = NoOpLogger
        private set

    @Volatile
    var data: Logger = NoOpLogger
        private set

    @Volatile
    var network: Logger = NoOpLogger
        private set

    /**
     * Initializes logger instances for each architectural layer.
     */
    fun init(factory: (tag: String) -> Logger) {
        ui = factory("MAL/UI")
        viewModel = factory("MAL/ViewModel")
        domain = factory("MAL/Domain")
        data = factory("MAL/Data")
        network = factory("MAL/Network")
    }
}
