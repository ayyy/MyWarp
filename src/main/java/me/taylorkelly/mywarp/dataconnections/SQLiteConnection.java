package me.taylorkelly.mywarp.dataconnections;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import me.taylorkelly.mywarp.MyWarp;
import me.taylorkelly.mywarp.data.Warp;

public class SQLiteConnection implements DataConnection {

    /**
     * Table.
     */
    private final String table;

    /**
     * Database creation SQL
     */
    private final String WARP_TABLE;

    /**
     * DSN.
     */
    private final String dsn;

    /**
     * Database connection.
     */
    private Connection conn;

    public SQLiteConnection(String dsn, String table) {
        this.dsn = dsn;
        this.table = table;

        WARP_TABLE = "CREATE TABLE `" + table + "` (" + "`id` INTEGER PRIMARY KEY,"
                + "`name` varchar(32) NOT NULL DEFAULT 'warp',"
                + "`creator` varchar(32) NOT NULL DEFAULT 'Player',"
                + "`world` varchar(32) NOT NULL DEFAULT '0'," + "`x` DOUBLE NOT NULL DEFAULT '0',"
                + "`y` smallint NOT NULL DEFAULT '0'," + "`z` DOUBLE NOT NULL DEFAULT '0',"
                + "`yaw` smallint NOT NULL DEFAULT '0'," + "`pitch` smallint NOT NULL DEFAULT '0',"
                + "`publicAll` boolean NOT NULL DEFAULT '1'," + "`permissions` text NOT NULL,"
                + "`groupPermissions` text NOT NULL," + "`welcomeMessage` varchar(100) NOT NULL DEFAULT '',"
                + "`visits` int DEFAULT '0'" + ");";
    }

    /**
     * Establishes a connection with the database
     * 
     * @return a valid connection to the database
     * @throws SQLException
     *             if a database access error occurs
     */
    private synchronized Connection getConnection() throws SQLException {
        if (conn == null || conn.isClosed()) {
            conn = DriverManager.getConnection(dsn);
        }
        return conn;

    }

    @Override
    public synchronized void close() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException ex) {
            MyWarp.logger().log(Level.SEVERE, "Unable to close SQL connection: " + ex);
        }
    }

    @Override
    public void checkDB(boolean createIfNotExist) throws DataConnectionException {
        // Ugly way to prevent JDBC from creating an empty file upon connection.
        if (!createIfNotExist) {
            File database = new File(MyWarp.inst().getDataFolder().getAbsolutePath(), "warps.db");
            if (!database.exists()) {
                throw new DataConnectionException("Database 'warps.db' does not exist.");
            }
        }
        Statement stmnt = null;

        try {
            conn = getConnection();
            DatabaseMetaData dbm = conn.getMetaData();
            stmnt = conn.createStatement();

            if (!JDBCUtil.tableExists(dbm, table)) {
                if (createIfNotExist) {
                    stmnt.execute(WARP_TABLE);
                } else {
                    throw new DataConnectionException("Table '" + table + "' does not exist.");
                }
            }

        } catch (SQLException ex) {
            MyWarp.logger().log(Level.SEVERE, "Table Check Exception: " + ex);
            throw new DataConnectionException(ex);
        } finally {
            try {
                if (stmnt != null) {
                    stmnt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                MyWarp.logger().log(Level.SEVERE, "Table Check Exception (on close): " + ex);
            }
        }
    }

    @Override
    public void updateDB(boolean updateIfNecessary) throws DataConnectionException {
        Statement stmnt = null;

        try {
            conn = getConnection();
            DatabaseMetaData dbm = conn.getMetaData();
            stmnt = conn.createStatement();

            // changing 'y' to smallint is not necessary in SQLite
            // groupPermissions, added with 2.4
            if (!JDBCUtil.columnExistsCaseSensitive(dbm, table, "groupPermissions")) {
                if (updateIfNecessary) {
                    stmnt.execute("ALTER TABLE " + table
                            + " ADD COLUMN `groupPermissions` text NOT NULL DEFAULT ''");
                } else {
                    throw new DataConnectionException("Column 'groupPermissions' does not exist.");
                }
            }
            // visits, added with 2.4
            if (!JDBCUtil.columnExistsCaseSensitive(dbm, table, "visits")) {
                if (updateIfNecessary) {
                    stmnt.execute("ALTER TABLE " + table + " ADD COLUMN `visits` int DEFAULT '0'");
                } else {
                    throw new DataConnectionException("Column 'visits' does not exist.");
                }
            }

        } catch (SQLException ex) {
            MyWarp.logger().log(Level.SEVERE, "Table Update Exception: " + ex);
            throw new DataConnectionException(ex);
        } finally {
            try {
                if (stmnt != null) {
                    stmnt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                MyWarp.logger().log(Level.SEVERE, "Table Update Exception (on close): " + ex);
            }
        }
    }

    @Override
    public Map<String, Warp> getMap() {
        HashMap<String, Warp> ret = new HashMap<String, Warp>();
        Statement stmnt = null;
        ResultSet rsWarps = null;

        try {
            conn = getConnection();
            stmnt = conn.createStatement();

            rsWarps = stmnt.executeQuery("SELECT * FROM " + table);
            while (rsWarps.next()) {
                int index = rsWarps.getInt("id");
                String name = rsWarps.getString("name");
                String creator = rsWarps.getString("creator");
                String world = rsWarps.getString("world");
                double x = rsWarps.getDouble("x");
                int y = rsWarps.getInt("y");
                double z = rsWarps.getDouble("z");
                int yaw = rsWarps.getInt("yaw");
                int pitch = rsWarps.getInt("pitch");
                boolean publicAll = rsWarps.getBoolean("publicAll");
                String permissions = rsWarps.getString("permissions");
                String groupPermissions = rsWarps.getString("groupPermissions");
                String welcomeMessage = rsWarps.getString("welcomeMessage");
                int visits = rsWarps.getInt("visits");
                Warp warp = new Warp(index, name, creator, world, x, y, z, yaw, pitch, publicAll,
                        permissions, groupPermissions, welcomeMessage, visits);
                ret.put(name, warp);
            }
        } catch (SQLException ex) {
            MyWarp.logger().log(Level.SEVERE, "Warp Load Exception: " + ex);
        } finally {
            try {
                if (stmnt != null) {
                    stmnt.close();
                }
                if (rsWarps != null) {
                    rsWarps.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                MyWarp.logger().log(Level.SEVERE, "Warp Load Exception (on close):" + ex);
            }
        }
        return ret;
    }

    @Override
    public synchronized void addWarp(Warp warp) {
        PreparedStatement stmnt = null;

        try {
            conn = getConnection();

            stmnt = conn
                    .prepareStatement("INSERT INTO "
                            + table
                            + " (id, name, creator, world, x, y, z, yaw, pitch, publicAll, permissions, groupPermissions, welcomeMessage, visits) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
            stmnt.setInt(1, warp.getIndex());
            stmnt.setString(2, warp.getName());
            stmnt.setString(3, warp.getCreator());
            stmnt.setString(4, warp.getWorld());
            stmnt.setDouble(5, warp.getX());
            stmnt.setInt(6, warp.getY());
            stmnt.setDouble(7, warp.getZ());
            stmnt.setInt(8, warp.getYaw());
            stmnt.setInt(9, warp.getPitch());
            stmnt.setBoolean(10, warp.isPublicAll());
            stmnt.setString(11, warp.permissionsString());
            stmnt.setString(12, warp.groupPermissionsString());
            stmnt.setString(13, warp.getRawWelcomeMessage());
            stmnt.setInt(14, warp.getVisits());
            stmnt.executeUpdate();
        } catch (SQLException ex) {
            MyWarp.logger().log(Level.SEVERE, "Warp Insert Exception: ", ex);
        } finally {
            try {
                if (stmnt != null) {
                    stmnt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                MyWarp.logger().log(Level.SEVERE, "Warp Insert Exception (on close): ", ex);
            }
        }

    }

    @Override
    public synchronized void deleteWarp(Warp warp) {
        PreparedStatement stmnt = null;
        try {
            conn = getConnection();

            stmnt = conn.prepareStatement("DELETE FROM " + table + " WHERE id = ?");
            stmnt.setInt(1, warp.getIndex());
            stmnt.executeUpdate();
        } catch (SQLException ex) {
            MyWarp.logger().log(Level.SEVERE, "Warp Delete Exception: ", ex);
        } finally {
            try {
                if (stmnt != null) {
                    stmnt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                MyWarp.logger().log(Level.SEVERE, "Warp Delete Exception (on close): ", ex);
            }
        }
    }

    @Override
    public synchronized void publicizeWarp(Warp warp, boolean publicAll) {
        PreparedStatement stmnt = null;

        try {
            conn = getConnection();

            stmnt = conn.prepareStatement("UPDATE " + table + " SET publicAll = ? WHERE id = ?");
            stmnt.setBoolean(1, publicAll);
            stmnt.setInt(2, warp.getIndex());
            stmnt.executeUpdate();
        } catch (SQLException ex) {
            MyWarp.logger().log(Level.SEVERE, "Warp Publicize Exception: ", ex);
        } finally {
            try {
                if (stmnt != null) {
                    stmnt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                MyWarp.logger().log(Level.SEVERE, "Warp Publicize Exception (on close): ", ex);
            }
        }

    }

    @Override
    public synchronized void updateCreator(Warp warp) {
        PreparedStatement stmnt = null;

        try {
            conn = getConnection();

            stmnt = conn.prepareStatement("UPDATE " + table + " SET creator = ? WHERE id = ?");
            stmnt.setString(1, warp.getCreator());
            stmnt.setInt(2, warp.getIndex());
            stmnt.executeUpdate();
        } catch (SQLException ex) {
            MyWarp.logger().log(Level.SEVERE, "Warp Creator Exception: ", ex);
        } finally {
            try {
                if (stmnt != null) {
                    stmnt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                MyWarp.logger().log(Level.SEVERE, "Warp Creator Exception (on close): ", ex);
            }
        }

    }

    @Override
    public synchronized void updateLocation(Warp warp) {
        PreparedStatement stmnt = null;

        try {
            conn = getConnection();

            stmnt = conn.prepareStatement("UPDATE " + table
                    + " SET world = ?, x = ?, y = ?, Z = ?, yaw = ?, pitch = ? WHERE id = ?");
            stmnt.setString(1, warp.getWorld());
            stmnt.setDouble(2, warp.getX());
            stmnt.setInt(3, warp.getY());
            stmnt.setDouble(4, warp.getZ());
            stmnt.setInt(5, warp.getYaw());
            stmnt.setInt(6, warp.getPitch());
            stmnt.setInt(7, warp.getIndex());
            stmnt.executeUpdate();
        } catch (SQLException ex) {
            MyWarp.logger().log(Level.SEVERE, "Warp Location Exception: ", ex);
        } finally {
            try {
                if (stmnt != null) {
                    stmnt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                MyWarp.logger().log(Level.SEVERE, "Warp Location Exception (on close): ", ex);
            }
        }
    }

    @Override
    public synchronized void updatePermissions(Warp warp) {
        PreparedStatement stmnt = null;

        try {
            conn = getConnection();

            stmnt = conn.prepareStatement("UPDATE " + table + " SET permissions = ? WHERE id = ?");
            stmnt.setString(1, warp.permissionsString());
            stmnt.setInt(2, warp.getIndex());
            stmnt.executeUpdate();
        } catch (SQLException ex) {
            MyWarp.logger().log(Level.SEVERE, "Warp Permissions Exception: ", ex);
        } finally {
            try {
                if (stmnt != null) {
                    stmnt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                MyWarp.logger().log(Level.SEVERE, "Warp Permissions Exception (on close): ", ex);
            }
        }

    }

    @Override
    public synchronized void updateGroupPermissions(Warp warp) {
        PreparedStatement stmnt = null;

        try {
            conn = getConnection();

            stmnt = conn.prepareStatement("UPDATE " + table + " SET groupPermissions = ? WHERE id = ?");
            stmnt.setString(1, warp.groupPermissionsString());
            stmnt.setInt(2, warp.getIndex());
            stmnt.executeUpdate();
        } catch (SQLException ex) {
            MyWarp.logger().log(Level.SEVERE, "Warp GroupPermissions Exception: ", ex);
        } finally {
            try {
                if (stmnt != null) {
                    stmnt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                MyWarp.logger().log(Level.SEVERE, "Warp GroupPermissions Exception (on close): ", ex);
            }
        }

    }

    @Override
    public synchronized void updateVisits(Warp warp) {
        PreparedStatement stmnt = null;

        try {
            conn = getConnection();

            stmnt = conn.prepareStatement("UPDATE " + table + " SET visits = ? WHERE id = ?");
            stmnt.setInt(1, warp.getVisits());
            stmnt.setInt(2, warp.getIndex());
            stmnt.executeUpdate();
        } catch (SQLException ex) {
            MyWarp.logger().log(Level.SEVERE, "Warp Visits Exception: ", ex);
        } finally {
            try {
                if (stmnt != null) {
                    stmnt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                MyWarp.logger().log(Level.SEVERE, "Warp Visits Exception (on close): ", ex);
            }
        }
    }

    @Override
    public synchronized void updateWelcomeMessage(Warp warp) {
        PreparedStatement stmnt = null;

        try {
            conn = getConnection();

            stmnt = conn.prepareStatement("UPDATE " + table + " SET welcomeMessage = ? WHERE id = ?");
            stmnt.setString(1, warp.getRawWelcomeMessage());
            stmnt.setInt(2, warp.getIndex());
            stmnt.executeUpdate();
        } catch (SQLException ex) {
            MyWarp.logger().log(Level.SEVERE, "Warp Creator Exception: ", ex);
        } finally {
            try {
                if (stmnt != null) {
                    stmnt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                MyWarp.logger().log(Level.SEVERE, "Warp Creator Exception (on close): ", ex);
            }
        }
    }
}
