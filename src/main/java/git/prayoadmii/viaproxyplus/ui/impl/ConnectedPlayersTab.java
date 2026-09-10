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

import com.mojang.authlib.GameProfile;
import io.netty.channel.Channel;
import git.prayoadmii.viaproxyplus.ViaProxy;
import git.prayoadmii.viaproxyplus.proxy.session.DummyProxyConnection;
import git.prayoadmii.viaproxyplus.proxy.session.ProxyConnection;
import git.prayoadmii.viaproxyplus.saves.impl.accounts.Account;
import git.prayoadmii.viaproxyplus.ui.I18n;
import git.prayoadmii.viaproxyplus.ui.SoundManager;
import git.prayoadmii.viaproxyplus.ui.UITab;
import git.prayoadmii.viaproxyplus.ui.ViaProxyWindow;
import git.prayoadmii.viaproxyplus.ui.events.UICloseEvent;
import net.lenni0451.lambdaevents.EventHandler;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import static git.prayoadmii.viaproxyplus.ui.ViaProxyWindow.BORDER_PADDING;

public class ConnectedPlayersTab extends UITab {

    private DefaultTableModel model;
    private JTable playersTable;
    private List<ProxyConnection> connections;
    private Timer refreshTimer;

    public ConnectedPlayersTab(final ViaProxyWindow frame) {
        super(frame, "connected_players");
    }

    @Override
    protected void init(final JPanel contentPane) {
        this.model = new DefaultTableModel(0, 4) {
            @Override
            public boolean isCellEditable(final int row, final int column) {
                return false;
            }
        };
        this.model.setColumnIdentifiers(new Object[]{
                I18n.get("tab.connected_players.name.label"),
                I18n.get("tab.connected_players.version.label"),
                I18n.get("tab.connected_players.proxied_to.label"),
                I18n.get("tab.connected_players.ip.label")
        });
        this.connections = new ArrayList<>();
        contentPane.setLayout(new BorderLayout(BORDER_PADDING, BORDER_PADDING));

        this.playersTable = new JTable(this.model);
        this.playersTable.setFillsViewportHeight(true);
        this.playersTable.setRowHeight(this.playersTable.getRowHeight() + BORDER_PADDING);
        this.playersTable.setAutoCreateRowSorter(true);
        this.playersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.playersTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
                                                           final boolean hasFocus, final int row, final int column) {
                final Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBorder(BorderFactory.createEmptyBorder(0, BORDER_PADDING, 0, BORDER_PADDING));
                return component;
            }
        });
        this.playersTable.setComponentPopupMenu(this.createContextMenu());
        this.playersTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent event) {
                this.selectPopupRow(event);
            }

            @Override
            public void mouseReleased(final MouseEvent event) {
                this.selectPopupRow(event);
                if (SwingUtilities.isLeftMouseButton(event)
                        && ConnectedPlayersTab.this.playersTable.rowAtPoint(event.getPoint()) >= 0) {
                    SoundManager.playClick();
                }
            }

            private void selectPopupRow(final MouseEvent event) {
                if (!event.isPopupTrigger()) return;
                final int row = ConnectedPlayersTab.this.playersTable.rowAtPoint(event.getPoint());
                if (row >= 0) ConnectedPlayersTab.this.playersTable.setRowSelectionInterval(row, row);
            }
        });

        final JScrollPane scrollPane = new JScrollPane(this.playersTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(BORDER_PADDING, BORDER_PADDING, BORDER_PADDING, BORDER_PADDING));
        contentPane.add(scrollPane, BorderLayout.CENTER);

        this.refreshTimer = new Timer(1000, event -> this.refresh());
        this.refreshTimer.start();
        this.refresh();
    }

    @Override
    protected void onTabOpened() {
        this.refresh();
    }

    private void refresh() {
        final ProxyConnection selectedConnection = this.getSelectedConnection();
        final List<ProxyConnection> currentConnections = new ArrayList<>();
        final List<Object[]> currentRows = new ArrayList<>();
        for (Channel channel : ViaProxy.getConnectedClients()) {
            final ProxyConnection connection = ProxyConnection.fromChannel(channel);
            if (connection == null || connection instanceof DummyProxyConnection || connection.isClosed()) continue;
            currentConnections.add(connection);
            currentRows.add(new Object[]{
                    this.getPlayerName(connection),
                    connection.getClientVersion() == null ? "" : connection.getClientVersion().getName(),
                    this.getAccountName(connection),
                    this.getIp(channel.remoteAddress())
            });
        }

        if (this.connections.equals(currentConnections) && this.rowsEqual(currentRows)) return;

        this.model.setColumnIdentifiers(new Object[]{
                I18n.get("tab.connected_players.name.label"),
                I18n.get("tab.connected_players.version.label"),
                I18n.get("tab.connected_players.proxied_to.label"),
                I18n.get("tab.connected_players.ip.label")
        });
        this.model.setRowCount(0);
        this.connections = currentConnections;
        for (Object[] row : currentRows) {
            this.model.addRow(row);
        }
        if (this.model.getRowCount() == 0) {
            this.model.addRow(new Object[]{I18n.get("tab.connected_players.none"), "", "", ""});
        }

        if (selectedConnection != null) {
            final int selectedIndex = this.connections.indexOf(selectedConnection);
            if (selectedIndex >= 0) {
                final int viewIndex = this.playersTable.convertRowIndexToView(selectedIndex);
                this.playersTable.setRowSelectionInterval(viewIndex, viewIndex);
            }
        }
    }

    private boolean rowsEqual(final List<Object[]> rows) {
        if (this.model.getRowCount() != rows.size()) return false;
        for (int row = 0; row < rows.size(); row++) {
            final Object[] values = rows.get(row);
            for (int column = 0; column < values.length; column++) {
                if (!java.util.Objects.equals(this.model.getValueAt(row, column), values[column])) return false;
            }
        }
        return true;
    }

    private String getPlayerName(final ProxyConnection connection) {
        final GameProfile gameProfile = connection.getGameProfile();
        if (gameProfile != null && gameProfile.getName() != null) return gameProfile.getName();
        if (connection.getLoginHelloPacket() != null) return connection.getLoginHelloPacket().name;
        return I18n.get("tab.connected_players.connecting");
    }

    private String getAccountName(final ProxyConnection connection) {
        if (connection.getUserOptions() == null) return I18n.get("tab.connected_players.none_value");
        final Account account = connection.getUserOptions().account();
        return account == null ? I18n.get("tab.connected_players.none_value") : account.getDisplayString();
    }

    private String getIp(final SocketAddress address) {
        if (address instanceof InetSocketAddress inetSocketAddress) {
            return inetSocketAddress.getAddress() == null ? inetSocketAddress.getHostString() : inetSocketAddress.getAddress().getHostAddress();
        }
        return address == null ? "" : address.toString();
    }

    private JPopupMenu createContextMenu() {
        final JPopupMenu contextMenu = new JPopupMenu();
        final JMenuItem kickItem = new JMenuItem(I18n.get("tab.connected_players.context_menu.kick"));
        kickItem.addActionListener(event -> this.kickSelectedPlayer());
        contextMenu.add(kickItem);
        contextMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(final PopupMenuEvent event) {
                kickItem.setEnabled(ConnectedPlayersTab.this.getSelectedConnection() != null);
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

    private ProxyConnection getSelectedConnection() {
        final int selectedRow = this.playersTable.getSelectedRow();
        if (selectedRow < 0) return null;
        final int modelRow = this.playersTable.convertRowIndexToModel(selectedRow);
        return modelRow < this.connections.size() ? this.connections.get(modelRow) : null;
    }

    private void kickSelectedPlayer() {
        final ProxyConnection connection = this.getSelectedConnection();
        if (connection == null) return;

        final int choice = JOptionPane.showConfirmDialog(this.viaProxyWindow,
                I18n.get("tab.connected_players.context_menu.kick.confirm", this.getPlayerName(connection)),
                I18n.get("tab.connected_players.context_menu.kick"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;

        try {
            connection.kickClient(I18n.get("tab.connected_players.context_menu.kick.message"));
        } catch (Throwable ignored) {
            connection.getC2P().close();
        }
        this.refresh();
    }

    @EventHandler(events = UICloseEvent.class)
    void stopRefreshTimer() {
        if (this.refreshTimer != null) this.refreshTimer.stop();
    }

}