package ai.platon.pulsar.net.browser

import ai.platon.pulsar.common.StringUtil
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.persist.metadata.BrowserType
import org.apache.commons.lang3.StringUtils
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.remote.RemoteWebDriver
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

data class DriverStat(
        var pageViews: Int = 0
)

enum class DriverStatus {
    UNKNOWN, FREE, WORKING, PAUSED, RETIRED, CRASHED, QUIT
}

data class DriverConfig(
        var pageLoadTimeout: Duration,
        var scriptTimeout: Duration,
        var scrollDownCount: Int,
        var scrollInterval: Duration
) {
    constructor(config: ImmutableConfig) : this(
            config.getDuration(CapabilityTypes.FETCH_PAGE_LOAD_TIMEOUT, Duration.ofMinutes(3)),
            // wait page ready using script, so it can not smaller than pageLoadTimeout
            config.getDuration(CapabilityTypes.FETCH_SCRIPT_TIMEOUT, Duration.ofSeconds(60)),
            config.getInt(CapabilityTypes.FETCH_SCROLL_DOWN_COUNT, 3),
            config.getDuration(CapabilityTypes.FETCH_SCROLL_DOWN_INTERVAL, Duration.ofMillis(500))
    )
}

class ManagedWebDriver(
        val id: Int,
        val driver: WebDriver,
        val priority: Int = 1000
) {
    private val log = LoggerFactory.getLogger(ManagedWebDriver::class.java)

    val stat = DriverStat()

    val status = AtomicReference<DriverStatus>()

    val isPaused
        get() = status.get() == DriverStatus.PAUSED

    val isRetired
        get() = status.get() == DriverStatus.RETIRED

    val isWorking
        get() = status.get() == DriverStatus.WORKING

    val isQuit
        get() = status.get() == DriverStatus.QUIT

    fun pause() {
        status.set(DriverStatus.PAUSED)
    }

    fun retire() {
        status.set(DriverStatus.RETIRED)
    }

    // The proxy entry ready to use
    val proxyEntry = AtomicReference<ProxyEntry>()

    val incognito = AtomicBoolean()

    val sessionId: String?
        get() {
            if (isQuit) {
                return null
            }

            return if (driver is ChromeDevtoolsDriver) {
                driver.sessionId?.toString()
            } else {
                StringUtils.substringBetween(driver.toString(), "(", ")").takeIf { it != "null" }
            }
        }

    val currentUrl: String
        get() {
            return try {
                if (isQuit) "" else driver.currentUrl
            } catch (t: Throwable) {
                "(exception)"
            }
        }

    val pageSource: String
        get() {
            return try {
                if (isQuit) "" else driver.pageSource
            } catch (t: Throwable) {
                "(exception)"
            }
        }

    val browserType: BrowserType
        get() =
            when (driver) {
                is ChromeDevtoolsDriver -> BrowserType.CHROME
                is ChromeDriver -> BrowserType.SELENIUM_CHROME
//            is HtmlUnitDriver -> page.lastBrowser = BrowserType.HTMLUNIT
                else -> {
                    log.warn("Actual browser is set to be NATIVE by selenium engine")
                    BrowserType.NATIVE
                }
            }

    fun navigate(url: String) {
        if (isQuit) {
            return
        }

        return driver.get(url)
    }

    fun evaluate(expression: String): Any? {
        if (isQuit) {
            return null
        }

        return when (driver) {
            is ChromeDriver -> {
                driver.executeScript(expression)
            }
            is ChromeDevtoolsDriver -> {
                driver.evaluate(expression)
            }
            else -> null
        }
    }

    fun evaluateSilently(expression: String): Any? {
        if (isQuit) {
            return null
        }

        try {
            return evaluate(expression)
        } catch (ignored: Throwable) {}

        return null
    }

    fun stopLoading() {
        if (isQuit) {
            return
        }

        if (driver is ChromeDevtoolsDriver) {
            driver.stopLoading()
        } else {
            evaluateSilently(";window.stop();")
        }
    }

    fun scrollDown() {
        if (isQuit) {
            return
        }
    }

    fun setTimeouts(driverConfig: DriverConfig) {
        if (isQuit) {
            return
        }

        if (driver is ChromeDriver) {
            val timeouts = driver.manage().timeouts()
            timeouts.pageLoadTimeout(driverConfig.pageLoadTimeout.seconds, TimeUnit.SECONDS)
            timeouts.setScriptTimeout(driverConfig.scriptTimeout.seconds, TimeUnit.SECONDS)
        } else if (driver is ChromeDevtoolsDriver) {
            driver.pageLoadTimeout = driverConfig.pageLoadTimeout
            driver.scriptTimeout = driverConfig.scriptTimeout
            driver.scrollDownCount = driverConfig.scrollDownCount
            driver.scrollInterval = driverConfig.scrollInterval
        }
    }

    /**
     * Quits this driver, close every associated window
     * */
    fun quit() {
        if (isQuit) {
            return
        }

        if (incognito.get()) {
            deleteAllCookiesSilently()
        }

        status.set(DriverStatus.QUIT)
        driver.quit()
    }

    /**
     * Close redundant web drivers and keep only one to release resources
     * TODO: buggy
     * */
    fun closeIfNotOnly() {
        if (isQuit) return

        val handles = driver.windowHandles.size
        if (handles > 1) {
            // TODO:
            // driver.close()
        }
    }

    fun deleteAllCookiesSilently() {
        if (isQuit) return

        try {
            if (driver is ChromeDevtoolsDriver) {
                val cookies = driver.getCookieNames()
                if (cookies.isNotEmpty()) {
                    val names = cookies.joinToString(", ") { it }
                    log.debug("Deleted cookies: $names")
                }

                // delete all cookies, this can be ignored
                driver.deleteAllCookies()
            } else if (driver is RemoteWebDriver) {
                val names = driver.manage().cookies.map { it.name }
                if (names.isNotEmpty()) {
                    names.forEach { name ->
                        driver.manage().deleteCookieNamed(name)
                    }

                    log.debug("Deleted cookies: $names")
                }

                // delete all cookies, this can be ignored
                driver.manage().deleteAllCookies()
            }
        } catch (e: Throwable) {
            log.info("Failed to delete cookies - {}", StringUtil.simplifyException(e))
        }
    }

    fun deleteAllCookiesSilently(targetUrl: String) {
        if (isQuit) return

        try {
            when (driver) {
                is RemoteWebDriver -> {
                    driver.get(targetUrl)
                    driver.manage().deleteAllCookies()
                }
                is ChromeDevtoolsDriver -> {
                    driver.get(targetUrl)
                    driver.deleteAllCookies()
                }
                else -> {

                }
            }
        } catch (e: Throwable) {
            log.info("Failed to delete cookies - {}", StringUtil.simplifyException(e))
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is ManagedWebDriver && other.id == this.id
    }

    override fun hashCode(): Int {
        return id
    }

    override fun toString(): String {
        return if (sessionId != null) "#$id-$sessionId" else "#$id(closed)"
    }
}
