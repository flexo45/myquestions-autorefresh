package ru.flexo.glideapps.myquestions.myquestionsautorefresh

import org.openqa.selenium.*
import org.openqa.selenium.NoSuchElementException
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.support.FindBy
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

open class SmartWebElement(private val by: By,
                           private val driver: WebDriver,
                           private val wait: WebDriverWait) {
    private val element = { driver.findElement(by) }
    fun click(): SmartWebElement {
        flackySafety {
            element().click()
        }
        return this
    }
    fun sendKeys(vararg keysToSend: CharSequence): SmartWebElement {
        flackySafety {
            element().sendKeys(*keysToSend)
        }
        return this
    }
    fun isDisplayed(timeout: Long = 0L): Boolean {
        Thread.sleep(timeout)
        return driver.findElements(by).isNotEmpty()
    }
    fun waitUntilDisplayed(): SmartWebElement {
        wait.until(ExpectedConditions.visibilityOfElementLocated(by))
        return this
    }
    fun waitUntilHide() {
        while (isDisplayed()) {
            Thread.sleep(1000)
        }
    }
    private fun flackySafety(action: () -> Unit) {
        var timeout = 30
        while (true) {
            try {
                action()
                return
            } catch (e: NoSuchElementException) {
                Thread.sleep(1000)
            } catch (e: StaleElementReferenceException) {
                Thread.sleep(1000)
            }
            if (timeout < 0) { throw TimeoutException("Cant find element $by") }
            timeout--
        }
    }
}

@Component
class SmartWebElementFactory @Autowired constructor(private val driver: WebDriver,
                                                    private val wait: WebDriverWait) {
    fun create(locator: By): SmartWebElement {
        return SmartWebElement(locator, driver, wait)
    }
}

abstract class Page(protected val driver: WebDriver, protected val elementFactory: SmartWebElementFactory) {
    val loadingScreen = elementFactory.create(By.cssSelector("div.oss-loading-overlay"))
    fun open() {
        goto("https://go.glideapps.com/o/rKs3tuIy0nS5EYsoBhGH")
    }
    fun goto(link: String) {
        driver.navigate().to(link)
    }
    fun screenshot() {
        val sc = (driver as ChromeDriver).getScreenshotAs(OutputType.FILE)
        val f = File("screens\\screen_error_${SimpleDateFormat("HHmmssddMMyyyy").format(Date())}.png")
        f.createNewFile()
        f.writeBytes(sc.readBytes())
    }
}

@Component
class AuthenticationPage @Autowired constructor(driver: WebDriver, elementFactory: SmartWebElementFactory)
    : Page(driver, elementFactory) {
    val googleSignInButton = elementFactory.create(By.xpath("//button[contains(text(), 'Sign up with Google')]"))
    val emailInput = elementFactory.create(By.cssSelector(".email-region > input"))
    val submit = elementFactory.create(By.cssSelector("button.email-button"))
    val tryAgainButton = elementFactory.create(By.xpath("//a[contains(text(),'Try again')]"))
}

@Component
class ProjectsPage @Autowired constructor(driver: WebDriver, elementFactory: SmartWebElementFactory)
    : Page(driver, elementFactory) {
    val myProjectCategory = elementFactory.create(By.xpath("//h3[contains(text(), 'My Questions')]"))
    val myProjectCard = elementFactory.create(By.name("My Questions"))
}

@Component
class MyProjectPage @Autowired constructor(driver: WebDriver, elementFactory: SmartWebElementFactory)
    : Page(driver, elementFactory) {
    val reloadButton = elementFactory.create(By.cssSelector("div.mini-action-reload"))
    val reloadSpin = elementFactory.create(By.cssSelector("div.mini-action-reload.spin"))
    val okTips = elementFactory.create(By.cssSelector("div.ts-ok-button"))

    fun reloadDb() {
        reloadButton.click()
        if (reloadSpin.isDisplayed(1000)) {
            println ("Reload Successful!!! at ${Date()}")
        } else {
            screenshot()
            println ("Something wrong! Cant reload! at ${Date()}")
        }
    }
}