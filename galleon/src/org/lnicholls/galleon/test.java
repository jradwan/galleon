package org.lnicholls.galleon;

import java.io.*;
import java.util.*;import java.nio.*;import java.nio.channels.*;
import java.awt.*;
import java.awt.image.*;import javax.imageio.*;
import org.lnicholls.galleon.media.*;
import org.lnicholls.galleon.util.*;import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

import org.clapper.util.misc.*;

import org.apache.log4j.Logger;

import java.sql.*;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
//import javax.sql.rowset.serial.SerialBlob;

import java.util.Properties;

import net.sf.hibernate.*;
import net.sf.hibernate.cfg.Configuration;

import java.sql.Time;
import java.util.Date;

import org.lnicholls.galleon.media.*;
import org.lnicholls.galleon.database.*;

import net.sf.hibernate.tool.hbm2ddl.*;

import java.util.regex.*;


//import org.jd3lib.*;
import pgbennett.id3.*;

import org.apache.commons.digester.Digester;
import org.apache.commons.lang.builder.ToStringBuilder;

import org.dom4j.*;
import org.dom4j.io.*;

/*Conclusions: NIO doesnt matterImage image = Toolkit.getDefaultToolkit().createImage(baos.toByteArray()); is a memory hog
BufferedImage image = ImageIO.read(newInputStream(buf)); is slow (when ImageIO.setUseCache(true); )but uses much less memory

use ImageIO for ALL filesImageIO.setUseCache(false);

FileHashmap much faster to access thumbnails stored in a single file; 50 times faster!!!
*/

public class test
{    private static Logger log = Logger.getLogger(test.class.getName());
    
    private final static Runtime runtime = Runtime.getRuntime();    private final static Button mediaTrackerComp = new Button();
    
    private static void logMemory() {
        System.out.println("Max Memory: " + runtime.maxMemory());
        System.out.println("Total Memory: " + runtime.totalMemory());
        System.out.println("Free Memory: " + runtime.freeMemory());    }
    
    public static void test1()
    {
        ImageIO.setUseCache(false);        
        logMemory();        long start = System.currentTimeMillis();
        FileInputStream is = null;        try
        {
            is = new FileInputStream("test.jpg");            BufferedInputStream bis = new BufferedInputStream(is);
            if (is != null) {                                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] data = new byte[1024];
                int success = bis.read(data);
                while (success != -1) {
                    baos.write(data, 0, success);                    success = bis.read(data);
                }                baos.close();

                java.awt.Image image = Toolkit.getDefaultToolkit().createImage(baos.toByteArray());                try {
                    MediaTracker mt = new MediaTracker(mediaTrackerComp);
                    mt.addImage(image, 0);
                    mt.waitForAll();
                } catch(InterruptedException e) {
                }
                                                //BufferedImage image = ImageIO.read(is);
            }               
        }        catch (Exception ex)        {
            ex.printStackTrace();        }
        finally
        {
            try
            {                if (is!=null)                    is.close();            }            catch (Exception ex) {}        
        }
        System.out.println("Time: "+(System.currentTimeMillis() - start ));
        System.gc();        logMemory();
    }    
    
    public static void test2()
    {
        logMemory();        long start = System.currentTimeMillis();
        FileInputStream is = null;        try
        {            File file = new File("test.jpg");
    
            // Create a read-only memory-mapped file
            FileChannel roChannel = new RandomAccessFile(file, "r").getChannel();
            ByteBuffer buf = roChannel.map(FileChannel.MapMode.READ_ONLY, 0, (int)roChannel.size());
    
            
            // Retrieve all bytes in the buffer
            buf.clear();
            byte[] bytes = new byte[buf.capacity()];
            buf.get(bytes, 0, bytes.length);
            
            java.awt.Image image = Toolkit.getDefaultToolkit().createImage(bytes);            try {
                MediaTracker mt = new MediaTracker(mediaTrackerComp);
                mt.addImage(image, 0);
                mt.waitForAll();
            } catch(InterruptedException e) {
            }                        //BufferedImage image = ImageIO.read(newInputStream(buf));
        }        catch (Exception ex)        {
            ex.printStackTrace();        }
        finally
        {
            try
            {                if (is!=null)                    is.close();            }            catch (Exception ex) {}        
        }
        System.out.println("Time: "+(System.currentTimeMillis() - start ));
        System.gc();        logMemory();
    }   
    
    
    // Returns an output stream for a ByteBuffer.
    // The write() methods use the relative ByteBuffer put() methods.
    public static OutputStream newOutputStream(final ByteBuffer buf) {
        return new OutputStream() {
            public synchronized void write(int b) throws IOException {
                buf.put((byte)b);
            }
    
            public synchronized void write(byte[] bytes, int off, int len) throws IOException {
                buf.put(bytes, off, len);
            }
        };
    }
        // Returns an input stream for a ByteBuffer.
    // The read() methods use the relative ByteBuffer get() methods.
    public static InputStream newInputStream(final ByteBuffer buf) {
        return new InputStream() {
            public synchronized int read() throws IOException {
                if (!buf.hasRemaining()) {
                    return -1;
                }
                return buf.get();
            }
    
            public synchronized int read(byte[] bytes, int off, int len) throws IOException {
                // Read only what's left
                len = Math.min(len, buf.remaining());
                buf.get(bytes, off, len);
                return len;
            }
        };
    }
    
    public static void find(String path, ArrayList list)
    {
        File file = new File(path);
        if (file.isDirectory()) {
            if (path.length() > 0 && !path.endsWith("/")) {
                path += "/";
            }
    	    String files[] = file.list();
            Arrays.sort(files);                
    	    for (int i = 0; i < files.length; i++) {
    	        find(path + files[i],list);
    	    }
        } else if (path.toLowerCase().endsWith(".mp3")) {
            //System.out.println("ADDING: " + path);
            list.add((String)path);
        }
    }    
    
    public static void test3()
    {
        ArrayList list = new ArrayList();
        find("c:/My Music",list);        System.out.println("Total: "+list.size());                int counter = 0;        int iterations = 100;        
        System.gc();
        logMemory();        long start = System.currentTimeMillis();        
        //Hashtable hash = new Hashtable();        File file = new File("somefile.db");
        if (file.exists())
            file.delete();        FileHashMap hash = null;
        try
        {            //FileHashMap hash = new FileHashMap ("somefile");//"D:/Download/hme_sdk_10b1_180095/samples/hashcache");
            hash = new FileHashMap ("somefile", FileHashMap.RECLAIM_FILE_GAPS);
            
            for (int j=0;j<iterations;j++)
            for (int i=0;i<list.size();i++)            {
                //System.out.println(list.get(i));
                String path = (String)list.get(i);                Media media = MediaManager.getMedia(path);
                //System.out.println(proxy);
                hash.put(path+(counter++),media);
                media = null;            } 
            //hash.save();
            //hash.close();        }        catch (Exception ex)         {
            ex.printStackTrace();        }                System.out.println("Time: "+(System.currentTimeMillis() - start ));
        System.gc();        logMemory();

/*        counter = 0;        
        for (int j=0;j<iterations;j++)
            for (int i=0;i<list.size();i++)            {
                //System.out.println(list.get(i));
                String path = (String)list.get(i);                Mp3Proxy proxy = (Mp3Proxy)hash.get(path+(counter++));
                //System.out.println(proxy);
                proxy = null;            }*/        
    }
    
    public static void test4()
    {
        FileInputStream is = null;
        FileHashMap hash = null;        try
        {
            hash = new FileHashMap ("somefile", FileHashMap.RECLAIM_FILE_GAPS);            
            is = new FileInputStream("DSCN0008.JPG");            BufferedInputStream bis = new BufferedInputStream(is);
            if (is != null) {                BufferedImage image = ImageIO.read(is);                
                java.awt.Image cachedImage = image.getScaledInstance(720, 480, java.awt.Image.SCALE_SMOOTH);                try {
                    MediaTracker mt = new MediaTracker(mediaTrackerComp);
                    mt.addImage(cachedImage, 0);
                    mt.waitForAll();
                } catch(InterruptedException e) {
                }
                image.flush();
                image = null;
                
                PixelGrabber grabber = new PixelGrabber(cachedImage, 0, 0, -1, -1, true);
                if(grabber.grabPixels())
                {
                    int width = grabber.getWidth();
                    int height = grabber.getHeight();
                    int pix[] = (int[])grabber.getPixels();
                    
                    hash.put("test",pix);                }    
                                int data[] = (int[])hash.get("test");                                image = new BufferedImage(720, 480, 1);
                image.setRGB(0, 0, 720, 480, data, 0, 720);                                FileOutputStream out = new FileOutputStream(new File("cached.jpg"));
                BufferedOutputStream buffer = new BufferedOutputStream(out, 10 * 1024);

                JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(buffer);
                JPEGEncodeParam param = encoder.getDefaultJPEGEncodeParam(image);
                param.setQuality(1, true);
                encoder.setJPEGEncodeParam(param);
                encoder.encode(image);
                buffer.close();
                buffer = null;                            }               
        }        catch (Exception ex)        {
            ex.printStackTrace();        }                finally
        {
            try
            {                if (is!=null)                    is.close();            }            catch (Exception ex) {}        
        }            
    }
    
    public static void test5()
    {
        FileHashMap hash = null;        try
        {
            File file = new File("somefile.db");
            if (file.exists())
                file.delete();            file = new File("somefile.ix");
            if (file.exists())
                file.delete();                
            hash = new FileHashMap ("somefile", FileHashMap.RECLAIM_FILE_GAPS);                        //JpgProxy proxy = (JpgProxy)MediaProxyFactory.getProxy("DSCN0008.JPG");
            BufferedImage image = JpgFile.getThumbnail("DSCN0008.JPG");
            
            PixelGrabber grabber = new PixelGrabber(image, 0, 0, -1, -1, true);
            if(grabber.grabPixels())
            {
                int counter = 0;
                int width = grabber.getWidth();
                int height = grabber.getHeight();
                int pix[] = (int[])grabber.getPixels();
                    
                for (int i=0;i<100;i++)
                    hash.put("test"+(counter++),pix);                            hash.save();                                System.gc();
                logMemory();                long start = System.currentTimeMillis();
            
                counter = 0;
                for (int i=0;i<100;i++)                {
                
                    int data[] = (int[])hash.get("test"+(counter++));                                            image = new BufferedImage(image.getWidth(null), image.getHeight(null), 1);
                    image.setRGB(0, 0, image.getWidth(null), image.getHeight(null), data, 0, image.getWidth(null));
                }               
                System.out.println("Time: "+(System.currentTimeMillis() - start ));
                System.gc();                logMemory();                            FileOutputStream out = new FileOutputStream(new File("cached.jpg"));
                BufferedOutputStream buffer = new BufferedOutputStream(out, 10 * 1024);
                JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(buffer);
                JPEGEncodeParam param = encoder.getDefaultJPEGEncodeParam(image);
                param.setQuality(1, true);
                encoder.setJPEGEncodeParam(param);
                encoder.encode(image);
                buffer.close();
                buffer = null;
            }
        }        catch (Exception ex)        {
            ex.printStackTrace();        }                finally
        {
        }    
    }
    
    public static void test6()
    {
        System.gc();
        logMemory();
        long start = System.currentTimeMillis();
        
        ArrayList items = new ArrayList();
        //ArrayList items = FileGatherer.gatherDirectory(new File("d:/download/mp3"), FileFilters.audioDirectoryFilter, true);
        //ArrayList items = FileGatherer.gatherDirectory(new File("c:/pics"), FileFilters.imageDirectoryFilter, true);
        log.info("Total MP3's found: " + items.size());
        
        System.out.println("Gather Time: "+(System.currentTimeMillis() - start )/1000f);
        System.gc();
        logMemory();
        start = System.currentTimeMillis();
        
        FileHashMap hash = null;
        try
        {
            File file = new File("somefile.db");
            if (file.exists())
                file.delete();
            file = new File("somefile.ix");
            if (file.exists())
                file.delete();                        
            hash = new FileHashMap ("somefile", FileHashMap.RECLAIM_FILE_GAPS);
            Iterator iterator = items.iterator();
            while (iterator.hasNext())
            {
                String path = (String)iterator.next();
                Media media = MediaManager.getMedia(path);
                hash.put(path,media);
                //System.out.println(proxy);
                media = null;
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }    
        
        System.out.println("Hash Time: "+(System.currentTimeMillis() - start )/1000f);
        System.gc();
        logMemory();
        start = System.currentTimeMillis();
        
        ArrayList sorted = new ArrayList(hash.size());
        for (Iterator i = hash.values().iterator(); i.hasNext(); /* Nothing */) {
            Media media = (Media) i.next();
            try {
                sorted.add(sorted.size(), new FileSorters.SortCollator(media));
            } catch (Exception ex) {
                log.error(ex.getMessage());
            }
        }

        FileSorters.Sort(sorted, FileSorters.titleComparator);
        
        System.out.println("Sorted Time: "+(System.currentTimeMillis() - start )/1000f);
        System.gc();
        logMemory();
        start = System.currentTimeMillis();

        int counter = 0;
        for (Iterator i = sorted.iterator(); i.hasNext(); /* Nothing */) {
            FileSorters.SortCollator sortCollator = (FileSorters.SortCollator) i.next();
            ///System.out.println(sortCollator.getItem());
        }        
        
        log.info("Total MP3's found: " + hash.size());
        
        System.out.println("Time: "+(System.currentTimeMillis() - start )/1000f);
        System.gc();
        logMemory();        
         
    }
    
    public static void test7()
    {
        System.gc();
        logMemory();
        ArrayList items = new ArrayList();
        //ArrayList items = FileGatherer.gatherDirectory(new File("d:/download/mp3"), FileFilters.audioDirectoryFilter, true);
        System.out.println("items="+items.size());
        
        FileList list = new FileList();
        
        long startTime = System.currentTimeMillis();
        for (int i=0;i<100;i++)
        {
        Iterator iterator = items.iterator();
        while (iterator.hasNext())
        {
            String path = (String)iterator.next();
            //System.out.println("path="+path);
            Media media = MediaManager.getMedia(path);
            //System.out.println("proxy="+proxy);
            list.add(1,media);
            //System.out.println(proxy);
            media = null;
        }
        }
        long estimatedTime = System.currentTimeMillis() - startTime;
        System.out.println("Time: "+estimatedTime);
        System.gc();
        logMemory();
        /*
        long shortest = Long.MAX_VALUE;
        long longest = 0;
        long total = 0;
        for (int i=0; i< list.size();i++)
        {
            startTime = System.nanoTime();
            Mp3Proxy proxy = (Mp3Proxy)list.get(i);
            estimatedTime = System.nanoTime() - startTime;
            total = total + estimatedTime;
            if (estimatedTime<shortest)
                shortest = estimatedTime;
            if (estimatedTime>longest)
            {
                longest = estimatedTime;
                System.out.println("proxy="+proxy.getPath()+" : "+i);
                System.out.println("Time: "+estimatedTime);
            }
            //System.out.println("Time: "+estimatedTime);
        }
        
        System.out.println("Shortest: "+shortest);
        System.out.println("Longest: "+longest);
        System.out.println("Avg: "+total/list.size());
        */
        
        System.gc();
        logMemory();
        startTime = System.currentTimeMillis();
        for (int i=0; i< list.size();i++)
        {
            Media media = (Media)list.get(i);
        }
        estimatedTime = System.currentTimeMillis() - startTime;
        System.out.println("Time: "+estimatedTime);
        System.gc();
        logMemory();
    }
    
    static void printSQLError(SQLException e)
    {
        while (e != null)
        {
            System.out.println(e.toString());
            e = e.getNextException();
        }
    }    
    
    public static String escape(String value)
    {
        return value.replaceAll("'","''");
    }
    
    public static void test8()
    {
        System.gc();
        logMemory();
        String framework = "embedded";
        String driver = "org.apache.derby.jdbc.EmbeddedDriver";
        String protocol = "jdbc:derby:";
        
        System.setProperty("derby.system.home", "d:/galleon/data");
        //System.setProperty("derby.storage.pageCacheSize", "40");
        
        long estimatedTime = 0;
        long startTime = 0;
        try
        {
            Class.forName(driver).newInstance();
            System.out.println("Loaded the appropriate driver.");

            Connection conn = null;
            Properties props = new Properties();
            //props.put("user", "galleon");
            //props.put("password", "galleon");

            conn = DriverManager.getConnection(protocol +
                    "galleon;create=true", props);
            
            System.out.println("Connected to and created database derbyDB");
            
            conn.setAutoCommit(false);
            
            Statement s = conn.createStatement(  ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY );
            
            DatabaseMetaData meta = conn.getMetaData();
            String[] tableTypes = new String[1];
            tableTypes[0] = "TABLE";
            ResultSet tables = meta.getTables(null, null, "AUDIO", tableTypes);
            if (tables.next())
            {
                System.out.println("Table Audio already exists");
            }
            else
            {
                String create = "create table binary ("+
                "id int generated always as identity,"+
                "content         blob(50K),"+
                "primary key (id))";            
                
                System.out.println(create);
                s.execute(create);
                System.out.println("Created table binary");            
                
                create = "create table audio ("+
                    "id int generated always as identity,"+
                    "path         varchar(256) default '' not null,"+
                    "title       varchar(100) default 'N/A' not null,"+
                    "artist       varchar(100) default 'N/A' not null,"+
                    "album       varchar(100) default 'N/A' not null,"+
                    "genre       varchar(50) default 'Other' not null,"+
                    "duration     int default -1,"+
                    "date    int default 0,"+
                    "track     int default 1,"+
                    "lastModified timestamp,"+
                    "size         int default 0,"+
                    "bitRate     int default 0,"+
                    "sampleRate  int default 0,"+
                    "copyright     varchar(100) default '' not null,"+
                    "comment     varchar(100) default '' not null,"+
                    "lyrics       varchar(1024) default '' not null,"+
                    "channels    int default 1,"+
                    "vbr    smallint default 0,"+
                    "image int,"+
                    "primary key (id))";
                
                System.out.println(create);
                s.execute(create);
                create = "create index audio_title on audio(title)";
                s.execute(create);
                create = "create index audio_artist on audio(artist)";
                s.execute(create);
                create = "create index audio_album on audio(album)";
                s.execute(create);
                create = "create index audio_genre on audio(genre)";
                s.execute(create);
                create = "create index audio_track on audio(track)";
                s.execute(create);
                System.out.println("Created table audio");
                
                create = "create table image ("+
                "id int generated always as identity,"+
                "path         varchar(256) default '' not null,"+
                "title       varchar(100) default 'N/A' not null,"+
                "lastModified timestamp,"+
                "creationDate timestamp,"+
                "lastChangedDate timestamp,"+            
                "captureDate timestamp,"+            
                "size         int default 0,"+
                "thumbnail int,"+
                "primary key (id))";            
                
                System.out.println(create);
                s.execute(create);
                create = "create index image_title on image(title)";
                s.execute(create);
                create = "create index image_date on image(creationDate)";
                s.execute(create);
                System.out.println("Created table image");
                
                ArrayList items = new ArrayList();
                //ArrayList items = FileGatherer.gatherDirectory(new File("d:/download/mp3/Amber"), FileFilters.audioDirectoryFilter, true);
                System.out.println("items="+items.size());
                
                FileList list = new FileList();
                
                startTime = System.currentTimeMillis();
                
                File imageFile = new File("d:/galleon/media/images/cloudy.png");
                FileChannel roChannel = new RandomAccessFile(imageFile, "r").getChannel();
                ByteBuffer buf = roChannel.map(FileChannel.MapMode.READ_ONLY, 0, (int)roChannel.size());
                buf.clear();
                byte[] bytes = new byte[buf.capacity()];
                buf.get(bytes, 0, bytes.length);
                ByteArrayInputStream is = new ByteArrayInputStream(bytes);
                
                //PreparedStatement pstmt = conn.prepareStatement("insert into mp3(name,year1,location,image) values( ?, ?, ?, ? )" );
                PreparedStatement pstmt = conn.prepareStatement("insert into audio(title,date,path) values( ?, ?, ? )" );
                int counter = 0;
                Iterator iterator = items.iterator();
                while (iterator.hasNext())
                {
                    String path = (String)iterator.next();
                    //System.out.println(""+(counter++)+"path="+path);
                    Media media = MediaManager.getMedia(path);
                    //System.out.println("proxy="+proxy);
                    //String insert = "insert into mp3(name,year1,location) values ('"+escape(proxy.getTitle())+"',"+proxy.getAlbumYear()+",'"+escape(proxy.getTitle())+"')";
                    //System.out.println(insert);
                    //s.execute(insert);
                    
                    pstmt.setString( 1, escape(media.getTitle()));
                    //pstmt.setInt( 2, media.getDate());
                    pstmt.setString( 3, escape(media.getTitle()));
                    
                    //pstmt.setString( 1, "Testing");
                    //pstmt.setInt( 2, 2004);
                    //pstmt.setString( 3, "somewhere");
                    
                    //pstmt.setBinaryStream( 4, is, (int)(imageFile.length()));
                    //SerialBlob blob = new SerialBlob(bytes);
                    //pstmt.setBlob(4, blob);
                    pstmt.executeUpdate();
                    //blob = null;
                    //is.reset();
    
                    //System.out.println(proxy);
                    //proxy = null;                
                    
                    //System.gc();
                    //logMemory();
                }
                is.close();
                estimatedTime = System.currentTimeMillis() - startTime;
                System.out.println("Time: "+estimatedTime);
                
                //s.execute("insert into derbyDB values (1956,'Webster St.')");
            }

            System.gc();
            logMemory();
            startTime = System.currentTimeMillis();
            ResultSet rs = s.executeQuery("SELECT title, date, path FROM audio ORDER BY title");
            System.gc();
            logMemory();
            
            int ndx = rs.getType();
            switch( ndx )
            {
              case ResultSet.TYPE_FORWARD_ONLY:
                      System.out.println("TYPE_FORWARD_ONLY");
                    return;

              case ResultSet.TYPE_SCROLL_INSENSITIVE:
                      System.out.println("TYPE_SCROLL_INSENSITIVE");
                   break;
              default:
                      System.out.println("OTHER");
                   break;
            }            

            FileList list = new FileList();
            int counter = 0;
            while (rs.next())
            {
                String name = rs.getObject( 1 ).toString();
                //System.out.println(name);
                //Blob blob = rs.getBlob(4);
                //int length = (int)blob.length();
                System.out.println(""+(counter++));
                //raf.write(_blob);
                //blob = null;
                //System.gc();
                
                //list.add(name);
                logMemory();
                
                conn.commit();  ///??????????????????????????
            }
            estimatedTime = System.currentTimeMillis() - startTime;
            System.out.println("Time: "+estimatedTime);

            System.out.println("Verified the rows");

            //s.execute("drop table audio");
            System.out.println("Dropped table audio");
            
            //s.execute("drop table image");
            System.out.println("Dropped table image");
            
            //s.execute("drop table binary");
            System.out.println("Dropped table binary");            

            rs.close();
            s.close();
            System.out.println("Closed result set and statement");

            conn.commit();
            conn.close();
            System.out.println("Committed transaction and closed connection");
            
            System.gc();
            logMemory();

            boolean gotSQLExc = false;

            if (framework.equals("embedded"))
            {
                try
                {
                    DriverManager.getConnection("jdbc:derby:;shutdown=true");
                }
                catch (SQLException se)
                {
                    gotSQLExc = true;
                }

                if (!gotSQLExc)
                {
                    System.out.println("Database did not shut down normally");
                }
                else
                {
                    System.out.println("Database shut down normally");
                }
            }
        }
        catch (Throwable e)
        {
            System.out.println("exception thrown:");

            if (e instanceof SQLException)
            {
                printSQLError((SQLException) e);
                e.printStackTrace();
            }
            else
            {
                e.printStackTrace();
            }
        }
        
    }
    
    public static void test9() throws Exception
    {
/*
        // Create a configuration based on the properties file we've put
        // in the standard place.
        Configuration config = new Configuration();

        // Tell it about the classes we want mapped, taking advantage of
        // the way we've named their mapping documents.
        config.addClass(Track.class);

        // Get the session factory we can use for persistence
        SessionFactory sessionFactory = config.buildSessionFactory();

        // Ask for a session using the JDBC information we've configured
        Session session = sessionFactory.openSession();
        Transaction tx = null;
        try {
            // Create some data and persist it
            tx = session.beginTransaction();

            Track track = new Track("Russian Trance",
                                    "vol2/album610/track02.mp3",
                                    new Date(), new Date(),
                                    new Short((short)0));
            session.save(track);

            track = new Track("Video Killed the Radio Star",
                              "vol2/album611/track12.mp3",
                              new Date(), new Date(),
                              new Short((short)0));
            session.save(track);

            
             track = new Track("Gravity's Angel",
                              "vol2/album175/track03.mp3",
                              new Date(), new Date(),
                               new Short((short)0));
            session.save(track);

            // We're done; make our changes permanent
            tx.commit();

        } catch (Exception e) {
            if (tx != null) {
                // Something went wrong; discard all partial changes
                tx.rollback();
            }
            throw e;
        } finally {
            // No matter what, close the session
            session.close();
        }

        // Clean up after ourselves
        sessionFactory.close();
                */
    }

    public static class Location
    {
        public String getId()
        {
            return mId;
        }
        public void setId(String value)
        {
            System.out.println("setId="+value);
            mId = value;
        }
        public String getType()
        {
            return mType;
        }
        public void setType(String value)
        {
            System.out.println("setType="+value);
            mType = value;
        }
        public String getValue()
        {
            return mValue;
        }
        public void setValue(String value)
        {
            System.out.println("setValue="+value);
            mValue = value;
        }        
        
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }
        
        private String mId;
        private String mType;
        private String mValue;
    }
    
    
    public static class Search {
        public Search()
        {
            mLocations = new ArrayList();
        }
        
        public void addLocation(Location location)
        {
            System.out.println("addLocaton="+location);
            mLocations.add(location);
        }
        public Iterator getLocations()
        {
            return mLocations.iterator();
        }
        public String getVersion()
        {
            return mVersion;
        }
        public void setVersion(String version)
        {
            System.out.println("setVersion="+version);
            mVersion = version;
        }
        
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }
        
        private String mVersion;
        private ArrayList mLocations;
      }
    
    public static class Link
    {
        public int getPosition()
        {
            return mPosition;
        }
        public void setPosition(int value)
        {
            System.out.println("setPosition="+value);
            mPosition = value;
        }
        public String getUrl()
        {
            return mUrl;
        }
        public void setUrl(String value)
        {
            System.out.println("setUrl="+value);
            mUrl = value;
        }
        public String getName()
        {
            return mName;
        }
        public void setName(String value)
        {
            System.out.println("setName="+value);
            mName = value;
        }
        
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }        
        
        private int mPosition;
        private String mUrl;
        private String mName;
    }    
    
    public static class CurrentConditions
    {
        public String getLastUpdate()
        {
            return mLastUpdate;
        }
        public void setLastUpdate(String value)
        {
            System.out.println("setLastUpdate="+value);
            mLastUpdate = value;
        }
        public String getStation()
        {
            return mStation;
        }
        public void setStation(String value)
        {
            System.out.println("setStation="+value);
            mStation = value;
        }
        public String getTemperature()
        {
            return mTemperature;
        }
        public void setTemperature(String value)
        {
            System.out.println("setTemperature="+value);
            mTemperature = value;
        }        
        public String getFeelsLike()
        {
            return mFeelsLike;
        }
        public void setFeelsLike(String value)
        {
            System.out.println("setFeelsLike="+value);
            mFeelsLike = value;
        }
        public String getConditions()
        {
            return mConditions;
        }
        public void setConditions(String value)
        {
            System.out.println("setConditions="+value);
            mConditions = value;
        }
        public String getIcon()
        {
            return mIcon;
        }
        public void setIcon(String value)
        {
            System.out.println("setIcon="+value);
            mIcon = value;
        }
        public String getHumidity()
        {
            return mHumidity;
        }
        public void setHumidity(String value)
        {
            System.out.println("setHumidity="+value);
            mHumidity = value;
        }
        public String getVisibility()
        {
            return mVisibility;
        }
        public void setVisibility(String value)
        {
            System.out.println("setVisibility="+value);
            mVisibility = value;
        }
        public String getDewPoint()
        {
            return mDewPoint;
        }
        public void setDewPoint(String value)
        {
            System.out.println("setDewPoint="+value);
            mDewPoint = value;
        }
        public String getBarometricPressure()
        {
            return mBarometricPressure;
        }
        public void setBarometricPressure(String value)
        {
            System.out.println("setBarometricPressure="+value);
            mBarometricPressure = value;
        }        
        public String getBarometricDescription()
        {
            return mBarometricDescription;
        }
        public void setBarometricDescription(String value)
        {
            System.out.println("setBarometricDescription="+value);
            mBarometricDescription = value;
        }
        public String getWindSpeed()
        {
            return mWindSpeed;
        }
        public void setWindSpeed(String value)
        {
            System.out.println("setWindSpeed="+value);
            mWindSpeed = value;
        }                
        public String getWindGusts()
        {
            return mWindGusts;
        }
        public void setWindGusts(String value)
        {
            System.out.println("setWindGusts="+value);
            mWindGusts = value;
        }
        public String getWindDirection()
        {
            return mWindDirection;
        }
        public void setWindDirection(String value)
        {
            System.out.println("setWindDirection="+value);
            mWindDirection = value;
        }
        public String getWindDescription()
        {
            return mWindDescription;
        }
        public void setWindDescription(String value)
        {
            System.out.println("setWindDescription="+value);
            mWindDescription = value;
        }
        public String getUltraVioletIndex()
        {
            return mUltraVioletIndex;
        }
        public void setUltraVioletIndex(String value)
        {
            System.out.println("setUltraVioletIndex="+value);
            mUltraVioletIndex = value;
        }
        public String getUltraVioletDescription()
        {
            return mUltraVioletDescription;
        }
        public void setUltraVioletDescription(String value)
        {
            System.out.println("setUltraVioletDescription="+value);
            mUltraVioletDescription = value;
        }
        public String getMoonPhaseIcon()
        {
            return mMoonPhaseIcon;
        }
        public void setMoonPhaseIcon(String value)
        {
            System.out.println("setMoonPhaseIcon="+value);
            mMoonPhaseIcon = value;
        }
        public String getMoonPhaseDescription()
        {
            return mMoonPhaseDescription;
        }
        public void setMoonPhaseDescription(String value)
        {
            System.out.println("setMoonPhaseDescription="+value);
            mMoonPhaseDescription = value;
        }
        
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }
        
        // TODO Figure out how to get rid of this
        public void setBar(Object value) {}
        public void setWind(Object value) {}
        public void setUv(Object value) {}
        public void setMoon(Object value) {}
        
        private String mLastUpdate;
        private String mStation;
        private String mTemperature;
        private String mFeelsLike;
        private String mConditions;
        private String mIcon;
        private String mHumidity;
        private String mVisibility;
        private String mDewPoint;
        private String mBarometricPressure;
        private String mBarometricDescription;
        private String mWindSpeed;
        private String mWindGusts;
        private String mWindDirection;
        private String mWindDescription;
        private String mUltraVioletIndex;
        private String mUltraVioletDescription;
        private String mMoonPhaseIcon;
        private String mMoonPhaseDescription;
    }    
    
    public static class Forecasts
    {
        public Forecasts()
        {
            mForecast = new ArrayList();
        }        
        public String getLastUpdate()
        {
            return mLastUpdate;
        }
        public void setLastUpdate(String value)
        {
            System.out.println("setLastUpdate="+value);
            mLastUpdate = value;
        }
        public void addForecast(Forecast forecast)
        {
            System.out.println("addForecast="+forecast);
            mForecast.add(forecast);
        }
        public Iterator getForecast()
        {
            return mForecast.iterator();
        }        
        
        private String mLastUpdate;
        private ArrayList mForecast;
    }
    
    public static class Part
    {
        public Part()
        {
            
        }        
        
        public String getIcon()
        {
            return mIcon;
        }
        public void setIcon(String value)
        {
            System.out.println("setIcon="+value);
            mIcon = value;
        }
        public String getHumidity()
        {
            return mHumidity;
        }
        public void setHumidity(String value)
        {
            System.out.println("setHumidity="+value);
            mHumidity = value;
        }
        public String getPrecipitation()
        {
            return mPrecipitation;
        }
        public void setPrecipitation(String value)
        {
            System.out.println("setPrecipitation="+value);
            mPrecipitation = value;
        }        
        public String getDescription()
        {
            return mDescription;
        }
        public void setDescription(String value)
        {
            System.out.println("setDescription="+value);
            mDescription = value;
        }
        public String getWindSpeed()
        {
            return mWindSpeed;
        }
        public void setWindSpeed(String value)
        {
            System.out.println("setWindSpeed="+value);
            mWindSpeed = value;
        }                
        public String getWindGusts()
        {
            return mWindGusts;
        }
        public void setWindGusts(String value)
        {
            System.out.println("setWindGusts="+value);
            mWindGusts = value;
        }
        public String getWindDirection()
        {
            return mWindDirection;
        }
        public void setWindDirection(String value)
        {
            System.out.println("setWindDirection="+value);
            mWindDirection = value;
        }
        public String getWindDescription()
        {
            return mWindDescription;
        }
        public void setWindDescription(String value)
        {
            System.out.println("setWindDescription="+value);
            mWindDescription = value;
        }
        
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }
        
        // TODO
        public void setWind(Object value) {}
        
        private String mIcon;
        private String mHumidity;
        private String mPrecipitation;
        private String mDescription;
        private String mWindSpeed;
        private String mWindGusts;
        private String mWindDirection;
        private String mWindDescription;
    }    
    
    public static class Forecast
    {
        public Forecast()
        {
            
        }
        
        public String getHigh()
        {
            return mHigh;
        }
        public void setHigh(String value)
        {
            System.out.println("setHigh="+value);
            mHigh = value;
        }
        public String getLow()
        {
            return mLow;
        }
        public void setLow(String value)
        {
            System.out.println("setLow="+value);
            mLow = value;
        }
        public String getSunrise()
        {
            return mSunrise;
        }
        public void setSunrise(String value)
        {
            System.out.println("setSunrise="+value);
            mSunrise = value;
        }        
        public String getSunset()
        {
            return mSunset;
        }
        public void setSunset(String value)
        {
            System.out.println("setSunset="+value);
            mSunset = value;
        }        
        public String getDay()
        {
            return mDay;
        }
        public void setDay(String value)
        {
            System.out.println("setDay="+value);
            mDay = value;
        }
        public String getDescription()
        {
            return mDescription;
        }
        public void setDescription(String value)
        {
            System.out.println("setDescription="+value);
            mDescription = value;
        }
        public String getDate()
        {
            return mDate;
        }
        public void setDate(String value)
        {
            System.out.println("setDate="+value);
            mDate = value;
        }
        public void addDayPart(Part part)
        {
            mDayPart = part;
        }
        public void addNightPart(Part part)
        {
            mNightPart = part;
        }        
        public Part getDayForecast()
        {
            return mDayPart;
        }
        public Part getNightForecast()
        {
            return mNightPart;
        }
        
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }
        
        private String mHigh;
        private String mLow;
        private String mSunrise;
        private String mSunset;
        private String mDay;
        private String mDescription;
        private String mDate;
        private Part mDayPart;
        private Part mNightPart;
    }    
    
    public static class Weather {
        public Weather()
        {
            mLinks = new ArrayList();
        }
        public String getLocale()
        {
            return mLocale;
        }
        public void setLocale(String value)
        {
            System.out.println("setLocale="+value);
            mLocale = value;
        }
        public String getTemperatureUnit()
        {
            return mTemperatureUnit;
        }
        public void setTemperatureUnit(String value)
        {
            System.out.println("setTemperatureUnit="+value);
            mTemperatureUnit = value;
        }
        public String getDistanceUnit()
        {
            return mDistanceUnit;
        }
        public void setDistanceUnit(String value)
        {
            System.out.println("setDistanceUnit="+value);
            mDistanceUnit = value;
        }
        public String getSpeedUnit()
        {
            return mSpeedUnit;
        }
        public void setSpeedUnit(String value)
        {
            System.out.println("setSpeedUnit="+value);
            mSpeedUnit = value;
        }        
        public String getPrecipitationUnit()
        {
            return mPrecipitationUnit;
        }
        public void setPrecipitationUnit(String value)
        {
            System.out.println("setPrecipitationUnit="+value);
            mPrecipitationUnit = value;
        }        
        public String getPressureUnit()
        {
            return mPressureUnit;
        }
        public void setPressureUnit(String value)
        {
            System.out.println("setPressureUnit="+value);
            mPressureUnit = value;
        }        
        public String getFormat()
        {
            return mFormat;
        }
        public void setFormat(String value)
        {
            System.out.println("setFormat="+value);
            mFormat = value;
        }
        public String getId()
        {
            return mId;
        }
        public void setId(String value)
        {
            System.out.println("setId="+value);
            mId = value;
        }
        public String getName()
        {
            return mName;
        }
        public void setName(String value)
        {
            System.out.println("setName="+value);
            mName = value;
        }
        public String getTime()
        {
            return mTime;
        }
        public void setTime(String value)
        {
            System.out.println("setTime="+value);
            mTime = value;
        }
        public String getLatitude()
        {
            return mLatitude;
        }
        public void setLatitude(String value)
        {
            System.out.println("setLatitude="+value);
            mLatitude = value;
        }
        public String getLongitude()
        {
            return mLongitude;
        }
        public void setLongitude(String value)
        {
            System.out.println("setLongitude="+value);
            mLongitude = value;
        }
        public String getSunrise()
        {
            return mSunrise;
        }
        public void setSunrise(String value)
        {
            System.out.println("setSunrise="+value);
            mSunrise = value;
        }        
        public String getSunset()
        {
            return mSunset;
        }
        public void setSunset(String value)
        {
            System.out.println("setSunset="+value);
            mSunset = value;
        }        
        public String getTimeZone()
        {
            return mTimeZone;
        }
        public void setTimeZone(String value)
        {
            System.out.println("setTimeZone="+value);
            mTimeZone = value;
        }        
        
        public CurrentConditions getCurrentConditions()
        {
            return mCurrentConditions;
        }
        public void setCurrentConditions(CurrentConditions value)
        {
            System.out.println("setCurrentConditions="+value);
            mCurrentConditions = value;
        }
        public Forecasts getForecasts()
        {
            return mForecasts;
        }
        public void setForecasts(Forecasts value)
        {
            System.out.println("setForecasts="+value);
            mForecasts = value;
        }        
        
        public void addLink(Link link)
        {
            System.out.println("addLink="+link);
            mLinks.add(link);
        }
        public Iterator getLinks()
        {
            return mLinks.iterator();
        }
        public String getVersion()
        {
            return mVersion;
        }
        public void setVersion(String version)
        {
            System.out.println("setVersion="+version);
            mVersion = version;
        }
        
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }
        
        private String mVersion;
        // head
        private String mLocale;
        private String mTemperatureUnit;
        private String mDistanceUnit;
        private String mSpeedUnit;
        private String mPrecipitationUnit;
        private String mPressureUnit;
        private String mFormat;
        // loc
        private String mId;
        private String mName;
        private String mTime;
        private String mLatitude;
        private String mLongitude;
        private String mSunrise;
        private String mSunset;
        private String mTimeZone;

        private ArrayList mLinks;
        private CurrentConditions mCurrentConditions;
        private Forecasts mForecasts;
      } 
    
    public static class WeatherRule extends org.apache.commons.digester.Rule {

        public WeatherRule(Weather weather) {
            mWeather = weather;
        }
        
        public void begin(String namespace, String name, org.xml.sax.Attributes attrs) {
            digester.push(mWeather);
        }
        
        private Weather mWeather;
    }    
    
    
    public static String getAttribute(Element element, String name)
    {
        String value = element.attributeValue( name );
        if (value==null)
        {
            Element child = element.element( name );
            if (child!=null)
                return child.getText();
        }
        return value;
    }
    
    public static void main(String[] args) throws Exception    {        log.info("Testing");
        //test1();
        //test2();        //test3();
        //test4();
        //test5();
        //test6();
        //test7();
        //test8();
        //test9();
        
/*        
        Session session = HibernateUtil.currentSession();
        
        Transaction tx= session.beginTransaction();

        Track track = new Track("Lalalalaalal",
                "lalalalala.mp3",
                new Date(), new Date(),
                 new Short((short)0));

        session.save(track);
        tx.commit();
        
        tx = session.beginTransaction();

        System.gc();
        logMemory();
        long startTime = System.currentTimeMillis();
        
        Query query = session.createQuery("select c from Track as c where c.title = :title");
        query.setString("title", "Lalalalaalal");
        for (Iterator it = query.iterate(); it.hasNext();) {
            track = (Track) it.next();
            System.out.println("Track: " + track.getTitle() );
        }
        
        long  estimatedTime = System.currentTimeMillis() - startTime;
        System.out.println("Time: "+estimatedTime);
        System.gc();
        logMemory();

        tx.commit();
        

        HibernateUtil.closeSession();
*/        
/*
        try
        {
            Configuration config = new Configuration();
            config.addClass(Track.class);
            Properties properties = new Properties();
            properties.load( new FileInputStream(new File("d:/galleon/conf/hibernate.properties")) );
            SchemaUpdate schemaUpdate = new SchemaUpdate(config, properties);
            schemaUpdate.execute(true,true);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
*/        
        /*
        try
        {
            Configuration config = new Configuration();
            config.addClass(Audio.class);
            Properties properties = new Properties();
            properties.load( new FileInputStream(new File("d:/galleon/conf/hibernate.properties")) );
            SchemaExport schemaExport = new SchemaExport(config, properties);
            schemaExport.create(true,true);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        */
        /*
        System.gc();
        logMemory();
        
        ArrayList items = FileGatherer.gatherDirectory(new File("d:/download/mp3"), FileFilters.audioDirectoryFilter, true);
        
        long startTime = System.currentTimeMillis();
        Iterator iterator = items.iterator();
        while (iterator.hasNext())
        {
            String path = (String)iterator.next();
            Mp3Proxy proxy = (Mp3Proxy)MediaProxyFactory.getProxy(path);
            Audio audio = new Audio(proxy.getTitle(), proxy.getArtist(), proxy.getAlbum(), proxy.getGenre(), proxy.getDuration(), proxy.getSize(), (short)proxy.getDate(), (short)proxy.getTrack(), (short)proxy.getBitRate(), proxy.getSampleRate(), (short)proxy.getChannels(), proxy.getMimeType(), (short)0, proxy.getPath(), (short)0, (short)0);
            AudioManager.createAudio(audio);
            //System.out.println(audio.getPath());
        }
        long estimatedTime = System.currentTimeMillis() - startTime;
        System.out.println("Time: "+estimatedTime);
        System.gc();
        logMemory();
        */
        /*
        HibernateUtil.initialize();
        
        System.gc();
        logMemory();
        
        long startTime = System.currentTimeMillis();
        java.util.List items = AudioManager.listBetween(1,4110);
        
        int counter = 0;
        Iterator iterator = items.iterator();
        while (iterator.hasNext())
        {
            Audio audio = (Audio)iterator.next();
            //System.out.println(""+(++counter)+":"+audio.getPath());
        }
        long estimatedTime = System.currentTimeMillis() - startTime;
        System.out.println("Time: "+estimatedTime);
        System.gc();
        logMemory();
*/        
        //Mp3Proxy proxy = (Mp3Proxy)MediaProxyFactory.getProxy("d:/download/mp3/10,000 Maniacs - MTV Unplugged/10,000 Maniacs - Because The Night.mp3");
        //System.out.println(proxy);
        
        //Mp3Proxy proxy = (Mp3Proxy)MediaProxyFactory.getProxy("d:/My Music/07 October.mp3");
        //System.out.println(proxy);
        
        System.setProperty("data", "d:/galleon/data");
        System.setProperty("conf", "d:/galleon/conf");
        System.setProperty("logs", "d:/galleon/logs");

/*        
        NetworkServerManager.initialize();
        
        HibernateUtil.initialize();
        if (NetworkServerManager.findSchema())
            HibernateUtil.updateSchema();
        else
            HibernateUtil.createSchema();
        
        System.gc();
        logMemory();
        
        long startTime = System.currentTimeMillis();
        FileGatherer.gatherDirectory(new File("d:/download/mp3"), FileFilters.audioDirectoryFilter, true, 
                new FileGatherer.GathererCallback()
                        {
                            public void visit(File file)
                            {
                                try
                                {
                                    Audio audio = (Audio)MediaManager.getMedia(file.getAbsolutePath());
                                    AudioManager.createAudio(audio);
                                }
                                catch (Exception ex)
                                {
                                    ex.printStackTrace();
                                    System.out.println(file.getAbsolutePath());
                                }
                            }
                        }
                        );
        
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Press [Enter] to stop Server 1");
        in.readLine();        
*/        
                        
        /*
        MediaRefreshThread mediaRefreshThread = new MediaRefreshThread();        
        mediaRefreshThread.addPath(new MediaRefreshThread.PathInfo("d:/download/mp3",FileFilters.audioDirectoryFilter));
        mediaRefreshThread.start();
*/
/*        
        try {
            AudioManager.scroll(new AudioManager.Callback() {
                int counter = 0;
                public void visit(Session session, Audio audio) {
                    File file = new File(audio.getPath());
                    if (!file.exists())
                    {
                        if (log.isDebugEnabled())
                            log.debug("Removed: "+file.getAbsolutePath());
                        
                        try
                        {
                            session.delete(audio);
                            Thread.sleep(10);  // give the CPU some breathing time
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                    //System.out.println(counter++);
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }        
                
        long estimatedTime = System.currentTimeMillis() - startTime;
        System.out.println("Time: "+estimatedTime);
        System.gc();
        logMemory();
        

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Press [Enter] to stop Server 1");
        in.readLine();
        
*/                        
        
        //Mp3Proxy proxy = (Mp3Proxy)MediaProxyFactory.getProxy("d:/download/mp3/10,000 Maniacs - MTV Unplugged/10,000 Maniacs - Because The Night.mp3");
        //System.out.println(proxy);
        
        //Mp3Proxy proxy = (Mp3Proxy)MediaProxyFactory.getProxy("d:/My Music/07 October.mp3");
        
        //RandomAccessFile in = new RandomAccessFile("d:/My Music/07 October.mp3","r");
 /*       
        MP3File mp3File = new MP3File("d:/My Music/07 October.mp3");
        
        System.out.println("hasID3v2="+mp3File.hasID3v2()); 
        System.out.println("getID3v2="+mp3File.getID3v2());
        System.out.println("getID3v1="+mp3File.getID3v1());
        
        MetaData metaData = mp3File.getMetaData();
        
        System.out.println("title="+metaData.getTitle());
        System.out.println("artist="+metaData.getArtist());
        System.out.println("album="+metaData.getAlbum());
        System.out.println("year="+metaData.getYear());
        System.out.println("comment="+metaData.getComment());
        System.out.println("track="+metaData.getTrack());
        System.out.println("genre="+metaData.getGenre());
*/
/*
        MP3File mp3File = new MP3File();
        try {
            mp3File.init(new File("d:/My Music/07 October.mp3"),MP3File.BOTH_TAGS);
        }
        catch(ID3Exception ex) {
            ex.printStackTrace();
        }
        
        System.out.println("id3v2Exists="+mp3File.id3v2Exists()); 
        System.out.println("id3v1Exists="+mp3File.id3v1Exists());
        
        System.out.println("title="+mp3File.getTitle());
        System.out.println("artist="+mp3File.getArtist());
        System.out.println("album="+mp3File.getAlbum());
        System.out.println("year="+mp3File.getYear());
        System.out.println("comment="+mp3File.getComment());
        System.out.println("track="+mp3File.getTrack());
        System.out.println("genre="+mp3File.getGenre());
*/

/*        
        String filename = "d:/galleon/location.xml";
        
        Digester digester = new Digester();
        digester.setValidating(false);
        
        digester.addObjectCreate("search", "org.lnicholls.galleon.test$Search");
        digester.addSetProperties("search", "ver", "version");
        digester.addObjectCreate("search/loc", "org.lnicholls.galleon.test$Location");
        digester.addSetProperties("search/loc");
        digester.addCallMethod("search/loc", "setValue", 0);
        digester.addSetNext("search/loc", "addLocation", "org.lnicholls.galleon.test$Location");
*/
        Search search = new Search();
        
        SAXReader saxReader = new SAXReader();
        Document document = saxReader.read(new File("d:/galleon/location.xml"));
        
        // search
        Element root = document.getRootElement();
        search.setVersion(getAttribute(root,"ver"));
        
        for ( Iterator i = root.elementIterator("loc"); i.hasNext(); ) {
            Element element = (Element) i.next();
            Location location = new Location();
            location.setId(getAttribute(element,"id"));
            location.setType(getAttribute(element,"type"));
            location.setValue(element.getText());
            search.addLocation(location);
        }
        
        System.out.println(search);
        
/*        
        String filename = "d:/galleon/weather.xml";
        
        Digester digester = new Digester();
        digester.setValidating(false);
        
        //digester.addObjectCreate("weather", "org.lnicholls.galleon.test$Weather");
        Weather weather = new Weather();
        WeatherRule weatherRule = new WeatherRule(weather);
        digester.addRule("weather", weatherRule);
        digester.addSetProperties("weather", "ver", "version");
        // head
        digester.addSetNestedProperties("weather/head", new String[]{"ut","ud","us","up","ur","form"}, new String[]{"temperatureUnit","distanceUnit","speedUnit","precipitationUnit","pressureUnit","format"});
        // loc
        digester.addSetNestedProperties("weather/loc", new String[]{"dnam","tm","lat","lon","sunr","suns","zone"}, new String[]{"name","time","latitude","longitude","sunrise","sunset","timeZone"});
        // cc
        digester.addObjectCreate("weather/cc", "org.lnicholls.galleon.test$CurrentConditions");
        digester.addSetNestedProperties("weather/cc", new String[]{"lsup","obst","tmp","flik","t","icon","hmid", "vis", "dewp"}, new String[]{"lastUpdate","station","temperature","feelsLike","conditions","icon","humidity","visibility","dewPoint"});
        digester.addSetNestedProperties("weather/cc/bar", new String[]{"r","d"}, new String[]{"barometricPressure","barometricDescription"});
        digester.addSetNestedProperties("weather/cc/wind", new String[]{"s","gust","d","t"}, new String[]{"windSpeed","windGusts","windDirection","windDescription"});
        digester.addSetNestedProperties("weather/cc/uv", new String[]{"i","t"}, new String[]{"ultraVioletIndex","ultraVioletDescription"});        
        digester.addSetNestedProperties("weather/cc/moon", new String[]{"icon","t"}, new String[]{"moonPhaseIcon","moonPhaseDescription"});
        digester.addSetNext("weather/cc", "setCurrentConditions", "org.lnicholls.galleon.test$CurrentConditions");        
        
        // dayf
        digester.addObjectCreate("weather/dayf", "org.lnicholls.galleon.test$Forecasts");
        digester.addSetNestedProperties("weather/dayf", new String[]{"lsup"}, new String[]{"lastUpdate"});
        digester.addObjectCreate("weather/dayf/day", "org.lnicholls.galleon.test$Forecast");
        digester.addSetProperties("weather/dayf/day", new String[]{"d","t","dt"}, new String[]{"day","description","date"});
        digester.addSetNestedProperties("weather/dayf/day", new String[]{"hi","low","sunr","suns"}, new String[]{"high","low","sunrise","sunset"});
        digester.addObjectCreate("weather/dayf/day/part", "org.lnicholls.galleon.test$Part");
        digester.addSetProperties("weather/dayf/day/part", "p", "which");
        digester.addSetNestedProperties("weather/dayf/day/part", new String[]{"icon","t","bt","ppcp","hmid"}, new String[]{"icon","description","bt","precipitation","humidity"});
        digester.addSetNestedProperties("weather/dayf/day/part/wnd", new String[]{"s","gust","d","t"}, new String[]{"windSpeed","windGusts","windDirection","windDescription"});
        //digester.addSetNext("weather/dayf/day/part", "addPart", "org.lnicholls.galleon.test$Part");
        digester.addSetNext("weather/dayf/day", "addForecast", "org.lnicholls.galleon.test$Forecast");
        digester.addSetNext("weather/dayf", "setForecasts", "org.lnicholls.galleon.test$Forecasts");
        
        // lnks
        digester.addObjectCreate("weather/lnks/link", "org.lnicholls.galleon.test$Link");
        digester.addSetProperties("weather/lnks/link");
        digester.addSetNestedProperties("weather/lnks/link", new String[]{"l","t"}, new String[]{"url","name"});
        digester.addSetNext("weather/lnks/link", "addLink", "org.lnicholls.galleon.test$Link");
        
        // Process the input file.
        try {
            java.io.File srcfile = new java.io.File(filename);
            weather = (Weather) digester.parse(srcfile);
            System.out.println(weather.toString());
        }
        catch(java.io.IOException ioe) {
            System.out.println("Error reading input file:" + ioe.getMessage());
            System.exit(-1);
        }
        catch(org.xml.sax.SAXException se) {
            System.out.println("Error parsing input file:" + se.getMessage());
            System.exit(-1);
        }
*/
        /*
        Weather weather = new Weather();
        
        SAXReader saxReader = new SAXReader();
        Document document = saxReader.read(new File("d:/galleon/weather.xml"));
        
        //Node node = document.selectSingleNode( "//weather" );
        
        //System.out.println(weather);
        
        // weather
        Element root = document.getRootElement();
        System.out.println(root);
        weather.setVersion(getAttribute(root,"ver"));
        
        //Element element = root.element( "head" );
        //System.out.println(getAttribute(element, "locale"));
        //node = node.selectSingleNode( "locale" );
        //System.out.println(node.getText());
        
        //System.out.println(getAttribute(node, "locale"));
        
        for ( Iterator i = root.elementIterator(); i.hasNext(); ) {
            Element element = (Element) i.next();
            if (element.getName().equals("head"))
            {
                weather.setLocale(getAttribute(element,"locale"));
                weather.setTemperatureUnit(getAttribute(element,"ut"));
                weather.setDistanceUnit(getAttribute(element,"ud"));
                weather.setSpeedUnit(getAttribute(element,"us"));
                weather.setPrecipitationUnit(getAttribute(element,"up"));
                weather.setPressureUnit(getAttribute(element,"ur"));
                weather.setFormat(getAttribute(element,"form"));
            }
            else
            if (element.getName().equals("loc")) {
                weather.setId(getAttribute(element,"id"));
                weather.setName(getAttribute(element,"dnam"));
                weather.setTime(getAttribute(element,"tm"));
                weather.setLatitude(getAttribute(element,"lat"));
                weather.setLongitude(getAttribute(element,"lon"));
                weather.setSunrise(getAttribute(element,"sunr"));
                weather.setSunset(getAttribute(element,"suns"));
                weather.setTimeZone(getAttribute(element,"zone"));
            } else if (element.getName().equals("lnks")) {
                for ( Iterator linksIterator = element.elementIterator(); linksIterator.hasNext(); ) {
                    Element linkElement = (Element) linksIterator.next();
                    Link link = new Link();
                    link.setUrl(getAttribute(linkElement,"l"));
                    link.setName(getAttribute(linkElement,"t"));
                    weather.addLink(link);
                }
            } else if (element.getName().equals("cc")) {
                CurrentConditions currentConditions = new CurrentConditions();
                currentConditions.setLastUpdate(getAttribute(element,"lsup"));
                currentConditions.setStation(getAttribute(element,"obst"));
                currentConditions.setTemperature(getAttribute(element,"tmp"));
                currentConditions.setFeelsLike(getAttribute(element,"flik"));
                currentConditions.setConditions(getAttribute(element,"t"));
                currentConditions.setIcon(getAttribute(element,"icon"));
                currentConditions.setHumidity(getAttribute(element,"hmid"));
                currentConditions.setVisibility(getAttribute(element,"vis"));
                currentConditions.setDewPoint(getAttribute(element,"dewp"));
                
                Element child = element.element( "bar" );
                if (child!=null)
                {
                    currentConditions.setBarometricPressure(getAttribute(child,"r"));
                    currentConditions.setBarometricDescription(getAttribute(child,"d"));
                }
                child = element.element( "wind" );
                if (child!=null)
                {
                    currentConditions.setWindSpeed(getAttribute(child,"s"));
                    currentConditions.setWindGusts(getAttribute(child,"gust"));
                    currentConditions.setWindDirection(getAttribute(child,"d"));
                    currentConditions.setWindDescription(getAttribute(child,"t"));                    
                }
                child = element.element( "uv" );
                if (child!=null)
                {
                    currentConditions.setUltraVioletIndex(getAttribute(child,"i"));
                    currentConditions.setUltraVioletDescription(getAttribute(child,"t"));
                }
                child = element.element( "moon" );
                if (child!=null)
                {
                    currentConditions.setMoonPhaseIcon(getAttribute(child,"icon"));
                    currentConditions.setMoonPhaseDescription(getAttribute(child,"t"));
                }
                weather.setCurrentConditions(currentConditions);
            } else if (element.getName().equals("dayf")) {
                Forecasts forecasts = new Forecasts();
                forecasts.setLastUpdate(getAttribute(element,"lsup"));
                
                for ( Iterator dayIterator = element.elementIterator("day"); dayIterator.hasNext(); ) {
                    Element dayElement = (Element) dayIterator.next();
                    Forecast forecast = new Forecast();

                    forecast.setDay(getAttribute(dayElement,"d"));
                    forecast.setDescription(getAttribute(dayElement,"t"));
                    forecast.setDate(getAttribute(dayElement,"dt"));
                    forecast.setHigh(getAttribute(dayElement,"hi"));
                    forecast.setLow(getAttribute(dayElement,"low"));
                    forecast.setSunrise(getAttribute(dayElement,"sunr"));
                    forecast.setSunset(getAttribute(dayElement,"suns"));
                    
                    for ( Iterator partIterator = dayElement.elementIterator("part"); partIterator.hasNext(); ) 
                    {
                        Element partElement = (Element) partIterator.next();
                        Part part = new Part();
                        part.setIcon(getAttribute(partElement,"icon"));
                        part.setDescription(getAttribute(partElement,"t"));
                        part.setPrecipitation(getAttribute(partElement,"ppcp"));
                        part.setHumidity(getAttribute(partElement,"hmid"));
                        
                        Element windElement = partElement.element( "wind" );
                        if (windElement!=null)
                        {
                            part.setWindSpeed(getAttribute(windElement,"s"));
                            part.setWindGusts(getAttribute(windElement,"gust"));
                            part.setWindDirection(getAttribute(windElement,"d"));
                            part.setWindDescription(getAttribute(windElement,"t"));
                        }
                        
                        String which = getAttribute(partElement,"p");
                        if (which.equals("n"))
                        {
                            forecast.addNightPart(part);
                        }
                        else
                            forecast.addDayPart(part);
                    }
                    
                    forecasts.addForecast(forecast);
                }
                weather.setForecasts(forecasts);
            }
        }

        System.out.println(weather);
                */
        
        //node = document.selectSingleNode( "//weather/head/locale" );
        //System.out.println(node.getStringValue());
        
        
   }
/*    
    private static void addRules(Digester d) {

        //--------------------------------------------------        
        // when we encounter a "person" tag, do the following:

        // create a new instance of class Person, and push that
        // object onto the digester stack of objects
        d.addObjectCreate("address-book/person", Person.class);
        
        // map *any* attributes on the tag to appropriate
        // setter-methods on the top object on the stack (the Person
        // instance created by the preceeding rule). 
        //
        // For example:
        // if attribute "id" exists on the xml tag, and method setId 
        // with one parameter exists on the object that is on top of
        // the digester object stack, then a call will be made to that
        // method. The value will be type-converted from string to
        // whatever type the target method declares (where possible), 
        // using the commons ConvertUtils functionality.
        //
        // Attributes on the xml tag for which no setter methods exist
        // on the top object on the stack are just ignored.
        d.addSetProperties("address-book/person");

        // call the addPerson method on the second-to-top object on
        // the stack (the AddressBook object), passing the top object
        // on the stack (the recently created Person object).
        d.addSetNext("address-book/person", "addPerson");        
        
        //--------------------------------------------------        
        // when we encounter a "name" tag, call setName on the top
        // object on the stack, passing the text contained within the
        // body of that name element [specifying a zero parameter count
        // implies one actual parameter, being the body text]. 
        // The top object on the stack will be a person object, because 
        // the pattern address-book/person always triggers the 
        // ObjectCreateRule we added previously.
        d.addCallMethod("address-book/person/name", "setName", 0);
        
        //--------------------------------------------------        
        // when we encounter an "email" tag, call addEmail on the top
        // object on the stack, passing two parameters: the "type"
        // attribute, and the text within the tag body.
        d.addCallMethod("address-book/person/email", "addEmail", 2);
        d.addCallParam("address-book/person/email", 0, "type");
        d.addCallParam("address-book/person/email", 1);
        
        //--------------------------------------------------        
        // When we encounter an "address" tag, create an instance of class
        // Address and push it on the digester stack of objects. After
        // doing that, call addAddress on the second-to-top object on the
        // digester stack (a "Person" object), passing the top object on
        // the digester stack (the "Address" object). And also set things
        // up so that for each child xml element encountered between the start
        // of the address tag and the end of the address tag, the text 
        // contained in that element is passed to a setXXX method on the 
        // Address object where XXX is the name of the xml element found.
        d.addObjectCreate("address-book/person/address", Address.class);
        d.addSetNext("address-book/person/address", "addAddress");
        d.addSetNestedProperties("address-book/person/address");
    }
        */
}


