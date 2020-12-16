package me.william278.huskhomes2.Data;

import me.william278.huskhomes2.Data.ErrorLogging.Error;
import me.william278.huskhomes2.Data.ErrorLogging.Errors;
import me.william278.huskhomes2.Main;
import me.william278.huskhomes2.Objects.Home;
import me.william278.huskhomes2.Objects.TeleportationPoint;
import me.william278.huskhomes2.Objects.Warp;
import me.william278.huskhomes2.permissionHomeLimits;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.logging.Level;

public abstract class Database {
    Main plugin;
    Connection connection;

    public Database(Main instance) {
        plugin = instance;
    }

    public abstract Connection getSQLConnection();

    public abstract void load();

    public void initialize() {
        connection = getSQLConnection();
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM " + Main.settings.getPlayerDataTable() + ";");
            ResultSet rs = ps.executeQuery();
            close(ps, rs);

        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Unable to retrieve connection", ex);
        }
    }

    // Insert a teleportation point, returns generated id
    public Integer addTeleportationPoint(TeleportationPoint point, Connection conn) {
        try {
            PreparedStatement ps = conn.prepareStatement("INSERT INTO " + Main.settings.getLocationsDataTable() + " (world,server,x,y,z,yaw,pitch) VALUES(?,?,?,?,?,?,?);", Statement.RETURN_GENERATED_KEYS);

            ps.setString(1, point.getWorldName());
            ps.setString(2, point.getServer());
            ps.setDouble(3, point.getX());
            ps.setDouble(4, point.getY());
            ps.setDouble(5, point.getZ());
            ps.setFloat(6, point.getYaw());
            ps.setFloat(7, point.getPitch());
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        }
        return null;
    }

    // Insert a home into the database
    public void addHome(Home home, int playerID) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = getSQLConnection();
            Integer locationID = addTeleportationPoint(home, conn);

            ps = conn.prepareStatement("INSERT INTO " + Main.settings.getHomesDataTable() + " (player_id,location_id,name,description,public) VALUES(?,?,?,?,?);");
            ps.setInt(1, playerID);
            ps.setInt(2, locationID);
            ps.setString(3, home.getName());
            ps.setString(4, home.getDescription());
            ps.setBoolean(5, home.isPublic());
            ps.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
    }

    // Insert a warp into the database
    public void addWarp(Warp warp) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = getSQLConnection();

            // Add the teleportation point
            Integer locationID = addTeleportationPoint(warp, conn);

            // Insert the warp with the location_id of the last inserted teleport point
            ps = conn.prepareStatement("INSERT INTO " + Main.settings.getWarpsDataTable() + " (location_id,name,description) VALUES(?,?,?);");
            ps.setInt(1, locationID);
            ps.setString(2, warp.getName());
            ps.setString(3, warp.getDescription());
            ps.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
    }

    // Delete a warp
    public void deleteWarp(String warpName) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = getSQLConnection();

            // Delete the warp with the given name
            ps = conn.prepareStatement("DELETE FROM " + Main.settings.getWarpsDataTable() + " WHERE `name`=?;");
            ps.setString(1, warpName);
            ps.executeUpdate();

        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
    }

    // Delete a warp
    public void deleteHome(String homeName, int playerID) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = getSQLConnection();

            // Delete the home with the given name and player ID
            ps = conn.prepareStatement("DELETE FROM " + Main.settings.getHomesDataTable() + " WHERE `name`=? AND `player_id`=?;");
            ps.setString(1, homeName);
            ps.setInt(2, playerID);
            ps.executeUpdate();

        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
    }

    // Insert a player into the database
    public void addPlayer(Player p) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = getSQLConnection();

            ps = conn.prepareStatement("INSERT INTO " + Main.settings.getPlayerDataTable() + " (user_uuid,username,home_slots,rtp_cooldown,is_teleporting) VALUES(?,?,?,?,?);");
            ps.setString(1, p.getUniqueId().toString());
            ps.setString(2, p.getName());
            ps.setInt(3, permissionHomeLimits.getFreeHomes(p));
            ps.setInt(4, 0);
            ps.setBoolean(5, false);

            ps.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
    }

    public void updatePlayerUsername(int playerID, String newName) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("UPDATE " + Main.settings.getPlayerDataTable() + " SET `username`=? WHERE `player_id`=?;");
            ps.setString(1, newName);
            ps.setInt(2, playerID);
            ps.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
    }

    public void setPlayerTeleporting(int playerID, boolean value) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("UPDATE " + Main.settings.getPlayerDataTable() + " SET `is_teleporting`=? WHERE `player_id`=?;");
            ps.setBoolean(1, value);
            ps.setInt(2, playerID);
            ps.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
    }

    public void setPlayerHomeSlots(int playerID, int newValue) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("UPDATE " + Main.settings.getPlayerDataTable() + " SET `home_slots`=? WHERE `player_id`=?;");
            ps.setInt(1, newValue);
            ps.setInt(2, playerID);
            ps.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
    }

    public void setRtpCooldown(int playerID, int newTime) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("UPDATE " + Main.settings.getPlayerDataTable() + " SET `rtp_cooldown`=? WHERE `player_id`=?;");
            ps.setInt(1, newTime);
            ps.setInt(2, playerID);
            ps.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
    }

    // Delete a teleportation point from SQL
    public void deleteTeleportationPoint(int locationID) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = getSQLConnection();

            ps = conn.prepareStatement("DELETE FROM " + Main.settings.getLocationsDataTable() + " WHERE `location_id`=?;");
            ps.setInt(1, locationID);

            ps.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
    }

    // Update the location of a home
    public void setHomePrivacy(String homeName, int ownerID, boolean isPublic) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = getSQLConnection();

            // Set the home location ID to the new teleport point for the given home
            ps = conn.prepareStatement("UPDATE " + Main.settings.getHomesDataTable() + " SET `public`=? WHERE `name`=? AND `player_id`=?;");
            ps.setBoolean(1, isPublic);
            ps.setString(2, homeName);
            ps.setInt(3, ownerID);
            ps.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
    }

    // Update the location of a home
    public void setHomeTeleportPoint(String homeName, int ownerID, TeleportationPoint point) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = getSQLConnection();

            // Add the teleportation point
            Integer locationID = addTeleportationPoint(point, conn);

            // Set the home location ID to the new teleport point for the given home
            ps = conn.prepareStatement("UPDATE " + Main.settings.getHomesDataTable() + " SET `location_id`=? WHERE `name`=? AND `player_id`=?;");
            ps.setInt(1, locationID);
            ps.setString(2, homeName);
            ps.setInt(3, ownerID);
            ps.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
    }

    // Update the location of a home
    public void setWarpTeleportPoint(String warpName, TeleportationPoint point) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = getSQLConnection();

            // Add the teleportation point
            Integer locationID = addTeleportationPoint(point, conn);

            // Set the warp location ID to the new teleport point
            ps = conn.prepareStatement("UPDATE " + Main.settings.getWarpsDataTable() + " SET `location_id`=? WHERE `name`=?;");
            ps.setInt(1, locationID);
            ps.setString(2, warpName);
            ps.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
    }
    // Update the description of a home
    public void setHomeDescription(String homeName, int ownerID, String newDescription) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = getSQLConnection();

            // Update the home description
            ps = conn.prepareStatement("UPDATE " + Main.settings.getHomesDataTable() + " SET `description`=? WHERE `name`=? AND `player_id`=?;");
            ps.setString(1, newDescription);
            ps.setString(2, homeName);
            ps.setInt(3, ownerID);
            ps.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
    }

    // Update the description of a warp
    public void setWarpDescription(String warpName, String newDescription) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = getSQLConnection();

            // Update the warp description
            ps = conn.prepareStatement("UPDATE " + Main.settings.getWarpsDataTable() + " SET `description`=? WHERE `name`=?;");
            ps.setString(1, newDescription);
            ps.setString(2, warpName);
            ps.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
    }

    // Update the name of a home
    public void setHomeName(String oldHomeName, int ownerID, String newHomeName) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = getSQLConnection();

            // Update the home name
            ps = conn.prepareStatement("UPDATE " + Main.settings.getHomesDataTable() + " SET `name`=? WHERE `name`=? AND `player_id`=?;");
            ps.setString(1, newHomeName);
            ps.setString(2, oldHomeName);
            ps.setInt(3, ownerID);

            ps.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
    }

    // Update the name of a warp
    public void setWarpName(String oldWarpName, String newWarpName) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = getSQLConnection();

            // Update the warp name
            ps = conn.prepareStatement("UPDATE " + Main.settings.getWarpsDataTable() + " SET `name`=? WHERE `name`=?;");
            ps.setString(1, newWarpName);
            ps.setString(2, oldWarpName);

            ps.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
    }

    // Update a player's last position teleport point
    public void setTeleportationLastPosition(int playerID, TeleportationPoint point) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = getSQLConnection();

            // Add the teleportation point
            Integer locationID = addTeleportationPoint(point, conn);

            // Set the destination location with the location_id of the last inserted teleport point
            ps = conn.prepareStatement("UPDATE " + Main.settings.getPlayerDataTable() + " SET `last_location_id`=? WHERE `player_id`=?;");
            ps.setInt(1, locationID);
            ps.setInt(2, playerID);
            ps.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
    }

    // Update a player's destination teleport point
    public void setTeleportationDestination(int playerID, TeleportationPoint point) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = getSQLConnection();

            // Add the teleportation point
            Integer locationID = addTeleportationPoint(point, conn);

            // Set the destination location with the location_id of the last inserted teleport point
            ps = conn.prepareStatement("UPDATE " + Main.settings.getPlayerDataTable() + " SET `dest_location_id`=? WHERE `player_id`=?;");
            ps.setInt(1, locationID);
            ps.setInt(2, playerID);
            ps.executeUpdate();

        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
    }

    // Clear a player's destination
    public void clearPlayerDestination(int playerID) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("UPDATE " + Main.settings.getPlayerDataTable() + " SET `dest_location_id`=NULL WHERE `player_id`=?;");
            ps.setInt(1, playerID);
            ps.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
    }

    // Clear a player's last position
    public void clearPlayerLastPosition(int playerID) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("UPDATE " + Main.settings.getPlayerDataTable() + " SET `last_location_id`=NULL WHERE `player_id`=?;");
            ps.setInt(1, playerID);
            ps.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null)
                    ps.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
    }

    public void close(PreparedStatement ps, ResultSet rs) {
        try {
            if (ps != null)
                ps.close();
            if (rs != null)
                rs.close();
        } catch (SQLException ex) {
            Error.close(plugin, ex);
        }
    }
}