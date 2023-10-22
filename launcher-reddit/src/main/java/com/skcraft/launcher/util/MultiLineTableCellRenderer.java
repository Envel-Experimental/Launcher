package com.skcraft.launcher.util;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class MultiLineTableCellRenderer extends DefaultTableCellRenderer {
    private JTextArea textArea;
    private Font originalFont;

    public MultiLineTableCellRenderer() {
        textArea = new JTextArea();
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        textArea.setText((value == null) ? "" : value.toString());
        if (isSelected) {
            textArea.setBackground(table.getSelectionBackground());
            textArea.setForeground(table.getSelectionForeground());
            textArea.setFont(originalFont);
        } else {
            textArea.setBackground(table.getBackground());
            textArea.setForeground(table.getForeground());
        }
        return textArea;
    }

    @Override
    public void setFont(Font font) {
        super.setFont(font);
        originalFont = font;
    }
}