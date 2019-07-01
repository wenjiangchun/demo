package com.haze.spatial.epsg.postgresql;

import org.geotools.referencing.factory.epsg.AbstractEpsgFactory;
import org.geotools.util.factory.Hints;
import org.opengis.referencing.FactoryException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class MyAnsiDialectEpsgFactory extends AbstractEpsgFactory {
    /** The default map using ANSI names. */
    private static final String[] ANSI = {
            "[Alias]", "epsg_alias",
            "[Area]", "epsg_area",
            "[Coordinate Axis]", "epsg_coordinateaxis",
            "[Coordinate Axis Name]", "epsg_coordinateaxisname",
            "[Coordinate_Operation]", "epsg_coordoperation",
            "[Coordinate_Operation Method]", "epsg_coordoperationmethod",
            "[Coordinate_Operation Parameter]", "epsg_coordoperationparam",
            "[Coordinate_Operation Parameter Usage]", "epsg_coordoperationparamusage",
            "[Coordinate_Operation Parameter Value]", "epsg_coordoperationparamvalue",
            "[Coordinate_Operation Path]", "epsg_coordoperationpath",
            "[Coordinate Reference System]", "epsg_coordinatereferencesystem",
            "[Coordinate System]", "epsg_coordinatesystem",
            "[Datum]", "epsg_datum",
            "[Ellipsoid]", "epsg_ellipsoid",
            "[Naming System]", "epsg_namingsystem",
            "[Prime Meridian]", "epsg_primemeridian",
            "[Supersession]", "epsg_supersession",
            "[Unit of Measure]", "epsg_unitofmeasure",
            "[Version History]", "epsg_versionhistory",
            "[ORDER]", "coord_axis_order" // a field in epsg_coordinateaxis
    };

    /**
     * Maps the MS-Access names to ANSI names. Keys are MS-Access names including bracket. Values
     * are ANSI names. Keys and values are case-sensitive. The default content of this map is:
     *
     * <pre><table>
     *   <tr><th align="center">MS-Access name</th>            <th align="center">ANSI name</th></tr>
     *   <tr><td>[Alias]</td>                                  <td>epsg_alias</td></tr>
     *   <tr><td>[Area]</td>                                   <td>epsg_area</td></tr>
     *   <tr><td>[Coordinate Axis]</td>                        <td>epsg_coordinateaxis</td></tr>
     *   <tr><td>[Coordinate Axis Name]</td>                   <td>epsg_coordinateaxisname</td></tr>
     *   <tr><td>[Coordinate_Operation]</td>                   <td>epsg_coordoperation</td></tr>
     *   <tr><td>[Coordinate_Operation Method]</td>            <td>epsg_coordoperationmethod</td></tr>
     *   <tr><td>[Coordinate_Operation Parameter]</td>         <td>epsg_coordoperationparam</td></tr>
     *   <tr><td>[Coordinate_Operation Parameter Usage]</td>   <td>epsg_coordoperationparamusage</td></tr>
     *   <tr><td>[Coordinate_Operation Parameter Value]</td>   <td>epsg_coordoperationparamvalue</td></tr>
     *   <tr><td>[Coordinate_Operation Path]</td>              <td>epsg_coordoperationpath</td></tr>
     *   <tr><td>[Coordinate Reference System]</td>            <td>epsg_coordinatereferencesystem</td></tr>
     *   <tr><td>[Coordinate System]</td>                      <td>epsg_coordinatesystem</td></tr>
     *   <tr><td>[Datum]</td>                                  <td>epsg_datum</td></tr>
     *   <tr><td>[Naming System]</td>                          <td>epsg_namingsystem</td></tr>
     *   <tr><td>[Ellipsoid]</td>                              <td>epsg_ellipsoid</td></tr>
     *   <tr><td>[Prime Meridian]</td>                         <td>epsg_primemeridian</td></tr>
     *   <tr><td>[Supersession]</td>                           <td>epsg_supersession</td></tr>
     *   <tr><td>[Unit of Measure]</td>                        <td>epsg_unitofmeasure</td></tr>
     *   <tr><td>[CA.ORDER]</td>                               <td>coord_axis_order</td></tr>
     * </table></pre>
     *
     * Subclasses can modify this map in their constructor in order to provide a different mapping.
     */
    protected final Map map = new LinkedHashMap();

    /**
     * The prefix before any table name. May be replaced by a schema if {@link #setSchema} is
     * invoked.
     */
    private String prefix = "epsg_";

    /**
     * Constructs an authority factory, the hints should describe the data source it use.
     *
     * @param userHints The underlying factories used for objects creation.
     * @throws FactoryException
     */
    public MyAnsiDialectEpsgFactory(final Hints userHints) throws FactoryException {
        super(userHints);
        for (int i = 0; i < ANSI.length; i++) {
            map.put(ANSI[i], ANSI[++i]);
        }
    }
    /**
     * Constructs an authority factory using the specified connection.
     *
     * @param hints The underlying factories used for objects creation.
     * @param connection The connection to the underlying EPSG database.
     * @throws FactoryException
     */
    public MyAnsiDialectEpsgFactory(Hints hints, Connection connection) {
        super(hints, connection);
        for (int i = 0; i < ANSI.length; i++) {
            map.put(ANSI[i], ANSI[++i]);
        }
    }
    /**
     * Constructs an authority factory using the specified connection.
     *
     * @param hints The underlying factories used for objects creation.
     * @param dataSource Used to create a connection to the underlying EPSG database
     * @throws FactoryException
     */
    public MyAnsiDialectEpsgFactory(Hints hints, DataSource dataSource) {
        super(hints, dataSource);
        for (int i = 0; i < ANSI.length; i++) {
            map.put(ANSI[i], ANSI[++i]);
        }
    }

    /**
     * Replaces the {@code "epsg_"} prefix by the specified schema name. If the removal of the
     * {@code "epsg_"} prefix is not wanted, append it to the schema name (e.g. {@code
     * "myschema.epsg_"}). This method should be invoked at construction time only.
     *
     * @param schema The database schema in which the epsg tables are stored.
     */
    public void setSchema(String schema) {
        schema = schema.trim();
        final int length = schema.length();
        if (length == 0) {
            throw new IllegalArgumentException(schema);
        }
        final char separator = schema.charAt(length - 1);
        if (separator != '.' && separator != '_') {
            schema += '.';
        } else if (length == 1) {
            throw new IllegalArgumentException(schema);
        }
        for (final Iterator it = map.entrySet().iterator(); it.hasNext(); ) {
            final Map.Entry entry = (Map.Entry) it.next();
            final String tableName = (String) entry.getValue();
           /* *
             * Update the map, prepending the schema name to the table name so long as the value is
             * a table name and not a field. This algorithm assumes that all old table names start
             * with "epsg_".
             */
           /* if (tableName.startsWith(prefix)) {
                entry.setValue(schema + tableName.substring(prefix.length()));
            }*/
            if (!tableName.equalsIgnoreCase("coord_axis_order")) {
                entry.setValue(schema + tableName);
            }

        }
        prefix = schema;
    }

    /**
     * Modifies the given SQL string to be suitable for non MS-Access databases. This replaces table
     * and field names in the SQL with the new names in the SQL DDL scripts provided with EPSG
     * database.
     *
     * @param statement The statement in MS-Access syntax.
     * @return The SQL statement in ANSI syntax.
     */
    protected String adaptSQL(final String statement) {
        final StringBuilder modified = new StringBuilder(statement);
        for (final Iterator it = map.entrySet().iterator(); it.hasNext(); ) {
            final Map.Entry entry = (Map.Entry) it.next();
            final String oldName = (String) entry.getKey();
            final String newName = (String) entry.getValue();
            /*
             * Replaces all occurences of 'oldName' by 'newName'.
             */
            int start = 0;
            while ((start = modified.indexOf(oldName, start)) >= 0) {
                modified.replace(start, start + oldName.length(), newName);
                start += newName.length();
            }
        }
        return modified.toString();
    }

}
