package page;

import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.support.FindBy;

public class HomePage {
    @FindBy(css = ".jumbotron h1")
    public SelenideElement header;
}
