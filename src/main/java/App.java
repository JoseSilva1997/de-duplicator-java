import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.UIManager;

import gui.Gui;

public class App {
    public static void main(String[] args) {
        // Modern Windows file picker.
        System.setProperty("sun.awt.windows.useCommonItemDialog", "true");

        FlatLightLaf.setup();

        // Make FlatLaf use the project accent across focus rings, default buttons, etc.
        UIManager.put("Component.accentColor",       new java.awt.Color(0x25, 0x63, 0xEB));
        UIManager.put("Component.focusColor",        new java.awt.Color(0x25, 0x63, 0xEB));
        UIManager.put("Button.arc",                  10);
        UIManager.put("Component.arc",               10);
        UIManager.put("TextComponent.arc",           10);

        Gui.launch();
    }
}
