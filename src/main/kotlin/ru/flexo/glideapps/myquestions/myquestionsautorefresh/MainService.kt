package ru.flexo.glideapps.myquestions.myquestionsautorefresh

import org.openqa.selenium.By
import org.openqa.selenium.OutputType
import org.openqa.selenium.support.ui.ExpectedConditions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

fun String.getLinks(): String? {
    return Regex("https:\\/\\/glide.+").find(this)?.value
}

@Service
class MainService @Autowired constructor(
    private val mail: EmailReceiver,
    private val selenium: SeleniumProvider,
    private val authenticationPage: AuthenticationPage,
    private val myProjectPage: MyProjectPage,
    private val projectsPage: ProjectsPage
) : NewMessageListener {
    init {
        var retry = true
        while (retry)
        try {
            run2()
        } catch (e: Exception) {
            println("FATAL ERROR: retry")
        }
    }
    private var actualDate = Date()
    private var signInLink = ""

    private fun startWaitSignInEmail() {
        actualDate = Date()
        signInLink = ""
    }

    private fun run2() {
        mail.addNewMessageListener(this)
        authenticationPage.open()
        authenticationPage.loadingScreen.waitUntilHide()
        while (true) {
            if (myProjectPage.reloadButton.isDisplayed()) {
                myProjectPage.reloadDb()
            } else if(authenticationPage.googleSignInButton.isDisplayed()) {
                // AUTHORIZE
                authenticationPage.emailInput.click().sendKeys("flexoqa@yandex.ru")
                startWaitSignInEmail()
                authenticationPage.submit.click()
                authenticationPage.tryAgainButton.waitUntilDisplayed()
                mail.start()

                authenticationPage.goto(signInLink)

                projectsPage.myProjectCategory.click()
                projectsPage.myProjectCard.click()

                myProjectPage.loadingScreen.waitUntilHide()
                while (myProjectPage.okTips.isDisplayed(1000)) {
                    myProjectPage.okTips.click()
                }

                myProjectPage.reloadDb()
            } else {
                myProjectPage.screenshot()
                println("Something wrong, see screenshot")
            }
            Thread.sleep(300000)
        }
    }

    private fun run() {
        mail.addNewMessageListener(this)
        selenium.execute(false) { driver, wait, selenium ->
            val targetProject = By.xpath("//h3[contains(text(), 'My Questions')]")
            val reloadButton = By.cssSelector("div.mini-action-reload")
            val reloadSpin = By.cssSelector("div.mini-action-reload.spin")
            val okTips = By.cssSelector("div.ts-ok-button")
            val loading = By.cssSelector("div.oss-loading-overlay")

            driver.navigate().to("https://go.glideapps.com/o/rKs3tuIy0nS5EYsoBhGH")
//            Thread.sleep(5000)
            selenium.waitLoading()
            while (true) {
                if (driver.findElements(reloadButton).isNotEmpty()){
                    // RELOAD
                    driver.findElement(reloadButton).click()
                    Thread.sleep(1000)
                    if (driver.findElements(reloadSpin).isNotEmpty()) {
                        println ("Reload Successful!!!")
                    } else {
                        println ("Something wrong! Cant reload!")
                    }
                } else if (driver.findElements(By.xpath("//button[contains(text(), 'Sign up with Google')]")).isNotEmpty()) {
                    wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".email-region > input")))
                    // AUTHORIZE
                    val emailInput = driver.findElementByCssSelector(".email-region > input")
                    val submit = driver.findElementByCssSelector("button.email-button")
                    startWaitSignInEmail()
                    emailInput.click()
                    emailInput.sendKeys("flexoqa@yandex.ru")
                    submit.click()
                    wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//a[contains(text(),'Try again')]")))
                    mail.start()
                    driver.navigate().to(signInLink)

                    selenium.waitLoading()

                    // NAVIGATE TO BUTTON
                    wait.until(ExpectedConditions.visibilityOfElementLocated(targetProject))
                    driver.findElement(targetProject).click()
                    wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("My Questions")))
                    driver.findElementByName("My Questions").click()

                    // SPEND TIPS IF DISPLAYED
                    while (driver.findElements(okTips).isNotEmpty()) {
                        driver.findElement(okTips).click()
                    }

                    // RELOAD
                    val dataStep = By.cssSelector("div.data-step")
                    wait.until(ExpectedConditions.visibilityOfElementLocated(dataStep))

                    driver.findElement(reloadButton).click()
                    Thread.sleep(1000)
                    if (driver.findElements(reloadSpin).isNotEmpty()) {
                        println ("Reload Successful!!!")
                    } else {
                        println ("Something wrong! Cant reload!")
                    }

                }
                else {
                    val sc = driver.getScreenshotAs(OutputType.FILE)
                    val f = File("screens\\screen_error_${SimpleDateFormat("HHmmssddMMyyyy").format(Date())}.png")
                    f.createNewFile()
                    f.writeBytes(sc.readBytes())
                    println("Something wrong, see screenshot")
                }
                Thread.sleep(10000)
            }
        }
    }
    override fun onNewMessage(messages: List<GlideSignInMassage>) {
        val mess = messages.lastOrNull { it.receivingDate >= actualDate }
        mess?.let {
            println("Found actual link from ${it.receivingDate}")
            signInLink = it.link.getLinks()!! //TODO retry
            mail.stop()
        }
    }
}