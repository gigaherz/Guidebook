package gigaherz.guidebook.common;

import java.io.File;

public interface IModProxy
{
    void init();

    void displayBook(String book);

    void preInit(File modConfigurationDirectory);
}
