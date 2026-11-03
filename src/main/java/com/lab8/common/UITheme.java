package com.lab8.common;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;

public class UITheme {

    public static void applyTheme(boolean isDark) {
        try {
            if (isDark) {
                UIManager.setLookAndFeel(new FlatDarkLaf());
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
            }

            // Thiết lập font chữ chuẩn hiện đại tiếng Việt
            Font defaultFont = new Font("Segoe UI", Font.PLAIN, 13);
            UIManager.put("defaultFont", defaultFont);
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("TextComponent.arc", 8);
            UIManager.put("ScrollBar.thumbArc", 8);
            UIManager.put("ProgressBar.arc", 8);
        } catch (Exception e) {
            System.err.println("Không thể nạp FlatLaf theme, dùng giao diện mặc định hệ thống: " + e.getMessage());
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
        }
    }

    public static Color getPrimaryColor() {
        return new Color(33, 150, 243);
    }

    public static Color getSuccessColor() {
        return new Color(76, 175, 80);
    }

    public static Color getWarningColor() {
        return new Color(255, 152, 0);
    }

    public static Color getDangerColor() {
        return new Color(244, 67, 54);
    }

    public static Color getCardBackgroundColor() {
        return new Color(45, 52, 65);
    }
}
