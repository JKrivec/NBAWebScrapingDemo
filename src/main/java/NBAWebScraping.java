import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxDriverLogLevel;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.Select;

import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NBAWebScraping {
    public static void main(String[] args) {
        // Strings, xpaths, etc
        final String nbaUrl = "https://stats.nba.com/";
        final String acceptCookiesElementId = "onetrust-accept-btn-handler";

        // Scan for player name
        Scanner sc = new Scanner(System.in);
        String playerName = sc.nextLine();

        // Remove logging, set webdriver options
        Logger.getLogger("org.openqa.selenium.remote").setLevel(Level.SEVERE);
        System.setProperty(FirefoxDriver.SystemProperty.BROWSER_LOGFILE,"/dev/null");
        // geckodriver.exe should be included in the same directory as JAR
        System.setProperty("webdriver.gecko.driver", "./geckodriver.exe");
        FirefoxOptions options = new FirefoxOptions();
        options.setHeadless(true);
        options.setLogLevel(FirefoxDriverLogLevel.FATAL);
        // Init webdriver
        FirefoxDriver driver = new FirefoxDriver(options);
        driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
        // Try and load the nba website
        try {
            driver.get(nbaUrl);
        } catch (org.openqa.selenium.WebDriverException err) {
            // Catch possible network error
            System.out.println("There was an error getting the NBA website");
            driver.quit();
            System.exit(1);
        }
        // Accept cookies
        driver.findElementById(acceptCookiesElementId).click();
        // Call function to search for given player
        attemptPlayerSearch(driver, playerName, 1);
        driver.quit();

    }

    private static void attemptPlayerSearch(FirefoxDriver driver, String playerName, int attempt){
        // Strings, xpaths, etc
        final String searchBar0Xpath = "/html/body/main/div[1]/div/div[1]/div[1]/a/span";
        final String searchBarXpath = "/html/body/main/div[1]/div/div[1]/div[1]/input";
        final String firstSearchResult = "/html/body/main/div[1]/div/div[1]/div[2]/div[2]/div[1]/div/a";

        // Locate the searchBar and search.
        // Sometimes the page randomly reloads, which calls for a retry
        try {
            WebElement searchBar0 =  driver.findElementByXPath(searchBar0Xpath);
            searchBar0.click();
            WebElement searchBar =  driver.findElementByXPath(searchBarXpath);
            searchBar.sendKeys(playerName);

            try {
                // Try to click the first result that pops up
                WebElement firstResult = driver.findElementByXPath(firstSearchResult);
                firstResult.click();
            } catch (org.openqa.selenium.NoSuchElementException | org.openqa.selenium.ElementNotInteractableException ex) {
                System.out.println("Player couldn't be found.");
                return;
            }
            // Players page was loaded, call the function to get his 3pa statistics
            find3paStatistics(driver);

        } catch (org.openqa.selenium.StaleElementReferenceException | org.openqa.selenium.ElementClickInterceptedException ex) {
            // Avoid endless recursion
            if (attempt > 5) {
                System.out.println("Error while searching for player.");
                return;
            }
            // Sleep and retry search in 0.2sec
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            attemptPlayerSearch(driver, playerName, attempt + 1);
        }

    }


    private static void find3paStatistics(FirefoxDriver driver){
        // Strings, xpaths, etc
        final String firstFoundTableNameXpath = "/html/body/main/div[2]/div/div/div[3]/div/div/div/div[5]/a";
        final String firstFoundTableBodyXpath = "/html/body/main/div[2]/div/div/div[3]/div/div/div/nba-stat-table[1]/div[2]/div[1]/table";
        final String threePAfieldXpath = "/html/body/main/div[2]/div/div/div[3]/div/div/div/nba-stat-table[1]/div[2]/div[1]/table/thead/tr/th[10]";

        // Some players only have one of the possible selections available, Historic players for example
        try {
            Select seasonType = new Select(driver.findElementByName("SeasonType"));
            Select perMode = new Select(driver.findElementByName("PerMode"));

            // Some players can select both, but dont have the needed options
            List<WebElement> seasonTypeOptions = seasonType.getOptions();
            List<WebElement> perModeOptions = perMode.getOptions();
            if (checkListForElement(seasonTypeOptions, "Regular Season") && checkListForElement(perModeOptions, "Per 40 Minutes")) {
                seasonType.selectByVisibleText("Regular Season");
                perMode.selectByVisibleText("Per 40 Minutes");
            } else {
                System.out.println("3PA statistics wanted are not available for this player.");
                return;
            }

        } catch (org.openqa.selenium.NoSuchElementException ex) {
            System.out.println("3PA statistics wanted are not available for this player.");
            return;
        }


        // I tried checking if the table name and column names matched,
        // but the tables just get messed up even after js finished loading
        // no matter what I do... so I sleep to get the right data..hopefully..
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Get table contents and print
        WebElement traditionalTableBody = driver.findElementByXPath(firstFoundTableBodyXpath);
        List<WebElement> rows = traditionalTableBody.findElements(By.tagName("tr"));
        for (int i = 1; i < rows.size(); i++) {
            WebElement row = rows.get(i);
            String season;
            String score3PA;

                season = row.findElement(By.xpath(".//td[1]")).getText();
                score3PA =  row.findElement(By.xpath(".//td[10]")).getText();

            System.out.println(season +" "+score3PA);
        }
    }

    // Checks if list of WebElements contains element with text equal to elName
    private static Boolean checkListForElement(List<WebElement> list,String elName) {
        for (WebElement el : list) {
            if (el.getText().equals(elName))
                return true;
        }
        return  false;
    }

}
