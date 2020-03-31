package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.browser.driver.BrowserControl
import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.files.ext.export
import ai.platon.pulsar.crawl.component.FetchComponent
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.protocol.ForwardingResponse
import ai.platon.pulsar.crawl.protocol.Response
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.RetryScope
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.MultiMetadata
import ai.platon.pulsar.persist.metadata.ProtocolStatusCodes
import ai.platon.pulsar.persist.model.ActiveDomMessage
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.SharedMetricRegistries
import org.openqa.selenium.OutputType
import org.openqa.selenium.remote.RemoteWebDriver
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.time.Duration
import java.time.Instant

open class BrowserEmulatorHandlers(
        private val driverPool: LoadingWebDriverPool,
        private val messageWriter: MessageWriter,
        private val immutableConfig: ImmutableConfig
) {
    private val log = LoggerFactory.getLogger(BrowserEmulatorHandlers::class.java)!!
    private val supportAllCharsets get() = immutableConfig.getBoolean(CapabilityTypes.PARSE_SUPPORT_ALL_CHARSETS, true)
    private val fetchMaxRetry = immutableConfig.getInt(CapabilityTypes.HTTP_FETCH_MAX_RETRY, 3)
    val charsetPattern = if (supportAllCharsets) SYSTEM_AVAILABLE_CHARSET_PATTERN else DEFAULT_CHARSET_PATTERN
    private val metricRegistry = SharedMetricRegistries.getDefault()
    private val pageSourceBytes = metricRegistry.histogram(MetricRegistry.name(BrowserEmulatorHandlers::class.java, "pageSourceBytes"))

    fun logBeforeNavigate(task: FetchTask, driverConfig: BrowserControl) {
        if (log.isTraceEnabled) {
            log.trace("Navigate {}/{}/{} in [t{}]{}, drivers: {}/{}/{}(w/f/o) | {} | timeouts: {}/{}/{}",
                    task.batchTaskId, task.batchSize, task.id,
                    Thread.currentThread().id,
                    if (task.nRetries <= 1) "" else "(${task.nRetries})",
                    driverPool.numWorking, driverPool.numFree, driverPool.numOnline,
                    task.page.configuredUrl,
                    driverConfig.pageLoadTimeout, driverConfig.scriptTimeout, driverConfig.scrollInterval
            )
        }
    }

    fun onAfterBrowse(browseTask: BrowseTask): Response {
        val t = browseTask
        pageSourceBytes.update(t.pageSource.length)

        val browserStatus = checkErrorPage(t.page, t.status)
        t.status = browserStatus.status
        if (browserStatus.code != ProtocolStatusCodes.SUCCESS_OK) {
            t.pageSource = ""
            return ForwardingResponse(t.pageSource, t.status, t.headers, t.page)
        }

        // Check quality of the page source, throw an exception if content is broken
        val integrity = checkHtmlIntegrity(t.pageSource, t.page, t.status, t.task)

        // Check browse timeout event, transform status to be success if the page source is good
        if (t.status.isTimeout) {
            if (integrity.isOK) {
                // fetch timeout but content is OK
                t.status = ProtocolStatus.STATUS_SUCCESS
            }
            handleBrowseTimeout(t.navigateTime, t.pageSource, t.status, t.page, t.driverConfig)
        }

        t.headers.put(HttpHeaders.CONTENT_LENGTH, t.pageSource.length.toString())
        if (integrity.isOK) {
            // Update page source, modify charset directive, do the caching stuff
            t.pageSource = handlePageSource(t.pageSource).toString()
        } else {
            // The page seems to be broken, retry it, if there are too many broken pages in a certain period, reset the browse context
            t.status = handleBrokenPageSource(t.task, integrity)
            logBrokenPage(t.task, t.pageSource, integrity)
        }

        // Update headers, metadata, do the logging stuff
        t.page.lastBrowser = t.driver.browserType
        t.page.htmlIntegrity = integrity
        handleBrowseFinish(t.page, t.headers)

        exportIfNecessary(t.pageSource, t.status, t.page, t.driver)

        // TODO: collect response headers of main resource

        return ForwardingResponse(t.pageSource, t.status, t.headers, t.page)
    }

    open fun checkErrorPage(page: WebPage, status: ProtocolStatus): BrowserStatus {
        val browserStatus = BrowserStatus(status, ProtocolStatusCodes.SUCCESS_OK)
        if (status.minorCode == ProtocolStatusCodes.BROWSER_ERR_CONNECTION_TIMED_OUT) {
            // The browser can not connect to remote peer, it must be caused by the bad proxy ip
            // It might be fixed by resetting the privacy context
            // log.warn("Connection timed out in browser, resetting the browser context")
            browserStatus.status = ProtocolStatus.retry(RetryScope.PRIVACY, status)
            browserStatus.code = status.minorCode
        } else if (status.minorCode == ProtocolStatusCodes.BROWSER_ERROR) {
            browserStatus.status = ProtocolStatus.retry(RetryScope.CRAWL, status)
            browserStatus.code = status.minorCode
        }

        return browserStatus
    }

    open fun checkHtmlIntegrity(pageSource: String,
                                          page: WebPage, status: ProtocolStatus, task: FetchTask): HtmlIntegrity {
        return HtmlIntegrity.OK
    }

    open fun handleBrowseTimeout(startTime: Instant, pageSource: String,
                                 status: ProtocolStatus, page: WebPage, driverConfig: BrowserControl) {
        if (log.isInfoEnabled) {
            val elapsed = Duration.between(startTime, Instant.now())
            val length = pageSource.length

            val link = AppPaths.uniqueSymbolicLinkForURI(page.url)
            log.info("Timeout ({}) after {} with {} drivers: {}/{}/{} timeouts: {}/{}/{} | file://{}",
                    status.minorName,
                    elapsed,
                    Strings.readableBytes(length.toLong()),
                    driverPool.numWorking, driverPool.numFree, driverPool.numOnline,
                    driverConfig.pageLoadTimeout, driverConfig.scriptTimeout, driverConfig.scrollInterval,
                    link)
        }
    }

    open fun handleBrowseFinish(page: WebPage, headers: MultiMetadata) {
        // The page content's encoding is already converted to UTF-8 by Web driver
        headers.put(HttpHeaders.CONTENT_ENCODING, "UTF-8")
        headers.put(HttpHeaders.Q_TRUSTED_CONTENT_ENCODING, "UTF-8")
        headers.put(HttpHeaders.Q_RESPONSE_TIME, System.currentTimeMillis().toString())

        val urls = page.activeDomUrls
        if (urls != null) {
            page.location = urls.location
            if (page.url != page.location) {
                // in-browser redirection
                messageWriter.debugRedirects(page.url, urls)
            }
        }
    }

    fun handlePageSource(pageSource: String): StringBuilder {
        // The browser has already convert source code to UTF-8
        return replaceHTMLCharset(pageSource, charsetPattern, "UTF-8")
    }

    /**
     * Chrome redirected to the error page chrome-error://
     * This page should be text analyzed to determine the actual error
     * */
    fun handleChromeError(message: String): BrowserError {
        val activeDomMessage = ActiveDomMessage.fromJson(message)
        val status = if (activeDomMessage.multiStatus?.status?.ec == BrowserError.CONNECTION_TIMED_OUT) {
            // chrome can not connect to the peer, it probably be caused by a bad proxy
            // convert to retry in PRIVACY_CONTEXT later
            ProtocolStatus.failed(ProtocolStatusCodes.BROWSER_ERR_CONNECTION_TIMED_OUT)
        } else {
            // unhandled exception
            ProtocolStatus.failed(ProtocolStatusCodes.BROWSER_ERROR)
        }
        return BrowserError(status, activeDomMessage)
    }

    fun handleBrokenPageSource(task: FetchTask, htmlIntegrity: HtmlIntegrity): ProtocolStatus {
        return when {
            htmlIntegrity.isBanned -> {
                // should cancel all running tasks and reset the privacy context and then re-fetch them
                ProtocolStatus.retry(RetryScope.PRIVACY, htmlIntegrity)
            }
            task.nRetries > fetchMaxRetry -> {
                // must come after privacy context reset, PRIVACY_CONTEXT reset have the higher priority
                ProtocolStatus.retry(RetryScope.CRAWL)
            }
            htmlIntegrity.isEmpty -> {
                ProtocolStatus.retry(RetryScope.PRIVACY, htmlIntegrity)
            }
            else -> ProtocolStatus.retry(RetryScope.CRAWL)
        }
    }

    fun exportIfNecessary(pageSource: String, status: ProtocolStatus, page: WebPage, driver: ManagedWebDriver) {
        if (log.isDebugEnabled && pageSource.isNotEmpty()) {
            val path = AppFiles.export(status, pageSource, page)

            // Create symbolic link with an url based, unique, shorter but not readable file name,
            // we can generate and refer to this path at any place
            val link = AppPaths.uniqueSymbolicLinkForURI(page.url)
            Files.deleteIfExists(link)
            Files.createSymbolicLink(link, path)

            if (log.isTraceEnabled) {
                // takeScreenshot(pageSource.length.toLong(), page, driver.driver as RemoteWebDriver)
            }
        }
    }

    fun takeScreenshot(contentLength: Long, page: WebPage, driver: RemoteWebDriver) {
        try {
            if (contentLength > 100) {
                val bytes = driver.getScreenshotAs(OutputType.BYTES)
                AppFiles.export(page, bytes, ".png")
            }
        } catch (e: Exception) {
            log.warn("Screenshot failed {} | {}", Strings.readableBytes(contentLength), page.url)
            log.warn(Strings.stringifyException(e))
        }
    }

    fun logBrokenPage(task: FetchTask, pageSource: String, integrity: HtmlIntegrity) {
        val proxyEntry = task.proxyEntry
        val domain = task.domain
        val link = AppPaths.uniqueSymbolicLinkForURI(task.url)
        val readableLength = Strings.readableBytes(pageSource.length.toLong())

        if (proxyEntry != null) {
            val count = proxyEntry.servedDomains.count(domain)
            log.warn("Page is broken with {}({}) using proxy {} in {}({}) | file://{} | {}",
                    readableLength, integrity.name,
                    proxyEntry.display, domain, count, link, task.url)
        } else {
            log.warn("Page is broken with {}({}) | file://{} | {}", readableLength, integrity.name, link, task.url)
        }
    }
}
