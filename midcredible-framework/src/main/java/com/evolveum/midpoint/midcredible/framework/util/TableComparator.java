package com.evolveum.midpoint.midcredible.framework.util;

import com.evolveum.midpoint.midcredible.framework.TableButler;
import com.evolveum.midpoint.midcredible.framework.util.structural.Attribute;
import com.evolveum.midpoint.midcredible.framework.util.structural.CsvReportPrinter;
import com.evolveum.midpoint.midcredible.framework.util.structural.Identity;
import com.evolveum.midpoint.midcredible.framework.util.structural.Jdbc.Column;
import groovy.lang.GroovyClassLoader;
import org.apache.commons.io.FileUtils;
import org.codehaus.groovy.jsr223.GroovyScriptEngineImpl;
import org.intellij.lang.annotations.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;

public class TableComparator {

    private final DataSource newResource;
    private final DataSource oldResource;
    private final String comparatorPath;
    private static final Logger LOG = LoggerFactory.getLogger(TableComparator.class);

    public TableComparator(TableButler newResource, DataSource OldResource, String comparatorPath){
    this(newResource.getClient().getDataSource(),OldResource, comparatorPath);
    }

    public TableComparator(DataSource newResource, DataSource oldResource, String comparatorPath){
        this.newResource = newResource;
        this.oldResource = oldResource;
        this.comparatorPath = comparatorPath;
    }


//    	public Comparator compare() {
//		try {
//			executeComparison(setupComparator(comparatorPath)) ;
//		} catch (IOException e) {
//			e.printStackTrace();
//		} catch (ScriptException e) {
//			e.printStackTrace();
//		} catch (IllegalAccessException e) {
//			e.printStackTrace();
//		} catch (InstantiationException e) {
//			e.printStackTrace();
//		}
//		finally {
//
//			//TODO clean up
//
//		}
//		return null;
//	}

//    	protected void executeComparison(Comparator comparator) {
//
//        //TODO path from property file
//        CsvReportPrinter reportPrinter = new CsvReportPrinter("./out.csv");
//
//		ResultSet oldRs = null;
//		ResultSet newRs = null;
//		ResultSetMetaData md =null;
//		List attributeList=null;
//
//		try {
//			oldRs = createResultSet(comparator.query(), oldResource);
//			newRs = createResultSet(comparator.query(), newResource);
//             md = oldRs.getMetaData();
//
//            int columns = md.getColumnCount();
//
//            for (int i = 0; i < columns; i++) {
//                attributeList.add(md.getColumnName(i));
//            }
//
//        } catch (SQLException e) {
//            //TODO
//            e.printStackTrace();
//        }
//
//            Identity oldRow;
//            Identity newRow;
//
//try {
//	while (oldRs.next()) {
//		oldRow = createIdentityFromRow(oldRs);
//
//		if (!newRs.next()) {
//			// there's nothing left in new table, old table contains more rows than it should, mark old rows as "-"
////			printCsvRow(printer, "-", oldRs);
//			break;
//		}
//
//		newRow = createIdentityFromRow(newRs);
//		State state = comparator.compareIdentity(oldRow, newRow);
//		switch (state) {
//			case EQUAL:
//				continue;
//			case OLD_BEFORE_NEW:
//				// new table contains row that shouldn't be there, mark new as "+"
//                reportPrinter.printCsvRow(attributeList,newRow);
//				//printCsvRow(printer, "+", newRs);
//				break;
//			case OLD_AFTER_NEW:
//				// new table misses some rows obviously, therefore old row should be marked as "-"
//				//printCsvRow(printer, "-", oldRs);
//				break;
//		}
//	}
//
//	while (newRs.next()) {
//		newRow = createMapFromRow(newRs);
//		// these remaining records are not in old result set, mark new rows as "+"
//		// todo print it out somehow
//		//printCsvRow(printer, "+", newRs);
//	}
//} catch (SQLException e){
//	// TODO
//	LOG.error("Sql exception white iterating trough result set " + e.getLocalizedMessage());
//}
//
//	}

    private Map<Column, Object> createMapFromRow(ResultSet rs) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int columns = md.getColumnCount();

        Map<Column, Object> map = new HashMap<>();
        for (int i = 0; i < columns; i++) {
            map.put(new Column(md.getColumnName(i), i), rs.getObject(i));
        }

        return map;
    }

    private Map<Column, Object> createIdentityFromRow(ResultSet rs, String identifier) throws SQLException {
        Identity identity = new Identity(rs.getObject(identifier).toString());
        ResultSetMetaData md = rs.getMetaData();
        int columns = md.getColumnCount();

        Map<Column, Object> map = new HashMap<>();
        for (int i = 0; i < columns; i++) {

            Attribute attr = new Attribute(md.getColumnName(i));
            Map<Diff, Collection> valueMap = new HashMap();
            attr.setInitialSingleValue(rs.getObject(i));
            identity.setAttributes(attr);
            map.put(new Column(md.getColumnName(i), i), rs.getObject(i));
        }

        return map;
    }

    private Comparator setupComparator(String comparatorPath) throws IOException, ScriptException, IllegalAccessException, InstantiationException {
        File file = new File(comparatorPath);
        String script = FileUtils.readFileToString(file, "utf-8");

        ScriptEngineManager engineManager = new ScriptEngineManager();
        ScriptEngine engine = engineManager.getEngineByName("groovy");

        GroovyScriptEngineImpl gse = (GroovyScriptEngineImpl) engine;
        gse.compile(script);

        Class<? extends Comparator> type = null;

        GroovyClassLoader gcl = gse.getClassLoader();
        for (Class c : gcl.getLoadedClasses()) {
            if (Comparator.class.isAssignableFrom(c)) {
                type = c;
                break;
            }
        }

        if (type == null) {
            throw new IllegalStateException("Couldn't find comparatorPath class that is assignable from Comparator "
                    + ", available classes: " + Arrays.toString(gcl.getLoadedClasses()));
        }

        return type.newInstance();
    }

    private ResultSet createResultSet(String query, DataSource ds) throws SQLException {
        Connection con = ds.getConnection();

        PreparedStatement pstmt = con.prepareStatement(query);
        return pstmt.executeQuery();
    }

}
