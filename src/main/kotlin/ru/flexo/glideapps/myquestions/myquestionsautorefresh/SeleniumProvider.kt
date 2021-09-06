package ru.flexo.glideapps.myquestions.myquestionsautorefresh

import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.support.ui.WebDriverWait
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

@Component
class SeleniumProvider {
    init {
        System.setProperty("webdriver.chrome.driver", "chromedriver.exe");
    }
    private lateinit var driver: ChromeDriver
    private lateinit var wait: WebDriverWait
    fun execute(exist: Boolean = false, script: (RemoteWebDriver, WebDriverWait, SeleniumProvider) -> Unit) {
        val options = ChromeOptions()
        options.setExperimentalOption("debuggerAddress", "127.0.0.1:1111")
        driver = if (exist) ChromeDriver(options) else ChromeDriver()
        wait = WebDriverWait(driver, 30)
        script(driver, wait, this)
    }

    fun waitLoading() {
        Thread.sleep(1000)
        while (driver.findElements(By.cssSelector("div.oss-loading-overlay")).isNotEmpty()) {
            Thread.sleep(1000)
        }
    }
}

@Configuration
class SeleniumConfig {
    private val driver = ChromeDriver()
    private val wait = WebDriverWait(driver, 30)
    @Bean
    fun webDriver(): WebDriver {
        return driver
    }

    @Bean
    fun webDriverWait(): WebDriverWait {
        return wait
    }
}