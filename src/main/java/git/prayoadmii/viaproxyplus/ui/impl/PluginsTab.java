/*
 * This file is part of ViaProxy - https://github.com/RaphiMC/ViaProxy
 * Copyright (C) 2021-2026 RK_01/RaphiMC and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package git.prayoadmii.viaproxyplus.ui.impl;

import git.prayoadmii.viaproxyplus.ViaProxy;
import git.prayoadmii.viaproxyplus.plugins.ViaProxyPlugin;
import git.prayoadmii.viaproxyplus.ui.I18n;
import git.prayoadmii.viaproxyplus.ui.SoundManager;
import git.prayoadmii.viaproxyplus.ui.UITab;
import git.prayoadmii.viaproxyplus.ui.ViaProxyWindow;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static git.prayoadmii.viaproxyplus.ui.ViaProxyWindow.BORDER_PADDING;

public class PluginsTab extends UITab {

    private DefaultTableModel model;
    private JTable pluginsTable;
    private List<Object> rowEntries;

    public PluginsTab(final ViaProxyWindow frame) {
        super(frame, "plugins");
    }

    @Override
    protected void init(final JPanel contentPane) {
        this.model = new DefaultTableModel(0, 5) {
            @Override
            public boolean isCellEditable(final int row, final int column) {
                return false;
            }
        };
        this.rowEntries = new ArrayList<>();
        contentPane.setLayout(new BorderLayout(BORDER_PADDING, BORDER_PADDING));

        this.pluginsTable = new JTable(this.model);
        this.pluginsTable.setFillsViewportHeight(true);
        this.pluginsTable.setRowHeight(this.pluginsTable.getRowHeight() + BORDER_PADDING);
        this.pluginsTable.setAutoCreateRowSorter(true);
        this.pluginsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.pluginsTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
                                                           final boolean hasFocus, final int row, final int column) {
                final Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBorder(BorderFactory.createEmptyBorder(0, BORDER_PADDING, 0, BORDER_PADDING));
                return component;
            }
        });
        this.pluginsTable.setComponentPopupMenu(this.createContextMenu());
        this.pluginsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent event) {
                this.selectPopupRow(event);
            }

            @Override
            public void mouseReleased(final MouseEvent event) {
                this.selectPopupRow(event);
                if (SwingUtilities.isLeftMouseButton(event)
                        && PluginsTab.this.pluginsTable.rowAtPoint(event.getPoint()) >= 0) {
                    SoundManager.playClick();
                }
            }

            private void selectPopupRow(final MouseEvent event) {
                if (!event.isPopupTrigger()) return;
                final int row = PluginsTab.this.pluginsTable.rowAtPoint(event.getPoint());
                if (row >= 0) PluginsTab.this.pluginsTable.setRowSelectionInterval(row, row);
            }
        });

        final JScrollPane scrollPane = new JScrollPane(this.pluginsTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(BORDER_PADDING, BORDER_PADDING, BORDER_PADDING, BORDER_PADDING));
        contentPane.add(scrollPane, BorderLayout.CENTER);
        this.refresh();
    }

    @Override
    protected void onTabOpened() {
        this.refresh();
    }

    private void refresh() {
        this.model.setColumnIdentifiers(new Object[]{
                I18n.get("tab.plugins.name.label"),
                I18n.get("tab.plugins.version.label"),
                I18n.get("tab.plugins.author.label"),
                I18n.get("tab.plugins.status.label"),
                I18n.get("tab.plugins.dependencies.label")
        });
        this.model.setRowCount(0);
        this.rowEntries.clear();
        for (ViaProxyPlugin plugin : ViaProxy.getPluginManager().getPlugins()) {
            this.rowEntries.add(plugin);
            this.model.addRow(new Object[]{
                    plugin.getName(),
                    plugin.getVersion(),
                    plugin.getAuthor(),
                    plugin.isEnabled() ? I18n.get("tab.plugins.status.enabled") : I18n.get("tab.plugins.status.disabled"),
                    String.join(", ", plugin.getDepends())
            });
        }
            for (File disabledFile : ViaProxy.getPluginManager().getDisabledPluginFiles()) {
                this.rowEntries.add(disabledFile);
                this.model.addRow(new Object[]{
                        disabledFile.getName().substring(0, disabledFile.getName().length() - ".jar.disabled".length()),
                        "",
                        "",
                        I18n.get("tab.plugins.status.disabled"),
                        ""
                });
            }
        if (this.model.getRowCount() == 0) {
            this.model.addRow(new Object[]{I18n.get("tab.plugins.none"), "", "", "", ""});
        }
    }

    private JPopupMenu createContextMenu() {
        final JPopupMenu contextMenu = new JPopupMenu();
        final JMenuItem deleteItem = new JMenuItem(I18n.get("tab.plugins.context_menu.delete"));
        deleteItem.addActionListener(event -> this.deleteSelectedPlugin());
        contextMenu.add(deleteItem);

        final JMenuItem disableItem = new JMenuItem(I18n.get("tab.plugins.context_menu.disable"));
        disableItem.addActionListener(event -> this.disableSelectedPlugin());
        contextMenu.add(disableItem);
        final JMenuItem enableItem = new JMenuItem(I18n.get("tab.plugins.context_menu.enable"));
        enableItem.addActionListener(event -> this.enableSelectedPlugin());
        contextMenu.add(enableItem);
        contextMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(final PopupMenuEvent event) {
            final Object selectedEntry = PluginsTab.this.getSelectedEntry();
            disableItem.setEnabled(selectedEntry instanceof ViaProxyPlugin);
            enableItem.setEnabled(selectedEntry instanceof File);
            }

            @Override
            public void popupMenuWillBecomeInvisible(final PopupMenuEvent event) {
            }

            @Override
            public void popupMenuCanceled(final PopupMenuEvent event) {
            }
        });
        return contextMenu;
    }

    private Object getSelectedEntry() {
        final int selectedRow = this.pluginsTable.getSelectedRow();
        if (selectedRow < 0) return null;

        final int modelRow = this.pluginsTable.convertRowIndexToModel(selectedRow);
        if (modelRow >= this.rowEntries.size()) return null;
        return this.rowEntries.get(modelRow);
    }

    private void deleteSelectedPlugin() {
        final Object selectedEntry = this.getSelectedEntry();
        if (selectedEntry == null) return;
        final String pluginName = selectedEntry instanceof ViaProxyPlugin plugin
                ? plugin.getName()
                : ((File) selectedEntry).getName();

        final int choice = JOptionPane.showConfirmDialog(this.viaProxyWindow,
                I18n.get("tab.plugins.context_menu.delete.confirm", pluginName),
                I18n.get("tab.plugins.context_menu.delete"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;

        final File pluginFile = selectedEntry instanceof ViaProxyPlugin plugin
            ? ViaProxy.getPluginManager().getPluginFile(plugin)
            : (File) selectedEntry;
        if (pluginFile == null) {
            ViaProxyWindow.showError(I18n.get("tab.plugins.context_menu.file_not_found"));
            return;
        }
        try {
            Files.delete(pluginFile.toPath());
            ViaProxyWindow.showInfo(I18n.get("tab.plugins.context_menu.deleted", pluginName));
        } catch (IOException e) {
            ViaProxyWindow.showError(I18n.get("tab.plugins.context_menu.operation_failed", e.getMessage()));
        }
    }

    private void disableSelectedPlugin() {
        final Object selectedEntry = this.getSelectedEntry();
        if (!(selectedEntry instanceof ViaProxyPlugin plugin)) return;

        final int choice = JOptionPane.showConfirmDialog(this.viaProxyWindow,
                I18n.get("tab.plugins.context_menu.disable.confirm", plugin.getName()),
                I18n.get("tab.plugins.context_menu.disable"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;

        final File pluginFile = ViaProxy.getPluginManager().getPluginFile(plugin);
        if (pluginFile == null) {
            ViaProxyWindow.showError(I18n.get("tab.plugins.context_menu.file_not_found"));
            return;
        }
        final File disabledFile = new File(pluginFile.getParentFile(), pluginFile.getName() + ".disabled");
        try {
            Files.move(pluginFile.toPath(), disabledFile.toPath());
            ViaProxyWindow.showInfo(I18n.get("tab.plugins.context_menu.restart_required"));
        } catch (IOException e) {
            ViaProxyWindow.showError(I18n.get("tab.plugins.context_menu.operation_failed", e.getMessage()));
        }
    }

    private void enableSelectedPlugin() {
        final Object selectedEntry = this.getSelectedEntry();
        if (!(selectedEntry instanceof File disabledFile)) return;

        final String pluginName = disabledFile.getName().substring(0, disabledFile.getName().length() - ".jar.disabled".length());
        final int choice = JOptionPane.showConfirmDialog(this.viaProxyWindow,
                I18n.get("tab.plugins.context_menu.enable.confirm", pluginName),
                I18n.get("tab.plugins.context_menu.enable"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;

        final String disabledSuffix = ".disabled";
        final File enabledFile = new File(disabledFile.getParentFile(), disabledFile.getName().substring(0, disabledFile.getName().length() - disabledSuffix.length()));
        try {
            Files.move(disabledFile.toPath(), enabledFile.toPath());
            ViaProxyWindow.showInfo(I18n.get("tab.plugins.context_menu.restart_required"));
        } catch (IOException e) {
            ViaProxyWindow.showError(I18n.get("tab.plugins.context_menu.operation_failed", e.getMessage()));
        }
    }

}